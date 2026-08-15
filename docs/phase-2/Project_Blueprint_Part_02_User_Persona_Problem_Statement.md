# Project Blueprint — Phần 2: User Persona & Problem Statement

> **Trạng thái:** Đã chốt  
> **Phiên bản:** 1.0 — MVP

---

## 2.1. Định hướng UX

UX là một nguyên tắc xuyên suốt của hệ thống.

Đối tượng người dùng hiện đại thường ưu tiên:

- Nhanh.
- Đơn giản.
- Ít thao tác.
- Dễ hiểu.
- Hiệu quả.
- Có thể sử dụng ngay mà không cần học nhiều.

Vì vậy, hệ thống ưu tiên nguyên tắc:

> **Quick → Simple → Useful**

Mọi chức năng nên được thiết kế sao cho người dùng đạt được mục tiêu với **ít bước và ít thông tin nhập nhất có thể**.

### Nguyên tắc UX chính

1. Không yêu cầu người dùng nhập quá nhiều thông tin ngay từ đầu.
2. Chỉ hỏi thêm thông tin khi thông tin đó thực sự cần thiết.
3. Ưu tiên lựa chọn có sẵn thay vì bắt người dùng nhập thủ công.
4. Tự động lấy dữ liệu có thể lấy được từ hệ thống/API.
5. Đưa ra kết quả rõ ràng, trực quan và dễ hành động.
6. Các đề xuất của AI phải dễ hiểu và có thể chỉnh sửa.
7. Không biến các chức năng nâng cao thành rào cản đối với người dùng mới.

---

## 2.2. Persona chính — Người du lịch cá nhân

### Hồ sơ

**Tên đại diện:** Minh  
**Độ tuổi:** 18–30  
**Hình thức du lịch:** Tự túc  
**Tần suất:** Một vài chuyến mỗi năm hoặc tùy nhu cầu  
**Thiết bị:** Smartphone  
**Đặc điểm:** Thích khám phá địa điểm mới nhưng không muốn mất nhiều thời gian để chuẩn bị và quản lý chuyến đi.

### Hành vi hiện tại

Khi chuẩn bị chuyến đi, người dùng thường phải kết hợp nhiều công cụ:

```text
Tìm địa điểm
    ↓
Google / TikTok / Facebook
    ↓
Lưu địa điểm yêu thích
    ↓
Mở bản đồ
    ↓
Tự xem khoảng cách
    ↓
Tự sắp xếp thứ tự
    ↓
Kiểm tra thời tiết
    ↓
Ghi chú lịch trình
```

Trong chuyến đi:

```text
Maps
+
Weather
+
Camera
+
Gallery
+
Notes
```

Trải nghiệm vì vậy bị phân tán trên nhiều ứng dụng.

---

## 2.3. Các vấn đề chính của Persona 1

### Problem 1 — Khó xây dựng lịch trình hợp lý

Người dùng có thể biết:

> "Tôi muốn đi A, B, C, D."

Nhưng không biết thứ tự nào hợp lý nhất dựa trên:

- khoảng cách;
- thời gian;
- thời gian tham quan;
- điều kiện thời tiết.

Người dùng thường phải tự thử nhiều phương án.

### Problem 2 — Lịch trình không gắn với điều kiện thực tế

Thời tiết có thể thay đổi kế hoạch.

Ví dụ:

```text
08:00 — Đi biển
14:00 — Đi tham quan ngoài trời

Nhưng dự báo:
08:00 — ☀️
14:00 — 🌧️
```

Người dùng phải tự phát hiện vấn đề và tự điều chỉnh.

### Problem 3 — Thông tin chuyến đi bị phân tán

Người dùng phải chuyển đổi giữa nhiều ứng dụng để:

- tìm địa điểm;
- xem bản đồ;
- xem thời tiết;
- ghi chú;
- lưu ảnh.

Không có một nơi thống nhất để quản lý chuyến đi.

### Problem 4 — Khó quản lý và tìm lại kỷ niệm

Sau chuyến đi, người dùng có thể có rất nhiều ảnh nhưng khó nhớ:

- ảnh được chụp ở đâu;
- trong chuyến đi nào;
- vào thời điểm nào;
- trải nghiệm gì tại địa điểm đó.

### Problem 5 — Không có cái nhìn tổng quan về hành trình đã khám phá

Người dùng có thể đã đi qua nhiều tỉnh/thành nhưng không có một hệ thống trực quan để theo dõi:

> "Tôi đã khám phá bao nhiêu nơi?"

Ví dụ:

> **25/63 tỉnh thành đã khám phá.**

---

## 2.4. Persona phụ — Nhóm bạn / gia đình

Nhóm bạn hoặc gia đình được xác định là **hướng mở rộng trong tương lai**, không thuộc phạm vi phát triển chính của MVP.

### Định hướng tương lai

Sau khi hệ thống cá nhân hoạt động ổn định, có thể phát triển khái niệm:

> **Travel Group**

Một nhóm người cùng tham gia một chuyến đi có thể:

- cùng tham gia một hành trình;
- chia sẻ hình ảnh;
- chia sẻ trải nghiệm;
- xem các kỷ niệm của những thành viên khác;
- hình thành một album/hành trình chung.

Ví dụ:

```text
Travel Group
     │
     ├── User A
     │     └── Ảnh + trải nghiệm
     │
     ├── User B
     │     └── Ảnh + trải nghiệm
     │
     └── User C
           └── Ảnh + trải nghiệm
```

### Nguyên tắc MVP

MVP **không triển khai**:

- cộng tác nhóm;
- realtime synchronization;
- chat nhóm;
- phân quyền nhóm phức tạp;
- chia sẻ trải nghiệm theo thời gian thực.

Tuy nhiên, kiến trúc dữ liệu về sau nên có khả năng mở rộng để hỗ trợ tính năng này mà không phải xây dựng lại toàn bộ hệ thống.

---

## 2.5. Problem Statement

> **Người dùng du lịch tự túc hiện phải sử dụng nhiều công cụ khác nhau để tìm kiếm địa điểm, lập lịch trình, kiểm tra thời tiết, điều hướng và lưu giữ kỷ niệm. Việc thiếu một hệ thống thống nhất khiến quá trình chuẩn bị và quản lý chuyến đi trở nên phân tán, người dùng khó điều chỉnh lịch trình khi điều kiện thực tế thay đổi và khó xem lại hành trình sau chuyến đi.**

Hệ thống hướng tới giải quyết vấn đề này bằng cách kết hợp:

```text
Lập kế hoạch
     +
Bản đồ
     +
Thời tiết
     +
AI
     +
Nhật ký
     +
Bản đồ hành trình
```

trong một trải nghiệm thống nhất.

---

## 2.6. Mapping vấn đề → giải pháp

| Vấn đề | Giải pháp MVP |
|---|---|
| Không biết sắp xếp địa điểm | AI + thuật toán gợi ý lịch trình |
| Tuyến đường chưa hợp lý | Bản đồ + dữ liệu khoảng cách |
| Thời tiết ảnh hưởng kế hoạch | Weather API |
| Thông tin bị phân tán | Quản lý tập trung trong một chuyến đi |
| Khó lưu kỷ niệm | Nhật ký + ảnh + địa điểm |
| Khó tìm lại ảnh cũ | Kỷ niệm gắn với địa điểm/chuyến đi |
| Không biết đã khám phá bao nhiêu | Bản đồ hành trình + thống kê |

---

## 2.7. User Journey — Happy Path

```text
Người dùng muốn đi du lịch
        ↓
Tạo chuyến đi
        ↓
Chọn thời gian
        ↓
Chọn / tìm địa điểm
        ↓
AI đề xuất lịch trình
        ↓
Người dùng xác nhận / chỉnh sửa
        ↓
Bắt đầu chuyến đi
        ↓
Xem lịch trình + bản đồ + thời tiết
        ↓
Check-in tại địa điểm
        ↓
Chụp ảnh + ghi chú
        ↓
Hoàn thành chuyến đi
        ↓
Nhật ký chuyến đi
        ↓
Bản đồ hành trình cá nhân
        ↓
Xem lại kỷ niệm
```

Đây là **happy path cốt lõi của MVP**.

---

## 2.8. Nguyên tắc thiết kế luồng tạo chuyến đi

Không nên bắt người dùng nhập quá nhiều thông tin ngay khi bắt đầu.

### Cách tiếp cận đề xuất

**Bước 1**

> Bạn muốn đi đâu?

**Bước 2**

> Bạn đi trong thời gian nào?

**Bước 3**

> Bạn muốn khám phá những địa điểm nào?

**Bước 4**

> AI tạo lịch trình.

Các thông tin khác chỉ được yêu cầu khi thực sự cần thiết.

Ví dụ:

```text
Người dùng
    ↓
"Đà Nẵng"
    ↓
15/08 → 17/08
    ↓
Chọn địa điểm
    ↓
AI lập lịch
```

Thay vì bắt người dùng hoàn thành một form dài trước khi nhìn thấy kết quả.

---

## 2.9. UX Principle — Progressive Disclosure

Hệ thống áp dụng nguyên tắc:

> **Chỉ hiển thị và yêu cầu thông tin khi người dùng thực sự cần đến nó.**

Ví dụ:

Không hỏi ngay:

- ngân sách;
- sở thích;
- mức độ vận động;
- thời gian tham quan từng địa điểm;
- phương tiện;

nếu những thông tin đó chưa cần thiết.

Thay vào đó:

```text
Thông tin cơ bản
       ↓
Tạo kết quả ban đầu
       ↓
Nếu cần cải thiện kết quả
       ↓
Yêu cầu thêm thông tin
       ↓
Cá nhân hóa sâu hơn
```

Điều này giúp giảm friction trong quá trình sử dụng.

---

## 2.10. UX Goal

Mục tiêu UX của MVP:

> **Người dùng mới có thể tạo một chuyến đi và nhận được lịch trình đầu tiên mà không cần đọc hướng dẫn dài hoặc điền một biểu mẫu phức tạp.**

Các tiêu chí UX sẽ được cụ thể hóa thành yêu cầu phi chức năng ở các phần sau.

---

## 2.11. Phạm vi người dùng sau MVP

```text
                    USER SYSTEM
                         │
             ┌───────────┴───────────┐
             │                       │
          MVP                     FUTURE
             │                       │
             ▼                       ▼
      👤 Cá nhân              👥 Travel Group
             │                       │
      Trip cá nhân             Shared Trip
      Nhật ký cá nhân          Shared Memories
      Bản đồ cá nhân           Group Album
      Thống kê cá nhân         Group Experience
```

MVP tập trung hoàn thiện **trải nghiệm cá nhân** trước khi mở rộng sang trải nghiệm nhóm.
