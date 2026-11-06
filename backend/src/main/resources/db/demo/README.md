# Dữ liệu mẫu — chỉ chạy chủ động trên database dev

Các file ở đây không nằm trong Flyway location mặc định `db/migration` và không có version Flyway. Chúng không chạy khi backend khởi động hoặc khi deploy production.

1. Dùng database dev riêng và chạy Flyway tới V16 trước.
2. Chạy từ thư mục `backend`, sử dụng kết nối PostgreSQL dev của bạn:

```powershell
psql --dbname=travel_demo --username=postgres --single-transaction --set=ON_ERROR_STOP=1 --file=src/main/resources/db/demo/seed_local_demo.sql
```

`seed_local_demo.sql` cung cấp bộ demo 50 tài khoản/chuyến đi và album; kiểm tra tính nhất quán, không ghi đè bản demo đã sửa và rollback cả script khi va chạm ID/email. Các chuyến này là dữ liệu minh họa, không phải dữ liệu đã được API tạo. Script đã cập nhật các cột lịch trình mới; nghiệm thu Trip CRUD/day metadata riêng trước khi dùng bộ demo làm bằng chứng tính năng.

`seed_memory_examples.sql` giữ bộ ví dụ Memory cũ từ nhánh nguồn để chạy thủ công khi cần. Không chạy hai bộ seed vào database có dữ liệu quan trọng hoặc production. Không dùng seed để thay việc test đăng ký/xác thực email/login thật.

Không đưa lại các file này vào `db/migration`; không thêm chúng vào `spring.sql.init`.
