# 2. YÊU CẦU CHỨC NĂNG HỆ THỐNG TRIPZY

**Phiên bản:** 1.0  
**Phạm vi:** Sản phẩm khả dụng tối thiểu (MVP)  
**Đối tượng áp dụng:** Ứng dụng web, ứng dụng di động và hệ thống backend Tripzy

---

## 2.1. Mục đích

Phần này mô tả các chức năng mà hệ thống Tripzy phải cung cấp để hỗ trợ người dùng lập kế hoạch, quản lý, thực hiện và lưu giữ trải nghiệm du lịch cá nhân. Các yêu cầu được trình bày theo từng nhóm chức năng, có mã định danh và mức độ ưu tiên để thuận tiện cho việc thiết kế, phát triển, kiểm thử và truy vết.

Tài liệu chỉ mô tả hành vi nghiệp vụ của hệ thống, không quy định chi tiết cấu trúc cơ sở dữ liệu, giao diện lập trình ứng dụng (API) hoặc cách cài đặt mã nguồn.

## 2.2. Quy ước và thuật ngữ

### 2.2.1. Mức độ ưu tiên

| Mức độ | Ý nghĩa |
|---|---|
| Must | Bắt buộc có trong MVP để hệ thống thực hiện được quy trình chính. |
| Should | Quan trọng nhưng có thể hoàn thiện sau khi các chức năng Must đã ổn định. |
| Could | Chức năng bổ sung, chỉ thực hiện khi còn đủ thời gian và nguồn lực. |

### 2.2.2. Thuật ngữ nghiệp vụ

| Thuật ngữ | Giải thích |
|---|---|
| Chuyến đi | Kế hoạch du lịch có thời gian, điểm đến, sở thích, phương tiện và các yêu cầu liên quan. |
| Lịch trình | Kế hoạch các hoạt động và địa điểm được tổ chức theo ngày và thời gian. |
| Địa điểm lịch trình | Một địa điểm cụ thể được thêm vào một ngày của lịch trình. |
| Thứ tự tham quan | Trình tự ghé thăm các địa điểm do người dùng thiết lập hoặc thuật toán đề xuất. |
| Tuyến đường | Đường di chuyển thực tế giữa hai địa điểm do dịch vụ bản đồ cung cấp. |
| Chặng di chuyển | Phần di chuyển giữa hai địa điểm liên tiếp trong lịch trình. |
| Memory | Bản ghi trải nghiệm gồm hình ảnh và ghi chú, được liên kết với chuyến đi và địa điểm. |
| Check-in | Hành động ghi nhận người dùng đã có mặt tại một địa điểm dựa trên vị trí thiết bị hoặc xác nhận chủ động. |

## 2.3. Tác nhân của hệ thống

| Mã | Tác nhân | Vai trò |
|---|---|---|
| ACT-01 | Khách | Người chưa đăng nhập, có thể đăng ký hoặc đăng nhập vào hệ thống. |
| ACT-02 | Người dùng | Người đã đăng nhập, có quyền quản lý dữ liệu cá nhân, chuyến đi, lịch trình và trải nghiệm của chính mình. |
| ACT-03 | Dịch vụ bản đồ và địa điểm | Cung cấp dữ liệu địa điểm, tọa độ, khoảng cách, thời gian di chuyển và tuyến đường. |
| ACT-04 | Dịch vụ thời tiết | Cung cấp thông tin dự báo thời tiết theo vị trí và thời gian. |
| ACT-05 | Dịch vụ AI | Phân tích ngữ cảnh, đưa ra đề xuất và giải thích lịch trình. |
| ACT-06 | Dịch vụ lưu trữ hình ảnh | Lưu trữ và cung cấp đường dẫn truy cập hình ảnh của Memory. |
| ACT-07 | Admin | Người có vai trò quản trị, được phép quản lý trạng thái tài khoản, dữ liệu danh mục dùng chung và xem thông tin vận hành tổng hợp trong phạm vi được cấp quyền. |

## 2.4. Phân rã chức năng

| Nhóm | Tên nhóm chức năng | Mục tiêu chính |
|---|---|---|
| FR-AUTH | Tài khoản và xác thực | Quản lý đăng ký, đăng nhập và bảo vệ tài khoản. |
| FR-PROFILE | Hồ sơ người dùng | Quản lý thông tin và tùy chọn cá nhân. |
| FR-PLACE | Khám phá địa điểm | Tìm kiếm, xem và lựa chọn địa điểm. |
| FR-TRIP | Quản lý chuyến đi | Tạo và quản lý vòng đời chuyến đi. |
| FR-ITIN | Quản lý lịch trình | Tổ chức địa điểm theo ngày và thời gian. |
| FR-OPT | Sắp xếp thứ tự tham quan | Đề xuất thứ tự địa điểm bằng thuật toán. |
| FR-CONTEXT | Bản đồ và thời tiết | Cung cấp dữ liệu không gian, định tuyến và thời tiết. |
| FR-AI | AI hỗ trợ lịch trình | Cá nhân hóa, cảnh báo và giải thích đề xuất. |
| FR-JOURNEY | Thực hiện chuyến đi | Hỗ trợ GPS, check-in và theo dõi tiến độ. |
| FR-MEMORY | Lưu giữ trải nghiệm | Quản lý hình ảnh, ghi chú và nhật ký hành trình. |
| FR-EXPLORE | Hồ sơ khám phá | Thống kê và trực quan hóa tiến trình khám phá. |
| FR-ADMIN | Quản trị hệ thống | Quản lý tài khoản, danh mục dùng chung và thông tin tổng quan của hệ thống. |

## 2.5. Yêu cầu chức năng chi tiết

### 2.5.1. Tài khoản và xác thực

| Mã | Yêu cầu chức năng | Mô tả | Kênh | Ưu tiên |
|---|---|---|---|---|
| FR-AUTH-01 | Đăng ký tài khoản | Hệ thống phải cho phép khách đăng ký bằng các thông tin bắt buộc như tên hiển thị, email và mật khẩu. | Web/Mobile | Must |
| FR-AUTH-02 | Kiểm tra dữ liệu đăng ký | Hệ thống phải kiểm tra định dạng email, độ mạnh mật khẩu, trường bắt buộc và tính duy nhất của email trước khi tạo tài khoản. | Backend | Must |
| FR-AUTH-03 | Đăng nhập | Hệ thống phải xác thực thông tin đăng nhập và cấp phiên hoặc mã truy cập hợp lệ cho người dùng. | Web/Mobile | Must |
| FR-AUTH-04 | Xử lý đăng nhập thất bại | Hệ thống phải từ chối yêu cầu khi thông tin không hợp lệ và trả về thông báo không làm lộ việc email có tồn tại hay không. | Backend | Must |
| FR-AUTH-05 | Duy trì phiên đăng nhập | Hệ thống phải cho phép người dùng tiếp tục phiên làm việc hợp lệ và yêu cầu đăng nhập lại khi phiên hết hạn hoặc bị thu hồi. | Web/Mobile | Must |
| FR-AUTH-06 | Đăng xuất | Hệ thống phải cho phép người dùng kết thúc phiên đăng nhập trên thiết bị hiện tại. | Web/Mobile | Must |
| FR-AUTH-07 | Đổi mật khẩu | Hệ thống phải cho phép người dùng đổi mật khẩu sau khi xác minh mật khẩu hiện tại hoặc thực hiện bước xác minh tương đương. | Web/Mobile | Must |
| FR-AUTH-08 | Khôi phục mật khẩu | Hệ thống nên hỗ trợ gửi yêu cầu đặt lại mật khẩu qua kênh xác minh đã đăng ký. | Web/Mobile | Should |
| FR-AUTH-09 | Vô hiệu hóa tài khoản | Hệ thống nên cho phép người dùng yêu cầu vô hiệu hóa tài khoản và ngăn đăng nhập sau khi yêu cầu có hiệu lực. | Web/Mobile | Should |

**Điều kiện nghiệm thu chính**

- Không tạo được hai tài khoản có cùng email sau khi chuẩn hóa.
- Mật khẩu không được lưu dưới dạng văn bản thuần.
- Người dùng chưa xác thực không thể truy cập dữ liệu chuyến đi, lịch trình hoặc Memory riêng tư.
- Sau khi đăng xuất hoặc phiên hết hạn, mã truy cập không còn được dùng để thực hiện thao tác được bảo vệ.

### 2.5.2. Hồ sơ và tùy chọn người dùng

| Mã | Yêu cầu chức năng | Mô tả | Kênh | Ưu tiên |
|---|---|---|---|---|
| FR-PROFILE-01 | Xem hồ sơ | Hệ thống phải cho phép người dùng xem thông tin hồ sơ của chính mình. | Web/Mobile | Must |
| FR-PROFILE-02 | Cập nhật hồ sơ | Hệ thống phải cho phép cập nhật tên hiển thị, ảnh đại diện và các thông tin cá nhân được hỗ trợ. | Web/Mobile | Must |
| FR-PROFILE-03 | Quản lý sở thích du lịch | Hệ thống phải cho phép người dùng lựa chọn và cập nhật các nhóm sở thích như thiên nhiên, ẩm thực, văn hóa hoặc giải trí. | Web/Mobile | Must |
| FR-PROFILE-04 | Quản lý tùy chọn chung | Hệ thống nên cho phép lưu các tùy chọn như phương tiện thường dùng, mức độ hoạt động và khoảng thời gian tham quan mong muốn. | Web/Mobile | Should |
| FR-PROFILE-05 | Phân quyền dữ liệu cá nhân | Hệ thống phải bảo đảm người dùng chỉ được xem và thay đổi hồ sơ thuộc tài khoản của mình. | Backend | Must |

### 2.5.3. Khám phá và lựa chọn địa điểm

| Mã | Yêu cầu chức năng | Mô tả | Kênh | Ưu tiên |
|---|---|---|---|---|
| FR-PLACE-01 | Tìm kiếm địa điểm | Hệ thống phải cho phép tìm kiếm địa điểm theo từ khóa và khu vực thông qua nguồn dữ liệu địa điểm được tích hợp. | Web/Mobile | Must |
| FR-PLACE-02 | Lọc địa điểm | Hệ thống nên cho phép lọc kết quả theo loại địa điểm, khu vực hoặc tiêu chí được nguồn dữ liệu hỗ trợ. | Web/Mobile | Should |
| FR-PLACE-03 | Xem thông tin địa điểm | Hệ thống phải hiển thị tối thiểu tên, địa chỉ, tọa độ và thông tin mô tả có sẵn của địa điểm. | Web/Mobile | Must |
| FR-PLACE-04 | Hiển thị địa điểm trên bản đồ | Hệ thống phải cho phép xem vị trí địa điểm trên bản đồ. | Web/Mobile | Must |
| FR-PLACE-05 | Chọn địa điểm cho chuyến đi | Hệ thống phải cho phép thêm một địa điểm tìm thấy vào danh sách địa điểm dự kiến của chuyến đi. | Web/Mobile | Must |
| FR-PLACE-06 | Loại bỏ địa điểm khỏi danh sách | Hệ thống phải cho phép loại bỏ địa điểm chưa cần thiết khỏi chuyến đi, sau khi xử lý các liên kết lịch trình liên quan. | Web/Mobile | Must |
| FR-PLACE-07 | Xử lý dữ liệu địa điểm trùng lặp | Hệ thống phải nhận diện địa điểm theo định danh của nguồn dữ liệu hoặc tọa độ phù hợp để hạn chế thêm trùng trong cùng chuyến đi. | Backend | Must |
| FR-PLACE-08 | Lưu địa điểm yêu thích | Hệ thống có thể cho phép người dùng lưu địa điểm để sử dụng cho chuyến đi trong tương lai. | Web/Mobile | Could |

### 2.5.4. Quản lý chuyến đi

| Mã | Yêu cầu chức năng | Mô tả | Kênh | Ưu tiên |
|---|---|---|---|---|
| FR-TRIP-01 | Tạo chuyến đi | Hệ thống phải cho phép tạo chuyến đi với tên, điểm đến chính, ngày bắt đầu và ngày kết thúc. | Web/Mobile | Must |
| FR-TRIP-02 | Khai báo thông tin chuyến đi | Hệ thống phải cho phép bổ sung phương tiện, sở thích, ngân sách dự kiến, ghi chú và các yêu cầu cá nhân. | Web/Mobile | Must |
| FR-TRIP-03 | Kiểm tra thời gian chuyến đi | Hệ thống phải kiểm tra ngày kết thúc không trước ngày bắt đầu và các ngày lịch trình nằm trong thời gian chuyến đi. | Backend | Must |
| FR-TRIP-04 | Xem danh sách chuyến đi | Hệ thống phải cho phép người dùng xem các chuyến đi của mình, được phân loại hoặc lọc theo trạng thái. | Web/Mobile | Must |
| FR-TRIP-05 | Xem chi tiết chuyến đi | Hệ thống phải hiển thị thông tin tổng quan, danh sách địa điểm, lịch trình, tiến độ và Memory liên quan. | Web/Mobile | Must |
| FR-TRIP-06 | Cập nhật chuyến đi | Hệ thống phải cho phép sửa thông tin chuyến đi khi trạng thái và quy tắc nghiệp vụ cho phép. | Web/Mobile | Must |
| FR-TRIP-07 | Thay đổi trạng thái | Hệ thống phải quản lý vòng đời chuyến đi theo các trạng thái: bản nháp, đã lập kế hoạch, đang diễn ra, đã hoàn thành và đã hủy. | Backend | Must |
| FR-TRIP-08 | Hủy chuyến đi | Hệ thống phải cho phép người dùng hủy chuyến đi và giữ lại dữ liệu theo chính sách lưu trữ của hệ thống. | Web/Mobile | Must |
| FR-TRIP-09 | Xóa chuyến đi | Hệ thống nên cho phép xóa chuyến đi sau bước xác nhận và xử lý an toàn dữ liệu lịch trình, check-in và Memory liên quan. | Web/Mobile | Should |
| FR-TRIP-10 | Bảo vệ quyền sở hữu | Hệ thống phải ngăn người dùng xem, sửa hoặc xóa chuyến đi không thuộc quyền sở hữu của mình. | Backend | Must |

### 2.5.5. Xây dựng và quản lý lịch trình

| Mã | Yêu cầu chức năng | Mô tả | Kênh | Ưu tiên |
|---|---|---|---|---|
| FR-ITIN-01 | Khởi tạo lịch trình | Hệ thống phải tạo hoặc cho phép tạo các ngày lịch trình tương ứng với khoảng thời gian chuyến đi. | Web/Mobile | Must |
| FR-ITIN-02 | Thêm địa điểm vào ngày | Hệ thống phải cho phép thêm địa điểm đã chọn vào một ngày cụ thể của lịch trình. | Web/Mobile | Must |
| FR-ITIN-03 | Thiết lập thời gian tham quan | Hệ thống phải cho phép nhập thời gian bắt đầu và thời lượng dự kiến tại từng địa điểm. | Web/Mobile | Must |
| FR-ITIN-04 | Chỉnh sửa mục lịch trình | Hệ thống phải cho phép thay đổi ngày, thời gian, thời lượng và ghi chú của một địa điểm lịch trình. | Web/Mobile | Must |
| FR-ITIN-05 | Xóa mục lịch trình | Hệ thống phải cho phép xóa một địa điểm khỏi lịch trình sau khi người dùng xác nhận khi cần thiết. | Web/Mobile | Must |
| FR-ITIN-06 | Sắp xếp thủ công | Hệ thống phải cho phép người dùng thay đổi thứ tự các địa điểm trong cùng một ngày. | Web/Mobile | Must |
| FR-ITIN-07 | Di chuyển địa điểm giữa các ngày | Hệ thống phải cho phép chuyển một địa điểm lịch trình sang ngày khác trong phạm vi chuyến đi. | Web/Mobile | Must |
| FR-ITIN-08 | Hiển thị chặng di chuyển | Hệ thống phải hiển thị khoảng cách và thời gian di chuyển dự kiến giữa các địa điểm liên tiếp khi dữ liệu khả dụng. | Web/Mobile | Must |
| FR-ITIN-09 | Phát hiện xung đột thời gian | Hệ thống phải cảnh báo khi các hoạt động bị chồng lấn hoặc tổng thời gian dự kiến vượt quá khung thời gian người dùng thiết lập. | Web/Mobile | Must |
| FR-ITIN-10 | Tính lại lịch trình | Khi thứ tự, thời gian hoặc địa điểm thay đổi, hệ thống phải cập nhật các chặng di chuyển và thời gian liên quan. | Backend | Must |
| FR-ITIN-11 | Xem lịch trình theo ngày | Hệ thống phải trình bày lịch trình theo từng ngày và theo đúng thứ tự thời gian. | Web/Mobile | Must |
| FR-ITIN-12 | Giữ quyền chỉnh sửa của người dùng | Hệ thống không được tự động ghi đè lịch trình hiện tại bằng kết quả thuật toán hoặc AI khi người dùng chưa xác nhận áp dụng. | Backend | Must |

### 2.5.6. Sắp xếp và tối ưu thứ tự tham quan

| Mã | Yêu cầu chức năng | Mô tả | Kênh | Ưu tiên |
|---|---|---|---|---|
| FR-OPT-01 | Thu thập dữ liệu chi phí | Hệ thống phải tạo ma trận chi phí giữa các địa điểm dựa trên khoảng cách hoặc thời gian di chuyển từ dịch vụ bản đồ. | Backend | Must |
| FR-OPT-02 | Chọn tiêu chí sắp xếp | Hệ thống phải cho phép xác định tiêu chí ưu tiên là tổng khoảng cách hoặc tổng thời gian di chuyển. | Web/Mobile | Must |
| FR-OPT-03 | Áp dụng Greedy | Hệ thống phải hỗ trợ thuật toán Greedy để nhanh chóng tạo một thứ tự tham quan khả thi. | Backend | Must |
| FR-OPT-04 | Áp dụng Held–Karp | Hệ thống phải hỗ trợ thuật toán Held–Karp để tìm thứ tự có tổng chi phí nhỏ nhất khi số địa điểm nằm trong ngưỡng xử lý được cấu hình. | Backend | Must |
| FR-OPT-05 | Lựa chọn thuật toán phù hợp | Hệ thống phải lựa chọn hoặc cho phép lựa chọn thuật toán dựa trên số lượng địa điểm và giới hạn tài nguyên xử lý. | Backend/Web | Must |
| FR-OPT-06 | Giữ điểm bắt đầu và kết thúc | Hệ thống phải hỗ trợ cố định điểm bắt đầu, điểm kết thúc hoặc cả hai khi người dùng có yêu cầu. | Web/Mobile | Must |
| FR-OPT-07 | Trả kết quả đề xuất | Hệ thống phải trả về thứ tự địa điểm đề xuất, tổng chi phí dự kiến và thuật toán đã sử dụng. | Backend | Must |
| FR-OPT-08 | So sánh với lịch trình hiện tại | Hệ thống nên hiển thị chênh lệch về tổng khoảng cách hoặc thời gian giữa phương án hiện tại và phương án đề xuất. | Web/Mobile | Should |
| FR-OPT-09 | Xác nhận áp dụng | Hệ thống phải cho phép người dùng xem trước và chủ động áp dụng hoặc bỏ qua kết quả sắp xếp. | Web/Mobile | Must |
| FR-OPT-10 | Xử lý thiếu dữ liệu | Nếu không lấy được đầy đủ ma trận chi phí, hệ thống phải thông báo rõ các địa điểm hoặc chặng thiếu dữ liệu và không được tuyên bố kết quả là tối ưu. | Backend/Web/Mobile | Must |

**Điều kiện nghiệm thu chính**

- Cùng một bộ dữ liệu đầu vào xác định phải tạo được kết quả có thể kiểm tra lại.
- Held–Karp chỉ chạy khi số địa điểm không vượt quá ngưỡng cấu hình đã được xác định bằng kiểm thử hiệu năng.
- Tổng chi phí hiển thị phải được tính theo cùng một tiêu chí và đơn vị đo.
- Người dùng có thể xem kết quả trước khi lịch trình hiện tại bị thay đổi.

### 2.5.7. Bản đồ, định tuyến và thời tiết

| Mã | Yêu cầu chức năng | Mô tả | Kênh | Ưu tiên |
|---|---|---|---|---|
| FR-CONTEXT-01 | Hiển thị bản đồ chuyến đi | Hệ thống phải hiển thị các địa điểm của chuyến đi trên bản đồ. | Web/Mobile | Must |
| FR-CONTEXT-02 | Hiển thị tuyến đường | Hệ thống phải hiển thị tuyến đường giữa các địa điểm liên tiếp khi dịch vụ bản đồ cung cấp dữ liệu. | Web/Mobile | Must |
| FR-CONTEXT-03 | Tính khoảng cách và thời gian | Hệ thống phải lấy hoặc tính khoảng cách và thời gian di chuyển dự kiến theo phương tiện đã chọn. | Backend | Must |
| FR-CONTEXT-04 | Mở chỉ đường | Ứng dụng di động nên cho phép mở chức năng chỉ đường bằng dịch vụ bản đồ được hỗ trợ. | Mobile | Should |
| FR-CONTEXT-05 | Lấy thông tin thời tiết | Hệ thống phải lấy dữ liệu thời tiết theo tọa độ và thời điểm dự kiến khi thời điểm đó nằm trong phạm vi dự báo của nhà cung cấp. | Backend | Must |
| FR-CONTEXT-06 | Hiển thị thời tiết | Hệ thống phải hiển thị các thông tin thời tiết cần thiết cho ngày hoặc địa điểm trong lịch trình. | Web/Mobile | Must |
| FR-CONTEXT-07 | Cảnh báo điều kiện bất lợi | Hệ thống nên cảnh báo khi dữ liệu cho thấy điều kiện thời tiết có thể ảnh hưởng đến hoạt động dự kiến. | Web/Mobile | Should |
| FR-CONTEXT-08 | Phân biệt dữ liệu chưa khả dụng | Hệ thống phải thông báo khi chuyến đi nằm ngoài phạm vi dự báo, thay vì hiển thị dữ liệu không chính xác như dự báo thực tế. | Web/Mobile | Must |
| FR-CONTEXT-09 | Xử lý lỗi dịch vụ ngoài | Hệ thống phải thông báo phù hợp và giữ dữ liệu người dùng đã nhập khi dịch vụ bản đồ hoặc thời tiết tạm thời không khả dụng. | Backend/Web/Mobile | Must |
| FR-CONTEXT-10 | Ghi nhận thời điểm dữ liệu | Hệ thống nên hiển thị hoặc lưu thời điểm cập nhật của dữ liệu thời tiết và định tuyến khi cần thiết. | Backend/Web/Mobile | Should |

### 2.5.8. AI hỗ trợ cá nhân hóa lịch trình

| Mã | Yêu cầu chức năng | Mô tả | Kênh | Ưu tiên |
|---|---|---|---|---|
| FR-AI-01 | Thu thập ngữ cảnh phù hợp | Hệ thống phải chuẩn bị dữ liệu cần thiết gồm lịch trình, kết quả thuật toán, sở thích, thời gian, thời tiết và yêu cầu cá nhân trước khi gửi yêu cầu đến AI. | Backend | Must |
| FR-AI-02 | Tạo đề xuất cá nhân hóa | Hệ thống phải cho phép yêu cầu AI đưa ra đề xuất điều chỉnh lịch trình dựa trên ngữ cảnh được cung cấp. | Web/Mobile | Must |
| FR-AI-03 | Giải thích đề xuất | Hệ thống phải yêu cầu và hiển thị lý do cho từng đề xuất theo cách người dùng có thể hiểu được. | Web/Mobile | Must |
| FR-AI-04 | Cảnh báo yếu tố ảnh hưởng | Hệ thống nên cho phép AI chỉ ra các yếu tố như thời tiết bất lợi, lịch trình quá dày hoặc thời gian di chuyển chưa hợp lý. | Web/Mobile | Should |
| FR-AI-05 | Nhận kết quả có cấu trúc | Backend phải kiểm tra cấu trúc, kiểu dữ liệu và các tham chiếu địa điểm trong phản hồi AI trước khi hiển thị hoặc áp dụng. | Backend | Must |
| FR-AI-06 | Xem trước thay đổi | Hệ thống phải cho phép người dùng xem các thay đổi AI đề xuất so với lịch trình hiện tại. | Web/Mobile | Must |
| FR-AI-07 | Áp dụng có chọn lọc | Hệ thống phải cho phép người dùng áp dụng, chỉnh sửa hoặc bỏ qua đề xuất; không tự động thay đổi lịch trình khi chưa được xác nhận. | Web/Mobile | Must |
| FR-AI-08 | Không bịa dữ liệu nghiệp vụ | Hệ thống không được tự động tạo địa điểm, khoảng cách, thời gian hoặc dữ liệu thời tiết không tồn tại trong nguồn dữ liệu đã xác thực. | Backend | Must |
| FR-AI-09 | Xử lý AI không khả dụng | Khi dịch vụ AI lỗi hoặc hết thời gian chờ, hệ thống phải giữ nguyên lịch trình và cho phép người dùng tiếp tục chỉnh sửa thủ công. | Backend/Web/Mobile | Must |
| FR-AI-10 | Thông báo tính tham khảo | Giao diện phải thể hiện rõ đề xuất AI chỉ mang tính hỗ trợ và người dùng chịu trách nhiệm quyết định cuối cùng. | Web/Mobile | Must |

### 2.5.9. Thực hiện và theo dõi chuyến đi

| Mã | Yêu cầu chức năng | Mô tả | Kênh | Ưu tiên |
|---|---|---|---|---|
| FR-JOURNEY-01 | Bắt đầu chuyến đi | Hệ thống phải cho phép người dùng chuyển chuyến đi hợp lệ sang trạng thái đang diễn ra. | Mobile | Must |
| FR-JOURNEY-02 | Xem lịch trình hôm nay | Ứng dụng di động phải hiển thị các địa điểm và hoạt động của ngày hiện tại theo thứ tự thực hiện. | Mobile | Must |
| FR-JOURNEY-03 | Xin quyền vị trí | Ứng dụng phải xin quyền truy cập vị trí theo cơ chế của thiết bị và giải thích mục đích sử dụng trước khi khai thác GPS. | Mobile | Must |
| FR-JOURNEY-04 | Xác định vị trí hiện tại | Hệ thống phải cho phép xác định vị trí hiện tại khi người dùng đã cấp quyền và thiết bị cung cấp dữ liệu hợp lệ. | Mobile | Must |
| FR-JOURNEY-05 | Check-in địa điểm | Hệ thống phải cho phép người dùng check-in tại địa điểm trong lịch trình và ghi nhận thời điểm thực hiện. | Mobile | Must |
| FR-JOURNEY-06 | Xác minh khoảng cách check-in | Khi có GPS, hệ thống phải so sánh vị trí hiện tại với tọa độ địa điểm theo bán kính được cấu hình và thông báo nếu người dùng ở ngoài phạm vi. | Backend/Mobile | Must |
| FR-JOURNEY-07 | Xác nhận thủ công | Hệ thống nên cho phép xác nhận thủ công trong trường hợp không thể lấy GPS, đồng thời ghi nhận phương thức xác nhận. | Mobile | Should |
| FR-JOURNEY-08 | Cập nhật trạng thái địa điểm | Hệ thống phải cho phép đánh dấu địa điểm là đã ghé thăm hoặc đã bỏ qua. | Mobile | Must |
| FR-JOURNEY-09 | Theo dõi tiến độ | Hệ thống phải tính và hiển thị tiến độ dựa trên số địa điểm đã ghé thăm so với tổng số địa điểm của chuyến đi. | Mobile | Must |
| FR-JOURNEY-10 | Điều chỉnh khi đang đi | Người dùng phải có thể thay đổi thứ tự, thời gian hoặc trạng thái địa điểm trong chuyến đi mà không làm mất dữ liệu đã ghi nhận. | Mobile | Must |
| FR-JOURNEY-11 | Hoàn thành chuyến đi | Hệ thống phải cho phép kết thúc chuyến đi, tổng hợp các địa điểm đã ghé thăm và chuyển trạng thái sang đã hoàn thành. | Mobile | Must |
| FR-JOURNEY-12 | Không theo dõi nền liên tục | Hệ thống không được thu thập vị trí liên tục ngoài nhu cầu của chức năng nếu người dùng chưa được thông báo và chưa cấp quyền phù hợp. | Mobile/Backend | Must |

### 2.5.10. Memory và nhật ký hành trình

| Mã | Yêu cầu chức năng | Mô tả | Kênh | Ưu tiên |
|---|---|---|---|---|
| FR-MEMORY-01 | Tạo Memory | Hệ thống phải cho phép người dùng tạo Memory gắn với một chuyến đi và địa điểm tương ứng. | Mobile/Web | Must |
| FR-MEMORY-02 | Thêm ghi chú | Hệ thống phải cho phép nhập và lưu nội dung ghi chú cá nhân cho Memory. | Mobile/Web | Must |
| FR-MEMORY-03 | Thêm hình ảnh | Hệ thống phải cho phép chọn hoặc chụp hình ảnh và tải lên Memory trong giới hạn định dạng, dung lượng và số lượng được cấu hình. | Mobile | Must |
| FR-MEMORY-04 | Theo dõi trạng thái tải ảnh | Hệ thống phải thông báo tiến trình, kết quả thành công hoặc lỗi của quá trình tải hình ảnh. | Mobile/Web | Must |
| FR-MEMORY-05 | Chỉnh sửa Memory | Hệ thống phải cho phép người dùng sửa ghi chú, hình ảnh và thông tin liên kết hợp lệ của Memory thuộc sở hữu của mình. | Mobile/Web | Must |
| FR-MEMORY-06 | Xóa Memory | Hệ thống phải cho phép xóa Memory sau bước xác nhận và xử lý các hình ảnh liên quan theo chính sách lưu trữ. | Mobile/Web | Must |
| FR-MEMORY-07 | Xem Memory theo chuyến đi | Hệ thống phải hiển thị danh sách Memory của một chuyến đi theo thời gian hoặc địa điểm. | Mobile/Web | Must |
| FR-MEMORY-08 | Xem chi tiết Memory | Hệ thống phải hiển thị đầy đủ hình ảnh, ghi chú, địa điểm và thời gian ghi nhận của Memory. | Mobile/Web | Must |
| FR-MEMORY-09 | Tạo dòng thời gian | Hệ thống phải tổng hợp các Memory theo trình tự thời gian để hình thành nhật ký hành trình. | Mobile/Web | Must |
| FR-MEMORY-10 | Bảo vệ quyền riêng tư | Hệ thống phải ngăn người dùng truy cập, chỉnh sửa hoặc xóa Memory không thuộc quyền sở hữu của mình. | Backend | Must |

### 2.5.11. Hồ sơ và tiến trình khám phá

| Mã | Yêu cầu chức năng | Mô tả | Kênh | Ưu tiên |
|---|---|---|---|---|
| FR-EXPLORE-01 | Tổng hợp số chuyến đi | Hệ thống phải thống kê số chuyến đi của người dùng theo trạng thái phù hợp. | Web/Mobile | Must |
| FR-EXPLORE-02 | Tổng hợp địa điểm đã khám phá | Hệ thống phải thống kê các địa điểm đã được ghi nhận là đã ghé thăm, tránh đếm trùng cùng một địa điểm. | Web/Mobile | Must |
| FR-EXPLORE-03 | Tổng hợp tỉnh/thành | Hệ thống phải thống kê số tỉnh/thành mà người dùng đã khám phá từ dữ liệu địa điểm hợp lệ. | Web/Mobile | Must |
| FR-EXPLORE-04 | Tổng hợp Memory và hình ảnh | Hệ thống phải thống kê số lượng Memory và hình ảnh thuộc tài khoản người dùng. | Web/Mobile | Must |
| FR-EXPLORE-05 | Hiển thị dòng thời gian khám phá | Hệ thống phải hiển thị các chuyến đi và Memory theo thời gian để người dùng xem lại tiến trình khám phá. | Web/Mobile | Must |
| FR-EXPLORE-06 | Hiển thị bản đồ hành trình | Hệ thống nên trực quan hóa các địa điểm hoặc khu vực đã khám phá trên bản đồ. | Web/Mobile | Should |
| FR-EXPLORE-07 | Xem lại địa điểm đã đến | Hệ thống phải cho phép mở chi tiết chuyến đi hoặc Memory liên quan từ danh sách địa điểm đã khám phá. | Web/Mobile | Must |
| FR-EXPLORE-08 | Cập nhật số liệu | Số liệu hồ sơ phải được cập nhật sau khi chuyến đi, trạng thái địa điểm hoặc Memory thay đổi. | Backend | Must |

### 2.5.12. Quản trị hệ thống

| Mã | Yêu cầu chức năng | Mô tả | Kênh | Ưu tiên |
|---|---|---|---|---|
| FR-ADMIN-01 | Xác thực quyền quản trị | Hệ thống phải xác thực tài khoản và vai trò Admin trước khi cho phép truy cập chức năng quản trị. | Backend/Web | Must |
| FR-ADMIN-02 | Tra cứu tài khoản người dùng | Hệ thống phải cho phép Admin xem danh sách, tìm kiếm và xem thông tin quản trị cần thiết của tài khoản người dùng. | Web | Must |
| FR-ADMIN-03 | Khóa tài khoản | Hệ thống phải cho phép Admin khóa tài khoản người dùng có lý do và ngăn tài khoản tiếp tục truy cập sau khi quyết định có hiệu lực. | Web/Backend | Must |
| FR-ADMIN-04 | Mở khóa tài khoản | Hệ thống phải cho phép Admin mở khóa tài khoản khi nguyên nhân hạn chế đã được xử lý. | Web/Backend | Must |
| FR-ADMIN-05 | Quản lý danh mục dùng chung | Hệ thống nên cho phép Admin thêm, cập nhật hoặc vô hiệu hóa các danh mục như sở thích và loại địa điểm do Tripzy quản lý. | Web | Should |
| FR-ADMIN-06 | Xem thống kê tổng hợp | Hệ thống nên cho phép Admin xem số liệu tổng hợp về tài khoản, chuyến đi và mức sử dụng chức năng mà không hiển thị nội dung riêng tư không cần thiết. | Web | Should |
| FR-ADMIN-07 | Xem trạng thái tích hợp | Hệ thống nên cho phép Admin xem trạng thái khả dụng tổng quan của các dịch vụ bản đồ, thời tiết, AI và lưu trữ hình ảnh. | Web | Should |
| FR-ADMIN-08 | Ghi nhận thao tác quản trị | Hệ thống phải ghi nhận Admin, thời điểm, loại thao tác, đối tượng bị tác động và lý do đối với các hành động quản trị quan trọng. | Backend | Must |
| FR-ADMIN-09 | Bảo vệ dữ liệu riêng tư | Hệ thống phải ngăn Admin truy cập nội dung chuyến đi, vị trí và Memory riêng tư nếu không có nghiệp vụ và quyền truy cập được quy định riêng. | Backend | Must |

## 2.6. Quy tắc nghiệp vụ

| Mã | Quy tắc |
|---|---|
| BR-01 | Mỗi tài khoản phải có một định danh duy nhất; email sau khi chuẩn hóa không được trùng lặp. |
| BR-02 | Người dùng chỉ được quản lý chuyến đi, lịch trình, check-in và Memory thuộc tài khoản của mình. |
| BR-03 | Ngày kết thúc chuyến đi phải bằng hoặc sau ngày bắt đầu. |
| BR-04 | Ngày của lịch trình phải nằm trong khoảng thời gian chuyến đi. |
| BR-05 | Trạng thái chuyến đi gồm: bản nháp, đã lập kế hoạch, đang diễn ra, đã hoàn thành và đã hủy. Việc chuyển trạng thái phải tuân theo luồng hợp lệ. |
| BR-06 | Một địa điểm không nên xuất hiện nhiều lần trong cùng một ngày, trừ khi người dùng chủ động xác nhận nhu cầu quay lại. |
| BR-07 | Dữ liệu khoảng cách và thời gian dùng cho thuật toán phải gắn với phương tiện và thời điểm cập nhật phù hợp. |
| BR-08 | Held–Karp chỉ được sử dụng khi số địa điểm không vượt quá ngưỡng cấu hình; khi vượt ngưỡng, hệ thống sử dụng Greedy hoặc phương án phù hợp khác. |
| BR-09 | Kết quả Held–Karp chỉ được xem là tối ưu đối với ma trận chi phí và các ràng buộc đã đưa vào bài toán, không đồng nghĩa với tối ưu mọi yếu tố thực tế của chuyến đi. |
| BR-10 | Tuyến đường thực tế do dịch vụ bản đồ cung cấp; thuật toán của Tripzy chỉ quyết định thứ tự ghé thăm các địa điểm. |
| BR-11 | Mọi kết quả do thuật toán hoặc AI đề xuất phải được người dùng xác nhận trước khi thay đổi lịch trình đang lưu. |
| BR-12 | AI chỉ sử dụng dữ liệu được hệ thống cung cấp và không được xem là nguồn xác thực cho khoảng cách, tuyến đường hoặc thời tiết. |
| BR-13 | Dữ liệu thời tiết chỉ được hiển thị như dự báo khi thời điểm chuyến đi nằm trong phạm vi dự báo của nhà cung cấp. |
| BR-14 | Check-in bằng GPS phải sử dụng bán kính hợp lệ được cấu hình; nếu xác nhận thủ công được cho phép, hệ thống phải phân biệt hai phương thức. |
| BR-15 | Một địa điểm chỉ được tính là đã khám phá khi có trạng thái đã ghé thăm hoặc check-in hợp lệ theo quy tắc hệ thống. |
| BR-16 | Một tỉnh/thành chỉ được tính một lần trong hồ sơ khám phá, dù người dùng có nhiều chuyến đi đến cùng khu vực. |
| BR-17 | Memory phải thuộc một người dùng và liên kết với chuyến đi; liên kết địa điểm là bắt buộc khi Memory được tạo tại một điểm tham quan cụ thể. |
| BR-18 | Thời gian phải được lưu và hiển thị theo múi giờ phù hợp với chuyến đi hoặc người dùng. |

## 2.7. Luồng nghiệp vụ tổng quát

1. Người dùng đăng ký hoặc đăng nhập vào hệ thống.
2. Người dùng tạo chuyến đi và nhập các thông tin, sở thích, thời gian và yêu cầu cá nhân.
3. Người dùng tìm kiếm và lựa chọn các địa điểm muốn khám phá.
4. Người dùng tự xây dựng lịch trình hoặc yêu cầu hệ thống sắp xếp thứ tự tham quan.
5. Hệ thống lấy dữ liệu khoảng cách, thời gian di chuyển và áp dụng thuật toán phù hợp.
6. AI phân tích kết quả cùng dữ liệu thời tiết và sở thích để đưa ra đề xuất, cảnh báo và giải thích.
7. Người dùng xem xét, áp dụng, chỉnh sửa hoặc bỏ qua đề xuất để chốt lịch trình.
8. Trong chuyến đi, người dùng xem lịch trình, sử dụng GPS, check-in và cập nhật trạng thái các địa điểm.
9. Người dùng tạo Memory bằng hình ảnh và ghi chú tại các địa điểm đã trải nghiệm.
10. Sau chuyến đi, hệ thống tổng hợp nhật ký, thống kê và cập nhật hồ sơ khám phá cá nhân.

## 2.8. Ma trận truy vết mục tiêu – nhóm chức năng

| Mục tiêu cụ thể | Nhóm yêu cầu liên quan |
|---|---|
| Quản lý người dùng và chuyến đi | FR-AUTH, FR-PROFILE, FR-TRIP |
| Tìm kiếm và lựa chọn địa điểm | FR-PLACE, FR-CONTEXT |
| Xây dựng và quản lý lịch trình | FR-ITIN |
| Sắp xếp thứ tự tham quan | FR-OPT, FR-CONTEXT |
| Cá nhân hóa bằng AI | FR-AI, FR-PROFILE, FR-CONTEXT |
| Theo dõi thực hiện chuyến đi | FR-JOURNEY |
| Lưu giữ trải nghiệm | FR-MEMORY |
| Xây dựng hồ sơ khám phá | FR-EXPLORE |
| Quản trị hệ thống | FR-ADMIN |

## 2.9. Phạm vi không bao gồm trong MVP

- Đặt và thanh toán vé máy bay, tàu, xe hoặc vé tham quan.
- Đặt phòng khách sạn và thanh toán dịch vụ lưu trú.
- Xây dựng hoặc kinh doanh tour du lịch.
- Chia sẻ chi phí, phân quyền thành viên hoặc lập kế hoạch cộng tác cho nhóm.
- Mạng xã hội du lịch, bảng tin công khai hoặc nhắn tin giữa người dùng.
- Theo dõi GPS liên tục trong nền khi không cần thiết cho chức năng người dùng chủ động sử dụng.
- Huấn luyện mô hình AI riêng hoặc xem đầu ra AI là quyết định bắt buộc.
- Tối ưu hạ tầng cho quy mô thương mại hoặc lượng người dùng đồng thời lớn.

## 2.10. Tiêu chí hoàn thành phạm vi chức năng MVP

Phạm vi chức năng MVP được xem là hoàn thành khi đáp ứng đồng thời các điều kiện sau:

1. Người dùng có thể đăng ký, đăng nhập và quản lý dữ liệu cá nhân an toàn.
2. Người dùng có thể tạo một chuyến đi, lựa chọn địa điểm và xây dựng lịch trình nhiều ngày.
3. Hệ thống có thể lấy dữ liệu khoảng cách hoặc thời gian và đề xuất thứ tự tham quan bằng Greedy hoặc Held–Karp theo điều kiện phù hợp.
4. Người dùng có thể xem trước và chủ động quyết định việc áp dụng kết quả thuật toán hoặc đề xuất AI.
5. Bản đồ, tuyến đường và dữ liệu thời tiết được hiển thị khi dịch vụ bên ngoài cung cấp dữ liệu hợp lệ.
6. Trên thiết bị di động, người dùng có thể xem lịch trình, sử dụng vị trí, check-in và cập nhật tiến độ chuyến đi.
7. Người dùng có thể tạo Memory bằng hình ảnh, ghi chú và xem lại nhật ký hành trình.
8. Hệ thống có thể tổng hợp các chỉ số cơ bản và trực quan hóa tiến trình khám phá cá nhân.
9. Các luồng chính, luồng lỗi quan trọng và quy tắc quyền sở hữu dữ liệu được kiểm thử đạt yêu cầu.
