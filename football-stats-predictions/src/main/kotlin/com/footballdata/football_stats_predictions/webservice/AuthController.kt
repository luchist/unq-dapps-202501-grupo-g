package com.footballdata.football_stats_predictions.webservice

import com.footballdata.football_stats_predictions.model.Role
import com.footballdata.football_stats_predictions.model.User
import com.footballdata.football_stats_predictions.repositories.UserRepository
import com.footballdata.football_stats_predictions.service.AuthenticationService
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.util.*

@RestController
@RequestMapping("/api/auth")
class AuthController(
    private val authenticationService: AuthenticationService,
    private val userRepository: UserRepository,
    private val encoder: PasswordEncoder
) {
    @PostMapping
    fun authenticate(
        @RequestBody authRequest: AuthenticationRequest
    ): AuthenticationResponse =
        authenticationService.authentication(authRequest)

    @PostMapping("/refresh")
    fun refreshAccessToken(
        @RequestBody request: RefreshTokenRequest
    ): TokenResponse = TokenResponse(token = authenticationService.refreshAccessToken(request.token))

    @PostMapping("/register")
    fun register(@RequestBody registerRequest: RegisterRequest): AuthenticationResponse {
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

    data class RegisterRequest(
        val username: String,
        val password: String
    )
}