package com.medibridge.common.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

/**
 * Swagger UI at {@code /api/swagger-ui.html}.
 *
 * <p>The bearer scheme is declared so the "Authorize" button works: paste a
 * token from {@code POST /auth/login} once and every subsequent call carries
 * it. Without this every secured endpoint returns 401 from the docs page and
 * looks broken.
 */
@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI mediBridgeOpenApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("MediBridge API")
                        .version("1.0.0")
                        .description("""
                                Digital healthcare platform - patients, doctors and administrators.

                                **Getting started**
                                1. `POST /auth/login` with one of the demo accounts below.
                                2. Copy the `token` from the response.
                                3. Click **Authorize** (top right) and paste it.

                                **Demo accounts** (password `Test@1234`, admin `Admin@123`)
                                - Patient: `john.doe@email.com`
                                - Doctor:  `sarah.johnson@medibridge.com`
                                - Admin:   `admin@medibridge.com`

                                **Booking flow**
                                `GET /doctors` -> `GET /doctors/{id}/slots?date=` ->
                                `POST /appointments` -> `POST /payments/order` ->
                                `POST /payments/verify` -> appointment is confirmed and a
                                consultation link is emailed.

                                Paying for a published slot confirms it outright - there is no
                                separate accept step. Cancelling refunds automatically per the
                                policy in System Settings.
                                """)
                        .contact(new Contact().name("MediBridge").email("support@medibridge.com")))
                .servers(List.of(
                        new Server().url("http://localhost:8080/api").description("Local")))
                .addSecurityItem(new SecurityRequirement().addList("bearerAuth"))
                .components(new Components().addSecuritySchemes("bearerAuth",
                        new SecurityScheme()
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")
                                .description("Paste the token from POST /auth/login "
                                        + "(without the 'Bearer ' prefix)")));
    }
}
