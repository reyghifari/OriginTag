import { Controller, Get, Param } from '@nestjs/common';
import { BlockchainService } from '../blockchain/blockchain.service';

@Controller('users')
export class UsersController {
  constructor(private readonly blockchain: BlockchainService) {}

  /** FR-09: semua passport milik wallet — sumber data dashboard "Barang Saya" */
  @Get(':walletAddress/passports')
  getPassports(@Param('walletAddress') walletAddress: string) {
    return this.blockchain.getPassportsByOwner(walletAddress);
  }

  /** Statistik & trust score untuk halaman profil */
  @Get(':walletAddress/stats')
  getStats(@Param('walletAddress') walletAddress: string) {
    return this.blockchain.getStats(walletAddress);
  }

  /** Saldo tBNB wallet (untuk profil / info gas) */
  @Get(':walletAddress/balance')
  getBalance(@Param('walletAddress') walletAddress: string) {
    return this.blockchain.getBalance(walletAddress);
  }
}
