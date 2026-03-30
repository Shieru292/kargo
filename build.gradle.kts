plugins {
    application
    kotlin("jvm") version "2.3.10"
    kotlin("plugin.serialization") version "2.3.10"
    id("org.graalvm.buildtools.native") version "1.0.0"
}

group = "net.shieru"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

application {
    mainClass.set("net.shieru.kargo.MainKt")
    applicationDefaultJvmArgs = listOf("--enable-native-access=ALL-UNNAMED")
}

tasks.named<JavaExec>("run") {
    standardInput = System.`in`
}

dependencies {
    implementation(kargo.slf4j.nop)
    implementation(kargo.mordant)
    implementation(kargo.ktor.client.core)
    implementation(kargo.ktor.client.java)
    implementation(kargo.clikt)
    implementation(kargo.ktoml.core)
    implementation(kargo.ktoml.file)
    implementation(kargo.kotlinx.serialization.json)
    testImplementation(kotlin("test"))
}

kotlin {
    jvmToolchain {
        languageVersion.set(JavaLanguageVersion.of(25))
        vendor.set(JvmVendorSpec.GRAAL_VM)
    }
}

graalvmNative {
    binaries {
        named("main") {
            javaLauncher.set(javaToolchains.launcherFor {
                languageVersion.set(JavaLanguageVersion.of(25))
                vendor.set(JvmVendorSpec.GRAAL_VM)
            })
        }
    }
}

tasks.test {
    useJUnitPlatform()
}
