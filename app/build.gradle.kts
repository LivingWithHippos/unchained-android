// Top-level build file where you can add configuration options common to all sub-projects/modules.
plugins {
    id("com.android.application") version "8.1.0" apply false
    // 1.9.0 will break stuff, wait for updates
    id("org.jetbrains.kotlin.android") version "1.8.0" apply false
    id("com.mikepenz.aboutlibraries.plugin") version "10.8.3"
    id("com.google.protobuf") version "0.9.4" apply false
    id("com.github.ben-manes.versions") version "0.47.0"
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
