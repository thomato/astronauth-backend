plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlin.spring)
    alias(libs.plugins.spring.boot)
    alias(libs.plugins.spring.dependency.management)
    alias(libs.plugins.ktlint)
    alias(libs.plugins.detekt)
    alias(libs.plugins.node.gradle)
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
    implementation(libs.bouncycastle)

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

// The first-party pages (ADR 0004): built from frontend/ with a Node and pnpm that Gradle downloads itself,
// so neither a developer's machine nor CI needs them installed
node {
    download = true
    version = "22.23.1"
    pnpmVersion = "10.18.2"
    nodeProjectDir = file("frontend")
}

val frontendSources =
    fileTree("frontend") {
        include("src/**", "public/**", "index.html", "package.json", "pnpm-lock.yaml", "*.config.*", "codegen.ts")
        include("tsconfig*.json", ".prettierrc.json", ".prettierignore")
        exclude("src/graphql/generated/**")
    }

val frontendBuild by tasks.registering(com.github.gradle.node.pnpm.task.PnpmTask::class) {
    description = "Builds the SPA into build/frontend."
    dependsOn(tasks.pnpmInstall)
    args = listOf("run", "build")
    inputs.files(frontendSources)
    // The frontend's types are generated from the server's schema
    inputs.dir("src/main/resources/graphql")
    outputs.dir(layout.buildDirectory.dir("frontend"))
}

val frontendTest by tasks.registering(com.github.gradle.node.pnpm.task.PnpmTask::class) {
    description = "Runs the SPA's component tests."
    dependsOn(tasks.pnpmInstall)
    args = listOf("run", "test")
    inputs.files(frontendSources)
    inputs.dir("src/main/resources/graphql")
}

val frontendLint by tasks.registering(com.github.gradle.node.pnpm.task.PnpmTask::class) {
    description = "Lints, formats and type-checks the SPA."
    dependsOn(tasks.pnpmInstall)
    args = listOf("run", "lint")
    inputs.files(frontendSources)
}

tasks.processResources {
    from(frontendBuild) { into("static") }
}

tasks.test {
    dependsOn(frontendTest)
}

tasks.check {
    dependsOn(frontendLint)
}

// Development: the dev profile turns on GraphiQL
tasks.bootRun {
    systemProperty("spring.profiles.active", "dev")
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
