package com.footballdata.football_stats_predictions.config

import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.MethodArgumentNotValidException
import org.springframework.web.bind.annotation.ControllerAdvice
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.context.request.WebRequest
import java.time.LocalDateTime

@ControllerAdvice
class GlobalExceptionHandler {

    @ExceptionHandler(MethodArgumentNotValidException::class)
    fun handleValidationExceptions(
        ex: MethodArgumentNotValidException,
        request: WebRequest
    ): ResponseEntity<Map<String, Any>> {
        val errors = mutableMapOf<String, String>()
        
        ex.bindingResult.fieldErrors.forEach { error ->
            errors[error.field] = error.defaultMessage ?: "Invalid value"
        }
        
        val errorResponse = mapOf(
            "timestamp" to LocalDateTime.now().toString(),
            "status" to 400,
            "error" to "Bad Request",
            "message" to "Validation failed",
            "path" to request.getDescription(false).removePrefix("uri="),
            "validationErrors" to errors
        )
        
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse)
    }

    @ExceptionHandler(IllegalArgumentException::class)
    fun handleIllegalArgumentException(
        ex: IllegalArgumentException,
        request: WebRequest
    ): ResponseEntity<Map<String, Any>> {
        val errorResponse = mapOf(
            "timestamp" to LocalDateTime.now().toString(),
            "status" to 400,
            "error" to "Bad Request",
            "message" to (ex.message ?: "Invalid request"),
            "path" to request.getDescription(false).removePrefix("uri=")
        )
        
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse)
    }
}
