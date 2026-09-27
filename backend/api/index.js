// Entry serverless Vercel: semua rute di-rewrite ke sini (lihat vercel.json). dist/ dibangun oleh `vercel-build`.
module.exports = require('../dist/vercel').default;
