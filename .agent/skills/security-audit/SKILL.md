---
name: security-audit
description: >-
  Automated multi-phase security audit skill inspired by Cloudflare security best practices.
  Use this skill when reviewing code, endpoints, authentication, authorization, or assessing
  security vulnerabilities in backend services, including IDOR, JWT validation, SQL/JPQL
  injection, CORS misconfigurations, and sensitive data leakage.
---

# Multi-Phase Security Audit Skill (Cloudflare Standard)

Bộ kỹ năng kiểm toán bảo mật đa tầng độc lập, cung cấp phương pháp rà soát an ninh hệ thống và tạo báo cáo kiểm toán có cấu trúc rõ ràng.

## Quy trình Kiểm toán 4 Giai đoạn (4-Phase Audit Workflow)

### Giai đoạn 1: Xác thực & Phân quyền (Authentication & Authorization)
1. **Kiểm tra JWT Filter & Token Lifecycle:**
   - Token signature, thuật toán ký (HMAC-SHA256/RSA), và thời hạn sống (`expiration`).
   - Kiểm tra xem token có được xác thực trong mọi request không (`JwtAuthenticationFilter`).
   - Cơ chế xử lý khi token hết hạn hoặc bị thu hồi (Blacklist/Revocation nếu có).
2. **Rà soát Whitelist & Endpoint Exposure:**
   - Soi xét kỹ các URL trong `WHITE_LIST` (ví dụ: `/api/v1/public/**`, `/actuator/**`).
   - Đảm bảo các route nhạy cảm (thay đổi trạng thái, thanh toán, thông tin cá nhân) **KHÔNG** bị lọt vào whitelist.
3. **Phân quyền Phương thức (Method Security):**
   - Đảm bảo các controller quản trị hoặc tác vụ nhạy cảm có `@PreAuthorize("hasRole('ADMIN')")` hoặc tương đương.

### Giai đoạn 2: Kiểm soát Truy cập & Lỗ hổng IDOR (Access Control & Insecure Direct Object References)
1. **Kiểm tra quyền sở hữu tài nguyên (Resource Ownership):**
   - Khi API nhận ID tài nguyên (ví dụ: `tripId`, `userId`, `itineraryId`), hệ thống **BẮT BUỘC** phải xác minh tài nguyên đó thuộc về người dùng đang đăng nhập (`currentUser.getId()`).
   - Tuyệt đối không cho phép User A sửa/xóa/đọc dữ liệu cá nhân của User B chỉ bằng cách đoán `UUID`/`ID`.
2. **Tác vụ Quản trị (Admin Actions):**
   - Mọi hành vi cập nhật quyền, khóa tài khoản hoặc can thiệp dữ liệu phải ghi nhận audit log rõ ràng.

### Giai đoạn 3: Phòng chống Injection & Xác thực Đầu vào (Input Validation & Injection Prevention)
1. **SQL / JPQL Injection:**
   - Tuyệt đối không cộng chuỗi (string concatenation) trong truy vấn động.
   - Luôn sử dụng Parameterized Query, JPA Query Method hoặc Hibernate Criteria / Specifications.
2. **Xác thực DTO & Sanitization:**
   - Các trường đầu vào phải có annotation kiểm tra (`@NotBlank`, `@NotNull`, `@Size`, `@Min`, `@Max`).
   - Các trường tọa độ (`latitude`, `longitude`) phải kiểm tra giới hạn hợp lệ ($-90 \le \text{lat} \le 90$, $-180 \le \text{lon} \le 180$).
   - Sử dụng `@Valid` ở Controller để kích hoạt xác thực tự động.
3. **XSS & HTML/Script Escaping:**
   - Làm sạch dữ liệu hiển thị (sanitization) đối với các trường text do người dùng nhập (như tên chuyến đi, ghi chú địa điểm).

### Giai đoạn 4: Bảo vệ Dữ liệu & Xử lý Ngoại lệ (Data Protection & Error Handling)
1. **Che giấu Thông tin Nhạy cảm (Information Disclosure):**
   - Không log thông tin bí mật (mật khẩu, JWT secret key, API key thời tiết/AI).
   - Global Exception Handler không được trả về raw stack trace hoặc tên bảng CSDL cho client ở môi trường production.
2. **Cấu hình CORS & Headers:**
   - CORS chỉ cho phép các domain tin cậy (tránh `allowCredentials: true` đi kèm `allowedOrigins: "*"`).
   - Đảm bảo cấu hình HTTP Security Headers cần thiết (X-Content-Type-Options, X-Frame-Options).

---

## Định dạng Báo cáo Kiểm toán (Audit Report Format)

Mỗi khi thực hiện kiểm toán, xuất báo cáo theo bảng phân loại mức độ rủi ro:

| Mức độ | Lỗ hổng | Vị trí (File & Dòng) | Rủi ro tiềm ẩn | Đề xuất khắc phục (Remediation) |
| :---: | :--- | :--- | :--- | :--- |
| 🔴 **CRITICAL** | Lỗ hổng trực tiếp cho phép chiếm quyền / leak toàn bộ dữ liệu | `File.java#L10-20` | Giải thích nguy cơ | Mã code sửa chữa cụ thể |
| 🟠 **HIGH** | IDOR, thiếu phân quyền, token bypass | `File.java#L50` | Nguy cơ thao túng dữ liệu | Cách ràng buộc quyền sở hữu |
| 🟡 **MEDIUM** | Thiếu validation, rò rỉ log hoặc thông tin lỗi | `File.java#L80` | Thu thập thông tin hệ thống | Thêm validation/Mask log |
| 🟢 **LOW / INFO** | Cải thiện cấu hình, best practices | `File.java#L12` | Tối ưu bảo mật | Tinh chỉnh cấu hình |
