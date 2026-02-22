import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'

export default defineConfig({
   plugins: [react()],
   base: '/',
   build: {
      outDir: '../src/main/resources/web',
      emptyOutDir: true,
   },
   server: {
      proxy: {
         '/api': 'http://rpi-dev-1.local:8080',
      },
   },
})
