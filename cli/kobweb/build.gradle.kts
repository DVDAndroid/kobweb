import org.jreleaser.model.Active

plugins {
    kotlin("jvm")
    kotlin("plugin.serialization")
    application
    alias(libs.plugins.jreleaser)
}

group = "com.varabyte.kobweb.cli"
version = libs.versions.kobweb.get()

dependencies {
    implementation(kotlin("stdlib"))
    implementation(libs.kotlinx.cli)
    implementation(libs.kotlinx.coroutines)
    implementation(libs.kotter)
    implementation(libs.jgit)
    implementation(libs.freemarker)
    implementation(libs.kaml)
    implementation(project(":common:kobweb-common"))
}

application {
    applicationDefaultJvmArgs = listOf("-Dkobweb.version=${version}")
    mainClass.set("MainKt")
}

// Avoid ambiguity / add clarity in generated artifacts
tasks.jar {
    archiveFileName.set("kobweb-cli.jar")
}

// Read about JReleaser at https://jreleaser.org/guide/latest/index.html
jreleaser {
    dryrun.set(true)
    gitRootSearch.set(true)
    project {
        website.set("https://kobweb.varabyte.com/")
        docsUrl.set("https://kobweb.varabyte.com/docs")
        description.set("Kobweb CLI provides commands to handle the tedious parts of building a Compose for Web app")
        longDescription.set("""
            Kobweb CLI provides commands to handle the tedious parts of building a Compose for Web app, including
            project setup and configuration.
        """.trimIndent())
        authors.set(listOf("David Herman"))
        license.set("Apache-2.0")
        copyright.set("Copyright © 2022 Varabyte. All rights reserved.")
    }
    release {
        github {
            owner.set("varabyte")
            releaseName.set("Kobweb CLI {{tagName}}")
            // Tags and releases are handled manually at github for now
            skipTag.set(true)
            skipRelease.set(true)

            overwrite.set(true)
            sign.set(true)
            uploadAssets.set(Active.RELEASE)
            commitAuthor {
                name.set("David Herman")
                email.set("bitspittle@gmail.com")
            }
            changelog {
                enabled.set(false)
            }
            milestone {
                // milestone management handled manually for now
                close.set(false)
            }

            // These values are specified in ~/.gradle/gradle.properties; otherwise sorry, no jreleasing for you :P
            mapOf<String, (String) -> Unit>(
                "varabyte.github.username" to { username.set(it) },
                "varabyte.github.token" to { token.set(it) },
            ).forEach { (key, setter) ->
                (findProperty(key) as? String)?.let { setter(it) } ?: run {
                    println("\"$key\" is missing so disabling github release")
                    enabled.set(false)
                }
            }
        }
    }
    distributions {
        create("kobweb") {
            listOf("zip", "tar").forEach { artifactExtension ->
                artifact {
                    setPath("build/distributions/{{distributionName}}-{{projectVersion}}.$artifactExtension")
                }
            }
        }
    }
}