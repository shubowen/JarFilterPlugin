# JarFilterPlugin

[English](README.md)

[![AGP 9 Compile Verification](https://github.com/shubowen/JarFilterPlugin/actions/workflows/agp-9-compile.yml/badge.svg)](https://github.com/shubowen/JarFilterPlugin/actions/workflows/agp-9-compile.yml)

JarFilterPlugin 可以在 Android 构建把依赖 jar 打进 DEX 之前，从 jar 中过滤指定文件，例如 `.class` 文件。

这个插件适合这样的场景：你只想替换第三方依赖中的少量 class，不想把整个依赖源码拉进项目。做法是把需要替换的源码复制到当前项目中，再用 JarFilterPlugin 从原始 jar 里移除对应 class，最后构建产物会使用你项目里重新编译出来的 class。

完整用法可以参考 [example](example) 模块。`plugins {}` DSL 使用的插件 id 是 `io.github.shubowen.jar-filter`。

## 集成方式

```gradle
pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}
```

在模块的 `build.gradle` 中应用并配置插件：

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

    // 本地 jar
    "android.local.jars:xxx.jar:(.*)" {
        includes = [
                'xxx'
        ]
    }
}
```

如果仍使用旧的 `buildscript` classpath 方式引入插件，原来的 `jar-filter` 插件 id 仍然可用。

## 构建验证

当前项目使用 Android Gradle Plugin `9.2.1` 做构建验证。GitHub Actions 会准备 JDK 17、Android SDK、API 36、Build Tools 36.0.0，然后执行：

```bash
./gradlew --version
./gradlew --no-daemon :test:test --stacktrace
./gradlew --no-daemon :example:assembleDebug --stacktrace
```

也可以在 GitHub Actions 中手动触发 `AGP 9 Compile Verification` workflow。

## 发布到 Maven Central

Maven Central 发布坐标配置为：

```text
io.github.shubowen:JarFilterPlugin:2.5.1
io.github.shubowen.jar-filter:io.github.shubowen.jar-filter.gradle.plugin:2.5.1
```

发布前需要先在 Central Portal 创建并验证 `io.github.shubowen` namespace，然后把 Central user token 和签名配置放到 `~/.gradle/gradle.properties`，或使用等价的 `ORG_GRADLE_PROJECT_*` 环境变量：

```properties
mavenCentralUsername=...
mavenCentralPassword=...
signingInMemoryKeyFile=/path/to/signing-private.asc
signingInMemoryKeyPassword=...
```

这台机器上的本地发布配置位置是：

```text
~/.gradle/gradle.properties
```

不要提交这个文件。它包含 Maven Central token、签名密码和本地签名配置。

上传到 Maven Central 但不自动 release：

```bash
./gradlew :buildSrc:publishToMavenCentral
```

发布不可变的 Maven Central release 时显式指定 release 版本：

```bash
./gradlew :buildSrc:publishAndReleaseToMavenCentral -PVERSION_NAME=2.5.1
```
