package eu.coevolution.adapter.config;

import eu.coevolution.adapter.service.keycloak.KeycloakProviderService;
import feign.RequestInterceptor;
import feign.codec.Encoder;
import feign.form.spring.SpringFormEncoder;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.cloud.openfeign.support.FeignEncoderProperties;
import org.springframework.cloud.openfeign.support.FeignHttpMessageConverters;
import org.springframework.cloud.openfeign.support.SpringEncoder;

public class MitigateFeignConfig {

    @Bean
    public RequestInterceptor requestInterceptor(KeycloakProviderService keycloakProviderService) {
        return requestTemplate -> {
            requestTemplate.header("Accept", "application/json");
            requestTemplate.header("Authorization", "Bearer " + keycloakProviderService.getToken());
        };
    }

    @Bean
    public Encoder multipartFormEncoder(
            ObjectProvider<FeignHttpMessageConverters> messageConverters) {
        return new SpringFormEncoder(
                new SpringEncoder(
                        new SpringFormEncoder(),
                        new FeignEncoderProperties(),
                        messageConverters));
    }
}
