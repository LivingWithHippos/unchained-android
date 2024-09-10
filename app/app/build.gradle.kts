import java.util.Properties


plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.parcelize")
    id("dagger.hilt.android.plugin")
    id("androidx.navigation.safeargs.kotlin")
    id("com.mikepenz.aboutlibraries.plugin")
    alias(libs.plugins.protobuf)
    alias(libs.plugins.ktfmt)
    kotlin("kapt")
}

fun readProperties(propertiesFile: File) = Properties().apply {
    if (propertiesFile.exists())
        propertiesFile.inputStream().use { fis ->
            load(fis)
        }
}

val keyPropertiesFile: File = rootProject.file("signingkey.properties")
val keyProperties = readProperties(keyPropertiesFile)

val apiPropertiesFile: File = rootProject.file("apikey.properties")
val apiProperties = readProperties(apiPropertiesFile)

protobuf {
    protoc {
        artifact = libs.protobuf.core.get().toString()
    }
    plugins {
        generateProtoTasks {
            all().forEach {
                it.builtins {
                    create("java") {
                        option("lite")
                    }
                }
            }
        }
    }
}

ktfmt {
    // KotlinLang style - 4 space indentation - From kotlinlang.org/docs/coding-conventions.html
    kotlinLangStyle()
}

kapt {
    correctErrorTypes = true
}

android {
    namespace = "com.github.livingwithhippos.unchained"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.github.livingwithhippos.unchained"
        minSdk = 22
        targetSdk = 34
        versionCode = 46
        versionName = "1.3.2"

        javaCompileOptions {
            annotationProcessorOptions {
                arguments["room.schemaLocation"] = "$projectDir/schemas"
                arguments["room.incremental"] = "true"
            }
        }

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    packaging {
        jniLibs {
            excludes.addAll(listOf("META-INF/proguard/*"))
        }
        resources {
            excludes.addAll(
                listOf(
                    "META-INF/*.version",
                    // manually added, markdown files should not be needed
                    // was crashing with the jakarta xml bind api
                    "META-INF/*.md",
                    "META-INF/proguard/*",
                    "/*.properties",
                    "fabric/*.properties",
                    "META-INF/*.properties"
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
            variant.outputs.map {
                it as com.android.build.gradle.internal.api.BaseVariantOutputImpl
            }.forEach {
                it.outputFileName = "${variant.name}-${variant.versionName}.apk"
            }
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
                                ?: "pDJz4WrY9XeBotXAaL9MYrraSwZNyDqfAPy8p38c")
                            + "\""
                ) as String
            )

            buildConfigField(
                "String",
                "COUNTLY_URL",
                apiProperties.getOrDefault(
                    "COUNTLY_URL",
                    "\"" + (System.getenv("COUNTLY_URL") ?: "http://localhost") + "\""
                ) as String
            )

        }


        release {
            ndk.debugSymbolLevel = "FULL"
            signingConfig = signingConfigs.getByName("release")
            isDebuggable = false
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }
    buildFeatures {
        dataBinding = true
        buildConfig = true
    }
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

    implementation(libs.jackson.kotlin)
    implementation(libs.jackson.xml)
    implementation(libs.woodstox)
    // replaced legacy jaxb with jakarta
    // https://github.com/FasterXML/jackson-modules-base
    // implementation(libs.stax)
    implementation(libs.jakarta.xmlapi)

    kapt(libs.moshi.kapt)
    implementation(libs.moshi.runtime)

    implementation(libs.retrofit.runtime)
    implementation(libs.retrofit.moshi)
    implementation(libs.retrofit.scalars)

    implementation(libs.okhttp3.runtime)
    implementation(libs.okhttp3.logging)
    implementation(libs.okhttp3.doh)

    implementation(libs.navigation.runtime)
    implementation(libs.navigation.fragment)
    implementation(libs.navigation.ui)

    kapt(libs.room.compiler)
    implementation(libs.room.runtime)
    implementation(libs.room.ktx)

    implementation(libs.coroutines.core)
    implementation(libs.coroutines.android)

    implementation(libs.material.version3)

    implementation(libs.lifecycle.viewmodel)
    implementation(libs.lifecycle.savedstate)
    implementation(libs.lifecycle.livedata)
    implementation(libs.lifecycle.service)
    implementation(libs.lifecycle.java8)

    implementation(libs.coil)

    kapt(libs.hilt.compiler)
    implementation(libs.hilt.navigation)
    implementation(libs.hilt.android)

    implementation(libs.paging.runtime)

    implementation(libs.statemachine)

    implementation(libs.timber)

    implementation(libs.jsoup)

    implementation(libs.android.work)

    implementation(libs.countly)

    implementation(libs.protobuf.javaLite)

    implementation(libs.about.core)
    implementation(libs.about.ui)

    androidTestImplementation(libs.test.core)
    androidTestImplementation(libs.test.espresso)
    androidTestImplementation(libs.test.runner)
    androidTestImplementation(libs.test.rules)
    androidTestImplementation(libs.test.junit)
    androidTestImplementation(libs.test.truth)
    testImplementation(libs.junit)
}