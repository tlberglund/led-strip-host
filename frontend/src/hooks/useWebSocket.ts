import { useEffect, useRef } from 'react';
import type { ViewportMessage } from '../types.ts';

/**
 * Decompresses deflate-compressed data using the browser's DecompressionStream API.
 */
async function inflate(compressed: Uint8Array): Promise<Uint8Array> {
   const ds = new DecompressionStream('deflate');
   const writer = ds.writable.getWriter();
   // Create a fresh ArrayBuffer copy to satisfy TypeScript's BufferSource type
   const copy = new Uint8Array(compressed).buffer;
   writer.write(copy);
   writer.close();

   const reader = ds.readable.getReader();
   const chunks: Uint8Array[] = [];
   let totalLength = 0;

   for (;;) {
      const { done, value } = await reader.read();
      if (done) break;
      chunks.push(value);
      totalLength += value.length;
   }

   // Fast path: single chunk (common case)
   if (chunks.length === 1) return chunks[0];

   // Concatenate multiple chunks
   const result = new Uint8Array(totalLength);
   let offset = 0;
   for (const chunk of chunks) {
      result.set(chunk, offset);
      offset += chunk.length;
   }
   return result;
}

export function useWebSocket(
   onConnectionChange: (connected: boolean) => void,
): React.MutableRefObject<ViewportMessage | null> {
   const viewportRef = useRef<ViewportMessage | null>(null);
   const onConnectionChangeRef = useRef(onConnectionChange);
   onConnectionChangeRef.current = onConnectionChange;

   useEffect(() => {
      let ws: WebSocket | null = null;
      let reconnectTimeout: ReturnType<typeof setTimeout> | null = null;
      let disposed = false;

      function connect() {
         if (disposed) return;

         // In dev mode, connect directly to Ktor to avoid Vite proxy
         // mangling binary WebSocket frames. In production, connect to same host.
         const wsUrl = import.meta.env.DEV
            ? 'ws://localhost:8080/viewport'
            : `${window.location.protocol === 'https:' ? 'wss:' : 'ws:'}//${window.location.host}/viewport`;

         ws = new WebSocket(wsUrl);
         ws.binaryType = 'arraybuffer';

         ws.onopen = () => {
            if (disposed) {
               ws?.close();
               return;
            }
            onConnectionChangeRef.current(true);
         };

         ws.onclose = () => {
            if (disposed) return;
            onConnectionChangeRef.current(false);

            // Reconnect after 2 seconds
            reconnectTimeout = setTimeout(() => {
               reconnectTimeout = null;
               connect();
            }, 2000);
         };

         ws.onerror = () => {
            // Error is followed by onclose, so no action needed here
         };

         ws.onmessage = async (event) => {
            if (!(event.data instanceof ArrayBuffer)) return;
            const bytes = new Uint8Array(event.data);
            if (bytes.length < 5) return;

            const flags = bytes[0];
            const width = (bytes[1] << 8) | bytes[2];
            const height = (bytes[3] << 8) | bytes[4];

            if (flags === 0x01) {
               // Deflate-compressed RGB data
               const compressed = bytes.subarray(5);
               try {
                  const pixelData = await inflate(compressed);
                  viewportRef.current = { width, height, data: pixelData };
               } catch {
                  // Skip frame on decompression error
               }
            } else {
               // Uncompressed RGB data
               viewportRef.current = { width, height, data: bytes.subarray(5) };
            }
         };
      }

      connect();

      return () => {
         disposed = true;
         if (reconnectTimeout) {
            clearTimeout(reconnectTimeout);
         }
         if (ws) {
            ws.onclose = null;
            ws.close();
         }
         onConnectionChangeRef.current(false);
      };
   }, []);

   return viewportRef;
}
