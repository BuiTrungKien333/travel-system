# Chạy mốc dữ liệu và truy xuất demo Đà Nẵng

## Phần đã triển khai

- 8 địa điểm có nguồn, metadata truy xuất và UUID cố định trong database demo; seed opt-in chạy lại không tạo trùng/ghi đè.
- Profile `rag-demo`, PostgreSQL 17 + pgvector riêng trên cổng 5435 (cấu hình qua `RAG_DEMO_DB_PORT`), volume riêng. Không đổi image hoặc volume PostgreSQL cũ.
- Exact cosine search + PostgreSQL lexical search có bỏ dấu; hợp nhất thứ hạng RRF. Embedding 768 chiều dùng Gemini qua REST, API key chỉ lấy từ môi trường.
- Chỉ trả các địa điểm đã duyệt còn tồn tại trong DB. Vector khác model hoặc stale content hash không được dùng.
- Các API catalog/search/index là public, không cần tài khoản hoặc token; có trong dev và rag-demo. Tài liệu này hướng dẫn database demo riêng; chạy cùng database dev xem [RAG dev](./RAG_DEV_RUNBOOK.md).

Đã bổ sung LLM tách ý định/phủ định và API search-from-text, xem [hướng dẫn nhập câu tự nhiên](./RAG_INTENT_POSTMAN.md). API [generate-draft](./RAG_DRAFT_POSTMAN.md) nối tiếp chọn/chia ngày, Held–Karp, timeline và chủ đề; chưa lưu itinerary hoặc tích hợp mobile. API search cũ vẫn nhận sở thích tích cực và exclusions có cấu trúc.

Đã bổ sung retry có giới hạn và executionReport phân biệt AI/fallback từng bước. Xem [kịch bản demo AI/fallback và đầu vào preferences](./RAG_AI_FALLBACK_DEMO.md). Có thể chạy `./scripts/Start-RagDemo.ps1 -WithoutAi` để kiểm tra fallback mà không sửa key trong file cấu hình.

Core đã được tổ chức lại theo feature/layer, gộp allocation + chủ đề và chạy thời tiết song song. Xem [cấu trúc, timing và kết quả đo trước/sau](./RAG_CORE_OPTIMIZATION.md). Sau khi đổi package, clean output build cũ trước khi khởi động lại.

## Chất lượng dữ liệu

Sự tồn tại và mô tả được đối chiếu nguồn ngày 30/10/2026; phạm vi kiểm chứng thể hiện ở `verificationScope`.

| Địa điểm | Nguồn |
|---|---|
| Bảo tàng Điêu khắc Chăm | [Hộ chiếu di sản của cổng du lịch Đà Nẵng](https://danangfantasticity.com/wp-content/uploads/2026/01/Ho-Chieu-Di-san-Da-Nang.pdf) |
| Bảo tàng Mỹ thuật Đà Nẵng | [Website bảo tàng](https://baotangdanang.vn/gioi-thieu-ve-bao-tang-my-thuat-da-nang) |
| Bảo tàng Đà Nẵng | [Website bảo tàng](https://www.baotangdanang.vn/) |
| Chợ Hàn | [Danh mục trong tài liệu cổng thương mại thành phố](https://trademap.danang.gov.vn/HuongDanSuDung.pdf) |
| Mỹ Khê | [Cổng du lịch thành phố](https://danangfantasticity.com/en/bien-vi/bien-my-khe) |
| Công viên Biển Đông | [Visit Da Nang](https://visitdanang.travel/vi/cong-vien-bien-dong) |
| Điểm ngắm Cầu Rồng | [Cổng thông tin thành phố](https://cttdt.danangportal.gov.vn/vi/w/cau-rong-i) |
| Chùa Linh Ứng Bãi Bụt | [Website chùa](https://chualinhung.vn/) |

Tọa độ là điểm neo xấp xỉ phục vụ demo, không phải cổng vào đã xác minh. Thời lượng là ước lượng lập kế hoạch. Giờ hoạt động để UNKNOWN/null, chưa đưa giá vé hoặc lịch biểu diễn vào dữ liệu. Không trình bày toàn bộ record như đã được xác minh từng thuộc tính. Chợ Cồn/Ngũ Hành Sơn chưa có trong seed đầu tiên.

## Khởi động

Yêu cầu: Docker Desktop đang chạy, Java 21, Maven. Tại `E:/travel-system`:

```powershell
# Optional for real semantic retrieval; do not commit the key.
$env:GEMINI_API_KEY = '<your-key>'
./scripts/Start-RagDemo.ps1
```

Script tạo `.env.rag-demo` bị Git ignore với mật khẩu DB và khóa JWT demo riêng; không in nội dung đó, không thay `.env` hiện có. Backend mặc định `http://localhost:18080`, Swagger `/swagger-ui.html`. Dừng backend bằng Ctrl+C; giữ DB demo để lần sau dùng tiếp. Không chạy docker down với `-v` nếu muốn giữ dữ liệu.

Trong Postman chọn Authorization → No Auth cho cả ba API demo; không cần tạo account. Các API nghiệp vụ khác vẫn giữ xác thực. Nếu backend đang chạy bản cũ, dừng bằng Ctrl+C và chạy lại script để nạp các đường dẫn public mới.

## API và ví dụ

```text
GET  /api/v1/public/planning/retrieval/catalog
POST /api/v1/public/planning/retrieval/index
POST /api/v1/public/planning/retrieval/search
```

Index: body rỗng. Trả `updated`, `skipped`, `failed`, `model`; phải kiểm tra `failed`, không coi HTTP 200 là mọi embedding đã thành công. Gọi lại chỉ xử lý nội dung chưa có embedding hiện hành. Khóa chống gọi trùng hiện ở một backend instance, phù hợp demo; chưa là distributed job.

Search mẫu cho nhóm văn hóa:

```json
{"positiveQuery":"lịch sử văn hóa nghệ thuật bảo tàng", "excludedTags":["BEACH"], "limit":3}
```

Search mẫu cho nhóm cảnh quan:

```json
{"positiveQuery":"ngắm biển cảnh quan dạo bộ thư giãn", "excludedTags":["MUSEUM"], "limit":4}
```

Chưa gửi câu có phủ định vào `positiveQuery` rồi kỳ vọng endpoint tự hiểu phủ định. Tag chuẩn trong seed: MUSEUM, HISTORY, CULTURE, ART, LOCAL, MARKET, SHOPPING, BEACH, SCENERY, RELAX, OUTDOOR, PARK, ARCHITECTURE, TEMPLE.

`retrievalMode=HYBRID` khi có embeddings hiện hành và query embedding thành công; thiếu key/provider lỗi sẽ là `LEXICAL_FALLBACK_EMBEDDING_UNAVAILABLE`; chưa index là `LEXICAL_ONLY_NO_CURRENT_EMBEDDINGS`. `rankScore` là điểm hợp nhất thứ hạng, không phải xác suất phù hợp. Top-k semantic luôn là các ứng viên gần nhất, chưa xác nhận một tên riêng lạ tồn tại; bước intent/entity resolution tiếp theo chịu trách nhiệm xử lý must-visit không tìm thấy.

## Kiểm thử

```powershell
cd backend
mvn '-Dtest=GeminiEmbeddingClientTest,RagCatalogIntegrationTest,RagApiIntegrationTest,RagDemoProfileTest' test
```

Integration tests dùng PostgreSQL + pgvector thật trong Testcontainers và migrations thật. Vector fixture chỉ kiểm tra cơ chế SQL/ranking/ID; không được trình bày thành kết quả chất lượng model embedding thật. Provider HTTP tests dùng MockRestServiceServer. Chạy riêng thử nghiệm có GEMINI_API_KEY trước khi khẳng định demo semantic đã hoạt động với provider thật.

Thay đổi text/tags/name làm embedding cũ bị loại theo SHA-256. Re-index để cập nhật. Thay model phải index lại; profile này cố định vector(768), không đổi dimension qua biến môi trường.

## Kết quả kiểm tra ngày 30/10/2026

- `mvn test`: 249 tests, 0 failures, 0 errors, 0 skipped (53 test suites).
- Trong đó 15 tests mới cho embedding client, SQL/seed/vector/FK và API/phân quyền.
- Database demo thực tế đã seed 8 địa điểm; backend khởi động trên cổng 18080 và `/actuator/health` trả UP.
- Chưa chạy indexing/query embedding với Gemini thật vì chưa có GEMINI_API_KEY trong môi trường local được kiểm tra. Không có embedding giả được nạp vào database demo; fixture chỉ dùng trong database Testcontainers của tests.

## Quyết định kiến trúc mở rộng

Người dùng đã yêu cầu thu nhỏ nhưng triển khai RAG/vector cho demo. Đây là ngoại lệ được chấp thuận với giới hạn embeddings/vector của phase 4C cũ: dùng pgvector trong PostgreSQL, seed chỉ trong profile demo, chưa thêm framework AI hoặc thay thuật toán. Khi hoàn thành orchestrator mới cần cập nhật ngân sách gọi AI của phase 4C; pipeline cũ hiện không thay đổi.
