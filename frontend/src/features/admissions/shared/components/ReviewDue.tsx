'use client';

import { useEffect, useState } from 'react';
import { Pill } from '@/components/ui/StatusBadge';
import { formatDateTime } from '@/lib/display';
import { reviewDueRelative, reviewDueTone } from '../lib/reviewDue';

export function ReviewDue({ dueAt }: { dueAt: string | null }) {
  const [now, setNow] = useState(() => Date.now());
  useEffect(() => {
    const timer = setInterval(() => setNow(Date.now()), 60000);
    return () => clearInterval(timer);
  }, []);
  const relative = reviewDueRelative(dueAt, now);
  if (!dueAt || !relative) return <span>Not recorded</span>;
  return (
    <span className="inline-flex flex-wrap items-center gap-2">
      <span>{formatDateTime(dueAt)}</span>
      <Pill tone={reviewDueTone(dueAt, now)} size="sm">{relative}</Pill>
    </span>
  );
}
