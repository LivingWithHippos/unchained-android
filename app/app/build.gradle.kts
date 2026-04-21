import java.util.Properties

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    alias(libs.plugins.kotlin.serialization)
    id("org.jetbrains.kotlin.plugin.parcelize")
    id("com.google.dagger.hilt.android")
    id("androidx.navigation.safeargs.kotlin")
    alias(libs.plugins.protobuf)
    alias(libs.plugins.ktfmt)
    alias(libs.plugins.ksp)
    alias(libs.plugins.room)
}

fun readProperties(propertiesFile: File) =
    Properties().apply {
        if (propertiesFile.exists()) propertiesFile.inputStream().use { fis -> load(fis) }
    }

val keyPropertiesFile: File = rootProject.file("signingkey.properties")
val keyProperties = readProperties(keyPropertiesFile)

val apiPropertiesFile: File = rootProject.file("apikey.properties")
val apiProperties = readProperties(apiPropertiesFile)

protobuf {
    protoc { artifact = libs.protobuf.core.get().toString() }
    plugins {
        generateProtoTasks { all().forEach { it.builtins { create("java") { option("lite") } } } }
    }
}

ktfmt {
    // KotlinLang style - 4 space indentation - From kotlinlang.org/docs/coding-conventions.html
    kotlinLangStyle()
}

kotlin { jvmToolchain(11) }

android {
    namespace = "com.github.livingwithhippos.unchained"
    compileSdk = 37

    defaultConfig {
        applicationId = "com.github.livingwithhippos.unchained"
        minSdk = 27
        targetSdk = 37
        versionCode = 59
        versionName = "1.7.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    room { schemaDirectory("$projectDir/schemas") }

    packaging {
        jniLibs { excludes.addAll(listOf("META-INF/proguard/*")) }
        resources {
            excludes.addAll(
                listOf(
                    "META-INF/*.version",
                    // manually added, Markdown files should not be needed
                    // was crashing with the jakarta XML bind api
                    "META-INF/*.md",
                    "META-INF/proguard/*",
                    "/*.properties",
                    "fabric/*.properties",
                    "META-INF/*.properties",
                )
            )
        }
    }
    signingConfigs {
        // use local file if available or Environment variables (for CI)
        create("release") {
            if (keyPropertiesFile.exists()) {
                storeFile = file(keyProperties["store"] as String? ?: "release.pfk")
                storePassword = keyProperties["releaseStorePassword"] as String
                keyAlias = keyProperties["keyAlias"] as String
                keyPassword = keyProperties["releaseStorePassword"] as String
            } else {
                storeFile = file(System.getenv("KEYSTORE") ?: "release.pfk")
                storePassword = System.getenv("KEYSTORE_PASSWORD")
                keyAlias = System.getenv("KEY_ALIAS")
                keyPassword = System.getenv("KEY_PASSWORD")
            }
        }
    }

    buildTypes {
        applicationVariants.forEach { variant ->
            variant.outputs
                .map { it as com.android.build.gradle.internal.api.BaseVariantOutputImpl }
                .forEach { it.outputFileName = "${variant.name}-${variant.versionName}.apk" }
        }

        debug {
            versionNameSuffix = "-dev"
            applicationIdSuffix = ".debug"
            signingConfig = signingConfigs.getByName("debug")

            buildConfigField(
                "String",
                "COUNTLY_APP_KEY",
                apiProperties.getOrDefault(
                    "COUNTLY_APP_KEY",
                    "\"" +
                        (System.getenv("COUNTLY_APP_KEY")
                            ?: "pDJz4WrY9XeBotXAaL9MYrraSwZNyDqfAPy8p38c") +
                        "\"",
                ) as String,
            )

            buildConfigField(
                "String",
                "COUNTLY_URL",
                apiProperties.getOrDefault(
                    "COUNTLY_URL",
                    "\"" + (System.getenv("COUNTLY_URL") ?: "http://localhost") + "\"",
                ) as String,
            )
        }

        release {
            signingConfig = signingConfigs.getByName("release")
            isDebuggable = false
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    buildFeatures {
        viewBinding = true
        buildConfig = true
    }

    testOptions { unitTests { isIncludeAndroidResources = true } }
}

dependencies {
    implementation(libs.kotlin.stdlib)
    implementation(libs.kotlin.reflect)
    implementation(libs.kotlin.datetime)

    implementation(libs.core.ktx)
    implementation(libs.android.appcompat)

    implementation(libs.android.constraintlayout)
    implementation(libs.fragment.ktx)

    implementation(libs.swiperefresh.layout)
    implementation(libs.preference.ktx)

    implementation(libs.recyclerview.core)
    implementation(libs.recyclerview.selection)
    implementation(libs.viewpager2)
    implementation(libs.flexbox)

    implementation(libs.datastore.core)
    implementation(libs.datastore.prefs)

    implementation(libs.documentfile)

    ksp(libs.moshi.codegen)
    implementation(libs.moshi.runtime)

    implementation(libs.retrofit.runtime)
    implementation(libs.retrofit.moshi)
    implementation(libs.retrofit.scalars)

    implementation(libs.okhttp.runtime)
    implementation(libs.okhttp.logging)
    implementation(libs.okhttp.doh)

    implementation(libs.navigation.runtime)
    implementation(libs.navigation.fragment)
    implementation(libs.navigation.ui)

    ksp(libs.room.compiler)
    implementation(libs.room.runtime)
    implementation(libs.room.ktx)

    implementation(libs.coroutines.core)
    implementation(libs.coroutines.android)
    implementation(libs.kotlin.serialization.json)

    implementation(libs.material.version3)

    implementation(libs.lifecycle.viewmodel)
    implementation(libs.lifecycle.savedstate)
    implementation(libs.lifecycle.livedata)
    implementation(libs.lifecycle.service)
    implementation(libs.lifecycle.java8)

    implementation(libs.coil)

    ksp(libs.hilt.compiler)
    implementation(libs.hilt.navigation)
    implementation(libs.hilt.android)

    implementation(libs.paging.runtime)

    implementation(libs.statemachine)

    implementation(libs.timber)

    implementation(libs.jsoup)

    implementation(libs.android.work)

    implementation(libs.countly)

    implementation(libs.protobuf.javaLite)

    androidTestImplementation(libs.test.core)
    androidTestImplementation(libs.test.espresso)
    androidTestImplementation(libs.test.runner)
    androidTestImplementation(libs.test.rules)
    androidTestImplementation(libs.test.junit)
    androidTestImplementation(libs.test.truth)
    testImplementation(libs.test.core)
    testImplementation(libs.junit)
    testImplementation(libs.robolectric)
}
