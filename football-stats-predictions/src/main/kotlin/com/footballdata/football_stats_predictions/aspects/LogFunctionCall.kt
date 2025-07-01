package com.footballdata.football_stats_predictions.aspects

import com.footballdata.football_stats_predictions.logger
import org.aspectj.lang.ProceedingJoinPoint
import org.aspectj.lang.annotation.Around
import org.aspectj.lang.annotation.Aspect
import org.aspectj.lang.reflect.MethodSignature
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.stereotype.Component

@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
annotation class LogFunctionCall()

@Aspect
@Component
class LogFunctionCallAspect() {

    @Around("@annotation(LogFunctionCall)")
    @Throws(Throwable::class)
    fun logFunctionCall(joinPoint: ProceedingJoinPoint): Any? {

        // Get the method signature and class name
        val signature = joinPoint.signature as MethodSignature
        val className = joinPoint.target.javaClass.simpleName.toString()

        // Logs class name and method name with parameters
        val message =
            if (joinPoint.args.isEmpty()) {
                "Invoked: " + className + "." + signature.name + " with no arguments"
            } else {
                "Invoked: " + className + "." + signature.name +
                        " with arguments: {${parametersToString(joinPoint.args)}}"
            }

        val startTime = System.currentTimeMillis()

        val result = joinPoint.proceed()

        // capture time taken to execute the method
        val endTime = System.currentTimeMillis()
        val timeTaken = endTime - startTime

        // Log the method execution time
        logger.info("$message in $timeTaken ms")

        return result
    }

    private fun parametersToString(parameterArray: Array<Any?>): String {
        // if the parameter is UsernamePasswordAuthenticationToken only log the username
        val parameterArray = parameterArray.map { param ->
            when (param) {
                is UsernamePasswordAuthenticationToken -> "[User: ${param.name}]"
                is String, is Number, is Boolean -> param
                else -> if (param != null) "Object of type ${param::class.simpleName}" else "null"
            }
        }
        return parameterArray.joinToString(", ")
    }
}