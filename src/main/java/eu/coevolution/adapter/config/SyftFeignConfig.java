package eu.coevolution.adapter.config;

import eu.coevolution.adapter.service.keycloak.KeycloakProviderService;
import feign.RequestInterceptor;
import feign.codec.Encoder;
import feign.form.spring.SpringFormEncoder;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.openfeign.support.FeignEncoderProperties;
import org.springframework.cloud.openfeign.support.FeignHttpMessageConverters;
import org.springframework.cloud.openfeign.support.SpringEncoder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;


public class SyftFeignConfig {

    @Bean
    public RequestInterceptor requestInterceptor(KeycloakProviderService keycloakProviderService) {

        return requestTemplate -> {
            String token = keycloakProviderService.getToken();
            requestTemplate.header("Authorization", "Bearer " + token);
        };
    }

    @Bean
    public Encoder feignEncoder(
            ObjectProvider<FeignHttpMessageConverters> messageConverters) {
        return new SpringFormEncoder(
                new SpringEncoder(
                        new SpringFormEncoder(),
                        new FeignEncoderProperties(),
                        messageConverters));
    }
}
