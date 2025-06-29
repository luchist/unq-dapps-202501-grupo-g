package com.footballdata.football_stats_predictions.aspects

import com.footballdata.football_stats_predictions.logger
import org.aspectj.lang.ProceedingJoinPoint
import org.aspectj.lang.annotation.Around
import org.aspectj.lang.annotation.Aspect
import org.springframework.stereotype.Component


@Target(AnnotationTarget.FUNCTION)
annotation class LogFunctionCall(val logLevel: String = "INFO")

@Aspect
@Component
public class LogFunctionCallAspect {

    @Around("@annotation(LogFunctionCall)")
    @Throws(Throwable::class)
    fun logExecutionTime(joinPoint: ProceedingJoinPoint): Any? {
        // The error level can be set dynamically

        // Log the method name and arguments if any
        if (joinPoint.args.isEmpty()) {
            logger.info("Function called: \'${joinPoint.signature.name}\' with no arguments")
        } else {
            logger.info(
                "Function called: \'${joinPoint.signature.name}\'" +
                        "with arguments: ${joinPoint.args.joinToString(", ")}"
            )
        }
        return joinPoint.proceed()
    }

}