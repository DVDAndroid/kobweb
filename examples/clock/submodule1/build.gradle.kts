plugins {
  alias(libs.plugins.kotlin.multiplatform)
  alias(libs.plugins.jetbrains.compose)
  id("com.google.devtools.ksp")
//  id(libs.plugins.kobweb.application.get().pluginId)
}

repositories {
  mavenCentral()
  maven("https://maven.pkg.jetbrains.space/public/p/compose/dev")
  google()
}

kotlin {
  js(IR) {
    moduleName = "clock2"
    browser {
      commonWebpackConfig {
        outputFileName = "clock2.js"
      }
    }
    binaries.executable()
  }
  sourceSets {
    val jsMain by getting {
      dependencies {
        implementation(compose.runtime)
        implementation(libs.kobweb.core)
        implementation(libs.kobweb.silk.core)
        implementation(libs.kobweb.silk.icons.fa)

        kotlin.srcDir("build/generated/ksp/js/jsMain/kotlin")
      }
    }
  }
}

dependencies {
  add("kspJs","com.varabyte.kobweb:kobweb-ksp-processor")
}

ksp {
  arg("kobweb.basePackageName", "clock.sub")
  arg("kobweb.isLibrary", "true")
  arg("kobweb.moduleName", "submodule1")
}