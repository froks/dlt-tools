import org.jetbrains.compose.desktop.application.dsl.TargetFormat

group = "de.debugco"
version = "1.0.0"

plugins {
    kotlin("jvm")
    id("org.jetbrains.compose")
}

repositories {
    mavenCentral()
    google()
}

dependencies {
    implementation(project(":dlt-core"))
    implementation(compose.desktop.currentOs)
    implementation("com.github.jiconfont:jiconfont-swing:1.0.0")
    implementation("com.github.jiconfont:jiconfont-google_material_design_icons:2.2.0.2")
    testImplementation("org.jetbrains.kotlin:kotlin-test")
}

tasks.named("jar") {
    dependsOn(":dlt-core:jar")
}

tasks.test {
    useJUnitPlatform()
}

kotlin {
    jvmToolchain(17)
}

compose.desktop {
    application {
        mainClass = "dltfilterapp.DltFilterApp"

        nativeDistributions {
            targetFormats(TargetFormat.Dmg, TargetFormat.Msi, TargetFormat.Deb)
            packageName = "dlt-filter"
            packageVersion = version as String
            description = "filter to preprocess AUTOSAR dlt log files"

            windows {
                menu = true
                dirChooser = true
                shortcut = true
                // TODO iconFile
            }
        }
    }
}
