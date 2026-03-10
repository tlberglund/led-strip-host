import { useState, useRef } from 'react';

interface SavePatternButtonProps {
   activePresetId: number | null;
   onSave: () => Promise<void>;
   onSaveAs: (name: string) => Promise<void>;
}

export function SavePatternButton({ activePresetId, onSave, onSaveAs }: SavePatternButtonProps) {
   const [showInput, setShowInput] = useState(false);
   const [name, setName] = useState('');
   const [error, setError] = useState<string | null>(null);
   const [busy, setBusy] = useState(false);
   const inputRef = useRef<HTMLInputElement>(null);

   async function handleSaveClick() {
      if(activePresetId !== null) {
         setBusy(true);
         setError(null);
         try {
            await onSave();
         }
         catch(e) {
            setError(e instanceof Error ? e.message : 'Save failed');
         }
         finally {
            setBusy(false);
         }
      }
      else {
         setShowInput(true);
         setTimeout(() => inputRef.current?.focus(), 0);
      }
   }

   async function handleConfirm() {
      const trimmed = name.trim();
      if(!trimmed) return;
      setBusy(true);
      setError(null);
      try {
         await onSaveAs(trimmed);
         setShowInput(false);
         setName('');
      }
      catch(e) {
         const msg = e instanceof Error ? e.message : 'Save failed';
         setError(msg.includes('already in use') ? 'Name already taken' : msg);
      }
      finally {
         setBusy(false);
      }
   }

   function handleCancel() {
      setShowInput(false);
      setName('');
      setError(null);
   }

   if(showInput) {
      return (
         <div className="save-pattern-input-wrap">
            <input
               ref={inputRef}
               className="save-as-input"
               type="text"
               placeholder="Preset name…"
               value={name}
               onChange={(e) => { setName(e.target.value); setError(null); }}
               onKeyDown={(e) => {
                  if(e.key === 'Enter') handleConfirm();
                  if(e.key === 'Escape') handleCancel();
               }}
            />
            {error && <span className="save-as-error">{error}</span>}
            <div className="save-input-actions">
               <button
                  className="settings-save-btn settings-save-btn--sm"
                  disabled={!name.trim() || busy}
                  onClick={handleConfirm}>
                  Save
               </button>
               <button
                  className="settings-save-btn settings-save-btn--sm settings-save-btn--neutral"
                  onClick={handleCancel}>
                  ✕
               </button>
            </div>
         </div>
      );
   }

   return (
      <div className="save-pattern-btn-wrap">
         <button
            className="settings-save-btn settings-save-btn--sm"
            disabled={busy}
            onClick={handleSaveClick}>
            Save
         </button>
         {error && <span className="save-as-error">{error}</span>}
      </div>
   );
}
