# Catatan Setup Dev/Demo OriginTag

Status & cara menjalankan setup saat ini (BSC Testnet, backend lokal + Cloudflare quick tunnel). Simpan file ini sebagai rujukan.

---

## Status saat ini

| Komponen | Nilai / lokasi |
|---|---|
| **Smart contract** | `0x6769eAeB7D0d900233c96ca4fD82EeB7438191ad` (BSC Testnet) · [BSCScan](https://testnet.bscscan.com/address/0x6769eAeB7D0d900233c96ca4fD82EeB7438191ad) |
| **Backend** | NestJS, jalan di laptop `localhost:3210` |
| **URL publik backend** | via Cloudflare quick tunnel (lihat di bawah — bisa berubah) |
| **App Android** | `API_BASE_URL` di `android/app/build.gradle.kts` menunjuk ke URL tunnel |
| **Wallet Deployer** | `0x0100F82FB1E2D635F35e517B033558FBa6b2DF8d` (admin kontrak) |
| **Wallet Backend** | `0x4a16A3F4A10633a4BBB9C1Ecc69F6AF49e553BbE` (mint, `AUTHENTICATOR_ROLE`) |
| **AI Authentication** | ✅ REAL — Google Gemini `gemini-3.5-flash` (vision) |
| **BNB Greenfield** | ✅ REAL — bucket `origintag-evidence-482acb`, wallet `0x78fa...2ACB` |

Secret ada di `backend/.env` (private key backend, contract address) dan `android/local.properties` (Web3Auth Client ID) — **keduanya gitignored, tidak ter-commit.**

---

## ⚠️ Aturan penting quick tunnel

- **Jangan restart proses `cloudflared`.** URL `*.trycloudflare.com` tetap sama SELAMA prosesnya hidup. Restart = URL baru = harus update app.
- **Laptop harus nyala** saat demo/tes (backend + tunnel jalan di laptop).
- URL berubah kalau: laptop reboot, proses cloudflared mati, atau terminal ditutup.

---

## Cara menjalankan dari nol (mis. setelah laptop reboot)

Jalankan 2 proses ini, biarkan kedua terminal terbuka:

```bash
# Terminal 1 — backend
cd ~/Documents/web3/OriginTag/backend
node dist/main.js
# (kalau habis ubah kode backend: npm run build dulu)

# Terminal 2 — tunnel
cloudflared tunnel --url http://localhost:3210
# → catat URL https://XXXX.trycloudflare.com yang muncul
```

Kalau URL tunnel **berbeda** dari sebelumnya, update di 2 tempat lalu rebuild app:

```bash
# 1. android/app/build.gradle.kts — ganti API_BASE_URL ke URL baru (akhiri dengan /)
#    buildConfigField("String", "API_BASE_URL", "\"https://URL-BARU.trycloudflare.com/\"")

# 2. backend/.env — ganti PUBLIC_VERIFY_BASE_URL ke URL baru (tanpa / di akhir)
#    PUBLIC_VERIFY_BASE_URL=https://URL-BARU.trycloudflare.com

# 3. restart backend (Terminal 1: Ctrl+C lalu jalankan lagi)

# 4. rebuild + install app
cd ~/Documents/web3/OriginTag/android
./gradlew installDebug
```

## Menjalankan emulator + app

```bash
# emulator
~/Library/Android/sdk/emulator/emulator @Pixel_7 &
# install app
cd ~/Documents/web3/OriginTag/android && ./gradlew installDebug
```

Cek koneksi cepat (dari browser HP/emulator atau terminal):
`https://URL-TUNNEL.trycloudflare.com/verify/0` → harus muncul passport Nike token #0.

---

## Alur tes di app

1. Login (Google/Email via Web3Auth) atau "Lewati (mode demo)".
2. **+ Daftarkan Barang** → isi brand/kategori → pilih foto → Verifikasi & Mint → passport ter-mint on-chain.
3. Muncul di Dashboard "Barang Saya".
4. **Transfer**: butuh login Web3Auth asli + wallet user diisi sedikit tBNB (kirim manual ke alamat wallet user setelah login).

---

## Yang masih TODO / opsi ke depan

- **URL permanen (server 24/7 tanpa laptop):** deploy backend ke Render (config `render.yaml` sudah siap & ter-commit, tinggal deploy — butuh kartu untuk verifikasi akun) atau Cloudflare named tunnel (butuh domain).
- **Tahap 2 — AI sungguhan:** isi `ANTHROPIC_API_KEY` di `backend/.env`.
- **Tahap 2 — Greenfield sungguhan:** siapkan wallet Greenfield + dana tBNB Greenfield, isi `GREENFIELD_PRIVATE_KEY`.
- **Gas transfer untuk user:** paymaster/Account Abstraction (PRD §9.5) belum diimplementasi — untuk demo kirim tBNB manual ke wallet user.

---

## Config Render (sudah disiapkan, belum dipakai)

`render.yaml` di root sudah dikonfigurasi untuk deploy backend. Kalau nanti mau pakai Render:
1. `git push origin main`
2. Render → New → Blueprint → pilih repo → isi secret `AUTHENTICATOR_PRIVATE_KEY` di dashboard.
3. Dapat URL `*.onrender.com` tetap → update `API_BASE_URL` app ke URL itu.

Catatan Render free tier: "tidur" setelah 15 menit idle, request pertama ~50 detik (timeout app sudah dinaikkan ke 120 detik untuk mengantisipasinya).
