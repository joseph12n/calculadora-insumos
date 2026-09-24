plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.ksp)
    alias(libs.plugins.room)
}

android {
    namespace = "com.bioplast.insumos"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.bioplast.insumos"
        // LocalDate/WeekFields (java.time) requieren API 26+ sin desugaring
        minSdk = 26
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    signingConfigs {
        create("release") {
            val ks = rootProject.layout.projectDirectory.file("keystore/release.jks").asFile
            if (ks.exists()) {
                storeFile = ks
                storePassword = providers.gradleProperty("KEYSTORE_PASSWORD").orNull
                keyAlias = providers.gradleProperty("KEY_ALIAS").orNull
                keyPassword = providers.gradleProperty("KEY_PASSWORD").orNull
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            signingConfig = signingConfigs.getByName("release").takeIf { it.storeFile != null }
                ?: signingConfigs.getByName("debug")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }

    kotlinOptions {
        jvmTarget = "11"
    }

    buildFeatures {
        compose = true
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

room {
    schemaDirectory("$projectDir/schemas")
}

dependencies {
    implementation(platform(libs.compose.bom))
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)

    // Room
    implementation(libs.room.runtime)
    implementation(libs.room.ktx)
    ksp(libs.room.compiler)

    // Cámara / OCR
    implementation(libs.camerax.core)
    implementation(libs.camerax.camera2)
    implementation(libs.camerax.lifecycle)
    implementation(libs.camerax.view)
    implementation(libs.mlkit.text.recognition)

    implementation(libs.kotlinx.coroutines.android)

    testImplementation(libs.junit)
    // Nota: sin accessor de catálogo para kotlinx-coroutines-test: el alias "-test"
    // choca con la extensión `test` de Kotlin DSL (no se genera el miembro).
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.9.0")
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.compose.bom))
    androidTestImplementation(libs.androidx.ui.test.junit4)
    androidTestImplementation(libs.kotlinx.coroutines.android)
    androidTestImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.9.0")
    // WORKAROUND (solo debug): coroutines-test dentro del APK de la app para que el
    // service file META-INF/services/kotlinx.coroutines.CoroutineExceptionHandler del
    // APK de la app incluya kotlinx.coroutines.test.internal.ExceptionCollectorAsService.
    // En instrumentación, CoroutineExceptionHandler se resuelve en el classloader de la
    // APP (parent-first) y solo ve los services del APK de la app; sin esta línea,
    // TestScopeImpl.enter falla con "Exception handler was not found via a ServiceLoader".
    // No llega a release: debugImplementation solo empaqueta en builds debug.
    debugImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.9.0")
    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)
}
