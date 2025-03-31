plugins {
    id("com.android.library")
    id("org.jetbrains.kotlin.android")
    `maven-publish`
}

android {
    namespace = "ua.ysmobdev.billing"
    compileSdk = 34

    defaultConfig {
        minSdk = 21
//        targetSdk = 33
    }

    buildTypes {
        release {
//            isMinifyEnabled = false
        }
    }
    kotlinOptions {
        jvmTarget = "17"
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

dependencies {
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.8.0")
    implementation("com.android.billingclient:billing-ktx:7.0.0")
    implementation("org.jetbrains.kotlinx:kotlinx-datetime:0.6.0")
}

publishing {
    publications {
        register<MavenPublication>("release") {
            groupId = "ua.ysmobdev.billing"
            artifactId = "library"
            version = "1.0.24"

            afterEvaluate {
                from(components["release"])
            }
        }
    }
}