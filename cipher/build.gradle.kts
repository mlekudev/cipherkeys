import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.agp.library)
}

kotlin {
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_11)
    }
}

android {
    namespace = "org.cipherkeys.cipher"
    compileSdk = project.properties["projectCompileSdk"].toString().toInt()

    defaultConfig {
        minSdk = project.properties["projectMinSdk"].toString().toInt()
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
}

dependencies {
    api(libs.pgpainless.core)
    implementation(libs.androidx.security.crypto)
}
