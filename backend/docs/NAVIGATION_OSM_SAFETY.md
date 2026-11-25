# S4 — nguồn OSM cho cảnh báo dẫn đường

Ngày: 2026-11-24. OSM là dữ liệu cộng đồng, không phải chứng nhận đầy đủ biển/camera tại Việt Nam.
Không lấy `max_speed` của router, tốc độ ETA hoặc camera CCTV làm giới hạn pháp lý/camera xử lý vi phạm.

## Tải dữ liệu miễn phí

Chạy tại `D:\WWW_JAVA\travel-system\backend`, Node 24 đang có; không cần key hoặc gói trả phí:

```powershell
# Biển, giới hạn đường, camera và công trình trong một vùng nhỏ.
node scripts/navigation/import-osm.mjs --bbox 16.045,108.20,16.075,108.24 --output target/navigation-s4/osm-danang.candidates.json

# Riêng camera được OSM khai báo trong ranh giới Việt Nam.
node scripts/navigation/import-osm.mjs --scope vn-cameras --output target/navigation-s4/osm-vietnam-cameras.candidates.json

# Khi máy chủ mặc định quá tải: chọn instance công khai miễn phí khác, không tự retry/chuyển máy chủ liên tục.
node scripts/navigation/import-osm.mjs --scope vn-cameras --server private-coffee --output target/navigation-s4/osm-vietnam-cameras.candidates.json

# Nhập bản xuất Overpass đã tải, không phát sinh truy vấn mạng.
node scripts/navigation/import-osm.mjs --input target/overpass.json --output target/navigation-s4/osm.candidates.json

node --test scripts/navigation/import-osm.test.mjs
```

Nguồn: [Overpass API](https://wiki.openstreetmap.org/wiki/Overpass_API),
[speed_camera](https://wiki.openstreetmap.org/wiki/Tag:highway%3Dspeed_camera),
[enforcement và hướng from/device/to](https://wiki.openstreetmap.org/wiki/Relation:enforcement),
[maxspeed](https://wiki.openstreetmap.org/wiki/Key:maxspeed),
[traffic_sign](https://wiki.openstreetmap.org/wiki/Key:traffic_sign).

Giới hạn truy vấn đầy đủ 0,25 × 0,25 độ, response 10 MB/10.000 element, timeout 35 giây,
không tự retry liên tục. Truy vấn camera toàn quốc chỉ lấy camera/enforcement, không lấy toàn bộ đường.
HTTP lỗi/Overpass `remark` được báo lỗi; không tạo catalog thành công rỗng từ response timeout.
Không gửi tài khoản, token, hành trình hay từng GPS fix tới Overpass. Backend/mobile không gọi Overpass trong lúc lái.

## Kiểm duyệt trước khi sử dụng

File tải về chứa `candidates`, giữ OSM identity, URL, version, timestamp, tags, geometry và relation members.
`sources.reviewStatus=PENDING`, `verifiedAt=null`, `speedZones=[]`, `events=[]`.
Ngày tải hoặc ngày OSM chỉnh sửa **không được đổi thành ngày khảo sát/kiểm tra biển**.

Người quản lý dữ liệu tạo **file reviewed riêng**, không sửa kết quả tìm tuyến hoặc fixture thành dữ liệu thật:

1. Kiểm tra URL OSM và bằng chứng hiện tại cho từng biển/camera. Phân biệt camera quan sát và xử lý vi phạm.
2. Xác định hình học đường **theo chiều di chuyển**, phương tiện `DRIVE` hoặc `TWO_WHEELER`; tạo hai hàng riêng nếu áp dụng cả hai.
3. Biển tốc độ cần số km/h rõ ràng, bằng chứng bắt đầu và kết thúc phạm vi. Điểm cuối một OSM way không tự chứng minh hết hiệu lực biển.
4. Biển theo giờ/làn/loại xe hoặc hiệu lực chưa giải quyết được không được chuyển thành `UNCONDITIONAL`.
5. Nguồn được duyệt theo từng lô đã kiểm tra: `reviewStatus=APPROVED`, ngày `verifiedAt` thực tế, attribution/license và quyền cache/phân phối.
6. `validFrom <= now < validUntil`. Biển/camera hết hạn muộn nhất 30 ngày sau lần kiểm tra; vật cản/đường sửa muộn nhất 24 giờ. Đây là trần bảo thủ của app, không phải bảo đảm dữ liệu đúng trong suốt khoảng đó.

Contract file (`version=1`):

| Nhóm | Field bắt buộc |
|---|---|
| `sources[]` | `id`, `title`, HTTPS `url`, `license`, `attribution`, ISO `verifiedAt`, `reviewStatus`, boolean `licenseAllowsCache`, `licenseAllowsRedistribution` |
| `speedZones[]` | `id`, `sourceId`, `vehicle`, directed `geometry:[{latitude,longitude},...]`, `validFrom`, `validUntil`, `condition:UNCONDITIONAL`, integer `limitKmh:1..150`, HTTPS `startEvidence`, `endEvidence` |
| `events[]` | `id`, `sourceId`, `vehicle`, directed approach `geometry`, `validFrom`, `validUntil`, `condition:UNCONDITIONAL`, `type:SPEED_CAMERA/RED_LIGHT_CAMERA/OBSTACLE/ROADWORKS` |

Hình học event chạy dọc đoạn tiếp cận, kết thúc ở vị trí event. Hình học zone chạy từ đầu đến cuối phạm vi đã kiểm tra.
ID phải ổn định qua các lần cập nhật để chống đọc/rung lặp. Fixture minh họa chỉ nằm trong test, không được đưa vào catalog production.

## Cấu hình và hành vi

Đặt `NAVIGATION_SAFETY_CATALOG` trong `backend/.env` thành đường dẫn tuyệt đối tới **file reviewed** rồi restart backend.
Để trống khi chưa có dữ liệu đủ điều kiện. Không cần migration/Flyway hoặc gọi thêm Geoapify.
Catalog được đọc khi khởi động, tối đa 2 MB/5.000 hàng mỗi nhóm. File lỗi/trùng ID không tạo safety claims;
hàng sai nguồn, phạm vi, loại xe, ngày hoặc hình học bị loại. Cập nhật/rút dữ liệu: sửa file reviewed và restart;
tuyến đã tải trên điện thoại giữ snapshot đến khi tải tuyến khác hoặc hết hạn. Chưa có push thu hồi offline.

Backend chỉ gắn dữ liệu trùng chiều và khớp hình học chặt (3 m), kiểm tra cả đoạn mỗi khoảng 20 m;
đường song song/lặp/không rõ vị trí bị loại. Một zone phải khớp trọn trong chặng, nên có thể bỏ sót zone bị cắt tại đầu/cuối chặng.
**Giới hạn:** chưa có road ID chung giữa OSM và router; trùng tim đường, nhiều tầng, sai số bản đồ hoặc hình học có thể gây bỏ sót/nhầm.
Không dùng kết quả unit test để tuyên bố đã phân biệt được mọi đường song song/cầu vượt ngoài thực địa.

Response bổ sung `safety:{routeId,speedZones,events}` với khoảng cách dọc từng chặng, source và thời hạn.
`dataAvailability` chỉ `UNKNOWN` hoặc `PARTIAL`, không có trạng thái phủ toàn bộ.
Không có dữ liệu hợp lệ thì `safety=null`, tuyến vẫn dùng được.

## Giấy phép

[OpenStreetMap copyright/ODbL](https://www.openstreetmap.org/copyright) cho phép sử dụng dữ liệu với các nghĩa vụ ghi nguồn và chia sẻ cơ sở dữ liệu phái sinh khi áp dụng.
App giữ attribution và liên kết bản quyền; không phát hành dữ liệu phái sinh với giấy phép đóng hoặc xóa attribution.
File candidates hiện ở `target/`, bị Git ignore; chưa đóng gói bộ dữ liệu OSM vào APK hay phát hành ra công chúng.
Khi phát hành catalog cho người dùng cần cung cấp giấy phép và cách tiếp cận cơ sở dữ liệu phái sinh tương ứng; việc giữ trong Git ignore không thay thế nghĩa vụ đó.

## Kiểm tra nguồn thật lần này

Vùng Đà Nẵng `16.045,108.20,16.075,108.24`: tải thành công 367 ứng viên (364 biển/giới hạn đường, 3 đường xây dựng), 0 camera trong kết quả.
Lần đầu vùng lớn hơn nhận HTTP 502. Truy vấn camera toàn quốc timeout ở cả osm-de và private-coffee, chưa có kết quả để kết luận độ phủ.
Không mục nào được tự duyệt. **Active zones/events = 0**; nguồn thật cho SPEED/EVENT còn NO DATA/BLOCKED.
Các số này là mẫu nhập dữ liệu, không chứng minh không có camera hoặc toàn bộ dữ liệu đều còn hiệu lực.
