plugins {
    kotlin("jvm")
    kotlin("plugin.serialization")
    application
    id("com.github.johnrengelman.shadow") version "8.1.1"
}

application {
    mainClass.set("de.debugco.dltmcp.MainKt")
}

dependencies {
    implementation("io.github.froks:dlt-core:0.4.3")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.7.1")
    implementation("org.slf4j:slf4j-api:2.0.12")
    implementation("ch.qos.logback:logback-classic:1.5.32")
    implementation("io.modelcontextprotocol:kotlin-sdk-server:0.11.1")
    testImplementation("org.jetbrains.kotlin:kotlin-test")
}

tasks.test {
    useJUnitPlatform()
}

tasks.shadowJar {
    archiveBaseName.set("dlt-mcp")
    archiveClassifier.set("")
    archiveVersion.set("")
    mergeServiceFiles()
}

kotlin {
    jvmToolchain(21)
}
