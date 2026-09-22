# Frontend Handover Checklist

Please review this checklist before starting and before finishing any frontend task in this repository.

## Before starting a frontend task

- [ ] Pull latest code
- [ ] Read `FRONTEND_GUIDE.md`
- [ ] Identify the correct feature folder
- [ ] Confirm backend API/DTO exists
- [ ] Check Figma/UI reference
- [ ] Do not modify shared foundation files unnecessarily

## Before finishing a frontend task

- [ ] Uses real backend API (`services/api.ts`)
- [ ] No mock business data
- [ ] No direct `fetch` in feature component
- [ ] No hardcoded backend URL
- [ ] Uses shared UI/design tokens
- [ ] Loading state handled
- [ ] Error state handled
- [ ] Empty state handled where relevant
- [ ] Forms prevent duplicate submission
- [ ] Correct role manually tested
- [ ] Wrong role tested where relevant
- [ ] `npm run lint` passes
- [ ] `npm run build` passes

> **Note:** Remember that backend `@PreAuthorize` is the absolute source of truth for authorization. Frontend role logic exists purely for UX and navigation purposes.
