import type { StatsData } from '../hooks/useStats.ts';
import type { PatternInfo, ParameterDef } from '../types.ts';
import { ConnectionStatus } from './ConnectionStatus.tsx';
import { ViewToggles } from './ViewToggles.tsx';
import { PatternSelector } from './PatternSelector.tsx';
import { ParameterControl } from './ParameterControl.tsx';
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
   patterns: PatternInfo[];
   selectedPattern: string;
   onPatternSelect: (pattern: string) => void;
   parameters: ParameterDef[];
   paramValues: Record<string, number | string>;
   onParamChange: (name: string, value: number | string) => void;
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
   parameters,
   paramValues,
   onParamChange,
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
         {parameters.map((param) => (
            <ParameterControl
               key={param.name}
               param={param}
               value={paramValues[param.name] ?? param.default}
               onChange={onParamChange}
            />
         ))}
         <ApplyPatternButton onApply={onApplyPattern} />
         <StatsDisplay stats={stats} resolution={resolution} />
      </div>
   );
}
