plugins {
    kotlin("jvm")
}

group = "de.debugco"
version = "1.0.0"

repositories {
    mavenCentral()
}

dependencies {
    implementation("io.github.froks:dlt-core:0.2.2")
    implementation("org.slf4j:slf4j-api:2.0.12")
    implementation("org.xerial:sqlite-jdbc:3.45.2.0")
    implementation("com.zaxxer:HikariCP:5.1.0")
    api("org.ktorm:ktorm-core:3.6.0")
    api("org.ktorm:ktorm-support-sqlite:3.6.0")

    testImplementation("org.jetbrains.kotlin:kotlin-test")
}

tasks.test {
    useJUnitPlatform()
}
kotlin {
    jvmToolchain(17)
}
