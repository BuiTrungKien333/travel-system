# Trip CRUD — vertical slice đầu tiên

Ngày: 02/11/2026. Đã triển khai create/list/detail, không có draft/AI trong MVP.

- `POST /api/v1/trips`: owner từ JWT; validate DTO/date/reference; insert Trip với unique request key và ON CONFLICT; tạo itinerary/ngày/pool trong cùng transaction. Replay cùng key/body trả200; mới trả201; khác payload trả TRIP_004/409.
- `GET /api/v1/trips`: page0,size20,max100; optional mode/q, sort whitelist, stable ID tie-break; count/page cùng read snapshot.
- `GET /api/v1/trips/{id}`: metadata + ngày. Không tồn tại/deleted/owner khác cùng TRIP_001/404.
- Status lưu PLANNING, mode theo ngày tại Asia/Ho_Chi_Minh; không cron, không provider call và không thay thuật toán planning.
- Service đặt transaction; JdbcTemplate repository dùng PostgreSQL ON CONFLICT để concurrent replay không làm transaction bị rollback-only do bắt unique exception. Read projections không load JPA graph để đếm.
- Flyway V10 thêm metadata/version/create key/hash, trip_days và backfill range hợp lệ. Trip entity tương thích schema mới; BaseEntity không đổi.
- Preferences mới ghi JSON array; legacy free-text không được tự chuyển thành [] gây mất dữ liệu. Cần audit/backfill riêng nếu DB hiện hữu có dữ liệu không theo contract.
- Date CHECK NOT VALID áp dụng write mới; chưa tuyên bố toàn bộ dữ liệu legacy đã sạch.
- Errors theo envelope hiện tại; AppException bổ sung field errors optional để cross-field validation có tên field và không phá callers cũ.
- Chưa PATCH/delete/date-change-preview/destinations; không public resource mới, không thêm dependency.

## Kiểm tra

```powershell
cd E:\travel-system\backend
./mvnw.cmd test
```

Docker cần chạy để PostgreSQL17 Testcontainers tự tạo DB cô lập, không dùng DB dev/production. Test integration đi qua JWT/login thật; SMTP là boundary được mock. Có test concurrent create và child-write rollback, không sleep để đồng bộ race.

Mobile bật EXPO_PUBLIC_UI_PREVIEW=false và EXPO_PUBLIC_API_URL theo thiết bị; env không có secret. Web token hiện chỉ ở memory nên full reload có thể cần login lại; dữ liệu chuyến trong DB không mất.

Đặc tả và OpenAPI trong repo mobile: `docs/TRIP-CRUD-CONTRACT.md`, `docs/api/trip-crud.openapi.json`. Hướng dẫn thử: `docs/TRIP-API-INTEGRATION.md`. Runtime chỉ3 operations nêu trên, không phải toàn bộ7 operations trong hợp đồng đích.
