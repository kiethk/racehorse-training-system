'use client';

import { RoleGuard } from '@/components/auth/RoleGuard';
import { RoleLanding } from '@/components/auth/RoleLanding';

export default function GroomPage() {
  return (
    <RoleGuard allowedRoles={['GROOM']}>
      <RoleLanding role="GROOM" />
    </RoleGuard>
  );
}
