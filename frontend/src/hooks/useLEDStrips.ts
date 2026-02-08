import { useEffect, useRef } from 'react';
import type { LEDStripData } from '../types.ts';

export function useLEDStrips(
   enabled: boolean,
): React.MutableRefObject<LEDStripData[]> {
   const ledStripsRef = useRef<LEDStripData[]>([]);

   useEffect(() => {
      if (!enabled) {
         ledStripsRef.current = [];
         return;
      }

      const interval = setInterval(async () => {
         try {
            const response = await fetch('/api/led-strips');
            const strips: LEDStripData[] = await response.json();
            ledStripsRef.current = strips;
         } catch (e) {
            console.error('Failed to fetch LED strips:', e);
         }
      }, 50); // 20 FPS

      return () => clearInterval(interval);
   }, [enabled]);

   return ledStripsRef;
}
