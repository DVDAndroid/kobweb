package com.varabyte.kobweb.ksp.pagemapping

import com.google.devtools.ksp.KspExperimental
import com.google.devtools.ksp.getAnnotationsByType
import com.google.devtools.ksp.processing.KSPLogger
import com.google.devtools.ksp.symbol.KSFile
import com.google.devtools.ksp.symbol.KSVisitorVoid
import com.varabyte.kobweb.core.PackageMapping
import com.varabyte.kobweb.ksp.processor.AnnotationProcessorProvider.Companion.kobwebKSPOptions
import com.varabyte.kobweb.ksp.processor.JSAnnotationProcessor.Companion.pageMappingEntries

@OptIn(KspExperimental::class)
class PageMappingAnnotationProcessor(
    private val logger: KSPLogger
) : KSVisitorVoid() {

    override fun visitFile(file: KSFile, data: Unit) {
        super.visitFile(file, data)

        val annotation = file.getAnnotationsByType(PackageMapping::class).singleOrNull() ?: return
        val mapping = annotation.value

        val currentPackage = file.packageName.asString()
        if (currentPackage.startsWith(kobwebKSPOptions.pagesPackageName)) {
            pageMappingEntries[currentPackage] = mapping.let { value ->
                // {} is a special value which means infer from the current package,
                // e.g. `a.b.pkg` -> `"{pkg}"`
                if (value != "{}") value else "{${currentPackage.substringAfterLast('.')}}"
            }
        } else {
            logger.warn("${file.filePath}: Skipped over `@file:PackageMapping`. It is defined under package `$currentPackage` but must exist under `${kobwebKSPOptions.pagesPackageName}`")
        }
    }
}