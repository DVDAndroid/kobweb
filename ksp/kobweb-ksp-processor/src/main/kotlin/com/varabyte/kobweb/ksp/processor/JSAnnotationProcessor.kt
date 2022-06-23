package com.varabyte.kobweb.ksp.processor

import com.google.devtools.ksp.getClassDeclarationByName
import com.google.devtools.ksp.getFunctionDeclarationsByName
import com.google.devtools.ksp.processing.CodeGenerator
import com.google.devtools.ksp.processing.KSPLogger
import com.google.devtools.ksp.processing.Resolver
import com.google.devtools.ksp.processing.SymbolProcessor
import com.google.devtools.ksp.symbol.KSAnnotated
import com.google.devtools.ksp.symbol.KSFile
import com.google.devtools.ksp.symbol.KSFunctionDeclaration
import com.google.devtools.ksp.symbol.KSName
import com.google.devtools.ksp.validate
import com.squareup.kotlinpoet.ksp.writeTo
import com.varabyte.kobweb.core.PackageMapping
import com.varabyte.kobweb.core.Page
import com.varabyte.kobweb.ksp.page.PageAnnotationProcessor
import com.varabyte.kobweb.ksp.page.PageEntry
import com.varabyte.kobweb.ksp.pagemapping.PageMappingAnnotationProcessor
import com.varabyte.kobweb.ksp.processor.AnnotationProcessorProvider.Companion.kobwebKSPOptions
import com.varabyte.kobweb.ksp.routes.buildPageRouterFile
import com.varabyte.kobweb.ksp.routes.buildPageRouterFun

class JSAnnotationProcessor(
    private val codeGenerator: CodeGenerator,
    private val logger: KSPLogger,
) : SymbolProcessor {

    companion object {
        internal val pageMappingEntries = hashMapOf<String, String>()
        internal val pagesEntries = mutableListOf<PageEntry>()
    }

    override fun process(resolver: Resolver): List<KSAnnotated> {
        val packageMappings = resolver.getSymbolsWithAnnotation(PackageMapping::class.qualifiedName!!)
        val pages = resolver.getSymbolsWithAnnotation(Page::class.qualifiedName!!)

        packageMappings.filter(KSAnnotated::validate)
            .filterIsInstance<KSFile>()
            .forEach { it.accept(PageMappingAnnotationProcessor(logger), Unit) }

        pages.filter(KSAnnotated::validate)
            .filterIsInstance<KSFunctionDeclaration>()
            .forEach { it.accept(PageAnnotationProcessor(logger), Unit) }

        pagesEntries.groupBy { it.route }
            .filter { routeToPages -> routeToPages.value.size > 1 }
            .forEach { routeToPages ->
                logger.warn("Route \"${routeToPages.key}\" was generated multiple times; only the one navigating to \"${routeToPages.value.first().fqn}()\" will be used.")
                routeToPages.value.asSequence().drop(1).forEach { page -> pagesEntries -= page }
            }

        return emptyList()
    }

    override fun finish() {
        super.finish()

        if (pagesEntries.isNotEmpty()) {
            buildPageRouterFile(kobwebKSPOptions.pagesPackageName, pagesEntries.map(::buildPageRouterFun))
                .writeTo(codeGenerator, aggregating = false)
        }

        if (!kobwebKSPOptions.isLibrary) {
            buildKobwebFile().writeTo(codeGenerator, aggregating = false)
        }
    }

}



