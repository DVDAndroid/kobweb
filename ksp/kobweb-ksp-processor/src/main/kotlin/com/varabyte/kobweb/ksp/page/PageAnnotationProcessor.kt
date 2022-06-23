package com.varabyte.kobweb.ksp.page

import androidx.compose.runtime.Composable
import com.google.devtools.ksp.KspExperimental
import com.google.devtools.ksp.getAnnotationsByType
import com.google.devtools.ksp.isAnnotationPresent
import com.google.devtools.ksp.processing.KSPLogger
import com.google.devtools.ksp.symbol.KSFunctionDeclaration
import com.google.devtools.ksp.symbol.KSVisitorVoid
import com.varabyte.kobweb.core.Page
import com.varabyte.kobweb.ksp.processor.AnnotationProcessorProvider.Companion.kobwebKSPOptions
import com.varabyte.kobweb.ksp.processor.JSAnnotationProcessor.Companion.pageMappingEntries
import com.varabyte.kobweb.ksp.processor.JSAnnotationProcessor.Companion.pagesEntries
import com.varabyte.kobweb.ksp.routes.resolvePackageName

@OptIn(KspExperimental::class)
class PageAnnotationProcessor(
    private val logger: KSPLogger
) : KSVisitorVoid() {

    override fun visitFunctionDeclaration(function: KSFunctionDeclaration, data: Unit) {
        super.visitFunctionDeclaration(function, data)

        val funName = function.simpleName.asString()
        if (!function.isAnnotationPresent(Composable::class)) {
            logger.error("${function.containingFile?.filePath}: `fun $funName` annotated with `@Page` must also be `@Composable`.")
            return
        }

        val routeOverride = function.getAnnotationsByType(Page::class).single().routeOverride.takeIf { it.isNotBlank() }
        val packageName = function.packageName.asString()
        if (routeOverride?.startsWith("/") == true
            || packageName.startsWith(kobwebKSPOptions.pagesPackageName)
        ) {
            // For simplicity for now, we reject route overrides which use the dynamic
            // route syntax in any part except for the last, e.g. in
            // "/dynamic/{}/route/{}/example/{}" the last "{}" is OK but the previous
            // ones are not currently supported.
            if (routeOverride == null || "{}" !in routeOverride.substringBeforeLast("/", missingDelimiterValue = "")) {
                val slugPrefix = if (routeOverride != null && routeOverride.startsWith("/")) {
                    // If route override starts with "/" it means the user set the full route explicitly
                    routeOverride.substringBeforeLast('/')
                } else {
                    resolvePackageName(pageMappingEntries, packageName)
                        .removePrefix(kobwebKSPOptions.pagesPackageName.replace('.', '/'))
                }


                val prefixExtra = if (routeOverride != null
                    && !routeOverride.startsWith("/")
                    && routeOverride.contains("/")
                ) {
                    // If route override did NOT begin with slash, but contains at least one subdir, it means append
                    // subdir to base route
                    "/" + routeOverride.substringBeforeLast("/")
                } else {
                    ""
                }

                val slugFromFile = function.containingFile
                    ?.fileName
                    ?.removeSuffix(".kt")
                    ?.lowercase() ?: error("No file name for ${function.containingFile?.filePath}")
                val slug = if (routeOverride != null && routeOverride.last() != '/') {
                    routeOverride.substringAfterLast("/").let { value ->
                        // {} is a special value which means infer from the current file,
                        // e.g. `Slug.kt` -> `"{slug}"`
                        if (value != "{}") value else "{$slugFromFile}"
                    }
                } else {
                    slugFromFile
                }.takeIf { it != "index" } ?: ""

                pagesEntries += PageEntry(
                    function.qualifiedName?.asString()
                        ?: error("No qualified name for ${function.simpleName.asString()}"),
                    "$slugPrefix$prefixExtra/$slug"
                )
            } else {
                logger.warn("${function.containingFile?.filePath}: Skipped over `@Page fun $funName`. Route override is invalid.")
            }
        } else {
            logger.warn("${function.containingFile?.filePath}: Skipped over `@Page fun ${funName}`. It is defined under package `${function.packageName.asString()}` but must exist under `${kobwebKSPOptions.pagesPackageName}`")
        }
    }

}

