# M02 — Checklist chuyển sang agent mới

## A. Chuẩn bị trên máy (bạn làm)

- [ ] **WSL2 Ubuntu 24.04** đã cài; mở được terminal Ubuntu.
- [ ] **Java 21** và **Node 22/24** (không dùng Node 26).
- [ ] **PostgreSQL** chạy; DB `rtms_system_test` đã có official demo seed.
- [ ] **DB target: `rtms_system_test`** — không phải `rtms_db`.
- [ ] `rtms_db` là DB gốc/main, đang lệch Flyway (143 rows, latest `202611121500`
  không có trong V*.sql) — **không kết nối**.
- [ ] Chỉ PostgreSQL server là dùng chung; các DB khác trên server phải bỏ qua.
- [ ] Chạy `SELECT current_database();` → phải trả `rtms_system_test`.
  Seed SQL cũng có guard `current_database()` và sẽ raise nếu chạy trên DB khác.
- [ ] Nếu có dùng smoke DB, ghi rõ tên `rtms_smoke` và set `SPRING_DATASOURCE_URL`;
  không tự đổi DB.
- [ ] Backup `rtms_system_test` trước khi seed/test.

- [ ] **Playwright Chromium** đã cài trong WSL:
  `npx playwright install --with-deps chromium`.
- [ ] Repo đã clone/copy vào WSL, ví dụ `~/work/rtms`; checkout branch `swt`.
- [ ] `frontend/npm ci` xong; `backend/mvnw -q -DskipTests package` xong.
- [ ] Chạy `M02_test_data_seed.sql` trên DB; lưu lại:
  - `M02_VALIDATION_ID`
  - `M02_FOREIGN_ID`
- [ ] Start backend `:8080`, frontend `:3000`; kiểm tra:
  - `http://localhost:3000/login` → 200
  - `http://localhost:8080/api/training-rules` → 401/403 là bình thường.
- [ ] Trên Jira: **close/waive `RTMS-48`** (UI-D08 Copilot overlay). Còn **13 defect**.
- [ ] Xác nhận: TC53/TC62/TC63 vẫn **Fail**; TC58/TC61 **Pass**; UI-D08 không test.

## B. File cần đưa cho agent

- [ ] `D:\Download\M02_Relative_Handoff.zip`
  - `plan/M02-relative-wsl2-agent-plan.md`
  - `plan/M02-relative-agent-prompts.md`
  - `data/*.xlsx`
  - `seed/M02_test_data_seed.sql`
  - `reports/*.md`
  - `text/*.txt`
- [ ] Repo path + branch: `~/work/rtms`, branch `swt`.
- [ ] DB info: host, port, dbname, user, password.
- [ ] `RUN_ID` (hoặc để agent tự tạo).
- [ ] Quyết định chạy: **1 DB + 1 app + 3 agent** (mặc định).

## C. Mở agent mới

1. Tạo session mới **trong repo** `~/work/rtms` (hoặc `D:\swp\racehorse-training-system` nếu agent chạy Windows).
2. Giải nén/attach `M02_Relative_Handoff.zip`.
3. Paste prompt master (bên dưới) + cho agent đọc 2 file plan/prompts.
4. Cấp quyền chạy shell/Playwright; không cấp quyền sửa code nếu không cần.
5. Nếu agent hỏi DB host, đưa IP Windows host hoặc `localhost` nếu Postgres trong WSL.

## D. Agent sẽ làm

- Preflight: Node/Java/DB/app/Playwright.
- Seed `M02_test_data_seed.sql`.
- Chạy 3 agent:
  - A: TC01–TC24
  - B: TC25–TC48
  - C: TC49–TC67
- Lưu screenshot vào `docs/bmad/testing/evidence/relative/agent-a|b|c/`.
- Xuất JSON + markdown report.
- Reconcile, cập nhật Excel Relative, plan Relative, defect list 13 defect.
- Không sửa code; fail thì log defect.

## E. Sau khi agent chạy xong (bạn kiểm tra)

- [ ] Đủ **67 case**, không trùng/thiếu.
- [ ] **64 Pass / 3 Fail / 0 Blocked**.
- [ ] TC53, TC62, TC63 vẫn **Fail**.
- [ ] TC58, TC61 **Pass**; không có UI-D08.
- [ ] Mỗi case có ít nhất 1 screenshot; tên file đúng `TRN_TCxx_*.png`.
- [ ] Không còn hardcoded date/ID/magic number trong steps:
  grep `2026-11`, `2027-11`, `#37`, `#46`, `#49`, `#50`, `18/11/3/6.4`.
- [ ] `SWT301_Test Case - M02 UI - Relative.xlsx` có 67 case + evidence.
- [ ] `M02_Test_Input_Data_Relative.xlsx` có giá trị tương đối.
- [ ] Defect list 13 defect, không có UI-D08.
- [ ] Nếu có defect mới: log Jira + ảnh chính xác.
- [ ] Đóng gói lại zip bàn giao.

## F. Prompt master để paste cho agent

```text
You are the M02 Head Trainer test orchestrator. Work in WSL2 at ~/work/rtms.

Before doing anything:
1. Read plan/M02-relative-wsl2-agent-plan.md.
2. Read plan/M02-relative-agent-prompts.md.

Goals:
- Rewrite 67 TRN_TC cases to relative dates/IDs/boundaries.
- Run 3 subagents: A TC01-24, B TC25-48, C TC49-67.
- Produce JSON reports, screenshots, relative Excel/plan, 13-defect list.

Hard rules:
- UI-only; no API calls or DB writes by test scripts. Only run M02_test_data_seed.sql.
- No hardcoded dates, IDs, counts or magic values.
- Every Pass needs a screenshot.
- Do not fix code; log defects.
- TC53/TC62/TC63 must remain Fail.
- TC58/TC61 stay Pass; do not test the Copilot button.
- UI-D08 is waived; RTMS-48 is closed. 13 Jira defects.

Start with preflight, seed, app health, then launch A/B/C.
Use the prompts in M02-relative-agent-prompts.md exactly. Reconcile and report counts.
```

## G. Nếu bị blocked

| Vấn đề | Xử lý |
|---|---|
| WSL không nối DB Windows | Dùng `WIN_HOST=$(ip route show default | awk '{print $3}')`; kiểm tra `pg_isready`; mở firewall/pg_hba |
| Playwright thiếu browser | `npx playwright install --with-deps chromium` |
| Node 26 lỗi test | Cài Node 22/24 |
| Port bận | Kill process cũ; đổi port và set `PLAYWRIGHT_BASE_URL` |
| Agent conflict data | Chạy lại seed SQL; hoặc 3 DB riêng |
| Case fail | Triage theo `FAILURE_TRIAGE`; không tự sửa code |
