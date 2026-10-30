package eu.coevolution.adapter.service.http;

import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.BufferingClientHttpRequestFactory;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.List;
import java.util.Map;

@Component
public class HttpClient extends RestTemplate {

    public String postRequestWithFile(String url, MultiValueMap<String, Object> body, Map<String, String> headerMap, Map<String, Object> parameters) {

        HttpHeaders headers = setHttpHeaders(headerMap);
        url  = setParameters(parameters, url);
        headers.setContentType(MediaType.MULTIPART_FORM_DATA);
        HttpEntity<MultiValueMap<String, Object>> requestEntity = new HttpEntity<>(body, headers);

        RestTemplate restTemplate = new RestTemplate(
                new BufferingClientHttpRequestFactory(new SimpleClientHttpRequestFactory()));
        ResponseEntity<String> result = restTemplate.postForEntity(url, requestEntity, String.class);

        return result.getBody();
    }

    private HttpHeaders setHttpHeaders(Map<String, String> headers) {

        HttpHeaders httpHeaders = new HttpHeaders();
        httpHeaders.setAccept(List.of(MediaType.APPLICATION_JSON));
        if (headers != null) {
            for (Map.Entry<String, String> entry : headers.entrySet()) {
                httpHeaders.set(entry.getKey(), entry.getValue());
            }
        }

        return httpHeaders;
    }

    private String setParameters(Map<String, Object> parameters, String url) {

        UriComponentsBuilder builder = UriComponentsBuilder.fromUriString(url);
        for (Map.Entry<String, Object> parameter : parameters.entrySet()) {
            builder.queryParam(parameter.getKey(), parameter.getValue());
        }

        return builder.build().toString();
    }

    private String setPagination(Pageable pageable, String url) {

        UriComponentsBuilder builder = UriComponentsBuilder.fromUriString(url);
        builder.queryParam("page", pageable.getPageNumber());
        builder.queryParam("size", pageable.getPageSize());

        return builder.build().toString();
    }
}
