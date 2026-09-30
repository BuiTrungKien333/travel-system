---
name: tech-lead-standards
description: >-
  Enterprise engineering standards and hybrid code review guidelines combining Tech Lead
  best practices with Alibaba's production ruleset. Use this skill when designing APIs,
  refactoring code, implementing algorithms, creating unit tests, and reviewing code for
  Null Pointer Exceptions (NPE), thread-safety, transaction boundaries, and resource leaks.
---

# Enterprise Tech Lead Engineering Standards & Alibaba Code Review Ruleset

Bộ quy chuẩn kỹ thuật toàn diện kết hợp tư duy Tech Lead cao cấp với bộ quy tắc thẩm định mã nguồn thực chiến tại quy mô lớn của Alibaba (Hybrid Static & AI Analysis).

---

## 1. Chuẩn Thiết kế API & DTO (RESTful & Contract Standards)

1. **Giao ước chuẩn hóa (Uniform Response Wrapper):**
   - Mọi response API phải bọc trong `ApiResponse<T>` với các trường: `success` (boolean), `message` (string), `data` (T).
   - Mã lỗi (Error response) phải bọc trong `ErrorResponse` và sử dụng enum `ErrorCode` định danh rõ ràng kèm mã HTTP phù hợp.
2. **DTO Bất biến (Immutability):**
   - Sử dụng **Java 21 Record** cho Request DTOs, Response DTOs và Value Objects.
   - Thêm annotation OpenAPI/Swagger (`@Schema`, `@Operation`, `@Parameter`) đầy đủ để tài liệu hóa API tự động.
3. **Idempotency & Phân trang (Pagination):**
   - Các API truy vấn danh sách phải luôn hỗ trợ phân trang (`page`, `size`, `sort`) hoặc giới hạn số lượng kết quả (limit), tuyệt đối không trả về unbounded lists.

---

## 2. Bộ Quy tắc Review Cốt lõi của Alibaba (Alibaba Critical Review Ruleset)

### 🔴 Quy tắc 1: Phòng chống NullPointerException (NPE Prevention)
- **Tuyệt đối không:** Gọi method trực tiếp trên object không chắc chắn khác null (ví dụ: `obj.getName().toLowerCase()`).
- **Bắt buộc:**
  - Dùng `Optional<T>` cho giá trị trả về có thể rỗng (ví dụ: `findForecastAt`, `findByEmail`).
  - Dùng `Objects.requireNonNull(val, "message")` hoặc fail-fast validation tại constructor.
  - Sử dụng Safe Navigation hoặc kiểm tra trước: `if (obj != null && obj.getField() != null)`.
  - Trong Java Records: Tận dụng Compact Constructor để validate và gán giá trị mặc định an toàn.

### 🔴 Quy tắc 2: An toàn Đa luồng (Thread-Safety & Concurrency in Spring)
- **Spring Beans là Singleton:**
  - Tuyệt đối không lưu trạng thái mutable của request trong field của `@Service`, `@Component`, `@Controller`.
  - Không sử dụng `SimpleDateFormat` (không an toàn đa luồng); bắt buộc dùng `java.time.*` (`DateTimeFormatter`, `LocalDateTime`, `LocalDate`).
  - Nếu cần cache nội bộ trong process: Sử dụng `ConcurrentHashMap` hoặc thư viện Cache chuyên dụng (Caffeine/Redis), không dùng raw `HashMap`.

### 🔴 Quy tắc 3: Ranh giới Giao dịch & CSDL (Transactional Boundaries & Query Performance)
- **Tránh Transaction quá dài (Long Transactions):**
  - Tuyệt đối không gọi API bên ngoài (như gọi Open-Meteo Weather API hoặc Gemini AI) bên trong method có `@Transactional`.
  - Phải tách riêng: Gọi API ngoài trước $\rightarrow$ Thu thập dữ liệu $\rightarrow$ Mở transaction ngắn để lưu CSDL.
- **N+1 Query & Lazy Loading:**
  - Kiểm tra các quan hệ `@OneToMany`, `@ManyToOne`. Sử dụng `JOIN FETCH`, EntityGraph hoặc DTO Projection khi truy vấn danh sách để triệt tiêu lỗi N+1 queries.

### 🔴 Quy tắc 4: Quản lý Tài nguyên (Resource Leak Prevention)
- Mọi tài nguyên I/O, File, Stream, DB Connection phải sử dụng cú pháp `try-with-resources`.
- Các HTTP client (như `RestClient`): Phải luôn cấu hình `connectTimeout` và `readTimeout` rõ ràng, không dùng timeout vô hạn mặc định.

---

## 3. Tiêu chuẩn Viết Kiểm thử Tự động (Unit Test Standards)

1. **Cấu trúc AAA (Arrange - Act - Assert):**
   - Mỗi test case phải phân tách rõ ràng 3 pha:
     - `Arrange`: Chuẩn bị mock data và stubbing (`when(...).thenReturn(...)`).
     - `Act`: Gọi phương thức cần kiểm thử.
     - `Assert`: Dùng `assertThat(...)` từ AssertJ để kiểm tra điều kiện đầu ra.
2. **Bao phủ Ca biên (Boundary & Edge Case Coverage):**
   - Luôn viết test cho: Input rỗng/null, tọa độ cực đoan, lỗi kết nối mạng (500/timeout), danh sách chỉ có 1 phần tử hoặc không có phần tử nào.
3. **Tiêu chí Zero Flakiness (Không phụ thuộc môi trường ngoài):**
   - Unit test không được phụ thuộc vào Internet thật: Luôn dùng `MockRestServiceServer` hoặc Mock Provider để test HTTP calls.
   - Luôn dọn dẹp mock và verify tương tác (`mockServer.verify()`, `verify(mock).method()`).

---

## 4. Kiến trúc Sạch & Tính Mở rộng (Clean Architecture & Extensibility)

1. **Strategy + Factory Pattern cho tích hợp bên thứ ba:**
   - Khi tích hợp dịch vụ bên ngoài (Weather, AI, Maps), luôn tạo Interface chuẩn hợp đồng (`WeatherProvider`, `AiModelProvider`) $\rightarrow$ Implementation độc lập $\rightarrow$ Factory tự động chọn Bean theo config.
2. **Khai thác tối đa Java 21 Hiện đại:**
   - Record patterns, pattern matching for `switch`, sealed interfaces, text blocks, immutable collections (`List.of()`, `Set.of()`).
