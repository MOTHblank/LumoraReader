plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.hilt.android)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.ksp)
}

android {
    namespace = "com.mothblank.lumorareader"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.mothblank.lumorareader"
        minSdk = 33
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
        isCoreLibraryDesugaringEnabled = true
    }
    kotlin {
        compilerOptions {
            freeCompilerArgs.add("-opt-in=kotlin.time.ExperimentalTime")
            jvmToolchain(21)}
        buildFeatures {
            compose = true
            buildConfig = true
        }
    }
    ksp {
        arg("room.schemaDirectory", "$projectDir/schemas")
    }

    dependencies {
        implementation(libs.androidx.appcompat)
        implementation(libs.material)
        implementation(libs.compose.material)
        implementation(libs.androidx.core.ktx)
        implementation(libs.androidx.lifecycle.runtime.ktx)
        implementation(libs.androidx.lifecycle.runtime.compose)
        implementation(libs.androidx.activity.compose)
        implementation(libs.androidx.activity.compose)
        implementation(platform(libs.androidx.compose.bom))
        implementation(libs.coil.compose)
        implementation(libs.androidx.ui)
        implementation(libs.androidx.ui.graphics)
        implementation(libs.androidx.webkit)
        implementation(libs.androidx.ui.tooling.preview)
        implementation(libs.androidx.material3)
        implementation(libs.androidx.material.icons.extended)
        implementation(libs.androidx.constraintlayout.compose)

        // Hilt
        implementation(libs.hilt.android)
        // Room
        implementation(libs.room.runtime)
        implementation(libs.room.ktx)
        ksp(libs.hilt.compiler)
        ksp(libs.room.compiler)
        ksp(libs.hilt.androidx.compiler)
        // Glance
        implementation(libs.glance.appwidget)
        implementation(libs.hilt.work)

        // Coroutines & WorkManager
        implementation(libs.kotlinx.coroutines.android)
        implementation(libs.work.runtime.ktx)

        // Testing
        testImplementation(libs.junit)
        androidTestImplementation(libs.androidx.junit)
        androidTestImplementation(libs.androidx.espresso.core)
        androidTestImplementation(platform(libs.androidx.compose.bom))
        androidTestImplementation(libs.androidx.ui.test.junit4)
        debugImplementation(libs.androidx.ui.tooling)
        debugImplementation(libs.androidx.ui.test.manifest)

        // Networking
        implementation(libs.jackson.core)
        implementation(libs.jackson.annotations)
        implementation(libs.jackson.databind)
        implementation(libs.eventbus)
        implementation(libs.retrofit)
        implementation(libs.okhttp)
        implementation(libs.converter.gson)
        implementation(libs.retrofit2.converter.jackson)


        // Android X
        implementation(libs.androidx.navigation.compose)
        implementation(libs.androidx.datastore.preferences)
        implementation(libs.hilt.navigation.compose)
        implementation(libs.glance.appwidget)
        implementation(libs.androidx.constraintlayout)
        implementation("androidx.activity:activity-ktx:1.10.1")
        implementation("androidx.fragment:fragment-ktx:1.8.8")

        // Timber for logging
        implementation(libs.timber)

        // Kotlin Reflect
        implementation(libs.kotlin.reflect)

        // Core library desugaring
        coreLibraryDesugaring(libs.desugar.jdk.libs)
    }}