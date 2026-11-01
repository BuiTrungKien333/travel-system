# Phạm vi demo nhanh: AI RAG lập hành trình

Ngày cập nhật: 30/10/2026. Theo yêu cầu thu nhỏ phạm vi của người dùng. Đây là cấu hình demo ưu tiên so với kế hoạch đầy đủ trong PLAN_AI_RAG_ITINERARY.md.

## 1. Phạm vi lựa chọn

- Một thành phố: Đà Nẵng; giới hạn khu vực demo quanh trung tâm, ven biển, Sơn Trà và Ngũ Hành Sơn, không mở rộng toàn địa giới thành phố.
- Khoảng 10 địa điểm; tối thiểu 8 nếu chưa đủ thông tin kiểm chứng. Điểm xuất phát nhập/chọn riêng, không tính là địa điểm tham quan bắt buộc.
- Lịch 1–2 ngày; khoảng 3–4 điểm/ngày là định hướng demo, không phải quota phải nhét đủ.
- Ba nhóm nhu cầu: văn hóa/nghệ thuật, biển/cảnh quan, chợ/khám phá địa phương.
- Ba kịch bản thành công, hai kiểm tra biên. Dùng cùng kho dữ liệu cho mọi kịch bản.
- Chưa hỗ trợ ngân sách số tiền, cafe yên tĩnh, accessibility hoặc lịch sự kiện nếu chưa có dữ liệu xác minh.

Lý do chọn Đà Nẵng: Postman hiện có nhiều request minh họa cho thành phố này, tận dụng được ví dụ origin, tọa độ và cách gọi pipeline. Đây chỉ là dữ liệu request mẫu; chưa xác nhận các UUID trong Postman đã được seed trong database.

## 2. Danh sách ứng viên để chuẩn bị dữ liệu

Danh sách dưới đây là đề xuất biên soạn, chưa phải tập dữ liệu đã duyệt/đã nhập DB. Nhóm trải nghiệm là nhãn dự kiến cần đối chiếu khi chuẩn bị mô tả. Không lấy tọa độ/giờ từ ví dụ rồi tự coi là đã xác minh.

| STT | Địa điểm ứng viên | Nhóm trải nghiệm dự kiến |
|---|---|---|
| 1 | Bảo tàng Điêu khắc Chăm | Văn hóa, lịch sử |
| 2 | Bảo tàng Mỹ thuật Đà Nẵng | Nghệ thuật |
| 3 | Bảo tàng Đà Nẵng | Lịch sử, địa phương |
| 4 | Chợ Hàn | Chợ, khám phá địa phương |
| 5 | Chợ Cồn | Chợ, khám phá địa phương |
| 6 | Bãi biển Mỹ Khê | Biển, cảnh quan |
| 7 | Công viên Biển Đông | Không gian ngoài trời |
| 8 | Cầu Rồng, điểm quan sát ở bờ sông | Cảnh quan đô thị; không mặc định có biểu diễn |
| 9 | Chùa Linh Ứng Bãi Bụt, Sơn Trà | Văn hóa, cảnh quan |
| 10 | Danh thắng Ngũ Hành Sơn | Di sản, cảnh quan |

Mỗi record cần: placeId DB thật, tên/alias, tỉnh, tọa độ điểm vào/điểm dừng, mô tả ngắn có nguồn, tags đã duyệt, loại không gian, thời lượng tham quan ước lượng có nhãn, giờ hoạt động hoặc UNKNOWN, nguồn và ngày kiểm chứng. Không nhầm ba chùa Linh Ứng, không đồng nhất cả bán đảo Sơn Trà thành một điểm dừng cụ thể.

Ưu tiên xác minh bằng website cơ quan du lịch/đơn vị quản lý; ghi nhận mâu thuẫn hoặc thuộc tính chưa biết. Chỉ đưa record đã duyệt vào retrieval; dữ liệu chưa biết không được LLM tự điền.

## 3. Bộ tình huống demo

| Mã | Yêu cầu mẫu | Điều cần chứng minh |
|---|---|---|
| D1 | “Tôi đi Đà Nẵng một ngày, thích lịch sử và nghệ thuật, ưu tiên bảo tàng, không đi biển. Bắt đầu 8 giờ, kết thúc trước 17 giờ.” | Trích xuất ý định; truy xuất đúng nhóm; xử lý phủ định; lịch vừa khung giờ |
| D2 | “Tôi đi Đà Nẵng một ngày, thích biển và ngắm cảnh, không tham quan bảo tàng. Bắt đầu 8 giờ, kết thúc trước 18 giờ.” | Cùng dữ liệu nhưng tập điểm thay đổi theo sở thích, không hard-code câu mẫu |
| D3 | “Tôi có hai ngày ở Đà Nẵng, muốn kết hợp bảo tàng, đi chợ và ngắm biển. Mỗi ngày đi thong thả từ 8 giờ đến 17 giờ.” | Chọn điểm, chia ngày, Held–Karp từng ngày, timeline, chủ đề đúng lịch cuối |
| E1 | “Tôi muốn ghé Địa điểm Demo Không Tồn Tại ZZZ ở Đà Nẵng.” | Trả không tìm thấy/cần xác nhận, không sinh ID hoặc tự thay điểm bắt buộc |
| E2 | “Tôi chỉ có 30 phút, bắt buộc tham quan đủ ba bảo tàng.” | Phát hiện không khả thi, không rút ngắn thời lượng tùy tiện hoặc bỏ must-visit âm thầm |

Ngày đi và origin được chọn rõ trong form khi chạy demo, không hard-code ngày đã qua trong câu mẫu. Tập điểm mong đợi là nhãn đánh giá do con người chuẩn bị, không phải đáp án trả cố định của hệ thống. Chạy thêm một câu diễn đạt lại D1/D2 để chứng minh hiểu ý nghĩa.

## 4. Thứ tự thực hiện rút gọn

1. Chuẩn bị và duyệt 8–10 record; thống nhất trường bổ sung tối thiểu và seed chạy lại được trong môi trường demo riêng. Đọc lại ID từ DB sau seed, không dùng UUID ví dụ Postman làm bằng chứng tồn tại.
2. PostgreSQL + pgvector, embedding cho record đã duyệt; exact vector search và tìm tên/từ khóa, không cần HNSW hoặc database vector riêng.
3. Intent extraction + validation + form xác nhận; retrieval có allowlist; kiểm tra D1, D2 và E1 trước khi nối lập lịch.
4. Chọn điểm/chia ngày -> Held–Karp -> timeline/rules; kiểm tra D3 và E2. Không gọi Copilot theo từng ngày trong luồng mới.
5. Sinh chủ đề từ lịch cuối; preview và xác nhận lưu; kiểm tra ownership, FK và draft revision nếu có lưu.
6. Chạy trọn bộ demo, ghi kết quả và giới hạn. Có thể dùng Postman trước khi hoàn thiện màn hình mobile để có demo backend sớm.

Không yêu cầu hoàn thành toàn bộ benchmark/hệ thống quản trị dữ liệu của kế hoạch lớn trước demo. Không cắt kiểm tra ID, quyền ghi, tính nhất quán timeline và xử lý unknown để rút thời gian.

## 5. Tiêu chí hoàn thành demo

- Có dữ liệu nhỏ đã duyệt và có ID thật; không nhánh trả lịch hard-code theo câu nhập.
- Truy xuất vector thực sự chạy trên pgvector; thể hiện danh sách ứng viên làm căn cứ cho LLM.
- D1/D2 khác nhau phù hợp về nhóm trải nghiệm; D3 có chia ngày và tối ưu thật.
- E1 không có ID giả; E2 không bị báo khả thi sai.
- Chủ đề phản ánh lịch cuối; thời gian/cự ly do backend tính, ghi nhãn ước lượng nếu dùng Haversine.
- Thời tiết dùng dữ liệu thật hoặc UNKNOWN; nếu chạy fixture mô phỏng phải có nhãn. Không thêm điều kiện mưa giả để làm demo trông thông minh.
- Có log stage/model/source phù hợp và kết quả kiểm thử tối thiểu. Bộ nhỏ phục vụ proof of concept, không chứng minh chất lượng trên toàn bộ Việt Nam.

## 6. Trạng thái tại thời điểm lập phạm vi

Đã triển khai bộ seed 8 điểm, PostgreSQL + pgvector, API catalog/search/indexing, intent parser, search-from-text và generate-draft nối chọn/chia ngày, Held–Karp, timeline và chủ đề. Xem [hướng dẫn chạy](./RAG_DEMO_RUNBOOK.md), [kiểm tra câu tự nhiên](./RAG_INTENT_POSTMAN.md) và [bản nháp lịch trình](./RAG_DRAFT_POSTMAN.md). Chưa lưu itinerary hoặc tích hợp mobile. Tọa độ vẫn là xấp xỉ demo và giờ hoạt động UNKNOWN. Kết quả provider thật và giới hạn được ghi riêng trong hướng dẫn draft.
