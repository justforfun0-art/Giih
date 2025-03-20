import org.jetbrains.kotlin.commonizer.OptimisticNumberCommonizationEnabledKey.alias
import java.util.Properties
import java.io.FileInputStream

plugins {
    alias(libs.plugins.androidApplication)
    alias(libs.plugins.kotlinAndroid)
    alias(libs.plugins.kotlinSerialization)
    alias(libs.plugins.hilt)
    //id("com.google.devtools.ksp")  // Uncomment this line
    id("kotlin-parcelize")
    id("com.google.gms.google-services")
    id("com.google.firebase.crashlytics")
    id("kotlin-kapt")
    id("org.jetbrains.kotlin.plugin.compose") version "2.0.0"
}

// Load properties file with proper error handling
val properties = Properties().apply {
    val propertiesFile = rootProject.file("keys.properties")
    if (propertiesFile.exists()) {
        load(FileInputStream(propertiesFile))
    } else {
        throw GradleException("keys.properties file not found. Please create it based on keys.properties.template")
    }
}

fun getConfigValue(key: String): String {
    return properties[key]?.toString() ?: throw GradleException("Missing required configuration property: $key")
}

android {
    namespace = "com.example.gigwork"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.example.gigwork"
        minSdk = 24
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "com.example.gigwork.CustomTestRunner"

        buildConfigField("String", "SUPABASE_URL", "\"${getConfigValue("SUPABASE_URL")}\"")
        buildConfigField("String", "SUPABASE_KEY", "\"${getConfigValue("SUPABASE_KEY")}\"")

        vectorDrawables {
            useSupportLibrary = true
        }
    }

    buildTypes {
        debug {
            buildConfigField("Boolean", "DEBUG", "true")
        }
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            buildConfigField("Boolean", "DEBUG", "false")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
        isCoreLibraryDesugaringEnabled = true
    }

    kotlinOptions {
        jvmTarget = "17"
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.8"
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }

    testOptions {
        unitTests {
            isIncludeAndroidResources = true
            isReturnDefaultValues = true
        }
    }

    // Configure sourcesets for KSP
   /* applicationVariants.all {
        kotlin {
            sourceSets {
                getByName(name) {
                    kotlin.srcDir("build/generated/ksp/$name/kotlin")
                }
            }
        }
    }*/
}

dependencies {
    testImplementation(libs.androidx.runner)
    coreLibraryDesugaring("com.android.tools:desugar_jdk_libs:2.1.5")

    // Version Constants
    val roomVersion = "2.6.1"
   // val supabaseVersion = "1.4.7" // Using a more stable version for compatibility
    val ktorVersion = "3.0.0" // Using a version compatible with Supabase 1.4.7
    val pagingVersion = "3.3.6"
    val hiltVersion = "2.48"
    val coroutinesVersion = "1.7.3" // Using a version compatible with Supabase 1.4.7
    val composeBomVersion = "2024.02.00" // Using a stable version
    val navVersion = "2.7.6" // Using a stable version

    // Networking
    implementation("com.squareup.retrofit2:retrofit:2.9.0")
    implementation("com.squareup.retrofit2:converter-gson:2.9.0")
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    implementation("com.squareup.okhttp3:logging-interceptor:4.12.0")

    // Navigation
    implementation("androidx.navigation:navigation-compose:$navVersion")

    implementation("com.google.android.gms:play-services-location:21.0.1")

    // Settings
    implementation("androidx.core:core-ktx:1.12.0")

    // Coil
    implementation("io.coil-kt:coil-compose:2.5.0")

    // Gson
    implementation("com.google.code.gson:gson:2.10.1")

    // Security
    implementation("androidx.security:security-crypto:1.0.0")

    // Compose (using BOM)
    implementation(platform("androidx.compose:compose-bom:$composeBomVersion"))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-core")
    implementation("androidx.compose.material:material-icons-extended")
    implementation("androidx.compose.animation:animation")
    implementation("androidx.activity:activity-compose:1.8.2")
    implementation("androidx.compose.runtime:runtime")

    // Coroutines
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:$coroutinesVersion")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:$coroutinesVersion")

    // Firebase (using BOM)
    implementation(platform("com.google.firebase:firebase-bom:32.7.0"))
    implementation("com.google.firebase:firebase-crashlytics-ktx")
    implementation("com.google.firebase:firebase-analytics-ktx")
    implementation("com.google.firebase:firebase-auth-ktx")
    implementation("androidx.datastore:datastore-preferences:1.0.0")


    // Supabase - using BOM for consistent versioning
    //implementation(platform("io.github.jan-tennert.supabase:bom:3.0.0")) // Update to the latest stable version

   // implementation(platform("io.github.jan-tennert.supabase:bom:2.2.0"))

   // implementation(platform("io.github.jan-tennert.supabase:bom:2.0.3"))


/* Supabase modules
    implementation("io.github.jan-tennert.supabase:postgrest-kt")
    implementation("io.github.jan-tennert.supabase:realtime-kt")
    implementation("io.github.jan-tennert.supabase:auth-kt") // Auth module
    implementation("io.github.jan-tennert.supabase:storage-kt")
*/
   // implementation("io.github.jan-tennert.supabase:auth-kt")

    //implementation("io.github.jan-tennert.supabase-kt:auth-kt:3.0.0") // Replace with the latest version

    implementation(platform(libs.supabase.bom))

    // Supabase modules (no versions needed)
    implementation(libs.supabase.postgrest)
    implementation(libs.supabase.auth)
    implementation(libs.supabase.realtime)
    implementation(libs.supabase.storage)




    // Required for serialization with Supabase
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.3")

    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.7.0")

    // Ensure consistent Kotlin versions
    implementation("org.jetbrains.kotlin:kotlin-stdlib:1.9.22")

    // Ktor
    implementation("io.ktor:ktor-client-android:$ktorVersion")
    implementation("io.ktor:ktor-client-core:$ktorVersion")
    implementation("io.ktor:ktor-client-json:$ktorVersion")
    implementation("io.ktor:ktor-utils:$ktorVersion")
    //implementation("io.ktor:ktor-client-[engine]:KTOR_VERSION")
    implementation(libs.ktor.client.cio)
    implementation(libs.jetbrains.annotations)


    /*   implementation("io.ktor:ktor-serialization-kotlinx-json:$ktorVersion")
   implementation("io.ktor:ktor-client-content-negotiation:$ktorVersion")*/

    // Room
    implementation("androidx.room:room-runtime:$roomVersion")
    implementation("androidx.room:room-ktx:$roomVersion")
   // ksp("androidx.room:room-compiler:$roomVersion")
    implementation("androidx.room:room-common:$roomVersion")
    kapt("androidx.room:room-compiler:$roomVersion") // Add this line


    // Datastore

    // Dagger Hilt
    implementation("com.google.dagger:hilt-android:$hiltVersion")
   // ksp("com.google.dagger:hilt-android-compiler:$hiltVersion")
    kapt("com.google.dagger:hilt-android-compiler:$hiltVersion")  // Change to KAPT


    // Core Dependencies
    implementation(libs.core.ktx)
    implementation(libs.lifecycle.runtime.ktx)
    implementation(libs.lifecycle.viewmodel.compose)
    implementation(libs.navigation.compose)
    implementation(libs.hilt.navigation.compose)


    // Paging
    implementation("androidx.paging:paging-runtime:$pagingVersion")
    implementation("androidx.paging:paging-compose:$pagingVersion")

    // Testing Dependencies
    testImplementation("junit:junit:4.13.2")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:$coroutinesVersion")
    testImplementation("app.cash.turbine:turbine:1.0.0")
    testImplementation("io.mockk:mockk:1.13.8")
    testImplementation("com.google.truth:truth:1.1.5")
    testImplementation("org.robolectric:robolectric:4.11.1")
    testImplementation("androidx.test:core:1.5.0")
    testImplementation("androidx.test.ext:junit:1.1.5")
    testImplementation("com.google.dagger:hilt-android-testing:$hiltVersion")
   // kspTest("com.google.dagger:hilt-android-compiler:$hiltVersion")
    kaptTest("com.google.dagger:hilt-android-compiler:$hiltVersion")  // Change to kaptTest


    // Android Testing
    androidTestImplementation(platform("androidx.compose:compose-bom:$composeBomVersion"))
    androidTestImplementation("androidx.compose.ui:ui-test-junit4")
    androidTestImplementation("androidx.test.ext:junit:1.1.5")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.5.1")
    androidTestImplementation("io.mockk:mockk-android:1.13.8")
    androidTestImplementation("com.google.dagger:hilt-android-testing:$hiltVersion")
    androidTestImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:$coroutinesVersion")
    androidTestImplementation("com.google.truth:truth:1.1.5")
    androidTestImplementation("androidx.paging:paging-testing:$pagingVersion")
   // kspAndroidTest("com.google.dagger:hilt-android-compiler:$hiltVersion")
    kaptAndroidTest("com.google.dagger:hilt-android-compiler:$hiltVersion")  // Change to kaptAndroidTest


    // Debug Implementation
    debugImplementation(platform("androidx.compose:compose-bom:$composeBomVersion"))
    debugImplementation("androidx.compose.ui:ui-tooling")
    debugImplementation("androidx.compose.ui:ui-test-manifest")
}

kapt {
    correctErrorTypes = true
    arguments {
        arg("dagger.fastInit", "enabled")
        arg("dagger.experimentalDaggerErrorMessages", "enabled")
    }
}

/*
ksp {
    // Room configuration
    arg("room.schemaLocation", "$projectDir/schemas")
    arg("room.incremental", "true")
    arg("room.expandProjection", "true")

    // Hilt configuration
    arg("dagger.fastInit", "enabled")
    arg("dagger.experimentalDaggerErrorMessages", "enabled")
}
*/