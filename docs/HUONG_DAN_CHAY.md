# HƯỚNG DẪN CÀI ĐẶT & CHẠY DỰ ÁN BT09 LẬP TRÌNH WEB

Tài liệu này hướng dẫn chi tiết cách cài đặt môi trường, thiết lập cơ sở dữ liệu, cấu hình biến môi trường và chạy từng dự án độc lập trong bài tập lớn BT09.

---

## 1. Yêu cầu Môi trường & Phần mềm

1. **Java Development Kit (JDK)**:
   - JDK 17, JDK 21 hoặc JDK 24/26.
   - Kiểm tra phiên bản:
     ```powershell
     java -version
     ```
2. **Apache Maven**:
   - Phiên bản Maven 3.8+ (khuyến nghị 3.9+).
   - Kiểm tra phiên bản:
     ```powershell
     mvn -version
     ```
3. **Hệ quản trị CSDL Microsoft SQL Server**:
   - SQL Server 2017+ hoặc SQL Server Express đang lắng nghe tại cổng `1433`.
   - Bật chế độ xác thực SQL Server and Windows Authentication mode (Mixed Mode).
   - Đảm bảo tài khoản `sa` đã được kích hoạt.

---

## 2. Khởi tạo Cơ sở Dữ liệu

Mở SQL Server Management Studio (SSMS) hoặc sqlcmd và thực thi:

```sql
-- Database cho ViDu1: LoginEmail
CREATE DATABASE webst4;
GO

-- Database cho ViDu1: LoginUsernameOrEmail và ViDu2
CREATE DATABASE webst9;
GO

-- Database cho ViDu3: Users, Roles, OtpTokens, Products
CREATE DATABASE webst3;
GO
```

---

## 3. Cấu trúc Thư mục Dự án

```
BT09_laptrinh_web/
├── .gitignore
├── README.md
├── docs/
│   └── HUONG_DAN_CHAY.md
├── ViDu1/
│   ├── README.md
│   ├── LoginEmail/
│   │   ├── pom.xml
│   │   ├── .env.example
│   │   └── src/...
│   └── LoginUsernameOrEmail/
│       ├── pom.xml
│       ├── .env.example
│       └── src/...
├── ViDu2/
│   ├── README.md
│   ├── pom.xml
│   ├── .env.example
│   └── src/...
└── ViDu3/
    ├── README.md
    ├── pom.xml
    ├── .env.example
    └── src/...
```

Mỗi thư mục dự án (`ViDu1/LoginEmail`, `ViDu1/LoginUsernameOrEmail`, `ViDu2`, `ViDu3`) là một **Maven project độc lập**, có thể mở trực tiếp bằng bất kỳ IDE nào (IntelliJ IDEA, Eclipse, VS Code) hoặc chạy bằng dòng lệnh `mvn`.

---

## 4. Hướng dẫn Chạy Từng Dự Án

### 4.1. Dự án ViDu1 - Nhánh 1: LoginEmail
- **Mục tiêu**: Đăng nhập bằng Email, hiển thị thông tin ở header, Thymeleaf không dùng Layout Dialect.
- **Port mặc định**: `8080`
- **Database**: `webst4`

**Các bước chạy:**
```powershell
cd D:\BT09_laptrinh_web\ViDu1\LoginEmail
copy .env.example .env
mvn clean test
mvn spring-boot:run
```
- Truy cập: [http://localhost:8080/](http://localhost:8080/)
- Tài khoản mẫu khởi tạo tự động:
  - Admin: `admin@example.com` / `123456`
  - User: `trungnh@example.com` / `123456`

---

### 4.2. Dự án ViDu1 - Nhánh 2: LoginUsernameOrEmail
- **Mục tiêu**: Đăng nhập linh hoạt bằng Username hoặc Email, hiển thị họ tên (`fullname`) và ảnh đại diện (`images`) ở header, sử dụng `thymeleaf-layout-dialect`.
- **Port mặc định**: `8081`
- **Database**: `webst9`

**Các bước chạy:**
```powershell
cd D:\BT09_laptrinh_web\ViDu1\LoginUsernameOrEmail
copy .env.example .env
mvn clean test
mvn spring-boot:run
```
- Truy cập: [http://localhost:8081/](http://localhost:8081/)
- Tài khoản mẫu khởi tạo tự động:
  - Admin: `admin` hoặc `admin@example.com` / `123456`
  - User: `trungnh` hoặc `trungnh@example.com` / `123456`

---

### 4.3. Dự án ViDu2 - Custom Login Username hoặc Email
- **Mục tiêu**: Hoàn thiện toàn bộ các bước và mã nguồn theo tài liệu `ViDu2.pdf` (UserDetailsService, CustomUserDetails, header avatar & fullname, Thymeleaf layout dialect).
- **Port mặc định**: `8082`
- **Database**: `webst9`

**Các bước chạy:**
```powershell
cd D:\BT09_laptrinh_web\ViDu2
copy .env.example .env
mvn clean test
mvn spring-boot:run
```
- Truy cập: [http://localhost:8082/](http://localhost:8082/)
- Tài khoản mẫu khởi tạo tự động:
  - Admin: `admin` hoặc `admin@example.com` / `123456`
  - User: `trungnh` hoặc `trungnh@example.com` / `123456`

---

### 4.4. Dự án ViDu3 - Hệ thống Users, Roles, OTP Email, Products & Cloudinary
- **Mục tiêu**: Ứng dụng quản trị toàn diện theo `ViDu3.pdf`:
  - Đăng ký tài khoản, gửi và xác thực mã OTP qua Email.
  - Quên mật khẩu xác thực OTP.
  - Quản lý người dùng (CRUD, tìm kiếm, phân trang, khóa tài khoản).
  - Quản lý sản phẩm (CRUD, tìm kiếm, phân trang).
  - **Server-side Ownership Enforcement**: Người dùng chỉ được sửa/xóa sản phẩm của mình; Admin quản lý tất cả.
  - Tải ảnh sản phẩm và người dùng lên Cloudinary.
- **Port mặc định**: `8083`
- **Database**: `webst3`

**Các bước chạy:**
```powershell
cd D:\BT09_laptrinh_web\ViDu3
copy .env.example .env
mvn clean test
mvn spring-boot:run
```
- Truy cập: [http://localhost:8083/](http://localhost:8083/)
- Tài khoản mẫu:
  - Admin: `admin` / `123456`
  - User: `trungnh` / `123456`

> [!NOTE]
> Để thử nghiệm gửi email OTP thật ra hộp thư ngoài Internet, hãy cung cấp `SPRING_MAIL_USERNAME` và `SPRING_MAIL_PASSWORD` (App Password Gmail) vào file `.env`.
> Để thử nghiệm upload ảnh trực tiếp lên Cloudinary, hãy cung cấp `CLOUDINARY_CLOUD_NAME`, `CLOUDINARY_API_KEY`, và `CLOUDINARY_API_SECRET` vào file `.env`.
