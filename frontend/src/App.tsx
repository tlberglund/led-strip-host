import { useState, useCallback } from 'react';
import './App.css';
import { useWebSocket } from './hooks/useWebSocket.ts';
import { usePatterns } from './hooks/usePatterns.ts';
import { useStats } from './hooks/useStats.ts';
import { useLEDStrips } from './hooks/useLEDStrips.ts';
import { useBackgroundImage } from './hooks/useBackgroundImage.ts';
import { PreviewArea } from './components/PreviewArea.tsx';
import { ControlsSidebar } from './components/ControlsSidebar.tsx';

function App() {
   // Connection state
   const [connected, setConnected] = useState(false);

   // View toggles
   const [showViewport, setShowViewport] = useState(true);
   const [showStrips, setShowStrips] = useState(false);
   const [showBackground, setShowBackground] = useState(false);

   // Pattern controls
   const [selectedPattern, setSelectedPattern] = useState('');
   const [speed, setSpeed] = useState(1.0);
   const [brightness, setBrightness] = useState(100);

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

   const handleResolutionChange = useCallback((width: number, height: number) => {
      setResolution(`${width}x${height}`);
      setViewportWidth(width);
      setViewportHeight(height);
   }, []);

   const handlePatternSelect = useCallback((pattern: string) => {
      setSelectedPattern(pattern);
      // Auto-apply on change, matching the original behavior
      if (pattern) {
         const normalizedBrightness = brightness / 100;
         fetch(`/api/pattern/${pattern}`, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ speed, brightness: normalizedBrightness }),
         }).catch((e) => console.error('Failed to apply pattern:', e));
      }
   }, [speed, brightness]);

   const handleApplyPattern = useCallback(async () => {
      if (!selectedPattern) return;

      const normalizedBrightness = brightness / 100;
      try {
         const response = await fetch(`/api/pattern/${selectedPattern}`, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ speed, brightness: normalizedBrightness }),
         });
         if (!response.ok) {
            console.error('Failed to apply pattern:', response.status);
         }
      } catch (e) {
         console.error('Exception applying pattern:', e);
      }
   }, [selectedPattern, speed, brightness]);

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
               speed={speed}
               onSpeedChange={setSpeed}
               brightness={brightness}
               onBrightnessChange={setBrightness}
               onApplyPattern={handleApplyPattern}
               stats={stats}
               resolution={resolution}
            />
         </div>
      </>
   );
}

export default App;
