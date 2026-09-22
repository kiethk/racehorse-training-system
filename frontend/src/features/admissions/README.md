# Frontend Architecture & Coding Conventions

This project uses a feature-based architecture to ensure scalability and avoid monolithic structures as the system grows. This `README.md` serves as the source of truth for frontend coding standards.

## 1. Feature Folder Structure

A typical feature (like `admissions`) should have the following structure:

```
features/admissions/
├── components/       # Domain-specific UI components (e.g., AdmissionTable.tsx, CandidateProfileCard.tsx)
├── services/         # Feature-specific API calls (e.g., admissionService.ts)
└── types/            # Feature-specific TypeScript types (e.g., admission.ts)
```

## 2. Architecture Rules

The `src/` directory is strictly organized by responsibility:

- `app/` = Next.js routing and page composition only.
- `features/` = Business feature UI, local services, and local types.
- `components/ui/` = Pure, reusable, business-agnostic visual primitives (e.g., Button, Panel).
- `components/layout/` = Global application layout (AppShell, TopNav, PageContainer).
- `components/auth/` = Auth wrappers and role guards.
- `services/` = Shared infrastructure (API transport `api.ts`, auth logic).
- `context/` = Global state/context (e.g., `AuthContext`).
- `config/` = Static configuration (e.g., `navigation.ts`).
- `lib/` = Small cross-feature helper utilities.
- `types/` = Shared, cross-feature TypeScript interfaces (`ApiResponse`, `AuthUser`, `Role`).

## 3. API Calling Convention

**CRITICAL: Components must NOT call `fetch()` directly.**

**Flow:** Page → Feature Component → Feature Service → Shared `api.ts` → Backend

**Example (`features/admissions/services/admissionService.ts`):**

```ts
import { apiGet } from '@/services/api';
import type { AdmissionSummary } from '../types/admission';

export function getTrainerAdmissions() {
  return apiGet<AdmissionSummary[]>("/api/admissions?status=TRAINER_REVIEW");
}
```

**Rules:**
- No hardcoded `http://localhost:8080` in components.
- Rely on `NEXT_PUBLIC_API_URL` within `api.ts`.
- `api.ts` natively handles `credentials: "include"`.
- Do not manually read or store JWTs in `localStorage` or `sessionStorage`.

## 4. Page Convention

Next.js `page.tsx` files should be incredibly thin containers.

**Preferred:**
```tsx
export default function AdmissionsPage() {
  return (
    <RoleGuard allowedRoles={['HEAD_TRAINER', 'CLUB_MANAGER']}>
      <AppShell>
        <PageContainer>
          <TrainerAdmissionsScreen />
        </PageContainer>
      </AppShell>
    </RoleGuard>
  );
}
```

**Avoid:**
- Writing hundreds of lines of UI or direct API fetching in `page.tsx`.

## 5. Component Convention

**Shared UI (`components/ui/`):**
Only place generic primitives here if they contain NO RTMS business logic (e.g., `Button`, `Panel`, `StatusBadge`, `DataTable`).

**Feature UI (`features/<feature>/components/`):**
Place components here if they understand domain concepts (e.g., `Horse`, `HealthRecord`, `TrainingPlan`, `Stall`). Do NOT place these in `components/ui/`.

## 6. Type Convention

- **Feature types (`features/<feature>/types/`):** Types strictly for that domain (e.g., `AdmissionSummary`, `HealthRecordResponse`).
- **Global types (`src/types/`):** Strictly cross-cutting types (`ApiResponse`, `AuthUser`, `Role`).
- **Rule:** Use backend response field names exactly. Do not duplicate the same backend DTO in multiple folders.

## 7. Styling Convention

The Figma/reference design tokens are the visual source of truth.

- **Prefer** existing design tokens (`var(--color-...)`).
- **Prefer** shared UI components instead of reinventing them.
- **Do not** invent arbitrary colors or hardcode Figma hex values in feature components.
- **Preferred:** `bg-[var(--color-surface)] text-[var(--color-text-primary)] border-[var(--color-border)]`
- **Avoid:** `bg-white text-gray-900 border-gray-200` when a token exists.

## 8. State Handling Convention

Every data-driven screen MUST handle 4 states explicitly:
1. Loading state
2. Error state
3. Empty state
4. Success/Content state

Do NOT silently render a blank white page. Avoid adding Redux/Zustand until complexity explicitly demands it.

## 9. Form Convention

- Use controlled inputs for simple forms.
- Validation errors must be visible near the field.
- Disable submit buttons during requests.
- Prevent duplicate submissions.
- **Backend remains the final validator.** Do not duplicate complex backend rules in the frontend.

## 10. Role and Authorization Convention

- Frontend role checks (`RoleGuard`, `navigation.ts`) are for UX/Navigation only.
- The **Backend (@PreAuthorize)** is the authoritative source of truth for permissions.
- Frontend must NEVER assume hiding a button provides security.

## 11. Naming Convention

- **Components:** `PascalCase.tsx` (e.g., `AdmissionTable.tsx`)
- **Services:** `camelCaseService.ts` (e.g., `admissionService.ts`)
- **Types:** Descriptive `PascalCase` (e.g., `AdmissionSummary`)
- **Functions/Vars:** `camelCase`
- **Routes:** `lowercase` or `kebab-case` (e.g., `/preventive-care`)

## 12. Imports

Use the Next.js TypeScript path alias (`@/`) where appropriate.

**Preferred:**
`import { Button } from '@/components/ui/Button';`

**Avoid:**
`import { Button } from '../../../../components/ui/Button';`
