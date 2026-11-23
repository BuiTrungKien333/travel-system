# WeGo Admin — giai đoạn nền tảng

Ứng dụng Next.js độc lập tại `frontend-web/admin`, chạy ở `/admin` trên port 3001. Không import API client, mock data, auth store hoặc layout của web cũ. Web cũ được giữ nguyên; tsconfig của web cũ loại trừ thư mục ứng dụng con này.

## Phạm vi hoàn thành

- Đăng nhập tài khoản ADMIN thật, xác minh role và permission hiện hành từ backend.
- Cookie HttpOnly, SameSite=Strict; Secure khi dùng HTTPS. Không lưu access/refresh token vào localStorage, sessionStorage, Zustand hay response JSON cho browser.
- BFF Route Handlers chỉ gọi các đường dẫn backend được cho phép, không có proxy tùy ý; giới hạn body, timeout, không theo redirect và trả lỗi đã lọc.
- Kiểm tra Origin trên login/refresh/logout/PATCH để chống CSRF. API phiên và dữ liệu trả no-store.
- Refresh có single-flight trong tab, Web Locks giữa các tab và kiểm tra access còn hợp lệ trước khi xoay refresh. BroadcastChannel đồng bộ đăng nhập/đăng xuất giữa tab. Trình duyệt cần hỗ trợ Web Locks để đảm bảo đồng bộ refresh giữa tab.
- Layout responsive, sidebar, hồ sơ admin, đăng xuất, loading/error/empty states.
- Trang tài khoản: tìm tên/email, phân trang, xem role/trạng thái/xác minh email, khóa/mở khóa, đổi role và hộp xác nhận. Không tự thay đổi quyền/khóa mình. Email chưa xác minh không được chọn ADMIN.
- Mutation không retry khi lỗi mạng/403/409. Version conflict không tự ghi đè; phải đóng hộp thoại, tải lại và xác nhận dữ liệu mới.

Dashboard và hai trang catalog/RAG đã được bổ sung ở đợt tiếp theo; dữ liệu mẫu cho biểu đồ và provider luôn có nhãn, không được dùng cho thao tác ghi.

## Chạy local

Backend phải chứa endpoint mới `GET /api/v1/admin/session`, bên cạnh `/admin/users` và `PATCH /admin/users/{id}`. Nếu dùng JAR cũ, cần đóng gói và khởi động lại backend.

```powershell
# Terminal 1, từ E:/travel-system — cần root .env của dự án
./scripts/Start-Dev.ps1 -UsePackagedJar

# Terminal 2
cd E:/travel-system/frontend-web/admin
npm ci
Copy-Item .env.example .env.local  # Chỉ khi chưa có .env.local
npm run dev
```

Mở **http://localhost:3001/admin/login**. Dùng tài khoản quản trị đã được cấp quyền và xác minh email trên chính backend đang kết nối. Không tự tạo, nâng quyền hoặc thay mật khẩu tài khoản thật. USER/GUEST bị từ chối và refresh token mới cấp khi thử đăng nhập sai role được thu hồi.

`.env.local` chỉ có cấu hình server:

```dotenv
BACKEND_URL=http://127.0.0.1:18081
ADMIN_ORIGIN=http://localhost:3001
```

`ADMIN_ORIGIN` phải khớp origin mở trong browser, không gồm `/admin`. Không dùng `NEXT_PUBLIC_*` cho token, mật khẩu hoặc URL cấu hình server này. Ở production cần khai báo cả hai biến; origin public và backend ngoài loopback phải dùng HTTPS. BFF chạy cùng origin với giao diện, không cần mở CORS backend cho browser admin.

Build/chạy production local:

```powershell
npm run build
npm start
```

## API bổ sung cho backend

`GET /api/v1/admin/session` yêu cầu ADMIN, trả `id`, `fullName`, `email`, `role`, `permissions` trong envelope chuẩn. Không trả token/hash mật khẩu. Role và permission vẫn lấy từ database qua JWT filter mỗi request. UI chỉ mở quản lý tài khoản khi có `admin:users`; backend tiếp tục quyết định quyền cho mọi thao tác.

Các API BFF dưới `/admin/api`:

- POST `/auth/login`, GET `/auth/session`, POST `/auth/refresh`, POST `/auth/logout`.
- GET `/users?q=&page=0&size=20`, PATCH `/users/{id}`.

Logout xóa cookie dù backend tạm không phản hồi và hiển thị thông báo không xác nhận được revocation. Nếu browser không tới được BFF, báo lỗi và cho phép thử lại. Backend hiện thu hồi refresh token khi logout; access JWT đã phát hành hết hiệu lực theo TTL hiện có. Khóa/đổi role tài khoản tăng sessionVersion và vô hiệu hóa cả access/refresh cũ ngay ở request tiếp theo.

## Kiểm tra

```powershell
npm run type-check
npm run lint
npm test
npm run test:e2e
npm run build
```

- Unit: BFF cookies/CSRF, giới hạn body, upstream allowlist, validation, version conflict, refresh, logout và các cuộc đua client.
- Browser E2E: Edge headless đã cài trên máy Windows. Tests tự mở Next ở port 3101 và upstream fixture loopback ở 19091; không kết nối database thật hoặc Gemini. Muốn dùng browser khác, chỉnh `channel` trong playwright.config.ts.
- Fixture chỉ tồn tại trong `tests/upstream-fixture.ts`, không được import trong ứng dụng. `npm run dev`/`npm start` luôn gọi backend thật đã cấu hình.
- Backend: `AuthorizationIntegrationTest` dùng PostgreSQL/JWT thật, kiểm tra phiên admin và hồi quy đổi quyền, khóa, truy cập chéo.

Kết quả cập nhật: 16 unit test, 11 browser E2E, 24 test phân quyền/thống kê và 5 test RAG backend đạt. Đã xem ảnh chụp desktop/màn hình hẹp. TypeScript, lint và production build được kiểm tra riêng trong thư mục admin; không khẳng định web cũ đã hết lỗi.

Trước khi đưa public cần triển khai HTTPS và giới hạn login tại gateway theo hạ tầng của hệ thống. Không nhúng credential trong giao diện. Số liệu mẫu minh họa được ghi nhãn riêng.

## Dashboard và catalog (bổ sung)

Menu có Dashboard, Quản lý user, Quản lý địa điểm, Quản lý kho tri thức AI. Dashboard lọc 7/30/90 ngày; tăng trưởng, top địa điểm/tỉnh thành tính theo chuyến tạo trong kỳ. Trạng thái đã lưu của chuyến, tags và RAG là snapshot hiện tại. Thống kê giữ dữ liệu seed trong database dev; không tự coi các tài khoản này là người dùng thật ngoài môi trường dev.

API mới: GET `/api/v1/admin/overview?days=30` yêu cầu `admin:system:read`; GET `/api/v1/admin/knowledge?page=0&size=20` yêu cầu `admin:catalog`. Truy vấn dùng transaction repeatable-read, múi giờ Việt Nam, không trả tên chuyến hay nội dung riêng tư. RAG đối chiếu cùng model/hash với retrieval; schema chưa bật pgvector vẫn trả catalog với indexAvailable=false.

Dữ liệu mẫu: chỉ các biểu đồ rỗng dùng mẫu có badge rõ ràng. Các thẻ số lượng và chuỗi thời gian giữ dữ liệu thật, kể cả 0. Kết quả AI dùng mẫu do chưa có telemetry tổng hợp chuẩn hóa. Provider thật hiển thị Chưa đo; checkbox riêng cho phép xem trạng thái minh họa có chữ MẪU. Database được xác nhận từ chính truy vấn thống kê, không có health probe có phí chạy nền.

Catalog và RAG đọc dữ liệu thật, duyệt/gỡ công khai có xác nhận và version; index phải xác nhận vì có thể phát sinh phí. Không tạo hồ sơ mẫu có thể bị ghi nhầm vào database. Trang này chưa bổ sung trình tạo/sửa toàn bộ trường địa điểm; thao tác hiện tại tập trung kiểm duyệt và index.

## MongoDB telemetry

Biểu đồ kết quả AI đã dùng API `/admin/api/telemetry` thay mẫu. Trang kho tri thức có nhật ký run và các bước, tổng/p50/p95 xấp xỉ, pending/retried và trạng thái Mongo thật. Khi disabled/unavailable, báo rõ và không giả số 0; dữ liệu cũ có thể chậm do outbox. API chỉ theo dõi lượt tạo draft chuyến đi từ V23, không tổng hợp tất cả công cụ AI. Hướng dẫn database, retention và khôi phục: [MongoDB telemetry](../../docs/MONGODB_AI_TELEMETRY.md).
