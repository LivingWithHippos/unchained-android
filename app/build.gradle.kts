// Top-level build file where you can add configuration options common to all sub-projects/modules.
plugins {
    id("com.android.application") version "8.2.0" apply false
    id("org.jetbrains.kotlin.android") version "1.9.20" apply false
    id("com.mikepenz.aboutlibraries.plugin") version "10.9.2"
    id("com.google.protobuf") version "0.9.4" apply false
    id("com.github.ben-manes.versions") version "0.50.0"
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