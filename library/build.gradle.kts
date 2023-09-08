plugins {
    id("com.android.library")
    id("org.jetbrains.kotlin.android")
    `maven-publish`
}

android {
    namespace = "ua.ysmobdev.billing"
    compileSdk = 33

    defaultConfig {
        minSdk = 21
        targetSdk = 33
    }

    buildTypes {
        release {
            isMinifyEnabled = false
        }
    }
    kotlinOptions {
        jvmTarget = "1.8"
    }
}

dependencies {
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.7.1")
    implementation("com.android.billingclient:billing-ktx:6.0.0")
    implementation("org.jetbrains.kotlinx:kotlinx-datetime:0.4.0")
}

publishing {
    publications {
        register<MavenPublication>("release") {
            groupId = "ua.ysmobdev.billing"
            artifactId = "library"
            version = "1.0.14"

            afterEvaluate {
                from(components["release"])
            }
        }
    }
}