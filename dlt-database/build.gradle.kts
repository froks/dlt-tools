plugins {
    kotlin("jvm")
}

group = "de.debugco"
version = "1.0.0"

repositories {
    mavenCentral()
}

dependencies {
    api(project(":dlt-core"))
    implementation("org.slf4j:slf4j-api:2.0.12")
    implementation("com.zaxxer:HikariCP:5.1.0")

    testImplementation("org.jetbrains.kotlin:kotlin-test")
}

tasks.test {
    useJUnitPlatform()
}
kotlin {
    jvmToolchain(17)
}