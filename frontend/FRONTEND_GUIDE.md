# RTMS Frontend Architecture Document

This is the frontend architecture for the Racehorse Training & Management System (RTMS). It follows the BMAD frontend-architecture layout and is the reference that developers and AI agents load before changing anything under `frontend/`.

Related documents:

- `AGENTS.md` — the frontend agent definition: persona, rules, tasks, primitive lookup and tokens.
- `HANDOVER_CHECKLIST.md` — the definition of done for a frontend change.

## Change Log

| Date | Version | Description | Author |
|---|---|---|---|
| 2026-10-10 | 2.1 | Every feature screen is on the shared primitives; the raw-element lint rule is now an error. Added `ChoiceInput`, `FileInput`, `Table bare`, `AuthCard`. Documented modal forms, chart colours and the open clean-up list. | Frontend team |
| 2026-10-09 | 2.0 | Rewritten in BMAD frontend-architecture format. Vertical sidebar shell, `(app)` route group, shared primitive library, unified admission screens, motion and token rules. Auth section corrected to the access-token + refresh-cookie flow. | Frontend team |
| — | 1.0 | Initial handover guide (top navigation, per-page shell). | Frontend team |

## 1. Template and Framework Selection

- **Starter:** `create-next-app` with the App Router and TypeScript. No UI kit starter.
- **Framework:** Next.js 16 (App Router) on React 19. This Next.js version has breaking changes compared with older releases; read the relevant guide in `node_modules/next/dist/docs/` before using a framework API. Dynamic `params` and `searchParams` are Promises.
- **Build:** `next build --webpack`.
- **Backend:** Spring Boot REST API. It is the single source of truth for business rules, authorization and data integrity. The frontend handles layout, routing and rendering only.
- **Constraints:** desktop-first (1024px and wider), light theme only, English UI strings and comments.

## 2. Frontend Tech Stack

| Category | Technology | Version | Purpose | Rationale |
|---|---|---|---|---|
| Framework | Next.js (App Router) | 16.3.4 | Routing, layouts, rendering | Team standard; nested layouts keep the shell mounted |
| UI library | React | 19.2.8 | Components | Required by Next.js 16 |
| Language | TypeScript (strict) | 5.x | Type safety | DTO types mirror the backend |
| Styling | Tailwind CSS (CSS-based `@theme`) | 4.x | Utility classes over design tokens | No config file; tokens live in `globals.css` |
| Class utilities | `clsx` + `tailwind-merge` | 2.x / 3.x | `cn()` helper | Caller classes reliably override base classes |
| Accessible primitives | Radix UI (dialog, dropdown-menu, popover, tooltip) | 1.x–2.x | Focus trap, Escape, ARIA for overlays | Behaviour without imposing a visual style |
| Motion | `tw-animate-css` | 1.x | `animate-in` / `animate-out` utilities | CSS only, no runtime cost |
| Toasts | `sonner` | 2.x | Transient feedback | Small, unstyled mode fits the tokens |
| Icons | In-house `Icon` (inline SVG) | — | Icon set | No dependency; add icons as needed |
| State | React state + Context | — | Local and session state | The app has no cross-screen client cache yet |
| Forms | Controlled inputs | — | Form state | Backend validates; forms stay simple |
| Data fetching | `services/api.ts` wrappers over `fetch` | — | HTTP transport | One place for auth headers and refresh |
| Testing | Node test runner (`node --test`, strip-types) | Node 18+ | Logic tests | No extra tooling |
| Lint | ESLint 9 + `eslint-config-next` | 9.x | Code quality and UI rules | Flags raw HTML controls in feature code |

Not used on purpose: a component kit (shadcn, MUI, Ant), a JS animation library, a global store, a data-fetching cache.

## 3. Project Structure

```
frontend/
├── AGENTS.md                 # agent definition, tasks, primitive lookup, tokens
├── FRONTEND_GUIDE.md         # this document
├── HANDOVER_CHECKLIST.md     # definition of done
├── eslint.config.mjs         # includes the raw-element rule
└── src/
    ├── app/
    │   ├── layout.tsx        # root layout: font, <Providers>
    │   ├── providers.tsx     # Auth, Notifications, Tooltip, Toaster
    │   ├── globals.css       # design tokens (@theme) and global rules
    │   ├── page.tsx          # redirect to /login or the role landing
    │   ├── login/  register/ # public pages, no shell
    │   ├── design-test/      # live showcase of every primitive
    │   └── (app)/            # every signed-in route
    │       ├── layout.tsx    # AuthGate > AppShell, mounted once
    │       ├── template.tsx  # page-enter animation
    │       └── manager/ trainer/ veterinarian/ groom/ owner/
    ├── components/
    │   ├── ui/               # business-agnostic primitives (index.ts is the catalogue)
    │   ├── layout/           # AppShell, Sidebar, SidebarNav, TopBar, UserMenu, NotificationBell, PageContainer
    │   └── auth/             # AuthGate, RoleGuard, RoleLanding, AuthCard (sign-in / register frame)
    ├── features/<feature>/   # admissions, training, racing, stable, staff, groom, ...
    │   ├── components/       # domain-aware screens and widgets
    │   ├── services/         # API wrappers for this feature
    │   ├── types/            # DTO types that mirror the backend
    │   └── shared/           # (optional) blocks reused across roles inside the feature
    ├── services/             # api.ts (transport), auth.ts
    ├── context/              # AuthContext
    ├── config/               # navigation.ts (sidebar per role)
    ├── lib/                  # cn, display, toast, roleRoute
    └── types/                # cross-cutting types: auth.ts (Role, AuthUser), horse.ts (ApiResponse<T> and the reference entity type)
```

Import alias: `@/*` maps to `src/*`.

## 4. Component Standards

### Layering

| Layer | Location | May know about the domain | May import from |
|---|---|---|---|
| Primitive | `src/components/ui` | No | `lib`, other primitives |
| Shell | `src/components/layout`, `src/components/auth` | Roles and navigation only | primitives, `config`, `context` |
| Feature | `src/features/<feature>/components` | Yes | primitives, the feature's services and types |
| Page | `src/app/**/page.tsx` | Route and role only | `RoleGuard`, `PageContainer`, one feature screen |

Feature code must not use raw `<button>`, `<input>`, `<select>`, `<textarea>`, `<table>` or hand-rolled overlays. `npm run lint` fails on them. The "need → primitive" table is in `AGENTS.md`, and every primitive is shown live at `/design-test`.

### Page template

```tsx
import type { Metadata } from 'next';
import { RoleGuard } from '@/components/auth/RoleGuard';
import { PageContainer } from '@/components/layout/PageContainer';
import { PlanList } from '@/features/training/components/PlanList';

export const metadata: Metadata = { title: 'Training plans | Trainer | RTMS' };

export default function TrainerPlansPage() {
  return (
    <RoleGuard allowedRoles={['HEAD_TRAINER']}>
      <PageContainer>
        <PlanList />
      </PageContainer>
    </RoleGuard>
  );
}
```

### Feature screen template

```tsx
'use client';

import { useEffect, useState } from 'react';
import { Button, DataTable, EmptyState, ListSkeleton, Notice, PageHeader, ScreenLayout } from '@/components/ui';
import { displayError } from '@/lib/display';
import { horseService } from '../services/horseService';
import type { HorseSummary } from '../types';

export function HorseList() {
  const [rows, setRows] = useState<HorseSummary[] | null>(null);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    horseService.list().then(setRows).catch((cause) => setError(displayError(cause, 'Unable to load horses.')));
  }, []);

  return (
    <ScreenLayout variant="list">
      <PageHeader title="Horses" description="Horses in your block." actions={<Button variant="primary" icon="plus">Add horse</Button>} />
      {error && <Notice tone="error">{error}</Notice>}
      {!rows && !error ? <ListSkeleton rows={8} /> : (
        <DataTable rows={rows ?? []} columns={columns} getRowKey={(row) => row.id} emptyTitle="No horses yet" />
      )}
    </ScreenLayout>
  );
}
```

### Naming conventions

| Item | Convention | Example |
|---|---|---|
| Component file and export | `PascalCase.tsx`, named export | `AdmissionTable.tsx` |
| Service file | `camelCaseService.ts` | `stableService.ts` |
| Type | Descriptive `PascalCase` | `AdmissionSummaryResponse` |
| Function, variable | `camelCase` | `formatDate` |
| Route folder | lowercase or kebab-case | `access-control` |
| Hook | `useCamelCase` | `useSidebarState` |
| Test | `<name>.test.ts` in `__tests__/` | `urgentCaseWorkflow.test.ts` |

Files over about 400 lines are split into section components.

## 5. State Management

- **Session:** `AuthContext` (`useAuth()`) exposes `user`, `loading`, `isAuthenticated`, `login`, `logout`, `refreshUser`.
- **Notifications:** `NotificationProvider` (`useNotifications()`) polls every 30 seconds and on window focus.
- **Screen data:** local `useState` + `useEffect` in the feature screen, loaded through the feature service.
- **URL as state:** list filters and tabs that should survive a reload or a back navigation live in search params (see the admission lists).
- **UI preferences:** `localStorage` is allowed for UI-only preferences such as the collapsed sidebar. Never for auth data.

There is no global store. Add one only when two unrelated screens need the same client-side data.

## 6. API Integration

Flow: `page` → `feature screen` → `feature service` → `src/services/api.ts` → Spring Boot.

- Components never call `fetch()`. Services use `apiGet`, `apiPost`, `apiPut`, `apiPatch`, `apiUpload` and `apiGetBlob` from `src/services/api.ts`.
- The base URL is `NEXT_PUBLIC_API_URL`. Never hard-code a host.
- `api.ts` attaches the access token, and on a `401` refreshes it once and retries the request.
- Errors: show them with `displayError(error, fallback)` in a `Notice` (blocking) or a `toast` (result of an action). Never fail silently.

### Service template

```ts
import { apiGet, apiPost } from '@/services/api';
import type { HorseSummary, CreateHorseRequest } from '../types';

export const horseService = {
  list: () => apiGet<HorseSummary[]>('/api/horses?mine=true'),
  create: (body: CreateHorseRequest) => apiPost<HorseSummary>('/api/horses', body),
};
```

### Authentication

1. **Login:** `POST /api/auth/login` returns an access token and sets an HttpOnly refresh cookie.
2. **Access token:** kept in memory only (a module variable in `api.ts`) and sent as `Authorization: Bearer`.
3. **Session restore:** on app mount `AuthContext` calls `POST /api/auth/refresh` (cookie) and then `GET /api/auth/me`.
4. **Expiry:** a `401` triggers one refresh and one retry. If the refresh fails the user is signed out.
5. **Logout:** `POST /api/auth/logout` clears the cookie; the frontend clears the token and user.

Rules: do not parse the JWT on the client, and do not store tokens in `localStorage` or `sessionStorage`.

## 7. Routing

### Route map

| Area | Path | Shell | Guard |
|---|---|---|---|
| Entry | `/` | none | redirects to `/login` or the role landing |
| Public | `/login`, `/register` | none | none |
| Showcase | `/design-test` | none | none |
| Signed-in | `/manager/**`, `/trainer/**`, `/veterinarian/**`, `/groom/**`, `/owner/**` | `(app)/layout.tsx` | `AuthGate` + per-page `RoleGuard` |

Role landings: `HORSE_OWNER` → `/owner`, `GROOM` → `/groom`, `VETERINARIAN` → `/veterinarian`, `HEAD_TRAINER` → `/trainer`, `CLUB_MANAGER` → `/manager` (`src/lib/roleRoute.ts`).

### Application shell

- `src/app/(app)/layout.tsx` mounts `AuthGate` and `AppShell` once, so the shell stays mounted while navigating. Pages must not render `AppShell`.
- `AppShell` = a vertical `Sidebar` on the left (sections and items from `src/config/navigation.ts`, collapsible to icons, account menu at the bottom) plus a `TopBar` (sidebar toggle, current section, notifications).
- The document scrolls; the sidebar and top bar are sticky. Sticky elements inside page content use `top-[var(--sticky-top)]`.
- `template.tsx` gives each page a short fade-in.
- Below 1024px the sidebar stays collapsed. A mobile drawer is not built yet.

### Protection

- `AuthGate` waits for the session and sends unauthenticated visitors to `/login`.
- `RoleGuard allowedRoles={[...]}` on each page redirects a wrong role to its own landing.
- There is no middleware. All of this is UX; the backend `@PreAuthorize` rules enforce security.

### Adding a route

Create `src/app/(app)/<role>/<route>/page.tsx` from the page template, then add the item to that role's section in `src/config/navigation.ts`. An item without `href` renders as a disabled "Soon" entry.

## 8. Styling Guidelines

### Approach

Tailwind utilities over the design tokens defined in the `@theme` block of `src/app/globals.css`. The tokens are the visual source of truth.

| Rule | Do | Do not |
|---|---|---|
| Colour | `bg-[var(--color-surface)]`, `text-[var(--color-text-secondary)]` | `bg-white`, `text-gray-500`, `#6657c7`, `dark:*` |
| Radius | `rounded-[var(--radius-lg)]` for cards, `--radius-sm` for controls | `rounded-md`, `rounded-xl` |
| Shadow | `shadow-[var(--shadow-panel)]`, `shadow-[var(--shadow-popover)]` | `shadow-lg`, `shadow-black/10` |
| Type scale | `text-xs` (12) · `text-sm` (13) · `text-base` (14) · `text-lg` (16) · `text-xl` (20) · `text-2xl` (24) | `text-[13px]`, anything under 12px |
| Class names | `cn('base', condition && 'extra', className)` | string concatenation |
| Layers | `z-[var(--z-modal)]` | `z-50`, `z-[999]` |

The token list is in `AGENTS.md` (Data → Tokens).

### Layout

- `PageContainer` sets the page max-width and padding.
- `ScreenLayout variant="list | detail | dashboard | form"` sets the vertical rhythm; `PageHeader` renders the title, description and actions.
- Cards are `Panel`. Filter rows are `FilterBar`. Uniform lists are `DataTable`.
- Screens for the same concept share the same blocks across roles. The admission list and detail of all five roles are built from `src/features/admissions/shared/components`.

### Motion

- Use the `animate-in` / `animate-out` utilities and the `--duration-fast` (150ms), `--duration-base` (200ms), `--duration-slow` (300ms) tokens.
- Animate opacity and transform only. Exits are faster than entrances. One or two animated things per view.
- Overlays, menus and tooltips already animate inside their primitives.
- `prefers-reduced-motion` is handled globally in `globals.css`.

### Charts

Charts are hand-drawn SVG. Series colours come from tokens passed to SVG attributes (`stroke="var(--color-success)"`), and the legend swatch uses the same token. The module accent tokens (`--color-training`, `--color-medical`, `--color-grooming`, `--color-racing`, `--color-finance`) are available when the status colours are not enough.

### Accessibility

- Every interactive element has a visible focus ring and works from the keyboard.
- Icon-only buttons use `IconButton`, which requires a label.
- Form controls sit inside `FormField`, which wires the label, hint and error to the control.
- Overlays use `Modal`, `ConfirmDialog`, `DropdownMenu` or `Popover` for focus trapping and Escape.
- Text contrast is at least 4.5:1; do not signal state by colour alone.

### Screen states

Every data screen handles four states explicitly: loading (`ListSkeleton`, `DetailSkeleton`, `Skeleton`), error (`Notice`, or `EmptyState` with a Retry action when nothing can be shown), empty (`EmptyState` explaining why), and content.

### Forms

- Controlled inputs inside `FormField`. Errors appear next to the field; a form-level error uses `Notice`.
- A form inside a `Modal` gets an `id`; the submit button lives in the modal `footer` and points at it with `form={id}`.
- Option cards (a bordered `<label>` wrapping a radio or checkbox plus rich content) use `ChoiceInput` for the control.
- Disable the submit button while the request is running (`Button loading`), and prevent duplicate submissions.
- The backend is the final validator. Do not re-implement complex business rules on the client.

## 9. Testing Requirements

- **Runner:** `npm test` runs the Node test runner over `src/features/admissions/components/__tests__/*.test.ts`.
- **What to test:** pure logic — validation, mapping, state transitions. Extract it from components so it can be tested without a DOM.
- **Where:** `__tests__/<name>.test.ts` next to the code it covers. Add the folder to the `test` script glob when a new feature gets tests.
- **Manual verification:** open the changed screens in the running app at 1280px and 1440px for each affected role; check the four states, keyboard focus, and that the correct and a wrong role behave as expected.

### Best practices

1. Test behaviour, not markup.
2. One reason to fail per test; name the test after the rule it protects.
3. No network in tests; pass data in.
4. Keep a regression test for every bug that reached a reviewer.

## 10. Environment Configuration

1. Start PostgreSQL (`docker-compose up -d` at the repo root) and the Spring Boot backend on port 8080.
2. Copy `.env.local.example` to `.env.local`.
3. Run:

```bash
cd frontend
npm install
npm run dev
```

4. Open `http://localhost:3000`. Seed test accounts are created by the backend migrations.

| Variable | Example | Purpose |
|---|---|---|
| `NEXT_PUBLIC_API_URL` | `http://localhost:8080` | Base URL of the Spring Boot API |

| Script | Purpose |
|---|---|
| `npm run dev` | Development server |
| `npm run lint` | ESLint, including the raw-element rule |
| `npm run build` | Production build (webpack) |
| `npm test` | Logic tests |

## 11. Frontend Developer Standards

### Critical coding rules

1. Never call `fetch()` in a component; go through a feature service and `services/api.ts`.
2. Never hard-code the backend URL; use `NEXT_PUBLIC_API_URL`.
3. Never store or parse tokens on the client beyond what `api.ts` does.
4. Never render `AppShell` in a page, and keep `page.tsx` free of UI and data fetching.
5. Never put domain components in `components/ui`, or generic primitives in a feature.
6. Never use raw HTML controls, tables or hand-rolled overlays in feature code.
7. Never use Tailwind palette colours, hex values, `dark:` classes or arbitrary pixel font sizes.
8. Never leave a data screen without loading, error and empty states.
9. Never treat a hidden button as security; the backend decides.
10. Never ship mock business data.

### Definition of done

- Real backend API through a feature service.
- Shared primitives and tokens only.
- Loading, error and empty states handled; forms prevent duplicate submission.
- Correct role and a wrong role checked manually.
- `npm run lint`, `npm run build` and `npm test` pass.
- `HANDOVER_CHECKLIST.md` walked through.

### Git workflow

- Pull the latest code before starting and read this document.
- Confirm the backend API and DTOs exist before building a service.
- One concern per pull request. Do not modify shared foundation files unless the change needs it.

### Quick reference

| I need to… | Go to |
|---|---|
| Find a primitive | `AGENTS.md` → Primitive lookup, or `/design-test` |
| Add a sidebar item | `src/config/navigation.ts` |
| Add or look up a token | `src/app/globals.css` |
| Format a date, enum, number or error | `src/lib/display.ts` |
| Show a toast | `toast` from `src/lib/toast.ts` |
| Combine class names | `cn` from `src/lib/cn.ts` |
| Call the backend | the feature's `services/` file → `src/services/api.ts` |
| Build an admission screen | `src/features/admissions/shared/components` |
| Follow a step-by-step workflow | `AGENTS.md` → Tasks (`*new-feature`, `*new-page`, `*add-nav-item`, `*new-primitive`, `*refactor-component`, `*audit`, `*verify`) |

### Current status

- **Complete:** auth and role routing, application shell with vertical sidebar, design tokens, primitive library, admission screens unified across the five roles, and every feature screen moved onto the shared primitives (the raw-element lint rule is an error).
- **Open clean-up:** eight feature files still format dates locally instead of using `src/lib/display.ts`; `VetReviewForm` and `VetAdmissionQueue` are far over the 400-line guideline; `AdmissionDocumentPreview` uses a native `<dialog>`. The list is in `AGENTS.md` under the `refactor-component` task.
- **Placeholders:** the Manager, Veterinarian, Groom and Owner landing pages still render `RoleLanding`; sidebar items without a route show a "Soon" tag; `features/horses`, `features/health` and `features/care` are empty scaffolds.
- **Not started:** mobile layout below 1024px, dark theme, internationalisation.
