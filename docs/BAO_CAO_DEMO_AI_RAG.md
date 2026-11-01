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

```text
AI = Recommendation (Khuyến nghị, phản biện)
Backend = Source of Truth (Nguồn chân lý dữ liệu & tính toán chính xác)
User = Final Decision Maker (Người dùng toàn quyền quyết định)
```

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

### 🔹 BƯỚC 3: Truy vấn thông minh (Hybrid Search & RRF Ranking)

* **API:** `POST http://localhost:18080/api/v1/public/planning/retrieval/search`
* **Request Body mẫu:**
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
  3. **Dung hợp thứ hạng RRF (Reciprocal Rank Fusion):** Cộng dồn điểm số từ cả 2 nhánh: $\text{Score} = \sum \frac{1}{60 + \text{rank} + 1}$ để xếp hạng khách quan nhất.
  4. **Xử lý câu phủ định (`excludedTags`):** Vì AI thường hiểu kém từ phủ định (*"không đi bảo tàng"*), hệ thống bóc tách thành danh sách cấm để loại trừ cứng bằng code logic, đảm bảo an toàn tuyệt đối.

---

### 🔹 BƯỚC 4: Kết nối sang các Tầng xử lý nâng cao (Next Steps)

Sau khi tầng RAG truy xuất được tập địa điểm người dùng ưng ý, hệ thống kích hoạt luồng xử lý nâng cao:
1. **Tối ưu thứ tự di chuyển (Held-Karp TSP):** Giải thuật quy hoạch động Bitmask tính toán lộ trình ngắn nhất, tiết kiệm tối đa thời gian di chuyển.
2. **Dựng trục thời gian thực (Timeline Engine):** Gắn giờ đến, giờ rời đi, tính thời gian chờ mở cửa và phát hiện ca qua đêm hoặc tràn ngày.
3. **Bộ thẩm định quy tắc cứng (Rule Gatekeeper):** Kiểm tra điểm đóng cửa, thời gian di chuyển quá gấp, hoặc dông bão/mưa lớn ngoài trời.
4. **AI Copilot phản biện (Human-In-The-Loop):** Kích hoạt cơ chế **Silent-by-Default** (tiết kiệm token). Chỉ khi phát hiện xung đột thời tiết hoặc giờ giấc, AI mới xuất hiện và đề xuất tối đa 3 thẻ hành động 1-click (`SWAP_PLACES`, `REPLACE_PLACE`).

---

## III. CÂU HỎI THƯỜNG GẶP CỦA HỘI ĐỒNG & CÁCH TRẢ LỜI "ĂN ĐIỂM"

**❓ Câu 1: Tại sao không để AI (ChatGPT/Gemini) tự sinh luôn lịch trình mà phải làm RAG và pgvector?**  
* **Trả lời:** LLM thường xuyên bị ảo giác (hallucination): tính sai vận tốc, tự bịa tọa độ và nhầm giờ đóng cửa. Em dùng RAG để biến Database thành Nguồn chân lý (Source of Truth), AI chỉ hỗ trợ tìm kiếm ngữ nghĩa trên dữ liệu thật, đảm bảo lịch trình khả thi 100% ngoài đời thực.

**❓ Câu 2: Hệ thống giải quyết bài toán chi phí (Token) và độ trễ ra sao?**  
* **Trả lời:** Ở bước Indexing, em dùng SHA-256 hash chỉ tính toán địa điểm mới. Ở bước duyệt lịch trình, hệ thống áp dụng cơ chế Silent-by-Default: nếu lịch trình đã tối ưu và không có xung đột, AI giữ im lặng hoàn toàn (tiêu thụ 0 token).

**❓ Câu 3: Nếu mất mạng hoặc hết hạn API Key của Google thì hệ thống có bị lỗi không?**  
* **Trả lời:** Hệ thống có cơ chế Graceful Fallback. Khi không gọi được Gemini, API tự động chuyển sang chế độ tìm kiếm từ khóa thuần (Lexical Search) bằng PostgreSQL, đảm bảo dịch vụ không bao giờ bị gián đoạn.
