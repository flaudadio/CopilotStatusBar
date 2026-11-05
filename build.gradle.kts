plugins {
    java
    id("org.jetbrains.intellij.platform") version "2.19.0"
}

group = "com.flaudadio"
version = "0.5.0"

repositories {
    mavenCentral()
    intellijPlatform {
        defaultRepositories()
    }
}

dependencies {
    intellijPlatform {
        rider("2025.3") {
            useInstaller = false
        }
    }
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(21))
    }
}

tasks.withType<JavaCompile>().configureEach {
    options.release.set(17)
}

tasks.named("buildSearchableOptions") {
    enabled = false
}

intellijPlatform {
    pluginConfiguration {
        ideaVersion {
            sinceBuild.set("253")
        }
        changeNotes.set("Adds English and Spanish language selection, configurable refresh settings, and preserves the last valid value.")
    }
}
