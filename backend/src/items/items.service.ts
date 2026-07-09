import { BadRequestException, Injectable } from '@nestjs/common';
import { ConfigService } from '@nestjs/config';
import { randomUUID } from 'crypto';
import { AiService } from '../ai/ai.service';
import { BlockchainService } from '../blockchain/blockchain.service';
import { GreenfieldService } from '../greenfield/greenfield.service';
import { RegisterItemDto, TransferItemDto } from './dto/items.dto';

/** Orkestrasi utama: flow registrasi & minting sesuai PRD §11.1 */
@Injectable()
export class ItemsService {
  constructor(
    private readonly config: ConfigService,
    private readonly ai: AiService,
    private readonly greenfield: GreenfieldService,
    private readonly blockchain: BlockchainService,
  ) {}

  async register(dto: RegisterItemDto, photos: Express.Multer.File[]) {
    if (photos.length < 1) {
      throw new BadRequestException('Minimal 1 foto wajib diunggah (FR-02)');
    }

    // 1. AI Authentication (Part 3)
    const aiResult = await this.ai.analyzeItem(photos, dto);

    // 2. Di bawah threshold → manual review, tidak mint (PRD §6.1)
    const threshold = Number(this.config.get('AUTH_SCORE_THRESHOLD') ?? 70);
    if (aiResult.score < threshold) {
      return { status: 'flagged_for_review', aiResult };
    }

    // 3. Simpan bukti ke Greenfield (Part 4) — FR-05
    const draftId = randomUUID();
    const evidenceObjectId = await this.greenfield.uploadEvidence(draftId, photos, aiResult);

    // 4. Mint passport NFT via wallet AUTHENTICATOR_ROLE backend (Part 1)
    const { tokenId, txHash } = await this.blockchain.mintPassport({
      to: dto.ownerAddress,
      brand: dto.brand,
      category: dto.category,
      serialNumber: dto.serialNumber ?? '',
      warrantyDurationSeconds: dto.warrantyDurationDays * 24 * 60 * 60,
      authenticityScore: aiResult.score,
      evidenceObjectId,
    });

    // Di Render, RENDER_EXTERNAL_URL diisi otomatis dengan URL publik service.
    const baseUrl =
      this.config.get('PUBLIC_VERIFY_BASE_URL') ??
      this.config.get('RENDER_EXTERNAL_URL') ??
      'http://localhost:3000';
    return {
      status: 'minted',
      tokenId,
      txHash,
      aiResult,
      verifyUrl: `${baseUrl}/verify/${tokenId}`,
    };
  }

  /** Passport publik + status recall (untuk banner di app & halaman verify). */
  async getPublicPassport(tokenId: string) {
    const passport = await this.blockchain.getPassport(tokenId);
    const recall = await this.blockchain.isRecalled(passport.brand, passport.category);
    return { ...passport, recall };
  }

  /** Daftar riwayat servis (FR-10) */
  async getServiceRecords(tokenId: string) {
    return { records: await this.blockchain.getServiceRecords(tokenId) };
  }

  /** Tambah riwayat servis (backend SERVICE_ROLE) */
  addServiceRecord(tokenId: string, note: string) {
    return this.blockchain.addServiceRecord(tokenId, note);
  }

  /** Trigger recall brand+kategori (backend AUTHENTICATOR_ROLE) */
  issueRecall(brand: string, category: string, reason: string) {
    return this.blockchain.issueRecall(brand, category, reason);
  }

  /**
   * MVP: transfer harus ditandatangani wallet pemilik NFT (validasi on-chain, PRD §8.1).
   * Backend hanya menyusun calldata; signing terjadi di app Android (Part 5d).
   */
  transfer(tokenId: string, dto: TransferItemDto) {
    return this.blockchain.buildTransferTx(tokenId, dto.toAddress);
  }

  async getWarranty(tokenId: string) {
    const remainingSeconds = await this.blockchain.getRemainingWarranty(tokenId);
    return { tokenId, remainingSeconds };
  }

  /** Jumlah foto bukti untuk passport ini (di Greenfield). */
  async getPhotoCount(tokenId: string): Promise<{ count: number }> {
    const passport = await this.blockchain.getPassport(tokenId);
    const draftId = this.draftIdFromEvidence(passport.evidenceObjectId);
    if (!draftId) return { count: 0 };
    return { count: await this.greenfield.countPhotos(draftId) };
  }

  /** Unduh 1 foto bukti (proxy dari Greenfield — foto disimpan privat). */
  async getPhoto(
    tokenId: string,
    index: number,
  ): Promise<{ data: Buffer; contentType: string } | null> {
    const passport = await this.blockchain.getPassport(tokenId);
    const draftId = this.draftIdFromEvidence(passport.evidenceObjectId);
    if (!draftId) return null;
    // App mengompres semua foto ke JPEG sebelum upload → ekstensi selalu .jpg
    return this.greenfield.downloadObject(`${draftId}/photos/${index}.jpg`);
  }

  /** Ambil draftId dari URI greenfield://bucket/{draftId} (null jika mock/kosong). */
  private draftIdFromEvidence(uri?: string): string | null {
    const m = uri?.match(/^greenfield:\/\/[^/]+\/(.+)$/);
    return m ? m[1] : null;
  }
}
