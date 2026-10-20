import type { ReactNode } from 'react';
import { ScreenLayout } from '@/components/ui/ScreenLayout';
import { PageHeader } from '@/components/ui/PageHeader';

export function AdmissionListLayout({
  title = 'Admissions',
  description,
  actions,
  children,
}: {
  title?: string;
  description?: ReactNode;
  actions?: ReactNode;
  children: ReactNode;
}) {
  return (
    <ScreenLayout variant="list">
      <PageHeader title={title} description={description} actions={actions} />
      {children}
    </ScreenLayout>
  );
}
