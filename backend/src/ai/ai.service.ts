import { Injectable, Logger } from '@nestjs/common';

export interface AiAuthResult {
  /** Skor keaslian 0-100 (FR-03) */
  score: number;
  conditionSummary: string;
  reasons: string[];
  flags: string[];
}

/** Part 3 — AI Authentication Engine (PRD §9.4) */
@Injectable()
export class AiService {
  private readonly logger = new Logger(AiService.name);

  /**
   * TODO(Part 3): implementasi via @anthropic-ai/sdk (vision multimodal):
   *   1. Encode foto ke base64, kirim bersama brand/kategori/serial number.
   *   2. Prompt: analisis logo, jahitan, material, konsistensi serial number
   *      vs ciri khas brand — minta output JSON persis bentuk AiAuthResult.
   *   3. Parse + clamp skor 0-100; kegagalan parse → flag manual review.
   */
  async analyzeItem(
    photos: Express.Multer.File[],
    metadata: { brand: string; category: string; serialNumber?: string },
  ): Promise<AiAuthResult> {
    this.logger.warn('AiService.analyzeItem masih MOCK — implementasi di Part 3');
    return {
      score: 92,
      conditionSummary: `[MOCK] ${metadata.brand} ${metadata.category}, ${photos.length} foto — kondisi baik`,
      reasons: ['hasil mock, AI belum diimplementasi (Part 3)'],
      flags: [],
    };
  }
}
