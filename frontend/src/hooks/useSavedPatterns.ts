import { useState, useEffect, useCallback } from 'react';

export interface SavedPreset {
   id: number;
   presetName: string;
   patternName: string;
   params: Record<string, unknown>;
   updatedAt: number;
}

interface UseSavedPatternsResult {
   presets: SavedPreset[];
   loading: boolean;
   error: string | null;
   savePreset: (presetName: string, patternName: string, params: Record<string, unknown>) => Promise<SavedPreset>;
   updatePreset: (id: number, patch: { presetName?: string; patternName?: string; params?: Record<string, unknown> }) => Promise<SavedPreset>;
   deletePreset: (id: number) => Promise<void>;
   renamePreset: (id: number, newName: string) => Promise<SavedPreset>;
   refresh: () => Promise<void>;
}

export function useSavedPatterns(): UseSavedPatternsResult {
   const [presets, setPresets] = useState<SavedPreset[]>([]);
   const [loading, setLoading] = useState(true);
   const [error, setError] = useState<string | null>(null);

   const fetchPresets = useCallback(async () => {
      try {
         setLoading(true);
         const response = await fetch('/api/saved-patterns');
         if(!response.ok) throw new Error(`Failed to fetch presets: ${response.status}`);
         const data: SavedPreset[] = await response.json();
         setPresets(data);
         setError(null);
      }
      catch(e) {
         setError(e instanceof Error ? e.message : 'Failed to load presets');
      }
      finally {
         setLoading(false);
      }
   }, []);

   useEffect(() => {
      fetchPresets();
   }, [fetchPresets]);

   const savePreset = useCallback(async (
      presetName: string,
      patternName: string,
      params: Record<string, unknown>
   ): Promise<SavedPreset> => {
      const response = await fetch('/api/saved-patterns', {
         method: 'POST',
         headers: { 'Content-Type': 'application/json' },
         body: JSON.stringify({ presetName, patternName, params }),
      });
      if(response.status === 409) throw new Error('Name already in use');
      if(!response.ok) throw new Error(`Failed to save preset: ${response.status}`);
      const created: SavedPreset = await response.json();
      await fetchPresets();
      return created;
   }, [fetchPresets]);

   const updatePreset = useCallback(async (
      id: number,
      patch: { presetName?: string; patternName?: string; params?: Record<string, unknown> }
   ): Promise<SavedPreset> => {
      const response = await fetch(`/api/saved-patterns/${id}`, {
         method: 'PUT',
         headers: { 'Content-Type': 'application/json' },
         body: JSON.stringify(patch),
      });
      if(response.status === 409) throw new Error('Name already in use');
      if(response.status === 404) throw new Error('Preset not found');
      if(!response.ok) throw new Error(`Failed to update preset: ${response.status}`);
      const updated: SavedPreset = await response.json();
      await fetchPresets();
      return updated;
   }, [fetchPresets]);

   const deletePreset = useCallback(async (id: number): Promise<void> => {
      const response = await fetch(`/api/saved-patterns/${id}`, { method: 'DELETE' });
      if(response.status === 404) throw new Error('Preset not found');
      if(!response.ok) throw new Error(`Failed to delete preset: ${response.status}`);
      await fetchPresets();
   }, [fetchPresets]);

   const renamePreset = useCallback(async (id: number, newName: string): Promise<SavedPreset> => {
      return updatePreset(id, { presetName: newName });
   }, [updatePreset]);

   return {
      presets,
      loading,
      error,
      savePreset,
      updatePreset,
      deletePreset,
      renamePreset,
      refresh: fetchPresets,
   };
}
