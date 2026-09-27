import { INestApplication, ValidationPipe } from '@nestjs/common';

/** Konfigurasi bersama untuk server lokal (main.ts) dan serverless Vercel (vercel.ts). */
export function configureApp(app: INestApplication) {
  app.useGlobalPipes(new ValidationPipe({ whitelist: true, transform: true }));
  app.enableCors(); // app Android memanggil dari origin berbeda
}
