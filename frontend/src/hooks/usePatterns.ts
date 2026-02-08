import { useState, useEffect } from 'react';

export function usePatterns(): string[] {
   const [patterns, setPatterns] = useState<string[]>([]);

   useEffect(() => {
      async function fetchPatterns() {
         try {
            const response = await fetch('/api/patterns');
            const data: string[] = await response.json();
            setPatterns(data);
         } catch (e) {
            console.error('Failed to load patterns:', e);
         }
      }

      fetchPatterns();
   }, []);

   return patterns;
}
