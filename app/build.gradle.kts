plugins {
    id("com.android.application")
}

android {
    namespace = "com.upv.quanttrack"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.upv.quanttrack"
        minSdk = 24
        targetSdk = 34
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
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
}

dependencies {

    implementation("androidx.appcompat:appcompat:1.6.1")
    implementation("com.google.android.material:material:1.11.0")
    implementation("androidx.constraintlayout:constraintlayout:2.1.4")
    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.1.5")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.5.1")
    // 1. Capa de Red y Serialización (Módulo A)
    // Retrofit para peticiones HTTP asíncronas
    implementation("com.squareup.retrofit2:retrofit:2.9.0")
    // Conversor GSON para mapear JSON a objetos Java
    implementation("com.squareup.retrofit2:converter-gson:2.9.0")
    // Interceptor para depurar el tráfico de red en el logcat (opcional pero recomendado)
    implementation("com.squareup.okhttp3:logging-interceptor:4.11.0")

    // 2. Capa de Presentación de Datos (Módulo D)
    // MPAndroidChart para gráficos financieros (Velas y Medias Móviles)
    implementation("com.github.PhilJay:MPAndroidChart:v3.1.0")
    //Para hacer el FPCA
    implementation("org.apache.commons:commons-math3:3.6.1")
}