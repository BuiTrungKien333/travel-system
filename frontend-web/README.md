# Frontend web

Frontend gọi Spring Boot thật qua `NEXT_PUBLIC_API_BASE_URL` (mặc định `http://localhost:8080/api/v1`). Không tự chuyển sang dữ liệu mẫu khi backend lỗi.

```powershell
cd D:\WWW_JAVA\travel-system\frontend-web
# Lần đầu: tạo .env.local từ .env.example nếu chưa có.
npm install
npm run dev
```

Mở [http://localhost:3000](http://localhost:3000). Backend cần chạy tại port 8080 và cho phép CORS từ `http://localhost:3000`. Đăng ký cần xác minh email trước khi đăng nhập; tài khoản thử local được mô tả trong [DEMO_DATA.md](../docs/DEMO_DATA.md).

Các phần đã kết nối:

- Auth: đăng nhập, đăng ký, logout, refresh token và bảo vệ các trang cá nhân.
- `/trips`: danh sách có đọc đầy đủ các trang và tóm tắt `/home/dashboard`.
- `/trips/new`: chọn danh mục tỉnh/thành từ backend, tạo trip với `clientRequestId`.
- Trip settings: cập nhật qua PATCH với `version`, kết thúc chuyến đang diễn ra và xóa có kiểm tra phiên bản.
- Itinerary: đọc lịch, chọn Ngày 1/Ngày 2 rồi thêm thẳng địa điểm bằng API batch có ngày; kéo thả, đánh dấu đã ghé, preview và apply kết quả tối ưu. Trang trip mặc định mở Lên lịch trình, sidebar gồm Lên lịch trình, Chỉ đường, Kỷ niệm và Cài đặt. Thời tiết là phần tham khảo; lỗi provider không chặn lịch trình.
- Memories: tạo metadata trước, upload ảnh qua `/media/uploads`, hiển thị kết quả lỗi một phần và giữ ID cho lần thử lại; xem ảnh và xóa kỷ niệm.
- `/journal` và `/memory`: cùng dùng dữ liệu thật từ trips, journal memories, place details, destinations và dashboard. Kỷ niệm không thuộc trip vẫn hiển thị, việc bỏ địa điểm khỏi trip không xóa lịch sử.
- Account: đọc/cập nhật `/users/me`.

Journal hiện tổng hợp các API hiện có tại frontend; số request tăng theo số địa điểm có kỷ niệm. Travel guides và nội dung marketing vẫn là nội dung tĩnh.

## Kiểm tra

```powershell
npm run type-check
npm run test:auth
npm run test:api
npm run build
```

Build dùng `.next-production`, dev dùng `.next-dev` để không ghi đè tài nguyên của nhau. Chạy bản build bằng `npm start`.

Kiểm tra HTTP thật với tài khoản thử: đặt `API_TEST_EMAIL`, `API_TEST_PASSWORD`, rồi chạy `node scripts/check-backend-contract.cjs --live`. Script tạo trip thử và dọn trong `finally`.

Kiểm tra UI: khi có Playwright và Edge, cùng hai biến trên, chạy `node scripts/smoke-backend-ui.cjs`. Có thể đặt `PLAYWRIGHT_MODULE` đến module Playwright đã cài sẵn. Script chạy trên frontend port 3000, backend port 8080; tạo/xóa dữ liệu thử. Luồng này không upload ảnh lên Cloudinary hoặc gọi tối ưu AI thật.

Tab Chỉ đường dùng vị trí hiện tại, phương tiện và lịch đã lưu để gọi `/navigation/routes`; backend tính tuyến bằng Geoapify (`geoapify-routing`). Frontend vẽ tuyến và hiển thị chỉ dẫn do Geoapify cung cấp; không chuyển hướng sang Google Maps. Mapbox chỉ là lớp hiển thị bản đồ. Dịch vụ nội bộ cần được cấu hình/bật theo `backend/docs/NAVIGATION_API.md`; frontend hiển thị lỗi khi backend chưa sẵn sàng.

Trạng thái trên nhánh `feature/web` ngày 25/11/2026: giao diện và lời gọi `/api/v1/navigation/routes` đã có, nhưng backend đi kèm hiện chỉ cung cấp API cũ `/api/v1/routes`. Cần tích hợp Navigation API đúng contract trước khi nghiệm thu chỉ đường xuyên suốt. Các tài liệu kiểm tra navigation cũ ghi nhận môi trường trước đó, không xác nhận backend hiện tại có endpoint này. Đặt `NEXT_PUBLIC_MAPBOX_ACCESS_TOKEN` trong `.env.local` để hiển thị bản đồ.

Thẻ gợi ý hiển thị `photo` từ API; catalog hiện trả `photo: null`, nên địa điểm thiếu ảnh có khung “Chưa có ảnh”. Không gán ảnh địa điểm ngẫu nhiên.
