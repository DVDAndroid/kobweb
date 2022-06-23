plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.jetbrains.compose)
//    id(libs.plugins.kobweb.application.get().pluginId)
    id("com.google.devtools.ksp") version "1.6.10-1.0.4"
}

repositories {
    mavenLocal()
    mavenCentral()
    maven("https://maven.pkg.jetbrains.space/public/p/compose/dev")
    google()
}

group = "clock"
version = "1.0-SNAPSHOT"

kotlin {
    js(IR) {
        moduleName = "clock"
        browser {
            commonWebpackConfig {
                outputFileName = "clock.js"
            }
        }
        binaries.executable()
    }
    sourceSets {
        val jsMain by getting {
            dependencies {
                implementation(compose.runtime)
                implementation(libs.kobweb.core)
                implementation(libs.kobweb.silk.core)
                implementation(libs.kobweb.silk.icons.fa)
                implementation(project(":submodule1"))

                kotlin.srcDir("build/generated/ksp/js/jsMain/kotlin")
            }
        }
    }
}

dependencies {
    add("kspJs","com.varabyte.kobweb:kobweb-ksp-processor")
}

ksp {
    arg("kobweb.basePackageName", "clock")
}