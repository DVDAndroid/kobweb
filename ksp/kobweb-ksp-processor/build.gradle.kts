import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("jvm")
    id("com.google.devtools.ksp") version "1.6.10-1.0.4"
    id("com.varabyte.kobweb.internal.publish")
    alias(libs.plugins.jetbrains.compose)
}

group = "com.varabyte.kobweb"
version = libs.versions.kobweb.libs.get()

dependencies {
    implementation(compose.runtime)
    implementation(project(":common:kobweb-annotations"))

    implementation("com.google.devtools.ksp:symbol-processing-api:1.6.10-1.0.4")
    implementation("com.squareup:kotlinpoet:1.11.0")
    implementation("com.squareup:kotlinpoet-ksp:1.11.0")
    implementation("com.google.auto.service:auto-service-annotations:1.0.1")
    ksp("dev.zacsweers.autoservice:auto-service-ksp:1.0.0")
}

kobwebPublication {
    artifactId.set("kobweb-ksp-processor")
    description.set("KSP")
}

tasks.withType<KotlinCompile>().configureEach {
    kotlinOptions.freeCompilerArgs += listOf(
        "-opt-in=kotlin.RequiresOptIn",
        "-opt-in=com.google.devtools.ksp.KspExperimental",
        "-opt-in=com.squareup.kotlinpoet.ksp.KotlinPoetKspPreview",
    )
}