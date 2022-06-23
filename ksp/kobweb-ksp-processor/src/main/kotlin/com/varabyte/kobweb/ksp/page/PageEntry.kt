package com.varabyte.kobweb.ksp.page

/**
 * Information about a method in the user's code targeted by an `@Page` annotation.
 *
 * @param fqn The fully qualified name of the method
 * @param route The associated route that should be generated for this page method, e.g. "/example/path". The final
 *   value is usually decided by the current file name but could be influenced by arguments in the `@Page` annotation
 *   as well.
 */
data class PageEntry(
    val fqn: String,
    val route: String,
) {
    val genFunctionName = "register${fqn.substringAfterLast(".")}"
}