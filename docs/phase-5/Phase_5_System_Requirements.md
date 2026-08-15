# PHASE 5 — SYSTEM REQUIREMENTS
## Functional Requirements, Business Rules, Non-Functional Requirements & Technical Constraints

> **Project:** Xây dựng hệ thống hỗ trợ lập kế hoạch và quản lý hành trình du lịch cá nhân tích hợp AI gợi ý lộ trình, thời tiết và nhật ký trải nghiệm  
> **Phase:** 5 — System Requirements  
> **Phạm vi:** MVP — Solo Developer  
> **Định hướng:** UX nhanh, đơn giản, hiệu quả; AI hỗ trợ quyết định; người dùng luôn có quyền quyết định cuối cùng.

---

# 1. Mục đích của Phase 5

Phase 5 chuyển các ý tưởng và đặc tả sản phẩm ở các phase trước thành các yêu cầu có thể dùng trực tiếp cho:

- Thiết kế kiến trúc hệ thống.
- Thiết kế database.
- Thiết kế API.
- Thiết kế giao diện.
- Lập trình backend/frontend.
- Thiết kế thuật toán Held–Karp.
- Tích hợp AI và các External API.
- Kiểm thử và nghiệm thu MVP.

Phase 5 gồm:

- **5A — Functional Requirements**
- **5B — Business Rules**
- **5C — Validation & Error Handling**
- **5D — Data & Permission Requirements**
- **5E — Business Rules & Constraints**
- **5F — Non-Functional Requirements**
- **5G — System Constraints & Technical Constraints**

---

# 2. Nguyên tắc tổng thể

Hệ thống được xây dựng theo nguyên tắc:

> **Algorithm đảm bảo tối ưu định lượng → AI cá nhân hóa và giải thích → External API cung cấp dữ liệu → Application kiểm soát business rules → User quyết định cuối cùng.**

Đặc biệt:

- AI **không phải System Command**.
- Weather chỉ là **soft context / recommendation**.
- Opening Hours có thể là **hard constraint** khi dữ liệu đáng tin cậy.
- Held–Karp chịu trách nhiệm giải bài toán tối ưu thứ tự địa điểm.
- User luôn có thể **Apply / Edit / Ignore** recommendation.
- MVP ưu tiên cá nhân; Group/Family là hướng mở rộng tương lai.

---

# 3. 5A — FUNCTIONAL REQUIREMENTS

## 3.1. Quản lý người dùng

### FR-01 — Đăng ký
Người dùng có thể tạo tài khoản bằng thông tin đăng ký phù hợp.

### FR-02 — Đăng nhập
Người dùng có thể đăng nhập và truy cập dữ liệu cá nhân.

### FR-03 — Hồ sơ
Người dùng có thể xem/chỉnh sửa thông tin hồ sơ cơ bản.

### FR-04 — Phân quyền dữ liệu
Người dùng chỉ được truy cập Trip, Memory, Photo và dữ liệu cá nhân thuộc tài khoản của mình.

## 3.2. Quản lý chuyến đi

### FR-05 — Tạo Trip
Người dùng có thể tạo chuyến đi với:

- Tên chuyến đi.
- Điểm đến.
- Ngày bắt đầu/kết thúc.
- Danh sách địa điểm muốn khám phá.
- Phương tiện.
- Sở thích.
- Ràng buộc/yêu cầu cá nhân.
- Ngân sách dự kiến nếu người dùng cung cấp.

Các thông tin không bắt buộc được thiết kế theo hướng optional để giảm thao tác.

### FR-06 — Quản lý Place
Người dùng có thể thêm, xóa, chỉnh sửa danh sách địa điểm trong Trip.

### FR-07 — Quản lý thời gian
Trip có ngày bắt đầu và kết thúc; lịch trình được tổ chức theo ngày và các khung giờ dự kiến.

### FR-08 — Quản lý phương tiện
Người dùng lựa chọn phương tiện phù hợp với các phương tiện mà Routing Provider hỗ trợ.

---

# 4. 5A — AI & ROUTE PLANNING

## 4.1. FR-09 — Gợi ý địa điểm

AI có thể phân tích:

- Điểm đến.
- Sở thích.
- Thời gian chuyến đi.
- Các địa điểm hiện có.

để đề xuất thêm địa điểm phù hợp.

Recommendation chỉ mang tính tham khảo.

## 4.2. FR-10 — Tối ưu thứ tự địa điểm

Khi người dùng có nhiều địa điểm, hệ thống thực hiện quy trình hai lớp.

### Lớp 1 — Thuật toán

Hệ thống sử dụng **Held–Karp Dynamic Programming** để giải bài toán TSP trong phạm vi giới hạn của MVP.

Input:

- Danh sách Place.
- Tọa độ.
- Distance/Duration Matrix từ Routing API.

Output:

- Thứ tự các địa điểm có tổng chi phí di chuyển tối ưu theo hàm mục tiêu đã chọn.

### Lớp 2 — AI

AI nhận kết quả thuật toán cùng context:

- Sở thích.
- Thời gian.
- Opening Hours.
- Weather.
- Mức độ ưu tiên địa điểm.
- Phương tiện.
- Ràng buộc cá nhân.

AI có thể:

- Giữ nguyên route.
- Đề xuất điều chỉnh phù hợp.
- Gợi ý khung giờ.
- Giải thích lý do.
- Đưa ra lưu ý trải nghiệm.

AI không được tự động thay đổi Final Itinerary.

## 4.3. FR-11 — AI Recommendation

Kết quả recommendation cần dễ hiểu và có:

- Thứ tự đề xuất.
- Khung giờ tham khảo.
- Lý do.
- Các lưu ý liên quan.
- Context thời tiết nếu có.
- Mức độ phù hợp nếu được sử dụng trong UI.

Người dùng có:

```text
Apply / Edit / Ignore
```

## 4.4. FR-12 — Weather

Hệ thống lấy Weather Data từ Weather API, có thể gồm:

- Nhiệt độ.
- Điều kiện thời tiết.
- Khả năng mưa.
- Gió.
- Dữ liệu theo thời gian/ngày.

Weather chỉ là **tham khảo**.

Ví dụ:

> “Khả năng mưa cao vào buổi chiều — bạn có thể cân nhắc hoạt động ngoài trời vào buổi sáng.”

Weather **không tự động thay đổi itinerary**.

## 4.5. FR-13 — Điều chỉnh kế hoạch

Người dùng có thể:

- Thay đổi thứ tự.
- Thay đổi thời gian.
- Thêm Place.
- Xóa Place.
- Bỏ recommendation.
- Yêu cầu AI phân tích lại.

Final Itinerary chỉ được cập nhật theo hành động của người dùng.

---

# 5. 5A — MAP & LOCATION

## FR-14 — Hiển thị bản đồ

Hiển thị:

- Vị trí Place.
- Route.
- Thứ tự các Place.
- Khoảng cách.
- Thời gian di chuyển ước tính.

## FR-15 — GPS

GPS là tính năng hỗ trợ, không bắt buộc.

Có thể dùng để:

- Hiển thị vị trí hiện tại.
- Hỗ trợ navigation.
- Hỗ trợ check-in.

## FR-16 — Check-in

Check-in là optional và không phải điều kiện bắt buộc để tạo Memory hoặc xác định tỉnh/thành đã khám phá.

---

# 6. 5A — MEMORY & JOURNAL

## FR-17 — Tạo Memory

Người dùng có thể lưu:

- Ảnh.
- Ghi chú.
- Place.
- Thời gian trải nghiệm/tạo Memory.

## FR-18 — Upload Photo

Người dùng có thể upload một hoặc nhiều ảnh vào Memory.

Ảnh thực tế được lưu ở Media Storage; database chỉ lưu metadata và reference phù hợp.

## FR-19 — Journal

Các Memory trong Trip được sắp xếp theo timeline:

```text
Trip
 ↓
Memory
 ↓
Sort by experiencedAt
 ↓
Journal
```

## FR-20 — Place Memory

Khi mở lại một Place, hệ thống có thể hiển thị:

- Số lần ghé.
- Tổng Memory.
- Tổng ảnh.
- Các Memory theo từng lần đến.

---

# 7. 5A — STATISTICS & EXPLORATION

## FR-21 — My Places

Tổng hợp các Place từng có Memory và số Memory tương ứng.

Dữ liệu có thể được tổng hợp trực tiếp từ Memory, không bắt buộc phải có bảng riêng.

## FR-22 — My Journey

Hiển thị thống kê hành trình cá nhân:

- Tỉnh/thành đã khám phá.
- Tổng Place.
- Tổng Trip.
- Tổng Memory.
- Tổng Photo.

## FR-23 — Bản đồ hành trình

Hiển thị bản đồ Việt Nam và trạng thái tỉnh/thành:

- Đã khám phá.
- Chưa khám phá.

## FR-24 — Quy tắc Explored

Một tỉnh/thành được tính là **đã khám phá** khi:

```text
Memory
 ↓
Place
 ↓
Province
 ↓
Explored
```

Không bắt buộc GPS hoặc Check-in.

Nếu Place chỉ nằm trong Itinerary nhưng chưa có Memory thì tỉnh/thành **chưa được tính là đã khám phá**.

---

# 8. 5B — BUSINESS RULES

- **BR-01 — User Ownership:** Mọi Trip và Memory phải thuộc về một User cụ thể.
- **BR-02 — Trip Ownership:** User chỉ được thao tác trên Trip thuộc quyền sở hữu của mình.
- **BR-03 — Memory Relationship:** Memory thuộc một Trip và một Place trong phạm vi MVP.
- **BR-04 — AI Recommendation:** AI Recommendation là đề xuất, không phải mệnh lệnh.
- **BR-05 — User Decision:** User luôn có quyền **Apply / Edit / Ignore**.
- **BR-06 — Weather:** Weather không được tự động thay đổi itinerary.
- **BR-07 — Opening Hours:** Nếu Opening Hours đáng tin cậy, hệ thống không nên đề xuất lịch trình rõ ràng nằm ngoài thời gian hoạt động.
- **BR-08 — Explored Province:** Explored được xác định từ Memory → Place → Province.
- **BR-09 — GPS:** GPS/Check-in không bắt buộc để tạo Memory.
- **BR-10 — Journal:** Journal được tổng hợp từ Memory, không nhất thiết lưu dữ liệu journal duplicate.

---

# 9. 5C — VALIDATION & ERROR HANDLING

## 9.1. Trip Validation

Kiểm tra:

- Tên Trip hợp lệ.
- Ngày bắt đầu ≤ ngày kết thúc.
- Có ít nhất một Place khi yêu cầu Generate Plan.
- Place không trùng ngoài trường hợp được cho phép.

## 9.2. Route Validation

Trước khi chạy Held–Karp:

- Số Place nằm trong giới hạn.
- Có tọa độ hợp lệ.
- Distance/Duration Matrix đầy đủ hoặc có cách xử lý missing data.
- Transportation hợp lệ.

## 9.3. AI Validation

AI output phải:

- Đúng schema.
- Chỉ tham chiếu các Place hợp lệ.
- Không tạo Place ID không tồn tại.
- Không chứa route sai cấu trúc.
- Có fallback khi parse thất bại.

## 9.4. External API Failure

Nếu Weather lỗi:

```text
Trip      ✅
Map       ✅
Itinerary ✅
Weather   ❌
```

Nếu AI lỗi:

```text
Manual Planning    ✅
Map                ✅
Memory             ✅
AI Recommendation  ❌
```

Hệ thống phải graceful degradation thay vì crash toàn bộ.

## 9.5. Error Message

Không hiển thị stack trace cho người dùng.

Nên hiển thị:

> “Không thể tải thông tin thời tiết. Vui lòng thử lại.”

---

# 10. 5D — DATA & PERMISSION REQUIREMENTS

## 10.1. Account

- User ID.
- Name.
- Email/username.
- Password hash.
- Profile information.

## 10.2. Trip

- Trip ID.
- User ID.
- Name.
- Destination.
- Start date.
- End date.
- Transportation.
- Preferences.
- Constraints.
- Budget nếu có.

## 10.3. Place

- Place ID.
- Provider ID nếu có.
- Name.
- Address.
- Province.
- Latitude.
- Longitude.

## 10.4. Route Data

- Distance.
- Duration.
- Transportation.
- Route information.

## 10.5. Weather

- Place.
- Date/time.
- Temperature.
- Condition.
- Rain probability.
- Wind.
- Forecast metadata nếu cần.

## 10.6. Memory

- Memory ID.
- User ID.
- Trip ID.
- Place ID.
- Note.
- ExperiencedAt/CreatedAt.

## 10.7. Photo

- Photo ID.
- Memory ID.
- Media URL/reference.
- MIME type.
- File size.
- Width/height.
- Display order.

## 10.8. Permission

User chỉ được:

- Xem dữ liệu của mình.
- Tạo dữ liệu cho mình.
- Chỉnh sửa dữ liệu của mình.
- Xóa dữ liệu của mình.

Memory/Photo mặc định là private trong MVP.

---

# 11. 5E — BUSINESS RULES & CONSTRAINTS

## 11.1. Recommendation vs Command

Luôn phân biệt:

```text
Recommendation → User quyết định
Command        → Hệ thống bắt buộc
```

AI chỉ nằm ở Recommendation layer.

## 11.2. Weather là Soft Context

Weather:

- Được đưa vào recommendation.
- Có thể tạo note/gợi ý.
- Không tự động thay route.
- Không tự động thay Final Itinerary.

## 11.3. Opening Hours

Opening Hours có thể được xem là Hard Constraint nếu provider cung cấp dữ liệu rõ ràng.

## 11.4. Final Itinerary

Chỉ User mới xác nhận Final Itinerary.

## 11.5. Memory & Explored

Có Memory tại Place → Place thuộc Province → Province được tính là Explored.

Chỉ có Place trong itinerary → chưa Explored.

---

# 12. 5F — NON-FUNCTIONAL REQUIREMENTS

## 12.1. Performance

| Loại thao tác | Mục tiêu |
|---|---:|
| API thông thường | khoảng ≤ 1–2 giây |
| External API | khoảng ≤ 3–5 giây |
| AI Recommendation | khoảng ≤ 10 giây |

Đây là mục tiêu trong điều kiện mạng và dịch vụ bình thường, không phải SLA tuyệt đối.

## 12.2. Held–Karp Performance

Held–Karp có:

```text
O(n² × 2ⁿ)
```

Do đó MVP giới hạn số Place để đảm bảo thời gian xử lý.

## 12.3. Image Performance

- Thumbnail.
- Resize.
- Lazy loading.
- CDN/Media Storage.
- Không tải ảnh gốc lớn cho thumbnail.

## 12.4. UX

Nguyên tắc:

> **Ít thao tác — thông tin đúng lúc — kết quả dễ hiểu.**

Ví dụ lưu Memory:

```text
Place
 ↓
Add Memory
 ↓
Select Photos
 ↓
Note (optional)
 ↓
Save
```

## 12.5. Reliability

External API lỗi không được làm crash toàn bộ hệ thống.

## 12.6. Security

- Hash password.
- Không lưu plaintext password.
- API key dùng environment variables/secrets.
- Backend kiểm tra authorization.
- Không log password/API key.

## 12.7. Privacy

- Trip/Memory/Photo private theo User.
- Không chia sẻ dữ liệu nếu chưa có cơ chế cho phép.
- Không thu thập dữ liệu không cần thiết.

## 12.8. Data Integrity

Đảm bảo quan hệ:

```text
User
 ↓
Trip
 ↓
Memory
 ↓
Photo
```

không tạo dữ liệu orphan không hợp lệ.

## 12.9. Maintainability

Kiến trúc nên tách:

```text
Controller
 ↓
Service
 ↓
Business Logic
 ↓
Repository
```

External integrations tách riêng.

## 12.10. Logging

Log:

- Request lỗi.
- External API failure.
- AI failure.
- Routing failure.
- Authentication failure.
- Database error.

Không log secret hoặc dữ liệu nhạy cảm không cần thiết.

---

# 13. 5G — SYSTEM CONSTRAINTS & TECHNICAL CONSTRAINTS

## 13.1. Geographic Scope

MVP chỉ hỗ trợ du lịch tại Việt Nam.

Không hỗ trợ:

- International travel.
- Multi-country trip.
- Visa/passport.
- International timezone complexity.

## 13.2. User Scope

MVP tập trung cá nhân.

Không triển khai:

- Group Trip.
- Family Trip.
- Shared itinerary.
- Real-time collaboration.
- Group chat.

Group/Family là Future Extension.

## 13.3. TSP / Held–Karp Limit

Held–Karp dùng để tìm thứ tự tối ưu đi qua các Place.

Độ phức tạp:

```text
O(n² × 2ⁿ)
```

MVP đề xuất:

```text
Recommended: 2–10 Places
Maximum initial limit: 12 Places / optimization
```

Giới hạn này có thể benchmark và điều chỉnh sau khi implementation hoàn thiện.

## 13.4. Distance Matrix

Held–Karp không tự tính khoảng cách GPS.

Luồng:

```text
Places
 ↓
Coordinates
 ↓
Routing API
 ↓
Distance/Duration Matrix
 ↓
Held–Karp
```

## 13.5. Objective Function

MVP ưu tiên:

> **Tối thiểu tổng thời gian di chuyển.**

Distance có thể được hiển thị bổ sung.

## 13.6. Transportation

Ưu tiên các mode provider hỗ trợ tốt:

- Car.
- Motorbike.
- Walking.

Public Transport chỉ bổ sung nếu provider phù hợp.

## 13.7. Weather API

Weather là dữ liệu dự báo từ provider và không đảm bảo chính xác tuyệt đối.

## 13.8. AI

MVP:

> Không train hoặc xây dựng custom AI model.

Sử dụng external AI API.

AI dùng cho:

- Phân tích sở thích.
- Recommendation.
- Context analysis.
- Route explanation.
- Suggest adjustment.

## 13.9. AI không phải nguồn dữ liệu authoritative

AI không tự quyết định:

- Coordinates.
- Distance.
- Duration.
- Opening Hours.
- Weather.
- Provider Place ID.

Các dữ liệu này đến từ nguồn tương ứng.

## 13.10. Structured AI Output

AI phải trả về structured output để hệ thống có thể validate và xử lý.

Ví dụ:

```json
{
  "recommendation": {
    "route": [
      "place_1",
      "place_3",
      "place_2",
      "place_4"
    ],
    "reason": [
      "Giảm thời gian di chuyển",
      "Phù hợp thời tiết buổi sáng"
    ]
  }
}
```

## 13.11. AI Output Validation

AI không được tạo Place ID ngoài danh sách hợp lệ.

Ví dụ input:

```text
A, B, C, D
```

thì output route không được chứa:

```text
X
```

nếu X chưa được hệ thống xác nhận.

## 13.12. External API Dependency

MVP có thể sử dụng:

```text
Places API
Maps / Routing API
Weather API
AI API
Media Storage
```

Provider có thể thay đổi response, quota, pricing hoặc availability.

Vì vậy cần abstraction layer:

```text
Business Logic
 ↓
RoutingService
 ↓
RoutingProvider
```

Tương tự:

```text
WeatherService → WeatherProvider
AIService      → AIProvider
```

## 13.13. Image Storage

Không lưu binary image trực tiếp trong database.

```text
Database
 └── Photo Metadata + URL

Media Storage
 └── Actual Image
```

## 13.14. Image Limit

MVP có thể giới hạn khoảng:

```text
10 MB / image
```

Các format ưu tiên:

```text
JPEG
PNG
WebP
```

HEIC có thể để Future Extension.

## 13.15. Memory

MVP:

```text
1 User
1 Trip
1 Place
N Photos
1 Note
1 Timestamp
```

## 13.16. Journal

Không cần bảng Journal riêng.

```text
Trip
 ↓
Memory
 ↓
Sort by experiencedAt
 ↓
Journal
```

## 13.17. My Journey

Không nhất thiết tạo bảng statistics riêng.

Có thể aggregate:

```text
Memory
 ↓
Place
 ↓
Province
```

và tính:

- Province count.
- Place count.
- Memory count.
- Photo count.
- Trip count.

## 13.18. Province / City

MVP sử dụng phạm vi 63 tỉnh/thành theo dữ liệu đã chốt của đề tài.

Nên sử dụng Province ID/mapping thay vì chỉ dựa vào text name.

## 13.19. GPS

GPS optional.

Không yêu cầu GPS để:

- Tạo Trip.
- Tạo Memory.
- Xác định Explored Province.

## 13.20. Check-in

Check-in optional.

Không phải điều kiện để tạo Memory hoặc tính Explored.

## 13.21. Offline

MVP không yêu cầu Offline-first.

Internet cần thiết cho:

- AI.
- Weather.
- Routing.
- Places.

## 13.22. Social / Group

Không triển khai trong MVP.

Future:

```text
Group
 ↓
Members
 ↓
Shared Trip
 ↓
Shared Memories
```

## 13.23. Gamification

MVP chỉ giữ thống kê cơ bản:

```text
25 / 63 Provinces
12 Trips
87 Places
120 Memories
350 Photos
```

Không cần:

- XP.
- Level.
- Leaderboard.
- Streak.
- Quest system.
- Badge engine phức tạp.

## 13.24. Booking

Hệ thống không phải Booking/OTA Platform.

Không xử lý:

- Hotel booking.
- Flight booking.
- Bus booking.
- Restaurant booking.
- Payment.

## 13.25. Notification

Push notification không thuộc MVP Core.

## 13.26. Architecture

Không cần Microservices.

MVP ưu tiên:

> **Modular Monolith**

Mô hình:

```text
Frontend
    ↓
Backend API
    ↓
Database

Backend
 ├── AI Integration
 ├── Map Integration
 ├── Weather Integration
 ├── Media Integration
 └── Held–Karp
```

## 13.27. Deployment

Ưu tiên managed services/API có sẵn, free tier hoặc chi phí thấp.

Mục tiêu:

> Giảm DevOps để tập trung vào nghiệp vụ, UX và thuật toán.

## 13.28. Cost Control

Ưu tiên:

- Free tier.
- Open-source.
- Managed service.
- Caching.
- Chỉ gọi AI khi cần.

Không gọi AI cho:

```text
Open Trip
View Memory
View Map
Statistics
```

Chỉ gọi AI cho:

```text
Generate Recommendation
Explain Route
Personalize Plan
Suggest Adjustment
```

---

# 14. MVP BOUNDARY SAU PHASE 5

## 14.1. MVP Core

```text
👤 User
   ↓
🧳 Trip Management
   ↓
📍 Places
   ↓
🗺️ Routing / Distance Matrix
   ↓
🧮 Held–Karp
   ↓
🤖 AI Recommendation
   ├── 🌤️ Weather Context
   ├── 🕒 Opening Hours
   ├── ❤️ Preferences
   └── 🚗 Transportation
   ↓
👤 Apply / Edit / Ignore
   ↓
🗓️ Final Itinerary
   ↓
📸 Memory
   ↓
📖 Journal
   ↓
🗺️ My Journey
   ↓
🇻🇳 Province Exploration
```

## 14.2. Explicitly Out of Scope

```text
❌ Group / Family
❌ Social Network
❌ Chat
❌ Booking
❌ Payment
❌ Custom AI Model
❌ Offline-first
❌ Real-time Collaboration
❌ Advanced Gamification
❌ Advanced Expense Management
❌ AI tự động điều khiển itinerary
```

---

# 15. TECHNICAL TRADE-OFFS

| Quyết định | Lý do |
|---|---|
| External AI API | Giảm scope và thời gian phát triển |
| Held–Karp | Có giá trị học thuật, dễ chứng minh thuật toán |
| Giới hạn số Place | Kiểm soát O(n²2ⁿ) |
| Weather là soft context | Forecast không chính xác tuyệt đối |
| Opening Hours là hard constraint khi đáng tin cậy | Tránh đề xuất địa điểm đã đóng |
| Memory xác định Explored | UX đơn giản, không phụ thuộc GPS |
| Media Storage thay DB binary | Tối ưu database |
| Modular Monolith | Phù hợp solo developer |
| API abstraction | Dễ thay provider |
| Không Group trong MVP | Giảm complexity |
| Không Booking | Giữ đúng phạm vi đề tài |
| Không Custom AI Model | Tập trung integration + recommendation |

---

# 16. KIẾN TRÚC TRÁCH NHIỆM CỐT LÕI

```text
                         USER
                           │
                           ▼
                  APPLICATION LAYER
                           │
              ┌────────────┼────────────┐
              │            │            │
              ▼            ▼            ▼
        HELD–KARP          AI       BUSINESS RULES
        Optimization   Recommend
              │            │            │
              └────────────┼────────────┘
                           │
                           ▼
                  EXTERNAL SERVICES
             ┌────────┬────────┬────────┐
             │        │        │        │
           Maps    Weather   Places   Media
             │        │        │        │
             └────────┴────────┴────────┘
                           │
                           ▼
                        DATABASE
```

### Nguyên tắc

**Held–Karp không thay AI.**

**AI không thay Map/Weather.**

**Weather không điều khiển Itinerary.**

**AI không thay quyền quyết định của User.**

**User là người quyết định Final Itinerary.**

---

# 17. QUY TẮC KIỂM SOÁT SCOPE TỪ PHASE 5 TRỞ ĐI

Mọi chức năng mới phải được đánh giá bằng câu hỏi:

> **Chức năng này có trực tiếp phục vụ mục tiêu cốt lõi của MVP không?**

Nếu không:

```text
→ Future Extension
```

Nếu hữu ích nhưng không cần thiết cho bản demo:

```text
→ P1 / P2
```

Chỉ chức năng thực sự cần thiết mới được đưa vào:

```text
→ MVP Core
```

Mục tiêu:

> **Hoàn thành một MVP nhỏ nhưng hoàn chỉnh, có giá trị sử dụng và có điểm nhấn học thuật rõ ràng, thay vì một hệ thống quá lớn nhưng không hoàn thiện.**

---

# 18. KẾT LUẬN PHASE 5

Sau Phase 5, phạm vi MVP đã được xác định:

### Người dùng

```text
Personal User
```

### Core Experience

```text
Khám phá
   ↓
Lập kế hoạch
   ↓
Tối ưu
   ↓
Điều chỉnh
   ↓
Trải nghiệm
   ↓
Lưu Memory
   ↓
Xem lại Journal
   ↓
Theo dõi Journey
```

### Core Technology

```text
Held–Karp
AI API
Maps / Routing API
Weather API
Places API
Media Storage
Database
```

### Core Philosophy

```text
Algorithm → tối ưu định lượng
AI        → cá nhân hóa + giải thích
Weather   → tham khảo
Map       → dữ liệu di chuyển
Business  → kiểm soát tính hợp lệ
User      → quyết định cuối cùng
```

Phase 5 là cơ sở để chuyển sang các phase kỹ thuật tiếp theo:

**System Architecture → Domain/Data Model → Database Design → API Design → UI/UX Specification → Algorithm Design → Implementation Plan**
