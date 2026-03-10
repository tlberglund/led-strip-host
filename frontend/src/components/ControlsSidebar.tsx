import type { PatternInfo, ParameterDef } from '../types.ts';
import type { SavedPreset } from '../hooks/useSavedPatterns.ts';
import { ConnectionStatus } from './ConnectionStatus.tsx';
import { ViewToggles } from './ViewToggles.tsx';
import { PatternSelector } from './PatternSelector.tsx';
import { ParameterControl } from './ParameterControl.tsx';
import { SavePatternButton } from './SavePatternButton.tsx';

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
   activePresetId: number | null;
   onSave: () => Promise<void>;
   onSaveAs: (name: string) => Promise<void>;
   onNew: () => void;
   savedPresets: SavedPreset[];
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
   activePresetId,
   onSave,
   onSaveAs,
   onNew,
   savedPresets,
}: ControlsSidebarProps) {
   const loadedPresetName = activePresetId !== null
      ? (savedPresets.find((p) => p.id === activePresetId)?.presetName ?? null)
      : null;

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
         <div className="preset-status">
            {activePresetId !== null
               ? <span className="preset-status__name">{loadedPresetName ?? '…'}</span>
               : <span className="preset-status__unsaved">&lt;unsaved pattern&gt;</span>
            }
         </div>
         {parameters.map((param) => (
            <ParameterControl
               key={param.name}
               param={param}
               value={paramValues[param.name] ?? param.default}
               onChange={onParamChange}
            />
         ))}
         <div className="pattern-actions">
            <SavePatternButton
               activePresetId={activePresetId}
               onSave={onSave}
               onSaveAs={onSaveAs}
            />
            <button
               className="settings-save-btn settings-save-btn--sm settings-save-btn--neutral"
               title="Start a new unsaved pattern"
               onClick={onNew}>
               New
            </button>
         </div>

      </div>
   );
}
