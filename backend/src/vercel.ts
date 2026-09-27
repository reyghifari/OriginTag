import { NestFactory } from '@nestjs/core';
import type { IncomingMessage, ServerResponse } from 'http';
import { AppModule } from './app.module';
import { configureApp } from './app.setup';

type Handler = (req: IncomingMessage, res: ServerResponse) => void;

// Vercel memakai ulang instance Nest selama function masih hangat; gagal init → coba lagi di request berikutnya.
let server: Promise<Handler> | undefined;

async function createServer(): Promise<Handler> {
  const app = await NestFactory.create(AppModule);
  configureApp(app);
  await app.init();
  return app.getHttpAdapter().getInstance();
}

export default async function handler(req: IncomingMessage, res: ServerResponse) {
  server ??= createServer().catch((e) => {
    server = undefined;
    throw e;
  });
  (await server)(req, res);
}
