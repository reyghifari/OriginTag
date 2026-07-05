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
- [ ] Part 1 — smart contract (kode dasar ada, belum deploy + test belum lengkap)
- [ ] Part 2 — backend API (skeleton jalan dengan mock mode)
- [ ] Part 3 — AI Authentication (stub, lihat `backend/src/ai/ai.service.ts`)
- [ ] Part 4 — BNB Greenfield (stub, lihat `backend/src/greenfield/greenfield.service.ts`)
- [ ] Part 5 — Android app (skeleton navigasi + network layer, wallet SDK belum)
- [ ] Part 6 — halaman verifikasi publik (versi minimal di `GET /verify/:tokenId`)
- [ ] Part 7 — integrasi & demo
