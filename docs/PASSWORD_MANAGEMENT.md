# Đổi và đặt lại mật khẩu

## Đổi mật khẩu khi đã đăng nhập

Android: **Cá nhân → Đổi mật khẩu**. Nhập mật khẩu hiện tại, mật khẩu mới và xác nhận. Không gửi email ở bước này. Sau khi thành công, đăng nhập lại trên mọi thiết bị.

```http
PUT /api/v1/users/me/password
Authorization: Bearer <access-token>
Content-Type: application/json

{"currentPassword":"<current-password>","newPassword":"<new-password>"}
```

- Thành công: `204`, không tự cấp phiên mới.
- `400/AUTH_015`: mật khẩu hiện tại sai; `400/AUTH_016`: mật khẩu mới trùng mật khẩu cũ.
- `409/AUTH_017`: tài khoản Google chưa có mật khẩu; dùng Quên mật khẩu để tạo mật khẩu qua email.
- `401`: cần đăng nhập. User lấy từ JWT, không nhận userId từ client.
- Mật khẩu mới từ 8–50 ký tự, tối đa 72 byte UTF-8. Không trim mật khẩu.
- Khóa user giống login/refresh/reset; cập nhật hash BCrypt, tăng sessionVersion, thu hồi mọi refresh token và action token trong một transaction. Sai mật khẩu không thay đổi phiên hoặc tiêu thụ link reset.

## Quên mật khẩu qua email trên Android

Backend có trang `GET /reset-password` và script `GET /auth/password-reset.js`; không cần chạy thêm frontend web. Cấu hình:

```properties
AUTH_RESET_PASSWORD_URL=https://<public-backend-origin>/reset-password
```

Local dùng URL HTTPS của tunnel đang trỏ đúng cổng backend. Android phải truy cập được URL này; localhost trên điện thoại trỏ về điện thoại. Không dùng custom scheme làm link email khi ứng dụng email không hỗ trợ mở app.

Origin HTTP(S) của `AUTH_RESET_PASSWORD_URL` được cho phép riêng tại `/api/v1/auth/reset-password`. Điều này xử lý tunnel/proxy kết thúc TLS rồi chuyển HTTP về backend; không mở thêm origin cho các API khác. Origin ngoài cấu hình vẫn bị chặn.

Khởi động lại backend sau khi đổi cấu hình và yêu cầu **email mới**. Email cũ giữ nguyên URL đã gửi. Khi đổi tunnel, cập nhật URL và gửi link mới.

Trang nhận token dùng một lần qua fragment, giữ trong bộ nhớ và xóa fragment khỏi lịch sử. Chỉ POST `/api/v1/auth/reset-password` khi người dùng xác nhận mật khẩu mới; mở link không tiêu thụ token. Không gửi mật khẩu/token sang dịch vụ ngoài hoặc tự đăng nhập. Link hết hạn/đã dùng được báo rõ; quay lại app yêu cầu email mới.

## Kiểm tra

- `EmailAuthIntegrationTest`: đổi mật khẩu, phiên cũ, link reset cũ, validation, Google-only, request đồng thời và public recovery page.
- `node --test backend/src/test/js/password-reset.test.cjs`: token, xác nhận, UTF-8, link hết hạn và xử lý 204.
- Repo `travel-mobile-ui`: `tests/password-change.test.ts`, auth UI/session/transport tests, `npx tsc --noEmit`, `npm run lint`.

SMTP được mô phỏng trong tests; không gửi thư đến tài khoản thật. Lượt xác minh này dùng database PostgreSQL 18 riêng do Docker/Testcontainers không khả dụng. Chưa nghiệm thu trực tiếp trên điện thoại nếu không có thiết bị kết nối.
