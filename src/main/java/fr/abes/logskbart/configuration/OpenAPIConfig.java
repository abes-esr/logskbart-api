package fr.abes.logskbart.configuration;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class OpenAPIConfig {

    @Value("${logskbart.openapi.url}")
    private String devUrl;

    @Bean
    public OpenAPI myOpenAPI() {
        Server devServer = new Server();
        devServer.setUrl(devUrl);
        devServer.setDescription("Server URL in Development environment");

        Contact contact = new Contact();
//        contact.setEmail("");
        contact.setName("Logskbart");
        contact.setUrl("https://");

//        License mitLicense = new License().name("MIT License").url("https://choosealicense.com/licenses/mit/");

        Info info = new Info()
                .title("Logskbart API")
                .version("1.0")
                .contact(contact)
                .description("This API retrieves kbart load logs from a Kafka bus and stores them in a DB for later availability.");
//                .termsOfService("https://");
//                .license(mitLicense);

        return new OpenAPI().info(info).servers(List.of(devServer));
    }
}
