# PHASE 4D — MEMORY, JOURNAL & TRAVEL EXPERIENCE

> **Status:** Finalized  
> **Project:** Hệ thống hỗ trợ lập kế hoạch và quản lý hành trình du lịch cá nhân tích hợp AI  
> **Scope:** MVP — Personal Travel Experience  
> **Phase:** 4D  
> **Related:** 4A — Core Product Flow, 4B — Itinerary & Planning, 4C — AI Planning & Recommendation

---

## 1. Mục tiêu của Phase 4D

Phase 4D định nghĩa toàn bộ hệ thống **Memory & Travel Experience** — phần giúp ứng dụng không chỉ hỗ trợ người dùng **lập kế hoạch chuyến đi**, mà còn lưu giữ và tái hiện những gì họ đã trải qua.

Triết lý cốt lõi:

> **Planner giúp người dùng biết sắp đi đâu.**  
> **Memory giúp người dùng lưu lại đã trải qua gì.**  
> **Journey giúp người dùng nhìn lại mình đã đi được bao xa.**

Phase 4D tập trung vào:

- Memory / Experience
- Photo & Media
- Journal & Story
- Place Memory Experience
- Travel Progress & Light Gamification

MVP ưu tiên **nhanh, gọn, dễ sử dụng**, tránh biến hệ thống thành một mạng xã hội, ứng dụng blogging hoặc game du lịch.

---

# 2. Product Philosophy

## 2.1. Experience-first

Người dùng không nên phải nhập quá nhiều dữ liệu.

Flow lý tưởng:

```text
Đi du lịch
    ↓
Đến địa điểm
    ↓
Chụp ảnh
    ↓
Lưu Memory
    ↓
(Optional) Viết vài dòng
    ↓
Tiếp tục hành trình
```

Không yêu cầu người dùng:

- nhập form dài;
- check-in bắt buộc;
- nhập thời gian chính xác;
- nhập mood;
- viết title;
- thêm hashtag;
- viết journal dài.

---

## 2.2. App ghi nhận, không điều khiển

Một nguyên tắc UX xuyên suốt:

> **App ghi nhận hành trình của người dùng, chứ không điều khiển hành trình của người dùng.**

Do đó:

- itinerary là kế hoạch, không phải mệnh lệnh;
- thời tiết chỉ là thông tin/đề xuất;
- thời gian check-in chỉ mang tính tham khảo;
- AI chỉ hỗ trợ;
- progress chỉ ghi nhận;
- Memory do người dùng chủ động tạo.

---

# 3. 4D.1 — MEMORY CORE

## 3.1. Khái niệm Memory

`Memory` đại diện cho một trải nghiệm mà người dùng muốn lưu lại tại một địa điểm trong một chuyến đi.

Một Memory có thể bao gồm:

```text
Memory
├── Trip
├── Place
├── Photos
├── Optional Note
└── CreatedAt
```

Ví dụ:

```text
📍 Hội An
📅 08/08/2026
📸 6 photos

"Buổi tối ở phố cổ rất đẹp..."
```

---

## 3.2. Memory là trung tâm của hệ thống trải nghiệm

Kiến trúc:

```text
Trip
  │
  └── Memory
       ├── Place
       ├── Photos
       └── Note
```

Memory là cầu nối giữa:

- itinerary;
- địa điểm;
- hình ảnh;
- journal;
- travel history;
- province progress.

---

## 3.3. Tạo Memory

Flow MVP:

```text
User
 ↓
Select / open Place
 ↓
Add Memory
 ↓
Select photos
 ↓
Optional note
 ↓
Save
```

Không bắt buộc phải có note.

Không bắt buộc phải có thời gian check-in.

---

## 3.4. Memory Ownership

MVP là personal-first.

Mỗi Memory thuộc về một user:

```text
Memory
├── id
├── userId
├── tripId
├── placeId
├── note
└── createdAt
```

Mặc định Memory là private.

---

## 3.5. Memory không phải Social Post

MVP không có:

- like;
- comment;
- share;
- follower;
- public post;
- feed;
- reaction.

Memory là:

> **Personal travel record.**

---

## 3.6. Memory Detail

Một Memory Detail có thể hiển thị:

```text
📍 Place
📅 Date
📸 Photo Gallery
📝 Note
```

Có thể truy cập Place từ Memory.

---

## 3.7. Edit Memory

User có thể chỉnh sửa:

- note;
- metadata cần thiết theo phạm vi MVP.

API dự kiến:

```http
PATCH /memories/{memoryId}
```

Không cần tạo Journal API riêng nếu Journal chỉ là note của Memory.

---

## 3.8. Delete Memory

Khi user xóa Memory:

- Memory bị xóa;
- các Photo thuộc Memory có thể được xử lý/xóa theo media lifecycle;
- progress của user được tính lại nếu Memory đó là bằng chứng duy nhất cho Place/Province.

Xóa Memory không được làm ảnh hưởng các Memory khác.

---

## 3.9. Không dùng GPS tự động

MVP không tự động:

- track GPS;
- detect user entering a Place;
- tạo Memory tự động;
- background location;
- geofencing.

Lý do:

- privacy;
- battery;
- permission;
- false positive;
- complexity.

User chủ động xác nhận trải nghiệm bằng cách tạo Memory.

---

# 4. 4D.2 — PHOTO & MEDIA MANAGEMENT

## 4.1. Vai trò

Ảnh là thành phần quan trọng nhất của Memory.

Mục tiêu:

> **Lưu ảnh chất lượng tốt nhưng tối ưu dung lượng, tốc độ tải và chi phí storage.**

---

## 4.2. Photo thuộc Memory

Không tạo album ảnh độc lập trong MVP.

```text
Memory
 └── Photos[]
```

Một Photo thuộc một Memory.

Place chỉ aggregate ảnh từ các Memory.

---

## 4.3. Media Storage Strategy

Ưu tiên sử dụng dịch vụ có sẵn thay vì tự xây media storage/processing.

Kiến trúc khuyến nghị:

```text
User
 ↓
Upload
 ↓
Object Storage / Media Service
 ↓
CDN
 ↓
Application
```

Có thể sử dụng các dịch vụ như Cloudinary, Cloudflare R2, S3-compatible storage hoặc provider tương đương tùy quyết định triển khai.

---

## 4.4. Không lưu binary trong Database

Database chỉ lưu metadata:

```text
Photo
├── id
├── memoryId
├── storageKey / publicId
├── url hoặc delivery reference
├── width
├── height
├── fileSize
├── mimeType
├── sortOrder
└── createdAt
```

Không lưu binary image trực tiếp trong DB.

---

## 4.5. Image Optimization

MVP nên có pipeline:

```text
Original
   ↓
Validation
   ↓
Upload / Processing
   ↓
Optimized image
   ↓
CDN
```

Ưu tiên:

- WebP/AVIF khi phù hợp;
- thumbnail;
- responsive delivery;
- lazy loading;
- CDN;
- giới hạn kích thước file.

---

## 4.6. Photo Upload UX

Flow phải nhanh:

```text
Add Memory
 ↓
Select multiple photos
 ↓
Preview
 ↓
Optional reorder
 ↓
Save
```

Không yêu cầu user chỉnh sửa ảnh trong app.

Không cần:

- crop editor;
- filter;
- advanced photo editor;
- AI photo enhancement.

---

## 4.7. Photo Order

Mỗi Memory có thể có thứ tự ảnh:

```text
sortOrder
```

Ví dụ:

```text
1 → cover
2 → gallery
3 → gallery
...
```

Ảnh đầu tiên có thể làm cover nếu user không chọn cover riêng.

---

## 4.8. Photo Deletion

Có thể xóa từng ảnh khỏi Memory.

Nếu Memory không còn ảnh:

```text
Memory
├── Place
└── Note
```

vẫn có thể tồn tại.

Tuy nhiên UI nên khuyến khích Memory có ít nhất một nội dung hữu ích.

---

## 4.9. Photo Privacy

Ảnh mặc định private.

Không được expose public URL tùy tiện nếu storage/provider hỗ trợ private delivery.

Authorization phải được kiểm tra trước khi trả resource của user.

---

# 5. 4D.3 — JOURNAL & STORY

## 5.1. Triết lý

Journal không phải blogging platform.

Mục tiêu:

> **Người dùng có thể ghi lại vài dòng để sau này nhớ mình đã trải qua điều gì.**

---

## 5.2. Journal = Optional Note

Không tạo `JournalEntry` riêng trong MVP.

Thay vào đó:

```text
Memory
├── Photos
└── Note
```

`note` chính là nội dung journal.

Lợi ích:

- giảm entity;
- giảm API;
- giảm database relation;
- giảm UI;
- giảm complexity.

---

## 5.3. Không cần Title

Không yêu cầu:

```text
Title
```

Hệ thống tự hiển thị context:

```text
📍 Hội An
📅 08/08/2026
```

User chỉ cần viết:

> "Buổi tối ở đây rất đẹp..."

---

## 5.4. Note là Optional

User có thể:

```text
📸 Upload ảnh
→ Save
```

hoặc:

```text
📸 Upload ảnh
→ Viết note
→ Save
```

Không bắt buộc viết journal.

---

## 5.5. Note Format

MVP:

```text
Plain Text
```

Không hỗ trợ:

- Markdown;
- rich text;
- font formatting;
- heading;
- embedded media;
- màu chữ.

---

## 5.6. Note Length

Đề xuất:

```text
Maximum: 2,000 characters / Memory
```

Đủ cho journal ngắn nhưng không biến thành blog.

---

## 5.7. Journal Prompt

Có thể cung cấp prompt nhẹ:

```text
"Điều gì bạn nhớ nhất ở đây?"
"Hôm nay có gì đặc biệt?"
"Khoảnh khắc đáng nhớ nhất?"
"Món ăn nào bạn muốn thử lại?"
```

Prompt chỉ là gợi ý.

Không bắt buộc.

---

## 5.8. AI-generated Journal

Không đưa vào MVP.

Không tự động:

- đọc ảnh rồi viết câu chuyện;
- tạo cảm xúc;
- tạo facts;
- tự viết trải nghiệm.

Future có thể hỗ trợ:

```text
User note
 ↓
AI rewrite
 ↓
Suggested text
 ↓
User accepts
```

AI chỉ hỗ trợ, không thay thế trải nghiệm thật.

---

## 5.9. Story

`Story` không phải entity trong MVP.

Story là một cách trình bày:

```text
Trip
 ↓
Memory[]
 ↓
Sort by time
 ↓
Trip Story
```

Ví dụ:

```text
Day 1

📍 Đà Nẵng
📸 5 photos
📝 Note

      ↓

📍 Hội An
📸 7 photos
📝 Note

Day 2

📍 Huế
📸 4 photos
📝 Note
```

---

## 5.10. Trip Story

Một chuyến đi có thể tự động trở thành Story.

Không cần:

```text
Story table
```

Không cần:

```text
Story entity
```

Story chỉ là view/model được dựng từ Trip + Memory.

---

## 5.11. Journal Privacy

Mặc định:

```text
PRIVATE
```

Chỉ owner thấy.

Future có thể có group/public story nhưng không thuộc MVP.

---

## 5.12. Journal Editing

UI:

```text
Memory Detail
 ↓
Edit note
 ↓
Save
```

Không cần editor phức tạp.

Không cần server-side draft trong MVP.

---

# 6. 4D.4 — PLACE MEMORY EXPERIENCE

## 6.1. Mục tiêu

Khi người dùng mở một địa điểm, họ có thể thấy:

> **"Mình đã từng đến đây và đã lưu những gì?"**

Ví dụ:

```text
📍 Hội An

Bạn đã đến đây 2 lần
13 kỷ niệm
```

---

## 6.2. Place vs Memory

### Place

Địa điểm thực tế:

```text
Hội An Ancient Town
```

### Memory

Trải nghiệm của user:

```text
08/08/2026
6 photos
"Buổi tối rất đẹp..."
```

Một Place có thể có nhiều Memory.

---

## 6.3. Place Memory Page

Ví dụ:

```text
📍 Hội An

Đã ghé: 2 lần
Kỷ niệm: 3
Ảnh: 13

2026
────────────
📸 08/08/2026
"Buổi tối rất đẹp..."
6 photos

2025
────────────
📸 15/06/2025
"Lần đầu đến Hội An..."
7 photos
```

Memory là nội dung chính.

Place là context.

---

## 6.4. Visit Count

Không dùng GPS.

Có thể tính số lần ghé dựa trên các Trip khác nhau:

```text
Trip A
 ├── Memory → Hội An
 └── Memory → Hội An

Trip B
 └── Memory → Hội An
```

Hiển thị:

```text
Đã ghé 2 lần
```

thay vì 3 visits.

---

## 6.5. First / Last Visit

Có thể tính:

```text
firstVisitedAt = MIN(memory.createdAt)
lastVisitedAt = MAX(memory.createdAt)
```

Không cần user nhập thủ công.

---

## 6.6. "Bạn đã đến đây"

Nếu có Memory:

```text
✨ Bạn đã từng đến đây
```

Nếu chưa:

```text
Bạn chưa lưu kỷ niệm nào ở đây.
```

Đây là micro-interaction quan trọng để tạo cảm giác app "nhớ" hành trình.

---

## 6.7. My Places

Một collection riêng cho user:

```text
MY PLACES

📍 Đà Nẵng
8 memories

📍 Hội An
13 memories

📍 Huế
5 memories

📍 Đà Lạt
21 memories
```

Không cần tạo `MyPlace` entity.

Chỉ aggregate từ Memory.

---

## 6.8. Place Provider

Place discovery ưu tiên dùng API có sẵn.

Kiến trúc:

```text
PlaceService
     ↓
PlaceProvider
     ↓
External Places API
```

Không hard-code provider trực tiếp vào business logic.

Có thể thay provider về sau.

---

## 6.9. Place Data

MVP chỉ cần dữ liệu cốt lõi:

```text
Place
├── id
├── externalPlaceId
├── provider
├── name
├── address
├── latitude
├── longitude
└── provinceId
```

Không cần lưu hàng loạt dữ liệu discovery như:

- review;
- rating;
- opening hours;
- phone;
- menu;
- price level.

---

## 6.10. External API Failure

Lịch sử Memory không được phụ thuộc runtime vào external Places API.

Nếu provider lỗi, user vẫn phải xem được:

```text
📍 Hội An
📸 Photos
📝 Note
```

Do đó hệ thống phải lưu dữ liệu Place tối thiểu trong database.

---

## 6.11. Province Mapping

Place cần liên kết với Province:

```text
Place
 ↓
Province
```

Ví dụ:

```text
Hội An
 ↓
Quảng Nam
```

Province là cơ sở để tính travel progress.

---

## 6.12. Province Progress

Một tỉnh/thành được xem là đã khám phá khi:

> User có ít nhất một Memory hợp lệ thuộc một Place nằm trong tỉnh/thành đó.

Không tính từ itinerary.

Ví dụ:

```text
Itinerary:
Đà Nẵng → Huế → Hội An

Memory:
Đà Nẵng
Hội An
```

Kết quả:

```text
Đà Nẵng ✓
Hội An / Quảng Nam ✓
Huế chưa được tính nếu chưa có Memory
```

---

# 7. 4D.5 — TRAVEL PROGRESS & LIGHT GAMIFICATION

## 7.1. Mục tiêu

Tạo cảm giác:

> **"Mỗi chuyến đi đều góp thêm một phần vào hành trình của mình."**

Ví dụ:

```text
🇻🇳 MY JOURNEY

25 / 63
TỈNH/THÀNH ĐÃ KHÁM PHÁ

████████████░░░░
```

---

## 7.2. Light Gamification

MVP chỉ sử dụng:

```text
Progress
Collection
Milestone
```

Không xây game system.

Không có:

- XP;
- level;
- coin;
- energy;
- quest;
- streak;
- leaderboard;
- ranking;
- PvP.

---

## 7.3. Progress Source

Progress được tính từ:

```text
Memory
 ↓
Place
 ↓
Province
```

Không tính từ:

```text
Itinerary
```

Nếu user chỉ lập kế hoạch đi Huế nhưng chưa có Memory:

```text
Huế = Not explored
```

---

## 7.4. "Explored" không cần Check-in

Không yêu cầu:

- GPS;
- QR;
- location tracking;
- check-in button.

Memory là bằng chứng trải nghiệm.

---

## 7.5. My Journey

Dashboard:

```text
MY JOURNEY

25 / 63 provinces

68 places
213 memories
824 photos
12 trips
```

Chỉ nên hiển thị vài chỉ số quan trọng.

Không biến thành analytics dashboard.

---

## 7.6. Province List

Có thể chia:

```text
Đã khám phá
────────────
✓ Đà Nẵng
✓ Huế
✓ Khánh Hòa

Chưa khám phá
────────────
○ Cao Bằng
○ Ninh Bình
○ Hà Giang
```

Có thể filter:

```text
All
Explored
Not explored
```

---

## 7.7. Vietnam Progress Map

Một bản đồ Việt Nam có thể visual hóa:

```text
Explored provinces
vs
Not explored provinces
```

Không xây GIS engine.

MVP có thể dùng:

```text
SVG / GeoJSON
```

hoặc provider phù hợp.

---

## 7.8. Province Detail

Click province:

```text
🇻🇳 Đà Nẵng

✓ Explored

8 memories
3 places

First visited:
12/05/2025

Last visited:
08/08/2026

[View memories]
```

---

## 7.9. Milestones

MVP có thể dùng các mốc:

```text
1
5
10
25
50
```

Ví dụ:

```text
🎉 New milestone!

Bạn đã khám phá 10 tỉnh/thành.
```

Milestone có thể được cấu hình, không cần hệ thống Badge phức tạp.

---

## 7.10. Milestone không cần Entity

Có thể cấu hình:

```text
MILESTONES = [1, 5, 10, 25, 50]
```

Kiểm tra:

```text
previousProgress < milestone
AND
currentProgress >= milestone
```

Không cần:

```text
Badge
UserBadge
Achievement
```

cho MVP.

---

## 7.11. Không có Daily Streak

Du lịch không phải hoạt động hàng ngày.

Không xây:

```text
🔥 12 days streak
```

---

## 7.12. Không có Daily Mission

Không ép:

```text
Visit 1 place today
Upload 3 photos today
```

Điều này không phù hợp với bản chất sản phẩm.

---

## 7.13. Không có Leaderboard

MVP không có:

```text
Top Travelers
#1 ...
#2 ...
```

Lý do:

- social chưa phải MVP;
- privacy;
- moderation;
- không đóng góp trực tiếp vào core value.

---

## 7.14. Planned vs Experienced

Có thể visual hóa:

```text
PLANNED
A → B → C → D

EXPERIENCED
A → B → D
```

C không được coi là lỗi.

Itinerary là kế hoạch.

Memory là trải nghiệm thực tế.

---

# 8. Data Model tổng thể của Phase 4D

MVP ưu tiên ít entity.

```text
User
 │
 └── Trip
      │
      └── Memory
           ├── Place
           │    └── Province
           │
           ├── Photos[]
           │
           └── Note
```

Entity chính:

```text
User
Trip
Place
Province
Memory
Photo
```

Không tạo riêng trong MVP:

```text
JournalEntry
Story
MyPlace
UserProgress
Badge
Achievement
UserBadge
Visit
```

Các khái niệm này được tính hoặc dựng từ dữ liệu hiện có.

---

# 9. Quan hệ dữ liệu

## 9.1. Memory

```text
Memory
├── id
├── userId
├── tripId
├── placeId
├── note
└── createdAt
```

---

## 9.2. Photo

```text
Photo
├── id
├── memoryId
├── storageKey / publicId
├── url / delivery reference
├── width
├── height
├── fileSize
├── mimeType
├── sortOrder
└── createdAt
```

---

## 9.3. Place

```text
Place
├── id
├── externalPlaceId
├── provider
├── name
├── address
├── latitude
├── longitude
└── provinceId
```

---

## 9.4. Province

Tối thiểu:

```text
Province
├── id
├── code
├── name
├── region
└── geometry (nếu cần map)
```

Dataset versioning có thể được bổ sung khi cần.

---

# 10. API định hướng

## 10.1. Memory

```http
POST /memories
GET /memories/{memoryId}
PATCH /memories/{memoryId}
DELETE /memories/{memoryId}
```

---

## 10.2. Photos

```http
POST /memories/{memoryId}/photos
DELETE /photos/{photoId}
PATCH /memories/{memoryId}/photos/order
```

API thực tế có thể thay đổi theo storage architecture.

---

## 10.3. Place

```http
GET /places/{placeId}
GET /places/{placeId}/memories
GET /me/places
```

---

## 10.4. Exploration

```http
GET /me/exploration
GET /me/exploration/provinces
GET /me/exploration/provinces/{provinceId}
```

---

# 11. Performance & Indexing

Các query chính cần index phù hợp:

```text
Memory(userId, placeId)
Memory(userId, createdAt)
Photo(memoryId, sortOrder)
Place(id, provinceId)
```

Progress có thể tính từ database trong MVP.

Không cần Redis cho progress ngay từ đầu.

Nếu scale lớn mới cân nhắc denormalization/cache.

---

# 12. Privacy & Security

Tất cả Memory và Photo mặc định private.

Authorization phải đảm bảo:

```text
User A
 ↓
Memory A
```

User B không thể:

```text
GET /memories/A
```

nếu không có quyền.

External media URL cũng phải được thiết kế phù hợp với privacy model.

---

# 13. UX Principles của Phase 4D

## Principle 1 — Minimal Friction

Người dùng phải có thể lưu Memory rất nhanh.

```text
Photo
→ Save
```

là một flow hợp lệ.

---

## Principle 2 — Optional Expression

Note là optional.

Không bắt viết journal.

---

## Principle 3 — Personal First

Memory mặc định private.

Social/group để future.

---

## Principle 4 — Contextual Memory

Khi mở Place, user thấy lại những Memory cũ.

```text
Place
 ↓
Your memories
```

---

## Principle 5 — Progress From Reality

Progress dựa trên trải nghiệm đã lưu, không phải kế hoạch.

---

## Principle 6 — No Forced Gamification

Gamification chỉ tạo cảm giác tiến bộ.

Không ép người dùng:

- đăng nhập mỗi ngày;
- hoàn thành quest;
- cạnh tranh;
- đạt streak.

---

## Principle 7 — External APIs Are Helpers

External API có thể hỗ trợ:

- Place search;
- media;
- AI;
- map;

nhưng dữ liệu lịch sử quan trọng của user phải được hệ thống lưu và kiểm soát.

---

# 14. MVP Scope Summary

## MUST HAVE

```text
MEMORY
✅ Create Memory
✅ View Memory
✅ Edit Memory
✅ Delete Memory
✅ Link Memory to Trip
✅ Link Memory to Place

PHOTO
✅ Multiple photo upload
✅ Photo preview
✅ Photo ordering
✅ Photo deletion
✅ Optimized storage/delivery

JOURNAL
✅ Optional note
✅ Plain text
✅ Edit note
✅ Delete/clear note
✅ Character limit

STORY
✅ Trip timeline
✅ Story view generated from Memory

PLACE EXPERIENCE
✅ Place linked to Memory
✅ Memories by Place
✅ My Places
✅ Visit count
✅ First/Last visit
✅ Visited indicator

TRAVEL PROGRESS
✅ Province mapping
✅ Explored provinces
✅ My Journey summary
✅ Province list
```

---

# 15. SHOULD HAVE

```text
🟡 Journal prompts
🟡 Character counter
🟡 Unsaved changes warning
🟡 "You visited here before"
🟡 Vietnam exploration map
🟡 Province detail
🟡 First/Latest exploration
🟡 Basic milestones
🟡 Planned vs Experienced visualization
```

Các tính năng SHOULD HAVE chỉ được triển khai nếu không ảnh hưởng tiến độ MVP.

---

# 16. FUTURE / OUT OF MVP

```text
❌ GPS auto-detection
❌ Background location tracking
❌ Geofencing
❌ Automatic check-in
❌ AI-generated journal
❌ AI-generated story
❌ AI photo analysis
❌ Rich text journal
❌ Markdown
❌ Mood tracking
❌ Hashtags
❌ Journal search
❌ Public stories
❌ Likes
❌ Comments
❌ Followers
❌ Group journal
❌ Collaborative memory
❌ Story sharing
❌ Story export
❌ Video story generation
❌ XP
❌ Levels
❌ Streaks
❌ Daily quests
❌ Leaderboards
❌ Badges system
❌ Friend competitions
❌ Rewards
```

---

# 17. Future Group Compatibility

Mặc dù MVP chỉ hỗ trợ cá nhân, kiến trúc không nên khóa khả năng mở rộng.

Future:

```text
Group
 │
 └── Group Trip
      │
      ├── User A → Memory
      ├── User B → Memory
      └── User C → Memory
```

Place vẫn có thể dùng chung:

```text
Place
 ↑
 ├── Personal Memory
 └── Group Memory
```

Nhưng MVP chỉ:

```text
Personal Memory
```

---

# 18. End-to-End Experience

Toàn bộ Phase 4D tạo thành một vòng lặp:

```text
             PLAN TRIP
                 │
                 ▼
             VISIT PLACE
                 │
                 ▼
            SAVE MEMORY
                 │
          ┌──────┴──────┐
          ▼             ▼
        PHOTOS         NOTE
          │             │
          └──────┬──────┘
                 ▼
             EXPERIENCE
                 │
       ┌─────────┴─────────┐
       ▼                   ▼
  PLACE HISTORY        TRIP STORY
       │                   │
       └─────────┬─────────┘
                 ▼
            MY JOURNEY
                 │
                 ▼
          PROGRESS / MAP
                 │
                 ▼
          NEXT ADVENTURE
```

---

# 19. Core Product Loop

Phase 4D hoàn thiện một product loop rất rõ:

```text
Plan
 ↓
Travel
 ↓
Capture
 ↓
Remember
 ↓
See Progress
 ↓
Plan Next Adventure
```

Đây là vòng lặp quan trọng của toàn bộ sản phẩm.

---

# 20. Architectural Principle

Một trong những quyết định quan trọng nhất:

> **Không xây riêng một hệ thống cho từng tính năng nếu dữ liệu có thể tái sử dụng.**

Ví dụ:

```text
Memory
 ↓
Photo
 ↓
Place
 ↓
Province
```

có thể đồng thời phục vụ:

- Memory Detail;
- Journal;
- Trip Story;
- Place History;
- My Places;
- Travel Progress;
- Province Map;
- Future AI context.

Điều này giúp MVP vừa đơn giản vừa có nền tảng để mở rộng.

---

# 21. Phase 4D — Final Decisions

Các quyết định được chốt:

### Memory

- Memory là trung tâm của trải nghiệm.
- Memory thuộc User + Trip + Place.
- Memory có thể chứa nhiều Photo.
- Note là optional.
- Memory mặc định private.

### Photo

- Photo thuộc Memory.
- Không lưu binary trong database.
- Ưu tiên media/storage API có sẵn.
- Tối ưu ảnh và CDN.
- Không xây photo editor trong MVP.

### Journal

- Journal = Note của Memory.
- Không tạo Journal entity.
- Plain text.
- Tối đa khoảng 2.000 ký tự.
- Không bắt buộc viết.
- AI-generated journal để future.

### Story

- Story không phải entity.
- Story được render từ Trip + Memory.
- Trip Story là personal timeline.
- Không có public story trong MVP.

### Place Experience

- Place là địa điểm thực tế.
- Memory là trải nghiệm tại Place.
- Một Place có thể có nhiều Memory.
- My Places được aggregate từ Memory.
- Không GPS auto-detection.
- External Places API chỉ phục vụ discovery.
- Lịch sử phải được lưu nội bộ.

### Travel Progress

- Progress dựa trên Memory.
- Không dựa trên itinerary.
- Province được tính là explored khi user có Memory hợp lệ tại đó.
- Có thể hiển thị bản đồ khám phá.
- Gamification chỉ ở mức progress + milestone.
- Không XP / level / streak / leaderboard.
- Không hard-code "63" vào business logic.

---

# 22. Final Phase 4D Architecture

```text
                         ┌──────────────┐
                         │     USER     │
                         └──────┬───────┘
                                │
                                ▼
                         ┌──────────────┐
                         │     TRIP     │
                         └──────┬───────┘
                                │
                                ▼
                         ┌──────────────┐
                         │    MEMORY    │
                         └──────┬───────┘
                                │
                ┌───────────────┼───────────────┐
                │               │               │
                ▼               ▼               ▼
           ┌─────────┐     ┌─────────┐     ┌─────────┐
           │  PLACE  │     │  PHOTO  │     │  NOTE   │
           └────┬────┘     └─────────┘     └─────────┘
                │
                ▼
           ┌──────────┐
           │ PROVINCE │
           └────┬─────┘
                │
        ┌───────┴────────┐
        ▼                ▼
  PLACE HISTORY      MY JOURNEY
        │                │
        ▼                ▼
   TRIP STORY      TRAVEL PROGRESS
                         │
                         ▼
                  EXPLORATION MAP
```

---

# 23. Final UX Statement

Phase 4D không nhằm biến ứng dụng thành:

- mạng xã hội;
- app viết nhật ký;
- app quản lý ảnh;
- game du lịch.

Mà tạo ra một lớp trải nghiệm thống nhất:

> **Người dùng đi → lưu lại → nhìn lại → cảm nhận hành trình → tiếp tục khám phá.**

Đây là nền tảng để sản phẩm có giá trị lâu dài:

```text
                YOUR JOURNEY

        "Where should I go?"
                  ↓
               Planner

        "What did I experience?"
                  ↓
               Memory

        "What do I remember?"
                  ↓
             Journal/Story

        "Where have I been?"
                  ↓
             My Places

        "How far have I gone?"
                  ↓
             My Journey
```

---

# 24. Phase 4D Completion Checklist

```text
[✓] 4D.1 — Memory Core
[✓] 4D.2 — Photo & Media Management
[✓] 4D.3 — Journal & Story
[✓] 4D.4 — Place Memory Experience
[✓] 4D.5 — Travel Progress & Light Gamification

Phase 4D: COMPLETED
```

> **Next:** Phase 4E — [to be defined after reviewing the complete Product Blueprint]
