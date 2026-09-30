import type { ReactNode } from 'react';
import { TopNav } from './TopNav';

export function AppShell({ children }: { children: ReactNode }) {
  return (
    <div className="flex h-full min-h-screen flex-col bg-[var(--color-background)]">
      <TopNav />
      <main className="flex min-h-0 flex-1 flex-col">{children}</main>
    </div>
  );
}
