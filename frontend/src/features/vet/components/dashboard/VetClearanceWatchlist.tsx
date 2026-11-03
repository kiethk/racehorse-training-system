"use client";

import { useState } from "react";
import { useRouter } from "next/navigation";
import { Panel, SectionTitle } from "@/components/ui/Panel";
import { Pill } from "@/components/ui/StatusBadge";
import { Button, LinkButton } from "@/components/ui/Button";
import { HorseAvatar } from "@/components/ui/HorseAvatar";
import { Icon } from "@/components/ui/Icon";
import { ListSkeleton } from "@/components/ui/states";
import type { Horse } from "@/features/stable/types";
import type { VetWatchlistHorse } from "../../types";

/** Watchlist rows shown before the "View all" toggle. */
const WATCHLIST_PREVIEW_COUNT = 4;

interface VetClearanceWatchlistProps {
  loading: boolean;
  blockedHorses: VetWatchlistHorse[];
  horseMap: Map<number, Horse>;
}

/** Right column card 2: horses on a BLOCKED training decision (stable-wide quarantine). */
export function VetClearanceWatchlist({
  loading,
  blockedHorses,
  horseMap,
}: VetClearanceWatchlistProps) {
  // The watchlist can hold the whole quarantine row; collapsed to a few rows by default.
  const [showAllWatchlist, setShowAllWatchlist] = useState(false);
  const router = useRouter();

  return (
    <Panel padded tone={blockedHorses.length > 0 ? "warning" : "default"}>
      <div className="mb-3 flex items-center gap-2">
        <SectionTitle>Training Clearance Watchlist</SectionTitle>
        {blockedHorses.length > 0 && (
          <Pill tone="warning" size="sm">
            {blockedHorses.length} Blocked
          </Pill>
        )}
      </div>

      {loading ? (
        <ListSkeleton rows={3} />
      ) : blockedHorses.length === 0 ? (
        <div className="flex items-center gap-3 rounded-[var(--radius-md)] border border-[var(--color-border)] bg-[var(--color-surface-muted)] p-3 text-sm text-[var(--color-text-secondary)]">
          <span className="flex h-8 w-8 shrink-0 items-center justify-center rounded-full bg-[var(--color-success-soft)] text-[var(--color-success)]">
            <Icon name="check" size={16} />
          </span>
          <p className="font-medium text-[var(--color-text-primary)]">
            All horses cleared
          </p>
        </div>
      ) : (
        <div className="space-y-3">
          {/* Collapsed by default: one row of cards; expand to see the whole quarantine list */}
          <div className="grid grid-cols-1 gap-3 sm:grid-cols-2 lg:grid-cols-4">
            {(showAllWatchlist
              ? blockedHorses
              : blockedHorses.slice(0, WATCHLIST_PREVIEW_COUNT)
            ).map((item) => (
            <div
              key={item.id}
              role="button"
              tabIndex={0}
              onClick={() => router.push(`/veterinarian/health?horseId=${item.id}`)}
              onKeyDown={(event) => {
                // Guard: Enter on the inner link bubbles here too — only act when the card itself is focused.
                if (event.target !== event.currentTarget) return;
                if (event.key === "Enter" || event.key === " ") {
                  event.preventDefault();
                  router.push(`/veterinarian/health?horseId=${item.id}`);
                }
              }}
              className="flex cursor-pointer flex-col gap-2 rounded-[var(--radius-md)] border border-[var(--color-border)] bg-[var(--color-surface)] p-3 text-sm transition-shadow hover:ring-1 hover:ring-[var(--color-border-strong)] focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-[var(--color-focus)]"
            >
              <div className="flex items-start justify-between gap-2">
                <div className="flex min-w-0 items-center gap-2">
                  <HorseAvatar
                    name={item.name}
                    image={horseMap.get(item.id)?.imageUrl}
                    size={32}
                    rounded="sm"
                  />
                  <div className="min-w-0">
                    <span className="block truncate font-semibold text-[var(--color-text-primary)]">
                      {item.name}
                    </span>
                    <p className="truncate text-xs text-[var(--color-text-muted)]">
                      Stall: {item.stallCode}
                      {item.breed ? ` · ${item.breed}` : ""}
                    </p>
                  </div>
                </div>
                <Pill tone="warning" size="sm">
                  BLOCKED
                </Pill>
              </div>

              <div className="rounded-[var(--radius-xs)] bg-[var(--color-warning-soft)] px-2 py-1.5 text-xs text-[var(--color-text-primary)]">
                <span className="font-medium text-[var(--color-warning)]">
                  Hold Reason:{" "}
                </span>
                {item.reason}
              </div>

              <div className="flex justify-end">
                <LinkButton
                  href={`/veterinarian/health?horseId=${item.id}`}
                  variant="tertiary"
                  size="sm"
                  onClick={(event) => event.stopPropagation()}
                >
                  View Health Profile &rarr;
                </LinkButton>
              </div>
            </div>
          ))}
          </div>

          {blockedHorses.length > WATCHLIST_PREVIEW_COUNT && (
            <Button
              variant="secondary"
              size="sm"
              icon={showAllWatchlist ? "chevron-right" : "chevron-down"}
              onClick={() => setShowAllWatchlist((v) => !v)}
              className="w-full transition-all active:scale-[0.98]"
            >
              {showAllWatchlist
                ? "Show less"
                : `View all (${blockedHorses.length} blocked horses)`}
            </Button>
          )}
        </div>
      )}
    </Panel>
  );
}
