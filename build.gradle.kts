import org.jetbrains.compose.desktop.application.dsl.TargetFormat

plugins {
    kotlin("jvm")
    id("org.jetbrains.compose")
}

group = "org.nexus.studios"
version = "0.0.3-pre"

repositories {
    mavenCentral()
    maven("https://maven.pkg.jetbrains.space/public/p/compose/dev")
    google()
}

dependencies {
    // Note, if you develop a library, you should use compose.desktop.common.
    // compose.desktop.currentOs should be used in launcher-sourceSet
    // (in a separate module for demo project and in testMain).
    // With compose.desktop.common you will also lose @Preview functionality
    implementation(compose.desktop.currentOs)

    testImplementation("org.junit.jupiter:junit-jupiter-api:5.7.0")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:5.7.0")
}

tasks.test {
    useJUnitPlatform()
}

compose.desktop {
    application {
        mainClass = "MainKt"

        nativeDistributions {
            targetFormats(TargetFormat.Dmg, TargetFormat.Msi, TargetFormat.Deb)
            packageName = "zenith-chess"
            packageVersion = "1.0.0"
        }
    }
}

val gameName = "Zenith Chess"
val gameTitle = "$gameName - v$version"
tasks.register("generateVersionFile") {
    doLast {
        val fileDir = file("$projectDir/src/main/kotlin/api")
        val fileName = "GameDetails.kt"
        fileDir.mkdirs()
        val file = fileDir.resolve(fileName)
        file.writeText("""
            package api

            object GameDetails {
                const val version = "${project.version}"
                const val name = "$gameName"
                const val title = "$gameTitle"
            }
        """.trimIndent())
    }
}

tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile>().configureEach {
    dependsOn("generateVersionFile")
}
