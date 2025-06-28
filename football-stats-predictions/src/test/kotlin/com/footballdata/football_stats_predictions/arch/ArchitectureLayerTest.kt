package com.footballdata.football_stats_predictions.arch

import com.tngtech.archunit.junit.AnalyzeClasses
import com.tngtech.archunit.junit.ArchTest
import com.tngtech.archunit.lang.ArchRule
import com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes
import com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses

@AnalyzeClasses(packages = ["com.footballdata.football_stats_predictions"])
class ArchitectureLayerTest {

    // 'access' catches only violations by real accesses, i.e. accessing a field, calling a method; compare 'dependOn' further down
    @ArchTest
    val services_should_not_access_controllers: ArchRule? = noClasses().that().resideInAPackage("..service..")
        .should().accessClassesThat().resideInAPackage("..webservice..")

    @ArchTest
    val persistence_should_not_access_services: ArchRule? = noClasses().that().resideInAPackage("..repositories..")
        .should().accessClassesThat().resideInAPackage("..service..")

    @ArchTest
    val services_should_only_be_accessed_by_controllers_or_other_services: ArchRule? =
        classes().that().resideInAPackage("..service..")
            .should().onlyBeAccessed()
            .byAnyPackage("..webservice..", "..service..", "..config..", "..integration..", "..data..")


    // 'dependOn' catches a wider variety of violations, e.g. having fields of type, having method parameters of type, extending type ...
    @ArchTest
    val services_should_not_depend_on_controllers: ArchRule? = noClasses().that().resideInAPackage("..service..")
        .should().dependOnClassesThat().resideInAPackage("..webservice..")

    @ArchTest
    val persistence_should_not_depend_on_services: ArchRule? = noClasses().that().resideInAPackage("..repositories..")
        .should().dependOnClassesThat().resideInAPackage("..service..")

    @ArchTest
    val services_should_only_be_depended_on_by_controllers_or_other_services: ArchRule? =
        classes().that().resideInAPackage("..service..")
            .should().onlyHaveDependentClassesThat()
            .resideInAnyPackage("..webservice..", "..service..", "..config..", "..integration..", "..data..")
}