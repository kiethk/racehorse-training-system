# PLAN 05 — ĐỢT 1: LUỒNG TIẾP NHẬN NGỰA (TRAINER)

> Hướng dẫn thi công chi tiết cho **Đợt 1** trong [PLAN_04 — Lộ trình](PLAN_04_ROADMAP.md).
> Gồm cả phần **kiểm thử trên giao diện**.
>
> Phạm vi: **1 việc backend + 6 file frontend**. Không migration, không sửa entity.
> Đối chiếu trực tiếp với mã nguồn ngày 2026-09-26.

---

## MỤC LỤC

- [0. Tổng quan](#0-tổng-quan)
- [0.1. Quy ước frontend của nhóm](#01-quy-ước-frontend-của-nhóm--đọc-trước-khi-gõ-dòng-nào)
- [PHẦN A — Backend](#phần-a--backend)
  - [A1. Sửa `AdmissionSummaryResponse.java`](#a1-sửa-admissionsummaryresponsejava)
  - [A2. Sửa `AdmissionQueryService.java`](#a2-sửa-admissionqueryservicejava)
  - [A3. Chạy kiểm thử backend](#a3-chạy-kiểm-thử-backend)
- [PHẦN B — Frontend](#phần-b--frontend)
  - [B0. Chuẩn bị môi trường ⚠️](#b0-chuẩn-bị-môi-trường-️)
  - [B1. `types/trainer.ts`](#b1-typestrainerts-mới)
  - [B2. `services/trainerAdmissionService.ts`](#b2-servicestraineradmissionservicets-mới)
  - [B3. `TrainerQueueList.tsx`](#b3-trainerqueuelisttsx-mới)
  - [B4. `TrainerReviewPanel.tsx`](#b4-trainerreviewpaneltsx-mới)
  - [B5. `app/trainer/admissions/page.tsx`](#b5-apptraineradmissionspagetsx-mới)
  - [B6. `config/navigation.ts`](#b6-confignavigationts-sửa-1-dòng)
- [PHẦN C — Kiểm thử trên giao diện](#phần-c--kiểm-thử-trên-giao-diện)
  - [C1. Khởi động hệ thống](#c1-khởi-động-hệ-thống)
  - [C2. Dựng dữ liệu test](#c2-dựng-dữ-liệu-test)
  - [C3. Chín kịch bản kiểm thử](#c3-chín-kịch-bản-kiểm-thử)
  - [C4. Lỗi hay gặp và cách sửa](#c4-lỗi-hay-gặp-và-cách-sửa)
- [PHẦN D — Nghiệm thu và bàn giao](#phần-d--nghiệm-thu-và-bàn-giao)

---

## 0. Tổng quan

### Vì sao chỉ cần một việc backend

Tôi đã rà từng thao tác của Trainer trong luồng tiếp nhận với endpoint có thật:

| # | Thao tác | API | Quyền | TT |
|---|---|---|---|---|
| 1 | Xem danh sách đơn chờ mình | `GET /api/admissions?status=TRAINER_REVIEW` | `ADMISSION_APPLICATION_VIEW` | ✅ |
| 2 | Mở hồ sơ ứng viên (gộp 1 lời gọi) | `GET /api/admissions/{id}/trainer-view` | `ADMISSION_APPLICATION_VIEW` | ✅ |
| 3 | Xem giấy tờ | `GET /api/admissions/{id}/documents` | `ADMISSION_DOCUMENT_VIEW` | ✅ |
| 4 | Tải file giấy tờ | `GET /api/admissions/{id}/documents/{docId}/file` | `ADMISSION_DOCUMENT_VIEW` | ✅ |
| 5 | **Biết ngựa ở chuồng nào để xuống gặp** | `quarantineStallCode` trong chi tiết | — | ✅ |
| 6 | Nộp đánh giá | `POST /api/admissions/{id}/trainer-review` | `ADMISSION_APPLICATION_TRAINER_REVIEW` + `RACING_READINESS_ASSESSMENT_CREATE` | ✅ |
| 7 | Xem lại đánh giá đã nộp | `trainer-view` trả kèm `existingAssessment` | — | ✅ |
| 8 | Lịch sử đánh giá | `GET /api/admissions/horses/{horseId}/readiness-history` | `RACING_READINESS_ASSESSMENT_VIEW` | ✅ |

`V27` đã cấp `HEAD_TRAINER` đủ ba quyền admission, `V47` cấp thêm hai quyền đánh giá.

**Thiếu duy nhất:** màn hình **danh sách** chỉ có `quarantineStallId` (số `18`), không có `quarantineStallCode` (`Q3`). Đó là việc backend duy nhất của đợt này.

### Danh sách file

| Bước | File | Loại |
|---|---|---|
| A1 | `backend/.../dto/AdmissionSummaryResponse.java` | sửa |
| A2 | `backend/.../service/AdmissionQueryService.java` | sửa |
| B0 | `frontend/.env.local` | **tạo** |
| B1 | `frontend/src/features/admissions/types/trainer.ts` | **mới** |
| B1b | `frontend/src/features/admissions/types/index.ts` | sửa 1 dòng |
| B2 | `frontend/src/features/admissions/services/trainerAdmissionService.ts` | **mới** |
| B3 | `frontend/src/features/admissions/components/TrainerQueueList.tsx` | **mới** |
| B4 | `frontend/src/features/admissions/components/TrainerReviewPanel.tsx` | **mới** |
| B5 | `frontend/src/app/trainer/admissions/page.tsx` | **mới** |
| B6 | `frontend/src/config/navigation.ts` | sửa 1 dòng |

### Một lệch so với roadmap

PLAN_04 ghi 2 route (`/admissions` và `/admissions/[id]`). Tôi đổi sang **master-detail một trang**, vì `ManagerQueueList.tsx` trong dự án đã làm đúng kiểu đó. Bám mẫu có sẵn thì ít code hơn và nhóm đọc chéo dễ hơn.

---

## 0.1. Quy ước frontend của nhóm — đọc trước khi gõ dòng nào

Thư mục `frontend/` có 5 tài liệu quy định. Toàn bộ code trong PLAN này đã được chỉnh cho khớp:

| File | Nội dung |
|---|---|
| **`FRONTEND_GUIDE.md`** (259 dòng) | Tài liệu chính thức: kiến trúc, quy ước, Definition of Done |
| **`HANDOVER_CHECKLIST.md`** | Checklist trước khi bắt đầu và trước khi kết thúc |
| **`README.md`** | Stack, lệnh chạy, biến môi trường |
| **`AGENTS.md`** | Cảnh báo Next.js 16 có breaking change so với kiến thức phổ thông |
| `CLAUDE.md` | Chỉ trỏ tới `AGENTS.md` |

### Những điều ràng buộc trực tiếp tới Đợt 1

| Mục | Quy định | Áp dụng ở đâu trong PLAN này |
|---|---|---|
| §8 **CRITICAL** | Mọi request đi qua `services/api.ts`. Component **không** gọi `fetch()` | B2 — xem phần xung đột quy ước |
| §9 | `page.tsx` cực mỏng, bọc `RoleGuard > AppShell > PageContainer` | B5 |
| §11 | Dùng design token, **không** hardcode màu Tailwind, **không** hex | B3, B4 — toàn bộ dùng `var(--color-*)` |
| §12 | Bắt buộc xử lý đủ **4 trạng thái**: Loading · Error · Empty · Content | B3, B4 |
| §13 | Vô hiệu nút khi đang gửi, chặn gửi trùng. **Backend là người kiểm tra cuối** | B4 |
| §14 | Service đặt tên `camelCaseService.ts` | B2 |
| §15 | Kiểm tra vai trò ở frontend **chỉ để UX**, `@PreAuthorize` mới là bảo mật thật | B5, KB-9 |
| §17 | Definition of Done — có cả `npm run lint` và `npm run build` | D1 |

### Ba điều đã có sẵn trong code, không cần làm lại

- **Xác thực**: cookie HttpOnly `jwt_token`, `AuthContext` gọi `GET /api/auth/me` lúc khởi động. **Không** đọc JWT ở client, **không** lưu vào `localStorage` (§3).
- **Điều hướng theo vai trò**: `RoleGuard` tự chuyển hướng về `/login` nếu chưa đăng nhập, về trang chủ đúng vai trò nếu sai quyền (§4).
- **Design token**: `globals.css` có sẵn 32 token (§11).

### ⚠️ Next.js 16

`AGENTS.md` cảnh báo: *"This is NOT the Next.js you know"* — phiên bản này có breaking change so với kiến thức phổ biến. Tài liệu gốc nằm ở `frontend/node_modules/next/dist/docs/`.

Đợt 1 **không vướng** thay đổi nào của Next 16, vì:
- Không dùng route động (`[id]`), nên không đụng chuyện `params` giờ là `Promise`
- Không dùng Server Action, không fetch phía server
- Chỉ dùng `'use client'` + hook — phần này không đổi

Từ **Đợt 3** trở đi nếu làm route động thì phải đọc lại tài liệu đó.

---

# PHẦN A — Backend

## A1. Sửa `AdmissionSummaryResponse.java`

Thêm **một trường** ngay dưới `quarantineStallId`:

```java
    private Long quarantineStallId;
    private String quarantineStallCode;      // ← THÊM
```

Thêm tham số vào constructor — **đặt cuối danh sách**:

```java
    public AdmissionSummaryResponse(
            Long admissionId,
            AdmissionStatus status,
            String candidateName,
            String breed,
            LocalDate dateOfBirth,
            LocalDateTime submittedAt,
            Long quarantineStallId,
            String quarantineStallCode) {          // ← THÊM
        this.admissionId = admissionId;
        this.status = status;
        this.candidateName = candidateName;
        this.breed = breed;
        this.dateOfBirth = dateOfBirth;
        this.submittedAt = submittedAt;
        this.quarantineStallId = quarantineStallId;
        this.quarantineStallCode = quarantineStallCode;   // ← THÊM
    }
```

Thêm getter ở cuối file:

```java
    /**
     * Mã chuồng cách ly, ví dụ "Q3".
     *
     * Vì sao không dùng quarantineStallId: đó là khoá nội bộ, không in trên
     * biển chuồng. Trainer cầm điện thoại xuống khu cách ly cần mã người đọc
     * được. Không có trường này thì phải mở chi tiết từng đơn mới biết đi đâu.
     */
    public String getQuarantineStallCode() {
        return quarantineStallCode;
    }
```

### Vì sao phải đặt cuối constructor

Constructor này có 7 tham số, trong đó `candidateName` / `breed` cùng kiểu `String`, `admissionId` / `quarantineStallId` cùng kiểu `Long`. Chèn tham số mới vào **giữa** sẽ khiến mọi lời gọi cũ **vẫn biên dịch được nhưng gán nhầm giá trị** — kiểu lỗi tệ nhất vì trình biên dịch im lặng.

Thêm vào cuối thì lời gọi cũ thiếu tham số → lỗi biên dịch ngay → bạn biết chính xác phải sửa ở đâu.

---

## A2. Sửa `AdmissionQueryService.java`

`stableStallRepository` **đã được inject sẵn** trong constructor — không cần thêm dependency.

Thêm import:

```java
import com.rtms.backend.entity.StableStall;
```

Sửa `toSummaryResponse` (khoảng dòng 126):

```java
    private AdmissionSummaryResponse toSummaryResponse(
            AdmissionApplication admission) {

        CandidateHorseProfile candidate = candidateHorseProfileRepository
                .findByAdmissionId(admission.getId())
                .orElseThrow(() -> new IllegalStateException(
                        "Candidate horse profile not found"));

        // Mã chuồng cách ly cho màn hình danh sách của Trainer.
        // Đơn chưa được xếp chuồng (GROOM_REVIEW, WAITING_FOR_STALL) -> null.
        String stallCode = admission.getQuarantineStallId() == null
                ? null
                : stableStallRepository.findById(admission.getQuarantineStallId())
                        .map(StableStall::getStallCode)
                        .orElse(null);

        return new AdmissionSummaryResponse(
                admission.getId(),
                admission.getStatus(),
                candidate.getName(),
                candidate.getBreed(),
                candidate.getDateOfBirth(),
                admission.getSubmittedAt(),
                admission.getQuarantineStallId(),
                stallCode                                  // ← THÊM
        );
    }
```

### Về N+1 — cố ý không tối ưu

Hàm này chạy trong `.map(this::toSummaryResponse)` nên mỗi đơn tốn thêm một truy vấn. Ba lý do không gom batch:

1. **Hàm này vốn đã N+1** — `candidateHorseProfileRepository.findByAdmissionId` cũng gọi mỗi vòng. Thêm một truy vấn nữa không đổi bậc độ phức tạp.
2. **Danh sách bị chặn tự nhiên** bởi số đơn đang chờ duyệt — thực tế dưới 20.
3. Gom batch phải phá cấu trúc `.map()` hiện tại, **đụng code dùng chung** của Manager và Owner.

Khác hẳn `TrainingLotController.toResponse` mà ta đã tối ưu trước đây: chỗ đó số truy vấn bằng số lot **nhân** số ngựa mỗi lot, tăng theo quy mô trang trại.

> ⚠️ **`AdmissionSummaryResponse` là DTO dùng chung** với màn hình Manager và Owner. Thêm trường mới là thao tác *additive* nên không phá ai, nhưng vẫn **báo nhóm một tiếng trước khi đẩy**.

---

## A3. Chạy kiểm thử backend

```bash
cd backend && ./mvnw test
```

Phải vẫn **64/64 pass**.

Nếu có test nào gọi trực tiếp constructor `AdmissionSummaryResponse` thì nó sẽ đỏ vì thiếu tham số — sửa bằng cách thêm `null` vào cuối lời gọi.

Kiểm nhanh trên Swagger (`http://localhost:8080/swagger-ui.html`):

```
GET /api/admissions?status=TRAINER_REVIEW
```

Kỳ vọng mỗi phần tử có thêm:

```json
{
  "admissionId": 3,
  "status": "TRAINER_REVIEW",
  "candidateName": "QABALAH MERCURY",
  "quarantineStallId": 18,
  "quarantineStallCode": "Q3"
}
```

---

# PHẦN B — Frontend

## B0. Chuẩn bị môi trường ⚠️

**Đây là bước chặn — bỏ qua thì mọi lời gọi API đều hỏng.**

Thư mục `frontend/` hiện **chưa có `.env.local`**, chỉ có `.env.local.example`. Mà `services/api.ts` đọc:

```ts
const API_URL = process.env.NEXT_PUBLIC_API_URL;
```

Biến không tồn tại → `API_URL` là `undefined` → `fetch` gọi tới `undefined/api/admissions` → lỗi khó hiểu.

```bash
cd frontend
cp .env.local.example .env.local
```

Nội dung phải là:

```
NEXT_PUBLIC_API_URL=http://localhost:8080
```

> Next.js chỉ đọc file env **lúc khởi động**. Tạo file xong phải **restart `npm run dev`**, sửa file cũng vậy. Đây là nguyên nhân số một của lỗi "sửa rồi mà vẫn không chạy".

---

## B1. `types/trainer.ts` (mới)

`frontend/src/features/admissions/types/trainer.ts`

```ts
import type { AdmissionDetailResponse } from './index';

/**
 * Khớp enums/RacingReadinessStatus.java.
 * DB có CHECK constraint chk_rra_status chỉ nhận đúng 3 giá trị này.
 *
 * UNSUITABLE đổi tên từ NOT_READY (V47) vì tên cũ không phân biệt được với
 * NEEDS_MORE_TRAINING. Đây là kênh DUY NHẤT để Trainer báo hiệu "không nên
 * nhận con này" — Trainer không có quyền từ chối đơn.
 */
export type RacingReadinessStatus = 'READY' | 'NEEDS_MORE_TRAINING' | 'UNSUITABLE';

export interface RacingReadinessAssessment {
  id: number;
  horseId: number;
  readinessStatus: RacingReadinessStatus;
  /** LUÔN null ở bước tiếp nhận — ngựa đang cách ly, không đo được thể lực. */
  fitnessScore: number | null;
  conformationScore: number | null;
  temperamentScore: number | null;
  gaitQualityScore: number | null;
  estimatedMonthsToRace: number | null;
  assessmentDate: string;
  /** null ở bước tiếp nhận (CHECK chk_rra_valid_until). */
  validUntil: string | null;
  remarks: string | null;
  trainerId: number | null;
  /** NOT NULL = đánh giá lúc tiếp nhận. null = đánh giá định kỳ. */
  admissionId: number | null;
  createdAt: string;
}

export interface HorseSummary {
  id: number;
  name: string;
  breed: string | null;
  dateOfBirth: string | null;
  currentStatus: string;
  currentStallId: number | null;
  registryName: string | null;
  registrationNumber: string | null;
}

export interface TrainerAdmissionView {
  admission: AdmissionDetailResponse;
  /** null nếu bước Groom chưa tạo hồ sơ Horse. Khi đó KHÔNG nộp đánh giá được. */
  horse: HorseSummary | null;
  /**
   * Backend đang trả rỗng CỨNG — module Thú y chưa ghi dữ liệu.
   * Đây KHÔNG phải lỗi. Giao diện phải hiện "chưa có dữ liệu",
   * khi nhóm Thú y xong thì backend đổi 2 dòng, frontend không sửa gì.
   */
  healthRecords: unknown[];
  healthMetrics: unknown[];
  /** Khác null = đã đánh giá rồi -> form chuyển sang chỉ đọc. */
  existingAssessment: RacingReadinessAssessment | null;
}

export interface TrainerReviewRequest {
  readinessStatus: RacingReadinessStatus;
  conformationScore?: number | null;
  temperamentScore?: number | null;
  gaitQualityScore?: number | null;
  estimatedMonthsToRace?: number | null;
  remarks?: string | null;
}
```

### B1b. Sửa `types/index.ts`

Thêm một dòng vào interface đã có:

```ts
export interface AdmissionSummaryResponse {
  admissionId: number;
  status: AdmissionStatus;
  candidateName: string;
  breed: string;
  dateOfBirth: string;
  submittedAt: string;
  quarantineStallId: number | null;
  quarantineStallCode: string | null;   // ← THÊM
}
```

> **Ghi nhận, không sửa:** `AdmissionStatus` ở frontend đang khai báo thêm `'SUBMITTED'` và `'ADDITIONAL_INFORMATION_REQUIRED'`, trong khi enum backend **chỉ có 7 giá trị** (`GROOM_REVIEW`, `WAITING_FOR_STALL`, `VET_REVIEW`, `TRAINER_REVIEW`, `MANAGER_REVIEW`, `APPROVED`, `REJECTED`). Union thừa thành viên thì chỉ ảnh hưởng `switch` vét cạn, không gây lỗi khi đọc. Đây là file của người khác — báo nhóm, đừng tự sửa.

---

## B2. `services/trainerAdmissionService.ts` (mới)

> **Tên file theo `FRONTEND_GUIDE.md` §14** — service đặt tên `camelCaseService.ts`.
> Hai file `api.ts` và `ownerApi.ts` trong cùng thư mục có trước khi guide được
> viết nên không khớp quy ước; file mới thì làm đúng.

### ⚠️ Xung đột quy ước phải giải quyết TRƯỚC

`FRONTEND_GUIDE.md` đặt ra hai yêu cầu **mâu thuẫn nhau** với code hiện tại:

| Mục | Yêu cầu |
|---|---|
| §8 (CRITICAL) | *"All network requests must route through `services/api.ts`"* |
| §12 · §19 | Phải hiện lỗi; *"Silently failing when an API request returns an error"* là lỗi thường gặp cần tránh |

Nhưng `apiPost` trong `services/api.ts` **vứt bỏ body lỗi**:

```ts
if (!res.ok) {
  throw new Error(`API error: ${res.status}`);   // ← mất sạch message
}
```

Không thể vừa dùng `apiPost` vừa hiện được câu *"Đơn đang ở bước MANAGER_REVIEW…"*. Hai cách:

#### ✅ Cách A — Sửa `services/api.ts` (khuyến nghị)

Guide §16 nói *"Do not modify shared foundation files **unnecessarily**"* — chỗ này **cần thiết**, vì không sửa thì không thoả được §12 và §19.

Quan trọng hơn: trong **chính file đó**, `apiUpload` đã làm đúng rồi. Đây là sửa cho **nhất quán nội bộ**, không phải đẻ pattern mới:

```ts
// services/api.ts — áp cho CẢ apiGet và apiPost
const payload = await res.json().catch(() => null);
if (!res.ok) {
  throw new Error(payload?.message || `API error: ${res.status}`);
}
return payload as T;
```

Sửa xong thì service của bạn chỉ còn `apiGet` / `apiPost`, không có `fetch` nào — đúng §8 tuyệt đối. **Hỏi leader trước, đây là file nền của cả nhóm.**

#### Cách B — Helper cục bộ (chỉ khi nhóm chưa đồng ý)

Ngoại lệ **có ghi chú**, kèm `TODO` để sau này gỡ. Phải đặt trong **service**, tuyệt đối không đặt trong component (§17).

Phần code dưới đây viết theo **Cách B** để bạn chạy được ngay. Khi nhóm duyệt Cách A, xoá `postWithMessage` và đổi lời gọi sang `apiPost` — mất đúng 2 dòng.

---

`frontend/src/features/admissions/services/trainerAdmissionService.ts`

```ts
import { apiGet } from '@/services/api';
import type { AdmissionSummaryResponse } from '../types';
import type { TrainerAdmissionView, TrainerReviewRequest } from '../types/trainer';

/** Khớp dto/ApiResponse.java — { success, data, message }. */
interface ApiResponse<T> {
  success: boolean;
  data: T;
  message?: string;
}

const API_URL = process.env.NEXT_PUBLIC_API_URL;

/**
 * TODO(nhóm): xoá hàm này khi services/api.ts được sửa để giữ lại message lỗi.
 *
 * NGOẠI LỆ CÓ CHỦ ĐÍCH so với FRONTEND_GUIDE.md §8.
 *
 * apiPost dùng chung VỨT BỎ body lỗi — nó chỉ ném new Error("API error: 400").
 * Màn hình này bắt buộc hiện nguyên văn lỗi nghiệp vụ, ví dụ:
 *   "Đơn đang ở bước MANAGER_REVIEW, không phải TRAINER_REVIEW — không thể đánh giá!"
 * Dùng apiPost thì vi phạm §12 (hiện lỗi) và §19 (không được nuốt lỗi).
 *
 * apiUpload trong CHÍNH services/api.ts đã xử lý đúng — xem đề xuất ở PHẦN D.
 * Hàm này đặt trong tầng service, KHÔNG đặt trong component.
 */
async function postWithMessage<T>(path: string, body: unknown): Promise<T> {
  const res = await fetch(`${API_URL}${path}`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    credentials: 'include',      // bắt buộc — cookie jwt_token là HttpOnly
    body: JSON.stringify(body),
  });
  const payload = await res.json().catch(() => null);
  if (!res.ok) {
    throw new Error(payload?.message || `API error: ${res.status}`);
  }
  return payload as T;
}

export const trainerAdmissionsApi = {
  /** Hàng đợi đơn đang chờ Trainer đánh giá. */
  getQueue: async (): Promise<AdmissionSummaryResponse[]> => {
    const res = await apiGet<ApiResponse<AdmissionSummaryResponse[]>>(
      '/api/admissions?status=TRAINER_REVIEW',
    );
    return res.data;
  },

  /** Hồ sơ ứng viên — gộp mọi thứ Trainer cần vào MỘT lời gọi. */
  getView: async (id: number): Promise<TrainerAdmissionView> => {
    const res = await apiGet<ApiResponse<TrainerAdmissionView>>(
      `/api/admissions/${id}/trainer-view`,
    );
    return res.data;
  },

  /** Nộp đánh giá -> backend TỰ chuyển đơn sang MANAGER_REVIEW. */
  submitReview: async (id: number, body: TrainerReviewRequest): Promise<void> => {
    await postWithMessage<ApiResponse<unknown>>(
      `/api/admissions/${id}/trainer-review`,
      body,
    );
  },

  /** Link tải giấy tờ — mở bằng thẻ <a>, cookie jwt_token tự gửi kèm. */
  documentFileUrl: (admissionId: number, documentId: number): string =>
    `${API_URL}/api/admissions/${admissionId}/documents/${documentId}/file`,
};
```

---

## B3. `TrainerQueueList.tsx` (mới)

`frontend/src/features/admissions/components/TrainerQueueList.tsx`

```tsx
'use client';

import { useCallback, useEffect, useState } from 'react';

import { Panel, SectionTitle } from '@/components/ui/Panel';
import { Pill } from '@/components/ui/StatusBadge';
import { EmptyState, ListSkeleton } from '@/components/ui/states';
import { trainerAdmissionsApi } from '../services/trainerAdmissionService';
import type { AdmissionSummaryResponse } from '../types';
import { TrainerReviewPanel } from './TrainerReviewPanel';

export function TrainerQueueList() {
  const [queue, setQueue] = useState<AdmissionSummaryResponse[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [selectedId, setSelectedId] = useState<number | null>(null);

  const loadQueue = useCallback(async () => {
    try {
      setLoading(true);
      setError(null);
      const data = await trainerAdmissionsApi.getQueue();
      setQueue(data);
      // Giữ nguyên lựa chọn nếu đơn đó còn trong hàng đợi, ngược lại chọn đơn đầu.
      setSelectedId((prev) =>
        prev && data.some((a) => a.admissionId === prev)
          ? prev
          : data[0]?.admissionId ?? null,
      );
    } catch (err) {
      console.error('Không tải được hàng đợi tiếp nhận:', err);
      setError('Không tải được danh sách đơn. Kiểm tra kết nối rồi thử lại.');
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    loadQueue();
  }, [loadQueue]);

  if (loading) return <ListSkeleton rows={5} />;

  if (error) {
    return (
      <Panel padded>
        <EmptyState icon="alert-triangle" title="Lỗi tải dữ liệu" description={error} />
      </Panel>
    );
  }

  return (
    <div className="grid gap-4 lg:grid-cols-[320px_1fr]">
      {/* ---------- Cột trái: hàng đợi ---------- */}
      <Panel padded>
        <SectionTitle>Chờ đánh giá ({queue.length})</SectionTitle>

        {queue.length === 0 ? (
          <EmptyState
            icon="clipboard"
            title="Không có đơn nào"
            description="Chưa có hồ sơ nào chuyển tới bước đánh giá của Huấn luyện viên."
          />
        ) : (
          <ul className="mt-3 space-y-2">
            {queue.map((item) => {
              const active = selectedId === item.admissionId;
              return (
                <li key={item.admissionId}>
                  <button
                    type="button"
                    onClick={() => setSelectedId(item.admissionId)}
                    className={`w-full rounded-[var(--radius-md)] border p-3 text-left transition ${
                      active
                        ? 'border-[var(--color-primary)] bg-[var(--color-primary-subtle)]'
                        : 'border-[var(--color-border)] hover:bg-[var(--color-surface-muted)]'
                    }`}
                  >
                    <div className="text-[13px] font-semibold text-[var(--color-text-primary)]">
                      {item.candidateName}
                    </div>
                    <div className="text-[11px] text-[var(--color-text-muted)]">
                      {item.breed || 'Chưa rõ giống'}
                    </div>
                    <div className="mt-1.5">
                      {item.quarantineStallCode ? (
                        <Pill tone="isolated" icon="shield" size="sm">
                          Chuồng {item.quarantineStallCode}
                        </Pill>
                      ) : (
                        <Pill tone="neutral" size="sm">
                          Chưa xếp chuồng
                        </Pill>
                      )}
                    </div>
                  </button>
                </li>
              );
            })}
          </ul>
        )}
      </Panel>

      {/* ---------- Cột phải: hồ sơ + form ---------- */}
      {selectedId ? (
        <TrainerReviewPanel admissionId={selectedId} onSubmitted={loadQueue} />
      ) : (
        <Panel padded>
          <EmptyState
            icon="search"
            title="Chọn một hồ sơ"
            description="Chọn đơn ở cột trái để xem chi tiết và đánh giá."
          />
        </Panel>
      )}
    </div>
  );
}
```

**Điểm cốt lõi:** hiện `quarantineStallCode` ngay trên mỗi dòng danh sách. Đó chính là lý do tồn tại của PHẦN A — Trainer liếc một cái là biết phải xuống chuồng nào, không phải mở từng đơn.

`Pill tone="isolated"` là tone dành riêng cho trạng thái cách ly, đã có sẵn trong `StatusBadge.tsx` cùng icon `shield`.

---

## B4. `TrainerReviewPanel.tsx` (mới)

`frontend/src/features/admissions/components/TrainerReviewPanel.tsx`

```tsx
'use client';

import { useCallback, useEffect, useState } from 'react';

import { Button } from '@/components/ui/Button';
import { FieldLabel, Panel, SectionTitle } from '@/components/ui/Panel';
import { Pill } from '@/components/ui/StatusBadge';
import { DetailSkeleton, EmptyState } from '@/components/ui/states';
import { trainerAdmissionsApi } from '../services/trainerAdmissionService';
import type {
  RacingReadinessStatus,
  TrainerAdmissionView,
  TrainerReviewRequest,
} from '../types/trainer';

const READINESS_OPTIONS: {
  value: RacingReadinessStatus;
  label: string;
  hint: string;
}[] = [
  {
    value: 'READY',
    label: 'Sẵn sàng thi đấu',
    hint: 'Hiếm gặp với ngựa mới nhập — chỉ chọn khi thật sự đã có nền tảng',
  },
  {
    value: 'NEEDS_MORE_TRAINING',
    label: 'Cần huấn luyện thêm',
    hint: 'Lựa chọn thường gặp nhất với ngựa mới',
  },
  {
    value: 'UNSUITABLE',
    label: 'Không phù hợp',
    hint: 'Kênh duy nhất để báo Quản lý rằng không nên nhận con này',
  },
];

const MIN_REMARKS = 20;

export function TrainerReviewPanel({
  admissionId,
  onSubmitted,
}: {
  admissionId: number;
  onSubmitted: () => void;
}) {
  const [view, setView] = useState<TrainerAdmissionView | null>(null);
  const [loading, setLoading] = useState(true);
  const [loadError, setLoadError] = useState<string | null>(null);
  const [submitting, setSubmitting] = useState(false);
  const [formError, setFormError] = useState<string | null>(null);

  const [form, setForm] = useState<TrainerReviewRequest>({
    readinessStatus: 'NEEDS_MORE_TRAINING',
    conformationScore: null,
    temperamentScore: null,
    gaitQualityScore: null,
    estimatedMonthsToRace: null,
    remarks: '',
  });

  const load = useCallback(async () => {
    try {
      setLoading(true);
      setLoadError(null);
      setFormError(null);
      setView(await trainerAdmissionsApi.getView(admissionId));
    } catch (err) {
      console.error('Không tải được hồ sơ:', err);
      setLoadError('Không tải được hồ sơ ứng viên.');
    } finally {
      setLoading(false);
    }
  }, [admissionId]);

  useEffect(() => {
    load();
  }, [load]);

  if (loading) {
    return (
      <Panel padded>
        <DetailSkeleton />
      </Panel>
    );
  }

  if (loadError || !view) {
    return (
      <Panel padded>
        <EmptyState
          icon="alert-triangle"
          title="Lỗi"
          description={loadError ?? 'Không có dữ liệu.'}
        />
      </Panel>
    );
  }

  const { admission, horse, healthRecords, existingAssessment } = view;
  const readOnly = existingAssessment !== null;
  const horseMissing = horse === null;

  const setNum =
    (key: keyof TrainerReviewRequest) =>
    (raw: string) =>
      setForm((f) => ({ ...f, [key]: raw === '' ? null : Number(raw) }));

  async function handleSubmit() {
    setFormError(null);

    const remarks = form.remarks?.trim() ?? '';
    if (remarks.length < MIN_REMARKS) {
      setFormError(
        `Nhận xét chuyên môn phải có ít nhất ${MIN_REMARKS} ký tự (hiện ${remarks.length}).`,
      );
      return;
    }

    try {
      setSubmitting(true);
      await trainerAdmissionsApi.submitReview(admissionId, { ...form, remarks });
      await load();    // nạp lại -> existingAssessment khác null -> form khoá
      onSubmitted();   // báo danh sách bỏ đơn này khỏi hàng đợi
    } catch (err) {
      setFormError(err instanceof Error ? err.message : 'Nộp đánh giá thất bại.');
    } finally {
      setSubmitting(false);
    }
  }

  return (
    <Panel padded>
      {/* ============ HỒ SƠ ỨNG VIÊN ============ */}
      <SectionTitle>Hồ sơ ứng viên</SectionTitle>
      <div className="mt-2 grid gap-3 sm:grid-cols-2 lg:grid-cols-3">
        <Field label="Tên" value={admission.candidate?.name} />
        <Field label="Giống" value={admission.candidate?.breed} />
        <Field label="Ngày sinh" value={admission.candidate?.dateOfBirth} />
        <Field label="Số đăng ký (UELN)" value={admission.candidate?.registrationNumber} />
        <Field label="Cha (sire)" value={admission.candidate?.sireName} />
        <Field label="Mẹ (dam)" value={admission.candidate?.damName} />
      </div>

      <div className="mt-3 flex flex-wrap items-center gap-2">
        <Pill tone="isolated" icon="shield">
          Khu cách ly — chuồng {admission.quarantineStallCode ?? 'chưa xếp'}
        </Pill>
        {horse && (
          <Pill tone="info" icon="horse">
            Hồ sơ ngựa #{horse.id} · {horse.currentStatus}
          </Pill>
        )}
      </div>

      {horseMissing && (
        <p className="mt-3 rounded-[var(--radius-md)] bg-[var(--color-warning-soft)] p-2 text-[12px] text-[var(--color-warning)]">
          Đơn này chưa gắn hồ sơ chiến mã. Bước Groom phải tạo hồ sơ và xếp chuồng
          cách ly trước khi Huấn luyện viên đánh giá được.
        </p>
      )}

      {/* ============ GIẤY TỜ ============ */}
      <div className="mt-5">
        <SectionTitle>Giấy tờ kèm theo</SectionTitle>
      </div>
      {admission.documents.length === 0 ? (
        <p className="mt-1 text-[12px] text-[var(--color-text-muted)]">
          Không có giấy tờ nào.
        </p>
      ) : (
        <ul className="mt-2 space-y-1">
          {admission.documents.map((doc) => (
            <li key={doc.id} className="text-[12px]">
              <a
                className="text-[var(--color-primary)] underline"
                href={trainerAdmissionsApi.documentFileUrl(admissionId, doc.id)}
                target="_blank"
                rel="noopener noreferrer"
              >
                {doc.documentType}
              </a>
              {doc.note && (
                <span className="text-[var(--color-text-muted)]"> — {doc.note}</span>
              )}
            </li>
          ))}
        </ul>
      )}

      {/* ============ DỮ LIỆU THÚ Y ============ */}
      <div className="mt-5">
        <SectionTitle>Dữ liệu thú y</SectionTitle>
      </div>
      {healthRecords.length === 0 ? (
        <p className="mt-1 text-[12px] text-[var(--color-text-muted)]">
          Chưa có dữ liệu khám. Module Thú y đang được xây dựng.
        </p>
      ) : (
        <p className="mt-1 text-[12px]">{healthRecords.length} bản ghi khám</p>
      )}

      {/* ============ ĐÁNH GIÁ ============ */}
      <div className="mt-5">
        <SectionTitle>
          {readOnly ? 'Đánh giá đã nộp' : 'Đánh giá của Huấn luyện viên'}
        </SectionTitle>
      </div>

      {readOnly && existingAssessment && (
        <div className="mt-2 space-y-1 rounded-[var(--radius-md)] bg-[var(--color-surface-muted)] p-3 text-[12px]">
          <Row label="Mức sẵn sàng" value={existingAssessment.readinessStatus} />
          <Row label="Dáng vóc" value={fmtScore(existingAssessment.conformationScore)} />
          <Row label="Tính nết" value={fmtScore(existingAssessment.temperamentScore)} />
          <Row label="Bước đi" value={fmtScore(existingAssessment.gaitQualityScore)} />
          <Row
            label="Ước tính"
            value={
              existingAssessment.estimatedMonthsToRace != null
                ? `${existingAssessment.estimatedMonthsToRace} tháng`
                : '—'
            }
          />
          <Row label="Ngày đánh giá" value={existingAssessment.assessmentDate} />
          <div className="mt-2 whitespace-pre-wrap text-[var(--color-text-secondary)]">
            {existingAssessment.remarks}
          </div>
        </div>
      )}

      {!readOnly && (
        <div className="mt-3 space-y-4">
          {/* --- Mức sẵn sàng --- */}
          <div>
            <FieldLabel>Mức độ sẵn sàng thi đấu</FieldLabel>
            <div className="mt-1.5 space-y-1.5">
              {READINESS_OPTIONS.map((opt) => (
                <label key={opt.value} className="flex items-start gap-2">
                  <input
                    type="radio"
                    name="readiness"
                    className="mt-1"
                    checked={form.readinessStatus === opt.value}
                    onChange={() =>
                      setForm((f) => ({ ...f, readinessStatus: opt.value }))
                    }
                  />
                  <span>
                    <span className="text-[13px] text-[var(--color-text-primary)]">
                      {opt.label}
                    </span>
                    <span className="block text-[11px] text-[var(--color-text-muted)]">
                      {opt.hint}
                    </span>
                  </span>
                </label>
              ))}
            </div>
          </div>

          {/* --- Ba điểm quan sát --- */}
          <div className="grid gap-3 sm:grid-cols-3">
            <ScoreInput
              label="Dáng vóc"
              value={form.conformationScore}
              onChange={setNum('conformationScore')}
            />
            <ScoreInput
              label="Tính nết"
              value={form.temperamentScore}
              onChange={setNum('temperamentScore')}
            />
            <ScoreInput
              label="Bước đi"
              value={form.gaitQualityScore}
              onChange={setNum('gaitQualityScore')}
            />
          </div>

          {/* --- Ước tính thời gian --- */}
          <div>
            <FieldLabel>Ước tính số tháng nữa đủ điều kiện đăng ký giải</FieldLabel>
            <input
              type="number"
              min={0}
              max={60}
              className="mt-1 w-32 rounded-[var(--radius-md)] border border-[var(--color-border-strong)] bg-[var(--color-surface)] px-2 py-1 text-[13px]"
              value={form.estimatedMonthsToRace ?? ''}
              onChange={(e) => setNum('estimatedMonthsToRace')(e.target.value)}
            />
          </div>

          {/* --- Nhận xét --- */}
          <div>
            <FieldLabel>Nhận xét chuyên môn</FieldLabel>
            <textarea
              rows={5}
              placeholder="Dáng vóc cân đối, vai dốc tốt. Tính nết bình tĩnh khi dắt tay. Bước đi đều nhưng chân sau hơi ngắn sải..."
              className="mt-1 w-full rounded-[var(--radius-md)] border border-[var(--color-border-strong)] bg-[var(--color-surface)] px-2 py-1.5 text-[13px]"
              value={form.remarks ?? ''}
              onChange={(e) => setForm((f) => ({ ...f, remarks: e.target.value }))}
            />
            <div className="mt-1 text-[11px] text-[var(--color-text-muted)]">
              {form.remarks?.trim().length ?? 0}/{MIN_REMARKS} ký tự tối thiểu
            </div>
          </div>

          {/* --- Giải thích vì sao không có ô thể lực --- */}
          <p className="rounded-[var(--radius-md)] bg-[var(--color-warning-soft)] p-2 text-[11px] text-[var(--color-warning)]">
            Điểm thể lực để trống — ngựa đang cách ly nên không đưa ra đường chạy
            chung để đo được. Chỉ số này sẽ chấm ở lần đánh giá định kỳ sau khi
            ngựa chính thức nhập trại.
          </p>

          {formError && (
            <p className="rounded-[var(--radius-md)] bg-[var(--color-danger-soft)] p-2 text-[12px] text-[var(--color-danger)]">
              {formError}
            </p>
          )}

          <div className="flex items-center gap-3">
            <Button
              variant="primary"
              disabled={submitting || horseMissing}
              onClick={handleSubmit}
            >
              {submitting ? 'Đang nộp...' : 'Nộp đánh giá'}
            </Button>
            <span className="text-[11px] text-[var(--color-text-muted)]">
              Nộp xong đơn tự chuyển sang bước Quản lý duyệt. Không sửa lại được.
            </span>
          </div>
        </div>
      )}
    </Panel>
  );
}

/* ---------------- Thành phần phụ ---------------- */

function fmtScore(v: number | null): string {
  return v == null ? '—' : `${v}/10`;
}

function Field({ label, value }: { label: string; value?: string | null }) {
  return (
    <div>
      <FieldLabel>{label}</FieldLabel>
      <div className="text-[13px] text-[var(--color-text-primary)]">{value || '—'}</div>
    </div>
  );
}

function Row({ label, value }: { label: string; value: string }) {
  return (
    <div className="flex justify-between gap-4">
      <span className="text-[var(--color-text-muted)]">{label}</span>
      <span className="text-[var(--color-text-primary)]">{value}</span>
    </div>
  );
}

function ScoreInput({
  label,
  value,
  onChange,
}: {
  label: string;
  value: number | null | undefined;
  onChange: (raw: string) => void;
}) {
  return (
    <div>
      <FieldLabel>{label} (0–10)</FieldLabel>
      <input
        type="number"
        min={0}
        max={10}
        step={0.5}
        className="mt-1 w-full rounded-[var(--radius-md)] border border-[var(--color-border-strong)] bg-[var(--color-surface)] px-2 py-1 text-[13px]"
        value={value ?? ''}
        onChange={(e) => onChange(e.target.value)}
      />
    </div>
  );
}
```

### Năm quy tắc nghiệp vụ được cài vào giao diện

| Quy tắc | Nguồn | Cài ở đâu |
|---|---|---|
| **Không có nút Duyệt / Từ chối** | `V37` xoá cột `trainer_decision` | Chỉ có một nút *Nộp đánh giá* |
| **Nộp một lần duy nhất** | Service chặn nếu đơn rời `TRAINER_REVIEW` | `readOnly = existingAssessment !== null`, nạp lại sau khi nộp |
| **`fitnessScore` luôn null** | Ngựa đang cách ly | Không render ô nhập + dòng giải thích màu vàng |
| **Ba mức sẵn sàng** | CHECK `chk_rra_status` | Radio với đúng 3 lựa chọn, kèm gợi ý ý nghĩa |
| **Phải có hồ sơ Horse trước** | Service ném lỗi nếu `horseId` null | `disabled={horseMissing}` + cảnh báo |

### ⚠️ Về luật "nhận xét tối thiểu 20 ký tự"

`FRONTEND_GUIDE.md` §13 nói rõ: ***"Backend remains the final validator. Do not duplicate complex backend business rules in frontend validation."***

Luật 20 ký tự này **backend không hề có** — `remarks` là `VARCHAR(3000)` nullable, gọi thẳng API với `remarks: ""` vẫn lọt. Nên phải hiểu đúng bản chất:

- Đây là **guard rail về trải nghiệm**, khuyến khích Trainer viết nhận xét có nội dung
- **Không phải** quy tắc nghiệp vụ, và **không** bảo đảm được gì
- Nó không vi phạm §13 vì không nhân bản luật của backend — nhưng cũng **không thay thế được** luật ở backend

**Nếu nhóm muốn đây là quy tắc thật**, phải thêm vào `AdmissionTrainerReviewService.completeAssessment`:

```java
if (request.getRemarks() == null || request.getRemarks().trim().length() < 20) {
    throw new IllegalArgumentException(
            "Nhận xét chuyên môn phải có ít nhất 20 ký tự!");
}
```

Lúc đó phần kiểm ở frontend trở thành "phản hồi sớm cho người dùng", đúng tinh thần §13. Tôi **không** đưa vào PHẦN A vì đây là quyết định nghiệp vụ, bạn chốt với leader.

---

## B5. `app/trainer/admissions/page.tsx` (mới)

`FRONTEND_GUIDE.md` §9 yêu cầu `page.tsx` phải **cực mỏng**, và §5 bắt buộc bọc trong `AppShell`. Tôi bám đúng khuôn của `app/manager/management/page.tsx`:

```tsx
import { Metadata } from 'next';
import { RoleGuard } from '@/components/auth/RoleGuard';
import { AppShell } from '@/components/layout/AppShell';
import { PageContainer } from '@/components/layout/PageContainer';
import { TrainerQueueList } from '@/features/admissions/components/TrainerQueueList';

export const metadata: Metadata = {
  title: 'Tiếp nhận chiến mã | Huấn luyện viên',
};

export default function TrainerAdmissionsPage() {
  return (
    <RoleGuard allowedRoles={['HEAD_TRAINER']}>
      <AppShell>
        <PageContainer>
          <TrainerQueueList />
        </PageContainer>
      </AppShell>
    </RoleGuard>
  );
}
```

### Bốn điểm bắt buộc đúng

| # | Điều | Vì sao |
|---|---|---|
| 1 | **KHÔNG có `'use client'`** | Page là server component. Có `'use client'` thì **không export được `metadata`**. `RoleGuard` tự khai `'use client'` bên trong nên vẫn chạy được |
| 2 | **Phải có `AppShell`** | §5 — nó render `TopNav` lấy menu từ `config/navigation.ts`. Thiếu nó thì trang mất hẳn thanh điều hướng. Mọi page khác trong dự án đều bọc |
| 3 | **Không có UI nào trong page** | §9 — tiêu đề chuyển vào `TrainerQueueList` |
| 4 | **Thư mục route viết thường** | §14 — `app/trainer/admissions/` |

### Kéo theo: thêm tiêu đề vào `TrainerQueueList`

Vì page không còn chứa UI, chèn khối này vào **ngay đầu** phần `return` của `TrainerQueueList`, bọc tất cả trong một `<div>`:

```tsx
  return (
    <div>
      <div className="mb-4">
        <h1 className="text-[18px] font-semibold tracking-tight text-[var(--color-text-primary)]">
          Tiếp nhận chiến mã
        </h1>
        <p className="text-[12px] text-[var(--color-text-secondary)]">
          Đánh giá tiềm năng thi đấu của ngựa ứng viên đang trong khu cách ly.
        </p>
      </div>

      <div className="grid gap-4 lg:grid-cols-[320px_1fr]">
        {/* ... nội dung như ở B3 ... */}
      </div>
    </div>
  );
```

Nhớ làm tương tự cho hai nhánh trả về sớm (`loading` và `error`), hoặc chấp nhận hai nhánh đó không có tiêu đề.

---

## B6. `config/navigation.ts` (sửa 1 dòng)

Mục `admissions` của `HEAD_TRAINER` hiện **không có `href`** nên đang là placeholder bị khoá:

```ts
    case 'HEAD_TRAINER':
      return [
        { id: 'dashboard', label: 'Dashboard', href: dashboardHref },
        { id: 'admissions', label: 'Admissions', href: '/trainer/admissions' },  // ← THÊM href
        { id: 'training', label: 'Training' },
        { id: 'horses', label: 'Horses' },
        { id: 'racing', label: 'Racing' },
      ];
```

Chú thích trong chính file đó ghi *"Only implemented features have a valid href"* — nay mục này đã được cài nên thêm href là đúng quy ước.

---

# PHẦN C — Kiểm thử trên giao diện

## C1. Khởi động hệ thống

Cần **ba** thứ chạy song song. Mở ba terminal.

### 1 · Cơ sở dữ liệu

```bash
docker compose up -d
```

Kiểm tra container sống và đúng cổng **5433**:

```bash
docker compose ps
```

### 2 · Backend

```bash
cd backend && ./mvnw spring-boot:run
```

Chờ tới dòng `Started BackendApplication`. Xác nhận Flyway chạy tới **V48** trở lên.

Mở `http://localhost:8080/swagger-ui.html` để chắc chắn backend sống.

### 3 · Frontend

```bash
cd frontend
cp .env.local.example .env.local     # nếu chưa làm ở B0
npm install                          # lần đầu
npm run dev
```

Mở `http://localhost:3000`.

> **Thứ tự quan trọng:** backend phải lên trước, nếu không lời gọi API đầu tiên của frontend sẽ lỗi kết nối và bạn sẽ tưởng code sai.

---

## C2. Dựng dữ liệu test

Cần **ít nhất một đơn ở trạng thái `TRAINER_REVIEW` đã có `horse_id`**.

### Cách 1 — Chạy luồng thật (khuyến nghị)

Đúng nghiệp vụ nhất, và tiện thể kiểm luôn phần của các bạn khác.

Tài khoản test (từ `V10__seed_test_accounts.sql` và `V3__seed_roles.sql`):

| Vai trò | Email |
|---|---|
| Quản lý | `admin@rtms.com` |
| Huấn luyện viên | `trainer@rtms.com` |
| Thú y | `vet@rtms.com` |
| Chăm sóc | `groom@rtms.com` |

> Mật khẩu **không được ghi trong `SETUP_NOTES.md`** — dùng mật khẩu bạn vẫn đăng nhập khi test Swagger. Nếu quên, hỏi leader hoặc đổi hash trong DB. Nên bổ sung mục này vào `SETUP_NOTES.md` cho cả nhóm.

Bốn bước, làm trên Swagger cho nhanh (hoặc trên giao diện nếu màn hình đã có):

| Bước | Đăng nhập | Gọi | Kết quả |
|---|---|---|---|
| 1 | owner | `POST /api/owner/admissions` | Đơn mới, `status = GROOM_REVIEW` |
| 2 | groom | `POST /api/admissions/{id}/groom-review` body `{"decision":"APPROVED","feedback":"..."}` | → `WAITING_FOR_STALL` hoặc `VET_REVIEW` |
| 2b | groom | `POST /api/admissions/{id}/quarantine-allocation` (nếu đang `WAITING_FOR_STALL`) | Xếp chuồng Q, → `VET_REVIEW` |
| 3 | vet | `POST /api/admissions/{id}/vet-review` body `{"decision":"APPROVED","feedback":"...","physicalExamConfirmed":true}` | → **`TRAINER_REVIEW`** ✅ |

Lưu ý `registrationNumber` (UELN) phải đúng **15 ký tự chữ-số** hoặc để chuỗi rỗng, nếu không validator chặn ở bước 1.

### Cách 2 — Kiểm tra bằng SQL xem đã có sẵn chưa

```sql
SELECT a.id,
       a.status,
       a.horse_id,
       a.quarantine_stall_id,
       s.stall_code,
       c.name AS candidate_name
FROM admission_applications a
LEFT JOIN stable_stalls s ON s.id = a.quarantine_stall_id
LEFT JOIN candidate_horse_profiles c ON c.admission_id = a.id
ORDER BY a.id DESC;
```

Đơn dùng được phải thoả **cả ba**:

- `status = 'TRAINER_REVIEW'`
- `horse_id IS NOT NULL` — thiếu thì nộp đánh giá sẽ bị chặn
- `quarantine_stall_id IS NOT NULL` — thiếu thì cột mã chuồng hiện "Chưa xếp chuồng"

### Cách 3 — Đẩy thẳng một đơn có sẵn về `TRAINER_REVIEW`

Dùng khi bạn đã có đơn nhưng nó đã trôi qua bước Trainer rồi. Thay `<ID>` bằng id thật:

```sql
-- Xoá bản đánh giá cũ (nếu có) để form quay lại chế độ nhập
DELETE FROM racing_readiness_assessments WHERE admission_id = <ID>;

UPDATE admission_applications
SET status             = 'TRAINER_REVIEW',
    trainer_id         = NULL,
    trainer_feedback   = NULL,
    trainer_reviewed_at = NULL,
    updated_at         = NOW()
WHERE id = <ID>;
```

> Phải xoá dòng trong `racing_readiness_assessments` trước, vì unique index `uq_rra_admission` chỉ cho **một** bản đánh giá mỗi đơn. Không xoá thì nộp lại sẽ lỗi trùng khoá.

---

## C3. Chín kịch bản kiểm thử

### KB-1 · Vào được màn hình

1. `http://localhost:3000` → đăng nhập `trainer@rtms.com`
2. Nhìn thanh điều hướng trên cùng

**Kỳ vọng:** mục **Admissions** bấm được (không bị xám), dẫn tới `/trainer/admissions`.

> Nếu vẫn xám: chưa sửa `navigation.ts`, hoặc chưa restart `npm run dev`.

### KB-2 · Hàng đợi hiện mã chuồng ← *điểm mấu chốt của đợt này*

**Kỳ vọng:**
- Tiêu đề *"Chờ đánh giá (N)"* với N đúng bằng số đơn ở `TRAINER_REVIEW`
- Mỗi dòng: tên ngựa, giống, và một **Pill tím nhạt "Chuồng Q3"**
- Đơn đầu tiên tự được chọn, viền xanh

**Chụp màn hình bước này để đưa vào báo cáo** — đây là bằng chứng của BE-1.1.

> Nếu Pill hiện "Chưa xếp chuồng" với **mọi** đơn: mở DevTools → Network → xem response của `/api/admissions?status=TRAINER_REVIEW`. Có `quarantineStallCode` không? Không có nghĩa backend chưa build lại.

### KB-3 · Hồ sơ ứng viên đầy đủ

Bấm một đơn ở cột trái.

**Kỳ vọng ở cột phải:**
- Sáu ô: Tên, Giống, Ngày sinh, UELN, Cha, Mẹ — ô trống hiện `—` chứ không phải `null`
- Pill *"Khu cách ly — chuồng Q3"*
- Pill *"Hồ sơ ngựa #7 · CANDIDATE"*
- Mục **Dữ liệu thú y** hiện *"Chưa có dữ liệu khám"* ← **đúng, không phải lỗi**

### KB-4 · Tải giấy tờ

Bấm một link trong mục *Giấy tờ kèm theo*.

**Kỳ vọng:** tab mới mở, file tải về hoặc hiển thị.

> File đi kèm cookie `jwt_token` (HttpOnly) nên thẻ `<a>` hoạt động bình thường. Nếu ra **403**, kiểm tra `ADMISSION_DOCUMENT_VIEW` của `HEAD_TRAINER` trong `V27`.

### KB-5 · Chặn ở client — nhận xét quá ngắn

1. Chọn *Cần huấn luyện thêm*
2. Gõ `"ok"` vào ô nhận xét
3. Bấm **Nộp đánh giá**

**Kỳ vọng:** hộp đỏ *"Nhận xét chuyên môn phải có ít nhất 20 ký tự (hiện 2)."*, **không** có request nào trong tab Network.

### KB-6 · Nộp thành công

1. Chọn *Cần huấn luyện thêm*
2. Dáng vóc `7.5`, Tính nết `8`, Bước đi `6.5`
3. Ước tính `4`
4. Nhận xét đủ dài, ví dụ:
   > *Dáng vóc cân đối, vai dốc tốt. Tính nết bình tĩnh, dễ tiếp cận khi dắt tay. Bước đi đều nhưng chân sau hơi ngắn sải. Cơ mông và vai còn mỏng do chưa được tập có hệ thống.*
5. Bấm **Nộp đánh giá**

**Kỳ vọng:**
- Nút đổi thành *"Đang nộp..."*
- Form biến mất, thay bằng khối xám **"Đánh giá đã nộp"** với đúng các số vừa nhập
- Đơn **biến khỏi** hàng đợi cột trái, số đếm giảm 1
- Panel tự nhảy sang đơn kế tiếp (hoặc hiện "Chọn một hồ sơ" nếu hết)

Kiểm chứng bằng SQL:

```sql
SELECT id, admission_id, horse_id, readiness_status,
       conformation_score, temperament_score, gait_quality_score,
       estimated_months_to_race, fitness_score, valid_until, trainer_id
FROM racing_readiness_assessments
ORDER BY id DESC LIMIT 1;
```

**Ba thứ phải đúng:**
- `fitness_score` = `NULL` ← ngựa đang cách ly
- `valid_until` = `NULL` ← ràng buộc `chk_rra_valid_until`
- `admission_id` = đúng id đơn ← đánh dấu đây là loại "tiếp nhận"

Và trạng thái đơn:

```sql
SELECT status, trainer_id, trainer_reviewed_at
FROM admission_applications WHERE id = <ID>;
```

→ `MANAGER_REVIEW`, `trainer_id` = 2, `trainer_reviewed_at` có giờ.

### KB-7 · Thông báo lỗi nghiệp vụ hiện đúng nguyên văn

Đây là kịch bản kiểm `postWithMessage`.

1. Trên Swagger, đẩy một đơn về `TRAINER_REVIEW` (SQL ở C2 cách 3)
2. Trên giao diện, mở đúng đơn đó, điền form, **chưa bấm nộp**
3. Quay lại Swagger, gọi `POST /api/admissions/{id}/trainer-review` để đơn chuyển sang `MANAGER_REVIEW`
4. Quay lại giao diện, bấm **Nộp đánh giá**

**Kỳ vọng:** hộp đỏ hiện **nguyên văn** thông báo backend:

> *Đơn đang ở bước MANAGER_REVIEW, không phải TRAINER_REVIEW — không thể đánh giá!*

**Nếu thấy "API error: 400"** thì bạn đang gọi nhầm `apiPost` dùng chung thay vì `postWithMessage`.

### KB-8 · Đã đánh giá thì chỉ đọc

Sau KB-6, dùng SQL đẩy đơn đó về `TRAINER_REVIEW` **nhưng giữ nguyên** dòng trong `racing_readiness_assessments`:

```sql
UPDATE admission_applications SET status = 'TRAINER_REVIEW' WHERE id = <ID>;
```

Tải lại giao diện, mở đơn đó.

**Kỳ vọng:** đơn xuất hiện lại trong hàng đợi, nhưng panel hiện **"Đánh giá đã nộp"** ở chế độ chỉ đọc — không có form, không có nút nộp.

> Đây là kiểm tra `existingAssessment !== null`. Tránh được tình huống Trainer nhập lại rồi nhận lỗi trùng khoá `uq_rra_admission`.

### KB-9 · Chặn quyền

1. Đăng xuất, đăng nhập `groom@rtms.com`
2. Gõ thẳng `http://localhost:3000/trainer/admissions`

**Kỳ vọng:** `RoleGuard` chặn — chuyển hướng hoặc hiện thông báo từ chối, **không** thấy dữ liệu đơn.

---

## C4. Lỗi hay gặp và cách sửa

| Triệu chứng | Nguyên nhân | Cách sửa |
|---|---|---|
| Network hiện request tới `undefined/api/...` | Chưa có `.env.local` | `cp .env.local.example .env.local` rồi **restart `npm run dev`** |
| Sửa `.env.local` rồi vẫn lỗi cũ | Next.js đọc env lúc khởi động | Dừng và chạy lại `npm run dev` |
| **401** ở mọi lời gọi | Cookie `jwt_token` không được gửi | `credentials: 'include'` phải có ở **mọi** hàm fetch — kiểm `postWithMessage` |
| **CORS** trong console | Backend chưa cho `localhost:3000` | Kiểm cấu hình CORS trong `SecurityConfig` |
| Hàng đợi luôn rỗng nhưng Swagger có dữ liệu | Gọi sai tham số | Phải là `?status=TRAINER_REVIEW`, viết hoa đúng |
| Pill luôn "Chưa xếp chuồng" | Backend chưa build lại sau PHẦN A | Restart `./mvnw spring-boot:run` |
| Nộp xong đơn vẫn nằm trong hàng đợi | Quên gọi `onSubmitted()` | Kiểm trong `handleSubmit` có `await load()` **và** `onSubmitted()` |
| Lỗi hiện "API error: 400" | Dùng nhầm `apiPost` | Đổi sang `postWithMessage` |
| **404** khi nộp | `GlobalExceptionHandler` map `RuntimeException` → 404 | Đơn không tồn tại, hoặc lỗi nghiệp vụ ném sai loại exception |
| Nút Nộp bị xám mãi | `horse === null` | Bước Groom chưa tạo hồ sơ Horse — xem C2 |
| TypeScript đỏ ở `Pill icon="horse"` | Tên icon sai | Tên hợp lệ có trong `Icon.tsx`: `horse`, `shield`, `clipboard`, `check`, `x`, `activity`, `alert-triangle`, `file-text`, `search`... |
| `PageContainer` lỗi import | Export dạng named | `import { PageContainer } from '@/components/layout/PageContainer'` |

### Mẹo đọc lỗi nhanh

Mở **DevTools → Network**, lọc `Fetch/XHR`. Với mỗi request kiểm ba thứ theo thứ tự:

1. **URL** có bắt đầu bằng `http://localhost:8080` không? Không → lỗi env.
2. **Status**: 401 → cookie; 403 → quyền; 400 → nghiệp vụ; 404 → không tìm thấy *hoặc* lỗi nghiệp vụ bị map nhầm.
3. **Response body** — `message` chính là câu tiếng Việt backend gửi. Nếu giao diện không hiện đúng câu này thì lỗi nằm ở tầng service frontend.

---

# PHẦN D — Nghiệm thu và bàn giao

## D1. Checklist

### Trước khi bắt đầu (`HANDOVER_CHECKLIST.md`)

- [ ] Pull code mới nhất
- [ ] Đã đọc `FRONTEND_GUIDE.md`
- [ ] Xác định đúng feature folder → `features/admissions/`
- [ ] **Xác nhận API/DTO backend đã tồn tại** → xem bảng ở mục 0
- [ ] Không sửa file nền dùng chung nếu không cần thiết

### Backend

- [ ] `./mvnw test` → 64/64 pass
- [ ] `GET /api/admissions?status=TRAINER_REVIEW` trả `quarantineStallCode`
- [ ] Đơn chưa xếp chuồng → `quarantineStallCode: null`, không ném lỗi

### Kiểm thử giao diện

- [ ] KB-1 — vào được màn hình từ thanh điều hướng
- [ ] KB-2 — hàng đợi hiện mã chuồng *(chụp màn hình)*
- [ ] KB-3 — hồ sơ đầy đủ, mục thú y hiện "chưa có dữ liệu"
- [ ] KB-4 — tải được giấy tờ
- [ ] KB-5 — chặn nhận xét ngắn tại client
- [ ] KB-6 — nộp thành công, đơn rời hàng đợi *(chụp màn hình)*
- [ ] KB-7 — lỗi nghiệp vụ hiện nguyên văn
- [ ] KB-8 — đã đánh giá thì chỉ đọc
- [ ] KB-9 — `RoleGuard` chặn groom

### Definition of Done (`FRONTEND_GUIDE.md` §17)

- [ ] Dùng API backend thật, **không** có dữ liệu giả
- [ ] **Không** có `fetch()` trong component *(chỉ có trong service, đã ghi chú ngoại lệ)*
- [ ] **Không** hardcode `http://localhost:8080`
- [ ] Dùng design token dùng chung, không có hex hay màu Tailwind cứng
- [ ] Xử lý đủ 4 trạng thái: Loading · Error · Empty · Content
- [ ] Form chặn gửi trùng (`disabled={submitting}`)
- [ ] Đã test **đúng vai trò** (KB-1…KB-8)
- [ ] Đã test **sai vai trò** (KB-9)
- [ ] `npm run lint` **pass**
- [ ] `npm run build` **pass**

```bash
cd frontend && npm run lint && npm run build
```

> `npm run build` bắt lỗi TypeScript mà `npm run dev` bỏ qua — **luôn chạy trước khi commit**.
>
> Nếu lint báo `react-hooks/set-state-in-effect`: dự án có bật rule này (xem
> `ManagerQueueList.tsx` đang phải `eslint-disable`). Code ở B3 gọi `setSelectedId`
> bên trong hàm async chứ không ở thân effect nên không vướng. Nếu vẫn bị,
> bọc vào `useCallback` thay vì tắt rule.

## D2. Gợi ý commit

Tách hai commit để lịch sử đọc được:

```
feat(admission): them quarantineStallCode vao danh sach don tiep nhan

Man hinh hang doi cua Trainer can ma chuong (Q3) de xuong khu cach ly
quan sat ngua. Truoc day chi co quarantineStallId la khoa noi bo,
phai mo chi tiet tung don moi biet di dau.

Them truong vao cuoi constructor de loi goi cu bao loi bien dich
thay vi gan nham gia tri.
```

```
feat(admission): man hinh Trainer danh gia ho so tiep nhan

- TrainerQueueList: hang doi don TRAINER_REVIEW kem ma chuong cach ly
- TrainerReviewPanel: ho so ung vien, giay to, form danh gia
- Khoa form sau khi nop (existingAssessment khac null)
- postWithMessage: doc message loi tu backend, apiPost dung chung nuot mat

Hoan thanh Dot 1 trong PLAN_04.
```

## D3. Hai việc báo nhóm

**1 · `AdmissionSummaryResponse` là DTO dùng chung** với màn hình Manager và Owner. Thêm trường là thao tác additive nên không phá ai, nhưng báo trước vẫn hơn.

**2 · `apiPost` trong `services/api.ts` vứt bỏ thông báo lỗi — nên sửa dùng chung.**

Đây là việc đáng đưa ra nhóm nhất, vì nó là **mâu thuẫn nội bộ của chính `FRONTEND_GUIDE.md`**:

| Guide nói | Code làm |
|---|---|
| §8 — mọi request đi qua `services/api.ts` | `apiPost` ném `Error("API error: 400")` |
| §12 — phải hiện lỗi rõ ràng | Không còn message để mà hiện |
| §19 — *"Silently failing when an API request returns an error"* là lỗi cần tránh | Chính `api.ts` đang làm việc đó |

Ai tuân thủ §8 thì vi phạm §12 và §19. **Không thể thoả cả ba với code hiện tại.**

Lập luận mạnh nhất để thuyết phục nhóm: **`apiUpload` trong chính file đó đã làm đúng rồi.**

```ts
// apiUpload — ĐANG CÓ SẴN, xử lý đúng
const payload = await res.json().catch(() => null);
if (!res.ok) throw new Error(payload?.message || `API error: ${res.status}`);
```

Nên đây không phải đề xuất pattern mới, mà là **bắt `apiGet` và `apiPost` nhất quán với người anh em cùng file**. Sửa 2 dòng mỗi hàm, lợi cho mọi màn hình của cả 5 actor.

Nhóm duyệt thì bạn xoá `postWithMessage` và đổi sang `apiPost` — mất đúng 2 dòng, và ngoại lệ so với §8 biến mất.

**3 · `SETUP_NOTES.md` thiếu mật khẩu tài khoản test.** File có liệt kê bước copy env nhưng không ghi mật khẩu của `trainer@rtms.com` và các tài khoản seed khác. Người mới vào nhóm sẽ kẹt ngay bước đăng nhập. Nên bổ sung.

**4 · `AdmissionStatus` phía frontend lệch backend.** Type ở `features/admissions/types/index.ts` khai thêm `'SUBMITTED'` và `'ADDITIONAL_INFORMATION_REQUIRED'`, trong khi enum backend chỉ có 7 giá trị. Chưa gây lỗi, nhưng sẽ làm sai mọi `switch` vét cạn về sau. Báo người phụ trách file đó.

## D4. Sau khi xong đợt 1

Quay lại [PLAN_04 — Đợt 2](PLAN_04_ROADMAP.md#4-đợt-2--chuồng-trại--phân-công-groom): chuồng trại và phân công Groom.

Việc backend đầu tiên của đợt đó là **BE-2.1** — `HorseService.getAllHorses` hiện trả **toàn bộ** ngựa cho `HEAD_TRAINER`, gồm cả ngựa khu khác và ngựa đang cách ly. Cần thêm tham số lọc theo khu, suy qua chuỗi `Horse.currentStallId → StableStall.areaId → Area.trainerId`.

---

*Tài liệu liên quan: [PLAN_04 — Lộ trình](PLAN_04_ROADMAP.md)*
