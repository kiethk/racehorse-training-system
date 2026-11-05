'use client';

import Link from 'next/link';
import type { ReactNode } from 'react';
import { cn } from '@/lib/cn';

export function EntityLink({ href, children, className }: { href: string | null; children: ReactNode; className?: string }) {
  if (!href) return <span className={className}>{children}</span>;
  return <Link href={href} data-prevent-row-click onClick={(event) => event.stopPropagation()}
    className={cn('font-medium hover:text-[var(--color-primary)] hover:underline cursor-pointer focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-[var(--color-focus)]', className)}>{children}</Link>;
}
