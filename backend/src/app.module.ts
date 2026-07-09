import { Module } from '@nestjs/common';
import { ConfigModule } from '@nestjs/config';
import { ItemsModule } from './items/items.module';
import { MarketplaceModule } from './marketplace/marketplace.module';
import { UsersModule } from './users/users.module';
import { VerifyModule } from './verify/verify.module';

@Module({
  imports: [
    ConfigModule.forRoot({ isGlobal: true }),
    ItemsModule,
    MarketplaceModule,
    UsersModule,
    VerifyModule,
  ],
})
export class AppModule {}
