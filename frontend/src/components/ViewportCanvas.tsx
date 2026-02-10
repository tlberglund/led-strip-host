import { memo, useRef } from 'react';
import type { ViewportMessage } from '../types.ts';
import { useViewportRenderer } from '../hooks/useViewportRenderer.ts';

interface ViewportCanvasProps {
   visible: boolean;
   viewportRef: React.MutableRefObject<ViewportMessage | null>;
   onResolutionChange?: (width: number, height: number) => void;
}

export const ViewportCanvas = memo(function ViewportCanvas({
   visible,
   viewportRef,
   onResolutionChange,
}: ViewportCanvasProps) {
   const canvasRef = useRef<HTMLCanvasElement>(null);

   useViewportRenderer(canvasRef, viewportRef, onResolutionChange);

   return (
      <canvas
         ref={canvasRef}
         id="canvas"
         style={{ visibility: visible ? 'visible' : 'hidden' }}
      />
   );
});
