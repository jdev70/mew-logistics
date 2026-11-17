package mil.dod.mew.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI mewOpenApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("MEW Logistics & Readiness SvcV-1 API")
                        .version("v1")
                        .description("Java EE 8 / Spring MVC / Hibernate JPA REST API for Maritime Electromagnetic Warfare Logistics & Readiness"));
    }
}
