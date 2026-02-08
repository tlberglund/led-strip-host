import { memo, useRef } from 'react';
import type { LEDStripData } from '../types.ts';
import { useLEDStripsRenderer } from '../hooks/useLEDStripsRenderer.ts';

interface LEDStripsCanvasProps {
   visible: boolean;
   ledStripsRef: React.MutableRefObject<LEDStripData[]>;
   viewportWidth: number;
   viewportHeight: number;
}

export const LEDStripsCanvas = memo(function LEDStripsCanvas({
   visible,
   ledStripsRef,
   viewportWidth,
   viewportHeight,
}: LEDStripsCanvasProps) {
   const canvasRef = useRef<HTMLCanvasElement>(null);

   useLEDStripsRenderer(canvasRef, ledStripsRef, viewportWidth, viewportHeight);

   return (
      <canvas
         ref={canvasRef}
         id="led-canvas"
         style={{ display: visible ? 'block' : 'none' }}
      />
   );
});
