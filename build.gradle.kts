plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlin.spring)
    war
    alias(libs.plugins.spring.boot)
    alias(libs.plugins.spring.dependency.management)
    alias(libs.plugins.kotlin.jpa)
    alias(libs.plugins.ktlint)
    alias(libs.plugins.detekt)
}

group = "dev.thomato"
version = "0.0.1-SNAPSHOT"

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(21)
    }
}

configurations {
    compileOnly {
        extendsFrom(configurations.annotationProcessor.get())
    }
}

repositories {
    mavenCentral()
}

dependencies {
    // Detekt plugins
    detektPlugins("io.gitlab.arturbosch.detekt:detekt-formatting:1.23.5")

    // Use bundles for better organization
    implementation(libs.bundles.spring.boot.web)
    implementation(libs.bundles.spring.data)
    implementation(libs.bundles.spring.security)
    implementation(libs.bundles.reactive)

    // Additional individual dependencies
    implementation(libs.spring.boot.starter.graphql)
    implementation(libs.spring.boot.starter.mail)

    // Database dependencies
    implementation(libs.flyway.core)
    implementation(libs.flyway.database.postgresql)
    runtimeOnly(libs.postgresql)
    runtimeOnly(libs.r2dbc.postgresql)

    // Development dependencies
    developmentOnly(libs.spring.boot.devtools)
    developmentOnly(libs.spring.boot.docker.compose)

    // Annotation processing
    annotationProcessor(libs.spring.boot.configuration.processor)

    // WAR deployment
    providedRuntime(libs.spring.boot.starter.tomcat)

    // Testing
    testImplementation(libs.bundles.testing)
    testRuntimeOnly(libs.junit.platform.launcher)
}

kotlin {
    compilerOptions {
        freeCompilerArgs.addAll("-Xjsr305=strict")
    }
}

allOpen {
    annotation("jakarta.persistence.Entity")
    annotation("jakarta.persistence.MappedSuperclass")
    annotation("jakarta.persistence.Embeddable")
}

tasks.withType<Test> {
    useJUnitPlatform()
}

// KtLint configuration
ktlint {
    reporters {
        reporter(org.jlleitschuh.gradle.ktlint.reporter.ReporterType.PLAIN)
        reporter(org.jlleitschuh.gradle.ktlint.reporter.ReporterType.CHECKSTYLE)
    }

    filter {
        exclude("**/generated/**")
        exclude("**/build/**")
    }
}

// Detekt configuration
detekt {
    buildUponDefaultConfig = true
    allRules = false
    config.setFrom("$projectDir/detekt.yml")
    ignoreFailures = true // Temporarily ignore failures to test
}
