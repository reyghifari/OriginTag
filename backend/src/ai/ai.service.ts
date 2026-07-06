import { Injectable, Logger } from '@nestjs/common';
import { ConfigService } from '@nestjs/config';
import OpenAI from 'openai';

export interface AiAuthResult {
  /** Skor keaslian 0-100 (FR-03) */
  score: number;
  conditionSummary: string;
  reasons: string[];
  flags: string[];
}

const SUPPORTED_IMAGE_TYPES = ['image/jpeg', 'image/png', 'image/gif', 'image/webp'];

const SYSTEM_PROMPT = `Kamu adalah expert authenticator barang branded (sneakers, tas mewah, fashion, elektronik) untuk OriginTag — protokol digital passport yang menilai keaslian barang preloved sebelum diterbitkan sertifikat NFT-nya.

Analisis setiap foto pada aspek berikut:
- Logo & branding: font, proporsi, posisi, kualitas emboss/print
- Jahitan & konstruksi: kerapian, pola, kepadatan stitch
- Material: tekstur, kilap, kualitas yang terlihat
- Label, serial number, tag: format konsisten dengan standar brand
- Kondisi keseluruhan: keausan, kerusakan, kelengkapan

Ketentuan skor — KONSERVATIF, karena meloloskan barang palsu jauh lebih merugikan daripada menahan barang asli untuk review manual (skor di bawah threshold akan direview manusia, bukan ditolak):
- 90-100: sangat yakin asli, seluruh detail konsisten dan terlihat jelas
- 70-89: kemungkinan besar asli, sebagian detail minor tidak terverifikasi
- 40-69: tidak dapat dipastikan — foto kurang detail atau ada kejanggalan
- 0-39: indikasi kuat palsu

Jika foto buram, gelap, tidak lengkap, atau tidak memperlihatkan detail kunci (logo close-up, label, serial number), turunkan skor dan catat kekurangannya di flags.`;

const JSON_INSTRUCTION = `Balas HANYA dengan satu objek JSON valid, tanpa teks pembuka/penutup dan tanpa blok markdown. Bentuk persis:
{"score": <bilangan bulat 0-100>, "conditionSummary": "<ringkasan kondisi 1-2 kalimat>", "reasons": ["<alasan pendukung skor>"], "flags": ["<kejanggalan; array kosong jika tidak ada>"]}`;

/**
 * Part 3 — AI Authentication Engine (PRD §9.4).
 * Default pakai Google Gemini lewat endpoint kompatibel-OpenAI
 * (AI_BASE_URL=https://generativelanguage.googleapis.com/v1beta/openai/).
 * Gemini mendukung analisis gambar (vision) — berbeda dari DeepSeek V4 yang teks saja.
 * Bisa juga dipakai untuk provider OpenAI-compatible lain (OpenAI, dll.) via env.
 */
@Injectable()
export class AiService {
  private readonly logger = new Logger(AiService.name);
  private readonly client?: OpenAI;
  private readonly model: string;

  constructor(config: ConfigService) {
    this.model = config.get<string>('AI_MODEL') ?? 'gemini-2.0-flash';
    const apiKey = config.get<string>('AI_API_KEY');
    const baseURL =
      config.get<string>('AI_BASE_URL') ??
      'https://generativelanguage.googleapis.com/v1beta/openai/';

    if (apiKey) {
      this.client = new OpenAI({ apiKey, baseURL });
      this.logger.log(`AI Authentication aktif (model: ${this.model}, baseURL: ${baseURL})`);
    } else {
      this.logger.warn('AI_API_KEY belum diset — AiService jalan di MOCK MODE');
    }
  }

  async analyzeItem(
    photos: Express.Multer.File[],
    metadata: { brand: string; category: string; serialNumber?: string },
  ): Promise<AiAuthResult> {
    if (!this.client) return this.mockResult(photos, metadata);

    const imageParts: OpenAI.Chat.Completions.ChatCompletionContentPart[] = photos
      .filter((f) => SUPPORTED_IMAGE_TYPES.includes(f.mimetype))
      .slice(0, 5)
      .map((f) => ({
        type: 'image_url',
        image_url: { url: `data:${f.mimetype};base64,${f.buffer.toString('base64')}` },
      }));

    if (imageParts.length === 0) {
      return this.manualReviewResult('Tidak ada foto dengan format yang didukung (JPEG/PNG/GIF/WebP)');
    }

    try {
      const response = await this.client.chat.completions.create({
        model: this.model,
        max_tokens: 2048,
        messages: [
          { role: 'system', content: SYSTEM_PROMPT },
          {
            role: 'user',
            content: [
              ...imageParts,
              {
                type: 'text',
                text:
                  `Nilai keaslian barang berikut berdasarkan foto di atas.\n` +
                  `Brand: ${metadata.brand}\n` +
                  `Kategori: ${metadata.category}\n` +
                  `Serial number (klaim penjual): ${metadata.serialNumber || 'tidak dicantumkan'}\n\n` +
                  JSON_INSTRUCTION,
              },
            ],
          },
        ],
      });

      const text = response.choices[0]?.message?.content ?? '';
      const parsed = this.parseResult(text);
      if (!parsed) {
        this.logger.error(`Output AI tidak bisa diparse. Mentah: ${text.slice(0, 200)}`);
        return this.manualReviewResult('Output AI tidak valid — perlu review manual');
      }

      this.logger.log(
        `AI Authentication: ${metadata.brand} ${metadata.category} → skor ${parsed.score}/100`,
      );
      return parsed;
    } catch (e) {
      const message = e instanceof Error ? e.message : String(e);
      this.logger.error(`Panggilan AI gagal: ${message}`);
      return this.manualReviewResult(`AI error: ${message}`);
    }
  }

  private parseResult(raw: string): AiAuthResult | null {
    try {
      // LLM kadang membungkus JSON dalam ```json ... ``` — bersihkan dulu.
      const cleaned = raw
        .trim()
        .replace(/^```(?:json)?\s*/i, '')
        .replace(/\s*```$/, '')
        .trim();
      const start = cleaned.indexOf('{');
      const end = cleaned.lastIndexOf('}');
      const jsonStr = start >= 0 && end > start ? cleaned.slice(start, end + 1) : cleaned;

      const data = JSON.parse(jsonStr);
      const score = Number(data.score);
      if (!Number.isFinite(score)) return null;
      return {
        score: Math.min(100, Math.max(0, Math.round(score))),
        conditionSummary: String(data.conditionSummary ?? ''),
        reasons: Array.isArray(data.reasons) ? data.reasons.map(String) : [],
        flags: Array.isArray(data.flags) ? data.flags.map(String) : [],
      };
    } catch {
      return null;
    }
  }

  /** Skor 0 memaksa jalur flagged_for_review di ItemsService — aman by default */
  private manualReviewResult(reason: string): AiAuthResult {
    return {
      score: 0,
      conditionSummary: 'Perlu review manual',
      reasons: [reason],
      flags: ['manual_review'],
    };
  }

  private mockResult(
    photos: Express.Multer.File[],
    metadata: { brand: string; category: string },
  ): AiAuthResult {
    return {
      score: 92,
      conditionSummary: `[MOCK] ${metadata.brand} ${metadata.category}, ${photos.length} foto — kondisi baik`,
      reasons: ['hasil mock — set AI_API_KEY untuk analisis sungguhan'],
      flags: [],
    };
  }
}
