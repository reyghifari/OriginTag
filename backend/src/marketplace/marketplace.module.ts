import { Module } from '@nestjs/common';
import { BlockchainModule } from '../blockchain/blockchain.module';
import { MarketplaceController } from './marketplace.controller';

@Module({
  imports: [BlockchainModule],
  controllers: [MarketplaceController],
})
export class MarketplaceModule {}
