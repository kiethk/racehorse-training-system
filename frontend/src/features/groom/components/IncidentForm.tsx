'use client';

import { useEffect, useState } from 'react';
import { useRouter } from 'next/navigation';
import Link from 'next/link';
import { useAuth } from '@/context/AuthContext';
import { Button } from '@/components/ui/Button';
import { Panel } from '@/components/ui/Panel';
import { Icon } from '@/components/ui/Icon';
import { incidentApi } from '../services/incidentService';
import { stableApi } from '@/features/stable/services/stableService';
import type { Horse, StableStall } from '@/features/stable/types';
import type { IncidentReport, IncidentSeverity } from '../types';

const SEVERITY_OPTIONS: { value: IncidentSeverity; label: string; hint: string; tone: string }[] = [
  { value: 'LOW', label: 'Nhẹ', hint: 'Theo dõi thêm, chưa cần can thiệp khẩn', tone: 'text-gray-600' },
  { value: 'MEDIUM', label: 'Trung bình', hint: 'Cần Thú y kiểm tra trong ngày', tone: 'text-blue-600' },
  { value: 'HIGH', label: 'Nặng', hint: 'Cần Thú y khám ngay càng sớm càng tốt', tone: 'text-amber-600' },
  { value: 'CRITICAL', label: 'Nguy kịch', hint: 'Khẩn cấp đe dọa sức khỏe — gọi điện song song', tone: 'text-red-600 font-semibold' },
];

export function IncidentForm() {
  const router = useRouter();
  const { user } = useAuth();

  const [loadingInitial, setLoadingInitial] = useState(true);
  const [horses, setHorses] = useState<Horse[]>([]);
  const [stalls, setStalls] = useState<StableStall[]>([]);

  const [horseId, setHorseId] = useState<number | ''>('');
  const [title, setTitle] = useState('');
  const [description, setDescription] = useState('');
  const [severity, setSeverity] = useState<IncidentSeverity>('MEDIUM');
  const [file, setFile] = useState<File | null>(null);
  const [previewUrl, setPreviewUrl] = useState<string | null>(null);

  const [submitting, setSubmitting] = useState(false);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    async function loadData() {
      try {
        setLoadingInitial(true);
        const [stallList, horseList] = await Promise.all([
          stableApi.getStalls(),
          stableApi.getHorses(),
        ]);

        setStalls(stallList);

        // Lọc ngựa thuộc chuồng của chính Groom hiện tại (BR-Groom)
        const myStallIds = stallList
          .filter((s) => s.groomId === user?.userId)
          .map((s) => s.id);

        const myHorses = horseList.filter(
          (h) => h.currentStallId && myStallIds.includes(h.currentStallId)
        );

        setHorses(myHorses);
        if (myHorses.length > 0) {
          setHorseId(myHorses[0].id);
        }
      } catch (err) {
        console.error('Lỗi khi nạp dữ liệu ngựa và chuồng:', err);
        setError('Không tải được danh sách chiến mã. Vui lòng tải lại trang.');
      } finally {
        setLoadingInitial(false);
      }
    }

    if (user?.userId) {
      loadData();
    }
  }, [user?.userId]);

  function handleFileChange(e: React.ChangeEvent<HTMLInputElement>) {
    const f = e.target.files?.[0] ?? null;
    if (f) {
      if (f.size > 10 * 1024 * 1024) {
        setError('Ảnh không được vượt quá 10 MB.');
        return;
      }
      setFile(f);
      const url = URL.createObjectURL(f);
      setPreviewUrl(url);
    } else {
      setFile(null);
      setPreviewUrl(null);
    }
  }

  function handleRemoveFile() {
    setFile(null);
    if (previewUrl) {
      URL.revokeObjectURL(previewUrl);
      setPreviewUrl(null);
    }
  }

  async function handleSubmit(e: React.FormEvent) {
    e.preventDefault();
    if (!horseId) {
      setError('Vui lòng chọn chiến mã gặp sự cố.');
      return;
    }
    if (!title.trim()) {
      setError('Vui lòng nhập tiêu đề sự cố.');
      return;
    }
    if (!description.trim()) {
      setError('Vui lòng nhập mô tả chi tiết sự cố.');
      return;
    }

    setError(null);
    setSubmitting(true);
    let created: IncidentReport | null = null;

    try {
      // Bước 1 — tạo báo cáo, lấy id
      created = await incidentApi.create({
        horseId: Number(horseId),
        title: title.trim(),
        description: description.trim(),
        severity,
      });

      // Bước 2 — có chọn ảnh thì đính vào
      if (file) {
        await incidentApi.uploadImage(created.id, file);
      }

      router.push('/groom/incidents');
    } catch (err) {
      console.error('Lỗi khi gửi báo cáo sự cố:', err);
      setError(
        created
          ? `Báo cáo #${created.id} đã gửi nhưng tải ảnh thất bại. Mở lại báo cáo để thử tải ảnh.`
          : (err instanceof Error ? err.message : 'Gửi báo cáo sự cố thất bại.')
      );
    } finally {
      setSubmitting(false);
    }
  }

  const stallMap = new Map<number, string>();
  for (const s of stalls) {
    stallMap.set(s.id, s.stallCode || `Chuồng #${s.stallNumber}`);
  }

  if (loadingInitial) {
    return (
      <Panel padded>
        <div className="flex items-center justify-center py-12 text-sm text-[var(--color-text-secondary)]">
          Đang tải thông tin chuồng và chiến mã...
        </div>
      </Panel>
    );
  }

  return (
    <div className="mx-auto max-w-2xl space-y-6">
      <div className="flex items-center justify-between">
        <div>
          <h1 className="text-[18px] font-semibold text-[var(--color-text-primary)]">
            Báo cáo sự cố đột xuất
          </h1>
          <p className="text-[12px] text-[var(--color-text-secondary)]">
            Gửi báo cáo dấu hiệu bất thường hoặc chấn thương của chiến mã tới Bác sĩ Thú y.
          </p>
        </div>
        <Link
          href="/groom/incidents"
          className="inline-flex items-center gap-1.5 text-xs text-[var(--color-text-secondary)] hover:text-[var(--color-text-primary)]"
        >
          <Icon name="arrow-left" size={14} /> Quay lại danh sách
        </Link>
      </div>

      {error && (
        <div className="rounded-[var(--radius-md)] border border-red-200 bg-red-50 p-3 text-sm text-red-700">
          {error}
        </div>
      )}

      {horses.length === 0 ? (
        <Panel padded>
          <div className="py-8 text-center text-sm text-[var(--color-text-secondary)]">
            Hiện bạn chưa được phân công phụ trách chuồng hoặc chiến mã nào trong hệ thống.
            <div className="mt-4">
              <Link href="/groom/incidents">
                <Button variant="secondary">Quay lại</Button>
              </Link>
            </div>
          </div>
        </Panel>
      ) : (
        <Panel padded>
          <form onSubmit={handleSubmit} className="space-y-5">
            {/* Chọn ngựa */}
            <div>
              <label className="block text-xs font-medium text-[var(--color-text-secondary)]">
                Chiến mã gặp sự cố <span className="text-red-500">*</span>
              </label>
              <select
                value={horseId}
                onChange={(e) => setHorseId(Number(e.target.value))}
                className="mt-1 w-full rounded-[var(--radius-md)] border border-[var(--color-border)] bg-[var(--color-bg-primary)] px-3 py-2 text-sm text-[var(--color-text-primary)] focus:border-[var(--color-primary)] focus:outline-none"
                required
              >
                {horses.map((h) => {
                  const stallLabel = h.currentStallId
                    ? stallMap.get(h.currentStallId) || `Chuồng #${h.currentStallId}`
                    : 'Chưa có chuồng';
                  return (
                    <option key={h.id} value={h.id}>
                      {h.name} (#{h.id}) — {stallLabel}
                    </option>
                  );
                })}
              </select>
              <p className="mt-1 text-[11px] text-[var(--color-text-muted)]">
                Chỉ hiển thị các chiến mã trong các chuồng do bạn phụ trách.
              </p>
            </div>

            {/* Mức độ nghiêm trọng */}
            <div>
              <label className="block text-xs font-medium text-[var(--color-text-secondary)]">
                Mức độ nghiêm trọng <span className="text-red-500">*</span>
              </label>
              <div className="mt-2 grid grid-cols-1 gap-2 sm:grid-cols-2">
                {SEVERITY_OPTIONS.map((opt) => (
                  <label
                    key={opt.value}
                    className={`flex cursor-pointer items-start gap-2.5 rounded-[var(--radius-md)] border p-2.5 transition-colors ${
                      severity === opt.value
                        ? 'border-[var(--color-primary)] bg-[var(--color-primary-soft)]'
                        : 'border-[var(--color-border)] bg-[var(--color-bg-primary)] hover:border-gray-300'
                    }`}
                  >
                    <input
                      type="radio"
                      name="severity"
                      value={opt.value}
                      checked={severity === opt.value}
                      onChange={() => setSeverity(opt.value)}
                      className="mt-0.5 text-[var(--color-primary)]"
                    />
                    <div>
                      <div className={`text-xs ${opt.tone}`}>{opt.label}</div>
                      <div className="text-[11px] text-[var(--color-text-muted)]">{opt.hint}</div>
                    </div>
                  </label>
                ))}
              </div>

              {severity === 'CRITICAL' && (
                <div className="mt-2.5 rounded-[var(--radius-md)] border border-red-300 bg-red-50 p-2.5 text-xs text-red-800">
                  <div className="font-semibold">⚠️ Cảnh báo khẩn cấp:</div>
                  Hệ thống không gửi thông báo tức thời. Với sự cố <strong>Nguy kịch</strong>, bạn cần{' '}
                  <strong>gọi điện thoại trực tiếp</strong> cho Thú y hoặc Quản lý ngay sau khi gửi báo cáo này!
                </div>
              )}
            </div>

            {/* Tiêu đề */}
            <div>
              <label className="block text-xs font-medium text-[var(--color-text-secondary)]">
                Tiêu đề sự cố <span className="text-red-500">*</span>
              </label>
              <input
                type="text"
                value={title}
                onChange={(e) => setTitle(e.target.value)}
                placeholder="Ví dụ: Sưng khớp gối trước phải, bỏ ăn sáng..."
                className="mt-1 w-full rounded-[var(--radius-md)] border border-[var(--color-border)] bg-[var(--color-bg-primary)] px-3 py-2 text-sm text-[var(--color-text-primary)] focus:border-[var(--color-primary)] focus:outline-none"
                required
              />
            </div>

            {/* Mô tả chi tiết */}
            <div>
              <label className="block text-xs font-medium text-[var(--color-text-secondary)]">
                Mô tả chi tiết <span className="text-red-500">*</span>
              </label>
              <textarea
                rows={4}
                value={description}
                onChange={(e) => setDescription(e.target.value)}
                placeholder="Mô tả cụ thể triệu chứng, thời điểm phát hiện, hành vi của chiến mã, vị trí đau/vết thương..."
                className="mt-1 w-full rounded-[var(--radius-md)] border border-[var(--color-border)] bg-[var(--color-bg-primary)] px-3 py-2 text-sm text-[var(--color-text-primary)] focus:border-[var(--color-primary)] focus:outline-none"
                required
              />
            </div>

            {/* Tải ảnh đính kèm */}
            <div>
              <label className="block text-xs font-medium text-[var(--color-text-secondary)]">
                Hình ảnh hiện trường / chấn thương (tuỳ chọn)
              </label>
              <div className="mt-1.5 flex flex-col gap-2">
                <input
                  type="file"
                  accept="image/jpeg,image/png,image/webp"
                  onChange={handleFileChange}
                  className="text-xs text-[var(--color-text-secondary)] file:mr-3 file:rounded-[var(--radius-sm)] file:border-0 file:bg-[var(--color-bg-secondary)] file:px-3 file:py-1.5 file:text-xs file:font-medium file:text-[var(--color-text-primary)] hover:file:bg-[var(--color-border)]"
                />
                <p className="text-[11px] text-[var(--color-text-muted)]">
                  Chỉ nhận định dạng JPEG, PNG, WebP. Tối đa 10 MB.
                </p>

                {previewUrl && (
                  <div className="relative mt-2 inline-block max-w-xs">
                    {/* eslint-disable-next-line @next/next/no-img-element */}
                    <img
                      src={previewUrl}
                      alt="Xem trước ảnh"
                      className="max-h-48 rounded-[var(--radius-md)] border border-[var(--color-border)] object-cover"
                    />
                    <button
                      type="button"
                      onClick={handleRemoveFile}
                      className="absolute right-2 top-2 rounded-full bg-black/60 p-1 text-white hover:bg-black/80"
                      title="Xoá ảnh"
                    >
                      <Icon name="x" size={14} />
                    </button>
                  </div>
                )}
              </div>
            </div>

            {/* Nút hành động */}
            <div className="flex items-center justify-end gap-3 pt-3">
              <Link href="/groom/incidents">
                <Button type="button" variant="secondary" disabled={submitting}>
                  Hủy
                </Button>
              </Link>
              <Button type="submit" variant="primary" disabled={submitting}>
                {submitting ? 'Đang gửi báo cáo...' : 'Gửi báo cáo sự cố'}
              </Button>
            </div>
          </form>
        </Panel>
      )}
    </div>
  );
}
