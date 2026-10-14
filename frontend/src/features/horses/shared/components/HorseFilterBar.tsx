'use client';

import { Select } from '@/components/ui';
import { cn } from '@/lib/cn';
import type { HorseStatus } from '@/features/stable/types';

export interface HorseFilterBarProps {
  status: HorseStatus | '';
  onStatusChange: (status: HorseStatus | '') => void;
  className?: string;
}

export function HorseFilterBar({
  status,
  onStatusChange,
  className = '',
}: HorseFilterBarProps) {
  return (
    <div className={cn('w-[180px] shrink-0', className)}>
      <label htmlFor="horse-status-filter" className="sr-only">
        Status
      </label>
      <Select
        id="horse-status-filter"
        aria-label="Status"
        value={status}
        onChange={(e) => onStatusChange(e.target.value as HorseStatus | '')}
      >
        <option value="">All statuses</option>
        <option value="CANDIDATE">Candidate</option>
        <option value="ELIGIBLE">Eligible</option>
        <option value="REJECTED">Rejected</option>
      </Select>
    </div>
  );
}
