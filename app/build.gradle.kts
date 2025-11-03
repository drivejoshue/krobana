plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
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
            buildConfigField(
                "String",
                "API_BASE_URL",
                "\"https://fe6ee20ecb03.ngrok-free.app/api/\"" )// <-- termina con '/'
            buildConfigField("String",  "REVERB_HOST", "\"fe6ee20ecb03.ngrok-free.app\"")
            buildConfigField("int",     "REVERB_PORT", "443")       // 6001/8080 en local; 443 en https
            buildConfigField("boolean", "REVERB_TLS",  "true")      // true si usas https
            buildConfigField("String",  "REVERB_KEY",  "\"localkey\"")

        }
        release {
            buildConfigField(
                "String",
                "API_BASE_URL",
                "\"https://fe6ee20ecb03.ngrok-free.app/api/\""
            )
            buildConfigField("String",  "REVERB_HOST", "\"fe6ee20ecb03.ngrok-free.app\"")
            buildConfigField("int",     "REVERB_PORT", "443")       // 6001/8080 en local; 443 en https
            buildConfigField("boolean", "REVERB_TLS",  "true")      // true si usas https
            buildConfigField("String",  "REVERB_KEY",  "\"localkey\"")

            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    // Kotlin/Java 17 recomendado
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions { jvmTarget = "17"
        freeCompilerArgs += listOf(
            "-opt-in=androidx.compose.material3.ExperimentalMaterial3Api"
        )
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }
    // OJO: con el plugin kotlin.compose ya no necesitas composeOptions
}

dependencies {
    // Core + ciclo de vida
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)

    // Compose BOM + UI base
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)

    // Navegación y ViewModel en Compose
    implementation(libs.androidx.navigation.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)

    // Red (Retrofit/OkHttp/Moshi)
    implementation(libs.retrofit)
    implementation(libs.retrofit.moshi)
    implementation(libs.okhttp)
    implementation(libs.okhttp.logging)
    implementation(libs.moshi.kotlin)

    // Coroutines / DataStore / imágenes
    implementation(libs.coroutines.android)
    implementation(libs.datastore.preferences)
    implementation(libs.coil.compose)

    // Ubicación (Maps Compose lo activamos cuando metas mapa)
    implementation(libs.play.services.location)
    // implementation(libs.maps.compose)

    // Realtime y logging
    implementation(libs.pusher)
    implementation(libs.timber)

    // Tests
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)

    // Debug tools
    debugImplementation(libs.androidx.compose.ui.tooling)
    debugImplementation(libs.androidx.compose.ui.test.manifest)

    implementation("androidx.core:core-splashscreen:1.0.1")
    implementation("com.google.android.material:material:1.12.0")
    implementation("androidx.appcompat:appcompat:1.7.0")
    implementation("androidx.navigation:navigation-compose:2.8.3")
    implementation(libs.moshi.kotlin)
    implementation("androidx.compose.material:material-icons-extended")
    implementation("com.google.code.gson:gson:2.10.1")

    implementation("com.google.android.gms:play-services-maps:18.2.0")
    implementation("com.google.maps.android:maps-compose:4.3.3")
}

