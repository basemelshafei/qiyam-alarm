plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
}

android {
    namespace = "com.basem.qiyam"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.basem.qiyam"
        minSdk = 26
        targetSdk = 34
        versionCode = (System.getenv("GITHUB_RUN_NUMBER") ?: "1").toInt()
        versionName = "1.0." + (System.getenv("GITHUB_RUN_NUMBER") ?: "0")
    }

    // One fixed key so every new build installs as an update over the old one.
    signingConfigs {
        create("qiyam") {
            storeFile = file("qiyam.keystore")
            storePassword = "qiyam-night"
            keyAlias = "qiyam"
            keyPassword = "qiyam-night"
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            signingConfig = signingConfigs.getByName("qiyam")
        }
        debug {
            signingConfig = signingConfigs.getByName("qiyam")
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
    testOptions {
        unitTests.isReturnDefaultValues = true
        unitTests.isIncludeAndroidResources = true
        unitTests.all { it.maxHeapSize = "3g" }
    }
}

dependencies {
    implementation("com.batoulapps.adhan:adhan:1.2.1")

    implementation("androidx.core:core-ktx:1.13.1")
    implementation("androidx.activity:activity-compose:1.9.2")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.6")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.8.6")

    implementation(platform("androidx.compose:compose-bom:2024.09.00"))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.foundation:foundation")
    implementation("androidx.compose.material3:material3")

    testImplementation("junit:junit:4.13.2")
    // Screen tests that run on a simulated Android in CI (no phone needed).
    testImplementation("org.robolectric:robolectric:4.13")
    testImplementation("androidx.test.ext:junit:1.2.1")
    testImplementation("androidx.compose.ui:ui-test-junit4")
    debugImplementation("androidx.compose.ui:ui-test-manifest")
}

// List every test by name in the build log, so a green build visibly means "these ran and passed".
tasks.withType<Test>().configureEach {
    testLogging { events("passed", "skipped", "failed") }
}
