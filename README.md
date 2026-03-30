# Kargo: Gradle Version Catalog Manager

A simple CLI tool for managing Gradle Version Catalogs.

## Installation

(WIP)

## Usage

### Initialize

First, you need to create a Version Catalog for Kargo and apply it to your project.
You can use the built-in init command:
```bash
kargo init

# You can also specify a custom path for the version catalog
kargo -c path/to/kargo.versions.toml init
```

Then, add the following to your `settings.gradle.kts` to apply the created version catalog:
```kotlin
dependencyResolutionManagement {
    versionCatalogs {
        create("kargo") {
            from(files("kargo.versions.toml"))
        }
    }
}
```

### Add a dependency

Use `kargo add` to add dependencies to your project.
```bash
# Specify a version explicitly
kargo add io.ktor:ktor-client-core:3.4.2

# Use the @latest keyword to automatically resolve the latest version
kargo add io.ktor:ktor-client-core:@latest

# If the version is omitted, kargo will ask which version to use
kargo add io.ktor:ktor-client-core

# You can also search Maven Central with a single keyword
kargo add ktor-client-core
```

Kargo will suggest a version reference name and ask which one to use:
```
Select a version reference
❯ ktor
  ktor-client
  ktor-client-core
  - Choose your own
↑ up • ↓ down • enter select
```

Once everything is resolved, kargo adds the library and version information to the
version catalog and provides a snippet to paste into your `build.gradle(.kts)`:
```
Added io.ktor:ktor-client-core (ref: ktor, version: 3.4.2) to the version catalog.
To use this dependency, add the following snippet to your build.gradle(.kts):

build.gradle (Groovy):
dependencies {
    implementation "kargo.ktor.client.core"
}

build.gradle.kts (Kotlin):
dependencies {
    implementation(kargo.ktor.client.core)
}
```

You can also use the `-r` (`--ref`) option to set the reference name automatically:
```bash
kargo add io.ktor:ktor-client-core -r ktor
```

Combined with `@latest`, this works great as a quiet mode — useful for documentation:
```bash
kargo add io.ktor:ktor-client-core:@latest -r ktor
```

## This project uses Kargo

This project manages its own dependencies with Kargo.
See `gradle/kargo.versions.toml`!
