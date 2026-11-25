# Dữ liệu demo local

Một file duy nhất: `backend/src/main/resources/db/migration/V11__seed_local_demo.sql`. V11 hiện nằm trong chuỗi migration chung và được Flyway áp dụng trên mọi database chạy chuỗi này, không còn giới hạn bởi profile `dev`. Đây là dữ liệu demo và các tài khoản dùng chung mật khẩu mẫu, không phải dữ liệu để phát hành production.

Theo quy tắc seed ở Phase 7, giữ nguyên các migration đã phát hành và không kích hoạt lại tài khoản demo cũ bị V7 vô hiệu hóa. Flyway quản lý transaction và chỉ áp dụng V11 một lần.

## Đăng nhập

- Email: `demo.local@example.com`
- Mật khẩu demo local: `DemoTravel!2026`
- Role: `USER`, đã xác minh email, không cần SMTP.
- Các tài khoản còn lại: `demo.user02@example.com` đến `demo.user50@example.com`, cùng mật khẩu `DemoTravel!2026`.

Tài khoản này chỉ để thử nghiệm ở local. Seed không tạo refresh token, email token hoặc liên kết Google.

## Bộ dữ liệu — 50 dòng cho mỗi bảng chính

| Bảng | Số dòng mẫu |
|---|---:|
| users | 50 |
| trips | 50 |
| places | 50 |
| memories | 50 |
| trip_places | 150 |
| itineraries | 38 |
| itinerary_items | 114 |
| media | 100 |

Các con số là tổng của namespace demo sau V11, không phải tổng toàn database. Dữ liệu cũ ngoài namespace vẫn được giữ. Master Province tái sử dụng 7 code lịch sử; bảng quyền và token không thêm cho đủ 50 dòng.

Trip gồm 13 COMPLETED, 13 ACTIVE, 12 READY và 12 PLANNING. Trip COMPLETED có 3 Memory; ACTIVE có thể đã ghi một vài Memory hoặc chưa ghi; READY/PLANNING chưa có Memory. Mỗi Trip có 3 Place trong cùng khu vực, mỗi Memory có 2 media. Mỗi ngày có 1 điểm đầu ngày, không tạo khoảng cách/thời gian đường đi chưa được xác minh.

Tài khoản `demo.local@example.com` giữ 4 Trip ban đầu dưới đây. 46 Trip bổ sung thuộc tài khoản demo số 02–47, giúp kiểm tra quyền sở hữu; số 48–50 chưa có Trip để kiểm tra empty state.

| Trip | Trạng thái lúc seed lần đầu | Place | Memory | Ảnh minh họa |
|---|---|---:|---:|---:|
| Đà Nẵng — Kỷ niệm mùa biển | COMPLETED, 30 ngày trước | 3 | 3 | 6 |
| Đà Lạt — Cuối tuần xanh | ACTIVE, từ 2 ngày trước đến ngày mai | 3 | 2 | 4 |
| Hà Giang — Hẹn ngày lên đường | READY, 10 ngày tới | 3 | 0 | 0 |
| Đà Nẵng — Kế hoạch trở lại | PLANNING, 30 ngày tới | 3 | 0 | 0 |

Hai Trip Đà Nẵng ban đầu dùng chung 3 Place nhưng có trạng thái visited riêng. Tài khoản chính có Memory ở Đà Nẵng và Lâm Đồng; chỉ lên lịch Hà Giang chưa có nghĩa đã khám phá. Place bổ sung trải trên 7 khu vực Đà Nẵng, Đà Lạt, Hà Giang, Sa Pa, Hội An, Phú Quốc và Huế.

Ngày được tính theo `Asia/Ho_Chi_Minh` tại lần chạy đầu. Chạy lại giữ ngày và các chỉnh sửa hiện có, không tự chuyển trạng thái Trip theo thời gian. Province được tra bằng code `DN`, `LD`, `HG`, `LC`, `QN`, `KG`, `TTH` từ master data lịch sử của dự án; seed không cập nhật dữ liệu hành chính.

Mỗi ngày có một điểm tham quan 09:00–10:30; Memory tương ứng lúc 09:30 cùng ngày. Điểm chưa đến không có Memory; Trip tương lai không có ảnh/kỷ niệm. Điểm đầu ngày có `distance_from_previous=0`, `travel_time_from_previous=0`; chưa mô hình hóa hành trình từ nơi lưu trú. Ngày giờ tạo parent luôn trước child.

Tọa độ là pin tham khảo, chưa được xác minh độ chính xác bởi Places API. Giờ chưa xác minh để NULL; lịch tuần là UNVERIFIED. URL ảnh minh họa lấy từ mock frontend, chưa upload Cloudinary và không phải ảnh đã xác minh của từng địa điểm. Tài khoản, Trip và Memory là giả lập; không gọi đây là hồ sơ/thông tin thực tế 100%. Việc thêm dữ liệu DB không tắt mock mode của frontend hoặc tạo API itinerary/Journey còn thiếu.

### Nguồn kiểm chứng địa điểm

- [Cổng du lịch Đà Nẵng — Bảo tàng Điêu khắc Chăm](https://danangfantasticity.com/en/bao-tang-lich-su-va-van-hoa/bao-tang-dieu-khac-cham-da-nang): địa chỉ 02 đường 2 Tháng 9, giờ 07:30–17:00.
- [Bảo tàng Lâm Đồng — Liên hệ](https://baotanglamdong.com.vn/contact): 04 Hùng Vương, phường Xuân Hương–Đà Lạt, giờ 07:30–17:30.
- [Hộ chiếu di sản Đà Nẵng 2026](https://danangfantasticity.com/wp-content/uploads/2026/01/Ho-Chieu-Di-san-Da-Nang.pdf): Ngũ Hành Sơn, 81 Huyền Trân Công Chúa.
- [Cổng du lịch Đà Nẵng — Mỹ Khê](https://danangfantasticity.com/ja/bien-vi/bien-my-khe) và [phường Xuân Trường–Đà Lạt — Cầu Đất](https://xuantruong.lamdong.gov.vn/chi-tiet-tin-tuc/?param=so-tra-dau-tien-cua-xu-dong-duong-5f36fd0e-e86b-4cde-b210-f570f1712f39): đối chiếu tên/khu vực.
- [Cục Du lịch Quốc gia — Hà Giang Loop](https://vietnam.travel/things-to-do/ha-giang-loop): Mã Pí Lèng, dinh họ Vương, Lũng Cú.
- Các nhóm bổ sung đối chiếu tên theo cổng du lịch quốc gia: [Sa Pa](https://vietnam.travel/places-to-go/northern-midlands-mountains/sapa/), [Huế](https://vietnam.travel/places-to-go/north-central-coast/hue/), [Phú Quốc](https://vietnam.travel/places-to-go/mekong-delta/phu-quoc/), cùng [di sản Hội An](https://hoianworldheritage.org.vn/en/news/Traditional-occupational-Village.hwh). Không seed Asia Park/Đà Nẵng Downtown đang có [thông báo tạm ngưng](https://danangfantasticity.com/en/outstanding-complexes/danang-downtown-temporarily-suspended).

Master data `HG` giữ theo dataset lịch sử của đề tài. [Nguồn tỉnh Tuyên Quang hiện tại](https://doingoai.tuyenquang.gov.vn/vi/post/danh-thuc-mo-vang-xanh-dua-du-lich-tro-thanh-nganh-kinh-te-mui-nhon?id=238345&type=NEWS) dùng bối cảnh hành chính Tuyên Quang cho những điểm này; V11 không tự đổi toàn bộ master data hoặc dữ liệu cũ của người dùng.

## Chạy

Từ thư mục `backend`, chạy/restart backend với profile dev và thông tin kết nối local trong `.env`:

```powershell
.\mvnw.cmd spring-boot:run -Dspring-boot.run.profiles=dev
```

Script chạy trong transaction của Flyway, khóa việc seed đồng thời và chỉ `INSERT ... ON CONFLICT (id) DO NOTHING`. Không có thao tác xóa, reset mật khẩu hay cập nhật đè bản ghi cũ. Các ID demo dùng prefix `d31` đến `d39`; xung đột tài khoản/quan hệ sẽ báo lỗi và rollback. Địa điểm trùng với dữ liệu cũ theo tên không bị hợp nhất tự động.

Restart không seed lại V11 và không ghi đè các chỉnh sửa sau đó. Database local từng chạy V11 bản nhỏ đã được bổ sung dữ liệu insert-only và đồng bộ checksum bằng Flyway repair theo yêu cầu giữ một file. `repair` chỉ đồng bộ metadata, không tự chạy lại SQL; không áp dụng cách này ngầm trên các database khác đã có V11 cũ.
