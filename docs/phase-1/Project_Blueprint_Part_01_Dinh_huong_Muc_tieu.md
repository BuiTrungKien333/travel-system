# Project Blueprint — Phần 1: Định hướng & Mục tiêu

> **Trạng thái:** Đã chốt  
> **Phiên bản:** 1.0 — MVP  
> **Phạm vi:** Toàn bộ Việt Nam

---

## 1.1. Tên đề tài

**Xây dựng hệ thống hỗ trợ lập kế hoạch và quản lý hành trình du lịch cá nhân tích hợp AI gợi ý lộ trình, thời tiết và nhật ký trải nghiệm**

> Tên đề tài có thể được tinh chỉnh sau khi hoàn thiện toàn bộ Blueprint để bảo đảm phản ánh chính xác phạm vi MVP.

---

## 1.2. Định hướng sản phẩm

Hệ thống là một nền tảng hỗ trợ người dùng quản lý **toàn bộ trải nghiệm của một chuyến du lịch**, tập trung vào ba giai đoạn:

1. **Trước chuyến đi:** lập kế hoạch và xây dựng lịch trình.
2. **Trong chuyến đi:** hỗ trợ theo dõi lịch trình, bản đồ, thời tiết và ghi nhận trải nghiệm.
3. **Sau chuyến đi:** lưu giữ nhật ký, hình ảnh và xem lại hành trình.

Sản phẩm hướng tới mô hình:

**Plan → Explore → Capture → Remember**

Trong đó bốn giai đoạn được liên kết thành một hành trình thống nhất thay vì hoạt động như các chức năng độc lập.

---

## 1.3. Đối tượng người dùng

### Người dùng chính

Khách du lịch **cá nhân**, đặc biệt là những người tự tổ chức chuyến đi và muốn có một công cụ hỗ trợ lập kế hoạch, theo dõi hành trình và lưu giữ trải nghiệm.

### Người dùng mở rộng

Nhóm bạn bè hoặc gia đình có thể sử dụng hệ thống cho một chuyến đi chung.

Tuy nhiên, **MVP ưu tiên trải nghiệm cá nhân**. Các chức năng cộng tác nhóm nâng cao chưa thuộc phạm vi bắt buộc của phiên bản đầu tiên.

---

## 1.4. Phạm vi địa lý

Hệ thống được định hướng hỗ trợ **toàn bộ Việt Nam**.

Người dùng có thể lập kế hoạch và quản lý hành trình tại các tỉnh/thành phố trên toàn quốc.

MVP có thể xây dựng bản đồ hành trình cá nhân, ví dụ:

> **25/63 tỉnh thành đã khám phá**

Việc xác định danh sách tỉnh/thành và cách tính thống kê sẽ được chuẩn hóa ở phần thiết kế dữ liệu.

---

## 1.5. Mục tiêu tổng quát

> **Xây dựng hệ thống hỗ trợ người dùng lập kế hoạch, thực hiện và quản lý hành trình du lịch cá nhân tại Việt Nam, tích hợp AI, dữ liệu bản đồ và thời tiết để đưa ra các đề xuất phù hợp với nhu cầu và điều kiện thực tế, đồng thời cung cấp khả năng ghi lại và nhìn lại những trải nghiệm trong chuyến đi.**

---

## 1.6. Mục tiêu cụ thể

### Mục tiêu 1 — Lập kế hoạch chuyến đi

Cho phép người dùng:

- Tạo chuyến đi.
- Xác định thời gian.
- Chọn điểm xuất phát.
- Chọn các địa điểm muốn tham quan.
- Chọn phương tiện.
- Thiết lập một số sở thích hoặc yêu cầu cơ bản.

### Mục tiêu 2 — Gợi ý và sắp xếp lịch trình

Hệ thống hỗ trợ sắp xếp các địa điểm thành một lịch trình hợp lý dựa trên các yếu tố chính:

- Khoảng cách giữa các địa điểm.
- Thời gian di chuyển.
- Thời gian tham quan dự kiến.
- Thời gian hoạt động của địa điểm nếu có dữ liệu.
- Thời gian khả dụng của chuyến đi.
- Điều kiện thời tiết.

### Mục tiêu 3 — Ứng dụng AI

AI được sử dụng ở mức phù hợp với MVP để:

- Gợi ý lịch trình.
- Gợi ý hoặc lựa chọn địa điểm phù hợp với nhu cầu.
- Giải thích lý do của một đề xuất.
- Hỗ trợ điều chỉnh lịch trình khi có thay đổi.

AI đóng vai trò **hỗ trợ ra quyết định**, người dùng vẫn là người quyết định cuối cùng.

### Mục tiêu 4 — Hỗ trợ trong chuyến đi

Trong quá trình thực hiện chuyến đi, hệ thống cho phép người dùng:

- Xem lịch trình.
- Xem bản đồ và tuyến đường.
- Xem thông tin thời tiết.
- Đánh dấu/check-in địa điểm.
- Theo dõi tiến độ chuyến đi.

### Mục tiêu 5 — Lưu giữ trải nghiệm

Người dùng có thể:

- Lưu ảnh.
- Viết ghi chú/cảm nhận.
- Gắn trải nghiệm với địa điểm.
- Gắn trải nghiệm với chuyến đi.
- Xem lại những kỷ niệm đã lưu.

### Mục tiêu 6 — Bản đồ và thống kê hành trình

Hệ thống cung cấp:

- Bản đồ các địa điểm đã ghé.
- Lịch sử chuyến đi.
- Số địa điểm đã khám phá.
- Số tỉnh/thành đã khám phá.
- Tổng hợp hình ảnh và kỷ niệm.

Các tính năng thành tích/gamification nâng cao có thể để **sau MVP** nếu còn thời gian.

---

## 1.7. Giá trị cốt lõi

| Giai đoạn | Giá trị |
|---|---|
| **Plan** | Lập kế hoạch và nhận đề xuất |
| **Explore** | Theo dõi bản đồ, lịch trình và thời tiết |
| **Capture** | Lưu ảnh và trải nghiệm |
| **Remember** | Xem lại nhật ký và hành trình |

Mục tiêu của hệ thống là kết nối bốn giai đoạn trên thành một trải nghiệm liền mạch.

---

## 1.8. Nguyên tắc thiết kế MVP

Để phù hợp với dự án cá nhân/solo developer, MVP tuân thủ các nguyên tắc:

### 1. Tập trung vào core journey

Ưu tiên hoàn thiện luồng:

**Tạo chuyến đi → Chọn địa điểm → Gợi ý lịch trình → Theo dõi chuyến đi → Check-in/lưu ảnh → Xem lại hành trình**

### 2. AI vừa đủ nhưng có giá trị

Không xây dựng hệ thống AI quá phức tạp hoặc huấn luyện mô hình riêng trong MVP.

Ưu tiên sử dụng AI như một lớp hỗ trợ trên dữ liệu bản đồ, thời tiết và thông tin chuyến đi.

### 3. Không xây mạng xã hội ở MVP

Các chức năng như:

- Follow người dùng.
- Feed cộng đồng.
- Like/comment.
- Chat.
- Mạng xã hội du lịch.

Không thuộc phạm vi MVP.

### 4. Nhóm du lịch chỉ là phạm vi mở rộng

MVP có thể thiết kế dữ liệu đủ linh hoạt để mở rộng cho nhóm, nhưng **không bắt buộc triển khai cộng tác thời gian thực hoặc phân quyền phức tạp**.

### 5. Gamification ở mức tối thiểu

Có thể triển khai thống kê như:

> **25/63 tỉnh thành**

nhưng các hệ thống thành tích, huy hiệu, streak hoặc phần thưởng có thể để sau MVP.

---

## 1.9. Phạm vi MVP dự kiến

### Bắt buộc

- Quản lý tài khoản cơ bản.
- Tạo và quản lý chuyến đi.
- Quản lý danh sách địa điểm.
- Hiển thị bản đồ.
- Gợi ý/sắp xếp lịch trình.
- Tích hợp dữ liệu thời tiết.
- AI hỗ trợ lập lịch trình.
- Theo dõi lịch trình.
- Check-in địa điểm.
- Lưu ảnh và ghi chú.
- Nhật ký chuyến đi.
- Bản đồ địa điểm đã khám phá.
- Thống kê cơ bản.

### Chưa ưu tiên

- Mạng xã hội.
- Chat.
- Cộng tác nhóm thời gian thực.
- Đặt vé/khách sạn.
- Thanh toán.
- AI tự động phân tích ảnh chuyên sâu.
- AI voice assistant.
- Gamification phức tạp.
- Hệ thống recommendation machine learning riêng.

---

## 1.10. Tầm nhìn

> **Xây dựng một nền tảng giúp người dùng không chỉ đi du lịch dễ dàng hơn mà còn hình thành một bản đồ ký ức về những hành trình của chính mình.**

Hệ thống hướng tới việc trả lời ba câu hỏi:

> **Tôi nên đi đâu?**  
> **Tôi nên đi như thế nào?**  
> **Tôi đã trải nghiệm những gì?**

---

## 1.11. Tóm tắt định hướng

```text
                    TRAVEL SYSTEM
                          │
            ┌─────────────┼─────────────┐
            │             │             │
          BEFORE        DURING         AFTER
            │             │             │
            ▼             ▼             ▼
        Lập kế hoạch   Theo dõi       Nhật ký
        AI đề xuất     Bản đồ         Hình ảnh
        Lịch trình     Thời tiết      Kỷ niệm
                       Check-in       Thống kê
            │             │             │
            └─────────────┼─────────────┘
                          ▼
                HÀNH TRÌNH CÁ NHÂN
```

**Mục tiêu của MVP:** xây dựng được một vòng đời chuyến đi hoàn chỉnh từ **lập kế hoạch → thực hiện → lưu giữ trải nghiệm → xem lại hành trình**, thay vì cố gắng xây dựng một nền tảng du lịch quá lớn ngay từ phiên bản đầu tiên.
