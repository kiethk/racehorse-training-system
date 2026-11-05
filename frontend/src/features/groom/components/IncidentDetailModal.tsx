'use client';

import { Button } from '@/components/ui/Button';
import { Icon } from '@/components/ui/Icon';
import { Modal } from '@/components/ui/Modal';
import { Pill } from '@/components/ui/StatusBadge';
import { ProtectedImage } from '@/components/ui/ProtectedImage';
import { formatDateTime } from '@/lib/display';
import { incidentApi } from '../services/incidentService';
import { assignedVetLabel } from '../lib/assignedVet';
import {
  INCIDENT_SEVERITY_MAP,
  INCIDENT_STATUS_MAP,
  formatIncidentReportedAt,
} from '../lib/incidentDisplay';
import type { IncidentReport } from '../types';

interface IncidentDetailModalProps {
  report: IncidentReport | null;
  horseName: string;
  onClose: () => void;
}

/** Full details of one incident report, opened from the Groom's incident table. */
export function IncidentDetailModal({ report, horseName, onClose }: IncidentDetailModalProps) {
  if (!report) return null;

  const statusCfg = INCIDENT_STATUS_MAP[report.status] || { tone: 'neutral', label: report.status };
  const severityCfg = INCIDENT_SEVERITY_MAP[report.severity] || { tone: 'neutral', label: report.severity };
  const imgSrc = incidentApi.imageSrc(report);
  const vetLabel = assignedVetLabel(report);

  return (
    <Modal
      open
      onClose={onClose}
      size="lg"
      title={`#${report.id} ${report.title}`}
      description={`Horse: ${horseName}`}
      footer={<Button variant="secondary" onClick={onClose}>Close</Button>}
    >
      <div className="space-y-4">
        <div className="flex flex-wrap items-center gap-2">
          <Pill tone={severityCfg.tone} size="sm">{severityCfg.label}</Pill>
          <Pill tone={statusCfg.tone} size="sm">{statusCfg.label}</Pill>
        </div>

        <dl className="grid grid-cols-2 gap-3 text-xs">
          <div>
            <dt className="text-[var(--color-text-muted)]">Reported</dt>
            <dd className="mt-0.5 text-[var(--color-text-primary)]">{formatIncidentReportedAt(report.reportedAt)}</dd>
          </div>
          {report.reportedByName && (
            <div>
              <dt className="text-[var(--color-text-muted)]">Reported by</dt>
              <dd className="mt-0.5 text-[var(--color-text-primary)]">{report.reportedByName}</dd>
            </div>
          )}
          {vetLabel && (
            <div>
              <dt className="text-[var(--color-text-muted)]">Veterinarian</dt>
              <dd className="mt-0.5 text-[var(--color-text-primary)]">{vetLabel.replace(/^Vet: /, '')}</dd>
            </div>
          )}
        </dl>

        <div>
          <p className="text-xs font-medium text-[var(--color-text-muted)]">Description</p>
          <p className="mt-1 whitespace-pre-wrap text-sm leading-relaxed text-[var(--color-text-primary)]">
            {report.description}
          </p>
        </div>

        {imgSrc && (
          <ProtectedImage
            src={imgSrc}
            alt={`Incident image: ${report.title}`}
            className="max-h-80 max-w-full rounded-[var(--radius-md)] border border-[var(--color-border)] object-contain shadow-[var(--shadow-panel)]"
          />
        )}

        {report.handlerNote && (
          <div className="rounded-[var(--radius-sm)] border border-[var(--color-success)] bg-[var(--color-success-soft)] p-3 text-xs">
            <div className="flex items-center gap-1.5 font-medium text-[var(--color-success)]">
              <Icon name="stethoscope" size={14} />
              <span>
                Veterinary assessment & guidance{' '}
                {report.handledAt ? `(${formatDateTime(report.handledAt)})` : ''}:
              </span>
            </div>
            <p className="mt-1.5 whitespace-pre-wrap text-[var(--color-text-primary)]">{report.handlerNote}</p>
          </div>
        )}
      </div>
    </Modal>
  );
}
