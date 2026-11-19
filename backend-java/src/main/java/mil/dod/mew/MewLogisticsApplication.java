package mil.dod.mew;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.web.servlet.support.SpringBootServletInitializer;
import org.springframework.scheduling.annotation.EnableScheduling;
import mil.dod.mew.config.ReadinessApiProperties;

/**
 * Entry point for the MEW Logistics &amp; Readiness Java EE 8 / Spring MVC backend.
 * Extends {@link SpringBootServletInitializer} for WAR deployment to WebLogic or JBoss.
 */
@SpringBootApplication
@EnableScheduling
@EnableConfigurationProperties(ReadinessApiProperties.class)
public class MewLogisticsApplication extends SpringBootServletInitializer {

    @Override
    protected SpringApplicationBuilder configure(SpringApplicationBuilder application) {
        return application.sources(MewLogisticsApplication.class);
    }

    public static void main(String[] args) {
        SpringApplication.run(MewLogisticsApplication.class, args);
    }
}
