package com.footballdata.football_stats_predictions.webservice

import com.footballdata.football_stats_predictions.dto.*
import com.footballdata.football_stats_predictions.model.Role
import com.footballdata.football_stats_predictions.model.User
import com.footballdata.football_stats_predictions.repositories.UserRepository
import com.footballdata.football_stats_predictions.service.AuthenticationService
import io.jsonwebtoken.ExpiredJwtException
import io.jsonwebtoken.JwtException
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.util.*

@RestController
@RequestMapping("/api/auth")
@Tag(name = "Authentication", description = "Endpoints for user authentication and registration")
class AuthController(
    private val authenticationService: AuthenticationService,
    private val userRepository: UserRepository,
    private val encoder: PasswordEncoder
) {
    @Operation(
        summary = "Authenticate user",
        description = "Authenticates a user and returns an authentication token"
    )
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "200", description = "Authentication was successful"),
            ApiResponse(responseCode = "403", description = "Invalid username or password")
        ]
    )
    @PostMapping
    fun authenticate(
        @Valid @RequestBody authRequest: AuthenticationRequest
    ): AuthenticationResponse =
        authenticationService.authentication(authRequest)

    @Operation(
        summary = "Refresh access token",
        description = "Refreshes the access token using a valid refresh token"
    )
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "200",
                description = "Refresh token was successful and a new access token is returned"
            ),
            ApiResponse(responseCode = "403", description = "Invalid or expired refresh token")
        ]
    )
    @PostMapping("/refresh")
    fun refreshAccessToken(
        @Valid @RequestBody request: RefreshTokenRequest
    ): ResponseEntity<TokenResponse> {
        return try {
            val newToken = authenticationService.refreshAccessToken(request.token)
            ResponseEntity.ok(TokenResponse(token = newToken))
        } catch (_: ExpiredJwtException) {
            ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(TokenResponse(error = "Token expired"))
        } catch (_: JwtException) {
            ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(TokenResponse(error = "Invalid token"))
        }
    }

    @Operation(
        summary = "Register a new user",
        description = "Registers a new user and returns valid authentication tokens"
    )
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "200",
                description = "User registration was successful and authentication tokens are returned"
            ),
            ApiResponse(responseCode = "400", description = "Invalid registration request"),
            ApiResponse(responseCode = "403", description = "User with the same username already exists")
        ]
    )
    @PostMapping("/register")
    fun register(@Valid @RequestBody registerRequest: RegisterRequest): AuthenticationResponse {
        val hashedPassword = encoder.encode(registerRequest.password)
        val user = User(
            id = UUID.randomUUID(),
            name = registerRequest.username,
            password = hashedPassword,
            role = Role.USER // Default role for new users
        )
        userRepository.save(user)
        return authenticationService.authentication(
            AuthenticationRequest(
                username = registerRequest.username,
                password = registerRequest.password
            )
        )
    }
}