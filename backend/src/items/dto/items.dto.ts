import { Type } from 'class-transformer';
import { IsInt, IsNotEmpty, IsOptional, IsString, Matches, Min } from 'class-validator';

const ETH_ADDRESS = /^0x[a-fA-F0-9]{40}$/;

export class RegisterItemDto {
  @IsString()
  @IsNotEmpty()
  brand!: string;

  @IsString()
  @IsNotEmpty()
  category!: string;

  @IsOptional()
  @IsString()
  serialNumber?: string;

  /** Tanggal beli asli, format ISO 8601 (field wajib FR-02) */
  @IsString()
  @IsNotEmpty()
  purchaseDate!: string;

  @Type(() => Number)
  @IsInt()
  @Min(0)
  warrantyDurationDays!: number;

  /** Wallet pemilik pertama — penerima NFT passport */
  @Matches(ETH_ADDRESS, { message: 'ownerAddress harus alamat EVM valid' })
  ownerAddress!: string;
}

export class TransferItemDto {
  @Matches(ETH_ADDRESS, { message: 'toAddress harus alamat EVM valid' })
  toAddress!: string;
}
