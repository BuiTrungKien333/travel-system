# Demo hiểu yêu cầu tự nhiên và truy xuất địa điểm

## Trạng thái và phạm vi

Mốc này bổ sung intent extraction bằng Gemini và nối với hybrid retrieval đã có. API public chỉ bật khi profile `rag-demo`; không cần token hoặc account.

Đã có: trích xuất điểm đến, số ngày, sở thích, nhóm/điểm bị loại, khung giờ, nhịp độ và tên điểm bắt buộc; kiểm tra JSON và dữ liệu nghiệp vụ; truy xuất ứng viên có ID từ catalog DB; trả câu hỏi khi thiếu thông tin cốt lõi/mâu thuẫn/không tìm được điểm.

Hai API trên trang này chỉ trích xuất và truy xuất, không tạo lịch. Luồng chọn/chia ngày, Held–Karp/timeline và chủ đề đã có ở API riêng [generate-draft](./RAG_DRAFT_POSTMAN.md), nhận thêm ngày bắt đầu và origin bằng dữ liệu có cấu trúc. Chưa lưu itinerary hoặc tích hợp mobile. Ngân sách và các yêu cầu đặc biệt ngoài schema chưa được xử lý.

## Chạy

Tại PowerShell trong `E:/travel-system`:

```powershell
$env:GEMINI_API_KEY = '<your-key>'
# Optional; default is gemini-3.8-flash. Use a model enabled for your API key.
$env:RAG_INTENT_MODEL = 'gemini-3.8-flash'
./scripts/Start-RagDemo.ps1
```

Nếu đã lưu các biến này trong `.env.rag-demo`, script cũng đọc file đó; giá trị trong file được script nạp vào process. Không đưa API key vào request Postman. Nếu backend cũ vẫn chạy thì Ctrl+C và khởi động lại; script không tự dừng tiến trình khác.

Generation và embedding là hai model khác nhau. Intent dùng `RAG_INTENT_MODEL`, vector dùng `RAG_EMBEDDING_MODEL` (mặc định gemini-embedding-001, 768 chiều). Không cần re-index khi chỉ đổi intent model. Chưa có vector thì chạy `/index` như hướng dẫn cũ.

## 1. Chỉ phân tích yêu cầu

POST `http://localhost:18080/api/v1/public/planning/retrieval/intent`

Postman: Authorization = No Auth, Body = raw JSON.

```json
{
  "text": "Tôi đi Đà Nẵng hai ngày, thích lịch sử và nghệ thuật, không đi biển, mỗi ngày từ 8 giờ đến 17 giờ, đi thong thả."
}
```

Kiểm tra các field trong `data.intent`: destination, numberOfDays=2, interests có HISTORY/ART, excludedTags có BEACH, dailyStartTime=08:00, dailyEndTime=17:00, travelPace=RELAXED. Mỗi field có thông tin phải kèm đoạn trích nguyên văn trong `evidence`; backend kiểm tra trích đoạn thuộc câu đầu vào. Đây là kiểm tra căn cứ trích xuất, không phải chứng minh tuyệt đối mọi suy luận ngữ nghĩa của model đều đúng.

`data.positiveQuery` do backend tạo chỉ từ sở thích tích cực và các điểm bắt buộc đã resolve; không đưa cả câu có phủ định sang embedding. `provinceCode=48` là phạm vi dữ liệu demo. Tên địa điểm được resolve về catalog DB, không lấy placeId do LLM sinh.

## 2. Phân tích và truy xuất trong một request

POST `http://localhost:18080/api/v1/public/planning/retrieval/search-from-text`

```json
{
  "text": "Tôi đi Đà Nẵng hai ngày, thích lịch sử và nghệ thuật, không đi biển, mỗi ngày từ 8 giờ đến 17 giờ, đi thong thả.",
  "limit": 3
}
```

Kết quả gồm:

- `data.status`: CANDIDATES_FOUND, NO_CANDIDATES, NEEDS_INPUT hoặc UNSUPPORTED_DESTINATION.
- `data.analysis`: cách hiểu, thông tin thiếu, câu hỏi, cảnh báo và model.
- `data.candidates`: retrievalMode, embeddingModel và matches; null khi có vấn đề chặn truy xuất.
- `data.candidates.matches[].place`: thông tin và ID thật của địa điểm.

`limit` không truyền mặc định 4; hợp lệ 1–8. Điểm bắt buộc được ưu tiên giữ và có trong `analysis.mustVisitPlaces`. Điểm được thêm trực tiếp từ yêu cầu bắt buộc có thể có rankScore=0 và cả hai cờ match=false; đó không phải điểm model sinh. Số điểm bắt buộc vượt limit trả NEEDS_INPUT thay vì âm thầm bỏ điểm.

## 3. So sánh sở thích khác nhau

```json
{
  "text": "Tôi đi Đà Nẵng một ngày, thích ngắm biển và thư giãn ngoài trời, không tham quan bảo tàng.",
  "limit": 4
}
```

Kỳ vọng excludedTags có MUSEUM và kết quả không có tag MUSEUM. Thứ hạng không bị hard-code; semantic chất lượng thực tế cần đánh giá trên model thật. Những giờ/số ngày chưa được nói sẽ null trong intent.

## 4. Các tình huống kiểm tra biên

Gửi cùng endpoint search-from-text, thay text:

| Câu mẫu | Kết quả cần kiểm tra |
|---|---|
| Tôi thích xem tranh và tìm hiểu nghệ thuật. | NEEDS_INPUT, thiếu destination, không tự mặc định Đà Nẵng |
| Tôi đi Huế hai ngày, thích lịch sử. | UNSUPPORTED_DESTINATION, không trả địa điểm Đà Nẵng |
| Tôi đi Đà Nẵng, bắt buộc ghé Địa điểm Demo Không Tồn Tại ZZZ. | NEEDS_INPUT, câu hỏi về điểm bắt buộc, không tạo ID |
| Tôi đi Đà Nẵng, muốn xem bảo tàng nhưng không muốn vào bảo tàng. | NEEDS_INPUT khi interests và exclusions mâu thuẫn |
| Tôi đi Đà Nẵng, bắt buộc ghé Mỹ Khê nhưng không đi biển. | NEEDS_INPUT khi điểm bắt buộc mâu thuẫn với nhóm bị loại |
| Tôi đi Đà Nẵng, thích ngắm cảnh nhưng không ghé Mỹ Khê. | Resolve điểm loại trừ cụ thể, không tự loại cả nhóm cảnh quan |
| Tôi đi Đà Nẵng, thích nghệ thuật, mỗi ngày đi từ 18 giờ đến 8 giờ. | NEEDS_INPUT, demo chưa hỗ trợ khung giờ qua đêm |

Nếu chỉ thiếu số ngày/giờ/nhịp độ nhưng đã có điểm đến và sở thích, hệ thống vẫn có thể truy xuất ứng viên; missingFields/warnings ghi rõ chưa đủ dữ liệu lập timeline.

## Lỗi và ngân sách gọi AI

- Một lần `/intent`: 1 generation call. `/search-from-text`: 1 generation + tối đa 1 query embedding; không gọi AI theo từng điểm/ngày, không tự retry.
- Input trống/quá 1.000 ký tự hoặc limit ngoài 1–8: HTTP 400.
- Không có key hoặc provider không dùng được: HTTP 503, AI_001. Không tạo intent giả hoặc dùng mock ngầm.
- Hạn ngạch intent bị vượt: HTTP 429, AI_003.
- Output sai schema/tag/kiểu/giờ/evidence, thiếu field hoặc bị cắt: HTTP 422, AI_005. Mã này dùng lại từ hệ thống hiện có; message chung có thể nhắc lịch trình dù đây là kiểm định intent.
- Nếu intent thành công nhưng query embedding lỗi: kết quả có thể dùng lexical fallback, thể hiện rõ retrievalMode. Đây không phải HYBRID thành công.

## Nguồn kỹ thuật

Kiểm tra trực tiếp trước khi chuyển mặc định sang 3.8, ngày 30/10/2026 với model `gemini-3.1-flash-lite` và 8 embeddings hiện có (không phải kết quả live của 3.8):

- Lịch sử/nghệ thuật, không đi biển: CANDIDATES_FOUND, HYBRID, trả ba bảo tàng.
- Ngắm biển/ngoài trời, không bảo tàng: CANDIDATES_FOUND, HYBRID, trả Mỹ Khê, Công viên Biển Đông, điểm ngắm Cầu Rồng và Linh Ứng Bãi Bụt.
- Không nói điểm đến: NEEDS_INPUT, destination=null, không truy xuất.
- Điểm bắt buộc ZZZ không có trong kho: NEEDS_INPUT, không sinh ID hoặc thay địa điểm.

Đây là smoke test bốn tình huống, không phải benchmark đánh giá độ chính xác tổng quát của LLM. Server tạm để kiểm tra dùng cổng khác với backend người dùng và được dừng sau kiểm tra.

- [Gemini structured output với generateContent](https://ai.google.dev/gemini-api/docs/generate-content/structured-output): dùng responseJsonSchema và JSON MIME, đồng thời vẫn kiểm tra nghiệp vụ ở backend.
- [Gemini 3.8 Flash](https://ai.google.dev/gemini-api/docs/models/gemini-3.8-flash): model mặc định configurable; không đổi model pipeline cũ.
