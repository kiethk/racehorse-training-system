# Shared horse detail

The common detail is composed from an identity banner, the Admissions-style tab strip,
and independent Overview cards. No component selects workflows from a role string.

## Use in an actor screen

`HorseProfileView` loads the horse and racing specialty once. Overview is always first.
Existing callers can keep passing an array of `extraTabs`. Client actor wrappers can
instead use a callback to reuse the loaded horse and specialty:

```tsx
'use client';

import { HorseProfileView } from '@/features/horses/shared/components/HorseProfileView';

// Import the actor's existing feature panels in the actual screen.
<HorseProfileView
  horseId={horseId}
  returnTo="/manager/horses"
  extraTabs={({ horse }) => [
    { id: 'operations', label: 'Operations', content: <OperationsPanel horseId={horse.id} /> },
  ]}
  overviewContent={({ horse }) => <AssignmentSummary horseId={horse.id} />}
/>
```

`OperationsPanel` and `AssignmentSummary` represent caller-owned feature components.
Callbacks must be supplied from a client component, not a server page.

- `overview` is reserved. Give additional tabs unique IDs.
- Only the active tab content is mounted; tab-local state resets when switching away.
- `headerActions` supplies actor actions; `sidebar` supplies an optional sticky column.
- `overviewContent` adds actor context below the shared cards (e.g. resolved stall and care team).
- Supply only permitted content; backend endpoints still enforce access.
- Trainer may add training and fitness; Owner pedigree and health; Groom care and tasks;
  Vet medical records; Manager operational reviews. These tabs are not auto-created.
- Existing Trainer, Owner, Groom and Admission screens are unchanged by this redesign.

## Compose without the shared loader

Screens already loading their own data can use `HorseDetailLayout`, `HorseProfileHeader`,
`HorseDetailTabs`, and `HorseOverview` directly. Pass the same horse and specialty to
these blocks rather than mounting a second loader.

`HorseInfoSection` provides consistent icon/title/description/aside slots.
`HorseTrainingClearance` displays the backend veterinary decision without calculating
training eligibility. `HorseSpecialtySection` displays backend specialties and rated
workout evidence; filling its evidence bar does not itself award a specialty.

The Overview grid responds to its available container width, including when an actor
supplies a sidebar. The optional location note is the existing `stableLocation` text,
not a resolved stall assignment. An actor should supply actual assignment details via
`overviewContent`; the common detail does not request privileged staff/stable lists.

## Photos

`Horse.imageUrl` points to the protected photo endpoint. `HorseProfilePhoto` loads it
with the existing authenticated helper and shows a skeleton while loading, **No photo**
when absent, and **Photo unavailable** on a failed request. It never substitutes another
horse's photo. Small avatars and the Trainer avatar appearance remain unchanged.

The source is the `HORSE_PHOTO` document linked through `AdmissionApplication.horseId`.
A missing file needs restoration from the original upload. A document from an unlinked
admission cannot serve as another horse's photo.
