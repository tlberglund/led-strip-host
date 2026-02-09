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
         '/api': 'http://localhost:8080',
         '/viewport': {
            target: 'http://localhost:8080',
            ws: true,
            timeout: 0,
            proxyTimeout: 0,
            configure: (proxy) => {
               proxy.on('error', (_err, _req, res) => {
                  if (res && 'writeHead' in res && !res.headersSent) {
                     res.writeHead(502);
                     res.end();
                  }
               });
               proxy.on('proxyReqWs', (_proxyReq, _req, socket) => {
                  socket.on('error', () => {});
               });
            },
         },
      },
   },
})
