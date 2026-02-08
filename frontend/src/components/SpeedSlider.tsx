interface SpeedSliderProps {
   speed: number;
   onSpeedChange: (speed: number) => void;
}

export function SpeedSlider({ speed, onSpeedChange }: SpeedSliderProps) {
   return (
      <div className="control-group">
         <label>Speed</label>
         <input
            type="range"
            id="speed"
            min="0.1"
            max="5"
            step="0.1"
            value={speed}
            onChange={(e) => onSpeedChange(parseFloat(e.target.value))}
         />
         <span className="slider-value" id="speed-value">
            {speed.toFixed(1)}x
         </span>
      </div>
   );
}
