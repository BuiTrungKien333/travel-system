# WeGo V1 — Cài đặt và demo

Bản bàn giao: Android APK cài trực tiếp + web dự phòng. Backend chạy trên laptop, điện thoại cùng Wi-Fi. Đây là bản demo nội bộ có HTTP LAN và chữ ký debug, không phải bản phát hành Play Store.

## 1. Khởi động nhanh mỗi ngày

1. Đánh thức laptop, cắm sạc, mở Docker Desktop và chờ Docker sẵn sàng.
2. Kết nối laptop và điện thoại vào cùng Wi-Fi. Tắt VPN nếu VPN làm thay đổi tuyến mạng.
3. Mở PowerShell, chạy:

```powershell
cd E:\travel-system
powershell -ExecutionPolicy Bypass -File .\scripts\Start-WeGo-Demo.ps1
```

Script tự tìm IP Wi-Fi, chạy PostgreSQL, khởi động backend từ JAR đã build và web dự phòng. Nếu backend WeGo cũ còn chạy, script khởi động lại với IP mới để cập nhật CORS và đường dẫn email. Không ghi đè file .env và không in secret. Chờ dòng Started TravelApplication trước khi kết nối app. Giữ cửa sổ này mở; Ctrl+C để dừng backend.

Nếu chọn nhầm card mạng:

```powershell
.\scripts\Start-WeGo-Demo.ps1 -LanIp 192.168.1.20
```

IP trên chỉ là ví dụ. Dùng IP thực tế mà máy bạn nhận được.

4. Nếu app đang báo lỗi khôi phục phiên do IP cũ, chọn **Kết nối máy chủ** ngay trên màn thử lại. Trên APK, tại màn đăng nhập chọn **Kết nối máy chủ**. Nếu đang đăng nhập: Cá nhân → biểu tượng cài đặt → Kết nối máy chủ. Nhập chính xác URL script in ra, ví dụ `http://192.168.1.20:18081`, rồi **Kiểm tra & kết nối**. Sau khi đổi máy chủ cần đăng nhập lại. Cấu hình được giữ sau khi đóng app; không cần build lại khi đổi Wi-Fi.
5. Web dự phòng trên laptop: `http://localhost:8082`. Chọn Kết nối máy chủ và nhập URL API mà script hiển thị nếu địa chỉ cũ không còn đúng. Web không lưu refresh token vào localStorage; reload tab có thể yêu cầu đăng nhập lại.
6. Kiểm tra nhanh backend:

```powershell
Invoke-RestMethod http://localhost:18081/actuator/health
```

Kết quả cần có `status: UP`. Trên trình duyệt điện thoại, mở `http://<IP-laptop>:18081/actuator/health` để kiểm tra đường mạng.

## 2. Nếu điện thoại không kết nối được

- Không dùng localhost hoặc 10.0.2.2 trên điện thoại thật; dùng IP Wi-Fi của laptop.
- Xác nhận backend đang chạy và đúng cổng 18081.
- Với Wi-Fi đáng tin cậy, đặt profile Windows là Private. Nếu Windows hỏi cho Java truy cập mạng, cho phép trên Private network.
- Nếu cần, mở PowerShell **Run as administrator**, thêm rule một lần:

```powershell
if (-not (Get-NetFirewallRule -DisplayName 'WeGo Demo LAN' -ErrorAction SilentlyContinue)) {
  New-NetFirewallRule -DisplayName 'WeGo Demo LAN' -Direction Inbound -Action Allow -Protocol TCP -LocalPort 18081,8082 -Profile Private
}
```

- Wi-Fi trường học/quán cà phê có thể chặn thiết bị nhìn thấy nhau. Khi đó dùng hotspot riêng để laptop và điện thoại liên lạc được. Internet vẫn cần cho Mapbox, thời tiết, ảnh online và upload cloud.
- App/backend ngừng liên lạc khi laptop sleep. Đánh thức laptop rồi chạy lại script trước khi demo.

## 3. Cài APK

- Chép APK mới sang điện thoại, cho phép cài ứng dụng từ nguồn bạn dùng để mở file, chọn Cập nhật/Cài đặt.
- Ưu tiên cài đè để giữ ảnh Xem sau và phiên đăng nhập. Không gỡ app, không xóa dữ liệu khi còn ảnh Xem sau chưa đồng bộ.
- Bản này tự chạy, không cần Metro/Expo Go hoặc cáp USB. Camera cần cấp quyền khi được hỏi. Bản đồ thật hỗ trợ kéo và pinch zoom.
- Nếu Android báo chữ ký không khớp, sao lưu/đưa các ảnh Xem sau vào chuyến đi trước khi cân nhắc gỡ bản cũ. Không gỡ tùy tiện để xử lý lỗi này.
- Ảnh tải/chụp được lưu vào thư viện thiết bị, tên file bắt đầu `WeGo-`. Trên máy ảo Android đã kiểm tra, file nằm trong `DCIM`; tìm trong mục ảnh gần đây của ứng dụng Thư viện/Google Photos. Ảnh Xem sau đồng thời có bản lưu riêng của ứng dụng.

## 4. Dữ liệu mẫu

Dữ liệu demo đã được nạp vào PostgreSQL dev: 50 tài khoản, 50 địa điểm, 50 chuyến đi, 50 nhật ký và 100 ảnh minh họa. UUID bắt đầu d31…d39 là vùng dữ liệu demo; script không xóa dữ liệu người dùng thật.

Mật khẩu các tài khoản demo: `DemoTravel!2026`.

| Tài khoản | Mục đích |
|---|---|
| demo.local@example.com | Bộ demo chính: có chuyến đang đi, sắp đi, kế hoạch và chuyến đã hoàn thành |
| demo.user50@example.com | Người mới, chưa có chuyến đi |
| demo.user04@example.com | Chuyến sắp khởi hành sau 3 ngày |
| demo.user02@example.com | Chuyến đã hoàn thành và kỷ niệm |

Trước buổi báo cáo, trong **PowerShell thứ hai**:

```powershell
cd E:\travel-system
powershell -ExecutionPolicy Bypass -File .\scripts\Prepare-WeGo-Demo.ps1
```

Script nạp dữ liệu theo ID cố định và cập nhật ngày cho một số chuyến demo, để màn đang đi/sắp đi vẫn đúng ngày báo cáo. Chỉ chạy trên database dev. Thao tác này đặt lại ngày/trạng thái của ba chuyến demo chuyên dụng; không chạy giữa lúc đang sửa những chuyến demo đó trên app.

Ảnh từ seed là ảnh minh họa từ nguồn public, không có định danh upload Cloudinary. Hãy kiểm thử xóa ảnh bằng ảnh mới do chính bạn chụp/upload, không dùng ảnh seed để chứng minh xóa file cloud.

## 5. Kịch bản demo khoảng 10–15 phút

1. **Khởi động & đăng nhập (1 phút):** mở app WeGo, logo trên nền trắng, đăng nhập tài khoản người mới. Home hiển thị đúng trạng thái chưa có chuyến đi; không có dữ liệu của tài khoản khác.
2. **Tạo chuyến đi (2 phút):** tạo chuyến tới Đà Nẵng, ngày bắt đầu sau 3 ngày, nhập tên và lưu. Kiểm tra danh sách chuyến đi rồi mở chi tiết. Dùng nút quay lại để kiểm tra dữ liệu vẫn còn.
3. **Địa điểm & lịch trình (3 phút):** thêm 3 địa điểm nội bộ, chọn ngày; mở bản đồ, kéo/pinch zoom. Sửa giờ xuất phát và thời lượng ghé. Kiểm tra giờ dự kiến tiếp theo tính theo thời gian di chuyển + thời lượng tham quan. Sắp xếp, mở xem trước tối ưu rồi áp dụng. Nếu đổi ngày chuyến đi làm điểm rơi ngoài ngày hợp lệ, mở Chưa xếp lịch và chọn nhiều điểm đưa vào ngày mong muốn.
4. **Sắp khởi hành (1 phút):** đăng nhập demo.user04@example.com để thấy Home Sắp khởi hành. Mở thời tiết, chọn ngày và xem dự báo theo giờ. Dự báo ngoài phạm vi nhà cung cấp phải hiện thông báo phù hợp thay vì số liệu bịa.
5. **Đang trong chuyến đi (2 phút):** đăng nhập demo.local@example.com. Mở chi tiết chuyến đang đi từ Home, xem điểm tiếp theo và bản đồ, đánh dấu đã ghé. Tối ưu chỉ phần chưa ghé; điểm đã ghé giữ ở đầu. Không cần kết thúc chuyến demo ngay ở bước này.
6. **Camera & ảnh (2 phút):** chụp một ảnh gắn vào chuyến đi hoặc upload 2–3 ảnh. Xem tiến độ, chờ đồng bộ thành công. Mở ảnh: thanh công cụ hiện sẵn; chạm để ẩn/hiện, vuốt ngang, double tap/pinch zoom, kéo xuống đóng. Thử tim đỏ, tải về thư viện và chia sẻ. Xóa một ảnh mới upload rồi xác nhận nó biến mất khỏi album.
7. **Xem sau (1 phút):** chụp nhanh chưa gắn chuyến đi, kiểm tra ảnh vào Xem sau; đóng/mở app và kiểm tra còn ảnh. Đưa ảnh vào chuyến rồi chờ upload xong. Ảnh chưa gắn chuyến đi được giữ tại máy; không tuyên bố nó đã có trên cloud.
8. **Hoàn thành & kỷ niệm (1 phút):** dùng một chuyến thử để Kết thúc chuyến đi, xác nhận rồi xem Home/chi tiết ở chế độ hoàn thành. Tab Kỷ niệm chuyển Ảnh/Album; chọn album, mở ảnh và thử lại trình xem dùng chung.
9. **Hồ sơ & lỗi mạng (1 phút):** sửa tên người dùng và tải lại. Bật chế độ máy bay/ngắt mạng ngắn để thấy thông báo lỗi/thử lại; kết nối lại, thử lại. Đăng xuất rồi đăng nhập tài khoản khác để kiểm tra không lẫn dữ liệu.

Không thử xóa tài khoản/chuyến thật trong buổi demo. Có thể tạo chuyến “Demo thử xóa” để minh họa xác nhận xóa và kết quả lưu backend.

## 6. Giới hạn V1 cần trình bày đúng

- Bản demo ưu tiên Android và web; chưa kiểm chứng trực tiếp trên iPhone.
- Thông báo đẩy chưa nằm trong MVP. Một số sở thích Cá nhân là lựa chọn local trong phiên, chưa đồng bộ backend.
- Nút tim trong trình xem ảnh có phản hồi màu đỏ; trạng thái yêu thích hiện là local trong phiên xem, chưa phải danh sách yêu thích đồng bộ tài khoản.
- Bản đồ, tuyến đường, thời tiết, SMTP/Google và upload cần đúng cấu hình provider, Internet và quota. Không có dữ liệu thì hiển thị trạng thái lỗi/trống, không coi dữ liệu mẫu là dữ liệu thực.
- API cho auth, ownership, version/conflict, trip, lịch trình và media có kiểm thử tự động; điều này không thay cho kiểm thử tải lớn hay chứng nhận production.
- Giữ nguyên các bản dependency theo quyết định ổn định V1. Expo Doctor còn cảnh báo 13 package lệch bản vá SDK 57; xử lý trong đợt nâng cấp riêng.
- Không chia sẻ file .env, secret JWT/SMTP/Cloudinary hoặc token Mapbox secret. APK chứa public token Mapbox theo thiết kế; giới hạn scope/quota tương ứng.

## 7. Khi cần build lại sau khi sửa code

APK chỉ nhận code mới khi build lại; sửa Wi-Fi không cần build. Xem script `scripts/Build-WeGo-V1.ps1` trong mobile. JAR backend được cập nhật bằng `backend/mvnw.cmd -DskipTests package`; nếu sửa backend sau bản bàn giao, hãy chạy kiểm thử phù hợp trước khi thay JAR.

## 8. Kết quả kiểm tra bản bàn giao

- APK 1.0.1, versionCode 2, package `com.travel.mobile`; arm64-v8a và x86_64; chữ ký APK hợp lệ, ZIP alignment 16 KB đạt. SHA-256 và kích thước trong README cạnh APK.
- TypeScript đạt; mobile 407/407 kiểm thử; backend 518/518 kiểm thử. Không chạy lại npm lint theo yêu cầu.
- API thật: đăng nhập, bốn trạng thái Home, gợi ý địa điểm, tạo/sửa/xóa chuyến thử, lưu lịch ngày, tuyến Mapbox và thời tiết đã được gọi kiểm tra. Upload hai ảnh khác nhau lên Cloudinary rồi xóa cloud/DB thành công; dữ liệu QA tạm được dọn.
- APK chạy độc lập khi Metro tắt: đăng nhập/khôi phục phiên, đổi máy chủ, Home người mới, form tạo chuyến, xem ảnh và toggle công cụ, tim đỏ, tải ảnh vào thư viện. Camera chụp thật trên máy ảo; Xem sau còn ảnh sau khi đóng/mở lại app.
- Bản đồ native đã hiển thị tuyến/ghim, kéo bản đồ và mở rộng khung. Chưa tự động kiểm chứng thao tác pinch hai ngón; cần thử trực tiếp trên điện thoại thật trước buổi báo cáo.
- Web được export và kiểm tra HTTP/CORS; chưa xác minh trực quan đầy đủ bằng trình duyệt vì công cụ tự động hóa trình duyệt không khả dụng trong phiên kiểm tra này.
- Máy ảo headless cần GPU `host` để Mapbox render ổn định; camera `emulated` được dùng cho QA. Đây là cấu hình máy ảo, không phải yêu cầu cho điện thoại thật.

Các kết quả trên là kiểm tra ổn định cho bản demo V1, không phải cam kết mọi luồng đã được kiểm thử trên mọi thiết bị.
