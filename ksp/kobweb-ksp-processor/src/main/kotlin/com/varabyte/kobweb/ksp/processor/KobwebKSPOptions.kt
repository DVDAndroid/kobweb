package com.varabyte.kobweb.ksp.processor

data class KobwebKSPOptions(
    val basePackageName: String,
    val pagesPackageName: String,
    val isLibrary: Boolean,
    val moduleName: String,
) {
    companion object {
        private const val KOBWEB = "kobweb."
        const val BASE_PACKAGE_NAME_OPTION = "${KOBWEB}basePackageName"
        const val PAGES_PACKAGE_NAME_OPTION = "${KOBWEB}pagesPackageName"
        const val IS_LIBRARY = "${KOBWEB}isLibrary"
        const val MODULE_NAME = "${KOBWEB}moduleName"
    }
}