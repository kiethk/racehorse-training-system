import { useState, useCallback, useRef } from 'react';

interface UseDraftOptions<T> {
  key: string;
  initialData: T;
  debounceMs?: number;
}

export function useDraft<T>({ key, initialData, debounceMs = 15000 }: UseDraftOptions<T>) {
  const [data, setData] = useState<T>(initialData);
  const [hasDraft, setHasDraft] = useState(() => {
    if (typeof window === 'undefined') return false;
    try {
      const stored = localStorage.getItem(`rtms_draft_${key}`);
      if (stored) {
        const parsed = JSON.parse(stored);
        return Boolean(parsed && parsed.data);
      }
    } catch {
      // Ignore
    }
    return false;
  });
  const [savedAt, setSavedAt] = useState<Date | null>(() => {
    if (typeof window === 'undefined') return null;
    try {
      const stored = localStorage.getItem(`rtms_draft_${key}`);
      if (stored) {
        const parsed = JSON.parse(stored);
        if (parsed?.savedAt) return new Date(parsed.savedAt);
      }
    } catch {
      // Ignore
    }
    return null;
  });
  const timerRef = useRef<NodeJS.Timeout | null>(null);

  // Lưu bản nháp (debounce)
  const saveDraft = useCallback(
    (newData: T) => {
      setData(newData);
      if (timerRef.current) clearTimeout(timerRef.current);

      timerRef.current = setTimeout(() => {
        if (typeof window === 'undefined') return;
        try {
          const payload = {
            data: newData,
            savedAt: new Date().toISOString(),
          };
          localStorage.setItem(`rtms_draft_${key}`, JSON.stringify(payload));
          setSavedAt(new Date());
          setHasDraft(true);
        } catch {
          // Ignore localStorage write errors
        }
      }, debounceMs);
    },
    [key, debounceMs]
  );

  // Phục hồi bản nháp
  const restoreDraft = useCallback(() => {
    if (typeof window === 'undefined') return false;
    try {
      const stored = localStorage.getItem(`rtms_draft_${key}`);
      if (stored) {
        const parsed = JSON.parse(stored);
        if (parsed && parsed.data) {
          setData(parsed.data);
          setHasDraft(false);
          return true;
        }
      }
    } catch {
      // Ignore
    }
    return false;
  }, [key]);

  // Xóa bản nháp khi hoàn thành form
  const clearDraft = useCallback(() => {
    if (typeof window === 'undefined') return;
    try {
      localStorage.removeItem(`rtms_draft_${key}`);
      setHasDraft(false);
      setSavedAt(null);
    } catch {
      // Ignore
    }
  }, [key]);

  return {
    data,
    setData,
    saveDraft,
    restoreDraft,
    clearDraft,
    hasDraft,
    savedAt,
  };
}
