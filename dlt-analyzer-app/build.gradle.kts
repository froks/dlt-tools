import org.jetbrains.compose.desktop.application.dsl.TargetFormat

plugins {
    kotlin("jvm")
    id("org.jetbrains.compose")
}

repositories {
    mavenCentral()
    google()
    maven("https://packages.jetbrains.team/maven/p/kpm/public/")
}

dependencies {
    implementation(project(":dlt-core"))
    implementation(project(":dlt-database"))

    implementation("org.xerial:sqlite-jdbc:3.45.2.0")
    implementation("org.apache.commons:commons-lang3:3.14.0")
    implementation("org.oxbow:swingbits:1.3.0")

    implementation(compose.desktop.currentOs)
    implementation(compose.materialIconsExtended)
    implementation(compose.material3)

    implementation("com.formdev:flatlaf:3.4.1")

    implementation("com.github.jiconfont:jiconfont-swing:1.0.0")
    implementation("com.github.jiconfont:jiconfont-google_material_design_icons:2.2.0.2")

    implementation("ch.qos.logback:logback-classic:1.5.3")

    testImplementation("org.jetbrains.kotlin:kotlin-test")
}

tasks.test {
    useJUnitPlatform()
}

kotlin {
    jvmToolchain(17)
}

tasks.named("jar") {
    dependsOn(":dlt-core:jar", ":dlt-database:jar")
}

compose.desktop {
    application {
        mainClass = "analyzer.DltAnalyzerApp"

        nativeDistributions {
            targetFormats(TargetFormat.Dmg, TargetFormat.Msi, TargetFormat.Deb)
            packageName = "dlt-analyzer"
            packageVersion = version as String
            description = "AUTOSAR dlt log files analyzer"

            modules("java.base", "java.desktop", "java.logging", "jdk.crypto.ec", "java.naming", "java.sql")

            windows {
                menu = true
                dirChooser = true
                shortcut = true
                // TODO iconFile
            }
        }
    }
}
