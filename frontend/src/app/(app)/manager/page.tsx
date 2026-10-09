'use client';

import { RoleGuard } from '@/components/auth/RoleGuard';
import { RoleLanding } from '@/components/auth/RoleLanding';

export default function ManagerPage() {
  return (
    <RoleGuard allowedRoles={['CLUB_MANAGER']}>
      <RoleLanding role="CLUB_MANAGER" />
    </RoleGuard>
  );
}
