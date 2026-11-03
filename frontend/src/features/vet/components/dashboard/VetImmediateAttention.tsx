"use client";

import { useRouter } from "next/navigation";
import { Panel, SectionTitle } from "@/components/ui/Panel";
import { Pill } from "@/components/ui/StatusBadge";
import { LinkButton } from "@/components/ui/Button";
import { HorseAvatar } from "@/components/ui/HorseAvatar";
import { Icon } from "@/components/ui/Icon";
import { ListSkeleton } from "@/components/ui/states";
import type { CareSchedule } from "@/features/admissions/types";
import type { Horse } from "@/features/stable/types";

interface VetImmediateAttentionProps {
  loading: boolean;
  emergencies: CareSchedule[];
  horseMap: Map<number, Horse>;
  stallMap: Map<number, string>;
}

/** Right column card 1: active urgent cases that need the vet's attention now. */
export function VetImmediateAttention({
  loading,
  emergencies,
  horseMap,
  stallMap,
}: VetImmediateAttentionProps) {
  const router = useRouter();

  return (
    <Panel
      padded
      tone={emergencies.length > 0 ? "danger" : "default"}
    >
      <div className="mb-3 flex items-center justify-between">
        <div className="flex items-center gap-2">
          <SectionTitle>Immediate Attention</SectionTitle>
          {emergencies.length > 0 && (
            <Pill tone="danger" size="sm">
              {emergencies.length} Active
            </Pill>
          )}
        </div>
        <LinkButton
          href="/veterinarian/urgent"
          variant="tertiary"
          size="sm"
        >
          View All
        </LinkButton>
      </div>

      {loading ? (
        <ListSkeleton rows={2} />
      ) : emergencies.length === 0 ? (
        <div className="flex items-center gap-3 rounded-[var(--radius-md)] border border-[var(--color-border)] bg-[var(--color-surface-muted)] p-3 text-sm text-[var(--color-text-secondary)]">
          <span className="flex h-8 w-8 shrink-0 items-center justify-center rounded-full bg-[var(--color-success-soft)] text-[var(--color-success)]">
            <Icon name="check" size={16} />
          </span>
          <p className="font-medium text-[var(--color-text-primary)]">
            No active emergencies
          </p>
        </div>
      ) : (
        <div className="space-y-3">
          {emergencies.map((schedule) => {
            const horse = horseMap.get(schedule.horseId);
            const horseName =
              schedule.candidateName ||
              horse?.name ||
              `Horse #${schedule.horseId}`;
            const stallCode =
              horse?.currentStallId != null
                ? stallMap.get(horse.currentStallId)
                : null;
            const excerpt =
              schedule.description ||
              schedule.notes ||
              "Emergency examination requested";

            return (
              <div
                key={schedule.id}
                role="button"
                tabIndex={0}
                onClick={() => router.push(`/veterinarian/urgent/${schedule.id}`)}
                onKeyDown={(event) => {
                  // Guard: Enter on the inner button bubbles here too — only act when the card itself is focused.
                  if (event.target !== event.currentTarget) return;
                  if (event.key === "Enter" || event.key === " ") {
                    event.preventDefault();
                    router.push(`/veterinarian/urgent/${schedule.id}`);
                  }
                }}
                className="cursor-pointer rounded-[var(--radius-md)] border border-[var(--color-danger)] bg-[var(--color-surface)] p-3 text-sm shadow-xs transition-shadow hover:ring-1 hover:ring-[var(--color-border-strong)] focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-[var(--color-focus)]"
              >
                <div className="flex items-center justify-between">
                  <div className="flex items-center gap-2">
                    <HorseAvatar
                      name={horseName}
                      image={horse?.imageUrl}
                      size={32}
                      rounded="sm"
                    />
                    <div>
                      <span className="font-semibold text-[var(--color-text-primary)]">
                        {horseName}
                      </span>
                      <span className="ml-2 text-xs text-[var(--color-text-muted)]">
                        {stallCode ? `Stall ${stallCode}` : "Unassigned"}
                      </span>
                    </div>
                  </div>
                  <Pill tone="danger" size="sm">
                    {schedule.status}
                  </Pill>
                </div>

                <p className="mt-2 line-clamp-2 text-xs text-[var(--color-text-secondary)]">
                  {excerpt}
                </p>

                <div className="mt-3 flex justify-end">
                  <LinkButton
                    href={`/veterinarian/urgent/${schedule.id}`}
                    variant="destructive"
                    size="sm"
                    onClick={(event) => event.stopPropagation()}
                  >
                    Attend Emergency
                  </LinkButton>
                </div>
              </div>
            );
          })}
        </div>
      )}
    </Panel>
  );
}
