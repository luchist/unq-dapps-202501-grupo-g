package com.footballdata.football_stats_predictions.webservice

import com.footballdata.football_stats_predictions.aspects.LogFunctionCall
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api")
@Tag(name = "Test", description = "Test endpoints for verifying JWT authentication")
class HelloController {
    @Operation(
        summary = "Hello endpoint",
        description = "Returns a greeting message for authorized users"
    )
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "200", description = "Greeting message returned successfully"),
            ApiResponse(responseCode = "403", description = "Access denied for unauthorized users")
        ]
    )
    @GetMapping("/hello")
    @LogFunctionCall
    fun hello(): ResponseEntity<String> {
        return ResponseEntity.ok("Hello, Authorized User!")
    }
}