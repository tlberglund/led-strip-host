interface SparklineProps {
   values: number[];
   color: string;
   width?: number;
   height?: number;
}

export function Sparkline({ values, color, width = 120, height = 32 }: SparklineProps) {
   if(values.length < 2) {
      return (
         <svg width={width} height={height} className="sparkline">
            <line x1={0} y1={height / 2} x2={width} y2={height / 2}
                  stroke={color} strokeWidth={1} strokeOpacity={0.4} />
         </svg>
      );
   }

   const min = Math.min(...values);
   const max = Math.max(...values);
   const range = max - min || 1;

   const points = values.map((v, i) => {
      const x = (i / (values.length - 1)) * width;
      const y = height - ((v - min) / range) * (height - 2) - 1;
      return `${x},${y}`;
   }).join(' ');

   return (
      <svg width={width} height={height} className="sparkline">
         <polyline points={points} fill="none" stroke={color} strokeWidth={1.5} strokeLinejoin="round" />
      </svg>
   );
}
