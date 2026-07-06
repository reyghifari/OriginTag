# OriginTag — Checklist Deployment End-to-End

Urutan persis untuk membawa OriginTag dari kode ke demo berjalan di perangkat fisik. Ikuti dari atas ke bawah — beberapa langkah menghasilkan nilai (alamat kontrak, alamat wallet) yang dibutuhkan langkah berikutnya.

Waktu perkiraan: 45–90 menit (mayoritas menunggu faucet & sync Greenfield).

---

## Prasyarat: siapkan 3 wallet + dana testnet

Kamu butuh **3 wallet terpisah** (boleh pakai MetaMask, buat 3 account). Jangan pakai wallet berisi dana asli.

| Wallet | Peran | Butuh dana | Dipakai di |
|---|---|---|---|
| **Deployer** | Admin kontrak (`DEFAULT_ADMIN_ROLE`) | tBNB (gas deploy) | `contracts/.env` → `PRIVATE_KEY` |
| **Backend Authenticator** | Mint passport (`AUTHENTICATOR_ROLE`) | tBNB (gas mint) | `backend/.env` → `AUTHENTICATOR_PRIVATE_KEY` |
| **Greenfield** | Upload bukti ke Greenfield | tBNB Greenfield | `backend/.env` → `GREENFIELD_PRIVATE_KEY` |

Ditambah **wallet user** (dibuat otomatis oleh Web3Auth saat login di app) yang butuh sedikit tBNB untuk gas transfer — lihat catatan "Gas untuk user" di bawah.

**Ambil dana:**
- tBNB BSC Testnet: https://www.bnbchain.org/en/testnet-faucet — masukkan alamat Deployer, Backend Authenticator, dan (nanti) wallet user.
- tBNB Greenfield: transfer tBNB BSC → Greenfield lewat https://testnet.dcellar.io (login wallet Greenfield, deposit/cross-transfer), atau faucet Greenfield.

**Catat 3 alamat + 3 private key** di tempat aman sementara. Jangan commit.

---

## Fase A — Deploy smart contract (Part 1)

```bash
cd contracts
npm install
cp .env.example .env
```

Isi `contracts/.env`:
```
PRIVATE_KEY=<private key wallet DEPLOYER>
BSCSCAN_API_KEY=<opsional, dari bscscan.com/myapikey untuk verify>
BACKEND_AUTHENTICATOR_ADDRESS=<ALAMAT wallet Backend Authenticator>
```

> `BACKEND_AUTHENTICATOR_ADDRESS` harus diisi **sebelum** deploy — skrip deploy otomatis memberi `AUTHENTICATOR_ROLE` ke alamat ini. Kalau kosong, kamu harus `grantRole` manual belakangan.

Jalankan:
```bash
npm test                 # pastikan 6/6 test hijau dulu
npm run deploy:testnet
```

Output akan mencetak **`OriginTagPassport deployed: 0x....`** dan **`AUTHENTICATOR_ROLE diberikan ke: 0x...`**.

➡️ **Catat contract address ini** — dipakai di Fase B.

(Opsional) verify di BSCScan: `npm run verify:testnet <contract-address>`.

---

## Fase B — Backend (Part 2, 3, 4, 6)

```bash
cd ../backend
npm install
cp .env.example .env
```

Isi `backend/.env`:
```
PORT=3210                          # BUKAN 3000 — port itu dipakai proses lain di mesin ini
PUBLIC_VERIFY_BASE_URL=http://<IP-LAN-MESIN-DEV>:3210

# On-chain (Fase A)
BSC_RPC_URL=https://data-seed-prebsc-1-s1.bnbchain.org:8545
CONTRACT_ADDRESS=<contract address dari Fase A>
AUTHENTICATOR_PRIVATE_KEY=<private key wallet Backend Authenticator>

# AI (Part 3)
ANTHROPIC_API_KEY=<key dari console.anthropic.com>
AI_MODEL=claude-opus-4-8
AUTH_SCORE_THRESHOLD=70

# Greenfield (Part 4)
GREENFIELD_RPC_URL=https://gnfd-testnet-fullnode-tendermint-us.bnbchain.org
GREENFIELD_CHAIN_ID=5600
GREENFIELD_BUCKET=<nama bucket unik global, mis. origintag-evidence-<random>>
GREENFIELD_PRIVATE_KEY=<private key wallet Greenfield>
```

Catatan:
- **`<IP-LAN-MESIN-DEV>`**: IP mesin dev di jaringan yang sama dengan HP (mis. `192.168.1.10`). Cari dengan `ipconfig getifaddr en0` (macOS). `localhost`/`10.0.2.2` tidak bisa diakses dari HP fisik.
- **`GREENFIELD_BUCKET`** harus unik secara global — kalau `origintag-evidence` sudah dipakai orang lain, `createBucket` akan gagal. Tambah suffix acak.
- Biarkan salah satu dari `CONTRACT_ADDRESS` / `AUTHENTICATOR_PRIVATE_KEY` kosong = backend jalan mock mode untuk bagian on-chain (berguna untuk tes cepat tanpa deploy).

Jalankan:
```bash
npm run build
npm run start   # atau start:dev untuk hot reload
```

Cek log startup — seharusnya **tidak** ada baris "MOCK MODE" untuk service yang sudah diisi kredensialnya. Uji cepat dari mesin dev:
```bash
curl http://localhost:3210/verify/0    # 404-style "tidak ditemukan" itu normal (belum ada passport)
```

---

## Fase C — Android app (Part 5)

### C1. Web3Auth Client ID
Ikuti [android/WEB3AUTH_SETUP.md](android/WEB3AUTH_SETUP.md):
1. Buat project di https://dashboard.web3auth.io (network **Sapphire Devnet**).
2. Whitelist redirect URI: `com.origintag.app://auth`.
3. Salin Client ID.

### C2. Isi konfigurasi di `android/app/build.gradle.kts`
```kotlin
buildConfigField("String", "WEB3AUTH_CLIENT_ID", "\"<CLIENT_ID>\"")
// Arahkan ke backend di IP LAN (bukan 10.0.2.2 kalau pakai HP fisik):
buildConfigField("String", "API_BASE_URL", "\"http://<IP-LAN-MESIN-DEV>:3210/\"")
```
Pastikan `MainActivity` memakai `Web3AuthNetwork.SAPPHIRE_DEVNET` (sudah default) sesuai network project. `BSC_RPC_URL` & `BSC_CHAIN_ID` (97) di build.gradle sudah benar untuk testnet.

### C3. Build & install ke device
```bash
cd android
./gradlew installDebug     # HP terhubung via USB, USB debugging aktif
# atau: ./gradlew assembleDebug lalu adb install app/build/outputs/apk/debug/app-debug.apk
```

---

## Fase D — Uji alur end-to-end di device

1. **Login** → "Masuk dengan Google/Email" → selesai OAuth → app menurunkan alamat wallet. (Catat alamatnya, kirim sedikit tBNB ke situ untuk langkah transfer.)
2. **Registrasi** → isi brand/kategori, pilih 1–5 foto barang, "Verifikasi & Mint" → AI menilai → jika skor ≥ 70, passport ter-mint. Cek tokenId & tx di BSCScan Testnet.
3. **Dashboard** → passport baru muncul di "Barang Saya".
4. **Verifikasi publik** → buka `http://<IP-LAN>:3210/verify/<tokenId>` di browser HP lain → status keaslian tampil tanpa login.
5. **Transfer** → buka detail passport → "Transfer Kepemilikan" → tempel alamat wallet buyer → kirim (ditandatangani wallet user, broadcast ke BSC). Cek kepemilikan berpindah.
6. **Scan QR** → dari dashboard "Scan QR Barang" → arahkan ke QR `origintag.app/verify/{tokenId}` → app membuka detail passport.

---

## Catatan & jebakan

- **Gas untuk user (transfer).** Wallet Web3Auth user butuh tBNB untuk gas transfer. Di produksi ini ditangani paymaster/gas sponsorship (Account Abstraction, PRD §9.5) yang **belum** diimplementasi. Untuk demo: kirim manual sedikit tBNB ke alamat wallet user setelah login.
- **Mint dilakukan backend, bukan user** — jadi user tidak perlu gas saat registrasi, hanya saat transfer.
- **Port 3000 dipakai proses lain** di mesin ini — selalu pakai `PORT=3210` (atau lain) dan samakan di `PUBLIC_VERIFY_BASE_URL` + `API_BASE_URL`.
- **Cleartext HTTP** — app mengaktifkan `usesCleartextTraffic` untuk backend lokal http. Hapus sebelum rilis (pakai HTTPS).
- **Threshold AI** — skor < `AUTH_SCORE_THRESHOLD` (70) mengembalikan status `flagged_for_review`, tidak mint. Turunkan sementara jika ingin menguji mint dengan foto seadanya.
- **App Links verify** (`https://origintag.app/verify/*` membuka app) butuh `assetlinks.json` di domain — belum relevan untuk demo lokal; QR mengarah ke URL backend langsung.
