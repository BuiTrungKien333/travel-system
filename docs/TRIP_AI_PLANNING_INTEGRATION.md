# Tích hợp AI lập lịch vào chuyến đi

## Phạm vi phiên bản đầu

Mobile có nút **Gợi ý lịch trình** trong chi tiết chuyến đang lập kế hoạch tại Đà Nẵng, có ngày đi trong tương lai, dài 1–2 ngày và chưa có điểm nào trong lịch trình đã lưu. Các địa điểm chưa xếp lịch vẫn được giữ khi áp dụng. Web Next.js chưa nối luồng mới.

Người dùng chọn điểm xuất phát có tọa độ từ danh mục thật hoặc GPS, giờ bắt đầu/kết thúc, nhịp độ, sở thích/loại trừ, điểm bắt buộc và quay về điểm xuất phát. Có lựa chọn nhập nhu cầu bằng lời; dữ liệu ngày/điểm đến/khung giờ của form được kèm vào câu nhập. Khi đọc câu không thành công, người dùng có thể chọn lại biểu mẫu có cấu trúc. Form chưa hỗ trợ mọi sở thích của Trip; không tự ánh xạ `food`/`adventure` thành tag khác.

Kết quả hiển thị theo ngày với bản đồ ghim địa điểm, giờ ghé, thời lượng, nghỉ, điểm chưa xếp và cảnh báo. Bản đồ không vẽ tuyến đường bộ giả. Nút Áp dụng chỉ mở cho đề xuất không có lỗi chặn; người dùng có thể chỉnh yêu cầu và tạo bản khác.

## API có xác thực

Base: `/api/v1/trips/{tripId}/planning/drafts`.

| Method/path | Contract |
|---|---|
| POST base | `clientRequestId` UUID, `expectedTripVersion` và `input` theo `GenerateDraftRequest` hiện có. Trả `TripDraftResponse`. |
| GET `/latest` | Bản nháp còn hạn mới nhất; đang tạo trả `DRAFT_003`, thất bại/gián đoạn trả `DRAFT_005`. Không có `data` nếu chưa có bản nháp (envelope bỏ trường null). |
| GET `/{draftId}` | Đọc bản nháp của đúng chủ sở hữu/chuyến đi. |
| POST `/{draftId}/apply` | Không nhận lịch hay payload AI từ client; áp dụng đúng bản nháp đã lưu trên server. Trả `ItineraryResponse`. |

`TripDraftResponse`: `id`, `tripId`, `expiresAt`, `applied`, `canApply`, `input`, `proposal` (`DraftResponse` của core).

```json
{
  "clientRequestId": "dcfe387a-4262-4a5f-b5f8-b2c061544c41",
  "expectedTripVersion": 0,
  "input": {
    "startDate": "2027-01-10",
    "origin": {"name": "Điểm xuất phát đã chọn", "latitude": 16.065, "longitude": 108.223},
    "roundTrip": true,
    "restMinutes": 45,
    "preferences": {
      "destination": "Đà Nẵng", "numberOfDays": 2,
      "interests": ["MUSEUM", "CULTURE"], "excludedTags": ["BEACH"],
      "dailyStartTime": "08:00", "dailyEndTime": "17:00", "travelPace": "RELAXED",
      "mustVisitNames": [], "excludedPlaceNames": []
    }
  }
}
```

Ngày trong ví dụ phải thay bằng ngày của chuyến đi thật; `expectedTripVersion` lấy từ GET Trip. `text` và `preferences` loại trừ nhau. Client giữ cùng `clientRequestId` khi thử lại nguyên request sau lỗi mạng; dùng UUID mới khi đổi nhu cầu.

Lỗi riêng: `DRAFT_001` không tìm thấy, `DRAFT_002` hết hạn, `DRAFT_003` đang xử lý, `DRAFT_004` ngoài phạm vi, `DRAFT_005` lần tạo trước thất bại/gián đoạn và cần request mới. `TRIP_008` khi revision thay đổi, `AI_003` khi vượt 3 yêu cầu/phút/tài khoản. Giới hạn được kiểm tra bằng khóa DB của user; lặp lại request đã có không gọi provider hoặc tính thêm lượt. Không có background queue: request bị gián đoạn không tự chạy tiếp. Bản nháp còn dở quá hai phút chuyển sang thông báo cần tạo lại, không treo trạng thái đang chạy vô hạn.

## Persistence và tính toàn vẹn

Migration V20 tạo `trip_planning_drafts`, V21 bổ sung mã lỗi lần tạo. Bản nháp tồn tại qua restart ứng dụng/backend, hết hiệu lực sau 24 giờ. Dữ liệu draft gồm nhu cầu và tọa độ riêng tư, chỉ đọc qua API có ownership. Chưa có job xóa bản nháp hết hạn; cần bổ sung chính sách retention trước khi vận hành dài hạn.

Tạo draft giữ snapshot version Trip và revision lịch/danh sách địa điểm, rồi gọi core bên ngoài transaction. Khi hoàn tất kiểm tra snapshot lần nữa. Preview không tạo TripPlace hay itinerary.

Apply khóa Trip, kiểm tra snapshot, hạn dùng, ngày hợp lệ, lỗi chặn và catalog hiện tại còn được duyệt/không đổi. Tạo liên kết TripPlace còn thiếu và lưu lịch, giờ ghé, thời lượng, số liệu di chuyển của bản nháp, điểm xuất phát và giờ xuất phát trong một transaction. Các liên kết có sẵn không bị xóa. Hai apply đồng thời hoặc lặp sau mất response chỉ tạo một lịch; bản đã áp dụng trả lịch hiện hành và không áp dụng lại.

## Chạy

- **Dev:** profile `dev` hiện có đã bật core, catalog và endpoint mới; khởi động lại backend để Flyway chạy đến V21.
- **Môi trường khác:** bật thêm profile `planning-ai`, ví dụ `SPRING_PROFILES_ACTIVE=prod,planning-ai`. Profile nhập cấu hình RAG và migrations/schema/catalog Đà Nẵng hiện có; PostgreSQL phải có pgvector và quyền tạo extension. Các controller RAG public dành cho dev/rag-demo không được bật bởi profile này.
- Cấu hình provider qua `GEMINI_API_KEY`, các biến `RAG_*` và `GEMINI_BASE_URL` hiện có; khóa chỉ ở backend. Thiếu provider vẫn có nhánh fallback từ form có cấu trúc; không tuyên bố đó là AI.
- Cấu hình `MAPBOX_ACCESS_TOKEN` ở backend để dùng đường ô tô. Tái sử dụng client, timeout và cách xử lý lỗi hiện có của Mapbox; không cần dependency hoặc migration mới.
- Mobile tiếp tục dùng cấu hình API URL hiện có; chạy `npm start` hoặc build lại APK để nhận giao diện mới. Không cần dependency/native module mới.
- Request tạo draft dùng timeout 75 giây riêng; core mặc định có deadline 60 giây. Cấu hình proxy phải phù hợp. Query/mutation không tự retry generation.

## Kiểm chứng

Backend `TripDraftIntegrationTest` dùng PostgreSQL/pgvector thật, migrations thật, full security chain, solver/timeline thật; mock ranh giới Gemini/embedding/weather. Kiểm tra preview, đọc lại, apply lặp/đồng thời, ownership, thay đổi version/catalog, hết hạn, lỗi chặn, quota, request dở và rollback giữa lúc ghi DB. Test xuất response thật ra `backend/target/trip-draft-contract.json` để kiểm tra schema mobile.

Mobile có test contract và phạm vi hiển thị trong `tests/planning-contract.test.ts`. Chạy TypeScript, lint các file thay đổi, test và export web để kiểm tra bundle. Kết quả chạy thực tế được báo trong bàn giao; không coi test provider mô phỏng là đo chất lượng Gemini thật.

Kết quả kiểm tra ngày 16/11/2026:

| Kiểm tra | Kết quả |
|---|---|
| Backend | 545 tests đạt, 0 failure/error/skip, sau khi chạy lại 8 test migration/seed với kỳ vọng V20. Trong đó 12 test tích hợp draft mới. |
| Mobile | Toàn bộ 439 tests đạt; có test contract, preview/apply, retry giữ mã yêu cầu và khôi phục nội dung câu nhập. |
| Ranh giới API | Response thật từ Spring/PostgreSQL khớp schema Zod mobile. |
| TypeScript | `npx tsc --noEmit` đạt. |
| Lint | Các file mới đạt. Hai file giao diện có sẵn được sửa không tăng lỗi lint so với HEAD; lint toàn repo còn lỗi có sẵn ở các phần khác. |
| Build | JAR backend được đóng gói; Expo web export thành công. |

Chưa gọi Gemini thật, chưa kiểm tra trực quan trên điện thoại hoặc build APK mới trong đợt này. Không khởi động lại server đang dùng database của người dùng; V20 đã được kiểm tra trên database test độc lập.

## Giới hạn giữ nguyên

UI lưu chế độ DRIVE; chưa hỗ trợ chọn phương tiện khác. Thời tiết chỉ tại origin, có thể UNKNOWN. Tọa độ/giờ hoạt động của catalog có thể chưa xác minh và được cảnh báo. Mở rộng địa bàn, chỉnh lịch hiện có và Copilot là các đợt tiếp theo.

## Cập nhật đường bộ — 17/11/2026

Draft gọi `MapboxDirectionsService.matrix` một lần cho origin và tối đa tám ứng viên. Ma trận được dùng lại cho mọi ngày và các vòng repair; không gọi lại Mapbox theo từng ngày hoặc từng lần thử. Không gọi AI để tính thời gian di chuyển.

Held–Karp dùng thời gian đường bộ có hướng làm cost. Khi có quay về, thêm origin làm terminal cố định trong ma trận con của solver, tính cả chặng về trong mục tiêu. Timeline tiếp tục tính giờ chờ mở cửa, tham quan, nghỉ và buffer; chỉ thay số liệu di chuyển bằng dữ liệu đường ô tô. Thời gian từng chặng được làm tròn lên phút; tổng hiển thị cộng đúng các phút này để khớp giờ đến/giờ kết thúc. Cự ly hiển thị làm tròn tới 0,01 km.

`proposal.metadata` có:

| Trường | Ý nghĩa |
|---|---|
| `routeSource=MAPBOX_DRIVING` | Dữ liệu đường ô tô từ Mapbox. |
| `optimizationScope=HELD_KARP_PER_DAY_ROAD_DURATION` | Tối ưu thời gian di chuyển trong từng tập điểm/ngày; không cam kết tối ưu toàn bộ bài toán chia ngày/giờ mở cửa. |
| `routeStatus=AVAILABLE` | Có tuyến nối các điểm trong lịch cuối. |
| `routeStatus=UNREACHABLE` | Chưa nối được đủ điểm; có issue chặn `ROUTE_UNAVAILABLE`. |
| `routeSource=ESTIMATED_HAVERSINE_35_KMH` | Thiếu token, lỗi/timeout provider hoặc dữ liệu provider không hợp lệ: toàn request dùng ước lượng, có cảnh báo rõ nguồn. Mục tiêu khi đó vẫn là khoảng cách. |
| `routeFallbackReason` | Mã nguyên nhân provider; không chứa URL/token. |
| `routeSource=NOT_CALCULATED` | Yêu cầu chưa tới bước tính tuyến. |

Khi Mapbox trả `NoRoute` hoặc cặp không thể định tuyến (`null`), không tự thay cặp đó bằng đường chim bay. Solver tìm thứ tự khác nếu có; repair có thể chuyển ngày hoặc bỏ điểm tùy chọn với lý do rõ ràng. Điểm bắt buộc chưa nối được giữ trong danh sách chưa xếp, có lỗi chặn và không được áp dụng. Bỏ hết điểm tùy chọn không được làm mất cảnh báo tuyến thất bại.

Mobile đọc metadata để hiển thị nhãn đường bộ hoặc ước lượng, dùng bản đồ ghim hiện có và không dựng geometry giả từ Matrix. Matrix không cung cấp polyline. Bản nháp cũ giữ số liệu cũ và được ghi nhãn ước lượng; tạo bản nháp mới để dùng đường bộ.

Mapbox `driving` trả thời gian dự kiến và khoảng cách theo mạng đường, có thể khác nhau theo chiều. Bản này không dùng `driving-traffic` và không gửi ngày khởi hành tương lai; không tuyên bố là ETA giao thông trực tiếp của ngày đi. Tham khảo [Mapbox Matrix API](https://docs.mapbox.com/api/navigation/matrix/).

Đã kiểm chứng trực tiếp một request Mapbox thật qua chính `DraftSchedulingService` + Held–Karp + TimelineEngine với origin và hai tọa độ catalog Đà Nẵng (Bảo tàng Chăm, Chợ Hàn): một HTTP call, tổng 3,3 km và 13 phút sau khi làm tròn từng chặng, gồm quay về. Thời gian gốc của thứ tự được chọn là 661,3 giây, thứ tự còn lại 708,1 giây; mất khoảng 2,9 giây cho phép kiểm tra này. Đây là một tình huống kiểm chứng provider, không phải benchmark hoặc bảo đảm thời gian của mọi chuyến. Kết quả nằm tại `backend/target/mapbox-draft-live-result.json`; không gọi Gemini và không sửa database người dùng trong phép kiểm tra này.

Kiểm tra cập nhật 17/11: 70 test backend thuộc các nhóm draft, RAG API, Mapbox, route và Trip draft đều đạt; toàn bộ 443 test mobile đạt. TypeScript và lint các file mobile thay đổi đạt, JAR mới đã được đóng gói. Có test kiểm tra nhãn preview road/fallback/legacy/unreachable, dùng lại một matrix khi repair và giữ số liệu đường bộ qua preview → đọc lại → apply. Không chạy lại toàn bộ suite backend trong đợt này; không build APK hay kiểm thử trực quan trên điện thoại.

## Hoàn thiện luồng MVP trước khi test

- `Start-WeGo-Demo.ps1` không còn luôn truyền `-WithoutAi`; mặc định dùng Gemini đã cấu hình. Muốn tắt chủ động vẫn dùng switch `-WithoutAi`.
- Mở lại khi server đang tạo nháp: mobile theo dõi trạng thái mỗi 2,5 giây, không mở form tạo trùng. Generation lỗi được lưu mã lỗi; request bị ngắt không đứng pending vô hạn. Retry request thất bại không tự gọi lại provider và vẫn giữ dấu vết cho quota.
- Chỉnh yêu cầu tự nhiên giữ nguyên câu nhập, giờ bắt đầu/kết thúc và nhịp độ. Khi intent provider không khả dụng, nút chuyển thẳng sang biểu mẫu; không giả lập kết quả AI.
- Gemini 3 dùng mức suy luận `LOW` mặc định qua `RAG_THINKING_LEVEL` cho tác vụ trích xuất/phân bổ có schema. Có thể để rỗng để giữ mặc định provider; model khác Gemini 3 không nhận tùy chọn này. Không đổi model hoặc nới validation. Đã kiểm tra trích xuất thật đúng destination, một ngày, 08:00–17:00, RELAXED, MUSEUM/HISTORY và loại BEACH trong khoảng 17,4 giây cho một câu thử; đây không phải cam kết latency. Tài liệu: [Gemini ThinkingConfig](https://ai.google.dev/api/generate-content).
- Có `POST /api/v1/admin/planning/retrieval/index` khi bật core: chỉ ADMIN, index địa điểm đã duyệt, bỏ qua embedding còn hiện hành. Endpoint public index cũ vẫn chỉ dành cho profile dev/rag-demo.
- Kiểm chứng trên database tạm độc lập: index thật 8/8 địa điểm, retrieval HYBRID, Gemini phân bổ và chủ đề, Mapbox đường bộ, lưu bản nháp 2 ngày/6 điểm rồi đọc lại/apply thành công. Không thay dữ liệu của người dùng trong phép kiểm tra này.

Người dùng thông báo đã thu hồi key Gemini và chưa cập nhật key mới trong lúc kiểm chứng. Vì vậy đã dừng gọi provider thật; kết quả trước đó không bảo đảm key hiện tại còn dùng được. Thay `GEMINI_API_KEY` trong root `.env`, giữ key ở backend và khởi động lại process. Không cần đưa key vào chat, source hoặc biến `EXPO_PUBLIC_*`.

Sau đó người dùng đã cập nhật key mới. Kiểm chứng lại trên database tạm thành công: index 8/8, biểu mẫu có retrieval HYBRID và ALLOCATION/THEMES từ Gemini, Mapbox road, lưu/apply lịch 2 ngày/6 điểm; câu tự nhiên cũng `READY_WITH_WARNINGS`, INTENT và ALLOCATION đều thành công lần đầu. Trong tình huống thử này, form khoảng 14 giây, câu tự nhiên khoảng 43 giây. Weather của profile test là mock nên trả UNKNOWN đúng nhãn; không dùng kết quả này để khẳng định weather thật. Chi tiết trong `backend/target/ai-live-flow-result.json` và `ai-new-key-live-flow.log`.

Catalog AI loại profile có scope `DEV_DEMO_ONLY_NOT_FACT_VERIFIED`, kể cả record demo cũ đã `approved=true`. Bộ lọc dùng chung cho catalog, candidates, lexical, semantic và ghi embedding, tránh ba tên trùng giữa seed V1 và catalog RAG. Địa điểm và liên kết chuyến đi hiện có không bị xóa/sửa; script chuẩn bị demo mới tạo profile này với `approved=false`. Catalog RAG vẫn có cảnh báo tọa độ ước lượng và giờ mở cửa chưa xác minh; lọc demo không biến dữ liệu còn lại thành dữ liệu đã xác minh đầy đủ.

Kiểm tra toàn bộ trước thay đổi lọc cuối: 559 test backend và 447 test mobile đạt, TypeScript/lint các file mobile thay đổi đạt. Sau thay đổi lọc, chạy lại 53 test catalog/dev/intent/trip draft đều đạt, có regression trên PostgreSQL/pgvector thật kiểm tra loại cached embedding cũ và giữ nguyên địa điểm gốc. Không build APK, chưa kiểm tra trực quan trên điện thoại.

Backend dev đã được đóng gói và khởi động lại với key mới tại `http://localhost:18081`: health `UP`, catalog AI 8 điểm, không trùng tên và không có scope demo chưa xác minh. Index lần cuối `updated=0, skipped=8, failed=0` vì cả tám embedding đã hiện hành.

Để test bằng mã nguồn, không build APK:

1. Tại `E:/travel-system`, sau khi thay key: `./scripts/Start-Dev.ps1 -UsePackagedJar` (hoặc `Start-WeGo-Demo.ps1 -WithoutWeb` nếu cần cấu hình IP LAN).
2. Nếu danh mục chưa có embeddings hiện hành, ở terminal khác trong môi trường **dev local**: `Invoke-RestMethod -Method Post http://localhost:18081/api/v1/admin/planning/retrieval/index -Headers @{ Authorization = "Bearer $env:ADMIN_ACCESS_TOKEN" } -TimeoutSec 180` (token ADMIN đăng nhập tại chính backend này). Kiểm tra `failed=0`; khi provider chưa dùng được, hệ thống vẫn có lexical fallback được ghi nhãn. Môi trường production dùng endpoint ADMIN với access token tương ứng.
3. Chạy lại Expo ở `E:/travel-mobile-ui`: `npm start`, dùng bản dev client hiện có. Tạo chuyến Đà Nẵng trong tương lai, 1–2 ngày, chưa có lịch; vào Gợi ý lịch trình.
4. Thử form và câu tự nhiên, đổi giờ, mở lại bản nháp, kiểm tra cảnh báo rồi Apply; mở lại chuyến để kiểm tra dữ liệu lưu. Không dùng APK release cũ để kết luận giao diện JavaScript mới chưa thay đổi.

Giới hạn chủ động của MVP: Đà Nẵng 1–2 ngày, lịch mới, ô tô; chưa mở rộng chatbot, nhiều tỉnh/ngày hoặc tự động sửa lịch đang đi. Giờ mở cửa/tọa độ chưa xác minh và thời tiết UNKNOWN được ghi rõ. Chính sách xóa bản nháp hết hạn và mở rộng catalog cần được triển khai trước khi vận hành quy mô lớn.
