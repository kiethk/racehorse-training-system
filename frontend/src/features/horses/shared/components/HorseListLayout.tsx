import type { ReactNode } from 'react';

import { PageHeader } from '@/components/ui/PageHeader';
import { ScreenLayout } from '@/components/ui/ScreenLayout';

export interface HorseListLayoutProps {
  title?: string;
  description?: ReactNode;
  actions?: ReactNode;
  children: ReactNode;
}

/**
 * Shared page shell for horse list screens.
 * Wraps title, optional description, header action buttons and list content
 * in a consistent ScreenLayout matching the Admissions pattern.
 */
export function HorseListLayout({
  title = 'Horses',
  description,
  actions,
  children,
}: HorseListLayoutProps) {
  return (
    <ScreenLayout variant="list">
      <PageHeader title={title} description={description} actions={actions} />
      {children}
    </ScreenLayout>
  );
}
