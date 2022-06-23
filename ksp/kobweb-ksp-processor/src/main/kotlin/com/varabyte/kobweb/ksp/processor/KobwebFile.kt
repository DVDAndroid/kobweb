package com.varabyte.kobweb.ksp.processor

import com.squareup.kotlinpoet.FileSpec
import com.squareup.kotlinpoet.FunSpec
import com.varabyte.kobweb.ksp.processor.AnnotationProcessorProvider.Companion.kobwebKSPOptions
import com.varabyte.kobweb.ksp.processor.JSAnnotationProcessor.Companion.pagesEntries
import com.varabyte.kobweb.ksp.routes.allRoutesFunName

fun buildKobwebFile(): FileSpec {
    val file = FileSpec.builder(kobwebKSPOptions.basePackageName, "kobweb").indent(" ".repeat(4))

    val routePrefix = "" //todo

    buildList {
        add("androidx.compose.runtime.CompositionLocalProvider")
        add("com.varabyte.kobweb.core.AppGlobalsLocal")
        add("com.varabyte.kobweb.navigation.RoutePrefix")
        add("com.varabyte.kobweb.navigation.Router")
        add("kotlinx.browser.document")
        add("kotlinx.browser.window")
        add("org.jetbrains.compose.web.renderComposable")
        if (pagesEntries.isNotEmpty()) {
            add("${kobwebKSPOptions.pagesPackageName}.${allRoutesFunName}")
        }
    }.sorted().forEach { file.addImport(it.substringBeforeLast('.'), it.substringAfterLast('.')) }

    val kobwebMain = FunSpec.builder("kobwebMain")
        .addStatement("RoutePrefix.set(%S)", routePrefix)
        .addStatement("val router = Router()")
        .apply {
            if (pagesEntries.isNotEmpty()) {
                addStatement("router.${allRoutesFunName}()")
            }
        }
        .addCode("""
                router.navigateTo(window.location.href.removePrefix(window.location.origin))

                // For SEO, we may bake the contents of a page in at build time. However, we will overwrite them
                // the first time we render this page with their composable, dynamic versions. Think of this as
                // poor man's hydration :)
                // See also: https://en.wikipedia.org/wiki/Hydration_(web_development)
                val root = document.getElementById("root")!!
                while (root.firstChild != null) {
                    root.removeChild(root.firstChild!!)
                }

                renderComposable(rootElementId = "root") {
                    CompositionLocalProvider(AppGlobalsLocal provides mapOf({appGlobals.map { entry -> "\"{entry.key.escapeQuotes()}\" to \"{entry.value.escapeQuotes()}\""}.joinToString() })) {
                        {siteData.app?.fqn ?: "com.varabyte.kobweb.core.KobwebApp"} {
                            router.renderActivePage()
                        }
                    }
                }
            """.trimIndent())

    return file.addFunction(kobwebMain.build()).build()
}