import type { StatsData } from '../hooks/useStats.ts';
import type { PatternInfo, ParameterDef } from '../types.ts';
import type { SavedPreset } from '../hooks/useSavedPatterns.ts';
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
   savedPresets: SavedPreset[];
   activePresetName: string | null;
   onSetDefaultPreset: (presetId: number, presetName: string) => void;
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
   savedPresets,
   activePresetName,
   onSetDefaultPreset,
}: ControlsSidebarProps) {
   function handleDefaultChange(e: React.ChangeEvent<HTMLSelectElement>) {
      const presetName = e.target.value;
      if(!presetName) return;
      const preset = savedPresets.find((p) => p.presetName === presetName);
      if(preset) onSetDefaultPreset(preset.id, preset.presetName);
   }

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

         <div className="control-group startup-default-group">
            <label htmlFor="startup-default-select">Startup Default</label>
            {savedPresets.length === 0
               ? <p className="startup-default-empty">No saved patterns yet</p>
               : (
                  <select
                     id="startup-default-select"
                     value={activePresetName ?? ''}
                     onChange={handleDefaultChange}>
                     <option value="" disabled>— none selected —</option>
                     {savedPresets.map((p) => (
                        <option key={p.id} value={p.presetName}>{p.presetName}</option>
                     ))}
                  </select>
               )
            }
         </div>

         <StatsDisplay stats={stats} resolution={resolution} />
      </div>
   );
}
