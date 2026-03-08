// Fixed internal coordinate space; SVG stretches to fill its CSS container.
const VB_W = 300;
const VB_H = 40;

interface SparklineProps {
   values: number[];
   color: string;
   minVal?: number;
   maxVal?: number;
}

export function Sparkline({ values, color, minVal, maxVal }: SparklineProps) {
   if(values.length < 2) {
      return (
         <svg viewBox={`0 0 ${VB_W} ${VB_H}`} preserveAspectRatio="none" className="sparkline">
            <line x1={0} y1={VB_H / 2} x2={VB_W} y2={VB_H / 2}
                  stroke={color} strokeWidth={1.5} strokeOpacity={0.4} />
         </svg>
      );
   }

   const min = minVal !== undefined ? minVal : Math.min(...values);
   const max = maxVal !== undefined ? maxVal : Math.max(...values);
   const range = max - min || 1;

   const points = values.map((v, i) => {
      const x = (i / (values.length - 1)) * VB_W;
      const y = VB_H - ((v - min) / range) * (VB_H - 2) - 1;
      return `${x},${y}`;
   }).join(' ');

   return (
      <svg viewBox={`0 0 ${VB_W} ${VB_H}`} preserveAspectRatio="none" className="sparkline">
         <polyline points={points} fill="none" stroke={color} strokeWidth={1.5} strokeLinejoin="round" />
      </svg>
   );
}
