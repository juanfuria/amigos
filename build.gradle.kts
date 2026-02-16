import org.jetbrains.compose.desktop.application.dsl.TargetFormat

plugins {
    kotlin("jvm") version "2.1.21"
    id("org.jetbrains.compose") version "1.7.3"
    id("org.jetbrains.kotlin.plugin.compose") version "2.1.21"
    id("app.cash.sqldelight") version "2.0.2"
}

group = "com.efetepe.amigos"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
    maven("https://maven.pkg.jetbrains.space/public/p/compose/dev")
    google()
}

dependencies {
    implementation(compose.desktop.currentOs)
    implementation(compose.material3)

    // SQLDelight
    implementation("app.cash.sqldelight:sqlite-driver:2.0.2")

    // Coroutines
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.9.0")

    // DateTime
    implementation("org.jetbrains.kotlinx:kotlinx-datetime:0.6.1")

    // Testing
    testImplementation(kotlin("test"))
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.9.0")
}

compose.desktop {
    application {
        mainClass = "com.efetepe.amigos.MainKt"
        nativeDistributions {
            targetFormats(TargetFormat.Dmg)
            packageName = "Amigos"
            packageVersion = "1.0.0"
            modules("java.sql")
            macOS {
                bundleID = "com.efetepe.amigos"
                dockName = "Amigos"
                iconFile.set(project.file("src/main/packaging/icon.icns"))
            }
        }
    }
}

sqldelight {
    databases {
        create("AmigosDatabase") {
            packageName.set("com.efetepe.amigos.data")
        }
    }
}

tasks.withType<Test> {
    useJUnitPlatform()
}
