# PHASE 6 — SYSTEM ARCHITECTURE

> **Trạng thái:** Đã hoàn thành (6A → 6I)  
> **Backend:** Java Spring Boot  
> **Kiến trúc MVP:** Modular Monolith  
> **Database:** PostgreSQL  
> **Cache:** Redis  
> **Media:** Object/Media Storage  
> **Planning:** Held–Karp + AI  
> **External Services:** Maps/Routing, Places, Weather, AI

---

## 1. Mục tiêu Phase 6

Phase 6 xác định kiến trúc tổng thể của hệ thống, từ nguyên tắc thiết kế, backend, planning/AI, dữ liệu, bảo mật, tích hợp dịch vụ bên ngoài, triển khai đến khả năng mở rộng.

Nguyên tắc xuyên suốt:

> **Core functionality should remain deterministic, persistent, and resilient; AI and external contextual services should enhance the experience rather than control the system.**

Điều này có nghĩa:
- Core data phải được lưu trữ độc lập và ổn định.
- Các logic cần tính chính xác, có thể kiểm chứng được xử lý bằng code/algorithm.
- AI đóng vai trò recommendation assistant.
- Weather là soft context.
- External APIs hỗ trợ hệ thống nhưng không được trở thành điểm phụ thuộc tuyệt đối.
- Người dùng luôn có quyền quyết định cuối cùng.

---

# 2. 6A — Architecture Principles & Design Philosophy

## 2.1. Modular trước, Microservices sau

MVP sử dụng **Modular Monolith** thay vì Microservices.

```text
Spring Boot Application
├── User
├── Trip
├── Place
├── Planning
├── AI
├── Map
├── Weather
├── Memory
├── Journal
└── Journey
```

Lợi ích:
- dễ phát triển;
- dễ debug;
- dễ test;
- triển khai đơn giản;
- giảm distributed-system complexity;
- vẫn có đường tiến hóa sang Microservices nếu cần.

## 2.2. Separation of Concerns

```text
Controller
   ↓
Application / Service
   ↓
Domain
   ↓
Repository
   ↓
Database
```

External provider được cô lập qua adapter/service:

```text
Application
   ↓
Provider Interface
   ↓
Provider Adapter
   ↓
External API
```

## 2.3. Core và Recommendation

### Core
- User
- Trip
- Place
- Itinerary
- Memory
- Journey data

### Recommendation / Context
- AI
- Weather
- External contextual information

AI không phải system command. Người dùng có thể:
- Áp dụng;
- Chỉnh sửa;
- Bỏ qua.

## 2.4. Algorithm + AI

```text
User Input
   ↓
Deterministic Planning
   ↓
Held–Karp
   ↓
Optimal Route
   ↓
AI Context & Personalization
   ↓
Recommendation
   ↓
User Decision
```

Held–Karp chịu trách nhiệm tối ưu định lượng; AI chịu trách nhiệm cá nhân hóa, giải thích và recommendation.

---

# 3. 6B — High-Level System Architecture

```text
                    USER
                      │
                      ▼
               Frontend / Mobile
                      │
                      ▼
              REST API / Backend
                      │
       ┌──────────────┼──────────────┐
       ▼              ▼              ▼
   PostgreSQL       Redis       Media Storage
                      │
                      │
                Planning Engine
                      │
             ┌────────┼────────┐
             ▼        ▼        ▼
         Held–Karp    AI    External APIs
                           ├── Maps
                           ├── Places
                           └── Weather
```

Backend là trung tâm điều phối nghiệp vụ.

PostgreSQL lưu dữ liệu nghiệp vụ quan trọng. Redis dùng cho cache/dữ liệu tạm thời phù hợp. Media Storage lưu ảnh. External APIs cung cấp dữ liệu bản đồ, địa điểm, thời tiết và AI.

---

# 4. 6C — Backend Architecture & Module Design

## 4.1. Technology

Backend sử dụng:
- Java;
- Spring Boot;
- Spring Web;
- Spring Data JPA;
- Spring Security;
- PostgreSQL;
- Redis;
- JWT;
- HTTP clients để tích hợp provider.

## 4.2. Package Structure

```text
com.project.travel
│
├── common
│   ├── config
│   ├── exception
│   ├── response
│   ├── security
│   └── util
│
├── user
│   ├── controller
│   ├── service
│   ├── domain
│   ├── repository
│   └── dto
│
├── trip
├── place
├── planning
├── ai
├── weather
├── map
├── memory
├── journal
└── journey
```

Mỗi business module có trách nhiệm rõ ràng và hạn chế coupling.

## 4.3. Planning Module

```text
planning
├── controller
├── application
├── domain
│   ├── planner
│   ├── route
│   └── constraint
├── algorithm
│   └── heldkarp
├── service
├── dto
└── repository
```

Luồng:

```text
Trip Input
   ↓
Planning Service
   ↓
Validate Constraints
   ↓
Route Information
   ↓
Held–Karp
   ↓
Optimal Order
   ↓
AI Context Layer
   ↓
Recommendation
```

---

# 5. 6D — Planning & AI Architecture

## 5.1. Planning Pipeline

```text
Destination
Travel Dates
Places
Transportation
Preferences
Constraints
      │
      ▼
Constraint Validation
      │
      ▼
Map / Route Data
      │
      ▼
Held–Karp
      │
      ▼
Quantitative Optimal Order
      │
      ▼
Weather + Opening Hours + Context
      │
      ▼
AI
      │
      ▼
Personalized Recommendation
```

## 5.2. Held–Karp

Held–Karp giải TSP bằng Dynamic Programming.

Độ phức tạp:

**O(n² 2ⁿ)**

Do đó phù hợp với số lượng địa điểm giới hạn trong một planning request.

Không nên cho số lượng địa điểm quá lớn chạy Held–Karp trực tiếp. Giới hạn thực tế cần được xác định bằng benchmark theo môi trường triển khai.

## 5.3. Vai trò AI

AI không thay thế Held–Karp.

AI thực hiện:
- phân tích sở thích;
- hiểu yêu cầu tự nhiên;
- diễn giải kết quả;
- đề xuất khung giờ;
- phân tích context;
- đề xuất điều chỉnh nhỏ;
- tạo lý do dễ hiểu.

AI trả về structured output để Backend validate trước khi sử dụng.

## 5.4. Weather

Weather là **Soft Context**.

```text
Weather API
    ↓
Forecast
    ↓
AI Context
    ↓
Recommendation / Note
```

Ví dụ:
- khả năng mưa vào buổi chiều;
- gợi ý cân nhắc hoạt động ngoài trời vào buổi sáng;
- khuyến nghị mang ô;
- cảnh báo nhiệt độ cao.

Weather **không tự động thay đổi itinerary**, vì dự báo có sai số và quyết định cuối cùng thuộc về người dùng.

---

# 6. 6E — External Integration Architecture

Các provider bên ngoài được truy cập thông qua adapter/service layer.

```text
Application
   │
   ├── MapProvider
   ├── WeatherProvider
   ├── PlaceProvider
   └── AIProvider
           │
           ▼
      Provider Adapter
           │
           ▼
       External API
```

Không để business logic phụ thuộc trực tiếp vào một SDK/provider cụ thể.

## 6.1. Maps / Routing

Cung cấp:
- latitude;
- longitude;
- distance;
- estimated travel time;
- route information.

Dữ liệu được sử dụng để:
- xây dựng Distance Matrix;
- tính khoảng cách;
- chạy Held–Karp;
- hiển thị route.

## 6.2. Weather

Cung cấp:
- nhiệt độ;
- điều kiện thời tiết;
- khả năng mưa;
- gió;
- forecast theo thời gian.

Được sử dụng như context.

## 6.3. AI Provider

Input:

```text
destination
travelDates
places
transportation
preferences
constraints
routeInformation
weatherInformation
```

Output là structured recommendation.

Backend phải validate output trước khi trả về frontend.

---

# 7. 6F — Data Architecture

## 7.1. Core Data

```text
User
  ↓
Trip
  ↓
Place
  ↓
Itinerary
  ↓
Memory
  ↓
Journal / Journey
```

Quan hệ nghiệp vụ:

```text
User
 └── Trip
      ├── Places
      ├── Itinerary
      └── Memories
             └── Place
```

## 7.2. Memory

```text
Memory
├── User
├── Trip
├── Place
├── Images
├── Note
└── CreatedAt
```

Một địa điểm có thể có nhiều Memory theo nhiều lần ghé.

## 7.3. My Journey

Journey statistics được tổng hợp từ dữ liệu thực tế:
- số tỉnh/thành đã khám phá;
- số địa điểm;
- số Memory;
- số ảnh;
- số chuyến đi.

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

Nếu Place chỉ nằm trong Itinerary nhưng chưa có Memory:

```text
Itinerary only
   ↓
Not Explored
```

Không bắt buộc GPS/check-in để xác định explored.

---

# 8. 6G — Security Architecture

## 8.1. Authentication

```text
Login
 ↓
Authentication
 ↓
JWT
 ↓
Request
 ↓
Security Filter
 ↓
Authorization
```

Backend được thiết kế stateless.

## 8.2. Authorization

User chỉ được truy cập dữ liệu thuộc quyền của mình.

Ví dụ User B không thể truy cập Memory của User A chỉ bằng cách thay đổi ID trong URL.

## 8.3. Input Validation

Backend kiểm tra:
- required fields;
- data type;
- date range;
- ownership;
- file type;
- file size;
- AI structured output;
- provider response.

Frontend validation chỉ là lớp hỗ trợ.

## 8.4. Media Security

Ảnh cần:
- kiểm tra MIME type;
- giới hạn kích thước;
- kiểm tra extension;
- lưu tại Media Storage;
- không lưu binary trực tiếp trong PostgreSQL nếu không cần.

---

# 9. 6H — Infrastructure & Deployment

## 9.1. MVP Deployment

```text
                    Internet
                       │
                       ▼
                  Frontend
                       │
                       ▼
                Spring Boot API
                  │        │
                  ▼        ▼
             PostgreSQL   Redis
                  │
                  ▼
             Media Storage

Spring Boot
    ├── AI API
    ├── Maps API
    └── Weather API
```

## 9.2. Containerization

Có thể container hóa:

```text
Docker
├── Spring Boot
├── PostgreSQL
└── Redis
```

## 9.3. Environment Configuration

Không hard-code:
- API keys;
- JWT secrets;
- database passwords;
- provider credentials.

Sử dụng environment variables hoặc secret management.

---

# 10. 6I — Scalability, Reliability & Future Evolution

## 10.1. Nguyên tắc

> **Scale only when necessary.**

Không triển khai distributed architecture phức tạp ngay từ MVP.

## 10.2. Backend Scaling

MVP:

```text
Spring Boot Instance
```

Khi traffic tăng:

```text
             Load Balancer
              /    |    \
       Backend1 Backend2 BackendN
              \    |    /
               PostgreSQL
```

Backend stateless để hỗ trợ horizontal scaling.

## 10.3. Database Scaling

Ưu tiên:
1. Indexing.
2. Query optimization.
3. Pagination.
4. Connection pooling.
5. Caching.
6. Read replica nếu thực sự cần.

## 10.4. Redis

Redis dùng cho:
- caching;
- giảm tải Database;
- giảm số lần gọi External API;
- dữ liệu tạm thời phù hợp.

Redis không phải nguồn dữ liệu nghiệp vụ chính.

Nếu Redis mất, Database vẫn phải hoạt động.

## 10.5. External API Reliability

Có thể áp dụng:
- timeout;
- retry có giới hạn;
- fallback;
- rate limiting;
- caching;
- circuit breaker khi hệ thống lớn.

## 10.6. Graceful Degradation

### Core

```text
Backend
PostgreSQL
```

### Context / Enhancement

```text
AI
Weather
Redis
```

Ví dụ:
- AI unavailable → Itinerary vẫn tồn tại.
- Weather unavailable → Itinerary vẫn tồn tại.
- Redis unavailable → Database vẫn tồn tại.

## 10.7. Media Scalability

```text
Image
 ↓
Optimization
 ↓
Object / Media Storage
 ↓
CDN
 ↓
User
```

Database lưu metadata/reference thay vì binary ảnh.

Có thể tạo:
- thumbnail;
- medium;
- display version.

## 10.8. Planning Scalability

Held–Karp:

**O(n² 2ⁿ)**

Vì vậy:

```text
Small / Medium n
       ↓
Held–Karp
```

Nếu số địa điểm rất lớn:

```text
Large Input
    ↓
Preprocessing / Grouping
    ↓
Smaller Planning Problems
    ↓
Held–Karp
```

hoặc chuyển sang hybrid/heuristic trong tương lai.

MVP vẫn ưu tiên Held–Karp để chứng minh phần thuật toán.

## 10.9. Async Processing

Tác vụ không cần phản hồi tức thời có thể chuyển sang queue/worker:

```text
User
 ↓
Create Story Request
 ↓
Queue
 ↓
Worker
 ↓
AI
 ↓
Save Result
```

Không áp dụng async mọi nơi trong MVP.

## 10.10. Event-driven Evolution

Tương lai có thể dùng:

```text
Memory Created
      ↓
MemoryCreatedEvent
      ├── Update Journey Statistics
      ├── Update Place Statistics
      ├── Update Timeline
      └── Analytics
```

MVP có thể xử lý trực tiếp để giảm complexity.

---

# 11. Reliability Model

| Thành phần | Vai trò | Mức độ |
|---|---|---|
| PostgreSQL | Core persistence | Critical |
| Spring Boot | Core business | Critical |
| Media Storage | Lưu ảnh | Important |
| Maps/Routing | Route calculation | Important |
| AI | Recommendation | Soft |
| Weather | Context | Soft |
| Redis | Cache | Soft |

Nguyên tắc:

> **Core user data không được phụ thuộc tuyệt đối vào External Recommendation Services.**

---

# 12. Architecture Evolution Roadmap

## Giai đoạn 1 — MVP

```text
Modular Monolith
+
PostgreSQL
+
Redis
+
Media Storage
+
Held–Karp
+
AI / Maps / Weather APIs
```

## Giai đoạn 2 — Growth

```text
Horizontal Backend Scaling
+
Caching
+
CDN
+
Monitoring
+
Database Optimization
```

## Giai đoạn 3 — Large Scale

```text
Dedicated Planning Service
+
Dedicated AI Service
+
Async Queue
+
Event-driven Components
+
Distributed Infrastructure
```

Không triển khai Giai đoạn 3 nếu chưa có nhu cầu thực tế.

---

# 13. Architecture Decision Summary

| Quyết định | Lý do |
|---|---|
| Modular Monolith | Đơn giản, dễ phát triển MVP |
| Spring Boot | Phù hợp backend Java và đồ án |
| PostgreSQL | Dữ liệu nghiệp vụ có quan hệ rõ ràng |
| Redis | Cache và giảm tải |
| Media Storage | Phù hợp dữ liệu ảnh lớn |
| JWT | Stateless authentication |
| Held–Karp | Tối ưu TSP chính xác, có giá trị học thuật |
| AI | Cá nhân hóa và giải thích |
| Weather | Soft context |
| Maps API | Khoảng cách và route |
| Structured AI Output | Dễ validate và tích hợp |
| Horizontal Scaling | Mở rộng khi traffic tăng |
| Async Processing | Chỉ dùng cho tác vụ phù hợp |
| Microservices | Chỉ tách khi có bottleneck thực tế |

---

# 14. Architecture Flow Tổng quát

```text
                         USER
                           │
                           ▼
                    Frontend / Mobile
                           │
                           ▼
                    Spring Boot API
                           │
             ┌─────────────┼─────────────┐
             │             │             │
             ▼             ▼             ▼
          User/Trip     Planning      Memory
             │             │             │
             │             ├── Maps      │
             │             ├── Held-Karp │
             │             ├── Weather   │
             │             └── AI        │
             │                           │
             └──────────┬────────────────┘
                        ▼
                   PostgreSQL
                        │
                  ┌─────┴─────┐
                  ▼           ▼
                Redis    Media Storage
                              │
                              ▼
                             CDN
```

---

# 15. Planning Flow — Điểm nhấn học thuật

```text
User Input
    │
    ├── Destination
    ├── Travel Dates
    ├── Places
    ├── Transportation
    ├── Preferences
    └── Constraints
    │
    ▼
Map / Route Data
    │
    ▼
Distance Matrix
    │
    ▼
Held–Karp
    │
    ▼
Optimal Quantitative Route
    │
    ├── Opening Hours
    ├── Weather Context
    ├── User Preferences
    └── Place Priority
    │
    ▼
AI
    │
    ▼
Recommendation
    │
    ├── Apply
    ├── Edit
    └── Ignore
    │
    ▼
USER DECISION
```

Điểm phản biện:

> **Held–Karp không bị AI thay thế. AI không tự quyết định route tối ưu về mặt toán học. AI sử dụng kết quả thuật toán và context bổ sung để cá nhân hóa, giải thích và đề xuất.**

---

# 16. Các nguyên tắc kiến trúc cốt lõi

1. **User-centric:** Người dùng luôn có quyền quyết định cuối cùng.
2. **Modular:** Mỗi module có trách nhiệm rõ ràng.
3. **Deterministic Core:** Logic cần tính chính xác được xử lý bằng code/algorithm.
4. **AI as Assistant:** AI hỗ trợ recommendation, personalization và explanation.
5. **External API as Enhancement:** Maps, Weather và AI không phải nguồn dữ liệu nghiệp vụ cốt lõi.
6. **Data Ownership:** PostgreSQL là nguồn dữ liệu nghiệp vụ chính.
7. **Scalable by Design:** Có khả năng mở rộng nhưng không over-engineer MVP.
8. **Evolvable Architecture:** Có thể tách module thành service riêng khi thực sự cần.

---

# 17. Kết luận Phase 6

Phase 6 đã hoàn thiện kiến trúc từ cấp nguyên tắc đến triển khai:

```text
6A — Architecture Principles
        ↓
6B — High-Level Architecture
        ↓
6C — Backend Architecture
        ↓
6D — Planning & AI Architecture
        ↓
6E — External Integration
        ↓
6F — Data Architecture
        ↓
6G — Security Architecture
        ↓
6H — Infrastructure & Deployment
        ↓
6I — Scalability & Reliability
```

Kiến trúc MVP được chốt:

```text
┌───────────────────────────────────────────────┐
│                    CLIENT                     │
│              Web / Mobile Frontend            │
└───────────────────────┬───────────────────────┘
                        │
                        ▼
┌───────────────────────────────────────────────┐
│              JAVA SPRING BOOT                │
│              MODULAR MONOLITH                 │
│                                               │
│ User │ Trip │ Place │ Planning │ Memory       │
│ AI   │ Map  │ Weather │ Journal │ Journey     │
└──────┬────────────┬───────────────┬───────────┘
       │            │               │
       ▼            ▼               ▼
 PostgreSQL       Redis       Media Storage
       │
       └──────────────────┐
                          ▼
                 External Services
              ┌────────┬────────┬────────┐
              ▼        ▼        ▼
             Maps    Weather     AI
              │
              ▼
        Planning Engine
              │
              ▼
           Held–Karp
```

**PHASE 6 — SYSTEM ARCHITECTURE: HOÀN THÀNH.**

Phase tiếp theo dự kiến:

> **PHASE 7 — DATABASE DESIGN & DATA MODELING**

Tập trung vào ERD, Entity, Relationship, Cardinality, Primary Key, Foreign Key, Constraints, Index, Normalization và mô hình dữ liệu:

```text
User
 ↓
Trip
 ├── Place
 ├── Itinerary
 └── Memory
       ├── Images
       └── Journal
```
