import { useState, useEffect } from 'react';
import type { PatternInfo } from '../types.ts';

export function usePatterns(): PatternInfo[] {
   const [patterns, setPatterns] = useState<PatternInfo[]>([]);

   useEffect(() => {
      async function fetchPatterns() {
         try {
            const response = await fetch('/api/patterns');
            const data: PatternInfo[] = await response.json();
            setPatterns(data);
         } catch (e) {
            console.error('Failed to load patterns:', e);
         }
      }

      fetchPatterns();
   }, []);

   return patterns;
}
