import org.jetbrains.compose.desktop.application.dsl.TargetFormat

plugins {
    kotlin("jvm") version "1.9.20"
    id("org.jetbrains.compose") version "1.5.11"
    kotlin("plugin.serialization") version "1.9.20"
}

group = "com.arka"
version = "2.0-SNAPSHOT"

repositories {
    mavenCentral()
    maven("https://maven.pkg.jetbrains.space/public/p/compose/dev")
    google()
}

dependencies {
    // ===== COMPOSE DESKTOP + ICÔNES ÉTENDUES =====
    implementation(compose.desktop.currentOs)
    implementation(compose.material)
    implementation(compose.materialIconsExtended) // 🆕 AJOUTÉ - Toutes les icônes Material Design

    // ===== BASE DE DONNÉES =====
    implementation("org.ktorm:ktorm-core:3.6.0")
    implementation("org.ktorm:ktorm-support-mysql:3.6.0")
    implementation("mysql:mysql-connector-java:8.0.33")

    // ===== INJECTION DE DÉPENDANCES =====
    implementation("io.insert-koin:koin-core:3.4.3")
    implementation("io.insert-koin:koin-compose:1.0.4")

    // ===== SÉCURITÉ =====
    implementation("org.mindrot:jbcrypt:0.4")

    // ===== COROUTINES =====
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.7.3")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-swing:1.7.3")

    // ===== DATE/TIME =====
    implementation("org.jetbrains.kotlinx:kotlinx-datetime:0.4.1")

    // ===== LOGGING =====
    implementation("ch.qos.logback:logback-classic:1.4.11")
    implementation("org.slf4j:slf4j-api:2.0.9")

    // ===== JSON =====
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.0")

    // ===== TESTS =====
    testImplementation(kotlin("test"))
    testImplementation("io.insert-koin:koin-test:3.4.3")
    testImplementation("io.mockk:mockk:1.13.8")
}

compose.desktop {
    application {
        mainClass = "MainKt"

        nativeDistributions {
            targetFormats(TargetFormat.Dmg, TargetFormat.Msi, TargetFormat.Deb)
            packageName = "Arka"
            packageVersion = "2.0.0"

            // Configuration pour les icônes étendues
            modules("java.desktop", "java.sql")
        }
    }
}

java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}

kotlin {
    jvmToolchain(17)
}

// Tâche simple pour vérifier les dépendances
tasks.register("checkDeps") {
    description = "Vérifie que toutes les dépendances sont disponibles"
    doLast {
        println("✅ Toutes les dépendances Arka chargées")
        println("📦 Compose Desktop + Material Icons EXTENDED")
        println("📦 Ktorm + MySQL")
        println("📦 Koin + BCrypt")
        println("📦 Coroutines + DateTime + JSON")
        println("🎨 Material Icons Extended: ~2000+ icônes disponibles")
    }
}