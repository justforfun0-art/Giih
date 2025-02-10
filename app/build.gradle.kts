plugins {
    alias(libs.plugins.androidApplication)
    alias(libs.plugins.kotlinAndroid)
    alias(libs.plugins.kotlinSerialization)
    alias(libs.plugins.hilt)
    kotlin("kapt")
    id("kotlin-parcelize") // Add this line
    id("com.google.gms.google-services")
    id("com.google.firebase.crashlytics")
}

android {
    namespace = "com.example.gigwork"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.example.gigwork"
        minSdk = 24
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables {
            useSupportLibrary = true

            buildConfigField("String", "SUPABASE_URL", "\"${project.findProperty("SUPABASE_URL") ?: ""}\"")
            buildConfigField("String", "SUPABASE_KEY", "\"${project.findProperty("SUPABASE_KEY") ?: ""}\"")
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
    }

    kotlinOptions {
        jvmTarget = "17"
    }

    buildFeatures {
        compose = true
    }

    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.4"
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

dependencies {

    val roomVersion = "2.5.0"

    implementation("androidx.room:room-runtime:$roomVersion")
    implementation("androidx.room:room-ktx:$roomVersion")
    kapt("androidx.room:room-compiler:$roomVersion")

    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.6.4")
    implementation("androidx.datastore:datastore-preferences:1.0.0")

    implementation("com.google.dagger:hilt-android:2.44")
    kapt("com.google.dagger:hilt-android-compiler:2.44")
    implementation("androidx.security:security-crypto:1.1.0-alpha06")

    implementation("com.google.firebase:firebase-crashlytics-ktx:18.6.0")

    // AndroidX Core
    implementation(libs.core.ktx)
    implementation(libs.lifecycle.runtime.ktx)
    implementation(libs.lifecycle.viewmodel.compose)
    implementation(libs.activity.compose)

    // Compose
    implementation(platform(libs.compose.bom))
    implementation(libs.compose.ui)
    implementation(libs.compose.ui.graphics)
    implementation(libs.compose.ui.tooling.preview)
    implementation(libs.compose.material3)
    implementation(libs.navigation.compose)

    // Hilt
    implementation(libs.hilt.android)
    kapt(libs.hilt.android.compiler)
    implementation(libs.hilt.navigation.compose)


    // Supabase
    implementation(libs.supabase.postgrest)
    implementation(libs.supabase.gotrue)
    implementation(libs.supabase.realtime)
    implementation(libs.ktor.client.android)

    implementation("io.github.jan-tennert.supabase:postgrest-kt:1.4.7")
    implementation("io.github.jan-tennert.supabase:gotrue-kt:1.4.7")
    implementation("io.github.jan-tennert.supabase:storage-kt:1.4.7")
    implementation("io.ktor:ktor-client-android:2.3.7")

    // Room
    implementation(libs.room.runtime)
    kapt(libs.room.compiler)
    implementation(libs.room.ktx)


    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.7.0")
    implementation("androidx.hilt:hilt-navigation-compose:1.1.0")

    // DataStore
    implementation(libs.datastore.preferences)

    implementation("androidx.navigation:navigation-compose:2.7.6")
    implementation("androidx.hilt:hilt-navigation-compose:1.1.0")
    implementation("androidx.compose.material3:material3:1.1.2")

    implementation("androidx.security:security-crypto:1.1.0-alpha06")


    // Image Loading
    implementation(libs.coil.compose)

    // Kotlin Serialization
    implementation(libs.kotlinx.serialization.json)

    // Google Auth
    //implementation(libs.play.services.auth)

    // Testing
    testImplementation(libs.junit)
    //androidTestImplementation(libs.androidx.test.ext.junit)
    androidTestImplementation(libs.espresso.core)
    androidTestImplementation(platform(libs.compose.bom))
    debugImplementation(libs.compose.ui.tooling)

    // Network
    implementation("com.squareup.retrofit2:retrofit:2.9.0")
    implementation("com.squareup.retrofit2:converter-gson:2.9.0")
    implementation("com.squareup.okhttp3:logging-interceptor:4.12.0")

    implementation("com.google.accompanist:accompanist-swiperefresh:0.32.0")

    implementation("androidx.activity:activity-compose:1.7.2")
    implementation("androidx.compose.ui:ui:1.5.4")
    implementation("androidx.compose.ui:ui-tooling-preview:1.5.4")

    val pagingVersion = "3.2.1"

    implementation("androidx.paging:paging-runtime:$pagingVersion")
    implementation("androidx.paging:paging-compose:$pagingVersion")
    implementation("io.coil-kt:coil-compose:2.4.0")  // For image loading


    // Unit Testing
    testImplementation("junit:junit:4.13.2")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.7.1")
    testImplementation("app.cash.turbine:turbine:1.0.0")
    testImplementation("io.mockk:mockk:1.13.5")
    testImplementation("com.google.truth:truth:1.1.5")
    testImplementation("org.robolectric:robolectric:4.10.3")
    testImplementation("androidx.paging:paging-common:3.2.1")

    // Android Testing
    androidTestImplementation("androidx.test.ext:junit:1.1.5")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.5.1")
    androidTestImplementation("androidx.compose.ui:ui-test-junit4:1.5.1")
    androidTestImplementation("com.google.dagger:hilt-android-testing:2.48")
    androidTestImplementation("androidx.paging:paging-testing:3.2.1")
    kaptAndroidTest("com.google.dagger:hilt-android-compiler:2.48")

    // Debug Implementation
    debugImplementation("androidx.compose.ui:ui-test-manifest:1.5.1")

    implementation("androidx.compose.material:material-icons-extended:1.6.7")
}

// Allow references to generated code
kapt {
    correctErrorTypes = true
}

