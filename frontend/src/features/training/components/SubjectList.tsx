'use client';

import { useCallback, useEffect, useState } from 'react';
import { Button } from '@/components/ui/Button';
import { Panel } from '@/components/ui/Panel';
import { Pill } from '@/components/ui/StatusBadge';
import { EmptyState, ListSkeleton } from '@/components/ui/states';
import { trainingApi } from '../services/trainingService';
import type { Subject, SubjectCategory, CreateSubjectRequest, SurfaceType, IntensityLevel, WorkoutType } from '../types';

export function SubjectList() {
  const [subjects, setSubjects] = useState<Subject[]>([]);
  const [categories, setCategories] = useState<SubjectCategory[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  // Form state
  const [showForm, setShowForm] = useState(false);
  const [categoryId, setCategoryId] = useState<number | ''>('');
  const [name, setName] = useState('');
  const [description, setDescription] = useState('');
  const [surfaceType, setSurfaceType] = useState<SurfaceType>('TURF');
  const [targetDistanceMeters, setTargetDistanceMeters] = useState<number>(1000);
  const [intensityLevel, setIntensityLevel] = useState<IntensityLevel>('MEDIUM');
  const [durationMinutes, setDurationMinutes] = useState<number>(60);
  const [workoutType, setWorkoutType] = useState<WorkoutType>('REGULAR');
  const [submitting, setSubmitting] = useState(false);
  const [formError, setFormError] = useState<string | null>(null);

  const loadData = useCallback(async () => {
    try {
      setLoading(true);
      setError(null);
      const [subs, cats] = await Promise.all([
        trainingApi.getSubjects(),
        trainingApi.getCategories(),
      ]);
      setSubjects(subs);
      setCategories(cats);
      if (cats.length > 0 && categoryId === '') {
        setCategoryId(cats[0].id);
      }
    } catch (err) {
      console.error('Lỗi khi nạp danh sách bài tập:', err);
      setError('Không tải được danh mục bài tập.');
    } finally {
      setLoading(false);
    }
  }, [categoryId]);

  useEffect(() => {
    // eslint-disable-next-line react-hooks/set-state-in-effect
    loadData();
  }, [loadData]);

  async function handleCreate(e: React.FormEvent) {
    e.preventDefault();
    if (!name.trim() || !categoryId) {
      setFormError('Vui lòng nhập tên bài tập và chọn phân loại.');
      return;
    }
    // Chặn thời lượng vượt quá 240 phút (khung giờ vàng 06:00 - 10:00)
    if (durationMinutes <= 0 || durationMinutes > 240) {
      setFormError('Thời lượng phải từ 1 đến 240 phút (khung giờ vàng chỉ có 240 phút).');
      return;
    }

    setFormError(null);
    setSubmitting(true);
    try {
      const payload: CreateSubjectRequest = {
        categoryId: Number(categoryId),
        name: name.trim(),
        description: description.trim() || undefined,
        surfaceType,
        targetDistanceMeters: Number(targetDistanceMeters),
        intensityLevel,
        durationMinutes: Number(durationMinutes),
        workoutType,
      };
      await trainingApi.createSubject(payload);
      setName('');
      setDescription('');
      setShowForm(false);
      await loadData();
    } catch (err) {
      setFormError(err instanceof Error ? err.message : 'Tạo bài tập thất bại.');
    } finally {
      setSubmitting(false);
    }
  }

  const categoryMap = new Map(categories.map((c) => [c.id, c.name]));

  if (loading) return <ListSkeleton rows={4} />;

  if (error) {
    return (
      <Panel padded>
        <EmptyState icon="alert-triangle" title="Lỗi nạp dữ liệu" description={error} />
      </Panel>
    );
  }

  return (
    <div className="space-y-4">
      <div className="flex flex-wrap items-center justify-between gap-3">
        <div>
          <h2 className="text-[16px] font-semibold text-[var(--color-text-primary)]">
            Thư viện bài tập ({subjects.length})
          </h2>
          <p className="text-[12px] text-[var(--color-text-secondary)]">
            Các bài tập tiêu chuẩn để xây dựng giáo án huấn luyện.
          </p>
        </div>
        <Button variant="primary" size="sm" onClick={() => setShowForm(!showForm)}>
          {showForm ? 'Đóng form' : '+ Tạo bài tập mới'}
        </Button>
      </div>

      {showForm && (
        <Panel padded>
          <form onSubmit={handleCreate} className="space-y-4">
            <h3 className="text-[14px] font-semibold text-[var(--color-text-primary)]">
              Tạo bài tập mới
            </h3>

            {formError && (
              <div className="rounded-[var(--radius-md)] bg-[var(--color-danger-soft)] p-3 text-[12px] text-[var(--color-danger)]">
                {formError}
              </div>
            )}

            <div className="grid grid-cols-1 gap-3 sm:grid-cols-2">
              <div>
                <label className="block text-[12px] font-medium text-[var(--color-text-primary)]">
                  Tên bài tập *
                </label>
                <input
                  type="text"
                  required
                  value={name}
                  onChange={(e) => setName(e.target.value)}
                  placeholder="Ví dụ: Gallop 1200m"
                  className="mt-1 w-full rounded-[var(--radius-md)] border border-[var(--color-border-strong)] bg-[var(--color-surface)] px-3 py-1.5 text-[13px] focus:outline-none focus:ring-2 focus:ring-[var(--color-focus)]"
                />
              </div>

              <div>
                <label className="block text-[12px] font-medium text-[var(--color-text-primary)]">
                  Phân loại (Category) *
                </label>
                <select
                  value={categoryId}
                  onChange={(e) => setCategoryId(e.target.value ? Number(e.target.value) : '')}
                  className="mt-1 w-full rounded-[var(--radius-md)] border border-[var(--color-border-strong)] bg-[var(--color-surface)] px-3 py-1.5 text-[13px] focus:outline-none focus:ring-2 focus:ring-[var(--color-focus)]"
                >
                  {categories.map((c) => (
                    <option key={c.id} value={c.id}>
                      {c.name}
                    </option>
                  ))}
                </select>
              </div>

              <div>
                <label className="block text-[12px] font-medium text-[var(--color-text-primary)]">
                  Mặt sân (Surface)
                </label>
                <select
                  value={surfaceType}
                  onChange={(e) => setSurfaceType(e.target.value as SurfaceType)}
                  className="mt-1 w-full rounded-[var(--radius-md)] border border-[var(--color-border-strong)] bg-[var(--color-surface)] px-3 py-1.5 text-[13px] focus:outline-none focus:ring-2 focus:ring-[var(--color-focus)]"
                >
                  <option value="TURF">Cỏ tự nhiên (TURF)</option>
                  <option value="DIRT">Cát / Đất (DIRT)</option>
                  <option value="SYNTHETIC">Nhân tạo (SYNTHETIC)</option>
                </select>
              </div>

              <div>
                <label className="block text-[12px] font-medium text-[var(--color-text-primary)]">
                  Cường độ (Intensity)
                </label>
                <select
                  value={intensityLevel}
                  onChange={(e) => setIntensityLevel(e.target.value as IntensityLevel)}
                  className="mt-1 w-full rounded-[var(--radius-md)] border border-[var(--color-border-strong)] bg-[var(--color-surface)] px-3 py-1.5 text-[13px] focus:outline-none focus:ring-2 focus:ring-[var(--color-focus)]"
                >
                  <option value="LOW">Nhẹ (LOW)</option>
                  <option value="MEDIUM">Vừa phải (MEDIUM)</option>
                  <option value="HIGH">Nặng / Tối đa (HIGH)</option>
                </select>
              </div>

              <div>
                <label className="block text-[12px] font-medium text-[var(--color-text-primary)]">
                  Cự ly mục tiêu (mét)
                </label>
                <input
                  type="number"
                  min="100"
                  step="50"
                  value={targetDistanceMeters}
                  onChange={(e) => setTargetDistanceMeters(Number(e.target.value))}
                  className="mt-1 w-full rounded-[var(--radius-md)] border border-[var(--color-border-strong)] bg-[var(--color-surface)] px-3 py-1.5 text-[13px] focus:outline-none focus:ring-2 focus:ring-[var(--color-focus)]"
                />
              </div>

              <div>
                <label className="block text-[12px] font-medium text-[var(--color-text-primary)]">
                  Thời lượng dự kiến (phút, tối đa 240) *
                </label>
                <input
                  type="number"
                  min="15"
                  max="240"
                  step="5"
                  required
                  value={durationMinutes}
                  onChange={(e) => setDurationMinutes(Number(e.target.value))}
                  className="mt-1 w-full rounded-[var(--radius-md)] border border-[var(--color-border-strong)] bg-[var(--color-surface)] px-3 py-1.5 text-[13px] focus:outline-none focus:ring-2 focus:ring-[var(--color-focus)]"
                />
                <span className="text-[11px] text-[var(--color-text-muted)]">
                  Quyết định độ dài lot. Khung giờ vàng tối đa 240 phút.
                </span>
              </div>

              <div>
                <label className="block text-[12px] font-medium text-[var(--color-text-primary)]">
                  Loại hình bài tập (Workout Type)
                </label>
                <select
                  value={workoutType}
                  onChange={(e) => setWorkoutType(e.target.value as WorkoutType)}
                  className="mt-1 w-full rounded-[var(--radius-md)] border border-[var(--color-border-strong)] bg-[var(--color-surface)] px-3 py-1.5 text-[13px] focus:outline-none focus:ring-2 focus:ring-[var(--color-focus)]"
                >
                  <option value="REGULAR">Bài tập thông thường (REGULAR)</option>
                  <option value="GATE_PRACTICE">Tập xuất phát cổng (GATE_PRACTICE)</option>
                  <option value="BREEZING">Nước đại tốc độ cao (BREEZING)</option>
                  <option value="SWIMMING">Bơi lội (SWIMMING)</option>
                  <option value="RECOVERY">Hồi phục thể lực (RECOVERY)</option>
                </select>
              </div>
            </div>


            <div>
              <label className="block text-[12px] font-medium text-[var(--color-text-primary)]">
                Mô tả chi tiết
              </label>
              <textarea
                rows={2}
                value={description}
                onChange={(e) => setDescription(e.target.value)}
                placeholder="Ghi chú kỹ thuật hoặc hướng dẫn cho Groom..."
                className="mt-1 w-full rounded-[var(--radius-md)] border border-[var(--color-border-strong)] bg-[var(--color-surface)] px-3 py-1.5 text-[13px] focus:outline-none focus:ring-2 focus:ring-[var(--color-focus)]"
              />
            </div>

            <div className="flex justify-end gap-2 pt-2">
              <Button variant="secondary" size="sm" type="button" onClick={() => setShowForm(false)}>
                Huỷ
              </Button>
              <Button variant="primary" size="sm" type="submit" disabled={submitting}>
                {submitting ? 'Đang tạo...' : 'Lưu bài tập'}
              </Button>
            </div>
          </form>
        </Panel>
      )}

      {subjects.length === 0 ? (
        <Panel padded>
          <EmptyState
            icon="clipboard"
            title="Chưa có bài tập nào"
            description="Hãy tạo bài tập đầu tiên để bắt đầu xây dựng khoá huấn luyện."
          />
        </Panel>
      ) : (
        <div className="grid grid-cols-1 gap-3 md:grid-cols-2 lg:grid-cols-3">
          {subjects.map((sub) => (
            <Panel key={sub.id} padded>
              <div className="flex items-start justify-between gap-2">
                <div>
                  <h4 className="text-[14px] font-semibold text-[var(--color-text-primary)]">
                    {sub.name}
                  </h4>
                  <div className="text-[11px] text-[var(--color-text-muted)]">
                    {categoryMap.get(sub.categoryId) || `Nhóm #${sub.categoryId}`}
                  </div>
                </div>
                <Pill
                  tone={
                    sub.intensityLevel === 'HIGH'
                      ? 'danger'
                      : sub.intensityLevel === 'MEDIUM'
                      ? 'warning'
                      : 'info'
                  }
                  size="sm"
                >
                  {sub.intensityLevel}
                </Pill>
              </div>

              {sub.description && (
                <p className="mt-2 text-[12px] text-[var(--color-text-secondary)] line-clamp-2">
                  {sub.description}
                </p>
              )}

              <div className="mt-3 flex flex-wrap gap-2 text-[11px] text-[var(--color-text-muted)] border-t border-[var(--color-border)] pt-2">
                <span>⏱️ {sub.durationMinutes} phút</span>
                <span>📏 {sub.targetDistanceMeters}m</span>
                <span>🏟️ {sub.surfaceType}</span>
              </div>
            </Panel>
          ))}
        </div>
      )}
    </div>
  );
}
