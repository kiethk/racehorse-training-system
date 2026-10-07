# ĐỀ XUẤT: TỐI GIẢN HÓA TRẠNG THÁI "NGỰA CÓ ĐƯỢC TẬP KHÔNG" (TRAINER ↔ VET)

| | |
|---|---|
| **Ngày** | 2026-10-07 |
| **Người đề xuất** | Phụ trách actor Trainer & Groom (phân tích cùng trợ lý AI) |
| **Trạng thái** | **ĐỀ XUẤT, CHƯA SỬA DÒNG CODE NÀO.** Chờ cả nhóm đồng ý |
| **Ai bị ảnh hưởng** | Trainer, **Vet**, Admission (Groom/Manager duyệt đơn), Race Registration, frontend của các actor trên |
| **Cần làm gì với file này** | Đọc mục 0 và mục 8; trả lời bảng xác nhận ở cuối |

---

## 0. TÓM TẮT (đọc mục này là đủ để biết đề xuất gì)

Hệ thống hiện có **năm chỗ** cùng trả lời một câu hỏi "ngựa này có được tập không", cộng với nhiều giá trị enum không bao giờ được gán và một số endpoint không ai gọi. Đề xuất: **bỏ gần hết, chỉ giữ một nguồn sự thật duy nhất.**

**Các quyết định đã chốt bởi phụ trách Trainer:**

1. **Bỏ trạng thái `RESTRICTED`.** Chỉ còn hai trạng thái y tế: *được tập* và *tạm nghỉ có thời hạn*.
2. **Khi ngựa bị chặn tập: hủy các buổi tập tương lai và hủy plan** (không tạm dừng).
3. **Gộp** `VetDecision` vào `ReviewDecision` (cùng bộ hai giá trị với Groom và Manager).

**Đề xuất bỏ thêm (cần nhóm đồng ý):**

| Bỏ | Lý do một dòng |
|---|---|
| `TrainingStatus` (enum) + cột `horses.training_status` | Trùng hoàn toàn với `trainingLocked` khi chỉ còn 2 trạng thái |
| `TrainingDecision` (enum) | Thay bằng một cờ boolean `trainingBlocked` |
| `VetDecision` + `AdmissionStatus.PENDING_RECHECK` | `RECHECK_REQUIRED` và `PENDING_RECHECK` không nơi nào tạo ra |
| `HorseStatus.INJURED / MONITORING / QUARANTINED` | Không luồng nào hiện tại gán; sức khỏe đã có `trainingLocked` |
| `TrainingPlanStatus.PAUSED`, `WorkoutStatus.IN_PROGRESS`, `LotStatus.IN_PROGRESS` | Không nơi nào gán |
| `CourseStatus` (cả enum và cột) | Chỉ còn một giá trị `ACTIVE` sau khi bỏ `ARCHIVED` |
| `WorkoutType` (cả enum và cột) | Không có logic nào đọc, **và frontend đang gửi 4/5 giá trị backend không biết** (xem P10) |
| 4 endpoint không ai gọi | `POST /injury-records`, `PUT /horses/{id}/status`, `PUT /training-plans/{id}/status`, `GET /horses/{id}/training-lock-status` |
| Khóa hành chính của ngựa `CANDIDATE` lưu trong cờ khóa | Suy ra từ `HorseStatus == CANDIDATE`, hết cần chuỗi cố định `"Admission pending..."` |

**Kết quả cuối cùng:** từ 5 nơi lưu còn **1** (`Horse.trainingLocked` + lý do + ngày xem xét), bỏ khoảng **8 enum/giá trị** và **4 endpoint**.

---

## 1. NGỮ CẢNH: ĐÃ TRAO ĐỔI VẤN ĐỀ GÌ

Phụ trách Trainer yêu cầu trợ lý đọc toàn bộ danh sách enum và chỉ ra những trạng thái nào liên quan đến Trainer và Vet, vì *"đang có rất nhiều cái thừa và không khớp"*. Các câu hỏi gốc, viết rút lại:

1. Những status nào liên quan đến Trainer và Vet? Cái nào thừa, cái nào không khớp?
2. `TrainingDecision` và `TrainingStatus` có **3 giá trị y hệt nhau**. Xử lý thế nào?
3. Trạng thái `RESTRICTED` nên xử lý ra sao? Có 4 hướng được nêu:
   - (a) Vẫn cho Trainer lập lịch, hay không cho và **hủy luôn plan hiện tại**?
   - (b) Chỉ cho chọn **khóa học phục hồi**? Hiện chưa có loại khóa đó; muốn làm phải thêm *category* cho khóa học. Khi đó phải ràng buộc ở luồng tạo khóa: *category phục hồi thì không được chọn bài tập cường độ cao*?
   - (c) Hay **bỏ `intensityLevel`** của Subject luôn?
   - (d) Hay làm đơn giản: **bỏ hẳn `RESTRICTED`**? Nhưng khi đó nếu ngựa **bị thương nhẹ** thì sao: xếp "sẵn sàng tập" hay chặn luôn?

**Quyết định sau khi cân nhắc: chọn (d) (phương án A), hủy plan, gộp.** Tài liệu này ghi lại toàn bộ phân tích và giải pháp để cả nhóm cùng duyệt.

> **Quy ước đọc:** mỗi vấn đề ở mục 3 gồm *Vấn đề → Bằng chứng → **Giải pháp đề xuất***. Dấu `file:dòng` được lấy từ code tại thời điểm viết.

---

## 2. HIỆN TRẠNG

### 2.1. Năm nơi lưu cùng một câu hỏi "ngựa có được tập không?"

| # | Nơi lưu | Kiểu | Giá trị |
|---|---|---|---|
| 1 | `Horse.trainingLocked` | boolean | true / false |
| 2 | `Horse.trainingStatus` | `TrainingStatus` | ALLOWED, RESTRICTED, BLOCKED |
| 3 | `HealthRecord.trainingDecision` | `TrainingDecision` | ALLOWED, RESTRICTED, BLOCKED |
| 4 | `HealthRecord.vetDecision` | `VetDecision` | APPROVED, RECHECK_REQUIRED, REJECTED |
| 5 | `AdmissionApplication.vetDecision` | `VetDecision` | như trên |

Ngoài ra `Horse.currentStatus` (`HorseStatus`) có thêm các giá trị INJURED, MONITORING, QUARANTINED nói về sức khỏe.

### 2.2. Giá trị enum không bao giờ được gán (đã grep toàn bộ Java, SQL, TypeScript)

| Enum.giá trị | Kết quả |
|---|---|
| `HorseStatus.MONITORING`, `QUARANTINED` | Không dòng code nào gán. Chỉ còn trong `CHECK`, một dòng seed cũ (V12) và danh sách ở frontend |
| `HorseStatus.INJURED` | Chỉ gán qua `POST /api/injury-records` và `PUT /api/horses/{id}/status`. **Frontend không gọi cái nào.** Luồng Vet mới không bao giờ đặt INJURED |
| `AdmissionStatus.PENDING_RECHECK` | Không nơi nào gán. Nhưng code vẫn kiểm tra nó và pipeline frontend vẫn vẽ |
| `TrainingPlanStatus.PAUSED` | Chỉ có trong một câu comment |
| `WorkoutStatus.IN_PROGRESS`, `LotStatus.IN_PROGRESS` | Không nơi nào gán |
| `CourseStatus.ARCHIVED` | Không nơi nào gán. `CourseRepository.findByStatus` không ai gọi |
| `WorkoutType.TRIAL_RUN` | Chỉ có trong comment |
| `IntensityLevel` (cả 3 giá trị) | Chỉ được lưu và trả ra, không logic nào đọc (xem P11) |

### 2.3. Enum còn đang được dùng thật (không đề xuất đổi)

`AdmissionStatus` (trừ `PENDING_RECHECK`), `ReviewDecision`, `RacingReadinessStatus`, `TrainingPlanStatus` (UPCOMING, ACTIVE, COMPLETED, CANCELLED), `WorkoutStatus` (SCHEDULED, COMPLETED, CANCELLED), `LotStatus` (SCHEDULED, COMPLETED, CANCELLED), `CareScheduleStatus`, `CareType`, `VetOfferStatus`, `IncidentStatus`, `IncidentSeverity`, `TrainingDay`, `SurfaceType`, `StallStatus`, `AreaType`.

---

## 3. CÁC VẤN ĐỀ VÀ GIẢI PHÁP ĐỀ XUẤT

### P1. Một câu hỏi, năm chỗ lưu, hai setter ghi đè lẫn nhau

**Vấn đề.** `trainingLocked`, `trainingStatus`, `trainingDecision` và hai `vetDecision` đều mô tả "được tập hay không", nhưng phải giữ đồng bộ bằng tay.

**Bằng chứng.**
- `TrainingDecision.toTrainingStatus()` chỉ là một `switch` ánh xạ 1-1 sang `TrainingStatus`.
- `Horse.setTrainingStatus()` đặt `locked = (status != ALLOWED)` ([Horse.java:100-103](backend/src/main/java/com/rtms/backend/entity/Horse.java:100)); `Horse.setTrainingLocked()` đặt ngược lại `trainingStatus` ([Horse.java:121-129](backend/src/main/java/com/rtms/backend/entity/Horse.java:121)). Hai setter rewrite nhau.
- Logic khóa bị lặp ở nhiều nơi, mỗi nơi tự đặt 3-4 trường: `CareScheduleService:96`, `:121`, `:389-394`, `InjuryRecordService:71`, `AdmissionGroomReviewService:156`.

**Giải pháp đề xuất.** Giữ **một** nguồn sự thật:

| Giữ | Bỏ |
|---|---|
| `Horse.trainingLocked` (boolean) + `trainingLockReason` + `trainingLockReviewDate` + `trainingLockVetId` + `trainingLockUpdatedAt` (đều đã có sẵn) | `Horse.trainingStatus`, enum `TrainingStatus` |
| `HealthRecord.trainingBlocked` (boolean, **mới**) | `HealthRecord.trainingDecision`, enum `TrainingDecision`, hàm `toTrainingStatus()` |
| `AdmissionApplication.vetDecision` kiểu `ReviewDecision` | `HealthRecord.vetDecision` (xem P3) |

Gom việc bật/tắt khóa vào **hai hàm duy nhất** trong `HorseService`, mọi nơi khác gọi hai hàm này thay vì tự đặt trường:

```
blockTraining(horse, reason, reviewDate, vetId)   // đặt khóa + gọi hủy buổi tập tương lai (P4)
clearTrainingBlock(horse)                          // gỡ khóa
```

Việc này đồng thời xóa mã lặp ở 5 nơi trên.

---

### P2. `RESTRICTED` giống hệt `BLOCKED` về hành vi, nhưng giao diện hứa khác

**Vấn đề.** Vet được nói `RESTRICTED` nghĩa là *"Limited or modified conditioning only"* (được tập hạn chế), trong khi hệ thống không cho tập gì cả.

**Bằng chứng.**
- [VetReviewForm.tsx:117-140](frontend/src/features/admissions/components/VetReviewForm.tsx:117): RESTRICTED = "Limited or modified conditioning only", BLOCKED = "Zero training permitted".
- Backend: cả hai đều đặt `trainingLocked = true`. `createPlan` chặn khi khóa ([HorseTrainingPlanService.java:280-287](backend/src/main/java/com/rtms/backend/service/HorseTrainingPlanService.java:280)); `RaceRegistrationService:191` cũng chặn đăng ký đua.
- Vet tin rằng họ cho ngựa "tập nhẹ", nhưng Trainer không có cách nào lập lịch nhẹ.

**Giải pháp đề xuất.** **Bỏ `RESTRICTED`.** Chỉ còn hai trạng thái y tế. Mức nặng nhẹ của chấn thương được thể hiện bằng **thời hạn nghỉ** (`followUpDate`), không bằng thêm trạng thái:

| Tình huống | Vet chọn |
|---|---|
| Ngựa khỏe | **Được tập** (có thể ghi chú theo dõi) |
| Thương nhẹ (ví dụ trầy móng) | **Tạm nghỉ** + `followUpDate` = 7 ngày sau |
| Thương nặng | **Tạm nghỉ** + `followUpDate` xa hơn, hoặc từ chối nhập |

**Quy tắc mới:** khi tạm nghỉ, ngày xem xét lại (`followUpDate`, lưu vào `Horse.trainingLockReviewDate`) và lý do là **bắt buộc**, để "tạm nghỉ" luôn có điểm kết thúc, không khóa mãi.

> **Lưu ý về V59:** migration V59 đã *cố ý gỡ* ràng buộc DB `chk_health_record_follow_up` (V52 từng bắt buộc `follow_up_date` cho mọi bản ghi khám). Đề xuất này **không** khôi phục ràng buộc đó: chỉ bắt buộc ngày xem xét lại **khi quyết định tạm nghỉ**, kiểm ở tầng service (không bằng `CHECK`), còn khám định kỳ bình thường vẫn không cần. Phụ trách Vet vui lòng xác nhận ở D1.

*Với thương nhẹ không xếp "sẵn sàng tập"*: ngựa đang đau mà hệ thống cho tập thì trái nguyên tắc cốt lõi của đề tài.

*Về các hướng đã cân nhắc rồi bỏ:*
- **Khóa học phục hồi theo category:** tốn nhất. Phải thêm cột `courses.category_id`, thêm luật ở `createCourse`, vẫn phải giữ `IntensityLevel` để kiểm luật, và khóa trộn bài nhẹ lẫn vừa không biết xếp vào đâu.
- **Giữ RESTRICTED chỉ cảnh báo:** không ép được gì, chẳng khác "được tập" kèm ghi chú.
- Nếu sau này vẫn muốn "tập nhẹ có kiểm soát", cách ít tốn nhất là *suy ra* từ dữ liệu có sẵn: khóa chỉ gồm bài `intensityLevel = LOW` mới được gán cho ngựa hạn chế. Không cần cột mới. Ghi vào báo cáo như **hướng phát triển**, không làm bây giờ.

---

### P3. `VetDecision` là di sản, và nhánh "recheck" không tồn tại

**Vấn đề.** `VetDecision` (APPROVED / RECHECK_REQUIRED / REJECTED) không phải ý kiến độc lập: nó được **tính** từ `trainingDecision` và `rejectAdmission`. Form Vet đặt tên biến là `legacyDecision`.

**Bằng chứng.**
- [CareScheduleService.java:415-424](backend/src/main/java/com/rtms/backend/service/CareScheduleService.java:415) tính `vetDecision` từ `trainingDecision`.
- [AdmissionReviewService.java:117-124](backend/src/main/java/com/rtms/backend/service/AdmissionReviewService.java:117) làm chiều ngược lại.
- `AdmissionStatus.PENDING_RECHECK` không nơi nào gán ([CareScheduleService.java:353-355](backend/src/main/java/com/rtms/backend/service/CareScheduleService.java:353) và `:450` chỉ kiểm tra nó).
- Migration gốc V25 chỉ cho `vet_decision IN ('APPROVED','REJECTED')`; V52 mới mở rộng thêm `RECHECK_REQUIRED`.

**Giải pháp đề xuất.**
- **Gộp `VetDecision` vào `ReviewDecision`** `{APPROVED, REJECTED}`: cùng bộ giá trị với `groomDecision` và `managerDecision`. Đơn đã qua Vet nhưng ngựa bị tạm nghỉ vẫn là `APPROVED` (đơn tiếp tục sang `TRAINER_REVIEW`); việc ngựa bị chặn tập được ghi riêng ở `trainingLocked`.
- **Bỏ `AdmissionStatus.PENDING_RECHECK`**, và các nhánh kiểm tra nó ở `CareScheduleService` và pipeline frontend.
- **Bỏ `HealthRecord.vetDecision`** (không có ý nghĩa với bản ghi khám định kỳ). Thông tin cần giữ nằm ở `trainingBlocked` và `rejectionReason`.
- Màn Trainer ([TrainerAdmissionDetailView.tsx](frontend/src/features/admissions/components/TrainerAdmissionDetailView.tsx)) đổi nhãn "RECHECK REQUIRED" thành "Được tập / Tạm nghỉ đến dd/mm" đọc từ health record.

---

### P4. Vet khóa ngựa nhưng buổi tập và plan đang chạy **không bị hủy**

**Vấn đề.** Cơ chế hủy (`cancelFutureTrainingForHorse`: hủy buổi tập tương lai, trả khe giờ, hủy plan) tồn tại nhưng **không nối vào luồng Vet thật**.

**Bằng chứng.**
- Chỉ một nơi gọi nó: `HorseService.updateHorseStatus` ([HorseService.java:141-163](backend/src/main/java/com/rtms/backend/service/HorseService.java:141)), tức endpoint `PUT /api/horses/{id}/status`. **Frontend không gọi endpoint này.**
- `CareScheduleService.completeCareSchedule` và `createSchedule` (loại URGENT) chỉ đặt cờ khóa, không gọi hủy.
- **Ví dụ:** ngựa Black Star còn 8 buổi `SCHEDULED`, Vet khóa ngày 18/10. Kết quả: ngựa bị khóa nhưng 8 buổi vẫn nằm trong lot, Groom vẫn thấy việc dắt ngựa đi tập, các buổi đó vẫn chiếm chỗ trong lot (ngựa khác không ghép được).

**Giải pháp đề xuất.** Hủy plan (đã chốt). Hàm `blockTraining` (P1) **tự gọi** `cancelFutureTrainingForHorse`, nên mọi đường khóa đều hủy. Gọi ở hai thời điểm:
1. Vet hoàn tất khám với kết quả **tạm nghỉ**.
2. Tạo lịch **URGENT** (khóa phòng ngừa trước khi Vet khám). Ưu tiên an toàn của ngựa: có báo chấn thương thì không để buổi tập ngày mai vẫn chạy. Hàm hủy là idempotent nên gọi hai lần không sao.

*Lý do hủy thay vì tạm dừng:* tạm dừng đòi thêm logic tiếp tục lại và quyết định buổi nào bị mất; sau chấn thương, kế hoạch dở dang hiếm khi còn đúng. Hết hạn nghỉ thì Trainer tạo plan mới, có thể ghép lại nhóm cũ.

*Điểm cần nhóm xác nhận (D4):* nếu Vet khám URGENT xong và kết luận "được tập" thì plan đã bị hủy từ lúc báo. Đề xuất chấp nhận (Trainer tạo lại), vì rẻ hơn rủi ro cho ngựa.

---

### P5. Thông báo lỗi khi bị khóa không nói lý do

**Vấn đề.** Trainer nhận *"đang bị KHOÁ HUẤN LUYỆN (trạng thái: ELIGIBLE)"*: câu mâu thuẫn, không nói vì sao và đến khi nào.

**Bằng chứng.** `TrainingLockStatusResponse` chỉ trả `currentStatus` và `locked`. `Horse` đã lưu `trainingLockReason`, `trainingLockReviewDate`, `trainingLockVetId` nhưng không đưa ra.

**Giải pháp đề xuất.** **Bỏ** `TrainingLockStatusController`, `TrainingLockStatusResponse` và `InjuryRecordService.getTrainingLockStatus` (frontend không gọi). Thay bằng một hàm trong `HorseService`/`Horse`:

```
horse.canTrain()      = currentStatus == ELIGIBLE && !trainingLocked
assertCanTrain(horse) // ném lỗi có lý do cụ thể
```

Thông báo lỗi mới: *"Ngựa 'Black Star' đang tạm nghỉ đến 25/10: bầm móng trước phải."* Frontend đọc `trainingLocked`/`trainingLockReason` thẳng từ `GET /api/horses/{id}` (đã trả nguyên entity).

---

### P6. Khóa hành chính và khóa y tế trộn trong một cờ bằng chuỗi cố định

**Vấn đề.** Ngựa `CANDIDATE` chờ duyệt cũng bị đặt `trainingLocked = true` với lý do là chuỗi `"Admission pending trainer and manager review"`. Khi Manager duyệt, code mở khóa bằng cách **so đúng chuỗi đó**.

**Bằng chứng.** [AdmissionManagerReviewService.java:112](backend/src/main/java/com/rtms/backend/service/AdmissionManagerReviewService.java:112); chuỗi được ghi ở `CareScheduleService:386` và `:480`; ngoài ra `CareScheduleService:96` và `AdmissionGroomReviewService:156` cũng khóa ngựa mới nhập. **Ai sửa chữ trong câu thông báo thì ngựa khóa vĩnh viễn mà không lỗi nào báo.**

**Giải pháp đề xuất.** **Không lưu khóa hành chính nữa.** Khóa này đã được diễn đạt bằng `currentStatus == CANDIDATE`. Quy tắc duy nhất:

```
canTrain = (currentStatus == ELIGIBLE) AND NOT trainingLocked
```

Khi đó:
- `trainingLocked` chỉ còn nghĩa **y tế** (do Vet hoặc lịch URGENT đặt).
- **Xóa** khối mở-khóa-bằng-so-chuỗi ở `AdmissionManagerReviewService:111-119`.
- **Xóa** các chỗ đặt khóa "pending" ở `CareScheduleService:96`, `:386`, `:480` và `AdmissionGroomReviewService:156`.
- `RaceRegistrationService:191` dùng `canTrain()`.

---

### P7. `HorseStatus` trộn hai trục: vòng đời và sức khỏe

**Vấn đề.** `CANDIDATE → ELIGIBLE / REJECTED` là vòng đời; `INJURED`, `MONITORING`, `QUARANTINED` là sức khỏe. Trục sức khỏe đã có `trainingLocked` kèm lý do, nên ba giá trị đó thừa (và đều không được gán, xem 2.2).

**Giải pháp đề xuất.** `HorseStatus` chỉ còn **`{CANDIDATE, ELIGIBLE, REJECTED}`**. Bỏ `INJURED`, `MONITORING`, `QUARANTINED` khỏi enum, `CHECK chk_horses_current_status` (V37) và frontend (`StatusBadge.tsx`, `features/stable/types`). Dòng seed cũ ở V12 giữ nguyên (migration không sửa), nhưng dữ liệu hiện tại cần chuyển (xem mục 6).

---

### P8. Bốn endpoint cũ không ai gọi

**Vấn đề.** Chúng là đường tắt qua mặt luồng nghiệp vụ và làm rối mô hình. Đã grep frontend và test: **không có caller**.

| Endpoint | Vì sao bỏ |
|---|---|
| `POST /api/injury-records` (`InjuryRecordController/Service`, `CreateInjuryRecordRequest`) | Đặt `INJURED` và khóa, bỏ qua quyết định của Vet; luồng Vet mới thay thế |
| `PUT /api/horses/{id}/status` (`HorseService.updateHorseStatus`, `UpdateHorseStatusRequest`) | Cho đặt trạng thái tùy ý, qua mặt luồng duyệt đơn. Sau P7 `HorseStatus` chỉ do luồng admission đặt |
| `PUT /api/training-plans/{id}/status` (`updatePlanStatus`, `UpdatePlanStatusRequest`) | Đặt trạng thái plan tùy ý mà **không** hủy buổi tập (đặt `CANCELLED` bằng tay để lại workout mồ côi). Sau khi bỏ `PAUSED`, plan chỉ do hệ thống đổi trạng thái |
| `GET /api/horses/{id}/training-lock-status` | Thay bằng `Horse.canTrain()` (P5) |

**Giải pháp đề xuất.** Bỏ cả 4 cùng service, DTO và các test liên quan (`HorseServiceTest` có các test cascade ở dòng 112-146, chuyển sang kiểm `blockTraining`).

Quyền của các endpoint này (`HORSE_STATUS_EDIT`, `TRAINING_PLAN_UPDATE`, `INJURY_RECORD_CREATE`, `HORSE_TRAINING_LOCK_VIEW`) sẽ mồ côi và hiện trên trang quản lý phân quyền của Manager. Đề xuất **xóa luôn** ở migration (đã grep: chỉ đúng các endpoint trên dùng chúng; xem lại một lần trước khi xóa).

**Cần hỏi phụ trách Vet (một câu):** bảng `injury_records` (có tọa độ vùng cơ thể, `HorseBodyRegion`) có phải nền cho tính năng sơ đồ cơ thể đang/sẽ làm không? Nếu **không** → bỏ luôn entity, repository và bảng. Nếu **có** → giữ bảng và entity, chỉ bỏ endpoint và việc đặt `INJURED`.

---

### P9. Các giá trị chết còn lại

**Vấn đề.** `PAUSED`, `WorkoutStatus.IN_PROGRESS`, `LotStatus.IN_PROGRESS`, `CourseStatus.ARCHIVED` không bao giờ được gán.

**Giải pháp đề xuất.**
- Bỏ `TrainingPlanStatus.PAUSED`, `WorkoutStatus.IN_PROGRESS`, `LotStatus.IN_PROGRESS` khỏi enum và `CHECK` (ví dụ `chk_training_lots_status` ở V43).
- **Bỏ cả enum `CourseStatus` và cột `courses.status`**: sau khi bỏ `ARCHIVED` chỉ còn một giá trị, một cột luôn bằng `'ACTIVE'` là vô nghĩa. Bỏ luôn `CourseRepository.findByStatus` (không ai gọi). Khi nào cần lưu trữ khóa học thì thêm lại, đừng giữ chỗ trước.

---

### P10. `WorkoutType`: chết, và **frontend gửi giá trị backend không nhận** (lỗi đang tồn tại)

**Vấn đề.** Form tạo bài tập của Trainer có dropdown "Loại hình bài tập" với 5 lựa chọn, nhưng backend chỉ biết 2.

**Bằng chứng.**
- [SubjectList.tsx:233-241](frontend/src/features/training/components/SubjectList.tsx:233) gửi `REGULAR | GATE_PRACTICE | BREEZING | SWIMMING | RECOVERY`.
- Backend `WorkoutType` chỉ có `REGULAR` và `TRIAL_RUN`.
- Chọn 4 trong 5 lựa chọn đầu → backend không giải mã được enum → **lỗi 400**. Đây là lỗi lệch kiểu frontend–backend, cùng loại với các lỗi `ACKNOWLEDGED` và alert đã gặp.
- Không có logic nào đọc `workoutType` (chỉ lưu và trả ra trong `PlanWorkoutItemResponse`).

**Giải pháp đề xuất.** **Bỏ hoàn toàn:** enum `WorkoutType`, cột `subjects.workout_type` (và `chk_subjects_workout_type`), trường trong `CreateSubjectRequest`, `Subject`, `PlanWorkoutItemResponse.workoutType`, dropdown ở `SubjectList.tsx`, type ở `features/training/types`. Việc này vừa dọn thừa vừa sửa lỗi ngay.

---

### P11. `IntensityLevel`: **giữ lại có chủ đích**

**Hiện trạng.** Chỉ lưu và hiển thị, không logic nào đọc.

**Đề xuất: giữ.** Đây không phải "trạng thái trùng lặp" mà là **thuộc tính mô tả bài tập** (cùng loại với `surfaceType`, `targetDistanceMeters`), có giá trị hiển thị cho Trainer. Bỏ nó tốn migration và sửa `SubjectList`/`CourseList` mà không đổi hành vi. Ghi trong báo cáo: *"Cường độ là thuộc tính mô tả"*. Nếu nhóm vẫn muốn cắt, bỏ được độc lập với phần còn lại (bỏ cột `intensity_level`, enum, DTO và dropdown).

Danh mục con `Hồi phục & Thả lỏng` (đã seed ở V23) cũng giữ nguyên.

---

### P12. Tên gây nhầm: `HorseStatus.ELIGIBLE` và `RacingReadinessStatus.READY`

`ELIGIBLE` = đã nhận vào nuôi; `READY` = đủ điều kiện đăng ký đua. Hai từ gần nghĩa, dễ lẫn khi đọc.

**Giải pháp đề xuất.** **Không đổi tên** (đổi chạm quá nhiều nơi, không đáng). Thêm một dòng giải thích vào phần thuật ngữ của báo cáo.

---

## 4. MÔ HÌNH ĐÍCH (SAU KHI DỌN)

**Chỉ còn các enum sau cho phần này:**

| Enum | Giá trị | Ý nghĩa |
|---|---|---|
| `HorseStatus` | `CANDIDATE`, `ELIGIBLE`, `REJECTED` | Vòng đời ngựa |
| `ReviewDecision` | `APPROVED`, `REJECTED` | Quyết định của Groom, **Vet**, Manager |
| `AdmissionStatus` | `GROOM_REVIEW`, `WAITING_FOR_STALL`, `VET_REVIEW`, `TRAINER_REVIEW`, `MANAGER_REVIEW`, `APPROVED`, `REJECTED` | Tiến độ đơn |
| `TrainingPlanStatus` | `UPCOMING`, `ACTIVE`, `COMPLETED`, `CANCELLED` | Kế hoạch |
| `WorkoutStatus` / `LotStatus` | `SCHEDULED`, `COMPLETED`, `CANCELLED` | Buổi tập / lot |

**Dữ liệu y tế:**

| Trường | Ý nghĩa |
|---|---|
| `Horse.trainingLocked` | Đang tạm nghỉ y tế |
| `Horse.trainingLockReason` / `trainingLockReviewDate` / `trainingLockVetId` | Lý do, ngày xem xét lại, Vet quyết định |
| `HealthRecord.trainingBlocked` | Lịch sử mỗi lần khám: có chặn tập không |

**Quy tắc duy nhất:** `canTrain = (currentStatus == ELIGIBLE) AND NOT trainingLocked`.

**Vòng đời của khóa y tế:**

```
   [được tập] ──(Vet kết luận tạm nghỉ  HOẶC  tạo lịch URGENT)──► [tạm nghỉ đến <ngày>]
        ▲                                                            │
        │                                                            │ blockTraining() tự động:
        │                                                            │   • hủy buổi tập SCHEDULED tương lai
        │                                                            │   • hủy lot rỗng, trả khe giờ
        │                                                            │   • plan ACTIVE/UPCOMING → CANCELLED
        └────────────(Vet khám lại, kết luận "được tập")─────────────┘
                     (Trainer tạo plan mới sau đó)
```

**Ví dụ dữ liệu.** Black Star (horseId 12) đang tập khóa 12 buổi, còn 8 buổi `SCHEDULED`. Ngày 17/10 Groom báo bầm móng, lịch URGENT được tạo:

| Thời điểm | Trước đề xuất | Sau đề xuất |
|---|---|---|
| 17/10, tạo lịch URGENT | Ngựa bị khóa nhưng 8 buổi vẫn còn trong lot | `blockTraining` hủy 8 buổi, hủy lot rỗng, plan thành `CANCELLED` |
| 18/10, Vet khám: "nghỉ 7 ngày" | Chọn `RESTRICTED` hay `BLOCKED`? Vet bối rối vì UI hứa "tập hạn chế" nhưng thực tế khóa hết | Chọn **Tạm nghỉ** + `followUpDate = 25/10` + lý do |
| 19/10, Trainer thử lập plan | Lỗi *"KHOÁ HUẤN LUYỆN (trạng thái: ELIGIBLE)"* | Lỗi *"Black Star đang tạm nghỉ đến 25/10: bầm móng trước phải"* |
| 25/10, Vet khám lại: "được tập" | Mở khóa, nhưng plan cũ đã mất | Mở khóa; Trainer tạo plan mới, có thể ghép lại nhóm cũ |

---

## 5. VIỆC CẦN LÀM, CHIA THEO NGƯỜI PHỤ TRÁCH

### 5.1. Phụ trách Trainer
- `HorseTrainingPlanService`: dùng `assertCanTrain` thay đoạn kiểm khóa ở `:280-287`; bỏ `updatePlanStatus`.
- `HorseTrainingPlanController`: bỏ `PUT /{id}/status`; xóa `UpdatePlanStatusRequest`.
- `HorseService`: thêm `blockTraining` / `clearTrainingBlock` / `assertCanTrain`; bỏ `updateHorseStatus`; `HorseController` bỏ `PUT /{id}/status`; xóa `UpdateHorseStatusRequest`.
- Bỏ `InjuryRecord*` (xem P8), `TrainingLockStatus*`.
- Bỏ `WorkoutType`, `CourseStatus`: enum, cột, DTO (`CreateSubjectRequest`, `PlanWorkoutItemResponse`), entity (`Subject`, `Course`), `CourseRepository.findByStatus`.
- Frontend: bỏ dropdown `workoutType` và type ở `SubjectList.tsx`, `features/training/types`; đổi nhãn Vet decision ở `TrainerAdmissionDetailView.tsx`.
- Test: sửa `HorseServiceTest` (dòng 112-146), `HorseTrainingPlanServiceTest` nếu kiểm lock.

### 5.2. Phụ trách Vet
- `CareScheduleService`: dùng `blockTraining` / `clearTrainingBlock`; bỏ tính `vetDecision` (`:415-424`); bỏ nhánh `PENDING_RECHECK` (`:353-355`, `:450`); bỏ khóa "pending" (`:96`, `:386`, `:480`); bắt `followUpDate` + lý do khi tạm nghỉ.
- `AdmissionReviewService`: bỏ ánh xạ `VetDecision`↔`TrainingDecision` (`:117-124`).
- DTO: `VetReviewRequest`, `CompleteCareScheduleRequest`: thay `trainingDecision` bằng `blockTraining: boolean`; bỏ `decision` kiểu `VetDecision`.
- `HealthRecord`: thay `trainingDecision`/`vetDecision` bằng `trainingBlocked`. `AdmissionApplication`/`AdmissionDetailResponse`/`VetReviewResponse`: `vetDecision` dùng `ReviewDecision`.
- Frontend: `VetReviewForm.tsx` (hai lựa chọn thay vì ba, bỏ `legacyDecision`), `VetAdmissionQueue.tsx`, `admissions/types/index.ts` (bỏ `TrainingDecision`, `TrainingStatus`, `VetDecision`).

### 5.3. Phụ trách Admission / Manager
- `AdmissionManagerReviewService:111-119`: xóa khối mở khóa bằng so chuỗi.
- `AdmissionGroomReviewService:156`: bỏ `setTrainingStatus(BLOCKED)` khi tạo ngựa `CANDIDATE`.
- `ManagerAdmissionDetailView.tsx`: đổi nhãn đọc `trainingDecision` sang `trainingBlocked`.
- `RaceRegistrationService:191`: dùng `canTrain()`.

### 5.4. Dùng chung
- `Horse.java`: bỏ `trainingStatus` và hai setter ghi đè lẫn nhau.
- Frontend: `components/ui/StatusBadge.tsx`, `features/stable/types/index.ts` (bỏ ba `HorseStatus`), pipeline đơn (bỏ `PENDING_RECHECK`).

---

## 6. MIGRATION: PHÁC THẢO (CHỈ MINH HỌA, CHƯA PHẢI MÃ CUỐI)

> **Quy tắc bắt buộc:** không sửa migration cũ (Flyway lưu checksum, sửa là app không khởi động được). Chỉ thêm migration mới. Chạy thử trên **bản sao DB** trước. Số hiệu `V??`: thống nhất trong nhóm để khỏi trùng (hiện đã tới V63; `out-of-order=true` nên xếp số cẩn thận).

### Bước 0. Kiểm tra dữ liệu thật **trước khi viết migration**

```sql
SELECT current_status,  COUNT(*) FROM horses                GROUP BY 1;
SELECT training_status, COUNT(*) FROM horses                GROUP BY 1;
SELECT training_decision, vet_decision, COUNT(*) FROM health_records GROUP BY 1,2;
SELECT vet_decision,    COUNT(*) FROM admission_applications GROUP BY 1;
SELECT status,          COUNT(*) FROM admission_applications GROUP BY 1;
SELECT status,          COUNT(*) FROM horse_training_plans   GROUP BY 1;
SELECT status,          COUNT(*) FROM training_lots          GROUP BY 1;
SELECT status,          COUNT(*) FROM training_workouts      GROUP BY 1;
```
> Hibernate **ném lỗi khi đọc** một chuỗi mà enum không còn. Vì vậy phải chuyển dữ liệu **trước** rồi mới xóa giá trị khỏi enum.

### Bước 1. Chuyển dữ liệu, rồi siết ràng buộc

```sql
-- Ngựa: bảo toàn khóa y tế trước khi bỏ trainingStatus / INJURED
UPDATE horses SET training_locked = TRUE
 WHERE training_status <> 'ALLOWED' OR current_status = 'INJURED';
UPDATE horses SET current_status = 'ELIGIBLE'
 WHERE current_status IN ('INJURED','MONITORING','QUARANTINED');
ALTER TABLE horses DROP CONSTRAINT chk_horses_current_status;
ALTER TABLE horses ADD  CONSTRAINT chk_horses_current_status
  CHECK (current_status IN ('CANDIDATE','ELIGIBLE','REJECTED'));
ALTER TABLE horses DROP COLUMN training_status;

-- Khóa hành chính cũ (chuỗi cố định) không còn ý nghĩa
UPDATE horses SET training_locked = FALSE, training_lock_reason = NULL
 WHERE training_lock_reason = 'Admission pending trainer and manager review'
   AND training_lock_vet_id IS NULL;

-- Health record
ALTER TABLE health_records ADD COLUMN training_blocked BOOLEAN NOT NULL DEFAULT FALSE;
UPDATE health_records SET training_blocked = TRUE
 WHERE training_decision IS NOT NULL AND training_decision <> 'ALLOWED';
ALTER TABLE health_records DROP COLUMN training_decision;
ALTER TABLE health_records DROP COLUMN vet_decision;   -- constraint chk_health_record_vet_decision đã được V59 gỡ từ trước

-- Đơn nhập học
UPDATE admission_applications SET vet_decision = 'APPROVED' WHERE vet_decision = 'RECHECK_REQUIRED';
-- thay chk_admission_vet_decision (V52) bằng CHECK (vet_decision IN ('APPROVED','REJECTED'))
UPDATE admission_applications SET status = 'VET_REVIEW' WHERE status = 'PENDING_RECHECK';
-- viết lại CHECK trạng thái đơn (V52 dòng 12-23) bỏ 'PENDING_RECHECK'

-- Plan / lot / workout
UPDATE horse_training_plans SET status = 'CANCELLED' WHERE status = 'PAUSED';
UPDATE training_lots        SET status = 'SCHEDULED' WHERE status = 'IN_PROGRESS';
UPDATE training_workouts    SET status = 'SCHEDULED' WHERE status = 'IN_PROGRESS';
-- viết lại các CHECK tương ứng (tra tên thật bằng \d <bảng>)

-- Subject / Course
ALTER TABLE subjects DROP CONSTRAINT chk_subjects_workout_type;
ALTER TABLE subjects DROP COLUMN workout_type;
ALTER TABLE courses  DROP COLUMN status;
```

### Bước 2. Dọn quyền mồ côi
```sql
DELETE FROM role_permissions WHERE permission_id IN
  (SELECT id FROM permissions WHERE code IN
   ('HORSE_STATUS_EDIT','TRAINING_PLAN_UPDATE','INJURY_RECORD_CREATE','HORSE_TRAINING_LOCK_VIEW'));
DELETE FROM permissions WHERE code IN
  ('HORSE_STATUS_EDIT','TRAINING_PLAN_UPDATE','INJURY_RECORD_CREATE','HORSE_TRAINING_LOCK_VIEW');
```

### Thứ tự triển khai đề xuất (mỗi bước chạy được độc lập, test xanh trước khi sang bước sau)

| PR | Nội dung | Rủi ro |
|---|---|---|
| **1** | P10 (sửa lỗi dropdown workoutType) + P9 (giá trị chết) + P8 (bỏ endpoint không ai gọi) | Thấp, không đổi nghiệp vụ |
| **2** | P4 + P5 + P6 (`blockTraining`, `canTrain`, bỏ khóa hành chính) | Trung bình, chạm Vet và Manager |
| **3** | P1 + P2 + P3 + P7 (bỏ `RESTRICTED`, `TrainingStatus`, `TrainingDecision`, `VetDecision`, `HorseStatus` cũ) | Cao nhất, đổi DTO và frontend Vet |

PR 1 làm được ngay mà không phụ thuộc ai. Hai PR sau cần phụ trách Vet đồng ý và tham gia.

---

## 7. RỦI RO VÀ CÁCH GIẢM

| Rủi ro | Giảm bằng cách |
|---|---|
| Lệch kiểu frontend–backend khi đổi enum (đã gặp 4 lần trong dự án) | Mỗi PR sửa **cả hai phía cùng lúc**; chạy `npm run build` và `./mvnw test` trước khi merge |
| Dòng DB còn giữ giá trị đã xóa khỏi enum → app không đọc được | Chạy **Bước 0** trước; chuyển dữ liệu trước, xóa enum sau |
| Nhiều người cùng thêm migration → trùng số | Thống nhất dải số `V??` trước khi bắt đầu |
| Xung đột merge ở file Vet (`VetReviewForm.tsx` hơn 1600 dòng) | PR 3 chỉ làm khi phụ trách Vet đã chốt thời điểm, tránh sửa song song |
| Hủy plan khi URGENT nhưng Vet sau đó kết luận "được tập" | Chấp nhận (D4); nếu nhóm thấy quá tay thì đổi thành chỉ hủy buổi tập, giữ plan |

---

## 8. BẢNG XÁC NHẬN CỦA NHÓM

Mỗi người trả lời cho phần của mình (✔ đồng ý / ✘ không, kèm lý do).

| # | Hạng mục | Phụ trách | Ý kiến |
|---|---|---|---|
| D1 | Bỏ `RESTRICTED`, chỉ còn *được tập / tạm nghỉ có thời hạn*; ngày xem xét lại và lý do bắt buộc **chỉ khi tạm nghỉ**, kiểm ở service (V59 đã cố ý gỡ ràng buộc `follow_up` ở DB, ta không khôi phục) | Vet | ☐ |
| D2 | Gộp `VetDecision` vào `ReviewDecision`; bỏ `PENDING_RECHECK`; bỏ `HealthRecord.vetDecision` | Vet | ☐ |
| D3 | Thay `trainingDecision` bằng `blockTraining: boolean`; bỏ `TrainingStatus` và cột `horses.training_status` | Vet | ☐ |
| D4 | Hủy plan khi khóa, **kể cả** lúc tạo lịch URGENT (khóa phòng ngừa) | Vet + Trainer | ☐ |
| D5 | Bảng `injury_records` / sơ đồ cơ thể: còn dùng không? (không → bỏ luôn entity và bảng; có → giữ bảng, chỉ bỏ endpoint) | Vet | ☐ |
| D6 | Không lưu khóa hành chính; dùng `canTrain = ELIGIBLE && !trainingLocked`; xóa khối so chuỗi khi Manager duyệt | Manager / Admission | ☐ |
| D7 | `HorseStatus` chỉ còn `CANDIDATE, ELIGIBLE, REJECTED` | Tất cả | ☐ |
| D8 | Bỏ 4 endpoint cũ và 4 quyền mồ côi (P8) | Trainer | ☐ |
| D9 | Bỏ `WorkoutType` (đồng thời sửa lỗi dropdown) | Trainer | ☐ |
| D10 | Bỏ `CourseStatus`, `PAUSED`, `IN_PROGRESS` (plan/lot/workout) | Trainer | ☐ |
| D11 | **Giữ** `IntensityLevel` như thuộc tính mô tả | Trainer | ☐ |
| D12 | Thứ tự PR 1 → 2 → 3 và dải số migration | Tất cả | ☐ |

**Nếu cả nhóm đồng ý:** phụ trách Trainer báo lại cho trợ lý, bắt đầu từ PR 1 (không phụ thuộc ai) rồi lần lượt hai PR còn lại.

---

## PHỤ LỤC. CÁCH ĐÃ KIỂM CHỨNG

- "Không bao giờ được gán": `grep` toàn bộ `*.java`, `*.sql`, `*.ts`, `*.tsx` theo `Enum.GIÁ_TRỊ` và chuỗi `'GIÁ_TRỊ'`, loại trừ định nghĩa enum và chú thích.
- "Frontend không gọi": `grep` các đường dẫn endpoint trong `frontend/src` và `backend/src/test`.
- `RESTRICTED` ≡ `BLOCKED`: đọc `Horse.setTrainingStatus`, `HorseTrainingPlanService.createPlan`, `RaceRegistrationService`.
- Phát hiện lỗi `WorkoutType`: so sánh `SubjectList.tsx` với `enums/WorkoutType.java` khi chuẩn bị tài liệu này.
- Chưa chạy migration nào và chưa sửa dòng code nào. Mọi con số dòng có thể lệch nếu code đã đổi sau ngày viết.
