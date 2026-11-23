# Racehorse Training Management System (RTMS)

RTMS is a full-stack platform for managing racehorse admissions, training, medical care, stable operations, staff workflows, and analytics for a racing club.

The system covers the full lifecycle of a horse:

1. A horse owner registers an account and submits an admission application.
2. A quarantine groom reviews the application, reserves a quarantine stall, and confirms the horse's arrival.
3. A veterinarian performs the initial examination and records the horse's training eligibility.
4. A head trainer reviews the candidate and submits an assessment.
5. A club manager performs the final review and moves the horse into a regular stall.
6. Trainers, grooms, veterinarians, and managers continue to manage training sessions, care schedules, health records, stable assignments, and reports.

---

## Project documentation

- **Software Requirements Specification (SRS):** [RTMS Software Requirements Specification (PDF)](./docs/RTMS_Software_Requirements_Specification.pdf) — Complete project specification including domain models, business rules, and 32 system use cases.

---

## Repository layout

| Path | Description |
|---|---|
| `docs/` | Project documentation and Software Requirements Specification (SRS PDF) |
| `backend/` | Spring Boot REST API, domain services, JPA repositories, Flyway migrations, tests |
| `frontend/` | Next.js App Router application for all user roles |
| `ai-worker/` | Optional FastAPI service for document layout detection and OCR |
| `scripts/` | Development and database utilities |
| `backend/docs/` | Backend domain documentation |
| `frontend/FRONTEND_GUIDE.md` | Frontend architecture and coding guide |
| `backend/AGENTS.md` | Backend conventions |
| `frontend/AGENTS.md` | Frontend conventions |

---

## Technology stack

### Backend

| Component | Version / technology |
|---|---|
| Language | Java 21 |
| Framework | Spring Boot 4.1.1 |
| Web | Spring MVC |
| Persistence | Spring Data JPA, Hibernate 7.4.5 |
| Database | PostgreSQL 16+ with pgvector |
| Migrations | Flyway |
| Security | Spring Security, JWT, method-level authorities |
| API documentation | springdoc OpenAPI |
| Email | Spring Mail / SMTP |
| File storage | Local disk or Cloudinary |

### Frontend

| Component | Version / technology |
|---|---|
| Framework | Next.js 16.3.4, App Router |
| UI | React 19, TypeScript |
| Styling | Tailwind CSS v4, design tokens |
| Components | Shared primitives under `src/components/ui` |
| 3D | React Three Fiber / Drei |
| Tests | Node test runner, Playwright for selected flows |

### AI worker

| Component | Version / technology |
|---|---|
| Language | Python 3.11 |
| API | FastAPI, Uvicorn |
| OCR | PaddleOCR, EasyOCR |
| Layout detection | Ultralytics YOLO |
| PDF processing | PyMuPDF |

---

## Prerequisites

- JDK 21
- Node.js 22.6 or newer
- PostgreSQL 16 or newer
- pgvector extension
- Python 3.11 (only when running the AI worker)
- Git
- Windows PowerShell, WSL, or a Unix shell

---

## Backend setup

### 1. Prepare the database

Create the database and enable pgvector:

```sql
CREATE DATABASE rtms_db;
\c rtms_db
CREATE EXTENSION IF NOT EXISTS vector;
```

On Windows, `scripts/setup-pgvector-windows.ps1` can help install the extension.

Flyway migrations live in `backend/src/main/resources/db/migration/`. They run automatically when the backend starts. `spring.jpa.hibernate.ddl-auto` is set to `validate`, so the schema must match the entities.

### 2. Create local configuration

```powershell
cd backend
Copy-Item src/main/resources/application-local.properties.example src/main/resources/application-local.properties
```

Edit `application-local.properties` and set:

- PostgreSQL URL, username, and password
- AI provider base URL and API key
- SMTP username and app password
- Any optional Cloudinary configuration

`application-local.properties` is ignored by Git. Never commit real credentials.

### 3. Run the backend

Windows:

```powershell
.\mvnw.cmd spring-boot:run
```

Linux, macOS, or WSL:

```bash
./mvnw spring-boot:run
```

Default URLs:

| Resource | URL |
|---|---|
| REST API | `http://localhost:8080` |
| Swagger UI | `http://localhost:8080/swagger-ui/index.html` |
| OpenAPI JSON | `http://localhost:8080/v3/api-docs` |

### 4. Cloud profile

The `cloud` profile reads credentials from environment variables.

```powershell
cd backend
Copy-Item .env.cloud.example .env.cloud
.\run-cloud.ps1
```

Required variables include:

- `SPRING_PROFILES_ACTIVE=cloud`
- `DB_URL`
- `DB_USERNAME`
- `DB_PASSWORD`
- `TURNSTILE_SECRET_KEY`

`backend/.env.cloud` is ignored by Git.

---

## Frontend setup

### 1. Install dependencies

```powershell
cd frontend
npm install
```

### 2. Create `.env.local`

```env
NEXT_PUBLIC_API_URL=http://localhost:8080
NEXT_PUBLIC_TURNSTILE_SITE_KEY=1x00000000000000000000AA
```

The Turnstile value above is Cloudflare's always-pass test key for local development. Use a real site key for staging and production.

`NEXT_PUBLIC_*` variables are compiled into the frontend bundle at build time. Changing them requires a rebuild.

### 3. Run the frontend

```powershell
npm run dev
```

The application runs at `http://localhost:3000`.

### 4. Frontend commands

| Command | Purpose |
|---|---|
| `npm run dev` | Start the development server |
| `npm run build` | Build for production |
| `npm run lint` | Run ESLint |
| `npm run test` | Run the Node test suite |

---

## AI worker setup

The AI worker is optional. The backend calls it for document OCR when `rtms.ai.ocr.service-url` is configured.

### Local run

```powershell
cd ai-worker
python -m venv .venv
.venv\Scripts\activate
pip install -r requirements.txt
uvicorn app.main:app --host 0.0.0.0 --port 8000
```

Linux, macOS, or WSL:

```bash
cd ai-worker
python3 -m venv .venv
source .venv/bin/activate
pip install -r requirements.txt
uvicorn app.main:app --host 0.0.0.0 --port 8000
```

Health check:

```text
GET http://localhost:8000/health
```

### Docker

```bash
cd ai-worker
docker build -t rtms-ai-worker .
docker run --rm -p 8000:8000 rtms-ai-worker
```

---

## Configuration reference

| Variable | Scope | Purpose |
|---|---|---|
| `SPRING_PROFILES_ACTIVE` | Backend | Active Spring profile (`local` by default, `cloud` for cloud deployments) |
| `DB_URL` | Backend | JDBC URL for the cloud profile |
| `DB_USERNAME` | Backend | Database username |
| `DB_PASSWORD` | Backend | Database password |
| `JWT_SECRET` | Backend | JWT signing secret |
| `APP_HMAC_SECRET` | Backend | HMAC secret used by the AI tool layer |
| `CLOUDINARY_URL` | Backend | Optional Cloudinary connection string |
| `AI_API_KEY` | Backend | OpenAI-compatible provider key |
| `AI_BASE_URL` | Backend | OpenAI-compatible provider base URL |
| `TURNSTILE_SECRET_KEY` | Backend | Cloudflare Turnstile secret key |
| `RTMS_CAPTCHA_ENABLED` | Backend | Enables or disables Turnstile verification |
| `RTMS_AI_OCR_SERVICE_URL` | Backend | AI worker base URL |
| `NEXT_PUBLIC_API_URL` | Frontend | Backend API base URL |
| `NEXT_PUBLIC_TURNSTILE_SITE_KEY` | Frontend | Cloudflare Turnstile site key |
| `AI_WORKER_HOST` | AI worker | Bind host |
| `AI_WORKER_PORT` | AI worker | Bind port |
| `OCR_ENGINE` | AI worker | `paddle`, `easyocr`, or `auto` |
| `YOLO_MODEL_PATH` | AI worker | Model path for layout detection |

Secrets must not be committed. The repository ignores local configuration files such as `application-local.properties`, `.env.cloud`, and `.env.local`.

---

## Testing

### Backend

Unit tests:

```powershell
cd backend
.\mvnw.cmd test
```

Integration tests require a real PostgreSQL database. Use a clone of the development database, never the primary `rtms_db`:

```powershell
$env:SPRING_DATASOURCE_URL="jdbc:postgresql://localhost:5432/rtms_clone"
$env:SPRING_DATASOURCE_USERNAME="postgres"
$env:SPRING_DATASOURCE_PASSWORD="your-password"
.\mvnw.cmd test
```

### Frontend

```powershell
cd frontend
npm run lint
npm run test
npm run build
```

### AI worker

```powershell
cd ai-worker
python -m pytest
```

---

## Core backend modules

| Module | Responsibility |
|---|---|
| `identity` | Accounts, authentication, JWT, roles, permissions, staff management |
| `admission` | Owner applications, groom review, vet review, trainer review, manager approval, capacity policy |
| `horse` | Horse profiles, pedigree, status, photos, edit requests |
| `stable` | Areas, stalls, groom assignments, quarantine and regular capacity |
| `medical` | Care schedules, health records, injuries, prescriptions, preventive care |
| `training` | Courses, course runs, sessions, workouts, performance |
| `analytics` | EDA, SPC, survival analysis, manager dashboards |
| `ai` | AI orchestration, tool registry, RAG, conversation handling |
| `notification` | In-app notifications, email delivery, urgent alerts |
| `audit` | Domain audit events and HTTP audit logs |

See `backend/docs/ADMISSION_FLOW.md` for the complete admission call flow and the capacity rules.

---

## Coding conventions

### Backend

- One package per module, with layered subpackages: `controller`, `dto`, `entity`, `repository`, `service`, and `util` where appropriate.
- Services are plain Spring components. Do not introduce `...Impl` classes.
- Entities do not use JPA relations. Reference other rows by ID and load them through repositories.
- DTOs are Java records unless a mutable shape is required.
- Errors are thrown as `ApiException` with an uppercase error code.
- Comments are written in Vietnamese first, followed by the English translation.
- Schema changes are new Flyway migrations. Never edit an already-applied migration.

### Frontend

- Pages compose the shared layout and feature screens.
- Use shared primitives from `src/components/ui` instead of raw HTML controls.
- Use design tokens from `src/app/globals.css` instead of palette classes or hard-coded colours.
- All API calls go through `src/services/api.ts`.
- Backend authorization is authoritative; frontend role checks are for user experience only.

---

## Windows notes

- The repository stores text as LF and checks files out as CRLF on Windows.
- Use `.\mvnw.cmd` in PowerShell or Command Prompt, and `./mvnw` in WSL or Git Bash.
- Do not convert Java files to LF manually; Git handles line endings.
- Keep repositories and the Maven local repository on paths without spaces when possible.
- The frontend requires Node.js 22.6 or newer for `--experimental-strip-types`.

---

## Documentation

| Document | Contents |
|---|---|
| `backend/AGENTS.md` | Backend structure, API, comments, database, and test rules |
| `frontend/AGENTS.md` | Frontend agent rules, primitives, tokens, and verification |
| `frontend/FRONTEND_GUIDE.md` | Frontend architecture, routing, API integration, and styling |
| `frontend/DRAG_DROP.md` | Drag-and-drop primitives |
| `backend/docs/ADMISSION_FLOW.md` | Admission status flow, service calls, capacity policy |
| `backend/docs/STABLE_AREA_LIFECYCLE.md` | Stable area lifecycle |
| `backend/docs/CLOUD_DEMO_SEED.md` | Cloud demo data seeding |
| `backend/docs/HORSE_SEX.md` | Horse sex rules |

---

## Security

- Do not commit credentials, private keys, API keys, or SMTP passwords.
- Use environment variables or ignored local configuration files.
- Rotate any key that has been exposed in a commit, log, or chat message.
- Keep the Turnstile secret key on the backend only.
- Use HTTPS in staging and production.
- Restrict CORS origins to the deployed frontend domain.

---

## License

No license file is currently included in this repository. Confirm licensing terms with the project owner before distributing or reusing the code.
