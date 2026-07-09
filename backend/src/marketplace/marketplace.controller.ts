import { Body, Controller, Get, Param, Post, Query } from '@nestjs/common';
import { IsNotEmpty, IsNumberString } from 'class-validator';
import { parseEther } from 'ethers';
import { BlockchainService } from '../blockchain/blockchain.service';

class ListDto {
  /** Harga jual dalam BNB (mis. "0.5") */
  @IsNotEmpty()
  @IsNumberString()
  priceBnb!: string;
}

/** Marketplace (reference) + explore/discovery. */
@Controller()
export class MarketplaceController {
  constructor(private readonly blockchain: BlockchainService) {}

  /** Semua passport yang sedang dijual */
  @Get('marketplace')
  marketplace() {
    return this.blockchain.listMarketplace();
  }

  /** Galeri/telusuri semua passport terverifikasi (filter opsional) */
  @Get('explore')
  async explore(
    @Query('brand') brand?: string,
    @Query('category') category?: string,
    @Query('minScore') minScore?: string,
  ) {
    let all = await this.blockchain.listAllPassports();
    if (brand) all = all.filter((p) => p.brand.toLowerCase().includes(brand.toLowerCase()));
    if (category)
      all = all.filter((p) => p.category.toLowerCase().includes(category.toLowerCase()));
    if (minScore) {
      const min = Number(minScore);
      all = all.filter((p) => p.authenticityScore >= min);
    }
    return all;
  }

  /** Calldata approve marketplace untuk sebuah token (langkah 1 sebelum list) */
  @Post('marketplace/:tokenId/approve-tx')
  approveTx(@Param('tokenId') tokenId: string) {
    return this.blockchain.buildApproveTx(tokenId);
  }

  /** Calldata listItem (langkah 2) */
  @Post('marketplace/:tokenId/list-tx')
  listTx(@Param('tokenId') tokenId: string, @Body() dto: ListDto) {
    const priceWei = parseEther(dto.priceBnb).toString();
    return this.blockchain.buildListTx(tokenId, priceWei);
  }

  /** Calldata buyItem (value = harga) */
  @Get('marketplace/:tokenId/buy-tx')
  buyTx(@Param('tokenId') tokenId: string) {
    return this.blockchain.buildBuyTx(tokenId);
  }

  /** Calldata cancelListing */
  @Post('marketplace/:tokenId/cancel-tx')
  cancelTx(@Param('tokenId') tokenId: string) {
    return this.blockchain.buildCancelTx(tokenId);
  }
}
