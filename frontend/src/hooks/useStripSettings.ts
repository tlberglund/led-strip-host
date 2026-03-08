import { useState, useEffect, useCallback } from 'react';
import type { StripSetting, StripSettingInput } from '../types.ts';

export function useStripSettings() {
   const [strips, setStrips] = useState<StripSetting[]>([]);
   const [loading, setLoading] = useState(true);

   const fetchStrips = useCallback(async () => {
      try {
         const res = await fetch('/api/settings/strips');
         if(res.ok) {
            const data: StripSetting[] = await res.json();
            setStrips(data);
         }
      } catch(e) {
         console.error('Failed to fetch strips:', e);
      } finally {
         setLoading(false);
      }
   }, []);

   useEffect(() => {
      fetchStrips();
   }, [fetchStrips]);

   const addStrip = useCallback(
      async (input: StripSettingInput): Promise<{ ok: boolean; error?: string }> => {
         try {
            const res = await fetch('/api/settings/strips', {
               method: 'POST',
               headers: { 'Content-Type': 'application/json' },
               body: JSON.stringify(input),
            });
            if(res.ok) {
               const created: StripSetting = await res.json();
               setStrips((prev) => [...prev, created]);
               return { ok: true };
            }
            const msg = await res.text();
            return { ok: false, error: msg || 'Create failed' };
         } catch(e) {
            return { ok: false, error: String(e) };
         }
      },
      []
   );

   const updateStrip = useCallback(
      async (id: number, input: Partial<StripSettingInput>): Promise<{ ok: boolean; error?: string }> => {
         try {
            const res = await fetch(`/api/settings/strips/${id}`, {
               method: 'PUT',
               headers: { 'Content-Type': 'application/json' },
               body: JSON.stringify(input),
            });
            if(res.ok) {
               setStrips((prev) =>
                  prev.map((s) => (s.id === id ? { ...s, ...input } : s))
               );
               return { ok: true };
            }
            const msg = await res.text();
            return { ok: false, error: msg || 'Update failed' };
         } catch(e) {
            return { ok: false, error: String(e) };
         }
      },
      []
   );

   const deleteStrip = useCallback(
      async (id: number): Promise<{ ok: boolean; error?: string }> => {
         try {
            const res = await fetch(`/api/settings/strips/${id}`, { method: 'DELETE' });
            if(res.ok || res.status === 204) {
               setStrips((prev) => prev.filter((s) => s.id !== id));
               return { ok: true };
            }
            const msg = await res.text();
            return { ok: false, error: msg || 'Delete failed' };
         } catch(e) {
            return { ok: false, error: String(e) };
         }
      },
      []
   );

   return { strips, loading, addStrip, updateStrip, deleteStrip, refetch: fetchStrips };
}
