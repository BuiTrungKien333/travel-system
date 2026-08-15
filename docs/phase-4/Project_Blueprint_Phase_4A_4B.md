# PROJECT BLUEPRINT — PHASE 4A & 4B
## Product Requirements & Core Functional Specification

> **Project:** Hệ thống hỗ trợ lập kế hoạch và quản lý hành trình du lịch cá nhân tích hợp AI gợi ý lộ trình, thời tiết và nhật ký trải nghiệm  
> **Scope:** MVP — Việt Nam  
> **Primary user:** Cá nhân  
> **Secondary/future:** Nhóm / gia đình  
> **Document status:** Frozen baseline for Phase 4A & 4B  
> **Version:** 1.0  
> **Date:** 2026-08-08

---

# 1. Mục đích tài liệu

Tài liệu này chốt toàn bộ các quyết định đã thống nhất trong:

- **Phase 4A — Core Product / UX Foundation**
- **Phase 4B — Core Functional Specification**

Đây là baseline dùng làm cơ sở cho các phase tiếp theo: AI, architecture, database, API, frontend, backend, testing và deployment.

> **Nguyên tắc:** Không thay đổi business rule cốt lõi một cách tùy tiện trong quá trình code. Mọi thay đổi lớn phải được ghi nhận thành decision/change mới.

---

# 2. Product Vision

Xây dựng một hệ thống giúp người dùng:

> **Lập kế hoạch → trải nghiệm → ghi lại → nhìn lại hành trình du lịch của chính mình.**

Hệ thống không chỉ là công cụ tạo lịch trình mà là một **personal travel companion** hỗ trợ toàn bộ vòng đời chuyến đi.

---

# 3. Product Philosophy

## 3.1 User-first

User là người quyết định cuối cùng.

AI, Weather, Map và các API bên ngoài chỉ đóng vai trò hỗ trợ.

```text
System suggests
       ↓
User decides
       ↓
System executes/displays
```

## 3.2 Plan is a guide, not a rule

Lịch trình chỉ là kế hoạch. Thực tế có thể thay đổi bởi sở thích, thời tiết, giao thông, sự cố, thời gian phát sinh hoặc quyết định tức thời của User.

> **Không ép User phải tuân thủ itinerary.**

## 3.3 AI is an assistant, not an authority

AI có thể đề xuất, giải thích, tối ưu và cá nhân hóa.

AI không được:

- tự ý thay đổi itinerary;
- tự quyết định thay User;
- được xem là nguồn dữ liệu tuyệt đối;
- thay thế dữ liệu thực tế.

## 3.4 Weather is context, not control

Weather chỉ là yếu tố tham khảo.

```text
Weather: 80% chance of rain
        ↓
Suggestion: "Có thể cân nhắc hoạt động trong nhà."
```

Không tự động sửa itinerary.

## 3.5 UX: Fast, Simple, Effective

- ít thao tác;
- thông tin vừa đủ;
- ưu tiên hành động quan trọng;
- context-aware;
- không ép nhập dữ liệu không cần thiết;
- không overload UI.

> **Một thời điểm — một hành động chính.**

---

# 4. Product Scope

## 4.1 Geographic Scope

MVP hỗ trợ:

> **Toàn bộ Việt Nam**

Dataset địa lý/hành chính phải được quản lý tập trung, không hard-code riêng ở frontend.

## 4.2 Primary User

Người dùng cá nhân muốn tự lập kế hoạch và lưu lại hành trình du lịch.

Ví dụ:

- sinh viên;
- Gen Z;
- người đi du lịch tự túc;
- người thích khám phá;
- người muốn lưu lại kỷ niệm.

## 4.3 Group / Family

**Không triển khai trong MVP.**

Future có thể mở rộng:

```text
User
  ↓
Group
  ↓
Shared Trip
  ↓
Shared Memories
```

Nhưng future scope không được làm phức tạp MVP.

---

# 5. Core Product Loop

```text
CREATE
  ↓
PLAN
  ↓
EXPLORE
  ↓
CAPTURE
  ↓
REMEMBER
  ↓
NEXT TRIP
```

Chi tiết:

```text
Create Trip
    ↓
Add Places
    ↓
Generate / Edit Itinerary
    ↓
Explore Places
    ↓
Check-in / Mark Visited
    ↓
Capture Memories
    ↓
View Journey
    ↓
Review Trip
    ↓
Plan Next Trip
```

---

# 6. Core Modules

MVP gồm:

1. User / Profile
2. Trip Management
3. Place Management
4. Itinerary
5. AI Recommendation
6. Route / Map
7. Weather
8. Check-in / Visited Progress
9. Memory / Travel Diary
10. Journey Progress
11. Personalization

---

# 7. PHASE 4A — PRODUCT & UX FOUNDATION

## 7.1 Product Structure

Object trung tâm là **Trip**.

```text
Trip
├── Places
├── Itinerary
├── Route
├── Weather Context
├── Visited Progress
└── Memories
```

User quản lý:

```text
User
├── Profile
├── Preferences
├── Trips
├── Memories
└── Journey Progress
```

## 7.2 Trip vs Journey

**Trip:** một chuyến đi cụ thể.

**Journey:** toàn bộ lịch sử trải nghiệm của User.

```text
Journey
├── Trip 1 — Đà Nẵng
├── Trip 2 — Đà Lạt
├── Trip 3 — Huế
└── ...
```

> **Trip = một chuyến đi.**  
> **Journey = lịch sử nhiều chuyến đi.**

## 7.3 Information Architecture

```text
Home
├── Current Trip
├── Upcoming Trips
└── Recent Trips

Trips
├── Active
├── Upcoming
└── Completed

Journey
├── Map
├── Progress
└── Statistics

Profile
├── Identity
├── Preferences
└── Settings
```

Memory được truy cập từ Trip, Place và Journey; không xây social feed riêng.

## 7.4 Home UX

### Có Trip đang diễn ra

```text
CURRENT TRIP

Đà Nẵng Summer
Day 2 / 4

📍 Tiếp theo: Hội An

[ Mở chuyến đi ]
```

### Có Trip upcoming

```text
UPCOMING

Đà Lạt
12 ngày nữa

[ Tiếp tục lập kế hoạch ]
```

### Không có Trip

```text
Bạn đang muốn đi đâu?

[ + Tạo chuyến đi ]
```

## 7.5 Trip Creation UX

Flow tối thiểu:

```text
+ Tạo chuyến đi
       ↓
Tên chuyến đi
       ↓
Destination
       ↓
Ngày đi
       ↓
Ngày về
       ↓
Tạo chuyến đi
```

Required:

```text
name
destination
startDate
endDate
```

Optional:

```text
description
coverImage
```

## 7.6 Draft-first UX

Trip được lưu ngay sau khi tạo.

Nếu chưa hoàn thiện:

```text
PLANNING
```

Không cần popup xác nhận lưu nháp.

## 7.7 UX Context Rule

Mỗi màn hình cần trả lời:

1. User đang ở Trip nào?
2. User đang làm gì?
3. Hành động tiếp theo quan trọng nhất là gì?

## 7.8 Error UX

Không hiển thị lỗi kỹ thuật trực tiếp.

Không:

```text
500 Internal Server Error
```

Nên:

```text
Không thể tải dữ liệu.
Kiểm tra kết nối và thử lại.

[Thử lại]
```

## 7.9 External API Failure

AI/Map/Weather API có thể lỗi, timeout hoặc hết quota.

Trip vẫn phải hoạt động bình thường; chỉ capability tương ứng bị unavailable.

## 7.10 Data Ownership

Dữ liệu cá nhân gồm:

- Trips;
- Memories;
- Travel history;
- Preferences.

Mặc định:

> **Private.**

---

# 8. PHASE 4B — CORE FUNCTIONAL SPECIFICATION

# 8.1 4B.1 — Place / Destination

Place là địa điểm người dùng muốn hoặc đã khám phá.

Ví dụ:

- Bà Nà Hills
- Mỹ Khê Beach
- Hội An Ancient Town

Place là entity độc lập.

```text
Trip
 ↓
TripPlace
 ↓
Place
```

Thông tin sơ bộ:

```text
id
name
description
latitude
longitude
address
regionId
category
externalId
```

Không cần lưu metadata dư thừa trong MVP.

## Place Categories

```text
Nature
Beach
Culture
History
Food
Cafe
Entertainment
Shopping
Other
```

Có thể mở rộng sau.

---

# 8.2 4B.2 — Trip Place

TripPlace là quan hệ giữa Trip và Place.

```text
Trip
  │
  ├── TripPlace A
  ├── TripPlace B
  └── TripPlace C
```

Có thể chứa:

```text
tripId
placeId
plannedOrder
plannedDate
visited
visitedAt
```

### Planned vs Visited

```text
PLANNED ≠ VISITED
```

User có thể lên kế hoạch nhưng không thực tế đến.

### Add Place

Có thể thêm Place sau khi Trip được tạo.

### Remove Place

Xóa TripPlace không xóa Place và không xóa Memory.

```text
TripPlace ❌
Place ✓
Memory ✓
```

### Reorder

User có thể thay đổi thứ tự:

```text
A → B → C → D
```

thành:

```text
A → C → B → D
```

---

# 8.3 4B.3 — Itinerary

Itinerary là kế hoạch lịch trình.

Ví dụ:

```text
Day 1
09:00 A
11:00 B
14:00 C

Day 2
09:00 D
13:00 E
```

Itinerary có thể:

- được AI đề xuất;
- do User tự tạo;
- được User chỉnh sửa.

## AI-generated vs Final

```text
User Context
     ↓
AI Suggestion
     ↓
User Review
     ↓
Final Itinerary
```

Sau khi User chỉnh:

> Final Itinerary thuộc quyền kiểm soát của User.

## Flexibility

Itinerary có thể chứa:

- Place;
- suggested time;
- estimated duration;
- order.

Thời gian không phải ràng buộc cứng.

---

# 8.4 4B.4 — Check-in / Time

Thời gian check-in tại Place:

> **Optional và chỉ mang tính tham khảo.**

Không ép User nhập giờ đến.

Không coi:

```text
09:00
```

là bắt buộc.

Lý do:

- sở thích khác nhau;
- sự cố;
- giao thông;
- địa điểm đóng/mở;
- thay đổi kế hoạch.

## Check-in

MVP dùng:

```text
Mark as Visited
```

hoặc:

```text
Check-in
```

Không bắt buộc GPS realtime.

GPS verification là future scope.

---

# 8.5 4B.5 — Weather

Weather là:

> **Context + Recommendation Input**

Không phải Decision Engine.

Ví dụ:

```text
Weather:
Rain likely

Suggestion:
"Có thể cân nhắc hoạt động ngoài trời vào buổi sáng."
```

Không:

```text
Weather
 ↓
Automatically modify itinerary
```

## Weather Uncertainty

Dùng ngôn ngữ:

- dự báo;
- có thể;
- nên cân nhắc;
- tham khảo.

Không khẳng định chắc chắn.

## Weather Failure

Weather lỗi không được khóa Trip.

---

# 8.6 4B.6 — Route / Map

Route phục vụ:

- xem vị trí;
- xem thứ tự;
- ước tính di chuyển;
- hỗ trợ itinerary.

Ví dụ:

```text
A → B → C → D
```

có thể được đề xuất thành:

```text
A → C → B → D
```

nếu hợp lý hơn.

## Route ≠ Itinerary

```text
Itinerary = kế hoạch trải nghiệm
Route = cách di chuyển
```

## Route Optimization

MVP dùng Map/Routing API có sẵn.

Input:

```text
Origin
Destination
Places
Travel mode
```

Output:

```text
Distance
Duration
Route
```

Không xây routing engine riêng.

## Travel Mode

MVP:

```text
Motorbike
Car
Walking
```

Public transport có thể bổ sung nếu API phù hợp.

## Route Failure

Route lỗi không làm Trip hỏng.

---

# 8.7 4B.7 — Memory / Travel Diary

Memory là lớp cảm xúc của sản phẩm.

```text
Visit Place
   ↓
Capture Photo
   ↓
Save Memory
```

Memory sơ bộ:

```text
id
userId
tripId
placeId (optional)
image
caption
createdAt
```

MVP ưu tiên:

> **Photo + Trip + optional Place + Timestamp**

## Memory không bắt buộc thuộc Place

Ví dụ ảnh trên đường đi:

```text
Trip ✓
Place ✗
```

## Memory theo Place

Mở:

```text
Hội An
```

có thể xem:

```text
Your Memories
📸 12 photos
```

## Timeline

Có thể hiển thị:

```text
15 Aug
📸 Mỹ Khê
📸 Sơn Trà

16 Aug
📸 Hội An
```

Timeline là P1/nice-to-have nếu cần giảm scope.

## Privacy

Mặc định:

```text
PRIVATE
```

## Delete

Xóa Memory không xóa Trip, Place hoặc Visited status.

---

# 8.8 4B.8 — Trip & Journey Management

Trip là container trung tâm:

```text
Trip
├── Basic Information
├── Places
├── Itinerary
├── Route
├── Progress
└── Memories
```

## Trip Status

MVP:

```text
PLANNING
READY
ACTIVE
COMPLETED
```

Lifecycle:

```text
PLANNING
    ↓
READY
    ↓
ACTIVE
    ↓
COMPLETED
```

## Trip Creation

Required:

```text
name
destination
startDate
endDate
```

Optional:

```text
description
coverImage
```

## Trip Detail

Tabs:

```text
Overview
Itinerary
Map
Memories
```

## Overview

```text
Đà Nẵng Summer 2026

15 → 18 Aug
📍 Đà Nẵng

5 địa điểm
3 đã khám phá
27 memories
```

## Active Trip

```text
Current Trip

Đà Nẵng
Day 2 / 4

📍 Tiếp theo: Hội An
```

## Completed Trip

```text
🎉 Chuyến đi đã kết thúc!

5 địa điểm
42 memories
3 ngày
```

## Edit Trip

Cho phép sửa:

```text
Name
Destination
Start date
End date
Description
Cover
```

Nếu thay đổi ngày:

> Không tự động xóa itinerary.

User có thể review/re-plan.

## Re-plan

```text
✨ Tối ưu lại
       ↓
AI Suggestion
       ↓
User Review
       ↓
Apply
```

Không auto-apply.

## Delete Trip

Có confirmation.

Quan trọng:

> **Delete Trip ≠ Delete Memory.**

## Archive

Ẩn Trip khỏi danh sách chính, không xóa dữ liệu.

Nice-to-have.

## Duplicate Trip

Future/nice-to-have.

Copy:

- Places;
- Itinerary.

Không copy:

- Memories;
- Visited state.

## Offline

MVP không xây offline-first hoàn chỉnh.

Có thể:

- giữ dữ liệu đã tải;
- retry;
- graceful error.

Không làm:

- offline map hoàn chỉnh;
- offline AI;
- full synchronization engine.

---

# 8.9 4B.9 — User Profile, Journey Progress & Personalization

## Profile Philosophy

Profile không phải social network profile.

Chỉ phục vụ:

- identity;
- journey;
- preferences;
- personalization.

## Basic Profile

MVP:

```text
displayName
avatar
createdAt
updatedAt
```

Không cần:

- gender;
- age;
- address;
- phone;
- bio dài.

## Profile UX

```text
👤 Nguyễn Văn A

🗺️ 25/63 tỉnh thành
🧳 8 chuyến đi
📸 124 kỷ niệm

Hành trình
Chuyến đi
Kỷ niệm
Cài đặt
```

---

# 9. Journey Progress

Core emotional feature:

> **Theo dõi User đã khám phá bao nhiêu tỉnh/thành Việt Nam.**

Ví dụ:

```text
🇻🇳 Vietnam Journey

25 / 63
Tỉnh/thành đã khám phá
```

## Visited Definition

Chỉ tính visited khi User thực sự xác nhận đã đến.

```text
Planned
  ≠
Visited
```

Không tính chỉ vì:

- Trip được tạo;
- Place được thêm;
- AI đề xuất;
- itinerary chứa Place.

## Check-in

MVP:

> User chủ động Mark as Visited / Check-in.

GPS verification là future scope.

## Memory Independence

Memory không phải điều kiện để tính visited.

```text
User đến Huế
↓
Mark as Visited
↓
Huế được tính
```

---

# 10. Region Dataset

Cần dataset tập trung:

```text
AdministrativeRegion
├── id
├── code
├── name
├── level
└── coordinates / geometry
```

Dataset hành chính phải có snapshot/version rõ ràng.

MVP có thể sử dụng phạm vi:

> **63 tỉnh/thành theo dataset được project chốt.**

UI nên dùng cách gọi:

> **Tỉnh / Thành phố đã khám phá**

---

# 11. Journey Map

```text
🇻🇳 Hành trình Việt Nam

25 / 63

[ Vietnam Map ]

✓ Hà Nội
✓ Huế
✓ Đà Nẵng
...
```

MVP chỉ cần:

- region dataset;
- map visualization;
- visited state.

Không cần GIS engine phức tạp.

---

# 12. Journey Statistics

MVP:

```text
25 / 63 regions
8 trips
34 places
124 memories
```

Không cần:

- travel score;
- CO₂;
- ranking;
- total money;
- advanced analytics.

---

# 13. Duplicate Region

Nếu User đến Đà Nẵng nhiều lần:

```text
Trip 1 → Đà Nẵng
Trip 2 → Đà Nẵng
Trip 3 → Đà Nẵng
```

Journey:

```text
Đà Nẵng = 1 region visited
```

nhưng:

```text
Trips = 3
```

---

# 14. Progress Formula

```text
visitedRegions = 25
totalRegions = 63

progress = 25 / 63
```

Hiển thị:

```text
25 / 63
≈ 39.7%
```

Không cần gamification engine.

---

# 15. Personalization

MVP không xây ML recommendation engine.

```text
User Preferences
+
Travel History
+
Trip Context
+
Place Data
+
Weather
       ↓
AI API
       ↓
Personalized Suggestion
```

## Travel Interests

Ví dụ:

```text
☑ Nature
☑ Food
☑ Culture
☑ Photography
☐ Nightlife
☐ Shopping
☐ History
☐ Beach
```

## Travel Pace

```text
○ Relaxed
○ Balanced
○ Explore More
```

## Budget

```text
○ Budget
○ Moderate
○ Comfortable
```

Budget chỉ là input cho AI nếu chưa có dữ liệu giá đáng tin cậy; không xây cost engine.

---

# 16. Explicit vs Implicit Preferences

## Explicit

User tự chọn:

```text
Nature
Food
Photography
```

Đây là nguồn chính trong MVP.

## Implicit

Hệ thống có thể thống kê sau này:

```text
Nature visits = 12
Food visits = 8
```

Không phải core MVP.

---

# 17. AI Personalization Input

Ví dụ:

```text
Destination:
Đà Lạt

Duration:
3 days

Interests:
Nature
Food
Photography

Pace:
Balanced

Budget:
Moderate
```

AI trả về Suggested Itinerary.

Chi tiết prompt/schema/validation nằm ở Phase 4C.

---

# 18. Preference Ownership

AI không tự động thay đổi Preference.

Nếu AI suy ra User thích cafe:

```text
favorite = cafe
```

không được tự lưu.

User phải chủ động xác nhận/chỉnh sửa.

---

# 19. Privacy

Mặc định:

```text
Profile = Private
Trips = Private
Memories = Private
Journey = Private
```

Không public live location.

---

# 20. Journey Navigation

Có thể đi:

```text
Profile
  ↓
Journey
  ↓
Region
  ↓
Place
  ↓
Trip
  ↓
Memory
```

Hoặc:

```text
Memory
  ↓
Place
  ↓
Trip
```

---

# 21. Unified Domain Model

```text
                           USER
                            │
             ┌──────────────┼──────────────┐
             │              │              │
             ▼              ▼              ▼
          PROFILE       PREFERENCES       JOURNEY
                                           │
                                           ▼
                                         REGION
                                           ▲
                                           │
             ┌─────────────────────────────┘
             │
             ▼
            PLACE
             ▲
             │
            TRIP
             │
       ┌─────┼───────────┬─────────────┐
       │     │           │             │
       ▼     ▼           ▼             ▼
    TRIPPLACE ITINERARY ROUTE       MEMORY
       │
       ▼
     VISITED
```

---

# 22. Preliminary Domain Models

## UserProfile

```text
UserProfile
├── userId
├── displayName
├── avatarUrl
├── createdAt
└── updatedAt
```

## TravelPreference

```text
TravelPreference
├── userId
├── interests[]
├── pace
├── budgetLevel
└── updatedAt
```

## Trip

```text
Trip
├── id
├── userId
├── name
├── description
├── destinationRegionId
├── startDate
├── endDate
├── coverImage
├── status
├── createdAt
└── updatedAt
```

## TripPlace

```text
TripPlace
├── id
├── tripId
├── placeId
├── plannedOrder
├── plannedDate
├── visited
├── visitedAt
└── createdAt
```

## Place

```text
Place
├── id
├── name
├── description
├── latitude
├── longitude
├── address
├── regionId
├── category
└── externalId
```

## Memory

```text
Memory
├── id
├── userId
├── tripId
├── placeId (optional)
├── image
├── caption
└── createdAt
```

Đây chỉ là conceptual model; database schema chính thức sẽ được quyết định ở phase architecture/database.

---

# 23. MVP Scope Matrix

| Feature | Priority | MVP |
|---|---:|---:|
| User profile | P0 | ✅ |
| Create Trip | P0 | ✅ |
| Edit Trip | P0 | ✅ |
| Delete Trip | P0 | ✅ |
| Trip list | P0 | ✅ |
| Trip detail | P0 | ✅ |
| Add Place | P0 | ✅ |
| Remove Place | P0 | ✅ |
| Reorder Place | P0 | ✅ |
| Itinerary | P0 | ✅ |
| AI itinerary suggestion | P0 | ✅ |
| User itinerary override | P0 | ✅ |
| Route | P0 | ✅ |
| Map | P0 | ✅ |
| Weather | P0 | ✅ |
| Weather suggestions | P1 | ✅ |
| Automatic weather itinerary mutation | — | ❌ |
| Check-in / Mark visited | P0 | ✅ |
| Memory photo | P0 | ✅ |
| Memory by Place | P0 | ✅ |
| Memory timeline | P1 | 🟡 |
| Journey progress | P0 | ✅ |
| 25/63 region progress | P0 | ✅ |
| Journey map | P0 | ✅ |
| Travel preferences | P1 | ✅ |
| AI personalization | P1 | ✅ |
| Milestones | P2 | 🟡 |
| Duplicate Trip | P2 | 🟡 |
| Archive Trip | P2 | 🟡 |
| Group Trip | P3 | ❌ |
| Shared Memories | P3 | ❌ |
| Public Profile | P3 | ❌ |
| Social Feed | P3 | ❌ |
| Followers | P3 | ❌ |
| Gamification engine | P3 | ❌ |
| ML Recommendation | P3 | ❌ |
| Offline-first | P3 | ❌ |

---

# 24. MVP Non-Goals

## Social

- Followers
- Following
- Like
- Comment
- Feed
- Public profile

## Group

- Group trip
- Family trip
- Collaborative editing
- Shared memory
- Group chat

## AI

- Train model riêng
- Fine-tuning
- ML recommendation pipeline
- Autonomous itinerary modification
- AI tự thao tác thay User

## Map

- Custom routing engine
- GIS platform
- Live group location
- Advanced offline map

## Weather

- Weather prediction model riêng
- Automatic itinerary mutation
- Weather guarantee

## Gamification

- XP
- Level
- Coins
- Leaderboard
- Streak

---

# 25. Critical Business Rules

### BR-01 — User is final decision maker

Mọi recommendation đều có thể bị User override.

### BR-02 — AI never silently modifies final itinerary

AI suggestion phải được User review/apply.

### BR-03 — Weather never automatically changes itinerary

Weather chỉ tạo context/suggestion.

### BR-04 — Planned does not mean visited

Place trong Trip không đồng nghĩa đã đến.

### BR-05 — Visited does not require Memory

Có thể check-in mà không chụp ảnh.

### BR-06 — Memory does not require check-in

Có thể lưu ảnh mà không dùng check-in.

### BR-07 — Deleting Trip does not necessarily delete Memory

Memory là dữ liệu cá nhân độc lập.

### BR-08 — Trip is private by default

Không public travel history trong MVP.

### BR-09 — External API failure must not break Trip

API lỗi chỉ làm mất capability tương ứng.

### BR-10 — User preferences are user-controlled

AI không tự ý thay đổi preference.

### BR-11 — Journey progress counts unique regions

Đi nhiều lần cùng một tỉnh/thành không tăng số region.

### BR-12 — Actual journey may differ from planned journey

```text
Plan ≠ Reality
```

---

# 26. UX Principles — Final Baseline

## UX-01
**Fast first-use.**

User phải tạo được Trip nhanh.

## UX-02
**Progressive disclosure.**

Chỉ hiển thị thông tin cần thiết ở từng bước.

## UX-03
**Context-aware home.**

Home ưu tiên Trip hiện tại.

## UX-04
**No unnecessary forms.**

Không hỏi dữ liệu chưa cần dùng.

## UX-05
**No hard dependency on external API.**

Weather/Map/AI lỗi không làm hỏng Trip.

## UX-06
**Human override.**

User luôn có thể sửa suggestion.

## UX-07
**Memory is emotional, not administrative.**

Memory phải dễ lưu, dễ xem lại.

## UX-08
**Journey should feel rewarding without becoming a game.**

Progress tạo động lực nhưng không biến sản phẩm thành game.

## UX-09
**One primary action at a time.**

Không overload UI.

## UX-10
**The system should remember, not control.**

Hệ thống ghi nhớ hành trình nhưng không kiểm soát hành trình.

---

# 27. MVP Success Criteria

MVP đạt mục tiêu nếu User mới có thể:

```text
1. Đăng nhập
       ↓
2. Tạo Trip
       ↓
3. Chọn địa điểm
       ↓
4. Nhận itinerary suggestion
       ↓
5. Chỉnh itinerary
       ↓
6. Xem route/map
       ↓
7. Xem weather context
       ↓
8. Đi đến địa điểm
       ↓
9. Mark visited
       ↓
10. Chụp/lưu memory
       ↓
11. Xem lại memory
       ↓
12. Thấy Journey progress tăng
```

Không cần:

- group;
- social network;
- ML model;
- offline engine;
- complex gamification.

---

# 28. Product Experience Statement

> **"Tôi chỉ cần nói mình muốn đi đâu và tôi thích kiểu trải nghiệm nào. Hệ thống giúp tôi chuẩn bị chuyến đi, còn trong chuyến đi tôi được tự do thay đổi mọi thứ. Sau chuyến đi, mọi trải nghiệm của tôi được lưu lại thành một phần hành trình cá nhân."**

---

# 29. Conceptual Product Architecture

```text
                         ┌─────────────┐
                         │    USER     │
                         └──────┬──────┘
                                │
              ┌─────────────────┼─────────────────┐
              │                 │                 │
              ▼                 ▼                 ▼
         PROFILE          PREFERENCES         JOURNEY
                                                  │
                                                  ▼
                                               REGIONS
                                                  │
                                                  ▼
                                               PLACES
                                                  ▲
                                                  │
                         ┌────────────────────────┘
                         │
                         ▼
                       TRIP
                         │
        ┌────────────────┼──────────────────┐
        │                │                  │
        ▼                ▼                  ▼
    ITINERARY           MAP               MEMORY
        │                │                  │
        └────────────┬───┘                  │
                     │                      │
                     ▼                      │
                 WEATHER                    │
                     │                      │
                     └──────────┬───────────┘
                                │
                                ▼
                         USER EXPERIENCE
```

AI là lớp hỗ trợ:

```text
                    ┌─────────────┐
                    │   AI API    │
                    └──────┬──────┘
                           │
             ┌─────────────┼──────────────┐
             │             │              │
             ▼             ▼              ▼
          Itinerary    Personalization  Suggestions
```

AI không phải trung tâm duy nhất của sản phẩm.

---

# 30. Final Decisions

## Product

- [x] Personal travel companion
- [x] Vietnam-wide scope
- [x] Individual-first
- [x] Group/family future
- [x] UX-first
- [x] MVP-focused

## Trip

- [x] Trip is central context
- [x] Planning → Ready → Active → Completed
- [x] Draft/planning supported
- [x] User can edit
- [x] User controls itinerary

## AI

- [x] Use external AI API
- [x] No custom model
- [x] Suggestion only
- [x] User approval required
- [x] No silent overwrite

## Weather

- [x] External Weather API
- [x] Advisory only
- [x] No automatic itinerary mutation
- [x] Failure must not break Trip

## Route

- [x] External Map/Routing API
- [x] Route supports itinerary
- [x] No custom routing engine

## Memory

- [x] Photo-based memories
- [x] Memory linked to Trip
- [x] Memory may link to Place
- [x] Memory private by default
- [x] Memory can survive Trip deletion where appropriate

## Journey

- [x] Track visited regions
- [x] Unique region count
- [x] Vietnam progress
- [x] Journey Map
- [x] Basic statistics

## Personalization

- [x] Interests
- [x] Pace
- [x] Optional budget
- [x] AI context
- [x] User-controlled preferences
- [x] No ML recommendation engine

## Social / Group

- [x] Explicitly outside MVP
- [x] Architecture should remain extensible

---

# 31. Definition of Done — Phase 4A & 4B

Phase 4A & 4B hoàn thành khi:

- [x] Product vision rõ ràng
- [x] Target user rõ ràng
- [x] MVP boundary rõ ràng
- [x] Core product loop rõ ràng
- [x] UX principles rõ ràng
- [x] Trip lifecycle rõ ràng
- [x] Place model concept rõ ràng
- [x] Itinerary behavior rõ ràng
- [x] AI responsibility rõ ràng ở mức product
- [x] Weather responsibility rõ ràng
- [x] Route responsibility rõ ràng
- [x] Memory behavior rõ ràng
- [x] Journey progress rõ ràng
- [x] Profile & personalization rõ ràng
- [x] Privacy baseline rõ ràng
- [x] Group/social boundary rõ ràng
- [x] Critical business rules được chốt
- [x] MVP success criteria được chốt

---

# 32. Next Phase

Sau khi Phase 4A & 4B được frozen, phase tiếp theo là:

> **PHASE 4C — AI & INTELLIGENT RECOMMENDATION SPECIFICATION**

Sẽ đặc tả:

```text
User Input
    ↓
Context Collection
    ↓
Place Data
    ↓
Weather Data
    ↓
Route Data
    ↓
User Preferences
    ↓
AI Prompt / Structured Input
    ↓
AI API
    ↓
Structured Output
    ↓
Validation
    ↓
Recommendation
    ↓
User Review
    ↓
Final Itinerary
```

Các nội dung chính:

- AI input;
- context;
- prompt structure;
- JSON output schema;
- validation;
- hallucination handling;
- invalid place handling;
- weather context;
- route context;
- personalization context;
- token/cost control;
- timeout/retry;
- fallback;
- security;
- prompt injection;
- AI recommendation vs final itinerary.

---

# 33. Final Principle

> ## **The system remembers the journey, but the user controls the journey.**

Hệ thống:

- giúp User lập kế hoạch;
- đề xuất;
- cung cấp thông tin;
- hỗ trợ di chuyển;
- lưu kỷ niệm;
- ghi nhận tiến trình;
- giúp User nhìn lại hành trình.

Nhưng:

> **User luôn là người quyết định chuyến đi thực tế.**
