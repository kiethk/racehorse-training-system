'use client';

import { useEffect, useState } from 'react';
import Link from 'next/link';
import { useRouter } from 'next/navigation';
import { Button } from '@/components/ui/Button';
import { Panel } from '@/components/ui/Panel';
import { ListSkeleton } from '@/components/ui/states';
import { stableApi } from '@/features/stable/services/stableService';
import type { Horse } from '@/features/stable/types';
import { racingService } from '../services/racingService';
import type { CreateRaceRegistrationRequest } from '../types';

export function TrainerRaceForm() {
  const router = useRouter();

  // Ngựa
  const [horses, setHorses] = useState<Horse[]>([]);
  const [loadingHorses, setLoadingHorses] = useState(true);
  const [horseError, setHorseError] = useState<string | null>(null);

  // Form state
  const [horseId, setHorseId] = useState<number | ''>('');
  const [selectionReason, setSelectionReason] = useState('');
  const [raceName, setRaceName] = useState('');
  const [raceCategory, setRaceCategory] = useState('');
  const [location, setLocation] = useState('');
  const [eventDate, setEventDate] = useState('');
  const [eventTime, setEventTime] = useState('');
  const [organizer, setOrganizer] = useState('');
  const [sourceUrl, setSourceUrl] = useState('');
  const [nominationDeadline, setNominationDeadline] = useState('');
  const [distanceMeters, setDistanceMeters] = useState<number | ''>('');
  const [trackType, setTrackType] = useState('');
  const [prizeDetails, setPrizeDetails] = useState('');
  const [trainerNotes, setTrainerNotes] = useState('');

  // UI state
  const [errors, setErrors] = useState<Record<string, string>>({});
  const [submitting, setSubmitting] = useState(false);
  const [submitError, setSubmitError] = useState<string | null>(null);

  useEffect(() => {
    async function fetchHorses() {
      try {
        setLoadingHorses(true);
        setHorseError(null);
        // Lấy ngựa trong khu trainer (mine=true), đủ điều kiện (ELIGIBLE)
        const list = await stableApi.getHorses({ mine: true, status: 'ELIGIBLE' });
        // Khớp Horse.canTrain() ở backend: ELIGIBLE và Thú y không chặn tập.
        const available = list.filter((h) => h.trainingDecision !== 'BLOCKED');
        setHorses(available);
      } catch (err) {
        console.error('Lỗi khi tải danh sách ngựa:', err);
        setHorseError(err instanceof Error ? err.message : 'Không tải được danh sách ngựa.');
      } finally {
        setLoadingHorses(false);
      }
    }
    fetchHorses();
  }, []);

  const todayStr = new Date().toISOString().split('T')[0];

  function validate(): boolean {
    const errs: Record<string, string> = {};

    if (!horseId) {
      errs.horseId = 'Vui lòng chọn một chiến mã đang phụ trách.';
    }
    if (!selectionReason.trim()) {
      errs.selectionReason = 'Vui lòng nhập lý do đề cử chiến mã cho cuộc đua này.';
    }
    if (!raceName.trim()) {
      errs.raceName = 'Vui lòng nhập tên cuộc đua/sự kiện.';
    }
    if (!raceCategory.trim()) {
      errs.raceCategory = 'Vui lòng nhập hạng mục/chặng cụ thể (ví dụ: Hạng 3 tuổi - 1.200 m).';
    }
    if (!location.trim()) {
      errs.location = 'Vui lòng nhập sân đua và địa điểm.';
    }
    if (!eventDate) {
      errs.eventDate = 'Vui lòng chọn ngày tổ chức cuộc đua.';
    } else if (eventDate < todayStr) {
      errs.eventDate = 'Ngày tổ chức không được nằm trong quá khứ.';
    }

    if (nominationDeadline) {
      if (eventDate && nominationDeadline > eventDate) {
        errs.nominationDeadline = 'Hạn nộp hồ sơ không được sau ngày tổ chức cuộc đua.';
      }
    }

    if (sourceUrl.trim()) {
      const url = sourceUrl.trim();
      if (!url.startsWith('http://') && !url.startsWith('https://')) {
        errs.sourceUrl = 'Link nguồn phải là đường dẫn HTTP hoặc HTTPS hợp lệ (ví dụ: https://...).';
      }
    }

    if (distanceMeters !== '' && distanceMeters <= 0) {
      errs.distanceMeters = 'Cự ly phải là số dương.';
    }

    setErrors(errs);
    return Object.keys(errs).length === 0;
  }

  async function handleSubmit(e: React.FormEvent) {
    e.preventDefault();
    if (!validate()) return;

    setSubmitting(true);
    setSubmitError(null);

    const payload: CreateRaceRegistrationRequest = {
      horseId: Number(horseId),
      raceName: raceName.trim(),
      raceCategory: raceCategory.trim(),
      location: location.trim(),
      eventDate,
      eventTime: eventTime ? eventTime : null,
      organizer: organizer.trim() ? organizer.trim() : null,
      sourceUrl: sourceUrl.trim() ? sourceUrl.trim() : null,
      nominationDeadline: nominationDeadline ? nominationDeadline : null,
      distanceMeters: distanceMeters !== '' ? Number(distanceMeters) : null,
      trackType: trackType.trim() ? trackType.trim() : null,
      prizeDetails: prizeDetails.trim() ? prizeDetails.trim() : null,
      selectionReason: selectionReason.trim(),
      trainerNotes: trainerNotes.trim() ? trainerNotes.trim() : null,
    };

    try {
      const created = await racingService.create(payload);
      router.push(`/trainer/racing/${created.id}`);
    } catch (err) {
      console.error('Lỗi khi nộp đơn đề cử:', err);
      setSubmitError(err instanceof Error ? err.message : 'Nộp đơn đề cử thất bại.');
    } finally {
      setSubmitting(false);
    }
  }

  const selectedHorse = horses.find((h) => h.id === horseId);

  return (
    <form onSubmit={handleSubmit} className="space-y-6">
      {/* Header */}
      <div className="flex items-center justify-between border-b border-[var(--color-border)] pb-4">
        <div>
          <div className="flex items-center gap-2">
            <Link
              href="/trainer/racing"
              className="text-[12px] text-[var(--color-text-secondary)] hover:text-[var(--color-text-primary)] transition"
            >
              ← Danh sách đơn
            </Link>
          </div>
          <h1 className="mt-1 text-[20px] font-bold text-[var(--color-text-primary)]">
            Tạo đơn đề cử ngựa dự đua
          </h1>
          <p className="text-[13px] text-[var(--color-text-secondary)]">
            Gửi đề cử nội bộ để ban quản lý xem xét kế hoạch tham gia giải đấu.
          </p>
        </div>
      </div>

      {submitError && (
        <div className="rounded-[var(--radius-md)] border border-[var(--color-danger)] bg-[var(--color-danger-soft)] p-4 text-[13px] text-[var(--color-danger)]">
          <strong>Lỗi nộp đơn:</strong> {submitError}
        </div>
      )}

      {/* Phần 1: Ngựa đề cử */}
      <Panel padded>
        <h2 className="text-[15px] font-semibold text-[var(--color-text-primary)] mb-1 flex items-center gap-2">
          <span>🏇</span> 1. Chiến mã đề cử
        </h2>
        <p className="text-[12px] text-[var(--color-text-secondary)] mb-4">
          Chỉ các chiến mã đang ở khu vực do bạn phụ trách, có trạng thái Đủ điều kiện (ELIGIBLE) và không bị khoá huấn luyện.
        </p>

        {loadingHorses ? (
          <ListSkeleton rows={2} />
        ) : horseError ? (
          <div className="rounded-[var(--radius-md)] border border-[var(--color-danger)] bg-[var(--color-danger-soft)] p-3 text-[12px] text-[var(--color-danger)]">
            {horseError}
          </div>
        ) : horses.length === 0 ? (
          <div className="rounded-[var(--radius-md)] border border-[var(--color-warning)] bg-[var(--color-warning-soft)] p-4 text-[13px] text-[var(--color-warning)]">
            <p className="font-semibold">Hiện không có chiến mã nào khả dụng để đề cử.</p>
            <p className="mt-1 text-[12px]">
              Ngựa cần được xếp chuồng trong khu của bạn, có trạng thái ELIGIBLE và không bị tạm hoãn huấn luyện.
            </p>
            <div className="mt-3 flex gap-2">
              <Link href="/trainer/stable">
                <Button variant="secondary" size="sm" type="button">
                  Xem quản lý chuồng
                </Button>
              </Link>
              <Link href="/trainer/horses">
                <Button variant="secondary" size="sm" type="button">
                  Xem danh sách ngựa
                </Button>
              </Link>
            </div>
          </div>
        ) : (
          <div className="space-y-4">
            <div>
              <label className="block text-[13px] font-medium text-[var(--color-text-primary)] mb-1">
                Chọn chiến mã <span className="text-[var(--color-danger)]">*</span>
              </label>
              <select
                value={horseId}
                onChange={(e) => setHorseId(e.target.value ? Number(e.target.value) : '')}
                className="w-full rounded-[var(--radius-md)] border border-[var(--color-border-strong)] bg-[var(--color-surface)] px-3 py-2 text-[13px] focus:outline-none focus:ring-2 focus:ring-[var(--color-focus)]"
              >
                <option value="">-- Chọn chiến mã do bạn phụ trách --</option>
                {horses.map((h) => (
                  <option key={h.id} value={h.id}>
                    {h.name} {h.registrationNumber ? `(${h.registrationNumber})` : ''} {h.breed ? `• ${h.breed}` : ''}
                  </option>
                ))}
              </select>
              {errors.horseId && (
                <p className="mt-1 text-[11px] text-[var(--color-danger)]">{errors.horseId}</p>
              )}
            </div>

            {selectedHorse && (
              <div className="rounded-[var(--radius-md)] bg-[var(--color-surface-muted)] p-3 text-[12px] text-[var(--color-text-secondary)] border border-[var(--color-border)]">
                <div>
                  Chiến mã đã chọn: <strong className="text-[var(--color-text-primary)]">{selectedHorse.name}</strong>
                  {selectedHorse.registrationNumber && (
                    <span className="ml-2 font-mono">({selectedHorse.registrationNumber})</span>
                  )}
                </div>
                {selectedHorse.breed && <div>Giống: {selectedHorse.breed}</div>}
              </div>
            )}

            <div>
              <label className="block text-[13px] font-medium text-[var(--color-text-primary)] mb-1">
                Lý do đề cử <span className="text-[var(--color-danger)]">*</span>
              </label>
              <textarea
                rows={3}
                placeholder="Nhận định của bạn về thể lực, cự ly sở trường, phong độ hiện tại và sự phù hợp của ngựa đối với giải đấu này..."
                value={selectionReason}
                onChange={(e) => setSelectionReason(e.target.value)}
                className="w-full rounded-[var(--radius-md)] border border-[var(--color-border-strong)] bg-[var(--color-surface)] px-3 py-2 text-[13px] focus:outline-none focus:ring-2 focus:ring-[var(--color-focus)]"
              />
              {errors.selectionReason && (
                <p className="mt-1 text-[11px] text-[var(--color-danger)]">{errors.selectionReason}</p>
              )}
            </div>
          </div>
        )}
      </Panel>

      {/* Phần 2: Thông tin cuộc đua */}
      <Panel padded>
        <h2 className="text-[15px] font-semibold text-[var(--color-text-primary)] mb-1 flex items-center gap-2">
          <span>🏆</span> 2. Thông tin cuộc đua / sự kiện
        </h2>
        <p className="text-[12px] text-[var(--color-text-secondary)] mb-4">
          Thông tin sự kiện do bạn tự tìm hiểu từ nguồn bên ngoài.
        </p>

        <div className="grid grid-cols-1 gap-4 md:grid-cols-2">
          <div>
            <label className="block text-[13px] font-medium text-[var(--color-text-primary)] mb-1">
              Tên cuộc đua / sự kiện <span className="text-[var(--color-danger)]">*</span>
            </label>
            <input
              type="text"
              placeholder="VD: Giải Đua Ngựa Mùa Thu 2026"
              value={raceName}
              onChange={(e) => setRaceName(e.target.value)}
              className="w-full rounded-[var(--radius-md)] border border-[var(--color-border-strong)] bg-[var(--color-surface)] px-3 py-2 text-[13px] focus:outline-none focus:ring-2 focus:ring-[var(--color-focus)]"
            />
            {errors.raceName && (
              <p className="mt-1 text-[11px] text-[var(--color-danger)]">{errors.raceName}</p>
            )}
          </div>

          <div>
            <label className="block text-[13px] font-medium text-[var(--color-text-primary)] mb-1">
              Hạng mục / Chặng đề cử <span className="text-[var(--color-danger)]">*</span>
            </label>
            <input
              type="text"
              placeholder="VD: Hạng 3 tuổi - 1.200 m hoặc Cup Mở Rộng"
              value={raceCategory}
              onChange={(e) => setRaceCategory(e.target.value)}
              className="w-full rounded-[var(--radius-md)] border border-[var(--color-border-strong)] bg-[var(--color-surface)] px-3 py-2 text-[13px] focus:outline-none focus:ring-2 focus:ring-[var(--color-focus)]"
            />
            {errors.raceCategory && (
              <p className="mt-1 text-[11px] text-[var(--color-danger)]">{errors.raceCategory}</p>
            )}
          </div>

          <div className="md:col-span-2">
            <label className="block text-[13px] font-medium text-[var(--color-text-primary)] mb-1">
              Sân đua & Địa điểm <span className="text-[var(--color-danger)]">*</span>
            </label>
            <input
              type="text"
              placeholder="VD: Trường đua Sóc Sơn, Hà Nội"
              value={location}
              onChange={(e) => setLocation(e.target.value)}
              className="w-full rounded-[var(--radius-md)] border border-[var(--color-border-strong)] bg-[var(--color-surface)] px-3 py-2 text-[13px] focus:outline-none focus:ring-2 focus:ring-[var(--color-focus)]"
            />
            {errors.location && (
              <p className="mt-1 text-[11px] text-[var(--color-danger)]">{errors.location}</p>
            )}
          </div>

          <div>
            <label className="block text-[13px] font-medium text-[var(--color-text-primary)] mb-1">
              Ngày tổ chức <span className="text-[var(--color-danger)]">*</span>
            </label>
            <input
              type="date"
              min={todayStr}
              value={eventDate}
              onChange={(e) => setEventDate(e.target.value)}
              className="w-full rounded-[var(--radius-md)] border border-[var(--color-border-strong)] bg-[var(--color-surface)] px-3 py-2 text-[13px] focus:outline-none focus:ring-2 focus:ring-[var(--color-focus)]"
            />
            {errors.eventDate && (
              <p className="mt-1 text-[11px] text-[var(--color-danger)]">{errors.eventDate}</p>
            )}
          </div>

          <div>
            <label className="block text-[13px] font-medium text-[var(--color-text-primary)] mb-1">
              Giờ xuất phát dự kiến <span className="text-[var(--color-text-muted)]">(tuỳ chọn)</span>
            </label>
            <input
              type="time"
              value={eventTime}
              onChange={(e) => setEventTime(e.target.value)}
              className="w-full rounded-[var(--radius-md)] border border-[var(--color-border-strong)] bg-[var(--color-surface)] px-3 py-2 text-[13px] focus:outline-none focus:ring-2 focus:ring-[var(--color-focus)]"
            />
          </div>

          <div>
            <label className="block text-[13px] font-medium text-[var(--color-text-primary)] mb-1">
              Đơn vị tổ chức <span className="text-[var(--color-text-muted)]">(tuỳ chọn)</span>
            </label>
            <input
              type="text"
              placeholder="VD: Liên đoàn Thể thao Đua ngựa..."
              value={organizer}
              onChange={(e) => setOrganizer(e.target.value)}
              className="w-full rounded-[var(--radius-md)] border border-[var(--color-border-strong)] bg-[var(--color-surface)] px-3 py-2 text-[13px] focus:outline-none focus:ring-2 focus:ring-[var(--color-focus)]"
            />
          </div>

          <div>
            <label className="block text-[13px] font-medium text-[var(--color-text-primary)] mb-1">
              Hạn nộp hồ sơ thật <span className="text-[var(--color-text-muted)]">(tuỳ chọn)</span>
            </label>
            <input
              type="date"
              value={nominationDeadline}
              onChange={(e) => setNominationDeadline(e.target.value)}
              className="w-full rounded-[var(--radius-md)] border border-[var(--color-border-strong)] bg-[var(--color-surface)] px-3 py-2 text-[13px] focus:outline-none focus:ring-2 focus:ring-[var(--color-focus)]"
            />
            {errors.nominationDeadline && (
              <p className="mt-1 text-[11px] text-[var(--color-danger)]">{errors.nominationDeadline}</p>
            )}
          </div>

          <div className="md:col-span-2">
            <label className="block text-[13px] font-medium text-[var(--color-text-primary)] mb-1">
              Link nguồn thể lệ / trang chính thức <span className="text-[var(--color-text-muted)]">(tuỳ chọn để Quản lý kiểm chứng)</span>
            </label>
            <input
              type="url"
              placeholder="https://example.org/race-conditions"
              value={sourceUrl}
              onChange={(e) => setSourceUrl(e.target.value)}
              className="w-full rounded-[var(--radius-md)] border border-[var(--color-border-strong)] bg-[var(--color-surface)] px-3 py-2 text-[13px] focus:outline-none focus:ring-2 focus:ring-[var(--color-focus)]"
            />
            {errors.sourceUrl && (
              <p className="mt-1 text-[11px] text-[var(--color-danger)]">{errors.sourceUrl}</p>
            )}
          </div>
        </div>
      </Panel>

      {/* Phần 3: Thông tin tham khảo */}
      <Panel padded>
        <h2 className="text-[15px] font-semibold text-[var(--color-text-primary)] mb-1 flex items-center gap-2">
          <span>📋</span> 3. Thông tin tham khảo & điều kiện
        </h2>
        <p className="text-[12px] text-[var(--color-text-secondary)] mb-4">
          Bổ sung chi tiết đường đua, giải thưởng hoặc các lưu ý về hậu cần/giấy tờ.
        </p>

        <div className="grid grid-cols-1 gap-4 md:grid-cols-2">
          <div>
            <label className="block text-[13px] font-medium text-[var(--color-text-primary)] mb-1">
              Cự ly (mét) <span className="text-[var(--color-text-muted)]">(tuỳ chọn)</span>
            </label>
            <input
              type="number"
              placeholder="VD: 1200"
              min={1}
              value={distanceMeters}
              onChange={(e) => setDistanceMeters(e.target.value ? Number(e.target.value) : '')}
              className="w-full rounded-[var(--radius-md)] border border-[var(--color-border-strong)] bg-[var(--color-surface)] px-3 py-2 text-[13px] focus:outline-none focus:ring-2 focus:ring-[var(--color-focus)]"
            />
            {errors.distanceMeters && (
              <p className="mt-1 text-[11px] text-[var(--color-danger)]">{errors.distanceMeters}</p>
            )}
          </div>

          <div>
            <label className="block text-[13px] font-medium text-[var(--color-text-primary)] mb-1">
              Mặt sân / đường đua <span className="text-[var(--color-text-muted)]">(tuỳ chọn)</span>
            </label>
            <input
              type="text"
              placeholder="VD: Cỏ, Cát, Tổng hợp..."
              value={trackType}
              onChange={(e) => setTrackType(e.target.value)}
              className="w-full rounded-[var(--radius-md)] border border-[var(--color-border-strong)] bg-[var(--color-surface)] px-3 py-2 text-[13px] focus:outline-none focus:ring-2 focus:ring-[var(--color-focus)]"
            />
          </div>

          <div className="md:col-span-2">
            <label className="block text-[13px] font-medium text-[var(--color-text-primary)] mb-1">
              Cơ cấu giải thưởng / Tiền thưởng <span className="text-[var(--color-text-muted)]">(tuỳ chọn)</span>
            </label>
            <textarea
              rows={2}
              placeholder="VD: Nhất 50 triệu VND; Nhì 25 triệu VND; Ba 10 triệu VND..."
              value={prizeDetails}
              onChange={(e) => setPrizeDetails(e.target.value)}
              className="w-full rounded-[var(--radius-md)] border border-[var(--color-border-strong)] bg-[var(--color-surface)] px-3 py-2 text-[13px] focus:outline-none focus:ring-2 focus:ring-[var(--color-focus)]"
            />
          </div>

          <div className="md:col-span-2">
            <label className="block text-[13px] font-medium text-[var(--color-text-primary)] mb-1">
              Ghi chú thêm của Trainer <span className="text-[var(--color-text-muted)]">(tuỳ chọn)</span>
            </label>
            <textarea
              rows={2}
              placeholder="Điều kiện tuổi/giới/handicap, thủ tục khám thú y đặc thù, phương án vận chuyển xe chuyên dụng..."
              value={trainerNotes}
              onChange={(e) => setTrainerNotes(e.target.value)}
              className="w-full rounded-[var(--radius-md)] border border-[var(--color-border-strong)] bg-[var(--color-surface)] px-3 py-2 text-[13px] focus:outline-none focus:ring-2 focus:ring-[var(--color-focus)]"
            />
          </div>
        </div>
      </Panel>

      {/* Hành động */}
      <div className="flex flex-col items-end gap-2 border-t border-[var(--color-border)] pt-4 sm:flex-row sm:items-center sm:justify-between">
        <p className="text-[12px] font-medium text-[var(--color-warning-strong)] bg-[var(--color-warning-soft)] px-3 py-1.5 rounded-[var(--radius-md)] border border-[var(--color-warning)]">
          ⚠️ Đây là đơn đề cử nội bộ, chưa đăng ký với ban tổ chức.
        </p>

        <div className="flex items-center gap-3">
          <Link href="/trainer/racing">
            <Button variant="secondary" type="button" disabled={submitting}>
              Huỷ
            </Button>
          </Link>
          <Button
            variant="primary"
            type="submit"
            disabled={submitting || horses.length === 0}
          >
            {submitting ? 'Đang gửi...' : 'Gửi chờ duyệt'}
          </Button>
        </div>
      </div>
    </form>
  );
}
