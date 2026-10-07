import Link from 'next/link';
import {HorseAvatar} from '@/components/ui/HorseAvatar';
import type {AdmissionSummaryResponse} from '../../types';
import {AdmissionStatusBadge} from './AdmissionStatusBadge';
import type {ReactNode} from 'react';

interface AdmissionTableProps {
    admissions: AdmissionSummaryResponse[];
    detailHref: (
        admissionId: AdmissionSummaryResponse['admissionId']
    ) => string;
    renderAvatar?: (admission: AdmissionSummaryResponse) => ReactNode;
}


export function AdmissionTable({
                                   admissions,
                                   detailHref,
                                    renderAvatar,
                               }: AdmissionTableProps) {
    return (
        <div className="overflow-x-auto">
            <table className="w-full whitespace-nowrap text-left text-sm">
                <thead className="border-b border-[var(--color-border)] text-xs font-medium uppercase tracking-wider text-[var(--color-text-muted)]">
                <tr>
                    <th scope="col" className="px-6 py-4">Horse</th>
                    <th scope="col" className="px-6 py-4">Status</th>
                    <th scope="col" className="px-6 py-4">Submitted</th>
                    <th scope="col" className="px-6 py-4 text-right">Action</th>
                </tr>
                </thead>

                <tbody className="divide-y divide-[var(--color-border)] text-[var(--color-text-primary)]">
                {admissions.map(admission => (
                    <tr
                        key={admission.admissionId}
                        className="transition-colors hover:bg-[var(--color-surface-muted)]"
                    >
                        <td className="px-6 py-4">
                            <div className="flex items-center gap-3">
                                {renderAvatar ? (
                                    renderAvatar(admission)
                                ) : (
                                    <HorseAvatar
                                        name={admission.candidateName}
                                        image={admission.imageUrl}
                                        size={32}
                                        rounded="md"
                                    />
                                )}
                                <div>
                    <span className="block font-semibold">
                      {admission.candidateName}
                    </span>
                                    <span className="text-[11px] text-[var(--color-text-muted)]">
                      {admission.breed}
                    </span>
                                </div>
                            </div>
                        </td>

                        <td className="px-6 py-4">
                            <AdmissionStatusBadge status={admission.status} />
                        </td>

                        <td className="px-6 py-4 text-[var(--color-text-secondary)]">
                            {new Date(admission.submittedAt).toLocaleDateString(
                                'en-GB',
                                {
                                    day: '2-digit',
                                    month: 'short',
                                    year: 'numeric',
                                }
                            )}
                        </td>

                        <td className="px-6 py-4 text-right">
                            <Link
                                href={detailHref(admission.admissionId)}
                                className="inline-flex items-center justify-center rounded-[var(--radius-sm)] bg-[var(--color-primary-soft)] px-4 py-1.5 text-xs font-semibold text-[var(--color-primary)] transition-colors hover:bg-[var(--color-primary-subtle)]"
                            >
                                View
                            </Link>
                        </td>
                    </tr>
                ))}
                </tbody>
            </table>
        </div>
    );
}