import { useState, useEffect } from 'react';

export function useBackgroundImage(): string | null {
   const [imageUrl, setImageUrl] = useState<string | null>(null);

   useEffect(() => {
      let objectUrl: string | null = null;

      async function fetchImage() {
         try {
            const response = await fetch('/api/background-image');
            if (!response.ok) {
               // No background image configured or not found
               return;
            }
            const blob = await response.blob();
            objectUrl = URL.createObjectURL(blob);
            setImageUrl(objectUrl);
         } catch {
            // Silently ignore errors
         }
      }

      fetchImage();

      return () => {
         if (objectUrl) {
            URL.revokeObjectURL(objectUrl);
         }
      };
   }, []);

   return imageUrl;
}
