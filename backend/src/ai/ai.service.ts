import Anthropic from '@anthropic-ai/sdk';
import { Injectable, Logger } from '@nestjs/common';
import { ConfigService } from '@nestjs/config';

export interface AiAuthResult {
  /** Skor keaslian 0-100 (FR-03) */
  score: number;
  conditionSummary: string;
  reasons: string[];
  flags: string[];
}

const SUPPORTED_IMAGE_TYPES = ['image/jpeg', 'image/png', 'image/gif', 'image/webp'];

/**
 * JSON schema untuk structured outputs — respons dijamin valid terhadap schema
 * ini, tidak perlu parsing defensif berlebihan. Batas nilai (0-100) tidak bisa
 * dinyatakan di schema (numerical constraints tidak didukung), jadi di-clamp
 * saat parsing.
 */
const OUTPUT_SCHEMA = {
  type: 'object',
  properties: {
    score: {
      type: 'integer',
      description:
        'Skor keaslian 0-100. 0 = hampir pasti palsu, 100 = hampir pasti asli. ' +
        'Konservatif: bukti visual kurang = skor turun, bukan naik.',
    },
    conditionSummary: {
      type: 'string',
      description: 'Ringkasan kondisi barang, 1-2 kalimat Bahasa Indonesia',
    },
    reasons: {
      type: 'array',
      items: { type: 'string' },
      description: 'Alasan spesifik pendukung skor (logo, jahitan, material, label, dst.)',
    },
    flags: {
      type: 'array',
      items: { type: 'string' },
      description: 'Kejanggalan yang butuh review manual; array kosong jika tidak ada',
    },
  },
  required: ['score', 'conditionSummary', 'reasons', 'flags'],
  additionalProperties: false,
} as const;

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

/** Part 3 — AI Authentication Engine (PRD §9.4) */
@Injectable()
export class AiService {
  private readonly logger = new Logger(AiService.name);
  private readonly client?: Anthropic;
  private readonly model: string;

  constructor(config: ConfigService) {
    this.model = config.get<string>('AI_MODEL') ?? 'claude-opus-4-8';
    const apiKey = config.get<string>('ANTHROPIC_API_KEY');
    if (apiKey) {
      this.client = new Anthropic({ apiKey });
      this.logger.log(`AI Authentication aktif (model: ${this.model})`);
    } else {
      this.logger.warn('ANTHROPIC_API_KEY belum diset — AiService jalan di MOCK MODE');
    }
  }

  async analyzeItem(
    photos: Express.Multer.File[],
    metadata: { brand: string; category: string; serialNumber?: string },
  ): Promise<AiAuthResult> {
    if (!this.client) return this.mockResult(photos, metadata);

    const imageBlocks: Anthropic.ImageBlockParam[] = photos
      .filter((f) => SUPPORTED_IMAGE_TYPES.includes(f.mimetype))
      .slice(0, 5)
      .map((f) => ({
        type: 'image',
        source: {
          type: 'base64',
          media_type: f.mimetype as 'image/jpeg' | 'image/png' | 'image/gif' | 'image/webp',
          data: f.buffer.toString('base64'),
        },
      }));

    if (imageBlocks.length === 0) {
      return this.manualReviewResult('Tidak ada foto dengan format yang didukung (JPEG/PNG/GIF/WebP)');
    }

    const response = await this.client.messages.create({
      model: this.model,
      max_tokens: 16000,
      thinking: { type: 'adaptive' },
      system: SYSTEM_PROMPT,
      output_config: {
        format: { type: 'json_schema', schema: OUTPUT_SCHEMA },
      },
      messages: [
        {
          role: 'user',
          content: [
            ...imageBlocks,
            {
              type: 'text',
              text:
                `Nilai keaslian barang berikut berdasarkan foto di atas.\n` +
                `Brand: ${metadata.brand}\n` +
                `Kategori: ${metadata.category}\n` +
                `Serial number (klaim penjual): ${metadata.serialNumber || 'tidak dicantumkan'}`,
            },
          ],
        },
      ],
    });

    // Safety classifier bisa menolak request — jangan baca content sebelum cek ini
    if (response.stop_reason === 'refusal') {
      this.logger.warn('AI menolak menganalisis foto (stop_reason: refusal)');
      return this.manualReviewResult('AI menolak menganalisis — perlu review manual');
    }

    const textBlock = response.content.find(
      (b): b is Anthropic.TextBlock => b.type === 'text',
    );
    const parsed = textBlock ? this.parseResult(textBlock.text) : null;
    if (!parsed) {
      this.logger.error('Output AI tidak bisa diparse sebagai AiAuthResult');
      return this.manualReviewResult('Output AI tidak valid — perlu review manual');
    }

    this.logger.log(
      `AI Authentication: ${metadata.brand} ${metadata.category} → skor ${parsed.score}/100`,
    );
    return parsed;
  }

  private parseResult(raw: string): AiAuthResult | null {
    try {
      const data = JSON.parse(raw);
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
      reasons: ['hasil mock — set ANTHROPIC_API_KEY untuk analisis sungguhan'],
      flags: [],
    };
  }
}
