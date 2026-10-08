import { Icon } from '@/components/ui/Icon';

interface AdmissionSearchFieldProps {
  value: string;
  onChange: (value: string) => void;
  label?: string;
  placeholder?: string;
  className?: string;
}

export function AdmissionSearchField({
  value,
  onChange,
  label = 'Search horse name',
  placeholder = 'Search horse name',
  className = '',
}: AdmissionSearchFieldProps) {
  return (
    <label className={`block min-w-0 text-xs font-medium text-[var(--color-text-secondary)] ${className}`}>
      {label}
      <span className="relative mt-1.5 block">
        <Icon name="search" size={15} className="pointer-events-none absolute left-3 top-1/2 -translate-y-1/2 text-[var(--color-text-muted)]" />
        <input
          type="search"
          value={value}
          onChange={(event) => onChange(event.target.value)}
          placeholder={placeholder}
          className="h-9 w-full rounded-[var(--radius-sm)] border border-[var(--color-border-strong)] bg-[var(--color-surface)] pl-9 pr-3 text-sm text-[var(--color-text-primary)] outline-none placeholder:text-[var(--color-text-muted)] focus-visible:ring-2 focus-visible:ring-[var(--color-focus)]"
        />
      </span>
    </label>
  );
}
