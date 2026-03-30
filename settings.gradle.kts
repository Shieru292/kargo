plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}
rootProject.name = "kargo"

dependencyResolutionManagement {
    versionCatalogs {
        create("kargo") {
            from(files("gradle/kargo.versions.toml"))
        }
    }
}
