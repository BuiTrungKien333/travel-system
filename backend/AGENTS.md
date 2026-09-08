# AGENTS.md — Bộ Quy Tắc Làm Việc Cho AI Agent
## Dự án Hệ thống hỗ trợ lập kế hoạch và quản lý hành trình du lịch cá nhân

> **Vai trò của file:** Đây là bộ quy tắc làm việc dành cho mọi AI Coding Agent khi tham gia dự án backend.
>
> Agent phải đọc file này trước khi phân tích, sửa hoặc tạo code.
>
> **Thứ tự ưu tiên khi có mâu thuẫn:**
> 1. Yêu cầu trực tiếp của người dùng trong task hiện tại.
> 2. Kiến trúc, quyết định kỹ thuật và business rule đã được chốt trong tài liệu dự án/codebase.
> 3. Quy tắc trong file này.
> 4. Best practice chung của Java/Spring Boot.
>
> Agent **không được tự ý thay đổi kiến trúc hoặc quyết định đã chốt** chỉ vì có một cách khác mà agent cho là "hay hơn". Nếu nhận thấy quyết định hiện tại có rủi ro đáng kể, phải nêu rõ và đề xuất phương án trước khi thay đổi.

---

# 0. CONTEXT CỦA DỰ ÁN

## 0.1. Mục tiêu

Đây là hệ thống hỗ trợ người dùng:

- tạo và quản lý chuyến đi;
- quản lý danh sách địa điểm;
- lập kế hoạch/lịch trình;
- tối ưu thứ tự tham quan;
- tích hợp bản đồ và định vị;
- tham khảo thông tin thời tiết;
- lưu giữ Memory/Journal;
- theo dõi thống kê và tiến trình khám phá;
- sử dụng AI để gợi ý, cá nhân hóa và diễn giải.

Phạm vi MVP tập trung vào **du lịch tại Việt Nam**.

## 0.2. Frontend

Backend phải cung cấp REST API cho:

- Web: **Next.js**
- Mobile: **React Native**

Frontend **không nằm trong phạm vi coding của repository backend này**, trừ khi task yêu cầu rõ ràng.

Backend phải thiết kế API theo hướng **client-agnostic**: không phụ thuộc vào giao diện của Next.js hay React Native.

## 0.3. Backend stack đã chốt

| Thành phần | Công nghệ |
|---|---|
| Language | Java 21 |
| Framework | Spring Boot |
| Web | Spring Web / REST |
| Persistence | Spring Data JPA |
| ORM | Hibernate |
| Database | PostgreSQL |
| Local DB | Docker / Docker Compose |
| Security | Spring Security |
| Authentication | JWT |
| Build | Maven |
| API Documentation | Springdoc OpenAPI / Swagger |
| Testing | JUnit 5, Mockito, Spring Test |
| Integration testing | Testcontainers khi phù hợp |
| Frontend consumers | Next.js, React Native |

> **Lưu ý:** Không tự ý đổi Spring Boot major version. Phải kiểm tra `pom.xml` của project hiện tại trước khi đề xuất dependency hoặc API tương ứng.

---

# 1. NGUYÊN TẮC LÀM VIỆC CỐT LÕI

## 1.1. Không đoán dữ liệu nghiệp vụ

Không tự bịa:

- tên field;
- enum value;
- business rule;
- trạng thái;
- quan hệ Entity;
- API contract;
- database constraint;
- provider ID;
- cách tính thống kê;
- quy tắc AI;
- quy tắc tối ưu lộ trình.

Nếu thông tin đã có trong:

- code;
- tài liệu `/docs`;
- `AGENTS.md`;
- API specification;
- database/domain model;

thì phải ưu tiên những nguồn đó.

Nếu vẫn thiếu thông tin và ảnh hưởng đến tính đúng đắn, agent phải:

1. tìm trong codebase;
2. tìm trong tài liệu;
3. nếu chưa đủ, nêu rõ assumption;
4. chỉ hỏi người dùng khi assumption có thể làm thay đổi kiến trúc hoặc business behavior.

**Không hỏi những câu có thể tự xác định từ codebase hoặc context.**

---

## 1.2. Không phá vỡ kiến trúc hiện có

Trước khi tạo class/package mới:

1. kiểm tra package hiện tại;
2. tìm module tương tự;
3. tìm pattern đang được sử dụng;
4. tái sử dụng pattern nếu phù hợp.

Không tự ý refactor toàn bộ project chỉ vì phát hiện một style khác.

Nếu cần thay đổi architecture lớn:

- giải thích lý do;
- nêu phạm vi ảnh hưởng;
- đề xuất trước;
- chỉ thực hiện khi task cho phép.

---

## 1.3. Không code giả

Không được:

- tạo stub rồi báo hoàn thành;
- để TODO thay cho phần logic chính;
- return dữ liệu giả để "test cho chạy";
- catch exception rồi bỏ qua;
- dùng `assertTrue(true)` hoặc assertion vô nghĩa;
- mock chính logic mà test cần kiểm chứng.

Nếu một phần chưa thể hoàn thiện, phải nói rõ:

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

---

## 1.4. Mỗi thay đổi phải có lý do

Sau mỗi task, agent phải tóm tắt:

- đã thay đổi gì;
- vì sao thay đổi;
- file/package bị ảnh hưởng;
- API/DB behavior bị ảnh hưởng như thế nào;
- test nào đã chạy;
- kết quả test;
- việc gì còn lại.

---

## 1.5. Ưu tiên tính đúng đắn

Thứ tự ưu tiên:

```text
Correctness
    ↓
Security & Data Integrity
    ↓
Maintainability
    ↓
Testability
    ↓
Performance
    ↓
Developer Convenience
```

Không tối ưu sớm khi chưa có bằng chứng về bottleneck.

---

## 1.6. Chống over-engineering — KISS & YAGNI (bắt buộc)

> Đây là quy tắc riêng để trị bệnh AI hay "suy nghĩ quá mức": lo cho những trường hợp xác suất cực thấp rồi đánh đổi sự đơn giản, dễ đọc, dễ maintain — trong khi task thực tế không cần tới mức đó.

- **Làm đúng scope của task, không tự mở rộng.** Nếu task là "thêm API tạo user", agent chỉ làm đúng việc đó — không tự ý thêm cache layer, retry mechanism, event bus, hay abstraction "phòng khi sau này cần" nếu không ai yêu cầu (nếu có hãy ghi chú đề xuất cho user).
- **YAGNI (You Aren't Gonna Need It):** Không tạo interface cho class chỉ có 1 implementation "để sau dễ mở rộng", không tạo generic/abstract layer cho use-case chưa tồn tại. Chỉ trừu tượng hóa khi đã có ≥2 use-case thật cần dùng chung.
- **KISS (Keep It Simple):** Giữa 2 cách giải quyết cho cùng kết quả, luôn ưu tiên cách đơn giản, ít tầng gián tiếp hơn, dễ đọc hơn — trừ khi có lý do nghiệp vụ rõ ràng bắt buộc phải phức tạp hơn.
- **Không tự thêm design pattern chỉ để "cho chuẩn".** Factory, Strategy, Builder, Observer... chỉ dùng khi thực sự giải quyết một vấn đề cụ thể đang tồn tại trong task, không dùng để thể hiện kỹ thuật hoặc vì "best practice sách vở nói vậy" (nếu có hãy ghi chú đề xuất cho user).
- **Đánh giá rủi ro theo đúng tỷ lệ (proportional engineering):** Với case xác suất xảy ra cực thấp và hậu quả nhỏ (không ảnh hưởng bảo mật/tiền/dữ liệu), agent xử lý ở mức tối thiểu hợp lý (validate cơ bản, log lại) thay vì viết thêm nhiều lớp xử lý/try-catch/fallback phức tạp cho nó. Mức độ phòng thủ phải tương xứng với mức độ rủi ro thật, không phải rủi ro lý thuyết.
    - Ngoại lệ: **không áp dụng nguyên tắc "đơn giản hóa" này cho bảo mật, tính toàn vẹn dữ liệu, và luồng tiền/thanh toán** — những phần đó luôn phải xử lý chặt chẽ dù xác suất thấp, vì Bảo mật và Database & Transaction luôn được ưu tiên cao hơn.
- **Không tự ý refactor ngoài phạm vi task.** Nếu agent thấy code khác "chưa tối ưu" nhưng không liên quan tới task đang làm, chỉ ghi chú lại trong phần tóm tắt, không tự sửa — tránh PR phình to, khó review, dễ phá vỡ thứ đang chạy tốt.
- **Giải thích được lý do độ phức tạp.** Nếu một giải pháp cần nhiều hơn 1 class/tầng để giải quyết, agent phải nêu rõ trong tóm tắt: vì sao cần mức phức tạp đó, không phải mặc định chọn cách "an toàn nhất về mặt kỹ thuật".
- **Ưu tiên code dễ đọc hơn code "khôn khéo".** Không viết một dòng xử lý 5 việc bằng stream/lambda lồng nhau khó đọc chỉ để ngắn gọn — thà chia làm vài dòng rõ ràng, dễ debug (nếu có hãy ghi chú đề xuất cho user).
- **Khi không chắc mức độ cần làm kỹ tới đâu, hỏi thay vì tự quyết định làm nhiều hơn yêu cầu.** Agent không nên tự cho rằng "làm nhiều hơn luôn an toàn hơn" — code thừa cũng là nợ kỹ thuật, không phải chất lượng.
- **Hãy luôn cập nhật những kiến thức mới, hiện đại nhất để thực hiện coding trong dự án này bởi vì dự án này sử dụng rất nhiều thư viện, công cụ version mới.**

# 2. KIẾN TRÚC BACKEND

## 2.1. Kiến trúc tổng thể

Backend sử dụng kiến trúc **modular layered architecture**, kết hợp **package-by-feature** cho domain nghiệp vụ.

Không ép toàn bộ project vào một package `controller/service/repository` khổng lồ.

Cấu trúc định hướng:

```text
com.<project>/
├── common/
│   ├── response/
│   ├── exception/
│   ├── validation/
│   └── util/
│
├── config/
│
├── security/
│
├── health/
│
├── user/
│   ├── controller/
│   ├── service/
│   ├── repository/
│   ├── entity/
│   ├── dto/
│   │   ├── request/
│   │   └── response/
│   └── mapper/
│
├── trip/
├── place/
├── planning/
├── route/
├── weather/
├── memory/
├── journal/
└── statistics/
```

Tên module phải phản ánh domain thực tế của dự án.

## 2.2. Dependency direction

Luồng chuẩn:

```text
Controller
    ↓
Application/Service
    ↓
Repository
    ↓
Database
```

Với integration bên ngoài:

```text
Service
    ↓
Provider/Client/Adapter
    ↓
External API
```

Controller **không** gọi trực tiếp:

- Repository;
- external API client;
- AI provider;
- map provider;
- weather provider.

---

## 2.3. Controller

Controller chỉ chịu trách nhiệm:

- nhận HTTP request;
- bind request DTO;
- trigger Bean Validation;
- lấy authentication context;
- gọi service;
- trả HTTP response.

Controller không chứa:

- business logic;
- thuật toán tối ưu;
- xử lý database;
- gọi OpenAI trực tiếp;
- gọi Map/Weather API trực tiếp;
- tính toán nghiệp vụ phức tạp.

### @RequestMapping prefix

Mỗi Controller phải khai báo base path tại class level bằng `@RequestMapping`:

```java
@RestController
@RequestMapping("/api/v1/trips")
public class TripController { ... }
```

Không hard-code full path lặp đi lặp lại trong từng method:

```java
// ❌ Sai
@GetMapping("/api/v1/trips/{tripId}")
@PostMapping("/api/v1/trips")

// ✅ Đúng
@GetMapping("/{tripId}")
@PostMapping
```

Base path phải tuân theo format `/api/v1/{resource}` đã chốt ở section 12.

---

## 2.4. Service

Service chịu trách nhiệm:

- business logic;
- orchestration;
- transaction boundary;
- kiểm tra business rule;
- phối hợp repository và external clients;
- điều phối thuật toán.

Service không nên phụ thuộc vào HTTP-specific objects nếu không cần thiết.

---

## 2.5. Repository

Repository chịu trách nhiệm persistence.

Ưu tiên:

- Spring Data JPA;
- derived query;
- JPQL;
- parameterized query;
- `Specification`/Criteria khi cần dynamic filtering;
- `@EntityGraph` khi cần kiểm soát fetch.

Không đưa business logic vào Repository.

---

# 3. DOMAIN ARCHITECTURE ĐẶC THÙ CỦA DỰ ÁN

## 3.1. Trip

Trip là aggregate nghiệp vụ quan trọng, có thể liên quan đến:

- thời gian;
- danh sách địa điểm;
- phương tiện;
- preferences;
- constraints;
- itinerary.

Không để Trip Service trở thành "God Service".

Khi logic đủ lớn, tách thành các component/service chuyên biệt.

---

## 3.2. Place

Place chứa thông tin địa điểm và dữ liệu liên quan đến provider.

Phải phân biệt:

```text
Place trong hệ thống
        ≠
Place ID của external provider
```

Không dùng external provider ID làm primary key nội bộ nếu chưa có quyết định rõ ràng.

---

## 3.3. Route & itinerary optimization

Đây là phần có ý nghĩa học thuật quan trọng của đồ án.

Hệ thống sử dụng **Held-Karp Dynamic Programming** cho bài toán tối ưu thứ tự đi qua các địa điểm trong phạm vi bài toán đã xác định.

Agent **không được tự ý thay Held-Karp bằng Nearest Neighbor, 2-opt hoặc heuristic khác** nếu task đang triển khai phần thuật toán chính.

Phân biệt rõ:

```text
Algorithmic optimization
    ↓
Held-Karp
    ↓
Tối ưu định lượng thứ tự/chi phí route

AI layer
    ↓
Contextual interpretation & personalization
    ↓
Weather / opening hours / preferences / priorities / explanation
```

AI không được âm thầm thay thế kết quả thuật toán nếu contract của module yêu cầu giữ nguyên kết quả tối ưu.

Nếu AI được phép điều chỉnh route, phải có business rule và API contract rõ ràng.

---

## 3.4. AI

AI là **advisory layer**, không phải system command.

AI có thể:

- phân tích preferences;
- phân tích weather;
- cân nhắc opening hours;
- phân tích mức độ ưu tiên;
- cá nhân hóa;
- giải thích recommendation;
- đề xuất điều chỉnh itinerary.

AI không được:

- tự ý ghi đè dữ liệu người dùng;
- tự ý thay đổi Trip;
- tự ý tạo Memory;
- tự ý thực hiện destructive action;
- tự ý thay đổi route đã được người dùng chốt.

Người dùng luôn có quyền:

```text
Apply
Edit
Ignore
```

AI output phải được coi là **untrusted external input** và phải được validate trước khi hệ thống sử dụng.

---

# 4. JPA / HIBERNATE & DATABASE

## 4.1. PostgreSQL

Database chính:

```text
PostgreSQL
```

Local development:

```text
Docker Compose
    ↓
travel-postgres
    ↓
travel_db
```

Không kết nối application local trực tiếp vào một PostgreSQL production database.

---

## 4.2. Entity là source of truth của domain model

Trong project này, cấu trúc bảng/cột được định nghĩa chủ yếu thông qua:

```text
Java Entity
    ↓
JPA annotations
    ↓
Hibernate schema management
```

Đây là quyết định phù hợp với giai đoạn phát triển hiện tại của đồ án.

### Development

Có thể sử dụng:

```yaml
spring:
  jpa:
    hibernate:
      ddl-auto: update
```

để Hibernate đồng bộ schema trong môi trường local development.

### Production

Không sử dụng `ddl-auto: create` hoặc `create-drop`.

Production phải kiểm soát schema một cách an toàn. Nếu schema được tạo sẵn từ pipeline/migration, dùng:

```yaml
ddl-auto: validate
```

> Nếu dự án sau này chốt Flyway/Liquibase làm schema migration chính thức, phải cập nhật lại quy tắc này và không để Hibernate tự sửa schema production.

**Agent không được tự ý dùng `ddl-auto: update` cho production.**

---

## 4.3. Entity rules

Không dùng `@Data` trên JPA Entity.

Ưu tiên:

```java
@Getter
@NoArgsConstructor
```

và constructor/method phù hợp với invariant của Entity.

Tránh:

- setter public cho mọi field;
- `toString()` đi qua lazy relationship;
- `equals/hashCode` dựa trực tiếp vào toàn bộ fields;
- serialize Entity trực tiếp ra REST API.

---

## 4.4. Relationship

Mặc định ưu tiên:

```java
fetch = FetchType.LAZY
```

Đặc biệt với:

```text
@OneToMany
@ManyToMany
```

Tránh:

```java
@ManyToOne(fetch = FetchType.EAGER)
```

nếu không có lý do rõ ràng.

Khi cần load relationship:

- `JOIN FETCH`;
- `@EntityGraph`;
- projection;
- query chuyên biệt.

Không chữa N+1 bằng cách đổi toàn bộ relationship sang EAGER.

---

## 4.5. Transaction

`@Transactional` đặt tại Service/Application layer.

Ví dụ:

```text
Create Trip
    ↓
save Trip
    ↓
save Trip Places
    ↓
update related data
```

Nếu cần atomicity:

```java
@Transactional
```

Không đặt transaction tùy tiện trên Controller.

---

## 4.6. Database naming

Ưu tiên:

```text
snake_case
```

Ví dụ:

```text
trip
trip_place
start_date
created_at
updated_at
```

Primary key, foreign key, unique constraint và index phải được thiết kế theo query pattern thực tế.

Không tạo index "cho có".

---

## 4.7. Soft Delete

Phải có quyết định rõ ràng cho từng entity trước khi implement delete:

```text
Hard delete  → xóa vĩnh viễn khỏi database
Soft delete  → đánh dấu deleted_at, lọc ra khỏi query thông thường
```

Không tự ý áp dụng soft delete nếu chưa được chốt trong domain model của entity đó.

Nếu dùng soft delete:

- thêm column `deleted_at TIMESTAMP` (nullable);
- mọi query thông thường phải lọc `WHERE deleted_at IS NULL`;
- repository không được trả về record đã bị soft delete nếu không có yêu cầu rõ ràng;
- cân nhắc dùng `@Where(clause = "deleted_at IS NULL")` hoặc Hibernate Filter nếu phù hợp.

Không để soft delete entity "rò rỉ" ra API dưới dạng record bình thường.

---

# 5. DTO, MAPPING & API CONTRACT

## 5.1. Không trả Entity trực tiếp

Luồng:

```text
Entity
  ↓
Mapper
  ↓
Response DTO
  ↓
JSON
```

Request:

```text
JSON
  ↓
Request DTO
  ↓
Service
  ↓
Entity
```

---

## 5.2. DTO theo use case

Không tạo một DTO khổng lồ cho mọi API.

Ví dụ:

```text
CreateTripRequest
UpdateTripRequest
TripSummaryResponse
TripDetailResponse
```

Response chỉ trả những dữ liệu cần thiết.

---

## 5.3. Mapper

Ưu tiên MapStruct nếu project đã sử dụng.

Nếu mapper đơn giản, manual mapping cũng chấp nhận được.

Không tạo abstraction phức tạp chỉ để map một vài field.

---

# 6. VALIDATION

Phân biệt:

### Input validation

Dùng Bean Validation:

```java
@NotBlank
@NotNull
@Email
@Size
@Min
@Max
@Past
@Future
```

### Business validation

Đặt ở Service/domain layer.

Ví dụ:

```text
endDate < startDate
```

không phải chỉ là syntax validation mà là business rule.

---

## 6.1. Không tin dữ liệu từ AI

AI output phải validate giống external input.

Ví dụ:

- địa điểm có tồn tại không;
- place ID có thuộc Trip không;
- thứ tự route có hợp lệ không;
- không duplicate place;
- thời gian có nằm trong trip range không;
- giá trị số có hợp lệ không.

---

# 7. EXCEPTION & ERROR RESPONSE

Toàn bộ REST error đi qua:

```java
@RestControllerAdvice
```

Không để stack trace xuất hiện trong response production.

Nên phân biệt:

```text
400 Bad Request
401 Unauthorized
403 Forbidden
404 Not Found
409 Conflict
422 Unprocessable Entity
429 Too Many Requests
500 Internal Server Error
502/503 External Service Error
```

Chỉ dùng status code khi phù hợp với semantics.

Error response phải thống nhất toàn backend.

Ví dụ:

```json
{
  "success": false,
  "code": "TRIP_NOT_FOUND",
  "message": "Trip not found",
  "timestamp": "..."
}
```

Không trả:

- SQL;
- stack trace;
- class name;
- internal path;
- secret;
- provider credentials.

---

# 8. SECURITY

## 8.1. Authentication

Dùng:

```text
Spring Security
    +
JWT
```

JWT dùng cho API authentication.

Không dùng session-based login page mặc định của Spring Security cho REST API.

---

## 8.2. Password

Password phải:

```text
plaintext
    ↓
BCrypt
    ↓
database
```

Không bao giờ lưu plaintext.

Không log password.

Không trả password hash trong DTO.

---

## 8.3. Authorization

Nguyên tắc:

```text
deny by default
```

Public endpoint phải được liệt kê rõ ràng.

Ví dụ:

```text
POST /api/v1/auth/register
POST /api/v1/auth/login
GET  /api/v1/health
```

Các endpoint còn lại mặc định yêu cầu authentication nếu business rule yêu cầu.

Không mở:

```java
.anyRequest().permitAll()
```

trong production.

---

## 8.4. Ownership

Các resource cá nhân phải kiểm tra ownership.

Ví dụ:

```text
User A
   ↓
Trip A
```

User B không được lấy:

```text
GET /api/v1/trips/{tripA}
```

chỉ bằng cách biết ID.

Authorization phải được kiểm tra ở Service/domain boundary, không chỉ dựa vào frontend.

---

## 8.5. CORS

CORS phải cấu hình theo environment.

Development có thể cho phép origin local đã biết.

Production chỉ cho phép origin chính thức của:

- Next.js Web;
- các origin cần thiết khác.

Không dùng:

```text
Access-Control-Allow-Origin: *
```

cho authenticated production API.

---

# 9. EXTERNAL API & INTEGRATION

Project có thể tích hợp:

```text
Places API
Map / Routing API
Weather API
AI API
Media Storage
```

Không gọi trực tiếp provider từ Controller.

Kiến trúc:

```text
Controller
    ↓
Service
    ↓
Provider interface
    ↓
Provider implementation
    ↓
External API
```

Ví dụ:

```text
WeatherService
    ↓
WeatherProvider
    ↓
OpenWeather / provider khác
```

Điều này giúp:

- test dễ hơn;
- thay provider dễ hơn;
- tránh coupling;
- kiểm soát retry/timeout/error.

---

## 9.1. Timeout

External API phải có:

- connect timeout;
- read/response timeout;
- giới hạn retry;
- xử lý failure.

Không retry vô hạn.

---

## 9.2. External API failure

Nếu Weather API chết:

```text
Weather unavailable
```

không được làm toàn bộ Trip API crash nếu weather chỉ là advisory feature.

Phải phân biệt:

```text
critical dependency
vs
optional/advisory dependency
```

---

# 10. AI INTEGRATION RULES

AI output phải được coi là dữ liệu không đáng tin cậy.

Không dùng AI response trực tiếp để:

```text
SQL
    hoặc
Entity mutation
```

mà không validate.

Ưu tiên structured output/schema rõ ràng.

Ví dụ:

```text
AI
 ↓
Structured Recommendation DTO
 ↓
Validation
 ↓
Business rules
 ↓
User confirmation
 ↓
Apply
```

AI recommendation không đồng nghĩa system command.

---

# 11. HELD-KARP — QUY TẮC ĐẶC BIỆT

Held-Karp là phần thuật toán có giá trị học thuật của đồ án.

Agent phải:

- tách thuật toán khỏi Controller;
- tách thuật toán khỏi persistence;
- viết unit test độc lập;
- giải thích rõ input/output;
- kiểm tra edge cases;
- không để AI thay thế thuật toán;
- không dùng API external để "giả vờ" giải bài toán.

Luồng định hướng:

```text
Trip places
    ↓
Coordinates / route cost
    ↓
Cost matrix
    ↓
Held-Karp
    ↓
Optimal ordering
    ↓
AI contextual refinement/recommendation
    ↓
User
```

Agent phải phân biệt:

```text
distance optimization
```

với:

```text
contextual recommendation
```

Không được gọi AI là "thuật toán tối ưu" nếu AI chỉ đang diễn giải/cá nhân hóa.

---

# 12. API DESIGN

API dùng REST.

Base path:

```text
/api/v1
```

Resource dùng noun.

Ví dụ:

```text
POST   /api/v1/auth/register
POST   /api/v1/auth/login

GET    /api/v1/trips
POST   /api/v1/trips
GET    /api/v1/trips/{tripId}
PATCH  /api/v1/trips/{tripId}
DELETE /api/v1/trips/{tripId}

GET    /api/v1/places/{placeId}
```

Không tạo endpoint kiểu:

```text
/getTrips
/createTrip
/doOptimization
```

nếu không có lý do đặc biệt.

---

## 12.1. HTTP status

Ưu tiên:

```text
200 OK
201 Created
204 No Content

400 Bad Request
401 Unauthorized
403 Forbidden
404 Not Found
409 Conflict
422 Unprocessable Entity

500 Internal Server Error
502 Bad Gateway
503 Service Unavailable
```

Không trả `200 OK` cho mọi tình huống.

---

## 12.2. Pagination

Mọi API trả danh sách phải hỗ trợ pagination.

Ưu tiên Spring Data `Pageable`:

```text
GET /api/v1/trips?page=0&size=20&sort=createdAt,desc
```

Response phải bao gồm metadata:

```json
{
  "success": true,
  "data": {
    "content": [...],
    "page": 0,
    "size": 20,
    "totalElements": 100,
    "totalPages": 5
  }
}
```

Quy tắc:

- không trả toàn bộ collection nếu không có lý do đặc biệt;
- không hard-code `size` mặc định quá lớn (> 100);
- không để client truyền `size` tùy ý mà không có giới hạn tối đa;
- phải validate `page >= 0` và `size > 0`.

Nếu một danh sách thực sự nhỏ và cố định (ví dụ: danh sách 5 loại phương tiện), có thể trả toàn bộ nhưng phải có quyết định rõ ràng.

---

# 13. API DOCUMENTATION

Dùng Springdoc OpenAPI.

API public phải có documentation rõ:

- summary;
- description;
- request DTO;
- response DTO;
- error responses;
- authentication requirement;
- example nếu cần.

Không bắt buộc viết annotation dài dòng nếu schema đã tự mô tả rõ.

Documentation phải phản ánh API thực tế, không được để Swagger mô tả khác implementation.

---

# 14. LOGGING & OBSERVABILITY

Dùng:

```java
@Slf4j
```

Không dùng:

```java
System.out.println()
```

Log phải hữu ích cho debugging.

Không log:

- password;
- JWT;
- API key;
- full personal data;
- raw AI prompt chứa dữ liệu nhạy cảm;
- raw external provider credentials.

---

## 14.1. Log levels

```text
DEBUG
    ↓
development diagnostics

INFO
    ↓
important application flow

WARN
    ↓
unexpected but handled situation

ERROR
    ↓
real failure
```

Không log mọi method entry/exit một cách máy móc.

Chỉ log ở những boundary/flow cần thiết.

---

# 15. CONFIGURATION & ENVIRONMENT

## 15.1. Không hard-code secret

Không commit:

```text
JWT_SECRET
DATABASE_PASSWORD
AI_API_KEY
MAP_API_KEY
WEATHER_API_KEY
CLOUDINARY_SECRET
```

Secret phải đến từ environment/secret manager.

---

## 15.2. Profiles

Project sử dụng:

```text
application.yml
application-dev.yml
application-test.yml
application-prod.yml
```

Không đưa production secret vào source.

---

## 15.3. `@ConfigurationProperties`

Khi có nhóm cấu hình:

```text
AI
Map
Weather
Storage
JWT
```

ưu tiên:

```java
@ConfigurationProperties
```

thay vì rải nhiều:

```java
@Value(...)
```

---

# 16. TESTING

Testing không phải phần làm sau cùng.

## 16.1. Unit test

Dùng:

```text
JUnit 5
Mockito
```

Tập trung mạnh vào:

- Service;
- business rules;
- Held-Karp;
- validators;
- mapper có logic;
- AI output validation.

---

## 16.2. Controller test

Dùng:

```text
@WebMvcTest
MockMvc
```

kiểm tra:

- request binding;
- validation;
- status code;
- response structure;
- security behavior khi phù hợp.

---

## 16.3. Integration test

Dùng:

```text
@SpringBootTest
Testcontainers
PostgreSQL
```

cho các flow cần database thật.

Ví dụ:

```text
Create User
    ↓
PostgreSQL

Create Trip
    ↓
PostgreSQL

Memory
    ↓
Place
    ↓
Trip
```

Không mock database trong integration test.

---

## 16.4. Test external API

Không gọi API thật trong unit test.

Dùng mock/fake/stub provider.

Integration test external provider chỉ thực hiện khi task yêu cầu rõ ràng.

---

## 16.5. Coverage

Không coi coverage là mục tiêu duy nhất.

Ưu tiên:

```text
meaningful tests
>
coverage percentage
```

Không được viết test vô nghĩa để đạt một con số coverage.

Có thể đặt target khoảng **80% cho business-critical service**, nhưng agent không được hy sinh chất lượng test để đạt target.

---

# 17. PERFORMANCE

Không tối ưu trước khi có evidence.

Khi có vấn đề:

1. đo;
2. xác định bottleneck;
3. tối ưu;
4. đo lại.

Đặc biệt với JPA:

- kiểm tra N+1;
- số lượng query;
- pagination;
- fetch strategy;
- index;
- query plan khi cần.

Không giải quyết performance bằng cách:

```text
EAGER everything
```

---

# 18. TRANSACTION & DATA INTEGRITY

Các operation có nhiều bước ghi dữ liệu phải đảm bảo atomicity.

Ví dụ:

```text
Create Trip
  ↓
Create Trip Places
  ↓
Create itinerary
```

Nếu business requirement yêu cầu tất cả thành công hoặc tất cả rollback:

```java
@Transactional
```

Không dùng transaction để bao toàn bộ external API call nếu không cần thiết.

Đặc biệt:

```text
Database transaction
    ≠
External API transaction
```

Không giả định có distributed transaction khi thực tế không có.

---

# 19. CONCURRENCY & IDEMPOTENCY

Các API có khả năng bị gọi lại phải xem xét idempotency.

Ví dụ:

- upload;
- create resource;
- apply recommendation;
- generate itinerary.

Không tạo duplicate chỉ vì client retry một request.

Nếu một operation có nguy cơ race condition, phải xem xét:

- unique constraint;
- optimistic locking;
- transaction isolation;
- atomic database operation.

Không giải quyết concurrency chỉ bằng Java `synchronized` nếu resource thực tế nằm trong database.

---

# 20. FILE / IMAGE / MEMORY

Memory có thể chứa:

```text
Image
Note
Created time
Place
Trip
```

File upload phải kiểm tra:

- MIME type;
- extension;
- file size;
- upload error;
- storage result.

Không tin extension do client gửi.

Không lưu file binary lớn trực tiếp vào database nếu kiến trúc storage đã tách media storage.

Không log raw file contents.

---

# 21. AI / WEATHER / MAP — FALLBACK

Các integration advisory không nên làm hệ thống mất toàn bộ chức năng khi provider lỗi.

Ví dụ:

```text
Weather API unavailable
        ↓
Trip vẫn tồn tại
        ↓
Weather field = unavailable
```

Tương tự với AI:

```text
AI unavailable
        ↓
Algorithmic route vẫn có thể hoạt động
        ↓
User vẫn có thể chỉnh itinerary
```

Đây là nguyên tắc **graceful degradation**.

---

# 22. GIT WORKFLOW

Commit message theo Conventional Commits:

```text
feat:
fix:
refactor:
test:
docs:
chore:
perf:
build:
```

Ví dụ:

```text
feat: add trip creation API
fix: handle duplicate trip place
test: add held-karp solver tests
refactor: extract route cost service
docs: update API specification
chore: configure postgres docker
```

---

## 22.1. Không commit

Không commit:

```text
.env
target/
.idea/
*.iml
logs/
```

và các secret/local artifact khác.

---

## 22.2. Agent không tự ý destructive Git command

Không tự chạy:

```text
git reset --hard
git push --force
git clean -fd
```

nếu chưa có sự cho phép rõ ràng.

---

## 22.3. Không sửa hàng loạt

Nếu phát hiện technical debt:

```text
phát hiện
  ↓
báo cáo
  ↓
đề xuất
  ↓
chỉ refactor khi task cho phép
```

Không tự tiện refactor toàn repository trong một feature task.

---

# 23. QUY TRÌNH LÀM VIỆC CHUẨN CỦA AI AGENT

Mỗi task nên đi theo:

```text
1. Đọc AGENTS.md (nên lưu vào context của phiên làm việc và nhớ nó, tránh đọc lại nhiều lần gây tốn token)
        ↓
2. Hiểu yêu cầu
        ↓
3. Kiểm tra docs/code hiện có
        ↓
4. Xác định affected modules
        ↓
5. Nêu assumption nếu có
        ↓
6. Lập implementation plan ngắn
        ↓
7. Code
        ↓
8. Format / compile
        ↓
9. Test
        ↓
10. Review diff
        ↓
11. Kiểm tra security
        ↓
12. Tóm tắt kết quả
```

Không cần hỏi người dùng để xác nhận những bước kỹ thuật hiển nhiên và an toàn.

---

## 23.1. Token Efficiency

Agent phải sử dụng token một cách có chủ ý.

Agent **không được**:

- đọc lại `AGENTS.md` nhiều lần trong cùng một phiên làm việc — đọc một lần, lưu vào context, dùng lại;
- đọc toàn bộ file lớn khi chỉ cần một phần cụ thể;
- lặp lại nguyên văn nội dung lớn từ file/codebase trong câu trả lời;
- tạo file tạm, file debug, file scaffold không cần thiết rồi bỏ đó;
- gọi tool tìm kiếm/đọc file cho thông tin đã có trong context hiện tại.

Agent **nên**:

- dùng `grep`/`search` để định vị chính xác trước khi đọc toàn file;
- chỉ đọc những file thực sự liên quan đến task hiện tại;
- đọc theo range (StartLine/EndLine) khi chỉ cần một phần của file;
- tận dụng thông tin đã có trong context thay vì re-fetch.

---

# 24. DEFINITION OF DONE

Một task chỉ được báo **completed** khi phù hợp với task và đã kiểm tra:

### Code

- [ ] Implementation hoàn chỉnh
- [ ] Không còn stub/TODO cho phần bắt buộc
- [ ] Không duplicate logic rõ ràng
- [ ] Đúng package/module
- [ ] Đúng naming convention

### Architecture

- [ ] Không phá vỡ dependency direction
- [ ] Controller không chứa business logic
- [ ] Entity không trả trực tiếp ra API
- [ ] External integration đi qua service/client boundary phù hợp

### Validation & Security

- [ ] Request được validate
- [ ] Business rule được kiểm tra
- [ ] Authorization/ownership được kiểm tra nếu cần
- [ ] Không có secret hard-code
- [ ] Không log sensitive data

### Database

- [ ] Transaction đúng chỗ
- [ ] Relationship/fetch strategy hợp lý
- [ ] Không tạo N+1 rõ ràng
- [ ] Constraint/index cần thiết đã xem xét

### API

- [ ] HTTP method đúng
- [ ] HTTP status đúng
- [ ] Response contract đúng
- [ ] Swagger/documentation cập nhật

### Testing

- [ ] Unit test cho business logic quan trọng
- [ ] Edge cases đã được xem xét
- [ ] Error cases đã được test
- [ ] Integration test khi task chạm DB/critical flow
- [ ] Test thực sự đã chạy

### Verification

- [ ] Build/compile thành công
- [ ] Test pass
- [ ] Git diff đã được review

---

# 25. BÁO CÁO SAU MỖI TASK

Agent phải kết thúc task bằng format tương tự:

```text
## Summary

### Changed
- ...

### Why
- ...

### Files
- `...`
- `...`

### API / DB Impact
- ...

### Tests
- `...` → PASS
- `...` → PASS

### Security
- ...

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

# 26. QUY TẮC KHI CÓ MÂU THUẪN

Nếu agent phát hiện mâu thuẫn giữa:

```text
Code
vs
Documentation
```

không được tự chọn ngẫu nhiên.

Phải:

1. xác định nguồn nào mới hơn;
2. kiểm tra task hiện tại;
3. kiểm tra quyết định architecture đã chốt;
4. nêu conflict;
5. đề xuất hướng xử lý.

Ví dụ:

```text
Docs: dùng Flyway
Code: đang dùng Hibernate ddl-auto
```

Agent phải báo rõ conflict thay vì âm thầm chuyển toàn bộ project sang Flyway.

---

# 27. CHECKLIST NGẮN TRƯỚC KHI VIẾT CODE

Agent phải tự hỏi:

```text
[ ] Tôi đã hiểu rõ quy tắc code trong AGENTS.md chưa?
[ ] Tôi đã kiểm tra code hiện tại chưa?
[ ] Tôi đã kiểm tra tài liệu/domain rule liên quan chưa?
[ ] Tôi biết module nào bị ảnh hưởng chưa?
[ ] Tôi có đang tự bịa business rule không?
[ ] Tôi có đang thay đổi architecture ngoài phạm vi task không?
[ ] Input đã được validate chưa?
[ ] Security/ownership đã được kiểm tra chưa?
[ ] Entity có bị trả trực tiếp không?
[ ] External API có timeout/error handling không?
[ ] Database operation có cần transaction không?
[ ] Có test phù hợp không?
[ ] Tôi có thực sự chạy test/build chưa?
```

---

# 28. QUY TẮC ĐẶC BIỆT CHO ĐỒ ÁN

Đây không chỉ là production application; đây còn là **đồ án tốt nghiệp có yêu cầu phản biện học thuật**.

Vì vậy agent phải ưu tiên:

```text
Implementation correctness
        +
Architecture clarity
        +
Algorithm transparency
        +
Traceability
        +
Testability
```

Đặc biệt đối với Held-Karp:

- phải có implementation rõ ràng;
- có input/output rõ;
- có test;
- có complexity discussion trong docs;
- không che giấu thuật toán phía sau AI/API;
- có thể giải thích khi phản biện.

AI chỉ là một layer hỗ trợ personalization/contextual recommendation, không được dùng để che khuất phần thuật toán mà đồ án cần chứng minh.

---

# 29. NGUYÊN TẮC CUỐI CÙNG

> **AI Agent là người hỗ trợ triển khai, không phải người tự quyết định kiến trúc và business rule của dự án.**

Agent phải:

```text
Understand
    ↓
Inspect
    ↓
Plan
    ↓
Implement
    ↓
Test
    ↓
Verify
    ↓
Explain
```

Không:

```text
Guess
    ↓
Code
    ↓
Assume it works
```

Mục tiêu cuối cùng là tạo ra backend:

- đúng nghiệp vụ;
- dễ mở rộng;
- dễ kiểm thử;
- an toàn;
- có thể giải thích;
- phù hợp với kiến trúc đã thiết kế;
- và đủ rõ ràng để phục vụ cả **triển khai thực tế lẫn phản biện đồ án**.
