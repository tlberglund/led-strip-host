import { useState, useEffect } from 'react';
import type { RenderStats, ClientCount } from '../types.ts';

export interface StatsData {
   fps: number;
   frameTime: number;
   clients: number;
}

export function useStats(connected: boolean): StatsData {
   const [stats, setStats] = useState<StatsData>({
      fps: 0,
      frameTime: 0,
      clients: 0,
   });

   useEffect(() => {
      if (!connected) return;

      const interval = setInterval(async () => {
         try {
            const [statsResponse, clientsResponse] = await Promise.all([
               fetch('/api/stats'),
               fetch('/api/clients'),
            ]);
            const statsData: RenderStats = await statsResponse.json();
            const clientsData: ClientCount = await clientsResponse.json();

            setStats({
               fps: statsData.fps,
               frameTime: statsData.frameTime,
               clients: clientsData.count,
            });
         } catch {
            // Silently ignore errors (server might not be ready)
         }
      }, 1000);

      return () => clearInterval(interval);
   }, [connected]);

   return stats;
}
