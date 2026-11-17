# Demo tạo lịch trình nháp từ câu yêu cầu

> Cập nhật phân quyền 17/11/2026: các URL `/public/planning/**` là đường dẫn tương thích cũ và **vẫn yêu cầu Bearer token USER/ADMIN**. Index yêu cầu ADMIN có `admin:ai:index`; ưu tiên `/api/v1/admin/planning/retrieval/index`. Xem [ma trận phân quyền](./AUTHORIZATION.md).

Ngày bổ sung: 01/11/2026. Đây là bản xem trước, không tạo Trip hoặc ghi itinerary vào database.

Cập nhật 17/11/2026: bản nháp ưu tiên ma trận đường ô tô Mapbox, dùng lại cho mọi ngày/vòng sửa; cấu hình `MAPBOX_ACCESS_TOKEN` ở backend. Chi tiết nguồn dữ liệu và phép kiểm chứng provider thật nằm trong [tài liệu tích hợp](./TRIP_AI_PLANNING_INTEGRATION.md#cập-nhật-đường-bộ--17112026).

## Chạy và gọi API

Backend bật profile `rag-demo`, dùng database demo và model đang cấu hình qua `RAG_INTENT_MODEL`. Không đổi cấu hình model hiện có trong bước triển khai này. Nếu backend đang chạy bản cũ, Ctrl+C rồi chạy lại:

```powershell
cd E:/travel-system
./scripts/Start-RagDemo.ps1
```

API public, Postman chọn Bearer Token (`{{accessToken}}`), Body raw JSON:

```text
POST http://localhost:18080/api/v1/public/planning/generate-draft
Content-Type: application/json
```

Ví dụ hai ngày:

```json
{
  "text": "Tôi đi Đà Nẵng hai ngày, thích lịch sử, nghệ thuật và khám phá địa phương, không đi biển, mỗi ngày từ 8 giờ đến 17 giờ, đi thong thả.",
  "startDate": "2026-11-02",
  "origin": {
    "name": "Điểm xuất phát demo ở trung tâm",
    "latitude": 16.065,
    "longitude": 108.223
  },
  "roundTrip": true,
  "restMinutes": 45
}
```

Điểm xuất phát ở ví dụ chỉ là điểm neo người dùng chọn, không phải một khách sạn được xác minh. Origin không cần và không nhận placeId giả. startDate là ngày kế hoạch do người dùng chọn, không được AI đoán từ hôm nay. Demo giữ cùng origin cho mỗi ngày; không hỗ trợ đổi nơi lưu trú giữa ngày.

- Cung cấp đúng một trong text hoặc preferences. text tối đa 1.000 ký tự; câu cần nói rõ 1–2 ngày và khung giờ mỗi ngày. Đầu vào preferences và cách kiểm tra nguồn AI/fallback được mô tả trong [hướng dẫn AI/fallback](./RAG_AI_FALLBACK_DEMO.md).
- startDate và origin bắt buộc; latitude/longitude hợp lệ, origin trong vùng demo Đà Nẵng (15.8–16.3, 107.9–108.4).
- roundTrip mặc định true, tính cả chặng quay về vào giờ kết thúc và tổng di chuyển.
- restMinutes mặc định 45, nhận 0–90; thêm nghỉ sau hai điểm nếu còn điểm tiếp theo. Đây là nghỉ giữa buổi, không cam kết khung ăn trưa cố định.
- Khoảng đệm giữa các điểm: RELAXED 15 phút, ACTIVE 5 phút, còn lại 10 phút. Nếu chưa nêu nhịp độ, sử dụng chính sách demo cân bằng và trả cảnh báo, không ghi thành sở thích do người dùng nói.

## Luồng thực hiện

1. Gemini trích xuất nhu cầu; backend kiểm tra và resolve tên điểm từ DB.
2. Hybrid retrieval lấy tối đa 8 ứng viên đã duyệt, áp dụng loại trừ và giữ điểm bắt buộc.
3. Một lần gọi AI đề xuất chọn điểm/chia ngày và gợi ý chủ đề/điểm nhấn bằng mã tham chiếu P1... trong tập ứng viên. Backend kiểm tra ngày, mã, trùng và điểm bắt buộc. Không dùng số liệu thời gian AI tự tạo.
4. Lấy một ma trận thời gian/khoảng cách đường ô tô từ Mapbox cho origin và các ứng viên. Mỗi ngày: Held–Karp tối ưu thứ tự theo thời gian đường bộ có hướng, từ origin cố định và tính cả chặng về nếu chọn. TimelineEngine được tái sử dụng qua adapter tính thời gian chờ, nghỉ, đệm. Chỉ lỗi provider mới dùng Haversine có nhãn; không thay cặp không có đường bằng số liệu giả.
5. Kiểm tra giờ đóng cửa, giờ kết thúc và dữ liệu chưa xác minh. Tối đa hai lượt sửa xác định: thử chuyển điểm sang ngày khác, hoặc bỏ điểm tùy chọn; không tự bỏ điểm bắt buộc.
6. Sau lịch cuối, backend kiểm tra lại gợi ý chủ đề/điểm nhấn đã nhận ở bước 3. Chỉ sử dụng nếu refs vẫn thuộc đúng ngày và chủ đề có căn cứ trong tags; nếu không, dùng template fallback từ lịch cuối. Không gọi LLM lần nữa. Không nhận đoạn văn tự do có thể thêm giá vé/thời tiết/sự kiện không có nguồn.
7. Dự báo tại origin được gọi song song với bước phân bổ trên executor có giới hạn; ngân sách mặc định 2 giây từ lúc khởi chạy. Kết quả mock/thiếu ngày/quá hạn trả UNKNOWN. Không gọi thời tiết lặp trong từng lượt solver.

Luồng text có hai tác vụ generation (intent và allocation kèm theme hints), một query embedding; chỉ cho phép thêm một lần retry dùng chung toàn request khi lỗi tạm thời và còn ngân sách AI. Luồng preferences bỏ qua tác vụ intent. Input chưa đủ điều kiện lập lịch được trả về trước khi gọi query embedding. Nếu intent lỗi, draft trả NEEDS_STRUCTURED_INPUT; không suy đoán intent. Nếu embedding/allocation/theme hints không dùng được thì dùng fallback có ghi rõ nguồn và lý do.

## Đọc response

Trong data:

| Field | Ý nghĩa |
|---|---|
| status | READY_WITH_WARNINGS / NEEDS_INPUT / NEEDS_STRUCTURED_INPUT / INFEASIBLE / CONFLICTS_DETECTED / NO_CANDIDATES / UNSUPPORTED_DESTINATION |
| analysis | Cách hiểu yêu cầu, câu hỏi, sở thích và điểm bắt buộc |
| candidates | Kết quả truy xuất làm căn cứ |
| days | Lịch cuối theo ngày, chủ đề/tóm tắt, điểm dừng, khoảng nghỉ và chặng về |
| unassignedPlaces | Ứng viên không được chọn hoặc bị bỏ để xử lý xung đột, có lý do |
| issues | blocking=true là vấn đề chưa giải quyết; false là cảnh báo/dữ liệu thiếu |
| weather | Dự báo tham khảo và phạm vi bao phủ; UNKNOWN nếu chưa có dữ liệu thật đầy đủ |
| metadata | Model, nguồn phân bổ, giới hạn tối ưu, số lượt sửa, chính sách nghỉ và nhãn PREVIEW_ONLY |
| executionReport | Nguồn từng bước, model đã gọi, số attempt/retry, lý do fallback và quyết định retry |

Mỗi days[].stops[] có:

- place: ID thật, tên, dữ liệu/nguồn kiểm chứng từ catalog.
- transitDeparture, arrival: bắt đầu chặng đi và đến điểm.
- visitStart: sau thời gian chờ mở cửa.
- departure: kết thúc đủ thời lượng tham quan, đã tính chờ.
- waitingMinutes, transitMinutes, distanceKm, openingStatus.

days[].finish tính cả quay về nếu roundTrip=true. totalTransitMinutes chỉ tính di chuyển, không tính tham quan/nghỉ/chờ. pauses ghi riêng nghỉ và khoảng đệm. Ngày trống được thể hiện và cảnh báo, không nhét điểm không phù hợp để đủ ngày.

allocationSource là AI hoặc RULE_BASED_FALLBACK. summarySource là AI_GROUNDED_TEMPLATE (AI chọn chủ đề/điểm nhấn, backend biên tập) hoặc RULE_BASED_FALLBACK. Không trình bày fallback như kết quả mô hình.

## Kịch bản kiểm tra thủ công

Giữ startDate/origin ở trên, thay text:

1. Một ngày, bảo tàng: “Tôi đi Đà Nẵng một ngày, thích lịch sử và nghệ thuật, không đi biển, từ 8 giờ đến 17 giờ.” Kiểm tra ID thật, không có BEACH, giờ kết thúc gồm quay về.
2. Một ngày, cảnh quan: “Tôi đi Đà Nẵng một ngày, thích ngắm biển và cảnh quan, không vào bảo tàng, từ 8 giờ đến 18 giờ.” So sánh tập điểm với kịch bản 1.
3. Hai ngày: dùng ví dụ đầu trang, kiểm tra không trùng điểm giữa ngày và chủ đề khớp điểm được xếp.
4. Bất khả thi có chứng cứ: “Tôi đi Đà Nẵng một ngày, bắt buộc ghé Bảo tàng Điêu khắc Chăm, Bảo tàng Mỹ thuật Đà Nẵng và Bảo tàng Đà Nẵng, từ 8 giờ đến 8 giờ 30.” Kỳ vọng INFEASIBLE do tổng thời lượng bắt buộc vượt quỹ giờ ngay cả khi chưa tính đường đi.
5. Chưa đủ thông tin: “Tôi đi Đà Nẵng, thích nghệ thuật.” Kỳ vọng NEEDS_INPUT, không tự điền số ngày/giờ.
6. Điểm không tồn tại: “Tôi đi Đà Nẵng một ngày, bắt buộc ghé Địa điểm Không Tồn Tại ZZZ, từ 8 giờ đến 17 giờ.” Kỳ vọng câu hỏi xác nhận, không sinh UUID hoặc tự thay điểm.

Payload thiếu origin/startDate, tọa độ sai hoặc restMinutes ngoài 0–90 trả HTTP 400. Các trạng thái nghiệp vụ vẫn có thể HTTP 200: luôn đọc data.status và issues, không coi HTTP 200 là lịch đã khả thi.

## Giới hạn cần trình bày đúng

- Đọc `metadata.routeSource` và `routeStatus`: `MAPBOX_DRIVING` là số liệu đường ô tô, chưa phải giao thông trực tiếp của ngày đi; `ESTIMATED_HAVERSINE_35_KMH` là fallback đường chim bay theo 35 km/h. Tọa độ demo và thời lượng chưa được xác minh thực địa. Tối ưu trong tập điểm của từng ngày, không cam kết tối ưu toàn bộ việc chọn/chia ngày.
- Held–Karp tối ưu thứ tự theo khoảng cách với tập điểm đã chọn trong từng ngày. Chọn điểm/chia ngày có AI và sửa có giới hạn, không phải nghiệm tối ưu toàn cục có time windows.
- INFEASIBLE chỉ dùng khi chứng minh được tổng thời lượng điểm bắt buộc vượt quỹ giờ. CONFLICTS_DETECTED nghĩa là thuật toán sửa giới hạn chưa tìm được lịch hợp lệ, không chứng minh mọi phương án đều bất khả thi.
- Dữ liệu seed có giờ hoạt động UNKNOWN; lịch demo thường READY_WITH_WARNINGS, không được gọi là đã xác minh đầy đủ. Lịch tuần được đọc từ DB nếu có; lỗi đọc giờ tuần có cảnh báo.
- Thời tiết chỉ tại origin, không được dùng để khẳng định mỗi điểm an toàn hoặc tự thay đổi lộ trình. Đây chưa phải tái lập lịch theo thời tiết từng điểm/khung giờ.
- Chưa xử lý ngân sách bằng tiền, yêu cầu vận động/khả năng tiếp cận, giờ hẹn cố định tại một điểm, giờ nghỉ trưa cụ thể, đặt vé hoặc thay đổi ngày từ mô tả tự nhiên. Chỉ các trường intent được hiển thị mới đang được dùng để lập lịch.
- Không lưu Trip/itinerary. Bước xác nhận Apply, revision và giao diện là giai đoạn tiếp theo.

## Kết quả kiểm tra trước khi bổ sung retry/provenance (01/11/2026)

- Toàn bộ suite backend ở lượt cuối: 296 tests, 0 failures, 0 errors, 0 skipped; sau đó chạy riêng service để kiểm tra thêm nhánh không chọn được điểm nào. Có kiểm tra hai ngày qua HTTP/security chain, PostgreSQL + pgvector thật, solver và timeline thật; chỉ các ranh giới AI/embedding/weather được mô phỏng trong kiểm thử.
- Kiểm tra provider thật với cấu hình local gemini-3.5-flash: request một ngày trả READY_WITH_WARNINGS, chủ đề “Văn hóa và nghệ thuật”, weather AVAILABLE_ORIGIN_ONLY. Bước phân bổ ở lần chạy này dùng RULE_BASED_FALLBACK vì provider 503; chủ đề dùng AI_GROUNDED_TEMPLATE. Sau smoke test, fallback được siết để không thêm ứng viên tùy chọn không có tag sở thích tích cực; có regression test riêng. Đây là một kết quả quan sát, không phải lịch hard-code.
- Ở phiên bản trước, các lần thử hai ngày với provider thật bị 503 ở bước intent; backend trả AI_001/503. Sau thay đổi hiện tại, draft sẽ trả NEEDS_STRUCTURED_INPUT khi không lấy được intent sau chính sách retry; các API intent/search-from-text riêng vẫn trả lỗi provider. Không dựng intent giả.
- Server kiểm tra dùng cổng tạm 18081 và được dừng sau kiểm tra; không để tiến trình nền chiếm cổng khi người dùng chạy lại demo.
