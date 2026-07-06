# OriginTag

Digital passport untuk autentikasi & kepemilikan barang fisik di BNB Chain (BSC + Greenfield), dengan **app Android native (Kotlin)** sebagai frontend.

Pembagian part, timeline, dan keputusan arsitektur ada di [PROJECT_BREAKDOWN.md](PROJECT_BREAKDOWN.md).

## Struktur Repo

```
OriginTag/
├── contracts/   # Part 1 — Smart contract BEP-721 (Hardhat, BSC Testnet)
├── backend/     # Part 2-4 & 6 — API NestJS + AI Authentication + Greenfield + halaman verifikasi publik
├── android/     # Part 5 — App Android (Kotlin + Jetpack Compose)
└── PROJECT_BREAKDOWN.md
```

## Prasyarat

- Node.js 20+ dan npm
- Android Studio (terbaru) + JDK 17+
- Wallet dengan tBNB dari [faucet BSC Testnet](https://www.bnbchain.org/en/testnet-faucet)

## Quick Start

### 1. Contracts (Part 1)

```bash
cd contracts
npm install
cp .env.example .env    # isi PRIVATE_KEY (wallet testnet, JANGAN wallet utama)
npm test                # unit test lokal
npm run deploy:testnet  # deploy ke BSC Testnet, catat contract address
```

### 2. Backend (Part 2)

```bash
cd backend
npm install
cp .env.example .env    # biarkan CONTRACT_ADDRESS kosong = MOCK MODE
npm run start:dev       # http://localhost:3000
```

**Mock mode:** selama `CONTRACT_ADDRESS` / `AUTHENTICATOR_PRIVATE_KEY` belum diisi, endpoint on-chain mengembalikan data mock (in-memory). Ini disengaja supaya app Android bisa dikembangkan paralel sebelum kontrak & AI siap (lihat timeline minggu 2 di PROJECT_BREAKDOWN.md).

### 3. Android (Part 5)

Buka folder `android/` di Android Studio → sync Gradle → run di emulator.

- `API_BASE_URL` default `http://10.0.2.2:3000/` (loopback host dari emulator, mengarah ke backend lokal).
- Gradle wrapper belum digenerate: Android Studio akan menawarkan membuatnya saat sync, atau jalankan `gradle wrapper --gradle-version 8.11.1` di folder `android/` jika punya gradle CLI.

## Alur End-to-End (MVP Hackathon)

registrasi barang → AI authentication → upload bukti ke Greenfield → mint passport NFT (BSC Testnet) → QR/halaman verifikasi publik → transfer kepemilikan + garansi ikut pindah

## Status Implementasi

- [x] Scaffold struktur repo
- [x] Part 1 — smart contract: implementasi lengkap, 6/6 unit test hijau (belum deploy ke testnet)
- [x] Part 2 — backend API: skeleton jalan, mock mode terverifikasi end-to-end
- [x] Part 3 — AI Authentication: implementasi live via Claude API (vision + structured outputs);
      isi `ANTHROPIC_API_KEY` di `backend/.env` untuk mengaktifkan, tanpa key = mock (skor 92)
- [x] Part 4 — BNB Greenfield: implementasi upload via `@bnb-chain/greenfield-js-sdk`
      (bucket ensure-exists + delegated object upload per struktur PRD §9.3);
      isi `GREENFIELD_PRIVATE_KEY` di `backend/.env` untuk aktif, tanpa key = mock
- [~] Part 5 — Android app: **build APK debug sukses**. Fungsional & tersambung backend:
      5b registrasi (Photo Picker + kompresi + multipart → mint), 5c detail passport.
      Masih TODO: 5a wallet SDK, 5d transfer signing, 5e scan QR (lihat penanda `TODO(Part 5x)`)
- [x] Part 6 — halaman verifikasi publik: versi minimal live di `GET /verify/:tokenId`
- [ ] Part 7 — integrasi & demo
