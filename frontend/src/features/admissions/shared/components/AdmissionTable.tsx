import Link from 'next/link';
import type { ReactNode } from 'react';
import { DataTable, type DataTableColumn } from '@/components/ui/DataTable';
import { HorseAvatar } from '@/components/ui/HorseAvatar';
import type { AdmissionSummaryResponse } from '../../types';
import { AdmissionStatusBadge } from './AdmissionStatusBadge';

interface AdmissionTableProps {
  admissions: AdmissionSummaryResponse[];
  detailHref: (admissionId: AdmissionSummaryResponse['admissionId']) => string;
  renderAvatar?: (admission: AdmissionSummaryResponse) => ReactNode;
  emptyTitle?: string;
  emptyDescription?: string;
  emptyAction?: ReactNode;
  searchable?: boolean;
}

export function AdmissionTable({ admissions, detailHref, renderAvatar, emptyTitle, emptyDescription, emptyAction, searchable = true }: AdmissionTableProps) {
  const columns: DataTableColumn<AdmissionSummaryResponse>[] = [
    {
      id: 'horse',
      header: 'Horse',
      sortValue: (admission) => admission.candidateName,
      render: (admission) => (
        <div className="flex items-center gap-3">
          {renderAvatar ? renderAvatar(admission) : (
            <HorseAvatar name={admission.candidateName} image={admission.imageUrl} size={32} rounded="md" />
          )}
          <div className="min-w-0">
            <span className="block font-semibold">{admission.candidateName}</span>
            <span className="text-xs text-[var(--color-text-muted)]">{admission.breed || 'Breed not provided'}</span>
          </div>
        </div>
      ),
    },
    {
      id: 'status',
      header: 'Status',
      sortValue: (admission) => admission.status,
      render: (admission) => <AdmissionStatusBadge status={admission.status} />,
    },
    {
      id: 'submitted',
      header: 'Submitted',
      sortValue: (admission) => new Date(admission.submittedAt),
      render: (admission) => new Date(admission.submittedAt).toLocaleDateString('en-GB', {
        day: '2-digit',
        month: 'short',
        year: 'numeric',
      }),
    },
    {
      id: 'actions',
      header: 'Action',
      align: 'right',
      render: (admission) => (
        <Link
          href={detailHref(admission.admissionId)}
          className="inline-flex min-h-9 items-center justify-center rounded-[var(--radius-sm)] bg-[var(--color-primary-soft)] px-4 text-xs font-semibold text-[var(--color-primary)] transition-colors hover:bg-[var(--color-primary-subtle)] focus-visible:outline-2 focus-visible:outline-[var(--color-focus)]"
        >
          View
        </Link>
      ),
    },
  ];

  return (
    <DataTable
      rows={admissions}
      columns={columns}
      getRowKey={(admission) => admission.admissionId}
      ariaLabel="Admission records"
      emptyTitle={emptyTitle ?? 'No admissions found'}
      emptyDescription={emptyDescription}
      emptyAction={emptyAction}
      pageSize={10}
      getSearchText={searchable ? (admission) => `${admission.candidateName} ${admission.breed ?? ''} ${admission.status}` : undefined}
      searchPlaceholder="Search horse, breed, or status…"
    />
  );
}
