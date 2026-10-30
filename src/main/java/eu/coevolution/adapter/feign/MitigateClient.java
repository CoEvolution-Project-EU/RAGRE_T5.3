package eu.coevolution.adapter.feign;

import eu.coevolution.adapter.config.MitigateFeignConfig;
import eu.coevolution.adapter.response.ApiResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@FeignClient(name = "Client", url = "${mitigate.base-url}${mitigate.partner-url}", configuration = { MitigateFeignConfig.class })
public interface MitigateClient {

    @GetMapping("/vulnerability/{vulnerabilityId}/weakness/all")
    ApiResponse findWeaknessByVulnerability(@PageableDefault Pageable pageable,
                                            @PathVariable String vulnerabilityId,
                                            @RequestParam(required = false) String weaknessId);

    @GetMapping("/vulnerability/{vulnerabilityId}/threat/all")
    ApiResponse findThreatByVulnerability(@PageableDefault Pageable pageable,
                                          @PathVariable String vulnerabilityId,
                                          @RequestParam(required = false) String threatId);

    @PostMapping(value = "/asset/sbom/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    ApiResponse createSBOMAsset(@RequestPart("asset") String asset,
                                @RequestPart(value = "file", required = false) MultipartFile file);

    @GetMapping("/asset/{assetId}")
    ApiResponse findAssetById(@PathVariable String assetId);

    @GetMapping("/asset/{assetId}/vulnerabilities/all")
    ApiResponse findVulnerabilityByAsset(@PathVariable String assetId,
                                         @RequestParam(required = false) String vulnerabilityId,
                                         @RequestParam(required = false) eu.coevolution.adapter.enums.Library library,
                                         @RequestParam(required = false) eu.coevolution.adapter.enums.BaseSeverity baseSeverity,
                                         @RequestParam(required = false) Integer assetVersion,
                                         @PageableDefault Pageable pageable);

    @GetMapping("/asset/{assetId}/components/all")
    ApiResponse findAssetComponents(@PathVariable String assetId,
                                    @RequestParam(required = false) String searchBy,
                                    @RequestParam(required = false) Integer assetVersion,
                                    @PageableDefault Pageable pageable);

    @GetMapping("/component/{componentId}/vulnerabilities/unpaged/all")
    ApiResponse findVulnerabilityByComponent(@PathVariable String componentId,
                                             @RequestParam(required = false) String searchBy,
                                             @RequestParam(required = false) eu.coevolution.adapter.enums.Library library);

    @GetMapping("/asset/{assetId}/vulnerabilities/with-mitigations/all")
    ApiResponse findVulnerabilityWithMitigationsByAsset(@PathVariable String assetId,
                                                        @RequestParam(required = false) String vulnerabilityId,
                                                        @RequestParam(required = false) eu.coevolution.adapter.enums.Library library,
                                                        @RequestParam(required = false) eu.coevolution.adapter.enums.BaseSeverity baseSeverity,
                                                        @RequestParam(required = false) Integer assetVersion,
                                                        @PageableDefault Pageable pageable);


}
