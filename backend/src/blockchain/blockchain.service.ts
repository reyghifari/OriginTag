import { Injectable, Logger, NotFoundException, OnModuleInit } from '@nestjs/common';
import { ConfigService } from '@nestjs/config';
import { Contract, JsonRpcProvider, Wallet, formatEther } from 'ethers';
import { ORIGINTAG_MARKETPLACE_ABI, ORIGINTAG_PASSPORT_ABI } from './abi';

export interface MarketplaceListing extends PassportView {
  price: string; // wei
  priceBnb: string;
  seller: string;
}

export interface RecallInfo {
  brand: string;
  category: string;
  reason: string;
}

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
export class BlockchainService implements OnModuleInit {
  private readonly logger = new Logger(BlockchainService.name);
  private contract?: Contract;
  private marketplace?: Contract;
  private provider?: JsonRpcProvider;
  private passportAddress?: string;
  private marketplaceAddress?: string;

  // Blok awal untuk queryFilter (recall/service record) — dihindari range RPC berlebihan
  private startBlock = 0;
  // Cache recall (event) — refresh berkala
  private recallsCache: { data: RecallInfo[]; ts: number } = { data: [], ts: 0 };
  private static readonly RECALL_TTL_MS = 30_000;

  // Sumber utama recall & service record: yang dipicu backend sendiri (in-memory).
  // RPC publik BSC membatasi eth_getLogs, jadi getLogs hanya pelengkap best-effort.
  private readonly triggeredRecalls: RecallInfo[] = [];
  private readonly triggeredServiceRecords = new Map<string, string[]>();

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
    const marketAddress = config.get<string>('MARKETPLACE_ADDRESS');

    if (rpc && address && pk) {
      this.provider = new JsonRpcProvider(rpc);
      const wallet = new Wallet(pk, this.provider);
      this.contract = new Contract(address, ORIGINTAG_PASSPORT_ABI, wallet);
      this.passportAddress = address;
      this.logger.log(`Terhubung ke kontrak ${address}`);
      if (marketAddress) {
        this.marketplace = new Contract(marketAddress, ORIGINTAG_MARKETPLACE_ABI, wallet);
        this.marketplaceAddress = marketAddress;
        this.logger.log(`Marketplace ${marketAddress}`);
      }
    } else {
      this.logger.warn('CONTRACT_ADDRESS/AUTHENTICATOR_PRIVATE_KEY belum diset — MOCK MODE aktif');
    }
  }

  async onModuleInit() {
    if (this.provider) {
      // Blok saat backend start — recall/service record demo terjadi setelah ini.
      // Lookback aman agar aktivitas sesi sebelum restart tetap tertangkap.
      const latest = await this.provider.getBlockNumber().catch(() => 0);
      this.startBlock = Math.max(0, latest - 200_000);
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

  // ── Explore / Marketplace ────────────────────────────────────────

  /** Semua passport (enumerate via ERC721Enumerable) — untuk explore. */
  async listAllPassports(): Promise<PassportView[]> {
    if (!this.contract) {
      return [...this.mockStore.values()].map((p) => this.mockToView(p));
    }
    const total = Number(await this.contract.totalSupply());
    const ids = await Promise.all(
      Array.from({ length: total }, (_, i) => this.contract!.tokenByIndex(i)),
    );
    return Promise.all(ids.map((id) => this.getPassport(String(id))));
  }

  /** Listing aktif untuk 1 token, atau null. */
  async getListing(
    tokenId: string,
  ): Promise<{ seller: string; price: string; active: boolean } | null> {
    if (!this.marketplace) return null;
    const l = await this.marketplace.getListing(tokenId);
    if (!l.active) return null;
    return { seller: l.seller, price: l.price.toString(), active: l.active };
  }

  /** Semua passport yang sedang dijual (dengan harga + data passport). */
  async listMarketplace(): Promise<MarketplaceListing[]> {
    if (!this.marketplace) return [];
    const all = await this.listAllPassports();
    const results: MarketplaceListing[] = [];
    await Promise.all(
      all.map(async (p) => {
        const listing = await this.getListing(p.tokenId);
        if (listing) {
          results.push({
            ...p,
            price: listing.price,
            priceBnb: formatEther(listing.price),
            seller: listing.seller,
          });
        }
      }),
    );
    return results;
  }

  // ── Tx builders (ditandatangani wallet user di app) ──────────────

  buildApproveTx(tokenId: string) {
    if (!this.contract || !this.marketplaceAddress) throw new Error('Marketplace tidak aktif');
    const data = this.contract.interface.encodeFunctionData('approve', [
      this.marketplaceAddress,
      tokenId,
    ]);
    return { unsignedTx: { to: this.passportAddress, data }, note: 'Approve marketplace' };
  }

  buildListTx(tokenId: string, priceWei: string) {
    if (!this.marketplace) throw new Error('Marketplace tidak aktif');
    const data = this.marketplace.interface.encodeFunctionData('listItem', [tokenId, priceWei]);
    return { unsignedTx: { to: this.marketplaceAddress, data }, note: 'List passport dijual' };
  }

  async buildBuyTx(tokenId: string) {
    if (!this.marketplace) throw new Error('Marketplace tidak aktif');
    const listing = await this.getListing(tokenId);
    if (!listing) throw new NotFoundException('Passport tidak sedang dijual');
    const data = this.marketplace.interface.encodeFunctionData('buyItem', [tokenId]);
    return {
      unsignedTx: { to: this.marketplaceAddress, data, value: listing.price },
      note: 'Beli passport (bayar harga)',
    };
  }

  buildCancelTx(tokenId: string) {
    if (!this.marketplace) throw new Error('Marketplace tidak aktif');
    const data = this.marketplace.interface.encodeFunctionData('cancelListing', [tokenId]);
    return { unsignedTx: { to: this.marketplaceAddress, data }, note: 'Batalkan listing' };
  }

  // ── Recall (FR-11) ───────────────────────────────────────────────

  async getRecalls(): Promise<RecallInfo[]> {
    if (!this.contract) return [];
    // Mulai dari yang dipicu backend (selalu tersedia).
    const merged = new Map<string, RecallInfo>();
    for (const r of this.triggeredRecalls) merged.set(`${r.brand}|${r.category}|${r.reason}`, r);

    // Lengkapi dengan getLogs (best-effort; RPC publik sering menolak).
    const now = Date.now();
    if (now - this.recallsCache.ts >= BlockchainService.RECALL_TTL_MS) {
      try {
        const events = await this.contract.queryFilter(
          this.contract.filters.RecallIssued(),
          this.startBlock,
          'latest',
        );
        this.recallsCache = {
          data: events.map((e: any) => ({
            brand: e.args.brand,
            category: e.args.category,
            reason: e.args.reason,
          })),
          ts: now,
        };
      } catch {
        this.recallsCache = { data: this.recallsCache.data, ts: now }; // jangan spam log
      }
    }
    for (const r of this.recallsCache.data) merged.set(`${r.brand}|${r.category}|${r.reason}`, r);
    return [...merged.values()];
  }

  async isRecalled(brand: string, category: string): Promise<RecallInfo | null> {
    const recalls = await this.getRecalls();
    return recalls.find((r) => r.brand === brand && r.category === category) ?? null;
  }

  /** Backend (AUTHENTICATOR_ROLE) memicu recall brand+kategori. */
  async issueRecall(brand: string, category: string, reason: string): Promise<{ txHash: string }> {
    if (!this.contract) throw new Error('Kontrak tidak aktif');
    const tx = await this.contract.issueRecall(brand, category, reason);
    const receipt = await tx.wait();
    this.triggeredRecalls.push({ brand, category, reason });
    this.recallsCache.ts = 0; // invalidasi cache getLogs
    return { txHash: receipt.hash };
  }

  // ── Service records (FR-10) ──────────────────────────────────────

  async getServiceRecords(tokenId: string): Promise<string[]> {
    if (!this.contract) return [];
    const local = this.triggeredServiceRecords.get(tokenId) ?? [];
    // Lengkapi dengan getLogs (best-effort).
    try {
      const events = await this.contract.queryFilter(
        this.contract.filters.ServiceRecordAdded(tokenId),
        this.startBlock,
        'latest',
      );
      const merged = new Set<string>(local);
      for (const e of events as any[]) merged.add(e.args.recordObjectId);
      return [...merged];
    } catch {
      return local;
    }
  }

  /** Backend (SERVICE_ROLE) menambah entri riwayat servis. */
  async addServiceRecord(tokenId: string, recordObjectId: string): Promise<{ txHash: string }> {
    if (!this.contract) throw new Error('Kontrak tidak aktif');
    const tx = await this.contract.addServiceRecord(tokenId, recordObjectId);
    const receipt = await tx.wait();
    const list = this.triggeredServiceRecords.get(tokenId) ?? [];
    list.push(recordObjectId);
    this.triggeredServiceRecords.set(tokenId, list);
    return { txHash: receipt.hash };
  }

  // ── Wallet / profil ──────────────────────────────────────────────

  async getBalance(address: string): Promise<{ wei: string; bnb: string }> {
    if (!this.provider) return { wei: '0', bnb: '0' };
    const wei = await this.provider.getBalance(address);
    return { wei: wei.toString(), bnb: formatEther(wei) };
  }

  async getStats(address: string): Promise<{
    owned: number;
    avgScore: number;
    highScoreCount: number;
    salesCount: number;
    trustScore: number;
    trustLabel: string;
  }> {
    const owned = await this.getPassportsByOwner(address).catch(() => []);
    const scores = owned.map((p) => p.authenticityScore);
    const avgScore = scores.length
      ? Math.round(scores.reduce((a, b) => a + b, 0) / scores.length)
      : 0;
    const highScoreCount = scores.filter((s) => s >= 90).length;

    let salesCount = 0;
    if (this.marketplace) {
      try {
        const events = await this.marketplace.queryFilter(
          this.marketplace.filters.ItemSold(null, address),
          this.startBlock,
          'latest',
        );
        salesCount = events.length;
      } catch {
        /* abaikan */
      }
    }

    const trustLabel =
      avgScore >= 85 ? 'Terpercaya' : avgScore >= 70 ? 'Baik' : owned.length ? 'Pemula' : 'Baru';
    return { owned: owned.length, avgScore, highScoreCount, salesCount, trustScore: avgScore, trustLabel };
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
