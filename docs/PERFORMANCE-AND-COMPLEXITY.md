# Đánh giá hiệu năng và độ phức tạp auth

## Kết luận

Thiết kế hiện tại có thể dùng làm nền tảng MVP. Source không đủ để khẳng định “hiệu năng cao trong production”: chưa đo thời gian BCrypt, thời gian chờ khóa/connection pool, độ trễ DB, cold JWKS, SMTP hay native Google trên thiết bị. Audit không chạy lại test/build hoặc tạo tải.

Ưu tiên giảm thời gian giữ transaction/khóa, giảm công việc housekeeping trong request và làm email đáng tin cậy. Chưa có cơ sở thêm Redis, message broker, microservice auth hoặc bỏ kiểm tra bảo mật để tiết kiệm vài query.

## Chi phí theo đường thành công

Đây là ước lượng số thao tác SQL từ source, không phải số đo SQL Hibernate. Chưa tính BEGIN/COMMIT, flush phát sinh, retry, cache, provider HTTP và query của endpoint nghiệp vụ.

| Đường | SQL logic ước lượng | Chi phí khác |
|---|---:|---|
| Login mật khẩu | 3 | SELECT khóa user, SELECT permissions, INSERT refresh; một BCrypt |
| Refresh | 4 | SELECT khóa user, DELETE consume, SELECT permissions, INSERT refresh |
| Register | Khoảng 5 | Kiểm tra tồn tại, INSERT user, tìm/revoke/INSERT action; một BCrypt; gửi mail sau commit |
| Google challenge | 3 | Hai DELETE hết hạn và INSERT challenge |
| Google login tài khoản đã liên kết | 5, cộng 3 của challenge | Đọc/consume challenge, tìm user theo sub, permissions, refresh; Google SDK và có thể lấy JWKS |
| API được bảo vệ | +1 mỗi request | Auth-state projection theo primary key trước xử lý endpoint |
| GET own profile | Khoảng 2 tổng cộng | Auth-state SELECT + User SELECT |

Nguồn: [AuthServiceImpl](E:/travel-system/backend/src/main/java/com/travel/auth/service/impl/AuthServiceImpl.java:79), [AuthTokenIssuer](E:/travel-system/backend/src/main/java/com/travel/auth/service/AuthTokenIssuer.java:25), [GoogleAuthService](E:/travel-system/backend/src/main/java/com/travel/auth/service/GoogleAuthService.java:49), [JWT filter](E:/travel-system/backend/src/main/java/com/travel/security/filter/JwtAuthenticationFilter.java:75).

### 1. BCrypt giữ khóa user và connection DB

Login mở transaction, `lockByEmail`, rồi mới `passwordEncoder.matches`. Mật khẩu sai vẫn giữ khóa trong thời gian BCrypt. Các login cùng tài khoản, reset và refresh phải chờ nhau. Register cũng làm BCrypt trong transaction sau query kiểm tra email; reset hash sau khi đã khóa user.

Đây là lựa chọn chống race có lợi cho tính đúng, nhưng critical section dài hơn cần thiết. Cách cải thiện có giới hạn: đọc snapshot/hash; chạy BCrypt ngoài transaction; vào transaction ngắn, khóa user và kiểm lại hash, trạng thái, sessionVersion trước cấp token. Có thể thêm một SELECT nhưng giảm thời gian chiếm pool và khóa. Nếu snapshot đã thay đổi, từ chối/retry có giới hạn. Không chỉ xóa khóa hoặc đưa BCrypt ra ngoài mà bỏ bước kiểm lại.

Không hạ BCrypt cost chỉ để API trông nhanh. Cần đo trên máy deploy và quyết định cost theo bảo mật/khả năng phục vụ. Source hiện dùng BCrypt cost mặc định và có dummy hash cho email không tồn tại; phần dummy hash là phòng chống timing oracle có lý do, không nên bỏ để tối ưu nhánh lỗi.

### 2. Cleanup nằm trong Google challenge

Mỗi challenge xóa toàn bộ challenge/link ticket hết hạn trước khi INSERT. Có index expiry, nhưng lượng backlog quyết định công việc của request. Chuyển housekeeping sang job định kỳ xóa theo batch giới hạn. Kiểm tra expiry và consume nguyên tử vẫn phải giữ trong đường xác thực.

Refresh/action token hết hạn chưa có cleanup tổng quát; thời hạn chặn sử dụng token, không tự xóa row. Thêm job retention nhỏ dùng DB hiện có. Không cần Redis chỉ để xử lý việc này.

### 3. Một query kiểm tra auth state mỗi API

Query này cho phép khóa/xóa account và reset mật khẩu vô hiệu hóa access token cũ ngay khi request mới được kiểm tra. Token vẫn là JWT, nhưng cách xác thực có state DB; gọi nó “hoàn toàn stateless” sẽ không đúng.

Với MVP, một projection theo primary key là đánh đổi hợp lý cho thu hồi phiên. Không truy vấn permission database mỗi API; permission query xảy ra khi cấp token. Chỉ xem xét cache ngắn hoặc session store khi đo được DB là bottleneck, và phải chấp nhận/thiết kế độ trễ revocation. Không bỏ query rồi vẫn hứa reset có hiệu lực tức thì.

### 4. Google challenge thêm một lượt HTTP

Nonce/challenge lưu DB và consume một lần ngăn replay tốt hơn chỉ kiểm chữ ký/audience của ID token. Đổi lại là endpoint challenge, table, housekeeping, một lượt mạng và khoảng 3 SQL trước login. Google ID token hợp lệ có thể được backend xác minh theo hướng dẫn cơ bản mà không dùng table challenge riêng; challenge này là lựa chọn tăng bảo vệ, không phải điều kiện của mọi ứng dụng Google login.

Có thể cân nhắc stateless signed challenge hoặc flow cơ bản khi yêu cầu thay đổi, nhưng mỗi phương án có tính chất replay khác. Hiện không cần refactor giao thức: sửa cleanup và đo round trip trước. Native auto-select/fallback đã giảm thao tác; iOS interactive login hiện cần token mang nonce mới nên không tự thay bằng silent cached token.

### 5. UX mobile

Refresh single-flight, storage queue và generation checks là các cơ chế nhỏ, xử lý race có tác động thật. Giữ chúng. Storage queue không tăng dần số token trong memory theo lượng user; giới hạn mail queue 100 cũng tránh buffer vô hạn. Không có số đo cho các kích thước heap/native SDK, nên không khẳng định chúng miễn phí hoặc quá tốn RAM.

Login/refresh đã trả user summary, nhưng `onUser` đặt profile cache `updatedAt: 0`, làm lần xem profile tiếp theo fetch `/me`. Người dùng có dữ liệu hiển thị ngay; request bổ sung để lấy thông tin profile đầy đủ/fresh. Có thể dùng cache riêng cho summary hoặc thời gian fresh ngắn nếu đã thống nhất contract; không ép kiểu summary thành full profile chỉ để bỏ request.

Web Google button xin challenge ngay khi mount, xin lại trước expiry và khi đổi ngôn ngữ. Prefetch giảm độ trễ khi nhấn Google nhưng tạo request cả khi người dùng chọn password/để trang mở. Chỉ renew khi trang active/cần đăng nhập; nếu web chưa là sản phẩm phát hành, có thể hoãn tối ưu flow web. Native không có vòng renew idle tương tự.

Rủi ro pending response vượt qua account replacement được ghi riêng trong NEEDS-VALIDATION; generation hiện bảo vệ credential và nhánh lỗi, chưa bảo vệ mọi response thành công. Nên kiểm chứng trước khi thêm các query chứa lịch trình/dữ liệu riêng tư.

## Những phần nên giữ và có thể đơn giản hóa

| Phần | Đánh giá |
|---|---|
| Hash action/refresh token, TTL, atomic consume | Cần thiết; DB bị lộ không có ngay bearer token, replay bị chặn |
| SessionVersion + reset revoke | Cần thiết cho recovery; thêm state có mục đích rõ |
| Unique email/Google sub + bounded transaction retry | Hợp lý; race hiếm nhưng hậu quả liên kết nhầm identity có thể lớn |
| Google verify/JWKS ngoài write transaction | Hợp lý, tránh giữ DB trong lúc chờ mạng |
| Gmail/Workspace auto-link; third-party email cần mailbox proof | Có lý do bảo mật; Google không luôn chứng thực quyền sở hữu hiện tại của email ngoài Gmail/Workspace ([Google](https://developers.google.com/identity/sign-in/android/backend-auth?hl=en)) |
| Tách controller/service/repository/issuer/codec | Phù hợp Spring; số class không tự chứng minh overengineering |
| Cleanup toàn bộ trong challenge + renew web idle | Công việc có thể đưa ra khỏi đường login/hạn chế |
| Thêm Redis/Kafka/auth microservice/full device session ngay | Chưa có dữ liệu tải hay yêu cầu để biện minh |
| Immediate logout server | Quyết định sản phẩm riêng; hiện refresh bị revoke, access có thể còn 30 phút. Không tự gọi đây là bypass |

Refresh rotation hiện tại xóa token cũ và từ chối reuse, nhưng không giữ quan hệ token family để thu hồi token mới khi phát hiện token cũ bị replay. Nếu kẻ khác đã lấy được refresh token và rotate trước, lần sử dụng token cũ của client thật không tự thu hồi refresh mới của kẻ đó. Đây là giới hạn phòng vệ sau khi bearer credential đã bị lộ, chưa phải bằng chứng code làm lộ token. Hướng dẫn OAuth mô tả rotation kèm giữ quan hệ và thu hồi token đang active khi reuse ([RFC 9700, §4.14.2](https://www.rfc-editor.org/rfc/rfc9700.html#section-4.14.2)). Hệ thống hiện dùng phiên JWT riêng, không được mặc định gọi là OAuth authorization server đạt chuẩn. Cân nhắc family/session record khi yêu cầu chống credential theft hoặc quản lý từng thiết bị rõ ràng; không thêm DPoP/mTLS chỉ vì một checklist.

## Điều cần đo trước khi cam kết production performance

Trên môi trường cô lập phù hợp, dùng tài khoản giả và provider giả để đo p50/p95/p99, thời gian BCrypt, SQL/transaction, chờ khóa, chờ pool và request lỗi. Kiểm chứng tối thiểu concurrency login/reset/refresh bằng lịch thực thi có kiểm soát; không cần tạo tải lớn để xác nhận race. Sau đó operator đo release build trên thiết bị thật và môi trường staging của chính mình: Google warm/cold, restore, mất mạng, SecureStore lỗi và SMTP lỗi. Mọi thao tác đó nằm ngoài kết quả đã thực hiện của audit này.
