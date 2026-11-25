# AGENTS.md — Bộ Quy Tắc Làm Việc Cho AI Agent
## Dự án Tripzy — Frontend (Web)

> **Vai trò của file:** Đây là bộ quy tắc làm việc dành cho mọi AI Coding Agent khi tham gia dự án **frontend web** của Tripzy.
>
> Agent phải đọc file này trước khi phân tích, sửa hoặc tạo code.
>
> **Thứ tự ưu tiên khi có mâu thuẫn:**
> 1. Yêu cầu trực tiếp của người dùng trong task hiện tại.
> 2. Kiến trúc, quyết định UI/UX và data-flow đã được chốt trong tài liệu dự án/codebase.
> 3. Quy tắc trong file này.
> 4. Best practice chung của Next.js/React.
>
> Agent **không được tự ý thay đổi kiến trúc hoặc quyết định đã chốt** chỉ vì có cách khác mà agent cho là "hay hơn". Nếu nhận thấy quyết định hiện tại có rủi ro đáng kể (UX, performance, accessibility), phải nêu rõ và đề xuất phương án trước khi thay đổi.

---

# 0. CONTEXT CỦA DỰ ÁN

## 0.1. Tên & Mục tiêu

**Tripzy** — Hệ thống hỗ trợ lập kế hoạch và quản lý hành trình du lịch cá nhân tích hợp AI gợi ý lộ trình, thời tiết và nhật ký trải nghiệm.

Ứng dụng phục vụ toàn bộ vòng đời chuyến đi theo mô hình:

```text
CREATE → PLAN → EXPLORE → CAPTURE → REMEMBER
```

Chi tiết:

```text
Tạo Trip
    ↓
Thêm địa điểm
    ↓
Hệ thống tạo/gợi ý Itinerary (Held–Karp + AI)
    ↓
User xác nhận / chỉnh sửa
    ↓
Bắt đầu Trip → Xem Map + Weather
    ↓
Check-in / Mark Visited
    ↓
Lưu Memory (ảnh + ghi chú)
    ↓
Hoàn thành Trip
    ↓
Journal + Journey Map + Thống kê
```

Sản phẩm chia thành 6 nhóm chức năng chính của MVP:

```text
1. Authentication (Đăng ký / Đăng nhập)
2. Trip Management (Danh sách Trip / Tạo Trip)
3. Trip Workspace (Itinerary · Map · Weather · Check-in)
4. Memory & Journal (Ảnh · Ghi chú · Timeline)
5. Journey Progress (Bản đồ Việt Nam · 25/63 tỉnh/thành)
6. Account & Profile (Hồ sơ · Sở thích du lịch)
```

> **Phạm vi MVP:** Toàn bộ Việt Nam. Đối tượng chính: cá nhân tự túc. Group/Family là hướng mở rộng tương lai, **không triển khai trong MVP**.

## 0.2. Triết lý sản phẩm (từ docs đã chốt)

| Nguyên tắc | Ý nghĩa |
|---|---|
| **User is final decision maker** | Mọi recommendation (AI, Weather) đều có thể bị User override |
| **System suggests, User decides** | AI / Weather / Route chỉ là hỗ trợ, không kiểm soát |
| **Plan is a guide, not a rule** | Itinerary là kế hoạch, không phải mệnh lệnh |
| **App ghi nhận, không điều khiển** | Hệ thống lưu hành trình nhưng không ép User tuân thủ |
| **UX: Fast · Simple · Effective** | Ít thao tác, ít form, kết quả rõ ràng, dễ hành động |
| **Progressive Disclosure** | Chỉ hỏi thông tin khi thực sự cần, không overload form ngay từ đầu |

## 0.3. Backend & API Contract

Frontend gọi REST API do backend Spring Boot cung cấp tại `/api/v1/...`.

Base error response shape của backend:

```json
{
  "status": 400,
  "code": "VALIDATION_ERROR",
  "message": "Invalid request",
  "errors": []
}
```

Frontend **không tự bịa** field/DTO/response shape — phải kiểm tra API contract thực tế (OpenAPI/Swagger hoặc codebase backend) trước khi implement. Nếu thiếu thông tin contract, phải nêu rõ assumption thay vì đoán.

Các API endpoint chính đã chốt (Phase 8):

```text
Auth:     POST /api/v1/auth/register · /auth/login · /auth/refresh · /auth/logout
User:     GET/PUT /api/v1/me
Trip:     POST/GET /api/v1/trips · GET/PUT/DELETE /api/v1/trips/{tripId}
Places:   POST/GET /api/v1/trips/{tripId}/places · DELETE /api/v1/trips/{tripId}/places/{placeId}
Planning: POST /api/v1/trips/{tripId}/planning
          GET/PUT /api/v1/trips/{tripId}/itinerary
Memory:   POST/GET /api/v1/trips/{tripId}/memories · PATCH /api/v1/memories/{memoryId}
Journey:  GET /api/v1/me/journey
```

## 0.4. Frontend stack đã chốt

| Thành phần | Công nghệ |
|---|---|
| Framework | Next.js (App Router) |
| Ngôn ngữ | TypeScript |
| Styling | Tailwind CSS |
| UI Components | shadcn/ui |
| Animation | Framer Motion |
| Server state | TanStack Query |
| Client/UI state | Zustand |
| Form | React Hook Form + Zod |
| Drag & Drop | @dnd-kit/core, @dnd-kit/sortable |
| Map | Google Maps (component riêng, đồng bộ với itinerary) |
| Places/Autocomplete | Google Places API |
| Upload | React Dropzone |
| Charts | Recharts (dùng cho Journey statistics) |
| Image | next/image |
| Resizable layout | react-resizable-panels |

> **Lưu ý:** Không tự ý đổi framework/thư viện chính nếu chưa có quyết định rõ ràng từ người dùng. Phải kiểm tra `package.json` hiện tại trước khi đề xuất dependency mới.

---

# 1. NGUYÊN TẮC LÀM VIỆC CỐT LÕI

## 1.1. Không đoán dữ liệu / hành vi UI

Không tự bịa:

- tên field trong response/request DTO;
- route/URL chưa được xác nhận;
- trạng thái (status) của Trip/Place;
- shape dữ liệu AI trả về;
- business rule (ví dụ: điều kiện Trip chuyển `COMPLETED`).

Nếu thông tin đã có trong: code hiện tại, tài liệu spec, `AGENTS.md`, API contract của backend — phải ưu tiên nguồn đó.

Nếu vẫn thiếu và ảnh hưởng đến tính đúng đắn:

1. tìm trong codebase (component tương tự, hook tương tự, store tương tự);
2. tìm trong tài liệu/spec (`docs/phase-*/`);
3. nếu chưa đủ, nêu rõ assumption;
4. chỉ hỏi người dùng khi assumption có thể thay đổi kiến trúc hoặc UX flow chính.

**Không hỏi những câu có thể tự xác định từ codebase hoặc context.**

## 1.2. Không phá vỡ kiến trúc hiện có

Trước khi tạo component/hook/store mới:

1. kiểm tra folder hiện tại theo feature;
2. tìm component/hook tương tự đã tồn tại;
3. tìm pattern state management đang dùng (Zustand slice, TanStack Query key);
4. tái sử dụng pattern nếu phù hợp.

Không tự ý refactor toàn bộ project chỉ vì phát hiện style khác. Nếu cần thay đổi kiến trúc lớn: giải thích lý do, nêu phạm vi ảnh hưởng, đề xuất trước, chỉ thực hiện khi task cho phép.

## 1.3. Không code giả

Không được:

- tạo component rỗng/placeholder rồi báo hoàn thành;
- để `TODO` thay cho phần logic chính;
- dùng mock data cứng thay cho gọi API thật khi task yêu cầu tích hợp thật;
- catch lỗi rồi bỏ qua im lặng (silent fail) mà không xử lý UI trạng thái lỗi;
- viết test kiểu `expect(true).toBe(true)`;
- disable ESLint/TypeScript error để "cho chạy" mà không giải thích lý do.

Nếu một phần chưa thể hoàn thiện, phải nói rõ theo format:

```text
Đã làm:
...
Chưa làm:
...
Lý do:
...
Cần quyết định/thông tin:
...
```

## 1.4. Mỗi thay đổi phải có lý do

Sau mỗi task, agent phải tóm tắt: đã thay đổi gì; vì sao; component/route/store bị ảnh hưởng; hành vi UI/UX thay đổi như thế nào; đã test/kiểm tra bằng cách nào; việc gì còn lại.

## 1.5. Ưu tiên tính đúng đắn & trải nghiệm

```text
Correctness (đúng dữ liệu, đúng flow)
    ↓
Accessibility & Responsiveness
    ↓
Maintainability (tách layer rõ ràng)
    ↓
Testability
    ↓
Performance
    ↓
Developer Convenience
```

Không tối ưu sớm khi chưa có bằng chứng về bottleneck.

---

# 2. KIẾN TRÚC TỔNG THỂ

## 2.1. Tầng hệ thống

Agent phải tư duy theo **tầng**, không phải "page nào thích dùng gì thì dùng":

```text
┌─────────────────────────────────────┐
│              UI Layer               │
│ Tailwind + shadcn/ui + Framer Motion│
├─────────────────────────────────────┤
│          Component Layer            │
│ Card / Modal / Sheet / Form / Map   │
├─────────────────────────────────────┤
│        Client State Layer           │
│             Zustand                 │
├─────────────────────────────────────┤
│         Server State Layer          │
│          TanStack Query             │
├─────────────────────────────────────┤
│       Interaction Layer             │
│ DnD / Resize / Gesture / Map        │
├─────────────────────────────────────┤
│           API Layer                 │
│ REST client / Next.js Route Handler │
├─────────────────────────────────────┤
│          External Services          │
│ Google Maps / Places / AI / Storage │
└─────────────────────────────────────┘
```

## 2.2. Cấu trúc thư mục định hướng (package-by-feature)

```text
src/
├── app/                          # Next.js App Router (routes)
│   ├── (marketing)/
│   │   └── page.tsx              # Landing page
│   ├── (auth)/
│   │   ├── login/page.tsx
│   │   └── register/page.tsx
│   ├── trips/
│   │   ├── page.tsx              # My Trips dashboard
│   │   ├── new/
│   │   │   └── page.tsx          # Create Trip wizard
│   │   └── [tripId]/
│   │       ├── layout.tsx        # TripLayout (Header + Sidebar + Map)
│   │       ├── page.tsx          # Trip Overview
│   │       ├── itinerary/page.tsx
│   │       ├── memories/page.tsx
│   │       └── settings/page.tsx
│   ├── journey/
│   │   └── page.tsx              # Journey Map (25/63 tỉnh/thành)
│   └── account/
│       └── page.tsx
│
├── features/                     # logic theo domain, tách khỏi routing
│   ├── trip/
│   │   ├── components/           # TripCard, TripStatusBadge
│   │   ├── hooks/
│   │   ├── api/                  # useTripQuery, useCreateTripMutation
│   │   ├── store/                # useTripUIStore (Zustand)
│   │   ├── schema/               # createTripSchema (Zod)
│   │   └── types/                # Trip, TripStatus types
│   ├── itinerary/
│   │   ├── components/           # DayContainer, PlaceCard, TravelInfo
│   │   ├── hooks/
│   │   ├── api/                  # useItineraryQuery, usePlanningMutation
│   │   ├── store/                # useItineraryUIStore (selectedPlaceId, hoveredPlaceId)
│   │   ├── schema/
│   │   └── types/
│   ├── place/
│   │   ├── components/           # PlaceSearch, PlaceDetail
│   │   ├── hooks/
│   │   ├── api/
│   │   └── types/
│   ├── map/                      # Isolated Google Maps logic
│   │   ├── components/           # MapView, TripMarker, RoutePolyline
│   │   ├── hooks/
│   │   └── types/
│   ├── memory/
│   │   ├── components/           # MemoryGallery, MemoryCard, MemoryLightbox
│   │   ├── hooks/
│   │   ├── api/                  # useMemoryQuery, useCreateMemoryMutation
│   │   ├── store/                # useMemoryUIStore
│   │   ├── schema/               # createMemorySchema (Zod)
│   │   └── types/
│   ├── journey/
│   │   ├── components/           # VietnamMap, ProvinceCard, JourneyStats
│   │   ├── hooks/
│   │   ├── api/                  # useJourneyQuery
│   │   └── types/
│   └── auth/
│       ├── components/           # LoginForm, RegisterForm
│       ├── hooks/
│       ├── api/
│       ├── store/                # useAuthStore
│       ├── schema/               # loginSchema, registerSchema (Zod)
│       └── types/
│
├── components/ui/                # shadcn/ui base components (dùng chung)
├── components/shared/            # component tái sử dụng liên-feature
│   ├── TripLayout.tsx
│   ├── TripHeader.tsx
│   └── TripSidebar.tsx
├── lib/                          # api client, utils, constants
│   ├── api.ts                    # axios instance (base URL, auth, error handling)
│   ├── utils.ts                  # cn(), formatCurrency(), formatDate()
│   └── constants.ts              # QUERY_KEYS, ROUTES
├── hooks/                        # hook dùng chung, không thuộc riêng feature
│   ├── useAuth.ts
│   └── useMediaQuery.ts
└── styles/
    └── globals.css
```

Tên feature phải phản ánh domain thực tế (`itinerary`, `memory`, `journey`...), không gộp tất cả vào `components/` phẳng.

## 2.3. Dependency direction

```text
Route (app/) → Feature component → Hook (store/api) → lib/api client → Backend
```

Route/page components:

- compose layout + gọi feature component;
- **không** chứa business logic phức tạp;
- **không** gọi trực tiếp `fetch`/axios — phải qua hook trong `features/*/api`.

Feature component:

- **không** tự quản lý server data bằng `useState` — phải qua TanStack Query hook;
- **không** gọi Google Maps/Places SDK trực tiếp — phải qua `features/map`.

---

# 3. STATE MANAGEMENT — 2 TẦNG BẮT BUỘC

Đây là quy tắc quan trọng nhất và **không được vi phạm**.

## 3.1. Server data → TanStack Query

Mọi dữ liệu đến từ backend (Trip, Place, Itinerary, Memory, Journey...) **phải** đi qua TanStack Query (`useQuery`/`useMutation`).

Không nhét API data vào Zustand store. Không cache thủ công bằng `useState` + `useEffect` cho dữ liệu server.

Quy ước:

```text
features/<domain>/api/
  ├── useTripQuery.ts
  ├── useTripPlacesQuery.ts
  └── useCreateTripMutation.ts
```

Query key phải có cấu trúc nhất quán, ví dụ: `['trip', tripId]`, `['trip', tripId, 'places']`, `['trip', tripId, 'itinerary']`, `['journey']`.

## 3.2. UI state → Zustand

Chỉ dùng Zustand cho state **thuộc về giao diện**, không phải dữ liệu nghiệp vụ:

```text
selectedPlaceId
hoveredPlaceId
activeDayId
sidebarOpen / sidebarCollapsed
mobileView
modal đang mở
filter / view mode (grid | timeline)
```

Mỗi feature có store riêng (ví dụ `useItineraryUIStore`, `useMemoryUIStore`), không gộp toàn bộ UI state vào một store khổng lồ toàn cục trừ khi state đó thực sự global (ví dụ theme, auth session flag).

## 3.3. Form → React Hook Form + Zod

Không tự quản lý form bằng 10–20 `useState`. Mọi form (Login, Create Trip wizard, Memory entry, Account settings...) dùng React Hook Form + Zod schema đặt tại `features/<domain>/schema/`.

## 3.4. Đồng bộ hai chiều Itinerary ↔ Map

`hoveredPlaceId`/`selectedPlaceId` là state UI dùng chung giữa Place Card và Map Marker, đặt trong Zustand store của feature `itinerary`. Luồng chuẩn:

```text
Place Card hover  → set hoveredPlaceId → Map Marker highlight
Map Marker click  → set selectedPlaceId → Place Card scroll-into-view
```

Không implement đồng bộ này bằng cách truyền callback qua nhiều tầng props (prop drilling) — dùng store chung.

---

# 4. TRIP LIFECYCLE & DOMAIN MODEL

## 4.1. Trip Status (đã chốt tại Phase 4B)

```text
PLANNING → READY → ACTIVE → COMPLETED
```

| Status | Ý nghĩa |
|---|---|
| `PLANNING` | Đang tạo/lên kế hoạch, chưa hoàn thiện |
| `READY` | Đã có itinerary, sẵn sàng khởi hành |
| `ACTIVE` | Đang diễn ra |
| `COMPLETED` | Đã kết thúc |

> **Quan trọng:** AGENTS.md cũ dùng `Draft/Upcoming/Past/Archived` — **sai với spec**. Phải dùng đúng 4 status trên.
>
> Status label trong UI phải hiển thị bằng chữ kèm màu sắc, không dùng màu là cách duy nhất truyền đạt trạng thái.

## 4.2. Domain Model chính (từ Phase 7)

```text
User
 └── Trip
       ├── TripPlace ─── Place ─── Province
       │
       ├── Itinerary
       │      └── ItineraryItem ─── Place
       │
       └── Memory ─── Media[]
```

Entity key & field quan trọng:

**Trip:**
```text
id, userId, name, destination, startDate, endDate,
transportation, preferences, budget, constraints,
status (PLANNING|READY|ACTIVE|COMPLETED), createdAt, updatedAt
```

**Place:**
```text
id, providerPlaceId, name, address, latitude, longitude,
provinceId, createdAt, updatedAt
```

**TripPlace** (quan hệ Trip ↔ Place):
```text
id, tripId, placeId, priority, estimatedDuration, notes, createdAt
```

**Itinerary / ItineraryItem:**
```text
Itinerary: id, tripId, name, status, createdAt, updatedAt
ItineraryItem: id, itineraryId, placeId, dayIndex, order,
               suggestedStartTime, suggestedDurationMinutes,
               distanceFromPreviousKm, durationFromPreviousMinutes, notes
```

**Memory:**
```text
id, userId, tripId, placeId (optional), note, createdAt
```

**Media (thuộc Memory):**
```text
id, memoryId, storageKey, url, width, height, fileSize, mimeType, sortOrder, createdAt
```

**Province** (Master data — 63 tỉnh/thành):
```text
id, code, name
```

> Không tự thêm field ngoài spec đã có mà không kiểm tra backend contract trước.

## 4.3. Business Rules bắt buộc (từ Phase 4B & 5B)

| Rule | Nội dung |
|---|---|
| **BR-01** | Mọi Trip/Memory thuộc về một User cụ thể |
| **BR-02** | User chỉ thao tác được Trip/Memory của chính mình |
| **BR-03** | AI Recommendation là đề xuất, không phải mệnh lệnh |
| **BR-04** | User luôn có quyền Apply / Edit / Ignore recommendation |
| **BR-05** | Weather **không** tự động thay đổi itinerary |
| **BR-06** | Place trong Itinerary ≠ đã đến (`PLANNED ≠ VISITED`) |
| **BR-07** | Xóa Memory không ảnh hưởng Trip hoặc Place |
| **BR-08** | Tỉnh/thành được tính Explored khi: Memory → Place → Province |
| **BR-09** | GPS/Check-in không bắt buộc để tạo Memory |
| **BR-10** | External API lỗi không được làm hỏng Trip core |
| **BR-11** | Journey progress đếm unique region, không đếm số lần ghé |
| **BR-12** | AI không được tự thay đổi Preference của User |

---

# 5. TRIP WORKSPACE

Trip Workspace (`/trips/[tripId]/*`) là nơi User quản lý và theo dõi chuyến đi.

## 5.1. Cấu trúc Trip Workspace (đã chốt Phase 4B)

Tab/section trong Trip detail:

```text
Overview      — Tóm tắt: tên, ngày, số địa điểm, số Memory, trạng thái
Itinerary     — Lịch trình theo ngày
Map           — Bản đồ địa điểm và route
Memories      — Ảnh và ghi chú
```

> **Không có** Budget/Checklist/Files/Reservations/AI Chat trong Trip Workspace của MVP. Đây là quyết định đã chốt theo MVP boundary. Nếu task yêu cầu thêm tab mới, phải nêu rõ với người dùng trước khi implement.

Tất cả các route dưới `/trips/[tripId]/*` dùng chung:

- `TripLayout` (Sidebar | Content | Map trên desktop; Drawer trên mobile/tablet);
- `TripHeader` (tên trip, ngày, trạng thái, settings);
- data layer của Trip hiện tại (không fetch lại Trip info riêng lẻ ở từng sub-page nếu đã có trong cache).

Không tạo một layout/header riêng cho từng sub-page của Trip Workspace.

## 5.2. Sidebar

Danh sách mục: `Overview · Itinerary · Map · Memories · Settings`.

- Desktop: expand/collapse/resize bằng `react-resizable-panels`.
- Tablet/Mobile: chuyển thành Drawer.
- Không viết logic responsive riêng biệt cho từng breakpoint bằng cách tạo 2 component khác nhau — dùng cùng component, điều khiển bằng CSS/props.

## 5.3. Itinerary

### Cấu trúc hiển thị

```text
Day Container (ngày, date, số địa điểm)
  └── Place Card (ảnh, tên, category, địa chỉ, giờ gợi ý, thời lượng, ghi chú)
       ├── Travel Info (khoảng cách, thời gian di chuyển đến Place tiếp theo)
       └── Check-in / Mark Visited button
```

### Planning Pipeline (2 lớp — từ Phase 5A & Phase 6)

```text
User input (places, dates, preferences, transportation)
    ↓
Backend: Held–Karp DP Algorithm
    (tìm thứ tự địa điểm tối ưu định lượng dựa trên Distance Matrix)
    ↓
Backend: AI Contextual Layer
    (cá nhân hóa, phân bổ thời gian, giải thích, xét Weather/Opening Hours)
    ↓
Structured Recommendation (schema đã chốt Phase 4C)
    ↓
Frontend: Preview → User Apply / Edit / Ignore
    ↓
Final Itinerary lưu vào DB
```

> **Quan trọng:** Hai lớp này có bản chất khác nhau:
> - **Held–Karp** = tối ưu định lượng (distance/time) — kết quả deterministic
> - **AI** = cá nhân hóa, giải thích, đề xuất điều chỉnh — kết quả có thể khác nhau cho cùng input
>
> UI phải phân biệt rõ hai loại kết quả:
> - `✨ AI đề xuất` (có context cá nhân hóa, giải thích)
> - `🧭 Lịch trình cơ bản` (fallback thuần thuật toán nếu AI thất bại)
>
> **Không gộp chung vào một nút "Optimize" mơ hồ.**

### AI trong Itinerary

AI trong MVP **là recommendation engine**, không phải chat assistant:

- AI tạo itinerary suggestion theo structured output;
- AI không streaming, không chat;
- User xem Preview → Apply / Edit / Ignore;
- AI lỗi/timeout → fallback về rule-based (Held–Karp + phân bổ đơn giản);
- AI lỗi **không làm crash Trip**.

```text
AI Loading UX (Phase 4C spec):
"✨ Đang phân tích các địa điểm..."
"🗺️ Đang tối ưu lịch trình..."
"✨ Đang hoàn thiện đề xuất..."
```

Không dùng spinner chung chung cho quá trình AI generate.

### Drag & Drop

Dùng `@dnd-kit/core` và `@dnd-kit/sortable`; dùng `DragOverlay` cho ghost card. Không tự viết HTML5 drag-drop native.

Phạm vi kéo-thả tối thiểu: reorder trong cùng ngày, di chuyển giữa các ngày.

### Travel Info (khoảng cách giữa 2 Place)

Dữ liệu khoảng cách/thời gian đến từ backend (đã gọi Map/Routing API khi planning). Frontend chỉ hiển thị, không tự tính.

Nếu dữ liệu chưa có từ backend, đánh dấu rõ trong code:

```ts
// TODO: [PHASE-2] Travel info pending Routing API integration from backend
```

## 5.4. Map

Map là **component riêng** (`features/map`), không rải logic Google Maps trong itinerary component. Map nhận props/state đã chuẩn hoá (danh sách marker có `placeId`, `dayIndex`, `order`) từ store chung, không tự fetch dữ liệu Trip riêng.

Marker phải hỗ trợ:
- Numbered marker theo thứ tự itinerary;
- Hover scale (liên kết với `hoveredPlaceId` trong `useItineraryUIStore`);
- Click → set `selectedPlaceId`;
- Fit bounds theo ngày đang xem.

## 5.5. Weather Context

Weather là **soft context** — hiển thị thông tin để User tham khảo, không tự động thay đổi itinerary.

```text
Weather hiển thị (ví dụ):
"🌧️ Khả năng mưa cao vào buổi chiều — bạn có thể cân nhắc hoạt động ngoài trời vào buổi sáng."
```

Nếu Weather API lỗi: ẩn section Weather, không crash Trip.

## 5.6. Check-in / Mark Visited

Nút "Mark as Visited" hoặc "Check-in" trên Place Card.

- Không bắt buộc GPS;
- Không bắt buộc có Memory trước;
- Check-in xác nhận User **thực sự đã đến** Place đó.

Chỉ khi Place được visited + có Memory mới tính Province là Explored (xem BR-08).

---

# 6. MEMORY & JOURNAL

Memory là module ghi lại trải nghiệm cá nhân của User. Route: `/trips/[tripId]/memories`, dùng chung `TripLayout`/`TripHeader`.

## 6.1. Data Model Memory (từ Phase 4D & Phase 7)

```text
Memory
├── id
├── userId
├── tripId
├── placeId (optional — ảnh trên đường đi không cần Place)
├── note (optional)
└── createdAt

Media[] (thuộc Memory)
├── id, memoryId
├── storageKey / publicId (lưu ở Media Storage, không phải DB binary)
├── url
├── width, height, fileSize, mimeType
├── sortOrder
└── createdAt
```

> Memory **không có** field `type: "photo"|"video"|"journal"` như AGENTS.md cũ mô tả — đây là sai so với spec Phase 4D và Phase 7. Memory MVP là: ảnh + optional note + optional Place.
>
> Journal không phải entity riêng — Journal là **view** tổng hợp Memory theo timeline (`sort by createdAt`).

## 6.2. Tạo Memory — Flow

```text
User mở Place → Add Memory
    ↓
Select multiple photos (React Dropzone)
    ↓
Preview + Optional note
    ↓
Save → POST /api/v1/trips/{tripId}/memories
        (backend upload ảnh lên Media Storage, lưu metadata)
```

Không yêu cầu User nhập form dài (title, hashtag, mood, v.v.).

## 6.3. Các view Memory

- **Gallery** (Grid layout mặc định): danh sách Memory của Trip.
- **Timeline (Journal)**: Memory sort theo `createdAt`, grouping theo ngày — đây là view "Journal".
- **Lightbox**: xem ảnh full-size, navigate prev/next, xem note, navigate đến Place.

Không tạo page riêng cho Lightbox — mở inline/modal.

## 6.4. Upload & Media

Upload flow: `Frontend → POST /api/v1/trips/{tripId}/memories → Backend → Media Storage → CDN URL`.

- Dùng React Dropzone, hỗ trợ multiple, preview, progress;
- Không lưu binary ảnh trong DB;
- Ảnh hiển thị qua `next/image` với CDN URL, lazy load ngoài viewport.

## 6.5. Liên kết Memory ↔ Place

Từ Place (trong Itinerary hoặc Map): có thể xem danh sách Memory gắn với `placeId` đó.

Xóa Memory không xóa Place hoặc Visited status.

## 6.6. Phase phân loại tính năng Memory

| Tính năng | Phase |
|---|---|
| Photo + note + Place | MVP (hiện tại) |
| Timeline / Journal view | MVP |
| Lightbox | MVP |
| Video | Sau MVP |
| Tag người trong ảnh | Phase 3 (sau) |
| AI Travel Story | Phase 4 (sau) |

Không tự ý triển khai sớm các phase sau nếu task hiện tại chỉ yêu cầu phase cơ bản.

---

# 7. JOURNEY PROGRESS

Journey là feature hiển thị User đã khám phá bao nhiêu tỉnh/thành Việt Nam.

Route: `/journey`, không nằm trong Trip Workspace.

## 7.1. Quy tắc Explored (từ Phase 5A FR-24)

Một tỉnh/thành được tính là **Explored** khi:

```text
Memory (tồn tại)
  ↓
Place (gắn với Memory)
  ↓
Province (tỉnh/thành của Place)
  ↓
→ Tỉnh/thành đó = Explored
```

**Không tính** nếu Place chỉ nằm trong Itinerary nhưng chưa có Memory.

**Không bắt buộc** GPS hoặc Check-in.

Đi nhiều lần Đà Nẵng → `Đà Nẵng = 1 explored region` (unique count).

## 7.2. Thống kê hiển thị

```text
🇻🇳 Hành trình Việt Nam

25 / 63
Tỉnh/thành đã khám phá

[ Vietnam Map visualization ]

Tổng chuyến đi: 8
Tổng địa điểm:  34
Tổng Memory:    124
Tổng ảnh:       327
```

Chart dùng Recharts nếu cần visualization.

## 7.3. Bản đồ Việt Nam

Hiển thị 63 tỉnh/thành với trạng thái `Explored / Chưa khám phá`.

Dataset province (Master data) do backend cung cấp qua API, không hard-code ở frontend.

Click vào tỉnh/thành → danh sách Trip/Memory tại đó.

---

# 8. AUTHENTICATION & AUTHORIZATION

## 8.1. Auth flow

```text
Register / Login → Backend → JWT → Client stores token → Authorization: Bearer <JWT>
```

Backend là stateless; JWT dùng cho mọi request authenticated.

Token lưu: theo quyết định của dự án (memory + refresh, hoặc httpOnly cookie). **Không tự ý chọn cách lưu mới** mà không kiểm tra spec. Mặc định assumption: Bearer token từ auth store Zustand, refresh flow để sau khi xác nhận với backend.

## 8.2. Route protection

Deny by default:

- Public routes: `/`, `/login`, `/register`
- Protected routes: `/trips/*`, `/journey`, `/account`

## 8.3. Form

Login/Register dùng React Hook Form + Zod. Hiển thị lỗi rõ ràng (`"Email không hợp lệ"`, `"Mật khẩu tối thiểu 8 ký tự"`...).

Sau login thành công → redirect `/trips`.

---

# 9. DATA FETCHING & API LAYER

## 9.1. Server Components vs Client Components

Ưu tiên Next.js Server Component cho nội dung tĩnh/SEO: Landing page, trang Explore (nếu có). Các trang tương tác cao (Trips, Trip Workspace, Journey) dùng Client Component + TanStack Query + Zustand.

## 9.2. API client (`lib/api.ts`)

Tất cả gọi API đi qua một lớp client thống nhất (axios instance). Không rải `fetch()` trực tiếp trong từng component. Lớp này chịu trách nhiệm:

- Base URL từ env (`NEXT_PUBLIC_API_BASE_URL`);
- Request interceptor: gắn Bearer token;
- Response interceptor: xử lý lỗi theo error contract backend.

## 9.3. Loading / Error / Empty state

Mọi khu vực dùng TanStack Query phải xử lý đủ 3 trạng thái:

- **Loading**: skeleton (không dùng spinner toàn trang cho khu vực nhỏ);
- **Error**: thông báo rõ ràng bằng ngôn ngữ người dùng + nút Retry (ví dụ: "Không thể tải dữ liệu. Thử lại.");
- **Empty**: empty state có hướng dẫn hành động (ví dụ: "Chưa có Memory nào — Thêm Memory").

Không hiển thị stack trace hoặc error kỹ thuật cho người dùng.

---

# 10. RESPONSIVE — MỘT COMPONENT, KHÔNG HAI APP

Không xây Desktop và Mobile thành hai bộ component tách biệt. Dùng cùng component, điều khiển hiển thị bằng Tailwind responsive classes / CSS.

Quy ước layout:

```text
Desktop:  Sidebar | Content | Map      (3 cột, resizable)
Tablet:   Sidebar → Drawer, Content | Map
Mobile:   Sidebar → Drawer, Content full-width, Map → tab/toggle riêng
```

Header marketing: desktop hiển thị đầy đủ menu, mobile thu gọn vào hamburger (`☰`). Không tạo `HeaderMobile`/`HeaderDesktop` là hai component riêng nếu không thật sự cần thiết.

---

# 11. ACCESSIBILITY & UX

- Component tương tác (Drag card, Marker, Modal, Drawer) phải có keyboard/focus handling tối thiểu qua shadcn/ui/Radix primitives, không tự viết modal/dropdown từ đầu nếu shadcn/ui đã có.
- Ảnh dùng `next/image` với `alt` mô tả, không để `alt=""` cho ảnh có ý nghĩa nội dung.
- Không dùng màu sắc là cách duy nhất truyền đạt trạng thái — Trip status `PLANNING/READY/ACTIVE/COMPLETED` phải có label chữ kèm màu.
- UX-01: User phải tạo được Trip nhanh (tối thiểu: tên + destination + dates).
- UX-02: Progressive disclosure — không hỏi thông tin chưa cần (budget, constraints, v.v. là optional).
- UX-07: Memory phải dễ lưu, dễ xem lại — không yêu cầu form dài.

---

# 12. PERFORMANCE

Không tối ưu trước khi có evidence. Khi có vấn đề: đo → xác định bottleneck → tối ưu → đo lại.

Lưu ý riêng:

- Itinerary/Map re-render: dùng selector Zustand đúng phạm vi, tránh subscribe toàn bộ store khi `hoveredPlaceId` đổi.
- Ảnh Memory Gallery: `next/image` với kích thước phù hợp, lazy load ngoài viewport.
- Danh sách dài (Gallery, Journey places list): cân nhắc pagination thay vì render toàn bộ DOM một lúc.
- Không giải quyết performance bằng cách tắt Suspense/loading state.

---

# 13. TESTING

## 13.1. Phạm vi ưu tiên

```text
Hook logic (custom hooks trong features/*/api, hooks/)
    >
Component logic quan trọng (form validation, drag reorder)
    >
UI rendering đơn thuần
```

## 13.2. Công cụ

- Unit/component test: React Testing Library + Vitest/Jest (theo config hiện có trong project).
- Không test bằng cách mock toàn bộ component cần kiểm chứng.
- Không gọi API thật trong test — mock qua MSW hoặc mock module của `lib/api`.

## 13.3. Coverage

Ưu tiên **meaningful tests** hơn coverage percentage. Không viết test vô nghĩa để đạt số coverage.

---

# 14. GIT WORKFLOW

Commit message theo Conventional Commits:

```text
feat: fix: refactor: test: docs: chore: perf: build: style:
```

Ví dụ:

```text
feat: add itinerary drag and drop between days
fix: sync hoveredPlaceId between place card and map marker
refactor: extract memory gallery view switch into shared hook
test: add zod schema tests for create trip wizard
docs: update AGENTS.md with Phase 7 domain model
```

## 14.1. Không commit

```text
.env / .env.local
.next/
node_modules/
*.log
```

## 14.2. Agent không tự ý destructive Git command

Không tự chạy `git reset --hard`, `git push --force`, `git clean -fd` nếu chưa có sự cho phép rõ ràng.

## 14.3. Không sửa hàng loạt

Phát hiện technical debt → báo cáo → đề xuất → chỉ refactor khi task cho phép.

---

# 15. QUY TRÌNH LÀM VIỆC CHUẨN

```text
1. Đọc AGENTS.md (một lần/phiên, lưu vào context, không đọc lại)
        ↓
2. Hiểu yêu cầu (route/feature nào bị ảnh hưởng)
        ↓
3. Kiểm tra code/spec hiện có (component, hook, store tương tự)
        ↓
4. Xác định tầng bị ảnh hưởng (UI / state / API / interaction)
        ↓
5. Nêu assumption nếu thiếu contract từ backend
        ↓
6. Lập implementation plan ngắn
        ↓
7. Code
        ↓
8. Lint / type-check / build
        ↓
9. Test (nếu có logic cần test)
        ↓
10. Review diff
        ↓
11. Kiểm tra responsive + accessibility cơ bản
        ↓
12. Tóm tắt kết quả
```

## 15.1. Token Efficiency

Agent **không được**:

- đọc lại `AGENTS.md` nhiều lần trong cùng phiên;
- đọc toàn bộ file lớn khi chỉ cần một phần cụ thể;
- lặp lại nguyên văn nội dung lớn từ file/codebase trong câu trả lời;
- tạo file scaffold/demo không cần thiết rồi bỏ đó;
- gọi tool tìm kiếm/đọc file cho thông tin đã có trong context hiện tại.

Agent **nên**: dùng `grep`/`search` định vị trước khi đọc toàn file; chỉ đọc file thực sự liên quan; đọc theo range khi chỉ cần một phần; tận dụng context đã có.

---

# 16. DEFINITION OF DONE

Một task chỉ được báo **completed** khi:

### Code

- [ ] Implementation hoàn chỉnh, không còn `TODO` cho phần bắt buộc
- [ ] Không duplicate logic rõ ràng giữa các feature
- [ ] Đúng vị trí theo `features/<domain>` hoặc `app/` route
- [ ] Đúng naming convention (PascalCase component, camelCase hook/store)

### Kiến trúc

- [ ] Server data đi qua TanStack Query, không lẫn vào Zustand
- [ ] UI state đi qua Zustand, không lẫn vào server cache
- [ ] Form dùng React Hook Form + Zod
- [ ] Drag/drop dùng dnd-kit, không tự viết HTML5 DnD
- [ ] Map logic nằm trong `features/map`, không rải trong itinerary
- [ ] Trip Workspace dùng chung `TripLayout`/`TripHeader`

### Domain

- [ ] Trip status dùng đúng: `PLANNING | READY | ACTIVE | COMPLETED`
- [ ] Memory schema đúng với Phase 7 (không có `type` field tùy tiện)
- [ ] Explored province đúng rule: Memory → Place → Province

### UI/UX

- [ ] Responsive đúng ba mốc: desktop / tablet / mobile, cùng một component
- [ ] Có xử lý loading / error / empty state
- [ ] Accessibility cơ bản (alt text, focus, label trạng thái)
- [ ] AI lỗi/timeout không làm crash phần còn lại của Trip Workspace

### Testing

- [ ] Test cho logic quan trọng (validation, reorder, derive state)
- [ ] Không gọi API thật trong test
- [ ] Test thực sự đã chạy

### Verification

- [ ] Lint/type-check/build thành công
- [ ] Test pass
- [ ] Git diff đã được review

---

# 17. BÁO CÁO SAU MỖI TASK

```text
## Summary

### Changed
- ...

### Why
- ...

### Files
- `...`
- `...`

### State / Data Impact
- Server state (TanStack Query): ...
- UI state (Zustand): ...

### UI/UX
- Responsive: ...
- Loading/Error/Empty: ...

### Tests
- `...` → PASS

### Remaining
- ...
```

Nếu chưa chạy được test/build, phải nói thẳng:

```text
Verification: NOT RUN
Reason: ...
```

Không được nói "đã hoàn thành" nếu chưa thực sự kiểm chứng phần bắt buộc.

---

# 18. QUY TẮC KHI CÓ MÂU THUẪN

Nếu agent phát hiện mâu thuẫn giữa code hiện tại và tài liệu spec, **không được tự chọn ngẫu nhiên**. Phải:

1. xác định nguồn nào mới hơn/đã được chốt gần nhất;
2. kiểm tra task hiện tại có yêu cầu rõ không;
3. nêu conflict rõ ràng cho người dùng;
4. đề xuất hướng xử lý, chỉ thực hiện khi được xác nhận.

Thứ tự độ tin cậy của nguồn: `docs/phase-8/` > `docs/phase-7/` > `docs/phase-6/` > `docs/phase-4/` > `docs/phase-3/` > `AGENTS.md` > code hiện tại.

---

# 19. NGUYÊN TẮC CUỐI CÙNG

> **AI Agent là người hỗ trợ triển khai frontend, không phải người tự quyết định kiến trúc, UX flow hay business rule của dự án.**

Agent phải:

```text
Understand → Inspect → Plan → Implement → Test → Verify → Explain
```

Không:

```text
Guess → Code → Assume it works
```

Mục tiêu cuối cùng là tạo ra frontend:

- phục vụ đúng vòng đời chuyến đi cá nhân: **Plan → Explore → Capture → Remember**;
- nhất quán 2 tầng state (server/UI) trong toàn bộ Trip Workspace;
- responsive bằng một bộ component duy nhất;
- an toàn (auth, validation, không tự apply AI action mà chưa xác nhận);
- dễ mở rộng theo `features/<domain>`;
- đúng với MVP boundary đã chốt trong docs — không tự ý mở rộng sang Group/Social/Booking.
