import type { ReactNode } from 'react';
import { Button, LinkButton } from './Button';

/**
 * The "back" control of a detail or form screen: an arrow and a label, placed first, at the top left of the page.
 * Pass `href` to go to a known list, or `onClick` when the caller decides (for example `router.back()`).
 */
export function BackLink({
  href,
  onClick,
  children,
}: { children: ReactNode } & ({ href: string; onClick?: never } | { onClick: () => void; href?: never })) {
  // The negative margin lines the arrow up with the page title under it.
  if (href !== undefined) {
    return (
      <LinkButton href={href} variant="tertiary" size="sm" icon="arrow-left" className="-ml-2.5 self-start">
        {children}
      </LinkButton>
    );
  }
  return (
    <Button type="button" variant="tertiary" size="sm" icon="arrow-left" onClick={onClick} className="-ml-2.5 self-start">
      {children}
    </Button>
  );
}
