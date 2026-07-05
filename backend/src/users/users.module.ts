import { Module } from '@nestjs/common';
import { BlockchainModule } from '../blockchain/blockchain.module';
import { UsersController } from './users.controller';

@Module({
  imports: [BlockchainModule],
  controllers: [UsersController],
})
export class UsersModule {}
