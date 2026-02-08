import { useEffect } from 'react';
import type { LEDStripData } from '../types.ts';

const PIXEL_SIZE = 10;
const LED_SIZE = 5;

export function useLEDStripsRenderer(
   canvasRef: React.RefObject<HTMLCanvasElement | null>,
   ledStripsRef: React.MutableRefObject<LEDStripData[]>,
   viewportWidth: number,
   viewportHeight: number,
): void {
   useEffect(() => {
      const canvas = canvasRef.current;
      if (!canvas) return;

      // Resize canvas to match viewport dimensions
      if (viewportWidth > 0 && viewportHeight > 0) {
         canvas.width = viewportWidth * PIXEL_SIZE;
         canvas.height = viewportHeight * PIXEL_SIZE;
      }
   }, [canvasRef, viewportWidth, viewportHeight]);

   useEffect(() => {
      let animationFrameId: number;

      function renderFrame() {
         const canvas = canvasRef.current;
         const strips = ledStripsRef.current;

         if (canvas && strips.length > 0) {
            const ctx = canvas.getContext('2d');
            if (!ctx) return;

            // Clear canvas with transparency
            ctx.clearRect(0, 0, canvas.width, canvas.height);

            // Render each LED at its viewport position
            const offset = (PIXEL_SIZE - LED_SIZE) / 2;

            for (const strip of strips) {
               for (const led of strip.leds) {
                  const canvasX = led.x * PIXEL_SIZE;
                  const canvasY = led.y * PIXEL_SIZE;

                  ctx.fillStyle = `rgb(0, 0, 0)`;
                  ctx.fillRect(
                     canvasX + offset - 1,
                     canvasY + offset - 1,
                     LED_SIZE + 2,
                     LED_SIZE + 2,
                  );


                  ctx.fillStyle = `rgb(${led.r}, ${led.g}, ${led.b})`;
                  ctx.fillRect(
                     canvasX + offset,
                     canvasY + offset,
                     LED_SIZE,
                     LED_SIZE,
                  );
               }
            }
         }

         animationFrameId = requestAnimationFrame(renderFrame);
      }

      animationFrameId = requestAnimationFrame(renderFrame);

      return () => cancelAnimationFrame(animationFrameId);
   }, [canvasRef, ledStripsRef]);
}
