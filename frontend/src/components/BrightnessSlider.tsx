interface BrightnessSliderProps {
   brightness: number;
   onBrightnessChange: (brightness: number) => void;
}

export function BrightnessSlider({ brightness, onBrightnessChange }: BrightnessSliderProps) {
   return (
      <div className="control-group">
         <label>Brightness</label>
         <input
            type="range"
            id="brightness"
            min="0"
            max="100"
            value={brightness}
            onChange={(e) => onBrightnessChange(parseInt(e.target.value))}
         />
         <span className="slider-value" id="brightness-value">
            {brightness}%
         </span>
      </div>
   );
}
