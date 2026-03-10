import { useState, useCallback, useMemo, useRef, useEffect } from 'react';
import './App.css';
import { useWebSocket } from './hooks/useWebSocket.ts';
import { usePatterns } from './hooks/usePatterns.ts';
import { useStats } from './hooks/useStats.ts';
import { useLEDStrips } from './hooks/useLEDStrips.ts';
import { useBackgroundImage } from './hooks/useBackgroundImage.ts';
import { useSavedPatterns } from './hooks/useSavedPatterns.ts';
import { useSettings } from './hooks/useSettings.ts';
import { PreviewArea } from './components/PreviewArea.tsx';
import { StatsDisplay } from './components/StatsDisplay.tsx';
import { ControlsSidebar } from './components/ControlsSidebar.tsx';
import { SavedPatternsPanel } from './components/SavedPatternsPanel.tsx';
import { StripManagerTab } from './components/StripManagerTab.tsx';
import { SettingsTab } from './components/SettingsTab.tsx';
import type { PatternInfo } from './types.ts';

type Tab = 'pattern' | 'strips' | 'settings';
type RightTab = 'controls' | 'saved';

const TABS: Tab[] = ['pattern', 'strips', 'settings'];

function tabFromPath(path: string): Tab {
   const segment = path.replace(/^\//, '').split('/')[0] as Tab;
   return TABS.includes(segment) ? segment : 'pattern';
}

function App() {
   const [activeTab, setActiveTab] = useState<Tab>(() => tabFromPath(window.location.pathname));
   const [rightTab, setRightTab] = useState<RightTab>('controls');

   useEffect(() => {
      const onPopState = () => setActiveTab(tabFromPath(window.location.pathname));
      window.addEventListener('popstate', onPopState);
      return () => window.removeEventListener('popstate', onPopState);
   }, []);

   const navigateTo = useCallback((tab: Tab) => {
      const path = `/${tab}`;
      if(window.location.pathname !== path) {
         history.pushState(null, '', path);
      }
      setActiveTab(tab);
   }, []);

   // Connection state
   const [connected, setConnected] = useState(false);

   // View toggles — initialized from DB settings, persisted on change
   const [showViewport, setShowViewport] = useState(true);
   const [showStrips, setShowStrips] = useState(false);
   const [showBackground, setShowBackground] = useState(false);
   const viewToggleSettingsReady = useRef(false);

   // Pattern controls — dynamic parameter values
   const [selectedPattern, setSelectedPattern] = useState('');
   const [paramValues, setParamValues] = useState<Record<string, number | string>>({});

   // Resolution tracking (updated by ViewportCanvas)
   const [resolution, setResolution] = useState('');

   // Viewport dimensions for LED strips canvas sizing
   const [viewportWidth, setViewportWidth] = useState(0);
   const [viewportHeight, setViewportHeight] = useState(0);

   // Hooks
   const viewportRef = useWebSocket(setConnected);
   const patterns = usePatterns();
   const stats = useStats(connected);
   const ledStripsRef = useLEDStrips(showStrips);
   const backgroundImageUrl = useBackgroundImage();
   const { settings: dbSettings, loading: settingsLoading, saveSettings } = useSettings();
   const { presets, loading: presetsLoading, error: presetsError, savePreset, updatePreset, deletePreset, renamePreset } = useSavedPatterns();

   // Seed view toggles from DB once settings load
   useEffect(() => {
      if(settingsLoading || viewToggleSettingsReady.current) return;
      viewToggleSettingsReady.current = true;
      setShowViewport(dbSettings.showViewport);
      setShowStrips(dbSettings.showStrips);
      setShowBackground(dbSettings.showBackground);
   }, [settingsLoading, dbSettings]);

   // Persist view toggle changes
   useEffect(() => {
      if(!viewToggleSettingsReady.current) return;
      saveSettings({ showViewport, showStrips, showBackground });
   }, [showViewport, showStrips, showBackground]); // eslint-disable-line react-hooks/exhaustive-deps

   const [activePresetName, setActivePresetName] = useState<string | null>(null);
   const [activePresetId, setActivePresetId] = useState<number | null>(null);

   // One-time sync: once both the active preset name and the presets list are
   // available (either order), set activePresetId so the UI reflects the loaded state.
   const initialPresetSyncDone = useRef(false);
   useEffect(() => {
      if(initialPresetSyncDone.current || presetsLoading || !activePresetName) return;
      initialPresetSyncDone.current = true;
      const preset = presets.find((p) => p.presetName === activePresetName);
      if(preset) setActivePresetId(preset.id);
   }, [presetsLoading, activePresetName, presets]);

   // Restore active pattern on mount; re-read the startup default whenever
   // the user navigates back to the pattern tab (they may have changed it in Settings).
   useEffect(() => {
      async function syncActivePattern(isMount: boolean) {
         try {
            const response = await fetch('/api/active-pattern');
            if(!response.ok) return;
            const data: { patternName: string; params: Record<string, unknown>; presetName?: string } = await response.json();
            if(isMount && data.patternName) {
               setSelectedPattern(data.patternName);
               setParamValues(data.params as Record<string, number | string>);
            }
            setActivePresetName(data.presetName ?? null);
         }
         catch(e) {
            // Non-fatal
         }
      }
      syncActivePattern(activeTab === 'pattern');
   }, [activeTab]); // re-runs whenever the tab changes

   // Look up the currently selected pattern's info
   const selectedPatternInfo: PatternInfo | undefined = useMemo(() => {
      return patterns.find((p) => p.name === selectedPattern);
   }, [patterns, selectedPattern]);

   const handleResolutionChange = useCallback((width: number, height: number) => {
      setResolution(`${width}x${height}`);
      setViewportWidth(width);
      setViewportHeight(height);
   }, []);

   // Initialize parameter defaults when a pattern is selected
   const handlePatternSelect = useCallback((patternName: string) => {
      setSelectedPattern(patternName);
      setActivePresetId(null);

      // Build default values from the pattern's parameter definitions
      const patternInfo = patterns.find((p) => p.name === patternName);
      if (patternInfo) {
         const defaults: Record<string, number | string> = {};
         for (const param of patternInfo.parameters) {
            defaults[param.name] = param.default;
         }
         setParamValues(defaults);

         // Auto-apply on pattern change
         fetch(`/api/pattern/${patternName}`, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify(defaults),
         }).catch((e) => console.error('Failed to apply pattern:', e));
      }
   }, [patterns]);

   const applyDebounceRef = useRef<ReturnType<typeof setTimeout> | null>(null);

   const handleParamChange = useCallback((name: string, value: number | string) => {
      setParamValues((prev) => {
         const next = { ...prev, [name]: value };

         if (applyDebounceRef.current) clearTimeout(applyDebounceRef.current);
         applyDebounceRef.current = setTimeout(() => {
            if (!selectedPattern) return;
            fetch(`/api/pattern/${selectedPattern}`, {
               method: 'POST',
               headers: { 'Content-Type': 'application/json' },
               body: JSON.stringify(next),
            }).catch((e) => console.error('Failed to apply pattern:', e));
         }, 50);

         return next;
      });
   }, [selectedPattern]);

   const handleSave = useCallback(async () => {
      if(activePresetId === null || !selectedPattern) return;
      await updatePreset(activePresetId, {
         patternName: selectedPattern,
         params: paramValues as Record<string, unknown>,
      });
   }, [activePresetId, selectedPattern, paramValues, updatePreset]);

   const handleSaveAs = useCallback(async (name: string) => {
      if(!selectedPattern) return;
      const created = await savePreset(name, selectedPattern, paramValues as Record<string, unknown>);
      setActivePresetId(created.id);
   }, [selectedPattern, paramValues, savePreset]);

   const handleNew = useCallback(() => {
      if(patterns.length === 0) return;
      const firstPattern = patterns[0];
      const defaults: Record<string, number | string> = {};
      for(const param of firstPattern.parameters) {
         defaults[param.name] = param.default;
      }
      setSelectedPattern(firstPattern.name);
      setParamValues(defaults);
      setActivePresetId(null);
      fetch(`/api/pattern/${firstPattern.name}`, {
         method: 'POST',
         headers: { 'Content-Type': 'application/json' },
         body: JSON.stringify(defaults),
      }).catch((e) => console.error('Failed to apply pattern:', e));
   }, [patterns]);

   // Load a saved preset into the renderer (without changing the startup default)
   const handleLoadPreset = useCallback((presetId: number, patternName: string, params: Record<string, unknown>) => {
      const typedParams = params as Record<string, number | string>;
      setSelectedPattern(patternName);
      setParamValues(typedParams);
      setActivePresetId(presetId);
      fetch(`/api/pattern/${patternName}`, {
         method: 'POST',
         headers: { 'Content-Type': 'application/json' },
         body: JSON.stringify(typedParams),
      }).catch((e) => console.error('Failed to apply preset:', e));
   }, []);

   // Set a preset as the startup default (and load it into the renderer)
   const handleSetDefaultPreset = useCallback((presetId: number, presetName: string) => {
      setActivePresetName(presetName);
      setActivePresetId(presetId);
      fetch(`/api/saved-patterns/${presetId}/load`, { method: 'POST' })
         .then(async (res) => {
            if(!res.ok) {
               console.error('Failed to set default preset:', res.status);
               setActivePresetName(null);
               return;
            }
            // Backend applied the preset; sync local pattern + param state
            const data: { patternName?: string; params?: Record<string, unknown> } = await res.json().catch(() => ({}));
            if(data.patternName) setSelectedPattern(data.patternName);
            if(data.params) setParamValues(data.params as Record<string, number | string>);
         })
         .catch((e) => { console.error('Failed to set default preset:', e); setActivePresetName(null); });
   }, []);

   return (
      <>
         <div id="top-nav">
            <div id="top-nav-left">
               <h1>LED Strip Host</h1>
               <div className="tabs" role="tablist">
                  <button
                     className={`tab ${activeTab === 'pattern' ? 'active' : ''}`}
                     onClick={() => navigateTo('pattern')}>
                     Pattern
                  </button>
                  <button
                     className={`tab ${activeTab === 'strips' ? 'active' : ''}`}
                     onClick={() => navigateTo('strips')}>
                     Strips
                  </button>
                  <button
                     className={`tab ${activeTab === 'settings' ? 'active' : ''}`}
                     onClick={() => navigateTo('settings')}>
                     Settings
                  </button>
               </div>
            </div>
            <StatsDisplay stats={stats} resolution={resolution} />
         </div>

         {activeTab === 'pattern' && (
            <div id="container">
               <PreviewArea
                  showViewport={showViewport}
                  showStrips={showStrips}
                  showBackground={showBackground}
                  backgroundImageUrl={backgroundImageUrl}
                  viewportRef={viewportRef}
                  ledStripsRef={ledStripsRef}
                  viewportWidth={viewportWidth}
                  viewportHeight={viewportHeight}
                  onResolutionChange={handleResolutionChange}
               />
               <div id="right-panel">
                  <div className="right-tabs" role="tablist">
                     <button
                        className={`right-tab${rightTab === 'controls' ? ' active' : ''}`}
                        onClick={() => setRightTab('controls')}>
                        Settings
                     </button>
                     <button
                        className={`right-tab${rightTab === 'saved' ? ' active' : ''}`}
                        onClick={() => setRightTab('saved')}>
                        Saved
                     </button>
                  </div>
                  {rightTab === 'controls' && (
                     <ControlsSidebar
                        connected={connected}
                        showViewport={showViewport}
                        showStrips={showStrips}
                        showBackground={showBackground}
                        hasBackgroundImage={backgroundImageUrl !== null}
                        onShowViewportChange={setShowViewport}
                        onShowStripsChange={setShowStrips}
                        onShowBackgroundChange={setShowBackground}
                        patterns={patterns}
                        selectedPattern={selectedPattern}
                        onPatternSelect={handlePatternSelect}
                        parameters={selectedPatternInfo?.parameters ?? []}
                        paramValues={paramValues}
                        onParamChange={handleParamChange}
                        activePresetId={activePresetId}
                        onSave={handleSave}
                        onSaveAs={handleSaveAs}
                        onNew={handleNew}
                        savedPresets={presets}
                     />
                  )}
                  {rightTab === 'saved' && (
                     <SavedPatternsPanel
                        presets={presets}
                        loading={presetsLoading}
                        error={presetsError}
                        activePresetName={activePresetName}
                        activePresetId={activePresetId}
                        onLoad={handleLoadPreset}
                        onSetDefault={handleSetDefaultPreset}
                        onSave={savePreset}
                        onUpdate={updatePreset}
                        onDelete={deletePreset}
                        onRename={renamePreset}
                        currentPatternName={selectedPattern}
                        currentParams={paramValues}
                     />
                  )}
               </div>
            </div>
         )}

         {activeTab === 'strips' && <StripManagerTab active={true} />}
         {activeTab === 'settings' && <SettingsTab />}
      </>
   );
}

export default App;
