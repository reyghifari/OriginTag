# Deploy backend ke Hugging Face Spaces

Space ini hanya berisi 2 file (`README.md` + `Dockerfile` di folder ini). Dockerfile mengambil
kode backend dari repo GitHub publik, lalu build & jalan di port 7860.

## 1. Syarat

- Repo GitHub `reyghifari/OriginTag` sudah **Public**.
- Punya akun di https://huggingface.co (gratis, tanpa kartu).

## 2. Buat Space

1. https://huggingface.co/new-space
2. **Space name**: `origintag-api` · **SDK**: Docker → *Blank* · **Hardware**: CPU basic (free) · **Public**.
3. Setelah Space dibuat, buka tab **Files** → **+ Add file → Create a new file**:
   - `Dockerfile` → salin isi `deploy/huggingface/Dockerfile`.
   - `README.md` → ganti isinya dengan `deploy/huggingface/README.md` (bagian `---` di atas wajib ada).

## 3. Isi variables & secrets

**Settings → Variables and secrets.**

**New variable** (nilai publik):

| Nama | Nilai |
|---|---|
| `BSC_RPC_URL` | `https://data-seed-prebsc-1-s1.bnbchain.org:8545` |
| `CONTRACT_ADDRESS` | `0xE3Be425968a96A79FC5a9bd370cc075247BD7b00` |
| `MARKETPLACE_ADDRESS` | `0xcE03f725544ed187776af608780A3F95df06F339` |
| `AI_BASE_URL` | `https://generativelanguage.googleapis.com/v1beta/openai/` |
| `AI_MODEL` | `gemini-3.5-flash` |
| `AUTH_SCORE_THRESHOLD` | `70` |
| `GREENFIELD_RPC_URL` | `https://gnfd-testnet-fullnode-tendermint-us.bnbchain.org` |
| `GREENFIELD_CHAIN_ID` | `5600` |
| `GREENFIELD_BUCKET` | `origintag-evidence-482acb` |

**New secret** — salin nilainya dari `backend/.env` di Mac (jangan kirim ke chat):

- `AUTHENTICATOR_PRIVATE_KEY`
- `GREENFIELD_PRIVATE_KEY`
- `AI_API_KEY`

`PUBLIC_VERIFY_BASE_URL` tidak perlu diisi — backend memakai alamat Space otomatis (`SPACE_HOST`).

## 4. Tunggu build & cek

- Tab **App** / **Logs** → tunggu status **Running** (build pertama ± 3–5 menit).
- Alamat publik: `https://<username>-origintag-api.hf.space`
- Cek di browser: `https://<username>-origintag-api.hf.space/marketplace` → harus tampil `[...]` (JSON).

## 5. Setelah online

- Kirim alamat Space ke Claude → `releaseApiBaseUrl` di `android/app/build.gradle.kts` diisi, lalu build APK release.
- **Matikan backend lokal di Mac** saat Space sudah jalan: dua backend dengan private key yang sama bisa bentrok nonce saat mint bersamaan.

## Catatan

- Space gratis **tidur setelah 48 jam tanpa akses**; akses pertama berikutnya butuh ±30–60 dtk untuk bangun
  (timeout app sudah 120 dtk). Buka URL `/marketplace` sekali sebelum demo/penjurian.
- Daftar recall & service record dilacak di memori backend, jadi bisa tidak tampil setelah Space restart (event-nya tetap tercatat on-chain). Passport, kepemilikan dan listing aman (on-chain).
- Update kode: push ke GitHub `main`, lalu Space → **Settings → Factory rebuild**.
