import { Module } from '@nestjs/common';
import { AiModule } from '../ai/ai.module';
import { BlockchainModule } from '../blockchain/blockchain.module';
import { GreenfieldModule } from '../greenfield/greenfield.module';
import { ItemsController } from './items.controller';
import { ItemsService } from './items.service';

@Module({
  imports: [AiModule, GreenfieldModule, BlockchainModule],
  controllers: [ItemsController],
  providers: [ItemsService],
  exports: [ItemsService],
})
export class ItemsModule {}
