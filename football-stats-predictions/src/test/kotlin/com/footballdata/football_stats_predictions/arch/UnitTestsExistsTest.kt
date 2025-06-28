package com.footballdata.football_stats_predictions.arch

import com.tngtech.archunit.core.domain.JavaClasses
import com.tngtech.archunit.junit.AnalyzeClasses
import com.tngtech.archunit.junit.ArchTest
import com.tngtech.archunit.lang.ArchRule
import com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes

@AnalyzeClasses(packages = ["com.footballdata.football_stats_predictions"])
class UnitTestsExistsTest {
    @ArchTest
    fun rule_as_method(importedClasses: JavaClasses) {
        // Ensure that each class in the model package has a corresponding unit test
        val myRule: ArchRule = classes()
            .that().resideInAPackage("..model..")
            .should().haveSimpleNameEndingWith("Test")

        myRule.check(importedClasses)
    }
}