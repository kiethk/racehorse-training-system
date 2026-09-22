'use client';

import { RoleGuard } from '@/components/auth/RoleGuard';
import { RoleLanding } from '@/components/auth/RoleLanding';

export default function VeterinarianPage() {
  return (
    <RoleGuard allowedRoles={['VETERINARIAN']}>
      <RoleLanding role="VETERINARIAN" />
    </RoleGuard>
  );
}
