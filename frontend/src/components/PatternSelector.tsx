interface PatternSelectorProps {
   patterns: string[];
   selectedPattern: string;
   onSelect: (pattern: string) => void;
}

export function PatternSelector({ patterns, selectedPattern, onSelect }: PatternSelectorProps) {
   return (
      <div className="control-group">
         <label>Pattern</label>
         <select
            id="pattern-select"
            value={selectedPattern}
            onChange={(e) => onSelect(e.target.value)}
         >
            {patterns.length === 0 ? (
               <option value="">Loading...</option>
            ) : (
               patterns.map((p) => (
                  <option key={p} value={p}>{p}</option>
               ))
            )}
         </select>
      </div>
   );
}
