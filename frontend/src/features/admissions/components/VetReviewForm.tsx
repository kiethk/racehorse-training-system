'use client';

import type { ReactNode } from 'react';
import { ClinicalExamForm } from './vet-review/ClinicalExamForm';
import type {
  CareSchedule,
  CareScheduleStatus,
  CareType,
  VetReviewResponse,
  AdmissionDocument,
} from '../types';

export interface VetReviewFormProps {
  admissionId: number;
  horseId?: number | null;
  candidateName?: string;
  quarantineStallCode?: string | null;
  careScheduleId?: number | null;
  careType?: CareType;
  scheduleStatus?: CareScheduleStatus;
  /** PREVENTIVE: the system-booked vaccination visit; it can defer vaccination instead of giving it. */
  schedulePurpose?: 'FOLLOW_UP' | 'PREVENTIVE';
  onStartExam?: () => Promise<void>;
  /** Rendered above the decision panel in the side column (e.g. the examination details card). */
  asideTop?: ReactNode;
  onSuccess: (result: VetReviewResponse | CareSchedule, completionKind: 'INITIAL' | 'CARE_SCHEDULE') => void;
  onDirtyChange?: (dirty: boolean) => void;
  documents?: AdmissionDocument[];
}

export function VetReviewForm({
  admissionId,
  horseId,
  candidateName = 'Candidate horse',
  // eslint-disable-next-line @typescript-eslint/no-unused-vars -- preserved prop, consumed by parent via interface only
  quarantineStallCode,
  careScheduleId,
  careType = 'INITIAL',
  scheduleStatus,
  schedulePurpose,
  onStartExam,
  asideTop,
  onSuccess,
  onDirtyChange,
  documents,
}: VetReviewFormProps) {
  const variant = careType === 'URGENT' ? 'URGENT_LEGACY' : 'STANDARD';

  return (
    <ClinicalExamForm
      variant={variant}
      admissionId={admissionId}
      horseId={horseId}
      candidateName={candidateName}
      careScheduleId={careScheduleId}
      careType={careType}
      scheduleStatus={scheduleStatus}
      schedulePurpose={schedulePurpose}
      onStartExam={onStartExam}
      asideTop={asideTop}
      onSuccess={(result, kind) => onSuccess(result, kind)}
      onDirtyChange={onDirtyChange}
      documents={documents}
    />
  );
}
