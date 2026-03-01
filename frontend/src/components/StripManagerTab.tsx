import { useState } from 'react';
import { useStripsWebSocket } from '../hooks/useStripsWebSocket.ts';

interface StripManagerTabProps {
   active: boolean;
}

export function StripManagerTab({ active }: StripManagerTabProps) {
   const { strips, activityLog, isScanning, isReconnecting } =
      useStripsWebSocket(active);

   // Track optimistic connection states per strip id
   const [optimistic, setOptimistic] = useState<Record<number, string>>({});

   const handleDisconnect = async (id: number) => {
      setOptimistic((prev) => ({ ...prev, [id]: 'disconnected' }));
      await fetch(`/api/strips/${id}/disconnect`, { method: 'POST' });
   };

   const handleReconnect = async (id: number) => {
      setOptimistic((prev) => ({ ...prev, [id]: 'connecting' }));
      await fetch(`/api/strips/${id}/connect`, { method: 'POST' });
   };

   if(!active) return null;

   return (
      <div className="strips-tab">
         <div className="strips-tab-header">
            {isReconnecting && (
               <span className="strip-reconnecting">Reconnecting…</span>
            )}
            {isScanning && (
               <span className="strip-scanning">Scanning…</span>
            )}
         </div>

         <div className="strip-list">
            {strips.length === 0 ? (
               <p className="strips-empty">No strip controllers discovered.</p>
            ) : (
               strips.map((strip) => {
                  const overrideStatus = optimistic[strip.id];
                  const isConnected = overrideStatus
                     ? overrideStatus === 'connected'
                     : strip.connected;
                  const isConnecting = overrideStatus === 'connecting';

                  return (
                     <div key={strip.id} className="strip-row">
                        <div className="strip-row-header">
                           <span className="strip-name">Strip {strip.id} — {strip.name}</span>
                           <span className={`strip-badge ${
                              isConnecting ? 'connecting' :
                              isConnected ? 'connected' : 'disconnected'
                           }`}>
                              {isConnecting ? 'Connecting…' :
                               isConnected ? 'Connected' : 'Disconnected'}
                           </span>
                        </div>
                        <div className="strip-meta">
                           <span>{strip.length} LEDs</span>
                           <span>{strip.address}</span>
                        </div>
                        {isConnected && !isConnecting && (
                           <button
                              className="strip-action-btn btn-danger"
                              onClick={() => handleDisconnect(strip.id)}
                           >
                              Disconnect
                           </button>
                        )}
                        {!isConnected && !isConnecting && (
                           <button
                              className="strip-action-btn"
                              onClick={() => handleReconnect(strip.id)}
                           >
                              Reconnect
                           </button>
                        )}
                     </div>
                  );
               })
            )}
         </div>

         <div className="strips-activity-log">
            <h3 className="activity-log-title">Discovery Activity</h3>
            {activityLog.length === 0 ? (
               <p className="activity-log-empty">Waiting for discovery events…</p>
            ) : (
               <ul className="activity-log-list">
                  {activityLog.map((entry, i) => (
                     <li key={i} className="activity-log-entry">
                        <span className="activity-log-time">{entry.timestamp}</span>
                        <span className="activity-log-msg">{entry.message}</span>
                     </li>
                  ))}
               </ul>
            )}
         </div>
      </div>
   );
}
