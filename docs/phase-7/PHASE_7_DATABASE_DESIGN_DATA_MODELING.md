# PHASE 7 — DATABASE DESIGN & DATA MODELING

## 1. Mục tiêu

Phase 7 hoàn thiện toàn bộ thiết kế dữ liệu của hệ thống:

- Lựa chọn Database.
- Domain/Data Model.
- Entity.
- Relationship & Cardinality.
- Database Schema.
- Constraint & Data Integrity.
- JPA/Hibernate Mapping.
- Transaction & Query Strategy.
- Migration, Seed Data và Environment.

### Kiến trúc đã chốt

```text
Java Spring Boot
        │
        ├── JPA / Hibernate
        ├── Flyway
        ▼
   PostgreSQL
```

Nguyên tắc:

> JPA/Hibernate chịu trách nhiệm ORM và validation; Flyway quản lý Schema Migration; PostgreSQL là Database thực tế.

---

# 2. 7A — Database Design Principles & Design Philosophy

## 2.1. Nguyên tắc chính

Database là nơi lưu trữ dữ liệu nghiệp vụ ổn định và nhất quán.

Database lưu các dữ liệu như:

- User
- Trip
- Place
- Province
- Itinerary
- Memory
- Media metadata

Database không trực tiếp xử lý:

- AI reasoning.
- AI recommendation.
- Tối ưu tuyến đường.
- Logic giao diện.

Các nghiệp vụ này thuộc Application/Service Layer.

## 2.2. Tính nhất quán

Sử dụng:

- Primary Key.
- Foreign Key.
- Unique Constraint.
- NOT NULL.
- Check Constraint khi cần.
- Index.
- Transaction.

## 2.3. Database độc lập với AI

Luồng:

```text
Places
   ↓
Distance Matrix
   ↓
Held-Karp
   ↓
Optimal Route
   ↓
AI Contextual Analysis
   ↓
Recommendation
   ↓
User Apply / Edit / Ignore
   ↓
Itinerary
   ↓
PostgreSQL
```

AI không được phép tự ý ghi dữ liệu nghiệp vụ cuối cùng vào Database mà phải đi qua Application Layer.

## 2.4. Lựa chọn PostgreSQL

PostgreSQL phù hợp vì:

- Domain có quan hệ rõ ràng.
- Cần Foreign Key và Referential Integrity.
- Cần Transaction.
- Phù hợp với Spring Boot + JPA/Hibernate.
- Hỗ trợ tốt Index, Constraint và Query.
- Phù hợp với dữ liệu Trip, Place, Itinerary, Memory có quan hệ chặt chẽ.

MongoDB không được chọn làm Database chính vì mô hình hiện tại thiên về dữ liệu quan hệ hơn document.

---

# 3. 7B — Domain & Data Modeling

## 3.1. Domain tổng quát

```text
User
 │
 └── Trip
       │
       ├── TripPlace ─── Place ─── Province
       │
       ├── Itinerary
       │      └── ItineraryItem ─── Place
       │
       └── Memory ─── Media
```

## 3.2. User Domain

Quản lý tài khoản và quan hệ với Trip/Memory.

## 3.3. Trip Domain

Một chuyến đi gồm:

- Tên.
- Điểm đến.
- Ngày bắt đầu/kết thúc.
- Địa điểm mong muốn.
- Phương tiện.
- Sở thích.
- Ngân sách.
- Ràng buộc.
- Trạng thái.

## 3.4. Place Domain

Địa điểm gồm:

- Tên.
- Địa chỉ.
- Tọa độ.
- Tỉnh/thành.
- Provider Place ID.
- Metadata cần thiết.

## 3.5. Itinerary Domain

Lưu lịch trình:

- Ngày.
- Thứ tự.
- Địa điểm.
- Thời gian.
- Khoảng cách.
- Thời gian di chuyển.
- Ghi chú.

## 3.6. Memory Domain

Memory là một trải nghiệm người dùng lưu tại một địa điểm:

- User.
- Trip.
- Place.
- Note.
- Created time.
- Media.

Memory là nguồn dữ liệu cho:

```text
Journal
Place Memory
My Places
My Journey
Exploration Progress
```

## 3.7. Media Domain

Database chỉ lưu metadata và URL/reference.

```text
Memory
  ↓
Media metadata
  ↓
External Media Storage
```

Không lưu binary ảnh lớn trực tiếp trong PostgreSQL.

---

# 4. 7C — Entity Definition

Các Entity chính:

```text
User
Province
Place
Trip
TripPlace
Itinerary
ItineraryItem
Memory
Media
```

## 4.1. User

```text
id
fullName
email / username
passwordHash
createdAt
updatedAt
```

## 4.2. Province

```text
id
code
name
```

Province là Master/Reference Data.

## 4.3. Place

```text
id
providerPlaceId
name
address
latitude
longitude
provinceId
createdAt
updatedAt
```

## 4.4. Trip

```text
id
userId
name
destination
startDate
endDate
transportation
preferences
budget
constraints
status
createdAt
updatedAt
```

## 4.5. TripPlace

Entity trung gian:

```text
id
tripId
placeId
priority
estimatedDuration
notes
createdAt
```

## 4.6. Itinerary

```text
id
tripId
name
status
createdAt
updatedAt
```

## 4.7. ItineraryItem

```text
id
itineraryId
placeId
date
startTime
endTime
sequence
distanceFromPrevious
travelTimeFromPrevious
note
```

## 4.8. Memory

```text
id
userId
tripId
placeId
note
createdAt
updatedAt
```

## 4.9. Media

```text
id
memoryId
url
mimeType
fileSize
width
height
displayOrder
createdAt
```

---

# 5. 7D — Relationship & Cardinality Design

```text
User      1 ─── N Trip
Province  1 ─── N Place
Trip      N ─── N Place       → TripPlace
Trip      1 ─── N Itinerary
Itinerary 1 ─── N ItineraryItem
Place     1 ─── N ItineraryItem
User      1 ─── N Memory
Trip      1 ─── N Memory
Place     1 ─── N Memory
Memory    1 ─── N Media
```

### Trip ↔ Place

Many-to-many được triển khai qua `TripPlace`, không dùng quan hệ N-N trực tiếp nếu cần thêm metadata cho mối quan hệ.

---

# 6. 7E — Database Schema Design

## 6.1. Các bảng chính

```text
users
provinces
places
trips
trip_places
itineraries
itinerary_items
memories
media
```

## 6.2. Logical Schema

```text
users
 └── trips
      ├── trip_places ─── places ─── provinces
      ├── itineraries
      │      └── itinerary_items ─── places
      └── memories ─── media
```

## 6.3. Không tạo bảng My Places

My Places được tổng hợp:

```text
Memory → Place
```

Không cần bảng `my_places`.

## 6.4. Không tạo bảng My Journey

My Journey được tổng hợp từ:

```text
Trip
Memory
Media
Place
Province
```

---

# 7. 7F — Constraints & Data Integrity

## 7.1. Primary Key

Mỗi Entity có ID duy nhất.

## 7.2. Foreign Key

Ví dụ:

```text
trips.user_id       → users.id
places.province_id  → provinces.id
memories.place_id   → places.id
memories.trip_id    → trips.id
media.memory_id     → memories.id
```

## 7.3. NOT NULL

Áp dụng cho dữ liệu bắt buộc theo nghiệp vụ.

## 7.4. Unique

Có thể áp dụng cho:

```text
User.email
Province.code
Provider Place ID
```

Nếu Place ID phụ thuộc provider, có thể dùng unique composite:

```text
(provider, provider_place_id)
```

## 7.5. Index

Các cột thường xuyên query/JOIN cần cân nhắc index:

```text
users.email
trips.user_id
trip_places.trip_id
trip_places.place_id
memories.user_id
memories.trip_id
memories.place_id
media.memory_id
places.province_id
```

Không tạo index dư thừa.

---

# 8. 7G — JPA Entity Mapping & Persistence Architecture

## 8.1. Persistence Flow

```text
Controller
    ↓
Service
    ↓
Repository
    ↓
JPA / Hibernate
    ↓
PostgreSQL
```

## 8.2. Entity Mapping

Ví dụ:

```java
@Entity
@Table(name = "trips")
public class Trip {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
}
```

## 8.3. Quan hệ

```java
@ManyToOne(fetch = FetchType.LAZY)
@JoinColumn(name = "user_id", nullable = false)
private User user;
```

## 8.4. Lazy Loading

Ưu tiên `LAZY` cho các quan hệ collection hoặc quan hệ có khả năng tải dữ liệu lớn.

Tránh eager-load toàn bộ:

```text
Trip
 ├── TripPlaces
 ├── Itineraries
 └── Memories
```

## 8.5. DTO

Không trả Entity trực tiếp qua REST API.

```text
Entity
   ↓
Service
   ↓
DTO
   ↓
Controller
   ↓
JSON
```

Lợi ích:

- Kiểm soát API Contract.
- Không lộ field nội bộ.
- Tránh vòng lặp serialization.
- Giảm phụ thuộc giữa API và Persistence Model.

---

# 9. 7H — Transaction & Query Strategy

## 9.1. Transaction

Nghiệp vụ nhiều bước cần Transaction.

Ví dụ:

```text
Create Trip
   ↓
Create Trip Places
   ↓
Create Itinerary
   ↓
Create Itinerary Items
```

Nếu lỗi:

```text
ROLLBACK
```

## 9.2. Memory

Ví dụ:

```text
Create Memory
   ↓
Create Media Metadata
```

Cần đảm bảo Database ở trạng thái nhất quán.

Media Storage là hệ thống bên ngoài nên không có ACID transaction chung với PostgreSQL.

## 9.3. Query Strategy

Ưu tiên:

- Spring Data JPA.
- JPQL.
- Native Query khi cần.
- DTO Projection.
- Pagination.

## 9.4. Tránh N+1

Có thể sử dụng:

- Fetch Join.
- EntityGraph.
- Projection.
- Batch fetching.

Tùy use case.

---

# 10. 7I — Migration, Seed Data & Environment

## 10.1. Database Lifecycle

```text
Development
     ↓
Testing
     ↓
Staging
     ↓
Production
```

Mỗi môi trường có Database riêng.

## 10.2. Flyway

Migration đặt tại:

```text
src/main/resources/db/migration/
```

Ví dụ:

```text
V1__create_users.sql
V2__create_provinces.sql
V3__create_places.sql
V4__create_trips.sql
V5__create_trip_places.sql
...
```

## 10.3. Naming Convention

```text
V{version}__{description}.sql
```

## 10.4. Immutable Migration

Migration đã chạy trên môi trường dùng chung không được sửa.

Nếu cần thay đổi:

```text
V10
 ↓
V11
```

## 10.5. Hibernate DDL

Chiến lược chính thức:

```properties
spring.jpa.hibernate.ddl-auto=validate
```

Không dùng `create`, `create-drop` hoặc `update` làm cơ chế Schema chính thức ở Production.

Hibernate chỉ kiểm tra Entity và Database Schema có tương thích hay không.

---

# 11. Seed Data

## 11.1. Reference Data

Province là Master/Reference Data.

Ví dụ:

```text
Đà Nẵng
Hà Nội
Thành phố Hồ Chí Minh
...
```

Dữ liệu tỉnh/thành được dùng chung.

Lưu ý: phạm vi “63 tỉnh/thành” là phạm vi dữ liệu của đề tài tại thời điểm thiết kế; nếu dữ liệu hành chính thay đổi, Master Data phải được cập nhật.

## 11.2. Place Seed

Không cần seed toàn bộ địa điểm Việt Nam.

MVP có thể seed một tập nhỏ để:

- Test AI Planner.
- Test Held-Karp.
- Test Itinerary.
- Test Map.
- Test Memory.
- Demo đồ án.

Địa điểm thực tế có thể lấy từ Places API.

## 11.3. Development Seed

Có thể có:

```text
Demo User
Demo Trip
Demo Places
Demo Itinerary
Demo Memories
```

Không đưa dữ liệu demo/test vào Production.

---

# 12. Environment Configuration

Các profile:

```text
application.yml
application-dev.yml
application-test.yml
application-prod.yml
```

Database:

```text
travel_dev
travel_test
travel_staging
travel_prod
```

Production:

```yaml
spring:
  jpa:
    hibernate:
      ddl-auto: validate

  flyway:
    enabled: true
```

Credentials phải sử dụng Environment Variables/Secrets:

```properties
spring.datasource.password=${DB_PASSWORD}
```

Không commit password/API key vào Git.

---

# 13. Docker Development

Có thể sử dụng Docker Compose:

```yaml
services:
  postgres:
    image: postgres:17
    environment:
      POSTGRES_DB: travel_dev
      POSTGRES_USER: travel
      POSTGRES_PASSWORD: ${DB_PASSWORD}
    ports:
      - "5432:5432"
```

Chạy:

```bash
docker compose up -d
```

---

# 14. Migration Flow

```text
Developer
   ↓
Design Schema Change
   ↓
Create Flyway Migration
   ↓
Modify JPA Entity
   ↓
Run Migration
   ↓
Hibernate Validate
   ↓
Integration Test
   ↓
Git Commit / PR
   ↓
CI/CD
   ↓
Staging
   ↓
Production
```

Flyway duy trì lịch sử trong:

```text
flyway_schema_history
```

Nếu migration lỗi, application không nên tiếp tục chạy với Schema không hợp lệ.

---

# 15. Backup & Security

Production Database cần:

- Automated Backup.
- Khả năng phục hồi.
- Kiểm tra backup định kỳ.

Database backup không thay thế backup của Media Storage.

Dữ liệu nhạy cảm như:

```text
Database password
API keys
JWT secrets
```

không được commit vào repository.

---

# 16. Demo Dataset

Dataset demo có thể mô phỏng:

```text
Demo User
    │
    ▼
Đà Nẵng Trip
    │
    ├── Sơn Trà
    ├── Mỹ Khê
    ├── Ngũ Hành Sơn
    └── Hội An
          │
          ▼
      Itinerary
          │
          ▼
       Memories
          ├── Photos
          └── Notes
```

Dùng để trình diễn:

```text
Khám phá
   ↓
Lập kế hoạch
   ↓
Held-Karp
   ↓
AI Recommendation
   ↓
Apply / Edit / Ignore
   ↓
Itinerary
   ↓
Memory
   ↓
Journal
   ↓
My Journey
```

---

# 17. Database Development Workflow

```text
┌─────────────────────────┐
│ 1. Modify Domain Model  │
└────────────┬────────────┘
             ↓
┌─────────────────────────┐
│ 2. Modify JPA Entity    │
└────────────┬────────────┘
             ↓
┌─────────────────────────┐
│ 3. Create Migration     │
└────────────┬────────────┘
             ↓
┌─────────────────────────┐
│ 4. Run Flyway           │
└────────────┬────────────┘
             ↓
┌─────────────────────────┐
│ 5. Hibernate Validate   │
└────────────┬────────────┘
             ↓
┌─────────────────────────┐
│ 6. Integration Test     │
└────────────┬────────────┘
             ↓
┌─────────────────────────┐
│ 7. Commit / Pull Request│
└─────────────────────────┘
```

---

# 18. Tổng thể Database Architecture

```text
                    ┌────────────────────────┐
                    │     Spring Boot        │
                    │                        │
                    │ Controller             │
                    │ Service                │
                    │ Algorithm              │
                    │ AI Integration         │
                    │ Repository             │
                    └────────────┬───────────┘
                                 │
                         JPA / Hibernate
                                 │
                                 ▼
                    ┌────────────────────────┐
                    │      PostgreSQL        │
                    │                        │
                    │ User                   │
                    │ Trip                   │
                    │ Place                  │
                    │ Province               │
                    │ Itinerary              │
                    │ Memory                 │
                    │ Media Metadata         │
                    └────────────┬───────────┘
                                 │
                              Flyway
                                 │
                         Schema Versioning
```

---

# 19. Database và Algorithm/AI

Database không thực hiện AI hoặc tối ưu tuyến đường.

```text
Places
  ↓
Distance Matrix
  ↓
Held-Karp
  ↓
Optimal Route
  ↓
AI Context Layer
  ↓
Recommendation
  ↓
Apply / Edit / Ignore
  ↓
Itinerary
  ↓
PostgreSQL
```

Thiết kế này giúp:

- Held-Karp độc lập với Database.
- AI Provider có thể thay đổi.
- PostgreSQL không phụ thuộc OpenAI hay AI Provider.
- Persistence Layer không chứa Business Logic phức tạp.

---

# 20. Các quyết định kiến trúc đã chốt

| Hạng mục | Quyết định |
|---|---|
| Database | PostgreSQL |
| Backend | Java Spring Boot |
| ORM | JPA / Hibernate |
| Migration | Flyway |
| Hibernate DDL | `validate` |
| Production DDL | Không dùng `update/create` |
| Reference Data | Seed |
| Province | Master/Reference Data |
| Development Data | Seed riêng |
| Test Database | Database riêng |
| Production Database | Database riêng |
| Migration | Versioned & immutable |
| Credentials | Environment Variables / Secrets |
| Backup | Bắt buộc cho Production |
| Connection Pool | HikariCP |
| Local Database | Docker PostgreSQL |
| Media Binary | External Media Storage |
| Media trong DB | Metadata + URL/Reference |
| My Places | Tổng hợp từ Memory |
| My Journey | Tổng hợp từ Trip/Memory/Place/Province/Media |
| Route Optimization | Held-Karp ở Application/Algorithm Layer |
| AI | Recommendation/Context Layer |
| AI ghi DB trực tiếp | Không |
| Entity trả trực tiếp qua API | Không, dùng DTO |

---

# 21. Phase 7 — Final Summary

```text
7A — Database Design Principles
        ↓
7B — Domain & Data Modeling
        ↓
7C — Entity Definition
        ↓
7D — Relationship & Cardinality
        ↓
7E — Database Schema Design
        ↓
7F — Constraints & Data Integrity
        ↓
7G — JPA Entity Mapping & Persistence
        ↓
7H — Transaction & Query Strategy
        ↓
7I — Migration & Development Environment
```

Kiến trúc cuối cùng:

```text
                    ┌──────────────────────┐
                    │     Client/UI        │
                    └──────────┬───────────┘
                               │ REST API
                               ▼
                    ┌──────────────────────┐
                    │     Spring Boot      │
                    │                      │
                    │ Controller           │
                    │ Service              │
                    │ Algorithm            │
                    │ AI Integration       │
                    │ Repository           │
                    └──────────┬───────────┘
                               │
                         JPA / Hibernate
                               │
                               ▼
                    ┌──────────────────────┐
                    │     PostgreSQL       │
                    │                      │
                    │ User                 │
                    │ Trip                 │
                    │ Place                │
                    │ Province             │
                    │ Itinerary            │
                    │ Memory               │
                    │ Media Metadata       │
                    └──────────┬───────────┘
                               │
                            Flyway
                               │
                       Schema Versioning
```

## Kết luận

Phase 7 đã hoàn thiện nền tảng dữ liệu cho toàn bộ hệ thống.

Thiết kế đảm bảo:

1. PostgreSQL phù hợp với mô hình quan hệ.
2. JPA/Hibernate quản lý Object-Relational Mapping.
3. Flyway kiểm soát lịch sử Schema.
4. Constraint đảm bảo Data Integrity.
5. Transaction đảm bảo tính nhất quán nghiệp vụ.
6. DTO tách API Contract khỏi Persistence Entity.
7. Media Storage tách file lớn khỏi Database.
8. My Places và My Journey được tổng hợp từ dữ liệu hiện có.
9. Held-Karp nằm ở Application/Algorithm Layer.
10. AI là Recommendation/Context Layer.
11. Development, Test, Staging và Production được tách biệt.
12. Migration và Seed Data được quản lý có kiểm soát.

> **PHASE 7 — DATABASE DESIGN & DATA MODELING: HOÀN TẤT**

## Phase tiếp theo

# PHASE 8 — API DESIGN & CONTRACTS

Các nội dung dự kiến:

- Authentication API.
- User API.
- Trip API.
- Place API.
- Itinerary API.
- AI Planner API.
- Route Optimization API.
- Weather API.
- Memory API.
- Journal API.
- My Journey API.
- Request/Response DTO.
- HTTP Status Code.
- Error Response Standard.
- Validation.
- Pagination.
- Authentication/Authorization.
- API Versioning.
- API Contract giữa Client và Spring Boot Backend.
