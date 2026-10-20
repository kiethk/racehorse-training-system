import type { ReactNode } from 'react';

/** Re-mounts on every navigation, so each page fades in while the shell stays put. */
export default function AppTemplate({ children }: { children: ReactNode }) {
  return (
    <div className="flex min-h-0 min-w-0 flex-1 flex-col animate-in fade-in slide-in-from-bottom-1 duration-200 ease-[var(--ease-out)]">
      {children}
    </div>
  );
}
