package eu.coevolution.adapter.feign;

import eu.coevolution.adapter.config.MitigateFeignConfig;
import eu.coevolution.adapter.config.SyftFeignConfig;
import eu.coevolution.adapter.response.ApiResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@FeignClient(name = "Client", url = "${mitigate.base-url}${mitigate.scan-url}", configuration = { MitigateFeignConfig.class })
public interface MitigateScannerClient {

    @PostMapping(value = "/generate/sbom", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    ApiResponse downloadSBOMFile(@RequestPart(value = "file", required = false) MultipartFile file, @RequestPart("scanInfoJson") String scanInfoJson);

}
