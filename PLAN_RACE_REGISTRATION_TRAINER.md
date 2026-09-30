# Kế hoạch triển khai: Trainer nộp đơn đề cử ngựa dự đua

## 1. Phạm vi và thuật ngữ

Trainer tự tìm hiểu cuộc đua ở bên ngoài RTMS, chọn **một ngựa đang do mình phụ trách** rồi nhập một **đơn đề cử nội bộ**. RTMS lưu đơn với trạng thái `PENDING` để Club Manager xem xét. Đây **không phải** lệnh đăng ký với ban tổ chức, không giữ chỗ, không thu phí và không xác nhận ngựa được phép ra sân. Sau khi Manager duyệt, Trainer thấy `APPROVED`/`REJECTED` và nhận xét; chức năng thông báo cho Trainer và Owner do phần Manager/Owner của nhóm xử lý.

Trong phạm vi Trainer: xem đơn của mình, tạo đơn, xem chi tiết/trạng thái. Không làm danh mục cuộc đua, đồng bộ lịch đua, đề xuất ngựa tự động, gửi đơn ra ngoài, hoặc giao diện duyệt.

Mỗi đơn chỉ đề cử **một ngựa cho một hạng mục/cuộc đua**. Cùng một sự kiện có thể có nhiều hạng mục, nên cần `raceName` và `raceCategory` riêng.

## 2. Đối chiếu code hiện tại

| Vị trí | Đang có | Hướng xử lý |
|---|---|---|
| `backend/.../controller/RaceRegistrationController.java` | `GET /api/race-registrations` trả tất cả cho người có `VIEW`; `POST` chỉ kiểm tra ngựa tồn tại, không kiểm tra ngựa thuộc khu Trainer; `PATCH /{id}/review` đã có | Giữ URL và quyền; chuyển logic tạo/lọc/xem chi tiết vào service. Không sửa nghiệp vụ duyệt trong phần Trainer, nhưng thống nhất DTO để đồng đội dùng. |
| `entity/RaceRegistration.java`, `dto/CreateRaceRegistrationRequest.java`, `repository/RaceRegistrationRepository.java` | Có `horseId`, `trainerId`, tên, địa điểm, `eventDate` dạng ngày giờ, cự ly, mặt sân, jockey, phí, ghi chú và trạng thái | Mở rộng đúng thông tin đơn đề cử; không tạo bảng `races` hay FK tới lịch đua. |
| `db/migration/V6__head_trainer.sql`, `V18__race_registration_permissions.sql` | Bảng và quyền `VIEW`/`CREATE` cho Trainer, `REVIEW` cho Manager đã tồn tại | **Không sửa migration cũ**. Thêm migration mới sau phiên bản mới nhất lúc triển khai (hiện tại là V54). |
| `frontend/src/config/navigation.ts` | Trainer có `Racing` nhưng không có `href`; Owner cũng có mục placeholder | Gán `href: '/trainer/racing'` cho Trainer. Để Owner nguyên trạng cho đồng đội. |
| `frontend/src/app/trainer/racing/` và `frontend/src/features/racing/` | Chưa có | Tạo mới. Không có màn hình tổng hợp cuộc đua cũ cần xóa. |
| `frontend/src/features/stable/services/stableService.ts` | Có `getHorses({ mine, status })` dùng `GET /api/horses`; `mine` suy ra từ chuồng → khu → Trainer | Dùng để đổ danh sách chọn ngựa; backend **phải kiểm tra lại** khi nộp. |

`RacingReadinessAssessment` trong admission/biểu đồ là đánh giá khả năng thi đấu của ngựa, không phải đơn đề cử. Giữ nguyên các trang này. `race_registrations` cũ còn cột `final_position`, `finish_time_seconds`, `prize_money`: chưa dùng cho luồng đề cử; không đưa lên form. Nếu muốn dọn DB, tách thành migration riêng khi nhóm xác nhận không còn consumer dữ liệu kết quả.

## 3. Trường dữ liệu đề xuất

Các chương trình đua thực tế công bố ngày, địa điểm, tên hạng mục, cự ly, điều kiện tuổi/giới/tỷ lệ, tiền thưởng, phí và hạn đề cử. Xem [BHA race details](https://www.britishhorseracing.com/racing/fixtures/upcoming/entries/entries-race/), [BHA owners' toolkit](https://www.britishhorseracing.com/regulation/ownership/owners-toolkit/) và [Racing Australia race program](https://www.racingaustralia.horse/FreeFields/RaceProgram.aspx?Key=2026May16%2CQLD%2CDoomben). RTMS chỉ ghi lại **những gì Trainer tự đọc được**; dữ liệu này chưa được hệ thống xác minh.

| Trường API / DB | Bắt buộc | Cách dùng |
|---|---:|---|
| `horseId` / `horse_id` | Có | Chọn từ ngựa trong khu Trainer; hiển thị tên, UELN nếu có. ID phải được xác thực ở backend. |
| `raceName` / `race_name` | Có | Tên cuộc đua/sự kiện theo nguồn Trainer đọc. |
| `raceCategory` / `race_category` | Có | Tên hạng mục hoặc chặng cụ thể, ví dụ “3 tuổi, 1.200 m”; nhập tự do, không tạo danh mục chuẩn khi chưa có yêu cầu. |
| `location` / `location` | Có ở đơn mới | Sân đua và địa điểm; đủ để Manager phân biệt các sự kiện trùng tên. |
| `eventDate` / `event_date` | Có | **Ngày** tổ chức, không buộc Trainer đoán giờ xuất phát. |
| `eventTime` / `event_time` | Không | Giờ xuất phát nếu nguồn đã công bố. |
| `organizer` / `organizer` | Không | Đơn vị tổ chức. |
| `sourceUrl` / `source_url` | Không | Link thể lệ/trang chính thức để Manager mở kiểm chứng. Không tải/cào dữ liệu từ link. |
| `nominationDeadline` / `nomination_deadline` | Không | Hạn đăng ký thật, nếu đã công bố. Hữu ích để Manager ưu tiên xét duyệt. |
| `distanceMeters` / `distance_meters` | Không | Cự ly số dương, đơn vị mét. |
| `trackType` / `track_type` | Không | Mặt sân/đường đua, nhập tự do vì cách gọi khác nhau theo nơi tổ chức. |
| `prizeDetails` / `prize_details` | Không | Tiền thưởng/cơ cấu các hạng và đơn vị tiền tệ dưới dạng văn bản; tránh giả định mọi giải cùng tiền tệ. |
| `entryFee` / `entry_fee` | Không | Phí công bố, chỉ mang tính tham khảo; muốn dùng số tiền thật cần thêm `currency` hoặc ghi tiền tệ trong `prizeDetails`. Đề xuất **không hiện trường phí số ở MVP** để tránh số không có đơn vị. |
| `selectionReason` / `selection_reason` | Có | Vì sao chọn ngựa: thể lực, cự ly, mặt sân, mục tiêu huấn luyện; là nhận định của Trainer, không phải đánh giá tự động của hệ thống. |
| `trainerNotes` / `trainer_notes` | Không | Điều kiện tham dự, giới hạn tuổi/giới/handicap, giấy tờ, lưu ý sức khỏe/hậu cần chưa có cấu trúc riêng. |

`jockeyName` cũ không cần bắt buộc: đề cử nội bộ có thể được nộp trước khi chốt người cưỡi. Để cột cũ nullable và không hiện trong form MVP. Nếu đội muốn theo dõi chi phí, dùng `entryFee` **kèm `currency`** trong cùng một thay đổi, không hiển thị số trần.

### Dữ liệu mẫu của request

```json
{
  "horseId": 42,
  "raceName": "Giải Mùa Thu 2026",
  "raceCategory": "Hạng 3 tuổi - 1.200 m",
  "location": "Trường đua A, Hà Nội",
  "eventDate": "2026-11-15",
  "eventTime": null,
  "organizer": "Câu lạc bộ A",
  "sourceUrl": "https://example.org/race-conditions",
  "nominationDeadline": "2026-11-01",
  "distanceMeters": 1200,
  "trackType": "Cỏ",
  "prizeDetails": "Nhất 50 triệu VND; nhì 25 triệu VND",
  "selectionReason": "Ngựa có kết quả tốt ở cự ly 1.200 m và đang tập ổn định.",
  "trainerNotes": "Cần xác minh điều kiện tuổi với ban tổ chức."
}
```

## 4. Backend: file và nội dung cần làm

### 4.1 Migration

Thêm `backend/src/main/resources/db/migration/V55__extend_race_registrations_for_trainer_proposals.sql` **nếu V55 vẫn trống lúc triển khai**. Nếu nhóm đã dùng V55, chọn số tiếp theo. Nội dung dự kiến:

```sql
ALTER TABLE race_registrations
    ADD COLUMN race_category VARCHAR(255),
    ADD COLUMN organizer VARCHAR(255),
    ADD COLUMN source_url VARCHAR(1000),
    ADD COLUMN nomination_deadline DATE,
    ADD COLUMN event_time TIME,
    ADD COLUMN prize_details TEXT,
    ADD COLUMN selection_reason TEXT;

-- Giữ giờ thật của dữ liệu cũ; 00:00 thường là giá trị ngày không có giờ.
UPDATE race_registrations
SET event_time = event_date::time
WHERE event_date::time <> TIME '00:00';

ALTER TABLE race_registrations
    ALTER COLUMN event_date TYPE DATE USING event_date::date;

CREATE INDEX idx_race_registrations_trainer_created
    ON race_registrations (trainer_id, created_at DESC);
```

Không đặt `NOT NULL` trực tiếp lên các cột mới vì có thể đã có bản ghi cũ. Validation ở API áp dụng cho **đơn mới**. Nếu môi trường có dữ liệu cũ quan trọng cần giữ giờ `00:00` thật, phải kiểm tra trước khi chạy migration; phương án khác là giữ `event_date TIMESTAMP` và thêm một cột ngày mới theo cách migrate dữ liệu của nhóm.

### 4.2 Entity, request và response

- Sửa `backend/src/main/java/com/rtms/backend/entity/RaceRegistration.java`: thêm các field ứng với migration; đổi `eventDate` từ `LocalDateTime` sang `LocalDate`, thêm `eventTime: LocalTime`. Giữ `status`, `reviewedById`, `managerFeedback`, `reviewedAt`, `createdAt`. `trainerId` luôn lấy từ người đăng nhập, tuyệt đối không nhận từ body.
- Sửa `backend/src/main/java/com/rtms/backend/dto/CreateRaceRegistrationRequest.java`: thêm các field trên, `@NotNull` cho `horseId`/`eventDate`, `@NotBlank` cho `raceName`/`raceCategory`/`location`/`selectionReason`, `@Size` phù hợp với DB, `@Positive` cho cự ly, `@PositiveOrZero` nếu sau này giữ phí số. Có thể dùng getter/setter như DTO hiện tại, không thêm Lombok.
- Thêm `backend/src/main/java/com/rtms/backend/dto/RaceRegistrationResponse.java`: trả `id`, `horseId`, `horseName`, `horseRegistrationNumber`, `trainerId`, toàn bộ thông tin cuộc đua, `status`, `managerFeedback`, `createdAt`, `reviewedAt`. DTO này tránh để frontend phụ thuộc trực tiếp JPA entity và tránh gọi thêm `GET /api/horses/{id}` cho từng hàng.
- Nếu vẫn giữ endpoint Manager `PATCH /{id}/review` trong controller hiện có, đổi kiểu response của nó cùng DTO trong cùng lần tích hợp để hai bên có một hợp đồng nhất quán; phần logic review để người phụ trách Manager quyết định.

### 4.3 Service và giới hạn quyền

Thêm `backend/src/main/java/com/rtms/backend/service/RaceRegistrationService.java`, `@Service`, `@Transactional` cho `create`; chuyển phần mapping/save ra khỏi controller. Dùng `@AuthenticationPrincipal AuthenticatedUser currentUser` như các controller mới; lấy `currentUser.getUserId()`.

Luồng `create(request, currentUser)` cụ thể:

1. Tìm ngựa theo `horseId`; không có → 404 qua `ApiException`.
2. Từ `Horse.currentStallId` tìm `StableStall.areaId`, sau đó `Area.trainerId`; thiếu chuồng/khu hoặc `trainerId` khác người gọi → 403/400 rõ lý do. Đây là cùng quy tắc quyền với `HorseService.getAllHorses(... mine=true)` và `HorseTrainingPlanService.resolveGroom`, **không** cần thêm `trainer_id` vào `horses`.
3. Chỉ cho đề cử ngựa `ELIGIBLE` và không `trainingLocked`; đây là chặn trạng thái cơ bản, không phải kiểm tra phù hợp với cuộc đua. Nếu nhóm muốn cho phép `MONITORING` để Manager cân nhắc giải diễn ra xa trong tương lai, cần thống nhất trước và ghi rõ cảnh báo trong form.
4. Kiểm tra `eventDate >= LocalDate.now()`, `nominationDeadline <= eventDate` nếu nhập; ngày hạn đã qua thì báo lỗi rõ ràng hoặc yêu cầu Trainer sửa nguồn, không âm thầm đổi. Kiểm tra `sourceUrl` là HTTP(S) nếu có và chuẩn hóa chuỗi trim; không fetch URL ở backend.
5. Tạo entity, gán `trainerId` từ phiên đăng nhập, `status = PENDING`, lưu. Không nhận `status`, `managerFeedback`, `reviewedAt`, `ownerId` từ request.

Phần xác thực ngựa nên viết tập trung trong service (ví dụ dưới chỉ minh họa các nhánh chính; phần mapping và exception cụ thể theo `ApiException` của repo):

```java
private Horse requireManagedHorse(Long horseId, Long trainerId) {
    Horse horse = horseRepository.findById(horseId)
            .orElseThrow(() -> new ApiException(
                    HttpStatus.NOT_FOUND, "RESOURCE_NOT_FOUND", "Không tìm thấy ngựa"));
    if (horse.getCurrentStallId() == null) {
        throw new ApiException(HttpStatus.BAD_REQUEST, "HORSE_NOT_ASSIGNED",
                "Ngựa chưa được xếp chuồng");
    }
    StableStall stall = stableStallRepository.findById(horse.getCurrentStallId())
            .orElseThrow(() -> new ApiException(
                    HttpStatus.BAD_REQUEST, "INVALID_STALL", "Chuồng của ngựa không tồn tại"));
    Area area = areaRepository.findById(stall.getAreaId())
            .orElseThrow(() -> new ApiException(
                    HttpStatus.BAD_REQUEST, "INVALID_AREA", "Khu của ngựa không tồn tại"));
    if (!trainerId.equals(area.getTrainerId())) {
        throw new AccessDeniedException("Ngựa không thuộc khu bạn phụ trách");
    }
    if (horse.getCurrentStatus() != HorseStatus.ELIGIBLE || horse.isTrainingLocked()) {
        throw new ApiException(HttpStatus.BAD_REQUEST, "HORSE_NOT_ELIGIBLE",
                "Ngựa hiện không thể được đề cử dự đua");
    }
    return horse;
}
```

`@Transactional` bao quanh create để việc kiểm tra và lưu nằm trong cùng transaction. Không lấy danh sách `mine=true` ở client làm bằng chứng quyền vì request POST có thể gửi `horseId` tùy ý.

Sửa `RaceRegistrationRepository.java`:

```java
List<RaceRegistration> findByTrainerIdOrderByCreatedAtDesc(Long trainerId);
Optional<RaceRegistration> findByIdAndTrainerId(Long id, Long trainerId);
```

Sửa `RaceRegistrationController.java` theo hướng:

```java
@PostMapping
@PreAuthorize("hasAuthority('RACE_REGISTRATION_CREATE')")
public ApiResponse<RaceRegistrationResponse> create(
        @Valid @RequestBody CreateRaceRegistrationRequest request,
        @AuthenticationPrincipal AuthenticatedUser user) {
    return ApiResponse.success(service.create(request, user));
}

@GetMapping
@PreAuthorize("hasAuthority('RACE_REGISTRATION_VIEW')")
public ApiResponse<List<RaceRegistrationResponse>> list(
        @RequestParam(required = false) Long horseId,
        @AuthenticationPrincipal AuthenticatedUser user) {
    return ApiResponse.success(service.listVisible(user, horseId));
}

@GetMapping("/{id}")
@PreAuthorize("hasAuthority('RACE_REGISTRATION_VIEW')")
public ApiResponse<RaceRegistrationResponse> detail(
        @PathVariable Long id,
        @AuthenticationPrincipal AuthenticatedUser user) {
    return ApiResponse.success(service.getVisible(id, user));
}
```

`listVisible`: Trainer **chỉ** thấy `trainer_id = userId`; Manager vẫn thấy danh sách toàn bộ để đồng đội xử lý. `getVisible`: Trainer chỉ mở đơn của mình; Manager mở được mọi đơn. Không dựa vào lọc ở frontend để bảo vệ dữ liệu. Các role khác không có `VIEW` theo V18. Nếu sau này Owner cần xem, thêm quyền/API có bộ lọc theo `Horse.ownerId` ở phần Owner.

`GET` hiện nhận `horseId` để lọc; giữ tham số này vì có thể có client khác đang dùng. Áp dụng nó **sau** ràng buộc Trainer, không để `horseId` làm lộ đơn của người khác. Nếu đổi hợp đồng API, báo trước cho người làm Manager.

### 4.4 Kiểm thử backend cần có

Thêm `backend/src/test/java/com/rtms/backend/service/RaceRegistrationServiceTest.java`: nộp hợp lệ tạo `PENDING` với ID Trainer từ phiên; ngựa của Trainer khác bị chặn; ngựa chưa xếp chuồng/trạng thái không hợp lệ bị chặn; ngày cũ hoặc cự ly âm bị từ chối; list/detail của Trainer không lộ đơn khác. Một controller test xác nhận `@Valid`, 401/403 và JSON `ApiResponse` nếu test setup của nhóm cho phép. Không cần test quy trình Manager trong phần này.

## 5. Frontend: file và giao diện

### 5.1 Cấu trúc file

```text
frontend/src/
  config/navigation.ts                          # HEAD_TRAINER Racing -> /trainer/racing
  app/trainer/racing/page.tsx                   # danh sách đơn của tôi
  app/trainer/racing/new/page.tsx               # form tạo đơn
  app/trainer/racing/[id]/page.tsx              # chi tiết, trạng thái, phản hồi
  features/racing/types/index.ts               # request, response, status
  features/racing/services/racingService.ts    # GET list/detail, POST create
  features/racing/components/TrainerRaceList.tsx
  features/racing/components/TrainerRaceForm.tsx
  features/racing/components/TrainerRaceDetail.tsx
```

Các `page.tsx` dùng `RoleGuard allowedRoles={['HEAD_TRAINER']}`, `AppShell`, `PageContainer` như `app/trainer/admissions/page.tsx` và `app/trainer/plans/new/page.tsx`. `racingService.ts` gọi `apiGet`/`apiPost` từ `src/services/api.ts`, trả `response.data`; không gọi `fetch` trực tiếp trong component. Lấy ngựa bằng `stableApi.getHorses({ mine: true, status: 'ELIGIBLE' })`; lọc thêm `trainingLocked` thì mở rộng type `Horse` trong `features/stable/types/index.ts` vì backend `Horse` đã trả thuộc tính này. Backend vẫn kiểm tra lại tất cả điều kiện lúc POST.

Type chính:

```ts
export type RaceRegistrationStatus = 'PENDING' | 'APPROVED' | 'REJECTED';

export interface CreateRaceRegistrationRequest {
  horseId: number;
  raceName: string;
  raceCategory: string;
  location: string;
  eventDate: string;              // YYYY-MM-DD
  eventTime?: string | null;      // HH:mm nếu biết
  organizer?: string | null;
  sourceUrl?: string | null;
  nominationDeadline?: string | null;
  distanceMeters?: number | null;
  trackType?: string | null;
  prizeDetails?: string | null;
  selectionReason: string;
  trainerNotes?: string | null;
}
```

`RaceRegistrationResponse` thêm `id`, `horseName`, `horseRegistrationNumber`, `trainerId`, `status`, `managerFeedback`, `createdAt`, `reviewedAt`. Dùng `ApiResponse<T>` chung nếu có type dùng chung; nếu chưa, định nghĩa type nhỏ ngay service như các feature hiện tại.

Ví dụ `features/racing/services/racingService.ts`:

```ts
import { apiGet, apiPost } from '@/services/api';
import type { CreateRaceRegistrationRequest, RaceRegistrationResponse } from '../types';

type ApiResponse<T> = { success: boolean; data: T; message: string };

export const racingService = {
  listMine: async () =>
    (await apiGet<ApiResponse<RaceRegistrationResponse[]>>('/api/race-registrations')).data,
  getMine: async (id: number) =>
    (await apiGet<ApiResponse<RaceRegistrationResponse>>(`/api/race-registrations/${id}`)).data,
  create: async (body: CreateRaceRegistrationRequest) =>
    (await apiPost<ApiResponse<RaceRegistrationResponse>>('/api/race-registrations', body)).data,
};
```

`listMine` dựa trên bộ lọc quyền **ở backend**; tên hàm nhấn mạnh đây là danh sách của Trainer. Component giữ `loading`, `error`, `submitting` riêng; request fail thì lấy `error.message` từ `ApiError` trong `services/api.ts`.

### 5.2 Màn danh sách `/trainer/racing`

- Tiêu đề **Đơn đề cử dự đua** và mô tả ngắn: “Bạn tự tìm hiểu cuộc đua và gửi đề cử nội bộ để quản lý xem xét.” Nút chính **Tạo đơn đề cử**.
- Bảng/thẻ responsive: tên cuộc đua + hạng mục, ngựa, ngày/địa điểm, trạng thái, ngày gửi, nút **Xem đơn**. Sắp mới nhất trước theo `createdAt` từ API. Bộ lọc đơn giản `Tất cả / Chờ duyệt / Đã duyệt / Từ chối`; không cần tìm cuộc đua ngoài hệ thống.
- Empty state: “Bạn chưa gửi đơn đề cử nào” + nút tạo. Loading và error state theo `components/ui/states.tsx`.
- Badge trạng thái dùng `Pill` trong `components/ui/StatusBadge.tsx`: `warning`, `success`, `danger`. Không ép type `HorseStatus` của `StatusBadge` vào trạng thái đơn.

### 5.3 Form `/trainer/racing/new`

Chia `Panel` thành 3 phần: **Cuộc đua** (tên, hạng mục, sân/địa điểm, ngày, giờ tùy chọn, đơn vị tổ chức, link nguồn, hạn nộp), **Thông tin tham khảo** (cự ly, mặt sân, cơ cấu giải thưởng, điều kiện/ghi chú), **Ngựa đề cử** (select một ngựa trong khu, tên/UELN/trạng thái và ô lý do chọn). Có thể đặt chọn ngựa lên đầu nếu Trainer thường đi từ hồ sơ ngựa; thứ tự không đổi payload.

`<input type="date">` cho `eventDate`, `<input type="time">` cho `eventTime`; không dùng `toISOString()` để chuyển ngày vì dễ lệch múi giờ. Link nguồn mở tab mới với `rel="noopener noreferrer"`. Trường quan trọng có nhãn `*` và thông báo lỗi ngay dưới ô. Hiển thị câu nhắc **“Đây là đơn đề cử nội bộ, chưa đăng ký với ban tổ chức.”** ngay trên nút **Gửi chờ duyệt**. Khi gửi: disable nút để tránh submit đôi; thành công chuyển đến `/trainer/racing/{id}` và báo đã gửi; thất bại giữ nguyên dữ liệu và hiện `ApiError.message`. Không hiện “đã đăng ký thi thành công”.

Nếu không có ngựa có thể chọn, hiện lý do và liên kết đến `/trainer/horses` hoặc `/trainer/stable`; không đưa ngựa của Trainer khác vào select. Không tự gợi ý ngựa theo cự ly/thành tích và không tính eligibility từ thể lực.

### 5.4 Chi tiết `/trainer/racing/[id]`

Trang chỉ đọc hiển thị toàn bộ nội dung Trainer đã nhập, ngựa đã chọn, trạng thái, ngày gửi, phản hồi Manager và ngày duyệt nếu đã có. Thông tin nguồn ngoài do Trainer cung cấp nên ghi “Thông tin do Trainer cung cấp; quản lý sẽ tự kiểm chứng”. Không có nút duyệt/từ chối. Có nút về danh sách; chưa cần sửa/xóa đơn khi không có quy tắc trạng thái rõ ràng.

## 6. Hợp đồng để bàn giao cho người làm Manager/Owner

1. Manager dùng `RACE_REGISTRATION_VIEW` và `RACE_REGISTRATION_REVIEW` hiện có, thấy đầy đủ `raceCategory`, `sourceUrl`, điều kiện, prize, lý do chọn để tự kiểm chứng. `PATCH /api/race-registrations/{id}/review` chỉ chuyển `PENDING` → `APPROVED`/`REJECTED`; việc duyệt **không** gửi đơn ra ban tổ chức.
2. Khi duyệt, phần Manager/notification phát sự kiện hoặc tạo thông báo nội bộ cho `registration.trainerId` và `Horse.ownerId` của ngựa. Repo hiện **chưa có hạ tầng notification**, nên không thể coi trạng thái đơn tự đổi là thông báo chủ động. Cần đồng đội thống nhất cơ chế (thông báo trong app/email) và quyền Owner trước khi hứa tính năng này đã xong.
3. Nếu `Horse.ownerId` có thể thay đổi sau khi Trainer nộp, nhóm cần quyết định Owner nhận thông báo là Owner **lúc nộp** hay **lúc duyệt**. Nếu chọn lúc nộp, thêm `owner_id` snapshot vào `race_registrations` trong migration và chỉ backend gán từ Horse. Với MVP, có thể lấy Owner hiện tại lúc duyệt và ghi rõ quy tắc đó.
4. Không tự cấp `RACE_REGISTRATION_VIEW` cho Owner bằng cách mở `GET` hiện tại: endpoint phải lọc theo ngựa của Owner, nếu không lộ đơn toàn câu lạc bộ.

## 7. Thứ tự triển khai và kiểm tra cuối

1. Chốt hợp đồng field/API với người làm Manager; chọn số migration trống.
2. Migration → entity/DTO → service/repository → controller → backend tests.
3. Frontend types/service → navigation/routes → form/list/detail.
4. Chạy backend tests và frontend lint/build; thử với hai tài khoản Trainer có khu khác nhau. Đảm bảo Trainer A không thể POST `horseId` khu B hay GET chi tiết đơn B bằng URL thủ công.
5. Thử một đơn hợp lệ rồi xác nhận DB lưu `PENDING`, nội dung không mất, Manager có thể đọc đơn; sau khi người làm Manager duyệt, Trainer thấy trạng thái/phản hồi. Kiểm tra riêng thông báo cho Trainer và Owner khi phần đó được tích hợp.

Các file backend đang có thay đổi không liên quan ở `TrainingWorkoutRepository`, `HorseTrainingPlanService` và test của nó; tránh chỉnh đè trong khi triển khai phần Racing.
