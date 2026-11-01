# BÁO CÁO TIẾN ĐỘ & KỊCH BẢN DEMO ỨNG DỤNG AI TRONG DỰ ÁN TRIPZY
> **Đề tài:** Hệ thống hỗ trợ lập kế hoạch và quản lý hành trình du lịch cá nhân tích hợp AI  
> **Kiến trúc:** Hybrid RAG + Toán học tất định (Held-Karp TSP) + Phản biện Copilot (Human-In-The-Loop)

---

## I. TỔNG QUAN KIẾN TRÚC & TRIẾT LÝ THIẾT KẾ AI

### 1. Vấn đề thực tế của việc áp dụng AI truyền thống
Nếu giao toàn bộ việc lập lịch trình cho Mô hình ngôn ngữ lớn (LLM như ChatGPT/Gemini), hệ thống sẽ gặp hiện tượng **Ảo giác (Hallucination)**:
* Tự bịa ra địa điểm không có thật hoặc nhầm lẫn tỉnh thành.
* Tính toán sai thời gian, khoảng cách và vận tốc di chuyển đô thị.
* Nhầm lẫn giờ mở cửa, đóng cửa hoặc các quy định tham quan.

### 2. Triết lý kiến trúc của dự án Tripzy
Hệ thống giải quyết triệt để vấn đề trên bằng cách phân tách rõ ràng trách nhiệm:
* **Backend & CSDL (PostgreSQL + pgvector) = Nguồn chân lý (Source of Truth):** Chịu trách nhiệm về tính chính xác tuyệt đối của địa điểm và dữ liệu địa lý đã được duyệt.
* **Giải thuật toán học (Held-Karp TSP) = Tối ưu quãng đường ngắn nhất chính xác 100%:** Giải bài toán Người du lịch bằng quy hoạch động Bitmask.
* **AI (Google Gemini) = Trợ lý ngữ nghĩa & Phản biện (Semantic Retrieval & Copilot):** Hỗ trợ tìm kiếm theo ngôn ngữ tự nhiên và đề xuất thẻ xử lý xung đột.

### 3. Nguyên tắc phân định vai trò cốt lõi

* **AI = Recommendation:** Khuyến nghị, phản biện và truy vấn ngữ nghĩa tự nhiên.
* **Backend = Source of Truth:** Nguồn chân lý dữ liệu, kiểm soát quy tắc cứng và tính toán tối ưu chính xác 100%.
* **User = Final Decision Maker:** Người dùng toàn quyền quyết định và phê duyệt lịch trình.

---

## II. KỊCH BẢN DEMO THỰC CHIẾN (TỪ CƠ BẢN ĐẾN NÂNG CAO)

### 🔹 BƯỚC 1: Quản lý & Khảo sát Dữ liệu mẫu (Data Grounding)

* **API:** `GET http://localhost:18080/api/v1/public/planning/retrieval/catalog`
* **Body:** Không cần (None) | **Quyền:** Public (`permitAll`)
* **Nội dung trình bày với Giảng viên:**
  * Hệ thống chuẩn bị sẵn danh mục **8 địa điểm chuẩn hóa tại Đà Nẵng** đã được đối chiếu nguồn gốc kiểm chứng (Hộ chiếu di sản Đà Nẵng, Cổng thông tin du lịch TP).
  * Mỗi địa điểm có đầy đủ siêu dữ liệu (metadata): Tọa độ, loại không gian (`INDOOR` / `OUTDOOR` / `HYBRID`), thời lượng tham quan ước tính và hệ thống thẻ chuẩn hóa (`tags`: `MUSEUM`, `BEACH`, `RELAX`, `CULTURE`...).
  * **Ý nghĩa:** Đây là nền tảng chống ảo giác, đảm bảo mọi đề xuất của AI đều bắt nguồn từ dữ liệu thật.

| STT | Tên địa điểm | Loại không gian | Thẻ phân loại (Tags) | Thời lượng |
|:---:|---|:---:|---|:---:|
| 1 | Bảo tàng Điêu khắc Chăm | INDOOR | MUSEUM, HISTORY, CULTURE, ART | 90 phút |
| 2 | Bảo tàng Mỹ thuật Đà Nẵng | INDOOR | MUSEUM, ART, CULTURE | 75 phút |
| 3 | Bảo tàng Đà Nẵng | INDOOR | MUSEUM, HISTORY, CULTURE, LOCAL | 90 phút |
| 4 | Chợ Hàn | HYBRID | MARKET, SHOPPING, LOCAL | 60 phút |
| 5 | Bãi biển Mỹ Khê | OUTDOOR | BEACH, SCENERY, RELAX, OUTDOOR | 75 phút |
| 6 | Công viên Biển Đông | OUTDOOR | PARK, SCENERY, RELAX, OUTDOOR | 45 phút |
| 7 | Cầu Rồng (Điểm ngắm bờ sông) | OUTDOOR | ARCHITECTURE, SCENERY, OUTDOOR | 30 phút |
| 8 | Chùa Linh Ứng Bãi Bụt (Sơn Trà) | HYBRID | TEMPLE, CULTURE, SCENERY | 75 phút |

---

### 🔹 BƯỚC 2: Lập chỉ mục Ngữ nghĩa Vector (Vector Indexing)

* **API:** `POST http://localhost:18080/api/v1/public/planning/retrieval/index`
* **Body:** Không cần (None)
* **Nội dung trình bày với Giảng viên:**
  * Hệ thống bóc tách văn bản mô tả của từng địa điểm, gọi mô hình `gemini-embedding-001` để biến văn bản thành **Vector 768 chiều** (tọa độ không gian ngữ nghĩa) và lưu vào cột `embedding` kiểu `vector(768)` trên PostgreSQL (thông qua tiện ích mở rộng `pgvector`).
  * **Điểm sáng kỹ thuật (Khoe với giảng viên):**
    * **Cơ chế Hash SHA-256:** Mỗi văn bản được băm mã SHA-256. Nếu địa điểm không bị sửa đổi nội dung, hệ thống tự động bỏ qua (`skipped`), **không gọi lại Gemini API để tiết kiệm chi phí/token**.
    * **Khóa an toàn Concurrency:** Dùng `AtomicBoolean` ngăn chặn tình trạng 2 tiến trình index đồng thời gây nghẽn mạng hoặc trùng lặp.

---

### 🔹 BƯỚC 3: Truy vấn Cấu trúc thông minh (Hybrid Search & RRF Ranking)

* **API:** `POST http://localhost:18080/api/v1/public/planning/retrieval/search`

**Request Body mẫu:**
```json
{
  "positiveQuery": "ngắm biển dạo bộ thư giãn mát mẻ",
  "excludedTags": ["MUSEUM"],
  "limit": 2
}
```

* **Cơ chế hoạt động 2 Mắt (Hybrid Search):**
  1. **Mắt từ khóa (Lexical):** Dùng PostgreSQL Full-Text Search kết hợp hàm `unaccent()` để tìm nhanh các từ khóa không phân biệt dấu tiếng Việt.
  2. **Mắt ngữ nghĩa (Semantic Vector):** Chuyển câu hỏi thành vector, dùng toán tử `<=>` của `pgvector` đo khoảng cách góc Cosine. Nhờ đó, người dùng gõ *"thư giãn mát mẻ"*, hệ thống vẫn hiểu và tìm ra *"Bãi biển Mỹ Khê"* và *"Công viên Biển Đông"* dù không trùng chữ.
  3. **Dung hợp thứ hạng RRF (Reciprocal Rank Fusion):** Cộng dồn điểm số từ cả 2 nhánh: `Score = Tổng( 1 / (60 + rank + 1) )` để xếp hạng khách quan nhất.
  4. **Xử lý câu phủ định (`excludedTags`):** Vì AI thường hiểu kém từ phủ định (*"không đi bảo tàng"*), hệ thống bóc tách thành danh sách cấm để loại trừ cứng bằng code logic, đảm bảo an toàn tuyệt đối.

---

### 🔹 BƯỚC 4: Phân tích Ý định Ngôn ngữ Tự nhiên (Natural Language Intent Understanding)

* **API:** `POST http://localhost:18080/api/v1/public/planning/retrieval/intent`

**Request Body mẫu:**
```json
{
  "text": "Tôi đi Đà Nẵng hai ngày, thích lịch sử và nghệ thuật, không đi biển, mỗi ngày từ 8 giờ đến 17 giờ, đi thong thả."
}
```
* **Nội dung trình bày với Giảng viên:**
  * Đây là bước **NLU (Natural Language Understanding)** giúp người dùng không cần bấm form phức tạp mà chỉ cần gõ một câu tự do.
  * **Mô hình AI:** Sử dụng `gemini-3.5-flash` với cấu hình Strict JSON Schema.
  * **Chuẩn hóa thông minh:**
    * Tự đổi chữ thành số: *"hai ngày"* → `numberOfDays: 2`.
    * Tự chuẩn hóa mốc giờ: *"8 giờ đến 17 giờ"* → `dailyStartTime: "08:00"`, `dailyEndTime: "17:00"`.
    * Tự ánh xạ nhịp độ: *"đi thong thả"* → `travelPace: "RELAXED"`.
    * Tự phân loại vào **Bộ 14 Tag quy chuẩn**: *"lịch sử, nghệ thuật"* → `interests: ["HISTORY", "ART"]`; *"không đi biển"* → `excludedTags: ["BEACH"]`.
  * **Cơ chế bằng chứng `evidence` (Chống ảo giác):** Yêu cầu AI trích dẫn nguyên văn câu gốc chứng minh cho từng trường trích xuất. Backend dùng `TravelIntentValidator` đối chiếu trực tiếp.
  * **Tương tác thông minh (`clarificationQuestions`):** Nếu người dùng nói thiếu tỉnh thành hoặc khung giờ mâu thuẫn, hệ thống trả về câu hỏi làm rõ chứ không tự đoán mò.

---

### 🔹 BƯỚC 5: Truy vấn Tự động Toàn diện từ Văn bản (End-to-End Search From Text)

* **API:** `POST http://localhost:18080/api/v1/public/planning/retrieval/search-from-text`

**Request Body mẫu:**
```json
{
  "text": "Tôi đi Đà Nẵng hai ngày, thích lịch sử và nghệ thuật, không đi biển, mỗi ngày từ 8 giờ đến 17 giờ, đi thong thả.",
  "limit": 3
}
```
* **Nội dung trình bày với Giảng viên:**
  * Đây là **API trọn gói cấp cao nhất (Pipeline Chaining)**: Kết hợp toàn bộ sức mạnh của Bước 4 (Hiểu ý định) và Bước 3 (Hybrid Retrieval).
  * **Quy trình vận hành:**
    1. Đọc hiểu câu nói tự nhiên → Rút trích `positiveQuery = "lịch sử nghệ thuật"` và `excludedTags = ["BEACH"]`.
    2. Tự động chuyển tiếp dữ liệu sang bộ tìm kiếm lai PostgreSQL `unaccent` + `pgvector`.
    3. Trả về đồng thời cả **Bản phân tích ý định (`analysis`)** lẫn **Danh sách ứng viên địa điểm thật trong CSDL (`matches`)** kèm điểm số `rankScore`.
  * **Ý nghĩa thực tiễn:** Giao diện ứng dụng di động chỉ cần 1 ô nhập văn bản (hoặc nhận diện giọng nói) là có thể hiển thị ngay kết quả gợi ý địa điểm chuẩn xác mà không cần qua nhiều bước trung gian.

* **Các tình huống kiểm tra biên (Edge Cases & Boundary Validation):**
  Khi kiểm thử hoặc demo với Giảng viên, gửi cùng endpoint `search-from-text`, thay thế giá trị `text` để kiểm tra năng lực xử lý biên và tính toàn vẹn dữ liệu:

| STT | Câu mẫu kiểm tra | Kết quả cần kiểm tra |
|:---:|---|---|
| 1 | Tôi thích xem tranh và tìm hiểu nghệ thuật. | `NEEDS_INPUT`, thiếu destination, không tự mặc định Đà Nẵng |
| 2 | Tôi đi Huế hai ngày, thích lịch sử. | `UNSUPPORTED_DESTINATION`, không trả địa điểm Đà Nẵng |
| 3 | Tôi đi Đà Nẵng, bắt buộc ghé Địa điểm Demo Không Tồn Tại ZZZ. | `NEEDS_INPUT`, câu hỏi về điểm bắt buộc, không tạo ID |
| 4 | Tôi đi Đà Nẵng, muốn xem bảo tàng nhưng không muốn vào bảo tàng. | `NEEDS_INPUT` khi interests và exclusions mâu thuẫn |
| 5 | Tôi đi Đà Nẵng, bắt buộc ghé Mỹ Khê nhưng không đi biển. | `NEEDS_INPUT` khi điểm bắt buộc mâu thuẫn với nhóm bị loại |
| 6 | Tôi đi Đà Nẵng, thích ngắm cảnh nhưng không ghé Mỹ Khê. | Resolve điểm loại trừ cụ thể, không tự loại cả nhóm cảnh quan |
| 7 | Tôi đi Đà Nẵng, thích nghệ thuật, mỗi ngày đi từ 18 giờ đến 8 giờ. | `NEEDS_INPUT`, demo chưa hỗ trợ khung giờ qua đêm |

> **Quy tắc nới lỏng thông minh:** Nếu chỉ thiếu số ngày/giờ/nhịp độ nhưng đã có điểm đến và sở thích, hệ thống vẫn có thể truy xuất ứng viên; `missingFields`/`warnings` ghi rõ chưa đủ dữ liệu lập timeline.

---

### 🔹 BƯỚC 6: Kết nối sang các Tầng xử lý nâng cao (Next Steps)

Sau khi tầng RAG truy xuất được tập địa điểm người dùng ưng ý từ Bước 5, hệ thống kích hoạt luồng xử lý nâng cao của bộ Core:
1. **Tối ưu thứ tự di chuyển (Held-Karp TSP):** Giải thuật quy hoạch động Bitmask tính toán lộ trình ngắn nhất, tiết kiệm tối đa thời gian di chuyển.
2. **Dựng trục thời gian thực (Timeline Engine):** Gắn giờ đến, giờ rời đi, tính thời gian chờ mở cửa và phát hiện ca qua đêm hoặc tràn ngày.
3. **Bộ thẩm định quy tắc cứng (Rule Gatekeeper):** Kiểm tra điểm đóng cửa, thời gian di chuyển quá gấp, hoặc dông bão/mưa lớn ngoài trời.
4. **AI Copilot phản biện (Human-In-The-Loop):** Kích hoạt cơ chế **Silent-by-Default** (tiết kiệm token). Chỉ khi phát hiện xung đột thời tiết hoặc giờ giấc, AI mới xuất hiện và đề xuất tối đa 3 thẻ hành động 1-click (`SWAP_PLACES`, `REPLACE_PLACE`).

---

## III. CÂU HỎI THƯỜNG GẶP CỦA HỘI ĐỒNG & CÁCH TRẢ LỜI "ĂN ĐIỂM"

**❓ Câu 1: Tại sao không để AI (ChatGPT/Gemini) tự sinh luôn lịch trình mà phải làm RAG và pgvector?**  
* **Trả lời:** LLM thường xuyên bị ảo giác (hallucination): tính sai vận tốc, tự bịa tọa độ và nhầm giờ đóng cửa. Em dùng RAG để biến Database thành Nguồn chân lý (Source of Truth), AI chỉ hỗ trợ tìm kiếm ngữ nghĩa trên dữ liệu thật, đảm bảo lịch trình khả thi 100% ngoài đời thực.

**❓ Câu 2: Các thẻ phân loại như BEACH, ART là AI tự bịa hay do ai quy định?**  
* **Trả lời:** Dạ đây là bộ từ điển chuẩn hóa (Enum) gồm 14 thẻ do hệ thống quy định và gán sẵn cho từng địa điểm trong Database. AI chỉ đóng vai trò phân loại (Text Classifier) để ánh xạ câu nói của người dùng vào đúng các thẻ có sẵn. Backend có `TravelIntentValidator` kiểm soát nghiêm ngặt, nếu AI tự chế thẻ lạ ngoài danh mục thì hệ thống từ chối ngay lập tức.

**❓ Câu 3: Hệ thống giải quyết bài toán chi phí (Token) và độ trễ ra sao?**  
* **Trả lời:** Ở bước Indexing, em dùng SHA-256 hash chỉ tính toán địa điểm mới. Ở bước duyệt lịch trình, hệ thống áp dụng cơ chế Silent-by-Default: nếu lịch trình đã tối ưu và không có xung đột, AI giữ im lặng hoàn toàn (tiêu thụ 0 token).

**❓ Câu 4: Nếu mất mạng hoặc hết hạn API Key của Google thì hệ thống có bị lỗi không?**  
* **Trả lời:** Hệ thống có cơ chế Graceful Fallback. Khi không gọi được Gemini, API tự động chuyển sang chế độ tìm kiếm từ khóa thuần (Lexical Search) bằng PostgreSQL, đảm bảo dịch vụ không bao giờ bị gián đoạn.
