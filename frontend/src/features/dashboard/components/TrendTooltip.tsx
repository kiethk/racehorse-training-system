import type { ReactNode } from 'react';

/** The content of a chart tooltip: a title and aligned label / value rows. */
export function TrendTooltip({
  title,
  rows,
  children,
}: {
  title: string;
  rows: [label: string, value: ReactNode][];
  /** Extra content under the rows, for example a list. */
  children?: ReactNode;
}) {
  return (
    <>
      <p className="font-semibold text-[var(--color-text-primary)]">{title}</p>
      <dl className="mt-1 grid grid-cols-[auto_auto] gap-x-3 gap-y-0.5 text-[var(--color-text-secondary)]">
        {rows.map(([label, value]) => (
          <div key={label} className="contents">
            <dt>{label}</dt>
            <dd className="text-right font-medium text-[var(--color-text-primary)]">{value}</dd>
          </div>
        ))}
      </dl>
      {children}
    </>
  );
}
