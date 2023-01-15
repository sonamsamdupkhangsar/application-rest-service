package me.sonam.application;

import io.r2dbc.spi.ConnectionFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.netflix.eureka.EnableEurekaClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.core.io.ClassPathResource;
import org.springframework.r2dbc.connection.init.ConnectionFactoryInitializer;
import org.springframework.r2dbc.connection.init.ResourceDatabasePopulator;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.reactive.CorsWebFilter;
import org.springframework.web.cors.reactive.UrlBasedCorsConfigurationSource;

//put the following scanBasePackages because jwt-validator needs to be scanned from me.sonam.security package
// also scan this application too.
@EnableEurekaClient
@SpringBootApplication(scanBasePackages = {"me.sonam"})
public class SpringApplication {
    private static final Logger LOG = LoggerFactory.getLogger(SpringApplication.class);
    public static void main(String[] args) {
        LOG.info("starting springApplication");
        System.out.println("starting spring application");
        org.springframework.boot.SpringApplication.run(SpringApplication.class, args);
    }

    @Bean()
    ConnectionFactoryInitializer initializer(ConnectionFactory connectionFactory) {
        ConnectionFactoryInitializer initializer = new ConnectionFactoryInitializer();
        initializer.setConnectionFactory(connectionFactory);
        // This will create our database table and schema
        initializer.setDatabasePopulator(new ResourceDatabasePopulator(new ClassPathResource("schema.sql")));

        // This will drop our table after we are done so we can have a fresh start next run
        //initializer.setDatabaseCleaner(new ResourceDatabasePopulator(new ClassPathResource("cleanup.sql")));
        return initializer;
    }

    @Bean
    CorsWebFilter corsWebFilter() {
        LOG.info("allow cors filter");
        CorsConfiguration corsConfig = new CorsConfiguration();
        corsConfig.setMaxAge(8000L);
        corsConfig.addAllowedOrigin("*");
        corsConfig.addAllowedMethod("GET");
        corsConfig.addAllowedMethod("POST");
        corsConfig.addAllowedHeader("Content-Type");
        corsConfig.addAllowedHeader("api_key");
        corsConfig.addAllowedHeader("Authorization");

        UrlBasedCorsConfigurationSource source =
                new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", corsConfig);

        return new CorsWebFilter(source);
    }

}
