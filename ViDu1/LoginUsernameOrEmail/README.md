# Ví dụ 2 trong ViDu1: Custom Login bằng Username hoặc Email (ViDu1/LoginUsernameOrEmail)

Dự án triển khai bài Ví dụ 2 trong file `ViDu1.pdf`: Cho bảng `User`, `Role`, viết chức năng Custom login có thể đăng nhập bằng username hoặc email đều được; thông tin fullname và images của user hiển thị ở `header.html`. Sử dụng Spring Boot 4.1.1, Spring Security, MapStruct và Thymeleaf với Dialect (`thymeleaf-layout-dialect`).

---

## 1. Thông tin công nghệ & phiên bản

- **Ngôn ngữ**: Java 26 (Oracle JDK 26.0.2.1 / release 26).
- **Build tool**: Apache Maven 3.9+.
- **Framework**: Spring Boot 4.1.1 (`spring-boot-starter-webmvc`, `spring-boot-starter-security`, `spring-boot-starter-thymeleaf`, `spring-boot-starter-data-jpa`, `spring-boot-starter-validation`).
- **Thư viện layout**: `nz.net.ultraq.thymeleaf:thymeleaf-layout-dialect` kết hợp `thymeleaf-extras-springsecurity6`.
- **Thư viện ánh xạ DTO**: MapStruct 1.6.3 (`lombok-mapstruct-binding` 0.2.0, Lombok 1.18.46 tương thích JDK 26).
- **Database**: Microsoft SQL Server (database `webst9`) và H2 in-memory phục vụ kiểm thử tự động.

---

## 2. Cấu trúc thư mục

```
ViDu1/LoginUsernameOrEmail/
├── pom.xml
├── .env.example
├── README.md
└── src/
    ├── main/
    │   ├── java/vn/iotstar/
    │   │   ├── Springboot19Application.java
    │   │   ├── config/
    │   │   │   └── SecurityConfig.java
    │   │   ├── controller/
    │   │   │   ├── AdminController.java
    │   │   │   ├── AuthController.java
    │   │   │   └── HomeController.java
    │   │   ├── dto/
    │   │   │   ├── LoginDTO.java
    │   │   │   └── UserDTO.java
    │   │   ├── entity/
    │   │   │   ├── Role.java
    │   │   │   └── User.java
    │   │   ├── mapper/
    │   │   │   └── UserMapper.java
    │   │   ├── repository/
    │   │   │   ├── RoleRepository.java
    │   │   │   └── UserRepository.java
    │   │   ├── security/
    │   │   │   ├── CustomUserDetails.java
    │   │   │   └── CustomUserDetailsService.java
    │   │   └── service/
    │   │       ├── UserService.java
    │   │       └── impl/UserServiceImpl.java
    │   └── resources/
    │       ├── application.properties
    │       ├── static/
    │       │   ├── css/app.css
    │       │   └── images/
    │       │       ├── avatar-default.png
    │       │       └── user.png
    │       └── templates/
    │           ├── admin/index.html
    │           ├── auth/
    │           │   ├── 403.html
    │           │   └── login.html
    │           ├── fragments/header.html
    │           ├── layouts/layout.html
    │           └── home.html
    └── test/
        ├── java/vn/iotstar/LoginUsernameOrEmailApplicationTests.java
        └── resources/application-test.properties
```

---

## 3. Biến môi trường & cấu hình Database

Hệ thống cấu hình trong file `src/main/resources/application.properties` và hỗ trợ biến môi trường hoặc file `.env`. Mẫu tại `.env.example`:

```properties
# ===============================
# DATABASE
# ===============================
DB_URL=jdbc:sqlserver://localhost:1433;databaseName=webst9;encrypt=false;trustServerCertificate=true;sslProtocol=TLSv1.2;characterEncoding=UTF-8
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
SERVER_PORT=8081
```

### Chuẩn bị database SQL Server:
```sql
IF NOT EXISTS (SELECT * FROM sys.databases WHERE name = 'webst9')
BEGIN
    CREATE DATABASE webst9;
END;
```

---

## 4. Hướng dẫn chạy ứng dụng

Từ thư mục dự án:
```bash
cd D:\BT09_laptrinh_web\ViDu1\LoginUsernameOrEmail
mvn spring-boot:run
```

Chạy kiểm thử tự động toàn diện:
```bash
mvn test
```

---

## 5. Tài khoản demo

Khởi tạo tự động qua `CommandLineRunner init`:
- **Tài khoản thường (USER)**:
  - Username: `user01`
  - Email: `user01@gmail.com`
  - Password: `123456`
  - Họ tên: "Nguyễn Hữu Trung"
  - Ảnh đại diện: `/images/user.png`
  - Role: `ROLE_USER`
- **Tài khoản quản trị (ADMIN)**:
  - Username: `admin01`
  - Email: `admin01@gmail.com`
  - Password: `123456`
  - Role: `ROLE_ADMIN`

---

## 6. URLs truy cập

- Trang đăng nhập: `http://localhost:8081/login`
- Trang chủ (yêu cầu đăng nhập): `http://localhost:8081/`
- Trang quản trị: `http://localhost:8081/admin` (yêu cầu quyền `ROLE_ADMIN`)
- Đăng xuất: POST `http://localhost:8081/logout` (kèm token CSRF)

---

## 7. Checklist kiểm thử thủ công

| STT | Kịch bản kiểm thử | Thao tác | Kết quả mong đợi |
|---|---|---|---|
| 1 | Chưa đăng nhập vào trang chủ | Mở `http://localhost:8081/` | Chuyển hướng 302 về trang đăng nhập `/login` |
| 2 | Đăng nhập bằng USERNAME | Nhập `user01` và `123456` | Đăng nhập thành công, vào trang chủ `/`. Header hiển thị ảnh đại diện, `Nguyễn Hữu Trung`, `(user01)`, `user01@gmail.com`, `ROLE_USER` |
| 3 | Đăng xuất | Bấm nút "Đăng xuất" trên Header | Hủy session, chuyển hướng về `/login?logout=true` |
| 4 | Đăng nhập bằng EMAIL | Nhập `user01@gmail.com` và `123456` | Đăng nhập thành công, vào trang chủ, hiển thị đầy đủ thông tin ở Header |
| 5 | Đăng nhập sai | Nhập sai username hoặc mật khẩu | Báo lỗi: "Username/email hoặc password không đúng" |
| 6 | Phân quyền truy cập | Dùng tài khoản `user01` truy cập `http://localhost:8081/admin` | Bị từ chối truy cập và chuyển đến trang lỗi `/403` |
| 7 | Truy cập quyền Admin | Đăng nhập `admin01` / `123456` rồi truy cập `http://localhost:8081/admin` | Hiển thị Admin Control Panel thành công |
