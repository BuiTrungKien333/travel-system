# Demo retry và phân biệt AI/fallback

Chỉ áp dụng luồng `rag-demo`. Không lưu Trip/itinerary, không nối mobile. Giữ model đang cấu hình trong `.env.rag-demo`; không xoay key/project để né quota.

## 1. Chạy bình thường: nhập câu tự nhiên

```powershell
cd E:/travel-system
./scripts/Start-RagDemo.ps1
```

Postman: **No Auth**, **POST**, Body → raw → JSON:

```text
http://localhost:18080/api/v1/public/planning/generate-draft
```

```json
{
  "text": "Tôi đi Đà Nẵng hai ngày, thích lịch sử, nghệ thuật và khám phá địa phương, không đi biển, mỗi ngày từ 8 giờ đến 17 giờ, đi thong thả.",
  "startDate": "2026-11-02",
  "origin": { "name": "Điểm xuất phát demo", "latitude": 16.065, "longitude": 108.223 },
  "roundTrip": true,
  "restMinutes": 45
}
```

Đọc `data.status`, `data.issues`, sau đó `data.executionReport`. HTTP 200 chỉ có nghĩa đã xử lý yêu cầu; chưa khẳng định có lịch hợp lệ.

Nếu không lấy được intent bằng AI: `status=NEEDS_STRUCTURED_INPUT`, `analysis=null`, `days=[]`; bước INTENT là UNAVAILABLE và các bước sau SKIPPED. Gửi request ở mục 2 để tiếp tục. Hệ thống không tự bịa sở thích hoặc giả vờ đã hiểu câu tự nhiên.

## 2. Đầu vào có cấu trúc: bỏ qua AI đọc câu

Cùng URL, thay toàn bộ body bằng:

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
    "mustVisitNames": ["Bảo tàng Điêu khắc Chăm"],
    "excludedPlaceNames": []
  },
  "startDate": "2026-11-02",
  "origin": { "name": "Điểm xuất phát demo", "latitude": 16.065, "longitude": 108.223 },
  "roundTrip": true,
  "restMinutes": 45
}
```

Cung cấp đúng một trong `text` không rỗng hoặc `preferences`; gửi cả hai/thiếu cả hai trả 400. Preferences yêu cầu destination, numberOfDays 1–2 và giờ HH:mm hợp lệ. Các list mặc định rỗng, tối đa 10 phần tử/list; tên địa điểm tối đa 160 ký tự. Khung bắt đầu phải sớm hơn kết thúc; mâu thuẫn và tên không xác định trong catalog trả NEEDS_INPUT. travelPace tùy chọn: RELAXED/BALANCED/ACTIVE.

Tags: MUSEUM, HISTORY, CULTURE, ART, LOCAL, MARKET, SHOPPING, BEACH, SCENERY, RELAX, OUTDOOR, PARK, ARCHITECTURE, TEMPLE. Không nhận tag ngoài danh sách. Tên điểm vẫn được resolve từ DB, giữ điểm bắt buộc và áp dụng loại trừ như luồng text.

INTENT sẽ là USER_PROVIDED, model của analysis là null và evidence rỗng. Các bước embedding/phân bổ/chủ đề **vẫn có thể dùng AI** nếu khả dụng; dùng preferences không đồng nghĩa toàn luồng không có AI.

## 3. Demo fallback ổn định, không cần gọi Gemini

Dừng backend cũ bằng Ctrl+C, rồi chạy:

```powershell
./scripts/Start-RagDemo.ps1 -WithoutAi
```

Switch này chỉ bỏ key khỏi tiến trình chạy lần này, không sửa key trong `.env.rag-demo`. Chạy lại không có switch để bật AI. Nếu cổng bận có thể thêm `-Port 18081` rồi đổi cổng trong Postman.

Gửi body preferences ở mục 2. Kỳ vọng có lịch từ dữ liệu DB và thuật toán, thường READY_WITH_WARNINGS vì giờ mở cửa demo UNKNOWN:

| Bước | source | Cách thực hiện |
|---|---|---|
| INTENT | USER_PROVIDED | Nhận preferences trực tiếp, không gọi LLM |
| RETRIEVAL | FALLBACK | Tìm từ khóa trên DB; reason=MISSING_API_KEY |
| ALLOCATION | FALLBACK | Chia ngày theo tải/thời lượng và sở thích; giữ điểm bắt buộc |
| OPTIMIZATION | DETERMINISTIC | Held–Karp, TimelineEngine và sửa có giới hạn |
| THEMES | FALLBACK | Mẫu văn bản từ các địa điểm đã xếp |
| WEATHER | EXTERNAL_API hoặc UNAVAILABLE | Dự báo thật tại origin nếu lấy được; không phải Gemini |

Các bước AI không có key có attempts=0; không thực hiện HTTP call. overallMode=FALLBACK, retriesUsed=0. Nếu gửi body text trong lần chạy này, phải nhận NEEDS_STRUCTURED_INPUT, không được nhận intent giả.

## 4. Hợp đồng nguồn kết quả

`executionReport` gồm overallMode, deadlineMillis, elapsedMillis, retriesUsed và stages theo thứ tự INTENT → RETRIEVAL → ALLOCATION → OPTIMIZATION → THEMES → WEATHER.

| source | Ý nghĩa |
|---|---|
| AI | Kết quả AI hợp lệ được sử dụng ở bước này |
| FALLBACK | Đã dùng phương án thay thế; xem method và reason |
| USER_PROVIDED | Dữ liệu người dùng nhập trực tiếp, không phải AI trích xuất |
| DETERMINISTIC | Thuật toán xác định, không phải AI |
| EXTERNAL_API | Dữ liệu API ngoài, dùng cho dự báo thời tiết thật |
| UNAVAILABLE | Không có kết quả dùng được, không thay bằng dữ liệu giả |
| SKIPPED | Chưa chạy bước này, ví dụ bị chặn ở intent/validation |

Mỗi stage có model, attempts (số lần thử gọi provider, gồm lần đầu), retries, reason, lastProviderStatus (mã lỗi provider gần nhất, có thể vẫn là 503 sau khi retry thành công), retryDecision. Không trả API key, prompt hoặc raw provider error.

overallMode=AI_ASSISTED khi có ít nhất một bước dùng AI và không có fallback; AI_WITH_FALLBACK khi có cả hai; FALLBACK khi chỉ có fallback; USER_PROVIDED khi mới xử lý đầu vào trực tiếp; UNAVAILABLE khi chưa có kết quả. Phải nhìn từng stage để biết chính xác phần nào dùng AI: AI_ASSISTED không có nghĩa solver hoặc thời tiết chạy bằng AI.

HYBRID_RRF dùng embedding AI + tìm từ khóa + RRF xác định; chỉ ghi AI khi truy xuất vector hiện hành được dùng. Nếu đã gọi embedding nhưng DB chưa có vector đúng model/hash thì RETRIEVAL=FALLBACK, reason=NO_CURRENT_EMBEDDINGS. Cần gọi endpoint index trước để trình diễn hybrid retrieval thực sự.

Query sinh từ intent ghép OR giữa các nhóm sở thích/tên điểm đã resolve, giữ AND trong mỗi cụm. Ví dụ bảo tàng và địa phương có thể trả các địa điểm thuộc một hoặc cả hai nhóm; không bắt mọi địa điểm phải đồng thời là bảo tàng và chợ. Bộ lọc loại trừ và điều kiện giữ điểm bắt buộc vẫn áp dụng riêng.

THEMES=AI có nghĩa AI chọn enum chủ đề và refs hợp lệ, backend dựng câu từ template; không phải mô hình tự viết toàn bộ đoạn văn. metadata.allocationSource và days[].summarySource cũ được giữ để dễ đối chiếu.

metadata.model là model đang cấu hình, không chứng minh model đã được gọi. Chỉ executionReport thể hiện nguồn thực tế và model/attempts của từng bước.

## 5. Chính sách retry và thời gian chờ

- Tối đa **một lần retry dùng chung cả request**, không phải mỗi bước được retry một lần. Luồng text có tối đa bốn tác vụ AI ban đầu và thêm một attempt nếu được phép.
- Chỉ retry HTTP 429 tạm thời, 500/502/503/504 hoặc timeout. Không retry lỗi quyền/key (401/403), model không tồn tại (404), request sai (400), JSON/schema/refs không hợp lệ, hoặc quota ngày/tháng/spend/giới hạn bằng 0 khi nhận diện được từ QuotaFailure.
- Dùng cùng key đang cấu hình. Chờ ít nhất max(backoff+jitter, Retry-After, RetryInfo.retryDelay). Với chỉ một retry, có một bước backoff; không có chuỗi retry tăng dần.
- Nếu thời gian provider yêu cầu chờ vượt ngân sách còn lại, không retry: RETRY_WAIT_EXCEEDS_DEADLINE. Nếu retry đã được dùng ở bước khác: REQUEST_RETRY_LIMIT_REACHED.
- Mặc định ngân sách AI 60.000 ms cho cả luồng, connect/read theo phần thời gian còn lại và tối đa 30.000 ms mỗi lần gọi. Kiểm tra deadline trước/sau provider; kết quả đến quá hạn không được dùng. Đây là ngân sách cho các tác vụ AI, **không phải cam kết toàn HTTP request kết thúc trong 60 giây**: truy vấn DB, solver, weather và overhead vẫn cần thời gian; read timeout là thời gian chờ socket, không phải hạn cứng toàn response stream.
- Có thể chỉnh RAG_AI_DEADLINE_MILLIS, RAG_AI_CALL_TIMEOUT_MILLIS, RAG_AI_RETRY_BACKOFF_MILLIS trong môi trường hoặc `.env.rag-demo`. Không nên tăng timeout/retry chỉ để che lỗi quota.

Hướng dẫn chính thức: [Gemini troubleshooting](https://ai.google.dev/gemini-api/docs/troubleshooting), [Gemini rate limits](https://ai.google.dev/gemini-api/docs/rate-limits).

## 6. Kiểm tra thủ công và giới hạn

1. Chạy không AI, gửi text: NEEDS_STRUCTURED_INPUT; không có analysis/lịch giả.
2. Cùng lần chạy, gửi preferences: lịch hai ngày, không trùng điểm, giữ Bảo tàng Điêu khắc Chăm, không có tag BEACH. Kiểm tra tất cả placeId bằng catalog/DB.
3. Đổi excludedTags thành MUSEUM trong body mẫu: NEEDS_INPUT do mâu thuẫn; không âm thầm bỏ điểm bắt buộc.
4. Đổi dailyStartTime thành 25:00 hoặc numberOfDays thành 3: HTTP 400.
5. Chạy lại có AI và index trước khi gửi text: kiểm tra nguồn từng bước theo kết quả thực tế; provider có thể lỗi và fallback là kết quả hợp lệ. Không báo rằng đã dùng toàn bộ AI chỉ vì request thành công.
6. Các nhánh retry 429/503, thời gian chờ, deadline và chia sẻ retry được kiểm tra bằng provider mô phỏng trong unit test; không cần cố tạo tải để làm cạn quota thật.

Chất lượng tuyến và dữ liệu giữ các giới hạn trong [hướng dẫn draft](./RAG_DRAFT_POSTMAN.md): Haversine/35 km/h là ước tính; tối ưu trong từng ngày; giờ hoạt động UNKNOWN có cảnh báo. Chưa lưu, Apply hoặc tích hợp mobile.

## Kết quả xác minh ngày 01/11/2026

- Bộ test backend và các lượt bổ sung/liên quan có tổng 318 tests: 0 failures, 0 errors, 0 skipped. Có kiểm tra PostgreSQL 17 + pgvector thật, solver/timeline thật; AI/embedding/weather được mô phỏng ở ranh giới provider trong test.
- Mock HTTP xác nhận 503 → thành công dùng đúng key ban đầu, bước tiếp theo không được thêm retry; các test riêng kiểm tra 429, Retry-After số/ngày HTTP, RetryInfo phân số, timeout, quota ngày, deadline và dọn request context sau lỗi.
- Chạy server thật trên cổng tạm 18081 với -WithoutAi: body preferences mẫu trả READY_WITH_WARNINGS, overallMode=FALLBACK, 2 ngày/4 điểm (Bảo tàng Mỹ thuật Đà Nẵng, Bảo tàng Điêu khắc Chăm, Chợ Hàn, Bảo tàng Đà Nẵng). Tất cả ID có trong catalog, không BEACH, không trùng điểm. Đây là một lần quan sát, không hard-code lịch.
- Số dòng itineraries trước/sau lần gọi đều bằng 0. Thời tiết lần kiểm tra lấy được nguồn open-meteo và được ghi EXTERNAL_API, không gắn nhãn AI.
- Gửi text khi không có key trả NEEDS_STRUCTURED_INPUT, analysis=null, days=[], INTENT=UNAVAILABLE/MISSING_API_KEY, bước sau SKIPPED.
- Server tạm đã dừng sau kiểm tra. Nhánh dùng AI thành công được kiểm tra bằng provider mô phỏng; không khẳng định đã chạy trọn provider Gemini thật trong lượt thay đổi này.
