# Kargo: Gradle version catalog manager

Gradle バージョンカタログを管理するシンプルなCLIツール。

## インストール

(WIP)

## 使い方

### 初期化

まず、Kargo用のバージョンカタログを作成し、プロジェクトに適用する必要があります。
組み込みの初期化コマンドを使用できます。

```bash
kargo init

# または、バージョンカタログへのパスを指定できます
kargo -c path/to/kargo.versions.toml init
```

次に、`settings.gradle.kts` に、作成したバージョンカタログを適用する設定を適用します。

```kotlin
dependencyResolutionManagement {
    versionCatalogs {
        create("kargo") {
            from(files("kargo.versions.toml"))
        }
    }
}
```

### 依存関係を追加する

`kargo add` を使用して、プロジェクトに依存関係を追加します。

```bash
# 明示的にバージョンを指定
kargo add io.ktor:ktor-client-core:3.2.0

# @latest キーワードを使用すると、パッケージの最新バージョンを自動で解決します。
kargo add io.ktor:ktor-client-core:@latest

# バージョンが省略されると、kargoはどのバージョンを使用するか尋ねます。
kargo add io.ktor:ktor-client-core

# 単一の検索キーワードを使用して、Maven Centralを検索することもできます。
kargo add ktor-client-core
```

kargoはバージョンの参照名を自動で提案し、どの参照名を使用するか尋ねます。

```
Select a version reference
❯ ktor
  ktor-client
  ktor-client-core
  - Choose your own
↑ up • ↓ down • enter select
```

全てが明確になると、kargoはバージョンカタログにライブラリとバージョンの情報を記載し、`build.gradle(.kts)`に適用できるコード スニペットを提供します。

```
Added io.ktor:ktor-client-core (ref: ktor, version: 3.2.0) to the version catalog.
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

`-r` (`--ref`) 引数を使用して、参照名を自動で適用することもできます:
`kargo add io.ktor:ktor-client-core -r ktor`

`@latest`と組み合わせることで、Quiet Modeのような形で動作することもできます:
`kargo add io.ktor:ktor-client-core:@latest -r ktor`

## Kargoを使用しているプロジェクト

まさに、このプロジェクトが使用しています！`gradle/kargo.versions.toml` を参照してください！
