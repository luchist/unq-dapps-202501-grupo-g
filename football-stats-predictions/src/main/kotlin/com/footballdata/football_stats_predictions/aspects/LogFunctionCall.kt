package com.footballdata.football_stats_predictions.aspects

import com.footballdata.football_stats_predictions.logger
import org.aspectj.lang.ProceedingJoinPoint
import org.aspectj.lang.annotation.Around
import org.aspectj.lang.annotation.Aspect
import org.aspectj.lang.reflect.MethodSignature
import org.springframework.stereotype.Component


@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
annotation class LogFunctionCall(val logLevel: String = "INFO")

@Aspect
@Component
public class LogFunctionCallAspect(private val defaultLogLevel: String = "INFO") {

    @Around("@annotation(LogFunctionCall)")
    @Throws(Throwable::class)
    fun logExecutionTime(joinPoint: ProceedingJoinPoint): Any? {
        // Get the method signature and annotation details
        val signature = joinPoint.signature as MethodSignature
        val method = signature.method
        val annotation = method.getAnnotation(LogFunctionCall::class.java)
        val logLevel = annotation?.logLevel ?: defaultLogLevel

        // Log the method name and arguments if any
        val message =
            if (joinPoint.args.isEmpty()) {
                "Function called: \'${joinPoint.signature.name}\' with no arguments"
            } else {
                "Function called: \'${joinPoint.signature.name}\'" +
                        "with arguments: ${joinPoint.args.joinToString(", ")}"
            }

        //Log level
        when (logLevel.uppercase()) {
            "TRACE" -> logger.trace(message)
            "DEBUG" -> logger.debug(message)
            "INFO" -> logger.info(message)
            "WARN" -> logger.warn(message)
            "ERROR" -> logger.error(message)
            else -> logger.info(message) // default to INFO
        }
        return joinPoint.proceed()
    }

}