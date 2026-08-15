# Project Blueprint — Phần 3: Product Scope & MVP Boundary

> **Trạng thái:** Đã chốt  
> **Phiên bản:** 1.0 — MVP  
> **Định hướng:** Solo Developer / API-first / UX-first

---

## 3.1. Mục tiêu

Phần này xác định rõ phạm vi của MVP:

- MVP phải làm gì.
- MVP không làm gì.
- Thành phần nào tự phát triển.
- Thành phần nào ưu tiên tích hợp API/dịch vụ có sẵn.
- Những ý tưởng nào được giữ lại cho phiên bản tương lai.

Mục tiêu là xây dựng một MVP **đủ hoàn chỉnh để thể hiện giá trị cốt lõi nhưng không vượt quá khả năng triển khai của một solo developer**.

---

## 3.2. Nguyên tắc phát triển

### 3.2.1. UX-first

Hệ thống ưu tiên:

> **Nhanh → Gọn → Dễ hiểu → Hiệu quả**

Người dùng không nên có cảm giác đang "điền dữ liệu cho hệ thống", mà chỉ cần thực hiện những thao tác cần thiết để lên và trải nghiệm một chuyến đi.

Các nguyên tắc:

- Ít thao tác.
- Ít thông tin nhập thủ công.
- Ưu tiên lựa chọn có sẵn.
- Tự động lấy dữ liệu có thể lấy từ API.
- Kết quả rõ ràng, trực quan.
- Cho phép chỉnh sửa khi cần.
- Không ép người dùng sử dụng tính năng AI.

### 3.2.2. API-first

Nếu một thành phần không phải core business logic và đã có API/dịch vụ phù hợp, MVP ưu tiên **tích hợp thay vì tự xây dựng từ đầu**.

> **Build the product, integrate the infrastructure.**

Ví dụ:

- AI → AI API.
- Bản đồ → Map API.
- Routing → Routing API.
- Thời tiết → Weather API.
- Geocoding → Geocoding API.
- Lưu trữ ảnh → Cloud storage.

### 3.2.3. AI không phải nguồn dữ liệu thực tế duy nhất

AI chịu trách nhiệm chủ yếu về:

- đề xuất;
- phân tích;
- cá nhân hóa;
- giải thích;
- điều chỉnh lịch trình.

Các dữ liệu thực tế như khoảng cách, tuyến đường và thời tiết phải lấy từ nguồn dữ liệu/API phù hợp.

Ví dụ:

```text
Map API
    ↓
Khoảng cách / Route
    ↓
Backend
    ↓
AI API
    ↓
Đề xuất lịch trình
```

Không để AI tự "đoán" dữ liệu thực tế.

### 3.2.4. Không xây AI model riêng trong MVP

MVP không thực hiện:

- Training model.
- Fine-tuning model riêng.
- Xây dựng ML pipeline.
- Deploy model AI riêng.

Thay vào đó, hệ thống tích hợp các AI API như OpenAI API hoặc dịch vụ tương đương phù hợp với chi phí và yêu cầu dự án.

---

# 3.3. Core Product

MVP tập trung vào 5 năng lực chính:

1. **Trip Management**
2. **AI Itinerary**
3. **Map & Weather**
4. **Travel Diary**
5. **Journey Map & Statistics**

Luồng tổng quát:

```text
PLAN
  ↓
Trip Management
  ↓
AI Itinerary
  ↓
EXPLORE
  ↓
Map + Weather + Check-in
  ↓
CAPTURE
  ↓
Photos + Notes
  ↓
REMEMBER
  ↓
Travel Diary + Journey Map
```

---

# 3.4. Module 1 — User & Profile

## MVP

- Đăng ký.
- Đăng nhập.
- Hồ sơ cá nhân cơ bản.
- Avatar.
- Một số thông tin sở thích du lịch nếu cần cho AI.

## Không làm trong MVP

- Follow/follower.
- Friend system.
- Public social profile.
- Chat.
- Social feed.

---

# 3.5. Module 2 — Trip Management

Đây là module trung tâm của hệ thống.

## Chức năng MVP

### Tạo chuyến đi

Người dùng có thể xác định:

- Tên chuyến đi.
- Thời gian.
- Điểm xuất phát.
- Các địa điểm muốn tham quan.

Ví dụ:

```text
Đà Nẵng Trip

15/08 → 17/08

Điểm xuất phát:
Đà Nẵng

Địa điểm:
- Sơn Trà
- Mỹ Khê
- Ngũ Hành Sơn
- Hội An
```

### Quản lý chuyến đi

- Xem chuyến đi.
- Chỉnh sửa chuyến đi.
- Thêm địa điểm.
- Xóa địa điểm.
- Thay đổi thời gian.
- Xem trạng thái chuyến đi.

## Trạng thái

MVP sử dụng trạng thái đơn giản:

```text
PLANNING
    ↓
ONGOING
    ↓
COMPLETED
```

---

# 3.6. Module 3 — AI Itinerary

Đây là một trong những điểm nhấn của đề tài.

## Phương án AI

Không xây model riêng.

Kiến trúc:

```text
User Input
    ↓
Backend
    ↓
Travel Data
    +
Map Data
    +
Weather Data
    ↓
AI API
    ↓
Structured AI Response
    ↓
Backend Validation
    ↓
Frontend
```

## AI Feature 1 — Generate Itinerary

Input có thể gồm:

- Điểm đến.
- Ngày.
- Khoảng thời gian.
- Các địa điểm người dùng muốn đi.
- Phương tiện.
- Sở thích cơ bản nếu có.

Output:

- Thứ tự địa điểm.
- Thời gian dự kiến.
- Khoảng thời gian tham quan.
- Lý do đề xuất ở mức phù hợp.

## AI Feature 2 — Explain Recommendation

AI giải thích ngắn gọn tại sao lịch trình được đề xuất.

Ví dụ:

> "Nên tham quan Sơn Trà vào buổi sáng vì thời tiết thuận lợi hơn và địa điểm gần điểm xuất phát."

## AI Feature 3 — Adjust Itinerary

Khi điều kiện thay đổi, người dùng có thể yêu cầu AI đề xuất lại.

Ví dụ:

> "Chiều nay có mưa, hãy điều chỉnh lịch trình."

AI tạo phương án thay thế dựa trên dữ liệu hiện tại.

---

# 3.7. Cách sử dụng AI trong MVP

MVP sử dụng mô hình **User-driven + AI-assisted**.

Người dùng có thể:

1. Tự chọn địa điểm muốn đi.
2. Yêu cầu AI sắp xếp lịch trình.
3. AI có thể đề xuất thêm một số địa điểm phù hợp nếu người dùng muốn.
4. Người dùng luôn có quyền chấp nhận, chỉnh sửa hoặc bỏ đề xuất.

Ví dụ:

```text
Người dùng:
"Tôi muốn đi Sơn Trà, Mỹ Khê, Hội An."

        ↓

AI:
"Đề xuất:
08:00 Sơn Trà
11:00 Mỹ Khê
16:00 Hội An"

        ↓

Người dùng:
✓ Chấp nhận
✎ Chỉnh sửa
↻ Tạo lại
```

AI là công cụ hỗ trợ, không tự ý quyết định thay người dùng.

---

# 3.8. Module 4 — Map & Weather

## Map

MVP cần:

- Hiển thị bản đồ.
- Hiển thị địa điểm.
- Hiển thị vị trí người dùng.
- Hiển thị route.
- Hiển thị thứ tự các điểm trong itinerary.

Sử dụng Map API/dịch vụ bản đồ có sẵn.

## Routing

Sử dụng Routing API để lấy:

- Khoảng cách.
- Thời gian di chuyển.
- Tuyến đường.

Không tự xây hệ thống bản đồ hoặc routing engine.

## Weather

MVP cần:

- Thời tiết hiện tại.
- Dự báo thời tiết phù hợp với lịch trình.
- Nhiệt độ.
- Tình trạng thời tiết.
- Khả năng mưa.

Các tính năng khí tượng nâng cao để sau MVP.

---

# 3.9. Module 5 — Travel Diary

Mục tiêu là lưu giữ trải nghiệm nhưng vẫn giữ UX đơn giản.

Người dùng có thể lưu:

- Ảnh.
- Ghi chú/cảm nhận.
- Địa điểm.
- Thời gian.
- Check-in.

Cấu trúc trải nghiệm cơ bản:

```text
📍 Địa điểm
📅 Thời gian
📷 Ảnh
📝 Ghi chú
```

Ví dụ:

```text
📍 Sơn Trà
15/08/2026 — 09:32

[Ảnh]

"Buổi sáng trời rất đẹp,
view từ trên núi rất chill."
```

## Không làm trong MVP

- Video editor.
- AI phân loại ảnh chuyên sâu.
- AI nhận diện địa điểm từ ảnh.
- Album nâng cao.
- Chỉnh sửa ảnh trong ứng dụng.

---

# 3.10. Module 6 — Journey Map & Statistics

Đây là thành phần tạo bản sắc cho sản phẩm.

## Journey Map

Hiển thị:

- Các tỉnh/thành đã khám phá.
- Các địa điểm đã ghé.
- Lịch sử hành trình.

Ví dụ:

```text
🟢 Đã khám phá
⚪ Chưa khám phá
```

## Statistics

MVP có thể hiển thị:

```text
25 / 63 tỉnh thành
12 chuyến đi
48 địa điểm
327 kỷ niệm
```

Click vào một địa điểm/tỉnh thành có thể xem:

```text
Địa điểm
    ↓
Các chuyến đi
    ↓
Ảnh
    ↓
Ghi chú / kỷ niệm
```

## Gamification

Chỉ ưu tiên thống kê cơ bản trong MVP.

Các hệ thống:

- XP.
- Streak.
- Leaderboard.
- Reward.
- Achievement phức tạp.

để sau MVP.

---

# 3.11. Future Scope — Travel Group

Travel Group là hướng mở rộng trong tương lai.

Ý tưởng:

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

Một nhóm có thể:

- Cùng tham gia một chuyến đi.
- Chia sẻ hình ảnh.
- Chia sẻ trải nghiệm.
- Xem kỷ niệm của các thành viên.
- Tạo album/hành trình chung.

## Không triển khai trong MVP

- Realtime collaboration.
- Group chat.
- Voting.
- Permission system phức tạp.
- Realtime synchronization.

Kiến trúc dữ liệu nên có khả năng mở rộng cho tính năng này trong tương lai nhưng không được làm tăng đáng kể độ phức tạp của MVP.

---

# 3.12. Các chức năng ngoài phạm vi MVP

## Social Network

Không triển khai:

- Follow.
- Like.
- Comment.
- Feed.
- Public profile.

## Booking

Không triển khai:

- Đặt khách sạn.
- Đặt vé máy bay.
- Đặt vé tham quan.
- Thanh toán.

## AI nâng cao

Không triển khai:

- Model AI riêng.
- Training.
- Fine-tuning riêng.
- Machine Learning recommendation engine.
- AI voice assistant.
- Phân tích ảnh chuyên sâu.

## Gamification nâng cao

Không triển khai:

- Leaderboard.
- XP.
- Streak.
- Reward system.
- Achievement system phức tạp.

---

# 3.13. Build vs Integrate

| Thành phần | Build | Integrate |
|---|:---:|:---:|
| User/Trip Management | ✅ | |
| Diary | ✅ | |
| Journey Statistics | ✅ | |
| Business Rules | ✅ | |
| Itinerary Orchestration | ✅ | |
| AI Reasoning | | ✅ |
| Map Rendering | | ✅ |
| Routing | | ✅ |
| Weather Data | | ✅ |
| Geocoding | | ✅ |
| Image Storage | | ✅ |

Nguyên tắc:

> **Tự xây phần tạo ra giá trị riêng của sản phẩm, tích hợp những hạ tầng/dịch vụ phổ biến đã có giải pháp tốt.**

---

# 3.14. MVP Boundary

```text
┌──────────────────────────────────────┐
│                MVP                   │
│                                      │
│  👤 User                             │
│      ↓                               │
│  🧳 Trip                             │
│      ↓                               │
│  📍 Places                           │
│      ↓                               │
│  🤖 AI Itinerary                     │
│      ↓                               │
│  🗺️ Map + 🌤️ Weather                │
│      ↓                               │
│  📍 Check-in                         │
│      ↓                               │
│  📷 Diary                            │
│      ↓                               │
│  🗺️ Journey Map                      │
│                                      │
└──────────────────────────────────────┘

              FUTURE
┌──────────────────────────────────────┐
│ 👥 Travel Group                      │
│ 💬 Chat                              │
│ ❤️ Social                            │
│ 🏆 Advanced Gamification             │
│ 🏨 Booking                           │
│ 🧠 Custom AI Model                  │
└──────────────────────────────────────┘
```

---

# 3.15. Feature Filter

Từ thời điểm này, mọi tính năng mới được đề xuất phải vượt qua 3 câu hỏi:

### 1. Có phục vụ core journey không?

```text
Plan → Explore → Capture → Remember
```

### 2. Có mang lại giá trị rõ ràng cho người dùng không?

### 3. Một solo developer có thể hoàn thiện trong MVP không?

Nếu một tính năng không đáp ứng đủ các tiêu chí trên, ưu tiên đưa vào **Future Scope**.

---

# 3.16. MVP Core Journey

```text
                    USER
                     │
                     ▼
              TẠO CHUYẾN ĐI
                     │
                     ▼
             CHỌN ĐỊA ĐIỂM
                     │
                     ▼
              AI LẬP LỊCH TRÌNH
                     │
                     ▼
             USER XÁC NHẬN
                     │
                     ▼
              BẮT ĐẦU CHUYẾN ĐI
                     │
          ┌──────────┼──────────┐
          ▼          ▼          ▼
        MAP       WEATHER     ROUTE
          │          │          │
          └──────────┼──────────┘
                     ▼
                  CHECK-IN
                     │
                     ▼
               📷 LƯU KỶ NIỆM
                     │
                     ▼
             HOÀN THÀNH CHUYẾN ĐI
                     │
                     ▼
              TRAVEL DIARY
                     │
                     ▼
              JOURNEY MAP
                     │
                     ▼
             "TÔI ĐÃ ĐI ĐÂU?"
```

---

## 3.17. Tóm tắt phạm vi MVP

### Phải có

- User cơ bản.
- Trip Management.
- Địa điểm.
- AI gợi ý/sắp xếp itinerary.
- Map.
- Routing.
- Weather.
- Check-in.
- Ảnh + ghi chú.
- Travel Diary.
- Journey Map.
- Thống kê cơ bản.

### Không làm

- Social Network.
- Travel Group.
- Chat.
- Booking.
- Payment.
- AI model riêng.
- ML recommendation riêng.
- Gamification nâng cao.

### Nguyên tắc

> **UX-first + API-first + MVP-focused**

Mục tiêu không phải xây một nền tảng du lịch khổng lồ, mà là hoàn thiện một **vòng đời chuyến đi cá nhân có giá trị và trải nghiệm tốt**.

---

## 3.18. Core Value Proposition

> **Người dùng chỉ cần cho hệ thống biết mình muốn đi đâu và trong khoảng thời gian nào; hệ thống hỗ trợ phần còn lại bằng cách kết hợp dữ liệu địa điểm, bản đồ, thời tiết và AI để giúp xây dựng một hành trình phù hợp, sau đó cho phép người dùng lưu giữ và nhìn lại những trải nghiệm trong chuyến đi.**
