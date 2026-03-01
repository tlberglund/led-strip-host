import { useState, useEffect, useRef } from 'react';
import type { StripStatus, ActivityLogEntry, StripsWsMessage } from '../types.ts';

const MAX_LOG_ENTRIES = 50;
const BACKOFF_INITIAL_MS = 1000;
const BACKOFF_MAX_MS = 30000;

export function useStripsWebSocket(active: boolean) {
   const [strips, setStrips] = useState<StripStatus[]>([]);
   const [activityLog, setActivityLog] = useState<ActivityLogEntry[]>([]);
   const [isConnected, setIsConnected] = useState(false);
   const [isReconnecting, setIsReconnecting] = useState(false);
   const backoffRef = useRef(BACKOFF_INITIAL_MS);

   useEffect(() => {
      if(!active) return;

      let ws: WebSocket | null = null;
      let reconnectTimeout: ReturnType<typeof setTimeout> | null = null;
      let disposed = false;

      function connect() {
         if(disposed) return;

         const wsHost = import.meta.env.VITE_WS_HOST ?? window.location.host;
         const wsUrl = `${window.location.protocol === 'https:' ? 'wss:' : 'ws:'}//${wsHost}/ws/strips`;

         ws = new WebSocket(wsUrl);

         ws.onopen = () => {
            if(disposed) { ws?.close(); return; }
            backoffRef.current = BACKOFF_INITIAL_MS;
            setIsConnected(true);
            setIsReconnecting(false);
         };

         ws.onclose = () => {
            if(disposed) return;
            setIsConnected(false);
            setIsReconnecting(true);
            const delay = backoffRef.current;
            backoffRef.current = Math.min(delay * 2, BACKOFF_MAX_MS);
            reconnectTimeout = setTimeout(() => {
               reconnectTimeout = null;
               connect();
            }, delay);
         };

         ws.onerror = () => {
            // Followed by onclose; no action needed
         };

         ws.onmessage = (event) => {
            if(typeof event.data !== 'string') return;
            try {
               const msg: StripsWsMessage = JSON.parse(event.data);
               if(msg.type === 'strips_update') {
                  setStrips(msg.strips);
               }
               else if(msg.type === 'discovery_event') {
                  const entry: ActivityLogEntry = {
                     timestamp: new Date().toLocaleTimeString(),
                     message: msg.message,
                  };
                  setActivityLog((prev) => [entry, ...prev].slice(0, MAX_LOG_ENTRIES));
               }
            }
            catch {
               // Ignore malformed messages
            }
         };
      }

      connect();

      return () => {
         disposed = true;
         if(reconnectTimeout) clearTimeout(reconnectTimeout);
         if(ws) { ws.onclose = null; ws.close(); }
         setIsConnected(false);
         setIsReconnecting(false);
      };
   }, [active]);

   const isScanning = activityLog.length > 0 &&
      activityLog[0].message.startsWith('Scanning');

   return { strips, activityLog, isScanning, isConnected, isReconnecting };
}
