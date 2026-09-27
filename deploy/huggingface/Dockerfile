# Hugging Face Space (Docker) untuk backend OriginTag.
# Kode diambil dari repo GitHub publik; secret diisi di Settings → Variables and secrets.
# Setelah push ke GitHub, pakai "Factory rebuild" di Space agar kode terbaru terambil.
FROM node:22-slim

RUN apt-get update \
 && apt-get install -y --no-install-recommends git ca-certificates \
 && rm -rf /var/lib/apt/lists/*

WORKDIR /app
ARG REPO=https://github.com/reyghifari/OriginTag.git
ARG REF=main
RUN git clone --depth 1 --branch "$REF" "$REPO" /tmp/src \
 && cp -r /tmp/src/backend/. /app \
 && rm -rf /tmp/src

RUN npm ci && npm run build && npm prune --omit=dev

ENV NODE_ENV=production PORT=7860
EXPOSE 7860
USER node
CMD ["node", "dist/main.js"]
