import { BrandLogo } from "@/components/ui/BrandLogo";
import { Button } from "@/components/ui/Button";
import { Panel, SectionTitle, FieldLabel } from "@/components/ui/Panel";
import { StatusBadge } from "@/components/ui/StatusBadge";
import { EmptyState, ListSkeleton, DetailSkeleton } from "@/components/ui/states";

export default function DesignTestPage() {
  return (
    <div className="p-8 space-y-8 max-w-4xl mx-auto">
      <h1 className="text-2xl font-bold">Design System Test</h1>

      <section className="space-y-4">
        <SectionTitle>Brand Logo</SectionTitle>
        <div className="flex gap-4 items-center">
          <BrandLogo className="w-8 h-8" />
          <BrandLogo className="w-12 h-12" />
          <BrandLogo className="w-16 h-16" />
        </div>
      </section>

      <section className="space-y-4">
        <SectionTitle>Buttons</SectionTitle>
        <div className="flex flex-wrap gap-4 items-center">
          <Button variant="primary">Primary</Button>
          <Button variant="secondary">Secondary</Button>
          <Button variant="tertiary">Tertiary</Button>
          <Button variant="destructive">Destructive</Button>
          <Button variant="primary" loading>Loading</Button>
          <Button variant="primary" disabled>Disabled</Button>
          <Button variant="primary" icon="plus">With Icon</Button>
        </div>
      </section>

      <section className="space-y-4">
        <SectionTitle>Panels</SectionTitle>
        <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
          <Panel padded>
            <SectionTitle>Panel Title</SectionTitle>
            <p className="mt-2 text-sm text-[var(--color-text-secondary)]">
              This is a standard padded panel. It uses surface background and subtle borders.
            </p>
          </Panel>
          <Panel padded>
            <FieldLabel>Field Label</FieldLabel>
            <div className="mt-1 font-medium text-[var(--color-text-primary)]">Value goes here</div>
          </Panel>
        </div>
      </section>

      <section className="space-y-4">
        <SectionTitle>Status Badges</SectionTitle>
        <div className="flex flex-wrap gap-4">
          <StatusBadge status="CANDIDATE" />
          <StatusBadge status="ELIGIBLE" />
          <StatusBadge status="MONITORING" />
          <StatusBadge status="INJURED" />
          <StatusBadge status="QUARANTINED" />
          <StatusBadge status="REJECTED" />
        </div>
      </section>

      <section className="space-y-4">
        <SectionTitle>Empty State</SectionTitle>
        <Panel>
          <EmptyState 
            icon="search" 
            title="No records found" 
            description="We couldn't find any data matching your criteria."
            action={<Button variant="primary">Create New</Button>}
          />
        </Panel>
      </section>

      <section className="space-y-4">
        <SectionTitle>Skeletons</SectionTitle>
        <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
          <Panel>
            <ListSkeleton rows={3} />
          </Panel>
          <Panel>
            <DetailSkeleton />
          </Panel>
        </div>
      </section>
    </div>
  );
}
