import type { ReactElement, SVGProps } from 'react';

type IconName =
  | 'search'
  | 'plus'
  | 'chevron-down'
  | 'chevron-right'
  | 'bell'
  | 'lock'
  | 'more'
  | 'grid'
  | 'list'
  | 'x'
  | 'download'
  | 'check'
  | 'alert-triangle'
  | 'heart-pulse'
  | 'activity'
  | 'calendar'
  | 'stethoscope'
  | 'user'
  | 'shield'
  | 'circle'
  | 'arrow-left'
  | 'clock'
  | 'clipboard'
  | 'pill'
  | 'flag'
  | 'gauge'
  | 'droplet'
  | 'scissors'
  | 'building'
  | 'users'
  | 'file-text'
  | 'target'
  | 'git-branch'
  | 'trending-up'
  | 'refresh'
  | 'rotate'
  | 'minus'
  | 'utensils'
  | 'horse'
  | 'camera'
  | 'star'
  | 'filter';

const paths: Record<IconName, ReactElement> = {
  search: (
    <>
      <circle cx="11" cy="11" r="7" />
      <path d="m21 21-4.3-4.3" />
    </>
  ),
  plus: <path d="M12 5v14M5 12h14" />,
  'chevron-down': <path d="m6 9 6 6 6-6" />,
  'chevron-right': <path d="m9 6 6 6-6 6" />,
  bell: (
    <>
      <path d="M6 8a6 6 0 0 1 12 0c0 7 3 9 3 9H3s3-2 3-9" />
      <path d="M10.3 21a1.94 1.94 0 0 0 3.4 0" />
    </>
  ),
  lock: (
    <>
      <rect x="4" y="11" width="16" height="10" rx="2" />
      <path d="M8 11V7a4 4 0 0 1 8 0v4" />
    </>
  ),
  more: (
    <>
      <circle cx="5" cy="12" r="1.4" />
      <circle cx="12" cy="12" r="1.4" />
      <circle cx="19" cy="12" r="1.4" />
    </>
  ),
  grid: (
    <>
      <rect x="3" y="3" width="7" height="7" rx="1" />
      <rect x="14" y="3" width="7" height="7" rx="1" />
      <rect x="3" y="14" width="7" height="7" rx="1" />
      <rect x="14" y="14" width="7" height="7" rx="1" />
    </>
  ),
  list: (
    <>
      <path d="M8 6h13M8 12h13M8 18h13" />
      <path d="M3 6h.01M3 12h.01M3 18h.01" />
    </>
  ),
  x: <path d="M18 6 6 18M6 6l12 12" />,
  download: <path d="M12 3v12m0 0 4-4m-4 4-4-4M5 21h14" />,
  check: <path d="m5 12 5 5L20 7" />,
  'alert-triangle': (
    <>
      <path d="M10.3 3.9 1.8 18a2 2 0 0 0 1.7 3h17a2 2 0 0 0 1.7-3L13.7 3.9a2 2 0 0 0-3.4 0Z" />
      <path d="M12 9v4M12 17h.01" />
    </>
  ),
  'heart-pulse': (
    <>
      <path d="M19 14c1.5-1.5 3-3.4 3-5.5A4.5 4.5 0 0 0 12 5 4.5 4.5 0 0 0 2 8.5c0 2.1 1.5 4 3 5.5l7 7Z" />
      <path d="M3.5 12h4l1.5-3 2.5 6 1.5-3h4" />
    </>
  ),
  activity: <path d="M22 12h-4l-3 9L9 3l-3 9H2" />,
  calendar: (
    <>
      <rect x="3" y="4" width="18" height="18" rx="2" />
      <path d="M16 2v4M8 2v4M3 10h18" />
    </>
  ),
  stethoscope: (
    <>
      <path d="M4 3v6a4 4 0 0 0 8 0V3" />
      <path d="M4 3H2M12 3h-2M8 17a5 5 0 0 0 10 0v-2" />
      <circle cx="19" cy="12" r="2" />
    </>
  ),
  user: (
    <>
      <circle cx="12" cy="8" r="4" />
      <path d="M4 21a8 8 0 0 1 16 0" />
    </>
  ),
  shield: <path d="M12 3 5 6v6c0 4 3 7 7 9 4-2 7-5 7-9V6l-7-3Z" />,
  circle: <circle cx="12" cy="12" r="9" />,
  'arrow-left': <path d="M19 12H5m0 0 6 6m-6-6 6-6" />,
  clock: (
    <>
      <circle cx="12" cy="12" r="9" />
      <path d="M12 7v5l3 2" />
    </>
  ),
  clipboard: (
    <>
      <rect x="8" y="3" width="8" height="4" rx="1" />
      <path d="M9 5H6a2 2 0 0 0-2 2v12a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V7a2 2 0 0 0-2-2h-3" />
    </>
  ),
  pill: (
    <>
      <rect x="3" y="8" width="18" height="8" rx="4" />
      <path d="M12 8v8" />
    </>
  ),
  flag: (
    <>
      <path d="M4 21V4" />
      <path d="M4 4h13l-2 4 2 4H4" />
    </>
  ),
  gauge: (
    <>
      <path d="M12 14 16 9" />
      <path d="M3.5 17a9 9 0 1 1 17 0" />
    </>
  ),
  droplet: <path d="M12 3s6 6.5 6 11a6 6 0 0 1-12 0c0-4.5 6-11 6-11Z" />,
  scissors: (
    <>
      <circle cx="6" cy="6" r="2.5" />
      <circle cx="6" cy="18" r="2.5" />
      <path d="M8 8l12 8M8 16 20 8" />
    </>
  ),
  building: (
    <>
      <rect x="4" y="3" width="16" height="18" rx="1" />
      <path d="M9 8h.01M9 12h.01M9 16h.01M15 8h.01M15 12h.01M15 16h.01" />
    </>
  ),
  users: (
    <>
      <circle cx="9" cy="8" r="3.5" />
      <path d="M3 20a6 6 0 0 1 12 0" />
      <path d="M16 5a3.5 3.5 0 0 1 0 6M21 20a6 6 0 0 0-4-5.6" />
    </>
  ),
  'file-text': (
    <>
      <path d="M14 3H7a2 2 0 0 0-2 2v14a2 2 0 0 0 2 2h10a2 2 0 0 0 2-2V8Z" />
      <path d="M14 3v5h5M9 13h6M9 17h6" />
    </>
  ),
  target: (
    <>
      <circle cx="12" cy="12" r="9" />
      <circle cx="12" cy="12" r="5" />
      <circle cx="12" cy="12" r="1.2" />
    </>
  ),
  'git-branch': (
    <>
      <circle cx="6" cy="5" r="2.5" />
      <circle cx="6" cy="19" r="2.5" />
      <circle cx="18" cy="7" r="2.5" />
      <path d="M6 7.5v9M18 9.5c0 4-4 4.5-6 5" />
    </>
  ),
  'trending-up': <path d="M3 17 10 10l4 4 7-7m0 0h-5m5 0v5" />,
  refresh: (
    <>
      <path d="M3 12a9 9 0 0 1 15-6.7L21 8" />
      <path d="M21 3v5h-5M21 12a9 9 0 0 1-15 6.7L3 16" />
      <path d="M3 21v-5h5" />
    </>
  ),
  rotate: (
    <>
      <path d="M4 12a8 8 0 0 1 13.7-5.6L21 9" />
      <path d="M21 4v5h-5M20 12a8 8 0 0 1-13.7 5.6L3 15" />
      <path d="M3 20v-5h5" />
    </>
  ),
  minus: <path d="M5 12h14" />,
  utensils: (
    <>
      <path d="M4 3v7a2 2 0 0 0 4 0V3M6 10v11" />
      <path d="M17 3c-1.5 0-3 1.5-3 5s1.5 4 3 4v9" />
    </>
  ),
  horse: (
    <>
      <path d="M4 16c1-3 1.5-7 5-8 2.5-.7 5 1 6.5 3 1.2 1.6 2.8 2.2 4.5 1.5" />
      <path d="M8 9 6 5l3 1 2-2 1.5 3M7 15l-1 5M15 15l1 5M18 13l3 2" />
      <path d="M10 12h.01" />
    </>
  ),
  camera: (
    <>
      <path d="M4 7h3l1.5-2h7L17 7h3a2 2 0 0 1 2 2v9a2 2 0 0 1-2 2H4a2 2 0 0 1-2-2V9a2 2 0 0 1 2-2Z" />
      <circle cx="12" cy="13" r="3.5" />
    </>
  ),
  star: (
    <path d="m12 3 2.8 5.7 6.2.9-4.5 4.4 1.1 6.2-5.6-2.9-5.6 2.9 1.1-6.2L3 9.6l6.2-.9L12 3Z" />
  ),
  filter: (
    <path d="M4 5h16M7 12h10M10 19h4" />
  ),
};

interface IconProps extends Omit<SVGProps<SVGSVGElement>, 'name'> {
  name: IconName;
  size?: number;
}

export function Icon({ name, size = 16, ...props }: IconProps) {
  return (
    <svg
      width={size}
      height={size}
      viewBox="0 0 24 24"
      fill="none"
      stroke="currentColor"
      strokeWidth="1.75"
      strokeLinecap="round"
      strokeLinejoin="round"
      aria-hidden="true"
      {...props}
    >
      {paths[name]}
    </svg>
  );
}

export type { IconName };
