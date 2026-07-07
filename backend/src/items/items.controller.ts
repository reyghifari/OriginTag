import {
  Body,
  Controller,
  Get,
  Param,
  Post,
  Res,
  UploadedFiles,
  UseInterceptors,
} from '@nestjs/common';
import { FilesInterceptor } from '@nestjs/platform-express';
import { Response } from 'express';
import { RegisterItemDto, TransferItemDto } from './dto/items.dto';
import { ItemsService } from './items.service';

/** Endpoint sesuai PRD §12 */
@Controller('items')
export class ItemsController {
  constructor(private readonly items: ItemsService) {}

  /** Upload foto (field `photos`, max 5) + metadata → AI → Greenfield → mint */
  @Post('register')
  @UseInterceptors(FilesInterceptor('photos', 5))
  register(
    @UploadedFiles() photos: Express.Multer.File[],
    @Body() dto: RegisterItemDto,
  ) {
    return this.items.register(dto, photos ?? []);
  }

  /** Data passport publik — dipakai app & halaman verifikasi */
  @Get(':tokenId')
  getOne(@Param('tokenId') tokenId: string) {
    return this.items.getPublicPassport(tokenId);
  }

  /** Menyiapkan/memicu transfer on-chain (FR-07) */
  @Post(':tokenId/transfer')
  transfer(@Param('tokenId') tokenId: string, @Body() dto: TransferItemDto) {
    return this.items.transfer(tokenId, dto);
  }

  /** Sisa masa garansi terkini (FR-08) */
  @Get(':tokenId/warranty')
  warranty(@Param('tokenId') tokenId: string) {
    return this.items.getWarranty(tokenId);
  }

  /** Jumlah foto bukti passport (app pakai untuk membangun URL tiap foto) */
  @Get(':tokenId/photos')
  photos(@Param('tokenId') tokenId: string) {
    return this.items.getPhotoCount(tokenId);
  }

  /** Proxy 1 foto bukti dari Greenfield (foto disimpan privat, diunduh via key backend) */
  @Get(':tokenId/photo/:index')
  async photo(
    @Param('tokenId') tokenId: string,
    @Param('index') index: string,
    @Res() res: Response,
  ) {
    const file = await this.items.getPhoto(tokenId, Number(index));
    if (!file) {
      res.status(404).send('Foto tidak ditemukan');
      return;
    }
    res.setHeader('Content-Type', file.contentType);
    res.setHeader('Cache-Control', 'public, max-age=3600');
    res.send(file.data);
  }
}
