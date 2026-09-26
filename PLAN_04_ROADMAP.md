# PLAN 04 — LỘ TRÌNH XÂY DỰNG (TRAINER & GROOM)

> **Phiên bản 2 — mô hình giao hàng xen kẽ Backend ↔ Frontend.**
>
> Bản 1 xếp theo mức ưu tiên P0–P4 và chỉ nói về backend. Bản này tổ chức lại
> theo **ĐỢT**: mỗi đợt là một lát cắt dọc chạy được từ API tới màn hình.
> Xong backend đủ cho một luồng thì chuyển sang làm giao diện cho đúng luồng đó,
> rồi mới đẩy tiếp backend của luồng kế.
>
> Cập nhật: 2026-09-26. Đối chiếu trực tiếp với mã nguồn, không theo trí nhớ.

---

## MỤC LỤC

- [0. Cách đọc tài liệu này](#0-cách-đọc-tài-liệu-này)
- [1. Trạng thái hiện tại](#1-trạng-thái-hiện-tại)
- [2. Nguyên tắc giao hàng xen kẽ](#2-nguyên-tắc-giao-hàng-xen-kẽ)
- [3. ĐỢT 1 — Luồng tiếp nhận ngựa (Trainer)](#3-đợt-1--luồng-tiếp-nhận-ngựa-trainer)
- [4. ĐỢT 2 — Chuồng trại & phân công Groom](#4-đợt-2--chuồng-trại--phân-công-groom)
- [5. ĐỢT 3 — Lập kế hoạch huấn luyện](#5-đợt-3--lập-kế-hoạch-huấn-luyện)
- [6. ĐỢT 4 — Vận hành buổi tập](#6-đợt-4--vận-hành-buổi-tập)
- [7. ĐỢT 5 — Bảng công việc hằng ngày của Groom](#7-đợt-5--bảng-công-việc-hằng-ngày-của-groom)
- [8. ĐỢT 6 — Báo cáo sự cố](#8-đợt-6--báo-cáo-sự-cố)
- [9. ĐỢT 7 — Bảng tiến độ & cảnh báo](#9-đợt-7--bảng-tiến-độ--cảnh-báo)
- [10. ĐỢT 8 — Khẩu phần ăn](#10-đợt-8--khẩu-phần-ăn)
- [11. ĐỢT 9 — Hoàn thiện](#11-đợt-9--hoàn-thiện)
- [12. Bảng tra API theo màn hình](#12-bảng-tra-api-theo-màn-hình)
- [13. Phụ thuộc vào actor khác](#13-phụ-thuộc-vào-actor-khác)
- [14. Hạn chế đã biết — đưa vào báo cáo](#14-hạn-chế-đã-biết--đưa-vào-báo-cáo)
- [15. Điểm mạnh nên nhấn khi bảo vệ](#15-điểm-mạnh-nên-nhấn-khi-bảo-vệ)

---

## 0. Cách đọc tài liệu này

Mỗi đợt có hai làn:

| Làn | Nội dung |
|---|---|
| **BE** | Việc backend cần xong **trước** khi bắt đầu giao diện của đợt đó |
| **FE** | Màn hình dựng được ngay khi làn BE xanh |

| Trạng thái | Ký hiệu |
|---|---|
| Đã xong, đã kiểm chứng | ✅ |
| Đã thiết kế, chưa code | 📐 |
| Chưa thiết kế | ⬜ |
| Chờ actor khác | ⏳ |
| Không làm | ❌ |

---

## 1. Trạng thái hiện tại

| Hạng mục | Tình trạng |
|---|---|
| Migration | **V48** (`protect admission snapshots`) |
| Test | **64/64 pass**, 0 failure |
| Controller | 26 file |
| Frontend | `app/trainer/page.tsx` và `app/groom/page.tsx` mới là khung rỗng |
| Feature admissions (FE) | Chỉ có `ManagerQueueList`, `ManagerFinalReviewPanel`, `OwnerAdmissionForm` |

### Backend đã hoàn thành

| Nhóm | Nội dung |
|---|---|
| **Cơ chế Lot** | `TrainingLot`, thuật toán tìm-hoặc-tạo + xếp khe first-fit, ghi danh theo nhóm, plan lưu khuôn mẫu (`trainingDays`, `groomId`). V43–V44 |
| **Vận hành lot** | `GET /api/lots` (đã lọc lot huỷ + Groom xem được lịch của mình), reschedule, cancel, `cancelLotIfEmpty` |
| **Đóng buổi tập** | `PATCH /api/workouts/{id}/complete` + tự chuyển plan sang `COMPLETED` |
| **Chuồng & Groom** | `assign-stall`, `assign-groom`, BR-06 (tối đa 3 chuồng/Groom) |
| **Groom** | Today Tasks gom 3 nguồn, vòng đời báo cáo sự cố. V45–V46 |
| **Trainer duyệt tiếp nhận** | `trainer-view`, `trainer-review`, `readiness-history`. **V47** |

### Việc đã gỡ khỏi mục "hạn chế"

Hai mục dưới đây ở bản 1 còn nằm trong "Hạn chế đã biết", nay **đã sửa xong**:

- `GET /api/lots` không còn trả lot đã huỷ (thêm `?includeCancelled=`, mặc định `false`)
- Groom gọi `GET /api/lots` đã ra dữ liệu (rẽ nhánh theo vai trò, JOIN qua `workouts.assigned_to_id`)

---

## 2. Nguyên tắc giao hàng xen kẽ

```
   BE đợt N  ──▶  FE đợt N  ──▶  BE đợt N+1  ──▶  FE đợt N+1  ──▶ ...
      │              │
      │              └── chỉ bắt đầu khi làn BE của CÙNG đợt đã xanh
      └── chỉ làm đúng phần API mà màn hình của đợt đó cần
```

Ba quy tắc giữ cho mô hình này không vỡ:

1. **Không làm backend vượt trước đợt.** API không có màn hình dùng tới là API chưa được kiểm chứng — rất dễ sai kiểu dữ liệu mà không ai biết cho tới lúc ghép.
2. **Mỗi đợt phải demo được.** Kết thúc một đợt là có một luồng bấm từ đầu tới cuối trên trình duyệt. Đây cũng là đơn vị để báo cáo tiến độ với giảng viên.
3. **Thiếu API thì chặn đợt, không vá ở frontend.** Nếu màn hình cần một trường mà API không trả, bổ sung ở backend rồi mới làm tiếp — không gọi vòng thêm vài API để tự ghép ở client.

### Quy ước frontend (theo `features/admissions/README.md`)

```
Page  →  Feature Component  →  Feature Service  →  services/api.ts  →  Backend
```

- Component **không** gọi `fetch()` trực tiếp
- Không hardcode `http://localhost:8080`, dùng `NEXT_PUBLIC_API_URL` trong `api.ts`
- Kiểu dữ liệu đặt ở `features/<tên>/types/`

---

## 3. ĐỢT 1 — Luồng tiếp nhận ngựa (Trainer)

> **Mục tiêu:** Trainer đăng nhập, thấy danh sách đơn đang chờ mình, mở hồ sơ,
> xuống khu cách ly xem ngựa, rồi nhập đánh giá và nộp.

### 3.1. Làn BE — kết quả rà soát

Tôi đã đối chiếu từng thao tác trong luồng với endpoint có thật:

| # | Thao tác của Trainer | API | Quyền | TT |
|---|---|---|---|---|
| 1 | Xem danh sách đơn chờ mình duyệt | `GET /api/admissions?status=TRAINER_REVIEW` | `ADMISSION_APPLICATION_VIEW` | ✅ |
| 2 | Mở hồ sơ ứng viên (gộp một lời gọi) | `GET /api/admissions/{id}/trainer-view` | `ADMISSION_APPLICATION_VIEW` | ✅ |
| 3 | Xem danh sách giấy tờ kèm theo | `GET /api/admissions/{id}/documents` | `ADMISSION_DOCUMENT_VIEW` | ✅ |
| 4 | Tải / xem file giấy tờ | `GET /api/admissions/{id}/documents/{docId}/file` | `ADMISSION_DOCUMENT_VIEW` | ✅ |
| 5 | **Biết ngựa nằm chuồng nào để xuống gặp** | `quarantineStallCode` trong `trainer-view` | — | ✅ |
| 6 | Nhập đánh giá & nộp | `POST /api/admissions/{id}/trainer-review` | `ADMISSION_APPLICATION_TRAINER_REVIEW` + `RACING_READINESS_ASSESSMENT_CREATE` | ✅ |
| 7 | Xem lại đánh giá đã nộp | `trainer-view` trả kèm `assessment` | — | ✅ |
| 8 | Lịch sử đánh giá của con ngựa | `GET /api/admissions/horses/{horseId}/readiness-history` | `RACING_READINESS_ASSESSMENT_VIEW` | ✅ |

Đã kiểm tra `V27`: `HEAD_TRAINER` có đủ cả `ADMISSION_APPLICATION_VIEW`, `ADMISSION_DOCUMENT_VIEW`, `ADMISSION_APPLICATION_TRAINER_REVIEW`. `V47` cấp thêm hai quyền đánh giá.

**Kết luận: backend đã đủ dùng. Chỉ thiếu đúng một trường nhỏ.**

### 3.2. 🔧 BE-1.1 — Bổ sung `quarantineStallCode` vào danh sách đơn

**Đây là việc backend duy nhất cần làm trước khi bắt đầu frontend đợt này.**

`AdmissionSummaryResponse` hiện có: `admissionId`, `status`, `candidateName`, `breed`, `dateOfBirth`, `submittedAt`, **`quarantineStallId`**.

Vấn đề: `quarantineStallId` là số nguyên nội bộ (ví dụ `18`). Trainer cầm điện thoại đi xuống khu cách ly cần **mã chuồng** (`Q3`), không phải id. Hiện muốn biết mã thì phải mở chi tiết từng đơn — với 5 đơn chờ là 5 lần bấm vào rồi thoát ra.

```
Thêm:  private String quarantineStallCode;
Vào:   dto/AdmissionSummaryResponse.java
       + chỗ dựng DTO trong AdmissionQueryService
```

**Rủi ro: thấp.** Đây là thêm trường mới (additive), không đổi và không xoá trường nào — màn hình Manager và Owner đang dùng chung DTO này sẽ không vỡ. Nhưng vì là code dùng chung, **báo cho nhóm một tiếng trước khi đẩy**.

> Nếu muốn tránh hẳn việc đụng DTO chung, giải pháp thay thế là để FE hiển thị id ở màn danh sách và chỉ hiện mã chuồng ở màn chi tiết. Chấp nhận được, nhưng trải nghiệm kém hơn rõ rệt.

### 3.3. Hai điều KHÔNG cần thêm API

Tôi có cân nhắc rồi quyết định không làm, ghi ra đây để khỏi bàn lại:

**(a) Trạng thái "Trainer đã nhận đơn / đang xem xét".**
Trainer nhận đơn hôm nay, có thể mai mới xuống chuồng quan sát được. Trong khoảng đó đơn vẫn ở `TRAINER_REVIEW` và không ai biết ai đang xử lý.

Không làm, vì: đề tài không yêu cầu; hệ thống chỉ có một Head Trainer nên không có tranh chấp; thêm trạng thái trung gian là thêm nhánh vòng đời phải kiểm thử. Việc "đi gặp ngựa" là hành động **vật lý** — hệ thống chỉ cần làm đúng hai việc là *nói ngựa ở đâu* (đã có, mục 5) và *nhận kết quả* (đã có, mục 6).

**(b) Lưu nháp đánh giá.**
Form chỉ có 3 điểm số + 1 ước tính + 1 ô nhận xét. Nhập một lần là xong. Lưu nháp sẽ đẻ thêm trạng thái `DRAFT` trên bảng đánh giá và một nhánh "nháp nhưng đơn đã chuyển bước".

### 3.4. ⚠️ Phụ thuộc cần biết trước khi dựng giao diện

`AdmissionTrainerReviewController.getTrainerView` hiện trả **rỗng cứng** hai mục:

```java
List<Object> healthRecords = List.of();
List<Object> healthMetrics = List.of();
```

Đây **không phải lỗi** — module Thú y chưa ghi dữ liệu vào `health_records` / `horse_health_metrics`. Frontend phải hiển thị *"Chưa có dữ liệu khám"* thay vì để trống hoặc báo lỗi. Khi nhóm Thú y xong, backend chỉ cần thay hai dòng trên bằng query thật, **frontend không phải sửa gì** nếu đã xử lý sẵn trường hợp mảng rỗng.

### 3.5. Làn FE — màn hình cần dựng

| # | Màn hình | Đường dẫn | Gọi API |
|---|---|---|---|
| **FE-1.1** | **Hàng đợi đơn chờ duyệt** | `app/trainer/admissions/page.tsx` | mục 1 |
| **FE-1.2** | **Hồ sơ ứng viên + form đánh giá** | `app/trainer/admissions/[id]/page.tsx` | mục 2, 3, 4, 6 |

**Tệp cần tạo** (theo quy ước trong `features/admissions/README.md`):

```
features/admissions/
├── services/trainerApi.ts          getTrainerQueue() · getTrainerView(id)
│                                   submitTrainerReview(id, body)
├── types/trainer.ts                TrainerAdmissionView · TrainerReviewRequest
│                                   RacingReadinessStatus
└── components/
    ├── TrainerQueueList.tsx        bám theo ManagerQueueList.tsx đã có
    └── TrainerReviewPanel.tsx      bám theo ManagerFinalReviewPanel.tsx
```

**Nội dung màn FE-1.2:**

```
┌── Hồ sơ ứng viên ─────────────────────────────┐
│  Tên · giống · ngày sinh · UELN               │
│  📍 Khu cách ly — chuồng Q3      ← đi gặp ngựa │
├── Giấy tờ (tải về) ────────────────────────────┤
├── Dữ liệu thú y ───────────────────────────────┤
│  "Chưa có dữ liệu khám"          ← xem 3.4     │
├── ĐÁNH GIÁ CỦA HUẤN LUYỆN VIÊN ────────────────┤
│  Mức sẵn sàng   ○ READY                        │
│                 ● NEEDS_MORE_TRAINING          │
│                 ○ UNSUITABLE                   │
│  Dáng vóc       [ 7.5 ] / 10                   │
│  Tính nết       [ 8.0 ] / 10                   │
│  Bước đi        [ 6.5 ] / 10                   │
│  Ước tính       [  4  ] tháng nữa đủ đi đua    │
│  Nhận xét       [ .................. ]         │
│                                                │
│  ⓘ Thể lực để trống — ngựa đang cách ly,       │
│    không đưa ra đường chạy chung được          │
│                        [ Nộp đánh giá ]        │
└────────────────────────────────────────────────┘
```

**Bốn điểm dễ sai khi làm giao diện:**

1. Trainer **không có nút Duyệt / Từ chối**. `V37` đã xoá cột `trainer_decision`. Nộp đánh giá xong đơn tự sang `MANAGER_REVIEW`.
2. **Ô thể lực (`fitnessScore`) không hiện trên form.** Backend cố ý để `NULL` ở bước tiếp nhận. Có CHECK constraint chặn `valid_until` ở loại đơn tiếp nhận.
3. **Nộp một lần duy nhất.** Gọi lại sẽ bị service chặn vì đơn đã rời `TRAINER_REVIEW`. Nộp xong phải khoá form và chuyển sang chế độ chỉ đọc.
4. Mã lỗi: nghiệp vụ sai → **400**, không tìm thấy → **404**, sai quyền → **403**.

### 3.6. Tiêu chí hoàn thành đợt 1

- [ ] BE-1.1 xong, `./mvnw test` vẫn xanh
- [ ] Đăng nhập trainer → thấy hàng đợi, mỗi dòng có mã chuồng cách ly
- [ ] Mở một đơn → thấy hồ sơ, tải được giấy tờ, mục thú y hiện "chưa có dữ liệu"
- [ ] Nhập đánh giá → nộp → đơn biến khỏi hàng đợi, chuyển `MANAGER_REVIEW`
- [ ] Mở lại đơn đó → thấy đánh giá đã nộp, form ở chế độ chỉ đọc
- [ ] Nộp lần hai bằng Swagger → nhận 400 với thông báo rõ ràng

---

## 4. ĐỢT 2 — Chuồng trại & phân công Groom

> **Vì sao xếp thứ hai:** đây là **tiền đề bắt buộc** của lập kế hoạch.
> `createPlan` sẽ từ chối ngay nếu ngựa chưa có chuồng, hoặc chuồng chưa có Groom.

### 4.1. Làn BE

| # | Việc | API | TT |
|---|---|---|---|
| 1 | Xem chuồng theo khu / trạng thái / groom | `GET /api/stalls?areaCode=&areaType=&status=&groomId=` | ✅ |
| 2 | Xem danh sách khu | `GET /api/areas` | ✅ |
| 3 | Xếp ngựa vào chuồng | `PUT /api/horses/{id}/assign-stall` | ✅ |
| 4 | Gán Groom cho chuồng (BR-06) | `PUT /api/stalls/{id}/assign-groom` | ✅ |
| 5 | **Lọc ngựa theo khu của Trainer** | — | ⬜ **BE-2.1** |

### 4.2. 🔧 BE-2.1 — Lọc ngựa theo khu phụ trách

`HorseService.getAllHorses` hiện chỉ lọc cho `HORSE_OWNER`:

```java
if ("HORSE_OWNER".equals(currentUser.getRole())) {
    return horseRepository.findByOwnerId(currentUser.getUserId());
}
return horseRepository.findAll();      // ← HEAD_TRAINER rơi vào đây
```

Trainer nhận **toàn bộ** ngựa của trang trại, gồm cả ngựa khu khác và ngựa đang cách ly. Màn hình "ngựa trong khu của tôi" không dựng được, và danh sách chọn khi lập kế hoạch sẽ đầy ngựa mà `createPlan` chắc chắn từ chối (BR-12).

Cần thêm tham số lọc, suy qua chuỗi quan hệ đã có sẵn — **không thêm cột nào**:

```
Horse.currentStallId → StableStall.areaId → Area.trainerId
```

Đề xuất: `GET /api/horses?mine=true` cho HEAD_TRAINER, hoặc `?areaId=`. Nên gộp luôn `?status=` để lọc `CANDIDATE` ra khỏi danh sách ghi danh.

> ⚠️ `HorseService` là code dùng chung nhiều actor. Thêm **tham số tuỳ chọn** thì hành vi cũ không đổi khi không truyền — an toàn. Đừng đổi hành vi mặc định.

### 4.3. Làn FE

| # | Màn hình | Đường dẫn |
|---|---|---|
| **FE-2.1** | Sơ đồ chuồng khu mình — trạng thái từng ô, ngựa đang ở, Groom phụ trách | `app/trainer/stable/page.tsx` |
| **FE-2.2** | Xếp ngựa vào chuồng + gán Groom | cùng trang, dạng hộp thoại |

Giao diện cần thể hiện rõ hai ràng buộc, **chặn ngay tại client** thay vì để backend trả lỗi:

- **BR-06** — Groom đã giữ 3 chuồng thì xám đi trong danh sách chọn
- **BR-07** — chuồng đã `OCCUPIED` thì không cho thả ngựa khác vào

---

## 5. ĐỢT 3 — Lập kế hoạch huấn luyện

> Luồng cốt lõi của actor Trainer. Xem [PLAN_01](PLAN_01_TRAINER_BACKEND.md).

### 5.1. Làn BE — ✅ đã đủ, không cần bổ sung

| Việc | API |
|---|---|
| Danh sách bài tập / khoá học | `GET /api/subjects` · `GET /api/courses` |
| Tạo bài tập / khoá / gắn bài vào khoá | `POST /api/subjects` · `POST /api/courses` · `POST /api/courses/{id}/subjects` |
| Gợi ý nhóm ghi danh chung | `GET /api/training-plans/joinable-cohorts` |
| **Ghi danh theo nhóm** | `POST /api/training-plans` |
| Xem chi tiết kế hoạch | `GET /api/training-plans/{id}` |
| Xem lịch lot theo tuần | `GET /api/lots?from=&to=` |

### 5.2. Làn FE

| # | Màn hình | Đường dẫn |
|---|---|---|
| **FE-3.1** | Quản lý bài tập & khoá học | `app/trainer/courses/page.tsx` |
| **FE-3.2** | **Form ghi danh theo nhóm** | `app/trainer/plans/new/page.tsx` |
| **FE-3.3** | Chi tiết kế hoạch — tiến độ + danh sách buổi | `app/trainer/plans/[id]/page.tsx` |

**FE-3.2 là màn hình khó nhất của cả dự án.** Ba điểm phải làm đúng:

1. **Chọn nhiều ngựa cùng lúc**, dùng chung `courseId` + `startDate` + `trainingDays`. Không dựng thành form đơn lẻ rồi bấm nhiều lần — sẽ mất hẳn tác dụng ghép nhóm.
2. **Gọi `joinable-cohorts` trước** để gợi ý ngày bắt đầu ghép được với nhóm đang chạy.
3. **Hiển thị lỗi "hết khe giờ vàng" cho tử tế.** Backend trả kèm số liệu (đã dùng bao nhiêu phút, mấy lot) — hiện nguyên văn và gợi ý đổi ngày, đừng nuốt thành "Có lỗi xảy ra".

> Nhắc lại hành vi đúng: các ngựa trong **cùng một** yêu cầu vẫn có thể rơi vào **các lot khác nhau** (do BR-09 hoặc lot đầy). Giao diện phải hiện được điều đó, đừng giả định một nhóm là một lot.

---

## 6. ĐỢT 4 — Vận hành buổi tập

### 6.1. Làn BE — ✅ đã đủ

| Việc | API |
|---|---|
| Lịch lot theo khoảng ngày | `GET /api/lots?from=&to=&includeCancelled=` |
| Chi tiết một lot + danh sách ngựa | `GET /api/lots/{id}` |
| Dời giờ lot | `PATCH /api/lots/{id}/reschedule` |
| Huỷ lot | `PATCH /api/lots/{id}/cancel` |
| Đóng buổi tập + ghi chỉ số | `PATCH /api/workouts/{id}/complete` |

### 6.2. Làn FE

| # | Màn hình | Đường dẫn |
|---|---|---|
| **FE-4.1** | **Dòng thời gian lot trong ngày** (06:00–10:00) | `app/trainer/schedule/page.tsx` |
| **FE-4.2** | Form ghi nhận kết quả buổi tập | hộp thoại trong FE-4.1 |

FE-4.1 nên vẽ dạng dải thời gian để thấy được **khe trống** — đó là lý do tồn tại của màn hình này:

```
06:00      07:00      08:00      09:00      10:00
├──────────┴──────────┼──────────┴──────────┤
│ Lot 25 · Gallop 90' │          │ Lot 27 · Trot 60'
│ 4/6 ngựa            │  TRỐNG   │ 6/6 ĐẦY
```

FE-4.2 nhập theo **từng con ngựa** trong lot: cự ly, thời lượng, tốc độ đỉnh/TB, nhịp tim TB/tối đa/hồi phục, điểm phong độ 1–10, nhận xét. Khi con cuối cùng của kế hoạch được đóng, backend **tự** chuyển plan sang `COMPLETED` — giao diện cần phản ánh chuyện đó mà không cần gọi thêm API.

---

## 7. ĐỢT 5 — Bảng công việc hằng ngày của Groom

### 7.1. Làn BE — ✅ đã đủ

| Việc | API |
|---|---|
| Bảng việc hôm nay (gom 3 nguồn) | `GET /api/groom-daily-tasks/today` |
| Tick hoàn thành | `PATCH /api/groom-daily-tasks/{id}/complete` |
| Sinh việc thường nhật | `POST /api/groom-daily-tasks/generate-routine` |
| Lịch lot của chính Groom | `GET /api/lots?from=&to=` |

### 7.2. Làn FE

| # | Màn hình | Đường dẫn |
|---|---|---|
| **FE-5.1** | Checklist hôm nay, xếp theo giờ | `app/groom/tasks/page.tsx` |

Ba nguồn việc đến kèm `TaskSource`, giao diện phải phân biệt được vì **mức quyền khác nhau**:

| `source` | Nội dung | Groom tick được? |
|---|---|---|
| `SOP` | Cho ăn, dọn chuồng, tắm chải | ✅ Có |
| `WORKOUT` | Buổi tập được giao — giờ đọc từ lot | ❌ Không — Trainer đóng |
| `PREVENTIVE_CARE` | Lịch thú y của ngựa trong chuồng mình | ❌ Không — chỉ để biết |

Trường `actionable` trong response đã nói sẵn tick được hay không. Dùng nó, đừng tự suy từ `source`.

---

## 8. ĐỢT 6 — Báo cáo sự cố

### 8.1. Làn BE — ✅ đã đủ

| Việc | API |
|---|---|
| Gửi báo cáo kèm ảnh | `POST /api/groom-incident-reports` |
| Xem chi tiết | `GET /api/groom-incident-reports/{id}` |
| Thú y tiếp nhận / kết luận | `PATCH /api/groom-incident-reports/{id}/handle` |

### 8.2. Làn FE

| # | Màn hình | Đường dẫn |
|---|---|---|
| **FE-6.1** | Form báo cáo sự cố + tải ảnh | `app/groom/incidents/new/page.tsx` |
| **FE-6.2** | Danh sách báo cáo đã gửi + trạng thái xử lý | `app/groom/incidents/page.tsx` |

Vòng đời `IncidentStatus`: `REPORTED` → `ACKNOWLEDGED` → `RESOLVED`. Giao diện Groom hiện được trạng thái và `handlerNote` — đây chính là mắt xích khép kín **tam giác Groom → Thú y → Trainer** đáng nhấn khi bảo vệ.

---

## 9. ĐỢT 7 — Bảng tiến độ & cảnh báo

> Đề tài yêu cầu rõ. **Dữ liệu đã đủ** sau đợt 4, không cần bảng mới.

### 9.1. 🔧 Làn BE — ⬜ chưa code

```
GET /api/horses/{id}/fitness-trend?from=&to=
   → chuỗi thời gian từ training_workouts đã COMPLETED
     [{ date, subjectName, distance, avgSpeed,
        avgHeartRate, maxHeartRate, recoveryHeartRate, performanceRating }]

GET /api/training-plans/dashboard
   → toàn bộ ngựa trong khu của Trainer
     [{ horseId, horseName, planStatus,
        completedSessions, totalSessions, progressPercent,
        latestPerformanceRating, avgPerformanceRating30d }]

GET /api/horses/{id}/alerts      (tính khi đọc, không lưu bảng)
```

Bộ luật cảnh báo tối giản:

| Cảnh báo | Điều kiện |
|---|---|
| Nhịp tim tối đa vượt ngưỡng | `maxHeartRate > 220` |
| Hồi phục kém | `recoveryHeartRate > 100` |
| Phong độ tụt liên tục | `performanceRating` giảm 3 buổi liên tiếp |
| Khối lượng tăng đột ngột | Tổng cự ly tuần này > 1.5 × tuần trước |
| **Sự cố lặp lại** | ≥ 2 `GroomIncidentReport` cho cùng một con trong 14 ngày |

> Dòng cuối là chỗ nối Groom với Trainer: báo cáo của Groom trở thành đầu vào cảnh báo cho Trainer.

**Độ khó: thấp.** Chỉ là truy vấn tổng hợp, không thêm entity, không migration.

### 9.2. Làn FE

| # | Màn hình | Đường dẫn |
|---|---|---|
| **FE-7.1** | Bảng tiến độ toàn khu | `app/trainer/dashboard/page.tsx` |
| **FE-7.2** | Biểu đồ thể lực một con | `app/trainer/horses/[id]/page.tsx` |

Biểu đồ thể lực nên vẽ chồng **hai nguồn khác thang đo**, và phải tách rời:

- Đường liền: chỉ số đo thật từ `training_workouts`
- Điểm mốc rời: `racing_readiness_assessments` định kỳ (gọi `readiness-history` **không** kèm `includeAdmission`)

Đánh giá lúc tiếp nhận (`admissionId != null`) **không được nối vào đường** — nó chấm bằng quan sát trong khu cách ly, khác hẳn thang đo từ dữ liệu tập. Mặc định API đã loại nó ra.

---

## 10. ĐỢT 8 — Khẩu phần ăn

> Đề tài yêu cầu: *"Xem chi tiết khẩu phần ăn (ngũ cốc, cỏ, vitamin) được duyệt cho từng bữa"*.
> **Chưa có dữ liệu** — cần bảng mới.

### 10.1. 🔧 Làn BE — ⬜ chưa thiết kế

```sql
feed_rations (
    id, horse_id, meal_slot,          -- MORNING | LUNCH | EVENING
    grain_kg, hay_kg, supplement_note,
    approved_by_id, approved_at,
    effective_from, effective_to
)
```

**Hai câu cần chốt trước khi code:**

1. **Ai duyệt khẩu phần** — Thú y hay Quản lý? Đề tài nói *"được duyệt"* nhưng không nói ai.
2. **Có nối khẩu phần vào `GroomDailyTask` loại `FEEDING` không**, hay chỉ là màn tra cứu riêng?

Phương án rẻ nhất: chỉ làm **màn tra cứu**, `SopSlot` mang thêm `mealSlot` để Groom bấm từ task sang xem khẩu phần.

### 10.2. Làn FE

| # | Màn hình | Đường dẫn |
|---|---|---|
| **FE-8.1** | Khẩu phần theo bữa của từng con | `app/groom/rations/page.tsx` |

---

## 11. ĐỢT 9 — Hoàn thiện

Làm nếu còn thời gian. Không đợt nào phụ thuộc vào nhóm này.

| # | Việc | Làn | TT | Ghi chú |
|---|---|---|---|---|
| 9.1 | Ngưng dùng khoá học | BE | ⬜ | `CourseStatus.ARCHIVED` có enum nhưng **không endpoint nào** set. Khoá lỗi thời nằm mãi trong danh sách chọn |
| 9.2 | Sửa bài tập | BE | ⬜ | `PUT /api/subjects/{id}` |
| 9.3 | Tự chuyển `UPCOMING` → `ACTIVE` | BE | ⬜ | Job cron 00:00 cạnh `GroomDailyTaskScheduler` |
| 9.4 | Map lỗi nghiệp vụ sang 409 | BE | ⬜ | Hiện `RuntimeException` → 404, hơi thô |
| 9.5 | Rà `RaceRegistration` khớp cơ chế lot | BE | ⬜ | Controller đã có từ trước đợt refactor |
| 9.6 | Sơ đồ chuồng trực quan (kéo thả) | FE | ⬜ | Dữ liệu đã đủ. Nâng cấp của FE-2.1 |

> ⚠️ **Cảnh báo nghiệp vụ cho 9.2:** sửa `subject.durationMinutes` **không** làm các lot đã sinh đổi giờ — lot đã lưu `startTime`/`endTime` cụ thể. Đó là hành vi **đúng**: giáo án đổi không được phép xáo trộn lịch đã chốt. Nhưng phải ghi rõ trong tài liệu để người dùng không bất ngờ.

---

## 12. Bảng tra API theo màn hình

| Màn hình | Đợt | API | TT |
|---|---|---|---|
| Hàng đợi đơn tiếp nhận | 1 | `GET /api/admissions?status=TRAINER_REVIEW` | ✅ |
| Hồ sơ ứng viên | 1 | `GET /api/admissions/{id}/trainer-view` | ✅ |
| Giấy tờ kèm theo | 1 | `GET /api/admissions/{id}/documents` + `/{docId}/file` | ✅ |
| Nộp đánh giá | 1 | `POST /api/admissions/{id}/trainer-review` | ✅ |
| *Mã chuồng trong danh sách* | 1 | *thêm `quarantineStallCode`* | ⬜ **BE-1.1** |
| Sơ đồ chuồng | 2 | `GET /api/stalls?areaCode=` · `GET /api/areas` | ✅ |
| Xếp ngựa / gán Groom | 2 | `PUT /api/horses/{id}/assign-stall` · `PUT /api/stalls/{id}/assign-groom` | ✅ |
| *Ngựa trong khu của tôi* | 2 | *thêm lọc cho `GET /api/horses`* | ⬜ **BE-2.1** |
| Bài tập & khoá học | 3 | `GET/POST /api/subjects` · `/api/courses` · `/api/courses/{id}/subjects` | ✅ |
| Gợi ý nhóm | 3 | `GET /api/training-plans/joinable-cohorts` | ✅ |
| Ghi danh nhóm | 3 | `POST /api/training-plans` | ✅ |
| Chi tiết kế hoạch | 3 | `GET /api/training-plans/{id}` | ✅ |
| Dòng thời gian lot | 4 | `GET /api/lots?from=&to=` · `GET /api/lots/{id}` | ✅ |
| Dời / huỷ lot | 4 | `PATCH /api/lots/{id}/reschedule` · `/cancel` | ✅ |
| Ghi kết quả buổi tập | 4 | `PATCH /api/workouts/{id}/complete` | ✅ |
| Checklist Groom | 5 | `GET /api/groom-daily-tasks/today` · `PATCH /{id}/complete` | ✅ |
| Báo cáo sự cố | 6 | `POST /api/groom-incident-reports` · `PATCH /{id}/handle` | ✅ |
| Bảng tiến độ | 7 | `GET /api/training-plans/dashboard` | ⬜ |
| Biểu đồ thể lực | 7 | `GET /api/horses/{id}/fitness-trend` | ⬜ |
| Lịch sử đánh giá | 7 | `GET /api/admissions/horses/{horseId}/readiness-history` | ✅ |
| Cảnh báo | 7 | `GET /api/horses/{id}/alerts` | ⬜ |
| Khẩu phần ăn | 8 | *chưa có bảng* | ⬜ |

**Tổng kết: 3 API còn thiếu, tất cả đều ở đợt 7 trở đi. Đợt 1–6 chỉ cần bổ sung 2 trường nhỏ.**

---

## 13. Phụ thuộc vào actor khác

| # | Việc | Chờ ai | Ảnh hưởng |
|---|---|---|---|
| 13.1 | `healthRecords` / `healthMetrics` trong `trainer-view` | **Thú y** | Đợt 1 vẫn làm được — hiện "chưa có dữ liệu". Khi họ xong, đổi 2 dòng ở backend, FE không sửa |
| 13.2 | Đề xuất bổ sung vật tư (Groom) | **Quản lý** | Chưa có bảng vật tư. Nếu họ không kịp, chuyển mục này sang "hướng phát triển" |
| 13.3 | Tạo tài khoản Groom / Trainer | **Quản lý** | Không có `UserController` trong toàn dự án |
| 13.4 | Gán khu vực cho Trainer qua giao diện | **Quản lý** | Đang seed bằng `V44` cho môi trường dev |
| 13.5 | Nối sự cố → bệnh án (`health_records.source_incident_id`) | **Thú y** | Khép kín tam giác phối hợp |

---

## 14. Hạn chế đã biết — đưa vào báo cáo

Những đơn giản hoá **có chủ đích**. Nêu chủ động khi bảo vệ tốt hơn là để bị hỏi.

| # | Hạn chế | Lý do chấp nhận |
|---|---|---|
| 1 | **Thuật toán xếp khe là first-fit, không tối ưu** | Bài toán tối ưu là dạng bin-packing (NP-hard). First-fit **tất định** (chạy lại ra kết quả y hệt, dễ viết test), `O(n log n)`, dễ giải thích. Ngoài đời Trainer cũng xếp tuần tự |
| 2 | **Tối đa ~3 nhóm song song mỗi Trainer** | Khung 240 phút chứa 2–3 lot. Quy mô 10–18 ngựa là đủ. Vượt thì vặn `max_capacity` hoặc nới khung |
| 3 | **Ngựa vào giữa chừng khoá không đồng bộ được** | Phải chờ tới điểm đồng pha (`joinable-cohorts` gợi ý). Giới hạn có thật ngoài đời |
| 4 | **Không mô hình hoá thời gian đệm giữa 2 lot liền kề** | Ngoài đời xử lý bằng vài phút đệm + hỗ trợ chéo |
| 5 | **Việc SOP sinh theo từng con, cùng một mốc giờ** | 3 con của một Groom đều có task tắm chải đóng dấu 10:00. Lưu theo từng con để **truy vết theo cá thể** (hồ sơ thú y, khẩu phần riêng). Hiển thị gộp thành khối là việc của giao diện, chưa làm |
| 6 | **Bỏ BR-04 (1 plan = 1 khung giờ cố định)** | Nhịp sinh học vẫn giữ ở mức "luôn tập buổi sáng". Lò đua thật cũng không cam kết một con mãi đi lot đầu |
| 7 | **Không tách vai "người cưỡi"** (exercise rider) | Ngành tách rõ groom ≠ rider (hướng dẫn chính thức của Anh, phân công ở Mỹ, licence ở Úc), nhưng lò đua Anh thường kiêm nhiệm. Gộp vào Groom để giữ đúng 5 actor của đề tài |
| 8 | **Không triển khai buổi tập 1-1** (phục hồi, thuần hoá) | Có thật ngoài đời nhưng không phải buổi tập đường chạy hằng ngày |
| 9 | **Ngân hàng bài tập càng nhiều thì lot càng phân mảnh** | Quan hệ nghịch: số lot/ngày ≤ min(số nhóm, số bài đang dùng). 4–6 bài là điểm cân bằng |
| 10 | **Không tự khôi phục kế hoạch sau khi ngựa hồi phục** | Sau chấn thương thường thay bằng khoá hồi phục mới, không tập tiếp khoá cũ |
| 11 | **Bỏ `GroomShift`** | Mô hình hiện tại là FULL_DAY; BR-06 đã đủ giới hạn khối lượng công việc |
| 12 | **Đổi Groom cho chuồng không chuyển việc SOP đang treo** | Task đã sinh vẫn ghi Groom cũ. Bàn giao chuồng là thao tác hiếm và thường làm đầu ngày trước khi sinh việc |
| 13 | **Ràng buộc ngầm giữa BR-06 và mốc SOP** | `MAX_STALLS_PER_GROOM = 3` × 30 phút = đúng 90 phút, khớp khít khoảng 10:00 → 11:30. Nâng lên 4 chuồng sẽ làm tắm chải tràn vào giờ cho ăn trưa, hiện **không có code nào kiểm tra** |

---

## 15. Điểm mạnh nên nhấn khi bảo vệ

1. **Ràng buộc bằng cấu trúc, không bằng câu lệnh kiểm tra.** `subject_id` nằm trên `training_lots` chứ không trên `training_workouts` → quy tắc "ngựa chung lot phải chung bài" **không thể vi phạm**, không cần một dòng validate nào. Trước refactor, trạng thái sai đó hoàn toàn lưu được xuống DB.

2. **Một nguồn sự thật cho thời gian.** `V43` xoá hẳn `workout_date` / `start_time` / `end_time` khỏi `training_workouts`. Dời lot vì trời mưa chỉ sửa **một dòng**, cả hàng ngựa dời theo — không thể có con nào "quên đồng bộ".

3. **BR-02 được thực thi miễn phí.** Thuật toán xếp khe vốn đã không cho lot chồng nhau → không cần query kiểm tra xung đột Trainer. Toàn hệ thống chỉ còn **2 chỗ** phải kiểm tra xung đột thay vì 6.

4. **Tính đồng pha là hệ quả toán học, không phải việc phải canh.** Các plan cùng nhóm dùng chung `courseId` + `startDate` + `trainingDays`, nên `i_A ≡ i_B (mod n)` đúng vĩnh viễn.

5. **Ba con số độc lập hội tụ cùng bậc độ lớn:** 3 chuồng/Groom (BR-06) ≈ 2–3 lot mỗi sáng ≈ 3–4 lot của lò đua thật. Dấu hiệu mô hình phản ánh đúng thực tế.

6. **Đòn bẩy đúng chỗ:** ghép nhóm cho hiệu quả **×4,5** (4 → 18 ngựa/Trainer) với chi phí một vòng lặp lồng; thuật toán xếp tối ưu chỉ thêm được ~×1.5 với chi phí gấp nhiều lần. Chọn cái rẻ và hiệu quả hơn.

7. **Giao hàng theo lát cắt dọc.** Mỗi đợt demo được một luồng hoàn chỉnh từ API tới màn hình, thay vì dựng xong toàn bộ backend rồi mới biết chỗ nào ghép không khớp.

---

*Tài liệu liên quan: [PLAN_01 — Trainer](PLAN_01_TRAINER_BACKEND.md) · [PLAN_02 — Groom](PLAN_02_GROOM_BACKEND.md) · [PLAN_03 — Test Swagger](PLAN_03_SWAGGER_TEST.md)*
