import type { ViewportMessage, LEDStripData } from '../types.ts';
import { BackgroundImage } from './BackgroundImage.tsx';
import { ViewportCanvas } from './ViewportCanvas.tsx';
import { LEDStripsCanvas } from './LEDStripsCanvas.tsx';

interface PreviewAreaProps {
   showViewport: boolean;
   showStrips: boolean;
   showBackground: boolean;
   backgroundImageUrl: string | null;
   viewportRef: React.MutableRefObject<ViewportMessage | null>;
   ledStripsRef: React.MutableRefObject<LEDStripData[]>;
   viewportWidth: number;
   viewportHeight: number;
   onResolutionChange: (width: number, height: number) => void;
}

export function PreviewArea({
   showViewport,
   showStrips,
   showBackground,
   backgroundImageUrl,
   viewportRef,
   ledStripsRef,
   viewportWidth,
   viewportHeight,
   onResolutionChange,
}: PreviewAreaProps) {
   return (
      <div id="preview">
         <div id="preview-content">
            {backgroundImageUrl && (
               <BackgroundImage
                  visible={showBackground}
                  imageUrl={backgroundImageUrl}
               />
            )}
            <ViewportCanvas
               visible={showViewport}
               viewportRef={viewportRef}
               onResolutionChange={onResolutionChange}
            />
            <LEDStripsCanvas
               visible={showStrips}
               ledStripsRef={ledStripsRef}
               viewportWidth={viewportWidth}
               viewportHeight={viewportHeight}
            />
         </div>
      </div>
   );
}
