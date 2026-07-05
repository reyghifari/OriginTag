import { Injectable, Logger, NotFoundException } from '@nestjs/common';
import { ConfigService } from '@nestjs/config';
import { Contract, JsonRpcProvider, Wallet } from 'ethers';
import { ORIGINTAG_PASSPORT_ABI } from './abi';

export interface MintParams {
  to: string;
  brand: string;
  category: string;
  serialNumber: string;
  warrantyDurationSeconds: number;
  authenticityScore: number;
  evidenceObjectId: string;
}

export interface PassportView {
  tokenId: string;
  owner: string;
  brand: string;
  category: string;
  serialNumber: string;
  authenticityScore: number;
  evidenceObjectId: string;
  remainingWarrantySeconds: number;
  ownershipHistory: string[];
  isFlaggedForReview: boolean;
}

/**
 * Part 2 — wrapper kontrak OriginTagPassport (ethers v6).
 * Tanpa BSC_RPC_URL + CONTRACT_ADDRESS + AUTHENTICATOR_PRIVATE_KEY di .env,
 * service jalan di MOCK MODE (in-memory) supaya app Android bisa dev paralel.
 */
@Injectable()
export class BlockchainService {
  private readonly logger = new Logger(BlockchainService.name);
  private contract?: Contract;

  // ── mock mode state ──
  private mockTokenCounter = 0;
  private readonly mockStore = new Map<
    string,
    MintParams & { tokenId: string; owner: string; ownershipHistory: string[]; mintedAtMs: number }
  >();

  constructor(config: ConfigService) {
    const rpc = config.get<string>('BSC_RPC_URL');
    const address = config.get<string>('CONTRACT_ADDRESS');
    const pk = config.get<string>('AUTHENTICATOR_PRIVATE_KEY');

    if (rpc && address && pk) {
      const provider = new JsonRpcProvider(rpc);
      const wallet = new Wallet(pk, provider);
      this.contract = new Contract(address, ORIGINTAG_PASSPORT_ABI, wallet);
      this.logger.log(`Terhubung ke kontrak ${address}`);
    } else {
      this.logger.warn('CONTRACT_ADDRESS/AUTHENTICATOR_PRIVATE_KEY belum diset — MOCK MODE aktif');
    }
  }

  async mintPassport(p: MintParams): Promise<{ tokenId: string; txHash: string }> {
    if (!this.contract) {
      const tokenId = String(this.mockTokenCounter++);
      this.mockStore.set(tokenId, {
        ...p,
        tokenId,
        owner: p.to,
        ownershipHistory: [p.to],
        mintedAtMs: Date.now(),
      });
      return { tokenId, txHash: `0xmock_mint_${tokenId}` };
    }

    const tx = await this.contract.mintPassport(
      p.to,
      p.brand,
      p.category,
      p.serialNumber,
      p.warrantyDurationSeconds,
      p.authenticityScore,
      p.evidenceObjectId,
    );
    const receipt = await tx.wait();

    // Ambil tokenId dari event PassportMinted
    const minted = receipt.logs
      .map((log: any) => {
        try {
          return this.contract!.interface.parseLog(log);
        } catch {
          return null;
        }
      })
      .find((e: any) => e?.name === 'PassportMinted');

    return {
      tokenId: minted ? String(minted.args.tokenId) : '',
      txHash: receipt.hash,
    };
  }

  async getPassport(tokenId: string): Promise<PassportView> {
    if (!this.contract) {
      const p = this.mockStore.get(tokenId);
      if (!p) throw new NotFoundException(`Passport ${tokenId} tidak ditemukan (mock)`);
      return this.mockToView(p);
    }

    const [p, owner, history, remaining] = await Promise.all([
      this.contract.getPassport(tokenId),
      this.contract.ownerOf(tokenId),
      this.contract.getOwnershipHistory(tokenId),
      this.contract.getRemainingWarranty(tokenId),
    ]);

    return {
      tokenId,
      owner,
      brand: p.brand,
      category: p.category,
      serialNumber: p.serialNumber,
      authenticityScore: Number(p.authenticityScore),
      evidenceObjectId: p.evidenceObjectId,
      remainingWarrantySeconds: Number(remaining),
      ownershipHistory: Array.from(history),
      isFlaggedForReview: p.isFlaggedForReview,
    };
  }

  /** FR-09: dashboard "Barang Saya" via balanceOf + tokenOfOwnerByIndex */
  async getPassportsByOwner(owner: string): Promise<PassportView[]> {
    if (!this.contract) {
      return [...this.mockStore.values()]
        .filter((p) => p.owner.toLowerCase() === owner.toLowerCase())
        .map((p) => this.mockToView(p));
    }

    const balance = Number(await this.contract.balanceOf(owner));
    const tokenIds = await Promise.all(
      Array.from({ length: balance }, (_, i) => this.contract!.tokenOfOwnerByIndex(owner, i)),
    );
    return Promise.all(tokenIds.map((id) => this.getPassport(String(id))));
  }

  async getRemainingWarranty(tokenId: string): Promise<number> {
    if (!this.contract) {
      const p = this.mockStore.get(tokenId);
      if (!p) throw new NotFoundException(`Passport ${tokenId} tidak ditemukan (mock)`);
      return this.mockRemainingWarranty(p);
    }
    return Number(await this.contract.getRemainingWarranty(tokenId));
  }

  /**
   * Transfer HARUS ditandatangani pemilik NFT — backend hanya menyusun calldata
   * `transferPassport(tokenId, to)` untuk di-sign wallet user di app (Part 5d).
   */
  async buildTransferTx(tokenId: string, to: string) {
    if (!this.contract) {
      const p = this.mockStore.get(tokenId);
      if (!p) throw new NotFoundException(`Passport ${tokenId} tidak ditemukan (mock)`);
      p.owner = to;
      p.ownershipHistory.push(to);
      return { txHash: `0xmock_transfer_${tokenId}`, note: 'mock mode: transfer langsung diterapkan' };
    }

    const data = this.contract.interface.encodeFunctionData('transferPassport', [tokenId, to]);
    return {
      unsignedTx: {
        to: await this.contract.getAddress(),
        data,
      },
      note: 'Sign & kirim transaksi ini dari wallet pemilik di app Android',
    };
  }

  private mockToView(
    p: MintParams & { tokenId: string; owner: string; ownershipHistory: string[]; mintedAtMs: number },
  ): PassportView {
    return {
      tokenId: p.tokenId,
      owner: p.owner,
      brand: p.brand,
      category: p.category,
      serialNumber: p.serialNumber,
      authenticityScore: p.authenticityScore,
      evidenceObjectId: p.evidenceObjectId,
      remainingWarrantySeconds: this.mockRemainingWarranty(p),
      ownershipHistory: p.ownershipHistory,
      isFlaggedForReview: false,
    };
  }

  private mockRemainingWarranty(p: { warrantyDurationSeconds: number; mintedAtMs: number }): number {
    const elapsed = Math.floor((Date.now() - p.mintedAtMs) / 1000);
    return Math.max(0, p.warrantyDurationSeconds - elapsed);
  }
}
