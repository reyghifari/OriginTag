import { Client, Long, VisibilityType } from '@bnb-chain/greenfield-js-sdk';
import { Injectable, Logger } from '@nestjs/common';
import { ConfigService } from '@nestjs/config';
import { computeAddress } from 'ethers';
import { AiAuthResult } from '../ai/ai.service';

interface EvidenceUpload {
  /** URI folder Greenfield yang disimpan di metadata NFT (FR-05) */
  evidenceObjectId: string;
  photoObjects: string[];
  reportObject: string;
}

/**
 * Part 4 — BNB Greenfield storage (PRD §9.3).
 * Tanpa GREENFIELD_PRIVATE_KEY di .env, service jalan MOCK MODE (tidak upload
 * betulan) supaya dev backend/Android bisa jalan paralel.
 */
@Injectable()
export class GreenfieldService {
  private readonly logger = new Logger(GreenfieldService.name);
  private readonly client?: Client;
  private readonly privateKey?: string;
  private readonly address?: string;
  private readonly bucket: string;

  constructor(config: ConfigService) {
    this.bucket = config.get<string>('GREENFIELD_BUCKET') ?? 'origintag-evidence';
    const rpc = config.get<string>('GREENFIELD_RPC_URL');
    const chainId = config.get<string>('GREENFIELD_CHAIN_ID');
    const pk = config.get<string>('GREENFIELD_PRIVATE_KEY');

    if (rpc && chainId && pk) {
      this.privateKey = pk.startsWith('0x') ? pk : `0x${pk}`;
      this.address = computeAddress(this.privateKey);
      this.client = Client.create(rpc, chainId);
      this.logger.log(`Greenfield aktif (bucket: ${this.bucket}, akun: ${this.address})`);
    } else {
      this.logger.warn('GREENFIELD_PRIVATE_KEY belum diset — GreenfieldService MOCK MODE');
    }
  }

  /**
   * Simpan foto + ai-report.json ke Greenfield per struktur PRD §9.3:
   *   {draftId}/photos/{n}.jpg   — foto asli
   *   {draftId}/ai-report.json   — hasil AI Authentication
   * Mengembalikan objectId untuk direferensikan di metadata NFT.
   */
  async uploadEvidence(
    draftId: string,
    photos: Express.Multer.File[],
    aiReport: AiAuthResult,
  ): Promise<string> {
    if (!this.client || !this.privateKey) {
      this.logger.warn('uploadEvidence MOCK — set GREENFIELD_PRIVATE_KEY untuk upload sungguhan');
      return `mock-greenfield://${this.bucket}/${draftId}`;
    }

    await this.ensureBucket();

    const uploaded: EvidenceUpload = {
      evidenceObjectId: `greenfield://${this.bucket}/${draftId}`,
      photoObjects: [],
      reportObject: '',
    };

    for (let i = 0; i < photos.length; i++) {
      const ext = this.extFromMime(photos[i].mimetype);
      const objectName = `${draftId}/photos/${i}.${ext}`;
      await this.putObject(objectName, photos[i].buffer, photos[i].mimetype, VisibilityType.VISIBILITY_TYPE_PRIVATE);
      uploaded.photoObjects.push(objectName);
    }

    const reportName = `${draftId}/ai-report.json`;
    await this.putObject(
      reportName,
      Buffer.from(JSON.stringify(aiReport, null, 2)),
      'application/json',
      // Metadata publik boleh dibaca siapa saja; foto asli tetap privat (PRD §15)
      VisibilityType.VISIBILITY_TYPE_PUBLIC_READ,
    );
    uploaded.reportObject = reportName;

    this.logger.log(
      `Bukti tersimpan di Greenfield: ${uploaded.photoObjects.length} foto + ai-report.json (${draftId})`,
    );
    return uploaded.evidenceObjectId;
  }

  private async putObject(
    objectName: string,
    content: Buffer,
    contentType: string,
    visibility: VisibilityType,
  ): Promise<void> {
    // delegateUploadObject: SP membuat object on-chain + upload dalam satu panggilan
    // memakai ECDSA auth (tanpa off-chain seed). Butuh SP delegated agent aktif.
    const res = await this.client!.object.delegateUploadObject(
      {
        bucketName: this.bucket,
        objectName,
        body: {
          name: objectName,
          type: contentType,
          size: content.length,
          content,
        },
        delegatedOpts: { visibility },
      },
      { type: 'ECDSA', privateKey: this.privateKey! },
    );

    if (res.code !== 0) {
      throw new Error(`Gagal upload ${objectName} ke Greenfield: ${res.message} (code ${res.code})`);
    }
  }

  /** Buat bucket sekali di awal jika belum ada. */
  private async ensureBucket(): Promise<void> {
    try {
      await this.client!.bucket.headBucket(this.bucket);
      return; // sudah ada
    } catch {
      // belum ada — lanjut buat
    }

    const sps = await this.client!.sp.getStorageProviders();
    if (sps.length === 0) throw new Error('Tidak ada Storage Provider tersedia di Greenfield');
    const sp = sps[0];

    const tx = await this.client!.bucket.createBucket({
      bucketName: this.bucket,
      creator: this.address!,
      visibility: VisibilityType.VISIBILITY_TYPE_PUBLIC_READ,
      chargedReadQuota: Long.fromInt(0),
      paymentAddress: this.address!,
      primarySpAddress: sp.operatorAddress,
    });

    const res = await tx.broadcast({
      denom: 'BNB',
      gasLimit: Number(210000),
      gasPrice: '5000000000',
      payer: this.address!,
      granter: '',
      privateKey: this.privateKey!,
    });

    if (res.code !== 0) {
      throw new Error(`Gagal membuat bucket ${this.bucket}: code ${res.code}`);
    }
    this.logger.log(`Bucket ${this.bucket} dibuat di Greenfield`);
  }

  private extFromMime(mime: string): string {
    const map: Record<string, string> = {
      'image/jpeg': 'jpg',
      'image/png': 'png',
      'image/gif': 'gif',
      'image/webp': 'webp',
    };
    return map[mime] ?? 'bin';
  }
}
