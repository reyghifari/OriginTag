# Rencana Peningkatan untuk Penilaian Juri

Rencana fitur & kemasan agar OriginTag lebih menonjol di mata juri hackathon BNB Chain. Disusun setelah alur inti terbukti jalan end-to-end (7 Juli 2026). **Belum dieksekusi — ini dokumen perencanaan.**

---

## Posisi proyek saat ini

### Yang sudah kuat (jangan diutak-atik)
- **Cerita ekosistem BNB:** BSC (NFT passport) + Greenfield (bukti foto & AI report) — dua dari tiga komponen dipakai **sungguhan**, bukan mock.
- **Diferensiator nyata:** AI vision (Gemini) terbukti menolak foto yang tidak cocok klaim — kasus uji "foto Kindle diklaim Nike Sneakers" ditolak dengan alasan detail. Ini materi pitch.
- **End-to-end jalan:** login social (Web3Auth) → registrasi + AI → mint on-chain → dashboard → transfer → verifikasi publik.

### Celah yang paling terasa oleh juri
1. **Loop demo belum "menutup" secara visual** — passport tidak menampilkan QR-nya sendiri, padahal scan QR adalah momen paling demoable.
2. **Halaman verifikasi publik masih polos** — padahal ini satu-satunya bagian yang juri bisa buka sendiri dari HP mereka.
3. **opBNB belum disentuh** — juri hackathon BNB sangat menghargai pemakaian ketiga komponen ekosistem.
4. **Kemasan submission** (README juri, video, arsitektur) belum ada.

---

## Tier 1 — Menutup loop demo (prioritas tertinggi, ~1 hari)

| # | Fitur | Kenapa juri suka | Effort | Status |
|---|---|---|---|---|
| 1 | **QR code passport** di layar detail app + di halaman verify (untuk dicetak/ditempel di barang) | Menutup loop: mint → tunjuk QR → HP lain scan → halaman verify. Momen "aha" di panggung | ~2–3 jam (ZXing generator; scanner sudah ada) | ⬜ |
| 2 | **Glow-up halaman verifikasi publik**: foto barang (endpoint proxy sudah ada), alasan & flags AI, timeline kepemilikan, link tx BSCScan, badge "AI-verified + on-chain + Greenfield" | "Wajah" produk yang bisa dibuka juri sendiri tanpa install apa pun | ~3–4 jam | ⬜ |
| 3 | **Alasan AI tampil di app** (reasons/flags dari ai-report.json di layar detail) | AI-nya menjelaskan, bukan black box | ~1–2 jam | ⬜ |
| 4 | **Transfer via scan QR alamat buyer** | Transfer live di panggung mulus, tanpa ketik `0x...` | ~1 jam (reuse scanner 5e) | ⬜ |

## Tier 2 — Kartu as ekosistem (~1 hari)

| # | Fitur | Kenapa juri suka | Effort | Status |
|---|---|---|---|---|
| 5 | **opBNB: ScanRegistry** — kontrak kecil di opBNB testnet, mencatat setiap scan/verifikasi publik sebagai event on-chain (backend log tiap hit `/verify/:tokenId`) | Melengkapi trio **BSC + Greenfield + opBNB**. Narasi: "operasi frekuensi tinggi di L2 <$0.001, kepemilikan tetap di L1" — arsitektur PRD §9.2 yang jalan sekarang, bukan "fase 2" | ~4–5 jam (kontrak mini + deploy; network opBNB sudah ada di hardhat.config) | ⬜ |
| 6 | **Demo recall (FR-11)** — kontrak sudah punya `issueRecall`; tinggal script trigger + banner merah di app & halaman verify untuk passport brand+kategori terdampak | Cerita B2B/brand yang membedakan dari "NFT biasa"; nilai bagi manufaktur & regulasi | ~3–4 jam | ⬜ |

## Tier 3 — Kemasan submission (wajib sebelum submit, ~1 hari)

| # | Item | Isi | Status |
|---|---|---|---|
| 7 | **README untuk juri** | Diagram arsitektur, screenshot alur, tabel "komponen BNB → dipakai untuk apa", link kontrak BSCScan, cara mencoba sendiri | ⬜ |
| 8 | **Video demo 2–3 menit** | Storyboard: masalah (barang palsu + regulasi ESPR) → registrasi + **AI menolak barang palsu (kasus Kindle!)** → mint → scan QR dari HP lain → transfer → recall | ⬜ |
| 9 | **Seed data demo** | 2–3 barang realistis dengan foto bagus supaya dashboard hidup saat presentasi | ⬜ |
| 10 | **Checklist demo-day** | Backend + tunnel hidup, URL dicek, tBNB di wallet user, emulator/HP siap, fallback disiapkan | ⬜ |

## Roadmap saja — JANGAN dikerjakan (cukup 1 slide "what's next")
- Paymaster / gas sponsorship (Account Abstraction, PRD §9.5)
- SDK integrasi marketplace pihak ketiga
- Trust score seller on-chain
- Dashboard manual review untuk item flagged
- `tokenURI` metadata NFT (butuh URL backend permanen dulu; tunnel berubah-ubah)

---

## Pemetaan ke kriteria penilaian umum

| Kriteria | Jawaban OriginTag |
|---|---|
| **Inovasi** | AI authentication + demo penolakan palsu (#2, #3 memamerkannya) |
| **Eksekusi teknis** | Semua nyata di testnet; #5 melengkapi trio BNB |
| **Ecosystem fit** | BSC ✓ · Greenfield ✓ · opBNB → #5 |
| **UX** | #1, #4 — loop QR mulus, onboarding tanpa seed phrase |
| **Viabilitas bisnis** | #6 recall (cerita B2B) + narasi regulasi ESPR (sudah di PRD) |

## Urutan eksekusi yang disarankan

- **Hari 1:** Tier 1 (#1 → #2 → #3 → #4) — demo langsung terasa jauh lebih hidup.
- **Hari 2:** Tier 2 (#5 opBNB dulu — bobot ekosistem terbesar; #6 recall jika waktu cukup).
- **Hari 3:** Tier 3 — kemasan & latihan demo.

**Kalau hanya punya 1 hari:** kerjakan **#1 + #2 + #5** saja — kombinasi effort-terkecil / kesan-terbesar.
