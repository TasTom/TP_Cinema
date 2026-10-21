package fr.butinfo.cinema.config;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import org.springframework.context.annotation.Configuration;

@Configuration
@OpenAPIDefinition(
        info = @Info(
                title = "Billetterie de cinéma",
                version = "1.0.0",
                description = """
                        API REST de billetterie de cinéma.
                        Les lectures de films, séances et salles sont publiques.
                        Les opérations de programmation, d'administration et de réservation
                        sont protégées par authentification HTTP Basic.
                        """))
@SecurityScheme(
        name = "basicAuth",
        type = SecuritySchemeType.HTTP,
        scheme = "basic",
        description = "Comptes de démonstration documentés dans le README.")
public class OpenApiConfig {
}
