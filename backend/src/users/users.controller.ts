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
}
