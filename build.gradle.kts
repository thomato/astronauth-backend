plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlin.spring)
    alias(libs.plugins.spring.boot)
    alias(libs.plugins.spring.dependency.management)
    alias(libs.plugins.ktlint)
    alias(libs.plugins.detekt)
}

group = "dev.thomato"
version = "0.0.1-SNAPSHOT"

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(25)
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

// Keep the Kotlin libraries on the compiler's version instead of the older one Spring Boot manages
extra["kotlin.version"] = libs.versions.kotlin.get()

dependencies {
    // Detekt plugins
    detektPlugins("io.gitlab.arturbosch.detekt:detekt-formatting:1.23.8")

    // Use bundles for better organization
    implementation(libs.bundles.spring.boot.web)
    implementation(libs.bundles.spring.data)
    implementation(libs.bundles.spring.security)

    // Additional individual dependencies
    implementation(libs.spring.boot.starter.graphql)
    implementation(libs.spring.boot.starter.mail)

    // Database dependencies
    implementation(libs.flyway.database.postgresql)
    runtimeOnly(libs.postgresql)

    // Development dependencies
    developmentOnly(libs.spring.boot.devtools)
    developmentOnly(libs.spring.boot.docker.compose)

    // Annotation processing
    annotationProcessor(libs.spring.boot.configuration.processor)

    // Testing
    testImplementation(libs.bundles.testing)
    testRuntimeOnly(libs.junit.platform.launcher)
}

kotlin {
    compilerOptions {
        freeCompilerArgs.addAll("-Xjsr305=strict")
    }
}

tasks.withType<Test> {
    useJUnitPlatform()
}

// KtLint configuration
ktlint {
    version.set("1.8.0")

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
}

// ktlint and detekt embed their own Kotlin compiler and break when it is aligned with the project's newer Kotlin
val lintKotlinVersions = mapOf("ktlint" to "2.2.21", "detekt" to "2.0.21")
configurations.matching { it.name in lintKotlinVersions }.configureEach {
    val kotlinVersion = lintKotlinVersions.getValue(name)
    resolutionStrategy.eachDependency {
        if (requested.group == "org.jetbrains.kotlin") {
            useVersion(kotlinVersion)
        }
    }
}

// detekt 1.23 cannot resolve types for JVM targets above 22
tasks.withType<io.gitlab.arturbosch.detekt.Detekt>().configureEach {
    jvmTarget = "22"
}
tasks.withType<io.gitlab.arturbosch.detekt.DetektCreateBaselineTask>().configureEach {
    jvmTarget = "22"
}
