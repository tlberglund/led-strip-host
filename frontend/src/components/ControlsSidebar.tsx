import type { StatsData } from '../hooks/useStats.ts';
import { ConnectionStatus } from './ConnectionStatus.tsx';
import { ViewToggles } from './ViewToggles.tsx';
import { PatternSelector } from './PatternSelector.tsx';
import { SpeedSlider } from './SpeedSlider.tsx';
import { BrightnessSlider } from './BrightnessSlider.tsx';
import { ApplyPatternButton } from './ApplyPatternButton.tsx';
import { StatsDisplay } from './StatsDisplay.tsx';

interface ControlsSidebarProps {
   connected: boolean;
   showViewport: boolean;
   showStrips: boolean;
   showBackground: boolean;
   hasBackgroundImage: boolean;
   onShowViewportChange: (show: boolean) => void;
   onShowStripsChange: (show: boolean) => void;
   onShowBackgroundChange: (show: boolean) => void;
   patterns: string[];
   selectedPattern: string;
   onPatternSelect: (pattern: string) => void;
   speed: number;
   onSpeedChange: (speed: number) => void;
   brightness: number;
   onBrightnessChange: (brightness: number) => void;
   onApplyPattern: () => void;
   stats: StatsData;
   resolution: string;
}

export function ControlsSidebar({
   connected,
   showViewport,
   showStrips,
   showBackground,
   hasBackgroundImage,
   onShowViewportChange,
   onShowStripsChange,
   onShowBackgroundChange,
   patterns,
   selectedPattern,
   onPatternSelect,
   speed,
   onSpeedChange,
   brightness,
   onBrightnessChange,
   onApplyPattern,
   stats,
   resolution,
}: ControlsSidebarProps) {
   return (
      <div id="controls">
         <ConnectionStatus connected={connected} />
         <ViewToggles
            showViewport={showViewport}
            showStrips={showStrips}
            showBackground={showBackground}
            hasBackgroundImage={hasBackgroundImage}
            onShowViewportChange={onShowViewportChange}
            onShowStripsChange={onShowStripsChange}
            onShowBackgroundChange={onShowBackgroundChange}
         />
         <PatternSelector
            patterns={patterns}
            selectedPattern={selectedPattern}
            onSelect={onPatternSelect}
         />
         <SpeedSlider speed={speed} onSpeedChange={onSpeedChange} />
         <BrightnessSlider brightness={brightness} onBrightnessChange={onBrightnessChange} />
         <ApplyPatternButton onApply={onApplyPattern} />
         <StatsDisplay stats={stats} resolution={resolution} />
      </div>
   );
}
