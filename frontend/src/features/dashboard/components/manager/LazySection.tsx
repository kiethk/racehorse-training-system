'use client';

import React, { useRef, useState, useEffect } from 'react';
import { Skeleton } from '@/components/ui';

interface LazySectionProps {
  children: React.ReactNode;
  height?: string | number;
  rootMargin?: string;
  fallback?: React.ReactNode;
  className?: string;
}

export const LazySection: React.FC<LazySectionProps> = ({
  children,
  height = '280px',
  rootMargin = '200px',
  fallback,
  className = '',
}) => {
  const [isVisible, setIsVisible] = useState(() => {
    return typeof window !== 'undefined' && typeof IntersectionObserver === 'undefined';
  });
  const containerRef = useRef<HTMLDivElement | null>(null);

  useEffect(() => {
    if (isVisible) return;
    const element = containerRef.current;
    if (!element || typeof IntersectionObserver === 'undefined') return;

    const observer = new IntersectionObserver(
      (entries) => {
        const [entry] = entries;
        if (entry.isIntersecting) {
          setIsVisible(true);
          observer.disconnect();
        }
      },
      { rootMargin }
    );

    observer.observe(element);
    return () => observer.disconnect();
  }, [isVisible, rootMargin]);

  return (
    <div ref={containerRef} className={`w-full ${className}`}>
      {isVisible ? (
        children
      ) : (
        fallback || (
          <div style={{ height }}>
            <Skeleton className="h-full w-full rounded-lg" />
          </div>
        )
      )}
    </div>
  );
};
