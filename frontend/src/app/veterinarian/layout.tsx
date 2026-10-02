import type { ReactNode } from 'react';
import { VetOfferNotifier } from '@/features/admissions/components/VetOfferNotifier';

export default function VeterinarianLayout({ children }: { children: ReactNode }) {
  return (
    <>
      <VetOfferNotifier />
      {children}
    </>
  );
}
