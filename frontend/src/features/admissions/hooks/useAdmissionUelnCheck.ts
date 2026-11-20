'use client';

import { useCallback, useEffect, useRef, useState } from 'react';
import { ownerAdmissionApi } from '../services/ownerApi';

type UelnCheck = {
  status: 'idle' | 'checking' | 'available' | 'unavailable' | 'error';
  message: string;
};

export function useAdmissionUelnCheck() {
  const [result, setResult] = useState<UelnCheck>({ status: 'idle', message: '' });
  const timer = useRef<ReturnType<typeof setTimeout> | null>(null);
  const revision = useRef(0);

  const check = useCallback((rawUeln: string) => {
    const currentRevision = ++revision.current;
    if (timer.current) clearTimeout(timer.current);
    const ueln = rawUeln.trim().toUpperCase();
    if (!/^[0-9]{6}[A-Z0-9]{9}$/.test(ueln)) {
      setResult({ status: 'idle', message: '' });
      return;
    }

    setResult({ status: 'checking', message: '' });
    timer.current = setTimeout(async () => {
      try {
        const availability = await ownerAdmissionApi.checkUeln(ueln);
        if (revision.current !== currentRevision) return;
        setResult({
          status: availability.available ? 'available' : 'unavailable',
          message: availability.message || '',
        });
      } catch {
        if (revision.current !== currentRevision) return;
        // Submission still validates on the server if the advisory check is unavailable.
        setResult({ status: 'error', message: 'Could not check UELN.' });
      }
    }, 400);
  }, []);

  useEffect(() => () => {
    revision.current++;
    if (timer.current) clearTimeout(timer.current);
  }, []);

  return { result, check };
}
