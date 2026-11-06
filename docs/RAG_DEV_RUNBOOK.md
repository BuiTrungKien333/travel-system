# Core AI RAG trong môi trường dev

## Chạy backend dev

Từ thư mục E:/travel-system:

```powershell
./scripts/Start-Dev.ps1
```

Script bật profile **dev**, dùng database chính `travel_db` của `travel-postgres` trên cổng cấu hình bằng POSTGRES_PORT (local hiện là 5432). HTTP mặc định **18081** để tránh đụng ứng dụng khác đang dùng 8080; chọn cổng khác bằng `-Port 18082`. Không cần bật thêm rag-demo.

Docker và backend nhận cùng môi trường: `.env` ở root, sau đó các giá trị trong `backend/.env` được ưu tiên. Port HTTP của script được áp dụng cuối cùng. GEMINI_API_KEY đang có trong môi trường được sử dụng cho cả AI cũ và RAG; không đọc/copy key từ `.env.rag-demo`.

Script dùng Maven wrapper, build image PostgreSQL nếu cần, khởi động riêng service postgres và chạy backend foreground. Ctrl+C dừng backend; database giữ nguyên dữ liệu. Không chạy đồng thời nhiều backend cùng cổng. Script không dừng service khác để lấy cổng.

Để chạy dev không gọi Gemini, dùng `./scripts/Start-Dev.ps1 -WithoutAi` và gửi preferences thay cho text. Key trong file không bị sửa.

Nếu chạy bằng IDE/Maven trực tiếp: dùng profile dev (cũng là profile mặc định) và khởi động service postgres từ compose. Cổng mặc định dev và SERVER_PORT trong backend/.env.example là 18081; backend/.env local cũng dùng 18081. application-dev.yaml tự đọc file môi trường ở root/backend trong các working directory thông dụng. Nếu Run Configuration của IntelliJ có SERVER_PORT=8080 hoặc --server.port=8080 thì xóa/đổi giá trị override đó thành 18081. Chạy một backend dev trên cổng này tại một thời điểm. Khi vừa đổi tên RagDemoProperties thành RagProperties, clean output cũ trước khi chạy lại.

## Database và dữ liệu

- Compose chính build image `travel-postgres-pgvector:17-0.8.2` từ **postgres:17-alpine**, bổ sung pgvector 0.8.2. Giữ PostgreSQL major 17, nền Alpine và volume `postgres_data` hiện có; không đổi sang Debian, không xóa/recreate volume.
- Source extension dùng bản phát hành [pgvector v0.8.2](https://github.com/pgvector/pgvector/tree/v0.8.2). Image cài extension binary; Flyway tạo extension trong database.
- Profile dev chạy `db/migration` rồi `db/rag`: có vector, unaccent, bảng place_embeddings và catalog Đà Nẵng gồm 8 địa điểm đã duyệt. Seed idempotent dùng ID cố định và ON CONFLICT DO NOTHING, không tự duyệt hoặc ghi đè metadata của địa điểm khác.
- Hai repeatable SQL được chuyển nguyên nội dung từ db/rag-demo sang db/rag, giữ tên/checksum để profile demo tiếp tục dùng được. Profile production không thêm location db/rag.
- Core hiện vẫn giới hạn Đà Nẵng, 1–2 ngày. Tọa độ/thời lượng là ước tính, giờ mở cửa UNKNOWN có cảnh báo; việc chuyển sang dev không nâng mức xác minh dữ liệu.
- Không chép bảng users/trips/itineraries hoặc vector từ database demo. Embedding được index lại trong dev khi có key/provider khả dụng.

Demo riêng vẫn chạy bằng Start-RagDemo.ps1, dùng travel_rag_demo trên **5435**, HTTP **18080**. Service bên ngoài đang dùng **5434** không bị thay đổi. Cả dev và demo cùng dùng core Java và cấu hình AI chung, nhưng datasource/volume độc lập.

## API dev

Các URL public được giữ theo luồng đã dùng để kiểm tra thủ công, không cần token:

```text
GET  http://localhost:18081/api/v1/public/planning/retrieval/catalog
POST http://localhost:18081/api/v1/public/planning/retrieval/index
POST http://localhost:18081/api/v1/public/planning/retrieval/search
POST http://localhost:18081/api/v1/public/planning/retrieval/intent
POST http://localhost:18081/api/v1/public/planning/retrieval/search-from-text
POST http://localhost:18081/api/v1/public/planning/generate-draft
```

Swagger: http://localhost:18081/swagger-ui.html. Các API khác tiếp tục áp dụng security hiện tại; nhóm RAG chỉ được bật trong dev/rag-demo, không tự bật production.

Sau khi cấu hình GEMINI_API_KEY, gọi index (không có body) và kiểm tra updated/skipped/failed. Nếu không có vector hiện hành hoặc provider lỗi, retrieval sẽ trả lexical fallback rõ nguồn; không tự giả định index đã thành công.

Body generate-draft mẫu:

```json
{
  "preferences": {
    "destination": "Đà Nẵng",
    "numberOfDays": 2,
    "interests": ["MUSEUM", "HISTORY", "ART", "LOCAL"],
    "excludedTags": ["BEACH"],
    "dailyStartTime": "08:00",
    "dailyEndTime": "17:00",
    "travelPace": "RELAXED",
    "mustVisitNames": ["Bảo tàng Điêu khắc Chăm"]
  },
  "startDate": "2026-11-06",
  "origin": {"name": "Điểm xuất phát dev", "latitude": 16.065, "longitude": 108.223},
  "roundTrip": true,
  "restMinutes": 45
}
```

Body text, quy tắc validation, executionReport và fallback như [hướng dẫn draft](./RAG_DRAFT_POSTMAN.md) và [AI/fallback](./RAG_AI_FALLBACK_DEMO.md); chỉ đổi port sang cổng backend dev. generate-draft vẫn là preview, không tạo Trip/itinerary.

## Cấu hình AI chung

application-rag.yaml được import bởi cả dev và rag-demo:

```text
GEMINI_API_KEY
GEMINI_BASE_URL                 (tùy chọn)
RAG_INTENT_MODEL                mặc định gemini-3.5-flash
RAG_EMBEDDING_MODEL             mặc định gemini-embedding-001
RAG_AI_DEADLINE_MILLIS          60000
RAG_AI_CALL_TIMEOUT_MILLIS      30000
RAG_AI_RETRY_BACKOFF_MILLIS     500
RAG_WEATHER_WAIT_MILLIS         2000
```

Không cần đổi model hiện có. Thuật toán, retry dùng chung, nhãn AI/fallback, thời tiết song song và stage timing được giữ.

## Kiểm chứng

RagDevIntegrationTest chạy profile test + dev với PostgreSQL/pgvector thật, migration/schema/catalog thật, HTTP/security/solver/timeline thật; chỉ mock ranh giới AI/embedding/weather. Kiểm tra dev có catalog và hybrid search, tạo draft fallback không cần đăng nhập và không ghi itinerary. RagDemoProfileTest kiểm tra controller bật ở dev/demo và tắt ở test/prod.

Trước khi thay container local, đã lưu backup database tại C:/Users/DELL/.codex/backups/travel-system/travel_db-before-rag-20261105-203344.dump. Volume dev được giữ nguyên.

Kết quả xác minh ngày 05/11/2026:

- `mvnw.cmd -q clean test`: 364 tests, 0 failures, 0 errors, 0 skipped.
- Chạy dev thật trên travel_db, xác nhận vector 0.8.2, unaccent, 8 profile đã duyệt và đầy đủ Flyway migration.
- Chạy tạm dev không Gemini trên cổng 18082: request preferences trả READY_WITH_WARNINGS, 2 ngày/4 điểm, nguồn FALLBACK rõ ràng; itineraries trước/sau đều 0. Server tạm đã dừng.
- Dev bình thường chạy trên cổng 18081. Index bằng Gemini embedding thật trả updated=8, skipped=0, failed=0; travel_db lưu 8 vector model gemini-embedding-001. Không chép vector từ demo hoặc fixture.
- Một lần gọi text với provider thật gặp Gemini 503 tại INTENT, thử lại đúng một lần rồi trả NEEDS_STRUCTURED_INPUT/PROVIDER_UNAVAILABLE, không dựng intent giả. Đây là kết quả quan sát ở thời điểm kiểm tra; không khẳng định lượt này đã chạy trọn generation thật.
- Hybrid search thật trả HYBRID với semanticMatch=true. Gửi preferences ở dev bình thường trả READY_WITH_WARNINGS, hai ngày, overallMode=AI_WITH_FALLBACK: retrieval dùng vector thật, allocation/themes fallback vì generation 503, solver DETERMINISTIC và thời tiết EXTERNAL_API.
- Checksum hai repeatable SQL trên database dev và demo khớp nhau sau khi chuyển location; không thay nội dung seed/schema demo đã áp dụng.
- Database demo vẫn trên 5435; service ngoài dự án trên 5434 giữ nguyên container ID và thời điểm khởi động.
