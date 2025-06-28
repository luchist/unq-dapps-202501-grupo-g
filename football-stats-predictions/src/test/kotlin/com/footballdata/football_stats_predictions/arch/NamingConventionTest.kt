package com.footballdata.football_stats_predictions.arch

import com.tngtech.archunit.junit.AnalyzeClasses
import com.tngtech.archunit.junit.ArchTest
import com.tngtech.archunit.lang.ArchRule
import com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes
import org.springframework.stereotype.Controller
import org.springframework.stereotype.Service


@AnalyzeClasses(packages = ["com.footballdata.football_stats_predictions"])
object NamingConventionTest {
    @ArchTest
    var services_should_be_prefixed: ArchRule? = classes()
        .that().resideInAPackage("..service..")
        .and().areAnnotatedWith(Service::class.java)
        .should().haveSimpleNameEndingWith("Service")

    @ArchTest
    var services_should_be_in_a_service_package: ArchRule? = classes()
        .that().haveSimpleNameEndingWith("Service")
        .should().resideInAPackage("..service..")

    @ArchTest
    var controllers_should_be_suffixed: ArchRule? = classes()
        .that().resideInAPackage("..webservice..")
        .and().areAnnotatedWith(Controller::class.java)
        .should().haveSimpleNameEndingWith("Controller")

    @ArchTest
    var classes_named_controller_should_be_in_a_controller_package: ArchRule? = classes()
        .that().haveSimpleNameEndingWith("Controller")
        .should().resideInAPackage("..webservice..")
}