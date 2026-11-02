'use client';

import React from 'react';

export function ModuleBadge({ moduleName }: { moduleName: string | null | undefined }) {
  const mod = (moduleName || 'SYSTEM').toUpperCase();
  let style = 'border-slate-300 text-slate-800 dark:border-zinc-700 dark:text-zinc-200';
  let dotColor = 'bg-slate-500';

  switch (mod) {
    case 'ADMISSIONS':
      style = 'border-blue-300 text-blue-800 dark:border-blue-700 dark:text-blue-300';
      dotColor = 'bg-blue-600';
      break;
    case 'MEDICAL':
      style = 'border-purple-300 text-purple-800 dark:border-purple-700 dark:text-purple-300';
      dotColor = 'bg-purple-600';
      break;
    case 'STABLE':
      style = 'border-amber-300 text-amber-800 dark:border-amber-700 dark:text-amber-300';
      dotColor = 'bg-amber-600';
      break;
    case 'HORSES':
      style = 'border-emerald-300 text-emerald-800 dark:border-emerald-700 dark:text-emerald-300';
      dotColor = 'bg-emerald-600';
      break;
    case 'STAFF':
      style = 'border-indigo-300 text-indigo-800 dark:border-indigo-700 dark:text-indigo-300';
      dotColor = 'bg-indigo-600';
      break;
    case 'TRAINING':
      style = 'border-teal-300 text-teal-800 dark:border-teal-700 dark:text-teal-300';
      dotColor = 'bg-teal-600';
      break;
    case 'RACING':
      style = 'border-rose-300 text-rose-800 dark:border-rose-700 dark:text-rose-300';
      dotColor = 'bg-rose-600';
      break;
    case 'AI':
      style = 'border-cyan-300 text-cyan-800 dark:border-cyan-700 dark:text-cyan-300';
      dotColor = 'bg-cyan-600';
      break;
    case 'SECURITY':
      style = 'border-violet-300 text-violet-800 dark:border-violet-700 dark:text-violet-300';
      dotColor = 'bg-violet-600';
      break;
  }

  return (
    <span className={`inline-flex items-center gap-1.5 px-2.5 py-0.5 rounded-md border bg-white text-[11px] font-bold tracking-wider shadow-2xs ${style}`}>
      <span className={`size-1.5 rounded-full ${dotColor}`} />
      {mod}
    </span>
  );
}
