# Trip & Trip Place API

Base URL: `http://localhost:8080/api/v1`. Các endpoint yêu cầu `Authorization: Bearer <accessToken>`; user được xác định từ JWT. Không truyền `userId` để chọn chủ sở hữu. `tripId` trong path là nguồn xác định Trip; body `tripId` không được dùng thay thế.

Contract này bổ sung triển khai cho [Phase 8 — 8C](phase-8/PHASE_8_API_BACKEND_SERVICE_DESIGN.md#8c--trip--place-api). OpenAPI từ backend là tài liệu schema đang chạy; collection bên dưới là ví dụ và kiểm tra hành vi, không thay đổi binding/workspace Postman.

## Endpoint

| Method | Path | Kết quả |
|---|---|---|
| POST | `/trips` | 201, Trip vừa tạo |
| GET | `/trips` | 200, mảng Trip của user hiện tại |
| GET | `/trips/{tripId}` | 200, Trip chi tiết |
| PUT | `/trips/{tripId}` | 200, Trip sau cập nhật từng phần |
| DELETE | `/trips/{tripId}` | 204, soft-delete Trip |
| POST | `/trips/{tripId}/places` | 201, quan hệ TripPlace vừa thêm |
| GET | `/trips/{tripId}/places` | 200, mảng TripPlace theo `plannedOrder` |
| PUT | `/trips/{tripId}/places/{placeId}` | 200, TripPlace sau cập nhật |
| DELETE | `/trips/{tripId}/places/{placeId}` | 204, gỡ quan hệ TripPlace |

`placeId` trong path là ID của Place, không phải ID của TripPlace. GET danh sách trả về mảng trong `data`, không trả đối tượng `Page`.

## Tạo và sửa Trip

```json
{
  "name": "Đà Nẵng 3 ngày",
  "destination": "Đà Nẵng",
  "startDate": "2027-06-10",
  "endDate": "2027-06-12",
  "transportation": "Car",
  "preferences": ["Nature", "Food"],
  "budget": 3000000,
  "constraints": "Không đi quá sớm",
  "description": "Chuyến đi cá nhân",
  "coverImage": "https://example.com/cover.jpg"
}
```

- POST bắt buộc `name`, `destination`, `startDate`, `endDate`; tên và điểm đến không được để trắng. Ngày dùng `YYYY-MM-DD`, ngày bắt đầu không sau ngày kết thúc. Các trường còn lại là optional; `budget` không âm và `preferences` là mảng chuỗi.
- Giới hạn: `name` 100 ký tự, `destination` 255, `transportation` 50, tối đa 30 preference (mỗi chuỗi không trắng và dài tối đa 100), `constraints` 2.000, `description` 10.000, `coverImage` 2.048. Cover không rỗng phải là URL HTTP/HTTPS. Budget có tối đa 17 chữ số phần nguyên và 2 chữ số thập phân.
- Trip mới có `status=PLANNING`. PUT nhận các trường trên và `status`: `PLANNING`, `READY`, `ACTIVE`, `COMPLETED`; `CANCELLED` được giữ để tương thích dữ liệu cũ.
- PUT là cập nhật từng phần: trường thiếu hoặc `null` giữ giá trị cũ. Chuỗi rỗng xóa nội dung của trường text optional; `preferences: []` xóa danh sách sở thích. Ngày vẫn được kiểm tra trên cặp ngày sau khi áp dụng cập nhật.
- Không tự xóa itinerary khi sửa ngày. Không cho đổi owner qua request.

Response Trip nằm trong wrapper:

```json
{
  "success": true,
  "message": "success",
  "data": {
    "id": "00000000-0000-0000-0000-000000000001",
    "userId": "00000000-0000-0000-0000-000000000002",
    "name": "Đà Nẵng 3 ngày",
    "destination": "Đà Nẵng",
    "startDate": "2027-06-10",
    "endDate": "2027-06-12",
    "status": "PLANNING",
    "preferences": ["Nature", "Food"],
    "placeCount": 0,
    "visitedCount": 0,
    "memoryCount": 0,
    "createdAt": "2027-01-01T00:00:00Z",
    "updatedAt": "2027-01-01T00:00:00Z"
  }
}
```

Các trường optional đã lưu như `transportation`, `budget`, `constraints`, `description`, `coverImage` cũng được trả về. Chi tiết và response create/update có số đếm `placeCount`, `visitedCount`, `memoryCount` để client cập nhật cache Trip.

## Thêm và quản lý Place trong Trip

Thêm Place đã tồn tại bằng internal UUID:

```json
{ "placeId": "00000000-0000-0000-0000-000000000003", "notes": "Đi buổi sáng" }
```

Hoặc tạo Place từ thông tin người dùng/provider:

```json
{
  "name": "Bãi biển Mỹ Khê",
  "latitude": 16.0544,
  "longitude": 108.2473,
  "providerPlaceId": "optional-provider-reference",
  "address": "Đà Nẵng",
  "category": "Beach",
  "priority": 1,
  "estimatedDuration": 90,
  "notes": "Ngắm biển",
  "plannedOrder": 0
}
```

- Gửi `placeId` để chọn master Place đã tồn tại; metadata Place khác không cập nhật master trong trường hợp này. Nếu không có `placeId`, bắt buộc dữ liệu Place mới với `name`, `latitude`, `longitude`. Latitude trong [-90, 90], longitude trong [-180, 180], không nhận NaN/vô cực; `provinceId` optional phải tham chiếu Province tồn tại.
- Metadata optional của Place mới: `providerPlaceId`, `address`, `provinceId`, `category`. Không tự gộp Place theo provider ID, tên hay tọa độ. Muốn dùng chung master Place, gửi internal `placeId`.
- Metadata quan hệ: `priority >= 0`, `estimatedDuration > 0` (phút), `notes`, `plannedOrder` từ 0 đến 1.000.000. `plannedOrder` là vị trí 0-based: chèn/di chuyển một link sẽ dịch vị trí các link còn lại; giá trị vượt độ dài danh sách được đưa về cuối. Nếu bỏ `plannedOrder` khi thêm, Place được thêm cuối; khi xóa link, thứ tự được chuẩn hóa liên tục.
- Giới hạn metadata: `name`/`providerPlaceId` 255 ký tự, `address`/`notes` 2.000, `category` 100.
- Một Trip không thể thêm trùng cùng internal `placeId`; server trả 409. Thêm dữ liệu Place mới không bảo đảm chống trùng vật lý theo tên/tọa độ.
- PUT chỉ sửa `priority`, `estimatedDuration`, `notes`, `plannedOrder`, `visited` của TripPlace. Không sửa master Place dùng chung. Trường thiếu/null giữ nguyên; `notes: ""` xóa ghi chú.
- Response TripPlace có `id`, `tripId`, `placeId`, nested `place`, metadata quan hệ, `visited`, `createdAt`, `updatedAt`. Nested Place gồm ID, tên, tọa độ và metadata đã lưu; `provinceName` có thể trả cùng `provinceId`.

## Quyền truy cập, xóa và lỗi

- Mọi operation kiểm tra Trip còn hoạt động và thuộc user đang đăng nhập. Trip của user khác được xử lý như không tồn tại.
- Xóa Trip chỉ soft-delete: Memory, ảnh và Place vẫn được giữ. Các API Trip/TripPlace không còn truy cập Trip đã xóa.
- Gỡ Place chỉ xóa quan hệ TripPlace, giữ master Place và Memory gắn với Place.
- Tạo/sửa nhiều bản ghi nằm trong một transaction. Thất bại validation không để lại TripPlace hay Place mới một phần.

| HTTP | Code | Trường hợp |
|---|---|---|
| 400 | `COMMON_003` | Body thiếu/sai trường hoặc vi phạm validation |
| 400 | `TRIP_004` | Ngày bắt đầu sau ngày kết thúc |
| 401 | `AUTH_001` | Thiếu hoặc không có xác thực hợp lệ |
| 404 | `TRIP_001` | Trip không tồn tại, đã xóa hoặc thuộc user khác |
| 404 | `PLACE_001` | Place tham chiếu không tồn tại |
| 404 | `PLACE_002` | Province tham chiếu không tồn tại |
| 404 | `TRIP_002` | Place chưa được gắn vào Trip |
| 409 | `TRIP_003` | Internal Place đã được gắn vào Trip |

Body lỗi gồm `success: false`, `code`, `message`, `timestamp`, và `errors` khi có lỗi theo field. DELETE thành công không có response body.

## Chạy sequence Postman local

Import [Travel_Trip_Place_Collection.postman_collection.json](../postman/Travel_Trip_Place_Collection.postman_collection.json). Đặt collection variable `accessToken` bằng access token hợp lệ của tài khoản hiện tại; không ghi token vào file Git. Không chọn environment có `baseUrl`/`accessToken` khác đè các biến này.

`baseUrl` mặc định là `http://localhost:8080/api/v1`. Chạy toàn bộ collection theo thứ tự; collection tự tạo ngày tương lai, tạo Trip, lấy/cập nhật Trip, tạo Place, kiểm tra duplicate, sửa/gỡ/thêm lại Place, rồi soft-delete Trip. ID được lấy từ response vào `tripId`/`placeId`; không cần đăng ký hoặc gửi email.

Sequence có kiểm tra status, wrapper, số đếm, cập nhật từng phần, lỗi ngày/trùng và quyền truy cập sau soft-delete. Nó ghi dữ liệu vào backend đang chọn: dùng môi trường local/test. Bước cleanup xóa Trip và gỡ link; master Place mẫu được giữ đúng contract. Nếu dừng giữa chừng, chạy hai request cleanup cuối trước bước kiểm tra 404.

Collection không kiểm chứng cách ly giữa hai user hoặc bảo toàn Memory có sẵn; các trường hợp này thuộc integration test backend. Chưa thay đổi contract Itinerary, Journey, Memory hay tích hợp Places API bên ngoài.
