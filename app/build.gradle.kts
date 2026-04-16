// Top-level build file where you can add configuration options common to all sub-projects/modules.
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.android) apply false
    alias(libs.plugins.protobuf) apply false
    alias(libs.plugins.hilt.android) apply false
    alias(libs.plugins.ksp) apply false
    alias(libs.plugins.room) apply false
    id("com.github.ben-manes.versions") version "0.53.0"
}

buildscript {
    repositories {
        google()
        mavenCentral()
    }
    dependencies {
        classpath(libs.kotlin.plugin)
        classpath(libs.navigation.plugin)
        classpath(libs.hilt.plugin)
    }
}

// dependencyUpdates fails in parallel mode with Gradle 9+ (https://github.com/ben-manes/gradle-versions-plugin/issues/968)
tasks.named("dependencyUpdates") {
    doFirst {
        gradle.startParameter.isParallelProjectExecutionEnabled = false
    }
}