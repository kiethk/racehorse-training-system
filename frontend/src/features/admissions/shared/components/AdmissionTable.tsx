import Link from 'next/link';
import {HorseAvatar} from '@/components/ui/HorseAvatar';
import type {AdmissionSummaryResponse} from '../../types';
import {AdmissionStatusBadge} from './AdmissionStatusBadge';
import type {ReactNode} from 'react';
import { Table, TableBody, TableCell, TableHead, TableHeader, TableRow, TableShell } from '@/components/ui/Table';

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
        <TableShell>
            <Table>
                <TableHeader className="uppercase tracking-wider">
                <tr>
                    <TableHead>Horse</TableHead>
                    <TableHead>Status</TableHead>
                    <TableHead>Submitted</TableHead>
                    <TableHead align="right">Action</TableHead>
                </tr>
                </TableHeader>

                <TableBody>
                {admissions.map(admission => (
                    <TableRow
                        key={admission.admissionId}
                    >
                        <TableCell>
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
                        </TableCell>

                        <TableCell>
                            <AdmissionStatusBadge status={admission.status} />
                        </TableCell>

                        <TableCell className="text-[var(--color-text-secondary)]">
                            {new Date(admission.submittedAt).toLocaleDateString(
                                'en-GB',
                                {
                                    day: '2-digit',
                                    month: 'short',
                                    year: 'numeric',
                                }
                            )}
                        </TableCell>

                        <TableCell align="right">
                            <Link
                                href={detailHref(admission.admissionId)}
                                className="inline-flex items-center justify-center rounded-[var(--radius-sm)] bg-[var(--color-primary-soft)] px-4 py-1.5 text-xs font-semibold text-[var(--color-primary)] transition-colors hover:bg-[var(--color-primary-subtle)]"
                            >
                                View
                            </Link>
                        </TableCell>
                    </TableRow>
                ))}
                </TableBody>
            </Table>
        </TableShell>
    );
}
