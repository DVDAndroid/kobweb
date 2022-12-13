package com.varabyte.kobweb.project

import java.nio.file.Files
import java.nio.file.Path

private const val KOBWEB_FOLDER = ".kobweb"

/**
 * The Kobweb folder is a special folder which contains various configuration files, runtime files, and other output
 * associated with a Kobweb project.
 *
 * If a normal directory has a Kobweb folder inside of it, then it is considered a Kobweb project.
 *
 * If a Kobweb project contains any subfolders that themselves own a Kobweb folder, then that is considered a different
 * project and opaque to the parent project.
 */
class KobwebFolder private constructor(val path: Path) {
    companion object {
        /**
         * Return a Kobweb folder if it is a child of the current path.
         */
        fun inPath(path: Path): KobwebFolder? {
            val kobwebFolderPath = path.resolve(KOBWEB_FOLDER).takeIf { Files.exists(it) } ?: return null
            return KobwebFolder(kobwebFolderPath)
        }

        /**
         * Convenience method for looking for a Kobweb folder in the current directory.
         */
        fun inWorkingDirectory(): KobwebFolder? = inPath(Path.of(""))

        /**
         * Return true if the current path represents a Kobweb application (that is, a directory that owns a Kobweb
         * folder).
         */
        fun isFoundIn(path: Path): Boolean {
            return inPath(path) != null
        }

        /**
         * Given some arbitrary path, find the Kobweb folder associated with it, climbing up the ancestor tree to do
         * so.
         *
         * So for example, "my-project/.kobweb/a/b/c/d.txt" will return "my-project" for "d.txt"
         *
         * The file may itself be under the Kobweb folder, or it may be a normal file that lives inside a Kobweb
         * project. This method will return the Kobweb folder for all cases.
         */
        fun fromChildPath(path: Path): KobwebFolder? {
            var curr: Path? = path
            while (curr != null) {
                inPath(curr)?.let { foundFolder -> return foundFolder }
                curr = curr.parent
            }
            return null
        }
    }

    /**
     * Return a child file that lives (or will live) within the Kobweb folder.
     */
    fun resolve(child: String): Path = path.resolve(child)
}