# PHASE 8 — API & BACKEND SERVICE DESIGN

> Dự án: Hệ thống hỗ trợ lập kế hoạch và quản lý hành trình du lịch cá nhân tích hợp AI  
> Backend: Java Spring Boot  
> Database: PostgreSQL  
> API: RESTful API  
> Authentication: JWT  
> Algorithm: Held-Karp  
> External Services: Places API, Map/Route API, Weather API, AI API

---

## 1. Mục tiêu Phase 8

Phase 8 xác định toàn bộ thiết kế API và backend service trước khi triển khai code.

Mục tiêu:

- Chuẩn hóa REST API.
- Xác định trách nhiệm của từng API/module.
- Thiết kế authentication và authorization.
- Thiết kế API cho User, Trip, Place, Planning, AI, Memory, Journal và Journey.
- Xác định cách tích hợp Held-Karp với AI.
- Chuẩn hóa validation và error handling.
- Xây dựng chiến lược documentation và testing.
- Đảm bảo backend có thể mở rộng mà không làm Controller trở nên phức tạp.

Nguyên tắc:

> API là lớp giao tiếp với client; business logic nằm trong Service/Domain layer; dữ liệu được truy cập qua Repository; dịch vụ bên ngoài được truy cập qua Integration/Adapter layer.

---

# 8A — API Design Principles

## 8A.1 RESTful API

Base URL:

```text
/api/v1
```

Ví dụ:

```text
GET    /api/v1/trips
POST   /api/v1/trips
GET    /api/v1/trips/{tripId}
PUT    /api/v1/trips/{tripId}
DELETE /api/v1/trips/{tripId}
```

## 8A.2 API Versioning

Sử dụng:

```text
/api/v1/...
```

Khi có breaking change lớn có thể phát triển `/api/v2/...`.

## 8A.3 DTO Boundary

Không expose trực tiếp JPA Entity ra API.

```text
HTTP Request
    ↓
Request DTO
    ↓
Controller
    ↓
Service
    ↓
Entity
    ↓
Repository
    ↓
Database
```

Response:

```text
Database
    ↓
Entity
    ↓
Service
    ↓
Response DTO
    ↓
JSON
```

## 8A.4 HTTP Status Codes

| Status | Ý nghĩa |
|---|---|
| 200 | Thành công |
| 201 | Tạo resource thành công |
| 204 | Thành công, không có body |
| 400 | Request không hợp lệ |
| 401 | Chưa xác thực |
| 403 | Không có quyền |
| 404 | Không tìm thấy resource |
| 409 | Conflict |
| 422 | Business validation thất bại |
| 429 | Rate limit |
| 500 | Internal Server Error |
| 502 | External service lỗi |

## 8A.5 Error Response

```json
{
  "status": 400,
  "code": "VALIDATION_ERROR",
  "message": "Invalid request",
  "errors": []
}
```

---

# 8B — Authentication & User API

## 8B.1 Authentication

Sử dụng JWT.

```text
Register
   ↓
Login
   ↓
Validate credentials
   ↓
Generate JWT
   ↓
Client stores token
   ↓
Authorization: Bearer <JWT>
```

## 8B.2 Authentication API

```text
POST /api/v1/auth/register
POST /api/v1/auth/login
POST /api/v1/auth/refresh
POST /api/v1/auth/logout
```

## 8B.3 User API

```text
GET /api/v1/me
PUT /api/v1/me
PUT /api/v1/me/password
```

## 8B.4 Security Rule

User được xác định từ JWT.

Không tin `userId` do client tự truyền để xác định owner.

```text
JWT
 ↓
Authenticated User
 ↓
Resource Ownership
```

---

# 8C — Trip & Place API

## 8C.1 Trip API

```text
POST   /api/v1/trips
GET    /api/v1/trips
GET    /api/v1/trips/{tripId}
PUT    /api/v1/trips/{tripId}
DELETE /api/v1/trips/{tripId}
```

Trip gồm:

- Tên chuyến đi.
- Điểm đến.
- Ngày bắt đầu/kết thúc.
- Phương tiện.
- Sở thích.
- Ngân sách nếu sử dụng.
- Ràng buộc/yêu cầu.
- Danh sách địa điểm.

## 8C.2 Trip Place API

```text
POST   /api/v1/trips/{tripId}/places
GET    /api/v1/trips/{tripId}/places
DELETE /api/v1/trips/{tripId}/places/{placeId}
```

## 8C.3 Place

Place có:

- Tên.
- Địa chỉ.
- Tỉnh/thành.
- Latitude.
- Longitude.
- Provider ID.
- Metadata cần thiết.

Place có thể đến từ:

```text
User Input
    +
Places API
```

---

# 8D — Planning & Route API

Planning API chịu trách nhiệm tạo và quản lý kế hoạch chuyến đi.

```text
POST /api/v1/trips/{tripId}/planning
GET  /api/v1/trips/{tripId}/itinerary
PUT  /api/v1/trips/{tripId}/itinerary
```

## 8D.1 Planning Flow

```text
Trip
 ↓
Places
 ↓
Map/Route API
 ↓
Distance Matrix
 ↓
Held-Karp
 ↓
Optimal Route
 ↓
Context
 ↓
AI Recommendation
 ↓
Itinerary
```

## 8D.2 Held-Karp

Held-Karp là thuật toán lõi do hệ thống tự triển khai.

Input:

```text
Places
Coordinates
Distance Matrix / Travel Time
```

Output:

```text
Optimal order
Total distance
Total travel cost/time
```

Mục tiêu:

> Tìm thứ tự đi qua các địa điểm với tổng chi phí định lượng tối ưu theo mô hình bài toán đã xác định.

## 8D.3 Route Reconstruction

Hệ thống phải reconstruct được route:

```text
A → C → B → D
```

để frontend có thể hiển thị.

---

# 8E — AI Recommendation API

AI không thay thế thuật toán tối ưu.

```text
             PLANNING ENGINE
                    │
          ┌─────────┴─────────┐
          ▼                   ▼
      Held-Karp               AI
     Quantitative          Contextual
          │                   │
          └─────────┬─────────┘
                    ▼
             Recommendation
```

## 8E.1 AI Input

```text
destination
travelDates
places
transportation
preferences
constraints
routeInformation
weatherInformation
openingHours
priority
```

## 8E.2 AI Output

```text
Suggested order
Time slots
Reasons
Weather notes
Activity suggestions
Warnings
```

## 8E.3 User Decision

AI Recommendation không phải System Command.

```text
AI Recommendation
       ↓
┌──────┼───────┐
▼      ▼       ▼
Apply  Edit   Ignore
```

Người dùng luôn có quyền quyết định cuối cùng.

## 8E.4 AI Output Validation

Backend phải kiểm tra:

- Place ID thuộc tập input.
- Không duplicate place.
- Không thiếu place nếu yêu cầu full itinerary.
- Time slot hợp lệ.
- JSON đúng schema.
- Không vi phạm business constraint quan trọng.

---

# 8F — Memory & Journal API

## 8F.1 Memory API

```text
POST   /api/v1/memories
GET    /api/v1/memories/{memoryId}
PUT    /api/v1/memories/{memoryId}
DELETE /api/v1/memories/{memoryId}
```

Memory gồm:

- User.
- Trip.
- Place.
- Ảnh.
- Ghi chú.
- Thời gian tạo.

## 8F.2 Upload Photo

Ảnh không lưu trực tiếp trong PostgreSQL.

```text
Client
 ↓
Backend
 ↓
Media Storage
 ↓
Image URL / Metadata
 ↓
Memory
```

## 8F.3 Journal API

```text
GET /api/v1/trips/{tripId}/journal
```

Journal:

```text
Trip
 ↓
Date
 ↓
Place
 ↓
Memory
 ↓
Photos + Notes
```

---

# 8G — Journey & Statistics API

## 8G.1 My Journey

```text
GET /api/v1/me/journey
```

Có thể trả về:

- Số chuyến đi.
- Số địa điểm.
- Số Memory.
- Số ảnh.
- Số tỉnh/thành đã khám phá.

## 8G.2 My Places

```text
GET /api/v1/me/places
```

Danh sách Place từng có Memory.

Số lượng Memory được tổng hợp từ Memory, không cần bảng MyPlaces riêng.

## 8G.3 Province Exploration

```text
GET /api/v1/me/journey/provinces
```

Quy tắc:

```text
Memory
 ↓
Place
 ↓
Province
 ↓
Explored
```

Chỉ có itinerary nhưng chưa có Memory:

```text
Itinerary
 ↓
No Memory
 ↓
Not Explored
```

Không dùng GPS/check-in bắt buộc để xác định tỉnh/thành đã khám phá.

---

# 8H — Security, Validation & Error Handling

## 8H.1 Authentication

Các API protected yêu cầu:

```http
Authorization: Bearer <JWT>
```

Kiểm tra:

- Missing token.
- Invalid token.
- Expired token.
- Malformed token.

## 8H.2 Authorization

Ví dụ:

```text
User A
 └── Trip A

User B
 └── Trip B
```

User A không được truy cập Trip B.

Tương tự với Memory, Photo và các resource thuộc user.

## 8H.3 Validation

Các rule quan trọng:

```text
Trip name không rỗng
Start date <= End date
Transportation hợp lệ
Place tồn tại
Memory thuộc user
Trip thuộc user
```

## 8H.4 Global Exception Handling

```text
Controller
    ↓
Exception
    ↓
Global Exception Handler
    ↓
Standard Error Response
```

Không để mỗi Controller xử lý lỗi theo cách khác nhau.

## 8H.5 External Service Failure

Map, Weather hoặc AI API có thể timeout, rate-limit hoặc trả dữ liệu lỗi.

Hệ thống phải graceful degradation.

Ví dụ:

```text
Weather API timeout
       ↓
Planning vẫn hoạt động
       ↓
weather = unavailable
```

AI không khả dụng:

```text
AI unavailable
       ↓
Held-Karp vẫn trả route
```

---

# 8I — API Documentation & Testing Strategy

## 8I.1 API Documentation

Sử dụng:

```text
OpenAPI
Swagger UI
springdoc-openapi
```

Mỗi endpoint cần mô tả:

- Summary.
- Description.
- Authentication.
- Parameters.
- Request body.
- Success response.
- Error responses.

## 8I.2 API Grouping

```text
Authentication
User
Trip
Place
Planning
AI Recommendation
Memory
Journal
Journey
Statistics
```

## 8I.3 Swagger JWT

```text
Authorize
 ↓
Bearer Token
 ↓
JWT
```

Swagger có thể dùng để test protected API trực tiếp.

---

# 9. Testing Strategy

Testing chia thành:

```text
Unit Test
Integration Test
Security Test
API Test
End-to-End Test
```

## 9.1 Unit Test

Các thành phần quan trọng:

```text
TripService
MemoryService
JourneyService
JwtService
PlanningService
HeldKarpService
AIResponseValidator
```

Công cụ:

```text
JUnit 5
Mockito
```

## 9.2 Held-Karp Testing

Test phải kiểm tra:

```text
- Route hợp lệ
- Đi qua đầy đủ địa điểm
- Không duplicate
- Không missing
- Total cost chính xác
- Route reconstruction chính xác
```

Complexity:

```text
O(n² × 2ⁿ)
```

Cần benchmark với nhiều số lượng địa điểm để xác định giới hạn phù hợp cho MVP.

## 9.3 AI Testing

Không yêu cầu AI luôn trả một route cố định.

Kiểm tra:

```text
Output đúng schema
Place ID hợp lệ
Không duplicate
Không tạo place không hợp lệ
Time slot hợp lệ
Reason tồn tại
```

## 9.4 Weather Testing

Mock các trường hợp:

```text
Weather available
Weather unavailable
Timeout
Invalid response
High rain probability
Low rain probability
```

## 9.5 Authentication Testing

```text
Login success
Wrong password
Unknown account
Missing JWT
Invalid JWT
Expired JWT
```

## 9.6 Authorization Testing

User A cố truy cập resource của User B phải bị từ chối.

```text
User A
   ↓
GET User B's Trip
   ↓
DENIED
```

## 9.7 Integration Testing

Luồng:

```text
Controller
   ↓
Service
   ↓
Repository
   ↓
PostgreSQL
```

Khuyến nghị:

```text
Spring Boot Test
Testcontainers
PostgreSQL
```

Nên dùng PostgreSQL Testcontainer thay vì chỉ dùng H2 để giảm khác biệt giữa test và production.

## 9.8 Repository Testing

Kiểm tra:

```text
Find user's trips
Find user's memories
Find places with memories
Count memories
Count photos
Count explored provinces
Journey statistics
```

Đặc biệt:

```text
JOIN
GROUP BY
COUNT
DISTINCT
```

## 9.9 Journey Statistics Testing

Ví dụ:

```text
Trip A
 ├── Sơn Trà
 └── Mỹ Khê

Trip B
 └── Sơn Trà
```

Nếu:

```text
Sơn Trà → 3 Memory
Mỹ Khê  → 2 Memory
```

Expected:

```text
Trip Count     = 2
Place Count    = 2
Memory Count   = 5
Province Count = 1
```

## 9.10 Province Exploration Testing

Có Memory:

```text
Memory
 ↓
Sơn Trà
 ↓
Đà Nẵng
 ↓
EXPLORED
```

Chỉ có itinerary:

```text
Itinerary
 ↓
Bà Nà Hills
 ↓
No Memory
 ↓
UNEXPLORED
```

## 9.11 End-to-End Business Flow

```text
Register
   ↓
Login
   ↓
JWT
   ↓
Create Trip
   ↓
Add Places
   ↓
Distance Matrix
   ↓
Held-Karp
   ↓
AI Recommendation
   ↓
Apply / Edit / Ignore
   ↓
Create Memory
   ↓
Journal
   ↓
My Journey
```

## 9.12 External API Mocking

Không để automated test phụ thuộc Internet.

```text
Real Map API
Real Weather API
Real AI API
```

được thay bằng:

```text
Mock / Stub
```

Lợi ích:

- Test ổn định.
- Không tốn quota.
- Không phụ thuộc Internet.
- Kiểm soát được dữ liệu.
- Mô phỏng được timeout/error.

---

# 10. Test Data

Dataset đề xuất:

```text
Test User
Test Trip
Test Places
Test Distance Matrix
Test Weather
Test Memories
Test Photos
```

Ví dụ:

```text
Trip: Đà Nẵng 3 ngày

Places:
1. Sơn Trà
2. Mỹ Khê
3. Ngũ Hành Sơn
4. Hội An
```

Có thể dùng dataset này cho:

```text
Unit Test
Integration Test
API Test
Demo
```

---

# 11. Test Naming Convention

Tên test thể hiện behavior:

```java
shouldCreateTripWhenRequestIsValid()

shouldRejectTripWhenEndDateBeforeStartDate()

shouldReturn401WhenJwtIsMissing()

shouldRejectAccessWhenTripBelongsToAnotherUser()

shouldReturnOptimalRouteForGivenDistanceMatrix()

shouldMarkProvinceAsExploredWhenMemoryExists()

shouldNotMarkProvinceAsExploredWhenOnlyItineraryExists()
```

---

# 12. Test Coverage

Không đặt mục tiêu 100% coverage một cách máy móc.

Ưu tiên coverage cao cho:

```text
Business Rules
Security
Held-Karp
Core Services
Repository Queries
AI Response Validation
Journey Statistics
Province Exploration
```

---

# 13. API Definition of Done

Một API hoàn thành khi:

```text
☑ Endpoint implemented
☑ Request DTO
☑ Response DTO
☑ Validation
☑ Authentication
☑ Authorization
☑ Business rule
☑ Error handling
☑ OpenAPI documentation
☑ Unit test
☑ Integration test nếu cần
```

Không xem chỉ việc API trả `200 OK` là đủ.

---

# 14. Critical Test Scenarios

10 scenario quan trọng:

### 1. Authentication

```text
Register → Login → JWT → Access API
```

### 2. Trip

```text
Create Trip → Add Places → Update Trip
```

### 3. Held-Karp

```text
Distance Matrix → Optimal Route
```

### 4. AI Recommendation

```text
Optimal Route + Context → AI Recommendation
```

### 5. User Choice

```text
Apply / Edit / Ignore
```

### 6. Weather

```text
Weather available / unavailable
```

### 7. Memory

```text
Place → Create Memory → Upload Photos
```

### 8. Journal

```text
Trip → Memories → Timeline
```

### 9. Exploration

```text
Memory → Place → Province → Explored
```

### 10. Security

```text
User A → Try access User B data → Denied
```

---

# 15. Backend API Architecture hoàn chỉnh

```text
                         FRONTEND
                            │
                            ▼
                     REST API / JSON
                            │
          ┌─────────────────┴─────────────────┐
          │                                   │
          ▼                                   ▼
   Spring Security                       Validation
          │                                   │
          └─────────────────┬─────────────────┘
                            ▼
                       CONTROLLERS
                            │
          ┌─────────────────┼──────────────────┐
          ▼                 ▼                  ▼
        AUTH             TRIP              PLANNING
                           │                  │
                           │          ┌───────┴────────┐
                           │          ▼                ▼
                           │     HELD-KARP            AI
                           │          │                │
                           │          └───────┬────────┘
                           │                  ▼
                           │           Recommendation
                           │
          ┌────────────────┼──────────────────────────┐
          ▼                ▼                          ▼
       MEMORY           JOURNAL                   JOURNEY
          │                │                          │
          ▼                ▼                          ▼
       PHOTOS          TIMELINE                 STATISTICS
          │                                           │
          └──────────────────┬────────────────────────┘
                             ▼
                       SERVICE LAYER
                             │
              ┌──────────────┼──────────────┐
              ▼              ▼              ▼
          PostgreSQL      Map API       Weather API
                                            │
                                            ▼
                                         AI API
```

---

# 16. Planning Architecture — Điểm nhấn học thuật

```text
                    PLANNING ENGINE
                           │
             ┌─────────────┴─────────────┐
             ▼                           ▼
       HELD-KARP                        AI
     Quantitative Layer           Context Layer
             │                           │
             │                           ├── Preferences
             │                           ├── Weather
             │                           ├── Opening Hours
             │                           ├── Priorities
             │                           └── User Constraints
             │
             ▼
      Optimal Route
             │
             └──────────────┬────────────┘
                            ▼
                     Recommendation
                            │
                     ┌──────┼──────┐
                     ▼      ▼      ▼
                   Apply   Edit   Ignore
```

Nguyên tắc:

> Held-Karp chịu trách nhiệm tối ưu hóa định lượng; AI chịu trách nhiệm phân tích ngữ cảnh, cá nhân hóa và giải thích đề xuất.

AI không phải nguồn quyết định tuyệt đối.

---

# 17. Definition of Done — Phase 8

```text
☑ REST API architecture
☑ API versioning
☑ Authentication API
☑ User API
☑ Trip API
☑ Place API
☑ Planning API
☑ Held-Karp integration
☑ AI Recommendation API
☑ Memory API
☑ Journal API
☑ Journey API
☑ Statistics API
☑ JWT Security
☑ Resource Ownership
☑ Validation
☑ Global Error Handling
☑ External API failure handling
☑ OpenAPI / Swagger
☑ Unit Testing Strategy
☑ Integration Testing Strategy
☑ Security Testing Strategy
☑ Held-Karp Testing
☑ AI Testing
☑ Weather Testing
☑ End-to-End Testing Strategy
```

---

# 18. Kết luận Phase 8

Sau Phase 8, backend không còn được mô tả đơn giản là:

```text
Frontend → Spring Boot → Database
```

mà được phân chia thành các lớp:

```text
Client
  ↓
REST API
  ↓
Security / Validation
  ↓
Controller
  ↓
Service / Business Logic
  ↓
Algorithm / AI / Integration
  ↓
Repository
  ↓
PostgreSQL
```

Trong đó:

- **Spring Boot** là nền tảng backend.
- **JWT + Spring Security** xử lý authentication/authorization.
- **JPA/Hibernate** xử lý persistence.
- **PostgreSQL** là database chính.
- **Held-Karp** là thuật toán tối ưu lộ trình do hệ thống tự triển khai.
- **AI API** xử lý recommendation, contextual analysis và personalization.
- **Map/Route API** cung cấp khoảng cách và thời gian di chuyển.
- **Weather API** cung cấp dữ liệu thời tiết tham khảo.
- **Memory/Journal/Journey** quản lý trải nghiệm.
- **OpenAPI/Swagger** document và kiểm thử API.
- **JUnit/Mockito/Testcontainers** hỗ trợ automated testing.

Điểm kiến trúc quan trọng:

```text
ALGORITHM
    ≠
AI
    ≠
USER DECISION
```

Held-Karp chịu trách nhiệm tối ưu hóa định lượng; AI xử lý ngữ cảnh và cá nhân hóa; người dùng luôn có quyền quyết định cuối cùng.

---

# 19. Chuẩn bị cho Phase tiếp theo

Sau Phase 8, nền tảng API và backend service đã được thiết kế. Phase tiếp theo tập trung vào **Core Business Logic & Algorithm Design**, đặc biệt:

```text
Trip Planning
     ↓
Distance Matrix
     ↓
Held-Karp
     ↓
Route Reconstruction
     ↓
Cost Function
     ↓
Time / Opening-hour Constraints
     ↓
Weather Context
     ↓
AI Recommendation
     ↓
Apply / Edit / Ignore
```

Đây là phần quan trọng để chứng minh tính đúng đắn của thuật toán và năng lực kỹ thuật của đồ án.
