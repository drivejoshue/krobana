plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)  // Compose compiler (K2) sin composeOptions
}

android {
    namespace = "com.example.orbanadrive"
    compileSdk = 36
    defaultConfig {
        applicationId = "com.example.orbanadrive"
        minSdk = 24
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        debug {
            buildConfigField("String", "API_BASE_URL", "\"https://fe6ee20ecb03.ngrok-free.app/api/\"")
            buildConfigField("String", "REVERB_HOST", "\"fe6ee20ecb03.ngrok-free.app\"")
            buildConfigField("int", "REVERB_PORT", "443")
            buildConfigField("boolean", "REVERB_TLS", "true")
            buildConfigField("String", "REVERB_KEY", "\"localkey\"")
        }
        release {
            isMinifyEnabled = false
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")

            buildConfigField("String", "API_BASE_URL", "\"https://fe6ee20ecb03.ngrok-free.app/api/\"")
            buildConfigField("String", "REVERB_HOST", "\"fe6ee20ecb03.ngrok-free.app\"")
            buildConfigField("int", "REVERB_PORT", "443")
            buildConfigField("boolean", "REVERB_TLS", "true")
            buildConfigField("String", "REVERB_KEY", "\"localkey\"")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
        // coreLibraryDesugaringEnabled = true // sólo si necesitas APIs Java 8+ en minSdk bajo
    }
    kotlinOptions { jvmTarget = "17" }

    buildFeatures {
        compose = true
        buildConfig = true
    }
}

dependencies {
    // Core
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)

    implementation(libs.material)


    // Compose BOM + UI
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material.icons.extended)

    // Activity/Navegación/Lifecycle
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.compose)

    // Red
    implementation(libs.retrofit)
    implementation(libs.retrofit.moshi)
    implementation(libs.okhttp)
    implementation(libs.okhttp.logging)
    implementation(libs.moshi.kotlin)
    implementation(libs.gson)

    // Corrutinas / DataStore / Imágenes
    implementation(libs.coroutines.android)
    implementation(libs.datastore.preferences)
    implementation(libs.coil.compose)

    // Ubicación / Mapas
    implementation(libs.play.services.location)
    implementation(libs.play.services.maps)
    implementation(libs.maps.compose)

    // Realtime y logging
    implementation(libs.pusher)
    implementation(libs.timber)

    // Extras UI
    implementation(libs.androidx.core.splashscreen)
    implementation(libs.androidx.compose.animation)

    // Test
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    implementation(libs.androidx.compose.foundation)
    // Debug
    debugImplementation(libs.androidx.compose.ui.tooling)
    debugImplementation(libs.androidx.compose.ui.test.manifest)

    // Si activas desugaring:
     coreLibraryDesugaring("com.android.tools:desugar_jdk_libs:2.0.4")
}
