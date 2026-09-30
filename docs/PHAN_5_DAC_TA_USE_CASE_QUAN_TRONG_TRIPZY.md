# 5. ĐẶC TẢ CÁC USE CASE QUAN TRỌNG CỦA TRIPZY

**Phiên bản:** 1.0  
**Phạm vi:** Sản phẩm khả dụng tối thiểu (MVP)  
**Thứ tự nghiệp vụ:** Đăng ký → Đăng nhập → Tạo chuyến đi → Quản lý địa điểm → Tạo lịch trình → Tối ưu thứ tự → AI đề xuất → Check-in → Memory → Hoàn thành chuyến đi

---

## 5.1. Mục đích

Tài liệu này mô tả chi tiết 10 Use Case quan trọng tạo thành luồng nghiệp vụ cốt lõi của Tripzy. Mỗi Use Case được trình bày theo cấu trúc thống nhất, bao gồm mục tiêu, actor, điều kiện, dữ liệu, luồng xử lý, quy tắc nghiệp vụ, ngoại lệ, tiêu chí nghiệm thu và mã yêu cầu liên quan.

## 5.2. Quy ước chung

- **Luồng chính** mô tả tình huống thành công phổ biến nhất.
- **Luồng thay thế** mô tả cách xử lý hợp lệ khác với luồng chính.
- **Luồng ngoại lệ** mô tả lỗi hoặc điều kiện khiến Use Case không thể tiếp tục.
- Mọi kiểm tra quyền sở hữu và dữ liệu quan trọng phải được thực hiện tại backend, không chỉ tại giao diện.
- Kết quả thuật toán và AI chỉ được áp dụng sau khi người dùng xác nhận.
- Dữ liệu khoảng cách, tuyến đường và thời tiết phải đến từ dịch vụ chuyên biệt; AI không phải nguồn xác thực các dữ liệu này.
- Khi dịch vụ bên ngoài lỗi, hệ thống phải giữ dữ liệu người dùng đã nhập và cung cấp khả năng thử lại hoặc tiếp tục thủ công.

### 5.2.1. Trạng thái tài khoản

| Trạng thái | Ý nghĩa |
|---|---|
| PENDING_VERIFICATION | Tài khoản đã được tạo nhưng đang chờ xác minh nếu chính sách xác minh email được bật. |
| ACTIVE | Tài khoản hợp lệ và được phép đăng nhập. |
| LOCKED | Tài khoản bị Admin khóa và không được phép đăng nhập. |
| DISABLED | Tài khoản đã được người dùng vô hiệu hóa hoặc ngừng hoạt động theo chính sách. |

### 5.2.2. Trạng thái chuyến đi

| Trạng thái | Ý nghĩa |
|---|---|
| DRAFT | Chuyến đi đang được tạo và chỉnh sửa. |
| PLANNED | Chuyến đi đã có kế hoạch và sẵn sàng thực hiện. |
| ONGOING | Chuyến đi đang diễn ra. |
| COMPLETED | Chuyến đi đã kết thúc. |
| CANCELLED | Chuyến đi đã bị hủy. |

---

## 5.3. UC-AUTH-01 — Đăng ký tài khoản

### 5.3.1. Thông tin chung

| Thuộc tính | Nội dung |
|---|---|
| Mục tiêu | Cho phép Guest tạo tài khoản Tripzy hợp lệ. |
| Actor chính | Guest |
| Actor hỗ trợ | Email Service, nếu bật xác minh email |
| Trigger | Guest chọn chức năng **Đăng ký**. |
| Tiền điều kiện | Guest chưa đăng nhập; hệ thống đang hoạt động. |
| Hậu điều kiện thành công | Tài khoản được tạo ở trạng thái ACTIVE hoặc PENDING_VERIFICATION; mật khẩu được băm an toàn. |
| Hậu điều kiện thất bại | Không tạo tài khoản mới; dữ liệu hợp lệ đã nhập được giữ trên giao diện khi phù hợp. |
| Kênh | Web/Mobile |
| Mức ưu tiên | Must |
| Tần suất | Thấp đối với mỗi cá nhân, cao ở giai đoạn thu hút người dùng mới. |

### 5.3.2. Dữ liệu vào và đầu ra

| Loại | Dữ liệu |
|---|---|
| Đầu vào bắt buộc | Tên hiển thị, email, mật khẩu, xác nhận mật khẩu, chấp thuận điều khoản bắt buộc nếu có. |
| Đầu vào tùy chọn | Ảnh đại diện và sở thích được bổ sung sau khi đăng ký. |
| Đầu ra thành công | Định danh tài khoản, trạng thái tài khoản và hướng dẫn đăng nhập hoặc xác minh email. |
| Đầu ra thất bại | Mã lỗi và thông báo phù hợp cho trường dữ liệu không hợp lệ, email đã tồn tại hoặc lỗi hệ thống. |

### 5.3.3. Luồng chính

| Bước | Actor/Hệ thống | Mô tả |
|---:|---|---|
| 1 | Guest | Mở màn hình đăng ký. |
| 2 | Hệ thống | Hiển thị biểu mẫu và yêu cầu dữ liệu bắt buộc. |
| 3 | Guest | Nhập thông tin và gửi yêu cầu đăng ký. |
| 4 | Hệ thống | Chuẩn hóa email, kiểm tra trường bắt buộc, định dạng email và chính sách mật khẩu. |
| 5 | Hệ thống | Kiểm tra email chưa được sử dụng bởi tài khoản khác. |
| 6 | Hệ thống | Băm mật khẩu bằng cơ chế an toàn và tạo tài khoản. |
| 7 | Hệ thống | Gán trạng thái ACTIVE nếu không yêu cầu xác minh email. |
| 8 | Hệ thống | Trả kết quả thành công và hướng dẫn Guest đăng nhập. |

### 5.3.4. Luồng thay thế

- **A1 – Có xác minh email:** tại bước 7, hệ thống tạo tài khoản ở trạng thái PENDING_VERIFICATION, tạo mã xác minh có thời hạn và yêu cầu Email Service gửi liên kết. Sau khi xác minh hợp lệ, tài khoản chuyển sang ACTIVE.
- **A2 – Email đã được nhập trước đó nhưng chưa xác minh:** hệ thống không tạo tài khoản trùng; có thể cho phép gửi lại email xác minh theo giới hạn tần suất.
- **A3 – Guest quay lại chỉnh sửa:** nếu kiểm tra phía giao diện phát hiện lỗi, biểu mẫu đánh dấu đúng trường và giữ các dữ liệu không nhạy cảm đã nhập.

### 5.3.5. Luồng ngoại lệ

- **E1 – Email sai định dạng hoặc thiếu trường:** từ chối yêu cầu và thông báo trường cần sửa.
- **E2 – Mật khẩu không đạt chính sách:** từ chối yêu cầu và hiển thị các điều kiện chưa đáp ứng.
- **E3 – Email đã tồn tại:** không tạo tài khoản; hiển thị thông báo phù hợp mà không làm lộ thông tin không cần thiết.
- **E4 – Email Service lỗi:** nếu xác minh là bắt buộc, giữ tài khoản PENDING_VERIFICATION và cho phép gửi lại; nếu không bắt buộc, không làm thất bại việc tạo tài khoản.
- **E5 – Lỗi cơ sở dữ liệu:** giao dịch được hoàn tác và không để lại tài khoản tạo dở.
- **E6 – Vượt giới hạn tần suất:** từ chối tạm thời và thông báo thời điểm hoặc điều kiện có thể thử lại.

### 5.3.6. Quy tắc nghiệp vụ

- Email phải được chuẩn hóa trước khi kiểm tra duy nhất.
- Mỗi email chỉ liên kết với một tài khoản.
- Mật khẩu không được lưu hoặc ghi log ở dạng thuần.
- Mã xác minh, nếu có, phải có thời hạn và chỉ sử dụng một lần.
- Hệ thống không tự đăng nhập nếu chính sách yêu cầu xác minh email trước.

### 5.3.7. Truy vết

- **FR:** FR-AUTH-01, FR-AUTH-02.
- **NFR:** NFR-SEC-02, NFR-SEC-05, NFR-SEC-06, NFR-SEC-08, NFR-PRIV-07, NFR-USE-02, NFR-USE-03.

### 5.3.8. Tiêu chí nghiệm thu

- **AC1:** Given email chưa tồn tại và dữ liệu hợp lệ, When Guest gửi đăng ký, Then hệ thống tạo đúng một tài khoản và không lưu mật khẩu dạng thuần.
- **AC2:** Given email đã tồn tại, When Guest đăng ký lại, Then hệ thống không tạo tài khoản thứ hai.
- **AC3:** Given mật khẩu không đạt chính sách, When Guest gửi biểu mẫu, Then hệ thống từ chối và chỉ rõ điều kiện chưa đạt.
- **AC4:** Given xác minh email được bật, When đăng ký thành công, Then tài khoản chưa được đăng nhập trước khi xác minh hợp lệ.
- **AC5:** Given cùng một yêu cầu bị gửi lặp, When backend xử lý, Then không tạo nhiều tài khoản cho cùng email.

---

## 5.4. UC-AUTH-02 — Đăng nhập

### 5.4.1. Thông tin chung

| Thuộc tính | Nội dung |
|---|---|
| Mục tiêu | Xác thực Guest hoặc Admin và tạo phiên truy cập đúng vai trò. |
| Actor chính | Guest, Admin |
| Trigger | Actor gửi email và mật khẩu tại màn hình đăng nhập. |
| Tiền điều kiện | Có tài khoản đã đăng ký; actor chưa có phiên hợp lệ trên giao diện hiện tại. |
| Hậu điều kiện thành công | Phiên hoặc cặp mã truy cập hợp lệ được tạo; actor được điều hướng theo vai trò. |
| Hậu điều kiện thất bại | Không tạo phiên; lần thất bại được xử lý theo chính sách bảo mật. |
| Kênh | Web/Mobile; Admin sử dụng Web. |
| Mức ưu tiên | Must |
| Tần suất | Cao |

### 5.4.2. Dữ liệu vào và đầu ra

| Loại | Dữ liệu |
|---|---|
| Đầu vào | Email, mật khẩu và thông tin thiết bị cần thiết cho quản lý phiên. |
| Đầu ra thành công | Access token hoặc session, thông tin hết hạn, vai trò và thông tin hồ sơ tối thiểu. |
| Đầu ra thất bại | Mã lỗi chuẩn hóa; thông báo không tiết lộ email hay mật khẩu nào không chính xác. |

### 5.4.3. Luồng chính

| Bước | Actor/Hệ thống | Mô tả |
|---:|---|---|
| 1 | Actor | Mở màn hình đăng nhập. |
| 2 | Actor | Nhập email, mật khẩu và gửi yêu cầu. |
| 3 | Hệ thống | Chuẩn hóa email, kiểm tra định dạng và giới hạn tần suất. |
| 4 | Hệ thống | Tìm tài khoản và so khớp mật khẩu với giá trị băm. |
| 5 | Hệ thống | Kiểm tra trạng thái tài khoản là ACTIVE. |
| 6 | Hệ thống | Tạo phiên hoặc mã truy cập có thời hạn với vai trò phù hợp. |
| 7 | Hệ thống | Ghi nhận sự kiện đăng nhập thành công ở mức cần thiết. |
| 8 | Hệ thống | Trả kết quả và điều hướng User vào ứng dụng hoặc Admin vào khu vực quản trị. |

### 5.4.4. Luồng thay thế

- **A1 – Phiên hiện tại còn hiệu lực:** hệ thống có thể điều hướng trực tiếp đến màn hình phù hợp thay vì yêu cầu đăng nhập lại.
- **A2 – Access token hết hạn nhưng refresh token hợp lệ:** hệ thống cấp lại access token theo chính sách phiên mà không yêu cầu nhập lại mật khẩu.
- **A3 – Tài khoản cần xác minh:** hệ thống thông báo trạng thái và cho phép gửi lại email xác minh nếu chính sách cho phép.

### 5.4.5. Luồng ngoại lệ

- **E1 – Thông tin đăng nhập không hợp lệ:** từ chối và hiển thị thông báo chung.
- **E2 – Tài khoản LOCKED:** từ chối đăng nhập và hướng dẫn liên hệ hoặc thực hiện quy trình hỗ trợ phù hợp.
- **E3 – Tài khoản DISABLED:** từ chối đăng nhập và thông báo trạng thái tài khoản.
- **E4 – Vượt giới hạn tần suất:** tạm thời từ chối yêu cầu và không tiếp tục so khớp thông tin.
- **E5 – Lỗi hệ thống:** không tạo phiên; trả lỗi chung và mã tương quan phục vụ truy vết.

### 5.4.6. Quy tắc nghiệp vụ

- Thông báo đăng nhập thất bại không được xác nhận riêng email có tồn tại hay không.
- User không được nhận quyền Admin từ dữ liệu do client tự gửi.
- Tài khoản LOCKED, DISABLED hoặc chưa xác minh bắt buộc không được tạo phiên sử dụng.
- Phiên phải có thời hạn và có thể bị thu hồi theo chính sách.

### 5.4.7. Truy vết

- **FR:** FR-AUTH-03, FR-AUTH-04, FR-AUTH-05, FR-ADMIN-01.
- **NFR:** NFR-SEC-01, NFR-SEC-03, NFR-SEC-04, NFR-SEC-06, NFR-SEC-09, NFR-SEC-13, NFR-SEC-14.

### 5.4.8. Tiêu chí nghiệm thu

- **AC1:** Given tài khoản ACTIVE và mật khẩu đúng, When đăng nhập, Then hệ thống tạo phiên đúng vai trò.
- **AC2:** Given mật khẩu sai, When đăng nhập, Then hệ thống không tạo phiên và trả thông báo chung.
- **AC3:** Given tài khoản LOCKED, When thông tin đăng nhập đúng, Then hệ thống vẫn từ chối truy cập.
- **AC4:** Given User thay vai trò trong request phía client, When backend xử lý, Then quyền thực tế vẫn lấy từ dữ liệu tin cậy phía server.
- **AC5:** Given yêu cầu vượt giới hạn, When đăng nhập tiếp, Then hệ thống từ chối tạm thời theo chính sách.

---

## 5.5. UC-TRIP-01 — Tạo chuyến đi

### 5.5.1. Thông tin chung

| Thuộc tính | Nội dung |
|---|---|
| Mục tiêu | Cho phép User khởi tạo một chuyến đi cá nhân làm cơ sở quản lý địa điểm và lịch trình. |
| Actor chính | User |
| Trigger | User chọn **Tạo chuyến đi mới**. |
| Tiền điều kiện | User đã đăng nhập; tài khoản ACTIVE. |
| Hậu điều kiện thành công | Một chuyến đi DRAFT thuộc User được tạo với định danh duy nhất. |
| Hậu điều kiện thất bại | Không tạo chuyến đi hoặc bản ghi liên quan ở trạng thái dở dang. |
| Kênh | Web/Mobile |
| Mức ưu tiên | Must |
| Tần suất | Trung bình |

### 5.5.2. Dữ liệu vào và đầu ra

| Loại | Dữ liệu |
|---|---|
| Bắt buộc | Tên chuyến đi, điểm đến chính, ngày bắt đầu, ngày kết thúc. |
| Tùy chọn | Phương tiện, ngân sách dự kiến, sở thích áp dụng cho chuyến đi, ghi chú và yêu cầu cá nhân. |
| Đầu ra | Chuyến đi DRAFT, định danh, thời gian tạo và thông tin vừa lưu. |

### 5.5.3. Luồng chính

| Bước | Actor/Hệ thống | Mô tả |
|---:|---|---|
| 1 | User | Chọn tạo chuyến đi mới. |
| 2 | Hệ thống | Hiển thị biểu mẫu và các tùy chọn hiện có từ hồ sơ cá nhân. |
| 3 | User | Nhập thông tin bắt buộc và các thông tin tùy chọn. |
| 4 | Hệ thống | Kiểm tra định dạng, ngày bắt đầu, ngày kết thúc, ngân sách và các tham chiếu danh mục. |
| 5 | User | Xác nhận tạo chuyến đi. |
| 6 | Hệ thống | Tạo chuyến đi với User là chủ sở hữu và trạng thái DRAFT. |
| 7 | Hệ thống | Ghi nhận thời gian, múi giờ và dữ liệu audit cần thiết. |
| 8 | Hệ thống | Hiển thị trang chi tiết và gợi ý bước tiếp theo là thêm địa điểm. |

### 5.5.4. Luồng thay thế

- **A1 – Dùng sở thích hồ sơ:** hệ thống điền sẵn sở thích và phương tiện thường dùng; User có thể thay đổi riêng cho chuyến đi.
- **A2 – Lưu thông tin tối thiểu:** User chỉ nhập trường bắt buộc, tạo DRAFT và bổ sung chi tiết sau.
- **A3 – Điểm đến chính từ kết quả tìm kiếm:** hệ thống lưu tham chiếu và tọa độ từ Map & Place Service khi User chọn một địa điểm xác định.

### 5.5.5. Luồng ngoại lệ

- **E1 – Ngày kết thúc trước ngày bắt đầu:** từ chối và yêu cầu sửa khoảng thời gian.
- **E2 – Điểm đến hoặc danh mục không hợp lệ:** từ chối tham chiếu không tồn tại hoặc đã vô hiệu hóa.
- **E3 – User mất quyền truy cập:** hủy xử lý và yêu cầu đăng nhập lại.
- **E4 – Yêu cầu bị gửi lặp:** hệ thống chỉ tạo một chuyến đi khi hai yêu cầu thuộc cùng thao tác tạo.
- **E5 – Lỗi lưu dữ liệu:** giao dịch được hoàn tác và biểu mẫu giữ dữ liệu hợp lệ để User thử lại.

### 5.5.6. Quy tắc nghiệp vụ

- Ngày kết thúc phải bằng hoặc sau ngày bắt đầu.
- Chuyến đi mới luôn bắt đầu ở trạng thái DRAFT.
- Sở thích của chuyến đi là bản cấu hình riêng; cập nhật hồ sơ sau này không tự động thay đổi chuyến đi đã tạo.
- Ngân sách, nếu nhập, phải không âm và có đơn vị tiền tệ xác định.
- Chuyến đi phải có đúng một chủ sở hữu trong MVP.

### 5.5.7. Truy vết

- **FR:** FR-TRIP-01, FR-TRIP-02, FR-TRIP-03, FR-TRIP-07, FR-TRIP-10.
- **NFR:** NFR-PERF-01, NFR-REL-02, NFR-REL-03, NFR-SEC-04, NFR-DATA-01, NFR-DATA-02, NFR-USE-02.

### 5.5.8. Tiêu chí nghiệm thu

- **AC1:** Given User đã đăng nhập và dữ liệu hợp lệ, When xác nhận tạo, Then một chuyến đi DRAFT thuộc User được tạo.
- **AC2:** Given ngày kết thúc trước ngày bắt đầu, When gửi biểu mẫu, Then hệ thống không tạo chuyến đi.
- **AC3:** Given User chỉ nhập trường bắt buộc, When tạo chuyến đi, Then hệ thống vẫn tạo DRAFT hợp lệ.
- **AC4:** Given định danh chuyến đi thuộc User A, When User B yêu cầu truy cập, Then backend từ chối.
- **AC5:** Given thao tác tạo được gửi lặp do mạng, When backend nhận lại, Then không tạo bản ghi trùng ngoài ý muốn.

---

## 5.6. UC-TRIP-05 — Quản lý danh sách địa điểm của chuyến đi

### 5.6.1. Thông tin chung

| Thuộc tính | Nội dung |
|---|---|
| Mục tiêu | Cho phép User tìm kiếm, thêm, xem và loại bỏ các địa điểm muốn khám phá trong một chuyến đi. |
| Actor chính | User |
| Actor hỗ trợ | Map & Place Service |
| Use Case liên quan | UC-PLACE-01, UC-PLACE-02, UC-PLACE-03 |
| Trigger | User mở mục **Địa điểm** của chuyến đi. |
| Tiền điều kiện | User đã đăng nhập; sở hữu chuyến đi; chuyến đi chưa CANCELLED. |
| Hậu điều kiện thành công | Danh sách địa điểm của chuyến đi phản ánh đúng thay đổi được User xác nhận. |
| Hậu điều kiện thất bại | Danh sách hiện tại không bị thay đổi ngoài ý muốn. |
| Kênh | Web/Mobile |
| Mức ưu tiên | Must |

### 5.6.2. Dữ liệu vào và đầu ra

| Loại | Dữ liệu |
|---|---|
| Đầu vào tìm kiếm | Từ khóa, khu vực và bộ lọc được nhà cung cấp hỗ trợ. |
| Dữ liệu địa điểm | Định danh nhà cung cấp, tên, địa chỉ, tọa độ, loại và dữ liệu mô tả khả dụng. |
| Dữ liệu bổ sung của chuyến đi | Mức ưu tiên, thời lượng tham quan dự kiến và ghi chú riêng. |
| Đầu ra | Danh sách địa điểm thuộc chuyến đi và trạng thái thêm, cập nhật hoặc loại bỏ. |

### 5.6.3. Luồng chính

| Bước | Actor/Hệ thống | Mô tả |
|---:|---|---|
| 1 | User | Mở danh sách địa điểm của chuyến đi. |
| 2 | Hệ thống | Kiểm tra quyền sở hữu và hiển thị các địa điểm đã chọn. |
| 3 | User | Nhập từ khóa hoặc điều kiện tìm kiếm. |
| 4 | Hệ thống | Gửi yêu cầu tới Map & Place Service và chuẩn hóa kết quả. |
| 5 | Hệ thống | Hiển thị danh sách kết quả cùng thông tin nhận diện cơ bản. |
| 6 | User | Mở chi tiết và chọn một địa điểm phù hợp. |
| 7 | Hệ thống | Kiểm tra địa điểm chưa bị thêm trùng vào chuyến đi. |
| 8 | User | Nhập thời lượng dự kiến, mức ưu tiên hoặc ghi chú nếu cần. |
| 9 | User | Xác nhận thêm địa điểm. |
| 10 | Hệ thống | Lưu tham chiếu nguồn, tọa độ và thông tin cần thiết vào danh sách chuyến đi. |
| 11 | Hệ thống | Hiển thị danh sách đã cập nhật. |

### 5.6.4. Luồng thay thế

- **A1 – Cập nhật thông tin riêng của địa điểm:** User thay đổi thời lượng dự kiến, mức ưu tiên hoặc ghi chú mà không thay đổi dữ liệu gốc của nhà cung cấp.
- **A2 – Loại bỏ địa điểm chưa có trong lịch trình:** hệ thống yêu cầu xác nhận và xóa liên kết khỏi chuyến đi.
- **A3 – Loại bỏ địa điểm đã có trong lịch trình:** hệ thống thông báo các mục lịch trình bị ảnh hưởng; chỉ xóa sau khi User xác nhận xử lý liên kết.
- **A4 – Địa điểm đã tồn tại:** hệ thống không thêm bản ghi trùng và điều hướng User đến địa điểm hiện có.
- **A5 – Chuyến đi ONGOING:** User có thể thêm địa điểm mới; hệ thống đánh dấu đây là thay đổi trong quá trình thực hiện và yêu cầu cập nhật lịch trình nếu cần.

### 5.6.5. Luồng ngoại lệ

- **E1 – Dịch vụ địa điểm timeout hoặc lỗi:** thông báo không thể tìm kiếm tạm thời; giữ nguyên danh sách hiện tại và cho phép thử lại.
- **E2 – Kết quả thiếu tọa độ hoặc định danh:** không cho phép dùng cho tối ưu cho đến khi dữ liệu bắt buộc được bổ sung hoặc xác minh.
- **E3 – Không có quyền sở hữu chuyến đi:** từ chối toàn bộ thao tác.
- **E4 – Chuyến đi CANCELLED hoặc COMPLETED:** từ chối thay đổi danh sách địa điểm, trừ nghiệp vụ chỉnh sửa sau chuyến đi được quy định riêng.
- **E5 – Địa điểm bị nguồn ngoài xóa:** giữ bản ghi đã lưu cho lịch sử và thông báo dữ liệu hiện tại không còn khả dụng.

### 5.6.6. Quy tắc nghiệp vụ

- Nhận diện trùng ưu tiên theo định danh nhà cung cấp; tọa độ và tên chuẩn hóa được dùng làm phương án bổ sung.
- Dữ liệu riêng của chuyến đi không được ghi đè lên dữ liệu gốc của nhà cung cấp.
- Địa điểm cần tọa độ hợp lệ để tham gia tính khoảng cách và tối ưu thứ tự.
- Loại bỏ địa điểm không được tạo mục lịch trình mồ côi.
- Địa điểm dự kiến chưa được tính là đã khám phá nếu chưa có trạng thái đã ghé thăm hoặc check-in hợp lệ.

### 5.6.7. Truy vết

- **FR:** FR-PLACE-01 đến FR-PLACE-07; FR-TRIP-05; FR-TRIP-10.
- **NFR:** NFR-EXT-01 đến NFR-EXT-07, NFR-SEC-04, NFR-DATA-04, NFR-DATA-05, NFR-USE-05.

### 5.6.8. Tiêu chí nghiệm thu

- **AC1:** Given chuyến đi thuộc User, When chọn địa điểm hợp lệ, Then địa điểm xuất hiện đúng một lần trong danh sách.
- **AC2:** Given địa điểm đã tồn tại, When thêm lại, Then hệ thống không tạo bản ghi trùng.
- **AC3:** Given địa điểm đang được dùng trong lịch trình, When yêu cầu loại bỏ, Then hệ thống thông báo tác động và chờ xác nhận.
- **AC4:** Given Map & Place Service lỗi, When tìm kiếm, Then danh sách địa điểm đã lưu không bị mất hoặc thay đổi.
- **AC5:** Given địa điểm chưa có tọa độ hợp lệ, When yêu cầu tối ưu, Then hệ thống không sử dụng địa điểm đó như dữ liệu đã xác minh.

---

## 5.7. UC-ITIN-01 — Tạo lịch trình theo ngày

### 5.7.1. Thông tin chung

| Thuộc tính | Nội dung |
|---|---|
| Mục tiêu | Tổ chức các địa điểm của chuyến đi thành lịch trình theo ngày, thời gian và thứ tự tham quan. |
| Actor chính | User |
| Use Case liên quan | UC-ITIN-02, UC-ITIN-03, UC-ITIN-05, UC-ITIN-07 |
| Trigger | User chọn **Tạo lịch trình** từ chuyến đi DRAFT hoặc PLANNED. |
| Tiền điều kiện | User sở hữu chuyến đi; ngày chuyến đi hợp lệ; có ít nhất một địa điểm được chọn. |
| Hậu điều kiện thành công | Lịch trình được lưu với các ngày và mục lịch trình hợp lệ; chuyến đi có thể chuyển sang PLANNED khi đáp ứng điều kiện. |
| Hậu điều kiện thất bại | Lịch trình hiện tại không bị ghi đè bởi dữ liệu không hợp lệ. |
| Kênh | Web/Mobile |
| Mức ưu tiên | Must |

### 5.7.2. Dữ liệu vào và đầu ra

| Loại | Dữ liệu |
|---|---|
| Đầu vào | Ngày lịch trình, địa điểm, thời gian bắt đầu, thời lượng dự kiến, thứ tự và ghi chú. |
| Dữ liệu tính toán | Thời gian kết thúc dự kiến, khoảng cách và thời gian của chặng di chuyển khi khả dụng. |
| Đầu ra | Lịch trình theo ngày, cảnh báo xung đột và tổng quan thời gian di chuyển. |

### 5.7.3. Luồng chính

| Bước | Actor/Hệ thống | Mô tả |
|---:|---|---|
| 1 | User | Mở chức năng tạo lịch trình. |
| 2 | Hệ thống | Tạo hoặc hiển thị các ngày nằm trong khoảng thời gian chuyến đi. |
| 3 | Hệ thống | Hiển thị danh sách địa điểm chưa được phân bổ. |
| 4 | User | Chọn một ngày và thêm địa điểm vào ngày đó. |
| 5 | User | Thiết lập thời gian bắt đầu, thời lượng dự kiến và ghi chú. |
| 6 | Hệ thống | Kiểm tra ngày, thời gian, địa điểm và quyền sở hữu. |
| 7 | Hệ thống | Tính thời gian kết thúc dự kiến và kiểm tra xung đột với các mục hiện có. |
| 8 | User | Tiếp tục thêm và sắp xếp các địa điểm theo nhu cầu. |
| 9 | Hệ thống | Lấy dữ liệu chặng di chuyển giữa các địa điểm liên tiếp khi khả dụng. |
| 10 | Hệ thống | Hiển thị cảnh báo lịch trình quá dày hoặc chồng lấn để User điều chỉnh. |
| 11 | User | Xác nhận lưu lịch trình. |
| 12 | Hệ thống | Lưu lịch trình và hiển thị theo từng ngày, đúng thứ tự thời gian. |

### 5.7.4. Luồng thay thế

- **A1 – Tạo lịch trình rỗng:** hệ thống khởi tạo các ngày trước; User thêm địa điểm sau.
- **A2 – Sắp xếp thủ công:** User kéo thả hoặc chọn vị trí mới; hệ thống cập nhật thứ tự và tính lại chặng liên quan.
- **A3 – Di chuyển địa điểm sang ngày khác:** hệ thống kiểm tra ngày mới thuộc chuyến đi và xử lý xung đột tại cả hai ngày.
- **A4 – Chấp nhận cảnh báo mềm:** User có thể lưu lịch trình dù có cảnh báo thời gian nếu không vi phạm ràng buộc bắt buộc.
- **A5 – Đánh dấu sẵn sàng:** sau khi lịch trình hợp lệ, User xác nhận kế hoạch và chuyến đi chuyển từ DRAFT sang PLANNED.

### 5.7.5. Luồng ngoại lệ

- **E1 – Ngày nằm ngoài chuyến đi:** từ chối thêm mục lịch trình.
- **E2 – Địa điểm không thuộc chuyến đi:** yêu cầu thêm địa điểm vào chuyến đi trước.
- **E3 – Thời lượng hoặc thời gian không hợp lệ:** đánh dấu trường lỗi và không lưu mục đó.
- **E4 – Dịch vụ định tuyến lỗi:** vẫn cho phép lưu lịch trình thủ công nhưng đánh dấu chặng chưa có dữ liệu.
- **E5 – Dữ liệu đã được cập nhật ở thiết bị khác:** thông báo xung đột phiên bản và yêu cầu tải lại hoặc hợp nhất an toàn.
- **E6 – Không có quyền sở hữu:** từ chối thao tác.

### 5.7.6. Quy tắc nghiệp vụ

- Mọi ngày lịch trình phải nằm trong ngày bắt đầu và kết thúc chuyến đi.
- Mỗi mục lịch trình thuộc đúng một ngày và một địa điểm của chuyến đi.
- Thời lượng tham quan phải lớn hơn 0.
- Xung đột thời gian và lịch trình quá dày là cảnh báo mềm, trừ dữ liệu không hợp lệ về cấu trúc.
- Thời tiết và giờ mở cửa, nếu có, chỉ cung cấp ngữ cảnh và không tự động xóa hoặc di chuyển địa điểm.
- Hệ thống không tự động ghi đè lịch trình bằng thuật toán hoặc AI.

### 5.7.7. Truy vết

- **FR:** FR-ITIN-01 đến FR-ITIN-12; FR-TRIP-03, FR-TRIP-07.
- **NFR:** NFR-PERF-01, NFR-REL-02, NFR-DATA-02, NFR-DATA-03, NFR-USE-02 đến NFR-USE-06.

### 5.7.8. Tiêu chí nghiệm thu

- **AC1:** Given chuyến đi ba ngày, When tạo lịch trình, Then chỉ các ngày trong khoảng chuyến đi được sử dụng.
- **AC2:** Given địa điểm thuộc chuyến đi, When thêm với thời lượng hợp lệ, Then mục lịch trình được lưu đúng ngày và thứ tự.
- **AC3:** Given hai hoạt động chồng lấn, When lưu, Then hệ thống hiển thị cảnh báo rõ ràng.
- **AC4:** Given dịch vụ định tuyến lỗi, When User lưu lịch trình, Then dữ liệu thủ công vẫn được lưu và chặng được đánh dấu chưa khả dụng.
- **AC5:** Given đề xuất thuật toán chưa được xác nhận, When User rời màn hình, Then lịch trình hiện tại không bị thay đổi.

---

## 5.8. UC-OPT-01 — Yêu cầu sắp xếp thứ tự tham quan

### 5.8.1. Thông tin chung

| Thuộc tính | Nội dung |
|---|---|
| Mục tiêu | Đề xuất thứ tự ghé thăm các địa điểm trong một ngày nhằm giảm tổng khoảng cách hoặc thời gian di chuyển. |
| Actor chính | User |
| Actor hỗ trợ | Map & Place Service |
| Use Case liên quan | UC-OPT-02, UC-OPT-03 |
| Trigger | User chọn **Tối ưu thứ tự** tại một ngày lịch trình. |
| Tiền điều kiện | User sở hữu chuyến đi; ngày lịch trình có ít nhất hai địa điểm có tọa độ hợp lệ. |
| Hậu điều kiện thành công | Một phương án sắp xếp được tạo và hiển thị để User xem xét; lịch trình chỉ thay đổi nếu User xác nhận áp dụng. |
| Hậu điều kiện thất bại | Lịch trình hiện tại được giữ nguyên; nguyên nhân không thể tối ưu được thông báo. |
| Kênh | Web/Mobile |
| Mức ưu tiên | Must |

### 5.8.2. Dữ liệu vào và đầu ra

| Loại | Dữ liệu |
|---|---|
| Đầu vào bắt buộc | Ngày cần tối ưu, danh sách địa điểm, phương tiện và tiêu chí chi phí: khoảng cách hoặc thời gian. |
| Đầu vào tùy chọn | Điểm bắt đầu cố định, điểm kết thúc cố định và thời gian khởi hành dự kiến. |
| Dữ liệu trung gian | Ma trận chi phí giữa các địa điểm theo phương tiện và tiêu chí đã chọn. |
| Đầu ra | Thứ tự đề xuất, tổng chi phí, thuật toán sử dụng, chênh lệch với phương án hiện tại và cảnh báo dữ liệu thiếu nếu có. |

### 5.8.3. Luồng chính

| Bước | Actor/Hệ thống | Mô tả |
|---:|---|---|
| 1 | User | Mở một ngày lịch trình và chọn tối ưu thứ tự. |
| 2 | Hệ thống | Hiển thị danh sách địa điểm, phương tiện, tiêu chí chi phí và tùy chọn điểm đầu/cuối. |
| 3 | User | Chọn cấu hình và gửi yêu cầu. |
| 4 | Hệ thống | Kiểm tra quyền sở hữu, số lượng địa điểm, tọa độ và tham số. |
| 5 | Hệ thống | Yêu cầu Map & Place Service cung cấp ma trận khoảng cách hoặc thời gian di chuyển. |
| 6 | Hệ thống | Kiểm tra ma trận đầy đủ, đơn vị đo và sự phù hợp với phương tiện. |
| 7 | Hệ thống | Chọn Held–Karp nếu số địa điểm không vượt ngưỡng cấu hình; nếu vượt ngưỡng, chọn Greedy. |
| 8 | Hệ thống | Tính thứ tự thỏa mãn các điểm đầu/cuối cố định và tiêu chí chi phí. |
| 9 | Hệ thống | Tính tổng chi phí và so sánh với thứ tự hiện tại. |
| 10 | Hệ thống | Hiển thị phương án, thuật toán, tổng chi phí và mức chênh lệch. |
| 11 | User | Xem và lựa chọn áp dụng hoặc bỏ qua. |
| 12 | Hệ thống | Nếu User áp dụng, cập nhật thứ tự trong một giao dịch và tính lại các chặng; nếu bỏ qua, giữ nguyên lịch trình. |

### 5.8.4. Luồng thay thế

- **A1 – User chọn Greedy:** hệ thống dùng Greedy dù số địa điểm đủ nhỏ cho Held–Karp và ghi rõ đây là phương án gần tối ưu.
- **A2 – User chọn Held–Karp:** chỉ cho phép khi số địa điểm không vượt ngưỡng cấu hình.
- **A3 – Cố định điểm đầu:** thuật toán chỉ sắp xếp các điểm còn lại sau điểm đã cố định.
- **A4 – Cố định điểm đầu và cuối:** thuật toán giữ hai điểm này và sắp xếp các điểm trung gian.
- **A5 – Không có cải thiện:** hệ thống vẫn hiển thị kết quả và cho biết phương án hiện tại bằng hoặc tốt hơn theo tiêu chí đã chọn.
- **A6 – User chỉnh sửa kết quả:** User thay đổi thủ công thứ tự xem trước trước khi xác nhận lưu.

### 5.8.5. Luồng ngoại lệ

- **E1 – Ít hơn hai địa điểm hợp lệ:** không chạy thuật toán và thông báo chưa cần hoặc chưa thể tối ưu.
- **E2 – Địa điểm thiếu tọa độ:** chỉ rõ địa điểm không hợp lệ và yêu cầu bổ sung hoặc loại khỏi lần tối ưu.
- **E3 – Ma trận chi phí thiếu chặng:** không tuyên bố kết quả tối ưu; cho phép thử lại hoặc tiếp tục sắp xếp thủ công.
- **E4 – Map & Place Service timeout:** dừng sau thời gian chờ, giữ nguyên lịch trình và cho phép thử lại.
- **E5 – Đầu vào vượt ngưỡng Held–Karp:** tự động chuyển Greedy hoặc yêu cầu User chọn phương án khác.
- **E6 – Thuật toán không tìm được phương án khả thi:** thông báo ràng buộc gây xung đột và không thay đổi lịch trình.
- **E7 – Lịch trình thay đổi trong lúc tính:** hủy áp dụng kết quả cũ và yêu cầu tính lại trên phiên bản mới.

### 5.8.6. Quy tắc nghiệp vụ

- Held–Karp tìm phương án tối ưu chỉ đối với ma trận chi phí và các ràng buộc được đưa vào bài toán.
- Ngưỡng Held–Karp mặc định của MVP là tối đa 12 địa điểm và phải có thể cấu hình.
- Greedy cung cấp phương án nhanh nhưng không được mô tả là tối ưu toàn cục.
- Thuật toán chỉ sắp xếp thứ tự; tuyến đường giao thông do Map & Place Service cung cấp.
- Thời tiết, sở thích và giờ mở cửa không phải ràng buộc cứng của thuật toán ở MVP; chúng được AI hoặc giao diện dùng làm ngữ cảnh mềm.
- Kết quả không được ghi đè lịch trình trước khi User xác nhận.

### 5.8.7. Truy vết

- **FR:** FR-OPT-01 đến FR-OPT-10; FR-ITIN-08, FR-ITIN-10, FR-ITIN-12.
- **NFR:** NFR-PERF-04, NFR-PERF-05, NFR-EXT-01 đến NFR-EXT-05, NFR-MAINT-06, NFR-AI-03.

### 5.8.8. Tiêu chí nghiệm thu

- **AC1:** Given không quá 12 địa điểm và ma trận đầy đủ, When chọn Held–Karp, Then kết quả có tổng chi phí nhỏ nhất theo dữ liệu đầu vào kiểm thử.
- **AC2:** Given số địa điểm vượt ngưỡng, When yêu cầu Held–Karp, Then hệ thống không chạy ngoài giới hạn và chuyển hoặc đề xuất Greedy.
- **AC3:** Given User cố định điểm đầu và cuối, When tối ưu, Then hai điểm giữ đúng vị trí yêu cầu.
- **AC4:** Given Map Service thiếu một chặng, When tối ưu, Then hệ thống không tuyên bố kết quả tối ưu.
- **AC5:** Given User chưa chọn áp dụng, When đóng kết quả, Then thứ tự hiện tại không thay đổi.
- **AC6:** Given lịch trình thay đổi sau lúc tính, When áp dụng kết quả cũ, Then backend từ chối và yêu cầu tính lại.

---

## 5.9. UC-AI-01 — Yêu cầu AI phân tích và đề xuất lịch trình

### 5.9.1. Thông tin chung

| Thuộc tính | Nội dung |
|---|---|
| Mục tiêu | Phân tích lịch trình và ngữ cảnh chuyến đi để đưa ra đề xuất, cảnh báo và giải thích mang tính tham khảo. |
| Actor chính | User |
| Actor hỗ trợ | AI Service, Weather Service; Map & Place Service khi cần dữ liệu đã xác thực |
| Use Case liên quan | UC-AI-02, UC-AI-03, UC-AI-04 |
| Trigger | User chọn **Nhận đề xuất AI** tại chuyến đi hoặc ngày lịch trình. |
| Tiền điều kiện | User sở hữu chuyến đi; có lịch trình hoặc danh sách địa điểm đủ để phân tích. |
| Hậu điều kiện thành công | Đề xuất hợp lệ được hiển thị kèm lý do; thay đổi chỉ được lưu khi User xác nhận. |
| Hậu điều kiện thất bại | Lịch trình được giữ nguyên và vẫn có thể chỉnh sửa thủ công. |
| Kênh | Web/Mobile |
| Mức ưu tiên | Must |

### 5.9.2. Dữ liệu vào và đầu ra

| Loại | Dữ liệu |
|---|---|
| Ngữ cảnh bắt buộc | Thông tin chuyến đi, lịch trình hiện tại, các địa điểm và kết quả tối ưu gần nhất nếu có. |
| Ngữ cảnh tùy chọn | Sở thích, phương tiện, thời tiết trong phạm vi dự báo, yêu cầu cá nhân và cảnh báo thời gian. |
| Không được gửi | Mật khẩu, token, khóa API và dữ liệu nhận dạng không cần thiết. |
| Đầu ra | Danh sách đề xuất có cấu trúc, địa điểm bị tác động, thay đổi đề nghị, lý do, cảnh báo và mức độ tin cậy nếu mô hình hỗ trợ. |

### 5.9.3. Luồng chính

| Bước | Actor/Hệ thống | Mô tả |
|---:|---|---|
| 1 | User | Yêu cầu AI phân tích lịch trình. |
| 2 | Hệ thống | Kiểm tra quyền sở hữu và dữ liệu tối thiểu. |
| 3 | Hệ thống | Thu thập lịch trình, kết quả thuật toán, sở thích và yêu cầu cá nhân cần thiết. |
| 4 | Hệ thống | Lấy dữ liệu thời tiết nếu ngày chuyến đi nằm trong phạm vi dự báo. |
| 5 | Hệ thống | Loại bỏ dữ liệu nhạy cảm và xây dựng yêu cầu theo mẫu có phiên bản. |
| 6 | Hệ thống | Gửi yêu cầu có cấu trúc tới AI Service với giới hạn thời gian chờ. |
| 7 | AI Service | Trả về đề xuất và giải thích theo schema đã yêu cầu. |
| 8 | Hệ thống | Kiểm tra schema, kiểu dữ liệu, độ dài và tham chiếu địa điểm. |
| 9 | Hệ thống | Đối chiếu khoảng cách, thời tiết và địa điểm với dữ liệu tin cậy; loại hoặc đánh dấu nội dung không xác minh được. |
| 10 | Hệ thống | Hiển thị đề xuất, lý do và sự khác biệt so với lịch trình hiện tại. |
| 11 | User | Chọn áp dụng toàn bộ, áp dụng từng phần, chỉnh sửa hoặc bỏ qua. |
| 12 | Hệ thống | Kiểm tra lại thay đổi được chọn và chỉ cập nhật lịch trình sau xác nhận cuối cùng. |

### 5.9.4. Luồng thay thế

- **A1 – Chuyến đi ngoài phạm vi dự báo:** hệ thống không gửi dữ liệu thời tiết giả định; AI phân tích bằng dữ liệu còn lại và thông báo giới hạn.
- **A2 – Không có kết quả tối ưu:** AI vẫn có thể giải thích lịch trình hiện tại nhưng không được tuyên bố đó là phương án tối ưu.
- **A3 – User áp dụng một phần:** hệ thống chỉ cập nhật các đề xuất được chọn và kiểm tra xung đột phát sinh.
- **A4 – User chỉnh sửa đề xuất:** thay đổi của User được xem như dữ liệu mới và phải qua kiểm tra nghiệp vụ trước khi lưu.
- **A5 – User bỏ qua:** hệ thống đóng đề xuất và không thay đổi lịch trình.

### 5.9.5. Luồng ngoại lệ

- **E1 – AI timeout quá 30 giây:** dừng chờ, thông báo tạm thời không khả dụng và giữ nguyên lịch trình.
- **E2 – Phản hồi sai schema:** không hiển thị như kết quả hợp lệ; có thể thử sửa định dạng một lần hoặc yêu cầu lại theo giới hạn cấu hình.
- **E3 – AI tham chiếu địa điểm không tồn tại:** loại đề xuất đó hoặc đánh dấu không thể áp dụng.
- **E4 – AI đưa dữ liệu khoảng cách hoặc thời tiết mâu thuẫn:** ưu tiên nguồn chuyên biệt và không lưu dữ liệu AI như sự thật đã xác minh.
- **E5 – Dịch vụ thời tiết lỗi:** tiếp tục không có thời tiết nếu User đồng ý hoặc cho phép thử lại.
- **E6 – Lịch trình thay đổi trong lúc AI xử lý:** hiển thị kết quả là đã cũ và yêu cầu phân tích lại trước khi áp dụng.
- **E7 – Vượt hạn mức AI:** thông báo giới hạn và cho phép tiếp tục chỉnh sửa thủ công.

### 5.9.6. Quy tắc nghiệp vụ

- AI chỉ đóng vai trò tư vấn; User là người quyết định cuối cùng.
- Mọi đề xuất quan trọng phải có lý do liên quan đến ngữ cảnh đầu vào.
- AI không được tự tạo và lưu địa điểm, tuyến đường, khoảng cách hoặc thời tiết chưa được xác minh.
- Đầu ra AI phải được backend kiểm tra như dữ liệu không tin cậy.
- Dữ liệu gửi AI phải tuân theo nguyên tắc tối thiểu cần thiết.
- Thay đổi chỉ được áp dụng trên đúng phiên bản lịch trình đã được phân tích.

### 5.9.7. Truy vết

- **FR:** FR-AI-01 đến FR-AI-10; FR-CONTEXT-05 đến FR-CONTEXT-10; FR-ITIN-12.
- **NFR:** NFR-PERF-06; NFR-PRIV-04; NFR-EXT-01 đến NFR-EXT-07; NFR-AI-01 đến NFR-AI-08.

### 5.9.8. Tiêu chí nghiệm thu

- **AC1:** Given lịch trình hợp lệ, When AI trả đúng schema, Then hệ thống hiển thị đề xuất cùng lý do và không tự áp dụng.
- **AC2:** Given AI tham chiếu địa điểm không thuộc chuyến đi, When backend kiểm tra, Then đề xuất đó không thể được áp dụng.
- **AC3:** Given chuyến đi ngoài phạm vi dự báo, When phân tích, Then hệ thống không trình bày dữ liệu thời tiết giả như dự báo thực tế.
- **AC4:** Given AI timeout, When hết 30 giây, Then User nhận thông báo và vẫn chỉnh sửa lịch trình thủ công được.
- **AC5:** Given User chỉ chọn một đề xuất, When xác nhận, Then chỉ thay đổi được chọn được lưu.
- **AC6:** Given lịch trình đã đổi sau khi yêu cầu AI, When áp dụng kết quả cũ, Then hệ thống từ chối hoặc yêu cầu xác nhận phân tích lại.

---

## 5.10. UC-JOURNEY-04 — Check-in tại địa điểm

### 5.10.1. Thông tin chung

| Thuộc tính | Nội dung |
|---|---|
| Mục tiêu | Ghi nhận User đã có mặt tại một địa điểm trong chuyến đi bằng GPS hoặc phương thức xác nhận được cho phép. |
| Actor chính | User |
| Actor hỗ trợ | Thiết bị di động |
| Trigger | User chọn **Check-in** tại một địa điểm lịch trình. |
| Tiền điều kiện | User đã đăng nhập; sở hữu chuyến đi ONGOING; địa điểm có tọa độ hợp lệ. |
| Hậu điều kiện thành công | Một check-in hợp lệ được ghi nhận; địa điểm chuyển sang VISITED; tiến độ được cập nhật. |
| Hậu điều kiện thất bại | Không ghi nhận check-in sai; trạng thái địa điểm được giữ nguyên. |
| Kênh | Mobile |
| Mức ưu tiên | Must |

### 5.10.2. Dữ liệu vào và đầu ra

| Loại | Dữ liệu |
|---|---|
| Đầu vào | Định danh chuyến đi, địa điểm lịch trình và thao tác xác nhận của User. |
| Dữ liệu thiết bị | Vĩ độ, kinh độ, độ chính xác, thời điểm đo và trạng thái quyền vị trí. |
| Đầu ra | Thời điểm check-in, phương thức xác minh, khoảng cách tới địa điểm, trạng thái VISITED và tiến độ mới. |

### 5.10.3. Luồng chính

| Bước | Actor/Hệ thống | Mô tả |
|---:|---|---|
| 1 | User | Mở chi tiết địa điểm và chọn check-in. |
| 2 | Hệ thống | Kiểm tra quyền sở hữu, trạng thái chuyến đi và địa điểm. |
| 3 | Ứng dụng | Giải thích mục đích và yêu cầu quyền vị trí nếu chưa được cấp. |
| 4 | User | Cấp quyền vị trí cho thao tác check-in. |
| 5 | Thiết bị | Cung cấp tọa độ hiện tại, độ chính xác và thời điểm đo. |
| 6 | Hệ thống | Kiểm tra dữ liệu vị trí còn mới và độ chính xác nằm trong ngưỡng chấp nhận. |
| 7 | Hệ thống | Tính khoảng cách từ vị trí hiện tại đến tọa độ địa điểm. |
| 8 | Hệ thống | Xác định vị trí nằm trong bán kính check-in được cấu hình. |
| 9 | User | Xác nhận hoàn tất check-in. |
| 10 | Hệ thống | Tạo bản ghi check-in, đặt phương thức GPS_VERIFIED và cập nhật địa điểm thành VISITED trong một giao dịch. |
| 11 | Hệ thống | Cập nhật tiến độ và hiển thị kết quả thành công. |

### 5.10.4. Luồng thay thế

- **A1 – Quyền vị trí đã được cấp:** bỏ qua bước yêu cầu quyền và lấy vị trí hiện tại.
- **A2 – Xác nhận thủ công được cho phép:** khi không thể lấy GPS, User xác nhận đã đến; hệ thống ghi phương thức MANUAL và không mô tả là GPS_VERIFIED.
- **A3 – Địa điểm đã VISITED:** hệ thống hiển thị check-in hiện có và không tạo bản ghi trùng.
- **A4 – User ở ngoài bán kính:** hiển thị khoảng cách và cho phép thử lấy vị trí lại; nếu xác nhận thủ công được phép, User có thể chọn luồng A2.

### 5.10.5. Luồng ngoại lệ

- **E1 – User từ chối quyền vị trí:** không truy cập GPS; giải thích rằng check-in GPS không thực hiện được và cung cấp lựa chọn hợp lệ khác nếu có.
- **E2 – GPS tắt hoặc không khả dụng:** cho phép thử lại, mở cài đặt hoặc dùng xác nhận thủ công theo chính sách.
- **E3 – Độ chính xác quá thấp:** không xác minh GPS; yêu cầu di chuyển đến vị trí thoáng hơn hoặc thử lại.
- **E4 – Vị trí quá cũ:** yêu cầu thiết bị lấy vị trí mới.
- **E5 – Chuyến đi chưa ONGOING hoặc đã COMPLETED:** từ chối tạo check-in mới.
- **E6 – Mất mạng:** không thông báo thành công khi server chưa ghi nhận; giữ thao tác ở trạng thái chờ thử lại nếu hỗ trợ.
- **E7 – Gửi yêu cầu lặp:** trả bản ghi check-in hiện có hoặc kết quả idempotent, không tăng tiến độ nhiều lần.

### 5.10.6. Quy tắc nghiệp vụ

- Bán kính check-in là cấu hình hệ thống và phải được kiểm thử trong môi trường thực tế.
- GPS chỉ được lấy khi User chủ động dùng chức năng và đã cấp quyền.
- Không thu thập vị trí liên tục trong nền trong phạm vi MVP.
- Check-in GPS và xác nhận thủ công phải được phân biệt.
- Mỗi địa điểm lịch trình chỉ có một check-in hiệu lực cho mỗi lần ghé thăm được mô hình hóa.
- Chỉ trạng thái VISITED hoặc check-in hợp lệ mới được tính vào hồ sơ khám phá.

### 5.10.7. Truy vết

- **FR:** FR-JOURNEY-03 đến FR-JOURNEY-09, FR-JOURNEY-12.
- **NFR:** NFR-PRIV-01 đến NFR-PRIV-03, NFR-REL-03, NFR-SEC-04, NFR-COMP-03, NFR-DATA-06.

### 5.10.8. Tiêu chí nghiệm thu

- **AC1:** Given User ở trong bán kính và GPS đủ chính xác, When xác nhận check-in, Then địa điểm chuyển VISITED và tiến độ tăng đúng một lần.
- **AC2:** Given User từ chối quyền, When check-in, Then ứng dụng không truy cập vị trí và không khóa các chức năng không cần GPS.
- **AC3:** Given vị trí ngoài bán kính, When check-in GPS, Then hệ thống không ghi GPS_VERIFIED.
- **AC4:** Given xác nhận thủ công được phép, When User dùng phương thức này, Then bản ghi có phương thức MANUAL.
- **AC5:** Given địa điểm đã check-in, When gửi lại cùng thao tác, Then hệ thống không tạo bản ghi trùng hoặc tăng tiến độ lần hai.

---

## 5.11. UC-MEMORY-01 — Tạo Memory

### 5.11.1. Thông tin chung

| Thuộc tính | Nội dung |
|---|---|
| Mục tiêu | Cho phép User lưu hình ảnh và ghi chú gắn với một địa điểm trong chuyến đi để hình thành nhật ký hành trình. |
| Actor chính | User |
| Actor hỗ trợ | Thiết bị di động, Media Storage Service |
| Trigger | User chọn **Tạo Memory** từ địa điểm hoặc chuyến đi. |
| Tiền điều kiện | User đã đăng nhập; sở hữu chuyến đi; địa điểm thuộc chuyến đi; chuyến đi ONGOING hoặc COMPLETED. |
| Hậu điều kiện thành công | Memory được lưu và liên kết đúng User, chuyến đi, địa điểm; hình ảnh thành công có tham chiếu hợp lệ. |
| Hậu điều kiện thất bại | Không tạo Memory hoàn chỉnh với tham chiếu ảnh lỗi; dữ liệu nhập được giữ để thử lại khi phù hợp. |
| Kênh | Mobile/Web; chụp ảnh trực tiếp áp dụng trên Mobile. |
| Mức ưu tiên | Must |

### 5.11.2. Dữ liệu vào và đầu ra

| Loại | Dữ liệu |
|---|---|
| Đầu vào bắt buộc | Chuyến đi, địa điểm và ít nhất một trong hai nội dung: ghi chú hoặc hình ảnh. |
| Đầu vào tùy chọn | Tiêu đề, nhiều hình ảnh, thời điểm trải nghiệm và chú thích ảnh. |
| Dữ liệu hình ảnh | Tệp, MIME type, dung lượng, kích thước, trạng thái tải và tham chiếu lưu trữ. |
| Đầu ra | Memory, định danh, thời gian tạo, liên kết địa điểm và danh sách hình ảnh đã lưu thành công. |

### 5.11.3. Luồng chính

| Bước | Actor/Hệ thống | Mô tả |
|---:|---|---|
| 1 | User | Chọn tạo Memory tại một địa điểm. |
| 2 | Hệ thống | Kiểm tra quyền sở hữu, trạng thái chuyến đi và liên kết địa điểm. |
| 3 | Hệ thống | Hiển thị biểu mẫu với chuyến đi và địa điểm đã chọn. |
| 4 | User | Nhập ghi chú và chọn hoặc chụp hình ảnh. |
| 5 | Ứng dụng | Xin quyền camera hoặc thư viện ảnh khi cần. |
| 6 | Hệ thống | Kiểm tra nội dung, loại tệp, MIME type, kích thước và số lượng ảnh. |
| 7 | Ứng dụng | Tải hình ảnh lên và hiển thị tiến trình từng tệp. |
| 8 | Media Storage Service | Trả tham chiếu cho các hình ảnh đã lưu thành công. |
| 9 | Hệ thống | Xác minh kết quả tải lên và tạo Memory cùng các liên kết ảnh trong một giao dịch phù hợp. |
| 10 | Hệ thống | Ghi nhận thời gian và cập nhật nhật ký, thống kê Memory và hình ảnh. |
| 11 | Hệ thống | Hiển thị Memory vừa tạo. |

### 5.11.4. Luồng thay thế

- **A1 – Chỉ có ghi chú:** tạo Memory không ảnh nếu nội dung ghi chú hợp lệ.
- **A2 – Chỉ có hình ảnh:** tạo Memory không ghi chú nếu có ít nhất một ảnh tải thành công.
- **A3 – Một số ảnh lỗi:** hiển thị từng ảnh lỗi; User có thể bỏ ảnh lỗi và lưu các ảnh thành công hoặc thử lại.
- **A4 – Tạo sau chuyến đi:** User chọn chuyến đi COMPLETED và địa điểm tương ứng, sau đó nhập Memory như bình thường.
- **A5 – Chỉnh thời điểm trải nghiệm:** User chọn thời điểm nằm trong khoảng hợp lý của chuyến đi; hệ thống vẫn giữ thời gian tạo thực tế riêng biệt.

### 5.11.5. Luồng ngoại lệ

- **E1 – Không có ghi chú và ảnh:** không cho phép tạo Memory rỗng.
- **E2 – Tệp không hợp lệ hoặc vượt giới hạn:** từ chối tệp đó và hiển thị định dạng hoặc dung lượng được chấp nhận.
- **E3 – User từ chối quyền camera/thư viện:** không truy cập tài nguyên; User vẫn có thể nhập ghi chú hoặc chọn phương thức khác.
- **E4 – Media Storage Service lỗi:** không lưu tham chiếu ảnh chưa tồn tại; giữ ghi chú và cho phép thử tải lại.
- **E5 – Địa điểm không thuộc chuyến đi:** từ chối liên kết.
- **E6 – Không có quyền sở hữu:** từ chối truy cập hoặc tạo Memory.
- **E7 – Giao dịch lưu Memory lỗi:** hoàn tác liên kết dữ liệu; xử lý ảnh đã tải nhưng chưa liên kết theo cơ chế dọn dẹp.

### 5.11.6. Quy tắc nghiệp vụ

- Memory thuộc đúng một User và một chuyến đi.
- Memory tại địa điểm cụ thể phải liên kết với địa điểm thuộc chuyến đi đó.
- Memory phải có ít nhất ghi chú hoặc một hình ảnh hợp lệ.
- Việc tạo Memory không tự động được xem là check-in GPS và không tự chuyển địa điểm sang VISITED.
- Hình ảnh phải tuân thủ danh sách định dạng, dung lượng và số lượng được cấu hình.
- Xóa chuyến đi hoặc Memory phải xử lý tham chiếu và tệp hình ảnh theo chính sách lưu trữ.

### 5.11.7. Truy vết

- **FR:** FR-MEMORY-01 đến FR-MEMORY-10; FR-EXPLORE-04, FR-EXPLORE-05, FR-EXPLORE-08.
- **NFR:** NFR-PERF-07, NFR-REL-01 đến NFR-REL-03, NFR-SEC-07, NFR-PRIV-03, NFR-PRIV-05, NFR-DATA-03.

### 5.11.8. Tiêu chí nghiệm thu

- **AC1:** Given địa điểm thuộc chuyến đi, When User lưu ghi chú hợp lệ, Then Memory được tạo và liên kết đúng.
- **AC2:** Given ít nhất một ảnh hợp lệ, When tải thành công, Then Memory lưu đúng tham chiếu ảnh.
- **AC3:** Given Memory không có ghi chú và ảnh, When User lưu, Then hệ thống từ chối.
- **AC4:** Given một ảnh lỗi trong nhiều ảnh, When User chọn tiếp tục, Then chỉ ảnh thành công được liên kết và ảnh lỗi được thông báo.
- **AC5:** Given User B sử dụng định danh chuyến đi của User A, When tạo Memory, Then backend từ chối.
- **AC6:** Given User tạo Memory tại địa điểm chưa check-in, When lưu, Then hệ thống không tự đánh dấu địa điểm VISITED.

---

## 5.12. UC-JOURNEY-08 — Hoàn thành chuyến đi

### 5.12.1. Thông tin chung

| Thuộc tính | Nội dung |
|---|---|
| Mục tiêu | Kết thúc chuyến đi đang diễn ra, chốt trạng thái địa điểm và cập nhật hồ sơ khám phá cá nhân. |
| Actor chính | User |
| Trigger | User chọn **Hoàn thành chuyến đi**. |
| Tiền điều kiện | User đã đăng nhập; sở hữu chuyến đi ONGOING. |
| Hậu điều kiện thành công | Chuyến đi chuyển COMPLETED; số liệu địa điểm đã ghé thăm, tỉnh/thành, Memory và hành trình được cập nhật. |
| Hậu điều kiện thất bại | Chuyến đi vẫn ONGOING và dữ liệu hiện tại không bị thay đổi ngoài ý muốn. |
| Kênh | Mobile; có thể hỗ trợ Web. |
| Mức ưu tiên | Must |

### 5.12.2. Dữ liệu vào và đầu ra

| Loại | Dữ liệu |
|---|---|
| Đầu vào | Định danh chuyến đi và xác nhận của User. |
| Dữ liệu kiểm tra | Trạng thái các địa điểm, check-in, Memory và các mục chưa hoàn thành. |
| Đầu ra | Trạng thái COMPLETED, thời điểm hoàn thành, bản tổng kết và số liệu hồ sơ khám phá đã cập nhật. |

### 5.12.3. Luồng chính

| Bước | Actor/Hệ thống | Mô tả |
|---:|---|---|
| 1 | User | Mở chuyến đi đang diễn ra và chọn hoàn thành. |
| 2 | Hệ thống | Kiểm tra quyền sở hữu và trạng thái ONGOING. |
| 3 | Hệ thống | Tổng hợp địa điểm VISITED, SKIPPED và chưa có trạng thái. |
| 4 | Hệ thống | Hiển thị bản tóm tắt cùng các mục chưa được xử lý. |
| 5 | User | Xem lại và xác nhận hoàn thành chuyến đi. |
| 6 | Hệ thống | Chuyển các mục chưa thực hiện sang SKIPPED nếu User đã xác nhận theo chính sách. |
| 7 | Hệ thống | Chuyển chuyến đi sang COMPLETED và ghi thời điểm hoàn thành. |
| 8 | Hệ thống | Cập nhật thống kê chuyến đi, địa điểm đã khám phá và tỉnh/thành dựa trên các mục VISITED hợp lệ. |
| 9 | Hệ thống | Cập nhật số lượng Memory và hình ảnh liên quan. |
| 10 | Hệ thống | Hiển thị bản tổng kết và cho phép User xem hoặc bổ sung Memory. |

### 5.12.4. Luồng thay thế

- **A1 – Còn mục chưa có trạng thái:** User quay lại lịch trình để đánh dấu VISITED hoặc SKIPPED trước khi hoàn thành.
- **A2 – Hoàn thành với mục còn lại:** User xác nhận các mục chưa thực hiện được xem là SKIPPED; hệ thống tiếp tục luồng chính.
- **A3 – Không có Memory:** vẫn cho phép hoàn thành và gợi ý thêm Memory sau chuyến đi.
- **A4 – User hủy xác nhận:** đóng hộp thoại và giữ chuyến đi ONGOING.
- **A5 – Tự động gợi ý hoàn thành:** sau ngày kết thúc, hệ thống có thể nhắc User nhưng không tự chuyển trạng thái nếu chưa xác nhận.

### 5.12.5. Luồng ngoại lệ

- **E1 – Chuyến đi không ở trạng thái ONGOING:** từ chối thao tác lặp hoặc chuyển trạng thái không hợp lệ.
- **E2 – Không có quyền sở hữu:** từ chối yêu cầu.
- **E3 – Dữ liệu địa điểm đang xung đột:** không hoàn thành cho đến khi tải lại trạng thái mới nhất.
- **E4 – Cập nhật thống kê lỗi:** giao dịch chuyển trạng thái và dữ liệu bắt buộc được hoàn tác hoặc sự kiện cập nhật được lưu để xử lý lại an toàn.
- **E5 – Yêu cầu hoàn thành bị gửi lặp:** chỉ ghi nhận một lần và trả kết quả COMPLETED hiện có.

### 5.12.6. Quy tắc nghiệp vụ

- Chỉ chuyến đi ONGOING được chuyển sang COMPLETED qua Use Case này.
- Chỉ địa điểm VISITED hoặc có check-in hợp lệ được tính là đã khám phá.
- Địa điểm PLANNED hoặc SKIPPED không được tính là đã khám phá.
- Một địa điểm và một tỉnh/thành không được đếm trùng trong thống kê cá nhân theo quy tắc đã xác định.
- Hoàn thành chuyến đi không khóa chức năng xem hoặc bổ sung Memory sau chuyến đi.
- Chuyến đi COMPLETED không nhận check-in mới, trừ khi có quy trình mở lại được bổ sung trong tương lai.

### 5.12.7. Truy vết

- **FR:** FR-JOURNEY-08 đến FR-JOURNEY-11; FR-TRIP-07; FR-EXPLORE-01 đến FR-EXPLORE-08.
- **NFR:** NFR-REL-02, NFR-REL-03, NFR-DATA-03, NFR-DATA-06, NFR-PERF-08, NFR-USE-04.

### 5.12.8. Tiêu chí nghiệm thu

- **AC1:** Given chuyến đi ONGOING và User xác nhận, When hoàn thành, Then trạng thái chuyển COMPLETED đúng một lần.
- **AC2:** Given có mục chưa xử lý, When User chưa xác nhận chuyển SKIPPED, Then hệ thống chưa hoàn thành chuyến đi.
- **AC3:** Given địa điểm VISITED, When hoàn thành, Then địa điểm được tính vào hồ sơ khám phá.
- **AC4:** Given địa điểm PLANNED hoặc SKIPPED, When hoàn thành, Then địa điểm không được tính là đã khám phá.
- **AC5:** Given User gửi lặp yêu cầu hoàn thành, When backend xử lý, Then số liệu không bị cộng nhiều lần.
- **AC6:** Given chuyến đi COMPLETED, When User tạo thêm Memory hợp lệ, Then hệ thống cho phép và cập nhật số lượng Memory.

---

## 5.13. Ma trận truy vết tổng hợp

| Thứ tự | Use Case | Yêu cầu chức năng chính | Nhóm NFR trọng yếu |
|---:|---|---|---|
| 1 | UC-AUTH-01 — Đăng ký | FR-AUTH | NFR-SEC, NFR-PRIV, NFR-USE |
| 2 | UC-AUTH-02 — Đăng nhập | FR-AUTH, FR-ADMIN | NFR-SEC, NFR-REL |
| 3 | UC-TRIP-01 — Tạo chuyến đi | FR-TRIP | NFR-REL, NFR-DATA, NFR-SEC |
| 4 | UC-TRIP-05 — Quản lý địa điểm | FR-PLACE, FR-TRIP | NFR-EXT, NFR-DATA, NFR-USE |
| 5 | UC-ITIN-01 — Tạo lịch trình | FR-ITIN | NFR-REL, NFR-DATA, NFR-USE |
| 6 | UC-OPT-01 — Tối ưu thứ tự | FR-OPT, FR-ITIN | NFR-PERF, NFR-EXT, NFR-MAINT |
| 7 | UC-AI-01 — AI đề xuất | FR-AI, FR-CONTEXT | NFR-AI, NFR-PRIV, NFR-EXT |
| 8 | UC-JOURNEY-04 — Check-in | FR-JOURNEY | NFR-PRIV, NFR-SEC, NFR-COMP |
| 9 | UC-MEMORY-01 — Tạo Memory | FR-MEMORY, FR-EXPLORE | NFR-SEC, NFR-REL, NFR-PRIV |
| 10 | UC-JOURNEY-08 — Hoàn thành chuyến đi | FR-JOURNEY, FR-EXPLORE | NFR-REL, NFR-DATA, NFR-PERF |

## 5.14. Điều kiện hoàn thành bộ đặc tả

Bộ đặc tả được xem là sẵn sàng cho thiết kế và kiểm thử khi:

1. Tên actor, Use Case, trạng thái và thuật ngữ thống nhất với các tài liệu trước.
2. Mỗi luồng chính có tiền điều kiện, hậu điều kiện và đầu ra xác định.
3. Các trường hợp lỗi dịch vụ ngoài không làm mất dữ liệu người dùng hoặc tự thay đổi lịch trình.
4. Thuật toán và AI được phân biệt rõ về vai trò, dữ liệu đầu vào và quyền áp dụng kết quả.
5. Quyền sở hữu dữ liệu được kiểm tra trong mọi Use Case liên quan.
6. Mỗi Use Case có tiêu chí nghiệm thu có thể chuyển thành test case.
7. Các mã FR và NFR được truy vết tới tài liệu yêu cầu tương ứng.

