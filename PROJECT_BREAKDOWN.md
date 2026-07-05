# OriginTag — Project Breakdown (Frontend: Android Kotlin)

Berdasarkan PRD v1.0 (5 Juli 2026), disesuaikan untuk frontend **Android native (Kotlin + Jetpack Compose)** menggantikan Next.js web app. Scope: solo developer, hackathon MVP (~1 bulan).

## Perubahan Arsitektur dari PRD

| Aspek | PRD Asli | Versi Android |
|---|---|---|
| Frontend | Next.js + wagmi/RainbowKit | Android Kotlin + Jetpack Compose |
| Wallet connect | RainbowKit (browser extension) | Web3Auth/Particle **Android SDK** (social login) + WalletConnect v2 (MetaMask/Trust Wallet mobile) |
| Upload foto | File input browser | CameraX + Photo Picker |
| Scan QR | Kamera web | ML Kit Barcode Scanning |
| Query on-chain dari client | wagmi/viem | **web3j** (read-only) atau lewat backend API |
| Halaman verifikasi publik | Bagian dari Next.js app | **Tetap perlu web page minimal** (di-serve backend) karena FR-06 mensyaratkan akses tanpa app. QR berisi URL `origintag.app/verify/{tokenId}` + Android App Link agar terbuka di app jika terinstall |

---

## Part 1 — Smart Contract (BSC Testnet)

**Tujuan:** Kontrak `OriginTagPassport` (BEP-721) deployed & terverifikasi di BSC Testnet.

- Setup Hardhat atau Foundry.
- Implementasi kontrak sesuai PRD §10: struct `Passport`, `mintPassport()`, `transferPassport()`, `getRemainingWarranty()`, role `AUTHENTICATOR_ROLE` / `SERVICE_ROLE`, events (`PassportMinted`, `PassportTransferred`, dst).
- OpenZeppelin: `ERC721Enumerable`, `AccessControl`, `ReentrancyGuard`.
- Unit test (mint, transfer, warranty countdown, access control).
- Deploy ke BSC Testnet + verify di BSCScan.

**Deliverable:** Contract address + ABI (dipakai Part 2 & Part 5).
**Dependensi:** Tidak ada — kerjakan pertama.
**Estimasi:** 3–4 hari.

## Part 2 — Backend API (Node.js/NestJS)

**Tujuan:** Orkestrator antara app Android, AI, Greenfield, dan smart contract.

- Endpoint sesuai PRD §12: `POST /items/register`, `GET /items/:tokenId`, `POST /items/:tokenId/transfer`, `GET /items/:tokenId/warranty`, `GET /users/:walletAddress/passports`.
- Wallet backend memegang `AUTHENTICATOR_ROLE` untuk memicu mint (via ethers.js/viem).
- Multipart upload untuk foto (1–5 gambar), validasi field wajib (FR-02).
- Threshold skor keaslian → mint atau flag manual review.

**Deliverable:** API berjalan (lokal + deploy, mis. Railway/Fly.io) dengan dokumentasi endpoint.
**Dependensi:** Part 1 (ABI + address). Bisa mulai paralel dengan mock.
**Estimasi:** 4–5 hari (di luar integrasi AI & Greenfield).

## Part 3 — AI Authentication Engine

**Tujuan:** Service yang menerima foto + kategori, mengembalikan skor keaslian 0–100 + ringkasan kondisi (JSON terstruktur).

- Panggil LLM multimodal via API (vision) — bukan training model sendiri.
- Prompt engineering: analisis logo, jahitan, material, serial number vs ciri khas brand/kategori.
- Output JSON stabil: `{ score, condition_summary, reasons[], flags[] }`.
- Metadata generator: LLM menyusun JSON metadata NFT dari form + hasil vision.

**Deliverable:** Modul di dalam backend (atau service terpisah) dengan output konsisten.
**Dependensi:** Part 2 (dipanggil dari `/items/register`).
**Estimasi:** 2–3 hari.

## Part 4 — BNB Greenfield Storage

**Tujuan:** Semua bukti (foto, ai-report.json) tersimpan di Greenfield, hash/objectId direferensikan di metadata NFT (FR-05).

- Setup bucket `origintag-evidence` di Greenfield Testnet.
- SDK `@bnb-chain/greenfield-js-sdk` di backend: create object per struktur PRD §9.3 (`{tokenId}/photos/`, `{tokenId}/ai-report.json`).
- Permission: metadata publik vs evidence privat.
- Kembalikan `evidenceObjectId` untuk parameter mint.

**Deliverable:** Upload/download flow berfungsi dari backend.
**Dependensi:** Part 2.
**Estimasi:** 2–3 hari (sisakan buffer — dokumentasi Greenfield SDK sering jadi bottleneck).

## Part 5 — Android App (Kotlin) ⭐ Part terbesar

**Tujuan:** App utama untuk seller & buyer.

**Stack:** Kotlin, Jetpack Compose, MVVM + Hilt, Retrofit/OkHttp, Coil, CameraX, ML Kit (QR), Web3Auth/Particle Android SDK, WalletConnect v2 (Kotlin), web3j (opsional untuk read on-chain langsung).

Pecah lagi jadi 5 modul fitur:

### 5a. Onboarding & Wallet (FR-01)
- Social login (email/Google) via Web3Auth/Particle Android SDK → embedded wallet.
- Opsi connect MetaMask/Trust Wallet via WalletConnect v2 deep link.
- Sesi wallet address tersimpan (DataStore).

### 5b. Registrasi Barang (FR-02, FR-03, FR-04)
- Form metadata (brand, kategori, serial number, tanggal beli).
- Ambil foto via CameraX / pilih dari galeri (1–5 foto), kompresi sebelum upload.
- Upload ke `POST /items/register`, tampilkan progress: AI analyzing → minting → passport aktif (dengan QR yang bisa dibagikan/dicetak).
- State "Flagged for Manual Review" jika skor di bawah threshold.

### 5c. Dashboard "Barang Saya" (FR-09)
- List semua passport milik wallet aktif (via `GET /users/:address/passports`).
- Detail passport: skor keaslian, foto, sisa garansi (countdown), riwayat kepemilikan.

### 5d. Transfer Kepemilikan (FR-07, FR-08)
- Input address buyer (ketik / scan QR wallet buyer).
- Trigger transfer (via backend atau signing langsung dari wallet SDK).
- Konfirmasi + update dashboard, tampilkan sisa garansi untuk pemilik baru.

### 5e. Scan & Verifikasi (sisi buyer)
- Scan QR fisik di barang via ML Kit → buka halaman detail passport publik di dalam app.
- Android App Links untuk `origintag.app/verify/*` agar link dari luar terbuka di app.

**Deliverable:** APK debug yang menjalankan seluruh flow end-to-end di BSC Testnet.
**Dependensi:** Part 2 (API contract). UI bisa mulai paralel dengan mock API.
**Estimasi:** 10–14 hari.

## Part 6 — Halaman Verifikasi Publik (Web, minimal)

**Tujuan:** Memenuhi FR-06 — siapa pun tanpa app/wallet bisa cek `origintag.app/verify/{tokenId}` dari browser mobile biasa.

- Satu halaman server-rendered ringan (bisa langsung dari backend NestJS + template, tidak perlu Next.js penuh).
- Tampilkan: status keaslian, skor, riwayat kepemilikan (address terpotong `0x71C...9a3F`), sisa garansi, foto publik.
- File `assetlinks.json` untuk Android App Links.

**Deliverable:** Halaman publik live + QR generator.
**Dependensi:** Part 2.
**Estimasi:** 1–2 hari.

## Part 7 — Integrasi, Testing & Demo

**Tujuan:** Semua part tersambung, siap demo hackathon.

- E2E test flow PRD §3.2: registrasi → AI → mint → QR → transfer → garansi ikut pindah.
- Uji di device fisik (kamera, deep link, WalletConnect ke Trust Wallet asli).
- Seed data demo (2–3 barang), video demo, README arsitektur.

**Dependensi:** Semua part.
**Estimasi:** 2–3 hari.

---

## Urutan Pengerjaan yang Disarankan

```
Minggu 1 : Part 1 (kontrak) → mulai Part 2 (skeleton API + mock AI)
Minggu 2 : Part 3 (AI) + Part 4 (Greenfield) → Part 2 selesai penuh
           Paralel: Part 5a-5b (Android: wallet + registrasi, pakai mock API dulu)
Minggu 3 : Part 5c-5e (dashboard, transfer, scan) + Part 6 (web verify)
Minggu 4 : Part 7 (integrasi, testing, demo) + buffer
```

**Prinsip:** Part 1 → 2 adalah jalur kritis. Android UI (Part 5) bisa jalan paralel sejak awal dengan API mock, lalu disambungkan begitu backend siap.

## Risiko Spesifik Android (tambahan dari PRD §18)

| Risiko | Mitigasi |
|---|---|
| Web3Auth/Particle Android SDK lebih ribet dari versi web | Fallback: WalletConnect saja untuk demo, atau backend-custodial-lite untuk hackathon |
| Greenfield SDK hanya matang di JS | Semua operasi Greenfield lewat backend, Android tidak sentuh Greenfield langsung |
| Review flow signing transaksi di mobile lambat | Mint dilakukan backend (AUTHENTICATOR_ROLE); user signing hanya untuk transfer |
| Upload foto besar dari kamera HP | Kompresi client-side sebelum upload |
