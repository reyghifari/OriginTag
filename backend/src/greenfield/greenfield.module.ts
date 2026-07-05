import { Module } from '@nestjs/common';
import { GreenfieldService } from './greenfield.service';

@Module({
  providers: [GreenfieldService],
  exports: [GreenfieldService],
})
export class GreenfieldModule {}
