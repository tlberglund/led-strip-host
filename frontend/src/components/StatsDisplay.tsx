import type { StatsData } from '../hooks/useStats.ts';

interface StatsDisplayProps {
   stats: StatsData;
   resolution: string;
}

export function StatsDisplay({ stats, resolution }: StatsDisplayProps) {
   return (
      <div id="stats">
         <div className="stat-row">
            <span className="stat-label">FPS:</span>
            <span className="stat-value" id="fps">
               {stats.fps > 0 ? stats.fps.toFixed(1) : '--'}
            </span>
         </div>
         <div className="stat-row">
            <span className="stat-label">Frame Time:</span>
            <span className="stat-value" id="frame-time">
               {stats.frameTime > 0 ? `${stats.frameTime.toFixed(2)}ms` : '--'}
            </span>
         </div>
         <div className="stat-row">
            <span className="stat-label">Resolution:</span>
            <span className="stat-value" id="resolution">
               {resolution || '--'}
            </span>
         </div>
         <div className="stat-row">
            <span className="stat-label">Clients:</span>
            <span className="stat-value" id="clients">
               {stats.clients > 0 ? stats.clients : '--'}
            </span>
         </div>
      </div>
   );
}
