import { useState } from 'react';
import { useStripsWebSocket } from '../hooks/useStripsWebSocket.ts';
import { Sparkline } from './Sparkline.tsx';

interface StripManagerTabProps {
   active: boolean;
}

function formatUptime(ms: number): string {
   const totalSeconds = Math.floor(ms / 1000);
   const days = Math.floor(totalSeconds / 86400);
   const hours = Math.floor((totalSeconds % 86400) / 3600);
   const minutes = Math.floor((totalSeconds % 3600) / 60);
   const seconds = totalSeconds % 60;
   return `${days}d ${hours}h ${minutes}m ${seconds}s`;
}

export function StripManagerTab({ active }: StripManagerTabProps) {
   const { strips, activityLog, stripTelemetry, isScanning, isReconnecting } =
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
                  const telemetry = stripTelemetry[strip.id];

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
                        <div className="strip-telemetry">
                           {telemetry ? (
                              <>
                                 <div className="strip-telemetry-values">
                                    <span className="telemetry-item">
                                       <span className="telemetry-label">Temp</span>
                                       <span className="telemetry-value">{telemetry.temperature.toFixed(1)} °C</span>
                                    </span>
                                    <span className="telemetry-item">
                                       <span className="telemetry-label">Current</span>
                                       <span className="telemetry-value">{telemetry.current.toFixed(2)} A</span>
                                    </span>
                                    <span className="telemetry-item">
                                       <span className="telemetry-label">Uptime</span>
                                       <span className="telemetry-value">{formatUptime(telemetry.uptimeMs)}</span>
                                    </span>
                                    <span className="telemetry-item">
                                       <span className="telemetry-label">Frames</span>
                                       <span className="telemetry-value">{telemetry.frames.toLocaleString()}</span>
                                    </span>
                                 </div>
                                 <div className="strip-telemetry-charts">
                                    <div className="sparkline-row">
                                       <span className="sparkline-label">Temp</span>
                                       <Sparkline
                                          values={telemetry.history.temperature}
                                          color="orange"
                                          minVal={Math.min(50, ...telemetry.history.temperature)}
                                          maxVal={Math.max(100, ...telemetry.history.temperature)}
                                       />
                                    </div>
                                    <div className="sparkline-row">
                                       <span className="sparkline-label">Current</span>
                                       <Sparkline
                                          values={telemetry.history.current}
                                          color="cyan"
                                          minVal={0}
                                          maxVal={3}
                                       />
                                    </div>
                                 </div>
                              </>
                           ) : (
                              <span className="telemetry-awaiting">Awaiting telemetry…</span>
                           )}
                        </div>
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
