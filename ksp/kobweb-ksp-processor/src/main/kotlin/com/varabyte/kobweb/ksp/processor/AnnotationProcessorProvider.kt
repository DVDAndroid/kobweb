package com.varabyte.kobweb.ksp.processor

import com.google.auto.service.AutoService
import com.google.devtools.ksp.processing.JsPlatformInfo
import com.google.devtools.ksp.processing.SymbolProcessor
import com.google.devtools.ksp.processing.SymbolProcessorEnvironment
import com.google.devtools.ksp.processing.SymbolProcessorProvider
import com.varabyte.kobweb.ksp.processor.KobwebKSPOptions.Companion.BASE_PACKAGE_NAME_OPTION
import com.varabyte.kobweb.ksp.processor.KobwebKSPOptions.Companion.IS_LIBRARY
import com.varabyte.kobweb.ksp.processor.KobwebKSPOptions.Companion.MODULE_NAME
import com.varabyte.kobweb.ksp.processor.KobwebKSPOptions.Companion.PAGES_PACKAGE_NAME_OPTION


@AutoService(SymbolProcessorProvider::class)
class AnnotationProcessorProvider : SymbolProcessorProvider {
    companion object {
        lateinit var kobwebKSPOptions: KobwebKSPOptions
    }

    override fun create(environment: SymbolProcessorEnvironment): SymbolProcessor {
        val basePackageName = environment.options[BASE_PACKAGE_NAME_OPTION]
        val isLibrary = environment.options[IS_LIBRARY]?.toBoolean() ?: false
        val moduleName = environment.options[MODULE_NAME]
        if (isLibrary && moduleName == null) error("Module name must be specified if building as library")

        kobwebKSPOptions = KobwebKSPOptions(
            basePackageName = basePackageName ?: error("$BASE_PACKAGE_NAME_OPTION is required"),
            pagesPackageName = environment.options[PAGES_PACKAGE_NAME_OPTION] ?: "${basePackageName}.pages",
            isLibrary = isLibrary,
            moduleName = moduleName.orEmpty(),
        )

        return when (val platform = environment.platforms.single()) {
            is JsPlatformInfo -> JSAnnotationProcessor(
                environment.codeGenerator,
                environment.logger,
            )
            else -> error("Unsupported platform: $platform")
        }
    }
}

