package dev.thomato.auth.account

import com.tngtech.archunit.core.domain.JavaClasses
import com.tngtech.archunit.core.importer.ImportOption
import com.tngtech.archunit.junit.AnalyzeClasses
import com.tngtech.archunit.junit.ArchTest
import com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes
import com.tngtech.archunit.library.Architectures.layeredArchitecture

/** The layer rules from ADR 0001. */
@AnalyzeClasses(packages = ["dev.thomato.auth.account"], importOptions = [ImportOption.DoNotIncludeTests::class])
class ArchitectureTests {
    @ArchTest
    fun `dependencies point inward, towards the domain`(classes: JavaClasses) {
        layeredArchitecture()
            .consideringOnlyDependenciesInLayers()
            .layer("Domain")
            .definedBy("..account.domain..")
            .layer("Application")
            .definedBy("..account.application..")
            .layer("Api")
            .definedBy("..account.api..")
            .layer("Infrastructure")
            .definedBy("..account.infrastructure..")
            .whereLayer("Api")
            .mayNotBeAccessedByAnyLayer()
            .whereLayer("Infrastructure")
            .mayNotBeAccessedByAnyLayer()
            .whereLayer("Application")
            .mayOnlyBeAccessedByLayers("Api", "Infrastructure")
            .whereLayer("Domain")
            .mayOnlyBeAccessedByLayers("Application", "Api", "Infrastructure")
            .check(classes)
    }

    @ArchTest
    fun `the domain depends on nothing but itself and the standard library`(classes: JavaClasses) {
        classes()
            .that()
            .resideInAPackage("..account.domain..")
            .should()
            .onlyDependOnClassesThat()
            .resideInAnyPackage("..account.domain..", "java..", "kotlin..", "org.jetbrains.annotations..")
            .check(classes)
    }

    @ArchTest
    fun `the application layer depends on nothing but the domain and the standard library`(classes: JavaClasses) {
        classes()
            .that()
            .resideInAPackage("..account.application..")
            .should()
            .onlyDependOnClassesThat()
            .resideInAnyPackage(
                "..account.application..",
                "..account.domain..",
                "java..",
                "kotlin..",
                "org.jetbrains.annotations..",
            ).check(classes)
    }
}
