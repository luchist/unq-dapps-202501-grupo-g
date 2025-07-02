package com.footballdata.football_stats_predictions.aspects

import com.footballdata.football_stats_predictions.logger
import com.footballdata.football_stats_predictions.service.QueryHistoryService
import jakarta.servlet.http.HttpServletRequest
import org.aspectj.lang.ProceedingJoinPoint
import org.aspectj.lang.annotation.Around
import org.aspectj.lang.annotation.Aspect
import org.aspectj.lang.reflect.MethodSignature
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.ResponseEntity
import org.springframework.security.core.Authentication
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.stereotype.Component
import org.springframework.web.bind.annotation.*
import org.springframework.web.context.request.RequestContextHolder
import org.springframework.web.context.request.ServletRequestAttributes
import java.lang.reflect.Method
import java.lang.reflect.Parameter

/**
 * Annotation to mark controller methods that should automatically save query history
 *
 * @param includeParams List of parameter names to include in query history.
 *                      If empty, all @PathVariable and @RequestParam will be included
 * @param excludeParams: List of parameter names to exclude from query history
 */
@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
annotation class Queryable(
    val includeParams: Array<String> = [],
    val excludeParams: Array<String> = ["authentication", "auth"]
)

/**
 * Aspect that automatically saves query history for methods annotated with @Queryable
 *
 * Key Features:
 * - Automatically extracts endpoint URL from request mapping annotations
 * - Captures actual HTTP status code from ResponseEntity
 * - Extracts authenticated user from SecurityContext
 * - Flexible parameter extraction based on annotation configuration
 * - Comprehensive error handling and logging
 */
@Aspect
@Component
class QueryableAspect(
    @Autowired private val queryHistoryService: QueryHistoryService
) {

    @Around("@annotation(Queryable)")
    @Throws(Throwable::class)
    fun saveInQueryHistory(joinPoint: ProceedingJoinPoint): Any? {
        var responseStatus: Int
        var errorMessage: String?

        try {
            // Extract annotation configuration
            val annotation = getQueryableAnnotation(joinPoint)

            // Extract user authentication
            val authentication = SecurityContextHolder.getContext().authentication
            val userName = authentication?.name ?: "anonymous"

            // Build endpoint URL and query parameters
            val endpointInfo = extractEndpointInfo(joinPoint, annotation)

            // Execute the actual method
            val result = joinPoint.proceed()

            // Extract status from ResponseEntity if available
            responseStatus = extractStatusFromResult(result)

            // Save successful query to history
            saveQueryHistory(
                userName = userName,
                endpoint = endpointInfo.endpoint,
                queryParams = endpointInfo.queryParams,
                status = responseStatus,
                message = null
            )

            return result

        } catch (exception: Throwable) {
            // Handle errors and save failed query to history
            responseStatus = determineErrorStatus(exception)
            errorMessage = exception.message

            try {
                val annotation = getQueryableAnnotation(joinPoint)
                val authentication = SecurityContextHolder.getContext().authentication
                val userName = authentication?.name ?: "anonymous"
                val endpointInfo = extractEndpointInfo(joinPoint, annotation)

                saveQueryHistory(
                    userName = userName,
                    endpoint = endpointInfo.endpoint,
                    queryParams = endpointInfo.queryParams,
                    status = responseStatus,
                    message = errorMessage
                )
            } catch (historyException: Exception) {
                logger.error("Failed to save error query history: ${historyException.message}")
            }

            throw exception // Re-throw the original exception
        }
    }

    private fun getQueryableAnnotation(joinPoint: ProceedingJoinPoint): Queryable {
        val signature = joinPoint.signature as MethodSignature
        return signature.method.getAnnotation(Queryable::class.java)
            ?: throw IllegalStateException("@Queryable annotation not found")
    }

    /**
     * Extracts endpoint URL and query parameters from the join point
     */
    private fun extractEndpointInfo(joinPoint: ProceedingJoinPoint, annotation: Queryable): EndpointInfo {
        val signature = joinPoint.signature as MethodSignature
        val method = signature.method
        val methodArgs = joinPoint.args
        val parameterNames = signature.parameterNames
        val parameters = method.parameters

        // Get the actual HTTP request for accurate endpoint extraction
        val request = getCurrentHttpRequest()
        val actualEndpoint = request?.requestURI ?: buildEndpointFromAnnotations(method, methodArgs, parameterNames)

        // Extract query parameters based on annotation configuration
        val queryParams = extractQueryParameters(
            parameters = parameters,
            parameterNames = parameterNames,
            methodArgs = methodArgs,
            annotation = annotation
        )

        return EndpointInfo(actualEndpoint, queryParams)
    }

    /**
     * Gets the current HTTP request from Spring's RequestContextHolder
     */
    private fun getCurrentHttpRequest(): HttpServletRequest? {
        return try {
            val requestAttributes = RequestContextHolder.currentRequestAttributes() as ServletRequestAttributes
            requestAttributes.request
        } catch (e: Exception) {
            logger.warn("Could not retrieve current HTTP request: ${e.message}")
            null
        }
    }

    /**
     * Builds endpoint URL from Spring mapping annotations as fallback
     */
    private fun buildEndpointFromAnnotations(
        method: Method,
        methodArgs: Array<Any?>,
        parameterNames: Array<String>
    ): String {
        val classMapping = method.declaringClass.getAnnotation(RequestMapping::class.java)
        val methodMapping = method.getAnnotation(GetMapping::class.java)
            ?: method.getAnnotation(PostMapping::class.java)
            ?: method.getAnnotation(PutMapping::class.java)
            ?: method.getAnnotation(DeleteMapping::class.java)
            ?: method.getAnnotation(RequestMapping::class.java)

        val basePath = classMapping?.value?.firstOrNull() ?: ""
        val methodPath = when (methodMapping) {
            is GetMapping -> methodMapping.value.firstOrNull() ?: ""
            is PostMapping -> methodMapping.value.firstOrNull() ?: ""
            is PutMapping -> methodMapping.value.firstOrNull() ?: ""
            is DeleteMapping -> methodMapping.value.firstOrNull() ?: ""
            is RequestMapping -> methodMapping.value.firstOrNull() ?: ""
            else -> ""
        }

        // Replace path variables with actual values
        var fullPath = "$basePath$methodPath"
        parameterNames.forEachIndexed { index, paramName ->
            if (methodArgs.size > index && methodArgs[index] != null) {
                fullPath = fullPath.replace("{$paramName}", methodArgs[index].toString())
            }
        }

        return fullPath
    }

    /**
     * Extracts query parameters based on annotation configuration
     */
    private fun extractQueryParameters(
        parameters: Array<Parameter>,
        parameterNames: Array<String>,
        methodArgs: Array<Any?>,
        annotation: Queryable
    ): String {
        val queryParams = mutableListOf<String>()

        parameters.forEachIndexed { index, parameter ->
            val paramName = parameterNames[index]
            val paramValue = methodArgs.getOrNull(index)

            // Skip excluded parameters
            if (annotation.excludeParams.contains(paramName)) {
                return@forEachIndexed
            }

            // Skip Authentication objects
            if (parameter.type == Authentication::class.java) {
                return@forEachIndexed
            }

            // Include parameter if it's in includeParams list or if includeParams is empty and it's a relevant parameter
            val shouldInclude = when {
                annotation.includeParams.isNotEmpty() -> annotation.includeParams.contains(paramName)
                else -> parameter.isAnnotationPresent(PathVariable::class.java) ||
                        parameter.isAnnotationPresent(RequestParam::class.java)
            }

            if (shouldInclude && paramValue != null) {
                queryParams.add("$paramName=$paramValue")
            }
        }

        return queryParams.joinToString("&")
    }

    /**
     * Extracts HTTP status code from method result
     */
    private fun extractStatusFromResult(result: Any?): Int {
        return when (result) {
            is ResponseEntity<*> -> result.statusCode.value()
            else -> 200 // Default to 200 for non-ResponseEntity returns
        }
    }

    /**
     * Determines appropriate HTTP status code from exception
     */
    private fun determineErrorStatus(exception: Throwable): Int {
        return when (exception::class.simpleName) {
            "TeamNotFoundException" -> 404
            "IllegalArgumentException" -> 400
            "AccessDeniedException" -> 403
            "AuthenticationException" -> 401
            else -> 500
        }
    }

    /**
     * Saves query to history with proper error handling
     */
    private fun saveQueryHistory(
        userName: String,
        endpoint: String,
        queryParams: String,
        status: Int,
        message: String?
    ) {
        try {
            queryHistoryService.saveQuery(
                userName = userName,
                endpoint = endpoint,
                queryParams = queryParams,
                status = status,
                message = message
            )

            logger.debug("Query history saved: user=$userName, endpoint=$endpoint, status=$status")
        } catch (e: Exception) {
            logger.error("Failed to save query history: ${e.message}", e)
            // Don't throw exception here to avoid breaking the main business logic
        }
    }

    /**
     * Data class to hold endpoint information
     */
    private data class EndpointInfo(
        val endpoint: String,
        val queryParams: String
    )
}