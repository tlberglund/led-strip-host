import { useRef, useEffect } from 'react';

interface LedStripProps {
   rgbValues: number[];   // flat [r, g, b, r, g, b, ...] triples
   ledCount: number;
   cellSize?: number;
}

export function LedStrip({ rgbValues, ledCount, cellSize = 4 }: LedStripProps) {
   const canvasRef = useRef<HTMLCanvasElement>(null);

   useEffect(() => {
      const canvas = canvasRef.current;
      if(!canvas) return;
      const ctx = canvas.getContext('2d');
      if(!ctx) return;

      for(let i = 0; i < ledCount; i++) {
         const ri = i * 3;
         const r = rgbValues[ri]     ?? 0x11;
         const g = rgbValues[ri + 1] ?? 0x11;
         const b = rgbValues[ri + 2] ?? 0x11;
         ctx.fillStyle = `rgb(${r},${g},${b})`;
         ctx.fillRect(i * cellSize, 0, cellSize, cellSize);
      }
   }, [rgbValues, ledCount, cellSize]);

   return (
      <canvas
         ref={canvasRef}
         className="strip-led-canvas"
         width={ledCount * cellSize}
         height={cellSize}
      />
   );
}
