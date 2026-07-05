import { Injectable, Logger } from '@nestjs/common';
import { AiAuthResult } from '../ai/ai.service';

/** Part 4 — BNB Greenfield storage (PRD §9.3) */
@Injectable()
export class GreenfieldService {
  private readonly logger = new Logger(GreenfieldService.name);

  /**
   * TODO(Part 4): implementasi via @bnb-chain/greenfield-js-sdk:
   *   1. Pastikan bucket GREENFIELD_BUCKET ada (create sekali di awal).
   *   2. Upload per struktur PRD §9.3:
   *        {id}/photos/{n}.jpg        — foto asli (akses privat)
   *        {id}/ai-report.json        — hasil AI (metadata publik)
   *   3. Set permission granular: metadata publik vs evidence privat (PRD §15).
   *   4. Kembalikan objectId/hash untuk direferensikan di metadata NFT (FR-05).
   */
  async uploadEvidence(
    draftId: string,
    photos: Express.Multer.File[],
    aiReport: AiAuthResult,
  ): Promise<string> {
    this.logger.warn('GreenfieldService.uploadEvidence masih MOCK — implementasi di Part 4');
    return `mock-greenfield://origintag-evidence/${draftId}`;
  }
}
