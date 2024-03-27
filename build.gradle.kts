plugins {
    kotlin("jvm")
}

allprojects {
    group = "de.debugco"
    version = "1.0.0"
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
