import { useEffect, useRef } from 'react';
import type { ViewportMessage } from '../types.ts';

const PIXEL_SIZE = 10;

export function useViewportRenderer(
   canvasRef: React.RefObject<HTMLCanvasElement | null>,
   viewportRef: React.MutableRefObject<ViewportMessage | null>,
   onResolutionChange?: (width: number, height: number) => void,
): void {
   const lastRenderedRef = useRef<string | null>(null);
   const sizeRef = useRef({ width: 0, height: 0 });
   const onResolutionChangeRef = useRef(onResolutionChange);
   onResolutionChangeRef.current = onResolutionChange;

   useEffect(() => {
      let animationFrameId: number;

      function renderFrame() {
         const viewport = viewportRef.current;
         const canvas = canvasRef.current;

         if (viewport && canvas && viewport.data !== lastRenderedRef.current) {
            lastRenderedRef.current = viewport.data;
            const ctx = canvas.getContext('2d');
            if (!ctx) return;

            // Resize canvas if needed
            if (sizeRef.current.width !== viewport.width || sizeRef.current.height !== viewport.height) {
               sizeRef.current = { width: viewport.width, height: viewport.height };
               canvas.width = viewport.width * PIXEL_SIZE;
               canvas.height = viewport.height * PIXEL_SIZE;
               onResolutionChangeRef.current?.(viewport.width, viewport.height);
            }

            // Decode Base64 RGBA bitmap
            const binaryString = atob(viewport.data);
            const bytes = new Uint8Array(binaryString.length);
            for (let i = 0; i < binaryString.length; i++) {
               bytes[i] = binaryString.charCodeAt(i);
            }

            // Create image data for the canvas
            const imageData = ctx.createImageData(
               viewport.width * PIXEL_SIZE,
               viewport.height * PIXEL_SIZE,
            );

            // Render each pixel from the decoded bitmap
            for (let y = 0; y < viewport.height; y++) {
               for (let x = 0; x < viewport.width; x++) {
                  const pixelIdx = (y * viewport.width + x) * 4;
                  const r = bytes[pixelIdx];
                  const g = bytes[pixelIdx + 1];
                  const b = bytes[pixelIdx + 2];

                  // Draw PIXEL_SIZE x PIXEL_SIZE block for each LED
                  for (let py = 0; py < PIXEL_SIZE; py++) {
                     for (let px = 0; px < PIXEL_SIZE; px++) {
                        const idx = ((y * PIXEL_SIZE + py) * canvas.width + (x * PIXEL_SIZE + px)) * 4;
                        imageData.data[idx] = r;
                        imageData.data[idx + 1] = g;
                        imageData.data[idx + 2] = b;
                        imageData.data[idx + 3] = 255;
                     }
                  }
               }
            }

            ctx.putImageData(imageData, 0, 0);
         }

         animationFrameId = requestAnimationFrame(renderFrame);
      }

      animationFrameId = requestAnimationFrame(renderFrame);

      return () => cancelAnimationFrame(animationFrameId);
   }, [canvasRef, viewportRef]);
}
