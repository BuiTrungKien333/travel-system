# Tổ chức và tối ưu core RAG — 02/11/2026

## Phạm vi

Refactor các feature `planning/draft`, `planning/ai/intent` và `planning/retrieval` theo modular layered architecture + package-by-feature của backend/AGENTS.md. URL, request JSON và các trường response cũ được giữ; executionReport.stages bổ sung elapsedMillis và hỗ trợ AI_WITH_FALLBACK khi chủ đề khác nguồn giữa các ngày. Không thay Held–Karp, thêm dependency Maven, tạo service interface chỉ có một implementation, migration, lưu itinerary hoặc nối mobile.

## Cấu trúc

```text
planning/
├── draft/
│   ├── config/DraftExecutionConfig.java
│   ├── controller/DraftController.java
│   ├── dto/request/GenerateDraftRequest.java
│   ├── dto/response/DraftResponse.java
│   ├── model/DraftAllocation.java
│   ├── provider/GeminiDraftClient.java
│   └── service/
│       ├── DraftService.java
│       ├── DraftAllocationService.java
│       ├── DraftSchedulingService.java
│       ├── DraftTimelinePlanner.java
│       ├── DraftThemeService.java
│       └── DraftWeatherService.java
├── ai/intent/
│   ├── controller/
│   ├── dto/response/
│   ├── model/
│   ├── provider/
│   ├── service/
│   └── validator/
└── retrieval/
    ├── config/
    ├── controller/
    ├── dto/response/
    ├── model/
    ├── provider/
    ├── repository/
    └── service/
```

Controller chỉ nhận/validate request và gọi service. DTO không phụ thuộc controller hoặc service. DraftService điều phối và kiểm tra điều kiện nghiệp vụ; client Gemini chỉ tạo payload/gọi provider/kiểm tra refs. Các service còn lại phụ trách fallback, dựng/sửa lịch, biên tập chủ đề và thời tiết. Không có workflow framework, generic pipeline hay bộ chấm điểm mới.

## Các thay đổi giảm thời gian chờ

1. **Gộp allocation và theme hints vào cùng một generation.** Luồng text giảm từ 3 xuống 2 lần generation; query embedding vẫn riêng. Context gửi cho allocation chỉ có preferences cần thiết và thẻ địa điểm, không lặp toàn bộ intent/evidence.
2. **Kiểm tra chủ đề sau lịch cuối bằng backend.** Chỉ dùng theme/UUID highlight nếu vẫn có căn cứ trong tags và thuộc đúng ngày sau repair. Nếu không hợp lệ thì template fallback cho ngày đó; không gọi LLM lần nữa. Có thể có THEMES.source=AI_WITH_FALLBACK.
3. **Weather chạy song song với allocation.** Executor có 2–4 worker, hàng đợi 16. Ngân sách mặc định 2.000 ms từ lúc khởi chạy; quá hạn/bận/lỗi trả UNKNOWN. Hủy là best effort, I/O còn chịu socket timeout của WeatherProvider. Chỉnh bằng RAG_WEATHER_WAIT_MILLIS.
4. **Chặn input thiếu điều kiện lập lịch trước query embedding.** Thiếu số ngày/giờ hoặc origin ngoài phạm vi không cần trả chi phí embedding.
5. **Tái sử dụng ngày không đổi trong vòng repair.** Cache Map cục bộ trong một lần lập lịch giữ kết quả theo dayIndex + tập ID có thứ tự; không chạy lại Held–Karp cho ngày không đổi. Không có cache xuyên request nên không thêm cơ chế hết hạn/đồng bộ dữ liệu.

Luồng bình thường:

```text
Intent → kiểm tra input → retrieval → kiểm tra DB/quỹ giờ
                                      ↓
                         ┌─ weather (I/O độc lập) ─────────┐
                         └─ allocation + theme hints      │
                            → Held–Karp/timeline/repair   │
                            → kiểm tra/render chủ đề      │
                            → nhận forecast/UNKNOWN ──────┘
                            → response + executionReport
```

## Đọc nguồn và thời gian

- INTENT, RETRIEVAL, ALLOCATION đo riêng thời gian xử lý, gồm retry nếu có.
- THEMES có thể source=AI nhưng attempts=0: dùng kết quả generation của ALLOCATION, model được kế thừa, method=ALLOCATION_HINTS_AND_GROUNDED_TEMPLATE. Không cộng thêm generation call cho bước này.
- Nếu một số ngày dùng AI và một số ngày fallback, stage THEMES và overallMode thể hiện AI_WITH_FALLBACK; từng days[].summarySource vẫn tách rõ.
- WEATHER được đo từ lúc submit đến khi có kết quả. Vì chạy song song, tổng elapsedMillis của các stage có thể lớn hơn elapsedMillis toàn request.
- Solver luôn DETERMINISTIC. Các phản hồi UNKNOWN, fallback và chủ đề từ template không được ghi thành kết quả AI mới.
- Giữ chính sách một retry dùng chung toàn request và ngân sách AI 60 giây; worker thời tiết không truy cập ThreadLocal AI context.

## Kiểm chứng

`backend/mvnw.cmd -q clean test`: **328 tests, 0 failures, 0 errors, 0 skipped**. Có PostgreSQL 17/pgvector thật, full security chain, solver/timeline thật; mock tại ranh giới AI/embedding/weather.

Test bổ sung kiểm tra: allocation giữ điểm bắt buộc; context gọn vẫn có mô tả/giờ hoạt động; chủ đề không nhắc điểm đã bị repair bỏ; nguồn hỗn hợp được báo rõ; không có generation riêng cho THEMES; weather chạy đồng thời với allocation; timeout/cancel/executor bận; input thiếu giờ không gọi embedding; timing được ghi kể cả stage lỗi.

### Phép đo có kiểm soát

Backend HTTP thật + DB demo thật; provider AI/embedding/weather là HTTP server local **mô phỏng 1.000 ms mỗi lời gọi**. Cùng câu tự nhiên hai ngày, ba lần trước/sau; không dùng key Gemini thật, không dùng kết quả này để khẳng định tốc độ provider Gemini thực tế.

| Lần | Trước (ms) | Sau (ms) |
|---|---:|---:|
| 1 | 5.438 | 3.749 |
| 2 | 5.061 | 3.053 |
| 3 | 5.059 | 3.068 |
| Trung vị | 5.061 | 3.068 |

Trung vị giảm khoảng **39%**. Trước có 5 HTTP calls nối tiếp: intent, embedding, allocation, themes, weather. Sau có 4 calls: intent, embedding, allocation và weather; weather chồng thời gian với allocation. Không tăng retry hay thay solver để đạt kết quả này.

Cả ba lần sau đều READY_WITH_WARNINGS, hai ngày; THEMES=AI/attempts=0 và WEATHER=EXTERNAL_API trong môi trường mô phỏng. Số dòng itineraries trước/sau đều 0. Dữ liệu đo và fixture tạm nằm trong backend/target, không đưa vào source. Các server kiểm tra tạm được dừng sau khi đo.

## Chạy lại

Dừng backend cũ. Sau thay đổi package, chạy `backend/mvnw.cmd clean` một lần nếu đang dùng output build cũ, rồi chạy `./scripts/Start-RagDemo.ps1` như trước. Body Postman hiện có không cần đổi. Đo latency thực tế bằng executionReport.stages[].elapsedMillis; kết quả còn phụ thuộc model, mạng, retry và provider.
