import type { ReactNode } from 'react';
import { Suspense } from 'react';
import { UrgentAssignmentNotifier } from '@/features/admissions/components/UrgentAssignmentNotifier';

export default function VeterinarianLayout({ children }: { children: ReactNode }) {
  return (
    <>
      <Suspense fallback={null}>
        <UrgentAssignmentNotifier />
      </Suspense>
      {children}
    </>
  );
}
