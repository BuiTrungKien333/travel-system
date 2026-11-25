# QA-0: khôi phục migration sau merge · 06/11/2026

## Nguyên nhân và phạm vi

Commit backend `8fc64b7` ghép hai bộ lịch sử Flyway không tương thích: trùng V9/V10/V11 và đổi tên/số V4–V8. Database dev travel_db có V1–V11 + hai repeatable RAG, 13/13 script/checksum khớp parent `90afbb0`.

Đã khôi phục đúng byte nội dung V4–V8 từ parent; V1–V3/V9–V11 và repeatable không đổi. Không repair/baseline/reset history. Đây là bộ lịch sử canonical cho bản hiện tại; database từ nhánh khác có lịch sử khác cần đối chiếu riêng, không tự gán lại version.

## Thay đổi

- V12 `persist_multi_day_itinerary` giữ migration mới chưa áp dụng từ source: arrival_at/duration_minutes, backfill lịch trình cũ và constraints.
- V13 `trip_details_and_place_category`: các cột metadata Trip/Place của nhánh nhập, trước đó trùng V10.
- V14 `memory_permissions_and_indexes`: chỉ quyền/index cần thiết, tách khỏi file V4 đã nhập. Không thay đổi nghiệp vụ RBAC actor trong lượt này.
- Dữ liệu mẫu chuyển sang `src/main/resources/db/demo` để chạy dev thủ công trong một transaction; không tự tạo tài khoản/chuyến/ảnh demo khi khởi động.
- Bỏ khai báo enum TRIP_NOT_FOUND bị trùng khiến javac/Lombok không compile sau merge. Không refactor service hay thay contract Trip trong QA-0.
- Test upgrade từ V11 kiểm tra checksum lịch sử đã phát hành và giữ nguyên visit qua đêm/note; schema sạch không tự sinh trips/memories/demo users. Test seed đổi sang chạy chủ động và giữ kiểm tra rollback/ownership.

## Kiểm chứng

- Đã chạy full-suite backend, sửa các lỗi phát hiện rồi chạy lại các class ảnh hưởng. Báo cáo Surefire cuối: **403 tests, 0 failures/errors/skipped** (không phải khẳng định toàn bộ 403 được chạy trong một lệnh cuối). Các rerun gồm TripIntegration/TripCrud/TripMutation 38 tests và Auth/Cloudinary/Media/migration 23 tests, đều đạt.
- Nâng cấp V11 → V14 trên PostgreSQL test giữ nguyên 11 checksum versioned, visit qua đêm/note và các trường cũ; database sạch không tự tạo Trip/Memory hay 50 user demo. Seed opt-in giữ kiểm tra idempotency và rollback khi trùng ID/email.
- Backend dev được khởi động lại bằng code hiện tại trên **port 18081**, PID quan sát `19612`. Flyway validate đủ **16 migrations**, áp dụng thành công V12/V13/V14, giữ nguyên 13 history rows cũ.
- So sánh fingerprint các trường cũ của 9 bảng nghiệp vụ trước/sau: không đổi. Dev vẫn 4 users, 8 places, 0 trips/itineraries/memories/media. Không reset/repair DB.
- HTTP thật: GET `/api/v1/health/database` → 200/CONNECTED; CORS Origin `http://localhost:8082` cho login POST và profile GET/Authorization → 200; GET profile không token → 401/AUTH_001; login dummy account không tồn tại → 401/AUTH_004. Không tạo user hoặc gửi email cho smoke test này.
- Frontend Metro cổng 8082 trả `packager-status:running`; env API của mobile trỏ port18081. Chưa thao tác UI/native, chưa nghiệm thu login hợp lệ, email hoặc upload provider thật.
- Chỉ dùng dependency có sẵn/Maven offline. Không sửa frontend runtime, không commit/push.

## Các blocker sau merge đã sửa thêm trong QA-0

- Test Memory cũ thiếu mock repository ownership mới; cập nhật fixture đúng dependency, không bỏ kiểm tra owner.
- Trip integration tests của source còn dùng PUT/DTO/list-array cũ. Đã đổi sang canonical PATCH/version, Read/Page envelopes và semantics soft delete đã chốt; giữ kiểm tra validation, ownership, transaction/concurrency, place ordering và data preservation. Không phục hồi endpoint CRUD legacy để làm test xanh.
- Phát hiện projection `memory_count` đếm bản ghi khác owner và soft-delete Trip cập nhật cả memory khác owner nếu legacy data có tham chiếu sai. Bổ sung owner predicate ở cả hai query; regression kiểm GET/list counts và giữ nguyên memory của người khác khi xóa Trip. Không thay RBAC actor.
- Lần boot dev đầu thất bại do Cloudinary credentials chưa có: `CloudinaryConfig` tạo SDK bean chỉ khi đủ ba giá trị; service lấy bean tùy chọn và trả **503/UPLOAD_006** khi upload/remote-delete chưa được cấu hình. Không dùng credential giả; tests cover missing/partial/complete configuration và upload unavailable. Core auth/Trip không bị media config chặn startup. Ba biến thiết lập ở phase Media là `CLOUDINARY_CLOUD_NAME`, `CLOUDINARY_API_KEY`, `CLOUDINARY_API_SECRET`; không đưa secret vào chat/Git.

## Việc để lại cho phase nghiệp vụ

- Các mã lỗi Trip mới nhập đang trùng chuỗi TRIP_002–TRIP_006 với CRUD canonical; cần thống nhất contract frontend/backend ở phase Trip, không coi các mã cùng chuỗi là cùng nghĩa.
- TripService legacy và TripCrudService cùng tồn tại để giữ API Places. Hai nhóm metadata (ví dụ budget và budget_amount) cần đối chiếu tại phase Trip.
- Itinerary/Live/Memory nghiệp vụ, native UI và production readiness chưa được nghiệm thu bởi việc sửa migration.
