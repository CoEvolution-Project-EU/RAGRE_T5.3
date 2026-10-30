package eu.coevolution.adapter.service.catalogue;

import com.fasterxml.jackson.databind.ObjectMapper;
import eu.coevolution.adapter.dto.EthicalGuidelineCatalogueEntryDTO;
import eu.coevolution.adapter.dto.EthicalGuidelinesCatalogueDTO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

@Service
public class EthicalGuidelinesCatalogueService {

    private static final String CATALOGUE_RESOURCE = "ethical-guidelines-catalogue.json";

    private final String catalogueId;
    private final String catalogueVersion;
    private final Map<String, EthicalGuidelineCatalogueEntryDTO> entriesByQuestionId;

    @Autowired
    public EthicalGuidelinesCatalogueService(ObjectMapper objectMapper) {
        this(objectMapper, CATALOGUE_RESOURCE);
    }

    public EthicalGuidelinesCatalogueService(ObjectMapper objectMapper, String catalogueResource) {
        EthicalGuidelinesCatalogueDTO catalogue = loadCatalogue(objectMapper, catalogueResource);
        this.catalogueId = catalogue.getCatalogueId();
        this.catalogueVersion = catalogue.getCatalogueVersion();
        this.entriesByQuestionId = indexByQuestionId(catalogue);
    }

    public String getCatalogueId() {
        return catalogueId;
    }

    public String getCatalogueVersion() {
        return catalogueVersion;
    }

    public Optional<EthicalGuidelineCatalogueEntryDTO> findEntry(String questionId) {
        return Optional.ofNullable(entriesByQuestionId.get(questionId));
    }

    private EthicalGuidelinesCatalogueDTO loadCatalogue(ObjectMapper objectMapper, String catalogueResource) {
        try (InputStream inputStream = new ClassPathResource(catalogueResource).getInputStream()) {
            return objectMapper.readValue(inputStream, EthicalGuidelinesCatalogueDTO.class);
        } catch (IOException e) {
            throw new IllegalStateException("Failed to load ethical guidelines catalogue from " + catalogueResource, e);
        }
    }

    private Map<String, EthicalGuidelineCatalogueEntryDTO> indexByQuestionId(EthicalGuidelinesCatalogueDTO catalogue) {
        Map<String, EthicalGuidelineCatalogueEntryDTO> index = new LinkedHashMap<>();
        if (catalogue.getEntries() == null) {
            return index;
        }
        for (EthicalGuidelineCatalogueEntryDTO entry : catalogue.getEntries()) {
            if (entry != null && entry.getQuestionId() != null) {
                validateEntry(entry);
                index.put(entry.getQuestionId(), entry);
            }
        }
        return index;
    }

    private void validateEntry(EthicalGuidelineCatalogueEntryDTO entry) {
        if (entry.getAnswers() == null || entry.getAnswers().isEmpty()) {
            throw new IllegalStateException("Ethical guidelines catalogue question " + entry.getQuestionId() + " has no allowed answers");
        }
        if (entry.getGuidelines() != null && !entry.getAnswers().containsAll(entry.getGuidelines().keySet())) {
            throw new IllegalStateException("Ethical guidelines catalogue question " + entry.getQuestionId()
                    + " has a guideline for an answer it does not allow");
        }
    }
}
