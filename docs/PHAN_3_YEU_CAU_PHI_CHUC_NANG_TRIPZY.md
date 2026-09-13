# 3. YÊU CẦU PHI CHỨC NĂNG HỆ THỐNG TRIPZY

**Phiên bản:** 1.0  
**Phạm vi:** Sản phẩm khả dụng tối thiểu (MVP)  
**Đối tượng áp dụng:** Ứng dụng web, ứng dụng di động, backend, cơ sở dữ liệu và các tích hợp bên ngoài của Tripzy

---

## 3.1. Mục đích

Phần này quy định các thuộc tính chất lượng và ràng buộc kỹ thuật mà hệ thống Tripzy phải đáp ứng bên cạnh các chức năng nghiệp vụ. Các yêu cầu tập trung vào hiệu năng, độ tin cậy, bảo mật, quyền riêng tư, khả năng sử dụng, tương thích, toàn vẹn dữ liệu, khả năng bảo trì, giám sát, triển khai và việc sử dụng AI có kiểm soát.

Mỗi yêu cầu có mã định danh, tiêu chí kiểm chứng và mức độ ưu tiên để phục vụ thiết kế, phát triển, kiểm thử và nghiệm thu.

## 3.2. Phạm vi áp dụng và nguyên tắc đánh giá

Các ngưỡng trong tài liệu là mục tiêu nghiệm thu cho MVP được triển khai trên hạ tầng thử nghiệm hoặc dịch vụ đám mây chi phí thấp. Việc đo hiệu năng phải được thực hiện trong điều kiện kiểm thử được ghi nhận rõ, bao gồm cấu hình máy chủ, kích thước dữ liệu, chất lượng mạng, số lượng người dùng đồng thời và tình trạng của dịch vụ bên ngoài.

Thời gian xử lý nội bộ không bao gồm thời gian chờ từ dịch vụ bản đồ, thời tiết, AI hoặc lưu trữ hình ảnh, trừ khi yêu cầu ghi rõ thời gian phản hồi đầu cuối. Nếu điều kiện triển khai thay đổi, các ngưỡng phải được cập nhật và phê duyệt lại thay vì tự ý loại bỏ.

### Mức độ ưu tiên

| Mức độ | Ý nghĩa |
|---|---|
| Must | Bắt buộc đáp ứng để MVP được nghiệm thu. |
| Should | Quan trọng và nên đáp ứng, nhưng có thể hoàn thiện sau các yêu cầu Must. |
| Could | Mục tiêu cải tiến khi còn đủ thời gian và nguồn lực. |

## 3.3. Tổng quan các nhóm yêu cầu

| Nhóm | Thuộc tính chất lượng | Mục tiêu |
|---|---|---|
| NFR-PERF | Hiệu năng | Bảo đảm các thao tác cốt lõi có thời gian phản hồi phù hợp. |
| NFR-REL | Độ tin cậy và khả dụng | Hạn chế mất dữ liệu và duy trì hoạt động ổn định. |
| NFR-SEC | Bảo mật | Bảo vệ tài khoản, API, dữ liệu và tài nguyên hệ thống. |
| NFR-PRIV | Quyền riêng tư | Kiểm soát việc thu thập, sử dụng và xóa dữ liệu cá nhân. |
| NFR-USE | Khả năng sử dụng và tiếp cận | Giúp người dùng thao tác rõ ràng, nhất quán và hạn chế sai sót. |
| NFR-COMP | Tương thích | Bảo đảm hệ thống hoạt động trên các nền tảng mục tiêu. |
| NFR-DATA | Toàn vẹn và chất lượng dữ liệu | Duy trì dữ liệu chính xác, nhất quán và có thể khôi phục. |
| NFR-EXT | Tích hợp dịch vụ bên ngoài | Kiểm soát lỗi, độ trễ và giới hạn của các API phụ thuộc. |
| NFR-AI | Chất lượng và an toàn AI | Bảo đảm đầu ra AI được kiểm tra và chỉ mang tính hỗ trợ. |
| NFR-MAINT | Khả năng bảo trì và kiểm thử | Giúp mã nguồn dễ hiểu, thay đổi và xác minh. |
| NFR-OBS | Ghi log và giám sát | Hỗ trợ phát hiện, phân tích và xử lý sự cố. |
| NFR-DEPLOY | Triển khai và cấu hình | Bảo đảm môi trường có thể tái tạo và cấu hình an toàn. |

## 3.4. Yêu cầu phi chức năng chi tiết

### 3.4.1. Hiệu năng

| Mã | Yêu cầu | Tiêu chí kiểm chứng | Ưu tiên |
|---|---|---|---|
| NFR-PERF-01 | Các API CRUD cốt lõi phải phản hồi nhanh trong điều kiện hoạt động bình thường. | Ít nhất 95% yêu cầu xem, tạo hoặc cập nhật chuyến đi và lịch trình có thời gian xử lý phía backend không quá 2 giây khi kiểm thử với 50 người dùng đồng thời. | Must |
| NFR-PERF-02 | Danh sách dữ liệu phải sử dụng phân trang hoặc giới hạn kết quả. | Mỗi yêu cầu danh sách không trả quá 50 bản ghi mặc định; người dùng có thể yêu cầu trang tiếp theo. | Must |
| NFR-PERF-03 | Giao diện phải phản hồi sớm cho thao tác của người dùng. | Trạng thái đang xử lý xuất hiện trong vòng 300 mili giây đối với thao tác không thể hoàn thành ngay. | Must |
| NFR-PERF-04 | Thuật toán Greedy phải xử lý nhanh sau khi đã có ma trận chi phí. | Thời gian tính toán không quá 2 giây với tối đa 50 địa điểm trên cấu hình kiểm thử của dự án. | Must |
| NFR-PERF-05 | Held–Karp phải được giới hạn theo quy mô đầu vào. | Thời gian tính toán không quá 5 giây với tối đa 12 địa điểm sau khi đã có ma trận chi phí; đầu vào lớn hơn phải chuyển sang phương án phù hợp khác. | Must |
| NFR-PERF-06 | Yêu cầu AI phải có giới hạn thời gian chờ. | Hệ thống kết thúc yêu cầu và hiển thị phương án xử lý phù hợp nếu không nhận được kết quả hợp lệ trong vòng 30 giây. | Must |
| NFR-PERF-07 | Tải hình ảnh không được làm khóa toàn bộ giao diện. | Việc tải ảnh diễn ra bất đồng bộ, hiển thị tiến trình và cho phép người dùng tiếp tục các thao tác không phụ thuộc. | Must |
| NFR-PERF-08 | Truy vấn thống kê phải có thời gian phản hồi phù hợp. | Hồ sơ khám phá cơ bản phản hồi không quá 3 giây với bộ dữ liệu kiểm thử của MVP. | Should |

### 3.4.2. Độ tin cậy và khả dụng

| Mã | Yêu cầu | Tiêu chí kiểm chứng | Ưu tiên |
|---|---|---|---|
| NFR-REL-01 | Lỗi của một dịch vụ bên ngoài không được làm mất dữ liệu người dùng đã nhập. | Khi bản đồ, thời tiết, AI hoặc lưu trữ ảnh bị lỗi, chuyến đi và lịch trình đã lưu vẫn truy cập và chỉnh sửa được. | Must |
| NFR-REL-02 | Các thao tác thay đổi nhiều bản ghi liên quan phải bảo đảm tính nguyên tử. | Nếu một bước bắt buộc thất bại, giao dịch được hoàn tác và không để lại dữ liệu lịch trình ở trạng thái không nhất quán. | Must |
| NFR-REL-03 | Yêu cầu được gửi lại không được tạo dữ liệu trùng ngoài ý muốn. | Các thao tác quan trọng như tạo chuyến đi, check-in hoặc tạo Memory phải có cơ chế phát hiện hoặc hạn chế yêu cầu lặp. | Must |
| NFR-REL-04 | Hệ thống phải xử lý được việc khởi động lại dịch vụ. | Backend khởi động lại không làm mất dữ liệu đã ghi nhận thành công trong cơ sở dữ liệu. | Must |
| NFR-REL-05 | Hệ thống phải cung cấp trạng thái sức khỏe dịch vụ. | Có cơ chế kiểm tra backend đang hoạt động và có thể kết nối đến các thành phần thiết yếu. | Must |
| NFR-REL-06 | Hệ thống phải có phương án sao lưu và khôi phục dữ liệu. | Có bản sao lưu cơ sở dữ liệu tối thiểu mỗi 24 giờ trong giai đoạn triển khai thử nghiệm và thực hiện thử khôi phục ít nhất một lần trước nghiệm thu. | Must |
| NFR-REL-07 | Mục tiêu mất dữ liệu và khôi phục phải phù hợp với MVP. | Mục tiêu RPO không quá 24 giờ và RTO không quá 8 giờ trong môi trường thử nghiệm. | Should |
| NFR-REL-08 | Các lỗi chưa xử lý không được làm dừng toàn bộ hệ thống. | Backend trả mã lỗi chuẩn hóa; ứng dụng hiển thị thông báo và vẫn sử dụng được các chức năng không bị ảnh hưởng. | Must |

### 3.4.3. Bảo mật

| Mã | Yêu cầu | Tiêu chí kiểm chứng | Ưu tiên |
|---|---|---|---|
| NFR-SEC-01 | Dữ liệu truyền qua mạng phải được mã hóa trong môi trường triển khai. | Tất cả kết nối từ web hoặc mobile đến backend sử dụng HTTPS với TLS 1.2 trở lên. | Must |
| NFR-SEC-02 | Mật khẩu phải được bảo vệ bằng hàm băm một chiều an toàn. | Cơ sở dữ liệu không chứa mật khẩu dạng thuần hoặc dữ liệu có thể giải mã trực tiếp. | Must |
| NFR-SEC-03 | Phiên đăng nhập phải có thời hạn và có thể bị thu hồi. | Mã truy cập hết hạn theo cấu hình; đăng xuất, đổi mật khẩu hoặc vô hiệu hóa tài khoản có thể làm mất hiệu lực phiên theo chính sách hệ thống. | Must |
| NFR-SEC-04 | Mọi API bảo vệ phải xác thực và kiểm tra quyền sở hữu tài nguyên ở backend. | Thử thay định danh chuyến đi, lịch trình hoặc Memory của người khác phải bị từ chối với mã lỗi phù hợp. | Must |
| NFR-SEC-05 | Dữ liệu đầu vào phải được kiểm tra phía máy chủ. | Backend từ chối dữ liệu sai kiểu, vượt giới hạn, chứa tham chiếu không hợp lệ hoặc vi phạm quy tắc nghiệp vụ. | Must |
| NFR-SEC-06 | Hệ thống phải hạn chế các yêu cầu có nguy cơ bị lạm dụng. | Đăng nhập, khôi phục mật khẩu, gọi AI và tải tệp được áp dụng giới hạn tần suất theo tài khoản, thiết bị hoặc địa chỉ mạng phù hợp. | Must |
| NFR-SEC-07 | Tệp hình ảnh tải lên phải được kiểm tra. | Hệ thống kiểm tra định dạng, MIME type, kích thước và từ chối nội dung không thuộc loại cho phép trước khi ghi nhận vào Memory. | Must |
| NFR-SEC-08 | Bí mật hệ thống không được lưu trực tiếp trong mã nguồn. | Mật khẩu cơ sở dữ liệu, khóa API và khóa ký được cung cấp qua biến môi trường hoặc cơ chế quản lý bí mật; không xuất hiện trong kho mã nguồn. | Must |
| NFR-SEC-09 | Thông báo lỗi không được làm lộ thông tin nhạy cảm. | Phản hồi cho người dùng không chứa stack trace, câu lệnh SQL, khóa API, đường dẫn nội bộ hoặc cấu hình máy chủ. | Must |
| NFR-SEC-10 | Log không được chứa dữ liệu xác thực nhạy cảm. | Không ghi mật khẩu, access token, refresh token, khóa API hoặc nội dung bí mật vào log. | Must |
| NFR-SEC-11 | Hệ thống phải phòng tránh các nhóm lỗ hổng web phổ biến. | Thực hiện kiểm tra tối thiểu đối với Broken Access Control, Injection, XSS, CSRF theo kiến trúc xác thực và tải tệp không an toàn trước nghiệm thu. | Must |
| NFR-SEC-12 | Các thư viện phụ thuộc phải được quản lý. | Không phát hành bản nghiệm thu khi công cụ quét phát hiện lỗ hổng mức nghiêm trọng hoặc rất nghiêm trọng chưa có biện pháp xử lý được ghi nhận. | Should |
| NFR-SEC-13 | Quyền quản trị phải tuân theo nguyên tắc đặc quyền tối thiểu. | Tài khoản thông thường không thể truy cập API quản trị; Admin chỉ nhận các quyền cần thiết cho nghiệp vụ đã được xác định. | Must |
| NFR-SEC-14 | Tài khoản quản trị phải được bảo vệ tăng cường. | Mật khẩu Admin tuân thủ chính sách mạnh hơn và hệ thống nên hỗ trợ xác thực đa yếu tố khi triển khai ngoài môi trường nội bộ. | Should |

### 3.4.4. Quyền riêng tư và bảo vệ dữ liệu cá nhân

| Mã | Yêu cầu | Tiêu chí kiểm chứng | Ưu tiên |
|---|---|---|---|
| NFR-PRIV-01 | Ứng dụng chỉ truy cập vị trí sau khi người dùng được thông báo và cấp quyền. | Từ chối quyền vị trí không ngăn người dùng sử dụng các chức năng không phụ thuộc GPS. | Must |
| NFR-PRIV-02 | Hệ thống chỉ thu thập vị trí khi cần cho chức năng chủ động. | Không thu thập GPS liên tục trong nền trong phạm vi MVP; mỗi lần lấy vị trí gắn với thao tác như xác định vị trí hoặc check-in. | Must |
| NFR-PRIV-03 | Người dùng phải có thể thu hồi quyền trên thiết bị. | Ứng dụng xử lý đúng khi quyền vị trí, camera hoặc thư viện ảnh bị thu hồi và hướng dẫn cấp lại khi cần. | Must |
| NFR-PRIV-04 | Dữ liệu gửi tới AI phải được giảm thiểu. | Chỉ gửi dữ liệu cần thiết để tạo đề xuất; không gửi mật khẩu, token, khóa API hoặc thông tin nhận dạng không liên quan. | Must |
| NFR-PRIV-05 | Dữ liệu chuyến đi và Memory mặc định là riêng tư. | Người khác không thể truy cập dữ liệu nếu chưa có chức năng và quyền chia sẻ được xác định rõ. | Must |
| NFR-PRIV-06 | Người dùng phải có quyền xóa dữ liệu do mình tạo. | Chuyến đi và Memory có thể được yêu cầu xóa; hệ thống thông báo rõ tác động và xử lý dữ liệu liên quan theo chính sách lưu trữ. | Must |
| NFR-PRIV-07 | Hệ thống phải minh bạch về dữ liệu được sử dụng. | Màn hình xin quyền hoặc chính sách quyền riêng tư nêu mục đích sử dụng vị trí, hình ảnh và dữ liệu gửi tới AI. | Should |

### 3.4.5. Khả năng sử dụng và tiếp cận

| Mã | Yêu cầu | Tiêu chí kiểm chứng | Ưu tiên |
|---|---|---|---|
| NFR-USE-01 | Giao diện phải sử dụng thuật ngữ nhất quán. | Các thuật ngữ Chuyến đi, Lịch trình, Địa điểm, Check-in và Memory được dùng thống nhất trên web và mobile. | Must |
| NFR-USE-02 | Các luồng chính phải rõ ràng và có phản hồi trạng thái. | Tạo chuyến đi, thêm địa điểm, áp dụng đề xuất, check-in và tạo Memory đều có trạng thái đang xử lý, thành công hoặc thất bại. | Must |
| NFR-USE-03 | Thông báo lỗi phải dễ hiểu và có hướng xử lý. | Thông báo nêu được vấn đề ở mức phù hợp và hướng dẫn người dùng sửa dữ liệu hoặc thử lại. | Must |
| NFR-USE-04 | Thao tác phá hủy dữ liệu phải được xác nhận. | Xóa chuyến đi, xóa Memory, hủy chuyến đi hoặc loại bỏ dữ liệu liên kết yêu cầu xác nhận trước khi thực hiện. | Must |
| NFR-USE-05 | Người dùng không được mất dữ liệu nhập do lỗi dịch vụ ngoài. | Nội dung biểu mẫu vẫn được giữ khi yêu cầu bản đồ, thời tiết, AI hoặc tải ảnh thất bại. | Must |
| NFR-USE-06 | Giao diện phải hỗ trợ các trạng thái dữ liệu đầy đủ. | Mỗi màn hình danh sách có trạng thái tải, rỗng, có dữ liệu và lỗi. | Must |
| NFR-USE-07 | Giao diện phải có độ tương phản và kích thước thao tác phù hợp. | Nội dung chính đạt độ tương phản tối thiểu theo WCAG 2.1 mức AA; vùng chạm quan trọng trên mobile có kích thước tối thiểu khoảng 44 × 44 điểm. | Should |
| NFR-USE-08 | Ứng dụng web phải hỗ trợ bàn phím cho thao tác cơ bản. | Có thể di chuyển focus, nhập biểu mẫu và kích hoạt nút chính mà không bắt buộc dùng chuột. | Should |
| NFR-USE-09 | Hình ảnh có ý nghĩa phải có nội dung thay thế trên web. | Ảnh địa điểm và ảnh chức năng có thuộc tính mô tả phù hợp; ảnh trang trí có thể được bỏ qua bởi công nghệ hỗ trợ. | Should |
| NFR-USE-10 | Định dạng hiển thị phải phù hợp với người dùng Việt Nam. | Ngày, giờ, khoảng cách, tiền tệ và thông báo được trình bày nhất quán theo cấu hình tiếng Việt. | Must |

### 3.4.6. Tương thích và tính di động

| Mã | Yêu cầu | Tiêu chí kiểm chứng | Ưu tiên |
|---|---|---|---|
| NFR-COMP-01 | Ứng dụng web phải hoạt động trên các trình duyệt mục tiêu. | Kiểm thử trên hai phiên bản ổn định gần nhất của Google Chrome và Microsoft Edge tại thời điểm nghiệm thu. | Must |
| NFR-COMP-02 | Giao diện web phải thích ứng với kích thước màn hình phổ biến. | Không xuất hiện cuộn ngang ngoài ý muốn ở chiều rộng từ 360 px đến 1920 px đối với các màn hình chính. | Must |
| NFR-COMP-03 | Ứng dụng mobile phải hỗ trợ nền tảng kiểm thử đã chọn. | MVP được kiểm thử đầy đủ trên Android 10 trở lên; hỗ trợ iOS chỉ được công bố khi đã kiểm thử riêng. | Must |
| NFR-COMP-04 | API phải độc lập với giao diện người dùng. | Web và mobile sử dụng cùng hợp đồng REST/JSON cho các nghiệp vụ chung, trừ chức năng phụ thuộc thiết bị. | Must |
| NFR-COMP-05 | Hệ thống phải hỗ trợ tiếng Việt và Unicode. | Tên người dùng, địa điểm và ghi chú tiếng Việt được lưu, tìm kiếm và hiển thị đúng dấu. | Must |
| NFR-COMP-06 | Thời gian và đơn vị đo phải nhất quán. | Thời gian được lưu theo chuẩn thống nhất và chuyển đổi khi hiển thị; khoảng cách dùng mét hoặc kilômét, thời lượng dùng phút hoặc giờ. | Must |

### 3.4.7. Toàn vẹn và chất lượng dữ liệu

| Mã | Yêu cầu | Tiêu chí kiểm chứng | Ưu tiên |
|---|---|---|---|
| NFR-DATA-01 | Các thực thể phải có định danh duy nhất. | Người dùng, chuyến đi, lịch trình, địa điểm liên kết, check-in và Memory có định danh không trùng lặp. | Must |
| NFR-DATA-02 | Cơ sở dữ liệu phải áp dụng các ràng buộc quan trọng. | Không thể lưu bản ghi thiếu trường bắt buộc, tham chiếu không tồn tại hoặc vi phạm tính duy nhất đã xác định. | Must |
| NFR-DATA-03 | Việc xóa dữ liệu phải bảo toàn quan hệ. | Xóa hoặc vô hiệu hóa thực thể không tạo bản ghi mồ côi; tác động lan truyền được xác định và kiểm thử. | Must |
| NFR-DATA-04 | Dữ liệu địa điểm bên ngoài phải giữ nguồn tham chiếu. | Lưu định danh nhà cung cấp, tọa độ và thời điểm cập nhật cần thiết để nhận diện và kiểm tra dữ liệu. | Must |
| NFR-DATA-05 | Dữ liệu khoảng cách, thời tiết và AI phải phân biệt với dữ liệu người dùng nhập. | Có thể xác định nguồn hoặc loại dữ liệu khi cần kiểm tra và xử lý lại. | Must |
| NFR-DATA-06 | Số liệu thống kê phải có quy tắc đếm rõ ràng. | Địa điểm và tỉnh/thành không bị đếm trùng trái với các quy tắc nghiệp vụ đã định nghĩa. | Must |
| NFR-DATA-07 | Thay đổi cấu trúc dữ liệu phải được quản lý bằng phiên bản. | Mọi thay đổi schema được thực hiện bằng migration có thứ tự và có thể chạy nhất quán trên môi trường mới. | Must |
| NFR-DATA-08 | Dữ liệu mẫu và dữ liệu thật phải được tách biệt. | Dữ liệu thử nghiệm không được tự động đưa vào môi trường nghiệm thu hoặc triển khai chính thức. | Should |

### 3.4.8. Tích hợp dịch vụ bên ngoài

| Mã | Yêu cầu | Tiêu chí kiểm chứng | Ưu tiên |
|---|---|---|---|
| NFR-EXT-01 | Mọi yêu cầu tới dịch vụ ngoài phải có thời gian chờ tối đa. | Không có kết nối bản đồ, thời tiết, AI hoặc lưu trữ ảnh chờ vô hạn. | Must |
| NFR-EXT-02 | Hệ thống chỉ thử lại các yêu cầu phù hợp. | Chỉ retry lỗi tạm thời và thao tác an toàn; số lần thử lại được giới hạn để tránh tạo dữ liệu trùng hoặc vượt hạn mức. | Must |
| NFR-EXT-03 | Lỗi dịch vụ ngoài phải được chuẩn hóa. | Backend chuyển lỗi nhà cung cấp thành mã lỗi nội bộ ổn định; giao diện không phụ thuộc trực tiếp vào thông báo lỗi của nhà cung cấp. | Must |
| NFR-EXT-04 | Hệ thống phải quản lý hạn mức API. | Có giới hạn gọi, cache hoặc cơ chế giảm yêu cầu lặp đối với dữ liệu phù hợp; khi hết hạn mức phải có thông báo rõ ràng. | Must |
| NFR-EXT-05 | Dữ liệu có thời hạn phải ghi nhận độ mới. | Dữ liệu thời tiết, tuyến đường hoặc ma trận chi phí lưu tạm phải có thời điểm tạo và thời hạn sử dụng phù hợp. | Must |
| NFR-EXT-06 | Tích hợp phải có khả năng thay thế nhà cung cấp ở mức hợp lý. | Mã nghiệp vụ chính không phụ thuộc trực tiếp vào cấu trúc phản hồi riêng của nhà cung cấp; dữ liệu được chuyển sang mô hình nội bộ. | Should |
| NFR-EXT-07 | Khóa và thông tin nhà cung cấp phải cấu hình riêng theo môi trường. | Có thể thay đổi khóa, URL hoặc cấu hình tích hợp mà không sửa mã nguồn nghiệp vụ. | Must |
| NFR-EXT-08 | Hệ thống phải tuân thủ điều kiện sử dụng của nguồn dữ liệu. | Hiển thị ghi công, logo hoặc thông tin nguồn khi điều khoản của nhà cung cấp yêu cầu. | Must |

### 3.4.9. Chất lượng và an toàn của chức năng AI

| Mã | Yêu cầu | Tiêu chí kiểm chứng | Ưu tiên |
|---|---|---|---|
| NFR-AI-01 | Phản hồi AI phải được xem là dữ liệu không tin cậy cho đến khi kiểm tra. | Backend kiểm tra schema, kiểu dữ liệu, độ dài và tham chiếu trước khi chuyển kết quả tới giao diện. | Must |
| NFR-AI-02 | AI không được tự động thay đổi dữ liệu nghiệp vụ. | Không có đề xuất nào được ghi đè vào lịch trình nếu chưa có thao tác xác nhận của người dùng. | Must |
| NFR-AI-03 | AI không phải nguồn xác thực dữ liệu thực tế. | Khoảng cách, tuyến đường, tọa độ và thời tiết phải lấy từ nguồn chuyên biệt; giá trị AI tự tạo không được lưu như dữ liệu đã xác minh. | Must |
| NFR-AI-04 | Đề xuất phải đi kèm giải thích. | Mỗi thay đổi quan trọng được đề xuất phải có lý do liên quan đến dữ liệu đầu vào đã cung cấp. | Must |
| NFR-AI-05 | Giao diện phải thể hiện tính tham khảo. | Người dùng nhìn thấy thông báo rằng đề xuất AI có thể không chính xác và có thể áp dụng, chỉnh sửa hoặc bỏ qua. | Must |
| NFR-AI-06 | Hệ thống phải có phương án dự phòng khi AI lỗi. | Người dùng vẫn có thể xem, sắp xếp và chỉnh sửa lịch trình thủ công khi AI không khả dụng. | Must |
| NFR-AI-07 | Đầu vào AI phải được giới hạn và xử lý an toàn. | Dữ liệu do người dùng nhập không được dùng để thực thi mã, truy cập bí mật hoặc bỏ qua quy tắc xác nhận của hệ thống. | Must |
| NFR-AI-08 | Việc gọi AI phải có thể truy vết ở mức kỹ thuật. | Ghi nhận thời điểm, trạng thái, loại yêu cầu, mô hình hoặc phiên bản cấu hình và mã tương quan mà không ghi dữ liệu nhạy cảm không cần thiết. | Should |

### 3.4.10. Khả năng bảo trì và kiểm thử

| Mã | Yêu cầu | Tiêu chí kiểm chứng | Ưu tiên |
|---|---|---|---|
| NFR-MAINT-01 | Mã nguồn phải được tổ chức theo mô-đun hoặc lớp trách nhiệm rõ ràng. | Nghiệp vụ, truy cập dữ liệu, tích hợp ngoài và giao diện API không trộn lẫn trách nhiệm không cần thiết. | Must |
| NFR-MAINT-02 | Mã nguồn phải tuân thủ quy ước thống nhất. | Có quy ước đặt tên, định dạng, cấu trúc thư mục và quy trình review áp dụng cho backend, web và mobile. | Must |
| NFR-MAINT-03 | API phải có tài liệu cập nhật. | Các endpoint được mô tả bằng OpenAPI hoặc tài liệu tương đương, gồm dữ liệu vào, dữ liệu ra và mã lỗi chính. | Must |
| NFR-MAINT-04 | Logic nghiệp vụ cốt lõi phải có kiểm thử tự động. | Các dịch vụ liên quan đến chuyến đi, lịch trình, thuật toán, quyền sở hữu và thống kê có unit test hoặc integration test. | Must |
| NFR-MAINT-05 | Phạm vi kiểm thử phải đạt ngưỡng tối thiểu. | Độ bao phủ dòng lệnh của lớp nghiệp vụ cốt lõi đạt tối thiểu 70%, không dùng tỷ lệ này thay thế việc kiểm thử tình huống quan trọng. | Should |
| NFR-MAINT-06 | Thuật toán phải có bộ dữ liệu kiểm thử xác định. | Greedy và Held–Karp được kiểm thử với trường hợp cơ bản, một địa điểm, nhiều địa điểm, chi phí bất đối xứng, dữ liệu thiếu và đầu vào vượt ngưỡng. | Must |
| NFR-MAINT-07 | Các tích hợp bên ngoài phải có khả năng mô phỏng trong kiểm thử. | Có mock, stub hoặc sandbox để kiểm thử lỗi, timeout và phản hồi không hợp lệ mà không phụ thuộc hoàn toàn vào dịch vụ thật. | Must |
| NFR-MAINT-08 | Thay đổi quan trọng phải được quản lý qua Git. | Mã nguồn được commit có mô tả, phát triển qua nhánh hoặc pull request và không đưa bí mật vào lịch sử Git. | Must |

### 3.4.11. Ghi log, giám sát và truy vết

| Mã | Yêu cầu | Tiêu chí kiểm chứng | Ưu tiên |
|---|---|---|---|
| NFR-OBS-01 | Backend phải ghi log có cấu trúc cho sự kiện kỹ thuật quan trọng. | Log có thời gian, mức độ, thành phần, mã tương quan và thông tin lỗi cần thiết. | Must |
| NFR-OBS-02 | Mỗi yêu cầu phải có khả năng truy vết. | Có request ID hoặc correlation ID xuyên suốt yêu cầu nội bộ và tích hợp ngoài khi phù hợp. | Should |
| NFR-OBS-03 | Không ghi dữ liệu nhạy cảm vào log. | Kiểm tra log không chứa mật khẩu, token, khóa API hoặc dữ liệu vị trí chi tiết không cần thiết. | Must |
| NFR-OBS-04 | Hệ thống phải phân biệt lỗi nghiệp vụ và lỗi kỹ thuật. | Mã lỗi, mức log và thông báo phản ánh đúng loại lỗi để hỗ trợ xử lý. | Must |
| NFR-OBS-05 | Phải theo dõi được lỗi tích hợp bên ngoài. | Ghi nhận nhà cung cấp, loại yêu cầu, thời gian phản hồi, trạng thái và lỗi đã chuẩn hóa. | Must |
| NFR-OBS-06 | Hệ thống nên có chỉ số vận hành cơ bản. | Có thể theo dõi số yêu cầu, tỷ lệ lỗi, thời gian phản hồi và trạng thái kết nối cơ sở dữ liệu. | Should |
| NFR-OBS-07 | Log phải có chính sách lưu giữ. | Thời gian lưu và cơ chế xóa hoặc luân chuyển log được cấu hình phù hợp với dung lượng môi trường thử nghiệm. | Should |

### 3.4.12. Triển khai và cấu hình

| Mã | Yêu cầu | Tiêu chí kiểm chứng | Ưu tiên |
|---|---|---|---|
| NFR-DEPLOY-01 | Môi trường chạy phải có khả năng tái tạo. | Backend và cơ sở dữ liệu có cấu hình Docker; thành viên mới có thể khởi động môi trường theo tài liệu hướng dẫn. | Must |
| NFR-DEPLOY-02 | Cấu hình phải tách khỏi mã nguồn. | URL cơ sở dữ liệu, khóa API, cổng, profile và giới hạn hệ thống được cung cấp từ biến môi trường hoặc tệp cấu hình theo môi trường. | Must |
| NFR-DEPLOY-03 | Các môi trường phải được phân tách. | Có cấu hình riêng tối thiểu cho phát triển và triển khai thử nghiệm; không dùng dữ liệu thật trong kiểm thử tự động. | Must |
| NFR-DEPLOY-04 | Migration cơ sở dữ liệu phải chạy có kiểm soát. | Phiên bản ứng dụng chỉ khởi động thành công khi schema tương thích hoặc migration cần thiết đã hoàn thành. | Must |
| NFR-DEPLOY-05 | Hệ thống phải kiểm tra cấu hình bắt buộc khi khởi động. | Thiếu khóa hoặc cấu hình thiết yếu phải tạo thông báo rõ ràng và không khởi động ở trạng thái hoạt động một phần không kiểm soát. | Must |
| NFR-DEPLOY-06 | Quy trình xây dựng phải có thể lặp lại. | Cùng phiên bản mã nguồn và cấu hình phụ thuộc tạo được artifact có thể triển khai trên môi trường mục tiêu. | Must |
| NFR-DEPLOY-07 | Có tài liệu vận hành tối thiểu. | Tài liệu mô tả cách cài đặt, cấu hình, khởi động, kiểm tra sức khỏe, sao lưu và khôi phục hệ thống. | Should |

## 3.5. Ma trận truy vết yêu cầu chức năng – thuộc tính chất lượng

| Nhóm chức năng | Nhóm yêu cầu phi chức năng trọng yếu |
|---|---|
| Tài khoản và xác thực | NFR-SEC, NFR-PRIV, NFR-REL |
| Hồ sơ người dùng | NFR-SEC, NFR-PRIV, NFR-DATA |
| Khám phá địa điểm | NFR-PERF, NFR-EXT, NFR-COMP |
| Quản lý chuyến đi | NFR-REL, NFR-DATA, NFR-USE |
| Quản lý lịch trình | NFR-PERF, NFR-REL, NFR-DATA |
| Sắp xếp thứ tự tham quan | NFR-PERF, NFR-MAINT, NFR-DATA |
| Bản đồ và thời tiết | NFR-EXT, NFR-REL, NFR-OBS |
| AI hỗ trợ lịch trình | NFR-AI, NFR-PRIV, NFR-EXT |
| GPS và check-in | NFR-PRIV, NFR-SEC, NFR-COMP |
| Memory và hình ảnh | NFR-SEC, NFR-PRIV, NFR-REL |
| Hồ sơ khám phá | NFR-PERF, NFR-DATA, NFR-USE |

## 3.6. Tiêu chí nghiệm thu tổng quát

Phần yêu cầu phi chức năng của MVP được xem là đạt khi đáp ứng các điều kiện sau:

1. Các luồng nghiệp vụ cốt lõi đạt ngưỡng hiệu năng đã quy định trong môi trường kiểm thử được ghi nhận.
2. Hệ thống không mất hoặc làm sai lệch dữ liệu khi dịch vụ bên ngoài lỗi, timeout hoặc trả dữ liệu không hợp lệ.
3. Các kiểm tra về xác thực, phân quyền sở hữu, dữ liệu đầu vào, tải tệp và bảo vệ bí mật đạt yêu cầu.
4. Việc sử dụng GPS, hình ảnh và AI tuân thủ nguyên tắc thông báo, xin quyền và giảm thiểu dữ liệu.
5. Web và mobile hoạt động trên các nền tảng mục tiêu, hiển thị đúng tiếng Việt và có đầy đủ trạng thái tải, rỗng, lỗi và thành công.
6. Thuật toán và nghiệp vụ cốt lõi có kiểm thử tự động; migration cơ sở dữ liệu và tài liệu API được cập nhật.
7. Có thể triển khai lại hệ thống từ mã nguồn và cấu hình được cung cấp mà không phụ thuộc vào thiết lập thủ công không được ghi chép.
8. AI không tự động ghi đè lịch trình, đầu ra được kiểm tra và hệ thống vẫn sử dụng được khi AI không khả dụng.

## 3.7. Giới hạn chất lượng trong phạm vi MVP

- Các ngưỡng hiệu năng là mục tiêu trong môi trường thử nghiệm, không phải cam kết dịch vụ thương mại.
- Hệ thống chưa đặt mục tiêu tính sẵn sàng cao đa vùng hoặc vận hành liên tục 24/7.
- Khả năng chịu tải được đánh giá ở quy mô nhỏ, chưa đại diện cho hàng nghìn người dùng đồng thời.
- Chế độ hoạt động ngoại tuyến đầy đủ chưa thuộc phạm vi MVP; ứng dụng chỉ cần xử lý mất mạng an toàn và thông báo rõ ràng.
- iOS chỉ được xem là nền tảng được hỗ trợ sau khi có thiết bị hoặc môi trường kiểm thử phù hợp.
- Chất lượng dữ liệu bản đồ, thời tiết và AI phụ thuộc một phần vào nhà cung cấp; Tripzy chịu trách nhiệm kiểm tra, thông báo và cung cấp phương án dự phòng trong phạm vi có thể kiểm soát.
