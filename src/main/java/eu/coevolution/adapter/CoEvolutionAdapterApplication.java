package eu.coevolution.adapter;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.jdbc.autoconfigure.DataSourceAutoConfiguration;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication(exclude = {DataSourceAutoConfiguration.class})
@EnableAsync
@EnableFeignClients(basePackages = "eu.coevolution.adapter.feign")
public class CoEvolutionAdapterApplication {

	public static void main(String[] args) {
		SpringApplication.run(CoEvolutionAdapterApplication.class, args);
	}

}
