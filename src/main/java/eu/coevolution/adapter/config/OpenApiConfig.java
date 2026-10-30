package eu.coevolution.adapter.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Value("${springdoc.api-docs.title}")
    private String TITLE;

    @Value("${springdoc.api-docs.subtitle}")
    private String DESCRIPTION;

    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title(TITLE)
                        .version("1.0.0")
                        .description(DESCRIPTION));
    }

}
