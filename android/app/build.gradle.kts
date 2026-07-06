import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.ksp)
    alias(libs.plugins.hilt)
}
val localProperties = Properties().apply {
    val file = rootProject.file("local.properties")
    if (file.exists()) file.inputStream().use { load(it) }
}
val web3authClientId: String =
    localProperties.getProperty("WEB3AUTH_CLIENT_ID") ?: "GANTI_DENGAN_CLIENT_ID_ANDA"

android {
    namespace = "com.origintag.app"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.origintag.app"
        minSdk = 26
        targetSdk = 35
        versionCode = 1
        versionName = "0.1.0"

        // Backend via Cloudflare Tunnel (HTTPS publik) — bisa diakses emulator & HP fisik.
        // URL quick-tunnel berubah tiap cloudflared restart; update di sini bila berubah.
        buildConfigField("String", "API_BASE_URL", "\"https://administrative-enrollment-preventing-video.trycloudflare.com/\"")

        // Part 5a — Web3Auth. Client ID dibaca dari local.properties (tidak di-commit).
        buildConfigField("String", "WEB3AUTH_CLIENT_ID", "\"$web3authClientId\"")
        buildConfigField("String", "WEB3AUTH_REDIRECT_URL", "\"com.origintag.app://auth\"")
        // BSC Testnet chainId 97 = 0x61 (BSC Mainnet 56 = 0x38)
        buildConfigField("String", "DEFAULT_CHAIN_ID", "\"0x61\"")
        // RPC untuk menandatangani & broadcast transfer langsung dari app (Part 5d)
        buildConfigField("String", "BSC_RPC_URL", "\"https://data-seed-prebsc-1-s1.bnbchain.org:8545\"")
        buildConfigField("long", "BSC_CHAIN_ID", "97L")

        // Web3Auth menangkap redirect OAuth lewat scheme ini (lihat AndroidManifest)
        manifestPlaceholders["web3authScheme"] = "com.origintag.app"
        manifestPlaceholders["web3authHost"] = "auth"
    }

    packaging {
        resources {
            // web3j/bouncycastle membawa duplikat metadata yang bentrok saat merge
            excludes += setOf(
                "META-INF/DEPENDENCIES",
                "META-INF/LICENSE",
                "META-INF/LICENSE.md",
                "META-INF/NOTICE",
                "META-INF/NOTICE.md",
                "META-INF/*.kotlin_module",
            )
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
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
        buildConfig = true
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.activity.compose)

    // Compose
    implementation(platform(libs.compose.bom))
    implementation(libs.compose.ui)
    implementation(libs.compose.material3)
    implementation(libs.compose.ui.tooling.preview)
    debugImplementation(libs.compose.ui.tooling)
    implementation(libs.navigation.compose)
    implementation(libs.lifecycle.viewmodel.compose)
    implementation(libs.lifecycle.runtime.compose)

    // DI
    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)
    implementation(libs.hilt.navigation.compose)

    // Network (Part 5 — komunikasi ke backend)
    implementation(libs.retrofit)
    implementation(libs.retrofit.kotlinx.serialization)
    implementation(libs.okhttp.logging)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.coil.compose)
    implementation(libs.datastore.preferences)

    // Part 5b — ambil foto barang; Part 5e — scan QR
    implementation(libs.camerax.core)
    implementation(libs.camerax.camera2)
    implementation(libs.camerax.lifecycle)
    implementation(libs.camerax.view)
    implementation(libs.mlkit.barcode)

    // Part 5a — Web3Auth (social login → embedded wallet, FR-01) + web3j untuk
    // menurunkan alamat ETH dari private key. web3j core versi -android khusus.
    implementation(libs.web3auth)
    implementation(libs.web3j.core)

    // TODO(Part 5a lanjutan): WalletConnect v2 untuk MetaMask/Trust Wallet mobile
    // implementation("com.reown:appkit:<latest>")
}
