package com.jse.chat.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI chatPlatformOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("JSE Chat Platform API")
                        .description("REST API for the JSE Chat Platform. Clients send messages via HTTP and receive real-time broadcasts over WebSocket (STOMP over SockJS at /ws).")
                        .version("1.0.0")
                        .contact(new Contact()
                                .name("JSE Development Team")));
    }
}
