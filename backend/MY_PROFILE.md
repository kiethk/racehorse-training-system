# My Profile

Shared self-service profile for Manager, Owner, Trainer, Vet and Groom.

## UI

- Open the account menu at the bottom of the sidebar, then **My profile** (`/profile`).
- Edit full name, phone (up to 20 characters) and address. Email, role and professional credentials are read-only.
- Saving updates the editor and account menu from the returned profile without a second request to reload the session.
- Select a JPG/PNG up to 5 MB, preview it, then save. Removing a photo is also pending until Save; Cancel discards the draft.
- Click the avatar to choose a photo. Hover or keyboard focus reveals a dark lower-half camera overlay; touch devices keep it visible. `AvatarUpload` in `components/ui` is reusable and delegates validation, preview and persistence to its caller.
- The shared `Avatar` accepts `image` and `size="sm" | "md" | "lg"`. Existing calls without an image retain initials.
- Profile state is scoped by authenticated user ID; account switching cannot reuse another user's draft.

## API

- `GET /api/auth/me`: existing response with additive `phone`, `address`, `avatarUrl`, `createdAt` fields. Existing role-specific `profile` stays intact.
- `POST /api/auth/me/profile`: multipart `profile` JSON plus optional `avatar` image.

```json
{
  "fullName": "Daniel Foster",
  "phone": "0909001003",
  "address": "RTMS Demo Staff Residence 03, Greenfield",
  "removeAvatar": false
}
```

- `GET /api/auth/me/avatar`: authenticated image stream for the caller. Missing photo returns 404.
- The user ID always comes from the authenticated principal. Requests cannot write email, role, password, activation status, avatar URL or professional credentials.
- Avatar updates serialize on the User row. Original files survive rollback; replacement files are removed on rollback; old files are removed only after commit.
- Uses the existing `FileStorageService` for local/Cloudinary storage. Only decoded JPEG/PNG images up to 5 MB and 16 megapixels are accepted.
- `users.avatar_url` is nullable and stores the storage key. DTO `avatarUrl` is a protected self-service endpoint with a version query, never the raw storage key. This self-service URL is for the signed-in user's Profile/UserMenu; the staff directory contract is unchanged.

## Rollout

Migration `V202610171604__add_user_avatar_url.sql` adds one nullable column. Existing accounts require no backfill. Login fields are additive; existing constructor signatures and role-specific profiles are preserved.

Local validation used an isolated PostgreSQL database. The local seeded DB was backed up before restart/migration; 48 Horses, 54 Admissions, 30 Staff remain intact.

Set `CLOUDINARY_URL` through backend environment/config to upload avatars to Cloudinary using the existing private file storage service. The local profile loads the optional `.env.cloud` file for storage credentials; keep `SPRING_DATASOURCE_URL`, username and password explicitly pointed at local PostgreSQL. Do not override `rtms.storage.cloudinary-url` with an empty command-line value when testing Cloudinary. Existing `local:` files remain readable. Browser previews never upload until Save.

Neon seeding remains pending separate approval.

Cloudinary verification: saved and read a private PNG through the real local API, confirmed the local database held a Cloudinary authenticated asset key, then removed the test avatar. Browser checks covered all five roles at 1280/1440px, hover, keyboard selection, invalid files, preview, Cancel and Save. The existing Owner avatar was preserved byte-for-byte.
