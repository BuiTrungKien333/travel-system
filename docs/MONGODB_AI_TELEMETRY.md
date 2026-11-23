# MongoDB — nhật ký thực thi AI

## Phạm vi và nguồn dữ liệu

PostgreSQL vẫn là nguồn chuẩn của tài khoản, phân quyền, chuyến đi, itinerary, bản nháp và trạng thái Apply. pgvector tiếp tục phục vụ retrieval. MongoDB là kho metadata thực thi, collection `ai_execution_runs`, scope đầu tiên **TRIP_DRAFT**. Chưa ghi riêng các lần gọi công cụ demo, index hoặc mọi HTTP request. Không di chuyển dữ liệu nghiệp vụ hoặc embedding sang MongoDB.

Mỗi lượt tạo draft thực sự có `_id = draftId`, schemaVersion=1, startedAt/finishedAt/durationMs, status, overallMode, routeSource và tối đa 16 bước thực thi. Các bước chỉ giữ stage/source/method/model, attempts/retries, elapsedMillis, reason dạng mã và HTTP status provider. Không ghi request_json, prompt, nội dung lịch trình, origin/GPS, userId/tripId, API key hay exception message. Không backfill tất cả lịch sử cũ; các draft cũ đang treo có thể được chốt INTERRUPTED khi đối chiếu.

## Luồng ghi và khôi phục

```text
TripDraftService → PostgreSQL draft outcome + trigger V23 → ai_execution_outbox
                                                          ↓ claim / lease
                                                ExecutionPublisher
                                                          ↓ upsert _id
                                               MongoDB ai_execution_runs
                                                          ↓
                                                  Admin telemetry API
```

Trigger `AFTER UPDATE OF response_json,failure_code` chỉ phát snapshot lần chuyển terminal đầu tiên. Transaction rollback không để lại event. Outbox không FK cascade tới draft để việc dọn dữ liệu nguồn không làm mất sự kiện chưa gửi. HTTP retry cùng request không tạo draft mới; retry provider nằm trong stages, không tăng tổng lượt chạy.

Worker lấy 5 sự kiện mỗi lượt, lease 60 giây bằng SKIP LOCKED. Không giữ transaction PostgreSQL trong lúc gọi MongoDB. Ghi Mongo theo `$setOnInsert` và `_id` duy nhất: giao thành công rồi crash trước ACK vẫn gửi lại an toàn. ACK chỉ hợp lệ với lease token đang sở hữu. Timeout kết nối/đọc có giới hạn; retry từ 15 giây, tăng dần tới tối đa 1 giờ. Không tự bỏ sự kiện chưa giao sau nhiều lỗi.

Mỗi lượt worker cũng chốt draft treo quá 10 phút thành INTERRUPTED (trên deadline xử lý hiện tại); phản hồi provider đến muộn không được ghi đè failure. Snapshot không có executionReport giữ stages rỗng, không suy diễn số lần gọi provider. Các outbox đã giao được dọn sau 7 ngày; outbox chưa giao được giữ để phục hồi.

MongoDB unavailable không tham gia transaction draft, không chặn việc lưu/Apply trên PostgreSQL. Admin có status UNAVAILABLE, pending/retried/oldestPending; không trả tổng 0 thay cho lỗi và không dùng mẫu để che lỗi kết nối. Khi Mongo hồi phục, worker tiếp tục theo lịch retry; số liệu có độ trễ.

## Schema, index và retention

Backend tạo/cập nhật validator và index khi sẵn sàng, có thể defer nếu Mongo chưa lên. `_id` là string; scope/schemaVersion cố định; Date BSON cho startedAt/finishedAt/expiresAt; duration không âm; stages có giới hạn. User ứng dụng có readWrite và dbAdmin **chỉ trên database telemetry** để bootstrap validator/index, không dùng root.

Index: `_id` unique mặc định, startedAt giảm dần, status+startedAt và TTL expiresAt (expireAfterSeconds=0). Retention mặc định 90 ngày tính từ finishedAt, cấu hình 1–3650 ngày. Snapshot đã hết hạn không được publish lại. API cũng lọc expiresAt để không phụ thuộc thời điểm TTL background thực sự xóa. Thay retention không tự sửa hạn của document đã ghi; dữ liệu tổng hợp dài hạn hơn chưa triển khai.

Runtime driver dùng majority+journal write concern. Mongo standalone đủ cho local vì mỗi lượt chỉ upsert một document, không cần transaction Mongo nhiều document. Khi production, dùng deployment Mongo có backup và HA phù hợp.

## Chạy local

Root `.env` cần MONGODB_ENABLED=true, MONGO_PORT (mặc định 27018), MONGO_ROOT_USER/PASSWORD, MONGO_APP_USER/PASSWORD, MONGODB_DATABASE và MONGODB_URI. Mẫu trong `.env.example`. Credentials URI phải URL-encode; backend dùng app user, không dùng root. Private credentials local đã được tạo trong `.env` bị gitignore, không ghi vào docs/chat/source.

```powershell
# Từ E:/travel-system
docker compose up -d --wait mongodb
./scripts/Start-Dev.ps1 -UsePackagedJar
```

Mongo image 8.0, volume mongo_data riêng, port chỉ bind 127.0.0.1. Init script tạo app user khi volume mới. Đổi password trong env không tự đổi user trong volume đang có; cần updateUser/rotate credentials đúng quy trình. Không xóa volume để sửa lỗi cấu hình. Backup PostgreSQL và MongoDB độc lập; outbox là cầu nối khi đồng bộ gián đoạn.

Start-Dev luôn yêu cầu PostgreSQL sẵn sàng, nhưng Mongo khởi động lỗi chỉ cảnh báo rồi tiếp tục backend. Ngoài dev, bật MONGODB_ENABLED rõ ràng. Profile test không tự tạo client tới Mongo local; integration test dùng container độc lập.

## API và giao diện

`GET /api/v1/admin/ai/executions?days=30&page=0`: ADMIN + admin:system:read. days thuộc 7/30/90, page>=0, size=20. Trả AVAILABLE/DISABLED/UNAVAILABLE, asOf, retentionDays, sync và data khi đọc được MongoDB. Thống kê theo startedAt trong kỳ, múi giờ Việt Nam; chỉ tính terminal snapshot còn retention. Mongo aggregation tính phân bố status và percentile p50/p95 xấp xỉ. Trang mới nhất sort startedAt giảm dần + _id ổn định.

Dashboard đọc telemetry thật, hiển thị tổng, p50/p95 và trạng thái Mongo thực tế. Kho tri thức AI có danh sách lượt chạy phân trang, mở từng lượt để xem các bước. p50/p95 là thời gian toàn luồng ghi nhận, không cộng các bước chạy song song. Chưa có chi phí token, usage billing hoặc thống kê Apply trong Mongo; Apply vẫn thuộc PostgreSQL.

BFF `/admin/api/telemetry` chỉ forward days/page đã validate và cookie access token tới backend, no-store. Mongo URI không gửi sang browser. Frontend không tự chuyển lỗi Mongo thành dữ liệu mẫu.

## Kiểm chứng

MongoTelemetryIntegrationTest dùng PostgreSQL 17 + MongoDB 8 thật: transaction rollback, sanitization, ghi lại không trùng, lease, lỗi/INTERRUPTED, validator/TTL và pause Mongo để kiểm tra PostgreSQL vẫn lưu được rồi unpause để drain queue. Các test phân quyền/TripDraft/migration và browser được chạy bổ sung; kết quả cuối trong báo cáo triển khai.

Kiểm thử gián đoạn trên môi trường dev của riêng bạn: dừng service mongodb, tạo nháp mới, xác nhận PG vẫn có draft và outbox tăng; bật mongodb lại, chờ retry và mở admin để thấy pending giảm. Không dùng dữ liệu mẫu làm bằng chứng đã gọi Gemini; việc ghi telemetry không tự tạo thêm lời gọi AI.

## Kết quả xác minh triển khai (17/11/2026)

- Backend: 594 tests, 0 failures/errors/skipped; package JAR thành công.
- Admin: 17 unit tests và 12 browser tests; lint, TypeScript và production build thành công.
- Local: migration V23 đã áp dụng; app user bootstrap validator/index Mongo thành công; backend health UP, API telemetry không đăng nhập trả 401 và admin login trả 200.
- Không gọi Gemini thật trong đợt kiểm thử này; test provider dùng boundary giả lập, test database dùng PostgreSQL/MongoDB thật trong container tách biệt. Không build APK.
