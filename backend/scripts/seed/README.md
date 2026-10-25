# Seed dữ liệu demo RTMS

Bộ seed này thay cho file dump và các script seed local cũ. Nó dựng lại toàn bộ dữ liệu demo cho mọi actor
(ngựa, đơn nhập học, nhân viên, chuồng, huấn luyện, Thú y, Groom, đăng ký giải) trên PostgreSQL local.

- Dữ liệu trải từ khoảng 5 tháng trước tới vài tuần sau, tính theo **ngày bạn chạy seed**. Chạy hôm nào thì dữ liệu "kết thúc" đúng hôm đó.
- Seed xóa sạch dữ liệu cũ trước khi nạp. Chạy lại bao nhiêu lần cũng ra cùng một bộ dữ liệu.
- Toàn bộ chạy trong một transaction. Cuối cùng có hơn 50 kiểm tra luật nghiệp vụ; vi phạm thì dừng và DB giữ nguyên.
- Đây là dữ liệu tối ưu cho **cấu trúc bảng hiện tại**. Ai đổi bảng của mình (migration mới) thì sửa luôn seed (xem mục "Sửa seed").

## Chạy

Đứng ở thư mục `backend`:

```powershell
# 1. Chép file hồ sơ nhập học (ảnh + PDF) vào backend\uploads\admissions. Bỏ qua nếu đã đủ và không đổi.
.\scripts\seed-admission-files.ps1

# 2. Seed database (hỏi xác nhận; thêm -Yes để bỏ hỏi).
.\scripts\seed-database.ps1
```

- `seed-admission-files.ps1` tự tìm thư mục `RTMS_Admission_Packs_<ngày>` nằm cạnh thư mục repo (hoặc dùng `-Source <đường dẫn>`).
  Nó so dấu vân tay SHA-256 của toàn bộ file nguồn. Nếu giống lần trước và đủ file thì bỏ qua. Nếu khác thì **xóa sạch**
  thư mục upload rồi chép lại, nên thư mục không bao giờ phình gấp đôi. Thêm `-Force` để luôn chép lại.
- `seed-database.ps1` đọc kết nối từ `src/main/resources/application-local.properties` (giống backend), hoặc từ tham số
  `-DbHost -Port -Database -User -Password`. Nó từ chối chạy khi:
  - DB chưa có bảng hoặc còn migration chưa áp. Khi đó chạy backend một lần cho Flyway làm trước.
  - DB không nằm trên máy (localhost). Muốn chạy ở nơi khác phải thêm `-AllowRemote`.
- Hai script độc lập nhau. Chạy lại seed DB không cần chép lại file, vì tên file trong DB suy ra cố định từ bộ hồ sơ.

## Tài khoản (mật khẩu `admin123`)

| Vai trò | Tài khoản chính | Khác |
|---|---|---|
| Club Manager | admin@rtms.com | |
| Horse Owner | owner@rtms.com | owner2..owner8@rtms.com |
| Head Trainer | trainer1@rtms.com (khu A, B) | trainer2 (C, D), trainer3 (E) |
| Veterinarian | vet1@rtms.com | vet2, vet3 |
| Groom | groom1@rtms.com (chuồng A01-A03) | groom2, 22 Groom `@demo.rtms.example` khác, 1 Groom đã nghỉ (bị khóa) |

## Có gì trong dữ liệu

- **Nhập học:** 55 đơn ở đủ trạng thái: GROOM_REVIEW, WAITING_FOR_ARRIVAL, ARRIVAL_EXPIRED, VET_REVIEW, TRAINER_REVIEW,
  MANAGER_REVIEW, APPROVED, bị Groom từ chối, bị Manager từ chối rồi nộp lại và được duyệt. Mỗi đơn có đủ ảnh và PDF.
  Có ngựa bị Thú y cho tạm nghỉ ngay lúc nhập trại, và ngựa có bố là một ngựa khác trong trại (phả hệ liên kết).
- **Thú y:** khám nhập trại; tiêm phòng / tẩy giun định kỳ theo phác đồ (gồm một lần hoãn tiêm, tiêm từ "To do", một lịch
  quá giờ chưa khám); sự cố do Groom báo ở mọi trạng thái (đang khám, vừa giao, đã xong, báo trùng bị bác).
  Còn có: ngựa đang tạm nghỉ chờ tái khám (có lịch đã quá hạn), chuỗi tái khám tự động +14 ngày, Manager hủy lịch tái khám
  thừa, chấn thương (kể cả bản bị hiệu chỉnh), phác đồ điều trị ACTIVE / COMPLETED / CANCELLED.
- **Sinh hiệu:** Thú y đo mỗi ngựa một lần mỗi tuần, mỗi ngựa một ngày riêng, đôi khi lệch nhẹ. Ngựa đang tạm nghỉ được
  đo hằng ngày với số đo bất thường tới khi khỏi; ngựa có buổi tập mệt thì chiều hôm đó nhịp tim cao.
- **Huấn luyện:** 3 Trainer, mỗi người có bài tập và khóa riêng. Khóa có nhiều kiểu: một bài lặp lại, nhiều bài xoay vòng,
  toàn bài khác nhau, khóa chưa ai học. Có khoảng 13 tuần lịch sử: plan COMPLETED / ACTIVE / UPCOMING, plan bị hủy vì ngựa
  bị chặn tập, lot bị Trainer hủy vì mưa, buổi đã qua chưa ghi kết quả, ngựa có dấu hiệu mệt. Điểm phong độ trải từ thấp
  tới cao và tính đúng công thức của app.
- **Groom:** 5 việc SOP mỗi ngày trong 14 ngày gần nhất, theo đúng Groom của chuồng ngựa đang ở; việc giao thêm; hoàn thành
  theo khối có ghi chú và ảnh; vài khối bị bỏ lỡ. groom1 còn việc hôm nay để thử tick.
- **Thi đấu:** đề cử PENDING / APPROVED / REJECTED.
- Thông báo và nhật ký hoạt động sinh ra như app thật.

## Cấu trúc

| File | Nội dung |
|---|---|
| `sql/seed.sql` | Chạy các file dưới đây theo thứ tự |
| `sql/00_helpers.sql` | Hàm dùng chung: dời ngày, tra id theo khóa, lịch Thú y, hoàn tất ca khám, mốc tiêm, thông báo, nhật ký |
| `sql/01_cleanup.sql` | TRUNCATE mọi bảng trừ bảng danh mục do migration nạp |
| `sql/10_data.sql` | Dữ liệu định danh: tài khoản, khu, ngựa, đơn, file hồ sơ |
| `sql/20_accounts_stable.sql` | Tài khoản, hồ sơ nhân viên, khu, chuồng, phân Groom |
| `sql/30_admissions.sql` | Luồng nhập học từng đơn |
| `sql/40_vet_cases.sql` | Ca Thú y sau nhập trại |
| `sql/45_preventive.sql` | Tiêm phòng tới hôm nay |
| `sql/50_training.sql` | Bài tập, khóa, plan, lot, buổi tập |
| `sql/60_groom.sql` | Việc hằng ngày của Groom |
| `sql/65_vitals.sql` | Chỉ số sinh hiệu Thú y đo khi đi buồng |
| `sql/70_racing.sql` | Đề cử thi đấu |
| `sql/99_verify.sql` | Kiểm tra luật nghiệp vụ |

## Sửa seed

1. **Đổi bảng (migration mới)**: thêm migration như thường lệ, chạy backend cho Flyway áp, rồi sửa file seed của phần mình
   cho khớp cột mới. Chạy `seed-database.ps1`: lỗi SQL hoặc vi phạm luật đều hiện ra ngay, DB không bị ghi dở.
2. **Thêm dữ liệu**: viết vào file của phần mình (hoặc tạo `NN_ten.sql` mới và thêm một dòng `\ir` vào `seed.sql`).
   - Không dùng id cứng. Dùng `pg_temp.uid('GR01')`, `pg_temp.hid('H001')`, `pg_temp.aid('H001')`, `pg_temp.stall('A01')`.
     Khóa tài khoản / ngựa nằm trong `10_data.sql`.
   - Ngày giờ viết tương đối với hôm nay: `pg_temp.at(-3, TIME '14:00')`, `pg_temp.today() + 5`, `pg_temp.clock()`.
     Dữ liệu trong `10_data.sql` viết theo ngày gốc 2026-10-25 và được dời bằng `pg_temp.d()` / `pg_temp.ts()`.
   - Đi qua hàm có sẵn thay vì INSERT tay khi có thể. Ví dụ `pg_temp.book` / `pg_temp.complete_care` cho lịch Thú y,
     `pg_temp.cohort` cho plan, `pg_temp.report_incident` cho sự cố. Các hàm này giữ luật giống service.
3. **Luật của phần mình**: thêm một dòng vào `99_verify.sql` (tên luật + câu SELECT trả về các dòng vi phạm).
4. **Bảng danh mục mới do migration nạp** (như `preventive_protocols`): thêm tên bảng vào `v_keep` trong `01_cleanup.sql`,
   nếu không seed sẽ xóa nó.
5. Muốn thử seed ở ngày / giờ khác (ví dụ trước buổi demo): `.\scripts\seed-database.ps1 -AsOf '2026-11-02 08:00'`.
   Chỉ để thử: chạy thật thì để seed lấy giờ hiện tại.

`10_data.sql` được sinh một lần từ `seed-preview.json` của bộ hồ sơ. Từ giờ sửa thẳng trên file SQL.
