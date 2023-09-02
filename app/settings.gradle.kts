pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
    // https://docs.gradle.org/current/userguide/platforms.html#sub:central-declaration-of-dependencies
    versionCatalogs {
        create("libs") {
            version("aboutlibraries", "10.8.3")
            version("android_gradle_plugin", "8.1.0")
            version("annotations", "1.3.0")
            version("appcompat", "1.6.1")
            version("atsl_core", "1.3.0")
            version("cardview", "1.0.0")
            version("constraint_layout", "2.1.4")
            version("coil", "2.4.0")
            version("core_ktx", "1.10.1")
            version("coroutines", "1.7.3")
            version("countly", "23.6.0")
            version("dagger", "2.48")
            version("espresso", "3.5.1")
            version("datastore", "1.0.0")
            version("fast_adapter", "5.7.0")
            version("flexbox", "3.0.0")
            version("fragment", "1.6.1")
            version("hilt_navigation", "1.0.0")
            version("jsoup", "1.16.1")
            version("junit", "4.13.2")
            version("kotlin", "1.8.21")
            version("ktlint", "11.0.0")
            version("lifecycle", "2.6.1")
            version("lottie", "5.2.0")
            version("material_2", "1.8.0")
            version("material_3", "1.9.0")
            version("modernstorage", "1.0.0-alpha06")
            version("moshi", "1.15.0")
            // 2.7.0 requires sdk 34
            version("navigation", "2.6.0")
            version("okhttp", "4.11.0")
            version("okio", "3.2.0")
            version("paging", "3.2.0")
            version("protobuf", "3.24.0")
            version("protobuf_plugin", "0.9.4")
            version("preference", "1.2.1")
            version("recyclerview", "1.3.1")
            version("recyclerview_selection", "1.1.0")
            version("retrofit", "2.9.0")
            version("room", "2.5.2")
            version("statemachine", "0.2.0")
            version("swiperefresh", "1.1.0")
            version("test", "1.5.0")
            version("test_junit", "1.1.5")
            version("test_orchestrator", "1.4.2")
            version("timber", "5.0.1")
            version("viewpager2", "1.0.0")
            version("work", "2.8.1")

            library("protobuf_core", "com.google.protobuf", "protoc").versionRef("protobuf")
            library("protobuf_javaLite", "com.google.protobuf", "protobuf-javalite").versionRef("protobuf")
            library("protobuf_kotlinLite", "com.google.protobuf", "protobuf-kotlin-lite").versionRef("protobuf")
            library("protobuf_plugin", "com.google.protobuf", "protobuf-gradle-plugin").versionRef("protobuf_plugin")

            library("about_plugin", "com.mikepenz.aboutlibraries.plugin", "aboutlibraries-plugin").versionRef("aboutlibraries")
            library("about_core", "com.mikepenz", "aboutlibraries-core").versionRef("aboutlibraries")
            library("about_ui", "com.mikepenz", "aboutlibraries").versionRef("aboutlibraries")

            library("android_gradle_plugin", "com.android.tools.build", "gradle").versionRef("android_gradle_plugin")
            library("annotations", "androidx.annotation", "annotation").versionRef("annotations")
            library("android_appcompat", "androidx.appcompat", "appcompat").versionRef("appcompat")
            library("cardview", "androidx.cardview", "cardview").versionRef("cardview")
            library("coil", "io.coil-kt", "coil").versionRef("coil")
            library("android_constraintlayout", "androidx.constraintlayout", "constraintlayout").versionRef("constraint_layout")
            library("core_ktx", "androidx.core", "core-ktx").versionRef("core_ktx")
            library("coroutines_core", "org.jetbrains.kotlinx", "kotlinx-coroutines-core").versionRef("coroutines")
            library("coroutines_android", "org.jetbrains.kotlinx", "kotlinx-coroutines-android").versionRef("coroutines")
            library("coroutines_test", "org.jetbrains.kotlinx", "kotlinx-coroutines-test").versionRef("coroutines")
            library("countly", "ly.count.android", "sdk").versionRef("countly")
            library("hilt_android", "com.google.dagger", "hilt-android").versionRef("dagger")
            library("hilt_compiler", "com.google.dagger", "hilt-android-compiler").versionRef("dagger")
            library("hilt_plugin", "com.google.dagger", "hilt-android-gradle-plugin").versionRef("dagger")
            library("hilt_navigation", "androidx.hilt", "hilt-navigation-fragment").versionRef("hilt_navigation")
            library("hilt_work", "androidx.hilt", "hilt-work").versionRef("dagger")

            library("fragment_runtime", "androidx.fragment", "fragment").versionRef("fragment")
            library("fragment_ktx", "androidx.fragment", "fragment-ktx").versionRef("fragment")

            library("datastore_prefs", "androidx.datastore", "datastore-preferences").versionRef("datastore")
            library("datastore_core", "androidx.datastore", "datastore").versionRef("datastore")
            library("espresso_core", "androidx.test.espresso", "espresso-core").versionRef("espresso")
            library("espresso_contrib", "androidx.test.espresso", "espresso-contrib").versionRef("espresso")
            library("espresso_intents", "androidx.test.espresso", "espresso-intents").versionRef("espresso")
            library("flexbox", "com.google.android.flexbox", "flexbox").versionRef("flexbox")
            library("jsoup", "org.jsoup", "jsoup").versionRef("jsoup")
            library("junit", "junit", "junit").versionRef("junit")
            library("kotlin_stdlib", "org.jetbrains.kotlin", "kotlin-stdlib").versionRef("kotlin")
            library("kotlin_test", "org.jetbrains.kotlin", "kotlin-test-junit").versionRef("kotlin")
            library("kotlin_plugin", "org.jetbrains.kotlin", "kotlin-gradle-plugin").versionRef("kotlin")
            library("kotlin_reflect", "org.jetbrains.kotlin", "kotlin-reflect").versionRef("kotlin")
            library("lifecycle_service", "androidx.lifecycle", "lifecycle-service").versionRef("lifecycle")
            library("lifecycle_runtime", "androidx.lifecycle", "lifecycle-runtime").versionRef("lifecycle")
            library("lifecycle_java8", "androidx.lifecycle", "lifecycle-common-java8").versionRef("lifecycle")
            library("lifecycle_compiler", "androidx.lifecycle", "lifecycle-compiler").versionRef("lifecycle")
            library("lifecycle_savedstate", "androidx.lifecycle", "lifecycle-viewmodel-savedstate").versionRef("lifecycle")
            library("lifecycle_viewmodel", "androidx.lifecycle", "lifecycle-viewmodel-ktx").versionRef("lifecycle")
            library("lifecycle_livedata", "androidx.lifecycle", "lifecycle-livedata-ktx").versionRef("lifecycle")
            library("lottie", "com.airbnb.android", "lottie").versionRef("lottie")
            library("lottie_compose", "com.airbnb.android", "lottie-compose").versionRef("lottie")
            library("material_version3", "com.google.android.material", "material").versionRef("material_3")
            library("modernstorage", "com.google.modernstorage", "modernstorage-storage").versionRef("modernstorage")
            library("modernstorage_permissions", "com.google.modernstorage", "modernstorage-permissions").versionRef("modernstorage")
            library("moshi_runtime", "com.squareup.moshi", "moshi-kotlin").versionRef("moshi")
            library("moshi_kapt", "com.squareup.moshi", "moshi-kotlin-codegen").versionRef("moshi")
            library("navigation_runtime", "androidx.navigation", "navigation-runtime-ktx").versionRef("navigation")
            library("navigation_fragment", "androidx.navigation", "navigation-fragment-ktx").versionRef("navigation")
            library("navigation_testing", "androidx.navigation", "navigation-testing").versionRef("navigation")
            library("navigation_ui", "androidx.navigation", "navigation-ui").versionRef("navigation")
            library("navigation_ui_ktx", "androidx.navigation", "navigation-ui-ktx").versionRef("navigation")
            library("navigation_plugin", "androidx.navigation", "navigation-safe-args-gradle-plugin").versionRef("navigation")
            library("okhttp3_runtime", "com.squareup.okhttp3", "okhttp").versionRef("okhttp")
            library("okhttp3_doh", "com.squareup.okhttp3", "okhttp-dnsoverhttps").versionRef("okhttp")
            library("okhttp3_logging", "com.squareup.okhttp3", "logging-interceptor").versionRef("okhttp")
            library("okhttp3_okio", "com.squareup.okio", "okio").versionRef("okio")
            library("paging_runtime", "androidx.paging", "paging-runtime").versionRef("paging")
            library("preference_ktx", "androidx.preference", "preference-ktx").versionRef("preference")
            library("recyclerview_core", "androidx.recyclerview", "recyclerview").versionRef("recyclerview")
            library("recyclerview_selection", "androidx.recyclerview", "recyclerview-selection").versionRef("recyclerview_selection")
            library("retrofit_runtime", "com.squareup.retrofit2", "retrofit").versionRef("retrofit")
            library("retrofit_gson", "com.squareup.retrofit2", "converter-gson").versionRef("retrofit")
            library("retrofit_moshi", "com.squareup.retrofit2", "converter-moshi").versionRef("retrofit")
            library("retrofit_mock", "com.squareup.retrofit2", "retrofit-mock").versionRef("retrofit")
            library("retrofit_scalars", "com.squareup.retrofit2", "converter-scalars").versionRef("retrofit")
            library("room_runtime", "androidx.room", "room-runtime").versionRef("room")
            library("room_compiler", "androidx.room", "room-compiler").versionRef("room")
            library("room_ktx", "androidx.room", "room-ktx").versionRef("room")
            library("rxjava2", "androidx.room", "room-rxjava2").versionRef("room")
            library("testing", "androidx.room", "room-testing").versionRef("room")
            library("statemachine", "com.tinder.statemachine", "statemachine").versionRef("statemachine")
            library("swiperefreshlayout", "androidx.swiperefreshlayout", "swiperefreshlayout").versionRef("swiperefresh")
            library("test_espresso", "androidx.test.espresso", "espresso-core").versionRef("espresso")
            library("test_core", "androidx.test", "core-ktx").versionRef("test")
            library("test_orchestrator", "androidx.test", "orchestrator").versionRef("test_orchestrator")
            library("test_runner", "androidx.test", "runner").versionRef("test")
            library("test_rules", "androidx.test", "rules").versionRef("test")
            library("test_junit", "androidx.test.ext", "junit").versionRef("test_junit")
            library("test_truth", "androidx.test.ext", "truth").versionRef("test")
            library("timber", "com.jakewharton.timber", "timber").versionRef("timber")
            library("viewpager2", "androidx.viewpager2", "viewpager2").versionRef("viewpager2")
            library("android_work", "androidx.work", "work-runtime-ktx").versionRef("work")

        }
    }
}

rootProject.name = "Unchained"
include(":app")
 