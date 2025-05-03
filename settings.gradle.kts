pluginManagement {
    repositories {
        maven("https://maven.pkg.jetbrains.space/public/p/compose/dev")
        google()
        gradlePluginPortal()
        mavenCentral()
    }

    plugins {
        kotlin("jvm").version("2.1.20")
        id("org.jetbrains.compose").version("1.8.0-rc01")
        id("org.jetbrains.kotlin.plugin.compose").version("2.1.20")
    }
}

rootProject.name = "Arka_desktop"