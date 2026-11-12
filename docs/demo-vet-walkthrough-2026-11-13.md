# Kịch bản Demo Luồng Thú Y (Veterinarian Walkthrough) — 13/11/2026

Tài liệu hướng dẫn demo toàn diện luồng Thú Y gồm 3 phân hệ cốt lõi:
1. **Khám nhập trại (Admission Review)**
2. **Khám định kỳ lâm sàng (Routine Care)**
3. **Tiêm phòng & Tẩy giun định kỳ (Preventive Care)**

---

## 1. Thông tin tài khoản & Chuẩn bị môi trường

* **Tài khoản Bác sĩ chính**: `vet1@rtms.com` / Mật khẩu: `admin123` (Dr. James Carter - VT01)
* **Bác sĩ phối hợp**:
  * `vet2@rtms.com` (Dr. Emily Wilson - VT02)
  * `vet3@rtms.com` (Dr. Hannah Reed - VT03)
* **Lệnh seed lại dữ liệu bất cứ lúc nào**:
  ```powershell
  powershell -File "backend\scripts\seed-database.ps1" -Yes -Port 5434 -Password 123456
  ```

---

## 2. Kịch bản 1: Khám lâm sàng Nhập trại (Admission Intake Exam)

> **Mục tiêu**: Chứng minh quy trình tiếp nhận ngựa mới từ khu cách ly, khám đạt chuẩn 6 chỉ số sinh hiệu và phê duyệt nhập trại.

### Các bước thực hiện:
1. Đăng nhập tài khoản `vet1@rtms.com`.
2. Truy cập menu **Admissions** (`/veterinarian/admissions`).
3. Tại danh sách chờ duyệt, chọn đơn của ngựa **FUTURA D AMIATA** (Mã hồ sơ `#52`, Mã ngựa `#47`, Chuồng cách ly: `Q-02`).
4. Bấm **Conduct Exam** để mở form khám lâm sàng nhập trại.
5. **Nhập bảng thông số sinh hiệu chuẩn**:

| Trường thông tin | Giá trị nhập | Đơn vị / Định dạng | Ý nghĩa lâm sàng |
| :--- | :--- | :--- | :--- |
| **Nhiệt độ (Temperature)** | `37.8` | °C (Chuẩn 37.0 - 38.5) | Bình thường, không sốt |
| **Nhịp tim (Heart Rate)** | `38` | bpm (Chuẩn 28 - 44) | Nhịp tim nghỉ ổn định |
| **Nhịp thở (Respiratory Rate)**| `14` | breaths/min (Chuẩn 10 - 24) | Nhịp thở đều, êm |
| **Thể trạng (BCS)** | `5.5` | Thang điểm 1 - 9 | Thể trạng lý tưởng cho huấn luyện |
| **Cân nặng (Weight)** | `465.0` | kg | Trọng lượng phù hợp lứa tuổi |
| **Trạng thái nước (Hydration)**| `NORMAL` | Tuyển chọn | Da đàn hồi tốt, CRT < 2s |
| **Quyết định (Decision)** | `ALLOWED` | Phê duyệt | Đủ điều kiện thể lực nhập đàn |
| **Ghi chú lâm sàng** | Ngựa đáp ứng tốt, mắt sáng, niêm mạc hồng hào, không có dấu hiệu chấn thương cơ xương khớp. | Ghi chú văn bản | Đạt yêu cầu cách ly |

6. Bấm **Submit Review**.
7. **Giải thích cho người nghe / Thầy cô**:
   * Hệ thống tự động chuyển trạng thái đơn sang bước tiếp theo của Quản lý (`MANAGER_REVIEW`) hoặc Huấn luyện viên (`TRAINER_REVIEW`).
   * Trạng thái ngựa được cập nhật, sẵn sàng phân bổ chuồng nuôi chính thức.

---

## 3. Kịch bản 2: Khám định kỳ lâm sàng (Routine Care) & Cơ chế Bucket

> **Mục tiêu**: Chứng minh quy tắc kiểm soát khung giờ vàng `13:30 - 15:30`, badge dời lịch `Rolled 1x`, bộ đếm hạn ngạch ngày `2/3 -> 3/3`.

### Các bước thực hiện:

#### Bước 2.1: Demo kiểm soát ngoài khung giờ (Nếu máy đang ngoài 13:30 - 15:30)
1. Truy cập **Daily Schedule** (`/veterinarian/schedule`).
2. Quan sát thanh tổng quan hạn ngạch:
   * **Routine Care Bucket**: hiển thị `2/3` kèm pill `Window 13:30 – 15:30`.
3. Nhìn danh sách ca khám hôm nay:
   * Ca 1: Ngựa **OR MAKOR** (Lịch thường).
   * Ca 2: Ngựa **NAUSICAA BY NIKA** kèm huy hiệu **Rolled 1x** (`TIME_EXCEEDED` - do hôm qua hết giờ khám nên tự động dời sang hôm nay).
4. Di chuột vào nút **Examine** của ca khám: Nút bị vô hiệu hóa hoặc có tooltip cảnh báo:
   * *"Routine examinations can only be started between 13:30 and 15:30"*.
   * **Thuyết minh**: *"Hệ thống bảo vệ quyền lợi và sinh hoạt của ngựa, ca khám định kỳ không được diễn ra buổi sáng để tránh trùng giờ tập huấn luyện."*

#### Bước 2.2: Demo thao tác khám trong khung giờ
1. **Chỉnh giờ đồng hồ máy tính sang 14:00** (hoặc thời điểm bất kỳ trong khoảng `13:30 - 15:30`).
2. Tải lại trang (`F5`). Nút **Examine** sáng lên, cho phép bắt đầu khám.
3. Bấm **Examine** tại ca ngựa **NAUSICAA BY NIKA** (Mã lịch `#116`). Trạng thái chuyển sang `IN_PROGRESS`.
4. Bấm **Complete Exam** và điền kết quả lâm sàng:

| Trường thông tin | Giá trị nhập | Ghi chú |
| :--- | :--- | :--- |
| **Temperature** | `37.9` | °C |
| **Heart Rate** | `36` | bpm |
| **Respiratory Rate** | `15` | breaths/min |
| **BCS** | `6.0` | Thể trạng tốt |
| **Weight** | `480.0` | kg |
| **Hydration** | `NORMAL` | Bình thường |
| **Decision** | `ALLOWED` | Tiếp tục tập luyện bình thường |
| **Summary** | Kiểm tra định kỳ sau đợt huấn luyện tải cao, khớp gối ổn định, không viêm. | Hoàn tất |

5. Bấm **Save & Complete**.
6. **Thuyết minh kết quả**:
   * Bộ đếm **Routine Care Bucket** nhảy lên **3/3** (Đạt trần hạn ngạch ngày).
   * Hệ thống ghi nhận hoàn thành đúng quy chuẩn, bảo vệ bác sĩ không bị quá tải.

---

## 4. Kịch bản 3: Tiêm phòng & Tẩy giun định kỳ (Preventive Care)

> **Mục tiêu**: Thể hiện quy trình tiêm phòng/tẩy giun đúng lịch, sinh mốc kế tiếp tự động, và tính năng chia sẻ tải ca khám `Transferred from VT02`.

### Các bước thực hiện:
1. Truy cập menu **Preventive Care** (`/veterinarian/preventive-care`).
2. Quan sát danh sách ca tiêm/tẩy giun hôm nay của Dr. James Carter:
   * Ca 1: Ngựa **BADEA MAFAL** (Mã ca `#24` - Gói: *Equine Encephalomyelitis EEE/WEE*).
   * Ca 2: Ngựa **WILD IVANA** (Mã ca `#147` - Gói: *Deworming*).
   * Điểm nhấn đặc biệt: Ca `#147` có huy hiệu **Transferred / San sẻ từ Dr. Emily Wilson (VT02)**:
     * **Thuyết minh**: *"Khi bác sĩ Emily Wilson phụ trách khu C gặp ca cấp cứu khẩn cấp (URGENT), hệ thống tự động cân bằng tải và san sẻ bớt ca tiêm ngừa sang cho Dr. James Carter để không bị trễ hạn tiêm."*
3. Bấm vào ca của ngựa **WILD IVANA** (Mã `#147`) để ghi nhận:
   * Phác đồ: **Deworming (Tẩy giun)**.
   * Ngày thực hiện: Hôm nay (`13/11/2026`).
   * Liều dùng: Ghi chú loại thuốc tẩy giun định kỳ (Ivermectin paste).
4. Bấm **Record Vaccination / Complete**.
5. **Thuyết minh kết quả**:
   * Ca tiêm hoàn tất.
   * Hệ thống tự động tính chu kỳ `90 ngày` và sinh một mốc tiêm tiếp theo vào tháng 02/2027 trên lịch chăm sóc của ngựa.

---

## 5. Bảng tổng hợp dữ liệu Demo hôm nay (13/11/2026)

| Bác sĩ | Loại ca | Tên ngựa | Mã ca | Tình trạng / Badge | Mục đích demo |
| :--- | :--- | :--- | :--- | :--- | :--- |
| **Dr. James Carter (VT01)** | Admission | FUTURA D AMIATA | `#97` | Sẵn sàng khám | Form 6 chỉ số + Duyệt nhập trại |
| **Dr. James Carter (VT01)** | Routine | NAUSICAA BY NIKA | `#116`| **Rolled 1x** (TIME_EXCEEDED) | Demo dời lịch tự động + Khung 13:30-15:30 |
| **Dr. James Carter (VT01)** | Routine | OR MAKOR | `#92` | Chờ khám bình thường | Bucket counter 2/3 -> 3/3 |
| **Dr. James Carter (VT01)** | Preventive | WILD IVANA | `#147`| **PEER_OFFLOAD** (Transferred) | San sẻ tải từ VT02 do ca khẩn |
| **Dr. James Carter (VT01)** | Preventive | BADEA MAFAL | `#24` | Chờ tiêm định kỳ | Tiêm chủng sinh mốc tự động |
| **Dr. Emily Wilson (VT02)** | Urgent | DOUGLAS | `#101`| Đang xử lý khẩn cấp | Phối hợp nhóm Thành (Cấp cứu) |
| **Dr. Emily Wilson (VT02)** | Routine | MYSTICA MAHRIACHI | `#106`| Rolled 1x | Lịch dày của VT02 |
| **Dr. Hannah Reed (VT03)** | Routine | SANTIAGO EGA | `#113`| Rolled 1x | Lịch dày của VT03 |
| **Dr. Hannah Reed (VT03)** | Preventive | SANTIAGO EGA | `#206`| Chờ tiêm chủng | Lịch dày của VT03 |
