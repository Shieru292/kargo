# Kargo: Gradle バージョンカタログ管理ツール

Gradle のバージョンカタログを管理するシンプルなCLIツール。

## インストール

Windows, macOS, Linux用のビルド済みバイナリが利用できます: [Releases](https://github.com/Shieru292/kargo/releases)

## 使用法

### 初期化

はじめに、Kargoのバージョンカタログを作成し、プロジェクトに適用する必要があります。  
組み込みの`init`コマンドを利用できます:
```bash
kargo init

# バージョンカタログへのカスタムパスを設定できます
kargo -c path/to/kargo.versions.toml init
```

次に、以下のコードを `settings.gradle.kts` に追加して、作成されたバージョンカタログを適用します:
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

`kargo add`を使用して、プロジェクトに依存関係を追加します。
```bash
# バージョンを指定する
kargo add io.ktor:ktor-client-core:3.4.2

# @latest キーワードを使うと、最新バージョンを自動で取得して解決します。
kargo add io.ktor:ktor-client-core:@latest

# バージョンが省略されると、kargoはどのバージョンを使用するか尋ねます。
kargo add io.ktor:ktor-client-core

# 単一キーワードを使用すると、Maven Centralを検索します。
kargo add ktor-client-core
```

Kargoはバージョン参照名を提案し、どれを使用するか尋ねます:
```
Select a version reference
❯ ktor
  ktor-client
  ktor-client-core
  - Choose your own
↑ up • ↓ down • enter select
```

全てが明確になると、Kargoはライブラリとバージョン情報をバージョンカタログに書き込み、  
`build.gradle(.kts)`に追加するコードスニペットを提供します。
```
Added io.ktor:ktor-client-core (ref: ktor, version: 3.4.2) to the version catalog.
To use this dependency, add the following snippet to your build.gradle(.kts):

build.gradle (Groovy):
dependencies {
    implementation kargo.ktor.client.core
}

build.gradle.kts (Kotlin):
dependencies {
    implementation(kargo.ktor.client.core)
}
```

`-r` (`--ref`) を使用すると、参照名を自動で設定します。
```bash
kargo add io.ktor:ktor-client-core -r ktor
```

`@latest`と組み合わせると、quiet modeとして動作します。  
ドキュメントを書く際に便利です。
```bash
kargo add io.ktor:ktor-client-core:@latest -r ktor
```

## このプロジェクトはKargoを使用しています！

このプロジェクトは、自身のバージョンをkargoで管理しています。  
`gradle/kargo.versions.toml` をご確認ください！
