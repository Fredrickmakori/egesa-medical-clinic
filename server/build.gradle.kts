plugins {
    kotlin("jvm")
    id("io.ktor.plugin")
}

group = "com.egesa"
version = "0.1.0"

application {
    mainClass.set("com.egesa.clinic.server.ServerKt")
}

dependencies {
    implementation(project(":shared"))
    implementation("io.ktor:ktor-server-core-jvm:2.3.12")
    implementation("io.ktor:ktor-server-netty-jvm:2.3.12")
    implementation("io.ktor:ktor-server-content-negotiation-jvm:2.3.12")
    implementation("io.ktor:ktor-serialization-kotlinx-json-jvm:2.3.12")
    implementation("ch.qos.logback:logback-classic:1.5.8")
}
