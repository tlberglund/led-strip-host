import type { ParameterDef } from '../types.ts';

interface ParameterControlProps {
   param: ParameterDef;
   value: number | string;
   onChange: (name: string, value: number | string) => void;
}

export function ParameterControl({ param, value, onChange }: ParameterControlProps) {
   if (param.type === 'float' || param.type === 'int') {
      const numValue = typeof value === 'number' ? value : param.default;
      const displayValue = param.type === 'float'
         ? numValue.toFixed(param.step < 1 ? 2 : 1)
         : String(numValue);

      return (
         <div className="control-group">
            <label>{param.label}</label>
            <input
               type="range"
               min={param.min}
               max={param.max}
               step={param.step}
               value={numValue}
               onChange={(e) => {
                  const parsed = param.type === 'float'
                     ? parseFloat(e.target.value)
                     : parseInt(e.target.value);
                  onChange(param.name, parsed);
               }}
            />
            <span className="slider-value">{displayValue}</span>
         </div>
      );
   }

   if (param.type === 'select') {
      const strValue = typeof value === 'string' ? value : param.default;

      return (
         <div className="control-group">
            <label>{param.label}</label>
            <select
               value={strValue}
               onChange={(e) => onChange(param.name, e.target.value)}
            >
               {param.options.map((opt) => (
                  <option key={opt} value={opt}>{opt}</option>
               ))}
            </select>
         </div>
      );
   }

   if (param.type === 'color') {
      const colorValue = typeof value === 'string' ? value : param.default;

      return (
         <div className="control-group">
            <label>{param.label}</label>
            <input
               type="color"
               value={colorValue}
               onChange={(e) => onChange(param.name, e.target.value)}
            />
         </div>
      );
   }

   return null;
}
