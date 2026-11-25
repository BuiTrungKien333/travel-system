# Geoapify navigation — kiểm tra local 2026-11-25

## Cấu hình và nguyên nhân NAV_001

API key có sẵn và hoạt động. `NAVIGATION_ENABLED=true`, `NAVIGATION_FREE_PLAN_CONFIRMED=true`, hạn mức dự trữ của ứng dụng là 768/24 giờ.

Giá trị local trước khi sửa: `NAVIGATION_CREDITS_PER_CALL=16`. `NavigationBudget.reserveRoutes` yêu cầu mỗi lần gọi provider phải dự trữ ít nhất `2 * legs + 10`, nên 16 chỉ cho tối đa 3 chặng. Đã tái hiện: tuyến 1 chặng HTTP 200, tuyến 4 chặng HTTP 503 / `NAV_001` với đúng thông báo người dùng báo.

Đã đổi `backend/.env` thành `NAVIGATION_CREDITS_PER_CALL=64`, giữ nguyên hạn mức 768, và khởi động lại backend tại port 8080. `.env` không đưa vào Git. `.env.example` bổ sung các biến navigation cùng giải thích giới hạn số chặng.

64 đáp ứng điều kiện dự trữ hiện tại cho tối đa 24 chặng. Mỗi lần tìm đường gọi hai ứng viên `balanced` và `short`, do đó dự trữ 128 credit của ứng dụng. Đây là sổ dự trữ nội bộ, không phải số credit thực tế trên tài khoản Geoapify. Chi phí upstream phụ thuộc số chặng và dữ liệu chi tiết được yêu cầu; xem [Routing API của Geoapify](https://apidocs.geoapify.com/docs/routing/routing/).

## Test chỉ đường thật

Các kết quả dưới đây đi qua backend local và Geoapify thật bằng key hiện có, không dùng fixture, mock hoặc simulation:

| Ca kiểm tra | HTTP | Provider | Chặng | Khoảng cách | Thời gian ước tính |
|---|---|---|---:|---:|---:|
| Quick route — ô tô | 200 | geoapify-routing | 4 | 7.429 km | 618.834 giây |
| Quick route — xe máy | 200 | geoapify-routing | 1 | 2.707 km | 210.052 giây |
| Lấy điểm từ lịch trình đã lưu — ô tô | 200 | geoapify-routing | 4 | 24.714 km | 2608.216 giây |

Collection tái sử dụng: [Navigation_Live_Smoke.postman_collection.json](../../postman/Navigation_Live_Smoke.postman_collection.json). Chạy local với Postman CLI, không xuất bản kết quả lên cloud: 4 request, 12 assertions, 0 lỗi, exit code 0. Kiểm tra provider, phương tiện, số chặng, khoảng cách/thời gian dương, geometry và chỉ số của chỉ dẫn.

Ca lấy lịch trình tạo một trip thử qua API, thêm 4 địa điểm catalog vào ngày đã chọn rồi gọi navigation với `expectedPlaceIds`. Response giữ đúng thứ tự đã lưu; trip thử đã được xóa. Token thử đã logout. Không lưu API key, token hoặc password trong collection/report.

Một lịch trình seed cũ trả `400 / COMMON_003` khi dùng trực tiếp làm dữ liệu thử; không dùng ca đó làm bằng chứng pass. Seed cũ không được chỉnh sửa trong lần kiểm tra cấu hình này.

## Hạn mức sau kiểm tra

Đọc sổ dự trữ bằng truy vấn chỉ đọc sau các ca thử: đã dự trữ **576/768**, còn **192**. Với 128 credit dự trữ mỗi lần tìm đường, còn **1** lần trong hạn mức hiện tại, trước khi các bản ghi cũ rời cửa sổ 24 giờ. Không xóa/reset sổ dự trữ hoặc nâng hạn mức.

Khi hết hạn mức, backend trả `NAV_002` / HTTP 429; trường hợp đó khác lỗi cấu hình/số chặng `NAV_001` đã sửa ở trên.
