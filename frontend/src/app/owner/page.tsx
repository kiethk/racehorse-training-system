'use client';

import { RoleGuard } from '@/components/auth/RoleGuard';
import { RoleLanding } from '@/components/auth/RoleLanding';
import Link from 'next/link';

export default function OwnerPage() {
  return (
    <RoleGuard allowedRoles={['HORSE_OWNER']}>
      <><RoleLanding role="HORSE_OWNER" /><div className="fixed right-6 bottom-6 rounded-lg p-3 bg-[var(--color-primary)] text-white shadow-lg"><Link href="/owner/admissions">My admissions →</Link></div></>
    </RoleGuard>
  );
}
