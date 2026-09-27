import { NestFactory } from '@nestjs/core';
import { AppModule } from './app.module';
import { configureApp } from './app.setup';

async function bootstrap() {
  const app = await NestFactory.create(AppModule);
  configureApp(app);

  const port = process.env.PORT ?? 3000;
  // 0.0.0.0 wajib agar bisa diakses dari luar container/HP di jaringan yang sama
  await app.listen(port, '0.0.0.0');
  console.log(`OriginTag backend jalan di port ${port}`);
}

bootstrap();
