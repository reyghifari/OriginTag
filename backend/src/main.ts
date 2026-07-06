import { ValidationPipe } from '@nestjs/common';
import { NestFactory } from '@nestjs/core';
import { AppModule } from './app.module';

async function bootstrap() {
  const app = await NestFactory.create(AppModule);
  app.useGlobalPipes(new ValidationPipe({ whitelist: true, transform: true }));
  app.enableCors(); // app Android memanggil dari origin berbeda

  const port = process.env.PORT ?? 3000;
  // 0.0.0.0 wajib agar bisa diakses di container Render (bukan hanya localhost)
  await app.listen(port, '0.0.0.0');
  console.log(`OriginTag backend jalan di port ${port}`);
}

bootstrap();
