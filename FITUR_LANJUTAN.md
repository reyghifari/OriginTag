# Riset & Rencana Fitur Lanjutan — OriginTag

Riset fitur untuk memperkaya OriginTag menjadi produk yang lebih lengkap & kompleks (marketplace, profil user, dll.), lengkap dengan cara implementasi per fitur. **Dokumen perencanaan — belum dieksekusi.**

Basis kode saat ini:
- **Kontrak** (`OriginTagPassport`, BSC Testnet `0x6769...191ad`): `mintPassport`, `transferPassport`, `getRemainingWarranty`, `getPassport`, `getOwnershipHistory`, `addServiceRecord` (SERVICE_ROLE), `issueRecall` (AUTHENTICATOR_ROLE). Events: `PassportMinted`, `PassportTransferred`, `ServiceRecordAdded`, `RecallIssued`.
- **Backend** (NestJS): items (register/get/transfer/warranty/photos), users, verify, blockchain (ethers), AI (Gemini), Greenfield (real).
- **App** (Android/Compose): onboarding, dashboard, register, detail, transfer, scan. **Belum ada:** profile, marketplace, service records, recall UI, search.

---

## 0. Catatan positioning — WAJIB baca dulu

PRD §5.2 menyatakan OriginTag adalah **infrastruktur trust-layer, BUKAN marketplace** ("Tidak membangun marketplace jual-beli sendiri") dan **tidak menangani pembayaran** (§5.2, §5.3). Menambahkan marketplace = bertentangan dengan positioning inti.

**Cara menyelesaikan tension ini (2 opsi framing):**

- **Opsi A — "Reference Marketplace" (disarankan):** bangun marketplace sebagai *contoh implementasi* yang membuktikan passport bisa dijualbelikan, sambil tetap menekankan bahwa passport **portable** ke marketplace mana pun via API/SDK. Narasi ke juri: *"Ini bukti konsep — marketplace apa pun bisa mengintegrasikan OriginTag; kami tunjukkan satu."* Konsisten dengan PRD sekaligus terasa lengkap.
- **Opsi B — Reposisi ke "trust-layer dengan resale bawaan":** ubah narasi jadi produk P2P resale yang trust-first. Lebih berani, tapi meninggalkan sudut "infrastruktur netral" yang justru jadi diferensiator.

Rekomendasi: **Opsi A**. Semua rencana marketplace di bawah memakai framing ini.

---

## 1. Profil & Identitas User (yang kamu sebut)

### 1.1 Halaman Profil
**Apa:** layar profil menampilkan identitas user + statistik + aksi wallet.

**Isi:**
- Avatar, nama, email → dari `web3Auth.getUserInfo()` (Web3Auth menyimpan `name`, `email`, `profileImage`, `typeOfLogin`).
- Alamat wallet + tombol **Copy** & **Share**.
- Saldo tBNB (untuk gas transfer).
- Statistik: jumlah passport dimiliki, jumlah diterbitkan, jumlah transfer keluar.
- Badge **Trust Score** (lihat §3).
- Tombol **Logout**.

**Cara implementasi:**
- **App:**
  - `feature/profile/ProfileScreen.kt` + `ProfileViewModel`.
  - Nama/email/foto: panggil `web3Auth.getUserInfo()` (lewat `LocalWeb3Auth`) → simpan di `WalletManager` saat login (tambah field `UserInfo` ke `SessionState.LoggedIn`).
  - Copy address: `ClipboardManager` (`LocalClipboardManager.current.setText(AnnotatedString(address))`).
  - Avatar: Coil `AsyncImage(model = profileImageUrl)`.
  - Tambah route `profile` di `AppNavHost` + item di bottom nav / ikon di TopAppBar Dashboard.
- **Backend:** endpoint baru `GET /users/:address/stats` → `{ owned, minted, transferredOut, trustScore }` dihitung dari event `PassportMinted`/`PassportTransferred` (query logs via ethers `queryFilter`).
- **Saldo:** `GET /users/:address/balance` (ethers `provider.getBalance`) atau web3j di app langsung.

**Kompleksitas:** Rendah–Sedang · **Dampak juri:** Sedang (produk terasa "jadi", bukan tool).

### 1.2 Copy address / Share passport
**Apa:** tombol copy alamat (profil, detail) + share link verifikasi publik passport.
**Cara:** `ClipboardManager` untuk copy; Android `Intent.ACTION_SEND` (Sharesheet) untuk share `verifyUrl`. Trivial, reuse di banyak tempat.
**Kompleksitas:** Rendah.

### 1.3 Bottom Navigation
**Apa:** navigasi bawah (Dashboard · Explore · Scan · Profile) menggantikan tombol-tombol terpisah.
**Cara:** `NavigationBar` Material3 + `NavHost` bersarang. Membuat app terasa seperti produk konsumen sungguhan.
**Kompleksitas:** Rendah.

---

## 2. Marketplace (fitur besar — centerpiece)

Framing: **Reference Marketplace** (§0 Opsi A). Passport yang sudah punya bukti AI + Greenfield bisa dijual, kepemilikan + garansi otomatis ikut pindah.

### 2.1 Pilihan arsitektur

| Pendekatan | Kelebihan | Kekurangan | Rekomendasi |
|---|---|---|---|
| **On-chain escrow** (kontrak Marketplace) | Trustless, "web3" sejati, dana aman di escrow | Butuh kontrak baru + buyer perlu tBNB | ✅ untuk impresi juri |
| **Off-chain listing + on-chain settle** | Browse cepat/gratis, tetap settle on-chain | Listing tak trustless | Hybrid terbaik |

**Rekomendasi:** kontrak escrow on-chain **+** backend indexer untuk feed browse (baca event, tampilkan cepat). Ini kombinasi paling meyakinkan.

### 2.2 Kontrak `OriginTagMarketplace` (escrow)

```
listItem(uint256 tokenId, uint256 price)     // seller; butuh approve NFT ke marketplace dulu
cancelListing(uint256 tokenId)               // seller batalkan
buyItem(uint256 tokenId) payable             // buyer bayar >= price; NFT pindah, dana ke seller (dikurangi fee)
getListing(uint256 tokenId) view             // {seller, price, active}
event ListingCreated(tokenId, seller, price)
event ItemSold(tokenId, seller, buyer, price)
event ListingCancelled(tokenId)
```
- Pola: OpenZeppelin `ReentrancyGuard`, platform fee opsional (mis. 2%) ke treasury.
- Buyer bayar dalam **tBNB** (native), escrow transfer NFT via `safeTransferFrom` + kirim dana ke seller.

> ⚠️ **Detail teknis penting:** `transferPassport` di kontrak passport meng-update `ownershipHistory`, TAPI transfer lewat marketplace memakai ERC721 `safeTransferFrom` standar yang **tidak** memicu logika itu. **Solusi:** refactor kontrak passport — pindahkan pencatatan `ownershipHistory` ke override hook `_update()` (dipanggil di SEMUA jalur transfer), sehingga transfer marketplace pun terekam. `transferPassport` jadi wrapper konsistensi. Ini justru menunjukkan kedalaman teknis ke juri. Garansi otomatis ikut karena `originalPurchaseDate` immutable.

### 2.3 Backend
- **Indexer:** dengarkan `ListingCreated`/`ItemSold`/`ListingCancelled` → cache listing aktif (in-memory Map atau SQLite ringan) untuk feed cepat.
- **Endpoint:**
  - `GET /marketplace` → daftar listing aktif (tokenId, brand, kategori, skor AI, foto, harga, seller, trust score).
  - `GET /marketplace/:tokenId` → detail listing.
  - `POST /marketplace/:tokenId/build-list-tx` & `build-buy-tx` → calldata unsigned (approve + list, atau buy) untuk ditandatangani wallet user di app (pola sama seperti transfer sekarang).

### 2.4 App
- **Explore/Marketplace feed:** `feature/marketplace/MarketplaceScreen` — grid kartu (foto, brand, skor, harga, badge trust). Filter brand/kategori/skor.
- **Detail listing:** reuse `PassportDetailScreen` + tombol **Beli** (harga) atau **Jual** (kalau pemilik).
- **List for sale:** dari detail passport milik sendiri → set harga → tanda tangani `approve` + `listItem` (2 tx atau `setApprovalForAll` sekali).
- **Buy:** tombol Beli → tanda tangani `buyItem` payable → passport pindah → muncul di dashboard.

**Kompleksitas:** Tinggi (kontrak + indexer + 3 layar) · **Dampak juri:** Sangat tinggi.

### 2.5 Integrasi trust
Tampilkan trust score seller (§3) di kartu marketplace — "beli dari seller terpercaya". Menghubungkan AI authentication → nilai jual nyata.

---

## 3. Trust Score & Reputasi Seller (PRD §14)

**Apa:** skor reputasi seller berdasarkan aktivitas on-chain.
**Rumus (contoh):** fungsi dari (jumlah passport diterbitkan dengan skor AI ≥90) + (jumlah penjualan sukses) − (jumlah di-flag/recall). Ditampilkan sebagai badge (mis. ⭐ 4.8 / "Verified Seller").
**Cara implementasi:**
- **Backend (disarankan untuk hackathon):** hitung on-the-fly dari event (`PassportMinted` per seller + skor dari ai-report Greenfield + `ItemSold`). Endpoint `GET /users/:address/trust`. Fleksibel, tanpa kontrak baru.
- **On-chain (fase lanjut):** kontrak `SellerReputation` yang di-update saat mint/sale — lebih "trustless" tapi kompleks.
**Kompleksitas:** Sedang · **Dampak:** Tinggi (mengikat AI ke ekonomi).

---

## 4. Riwayat Servis / Service Records (FR-10 — kontrak SUDAH siap)

**Apa:** service center resmi mencatat riwayat servis ke passport. Kontrak sudah punya `addServiceRecord` + `SERVICE_ROLE` + event `ServiceRecordAdded` — **tinggal UI + backend**.
**Cara implementasi:**
- **Backend:** grant `SERVICE_ROLE` ke wallet demo service-center. Endpoint `POST /items/:tokenId/service-records` (upload dokumen/foto servis ke Greenfield → objectId → `addServiceRecord`). List via event `ServiceRecordAdded`.
- **App:** timeline riwayat servis di `PassportDetailScreen`; mode "Service Center" (screen terpisah) untuk menambah record.
**Kompleksitas:** Sedang (kontrak gratis, sudah ada) · **Dampak:** Sedang–Tinggi (cerita B2B, lifecycle lengkap).

---

## 5. Recall & Notifikasi (FR-11 — kontrak SUDAH siap)

**Apa:** brand memicu recall; pemilik saat ini dapat peringatan. Kontrak sudah punya `issueRecall` + event `RecallIssued`.
**Cara implementasi:**
- **Backend:** endpoint admin `POST /recalls` (panggil `issueRecall`). Indexer dengarkan `RecallIssued`, tandai passport brand+kategori terdampak. `GET /items/:tokenId` sertakan `recalled: bool`.
- **App & halaman verify:** banner merah "⚠ RECALL" pada passport terdampak. (Opsional: push notification via FCM — lebih berat.)
**Kompleksitas:** Sedang · **Dampak:** Tinggi (diferensiator vs "NFT biasa", nilai regulasi/keamanan).

---

## 6. Discovery: Search, Explore, Filter

**Apa:** telusuri semua passport terverifikasi (galeri publik), cari per brand/kategori, filter skor.
**Cara implementasi:**
- **Backend:** indexer `PassportMinted` → daftar searchable (brand, kategori, skor, foto). `GET /explore?brand=&category=&minScore=`.
- **App:** `ExploreScreen` dengan search bar + filter chips (bisa jadi tab di bottom nav).
- **Halaman verify web** juga bisa punya galeri publik → juri bisa jelajah tanpa app.
**Kompleksitas:** Sedang · **Dampak:** Sedang (produk terasa "berisi").

---

## 7. Wallet & Transaksi

| Fitur | Cara | Kompleksitas |
|---|---|---|
| **Saldo tBNB di profil** | `provider.getBalance` (backend/web3j) | Rendah |
| **Receive QR** (QR alamat sendiri untuk terima transfer) | Generate QR dari address (ZXing, generator yang sama dengan QR passport di RENCANA_JURI) | Rendah |
| **Riwayat transaksi** | Index event per address atau BSCScan API `GET /api?module=account&action=txlist` | Sedang |
| **Faucet shortcut** | Link/tombol ke faucet BSC untuk isi gas user | Trivial |

---

## 8. Warranty Claim Flow (FR-04)

**Apa:** pemilik ajukan klaim garansi; brand verifikasi sisa garansi + kepemilikan langsung on-chain.
**Cara implementasi:**
- **App:** di detail passport, kalau garansi masih aktif → tombol "Ajukan Klaim Garansi" → tampilkan sisa garansi + generate referensi klaim.
- **Backend:** `POST /items/:tokenId/warranty-claim` → verifikasi `ownerOf` + `getRemainingWarranty` on-chain → simpan klaim (atau emit event). Brand lihat daftar klaim.
**Kompleksitas:** Sedang · **Dampak:** Sedang (melengkapi janji inti "garansi ikut pindah").

---

## Matriks Kompleksitas vs Dampak

| Fitur | Kompleksitas | Dampak juri | Butuh kontrak baru? |
|---|---|---|---|
| Profil + copy address (§1) | Rendah | Sedang | Tidak |
| Bottom nav (§1.3) | Rendah | Rendah–Sedang | Tidak |
| **Marketplace escrow (§2)** | **Tinggi** | **Sangat tinggi** | Ya (+ refactor passport) |
| Trust score (§3) | Sedang | Tinggi | Tidak (backend) |
| Service records (§4) | Sedang | Sedang–Tinggi | Tidak (sudah ada) |
| Recall (§5) | Sedang | Tinggi | Tidak (sudah ada) |
| Explore/search (§6) | Sedang | Sedang | Tidak |
| Wallet balance/QR (§7) | Rendah | Sedang | Tidak |
| Warranty claim (§8) | Sedang | Sedang | Tidak |

---

## Paket Rekomendasi

### Paket "Impact Cepat" (2–3 hari) — kalau waktu terbatas
Profil + copy address (§1) → Bottom nav (§1.3) → Recall (§5) → Service records (§4).
> Semua **tanpa kontrak baru** (recall & service pakai fungsi yang sudah ada), langsung bikin app terasa lengkap & lifecycle-nya utuh. Risiko rendah.

### Paket "Wow Juri" (4–6 hari) — kalau mau ambisius
Paket Impact Cepat **+ Marketplace escrow (§2) + Trust score (§3) + Explore (§6)**.
> Marketplace = centerpiece yang membedakan. Trust score mengikat AI ke nilai jual. Explore + profil bikin terasa produk konsumen matang.

### Yang cukup jadi slide "roadmap" (jangan dikoding)
Paymaster/AA gas sponsorship, SDK marketplace pihak ketiga, notifikasi push FCM, reputasi on-chain penuh, multi-kategori garansi kompleks.

---

## Urutan implementasi teknis yang disarankan (kalau ambil Paket Wow)

1. **Refactor kontrak passport** — pindah `ownershipHistory` ke `_update()` hook (prasyarat marketplace). Redeploy + update address.
2. **Kontrak Marketplace** — list/buy/cancel + escrow, deploy ke BSC Testnet.
3. **Backend indexer** — event listener (`ListingCreated`, `ItemSold`, `PassportMinted`, `RecallIssued`, `ServiceRecordAdded`) → cache untuk feed cepat.
4. **Endpoint** marketplace/explore/profil-stats/trust/service/recall.
5. **App** — bottom nav → Profile → Explore/Marketplace → List/Buy → Service/Recall UI.

**Ketergantungan kunci:** marketplace butuh (a) refactor kontrak, (b) wallet user punya tBNB untuk buy, (c) backend indexer. Sisipkan URL backend permanen (Render/named tunnel) jika demo perlu diakses juri kapan saja.
