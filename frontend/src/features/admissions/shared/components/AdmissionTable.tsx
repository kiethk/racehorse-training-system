import type { ReactNode } from 'react';
import { DataTable, type DataTableColumn, type DataTablePagination } from '@/components/ui/DataTable';
import { LinkButton } from '@/components/ui/Button';
import { HorseAvatar } from '@/components/ui/HorseAvatar';
import { formatDate } from '@/lib/display';
import { AdmissionStatusBadge } from './AdmissionStatusBadge';

/** The fields every role's admission row has in common. */
export interface AdmissionRow {
  admissionId: number;
  status: string;
  candidateName: string;
  breed?: string | null;
  imageUrl?: string | null;
  submittedAt: string;
}

interface AdmissionTableProps<Row extends AdmissionRow> {
  admissions: Row[];
  detailHref: (admissionId: Row['admissionId']) => string;
  renderAvatar?: (admission: Row) => ReactNode;
  /** Role-specific columns, placed between Status and Submitted. */
  extraColumns?: DataTableColumn<Row>[];
  /** Show only In Progress / Approved / Rejected. */
  simplifiedStatus?: boolean;
  /** Label and emphasis of the row action. Defaults to a quiet "View". */
  action?: (admission: Row) => { label: string; primary?: boolean };
  emptyTitle?: string;
  emptyDescription?: string;
  emptyAction?: ReactNode;
  searchable?: boolean;
  loading?: boolean;
  /** Server- or caller-driven pagination. Without it the table pages 10 rows client-side. */
  pagination?: DataTablePagination;
}

/** The one admission list table. Every role renders this so rows look identical. */
export function AdmissionTable<Row extends AdmissionRow>({
  admissions,
  detailHref,
  renderAvatar,
  extraColumns = [],
  simplifiedStatus = false,
  action,
  emptyTitle,
  emptyDescription,
  emptyAction,
  searchable = true,
  loading,
  pagination,
}: AdmissionTableProps<Row>) {
  const columns: DataTableColumn<Row>[] = [
    {
      id: 'horse',
      header: 'Horse',
      sortValue: (admission) => admission.candidateName,
      render: (admission) => (
        <div className="flex min-w-0 items-center gap-3">
          {renderAvatar ? renderAvatar(admission) : (
            <HorseAvatar name={admission.candidateName} image={admission.imageUrl} size={32} rounded="md" />
          )}
          <div className="min-w-0">
            <span className="block truncate font-semibold">{admission.candidateName}</span>
            <span className="block truncate text-xs text-[var(--color-text-muted)]">{admission.breed || 'Breed not provided'}</span>
          </div>
        </div>
      ),
    },
    {
      id: 'status',
      header: 'Status',
      sortValue: (admission) => admission.status,
      render: (admission) => <AdmissionStatusBadge status={admission.status} simplified={simplifiedStatus} />,
    },
    ...extraColumns,
    {
      id: 'submitted',
      header: 'Submitted',
      sortValue: (admission) => new Date(admission.submittedAt),
      render: (admission) => formatDate(admission.submittedAt),
    },
    {
      id: 'actions',
      header: 'Action',
      align: 'right',
      render: (admission) => {
        const { label, primary } = action?.(admission) ?? { label: 'View' };
        return (
          <LinkButton
            href={detailHref(admission.admissionId)}
            size="sm"
            variant={primary ? 'primary' : 'secondary'}
            className="min-w-[4.5rem]"
          >
            {label}
          </LinkButton>
        );
      },
    },
  ];

  return (
    <DataTable
      rows={admissions}
      columns={columns}
      getRowKey={(admission) => admission.admissionId}
      ariaLabel="Admission records"
      loading={loading}
      emptyTitle={emptyTitle ?? 'No admissions found'}
      emptyDescription={emptyDescription}
      emptyAction={emptyAction}
      pageSize={pagination ? undefined : 10}
      pagination={pagination}
      getSearchText={searchable ? (admission) => `${admission.candidateName} ${admission.breed ?? ''} ${admission.status}` : undefined}
      searchPlaceholder="Search horse, breed, or status…"
    />
  );
}
