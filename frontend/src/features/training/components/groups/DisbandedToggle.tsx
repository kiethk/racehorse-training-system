'use client';

import { useState, type ReactNode } from 'react';
import { Button } from '@/components/ui';

/** "Disbanded (N)" button that shows or hides the Disbanded groups listed under it. */
export function DisbandedToggle({ count, children }: { count: number; children: ReactNode }) {
  const [open, setOpen] = useState(false);
  return (
    <div className="space-y-3">
      <Button
        variant="tertiary"
        size="sm"
        icon={open ? 'chevron-down' : 'chevron-right'}
        aria-expanded={open}
        onClick={() => setOpen((value) => !value)}
      >
        Disbanded ({count})
      </Button>
      {open && children}
    </div>
  );
}
