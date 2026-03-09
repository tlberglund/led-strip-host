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

export function ColorPicker({ value, onChange }: ColorPickerProps) {
   const [open, setOpen] = useState(false);
   const [mode, setMode] = useState<'rgb' | 'hsv'>('rgb');

   const { r, g, b, brightness } = parseValue(value);
   const hsv = rgbToHsv(r, g, b);
   const hex = `#${toHex2(r)}${toHex2(g)}${toHex2(b)}`;

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

               {/* Hue strip + preview */}
               <div className="color-picker-hue-row">
                  <div className="color-picker-preview" style={{ background: hex }} />
                  <input
                     type="range"
                     min={0}
                     max={360}
                     value={hsv.h}
                     className="color-picker-hue-slider"
                     onChange={e => emitHsv(+e.target.value, hsv.s, hsv.v)}
                  />
               </div>

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

               {/* Mode-specific numeric inputs */}
               {mode === 'rgb' ? (
                  <div className="color-picker-fields">
                     <div>
                        <label>R</label>
                        <input type="number" min={0} max={255} value={r}
                           onChange={e => emit(+e.target.value, g, b, brightness)} />
                     </div>
                     <div>
                        <label>G</label>
                        <input type="number" min={0} max={255} value={g}
                           onChange={e => emit(r, +e.target.value, b, brightness)} />
                     </div>
                     <div>
                        <label>B</label>
                        <input type="number" min={0} max={255} value={b}
                           onChange={e => emit(r, g, +e.target.value, brightness)} />
                     </div>
                  </div>
               ) : (
                  <div className="color-picker-fields">
                     <div>
                        <label>H</label>
                        <input type="number" min={0} max={360} value={hsv.h}
                           onChange={e => emitHsv(+e.target.value, hsv.s, hsv.v)} />
                     </div>
                     <div>
                        <label>S</label>
                        <input type="number" min={0} max={100} value={hsv.s}
                           onChange={e => emitHsv(hsv.h, +e.target.value, hsv.v)} />
                     </div>
                     <div>
                        <label>V</label>
                        <input type="number" min={0} max={100} value={hsv.v}
                           onChange={e => emitHsv(hsv.h, hsv.s, +e.target.value)} />
                     </div>
                  </div>
               )}

               {/* Brightness slider */}
               <div className="color-picker-brightness">
                  <div className="color-picker-brightness-header">
                     <label>Brightness</label>
                     <span>{Math.round(brightness / 31 * 100)}%</span>
                  </div>
                  <input
                     type="range"
                     min={0}
                     max={31}
                     value={brightness}
                     onChange={e => emit(r, g, b, +e.target.value)}
                  />
               </div>

               {/* Hex display */}
               <div className="color-picker-hex">{hex}</div>

            </div>
         </>}
      </div>
   );
}
