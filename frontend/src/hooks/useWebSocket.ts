import { useEffect, useRef } from 'react';
import type { ViewportMessage } from '../types.ts';

export function useWebSocket(
   onConnectionChange: (connected: boolean) => void,
): React.MutableRefObject<ViewportMessage | null> {
   const viewportRef = useRef<ViewportMessage | null>(null);
   const onConnectionChangeRef = useRef(onConnectionChange);
   onConnectionChangeRef.current = onConnectionChange;

   useEffect(() => {
      let ws: WebSocket | null = null;
      let reconnectTimeout: ReturnType<typeof setTimeout> | null = null;
      let disposed = false;

      function connect() {
         if (disposed) return;

         const protocol = window.location.protocol === 'https:' ? 'wss:' : 'ws:';
         const wsUrl = `${protocol}//${window.location.host}/viewport`;

         ws = new WebSocket(wsUrl);

         ws.onopen = () => {
            if (disposed) {
               ws?.close();
               return;
            }
            onConnectionChangeRef.current(true);
         };

         ws.onclose = () => {
            if (disposed) return;
            onConnectionChangeRef.current(false);

            // Reconnect after 2 seconds
            reconnectTimeout = setTimeout(() => {
               reconnectTimeout = null;
               connect();
            }, 2000);
         };

         ws.onerror = () => {
            // Error is followed by onclose, so no action needed here
         };

         ws.onmessage = (event) => {
            try {
               const data = JSON.parse(event.data);
               if (data.type === 'viewport') {
                  viewportRef.current = data as ViewportMessage;
               }
            } catch (e) {
               console.error('Failed to parse message:', e);
            }
         };
      }

      connect();

      return () => {
         disposed = true;
         if (reconnectTimeout) {
            clearTimeout(reconnectTimeout);
         }
         if (ws) {
            ws.onclose = null;
            ws.close();
         }
         onConnectionChangeRef.current(false);
      };
   }, []);

   return viewportRef;
}
