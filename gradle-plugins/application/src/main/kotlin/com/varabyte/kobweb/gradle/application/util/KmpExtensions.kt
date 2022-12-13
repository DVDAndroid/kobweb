package com.varabyte.kobweb.gradle.application.util

import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension

fun KotlinMultiplatformExtension.configAsKobwebApplication(
    moduleName: String? = null,
    includeServer: Boolean = false,
    targetName: String = "js",
) {
    js(targetName, IR) {
        kobwebApplicationBrowser(moduleName)
    }
    if (includeServer) {
        jvm {
            kobwebServerJar(moduleName)
        }
    }
}
