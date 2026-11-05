import type { ReactNode } from 'react';
import { cn } from '@/lib/cn';
import { STALL_STATES, stallToneDot } from './StallCell';

/** The stall colour key, shared by the 2D plan and the 3D barn. `children` adds the lens's own note. */
export function StableLegend({ children }: { children?: ReactNode }) {
  return (
    <div className="flex flex-wrap items-center gap-x-4 gap-y-1 text-xs text-[var(--color-text-secondary)]">
      {STALL_STATES.map((state) => (
        <span key={state.label} className="inline-flex items-center gap-1.5">
          <span className={cn('h-2 w-2 rounded-full', stallToneDot(state.tone))} />
          {state.label}
        </span>
      ))}
      {children && <span className="text-[var(--color-text-muted)]">{children}</span>}
    </div>
  );
}
