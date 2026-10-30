import { defineConfig } from 'astro/config';
import tailwindcss from '@tailwindcss/vite';

const backend = process.env.API_PROXY_TARGET || 'http://127.0.0.1:8080';
export default defineConfig({
  devToolbar: { enabled: false },
  vite: {
    plugins: [tailwindcss()],
    server: { proxy: { '/api': backend } },
    preview: { proxy: { '/api': backend } },
  },
});
