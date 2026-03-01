import { useState, useEffect, useCallback } from 'react';
import type { StripStatus } from '../types.ts';

export function useStrips(active: boolean) {
   const [strips, setStrips] = useState<StripStatus[]>([]);
   const [refetchKey, setRefetchKey] = useState(0);

   useEffect(() => {
      if(!active) return;

      const fetchStrips = () => {
         fetch('/api/strips')
            .then((r) => r.json())
            .then(setStrips)
            .catch(console.error);
      };

      fetchStrips();
      const interval = setInterval(fetchStrips, 3000);
      return () => clearInterval(interval);
   }, [active, refetchKey]);

   const refetch = useCallback(() => setRefetchKey((k) => k + 1), []);

   return { strips, refetch };
}
