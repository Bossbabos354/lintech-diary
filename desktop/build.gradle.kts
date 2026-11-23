import org.jetbrains.compose.desktop.application.dsl.TargetFormat
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    kotlin("jvm") version "2.0.21"
    id("org.jetbrains.kotlin.plugin.compose") version "2.0.21"
    id("org.jetbrains.compose") version "1.7.3"
}

group = "ru.school28"
version = "3.0.2"

dependencies {
    implementation(compose.desktop.currentOs)
    implementation(compose.material3)
    implementation(compose.materialIconsExtended)
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    implementation("com.google.code.gson:gson:2.10.1")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-swing:1.9.0")
    implementation("org.apache.pdfbox:pdfbox:2.0.31")

    testImplementation(kotlin("test-junit"))
    testImplementation("junit:junit:4.13.2")
}

tasks.processResources {
    val appVersion = project.version.toString()
    inputs.property("appVersion", appVersion)
    filesMatching("version.properties") {
        expand("version" to appVersion)
    }
}

tasks.test {
    testLogging { events("passed", "failed", "skipped") }
}

// RPM со своим .desktop: человеческое имя в меню и Categories=Education.
// Обычный packageRpm (jpackage) оставляет техническое имя пакета.
tasks.register<Exec>("packageRpmWithMenu") {
    dependsOn("createDistributable")
    val rpmRelease = (findProperty("rpmRelease") as String?) ?: "2"
    commandLine(
        "bash",
        file("packaging/build-rpm.sh").absolutePath,
        project.version.toString(),
        rpmRelease,
    )
}

kotlin {
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_17)
    }
}

java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}

compose.desktop {
    application {
        mainClass = "ru.school28.diary.MainKt"

        nativeDistributions {
            targetFormats(TargetFormat.Deb, TargetFormat.Rpm, TargetFormat.Msi)
            packageName = "lintech28-diary"
            packageVersion = project.version.toString()
            description = "Lintech 28 diary"
            vendor = "Linteh28"
            copyright = "(c) 2026 Linteh28"

            linux {
                installationPath = "/usr/lib"
                menuGroup = "Образование"
                appCategory = "Education"
                debMaintainer = "diary@school28-kirov.ru"
            }
            windows {
                menuGroup = "Linteh28"
                shortcut = true
                dirChooser = true
            }
        }
    }
}
