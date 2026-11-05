plugins {
    id("com.android.application")
}

android {
    namespace = "xyz.i4o.uni.launcher"
    compileSdk = 36
    buildFeatures {
        buildConfig = true
        viewBinding = true
    }

    packaging {
        jniLibs { useLegacyPackaging = true }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    defaultConfig {
        applicationId = "xyz.i4o.uni.launcher"
        minSdk = 24
        targetSdk = 28
        versionCode = 55
        versionName = "0.8.0-native"
    }

    lint {
        disable += "ExpiredTargetSdkVersion"
    }

    sourceSets.getByName("main") {
        java.srcDirs("src/main/java")
        assets.srcDir(layout.buildDirectory.dir("generated/officialCatalog").get().asFile)
    }

    signingConfigs {
        create("platform") {
            val keyFile = file("../../../../tools/platform.p12")
            if (keyFile.exists()) {
                storeFile = keyFile
                storePassword = "android"
                keyAlias = "platform"
                keyPassword = "android"
                storeType = "PKCS12"
            }
        }
    }

    buildTypes {
        debug {
            signingConfig = signingConfigs.getByName("platform")
        }
        release {
            signingConfig = signingConfigs.getByName("platform")
        }
    }
}


dependencies {
    implementation("androidx.appcompat:appcompat:1.6.1")
    implementation("androidx.core:core-ktx:1.12.0")
    implementation("androidx.viewpager2:viewpager2:1.1.0")
    implementation("androidx.recyclerview:recyclerview:1.3.2")
    implementation("androidx.constraintlayout:constraintlayout:2.1.4")
    implementation("com.google.android.material:material:1.11.0")
    implementation("androidx.lifecycle:lifecycle-viewmodel-ktx:2.7.0")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.7.0")
    implementation("com.google.zxing:core:3.5.3")

    testImplementation("junit:junit:4.13.2")
    testImplementation("org.json:json:20240303")
}

val syncOfficialCatalog by tasks.registering(Copy::class) {
    from("../../../analysis/official-widgets.json")
    into(layout.buildDirectory.dir("generated/officialCatalog"))
}
tasks.named("preBuild") { dependsOn(syncOfficialCatalog) }
