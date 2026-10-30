package eu.coevolution.adapter.service.ragre;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import eu.coevolution.adapter.feign.MitigateScannerClient;
import eu.coevolution.adapter.response.ApiResponse;
import org.cyclonedx.model.Bom;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

@Service
public class ScanService {

    @Autowired
    private MitigateScannerClient mitigateScannerClient;

    private final ObjectMapper objectMapper = new ObjectMapper();

    public Bom generateSbom(@RequestPart(value = "file",required = false) MultipartFile file, @RequestPart("scanInfoJson")  String scanInfoJson) {
        ApiResponse apiResponse = mitigateScannerClient.downloadSBOMFile(file, scanInfoJson);
        if (apiResponse == null || apiResponse.getData() == null) {
            return null;
        }
        return objectMapper.convertValue(apiResponse.getData(), Bom.class);
    }

    public Bom generateSbomForUnifiedReport(MultipartFile file) {
        try {
            String scanInfoJson = objectMapper.writeValueAsString(Map.of(
                    "scanType", "FILESYSTEM",
                    "format", "cyclonedx-json"
            ));
            Bom sbom = generateSbom(file, scanInfoJson);
            if (sbom == null) {
                String filename = file != null ? file.getOriginalFilename() : "unknown";
                throw new IllegalStateException("SBOM generation returned no data for file " + filename);
            }
            return sbom;
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Failed to build unified report scan configuration", e);
        }
    }
}
