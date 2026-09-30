# RTMS Frontend Development Guide

Welcome to the Riverside Training & Management System (RTMS) frontend codebase. This guide serves as the official handover document for the frontend development team, detailing the architecture, conventions, and how to work within the repository.

---

## 1. Frontend Overview

**Technology Stack:**
- **Framework:** Next.js 16 (App Router)
- **Library:** React 19
- **Language:** TypeScript
- **Styling:** Tailwind CSS v4 (using CSS variables/design tokens)
- **Backend:** Spring Boot (REST API)
- **Auth:** HttpOnly cookie-based JWT

**Core Philosophy:**
- **Next.js** handles UI layout, client-side routing, and rendering.
- **Spring Boot** is the single source of truth for business rules, authorization, and data integrity.
- **UI Reference (`ui-reference/`)** is the source of truth for visual design and layout, but *not* for business logic, mock data, or architecture.

---

## 2. How to Run the Frontend

1. Ensure the Spring Boot backend is running locally on port 8080.
2. Navigate to the frontend directory:

```bash
cd frontend
npm install
npm run dev
```

3. Open your browser to the default URL: `http://localhost:3000`

**Environment Variables:**
Ensure you have a `.env.local` file in the root of the `frontend/` directory with the following expected variable:

```env
NEXT_PUBLIC_API_URL=http://localhost:8080
```

---

## 3. Authentication Flow

Authentication relies on an HttpOnly cookie (`jwt_token`) managed entirely by the Spring Boot backend. 

1. **Login:** The user submits credentials to `POST /api/auth/login`. The backend sets the `jwt_token` cookie.
2. **Session Restore:** On app mount, `AuthContext` calls `GET /api/auth/me` to retrieve the user's profile using the cookie.
3. **Logout:** Calling `POST /api/auth/logout` clears the cookie on the backend, and the frontend clears its in-memory user state.

**Important Restrictions:**
- Do **NOT** read or parse the JWT manually on the client.
- Do **NOT** store authentication state in `localStorage` or `sessionStorage`.

---

## 4. Role Routing

The `RoleGuard` intercepts unauthenticated users and redirects them to `/login`. It also ensures users can only access their assigned role landing pages:
- `HORSE_OWNER` → `/owner`
- `GROOM` → `/groom`
- `VETERINARIAN` → `/veterinarian`
- `HEAD_TRAINER` → `/trainer`
- `CLUB_MANAGER` → `/manager`

---

## 5. Application Shell

The `AppShell` component wraps all authenticated pages. It renders the `TopNav` (which displays role-specific navigation items from `config/navigation.ts`) and provides a flexible container for page content. Page contents should be wrapped in the `PageContainer` component for consistent max-width and padding.

---

## 6. Project Structure

The `frontend/src/` directory is strictly organized by responsibility:

- `app/`: Next.js App Router definitions. Used exclusively for route definition and page composition.
- `features/`: Contains business feature modules (e.g., `admissions`, `horses`).
- `components/layout/`: Global layout structures (`AppShell`, `TopNav`, `PageContainer`).
- `components/ui/`: Pure, reusable, business-agnostic visual components (`Button`, `Panel`, `Icon`).
- `components/auth/`: Components related to auth states and routing (`RoleGuard`, `RoleLanding`).
- `services/`: Shared infrastructure, including the core `api.ts` transport layer and global auth service.
- `context/`: React context providers (e.g., `AuthContext`).
- `config/`: Shared configuration files (e.g., `navigation.ts`).
- `types/`: Global, cross-cutting TypeScript definitions (`ApiResponse`, `AuthUser`, `Role`).
- `lib/`: Small utility functions and helpers (`roleRoute.ts`).

---

## 7. Feature Development Pattern

When building a new business feature, work within its dedicated directory in `src/features/`.

Example (`src/features/admissions/`):
- `components/`: Domain-aware UI (e.g., `AdmissionTable.tsx`). Do not put generic UI here, and do not put domain UI in `src/components/ui/`.
- `services/`: Feature-specific API wrappers (`admissionService.ts`).
- `types/`: DTOs matching the backend exactly (`AdmissionSummary`).

---

## 8. API Calling Convention

**CRITICAL: Components must NOT call `fetch()` directly.**

All network requests must route through `services/api.ts` (using `apiGet`, `apiPost`, etc.), which automatically attaches the `credentials: "include"` flag required for cookie-based auth.

**Correct Flow:**
`Page Component` → `Feature Component` → `Feature Service` → `services/api.ts` → `Spring Boot Backend`

**Example (Feature Service):**
```typescript
import { apiGet } from '@/services/api';
import type { AdmissionSummary } from '../types/admission';

export function getTrainerAdmissions() {
  return apiGet<AdmissionSummary[]>('/api/admissions?status=TRAINER_REVIEW');
}
```

---

## 9. Page Convention

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

---

## 10. UI Component Rules

**Shared UI (`components/ui/`):**
Only place generic primitives here if they contain NO RTMS business logic (e.g., `Button`, `Panel`, `StatusBadge`, `DataTable`).

**Feature UI (`features/<feature>/components/`):**
Place components here if they understand domain concepts (e.g., `Horse`, `HealthRecord`). Do NOT place these in `components/ui/`.

---

## 11. Figma / Styling Rules

The Figma/reference design tokens defined in `src/app/globals.css` are the visual source of truth.

- **Prefer existing tokens:** Always use CSS variables (e.g., `bg-[var(--color-surface)]`, `text-[var(--color-text-primary)]`) over hardcoded Tailwind utility colors (`bg-white`, `text-gray-900`).
- **Reuse UI Components:** Rely on standard components (`Panel`, `Button`) to ensure consistent border radius, shadows, and padding.
- **No inline hex codes:** Do not extract arbitrary hex codes from Figma. If a color exists, there is a token for it.

---

## 12. Loading / Error / Empty / Success States

For any data-driven screen, always account for four states explicitly:
1. **Loading** (Use lightweight spinners or skeletons)
2. **Error** (Show inline error messages, do not crash the app)
3. **Empty** (Explain why no data is present)
4. **Success/Content**

Do not silently render blank screens on failure. Reuse components to display these states cleanly.

---

## 13. Form Rules

- Use controlled inputs for simple forms.
- Validation errors must be visible near the relevant field or form.
- Disable submit buttons during requests.
- Prevent duplicate submissions.
- **Backend remains the final validator.** Do not duplicate complex backend business rules in frontend validation.

---

## 14. Naming Rules

- **React Components:** `PascalCase.tsx` (e.g., `AdmissionTable.tsx`)
- **Services:** `camelCaseService.ts` (e.g., `admissionService.ts`)
- **Types:** Descriptive `PascalCase` (e.g., `AdmissionSummary`)
- **Functions/Variables:** `camelCase`
- **Route folders:** `lowercase` or `kebab-case` if multiple words

---

## 15. Security Rules

- Frontend role checks (`RoleGuard`, `navigation.ts`) exist for **UX and navigation only**.
- **Backend `@PreAuthorize` rules enforce true security.** The backend is authoritative.
- Frontend must NEVER assume hiding a button provides security.

---

## 16. Git / Team Workflow

- Before starting a task, pull the latest code and review this guide.
- Check the Figma/UI reference for layout and styles.
- Identify the correct feature folder for your task.
- Confirm backend API/DTOs exist before building frontend services.
- Do not modify shared foundation files unnecessarily.

---

## 17. Definition of Done — Frontend Feature

- Uses real backend API (`api.ts`).
- No mock business data.
- No direct `fetch` in feature components.
- Uses shared UI/design tokens.
- Loading, Error, and Empty states handled.
- Forms prevent duplicate submission.
- Manual testing passed for correct and wrong roles.
- `npm run lint` passes.
- `npm run build` passes.

---

## 18. Example: How to Start a New Feature

1. Identify the feature domain (e.g., `horses`).
2. Create feature types in `src/features/horses/types/horse.ts` matching the backend DTOs.
3. Create feature service in `src/features/horses/services/horseService.ts`.
4. Create domain UI in `src/features/horses/components/HorseList.tsx`.
5. Create the Next.js page in `src/app/horses/page.tsx`, import the component, and wrap it in `RoleGuard`, `AppShell`, and `PageContainer`.

---

## 19. Common Mistakes to Avoid

- Storing the JWT in `localStorage`.
- Hardcoding `http://localhost:8080` in components (use `NEXT_PUBLIC_API_URL`).
- Creating feature-specific UI (like a `HorseCard`) in `src/components/ui/`.
- Doing API calls directly in `page.tsx` instead of using a feature service.
- Silently failing when an API request returns an error.

---

## 20. Current Foundation Status

- **Auth:** Complete (Cookie-based JWT, RoleGuard).
- **Layout:** Complete (AppShell, TopNav, PageContainer).
- **Design Tokens:** Complete (Tailwind v4 with UI reference tokens).
- **Feature Structure:** Established with empty directories ready for implementation (`admissions/`, `horses/`, `health/`, etc.).
