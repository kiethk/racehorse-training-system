import type { ReactNode } from 'react';

interface PageContainerProps {
  children: ReactNode;
}

export function PageContainer({ children }: PageContainerProps) {
  return (
    <div className="mx-auto w-full min-w-0 max-w-[var(--layout-max-width)] px-4 py-[var(--spacing-page-y)] sm:px-6 lg:px-8">
      {children}
    </div>
  );
}
