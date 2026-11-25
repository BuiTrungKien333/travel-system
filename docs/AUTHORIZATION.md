# Phân quyền Guest, User, Admin

Triển khai ngày 17/11/2026 cho backend và ứng dụng mobile. Các policy nằm trong `SecurityConfig`; endpoint mới không thuộc policy sẽ bị từ chối. Tất cả kiểm tra ownership hiện có vẫn giữ nguyên cho cả USER và ADMIN.

## Vai trò và dữ liệu

- Guest chưa đăng nhập khám phá tỉnh thành, địa điểm INTERNAL đã duyệt và thời tiết công khai. Không tạo tài khoản/JWT giả cho chế độ khách. Tài khoản có role GUEST nếu tồn tại cũng chỉ dùng dữ liệu công khai, đọc `/users/me`, refresh và logout phiên của chính mình.
- USER dùng workspace, thao tác chuyến đi, lịch trình, AI, kỷ niệm và media của chính mình.
- ADMIN có quyền người dùng với dữ liệu của chính mình, thêm quyền quản trị tài khoản, duyệt catalog, index AI và xem vận hành. Không có quyền bỏ qua ownership để đọc chuyến đi/ảnh/kỷ niệm người khác.
- Không có chia sẻ chuyến đi công khai hoặc cộng tác nhiều người trong phiên bản này.

## Ma trận API hiện tại

Các đường dẫn dưới đây có prefix `/api/v1`, trừ Actuator/Swagger.

| API / thao tác | Điều kiện |
|---|---|
| POST `/auth/register`, `/login`, `/google`, `/google/challenge`, `/google/verify-link`, `/verify-email`, `/resend-verification`, `/forgot-password`, `/reset-password` | Public; validation, token xác minh và trạng thái tài khoản vẫn được kiểm tra |
| POST `/auth/refresh`, `/auth/logout` | Public ở tầng access-token; refresh token trong body phải hợp lệ theo nghiệp vụ |
| GET `/auth/verify-email`, `/auth/google/verify-link` | Chỉ để trả 405 cho phương thức sai; không tiêu thụ token hoặc xác minh tài khoản |
| GET `/destinations`, `/public/places`, `/public/places/{id}` | Public; địa điểm chỉ INTERNAL, approved và không có scope demo chưa xác minh |
| GET `/public/weather/current`, `/public/weather/forecast` | Public, có giới hạn request |
| GET `/users/me` | Tài khoản đã xác thực, bao gồm GUEST |
| PUT `/users/me` | USER/ADMIN + `profile:update`, hồ sơ mình |
| GET `/home/dashboard`, `/trips`, `/trips/**` | USER/ADMIN + `trip:read`, dữ liệu mình; các nhánh đặc biệt bên dưới được xét trước |
| POST `/trips` | USER/ADMIN + `trip:create` |
| PATCH `/trips/{id}`, POST đổi ngày/hoàn thành/thêm điểm, PUT/DELETE điểm trong chuyến | USER/ADMIN + `trip:update`, chuyến mình |
| DELETE `/trips/{id}` | USER/ADMIN + `trip:delete`, chuyến mình |
| `/trips/{id}/itinerary/**`, gồm route/optimize theo ngày | USER/ADMIN + `itinerary:manage`, chuyến mình |
| `/trips/{id}/planning/drafts/**`, gồm tạo/đọc/latest/apply | USER/ADMIN + `planning:generate`, chuyến mình; quota tạo nháp giữ nguyên |
| GET `/journal/memories/**` | USER/ADMIN + `memory:read`, dữ liệu mình |
| POST/PATCH/DELETE memory | USER/ADMIN + `memory:create` / `memory:update` / `memory:delete`, dữ liệu mình |
| GET `/media/limits`, POST `/media/upload`, `/media/uploads` | USER/ADMIN + `media:upload`, kiểm tra ownership khi gắn memory/trip |
| DELETE `/media`, `/media/delete-media` | USER/ADMIN + `media:delete`, dữ liệu mình |
| GET `/places/search`, `/places/addresses`; POST `/routes` | USER/ADMIN + `maps:use` |
| GET `/places/suggestions`, `/places/{id}` | USER/ADMIN + `place:read`; có thể nhìn thấy địa điểm riêng của mình |
| `/planning/**`, `/public/planning/**` còn lại | USER/ADMIN + `planning:generate`; không có quyền index qua quy tắc này |
| POST `/admin/planning/retrieval/index`, `/public/planning/retrieval/index` cũ | ADMIN + `admin:ai:index`; controller legacy chỉ có ở dev/rag-demo |
| GET `/admin/session` | ADMIN; trả thông tin phiên và permission hiện hành, không trả token |
| GET `/admin/users`, `/admin/users/events`; PATCH `/admin/users/{id}` | ADMIN + `admin:users` |
| GET `/admin/catalog`; PATCH `/admin/catalog/{id}` | ADMIN + `admin:catalog` |
| GET `/health`, `/actuator/health`, `/actuator/health/liveness`, `/actuator/health/readiness` | Public, không cấu hình lộ component/detail |
| `/actuator/**` còn lại | ADMIN + `admin:system:read`; còn phụ thuộc endpoint được expose |
| Swagger/OpenAPI | Public ở dev/test/rag-demo; ngoài các profile này yêu cầu ADMIN + `admin:system:read` |
| Endpoint chưa được phân loại; `/admin/**` chưa đăng ký | Deny by default |

## Quyền và phiên đăng nhập

Migration V22 thêm `locked_at`, permission còn thiếu và hai bảng lịch sử quản trị. Không sửa migration đã phát hành; không xóa dữ liệu người dùng, không tự tạo/nâng quyền tài khoản thật.

JWT vẫn kiểm tra chữ ký, loại token, user/email/sessionVersion và xác minh email. Role và permission thực thi lấy từ database mỗi request; không tin quyền cũ trong JWT. Đổi role, khóa và mở khóa đều tăng `sessionVersion`, thu hồi refresh token. Mở khóa không làm token cũ sống lại. Tài khoản khóa không đăng nhập bằng mật khẩu/Google, refresh hoặc hoàn tất email action để vượt trạng thái khóa.

Gửi thay đổi tài khoản:

```json
{"role":"USER","locked":true,"version":0}
```

`version` lấy từ danh sách tài khoản; dữ liệu cũ trả `409 ADMIN_001`. Không cho admin tự khóa/tự đổi role. Không cấp ADMIN cho email chưa xác minh. Các thay đổi quản trị được serialize bằng advisory transaction lock và kiểm tra lại actor trong transaction, tránh hai admin đồng thời hạ quyền nhau. Phải giữ ít nhất một admin đã xác minh, không khóa, không xóa. Lịch sử lưu actor, target, role và trạng thái trước/sau trong cùng transaction.

Catalog dùng `{"approved":true,"version":1}`. Chỉ duyệt profile INTERNAL đã có thông tin provenance; từ chối scope `DEV_DEMO_ONLY_NOT_FACT_VERIFIED`. Approval là quyết định công bố, không tự xác minh tọa độ/giờ mở cửa hoặc thay metadata nguồn. Mọi lần đổi trạng thái ghi `admin_catalog_events`; version cũ bị chặn. Index là thao tác riêng, có thể phát sinh chi phí embedding.

## Mobile và kiểm thử thủ công

1. Chạy backend có V22; đăng nhập lại nếu phiên cũ đã bị thu hồi. Đăng ký mới luôn nhận USER, không nhận role từ client.
2. Từ màn đăng nhập chọn **Khám phá điểm đến**: chọn tỉnh/thành, xem danh sách và chi tiết địa điểm công khai; không có dữ liệu thì hiển thị trạng thái rỗng.
3. USER dùng workspace hiện có. Không có nút quản trị và không mở được route `/admin` bằng deep link.
4. ADMIN vào **Hồ sơ → Quản trị hệ thống**. Có danh sách/tìm kiếm/phân trang tài khoản, khóa/mở khóa, đổi role và bước xác nhận; catalog có duyệt/gỡ, index và lịch sử thay đổi tài khoản. Lịch sử catalog hiện lưu ở backend.
5. Khóa hoặc đổi role một tài khoản test khác đang đăng nhập; request tiếp theo của phiên cũ phải thất bại. Đăng nhập lại để nhận quyền mới. Kiểm tra tài khoản quản trị không mở được ID chuyến đi của người khác.

Backend trả `401 AUTH_001` khi thiếu/không còn phiên hợp lệ; `403 AUTH_002` khi đã xác thực nhưng thiếu quyền. Resource của người khác tiếp tục trả 404. Mobile không refresh/lặp lại request do thiếu quyền; request public không gửi bearer hoặc kích hoạt refresh.

## Giới hạn vận hành

Limiter trong process giới hạn 120 request/phút cho IP chưa xác thực, 180 request/phút cho tài khoản trên các API khám phá/thời tiết/maps/công cụ. Bộ nhớ tối đa 10.000 khóa, reset theo phút; trả 429 và `Retry-After`. Không tin `X-Forwarded-For` từ client. Khi dùng reverse proxy/nhiều instance cần áp dụng giới hạn chia sẻ ở gateway và cấu hình IP tin cậy. Limiter này không thay thế quota AI 3 bản nháp/phút hoặc bảo vệ đăng nhập/email trước brute force tại gateway.

Chưa có giao diện chỉnh bảng role_permissions, tạo mới hồ sơ catalog, đọc lịch sử catalog hoặc xem dữ liệu riêng của người dùng. Những khả năng này không cần để thực thi mô hình ba vai trò hiện tại.

## Kết quả kiểm chứng

- Toàn bộ 584 test backend đạt, 0 failure/error/skipped; kiểm tra thêm endpoint lịch sử sau thay đổi tài khoản cũng đạt. Có 22 integration test phân quyền/JWT/PostgreSQL và 2 test limiter mới; bao gồm thu hồi phiên, quyền thay đổi trong DB, truy cập chéo, concurrent cross-demotion, catalog công khai và duyệt catalog.
- Toàn bộ 453 test mobile đạt; có kiểm tra role guard, contract, xác nhận thay đổi admin và transport public không gửi bearer/refresh.
- TypeScript và lint các file sửa trong đợt phân quyền đạt; Expo web export thành công. Lint toàn repo vẫn có 65 lỗi và 5 warning ở các thành phần có sẵn ngoài phạm vi này (camera, map, motion và một số màn trip/home).
- JAR được đóng gói. Backend dev khởi động thành công tại `http://localhost:18081`, migration V22 áp dụng; health UP, GET destinations không đăng nhập trả thành công, GET trips và POST index không đăng nhập trả 401.
- Không build APK, không gọi Gemini thật để kiểm chứng phân quyền và chưa kiểm tra trực quan trên điện thoại.

## Cổng quản trị web

Ứng dụng Next.js độc lập tại `frontend-web/admin`, URL local `http://localhost:3001/admin/login`. Giai đoạn đầu có đăng nhập/refresh/logout bằng BFF và cookie HttpOnly, layout riêng, quản lý tài khoản với quyền `admin:users`, tìm kiếm/phân trang và xác nhận trước khi đổi role hoặc khóa/mở khóa. Không dùng mock/API client/auth store của web cũ. Hướng dẫn và phạm vi kiểm chứng trong [README admin](../frontend-web/admin/README.md).
