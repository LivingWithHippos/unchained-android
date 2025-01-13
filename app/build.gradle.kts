// Top-level build file where you can add configuration options common to all sub-projects/modules.
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.android) apply false
    alias(libs.plugins.protobuf) apply false
    id("com.mikepenz.aboutlibraries.plugin") version "11.2.3"
    id("com.github.ben-manes.versions") version "0.51.0"
    // id("se.ascp.gradle.gradle-versions-filter") version "0.1.16"
    alias(libs.plugins.ksp) apply false
}

buildscript {
    repositories {
        google()
        mavenCentral()
    }
    dependencies {
        classpath(libs.kotlin.plugin)
        classpath(libs.navigation.plugin)
    }
}