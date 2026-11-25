# Danh mục địa điểm để chọn khi lên kế hoạch

Phạm vi đã được người dùng chốt: **63 tỉnh/thành theo danh mục của đề tài trước sắp xếp năm 2025**. Đây không phải danh mục hành chính hiện hành. Giữ tên Thừa Thiên Huế theo phạm vi này.

## Dữ liệu và Flyway

- Danh mục hiện tại: **12.726 địa điểm tham khảo**, đủ **63 tỉnh/thành, mỗi tỉnh/thành ít nhất 20 điểm**. Chi tiết số lượng: [CATALOG_COVERAGE.md](CATALOG_COVERAGE.md). Đây không phải danh sách địa điểm được chứng nhận du lịch hoặc đã xác minh thực địa.
- `src/main/resources/db/catalog/vietnam-tourism-v1.json`: snapshot khởi đầu 104 điểm, giữ nguyên để bảo toàn checksum V25.
- `src/main/resources/db/catalog/vietnam-tourism-v2.json`: 12.622 bản ghi bổ sung bằng `V26__Expand_tourism_catalog.java`; tổng cộng 12.726 khi nâng cấp từ V25. ID cũ và đánh giá/điều chỉnh đã lưu được giữ nguyên. Snapshot v2 cũng bất biến sau khi áp dụng; cập nhật tiếp phải dùng migration và snapshot mới.
- `src/main/java/db/catalog/V25__Tourism_catalog.java`: Java migration Flyway V25, đọc snapshot JSON đóng gói cùng backend, chạy trong transaction. Không gọi dịch vụ ngoài khi khởi động.
- `FlywayHistoryConfiguration` thêm location `classpath:db/catalog` sau khi chọn đúng lịch sử SQL của database. Áp dụng được cho database canonical mới và lịch sử merge hiện tại. Không chỉnh sửa các migration đã chạy.
- Checksum V25 được tính từ bytes của snapshot JSON. Sau khi áp dụng, không sửa V25 hoặc snapshot v1; thêm migration và snapshot phiên bản tiếp theo để cập nhật.
- ID địa điểm được giữ cố định; không sinh lại mỗi lần khởi động. Hồ sơ danh mục có nguồn, ngày đối chiếu, phạm vi kiểm chứng và nhãn chất lượng.
- Các mã cũ DN, LD, HG, LC, QN, KG, TTH được chuẩn hóa sang mã danh mục. Giữ ID tỉnh cũ khi có thể; nếu có cả mã cũ/mã chuẩn cho cùng tỉnh thì chuyển liên kết Place/Trip trước khi hợp nhất bản ghi trùng. Không chỉnh sửa Trip, Memory hay ảnh ngoài khóa liên kết tỉnh cần hợp nhất.
- Không biến địa điểm `USER` thành danh mục công khai. Nếu ID snapshot xung đột với địa điểm riêng tư, migration dừng và rollback để kiểm tra thủ công. Metadata/profile đã tồn tại được giữ nguyên, kể cả trạng thái bỏ duyệt.

## API sử dụng

Các API yêu cầu JWT hiện có:

```text
GET /api/v1/destinations?q=ha%20noi&page=0&size=20
GET /api/v1/places/suggestions?provinceId=<id-tỉnh>&q=&page=0&size=20
GET /api/v1/places/suggestions?tripId=<id-chuyến-đi>&page=0&size=20
GET /api/v1/places/<id-địa-điểm>
POST /api/v1/trips
POST /api/v1/trips/<id-chuyến-đi>/places/batch
```

Chọn tỉnh → lấy suggestions → gửi các ID đã chọn vào `initialPlaceIds` khi tạo Trip hoặc `placeIds` khi thêm hàng loạt. Suggestions theo Trip bỏ các điểm đã có, kiểm tra ownership trước khi đọc và giữ pagination. Không truyền cả tripId và provinceId cùng lúc.

Chi tiết địa điểm trả mô tả và attribution URL từ hồ sơ được duyệt. Địa điểm riêng tư vẫn chỉ chủ sở hữu xem được; hồ sơ riêng tư không được dùng làm attribution danh mục.

## Nguồn và giới hạn dữ liệu

- Danh mục mã/tên: snapshot [API tỉnh/thành v1](https://provinces.open-api.vn/api/v1/?depth=1), theo phạm vi 63 tỉnh/thành đã chọn. Chuẩn hóa Hòa Bình và Thừa Thiên Huế theo tên của đề tài.
- Phần lớn tọa độ tham chiếu: [Wikidata](https://www.wikidata.org/wiki/Wikidata:Data_access), dữ liệu cấu trúc CC0; một số tọa độ từ GeoData/template của Wikipedia, ghi nguồn từng record. Không sao chép bài viết hoặc ảnh. Mô tả ngắn và nhóm trải nghiệm được biên soạn riêng.
- Ao Bà Om: tọa độ công bố tại [Tanitour](https://tanitour.vn/ao-ba-om/); danh thắng được giới thiệu bởi [Cục Du lịch Quốc gia Việt Nam](https://vietnamtourism.vn/index.php/tourism/items/1395).
- Cát Bà: dùng điểm giữa phạm vi công bố trong [Quyết định 333/2004/QĐ-TTg](https://vbpl.moj.gov.vn/vanphongchinhphu/Pages/vbpq-toanvan.aspx?ItemID=81202), nhãn `AREA_REFERENCE` để hiển thị khu vực. Không dùng điểm đó như cổng vào đường bộ đã xác minh.
- Bổ sung từ [OpenStreetMap](https://www.openstreetmap.org/copyright), bản trích xuất Việt Nam của [Geofabrik](https://download.geofabrik.de/asia/vietnam.html), thu thập ngày 09/11/2026. Snapshot chứa checksum SHA-256 của đầu vào, URL nguồn từng điểm và nhãn `OSM_POINT`/`OSM_AREA_REFERENCE`. Giữ tên gốc khi nguồn không có tên tiếng Việt.
- Chỉ chọn đối tượng có tên và thẻ thuộc nhóm điểm tham quan, bảo tàng, di tích, cảnh quan tự nhiên, công viên hoặc địa điểm văn hóa/tín ngưỡng. Loại địa điểm có thẻ riêng tư/cấm vào, ngừng hoạt động, nghĩa trang, tên chung chung và bản ghi trùng theo ID nguồn hoặc tên/vị trí. Đây là bộ lọc dữ liệu cộng đồng, không thay cho khảo sát từng địa điểm hoặc xác nhận quyền tiếp cận.
- Gán tỉnh theo tọa độ trong polygon của bộ 63 tỉnh trước sắp xếp: Nguyễn Duy Liêm (2025), [Vietnam GIS Data](https://github.com/nguyenduy1133/Free-GIS-Data), commit `ccb9f4ae992418bfeefd06da1eb42d0249632176`. Điểm không nằm trong ranh giới được bỏ qua, không gán sang tỉnh gần nhất. Ranh giới và tâm vùng chỉ là dữ liệu tham chiếu.
- Bắc Kạn/Bạc Liêu được bổ sung từ tọa độ công bố tại [Cổng thông tin du lịch Thái Nguyên](https://mythainguyen.vn/vi/places) và Tanitour; vẫn gán theo tỉnh Bắc Kạn/Bạc Liêu cũ. Chỉ lấy tên và tọa độ; không sao chép nội dung bài viết hoặc ảnh.
- Snapshot v2 có dữ liệu dẫn xuất OpenStreetMap, phát hành theo **ODbL 1.0**, ghi công **© OpenStreetMap contributors**. Metadata cấp dataset lưu URL giấy phép và nguồn; mô tả từng điểm OSM cùng API chi tiết cũng có ghi công/đường dẫn giấy phép. Snapshot công khai này không chứa dữ liệu tài khoản hoặc địa điểm riêng tư.
- 8 điểm pilot Đà Nẵng giữ ID, nguồn và điều kiện dữ liệu ban đầu. RAG vẫn chỉ truy xuất tỉnh Đà Nẵng, nay có thêm các điểm thuộc tỉnh này; việc nạp catalog không gọi API embedding hoặc dịch vụ AI.
- `approved` nghĩa là được tuyển chọn vào danh mục, không có nghĩa mọi thuộc tính đã xác minh. `REFERENCE_POINT` và `APPROXIMATE_DEMO` không bảo đảm cổng vào hoặc điểm đỗ xe.
- Không tự điền giờ mở cửa, giá vé, lịch biểu diễn hoặc URL ảnh. Các mục này vẫn UNKNOWN/NOT_PROVIDED nếu chưa có dữ liệu. Thời lượng tham quan là ước lượng lập kế hoạch.
- API gợi ý theo tỉnh dùng PostgreSQL thông thường, không cần pgvector. Mobile tiếp tục gọi cùng endpoint và phân trang; không cần đóng gói toàn bộ 12.726 điểm vào app.

## Biên soạn snapshot

`scripts/catalog/build-vietnam-tourism-v2.mjs` là công cụ biên soạn offline, không chạy cùng ứng dụng và không ghi đè file đã phát hành. Công cụ đọc các đầu vào khảo sát trong thư mục được truyền vào: `mapped-pois.json`, `historical-provinces.geojson`, `boundary-source.json`, `portal-pois.json`, `baclieu-supplements.json` và bản trích xuất `vietnam-20261108.osm.pbf` đã ghi checksum. Các file thô lớn nằm ngoài Git. Nếu cần kiểm tra lại trong workspace hiện tại, truyền một đường dẫn output mới bên ngoài thư mục migrations; không sửa snapshot v1/v2 sau khi đã chạy Flyway.

## Kiểm chứng

`TourismCatalogIntegrationTest` kiểm tra 63 tỉnh có gợi ý, tìm không dấu, phân trang, mô tả/nguồn, chọn điểm tạo Trip, thêm điểm hàng loạt và không gợi ý lại điểm đã thêm. `FlywayHistoryIntegrationTest` kiểm tra nâng cấp lịch sử cũ, checksum, giữ ảnh, giữ ID tỉnh và dữ liệu địa điểm riêng tư. Các test dùng PostgreSQL riêng; chưa coi tọa độ tham chiếu là nghiệm thu thực địa.

V26: **79 test PASS** trên PostgreSQL 18 riêng (TourismCatalog, PlaceDiscovery, DestinationCatalog, FlywayHistory, TripCrud, Trip, EmailAuth). Test đọc tất cả các trang API của 63 tỉnh và đối chiếu 12.726 ID duy nhất, kiểm tra tối thiểu 20 điểm/tỉnh, nguồn OSM và rollback toàn bộ V26 khi một ID cuối snapshot xung đột với địa điểm riêng tư. V25 giữ nguyên checksum `-1526114177`; V26 có checksum `355063705`.

Đã sao lưu database local trước nâng cấp và áp dụng V26 qua Flyway. Đối chiếu thực tế: 63 tỉnh, min 20/max 1.572 điểm mỗi tỉnh, tổng 12.726 điểm được duyệt; catalog RAG Đà Nẵng có 220 điểm, health HTTP 200. Fingerprint của toàn bộ các địa điểm đã tồn tại, users, trips, memories, media, trip_places, itinerary_items và ID tỉnh không đổi. `RagDevIntegrationTest` đã cập nhật expectation cho catalog lớn; chưa chạy test pgvector này vì Docker hiện không khả dụng.

Lượt V25 trước đó: 76 test thuộc các nhóm trên đã PASS. Database được sao lưu trước khi Flyway áp dụng V25; khi đó có 63 tỉnh/thành, 104 địa điểm được duyệt và 63 tỉnh có gợi ý, catalog RAG Đà Nẵng có 8 điểm.

Lượt regression mở rộng ngày 09/11/2026 chạy 140 test, còn 17 lỗi ngoài thay đổi danh mục: 15 test `MemoryCaptureIntegrationTest` liên quan upload/provider và 2 test `HomeDashboardIntegrationTest` vẫn kỳ vọng mã read-only `TRIP_006` thay vì `TRIP_013`. Chưa sửa các module này trong task danh mục; không coi toàn bộ regression đã xanh.
