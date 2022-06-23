package com.varabyte.kobweb.ksp.routes

import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.FileSpec
import com.squareup.kotlinpoet.FunSpec
import com.varabyte.kobweb.ksp.page.PageEntry
import com.varabyte.kobweb.ksp.processor.AnnotationProcessorProvider
import com.varabyte.kobweb.ksp.processor.AnnotationProcessorProvider.Companion.kobwebKSPOptions

private val routerClass = ClassName("com.varabyte.kobweb.navigation", "Router")

fun buildPageRouterFun(page: PageEntry): FunSpec = FunSpec.builder(page.genFunctionName)
    .receiver(routerClass)
    .addStatement("register(%S) { %L() }", page.route, page.fqn)
    .build()

fun buildPageRouterFile(basePackageName: String, funs: List<FunSpec>): FileSpec =
    FileSpec.builder(basePackageName, "KobwebRoutes")
        .apply { funs.forEach { addFunction(it) } }
        .addFunction(FunSpec.builder(allRoutesFunName)
            .receiver(routerClass)
            .apply { funs.forEach { addStatement("%L()", it.name) } }
            .build())
        .build()

val allRoutesFunName get() = buildString {
    append("all")
    append(kobwebKSPOptions.moduleName.replaceFirstChar { it.uppercase() })
    append("Routes")
}

/**
 * Given a qpackage, e.g. "a._123.int_", and some mappings, e.g. "a._123" to "123" and "a._123.int_" to "int",
 * generate a final route, e.g. "a/123/int"
 */
fun resolvePackageName(packageMappings: Map<String, String>, pkg: String): String {
    // We have a bunch of potential package to URL mappings, which work on fully qualified packages, so
    // we process each part of the package separately, going back to front. An example will help here.
    //
    // If we had the following mappings:
    //
    //   site.pages.blogs._2021._12 -> 12
    //   site.pages.blogs._2021 -> 2021
    //
    // then we'd transform the following fully-qualified package by first building up a list of parts in
    // reverse. So:
    //
    //   site.pages.blogs._2021._12.tutorials
    //
    // is processed like so (* means a mapping match was found):
    //
    //   site.pages.blogs._2021._12.tutorials ---> [tutorials]
    //   site.pages.blogs._2021._12 (*)       ---> [12, tutorials]
    //   site.pages.blogs._2021 (*)           ---> [2021, 12, tutorials]
    //   site.pages.blogs                     ---> [blogs, 2021, 12, tutorials]
    //   site.pages                           ---> [pages, blogs, 2021, 12, tutorials]
    //   site                                 ---> [site, pages, blogs, 2021, 12, tutorials]
    //
    // At which point, we're done, and we can just join the final list together to a path:
    //
    //   site/pages/blogs/2021/12/tutorials
    @Suppress("NAME_SHADOWING") // Make pkg writable
    var pkg = pkg
    val transformedParts = mutableListOf<String>()
    while (pkg.isNotEmpty()) {
        transformedParts.add(0, packageMappings[pkg] ?: pkg.substringAfterLast('.'))
        pkg = (pkg.takeIf { it.contains('.') } ?: "").substringBeforeLast('.')
    }
    return transformedParts.joinToString("/")
}