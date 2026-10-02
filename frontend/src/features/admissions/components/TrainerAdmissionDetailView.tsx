'use client';

import { useEffect, useState, useCallback } from 'react';
import Link from 'next/link';
import { Button } from '@/components/ui/Button';
import { EmptyState, ListSkeleton } from '@/components/ui/states';
import { admissionsApi } from '../services/api';
import { trainerAdmissionsApi } from '../services/trainerAdmissionService';
import type { TrainerAdmissionView } from '../types/trainer';

import { AdmissionDetailLayout } from '../shared/components/AdmissionDetailLayout';
import { AdmissionDetailHeader } from '../shared/components/AdmissionDetailHeader';
import { AdmissionPipeline } from '../shared/components/AdmissionPipeline';
import { AdmissionInfoSection, InfoRow } from '../shared/components/AdmissionInfoSection';
import { AdmissionDocumentsSection } from '../shared/components/AdmissionDocumentsSection';
import { TrainerReviewActionPanel } from './TrainerReviewActionPanel';

function date(value: string | null) {
  return value ? new Date(value).toLocaleDateString('vi-VN') : 'Chưa ghi nhận';
}

function datetime(value: string | null) {
  return value ? new Date(value).toLocaleString('vi-VN', { dateStyle: 'short', timeStyle: 'short' }) : 'Chưa ghi nhận';
}

interface TrainerAdmissionDetailViewProps {
  admissionId: number;
  returnTo: string;
}

export function TrainerAdmissionDetailView({ admissionId, returnTo }: TrainerAdmissionDetailViewProps) {
  const [view, setView] = useState<TrainerAdmissionView | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  const load = useCallback(async () => {
    setLoading(true);
    setError(null);
    try {
      const data = await trainerAdmissionsApi.getView(admissionId);
      setView(data);
    } catch (cause) {
      setError(cause instanceof Error ? cause.message : 'Không thể tải hồ sơ tiếp nhận.');
    } finally {
      setLoading(false);
    }
  }, [admissionId]);

  useEffect(() => {
    // eslint-disable-next-line react-hooks/set-state-in-effect
    void load();
  }, [load]);

  if (loading) {
    return (
      <div className="p-6">
        <ListSkeleton rows={8} />
      </div>
    );
  }

  if (error && !view) {
    return (
      <EmptyState
        icon="alert-triangle"
        title="Không thể tải hồ sơ"
        description={error}
        action={<Button size="sm" onClick={() => void load()}>Thử lại</Button>}
      />
    );
  }

  const detail = view?.admission;
  const candidate = detail?.candidate;

  if (!detail || !candidate) {
    return (
      <EmptyState
        title="Không tìm thấy hồ sơ"
        description="Hồ sơ tiếp nhận không tồn tại hoặc đã bị xóa."
        action={
          <Link className="text-sm font-medium text-[var(--color-primary)]" href={returnTo}>
            Quay lại danh sách hồ sơ
          </Link>
        }
      />
    );
  }

  const horse = view.horse;
  const healthRecords = view.healthRecords;
  const healthMetrics = view.healthMetrics;
  const horsePhoto = detail.documents.find((doc) => doc.documentType === 'HORSE_PHOTO');

  // 1. Gia phả & Chuồng nuôi
  const candidateSection = (
    <AdmissionInfoSection title="Gia phả & Chuồng nuôi">
      <InfoRow label="Sổ đăng bạ" value={candidate.registryName} />
      <InfoRow label="Số UELN" value={candidate.registrationNumber} />
      <InfoRow label="Cha (Sire)" value={candidate.sireName} />
      <InfoRow label="Mẹ (Dam)" value={candidate.damName} />
      <div className="pt-2 border-t border-[var(--color-border)] space-y-3">
        <InfoRow
          label="Chuồng cách ly"
          value={detail.quarantineStallCode ? `Chuồng ${detail.quarantineStallCode}` : 'Chưa xếp chuồng'}
        />
        <InfoRow
          label="Hồ sơ chiến mã"
          value={horse ? `#${horse.id} · ${horse.currentStatus}` : 'Chưa lập hồ sơ'}
        />
        {candidate.pedigreeNotes && (
          <div className="mt-2 text-[var(--color-text-secondary)] italic">
            &quot;{candidate.pedigreeNotes}&quot;
          </div>
        )}
      </div>
    </AdmissionInfoSection>
  );

  // 2. Dữ liệu y tế & Khám bệnh thú y
  const healthSection = (
    <AdmissionInfoSection title="Dữ liệu Khám Thú y & Sinh hiệu">
      <InfoRow
        label="Kết luận của Thú y"
        value={
          detail.vetDecision
            ? detail.vetDecision === 'APPROVED'
              ? 'ĐẠT (Đủ điều kiện)'
              : detail.vetDecision === 'RECHECK_REQUIRED'
              ? 'CẦN KHÁM LẠI'
              : 'TỪ CHỐI'
            : 'Chưa có kết luận'
        }
      />
      {detail.vetFeedback && (
        <div className="pt-2 border-t border-[var(--color-border)]">
          <span className="text-[var(--color-text-muted)] block mb-1 font-medium">Nhận xét của Bác sĩ:</span>
          <p className="text-[var(--color-text-primary)]">{detail.vetFeedback}</p>
        </div>
      )}

      {/* Chỉ số sinh hiệu gần nhất */}
      {healthMetrics && healthMetrics.length > 0 && (
        <div className="pt-2 border-t border-[var(--color-border)] space-y-1.5">
          <span className="text-[var(--color-text-muted)] block font-medium">
            Chỉ số sinh hiệu gần nhất ({new Date(healthMetrics[0].recordedAt).toLocaleDateString('vi-VN')}):
          </span>
          <div className="grid grid-cols-2 gap-2 text-[11px] bg-[var(--color-surface-muted)] p-2 rounded">
            <div>Thân nhiệt: <strong>{healthMetrics[0].temperature != null ? `${healthMetrics[0].temperature} °C` : '—'}</strong></div>
            <div>Nhịp tim: <strong>{healthMetrics[0].heartRate != null ? `${healthMetrics[0].heartRate} bpm` : '—'}</strong></div>
            <div>Nhịp thở: <strong>{healthMetrics[0].respiratoryRate != null ? `${healthMetrics[0].respiratoryRate} bpm` : '—'}</strong></div>
            <div>Cân nặng: <strong>{healthMetrics[0].weight != null ? `${healthMetrics[0].weight} kg` : '—'}</strong></div>
            <div>Bù nước: <strong>{healthMetrics[0].hydrationStatus || '—'}</strong></div>
            <div>Điểm BCS: <strong>{healthMetrics[0].bodyConditionScore != null ? `${healthMetrics[0].bodyConditionScore}/9` : '—'}</strong></div>
          </div>
        </div>
      )}

      {/* Danh sách khám bệnh lâm sàng */}
      <div className="pt-2 border-t border-[var(--color-border)]">
        <span className="text-[var(--color-text-muted)] block mb-2 font-medium">Lịch sử khám lâm sàng</span>
        {healthRecords && healthRecords.length > 0 ? (
          <ul className="space-y-2">
            {healthRecords.map((hr) => (
              <li key={hr.id} className="bg-[var(--color-surface-muted)] p-2 rounded text-[11px] space-y-1">
                <div className="flex justify-between font-medium">
                  <span>{hr.recordType || 'Khám nhập học'}</span>
                  <span className="text-[10px] text-[var(--color-text-muted)]">{date(hr.examinedAt)}</span>
                </div>
                {hr.trainingDecision && <div className="font-semibold">Quyết định huấn luyện: {hr.trainingDecision}</div>}
                {hr.restrictionDetails && <div>Hạn chế y tế: {hr.restrictionDetails}</div>}
                {hr.followUpDate && <div>Ngày tái khám: {date(hr.followUpDate)}</div>}
                {hr.diagnosis && <div>Chẩn đoán: {hr.diagnosis}</div>}
                {hr.symptoms && <div className="text-[var(--color-text-secondary)]">Triệu chứng: {hr.symptoms}</div>}
                {hr.treatment && <div className="text-[var(--color-text-secondary)]">Điều trị: {hr.treatment}</div>}
                {hr.notes && <div className="text-[var(--color-text-secondary)] italic">Ghi chú: {hr.notes}</div>}
              </li>
            ))}
          </ul>
        ) : (
          <span className="text-[var(--color-text-muted)] italic">Chưa có bản ghi khám bệnh chi tiết.</span>
        )}
      </div>
    </AdmissionInfoSection>
  );

  // 3. Lịch sử xét duyệt qua các khâu
  const reviewHistorySection = (
    <AdmissionInfoSection title="Lịch sử xét duyệt">
      <div className="space-y-4">
        {detail.groomReviewedAt ? (
          <div className="space-y-1">
            <h4 className="text-[11px] font-semibold text-[var(--color-text-secondary)] uppercase tracking-wider">
              Chăm sóc viên (Groom)
            </h4>
            <InfoRow label="Quyết định" value={detail.groomDecision} />
            <InfoRow label="Thời gian" value={datetime(detail.groomReviewedAt)} />
            <InfoRow label="Nhận xét" value={detail.groomFeedback} />
          </div>
        ) : null}

        {detail.vetReviewedAt ? (
          <div className="space-y-1 pt-3 border-t border-[var(--color-border)]">
            <h4 className="text-[11px] font-semibold text-[var(--color-text-secondary)] uppercase tracking-wider">
              Bác sĩ thú y (Veterinarian)
            </h4>
            <InfoRow label="Quyết định" value={detail.vetDecision} />
            <InfoRow label="Thời gian" value={datetime(detail.vetReviewedAt)} />
            <InfoRow label="Nhận xét" value={detail.vetFeedback} />
          </div>
        ) : null}

        {detail.trainerReviewedAt ? (
          <div className="space-y-1 pt-3 border-t border-[var(--color-border)]">
            <h4 className="text-[11px] font-semibold text-[var(--color-text-secondary)] uppercase tracking-wider">
              Huấn luyện viên (Head Trainer)
            </h4>
            <InfoRow label="Thời gian" value={datetime(detail.trainerReviewedAt)} />
            <InfoRow label="Nhận xét" value={detail.trainerFeedback} />
          </div>
        ) : null}

        {detail.managerReviewedAt ? (
          <div className="space-y-1 pt-3 border-t border-[var(--color-border)]">
            <h4 className="text-[11px] font-semibold text-[var(--color-text-secondary)] uppercase tracking-wider">
              Quản lý (Manager)
            </h4>
            <InfoRow label="Quyết định" value={detail.managerDecision} />
            <InfoRow label="Thời gian" value={datetime(detail.managerReviewedAt)} />
            <InfoRow label="Nhận xét" value={detail.managerFeedback} />
          </div>
        ) : null}

        {!detail.groomReviewedAt && !detail.vetReviewedAt && !detail.trainerReviewedAt && (
          <span className="text-[var(--color-text-muted)] italic">Chưa có thông tin duyệt trước đó.</span>
        )}
      </div>
    </AdmissionInfoSection>
  );

  return (
    <>
      {error && (
        <div role="alert" className="mb-4 border-l-2 border-[var(--color-danger)] bg-[var(--color-danger-soft)] px-4 py-3 text-sm text-[var(--color-text-primary)]">
          {error}
        </div>
      )}
      <AdmissionDetailLayout
        returnTo={returnTo}
        header={
          <AdmissionDetailHeader
            detail={detail}
            horsePhotoUrl={horsePhoto ? admissionsApi.assetUrl(horsePhoto.fileUrl) : undefined}
          />
        }
        pipeline={<AdmissionPipeline detail={detail} />}
        sections={[
          candidateSection,
          healthSection,
          <AdmissionDocumentsSection
            key="docs"
            documents={detail.documents}
            assetUrl={admissionsApi.assetUrl}
          />,
          reviewHistorySection,
        ]}
        actions={<TrainerReviewActionPanel view={view} onSuccess={load} />}
      />
    </>
  );
}
