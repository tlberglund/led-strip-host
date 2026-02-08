interface ConnectionStatusProps {
   connected: boolean;
}

export function ConnectionStatus({ connected }: ConnectionStatusProps) {
   return (
      <div
         id="connection-status"
         className={connected ? 'connected' : 'disconnected'}
      >
         {connected ? 'Connected' : 'Disconnected'}
      </div>
   );
}
