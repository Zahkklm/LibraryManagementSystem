package com.librarysystem.borrowservice.config;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.info.Info;
import org.springframework.context.annotation.Configuration;

@Configuration
@OpenAPIDefinition(
    info = @Info(
        title = "Borrow Service API",
        version = "1.0",
        description = "Documentation for Borrow Service"
    )
)
public class OpenApiConfig {
    
}