plugins {
    kotlin("jvm") version "2.1.20"
    id("org.jetbrains.compose") version "1.8.0-rc01"
    id("org.jetbrains.kotlin.plugin.compose") version "2.2.0-Beta1"
}

group = "com.esicad.caristsi"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
    maven("https://maven.pkg.jetbrains.space/public/p/compose/dev")
    google()
}

dependencies {
    // Dépendance de base Compose (sans icônes Material)
    implementation(compose.desktop.currentOs)

    // Vos autres dépendances
    implementation("org.ktorm:ktorm-core:4.1.1")
    implementation("org.ktorm:ktorm-support-mysql:3.6.0")
    implementation("com.mysql:mysql-connector-j:8.3.0")
    implementation("org.mindrot:jbcrypt:0.4")
}

compose.desktop {
    application {
        mainClass = "MainKt"

        nativeDistributions {
            targetFormats(
                org.jetbrains.compose.desktop.application.dsl.TargetFormat.Dmg,
                org.jetbrains.compose.desktop.application.dsl.TargetFormat.Msi,
                org.jetbrains.compose.desktop.application.dsl.TargetFormat.Deb
            )
            packageName = "esicad_Arka_desktop"
            packageVersion = "1.0.0"
        }
    }
}