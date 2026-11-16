# M02 — Relative-value re-run on WSL2 with subagents

**Mục tiêu:** chuyển 67 test case `TRN_TC01–TRN_TC67` từ giá trị/ngày/ID cố định sang **giá trị tương đối**, bỏ defect Copilot `UI-D08` khỏi báo cáo/Jira, rồi chạy lại toàn bộ trên **WSL2** bằng 3 subagent song song.

**Baseline hiện tại**

| Hạng mục | Giá trị |
|---|---|
| Test suite | 67 pure-UI case `TRN_TC01–TRN_TC67` + TC68 out of scope |
| Kết quả | 64 Pass / 3 Fail / 0 Blocked |
| Fail thật | TC53 (readiness validation), TC62 (incident image), TC63 (LOW incident routing) |
| Defect | 13 sau khi bỏ UI-D08 (UI-D09 automation-only, UI-D10 closed) |
| UI-D08 | **Removed by decision** — TC58/TC61 vẫn Pass; đóng/waive Jira `RTMS-48` |
| Repo | `D:\swp\racehorse-training-system`, branch `swt`/`develop` (code giống nhau) |

---

## 0. DB target — đọc trước

- **Target DB: `rtms_system_test`**, không phải `rtms_db`.
- `rtms_db` là DB gốc/main, đang lệch Flyway: 143 history rows, latest
  `202611121500` không có trong V*.sql hiện tại. **Không kết nối `rtms_db`.**
- Chỉ **PostgreSQL server** là dùng chung. Trên server có thể có `rtms_db`,
  `rtms_system_test`, `rtms_smoke`; agent chỉ được kết nối `rtms_system_test`.
- Trước khi seed/test, chạy `SELECT current_database();` — phải trả
  `rtms_system_test`. Seed SQL có guard `current_database()` và sẽ raise nếu chạy
  trên DB khác `rtms_system_test`/`rtms_smoke`.
- Nếu có yêu cầu dùng smoke DB, dùng tên **`rtms_smoke`** và set
  `SPRING_DATASOURCE_URL` tương ứng; không tự đổi DB.
- Backup `rtms_system_test` trước khi chạy seed/test vì test sẽ tạo/sửa dữ liệu.

## 1. Quyết định đã chốt

1. **UI-D08 Copilot overlay:** bỏ khỏi defect list, Jira export và test report. Không thêm bước kiểm tra Copilot vào TC58/TC61. TC58/TC61 giữ **Pass**. `RTMS-48` đóng với lý do *Waived — out of scope for this run*.
2. **Relative values:** mọi case dùng ngày/ID/số cụ thể phải chuyển sang:
   - Ngày: `today`, `yesterday`, `today + N`, `today + horizon`, `today + horizon + 1`.
   - ID: lấy từ UI (search/first row) hoặc từ fixture SQL trước khi chạy; không hardcode `#37`, `#46`, `#49`, `#50`.
   - Số/ngưỡng: `min`, `max`, `min - 1`, `max + 1`, `max + 2`, `count before + 1`, `count before - 1`.
   - Tên: `M02_<case>_<RUN_ID>`; `RUN_ID` lấy từ timestamp.
3. **Không sửa code app** trong lúc test. Nếu fail, log defect + screenshot; không fix để “cho Pass”.
4. **UI-only:** test script không gọi API, không ghi DB. Chỉ chạy `M02_test_data_seed.sql` trước khi test để chuẩn bị fixture.

---

## 2. Kiến trúc chạy trên WSL2

### 2.1. Cài WSL2 + Ubuntu

Trên Windows PowerShell (Admin):

```powershell
wsl --install -d Ubuntu-24.04
wsl --set-default-version 2
```

Mở Ubuntu, tạo user, rồi:

```bash
sudo apt update
sudo apt install -y openjdk-21-jdk postgresql postgresql-contrib curl git unzip
curl -fsSL https://deb.nodesource.com/setup_22.x | sudo -E bash -
sudo apt install -y nodejs
node -v    # phải là v22.x hoặc v24.x, KHÔNG dùng v26
npm -v
java -version
```

> **Quan trọng:** dùng Node 22/24. Node 26 không hỗ trợ `--experimental-transform-types` và làm `npm test` fail.

### 2.2. Lấy repo

Khuyến nghị copy vào filesystem WSL (nhanh hơn `/mnt/d`):

```bash
mkdir -p ~/work && cd ~/work
git clone <repo-url> rtms
cd rtms
git checkout swt
```

Nếu chỉ có repo trên Windows:

```bash
cp -r /mnt/d/swp/racehorse-training-system ~/work/rtms
cd ~/work/rtms
```

### 2.3. Database — chọn 1 trong 2 cách

**Cách A — dùng PostgreSQL trên Windows (khuyến nghị, giữ nguyên seed hiện có):**

1. Trên Windows, cho PostgreSQL listen ra ngoài:
   - `postgresql.conf`: `listen_addresses = '*'`
   - `pg_hba.conf`: thêm `host all all 172.16.0.0/12 scram-sha-256` (hoặc subnet WSL của bạn)
   - Restart PostgreSQL service.
2. Trong WSL lấy IP Windows:

```bash
WIN_HOST=$(ip route show default | awk '{print $3}')
echo $WIN_HOST
pg_isready -h "$WIN_HOST" -p 5432
```

3. Set env khi chạy backend:

```bash
export SPRING_DATASOURCE_URL="jdbc:postgresql://$WIN_HOST:5432/rtms_system_test"
export SPRING_DATASOURCE_USERNAME=postgres
export SPRING_DATASOURCE_PASSWORD=1
```

**Cách B — PostgreSQL trong WSL:**

```bash
sudo service postgresql start
sudo -u postgres createuser -s "$USER"
createdb rtms_system_test
```

Sau đó dump DB từ Windows (nếu muốn giữ nguyên seed):

```bash
# Trên Windows hoặc WSL trỏ tới Windows host
pg_dump -h "$WIN_HOST" -U postgres rtms_system_test > /tmp/rtms_system_test.sql
psql -d rtms_system_test -f /tmp/rtms_system_test.sql
```

Nếu tạo DB mới hoàn toàn: chạy official demo seed trước, sau đó chạy `M02_test_data_seed.sql`.

### 2.4. Chạy app

Backend:

```bash
cd ~/work/rtms/backend
export SPRING_DATASOURCE_URL="jdbc:postgresql://$WIN_HOST:5432/rtms_system_test"
export SPRING_DATASOURCE_USERNAME=postgres
export SPRING_DATASOURCE_PASSWORD=1
./mvnw spring-boot:run
```

Frontend (terminal khác):

```bash
cd ~/work/rtms/frontend
npm ci
npm run dev -- --hostname 0.0.0.0
```

Kiểm tra:

```bash
curl -s -o /dev/null -w "frontend=%{http_code}\n" http://localhost:3000/login
curl -s -o /dev/null -w "backend=%{http_code}\n" http://localhost:8080/api/training-rules
```

`backend=401/403` là bình thường (chưa login).

### 2.5. Playwright

```bash
cd ~/work/rtms/frontend
npx playwright install --with-deps chromium
```

Config phải trỏ `baseURL`:

```bash
export PLAYWRIGHT_BASE_URL=http://localhost:3000
```

---

## 3. Seed data trước khi test

```bash
cd ~/work/rtms
psql -h "$WIN_HOST" -U postgres -d rtms_system_test \
  -f docs/bmad/testing/evidence/fixtures/M02_test_data_seed.sql
```

Kiểm tra:

```sql
SELECT a.id, h.name, a.status, a.trainer_id
FROM admission_applications a
JOIN horses h ON h.id = a.horse_id
WHERE h.name IN ('EROIKA','ARC KAABA','M02_ADM_VALIDATION','M02_ADM_FOREIGN')
ORDER BY h.name;

SELECT h.name, ss.stall_code, ss.groom_id, h.training_decision
FROM horses h
LEFT JOIN stable_stalls ss ON h.current_stall_id = ss.id
WHERE h.name LIKE 'M02FIX_%'
ORDER BY h.name;
```

Ghi lại ID của `M02_ADM_VALIDATION` và `M02_ADM_FOREIGN` vào file:

```bash
cat > /tmp/m02-fixtures.env <<'EOF'
M02_VALIDATION_ID=<id>
M02_FOREIGN_ID=<id>
EOF
```

Agent C dùng các ID này; test script không query DB.

---

## 4. Quy tắc giá trị tương đối

### 4.1. Token ngày

```bash
TODAY=$(date +%F)
YESTERDAY=$(date -d 'yesterday' +%F)
TOMORROW=$(date -d 'tomorrow' +%F)
T_PLUS_365=$(date -d '+365 days' +%F)
T_PLUS_366=$(date -d '+366 days' +%F)
```

Trong Playwright/JS:

```js
const today = new Date();
const addDays = (d, n) => { const x = new Date(d); x.setDate(x.getDate() + n); return x; };
const iso = (d) => d.toISOString().slice(0, 10);
```

### 4.2. Hằng số rules (không magic number)

```js
const RULES = {
  exerciseDurationMin: 15,
  exerciseDurationMax: 240,
  exerciseDistanceMin: 100,
  exerciseDistanceMax: 10000,
  courseSessionsMin: 1,
  courseSessionsMax: 60,
  courseExercisesMax: 30,
  groupCapacity: 6,
  groupNameMax: 100,
  feedbackMax: 2000,
  hrMin: 30,
  hrMax: 240,
  monthsMin: 0,
  monthsMax: 60,
  fileMaxMB: 10,
  pageSize: 10,
};
```

Nếu app đổi rules, chỉ sửa `RULES`; không sửa từng test case.

### 4.3. Bảng chuyển giá trị cụ thể → tương đối

| Case | Giá trị cũ | Giá trị tương đối |
|---|---|---|
| TC04 | 14 / 241 / 99 / 10001 / 15 / 240 / 100 / 10000 | `durationMin-1`, `durationMax+1`, `distanceMin-1`, `distanceMax+1`, `durationMin`, `durationMax`, `distanceMin`, `distanceMax` |
| TC07 | 0 / 1 / 60 / 61 | `sessionsMin-1`, `sessionsMin`, `sessionsMax`, `sessionsMax+1` |
| TC09 | 30 / 31 | `courseExercisesMax`, `courseExercisesMax+1` |
| TC10 | 6 exercises / 5 sessions | `sessions+1` exercises; duplicate; identical rotation |
| TC15 | 100 / 101 ký tự | `groupNameMax`, `groupNameMax+1` |
| TC20 | 6 / 7 horses | `groupCapacity`, `groupCapacity+1` |
| TC26 | start date cụ thể | `today + 1..7`; chọn weekdays; preview = số session của course |
| TC27 | 2026-11-10 | `yesterday` |
| TC29 | 2026-11-18 | `existingRun.startDate + 1 day` |
| TC31 | 2027-11-11 / 12 | `today + horizon`, `today + horizon + 1` |
| TC33 | 2026-11-12 / 15 | `today`, ngày trống kế tiếp |
| TC35 | +15 phút | đúng 1 bước 15 phút |
| TC36 | 07:10 / right edge | giờ lệch quarter-hour / cuối training window |
| TC38 | 2026-11-12 / 18 | `today`, `yesterday`, session cancelled |
| TC42 | 800/30/55/48/150/200/80 | readings hợp lệ trong rules; **không hardcode rating**; ghi rating hiển thị |
| TC44 | 30/240/29/241/200/150 | `hrMin`, `hrMax`, `hrMin-1`, `hrMax+1`, `avg > max` |
| TC45 | 2001 ký tự; 600/20/40/55... | `feedbackMax+1`; valid values; `avg > top`; `distance=0` |
| TC46 | 20 / 190 | đổi một giá trị hợp lệ bất kỳ; so rating trước/sau |
| TC49 | 2 pending / 19 reviewed | đọc count từ UI; assert tab/filter theo count hiện tại |
| TC50–TC54 | #49/#50, 8/7/9, 6/61/60 | “a TRAINER_REVIEW admission assigned to trainer1”; scores trong 0–10; `monthsMax`, `monthsMax+1` |
| TC55 | #46 / M02_ADM_FOREIGN | “an admission assigned to another trainer”; ID lấy từ `M02_FOREIGN_ID` |
| TC57 | 18/11/3/6.4 | KPI phải khớp table rows; không hardcode |
| TC58 | 18 horses / page 2 | `count > pageSize` → page 2 tồn tại; search registration của row đầu |
| TC60 | #37 ENTULA | “a horse in another trainer’s area”; ID lấy từ fixture/seed |
| TC62 | FC NAJAA | “an incident with an attachment” |
| TC63 | FELICITY #18 | “a horse in trainer’s area with no open incident” |
| TC64 | 11 MB / 10 MB | `fileMaxMB+1` / `fileMaxMB` |

Các case không có giá trị cụ thể (TC01, 02, 03, 05, 06, 08, 11, 12, 13, 14, 16, 17, 18, 19, 21, 22, 23, 24, 25, 28, 30, 32, 34, 37, 39, 40, 41, 43, 47, 48, 56, 59, 61) giữ nguyên intent, chỉ đổi tên/ID nếu có.

---

## 5. Chia agent

| Agent | Cases | Account chính | Data writes | Evidence |
|---|---|---|---|---|
| **A — Course Library + Groups** | TC01–TC24 | `trainer1@rtms.com` (course), `trainer3@rtms.com` (groups fixture) | Courses, exercises, groups | `evidence/relative/agent-a/` |
| **B — Runs + Schedule + Workouts** | TC25–TC48 | `trainer2@rtms.com` | Course runs, sessions, workouts | `evidence/relative/agent-b/` |
| **C — Admissions + Dashboard + Incidents + Security** | TC49–TC67 | `trainer1@rtms.com`; `trainer2` for foreign data | Admissions assessments, incidents | `evidence/relative/agent-c/` |
| **Orchestrator** | Merge + Excel + defect list | — | Không test | `evidence/relative/` |

### 5.1. Shared DB — đã chốt

- Dùng **chung 1 DB `rtms_system_test` và 1 app**; không tạo 3 DB riêng.
- A/B/C chạy song song; data domains đã tách:
  - A ghi course/exercise/group.
  - B ghi run/session/workout.
  - C ghi admission assessment/incident.
- A và C cùng trainer1 nhưng khác domain; C chạy **TC56/TC57 trước TC63/TC64** để incident không làm đổi alert count.
- B dùng trainer2; C đọc trainer2 cho TC55/TC60 nhưng không ghi.
- Nếu thấy drift (count/row bị đổi): **stop agent bị ảnh hưởng → chạy lại seed SQL → chạy lại agent đó**. Không tạo DB riêng, không reset DB khi agent khác đang chạy.

---

## 6. Prompt cho agent

Prompt copy-paste nằm trong file:

```text
docs/bmad/testing/M02-relative-agent-prompts.md
```

Gồm:
- `MASTER_ORCHESTRATOR`
- `AGENT_A_COURSE_GROUPS`
- `AGENT_B_RUNS_SCHEDULE_WORKOUTS`
- `AGENT_C_ADMISSIONS_DASHBOARD_INCIDENTS_SECURITY`
- `RECONCILER`
- `FAILURE_TRIAGE`

---

## 7. Quy trình chạy

### Phase 0 — Preflight

```bash
cd ~/work/rtms
git status
node -v
java -version
psql -h "$WIN_HOST" -U postgres -d rtms_system_test -c "select count(*) from users;"
curl -s -o /dev/null -w "%{http_code}\n" http://localhost:3000/login
```

- Node 22/24, Java 21.
- DB có official seed + M02 fixtures.
- Frontend/backend up.
- Playwright Chromium installed.

### Phase 1 — Reset fixture

```bash
psql -h "$WIN_HOST" -U postgres -d rtms_system_test \
  -f docs/bmad/testing/evidence/fixtures/M02_test_data_seed.sql
```

Ghi `M02_VALIDATION_ID`, `M02_FOREIGN_ID`.

### Phase 2 — Start app

Backend terminal + frontend terminal như mục 2.4.

### Phase 3 — Chạy 3 agent

Mỗi agent một terminal/session riêng, cùng `RUN_ID`:

```bash
export RUN_ID=$(date +%Y%m%d%H%M)
export PLAYWRIGHT_BASE_URL=http://localhost:3000
```

Agent A:

```bash
cd ~/work/rtms/frontend
npx playwright test tests/m02/agent-a.spec.js \
  --reporter=json,line --workers=1 \
  --output=../docs/bmad/testing/evidence/relative/agent-a
```

Agent B/C tương tự.

### Phase 4 — Reconcile

Orchestrator đọc 3 JSON, kiểm tra:
- Đủ 67 case, không trùng, không thiếu.
- Kết quả: 64 Pass / 3 Fail (hoặc cập nhật nếu fail mới).
- Mỗi case có screenshot.
- Không có hardcoded date/ID trong spec (grep).
- UI-D08 không xuất hiện.

### Phase 5 — Cập nhật Excel

Tạo/cập nhật:
- `SWT301_Test Case - M02 UI - Relative.xlsx`
  - Sheet `M02_Head_Trainer`: 67 case, steps relative, result từ JSON, note + evidence.
  - Sheet `Case Audit`: 67 pure UI, ghi chú relative.
  - Sheet `Out of Scope`: TC68.
- `M02_Test_Input_Data_Relative.xlsx`: input values relative + token guide.
- `M02-head-trainer-test-plan-relative.md`: plan 67 case relative.

### Phase 6 — Defect list

- 13 defect (bỏ UI-D08).
- TC53/TC62/TC63 vẫn Fail.
- UI-D09 automation-only, UI-D10 closed.
- Nếu agent tìm defect mới: thêm vào `M02-head-trainer-defect-list.md` + Jira-ready CSV; không sửa code.

---

## 8. Evidence & format

### 8.1. Screenshot

```text
docs/bmad/testing/evidence/relative/agent-a/TRN_TC01_course-library.png
docs/bmad/testing/evidence/relative/agent-b/TRN_TC26_preview.png
docs/bmad/testing/evidence/relative/agent-c/TRN_TC53_validation.png
```

Không dùng wildcard trong Jira; mỗi defect phải có tên file chính xác.

### 8.2. JSON result

Mỗi agent xuất:

```json
{
  "agent": "A",
  "runId": "202611130930",
  "cases": [
    {
      "case": "TRN_TC01",
      "status": "Pass",
      "evidence": ["TRN_TC01_course-library.png"],
      "notes": "relative: viewport, no data input"
    }
  ],
  "summary": { "total": 24, "pass": 24, "fail": 0, "blocked": 0 }
}
```

### 8.3. Markdown report

Mỗi agent tạo `agent-a-report.md`:
- Môi trường, commit, RUN_ID.
- Bảng case / status / evidence / note.
- Fail + defect nếu có.
- Không claim Pass nếu không có screenshot.

---

## 9. Acceptance criteria

- [ ] 67/67 case executed trên WSL2.
- [ ] 64 Pass / 3 Fail / 0 Blocked (hoặc lý do rõ nếu khác).
- [ ] Không còn hardcoded date/ID/magic number trong steps.
- [ ] Mỗi case có ít nhất 1 screenshot.
- [ ] TC58/TC61 Pass; UI-D08 không còn trong báo cáo/Jira.
- [ ] 13 defect: 1 High (UI-D01), 6 Medium, 5 Low, 1 Cosmetic (sau khi bỏ UI-D08).
- [ ] TC53/TC62/TC63 vẫn Fail.
- [ ] Test script không gọi API/ghi DB; chỉ seed SQL.
- [ ] Excel + plan + evidence đóng gói được.

---

## 10. Troubleshooting WSL2

| Vấn đề | Cách xử lý |
|---|---|
| `localhost:3000` không mở từ Windows | WSL2 thường forward; thử `http://<wsl-ip>:3000`; chạy frontend `--hostname 0.0.0.0` |
| Backend không kết nối DB | Kiểm tra `WIN_HOST`, `pg_isready`, `pg_hba.conf`, firewall Windows |
| Playwright thiếu browser | `npx playwright install --with-deps chromium` |
| `--experimental-transform-types` fail | Dùng Node 22/24, không dùng Node 26 |
| Port 3000/8080 bận | `ss -ltnp | grep 3000`; kill process cũ |
| Agent conflict data | Stop agent bị ảnh hưởng, chạy lại seed SQL, chạy lại agent đó; không tạo DB riêng vì đã chốt dùng chung DB |
| Screenshot folder lẫn | Mỗi agent `--output` riêng |
| Node modules lỗi | `rm -rf node_modules package-lock.json && npm install` (chỉ khi cần) |

---

## 11. Handoff message

Copy nguyên block này cho agent/orchestrator mới:

```text
You are the M02 Head Trainer test orchestrator. Work in WSL2 at ~/work/rtms.
Read docs/bmad/testing/M02-relative-wsl2-agent-plan.md and
docs/bmad/testing/M02-relative-agent-prompts.md first.

Goals:
1. Rewrite the 67 TRN_TC cases to relative dates/IDs/boundaries.
2. Remove UI-D08 (Copilot overlay) from the report/Jira; 13 defects remain.
3. Run 3 subagents (A: TC01-24, B: TC25-48, C: TC49-67) against the UI.
4. Produce JSON reports, screenshots, updated Excel/plan, defect list.

Hard rules:
- UI-only; no API calls or DB writes by test scripts. Only run M02_test_data_seed.sql.
- No hardcoded dates, IDs, counts or magic numbers in steps.
- Every Pass needs a screenshot.
- Do not fix code; log defects.
- TC53/TC62/TC63 must remain Fail.
- TC58/TC61 stay Pass; do not test the Copilot button.

Start with preflight, seed, app health, then launch A/B/C. Use the prompts in
M02-relative-agent-prompts.md exactly. Reconcile at the end and report counts.
```
