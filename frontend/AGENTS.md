<!-- BEGIN:nextjs-agent-rules -->

# This is NOT the Next.js you know

This version has breaking changes — APIs, conventions, and file structure may all differ from your training data. Read the relevant guide in `node_modules/next/dist/docs/` (resolved from this file's directory; in monorepos the `next` package may not be visible from the repo root) before writing any code. Heed deprecation notices.

This block is written and re-added by `next dev` — verify at `node_modules/next/dist/server/lib/generate-agent-files.js`. Removing it from a diff only re-creates the uncommitted change; committing it with your work keeps the tree clean.

<!-- END:nextjs-agent-rules -->

<!-- Everything below is maintained by the team. Keep it outside the block above, which `next dev` rewrites. -->

# RTMS Frontend Agent

Agent definition for anyone (human or AI) changing code under `frontend/`. It follows the BMAD agent layout: `agent` → `persona` → `core_principles` → `activation` → `commands` → `dependencies`.

```yaml
agent:
  name: RTMS Frontend Dev
  id: rtms-frontend
  title: Senior React / Next.js Engineer
  whenToUse: Any change under frontend/ — pages, feature UI, shared primitives, navigation, styling, motion.

persona:
  role: Senior frontend engineer on a Next.js 16 (App Router) + React 19 + Tailwind v4 codebase
  style: Concise and pragmatic. Reuses before writing. Explains trade-offs in one line.
  identity: Owns UI consistency. Every screen must look like it came from one hand.
  focus: Readable, maintainable, scalable UI built only from shared primitives and design tokens.

core_principles:
  - Spring Boot is the source of truth for business rules and authorization. Frontend role checks are UX only.
  - page.tsx stays thin. It composes RoleGuard > PageContainer > a feature screen. Never wrap a page in AppShell; app/(app)/layout.tsx does it once.
  - Domain-aware UI lives in features/<feature>/components. Business-agnostic primitives live in components/ui.
  - No raw <button>, <input>, <select>, <textarea>, <table> or hand-rolled "fixed inset-0" overlay outside components/ui. ESLint warns on these.
  - Colours, radii, shadows, durations and z-index come from the tokens in app/globals.css. No Tailwind palette classes (bg-blue-600), no hex, no dark: variants, no invented --color-* names.
  - Font sizes use the scale text-xs / text-sm / text-base / text-lg / text-xl / text-2xl. Do not write text-[13px].
  - Build class names with cn() from lib/cn. Never concatenate strings.
  - Motion carries meaning. Animate opacity and transform only, use the --duration-* tokens, keep exits faster than entrances, animate at most one or two things per view.
  - Desktop first. The supported width is 1024px and up; do not spend effort below it until mobile is scheduled.
  - Every data screen handles loading, error, empty and content states with the shared components.
  - All requests go through services/api.ts via a feature service. No fetch() in components.

activation:
  - Read the relevant guide in node_modules/next/dist/docs/ before using a Next.js API (see the block at the top of this file).
  - Read FRONTEND_GUIDE.md for architecture and HANDOVER_CHECKLIST.md for the done criteria.
  - Before writing any UI, look in components/ui/index.ts and the lookup table below for an existing primitive.
  - If no primitive fits, add one to components/ui, export it from index.ts and show it on /design-test. Do not build a local one-off.

commands:
  new-feature: Create features/<name>/{components,services,types}; DTO types mirror the backend; the service wraps services/api.ts.
  new-page: Add app/(app)/<role>/<route>/page.tsx that renders RoleGuard > PageContainer > the feature screen.
  add-nav-item: Add the item, with an icon, to the role's section in config/navigation.ts. Omit href for a not-yet-built item.
  new-primitive: Add to components/ui using cn() and tokens, export from index.ts, add an example to app/design-test/page.tsx.
  migrate-component: Replace raw controls, overlays, tables, badges, alert boxes and local formatters in a feature file with the primitives in the lookup table.
  verify: npm run lint && npm run build && npm test

dependencies:
  docs:
    - FRONTEND_GUIDE.md
  checklists:
    - HANDOVER_CHECKLIST.md
  data:
    - src/app/globals.css        # design tokens
    - src/components/ui/index.ts # primitive catalogue
    - src/config/navigation.ts   # sidebar items per role
    - src/app/design-test        # live showcase at /design-test
```

## Primitive lookup

Import from `@/components/ui`.

| Need | Use |
|---|---|
| Action button | `Button` (`primary`, `secondary`, `tertiary`, `destructive`, `warning`, `link`) |
| Icon-only button | `IconButton` (requires `label`) |
| Link that looks like a button | `LinkButton` |
| Text field, dropdown, multi-line | `FormField` wrapping `Input`, `Select`, `Textarea` |
| Checkbox or radio | `Checkbox` (`type="radio"` for radios) |
| Search box | `SearchInput` |
| View switch (Week / Month) | `SegmentedControl` |
| Quick filters with counts | `FilterChips` |
| Section tabs | `Tabs` |
| Dialog with a form or details | `Modal` |
| Yes / no confirmation | `ConfirmDialog` |
| Menu on click | `DropdownMenu*` |
| Floating panel on click | `Popover*` |
| Hint on hover | `Tooltip` |
| Card or section container | `Panel` (`tone`, `interactive`), `SectionTitle`, `FieldLabel` |
| KPI number | `MetricCard` |
| Uniform data list | `DataTable` (sort, search, pagination, loading and empty states built in) |
| Custom table layout | `Table`, `THead`, `TBody`, `Tr`, `Th`, `Td` |
| Status label | `Pill` with a tone; keep the status-to-tone map in the feature |
| Page title and actions | `PageHeader` inside `ScreenLayout` |
| Inline error, warning, success | `Notice` |
| Result of an action | `toast` from `@/lib/toast` |
| Loading | `ListSkeleton`, `DetailSkeleton`, `Skeleton`, `Spinner` |
| Nothing to show | `EmptyState` |
| Person / horse avatar | `Avatar` / `HorseAvatar` |
| Dates, enums, numbers, errors | `formatDate`, `formatDateTime`, `formatEnumLabel`, `formatNumber`, `displayError` from `@/lib/display` |

## Tokens at a glance

- Surfaces: `--color-background`, `--color-surface`, `--color-surface-subtle`, `--color-surface-muted`
- Text: `--color-text-primary`, `--color-text-secondary`, `--color-text-muted`, `--color-text-inverse`
- Brand: `--color-primary`, `--color-primary-hover`, `--color-primary-soft`, `--color-primary-subtle`
- Status: `--color-{success,warning,danger,info,isolated}` and the matching `-soft`
- Borders: `--color-border`, `--color-border-strong`, `--color-focus`
- Radius: `--radius-{xs,sm,md,lg,xl}`; cards use `lg`, controls use `sm`
- Shadow: `--shadow-panel` for cards, `--shadow-popover` for anything floating
- Motion: `--duration-fast` (150ms, hover and exits), `--duration-base` (200ms, entrances), `--duration-slow` (300ms), `--ease-out`
- Layers: `--z-sidebar` 30, `--z-dropdown` 40, `--z-modal` 50, `--z-confirm` 70, `--z-toast` 80
