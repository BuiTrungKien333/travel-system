# Memory & ảnh

API nguồn: controller/DTO trong `backend`, công bố qua `/v3/api-docs`. Snapshot dành cho client: [memory-media.openapi.json](api/memory-media.openapi.json). Snapshot được trích từ Springdoc, không chỉnh độc lập với code.

## Luồng lưu

1. `POST /api/v1/journal/memories` bằng JSON: `clientRequestId` (UUID, nên gửi), `tripId`, `placeId`, `note`, `experiencedAt` (tất cả ngoài request ID đều tùy chọn). `placeId` phải thuộc Trip của người dùng. Không nhận URL ảnh do client tự khai; `media` chỉ chấp nhận danh sách rỗng.
2. `POST /api/v1/media/uploads/atomic?memoryId=...&tripId=...` bằng multipart, trường `files` lặp lại. Upload một ảnh dùng `/media/upload`, trường `file`. **memoryId bắt buộc**, `tripId` không bắt buộc nhưng phải khớp Memory. Memory không gắn Trip vẫn upload được.
3. `GET /api/v1/journal/memories/{id}` để nhận version, note, thời gian và toàn bộ media ID/URL/kích thước/thứ tự.

`clientRequestId` được lưu với hash payload chuẩn hóa và unique index theo người dùng. Gửi lại cùng ID/payload trả cùng Memory; khác payload trả `409 MEMORY_004`. Việc tạo không tự lặp khi mất kết nối: client giữ request ID và payload ban đầu để thử lại. Memory đã xóa không được khôi phục bởi request tạo lại.

Upload kiểm tra SHA-256 theo Memory: gửi lại byte giống nhau trả media đã có. Ảnh khác metadata/EXIF có thể có hash khác. Hai upload đồng thời cùng nội dung tạo một media; tài nguyên Cloudinary thừa được dọn. Batch mới chỉ được gắn vào Memory khi tất cả upload thành công và transaction ghi DB commit. Nếu thất bại, các ảnh sẵn có vẫn được giữ.

Hủy ở trình duyệt dừng chờ request, không đảm bảo server chưa commit. Giao diện giữ Memory đã tạo, cho phép kiểm tra bản đã lưu và thử lại; không tự xóa bản ghi khi kết quả upload chưa rõ.

## Giới hạn mặc định

| Giới hạn | Mặc định | Cấu hình môi trường |
|---|---:|---|
| Dung lượng một ảnh | 10 MiB | `MEDIA_MAX_FILE_BYTES` |
| Tổng ảnh mỗi lượt | 50 MiB | `MEDIA_MAX_BATCH_BYTES` |
| Số ảnh mỗi lượt | 10 | `MEDIA_MAX_FILES_PER_REQUEST` |
| Số ảnh mỗi Memory | 20 | `MEDIA_MAX_FILES_PER_MEMORY` |
| Số pixel ảnh đầu tiên | 25.000.000 | `MEDIA_MAX_PIXELS` |
| Chiều dài mỗi cạnh | 12.000 px | `app.media.max-dimension` |
| Upload mỗi user mỗi phút | 20 request | `MEDIA_UPLOADS_PER_MINUTE` |
| Upload đồng thời mỗi user | 2 | `MEDIA_CONCURRENT_UPLOADS_PER_USER` |
| Upload đồng thời toàn process | 2 | `MEDIA_CONCURRENT_UPLOADS` |
| Ghi chú | 2.000 ký tự | DTO/database |
| Danh sách Memory | 20 mặc định, tối đa 50/trang | `size` trong request |

`GET /api/v1/media/limits` trả giới hạn hiện tại cho form. Servlet chặn 10 MiB/file và 101 MiB/request (chừa multipart overhead); batch atomic vẫn bị chặn ở 50 MiB tại ứng dụng; giới hạn ứng dụng không được tăng vượt trần servlet mà không cập nhật cấu hình và kiểm tra tài nguyên. Các giá trị cấu hình có validation khi khởi động.

JPEG, PNG, GIF, WebP được nhận diện bằng ImageIO, kiểm tra kích thước trước khi giải mã ảnh đầu tiên; khai MIME giả hoặc nội dung không giải mã được bị từ chối. WebP dùng TwelveMonkeys ImageIO. Không nhận SVG, PDF hoặc video. Không tự giảm chất lượng ảnh gốc.

Giới hạn admission/rate áp dụng **mỗi process** và bộ nhớ trạng thái có giới hạn. Khi chạy nhiều replica cần rate limiter chung tại gateway/Redis. Quota số ảnh và chống trùng trong PostgreSQL vẫn được kiểm tra dưới khóa bản ghi, bảo đảm giữa các replica. Chưa áp dụng quota tổng dung lượng trọn đời cho user.

## Đọc, sửa, xóa

- `GET /journal/memories?tripId=...&page=0&size=20&sort=experiencedAt,desc`: `data.content`, `number`, `totalElements`, `totalPages`, `last`. Chỉ trả dữ liệu của user hiện tại, kiểm tra quyền Trip khi lọc. Sort chỉ nhận `id`, `experiencedAt`, `createdAt`, `updatedAt` (hỗ trợ tên snake_case); ID được thêm làm tie-break. Pagination ở DB, media nạp theo batch; không join-fetch collection làm phân trang trong RAM.
- `PATCH /journal/memories/{id}`: bắt buộc `version` là số nguyên không âm. `note` bỏ qua thì giữ nguyên, `null`/rỗng để xóa ghi chú; `experiencedAt` không được ở tương lai; `mediaOrder` phải chứa toàn bộ media ID hiện tại, mỗi ID đúng một lần. Ảnh đầu là ảnh bìa. Version cũ trả `409 MEMORY_003`.
- `DELETE /media?id={mediaId}`: xác nhận xóa Cloudinary trước khi xóa metadata và cập nhật version Memory. Lỗi provider giữ ảnh và lưu yêu cầu để retry; gửi lại yêu cầu xóa là idempotent.
- `DELETE /journal/memories/{id}`: soft-delete Memory, enqueue dọn Cloudinary. Không đổi Place/visited. Alias query `DELETE /journal/memories?id=...` và `GET /journal/memories/detail?memoryId=...` vẫn hoạt động.

`/memory` là gallery/timeline có tải thêm; `/journal` chuyển tới cùng view. `/trips/{id}/memories` là view lọc theo Trip. Đăng xuất xóa cache và đóng Memory đang mở.

## Lưu trữ & dọn lỗi

Mỗi upload có public ID ngẫu nhiên và một cleanup intent bền vững trong DB **trước** khi gọi Cloudinary. Attach thành công xóa intent trong cùng transaction ghi metadata; lỗi làm job được chạy sớm. Nếu process dừng giữa chừng, intent chưa hủy đến hạn sau một giờ. Không giữ DB transaction qua Cloudinary I/O.

Worker chạy mỗi 30 giây, claim job bằng `FOR UPDATE SKIP LOCKED`, lease 2 phút, xử lý tối đa 20 job/lượt. Lỗi được thử lại với backoff tối đa một giờ. Cấu hình `app.media.cleanup-enabled=false` để tắt, `app.media.cleanup-interval` để đổi chu kỳ. Worker cũng tìm ảnh thuộc Memory soft-delete bằng Trip CRUD. Trước khi xóa storage, worker kiểm tra ảnh có còn gắn với Memory đang hoạt động hay không; điều này bảo vệ trường hợp mất xác nhận commit.

Xóa Cloudinary dùng `invalidate=true`; CDN có thể cần thời gian để vô hiệu cache. Dữ liệu ảnh cũ thiếu `public_id` không bị đoán ID từ URL và không được tự dọn; cần kiểm kê riêng. Metadata Memory được bảo vệ bằng quyền sở hữu; URL delivery Cloudinary hiện vẫn là URL CDN công khai khi người khác có đường dẫn, không phải signed private delivery.

Sau khi gộp nhánh: giữ bộ migration V1–V19 của `develop`; ghim điểm, tọa độ xuất phát, capture request ID và upload integrity nằm ở V20–V23. Database từ lịch sử Flyway cũ của `feature/merge-test` cần đối chiếu riêng trước khi nâng cấp; không tự repair/reset.

## Mã lỗi chính

| HTTP | Code | Ý nghĩa |
|---:|---|---|
| 400 | `UPLOAD_003`, `UPLOAD_008` | Nội dung/MIME hoặc pixel không hợp lệ |
| 413 | `UPLOAD_002`, `UPLOAD_011` | Vượt dung lượng ảnh/request |
| 409 | `UPLOAD_007` | Vượt quota ảnh của Memory |
| 429 | `UPLOAD_009` | Vượt rate/concurrency; header `Retry-After: 60` |
| 500 | `UPLOAD_004` | Provider lỗi/metadata không hợp lệ; không gắn batch dở dang |
| 403/404 | `MEMORY_002`/`MEMORY_001` | Không có quyền/không tìm thấy Memory |

## Kiểm tra

```powershell
cd backend
./mvnw.cmd test '-Dapi.version=1.44'
cd ../frontend-web
npm run test:memory
npm run type-check
npm run build
```

Docker API override chỉ cần cho Testcontainers trên Docker hiện tại của máy. Test HTTP của SDK mặc định chạy với HTTP stand-in nội bộ, không cần Cloudinary thật.

Để chạy SDK thật qua Postman simulation local (không đăng nhập/publish):

```powershell
postman simulation run postman/simulations/memory-storage.sim.yaml --port 4508 --no-history --output ndjson
# Terminal khác, trong backend:
./mvnw.cmd test '-Dtest=CloudinaryStorageHttpTest' '-Dcloudinary.simulation-url=http://127.0.0.1:4508/cloudinary'
```

Mock được tạo từ subset contract của provider tại `postman/contracts/cloudinary-storage.openapi.yaml`, rồi bổ sung state và tình huống lỗi. CLI chưa đăng nhập nên không tìm/pull mock của chủ dịch vụ. Simulation chứng minh hành vi caller với stand-in, không xác nhận credentials, quyền upload hoặc CDN trên tài khoản Cloudinary thật.

Tham chiếu provider: [Cloudinary Upload API](https://cloudinary.com/documentation/image_upload_api_reference), [TwelveMonkeys ImageIO](https://github.com/haraldk/TwelveMonkeys).

API batch mới từ develop vẫn dùng `POST /api/v1/media/uploads` (cần cả tripId/memoryId), trả HTTP 200 với `items`, `succeeded`, `failed`. Frontend web dùng `/uploads/atomic` để giữ hành vi lưu cả batch trong một transaction. `size` được giới hạn tối đa 50; tham số `date` kèm `tripId` lọc theo ngày tại Việt Nam.
