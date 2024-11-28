plugins {
    kotlin("jvm")
    kotlin("plugin.serialization") version "1.9.21" apply false
}

allprojects {
    group = "de.debugco"
    version = "1.5.0"
}

repositories {
    mavenCentral()
}

tasks.test {
    useJUnitPlatform()
}

kotlin {
    jvmToolchain(17)
}
