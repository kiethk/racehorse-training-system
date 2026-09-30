import type { IconName } from './Icon';
import { Icon } from './Icon';

export interface TabItem {
  id: string;
  label: string;
  icon?: IconName;
  count?: number;
}

interface TabsProps {
  tabs: TabItem[];
  active: string;
  onChange: (id: string) => void;
  size?: 'sm' | 'md';
}

/** Underlined tab strip — Prism style. Never a filled segmented block. */
export function Tabs({ tabs, active, onChange, size = 'md' }: TabsProps) {
  return (
    <div
      className="flex items-center gap-0.5 overflow-x-auto border-b border-[var(--color-border)]"
      role="tablist"
      style={{ scrollbarWidth: 'none' }}
    >
      {tabs.map((t) => {
        const isActive = t.id === active;
        return (
          <button
            key={t.id}
            role="tab"
            aria-selected={isActive}
            onClick={() => onChange(t.id)}
            className={
              'relative flex shrink-0 items-center gap-1.5 whitespace-nowrap px-3 outline-none transition-colors focus-visible:ring-2 focus-visible:ring-[var(--color-focus)] ' +
              (size === 'sm' ? 'h-9 text-[12px]' : 'h-11 text-[13px]') +
              ' font-medium ' +
              (isActive
                ? 'text-[var(--color-primary)]'
                : 'text-[var(--color-text-secondary)] hover:text-[var(--color-text-primary)]')
            }
          >
            {t.icon && <Icon name={t.icon} size={14} />}
            {t.label}
            {typeof t.count === 'number' && (
              <span
                className={
                  'font-metric rounded-full px-1.5 py-0.5 text-[10px] ' +
                  (isActive
                    ? 'bg-[var(--color-primary-soft)] text-[var(--color-primary)]'
                    : 'bg-[var(--color-surface-muted)] text-[var(--color-text-muted)]')
                }
              >
                {t.count}
              </span>
            )}
            {isActive && (
              <span className="absolute inset-x-2 bottom-0 h-0.5 rounded-full bg-[var(--color-primary)]" />
            )}
          </button>
        );
      })}
    </div>
  );
}
