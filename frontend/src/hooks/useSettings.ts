import { useState, useEffect, useCallback } from 'react';
import type { ScalarSettings } from '../types.ts';

const DEFAULTS: ScalarSettings = {
   viewportWidth: 240,
   viewportHeight: 135,
   targetFPS: 60,
   scanIntervalSeconds: 15,
};

export function useSettings() {
   const [settings, setSettings] = useState<ScalarSettings>(DEFAULTS);
   const [loading, setLoading] = useState(true);

   const fetchSettings = useCallback(async () => {
      try {
         const res = await fetch('/api/settings');
         if(res.ok) {
            const data: ScalarSettings = await res.json();
            setSettings(data);
         }
      } catch(e) {
         console.error('Failed to fetch settings:', e);
      } finally {
         setLoading(false);
      }
   }, []);

   useEffect(() => {
      fetchSettings();
   }, [fetchSettings]);

   const saveSettings = useCallback(
      async (partial: Partial<ScalarSettings>): Promise<{ ok: boolean; error?: string }> => {
         try {
            const res = await fetch('/api/settings', {
               method: 'PUT',
               headers: { 'Content-Type': 'application/json' },
               body: JSON.stringify(partial),
            });
            if(res.ok) {
               setSettings((prev) => ({ ...prev, ...partial }));
               return { ok: true };
            }
            const body = await res.json().catch(() => ({}));
            return { ok: false, error: body.errors?.join(', ') ?? 'Save failed' };
         } catch(e) {
            return { ok: false, error: String(e) };
         }
      },
      []
   );

   return { settings, loading, saveSettings, refetch: fetchSettings };
}
