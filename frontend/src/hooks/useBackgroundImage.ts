import { useState, useEffect } from 'react';

export function useBackgroundImage(): string | null {
   const [imageUrl, setImageUrl] = useState<string | null>(null);

   useEffect(() => {
      let objectUrl: string | null = null;
      let cancelled = false;
      let retryTimer: ReturnType<typeof setTimeout> | null = null;

      async function fetchImage() {
         try {
            const response = await fetch('/api/background-image');
            if(response.ok) {
               const blob = await response.blob();
               if(!cancelled) {
                  objectUrl = URL.createObjectURL(blob);
                  setImageUrl(objectUrl);
               }
               return; // success — stop retrying
            }
         }
         catch {
            // network error — fall through to retry
         }

         if(!cancelled) {
            retryTimer = setTimeout(fetchImage, 3000);
         }
      }

      fetchImage();

      return () => {
         cancelled = true;
         if (retryTimer) clearTimeout(retryTimer);
         if (objectUrl) URL.revokeObjectURL(objectUrl);
      };
   }, []);

   return imageUrl;
}
