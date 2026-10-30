# Kế hoạch triển khai lập hành trình từ ngôn ngữ tự nhiên bằng RAG và Held–Karp

Ngày lập: 30/10/2026. Trạng thái: kế hoạch đề xuất để triển khai, chưa thay đổi runtime/API/database.

> Cập nhật phạm vi theo yêu cầu demo nhanh: ưu tiên [DEMO_AI_RAG_SCOPE.md](./DEMO_AI_RAG_SCOPE.md): Đà Nẵng, khoảng 10 địa điểm, chuyến đi 1–2 ngày, 3 trường hợp chính và 2 trường hợp kiểm tra biên. Các con số 60–100 địa điểm, bộ benchmark lớn và thời lượng bên dưới thuộc kế hoạch đầy đủ, không phải yêu cầu hoàn thành trước demo.

## 1. Kết quả cần đạt

Người dùng mô tả nhu cầu bằng tiếng Việt. Hệ thống hiểu yêu cầu, truy xuất địa điểm đã kiểm chứng trong PostgreSQL bằng hybrid retrieval, lựa chọn và phân bổ điểm theo ngày, tối ưu thứ tự bằng Held–Karp, dựng timeline, kiểm tra tính khả thi, rồi sinh chủ đề và tóm tắt từng ngày. Người dùng xem trước, chỉnh sửa và xác nhận trước khi lưu.

Ví dụ: “Đi Huế hai ngày, thích lịch sử và cà phê yên tĩnh, đi thong thả, xuất phát lúc 8 giờ từ khách sạn và về trước 18 giờ.”

Kết quả gồm: cách hệ thống hiểu nhu cầu; địa điểm và lý do phù hợp; lịch từng ngày; nguồn dữ liệu và giới hạn kiểm chứng; thời gian/cự ly; cảnh báo; điểm chưa xếp được; chủ đề ngày; nút sửa/tạo lại/xác nhận.

Cam kết chính: mọi địa điểm được chấp nhận/lưu phải thuộc tập ứng viên hợp lệ và tồn tại trong database tại thời điểm kiểm tra/lưu. Không cam kết LLM không bao giờ sinh ID sai hoặc mọi thuộc tính của địa điểm luôn đúng ngoài thực tế.

## 2. Cơ sở từ repository

| Thành phần | Hiện trạng đã quan sát | Việc cần làm |
|---|---|---|
| `planning/algorithm/HeldKarpSolver` | Giải thứ tự, tối đa 16 điểm đầu vào | Giữ thuật toán; tính cả origin khi giới hạn; không đưa toàn bộ retrieval candidates vào solver |
| `HaversineDistanceMatrixService`, `DistanceCalculator` | Khoảng cách chim bay, thời gian theo 35 km/h | Gắn nhãn ước lượng; bổ sung routing theo phương tiện khi đưa vào dùng thực tế |
| `planning/timeline/TimelineEngine` | Dựng và tính lại lịch | Bổ sung thời gian chờ thực sự vào timeline, nghỉ/đệm, giới hạn giờ kết thúc, bảo toàn origin và ngày |
| `planning/rule` | Giờ hoạt động, thời tiết, di chuyển, va chạm | Phân biệt lỗi kiểm tra/thiếu dữ liệu với đã kiểm tra đạt |
| `TripOptimizationServiceImpl` | Tối ưu từng ngày đã được phân điểm sẵn | Thêm chọn điểm và phân bổ ngày; tách đánh giá thuần khỏi gọi Copilot |
| `planning/ai/provider/GeminiAiProvider` | Tích hợp REST, JSON MIME, fallback | Thêm schema và kiểm tra nghiệp vụ theo từng nhiệm vụ; giới hạn lời gọi |
| `planning/ai/validator/AiResponseValidator` | Bù điểm bị thiếu vào ngày ít hoạt động | Không tái sử dụng kiểu bù này cho luồng mới vì chưa kiểm tra sức chứa ngày |
| `place/entity/Place` | Tên, địa chỉ, tọa độ, tỉnh, giờ, loại không gian | Thiếu mô tả trải nghiệm, trạng thái/nguồn xác minh, thời lượng chuẩn |
| `trip/entity/Trip`, `TripPlace` | Có Trip và liên kết địa điểm | Chưa đủ để biểu diễn/lưu trọn vẹn kết quả nhiều ngày trong Java domain hiện thấy |
| `V1__init_schema.sql` | Đã tạo `itineraries`, `itinerary_items` | Tận dụng bảng có sẵn; kiểm tra DB/schema thực tế, bổ sung entity/service và migration chênh lệch |
| `docker-compose.yml` | PostgreSQL 17 Alpine | Chuẩn bị PostgreSQL 17 có pgvector và bảo toàn dữ liệu |
| `E:/travel-mobile-ui` | Có giao diện hành trình và thao tác tối ưu | Xây luồng nhập yêu cầu, xác nhận ý định, preview và lưu bằng API thật |

Tài liệu `docs/phase-4/04C-ai-recommendation-architecture.md` đang đóng băng phạm vi cũ, loại embeddings/vector database và giới hạn 1 lần gọi + 1 retry. Luồng mới là mở rộng được người dùng yêu cầu, cần ghi ADR/sửa phạm vi tài liệu trước khi code; không xem kiến trúc mới là đã được triển khai.

Repository đang có thay đổi dở dang ở pom/config/skill và xóa travel-web. Không hoàn nguyên hoặc đưa các thay đổi đó vào phạm vi triển khai này.

## 3. Phạm vi phiên bản đầu

### Bắt buộc

- Một tỉnh/thành cho mỗi lần tạo; 1–5 ngày; tiếng Việt; giới hạn này là mặc định đề xuất cần ghi vào contract.
- Có ngày đi và điểm xuất phát rõ; cho phép người dùng bỏ origin nhưng phải ghi giới hạn kết quả.
- Chỉ dùng địa điểm đã được duyệt trong hệ thống. Dữ liệu thiếu phải được thể hiện.
- Sở thích, nhịp độ, thời gian hoạt động ngày, điểm bắt buộc và điểm loại trừ.
- PostgreSQL + pgvector; truy xuất kết hợp semantic, tên/từ khóa và metadata.
- Chọn điểm, chia ngày, Held–Karp, timeline, kiểm tra; giải thích điểm chưa xếp được.
- Tóm tắt/chủ đề từ lịch cuối cùng; preview, chỉnh sửa, xác nhận lưu, chống lưu trùng.
- Phân biệt nguồn AI, rule-based fallback, dữ liệu ước lượng và dữ liệu chưa có.
- Kiểm thử tính toàn vẹn, đánh giá chất lượng truy xuất và demo có thể tái hiện.

### Sau phiên bản đầu

- Nhiều tỉnh, lịch dài, tái lập kế hoạch theo GPS/thời gian thực, học sở thích từ lịch sử, nhật ký AI.
- Giá vé/ngân sách chính xác nếu chưa có dữ liệu đáng tin; không nhận là hard constraint đã bảo đảm khi chỉ có budget tier.
- Agent tự gọi tool, tìm địa điểm tùy ý ngoài DB, fine-tuning, streaming, message broker và database vector riêng.
- Tối ưu toàn cục đồng thời chọn điểm, chia ngày, time windows: không gán cam kết này cho Held–Karp hiện tại.

## 4. Kiến trúc và trách nhiệm

```text
Mobile nhập yêu cầu
  -> Intent extraction bằng LLM + validation + xác nhận thông tin thiếu
  -> Lọc metadata và hybrid retrieval trong PostgreSQL/pgvector
  -> Candidate snapshot từ dữ liệu chuẩn
  -> LLM chọn và đề xuất phân bổ ngày từ snapshot
  -> Backend kiểm tra ID, yêu cầu bắt buộc và phân bổ
  -> Held–Karp từng ngày -> Timeline -> Rule evaluation
  -> Sửa có giới hạn nếu lịch chưa khả thi
  -> LLM sinh chủ đề/tóm tắt từ lịch cuối
  -> Preview -> chỉnh sửa/tái kiểm tra -> xác nhận lưu bằng transaction
```

Đề xuất mở rộng trong module `planning`, ví dụ nhóm `intent`, `retrieval`, `generation`; tái sử dụng provider hiện tại, tránh một kiến trúc AI song song. Chỉ tách component khi có trách nhiệm rõ. Backend điều phối DB/API; provider nhận context và trả DTO, không tự truy cập DB.

Trước khi triển khai cần chốt model embedding và generation bằng thử nghiệm tiếng Việt trên dữ liệu thật. Cấu hình model, dimension, schema/prompt version; không hard-code model từ ví dụ. Không cần thêm Spring AI chỉ để sử dụng vector nếu JDBC/native query và REST hiện có đáp ứng đủ.

## 5. Dữ liệu và bất biến nghiệp vụ

### 5.1. Dữ liệu địa điểm

Các field dưới đây là đề xuất mới, không phải field đã tồn tại:

- Mô tả đã duyệt, category/tags, thời lượng tham quan mặc định và nguồn ước lượng.
- Trạng thái kiểm chứng (`DRAFT/VERIFIED/REJECTED` là ví dụ tên đề xuất), nguồn và thời điểm xác minh, trạng thái còn hoạt động.
- Mức vận động/khả năng tiếp cận/chỗ nghỉ/giá chỉ thêm nếu có dữ liệu và phạm vi thực sự sử dụng; unknown không được chuyển thành phù hợp.
- Phân biệt xác minh sự tồn tại địa điểm với xác minh giờ mở cửa hay giá vé. Không dùng một nhãn VERIFIED để khẳng định mọi thuộc tính.

Tạo dữ liệu pilot đề xuất: 60–100 địa điểm thuộc một tỉnh/thành, đa dạng trải nghiệm, có tọa độ, nguồn xác minh và thời lượng. Không bịa mô tả “yên tĩnh”, “phù hợp người cao tuổi” để cải thiện retrieval. Nội dung AI hỗ trợ biên tập phải được duyệt.

### 5.2. Dữ liệu vector

Đề xuất bảng `place_embeddings` liên kết FK tới `places`; một bản ghi/địa điểm/model đang sử dụng, chứa văn bản truy xuất, vector, model/version, dimension theo schema, content hash, thời điểm cập nhật.

- Chọn dimension trước migration; không trộn vector khác model/dimension vào cùng không gian tìm kiếm.
- Chỉ embedding văn bản đã duyệt, bỏ tọa độ thô và dữ liệu thay đổi nhanh khỏi nội dung ngữ nghĩa chính.
- Cập nhật văn bản bằng content hash; chỉ re-embed khi đổi; lệnh batch có checkpoint/báo cáo lỗi và chạy lại được.
- Sau khi gọi embedding, kiểm tra content hash chưa đổi trước khi lưu để tránh ghi vector cũ lên mô tả mới.
- Xóa địa điểm xử lý FK; bỏ duyệt phải bị lọc ngay, kể cả khi vector vẫn còn.
- Giai đoạn pilot dùng exact vector search để dễ kiểm chứng. Chỉ bổ sung HNSW khi đo thấy cần và có kiểm tra recall sau lọc.

Giờ mở cửa và thời tiết được đọc lại lúc dựng lịch; không dùng văn bản embedding làm nguồn chính cho dữ liệu động.

### 5.3. Bất biến của kết quả

1. ID được nhận phải thuộc candidate snapshot của lần tạo và còn hợp lệ trong DB; điểm bắt buộc hợp lệ được bổ sung vào snapshot trước khi LLM chọn.
2. ID lạ/trùng sai quy tắc/payload sai bị từ chối. Không sửa bằng UUID ngẫu nhiên, không ánh xạ phỏng đoán theo tên.
3. Tên, vị trí, giờ hoạt động chuẩn lấy từ DB. Origin do người dùng cung cấp là điểm neo riêng; không biến thành địa điểm DB giả.
4. Mỗi địa điểm tham quan tối đa một lần trong MVP, ngoại trừ origin/quay về được biểu diễn riêng.
5. Hard constraints không được tự nới; không thể đáp ứng thì trả lý do và yêu cầu chỉnh sửa. Điểm ưu tiên có thể chưa được xếp nhưng phải có lý do.
6. Chủ đề ngày không được thêm trải nghiệm/địa điểm ngoài lịch, số liệu do backend tính.
7. Giao diện không ghi “đã kiểm tra đạt” khi checker lỗi hoặc dữ liệu thiếu.
8. Khi Apply phải kiểm tra quyền, revision, trạng thái địa điểm, ràng buộc và ghi atomic; preview không đồng nghĩa đã lưu.

## 6. Các giai đoạn triển khai

Các thời lượng là ước lượng ngày làm việc tập trung cho một người đã hiểu codebase; không phải cam kết deadline. Kiểm thử nằm trong từng giai đoạn.

### G0 — Chốt contract và baseline (1–2 ngày)

- [ ] Viết ADR mở rộng phạm vi AI/RAG; cập nhật tài liệu AI cũ ở mức thiết kế.
- [ ] Chốt phạm vi MVP, hard/soft constraints, nơi nhập origin, timezone Asia/Ho_Chi_Minh và cách trả unknown.
- [ ] Kiểm tra schema DB thật, entity và migration trước khi chọn field/bảng mới; kiểm kê dữ liệu địa điểm đủ điều kiện.
- [ ] Định nghĩa DTO intent, candidate, proposed allocation, validated itinerary, provenance, issues, generation metadata.
- [ ] Chốt giới hạn request: độ dài text, ngày/điểm, timeout, quota; không dùng endpoint public cũ cho generation tốn phí.
- [ ] Lập bộ yêu cầu kiểm thử/đánh giá ban đầu, lưu kết quả pipeline hiện tại làm baseline.

Đầu ra: ADR, API/schema dự thảo, bộ ví dụ và backlog. Nghiệm thu: không còn mơ hồ về nguồn ID, quyền ghi, tình huống thiếu dữ liệu và phạm vi thuật toán.

### G1 — Hạ tầng pgvector và dữ liệu pilot (3–5 ngày)

- [ ] Chọn image PostgreSQL 17 có extension pgvector, pin phiên bản sau xác minh tương thích. Backup và thử trên DB riêng trước khi đổi local volume; không xóa volume để cài extension.
- [ ] Flyway migration bổ sung metadata kiểm chứng/mô tả/thời lượng và vector table. Chọn migration number còn trống khi triển khai, không sửa V1 đã chạy.
- [ ] Cập nhật entity/query; tạo quy trình import/review đơn giản bằng batch/admin hiện có, chưa xây dashboard quản trị lớn.
- [ ] Chuẩn bị pilot, ghi nguồn và ngày xác minh, phân biệt unknown.
- [ ] Viết batch sinh embedding với provider configurable; chạy lại không tạo trùng; báo số thành công/thất bại/stale.
- [ ] Kiểm thử migration + vector bằng Testcontainers PostgreSQL có pgvector; không dùng H2 thay thế.

Đầu ra: DB có vector và dữ liệu pilot truy xuất được. Nghiệm thu: mọi vector nối tới địa điểm thật; nơi chưa duyệt không vào kết quả; rerun ổn định.

### G2 — Hiểu nhu cầu và xử lý thiếu thông tin (2–3 ngày)

- [ ] Thêm intent extraction với structured output, schema validation và business validation.
- [ ] Chuẩn hóa destination về provinceId từ danh mục thật; ngày tương đối dựa vào ngày hiện tại phía server theo timezone chuyến đi và hiển thị ngày đã hiểu.
- [ ] Phân biệt dữ liệu người dùng nói rõ, suy luận và thiếu; không tạo một điểm số confidence giả để coi là độ chắc chắn đã hiệu chuẩn.
- [ ] Nếu thiếu ngày/origin hoặc tên điểm mơ hồ: trả các câu hỏi/người dùng chọn địa điểm từ DB; chặn bước phụ thuộc dữ liệu đó.
- [ ] Người dùng xác nhận intent bằng form; dữ liệu sửa trực tiếp ưu tiên hơn kết quả trích xuất cũ.
- [ ] Khi provider lỗi: cho nhập form có cấu trúc để tiếp tục, không giả lập kết quả hiểu tự nhiên.

Nghiệm thu: đúng dữ liệu trong bộ intent test; xử lý câu phủ định “không đi chùa”, giờ cố định, tên địa điểm trùng và yêu cầu ngoài phạm vi.

### G3 — Hybrid retrieval có căn cứ (2–4 ngày)

- [ ] Query embedding từ intent đã xác nhận; phân tách sở thích tích cực và điều kiện loại trừ, không chỉ embedding nguyên câu chứa phủ định.
- [ ] Lọc địa điểm active/verified và province; tìm vector + tên/từ khóa có xử lý tiếng Việt/dấu; exact-name/must-visit có đường xử lý riêng.
- [ ] Hợp nhất thứ hạng bằng cơ chế đơn giản như Reciprocal Rank Fusion; không cộng trực tiếp cosine và điểm lexical khác thang đo.
- [ ] Cấu hình candidate limit khởi đầu 20–40 điểm cho pilot và điều chỉnh qua đánh giá; tránh phụ thuộc một ngưỡng similarity tùy tiện cho mọi truy vấn.
- [ ] Giữ sự đa dạng sở thích; trả candidate snapshot, lý do truy xuất và metadata nguồn. Không gọi điểm tương đồng là xác suất phù hợp.
- [ ] Không có đủ kết quả: báo thiếu dữ liệu, cho đổi sở thích/phạm vi; không tự mở rộng sang tỉnh khác hoặc sinh điểm mới.
- [ ] Khi embedding API lỗi: lexical/metadata fallback có nhãn; khi pgvector/DB lỗi thì trả lỗi phù hợp, không dùng dữ liệu giả.

Nghiệm thu: so sánh keyword, vector và hybrid trên bộ truy vấn gán nhãn; không có địa điểm sai vùng/bỏ duyệt; xử lý cả tên riêng và sở thích diễn đạt gián tiếp.

### G4 — Chọn điểm và đề xuất chia ngày (3–4 ngày)

- [ ] LLM nhận intent + candidate cards ngắn: mã P01... ánh xạ UUID, mô tả nguồn, vị trí, thời lượng, giờ và trạng thái dữ liệu.
- [ ] Output chỉ là ID tham chiếu theo ngày, thứ tự ưu tiên, lý do chọn và ứng viên dự phòng; không coi thời gian LLM viết là timeline chuẩn.
- [ ] Validate ID allowlist, ngày, trùng, điểm bắt buộc, loại trừ, số điểm/ngày và sức chứa thời gian sơ bộ.
- [ ] Xem xét tính gần nhau về địa lý và đa dạng trải nghiệm; kiểm tra tổng thời lượng trước khi gọi solver. Không cần thêm thuật toán clustering riêng ngay.
- [ ] Tránh chuyển mọi ứng viên thành điểm phải ghé. Hiển thị rõ điểm đã chọn/đề xuất thêm/không chọn.
- [ ] Fallback xác định: ưu tiên điểm bắt buộc, sau đó thứ hạng và gần cụm hiện có, thêm khi còn sức chứa; nếu không thể thì trả partial/infeasible minh bạch.

Nghiệm thu: tập lựa chọn hợp lệ, khác nhau hợp lý theo sở thích, không vượt solver limit, không mất điểm bắt buộc âm thầm.

### G5 — Tối ưu, dựng lịch và sửa lịch có giới hạn (4–6 ngày)

- [ ] Tách logic tính toán/đánh giá khỏi gọi AI trong `TripOptimizationServiceImpl`; giữ endpoint cũ tương thích. Luồng mới không gọi Copilot lặp từng ngày.
- [ ] Tạo DTO nội bộ từ DB, không tái sử dụng đường nhập ID tùy chọn rồi sinh UUID ngẫu nhiên của endpoint hiện tại.
- [ ] Held–Karp theo ngày với origin, roundTrip và số node hợp lệ; công khai loại cost tối ưu.
- [ ] Sửa TimelineEngine: arrival, visitStart sau chờ, departure, khoảng đệm/nghỉ, thời điểm quay về origin và giới hạn kết thúc ngày; qua đêm dùng timestamp/date rõ ràng.
- [ ] Truyền lịch hoạt động theo tuần đã hydrate từ DB xuyên suốt pipeline; tránh mất dữ liệu bổ sung giữa service và timeline.
- [ ] Có thời lượng mặc định theo dữ liệu; không tự rút ngắn xuống bất hợp lý chỉ để “fit”.
- [ ] Đánh giá thời tiết trên khoảng hoạt động, cache/gom lần lấy forecast trong một generation; missing/out-of-range không thành an toàn hoặc nắng đẹp.
- [ ] Loại bỏ mock weather fallback khỏi luồng dùng thật; mô phỏng demo có nhãn tách biệt.
- [ ] Sửa xác định tối đa 2 lượt: chuyển/đổi điểm tùy chọn, đổi ngày, tính lại; không sửa điểm bắt buộc hoặc hard constraints mà chưa có quyết định người dùng.
- [ ] Nếu vẫn không khả thi, trả nhóm xung đột và lựa chọn điều chỉnh; không trả READY giả. Đưa ứng viên dự phòng vào kiểm tra cùng quy tắc.
- [ ] Dùng routing matrix theo phương tiện nếu đáp ứng thời gian triển khai; nếu chưa, nhãn ESTIMATED_HAVERSINE xuyên suốt và không khẳng định thời gian đi đường chính xác. Số liệu sau Apply phải dùng cùng nguồn tính với preview.

Nghiệm thu: kết quả tính nhất quán từ origin tới chặng quay về, có chờ/nghỉ; không chồng lấn; hard conflict chặn READY; unknown được hiển thị. Kiểm thử quy tắc/solver xác định trước khi đánh giá LLM.

### G6 — Chủ đề từng ngày và kiểm soát số lần gọi (1–2 ngày)

- [ ] Gửi lịch cuối cùng, sở thích và các fact đã xác minh cho LLM sinh title/summary/selection reasons.
- [ ] Structured output; kiểm tra mã tham chiếu và độ dài. Không cho mô hình viết lại cự ly/giờ/giá trong text khi có thể render từ field chuẩn.
- [ ] Nội dung không có bằng chứng bị loại bỏ/đánh dấu; kiểm tra thủ công trên benchmark vì kiểm tra schema không chứng minh mọi câu văn đúng.
- [ ] Summary lỗi: dựng tiêu đề/tóm tắt theo template dựa trên category và điểm thật, không làm mất lịch hợp lệ.
- [ ] Mặc định 3 lần generation LLM cho toàn luồng: intent, selection/allocation, summary; tối đa 1 corrective retry tổng ở intent/selection, không retry summary. Một query embedding riêng, batch indexing không tính vào request người dùng.
- [ ] User sửa intent không cần gọi lại extraction. Sửa timeline không chạy lại retrieval nếu sở thích/phạm vi không đổi. Đo tokens/latency/cost theo stage.

Nghiệm thu: chủ đề đúng lịch cuối, không gọi AI theo từng địa điểm/ngày, có nguồn fallback và giới hạn request deadline tổng. Điều chỉnh timeout dựa trên đo thực tế, không cộng vô hạn timeout của từng stage.

### G7 — Preview, lưu và tích hợp mobile (4–6 ngày)

- [ ] Hoàn thiện domain/persistence `itineraries`/`itinerary_items` từ schema có sẵn; thêm dữ liệu preview/revision/expiry tối thiểu cần cho Apply. Không lưu tất cả candidate corpus hay embedding vào itinerary.
- [ ] API authenticated; kiểm tra ownership Trip/draft ở mọi thao tác. Không nhận currentSchedule tùy ý làm nguồn chuẩn để lưu.
- [ ] Lưu draft phía server; Apply dùng draftId/revision và idempotencyKey. Sửa lịch tạo revision mới; draft cũ/đã hết hạn trả xung đột rõ ràng.
- [ ] Trong transaction ngắn: kiểm tra trip revision, quyền, FK, địa điểm còn được duyệt, dữ liệu lịch và idempotency; ghi lịch atomic. Không gọi AI/weather/route API trong transaction.
- [ ] Nếu revision place/trip thay đổi cần tái kiểm tra trước và so sánh lại khi commit, trả 409 nếu stale; không dùng preview cũ âm thầm ghi đè lịch vừa sửa.
- [ ] Mobile: nhập nhu cầu -> xác nhận intent -> xem lịch -> chỉnh điểm/giờ/ngày -> tái kiểm tra -> xác nhận lưu. Giữ input khi lỗi và chống gửi lặp.
- [ ] Các trạng thái: cần bổ sung thông tin, không có ứng viên, đang tạo, lịch khả thi nhưng thiếu dữ liệu, lịch xung đột, lỗi provider, hết hạn preview, lưu thành công.
- [ ] Hiển thị vì sao chọn điểm và dữ liệu kiểm chứng tới đâu; thể hiện chủ đề ngày, nghỉ/chờ, điểm chưa xếp, weather/routing source.

Nghiệm thu: luồng end-to-end bằng API thật; sửa/lưu/tạo lại không mất dữ liệu người dùng; apply lặp không tạo lịch trùng; người khác không đọc/ghi draft.

### G8 — Đánh giá, hoàn thiện và báo cáo (3–4 ngày)

- [ ] Hoàn thành test suite theo mục 9 và chạy regression liên quan.
- [ ] Chạy benchmark retrieval và lịch trình trên bộ test giữ riêng; freeze model/prompt/data version, ghi seed nếu có.
- [ ] Đo request đầu/cuối, số call, thời gian từng stage và tỷ lệ fallback; kiểm tra ngân sách provider và giới hạn concurrency.
- [ ] Hoàn thiện OpenAPI, ADR, schema, cách seed/index lại, hạn chế và kết quả đánh giá thật.
- [ ] Chuẩn bị 5 kịch bản demo ở mục 10, bao gồm cả đường lỗi có chủ đích.

Nghiệm thu: có bằng chứng thực nghiệm, có thể chạy lại, không trình bày target hoặc dữ liệu mô phỏng như kết quả đã đo.

## 7. Contract API đề xuất

Tên endpoint/status/field dưới đây là thiết kế dự thảo, phải thống nhất với API conventions trước khi code.

| API | Mục đích |
|---|---|
| `POST /api/v1/planning/intents/parse` | Text và context -> intent, missing fields, câu hỏi, provenance |
| `POST /api/v1/trips/{tripId}/itinerary-drafts` | Intent đã xác nhận -> retrieval, selection, lập lịch, lưu preview |
| `GET /api/v1/trips/{tripId}/itinerary-drafts/{draftId}` | Đọc preview thuộc quyền người dùng |
| `PATCH /api/v1/trips/{tripId}/itinerary-drafts/{draftId}` | Thay đổi có kiểu rõ ràng -> revision mới sau kiểm tra |
| `POST /api/v1/trips/{tripId}/itinerary-drafts/{draftId}/apply` | Revision + idempotency key -> xác nhận lưu |

Nếu chưa có Trip, tạo Trip nháp qua use case phù hợp trước; không để LLM tạo/lưu Trip ngầm. Không dùng các URI đề xuất như contract đã tồn tại.

Generation response nên tách: `draftId`, `revision`, `status`, `interpretedIntent`, `days`, `unassignedPlaces`, `issues`, `dataQuality`, `metadata`. Các trạng thái đề xuất: NEEDS_INPUT, INSUFFICIENT_CANDIDATES, READY, READY_WITH_WARNINGS, INFEASIBLE. HTTP lỗi vẫn theo envelope/ErrorCode chung; không trộn lỗi provider và trạng thái nghiệp vụ.

Items phân biệt VISIT/BREAK/TRANSIT/ORIGIN nếu cần; VISIT bắt buộc FK placeId thật, nghỉ không được gán placeId giả. Metadata gồm model/prompt/embedding version, fallback, source khoảng cách/thời tiết, generatedAt; không trả prompt thô hoặc dữ liệu nhạy cảm.

## 8. Thứ tự và mốc bàn giao

| Mốc | Phụ thuộc | Sản phẩm review được |
|---|---|---|
| M1 | G0 + G1 | Dữ liệu đã duyệt và truy vấn vector trên PostgreSQL thật |
| M2 | M1 + G2 + G3 | Nhập câu tự nhiên -> xác nhận nhu cầu -> danh sách ứng viên có ID thật |
| M3 | M2 + G4 + G5 | Lịch nhiều ngày đã tính và kiểm tra, chưa cần văn bản đẹp |
| M4 | M3 + G6 + G7 | Demo đầy đủ từ nhập câu tới lưu lịch trên mobile |
| M5 | M4 + G8 | Bộ kết quả đánh giá và tài liệu báo cáo |

Tổng ước lượng: 23–36 ngày làm việc tập trung; nếu 5 ngày/tuần khoảng 5–8 tuần sau dự phòng. Dữ liệu cần thu thập lại hoặc routing phức tạp có thể kéo dài. Không có deadline từ người dùng nên chưa gán ngày lịch.

Không cắt validation, nguồn ID, kiểm chứng dữ liệu, Apply atomic hoặc kiểm tra timeline để rút thời gian. Nếu cần rút scope: một tỉnh, ba ngày, exact vector search, dữ liệu pilot nhỏ hơn nhưng đủ đa dạng, routing ước lượng có nhãn, chưa hỗ trợ ngân sách số tiền và realtime.

## 9. Kiểm thử và tiêu chí chất lượng

### Kiểm thử xác định trong CI

- Unit: intent validation, phủ định, hard/soft, ID allowlist, phân bổ, duplicate, số node gồm origin, round trip, thời gian chờ/nghỉ, cửa qua đêm, giới hạn cuối ngày.
- Unit/service: không đủ ứng viên, AI output sai, provider timeout/quota, summary fallback, ngân sách số call, checker lỗi -> unknown, không có dữ liệu weather.
- Repository/integration: PostgreSQL 17 + pgvector và Flyway thật; lọc verified/province, FK, vector dimension, update hash, nội dung cũ, ranking fixture cố định.
- Integration security/data: không quyền, draft hết hạn, revision cũ, địa điểm bị bỏ duyệt/xóa trước Apply, hai request Apply đồng thời, rollback khi một item lỗi, idempotency.
- Mock chỉ external boundaries AI/embedding/weather/route. Dùng vector fixture xác định để test truy vấn, không gọi model thật trong CI.
- Mobile: hợp đồng input/response, trạng thái async, lỗi retry, sửa draft và preview stale; lint/TypeScript và smoke thiết bị.

### Đánh giá LLM/RAG ngoài CI

- Bộ khởi đầu 40–60 yêu cầu tiếng Việt, tách development và holdout; gồm truy vấn tên riêng, sở thích, phủ định, thiếu thông tin, không thể đáp ứng và yêu cầu dụ tạo địa điểm ngoài DB.
- Gán nhãn địa điểm theo mức phù hợp 0–2 trên tập pilot, ghi rubric; ưu tiên hai người đánh giá và giải quyết bất đồng nếu có điều kiện.
- So sánh keyword-only, vector-only, hybrid trên cùng dữ liệu và k. Đo Recall@k, nDCG@k; nếu chỉ chấm một phần corpus thì ghi rõ giới hạn ground truth.
- So sánh itinerary cùng tập điểm trước/sau Held–Karp để đánh giá quãng đường; so sánh keyword pipeline và hybrid pipeline để đánh giá cá nhân hóa. Không dùng quãng đường khác nhau do bỏ điểm để tuyên bố thuật toán tối ưu tốt hơn.
- Đo extraction accuracy/F1 theo field, đáp ứng must-visit/khung giờ, số hard conflicts, mức phù hợp/đa dạng, số thao tác sửa, p50/p95 latency và cost/generation.
- Tối thiểu 3 lần chạy cho tập đánh giá generation đủ nhỏ để quan sát biến thiên; lưu raw result đã loại dữ liệu nhạy cảm, model/prompt/dataset version. LLM judge chỉ hỗ trợ, không thay ground truth nghiệp vụ.

### Release gates

1. 100% kết quả được chấp nhận/lưu thỏa tính toàn vẹn ID trong bộ kiểm thử; luôn có validation runtime và FK. Không suy rộng thành “mọi output LLM đều đúng”.
2. READY không có hard conflicts theo dữ liệu và checker đã thực hiện; unknown được trình bày riêng.
3. Hard constraint không thỏa phải bị chặn hoặc trả INFEASIBLE/NEEDS_INPUT, không âm thầm nới.
4. Không có ghi đè trái quyền, lưu trùng hoặc partial commit trong integration tests.
5. Hybrid phải có bằng chứng cải thiện/đánh đổi so với keyword trên holdout; nếu không, điều chỉnh dữ liệu/retrieval và báo cáo đúng kết quả.
6. Mục tiêu thử nghiệm: extraction trường cốt lõi >=90%, Recall@10 >=0.8 trên bộ có ground truth đủ, p95 tạo draft <=30 giây dưới tải demo đã định nghĩa. Đây là target ban đầu cần đo và hiệu chỉnh, không phải thành tích đã đạt.

## 10. Demo báo cáo

1. Hai người cùng đến Huế, một người thích lịch sử/thư thả, một người thích khám phá: so sánh tập địa điểm và lịch từ cùng kho dữ liệu.
2. Yêu cầu một địa điểm không có trong DB: hệ thống thông báo không tìm thấy/cho chọn kết quả xác minh, không sinh UUID giả.
3. Nhiều điểm bắt buộc nhưng chỉ có một buổi: trả không khả thi và nêu điều kiện cần thay đổi.
4. Weather/LLM lỗi: lịch hoặc form fallback vẫn dùng dữ liệu thật, nhãn nguồn rõ; không giả dự báo.
5. Địa điểm bị bỏ duyệt hoặc lịch bị sửa sau preview: Apply trả stale/conflict, không lưu bản cũ.

Báo cáo cần: sơ đồ pipeline, lược đồ dữ liệu vector, ví dụ candidate snapshot/validation, vai trò và giới hạn Held–Karp, màn hình trước/sau, bảng so sánh retrieval, đo latency/cost và hạn chế dữ liệu. Phân biệt live demo và fixture demo tái hiện; không đưa kết quả fixture thành chất lượng provider thực tế.

## 11. Việc bắt đầu ngay

1. G0: chốt pilot, ADR, contract ý định và quy tắc ID/Apply.
2. G1: kiểm kê và duyệt dữ liệu thật, pgvector trên DB riêng, migration và batch embedding.
3. G2–G3: hoàn thành lát cắt nhỏ nhất “câu tự nhiên -> danh sách địa điểm phù hợp có ID thật” trước khi mở rộng lập lịch.

Không sửa toàn bộ provider hoặc giao diện trước khi chứng minh retrieval có chất lượng trên dữ liệu pilot.

## 12. Nguồn kỹ thuật đã đối chiếu

- [pgvector: exact search, filtering, indexing và hybrid search](https://github.com/pgvector/pgvector). Bắt đầu exact search; chỉ dùng approximate index khi cần và đánh giá recall.
- [Gemini structured output](https://ai.google.dev/gemini-api/docs/structured-output?hl=en). Schema giúp kiểm soát cấu trúc, không thay thế kiểm tra fact/ID/nghiệp vụ.
- [Gemini embeddings](https://ai.google.dev/gemini-api/docs/embeddings). Chọn cấu hình query/document phù hợp model thực tế và đánh giá tiếng Việt trước khi chốt.

Tài liệu nội bộ cần cập nhật khi triển khai: phase 4C AI, phase 7 data model, phase 8 API, OpenAPI và hướng dẫn môi trường/index dữ liệu. Bản kế hoạch này chưa sửa các quyết định cũ hoặc cài thêm dependency.
