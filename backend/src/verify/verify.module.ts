import { Module } from '@nestjs/common';
import { ItemsModule } from '../items/items.module';
import { VerifyController } from './verify.controller';

@Module({
  imports: [ItemsModule],
  controllers: [VerifyController],
})
export class VerifyModule {}
