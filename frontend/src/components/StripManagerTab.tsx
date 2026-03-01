import { useStrips } from '../hooks/useStrips.ts';

interface StripManagerTabProps {
   active: boolean;
}

export function StripManagerTab({ active }: StripManagerTabProps) {
   const { strips, refetch } = useStrips(active);

   const handleConnect = async (id: number) => {
      await fetch(`/api/strips/${id}/connect`, { method: 'POST' });
      refetch();
   };

   const handleDisconnect = async (id: number) => {
      await fetch(`/api/strips/${id}/disconnect`, { method: 'POST' });
      refetch();
   };

   if(!active) return null;

   if(strips.length === 0) {
      return <p className="strips-empty">No strip controllers discovered.</p>;
   }

   return (
      <div className="strip-list">
         {strips.map((strip) => (
            <div key={strip.id} className="strip-row">
               <div className="strip-row-header">
                  <span className="strip-name">Strip {strip.id} — {strip.name}</span>
                  <span className={`strip-badge ${strip.connected ? 'connected' : 'disconnected'}`}>
                     {strip.connected ? 'Connected' : 'Disconnected'}
                  </span>
               </div>
               <div className="strip-meta">
                  <span>{strip.length} LEDs</span>
                  <span>{strip.address}</span>
               </div>
               <button
                  className={`strip-action-btn ${strip.connected ? 'btn-danger' : ''}`}
                  onClick={() => strip.connected ? handleDisconnect(strip.id) : handleConnect(strip.id)}
               >
                  {strip.connected ? 'Disconnect' : 'Connect'}
               </button>
            </div>
         ))}
      </div>
   );
}
