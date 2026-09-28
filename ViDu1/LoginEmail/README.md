# Ví dụ 1: Login bằng Email (ViDu1/LoginEmail)

Dự án triển khai Ví dụ 1 theo tài liệu `ViDu1.pdf`: Cho bảng `User`, `Role`, viết chức năng đăng nhập bằng email, thông tin của user hiển thị ở `header.html`. Sử dụng Spring Boot 4.1.1, Spring Security (Spring Security 7 / Spring Security 6 specs), MapStruct, Thymeleaf (layout không dùng Dialect).

---

## 1. Thông tin công nghệ & phiên bản

- **Ngôn ngữ**: Java 26 (Oracle JDK 26.0.2.1 / release 26).
- **Build tool**: Apache Maven 3.9+.
- **Framework**: Spring Boot 4.1.1 (sử dụng starter `spring-boot-starter-webmvc`, `spring-boot-starter-security`, `spring-boot-starter-thymeleaf`, `spring-boot-starter-data-jpa`, `spring-boot-starter-validation`).
- **Thư viện ánh xạ DTO**: MapStruct 1.6.3 (`lombok-mapstruct-binding` 0.2.0, Lombok 1.18.46 tương thích JDK 26).
- **Template Engine**: Thymeleaf với `thymeleaf-extras-springsecurity6` (dựng layout bằng Thymeleaf fragments tiêu chuẩn, **không sử dụng** `thymeleaf-layout-dialect`).
- **Database**: Microsoft SQL Server (database `webst4`) và có sẵn cấu hình H2 in-memory phục vụ kiểm thử tự động.

---

## 2. Cấu trúc thư mục

```
ViDu1/LoginEmail/
├── pom.xml
├── .env.example
├── README.md
└── src/
    ├── main/
    │   ├── java/vn/iotstar/
    │   │   ├── Springboot18Application.java
    │   │   ├── config/
    │   │   │   ├── DataInitializer.java
    │   │   │   ├── EncodingConfig.java
    │   │   │   └── SecurityConfig.java
    │   │   ├── controller/
    │   │   │   ├── AuthController.java
    │   │   │   └── HomeController.java
    │   │   ├── dto/
    │   │   │   ├── LoginDTO.java
    │   │   │   └── UserDTO.java
    │   │   ├── entity/
    │   │   │   ├── Product.java
    │   │   │   ├── Role.java
    │   │   │   └── User.java
    │   │   ├── mapper/
    │   │   │   └── UserMapper.java
    │   │   ├── repository/
    │   │   │   ├── RoleRepository.java
    │   │   │   └── UserRepository.java
    │   │   └── security/
    │   │       └── CustomUserDetailsService.java
    │   └── resources/
    │       ├── application.properties
    │       ├── static/css/app.css
    │       └── templates/
    │           ├── auth/
    │           │   ├── access-denied.html
    │           │   └── login.html
    │           ├── fragments/header.html
    │           ├── layouts/layout.html
    │           ├── dashboard.html
    │           └── home.html
    └── test/
        ├── java/vn/iotstar/LoginEmailApplicationTests.java
        └── resources/application-test.properties
```

---

## 3. Biến môi trường & cấu hình Database

Hệ thống hỗ trợ đọc cấu hình từ file `.env` (ngang hàng `pom.xml`) hoặc các biến môi trường hệ thống. Mẫu cấu hình tham khảo tại `.env.example`:

```properties
# ===============================
# DATABASE
# ===============================
DB_URL=jdbc:sqlserver://localhost:1433;databaseName=webst4;encrypt=false;trustServerCertificate=true;sslProtocol=TLSv1.2;characterEncoding=UTF-8
DB_USERNAME=sa
DB_PASSWORD=your_password_here

# ===============================
# JPA
# ===============================
DDL_AUTO=update
SHOW_SQL=true

# ===============================
# SERVER
# ===============================
SERVER_PORT=8088

# ===============================
# ADMIN SEED ACCOUNT
# ===============================
ADMIN_EMAIL=trungnh@hcmute.edu.vn
ADMIN_PASSWORD=123456
```

### Chuẩn bị cơ sở dữ liệu SQL Server:
Chạy script SQL sau trong SQL Server Management Studio (SSMS) hoặc qua `sqlcmd`:
```sql
IF NOT EXISTS (SELECT * FROM sys.databases WHERE name = 'webst4')
BEGIN
    CREATE DATABASE webst4;
END;
```

---

## 4. Hướng dẫn chạy ứng dụng

Từ thư mục gốc dự án, di chuyển vào thư mục `ViDu1/LoginEmail` và chạy:

```bash
cd D:\BT09_laptrinh_web\ViDu1\LoginEmail
mvn spring-boot:run
```

Chạy kiểm thử tự động (Unit & Integration tests với HTTP Mock client và in-memory H2):
```bash
mvn test
```

---

## 5. Tài khoản demo mặc định

Dữ liệu được khởi tạo tự động khi khởi động qua `DataInitializer`:
- **Tài khoản Quản trị viên (ADMIN)**:
  - Email: `trungnh@hcmute.edu.vn`
  - Mật khẩu: `123456`
  - Quyền: `ROLE_ADMIN`
- **Tài khoản Người dùng (USER)**:
  - Email: `user01@gmail.com`
  - Mật khẩu: `123456`
  - Quyền: `ROLE_USER`

---

## 6. URLs truy cập

- Trang chủ: `http://localhost:8088/`
- Trang đăng nhập: `http://localhost:8088/login`
- Trang quản trị: `http://localhost:8088/dashboard` (chỉ quyền ADMIN truy cập được)
- Đăng xuất: POST tới `http://localhost:8088/logout` (kèm CSRF token)

---

## 7. Checklist kiểm thử thủ công

| STT | Kịch bản kiểm thử | Thao tác thực hiện | Kết quả mong đợi |
|---|---|---|---|
| 1 | Truy cập trang chủ khi chưa đăng nhập | Mở trình duyệt đến `http://localhost:8088/` | Hiển thị trang chủ với nút "Đăng nhập" ở Header, thông báo chưa đăng nhập |
| 2 | Truy cập trang bị chặn | Truy cập `http://localhost:8088/dashboard` khi chưa login | Tự động chuyển hướng (302) về `http://localhost:8088/login` |
| 3 | Đăng nhập sai mật khẩu | Nhập `trungnh@hcmute.edu.vn` / `sai_mat_khau` | Báo lỗi màu đỏ: "Email hoặc mật khẩu không đúng." |
| 4 | Đăng nhập thành công với tài khoản Admin | Nhập `trungnh@hcmute.edu.vn` / `123456` | Đăng nhập thành công, chuyển hướng vào `/dashboard`. Header hiển thị tên email `trungnh@hcmute.edu.vn` và nút "Đăng xuất" |
| 5 | Đăng xuất | Bấm nút "Đăng xuất" trên Header | Phiên làm việc bị hủy, chuyển hướng về `/login?logout=true`, thông báo "Bạn đã đăng xuất." |
| 6 | Phân quyền USER | Đăng nhập tài khoản `user01@gmail.com` / `123456`, sau đó vào `/dashboard` | Bị từ chối truy cập và chuyển hướng sang trang `/access-denied` (403 Forbidden) |

---

## 8. Ghi chú sai khác so với mã nguồn thô trong PDF

1. **Lombok & JDK 26**: JDK 26 thay đổi cấu trúc nội bộ của `com.sun.tools.javac`, phiên bản Lombok mặc định cũ hơn sẽ gây lỗi biên dịch. Đã sử dụng `lombok` 1.18.46 kết hợp `lombok-mapstruct-binding` 0.2.0 để biên dịch trơn tru với Java 26.
2. **Spring Boot Bean Overriding**: Khai báo `FilterRegistrationBean` trong `EncodingConfig` được cấu hình `customCharacterEncodingFilter` và bật `spring.main.allow-bean-definition-overriding=true` để không xung đột với bộ lọc mặc định của Spring Boot.
3. **Cơ chế kiểm thử không phụ thuộc dịch vụ ngoài**: Tích hợp cấu hình test profile dùng H2 để lệnh `mvn test` chạy tự động, độc lập và ổn định mà không yêu cầu khởi động SQL Server trước.
