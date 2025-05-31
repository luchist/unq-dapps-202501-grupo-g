package com.footballdata.football_stats_predictions.unittest.repositories

import com.footballdata.football_stats_predictions.model.Role
import com.footballdata.football_stats_predictions.model.User
import com.footballdata.football_stats_predictions.repositories.UserRepository
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.`when`
import org.springframework.security.crypto.password.PasswordEncoder
import java.util.*

class UserRepositoryTest {
    private lateinit var repository: UserRepository
    private lateinit var encoder: PasswordEncoder

    @BeforeEach
    fun setUp() {
        encoder = mock(PasswordEncoder::class.java)
        `when`(encoder.encode("pass1")).thenReturn("encoded_pass1")
        `when`(encoder.encode("pass2")).thenReturn("encoded_pass2")
        `when`(encoder.encode("pass3")).thenReturn("encoded_pass3")
        repository = UserRepository(encoder)
    }

    @Test
    fun `findByUsername should return user when exists`() {
        // Act
        val result = repository.findByUsername("email-1@gmail.com")

        // Assert
        assertNotNull(result)
        assertEquals("email-1@gmail.com", result?.name)
        assertEquals("encoded_pass1", result?.password)
        assertEquals(Role.USER, result?.role)
    }

    @Test
    fun `findByUsername should return null when user does not exist`() {
        // Act
        val result = repository.findByUsername("nonexistent@gmail.com")

        // Assert
        assertNull(result)
    }

    @Test
    fun `save should add new user to repository`() {
        // Arrange
        val newUser = User(
            id = UUID.randomUUID(),
            name = "new@gmail.com",
            password = "encoded_new_pass",
            role = Role.USER
        )

        // Act
        repository.save(newUser)
        val result = repository.findByUsername("new@gmail.com")

        // Assert
        assertNotNull(result)
        assertEquals(newUser.name, result?.name)
        assertEquals(newUser.password, result?.password)
        assertEquals(newUser.role, result?.role)
    }

    @Test
    fun `repository should initialize with three default users`() {
        // Act
        val user1 = repository.findByUsername("email-1@gmail.com")
        val user2 = repository.findByUsername("email-2@gmail.com")
        val user3 = repository.findByUsername("email-3@gmail.com")

        // Assert
        assertNotNull(user1)
        assertNotNull(user2)
        assertNotNull(user3)
        assertEquals(Role.USER, user1?.role)
        assertEquals(Role.ADMIN, user2?.role)
        assertEquals(Role.USER, user3?.role)
    }
}