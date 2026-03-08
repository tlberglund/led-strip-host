import { useState, useRef, useCallback } from 'react';
import { useSettings } from '../hooks/useSettings.ts';
import { useStripSettings } from '../hooks/useStripSettings.ts';
import type { StripSetting, StripSettingInput } from '../types.ts';

// ── Feedback helper ────────────────────────────────────────────────────────

type FeedbackState = { type: 'success' | 'error'; message: string } | null;

function useFeedback() {
   const [feedback, setFeedback] = useState<FeedbackState>(null);
   const timer = useRef<ReturnType<typeof setTimeout> | null>(null);

   const show = useCallback((type: 'success' | 'error', message: string) => {
      if(timer.current) clearTimeout(timer.current);
      setFeedback({ type, message });
      timer.current = setTimeout(() => setFeedback(null), 4000);
   }, []);

   return { feedback, show };
}

// ── Section card wrapper ───────────────────────────────────────────────────

function SectionCard({ title, children }: { title: string; children: React.ReactNode }) {
   return (
      <div className="settings-card">
         <h2 className="settings-card-title">{title}</h2>
         {children}
      </div>
   );
}

// ── Feedback banner ────────────────────────────────────────────────────────

function FeedbackBanner({ feedback }: { feedback: FeedbackState }) {
   if(!feedback) return null;
   return (
      <div className={`settings-feedback settings-feedback--${feedback.type}`}>
         {feedback.message}
      </div>
   );
}

// ── Viewport & Performance section ────────────────────────────────────────

function ViewportSection() {
   const { settings, loading, saveSettings } = useSettings();
   const { feedback, show } = useFeedback();

   const [w, setW] = useState('');
   const [h, setH] = useState('');
   const [fps, setFps] = useState('');
   const [scan, setScan] = useState('');
   const [dirty, setDirty] = useState(false);

   // Sync local state once settings load
   const synced = useRef(false);
   if(!loading && !synced.current) {
      synced.current = true;
      setW(String(settings.viewportWidth));
      setH(String(settings.viewportHeight));
      setFps(String(settings.targetFPS));
      setScan(String(settings.scanIntervalSeconds));
   }

   const valid =
      Number(w) > 0 && Number(h) > 0 && Number(fps) > 0 && Number(scan) > 0 &&
      Number.isInteger(Number(w)) && Number.isInteger(Number(h)) &&
      Number.isInteger(Number(fps)) && Number.isInteger(Number(scan));

   const handleSave = async () => {
      const result = await saveSettings({
         viewportWidth: Number(w),
         viewportHeight: Number(h),
         targetFPS: Number(fps),
         scanIntervalSeconds: Number(scan),
      });
      if(result.ok) {
         show('success', 'Settings saved');
         setDirty(false);
      } else {
         show('error', result.error ?? 'Save failed');
      }
   };

   if(loading) return <p className="settings-loading">Loading…</p>;

   return (
      <>
         <div className="settings-fields">
            <div className="settings-field">
               <label>Viewport Width (px)</label>
               <input
                  type="number"
                  min="1"
                  value={w}
                  onChange={(e) => { setW(e.target.value); setDirty(true); }}
               />
            </div>
            <div className="settings-field">
               <label>Viewport Height (px)</label>
               <input
                  type="number"
                  min="1"
                  value={h}
                  onChange={(e) => { setH(e.target.value); setDirty(true); }}
               />
            </div>
            <div className="settings-field">
               <label>Target FPS</label>
               <input
                  type="number"
                  min="1"
                  value={fps}
                  onChange={(e) => { setFps(e.target.value); setDirty(true); }}
               />
            </div>
            <div className="settings-field">
               <label>BLE Scan Interval (sec)</label>
               <input
                  type="number"
                  min="1"
                  value={scan}
                  onChange={(e) => { setScan(e.target.value); setDirty(true); }}
               />
            </div>
         </div>
         <button
            className="settings-save-btn"
            onClick={handleSave}
            disabled={!valid || !dirty}
         >
            Save
         </button>
         <FeedbackBanner feedback={feedback} />
      </>
   );
}

// ── Background Image section ───────────────────────────────────────────────

function BackgroundImageSection() {
   const { feedback, show } = useFeedback();
   const [previewUrl, setPreviewUrl] = useState<string | null>(null);
   const [hasImage, setHasImage] = useState(false);
   const [dragging, setDragging] = useState(false);
   const fileInputRef = useRef<HTMLInputElement>(null);

   // Check if an image is already stored
   const checkImage = useCallback(async () => {
      try {
         const res = await fetch('/api/background-image', { method: 'HEAD' });
         setHasImage(res.ok);
      } catch {
         setHasImage(false);
      }
   }, []);

   useState(() => {
      checkImage();
      fetch('/api/background-image')
         .then((r) => (r.ok ? r.blob() : null))
         .then((blob) => {
            if(blob) setPreviewUrl(URL.createObjectURL(blob));
         })
         .catch(() => {});
   });

   const upload = useCallback(async (file: File) => {
      if(!file.type.startsWith('image/')) {
         show('error', 'Only image files are accepted');
         return;
      }
      const form = new FormData();
      form.append('image', file);
      try {
         const res = await fetch('/api/settings/background-image', {
            method: 'POST',
            body: form,
         });
         if(res.ok) {
            const url = URL.createObjectURL(file);
            setPreviewUrl(url);
            setHasImage(true);
            show('success', 'Background image uploaded');
         } else {
            show('error', `Upload failed: ${await res.text()}`);
         }
      } catch(e) {
         show('error', String(e));
      }
   }, [show]);

   const handleRemove = async () => {
      try {
         const res = await fetch('/api/settings/background-image', { method: 'DELETE' });
         if(res.ok || res.status === 204) {
            setPreviewUrl(null);
            setHasImage(false);
            show('success', 'Background image removed');
         } else {
            show('error', 'Remove failed');
         }
      } catch(e) {
         show('error', String(e));
      }
   };

   const handleDrop = useCallback((e: React.DragEvent) => {
      e.preventDefault();
      setDragging(false);
      const file = e.dataTransfer.files[0];
      if(file) upload(file);
   }, [upload]);

   const handleFileChange = (e: React.ChangeEvent<HTMLInputElement>) => {
      const file = e.target.files?.[0];
      if(file) upload(file);
      e.target.value = '';
   };

   return (
      <>
         <div
            className={`settings-dropzone ${dragging ? 'settings-dropzone--active' : ''}`}
            onDragOver={(e) => { e.preventDefault(); setDragging(true); }}
            onDragLeave={() => setDragging(false)}
            onDrop={handleDrop}
            onClick={() => fileInputRef.current?.click()}
         >
            {previewUrl ? (
               <img src={previewUrl} alt="Background preview" className="settings-bg-preview" />
            ) : (
               <span className="settings-dropzone-hint">
                  Drop an image here or click to browse
               </span>
            )}
         </div>
         <input
            ref={fileInputRef}
            type="file"
            accept="image/*"
            style={{ display: 'none' }}
            onChange={handleFileChange}
         />
         <div className="settings-bg-actions">
            <button className="settings-save-btn" onClick={() => fileInputRef.current?.click()}>
               Choose File
            </button>
            {hasImage && (
               <button className="settings-save-btn settings-save-btn--danger" onClick={handleRemove}>
                  Remove
               </button>
            )}
         </div>
         <FeedbackBanner feedback={feedback} />
      </>
   );
}

// ── Strip Controllers section ──────────────────────────────────────────────

function emptyInput(): StripSettingInput {
   return { btName: '', length: null, startX: null, startY: null, endX: null, endY: null, reverse: false };
}

function StripRow({
   strip,
   onSave,
   onDelete,
}: {
   strip: StripSetting;
   onSave: (id: number, input: Partial<StripSettingInput>) => Promise<{ ok: boolean; error?: string }>;
   onDelete: (id: number) => Promise<{ ok: boolean; error?: string }>;
}) {
   const [editing, setEditing] = useState(false);
   const [form, setForm] = useState<StripSettingInput>({
      btName: strip.btName,
      length: strip.length,
      startX: strip.startX,
      startY: strip.startY,
      endX: strip.endX,
      endY: strip.endY,
      reverse: strip.reverse,
   });
   const [confirming, setConfirming] = useState(false);
   const { feedback, show } = useFeedback();

   const handleSave = async () => {
      const result = await onSave(strip.id, form);
      if(result.ok) {
         setEditing(false);
         show('success', 'Strip updated');
      } else {
         show('error', result.error ?? 'Update failed');
      }
   };

   const handleDelete = async () => {
      const result = await onDelete(strip.id);
      if(!result.ok) show('error', result.error ?? 'Delete failed');
   };

   return (
      <div className="settings-strip-row">
         {editing ? (
            <div className="settings-strip-edit">
               <div className="settings-strip-edit-grid">
                  <div className="settings-field">
                     <label>BT Name</label>
                     <input value={form.btName} onChange={(e) => setForm({ ...form, btName: e.target.value })} />
                  </div>
                  <div className="settings-field">
                     <label>Length</label>
                     <input type="number" min="1"
                        value={form.length ?? ''}
                        onChange={(e) => setForm({ ...form, length: Number(e.target.value) || null })} />
                  </div>
                  <div className="settings-field">
                     <label>Start X</label>
                     <input type="number"
                        value={form.startX ?? ''}
                        onChange={(e) => setForm({ ...form, startX: Number(e.target.value) || null })} />
                  </div>
                  <div className="settings-field">
                     <label>Start Y</label>
                     <input type="number"
                        value={form.startY ?? ''}
                        onChange={(e) => setForm({ ...form, startY: Number(e.target.value) || null })} />
                  </div>
                  <div className="settings-field">
                     <label>End X</label>
                     <input type="number"
                        value={form.endX ?? ''}
                        onChange={(e) => setForm({ ...form, endX: Number(e.target.value) || null })} />
                  </div>
                  <div className="settings-field">
                     <label>End Y</label>
                     <input type="number"
                        value={form.endY ?? ''}
                        onChange={(e) => setForm({ ...form, endY: Number(e.target.value) || null })} />
                  </div>
                  <div className="settings-field settings-field--checkbox">
                     <label>
                        <input type="checkbox"
                           checked={form.reverse}
                           onChange={(e) => setForm({ ...form, reverse: e.target.checked })} />
                        Reverse
                     </label>
                  </div>
               </div>
               <div className="settings-strip-edit-actions">
                  <button className="settings-save-btn" onClick={handleSave}>Save</button>
                  <button className="settings-save-btn settings-save-btn--neutral"
                     onClick={() => setEditing(false)}>Cancel</button>
               </div>
            </div>
         ) : (
            <div className="settings-strip-summary">
               <div className="settings-strip-summary-info">
                  <span className="settings-strip-name">#{strip.id} — {strip.btName}</span>
                  <span className="settings-strip-meta">
                     {strip.length ?? '?'} LEDs &nbsp;·&nbsp;
                     ({strip.startX},{strip.startY}) → ({strip.endX},{strip.endY})
                     {strip.reverse ? ' · reversed' : ''}
                  </span>
               </div>
               <div className="settings-strip-summary-actions">
                  <button className="settings-save-btn settings-save-btn--sm"
                     onClick={() => setEditing(true)}>Edit</button>
                  {confirming ? (
                     <>
                        <span className="settings-confirm-label">Delete?</span>
                        <button className="settings-save-btn settings-save-btn--danger settings-save-btn--sm"
                           onClick={handleDelete}>Yes</button>
                        <button className="settings-save-btn settings-save-btn--neutral settings-save-btn--sm"
                           onClick={() => setConfirming(false)}>No</button>
                     </>
                  ) : (
                     <button className="settings-save-btn settings-save-btn--danger settings-save-btn--sm"
                        onClick={() => setConfirming(true)}>Delete</button>
                  )}
               </div>
            </div>
         )}
         <FeedbackBanner feedback={feedback} />
      </div>
   );
}

function StripsSection() {
   const { strips, loading, addStrip, updateStrip, deleteStrip } = useStripSettings();
   const { feedback, show } = useFeedback();
   const [adding, setAdding] = useState(false);
   const [newStrip, setNewStrip] = useState<StripSettingInput>(emptyInput());

   const handleAdd = async () => {
      if(!newStrip.btName.trim()) {
         show('error', 'BT Name is required');
         return;
      }
      if(!newStrip.length || newStrip.length <= 0) {
         show('error', 'Length must be a positive number');
         return;
      }
      const result = await addStrip(newStrip);
      if(result.ok) {
         setNewStrip(emptyInput());
         setAdding(false);
         show('success', 'Strip added');
      } else {
         show('error', result.error ?? 'Add failed');
      }
   };

   if(loading) return <p className="settings-loading">Loading…</p>;

   return (
      <>
         <div className="settings-strips-list">
            {strips.length === 0 ? (
               <p className="settings-empty">No strips configured.</p>
            ) : (
               strips.map((strip) => (
                  <StripRow
                     key={strip.id}
                     strip={strip}
                     onSave={updateStrip}
                     onDelete={deleteStrip}
                  />
               ))
            )}
         </div>

         {adding ? (
            <div className="settings-strip-add">
               <h3 className="settings-strip-add-title">Add Strip</h3>
               <div className="settings-strip-edit-grid">
                  <div className="settings-field">
                     <label>BT Name *</label>
                     <input
                        placeholder="strip00"
                        value={newStrip.btName}
                        onChange={(e) => setNewStrip({ ...newStrip, btName: e.target.value })}
                     />
                  </div>
                  <div className="settings-field">
                     <label>Length *</label>
                     <input type="number" min="1"
                        value={newStrip.length ?? ''}
                        onChange={(e) => setNewStrip({ ...newStrip, length: Number(e.target.value) || null })}
                     />
                  </div>
                  <div className="settings-field">
                     <label>Start X</label>
                     <input type="number"
                        value={newStrip.startX ?? ''}
                        onChange={(e) => setNewStrip({ ...newStrip, startX: Number(e.target.value) || null })}
                     />
                  </div>
                  <div className="settings-field">
                     <label>Start Y</label>
                     <input type="number"
                        value={newStrip.startY ?? ''}
                        onChange={(e) => setNewStrip({ ...newStrip, startY: Number(e.target.value) || null })}
                     />
                  </div>
                  <div className="settings-field">
                     <label>End X</label>
                     <input type="number"
                        value={newStrip.endX ?? ''}
                        onChange={(e) => setNewStrip({ ...newStrip, endX: Number(e.target.value) || null })}
                     />
                  </div>
                  <div className="settings-field">
                     <label>End Y</label>
                     <input type="number"
                        value={newStrip.endY ?? ''}
                        onChange={(e) => setNewStrip({ ...newStrip, endY: Number(e.target.value) || null })}
                     />
                  </div>
                  <div className="settings-field settings-field--checkbox">
                     <label>
                        <input type="checkbox"
                           checked={newStrip.reverse}
                           onChange={(e) => setNewStrip({ ...newStrip, reverse: e.target.checked })}
                        />
                        Reverse
                     </label>
                  </div>
               </div>
               <div className="settings-strip-edit-actions">
                  <button className="settings-save-btn" onClick={handleAdd}>Add Strip</button>
                  <button className="settings-save-btn settings-save-btn--neutral"
                     onClick={() => { setAdding(false); setNewStrip(emptyInput()); }}>
                     Cancel
                  </button>
               </div>
               <FeedbackBanner feedback={feedback} />
            </div>
         ) : (
            <button className="settings-save-btn settings-add-strip-btn"
               onClick={() => setAdding(true)}>
               + Add Strip
            </button>
         )}

         {!adding && <FeedbackBanner feedback={feedback} />}
      </>
   );
}

// ── Root SettingsTab ───────────────────────────────────────────────────────

export function SettingsTab() {
   return (
      <div className="settings-tab">
         <SectionCard title="Viewport & Performance">
            <ViewportSection />
         </SectionCard>

         <SectionCard title="Background Image">
            <BackgroundImageSection />
         </SectionCard>

         <SectionCard title="Strip Controllers">
            <StripsSection />
         </SectionCard>
      </div>
   );
}
