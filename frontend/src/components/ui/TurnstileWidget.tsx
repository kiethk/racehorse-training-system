'use client';

import { useCallback, useEffect, useRef, type Ref } from 'react';
import Script from 'next/script';
import { cn } from '@/lib/cn';

interface TurnstileRenderOptions {
  sitekey: string;
  callback: (token: string) => void;
  'expired-callback': () => void;
  'error-callback': () => void;
  theme: 'auto';
  size: 'flexible';
}

declare global {
  interface Window {
    turnstile?: {
      render: (container: HTMLElement, options: TurnstileRenderOptions) => string;
      remove: (widgetId: string) => void;
      reset: (widgetId: string) => void;
    };
  }
}

export interface TurnstileWidgetHandle {
  reset: () => void;
}

interface TurnstileWidgetProps {
  siteKey: string;
  onVerify: (token: string) => void;
  onExpire: () => void;
  onError: () => void;
  className?: string;
  id?: string;
  ref?: Ref<TurnstileWidgetHandle>;
}

export function TurnstileWidget({
  siteKey,
  onVerify,
  onExpire,
  onError,
  className,
  id,
  ref,
}: TurnstileWidgetProps) {
  const containerRef = useRef<HTMLDivElement>(null);
  const widgetIdRef = useRef<string | null>(null);
  const callbacksRef = useRef({ onVerify, onExpire, onError });

  useEffect(() => {
    callbacksRef.current = { onVerify, onExpire, onError };
  }, [onVerify, onExpire, onError]);

  const reset = useCallback(() => {
    if (widgetIdRef.current) window.turnstile?.reset(widgetIdRef.current);
  }, []);

  useEffect(() => {
    if (!ref) return;
    if (typeof ref === 'function') ref({ reset });
    else ref.current = { reset };
    return () => {
      if (typeof ref === 'function') ref(null);
      else ref.current = null;
    };
  }, [ref, reset]);

  const renderWidget = useCallback(() => {
    const container = containerRef.current;
    if (!container || !window.turnstile || widgetIdRef.current) return;
    widgetIdRef.current = window.turnstile.render(container, {
      sitekey: siteKey,
      callback: (token) => callbacksRef.current.onVerify(token),
      'expired-callback': () => callbacksRef.current.onExpire(),
      'error-callback': () => callbacksRef.current.onError(),
      theme: 'auto',
      size: 'flexible',
    });
  }, [siteKey]);

  useEffect(() => () => {
    if (widgetIdRef.current) window.turnstile?.remove(widgetIdRef.current);
    widgetIdRef.current = null;
  }, []);

  return (
    <>
      <Script
        src="https://challenges.cloudflare.com/turnstile/v0/api.js?render=explicit"
        strategy="afterInteractive"
        onReady={renderWidget}
        onError={() => callbacksRef.current.onError()}
      />
      <div
        ref={containerRef}
        id={id}
        className={cn('min-h-[4.5rem]', className)}
        aria-label="Security check"
      />
    </>
  );
}
