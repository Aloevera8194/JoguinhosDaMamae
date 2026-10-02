plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "com.oliveira.joguinhosdamamae"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.oliveira.joguinhosdamamae"
        minSdk = 23
        targetSdk = 35
        versionCode = 2
        versionName = "0.1"
    }

    signingConfigs {
        create("release") {
            storeFile = file("keystore/joguinhos-release.keystore")
            storePassword = System.getenv("JOGUINHOS_KEYSTORE_PASSWORD")
            keyAlias = "joguinhos"
            keyPassword = System.getenv("JOGUINHOS_KEYSTORE_PASSWORD")
        }
    }

    buildTypes {
        release {
            signingConfig = signingConfigs.getByName("release")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }
}
