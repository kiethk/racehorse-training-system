# Handoff — Vet FE session 2026-10-25/26

Actor: VETERINARIAN · Scope constraint: chỉ sửa trong phạm vi Vet (FE: `features/vet`, `app/(app)/veterinarian/**`, các component Vet dùng chung trong `features/admissions`/`features/care`/`features/health` khi render trên màn Vet). BE không đụng trừ khi user yêu cầu rõ (đã có 1 lần: marker correction endpoint).

## Bối cảnh project

- Monorepo: `backend/` (Spring Boot, :8080, Maven) + `frontend/` (Next.js 16 App Router + React 19 + Tailwind v4, :3000, npm).
- FE rules bắt buộc (frontend/AGENTS.md): primitives-only (cấm `<button>`/`<input>`/`<table>` raw ngoài `components/ui`), tokens-only (cấm hex, Tailwind palette), 1 type scale, `cn()`, verify = `npm run lint` + `npm run build` + `npm test` (node --test) + 1280/1440px 4 states.
- FE test runner: `npm test` = `node --experimental-transform-types --test` (không phải vitest). File test ở `features/**/__tests__/*.test.ts` viết bằng `node:test`.
- Tài khoản demo Vet: `vet1@rtms.com` (seed; mật khẩu `RtmsDemo@2026` hoặc `admin123`).

## Việc đã hoàn tất trong session (theo thứ tự)

1. **Khảo sát FE Vet toàn bộ 7 màn hình** (Playwright + code) + inventory BE endpoints/DTOs. Báo cáo kèm ASCII từng màn. Có trong hội thoại, chưa lưu doc.
2. **Review pipeline màu theo state** (Admissions) — bỏ hardcode tím/xám theo vị trí cột + hex (`#6657c7`): circle = completed→xanh, current→tím, rejected→đỏ; line xanh giữa completed và vào stage active. ⚠️ Sau đó bị refactor ngoài ("fix bug ui ux not reposive") đơn giản hóa line về xanh/xám — circle semantic còn, line-tím vào stage active ĐÃ BỊ MẤT, cần re-apply nếu user muốn giữ spec.
3. **Xác nhận "bắt buộc tạo Schedule"** cho Routine follow-up (FE `followUpRequired=true` + BE validate BLOCKED⇒nextSchedule) và Preventive (BE auto milestone) — không cần sửa.
4. **Form xem lại HealthRecord dùng chung**: mở rộng `CareRecordDetailModal` (features/care) thành 2 mode (`scheduleId` | `record` đầy đủ), đủ field HealthRecord (recordType, examinedAt, symptoms, productOrService, notes…), rồi.
5. **Trang dùng chung `/veterinarian/records` + `RecordDetailView`** (features/care/components) thay modal cho Routine list "View Record" + Health "View Record" + CareRecordDetailModal giờ là modal shell mỏng render cùng body. Có injury tag list: 1 dòng (● type · zone · [status]) toggle mở full chi tiết.
6. **Marker 3D**: nguyên nhân "marker sai" = injury không có coords bị default (0,0,0) → chỉ render marker khi `positionX != null`, injury thiếu coords chỉ nằm trong tag list với nhãn "no 3D marker". Zone label từ `injuryZones.zoneById`.
7. **BE marker correction** (duy nhất task đụng BE): thêm `MarkerPlacementRequest` record; `CorrectInjuryRecordRequest` thêm field `marker` (giữ compat 5-arg ctor cho tests cũ); `InjuryRecordService.correctInjuryRecord` override tọa độ khi có marker. Tests: `InjuryRecordServiceTest` 4/4.
8. **FE Reposition marker**: `InjuryCorrectionModal` thêm viewer 3D + "Reposition marker" (isAddMode), payload `marker` lên `/injury-records/{id}/corrections`.
9. **Sự cố + phục hồi**: công việc bị `git stash` (68 files) khi pull PR #164; đã apply lại `stash@{0}` (0 conflict), unstage rác `.playwright-mcp/*.log`, tạo backup branch `backup-stash-vet-3d`. Verify lại: BE 29/29, FE 181/181, Playwright OK, cả 2 dev server đã restart chạy code mới.

## Trạng thái repo hiện tại

- Branch: `feature/thanh-vet-urgent` (@ ca7f8343) + toàn bộ WIP đã apply vào working tree (UNCOMMITTED — cần commit sớm).
- `develop` đã có PR #165 (horse INACTIVE lifecycle của Kiet).
- STAGED sẵn trong index: feature horse INACTIVE của Kiet (~20 files) — hành lý của người khác, chờ user quyết.
- `stash@{0}` còn nguyên (đừng pop khi chưa commit); backup branch `backup-stash-vet-3d`.
- Lệnh verify: `cd frontend && npm test` (181 Pass); `cd backend && ./mvnw -Dtest="InjuryRecordServiceTest,MedicalServicesIntegrityTest,RoutineCadenceTest,CareSchedulePreventiveIntegrationTest" test`.

## Backlog đã phát hiện chưa xử (từ khảo sát ban đầu)

1. Dashboard/Schedule hardcode khung giờ "13:30 – 15:30" + "120-min window" — BE không có working hours.
2. Preventive Care: 191 dòng "Unknown Protocol"/"Overdue" — mapping protocol FE↔BE hỏng.
3. Health list MetricCard "Urgent Cases: 3" vs Dashboard "Active Emergencies: 1" — lệch nguồn số liệu.
4. Urgent workspace: "Case Severity" hiển thị rỗng dù BE có severity.
5. Timezone bug: `filterTodayAppointments` so `scheduledAt.startsWith(todayLocal)` (ISO UTC vs local date).
6. "Simulate Auto-Exam" (VetReviewForm random vitals/notes) — nút demo trong form production, nên remove hoặc feature-flag.
7. Schedule timeline chưa wire sang trang records (vẫn drawer cũ).
8. Lỗi dev-only `@react-three/fiber` + drei `<Html>` khi unmount (từ trước, không blocker).
9. `positionX` lưu world-space — nếu đổi GLB/scale phải migrate sang `localX/Y/Z + meshName` (BE đã có cột).
10. ⚠️ File test `admissions/components/__tests__/admissionPipeline.test.ts` từng bị đổi nhầm runner sang vitest giữa session — giữ `node:test`, KHÔNG đổi lại (project dùng `node --test`).

## Quy ước làm việc với user

- Nhận task FE → mô tả UI (ASCII) trước khi sửa → sửa smallest-diff → verify (lint/test/Playwright) → báo cáo kèm mục "skip/risk".
- Ưu tiên reuse component có sẵn; không tạo abstraction không được yêu cầu.
- Xác minh số liệu FE hiển thị phải đối chiếu BE DTO (chống "thông số BE không có mà FE thêm vào").
