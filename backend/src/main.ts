import { ValidationPipe } from '@nestjs/common';
import { NestFactory } from '@nestjs/core';
import { AppModule } from './app.module';

async function bootstrap() {
  const app = await NestFactory.create(AppModule);
  app.useGlobalPipes(new ValidationPipe({ whitelist: true, transform: true }));
  app.enableCors(); // app Android memanggil dari origin berbeda

  const port = process.env.PORT ?? 3000;
  await app.listen(port);
  console.log(`OriginTag backend jalan di http://localhost:${port}`);
}

bootstrap();
