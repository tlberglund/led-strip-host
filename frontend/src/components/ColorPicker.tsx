import { useState } from 'react';

interface ColorPickerProps {
   value: string;   // #RRGGBBbb wire format
   onChange: (value: string) => void;
}

function toHex2(n: number): string {
   return Math.round(n).toString(16).padStart(2, '0');
}

function parseValue(v: string): { r: number; g: number; b: number; brightness: number } {
   try {
      if(!v || v.length < 7 || v[0] !== '#') return { r: 255, g: 0, b: 0, brightness: 31 };
      const r = parseInt(v.substring(1, 3), 16);
      const g = parseInt(v.substring(3, 5), 16);
      const b = parseInt(v.substring(5, 7), 16);
      const brightness = v.length >= 9
         ? Math.min(31, parseInt(v.substring(7, 9), 16))
         : 31;
      if(isNaN(r) || isNaN(g) || isNaN(b)) return { r: 255, g: 0, b: 0, brightness: 31 };
      return { r, g, b, brightness };
   }
   catch {
      return { r: 255, g: 0, b: 0, brightness: 31 };
   }
}

function buildValue(r: number, g: number, b: number, brightness: number): string {
   return `#${toHex2(r)}${toHex2(g)}${toHex2(b)}${toHex2(brightness)}`;
}

function rgbToHsv(r: number, g: number, b: number): { h: number; s: number; v: number } {
   const rn = r / 255, gn = g / 255, bn = b / 255;
   const max = Math.max(rn, gn, bn);
   const min = Math.min(rn, gn, bn);
   const delta = max - min;
   let h = 0;
   if(delta !== 0) {
      if(max === rn) h = 60 * (((gn - bn) / delta) % 6);
      else if(max === gn) h = 60 * ((bn - rn) / delta + 2);
      else h = 60 * ((rn - gn) / delta + 4);
   }
   if(h < 0) h += 360;
   return {
      h: Math.round(h),
      s: Math.round(max === 0 ? 0 : (delta / max) * 100),
      v: Math.round(max * 100),
   };
}

function hsvToRgb(h: number, s: number, v: number): { r: number; g: number; b: number } {
   const sn = s / 100, vn = v / 100;
   const c = vn * sn;
   const x = c * (1 - Math.abs(((h / 60) % 2) - 1));
   const m = vn - c;
   let rp = 0, gp = 0, bp = 0;
   if     (h < 60)  { rp = c; gp = x; bp = 0; }
   else if(h < 120) { rp = x; gp = c; bp = 0; }
   else if(h < 180) { rp = 0; gp = c; bp = x; }
   else if(h < 240) { rp = 0; gp = x; bp = c; }
   else if(h < 300) { rp = x; gp = 0; bp = c; }
   else             { rp = c; gp = 0; bp = x; }
   return {
      r: Math.round((rp + m) * 255),
      g: Math.round((gp + m) * 255),
      b: Math.round((bp + m) * 255),
   };
}

function clamp(n: number, lo: number, hi: number): number {
   return Math.max(lo, Math.min(hi, Math.round(n)));
}

interface SliderRowProps {
   label: string;
   min: number;
   max: number;
   value: number;
   gradient: string;
   onChange: (v: number) => void;
}

function SliderRow({ label, min, max, value, gradient, onChange }: SliderRowProps) {
   return (
      <div className="color-picker-slider-row">
         <span className="color-picker-slider-label">{label}</span>
         <input
            type="range"
            min={min}
            max={max}
            value={value}
            className="color-picker-channel-slider"
            style={{ background: gradient }}
            onChange={e => onChange(+e.target.value)}
         />
         <input
            type="number"
            min={min}
            max={max}
            value={value}
            className="color-picker-channel-input"
            onChange={e => onChange(clamp(+e.target.value, min, max))}
         />
      </div>
   );
}

export function ColorPicker({ value, onChange }: ColorPickerProps) {
   const [open, setOpen] = useState(false);
   const [mode, setMode] = useState<'rgb' | 'hsv'>('rgb');

   const { r, g, b, brightness } = parseValue(value);
   const hsv = rgbToHsv(r, g, b);
   const hex = `#${toHex2(r)}${toHex2(g)}${toHex2(b)}`;

   // Pure hue color for HSV slider gradients
   const pureHue = hsvToRgb(hsv.h, 100, 100);
   const pureHueHex = `#${toHex2(pureHue.r)}${toHex2(pureHue.g)}${toHex2(pureHue.b)}`;

   const emit = (nr: number, ng: number, nb: number, nbri: number) =>
      onChange(buildValue(clamp(nr, 0, 255), clamp(ng, 0, 255), clamp(nb, 0, 255), clamp(nbri, 0, 31)));

   const emitHsv = (h: number, s: number, v: number) => {
      const rgb = hsvToRgb(clamp(h, 0, 360), clamp(s, 0, 100), clamp(v, 0, 100));
      emit(rgb.r, rgb.g, rgb.b, brightness);
   };

   return (
      <div className="color-picker-container">
         <div
            className="color-swatch"
            style={{ background: hex }}
            onClick={() => setOpen(o => !o)}
         />
         {open && <>
            <div className="color-picker-backdrop" onClick={() => setOpen(false)} />
            <div className="color-picker-popover">

               {/* Color preview */}
               <div className="color-picker-preview-block" style={{ background: hex }} />

               {/* Mode toggle */}
               <div className="color-picker-mode-toggle">
                  <button
                     className={mode === 'rgb' ? 'active' : ''}
                     onClick={() => setMode('rgb')}
                  >RGB</button>
                  <button
                     className={mode === 'hsv' ? 'active' : ''}
                     onClick={() => setMode('hsv')}
                  >HSV</button>
               </div>

               {/* Channel sliders */}
               {mode === 'rgb' ? (<>
                  <SliderRow label="R" min={0} max={255} value={r}
                     gradient={`linear-gradient(to right, rgb(0,${g},${b}), rgb(255,${g},${b}))`}
                     onChange={v => emit(v, g, b, brightness)} />
                  <SliderRow label="G" min={0} max={255} value={g}
                     gradient={`linear-gradient(to right, rgb(${r},0,${b}), rgb(${r},255,${b}))`}
                     onChange={v => emit(r, v, b, brightness)} />
                  <SliderRow label="B" min={0} max={255} value={b}
                     gradient={`linear-gradient(to right, rgb(${r},${g},0), rgb(${r},${g},255))`}
                     onChange={v => emit(r, g, v, brightness)} />
               </>) : (<>
                  <SliderRow label="H" min={0} max={360} value={hsv.h}
                     gradient="linear-gradient(to right, #f00, #ff0, #0f0, #0ff, #00f, #f0f, #f00)"
                     onChange={v => emitHsv(v, hsv.s, hsv.v)} />
                  <SliderRow label="S" min={0} max={100} value={hsv.s}
                     gradient={`linear-gradient(to right, #fff, ${pureHueHex})`}
                     onChange={v => emitHsv(hsv.h, v, hsv.v)} />
                  <SliderRow label="V" min={0} max={100} value={hsv.v}
                     gradient={`linear-gradient(to right, #000, ${pureHueHex})`}
                     onChange={v => emitHsv(hsv.h, hsv.s, v)} />
               </>)}

               {/* Brightness — always visible, independent of mode */}
               <div className="color-picker-brightness-divider" />
               <SliderRow label="Bri" min={0} max={31} value={brightness}
                  gradient={`linear-gradient(to right, #000, ${hex})`}
                  onChange={v => emit(r, g, b, v)} />
               <div className="color-picker-brightness-pct">
                  {Math.round(brightness / 31 * 100)}%
               </div>

               {/* Hex display */}
               <div className="color-picker-hex">{hex}</div>

            </div>
         </>}
      </div>
   );
}
