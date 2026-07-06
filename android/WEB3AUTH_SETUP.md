# Setup Web3Auth (Part 5a)

Login social (Google/Email → embedded wallet tanpa seed phrase) memakai **Web3Auth PnP Android SDK v10**. Kode integrasinya sudah lengkap; kamu hanya perlu mengisi Client ID dan mendaftarkan redirect URI.

## 1. Buat project di dashboard Web3Auth

1. Buka https://dashboard.web3auth.io → login → **Create Project**.
2. Pilih environment **Sapphire Devnet** (untuk dev/hackathon; ganti ke Sapphire Mainnet saat produksi).
3. Platform: **Android**.
4. Salin **Client ID**.

## 2. Daftarkan redirect URI

Di halaman project → **Whitelist / Redirect URIs**, tambahkan persis:

```
com.origintag.app://auth
```

Ini harus sama dengan `WEB3AUTH_REDIRECT_URL` di `app/build.gradle.kts` dan intent-filter di `AndroidManifest.xml` (scheme `com.origintag.app`, host `auth`).

## 3. Isi Client ID

Di [app/build.gradle.kts](app/build.gradle.kts), ganti placeholder:

```kotlin
buildConfigField("String", "WEB3AUTH_CLIENT_ID", "\"CLIENT_ID_DARI_DASHBOARD\"")
```

> Untuk produksi, jangan hardcode — pindahkan ke `local.properties` / secret dan baca lewat `buildConfigField` agar tidak ikut ter-commit.

Network di [MainActivity.kt](app/src/main/java/com/origintag/app/MainActivity.kt) saat ini `Web3AuthNetwork.SAPPHIRE_DEVNET` — samakan dengan environment project.

## 4. Jalankan

Tanpa Client ID valid, tombol "Masuk dengan Google/Email" akan gagal saat login — gunakan tombol **"Lewati (mode demo)"** di layar onboarding untuk mencoba alur registrasi/dashboard dengan wallet demo (`0x00...01`).

Setelah Client ID diisi:
- **Masuk dengan Google/Email** → CustomTab OAuth → redirect ke `com.origintag.app://auth` → app menurunkan alamat EVM dari private key (via web3j) → disimpan di DataStore (`WalletManager`).
- Alamat itu otomatis dipakai sebagai `ownerAddress` saat registrasi dan sebagai query dashboard "Barang Saya".

## Alur teknis

```
OnboardingScreen ──(LocalWeb3Auth)──▶ web3Auth.connectTo(LoginParams(AuthConnection.GOOGLE))
                                              │ CompletableFuture<Web3AuthResponse>
                                              ▼
                              web3Auth.getPrivateKey()  (secp256k1 hex)
                                              │
                      WalletManager.addressFromPrivateKey()  (web3j Credentials)
                                              │
                          WalletManager.saveSession(address)  (DataStore)
                                              │
                    Dashboard / Register membaca WalletManager.address
```

Web3Auth dimiliki `MainActivity` (butuh Activity context + CustomTab; disimpan di singleton akan me-leak Activity) dan disediakan ke Compose lewat `LocalWeb3Auth`. Restore sesi otomatis di `MainActivity.onCreate` lewat `web3Auth.initialize()`.
