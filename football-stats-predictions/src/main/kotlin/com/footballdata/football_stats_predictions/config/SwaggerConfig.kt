package com.footballdata.football_stats_predictions.config

import io.swagger.v3.oas.models.OpenAPI
import io.swagger.v3.oas.models.info.Contact
import io.swagger.v3.oas.models.info.Info
import io.swagger.v3.oas.models.info.License
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class SwaggerConfig {

    @Bean
    fun customOpenAPI(): OpenAPI {
        return OpenAPI()
            .info(
                Info()
                    .title("FootballStatistics API")
                    .version("1.0.0")
                    .description("API documented with Swagger and SpringDoc.")
                    .contact(
                        Contact()
                            .name("Contact")
                            .url("https://www.football-data.org/")
                            .email("fakeemail@gmail.com")
                    )
                    .termsOfService("https://www.football-data.org/terms")
                    .license(
                        License()
                            .name("GNU GPL v3")
                            .url("https://www.gnu.org/licenses/gpl-3.0.en.html")
                    )
            )
    }
}