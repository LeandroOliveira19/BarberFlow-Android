plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.ksp)
}

android {
    namespace = "barberflow.cliente"
    compileSdk {
        version = release(37)
    }

    defaultConfig {
        applicationId = "barberflow.cliente"
        minSdk = 26
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        // Vazio = app genérica (pergunta a barbearia). Um APK próprio de uma barbearia define o slug.
        buildConfigField("String", "SLUG_FIXO", "\"\"")
    }

    // Dois ambientes: XAMPP/php -S local (emulador → 10.0.2.2) e o servidor da UA
    flavorDimensions += "ambiente"
    productFlavors {
        create("local") {
            dimension = "ambiente"
            applicationIdSuffix = ".local"
            versionNameSuffix = "-local"
            buildConfigField("String", "BASE_URL", "\"http://10.0.2.2:8080/projeto/projeto/api/v1/\"")
        }
        // Telemóvel ligado por cabo: "adb reverse tcp:8080 tcp:8080" faz o 127.0.0.1:8080 do telemóvel
        // chegar ao PC pelo cabo USB (sem Wi-Fi nem firewall). Usa o mesmo network_security_config do local.
        create("usb") {
            dimension = "ambiente"
            applicationIdSuffix = ".usb"
            versionNameSuffix = "-usb"
            buildConfigField("String", "BASE_URL", "\"http://127.0.0.1:8080/projeto/projeto/api/v1/\"")
        }
        // APK para partilhar: servidor da UA e já fixo na RL Barber (abre direto, sem perguntar a barbearia)
        create("ua") {
            dimension = "ambiente"
            buildConfigField("String", "SLUG_FIXO", "\"rlbarber\"")
            buildConfigField("String", "BASE_URL", "\"https://esan-tesp-ds-paw.web.ua.pt/tesp-ds-g9/projeto/api/v1/\"")
        }
    }

    buildTypes {
        release {
            optimization {
                enable = false
            }
        }
    }

    buildFeatures {
        viewBinding = true
        buildConfig = true
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.androidx.activity.ktx)
    implementation(libs.androidx.fragment.ktx)
    implementation(libs.androidx.constraintlayout)
    implementation(libs.androidx.swiperefreshlayout)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.navigation.fragment.ktx)
    implementation(libs.androidx.navigation.ui.ktx)
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    ksp(libs.androidx.room.compiler)
    implementation(libs.androidx.work.runtime.ktx)
    implementation(libs.retrofit)
    implementation(libs.retrofit.converter.gson)
    implementation(libs.glide)

    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.test.runner)
    androidTestImplementation(libs.androidx.room.testing)
}
