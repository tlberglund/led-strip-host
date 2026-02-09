import { useState, useCallback, useMemo } from 'react';
import './App.css';
import { useWebSocket } from './hooks/useWebSocket.ts';
import { usePatterns } from './hooks/usePatterns.ts';
import { useStats } from './hooks/useStats.ts';
import { useLEDStrips } from './hooks/useLEDStrips.ts';
import { useBackgroundImage } from './hooks/useBackgroundImage.ts';
import { PreviewArea } from './components/PreviewArea.tsx';
import { ControlsSidebar } from './components/ControlsSidebar.tsx';
import type { PatternInfo } from './types.ts';

function App() {
   // Connection state
   const [connected, setConnected] = useState(false);

   // View toggles
   const [showViewport, setShowViewport] = useState(true);
   const [showStrips, setShowStrips] = useState(false);
   const [showBackground, setShowBackground] = useState(false);

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

   const handleParamChange = useCallback((name: string, value: number | string) => {
      setParamValues((prev) => ({ ...prev, [name]: value }));
   }, []);

   const handleApplyPattern = useCallback(async () => {
      if (!selectedPattern) return;

      try {
         const response = await fetch(`/api/pattern/${selectedPattern}`, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify(paramValues),
         });
         if (!response.ok) {
            console.error('Failed to apply pattern:', response.status);
         }
      } catch (e) {
         console.error('Exception applying pattern:', e);
      }
   }, [selectedPattern, paramValues]);

   return (
      <>
         <h1>LED Strip Preview</h1>
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
               onApplyPattern={handleApplyPattern}
               stats={stats}
               resolution={resolution}
            />
         </div>
      </>
   );
}

export default App;
