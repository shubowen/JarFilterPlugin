# JarFilterPlugin

[中文文档](README.zh-CN.md)

[![AGP 9 Compile Verification](https://github.com/shubowen/JarFilterPlugin/actions/workflows/agp-9-compile.yml/badge.svg)](https://github.com/shubowen/JarFilterPlugin/actions/workflows/agp-9-compile.yml)

JarFilterPlugin filters files, such as `.class` files, from dependency jars
before Android builds package them into DEX archives. This is useful when you
need to replace a small number of classes from a third-party dependency without
vendoring the whole dependency source tree.

See the [example](example) module for a complete Android application setup. The
plugin id for the `plugins {}` DSL is `io.github.shubowen.jar-filter`.

## Integration

```gradle
pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}
```

Apply and configure the plugin in the module `build.gradle`:

```gradle
plugins {
    id "io.github.shubowen.jar-filter" version "2.5.1"
}

jarFilters {
    "com.android.support:appcompat-v7:(.*)" {
        excludes = [
                'android/support/v7/app/AppCompatActivity.class',
                'android/support/v7/app/AppCompatActivity\\$(.*).class'
        ]
    }

    // Local jar
    "android.local.jars:xxx.jar:(.*)" {
        includes = [
                'xxx'
        ]
    }
}
```

The legacy `jar-filter` plugin id is still available when the plugin is added
through the older `buildscript` classpath style.

## Build verification

This project is verified against Android Gradle Plugin `9.2.1` with a complete Android build environment in GitHub Actions. The workflow provisions JDK 17, the Android SDK, API 36, Build Tools 36.0.0, and then runs:

```bash
./gradlew --version
./gradlew --no-daemon :test:test --stacktrace
./gradlew --no-daemon :example:assembleDebug --stacktrace
```

You can also trigger the same check manually from the `AGP 9 Compile Verification` workflow.

## Publishing to Maven Central

The Maven Central publication is configured for:

```text
io.github.shubowen:JarFilterPlugin:2.5.1
io.github.shubowen.jar-filter:io.github.shubowen.jar-filter.gradle.plugin:2.5.1
```

Before publishing, create and verify the `io.github.shubowen` namespace in the
Central Portal, then put the generated user token and signing key in
`~/.gradle/gradle.properties` or equivalent `ORG_GRADLE_PROJECT_*` environment
variables:

```properties
mavenCentralUsername=...
mavenCentralPassword=...
signingInMemoryKeyFile=/path/to/signing-private.asc
signingInMemoryKeyPassword=...
```

The local publishing configuration used on this machine is stored at:

```text
~/.gradle/gradle.properties
```

Do not commit this file. It contains the Maven Central token, signing password,
and local signing configuration.

Upload to Maven Central without automatically releasing:

```bash
./gradlew :buildSrc:publishToMavenCentral
```

Use an explicit release version when publishing an immutable Maven Central
release:

```bash
./gradlew :buildSrc:publishAndReleaseToMavenCentral -PVERSION_NAME=2.5.1
```
