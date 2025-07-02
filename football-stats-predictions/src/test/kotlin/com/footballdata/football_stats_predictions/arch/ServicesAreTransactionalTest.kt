package com.footballdata.football_stats_predictions.arch

import com.tngtech.archunit.core.domain.JavaClasses
import com.tngtech.archunit.junit.AnalyzeClasses
import com.tngtech.archunit.junit.ArchTest
import com.tngtech.archunit.lang.ArchRule
import com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes
import org.springframework.transaction.annotation.Transactional

@AnalyzeClasses(packages = ["com.footballdata.football_stats_predictions.service"])
class ServicesAreTransactionalTest {
    @ArchTest
    fun services_are_annotated_as_transactional(importedClasses: JavaClasses) {
        val myRule: ArchRule = classes()
            .that().resideInAPackage("..service..")
            .and().areNotNestedClasses()
            .and().haveSimpleNameNotContaining("$") // Excluded compiler generated classes and anonymous classes
            .should().beAnnotatedWith(Transactional::class.java)

        myRule.check(importedClasses)
    }
}