package eu.coevolution.adapter.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import eu.coevolution.adapter.dto.AssessmentReportDTO;
import eu.coevolution.adapter.dto.AssetDTO;
import eu.coevolution.adapter.dto.ComponentDTO;
import eu.coevolution.adapter.response.ApiResponse;
import eu.coevolution.adapter.response.PagedApiResponse;
import eu.coevolution.adapter.service.ragre.RagreService;
import eu.coevolution.adapter.service.ragre.ScanService;
import io.swagger.v3.oas.annotations.Operation;
import org.cyclonedx.model.Bom;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api")
public class RagreController {

    private final ScanService scanService;

    private final RagreService ragreService;

    public RagreController(ScanService scanService, RagreService ragreService) {
        this.scanService = scanService;
        this.ragreService = ragreService;
    }

    @PostMapping(value = "/sbom", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary =  "Generate SBOM file")
    public ResponseEntity<?> downloadSBOMFile(@RequestPart(value = "file", required = false) MultipartFile file, @RequestPart("scanInfoJson") String scanInfoJson) {

        Bom sbom = scanService.generateSbom(file, scanInfoJson);
        return  ResponseEntity.ok(new ApiResponse(sbom, true));
    }

    @PostMapping(value = "/asset/create", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary =  "Create asset from sbom")
    public ResponseEntity<?> createAsset(@RequestPart("asset") String asset,
                                         @RequestPart(value = "file", required = false) MultipartFile file,
                                         @RequestPart("scanInfoJson") String scanInfoJson) throws JsonProcessingException {

        AssetDTO response = ragreService.createAsset(asset, file, scanInfoJson);
        return  ResponseEntity.ok(new ApiResponse(response, true));
    }

    @GetMapping("/asset/{assetId}/components")
    @Operation(summary = "Return backend-derived components for an asset")
    public ResponseEntity<?> findAssetComponents(@PathVariable String assetId,
                                                 @PageableDefault Pageable pageable) {

        PagedApiResponse<ComponentDTO> response = ragreService.findAssetComponentsPage(assetId, pageable);
        return ResponseEntity.ok(new ApiResponse(response, true));
    }

    @PostMapping(value = "/report/unified/ai-risk", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary =  "Calculate the aggregated AI risk score for a model from tool assessment results")
    public ResponseEntity<?> calculateAiRisk(@RequestBody List<AssessmentReportDTO> request) {
        double response = ragreService.calculateAggregatedAiRiskScore(request);
        return ResponseEntity.ok(new ApiResponse(response, true));
    }

    @PostMapping(value = "/report/unified", consumes = MediaType.MULTIPART_FORM_DATA_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary =  "Generate a unified CycloneDX report enriched with backend vulnerability data")
    public ResponseEntity<String> generateUnifiedReport(@RequestPart("asset") String asset,
                                                        @RequestPart(value = "file", required = false) MultipartFile file) throws JsonProcessingException {
        String response = ragreService.generateUnifiedReportJson(asset, file);
        return ResponseEntity.ok(response);
    }
}
