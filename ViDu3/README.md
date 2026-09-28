# ViDu3 - Quản lý Người dùng, Xác thực OTP Email, Sản phẩm & Tải ảnh Cloudinary

Dự án hoàn chỉnh triển khai theo yêu cầu của tài liệu `ViDu3.pdf` trong BT09 Lập trình Web.

---

## 1. Giới thiệu & Tính năng chính

- **Spring Boot 4.1.1 (Java 26 / Java 17+)** & **Spring Security 6**.
- **Xác thực & Phân quyền**:
  - Đăng ký tài khoản (`ROLE_USER`) gửi OTP 6 số qua email.
  - Xác thực OTP kích hoạt tài khoản trong vòng 5 phút (tối đa 5 lần nhập sai).
  - Đăng nhập qua form, lưu phiên làm việc (Session).
  - Quên mật khẩu: Gửi OTP xác thực reset mật khẩu an toàn, không lộ OTP ra log console/HTML.
  - Mật khẩu mã hóa BCrypt.
  - Bảo vệ chống tấn công CSRF trên toàn bộ form POST.
- **Quản lý Người dùng (Admin only)**:
  - Xem danh sách người dùng, tìm kiếm theo username/email/fullname.
  - Phân trang, tạo người dùng mới, gán role, cập nhật, xóa, khóa/mở khóa tài khoản (`enabled`).
- **Quản lý Sản phẩm (Product CRUD & Phân quyền sở hữu)**:
  - Xem danh sách sản phẩm, tìm kiếm theo tên, phân trang, lọc theo trạng thái hoạt động.
  - Người dùng thông thường (`ROLE_USER`) chỉ được phép xem tất cả, thêm mới sản phẩm và **chỉ được sửa / xóa các sản phẩm do chính mình tạo ra** (`owner`).
  - Quản trị viên (`ROLE_ADMIN`) có quyền sửa / xóa mọi sản phẩm trong hệ thống.
  - Cơ chế kiểm tra quyền sở hữu được thực thi ở tầng Server-side (`ProductService` & `ProductController`), ngăn chặn triệt để hành vi bypass qua URL hay ID injection.
- **Tải ảnh Cloudinary**:
  - Tải ảnh sản phẩm và avatar người dùng lên dịch vụ Cloudinary.
  - Hỗ trợ lưu trữ fallback cục bộ hoặc mock an toàn khi chưa cung cấp khóa API Cloudinary.

---

## 2. Yêu cầu môi trường

- **Java Development Kit (JDK)**: JDK 17, 21, 24 hoặc 26.
- **Apache Maven**: 3.8+ (khuyến nghị 3.9+).
- **Microsoft SQL Server**: Bản 2017 trở lên hoặc SQL Server Express (đang chạy cổng mặc định `1433`).
- **Cơ sở dữ liệu**: Tên database `webst3`.

---

## 3. Cấu hình biến môi trường (`.env`)

Sao chép file `.env.example` thành `.env` hoặc cấu hình các biến môi trường trong hệ thống:

```bash
cp .env.example .env
```

Nội dung `.env`:
```env
# Server Configuration
SERVER_PORT=8083

# Microsoft SQL Server Database Configuration
SPRING_DATASOURCE_URL=jdbc:sqlserver://localhost:1433;databaseName=webst3;encrypt=true;trustServerCertificate=true
SPRING_DATASOURCE_USERNAME=sa
SPRING_DATASOURCE_PASSWORD=YourPasswordHere

# JPA Hibernate Configuration
SPRING_JPA_HIBERNATE_DDL_AUTO=update
SPRING_JPA_SHOW_SQL=true

# Spring Mail (SMTP Gmail) Configuration
SPRING_MAIL_HOST=smtp.gmail.com
SPRING_MAIL_PORT=587
SPRING_MAIL_USERNAME=your_email@gmail.com
SPRING_MAIL_PASSWORD=your_app_password
SPRING_MAIL_PROPERTIES_MAIL_SMTP_AUTH=true
SPRING_MAIL_PROPERTIES_MAIL_SMTP_STARTTLS_ENABLE=true

# Cloudinary Storage Configuration
CLOUDINARY_CLOUD_NAME=your_cloud_name
CLOUDINARY_API_KEY=your_api_key
CLOUDINARY_API_SECRET=your_api_secret
```

> [!WARNING]
> **Lưu ý về Dịch vụ Bên thứ ba (SMTP & Cloudinary)**:
> - **Email SMTP thực tế**: Cần cấu hình `SPRING_MAIL_USERNAME` và `SPRING_MAIL_PASSWORD` (Mật khẩu ứng dụng Gmail 16 ký tự). Nếu chưa cấu hình, hệ thống sẽ log cảnh báo và không thể gửi email ra Internet (trạng thái: **BLOCKED** đối với gửi thư thật).
> - **Cloudinary**: Cần tạo tài khoản trên [cloudinary.com](https://cloudinary.com) và điền 3 thông số API. Nếu không điền, chức năng upload ảnh sản phẩm từ xa sẽ ghi nhận trạng thái **BLOCKED**; hệ thống vẫn tự động gán đường dẫn ảnh placeholder/mặc định để toàn bộ quy trình CRUD sản phẩm hoạt động bình thường.

---

## 4. Khởi tạo Cơ sở dữ liệu & Tài khoản mẫu

1. Tạo database trên SQL Server:
```sql
CREATE DATABASE webst3;
GO
```

2. Khi chạy ứng dụng, Hibernate với `ddl-auto=update` sẽ tự động tạo các bảng:
   - `roles`
   - `users`
   - `user_roles`
   - `otp_tokens`
   - `products`

3. Ứng dụng tự động seed 2 tài khoản mặc định kèm vai trò khi khởi động lần đầu:
   - **Tài khoản Quản trị (Admin)**:
     - Username: `admin`
     - Email: `admin@example.com`
     - Mật khẩu: `123456`
     - Vai trò: `ROLE_ADMIN`, `ROLE_USER`
   - **Tài khoản Người dùng (User)**:
     - Username: `trungnh`
     - Email: `trungnh@example.com`
     - Mật khẩu: `123456`
     - Vai trò: `ROLE_USER`

---

## 5. Hướng dẫn chạy ứng dụng

Từ thư mục `ViDu3/`:

### Chạy kiểm thử tự động (Unit & Integration Tests)
```bash
mvn clean test
```

### Khởi chạy ứng dụng Web
```bash
mvn spring-boot:run
```

Truy cập trình duyệt:
- Trang chủ: [http://localhost:8083/](http://localhost:8083/)
- Đăng nhập: [http://localhost:8083/auth/login](http://localhost:8083/auth/login)
- Đăng ký: [http://localhost:8083/auth/register](http://localhost:8083/auth/register)
- Quên mật khẩu: [http://localhost:8083/auth/forgot-password](http://localhost:8083/auth/forgot-password)
- Danh sách sản phẩm: [http://localhost:8083/products](http://localhost:8083/products)
- Quản lý người dùng (Admin): [http://localhost:8083/users](http://localhost:8083/users)

---

## 6. Checklist kiểm thử & Yêu cầu đề bài

| STT | Tính năng | Trạng thái | Ghi chú |
|---|---|---|---|
| 1 | Đăng ký tài khoản mới & sinh OTP | Hoàn thành | Sinh mã OTP 6 số, hết hạn sau 5 phút |
| 2 | Xác thực OTP kích hoạt tài khoản | Hoàn thành | Đếm số lần nhập sai (khóa sau 5 lần) |
| 3 | Đăng nhập form session & Remember me | Hoàn thành | Kiểm tra trạng thái `enabled` của user |
| 4 | Quên mật khẩu & Reset bằng OTP | Hoàn thành | Xác nhận OTP hợp lệ mới cho phép đổi mật khẩu |
| 5 | Quản lý Users (Admin) | Hoàn thành | CRUD, tìm kiếm, phân trang, toggle lock |
| 6 | Quản lý Products (CRUD, Search, Paging) | Hoàn thành | Hỗ trợ tìm kiếm theo tên, phân trang JPA |
| 7 | Server-side Ownership check cho Products | Hoàn thành | User thường chỉ sửa/xóa sản phẩm của chính mình; Admin can thiệp tất cả |
| 8 | Cloudinary image upload | Hoàn thành | Tích hợp Cloudinary SDK; fallback URL an toàn nếu chưa có API key |
| 9 | Kiểm thử tự động (Integration Test) | Hoàn thành | Bộ test suite H2/MockMvc mô phỏng đầy đủ chu trình xác thực và phân quyền |
