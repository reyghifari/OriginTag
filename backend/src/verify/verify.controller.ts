import { Controller, Get, Header, Param } from '@nestjs/common';
import { ItemsService } from '../items/items.service';

/**
 * Part 6 — halaman verifikasi publik (FR-06).
 * Harus bisa dibuka dari browser HP biasa TANPA wallet/login/app.
 * QR fisik di barang mengarah ke {PUBLIC_VERIFY_BASE_URL}/verify/{tokenId}.
 */
@Controller('verify')
export class VerifyController {
  constructor(private readonly items: ItemsService) {}

  @Get(':tokenId')
  @Header('Content-Type', 'text/html; charset=utf-8')
  async verify(@Param('tokenId') tokenId: string): Promise<string> {
    try {
      const p = await this.items.getPublicPassport(tokenId);
      // Privasi (PRD §8.3): alamat wallet ditampilkan terpotong
      const anon = (a: string) => (a && a.length > 10 ? `${a.slice(0, 5)}...${a.slice(-4)}` : a);
      const warrantyDays = Math.floor(p.remainingWarrantySeconds / 86400);
      const historyHtml = p.ownershipHistory
        .map((a, i) => `<li>${i === 0 ? 'Pemilik pertama' : `Pemilik ke-${i + 1}`}: <code>${anon(a)}</code></li>`)
        .join('');

      return `<!doctype html>
<html lang="id">
<head>
  <meta charset="utf-8">
  <meta name="viewport" content="width=device-width, initial-scale=1">
  <title>OriginTag — Passport #${tokenId}</title>
  <style>
    body { font-family: system-ui, sans-serif; max-width: 480px; margin: 0 auto; padding: 24px; color: #1a1a1a; }
    .badge { display: inline-block; padding: 4px 12px; border-radius: 999px; font-weight: 600;
             background: ${p.authenticityScore >= 70 ? '#dcfce7; color:#166534' : '#fef9c3; color:#854d0e'}; }
    .card { border: 1px solid #e5e5e5; border-radius: 12px; padding: 16px; margin: 16px 0; }
    h1 { font-size: 1.3rem; } code { background: #f5f5f5; padding: 2px 6px; border-radius: 4px; }
  </style>
</head>
<body>
  <h1>🏷️ OriginTag Passport #${tokenId}</h1>
  <p><span class="badge">${p.authenticityScore >= 70 ? '✓ Terverifikasi' : '⚠ Perlu Review'} — skor ${p.authenticityScore}/100</span></p>
  <div class="card">
    <strong>${p.brand}</strong> — ${p.category}<br>
    Serial: <code>${p.serialNumber || '-'}</code><br>
    Sisa garansi: <strong>${warrantyDays} hari</strong>
  </div>
  <div class="card">
    <strong>Riwayat kepemilikan</strong>
    <ol>${historyHtml}</ol>
    Pemilik saat ini: <code>${anon(p.owner)}</code>
  </div>
  <p style="color:#737373;font-size:0.85rem">Data langsung dari BNB Smart Chain — immutable & bisa diverifikasi independen.</p>
</body>
</html>`;
    } catch {
      return `<!doctype html>
<html lang="id"><head><meta charset="utf-8"><meta name="viewport" content="width=device-width, initial-scale=1"><title>OriginTag</title></head>
<body style="font-family:system-ui,sans-serif;max-width:480px;margin:0 auto;padding:24px">
  <h1>Passport tidak ditemukan</h1>
  <p>Token #${tokenId} tidak terdaftar. Barang ini mungkin belum memiliki passport OriginTag — hati-hati terhadap barang palsu.</p>
</body></html>`;
    }
  }
}
