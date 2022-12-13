package com.varabyte.kobweb.gradle.library.util

import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension

fun KotlinMultiplatformExtension.configAsKobwebLibrary(
    includeServer: Boolean = false,
    targetName: String = "js",
) {
    js(targetName, IR) {
        kobwebLibraryBrowser()
    }
    if (includeServer) {
        jvm()
    }
}
