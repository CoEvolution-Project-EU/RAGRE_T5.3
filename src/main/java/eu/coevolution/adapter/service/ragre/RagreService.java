package eu.coevolution.adapter.service.ragre;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import eu.coevolution.adapter.dto.AssessmentReportDTO;
import eu.coevolution.adapter.dto.AssetDTO;
import eu.coevolution.adapter.dto.BasicWeaknessDTO;
import eu.coevolution.adapter.dto.ComponentDTO;
import eu.coevolution.adapter.dto.MitigationDTO;
import eu.coevolution.adapter.dto.ThreatDTO;
import eu.coevolution.adapter.dto.VulnerabilityDTO;
import eu.coevolution.adapter.dto.VulnerabilityWithMitigationsDTO;
import eu.coevolution.adapter.dto.WeaknessDTO;
import eu.coevolution.adapter.enums.VulnerabilitySourceInfo;
import eu.coevolution.adapter.feign.MitigateClient;
import eu.coevolution.adapter.response.ApiResponse;
import eu.coevolution.adapter.response.PagedApiResponse;
import eu.coevolution.adapter.service.report.ReportBuilderUtils;
import feign.FeignException;
import org.cyclonedx.CycloneDxSchema;
import org.cyclonedx.generators.BomGeneratorFactory;
import org.cyclonedx.model.Bom;
import org.cyclonedx.model.Component;
import org.cyclonedx.model.Metadata;
import org.cyclonedx.model.Property;
import org.cyclonedx.model.Tool;
import org.cyclonedx.model.vulnerability.Vulnerability;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Service
public class RagreService {

    private static final Logger logger = LoggerFactory.getLogger(RagreService.class);
    private static final String REPORT_SERIAL_URN_PREFIX = "urn:uuid:";
    private static final String PENDING_VALUE = "pending";
    private static final int POLL_MAX_ATTEMPTS = 30;
    private static final long POLL_SLEEP_MS = 2000L;
    private static final Pageable ENRICHMENT_PAGEABLE = PageRequest.of(0, 1000);

    @Autowired
    private MitigateClient mitigateClient;

    @Autowired
    private ScanService scanService;

    @Autowired
    private AiRiskScoringService aiRiskScoringService;

    private final ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();

    public AssetDTO createAsset(String asset, MultipartFile file, String scanInfoJson) throws JsonProcessingException {
        Bom sbom = scanService.generateSbom(file, scanInfoJson);
        if (sbom == null) {
            return null;
        }
        return createAssetFromBom(asset, sbom);
    }

    public Bom generateUnifiedReport(String asset, MultipartFile file) throws JsonProcessingException {
        Bom sourceBom = scanService.generateSbomForUnifiedReport(file);
        if (sourceBom == null) {
            return null;
        }

        AssetDTO createdAsset = createAssetFromBom(asset, sourceBom);
        if (createdAsset == null || createdAsset.getId() == null) {
            return sourceBom;
        }

        AssetDTO processedAsset = waitForAssetReady(createdAsset.getId());
        if (processedAsset == null) {
            return sourceBom;
        }

        List<ComponentDTO> assetComponents = findAssetComponents(processedAsset.getId());
        Map<String, VulnerabilityWithMitigationsDTO> mitigationsByVulnerability = toVulnerabilityMitigationsMap(findVulnerabilityWithMitigationsByAsset(processedAsset.getId()));

        Bom reportBom = initializeReportBom(sourceBom, processedAsset, assetComponents);
        enrichBom(reportBom, assetComponents, mitigationsByVulnerability);
        return reportBom;
    }

    public String generateUnifiedReportJson(String asset, MultipartFile file) throws JsonProcessingException {
        Bom reportBom = generateUnifiedReport(asset, file);
        if (reportBom == null) {
            return null;
        }
        String reportJson = BomGeneratorFactory.createJson(CycloneDxSchema.VERSION_LATEST, reportBom).toJsonString();
        return alignMetadataToolsToCycloneDx17(reportJson);
    }

    public double calculateAggregatedAiRiskScore(List<AssessmentReportDTO> reports) {
        return aiRiskScoringService.scoreAssessmentReports(reports);
    }

    public List<ComponentDTO> findAssetComponents(String assetId) {
        ApiResponse apiResponse = mitigateClient.findAssetComponents(assetId, null, null, ENRICHMENT_PAGEABLE);
        return convertPagedResponse(apiResponse, ComponentDTO.class);
    }

    public PagedApiResponse<ComponentDTO> findAssetComponentsPage(String assetId, Pageable pageable) {
        ApiResponse apiResponse = mitigateClient.findAssetComponents(assetId, null, null, pageable);
        if (apiResponse == null || apiResponse.getData() == null) {
            return new PagedApiResponse<>();
        }

        JavaType pagedType = objectMapper.getTypeFactory()
                .constructParametricType(PagedApiResponse.class, ComponentDTO.class);
        return objectMapper.convertValue(apiResponse.getData(), pagedType);
    }

    public List<VulnerabilityDTO> findVulnerabilityByAsset(String assetId) {
        ApiResponse apiResponse = mitigateClient.findVulnerabilityByAsset(assetId, null, null, null, null, ENRICHMENT_PAGEABLE);
        return convertPagedResponse(apiResponse, VulnerabilityDTO.class);
    }

    public List<VulnerabilityDTO> findVulnerabilityByComponent(String componentId) {
        ApiResponse apiResponse = mitigateClient.findVulnerabilityByComponent(componentId, null, null);
        if (apiResponse == null || apiResponse.getData() == null) {
            return Collections.emptyList();
        }

        JavaType listType = objectMapper.getTypeFactory()
                .constructCollectionType(List.class, VulnerabilityDTO.class);
        return objectMapper.convertValue(apiResponse.getData(), listType);
    }

    public List<VulnerabilityWithMitigationsDTO> findVulnerabilityWithMitigationsByAsset(String assetId) {
        ApiResponse apiResponse = mitigateClient.findVulnerabilityWithMitigationsByAsset(assetId, null, null, null, null, ENRICHMENT_PAGEABLE);
        return convertPagedResponse(apiResponse, VulnerabilityWithMitigationsDTO.class);
    }

    public List<WeaknessDTO> findWeaknessByVulnerability(String vulnerabilityId) {
        try {
            ApiResponse apiResponse = mitigateClient.findWeaknessByVulnerability(ENRICHMENT_PAGEABLE, vulnerabilityId, null);
            return convertPagedResponse(apiResponse, WeaknessDTO.class);
        } catch (FeignException e) {
            logger.warn("Skipping weakness enrichment for vulnerability {}. Mitigate returned HTTP {}",
                    vulnerabilityId, e.status());
            return List.of();
        }
    }

    public List<ThreatDTO> findThreatByVulnerability(String vulnerabilityId) {
        try {
            ApiResponse apiResponse = mitigateClient.findThreatByVulnerability(ENRICHMENT_PAGEABLE, vulnerabilityId, null);
            return convertPagedResponse(apiResponse, ThreatDTO.class);
        } catch (FeignException e) {
            logger.warn("Skipping threat enrichment for vulnerability {}. Mitigate returned HTTP {}",
                    vulnerabilityId, e.status());
            return List.of();
        }
    }

    private AssetDTO createAssetFromBom(String asset, Bom sbom) throws JsonProcessingException {
        byte[] sbomBytes = objectMapper.writeValueAsBytes(sbom);
        MultipartFile sbomFile = new InMemoryMultipartFile(
                "file",
                "sbom.json",
                "application/json",
                sbomBytes
        );

        ApiResponse apiResponse = mitigateClient.createSBOMAsset(asset, sbomFile);
        if (apiResponse == null || apiResponse.getData() == null) {
            return null;
        }
        return objectMapper.convertValue(apiResponse.getData(), AssetDTO.class);
    }

    private AssetDTO waitForAssetReady(String assetId) {
        for (int attempt = 0; attempt < POLL_MAX_ATTEMPTS; attempt++) {
            ApiResponse apiResponse = mitigateClient.findAssetById(assetId);
            if (apiResponse != null && apiResponse.getData() != null) {
                AssetDTO asset = objectMapper.convertValue(apiResponse.getData(), AssetDTO.class);
                if (asset != null && asset.getStatus() != null) {
                    switch (asset.getStatus()) {
                        case Done:
                        case ActionNeeded:
                            return asset;
                        case Deleted:
                            return null;
                        default:
                            break;
                    }
                }
            }

            try {
                Thread.sleep(POLL_SLEEP_MS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return null;
            }
        }
        return null;
    }

    private <T> List<T> convertPagedResponse(ApiResponse apiResponse, Class<T> contentClass) {
        if (apiResponse == null || apiResponse.getData() == null) {
            return List.of();
        }

        JavaType pagedType = objectMapper.getTypeFactory()
                .constructParametricType(PagedApiResponse.class, contentClass);
        PagedApiResponse<T> page = objectMapper.convertValue(apiResponse.getData(), pagedType);
        if (page == null || page.getContent() == null) {
            return List.of();
        }
        return page.getContent();
    }

    private Bom initializeReportBom(Bom sourceBom, AssetDTO asset, List<ComponentDTO> assetComponents) {
        Bom reportBom = new Bom();
        reportBom.setSerialNumber(REPORT_SERIAL_URN_PREFIX + UUID.randomUUID());
        reportBom.setVersion(1);
        reportBom.setMetadata(buildReportMetadata(sourceBom, asset));

        List<Component> reportComponents = new ArrayList<>();
        for (ComponentDTO assetComponent : assetComponents) {
            Component component = new Component();
            component.setBomRef(assetComponent.getId() != null ? assetComponent.getId() : UUID.randomUUID().toString());
            component.setType(Component.Type.LIBRARY);
            component.setName(assetComponent.getName());
            component.setGroup(assetComponent.getGroup());
            component.setVersion(assetComponent.getVersion());
            component.setCpe(assetComponent.getCpe());
            component.setPurl(assetComponent.getPurl());
            component.setDescription(assetComponent.getDescription());
            component.addProperty(property("ragre:component:custom", Boolean.toString(assetComponent.isCustom())));
            if (assetComponent.getRiskScore() != null) {
                component.addProperty(property("ragre:component:riskScore", assetComponent.getRiskScore().toString()));
            }
            if (assetComponent.getRiskScorePercentage() != null) {
                component.addProperty(property("ragre:component:riskScorePercentage", assetComponent.getRiskScorePercentage().toString()));
            }
            if (assetComponent.getRiskScoreSeverity() != null) {
                component.addProperty(property("ragre:component:riskScoreSeverity", assetComponent.getRiskScoreSeverity().name()));
            }
            if (assetComponent.getVulnerabilitiesCount() != null) {
                component.addProperty(property("ragre:component:vulnerabilitiesCount", assetComponent.getVulnerabilitiesCount().toString()));
            }
            reportComponents.add(component);
        }
        reportBom.setComponents(reportComponents);
        return reportBom;
    }

    private Metadata buildReportMetadata(Bom sourceBom, AssetDTO asset) {
        Metadata metadata = new Metadata();
        metadata.setTimestamp(new Date());
        metadata.setTools(List.of(buildReportTool()));
        metadata.setComponent(buildAiModelMetadataComponent(sourceBom, asset));
        return metadata;
    }

    private Tool buildReportTool() {
        Tool tool = new Tool();
        tool.setVendor(ReportBuilderUtils.REPORT_TOOL_VENDOR);
        tool.setName(ReportBuilderUtils.REPORT_TOOL_NAME);
        tool.setVersion(ReportBuilderUtils.REPORT_TOOL_VERSION);
        return tool;
    }

    private String alignMetadataToolsToCycloneDx17(String reportJson) throws JsonProcessingException {
        ObjectNode root = (ObjectNode) objectMapper.readTree(reportJson);
        ObjectNode metadata = root.with("metadata");

        ObjectNode tools = objectMapper.createObjectNode();
        ArrayNode components = objectMapper.createArrayNode();
        ObjectNode toolComponent = objectMapper.createObjectNode();
        toolComponent.put("type", ReportBuilderUtils.REPORT_TOOL_TYPE);
        toolComponent.put("bom-ref", UUID.randomUUID().toString());
        toolComponent.put("name", ReportBuilderUtils.REPORT_TOOL_NAME);
        toolComponent.put("version", ReportBuilderUtils.REPORT_TOOL_VERSION);

        ArrayNode properties = objectMapper.createArrayNode();
        properties.add(propertyNode("tool:vendor", ReportBuilderUtils.REPORT_TOOL_VENDOR));
        toolComponent.set("properties", properties);

        components.add(toolComponent);
        tools.set("components", components);
        metadata.set("tools", tools);

        return objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(root);
    }

    private Component buildAiModelMetadataComponent(Bom sourceBom, AssetDTO asset) {
        Component component = new Component();
        component.setType(Component.Type.MACHINE_LEARNING_MODEL);
        component.setName(resolveMetadataComponentName(sourceBom, asset));
        component.setVersion(resolveMetadataComponentVersion(sourceBom));
        component.addProperty(property("ragre:ai-risk:score", PENDING_VALUE));
        return component;
    }

    private String resolveMetadataComponentName(Bom sourceBom, AssetDTO asset) {
        if (asset != null && asset.getName() != null && !asset.getName().isBlank()) {
            return asset.getName();
        }
        if (sourceBom != null
                && sourceBom.getMetadata() != null
                && sourceBom.getMetadata().getComponent() != null
                && sourceBom.getMetadata().getComponent().getName() != null
                && !sourceBom.getMetadata().getComponent().getName().isBlank()) {
            return sourceBom.getMetadata().getComponent().getName();
        }
        return "pending";
    }

    private String resolveMetadataComponentVersion(Bom sourceBom) {
        if (sourceBom != null
                && sourceBom.getMetadata() != null
                && sourceBom.getMetadata().getComponent() != null
                && sourceBom.getMetadata().getComponent().getVersion() != null
                && !sourceBom.getMetadata().getComponent().getVersion().isBlank()) {
            return sourceBom.getMetadata().getComponent().getVersion();
        }
        return PENDING_VALUE;
    }

    private void enrichBom(Bom bom, List<ComponentDTO> assetComponents, Map<String, VulnerabilityWithMitigationsDTO> mitigationsByVulnerability) {
        Map<String, Component> componentByRef = new LinkedHashMap<>();
        Map<String, Vulnerability> vulnerabilityById = new LinkedHashMap<>();
        Map<String, Set<String>> affectRefsByVulnerabilityId = new LinkedHashMap<>();

        if (bom.getComponents() != null) {
            for (Component component : bom.getComponents()) {
                if (component.getBomRef() == null || component.getBomRef().isBlank()) {
                    component.setBomRef(UUID.randomUUID().toString());
                }
                componentByRef.put(component.getBomRef(), component);
            }
        }

        Set<String> seenPairs = new LinkedHashSet<>();

        for (ComponentDTO assetComponent : assetComponents) {
            String componentRef = assetComponent.getId();
            if (componentRef == null || !componentByRef.containsKey(componentRef)) {
                continue;
            }

            List<VulnerabilityDTO> componentVulnerabilities = findVulnerabilityByComponent(assetComponent.getId());
            for (VulnerabilityDTO source : componentVulnerabilities) {
                String vulnerabilityId = getVulnerabilityId(source);
                if (vulnerabilityId == null || vulnerabilityId.isBlank()) {
                    continue;
                }

                String pairKey = componentRef + "::" + vulnerabilityId;
                if (!seenPairs.add(pairKey)) {
                    continue;
                }

                Vulnerability cycloneDxVulnerability = vulnerabilityById.computeIfAbsent(
                        vulnerabilityId,
                        ignored -> buildCycloneDxVulnerability(source, vulnerabilityId, mitigationsByVulnerability.get(vulnerabilityId))
                );

                affectRefsByVulnerabilityId
                        .computeIfAbsent(vulnerabilityId, ignored -> new LinkedHashSet<>())
                        .add(componentRef);
                addPropertyToComponent(componentByRef.get(componentRef), "ragre:vulnerability:id", cycloneDxVulnerability.getId());
            }
        }

        List<Vulnerability> cyclonedxVulnerabilities = new ArrayList<>(vulnerabilityById.values());
        for (Map.Entry<String, Set<String>> entry : affectRefsByVulnerabilityId.entrySet()) {
            Vulnerability vulnerability = vulnerabilityById.get(entry.getKey());
            if (vulnerability == null) {
                continue;
            }
            List<Vulnerability.Affect> affects = new ArrayList<>();
            for (String componentRef : entry.getValue()) {
                Vulnerability.Affect affect = new Vulnerability.Affect();
                affect.setRef(componentRef);
                affects.add(affect);
            }
            vulnerability.setAffects(affects);
        }

        bom.setVulnerabilities(cyclonedxVulnerabilities);
    }

    private Vulnerability buildCycloneDxVulnerability(VulnerabilityDTO source,
                                                      String vulnerabilityId,
                                                      VulnerabilityWithMitigationsDTO vulnerabilityWithMitigations) {
        Vulnerability cycloneDxVulnerability = new Vulnerability();
        cycloneDxVulnerability.setBomRef(source.getId() != null ? source.getId() : UUID.randomUUID().toString());
        cycloneDxVulnerability.setId(vulnerabilityId);
        cycloneDxVulnerability.setDescription(source.getDescription());
        cycloneDxVulnerability.setPublished(toDate(source.getCreatedAt()));
        cycloneDxVulnerability.setUpdated(toDate(source.getUpdatedAt()));
        cycloneDxVulnerability.setSource(buildSource(source));

        Vulnerability.Rating rating = buildRating(source);
        if (rating != null) {
            cycloneDxVulnerability.addRating(rating);
        }

        List<WeaknessDTO> weaknesses = findWeaknessByVulnerability(vulnerabilityId);
        Set<Integer> cwes = extractCwes(weaknesses);
        if (!cwes.isEmpty()) {
            cycloneDxVulnerability.setCwes(new ArrayList<>(cwes));
        }

        List<ThreatDTO> threats = findThreatByVulnerability(vulnerabilityId);
        List<Property> properties = new ArrayList<>();
        if (source.getEpss() != null) {
            properties.add(property("ragre:epss:score", source.getEpss().toString()));
        }
        if (source.getPercentile() != null) {
            properties.add(property("ragre:epss:percentile", source.getPercentile().toString()));
        }
        if (source.getEpssDate() != null) {
            properties.add(property("ragre:epss:date", source.getEpssDate()));
        }
        addWeaknessProperties(properties, weaknesses);
        addThreatProperties(properties, threats);
        addMitigationProperties(properties, vulnerabilityWithMitigations);
        if (!properties.isEmpty()) {
            cycloneDxVulnerability.setProperties(properties);
        }

        return cycloneDxVulnerability;
    }

    private Vulnerability.Source buildSource(VulnerabilityDTO source) {
        Vulnerability.Source vulnerabilitySource = new Vulnerability.Source();
        VulnerabilitySourceInfo resolved = VulnerabilitySourceInfo.fromLibrary(source.getLibrary());
        if (resolved == null) {
            resolved = VulnerabilitySourceInfo.fromVulnerabilityId(getVulnerabilityId(source));
        }

        if (resolved == null) {
            vulnerabilitySource.setName("Mitigate");
            return vulnerabilitySource;
        }

        vulnerabilitySource.setName(resolved.getSourceName());
        vulnerabilitySource.setUrl(resolved.getSourceUrl());
        return vulnerabilitySource;
    }

    private Vulnerability.Rating buildRating(VulnerabilityDTO source) {
        if (source.getBaseScore() <= 0 && source.getBaseSeverity() == null) {
            return null;
        }

        Vulnerability.Rating rating = new Vulnerability.Rating();
        rating.setScore(source.getBaseScore());
        if (source.getBaseSeverity() != null) {
            rating.setSeverity(Vulnerability.Rating.Severity.fromString(source.getBaseSeverity().name().toLowerCase()));
        }
        if (source.getStringVector() != null) {
            rating.setVector(source.getStringVector());
        }

        Vulnerability.Source sourceRef = buildSource(source);
        rating.setSource(sourceRef);
        return rating;
    }

    private String getVulnerabilityId(VulnerabilityDTO source) {
        return source.getVulnerabilityId();
    }

    private Set<Integer> extractCwes(List<WeaknessDTO> weaknesses) {
        Set<Integer> cwes = new LinkedHashSet<>();
        if (weaknesses == null) {
            return cwes;
        }
        for (WeaknessDTO weakness : weaknesses) {
            if (weakness.getWeaknessId() == null) {
                continue;
            }
            String normalized = weakness.getWeaknessId().replace("CWE-", "").trim();
            try {
                cwes.add(Integer.parseInt(normalized));
            } catch (NumberFormatException ignored) {
                // Keep the report generation resilient even if a weakness id is not a numeric CWE.
            }
        }
        return cwes;
    }

    private void addWeaknessProperties(List<Property> properties, List<WeaknessDTO> weaknesses) {
        if (weaknesses == null) {
            return;
        }
        for (WeaknessDTO weakness : weaknesses) {
            String weaknessPropertyValue = toWeaknessPropertyValue(weakness);
            if (weaknessPropertyValue != null) {
                properties.add(property("ragre:weakness", weaknessPropertyValue));
            }
        }
    }

    private void addMitigationProperties(List<Property> properties, VulnerabilityWithMitigationsDTO vulnerabilityWithMitigations) {
        if (vulnerabilityWithMitigations == null) {
            return;
        }
        addVulnerabilityMitigationSummaryProperties(properties, vulnerabilityWithMitigations);
        if (vulnerabilityWithMitigations.getMitigations() == null) {
            return;
        }
        for (MitigationDTO mitigation : vulnerabilityWithMitigations.getMitigations()) {
            String mitigationPropertyValue = toMitigationPropertyValue(mitigation);
            if (mitigationPropertyValue != null) {
                properties.add(property("ragre:mitigation", mitigationPropertyValue));
            }
        }
    }

    private void addVulnerabilityMitigationSummaryProperties(List<Property> properties,
                                                             VulnerabilityWithMitigationsDTO vulnerabilityWithMitigations) {
        if (vulnerabilityWithMitigations.getCisaExploitAdd() != null) {
            properties.add(property("ragre:cisa:exploitAdd", vulnerabilityWithMitigations.getCisaExploitAdd()));
        }
        if (vulnerabilityWithMitigations.getCisaActionDue() != null) {
            properties.add(property("ragre:cisa:actionDue", vulnerabilityWithMitigations.getCisaActionDue()));
        }
        if (vulnerabilityWithMitigations.getCisaRequiredAction() != null) {
            properties.add(property("ragre:cisa:requiredAction", vulnerabilityWithMitigations.getCisaRequiredAction()));
        }
        if (vulnerabilityWithMitigations.getCisaVulnerabilityName() != null) {
            properties.add(property("ragre:cisa:vulnerabilityName", vulnerabilityWithMitigations.getCisaVulnerabilityName()));
        }
        if (vulnerabilityWithMitigations.getCvssV4BaseScore() != null) {
            properties.add(property("ragre:cvss:v4:baseScore", vulnerabilityWithMitigations.getCvssV4BaseScore().toString()));
        }
        if (vulnerabilityWithMitigations.getCvssV4BaseSeverity() != null) {
            properties.add(property("ragre:cvss:v4:baseSeverity", vulnerabilityWithMitigations.getCvssV4BaseSeverity().name()));
        }
        if (vulnerabilityWithMitigations.getCvssV2BaseScore() != null) {
            properties.add(property("ragre:cvss:v2:baseScore", vulnerabilityWithMitigations.getCvssV2BaseScore().toString()));
        }
        if (vulnerabilityWithMitigations.getCvssV2BaseSeverity() != null) {
            properties.add(property("ragre:cvss:v2:baseSeverity", vulnerabilityWithMitigations.getCvssV2BaseSeverity().name()));
        }
        if (vulnerabilityWithMitigations.getCvssV2ExploitabilityScore() != null) {
            properties.add(property("ragre:cvss:v2:exploitabilityScore", vulnerabilityWithMitigations.getCvssV2ExploitabilityScore().toString()));
        }
        if (vulnerabilityWithMitigations.getCvssV2ImpactScore() != null) {
            properties.add(property("ragre:cvss:v2:impactScore", vulnerabilityWithMitigations.getCvssV2ImpactScore().toString()));
        }
        properties.add(property("ragre:exploit:githubReported", Boolean.toString(vulnerabilityWithMitigations.isGithubReportedExploited())));
        properties.add(property("ragre:exploit:metasploitReported", Boolean.toString(vulnerabilityWithMitigations.isMetasploitReportedExploited())));
        properties.add(property("ragre:exploit:nucleiReported", Boolean.toString(vulnerabilityWithMitigations.isNucleiReportedExploited())));
        properties.add(property("ragre:exploit:exploitDbReported", Boolean.toString(vulnerabilityWithMitigations.isExploitDBReportedExploited())));
        properties.add(property("ragre:exploit:vulnCheckReported", Boolean.toString(vulnerabilityWithMitigations.isVulnCheckReportedExploited())));
    }

    private void addThreatProperties(List<Property> properties, List<ThreatDTO> threats) {
        if (threats == null) {
            return;
        }
        for (ThreatDTO threat : threats) {
            String threatPropertyValue = toThreatPropertyValue(threat);
            if (threatPropertyValue != null) {
                properties.add(property("ragre:threat", threatPropertyValue));
            }
        }
    }

    private void addPropertyToComponent(Component component, String name, String value) {
        if (component == null || value == null) {
            return;
        }
        component.addProperty(property(name, value));
    }

    private Property property(String name, String value) {
        Property property = new Property();
        property.setName(name);
        property.setValue(value);
        return property;
    }

    private ObjectNode propertyNode(String name, String value) {
        ObjectNode property = objectMapper.createObjectNode();
        property.put("name", name);
        property.put("value", value);
        return property;
    }

    private String toMitigationPropertyValue(MitigationDTO mitigation) {
        Map<String, String> mitigationProperty = new LinkedHashMap<>();
        if (mitigation.getMitigationId() != null) {
            mitigationProperty.put("mitigationId", mitigation.getMitigationId());
        }
        if (mitigation.getName() != null) {
            mitigationProperty.put("name", mitigation.getName());
        }
        if (mitigation.getDescription() != null) {
            mitigationProperty.put("description", mitigation.getDescription());
        }
        if (mitigation.getExternalId() != null) {
            mitigationProperty.put("externalId", mitigation.getExternalId());
        }
        if (mitigation.getType() != null) {
            mitigationProperty.put("type", mitigation.getType());
        }
        if (mitigation.getVersion() != null) {
            mitigationProperty.put("version", mitigation.getVersion());
        }
        if (mitigation.getLibrary() != null) {
            mitigationProperty.put("library", mitigation.getLibrary().name());
        }
        if (mitigationProperty.isEmpty()) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(mitigationProperty);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Failed to serialize mitigation property", e);
        }
    }

    private String toThreatPropertyValue(ThreatDTO threat) {
        Map<String, Object> threatProperty = new LinkedHashMap<>();
        if (threat.getThreatId() != null) {
            threatProperty.put("threatId", threat.getThreatId());
        }
        if (threat.getName() != null) {
            threatProperty.put("name", threat.getName());
        }
        if (threat.getSeverity() != null) {
            threatProperty.put("severity", threat.getSeverity());
        }
        if (threat.getDescription() != null) {
            threatProperty.put("description", threat.getDescription());
        }
        List<String> affects = new ArrayList<>();
        if (threat.getCwes() != null) {
            for (BasicWeaknessDTO weakness : threat.getCwes()) {
                if (weakness != null && weakness.getWeaknessId() != null && !weakness.getWeaknessId().isBlank()) {
                    affects.add(weakness.getWeaknessId());
                }
            }
        }
        if (!affects.isEmpty()) {
            threatProperty.put("affects", affects);
        }
        if (threatProperty.isEmpty()) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(threatProperty);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Failed to serialize threat property", e);
        }
    }

    private String toWeaknessPropertyValue(WeaknessDTO weakness) {
        Map<String, String> weaknessProperty = new LinkedHashMap<>();
        if (weakness.getWeaknessId() != null) {
            weaknessProperty.put("weaknessId", weakness.getWeaknessId());
        }
        if (weakness.getDescription() != null) {
            weaknessProperty.put("description", weakness.getDescription());
        }
        if (weaknessProperty.isEmpty()) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(weaknessProperty);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Failed to serialize weakness property", e);
        }
    }

    private Map<String, VulnerabilityWithMitigationsDTO> toVulnerabilityMitigationsMap(List<VulnerabilityWithMitigationsDTO> vulnerabilities) {
        Map<String, VulnerabilityWithMitigationsDTO> result = new HashMap<>();
        for (VulnerabilityWithMitigationsDTO vulnerability : vulnerabilities != null ? vulnerabilities : Collections.<VulnerabilityWithMitigationsDTO>emptyList()) {
            if (vulnerability.getVulnerabilityId() != null) {
                result.put(vulnerability.getVulnerabilityId(), vulnerability);
            }
        }
        return result;
    }

    private Date toDate(Instant value) {
        return value != null ? Date.from(value) : null;
    }
}
