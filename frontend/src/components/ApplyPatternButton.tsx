interface ApplyPatternButtonProps {
   onApply: () => void;
}

export function ApplyPatternButton({ onApply }: ApplyPatternButtonProps) {
   return (
      <button onClick={onApply}>Apply Pattern</button>
   );
}
