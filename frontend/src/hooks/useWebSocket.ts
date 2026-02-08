import { useEffect, useRef, useCallback } from 'react';
import type { ViewportMessage } from '../types.ts';

export function useWebSocket(
   onConnectionChange: (connected: boolean) => void,
): React.MutableRefObject<ViewportMessage | null> {
   const viewportRef = useRef<ViewportMessage | null>(null);
   const wsRef = useRef<WebSocket | null>(null);
   const reconnectTimeoutRef = useRef<ReturnType<typeof setTimeout> | null>(null);
   const onConnectionChangeRef = useRef(onConnectionChange);
   onConnectionChangeRef.current = onConnectionChange;

   const connect = useCallback(() => {
      const protocol = window.location.protocol === 'https:' ? 'wss:' : 'ws:';
      const wsUrl = `${protocol}//${window.location.host}/viewport`;

      const ws = new WebSocket(wsUrl);
      wsRef.current = ws;

      ws.onopen = () => {
         onConnectionChangeRef.current(true);
         if (reconnectTimeoutRef.current) {
            clearTimeout(reconnectTimeoutRef.current);
            reconnectTimeoutRef.current = null;
         }
      };

      ws.onclose = () => {
         onConnectionChangeRef.current(false);
         if (!reconnectTimeoutRef.current) {
            reconnectTimeoutRef.current = setTimeout(() => {
               reconnectTimeoutRef.current = null;
               connect();
            }, 2000);
         }
      };

      ws.onerror = (error) => {
         console.error('WebSocket error:', error);
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
   }, []);

   useEffect(() => {
      connect();

      return () => {
         if (reconnectTimeoutRef.current) {
            clearTimeout(reconnectTimeoutRef.current);
         }
         if (wsRef.current) {
            wsRef.current.onclose = null; // Prevent reconnect on cleanup
            wsRef.current.close();
         }
      };
   }, [connect]);

   return viewportRef;
}
