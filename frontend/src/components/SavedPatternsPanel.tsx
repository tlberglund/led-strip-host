import { useState, useRef } from 'react';
import { useSavedPatterns } from '../hooks/useSavedPatterns.ts';
import type { SavedPreset } from '../hooks/useSavedPatterns.ts';

interface SavedPatternsPanelProps {
   onLoad: (patternName: string, params: Record<string, unknown>) => void;
   currentPatternName: string;
   currentParams: Record<string, number | string>;
}

interface Feedback {
   message: string;
   kind: 'success' | 'error';
}

export function SavedPatternsPanel({ onLoad, currentPatternName, currentParams }: SavedPatternsPanelProps) {
   const { presets, loading, error, savePreset, updatePreset, deletePreset, renamePreset } = useSavedPatterns();

   const [saveAsName, setSaveAsName] = useState('');
   const [saveAsError, setSaveAsError] = useState<string | null>(null);
   const [feedback, setFeedback] = useState<Feedback | null>(null);
   const feedbackTimerRef = useRef<ReturnType<typeof setTimeout> | null>(null);

   const [loadedPresetId, setLoadedPresetId] = useState<number | null>(null);
   const [editingId, setEditingId] = useState<number | null>(null);
   const [editingName, setEditingName] = useState('');
   const [deletingId, setDeletingId] = useState<number | null>(null);

   function showFeedback(message: string, kind: 'success' | 'error') {
      setFeedback({ message, kind });
      if(feedbackTimerRef.current) clearTimeout(feedbackTimerRef.current);
      feedbackTimerRef.current = setTimeout(() => setFeedback(null), 4000);
   }

   function handleLoad(preset: SavedPreset) {
      onLoad(preset.patternName, preset.params);
      setLoadedPresetId(preset.id);
   }

   async function handleSaveAs() {
      const name = saveAsName.trim();
      if(!name) return;
      setSaveAsError(null);
      try {
         await savePreset(name, currentPatternName, currentParams as Record<string, unknown>);
         setSaveAsName('');
         showFeedback(`Saved "${name}"`, 'success');
      }
      catch(e) {
         const msg = e instanceof Error ? e.message : 'Save failed';
         if(msg.includes('already in use')) {
            setSaveAsError('Name already in use');
         }
         else {
            showFeedback(msg, 'error');
         }
      }
   }

   async function handleUpdate() {
      if(loadedPresetId === null) return;
      try {
         await updatePreset(loadedPresetId, {
            patternName: currentPatternName,
            params: currentParams as Record<string, unknown>,
         });
         showFeedback('Preset updated', 'success');
      }
      catch(e) {
         showFeedback(e instanceof Error ? e.message : 'Update failed', 'error');
      }
   }

   function startEdit(preset: SavedPreset) {
      setEditingId(preset.id);
      setEditingName(preset.presetName);
      setDeletingId(null);
   }

   async function commitEdit(preset: SavedPreset) {
      const name = editingName.trim();
      if(!name || name === preset.presetName) {
         setEditingId(null);
         return;
      }
      try {
         await renamePreset(preset.id, name);
         setEditingId(null);
         showFeedback(`Renamed to "${name}"`, 'success');
      }
      catch(e) {
         showFeedback(e instanceof Error ? e.message : 'Rename failed', 'error');
         setEditingId(null);
      }
   }

   async function confirmDelete(id: number) {
      try {
         await deletePreset(id);
         if(loadedPresetId === id) setLoadedPresetId(null);
         setDeletingId(null);
         showFeedback('Preset deleted', 'success');
      }
      catch(e) {
         showFeedback(e instanceof Error ? e.message : 'Delete failed', 'error');
         setDeletingId(null);
      }
   }

   if(loading) return <p className="settings-loading">Loading presets…</p>;
   if(error) return <p className="settings-feedback settings-feedback--error">{error}</p>;

   return (
      <div className="saved-patterns-panel">
         <h3 className="saved-patterns-heading">Saved Patterns</h3>

         {presets.length === 0
            ? <p className="saved-patterns-empty">No saved patterns yet</p>
            : (
               <ul className="saved-preset-list">
                  {presets.map((preset) => (
                     <li
                        key={preset.id}
                        className={`saved-preset-row${loadedPresetId === preset.id ? ' preset-active' : ''}`}>

                        <div className="saved-preset-info">
                           {editingId === preset.id
                              ? (
                                 <input
                                    className="preset-name-input"
                                    value={editingName}
                                    autoFocus
                                    onChange={(e) => setEditingName(e.target.value)}
                                    onKeyDown={(e) => {
                                       if(e.key === 'Enter') commitEdit(preset);
                                       if(e.key === 'Escape') setEditingId(null);
                                    }}
                                    onBlur={() => commitEdit(preset)}
                                 />
                              )
                              : <span className="saved-preset-name">{preset.presetName}</span>
                           }
                           <span className="saved-preset-badge">{preset.patternName}</span>
                        </div>

                        <div className="saved-preset-actions">
                           {deletingId === preset.id
                              ? (
                                 <>
                                    <span className="saved-preset-confirm-label">Delete?</span>
                                    <button
                                       className="settings-save-btn settings-save-btn--sm settings-save-btn--danger"
                                       onClick={() => confirmDelete(preset.id)}>
                                       Yes
                                    </button>
                                    <button
                                       className="settings-save-btn settings-save-btn--sm settings-save-btn--neutral"
                                       onClick={() => setDeletingId(null)}>
                                       No
                                    </button>
                                 </>
                              )
                              : (
                                 <>
                                    <button
                                       className="settings-save-btn settings-save-btn--sm"
                                       onClick={() => handleLoad(preset)}>
                                       Load
                                    </button>
                                    <button
                                       className="settings-save-btn settings-save-btn--sm settings-save-btn--neutral"
                                       title="Rename"
                                       onClick={() => startEdit(preset)}>
                                       ✎
                                    </button>
                                    <button
                                       className="settings-save-btn settings-save-btn--sm settings-save-btn--danger"
                                       title="Delete"
                                       onClick={() => { setDeletingId(preset.id); setEditingId(null); }}>
                                       ✕
                                    </button>
                                 </>
                              )
                           }
                        </div>
                     </li>
                  ))}
               </ul>
            )
         }

         <div className="save-as-row">
            <div className="save-as-input-wrap">
               <input
                  className="save-as-input"
                  type="text"
                  placeholder="Preset name…"
                  value={saveAsName}
                  onChange={(e) => { setSaveAsName(e.target.value); setSaveAsError(null); }}
                  onKeyDown={(e) => { if(e.key === 'Enter') handleSaveAs(); }}
               />
               {saveAsError && <span className="save-as-error">{saveAsError}</span>}
            </div>
            <button
               className="settings-save-btn settings-save-btn--sm"
               disabled={!saveAsName.trim()}
               onClick={handleSaveAs}>
               Save As…
            </button>
            {loadedPresetId !== null && (
               <button
                  className="settings-save-btn settings-save-btn--sm settings-save-btn--neutral"
                  onClick={handleUpdate}>
                  Update
               </button>
            )}
         </div>

         {feedback && (
            <div className={`settings-feedback settings-feedback--${feedback.kind}`}>
               {feedback.message}
            </div>
         )}
      </div>
   );
}
