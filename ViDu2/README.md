# Ví dụ 2: Custom Login với Spring Boot 4 + Spring Security + MapStruct + Thymeleaf (ViDu2)

Dự án triển khai bài tập trong file `ViDu2.pdf`:
Cho bảng `User`, `Role`, viết chức năng Custom login có thể đăng nhập bằng **Username hoặc Email** đều được; thông tin `fullname` và `images` của user hiển thị ở `header.html`. Dự án độc lập tuân thủ kiến trúc nhiều tầng: Entity – Repository – DTO – Mapper – Security – Controller – View.

---

## 1. Thông tin công nghệ & phiên bản

- **Ngôn ngữ**: Java 26 (Oracle JDK 26.0.2.1 / compiler release 26).
- **Build tool**: Apache Maven 3.9+.
- **Framework**: Spring Boot 4.1.1 (`spring-boot-starter-webmvc`, `spring-boot-starter-security`, `spring-boot-starter-thymeleaf`, `spring-boot-starter-data-jpa`, `spring-boot-starter-validation`).
- **Thư viện giao diện**: Thymeleaf kết hợp `thymeleaf-layout-dialect` và `thymeleaf-extras-springsecurity6`.
- **Thư viện ánh xạ**: MapStruct 1.6.3 (`lombok-mapstruct-binding` 0.2.0, Lombok 1.18.46 tương thích JDK 26).
- **Database**: Microsoft SQL Server (tên database `webst9`) và H2 in-memory cho tự động hóa kiểm thử.

---

## 2. Cấu trúc dự án

```
ViDu2/
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
        ├── java/vn/iotstar/ViDu2ApplicationTests.java
        └── resources/application-test.properties
```

---

## 3. Biến môi trường & Cơ sở dữ liệu

Cấu hình trong file `src/main/resources/application.properties` và nạp từ file `.env` (nếu có). File mẫu `.env.example`:

```properties
# ===============================
# DATABASE
# ===============================
DB_URL=jdbc:sqlserver://localhost:1433;databaseName=webst9;encrypt=false;trustServerCertificate=true;sslProtocol=TLSv1.2;characterEncoding=UTF-8
DB_USERNAME=sa
DB_PASSWORD=your_database_password_here

# ===============================
# JPA
# ===============================
DDL_AUTO=update
SHOW_SQL=true

# ===============================
# SERVER
# ===============================
SERVER_PORT=8082
```

### Tạo cơ sở dữ liệu trên SQL Server:
```sql
IF NOT EXISTS (SELECT * FROM sys.databases WHERE name = 'webst9')
BEGIN
    CREATE DATABASE webst9;
END;
```

---

## 4. Hướng dẫn chạy và kiểm thử

### Lệnh chạy ứng dụng từ thư mục `ViDu2`:
```bash
cd D:\BT09_laptrinh_web\ViDu2
mvn spring-boot:run
```

### Lệnh chạy kiểm thử tự động:
```bash
mvn test
```

---

## 5. Tài khoản demo

Khởi tạo tự động qua `CommandLineRunner init`:
- **Tài khoản User**:
  - Tên đăng nhập: `user01`
  - Email: `user01@gmail.com`
  - Mật khẩu: `123456`
  - Họ tên: "Nguyễn Hữu Trung"
  - Hình ảnh: `/images/user.png`
  - Quyền: `ROLE_USER`
- **Tài khoản Admin**:
  - Tên đăng nhập: `admin01`
  - Email: `admin01@gmail.com`
  - Mật khẩu: `123456`
  - Quyền: `ROLE_ADMIN`

---

## 6. URLs truy cập

- Đăng nhập: `http://localhost:8082/login`
- Trang chủ (yêu cầu xác thực): `http://localhost:8082/`
- Quản trị (yêu cầu ADMIN): `http://localhost:8082/admin`
- Đăng xuất: POST `http://localhost:8082/logout`

---

## 7. Checklist kiểm thử thủ công

| STT | Kịch bản | Thao tác thực hiện | Kết quả mong đợi |
|---|---|---|---|
| 1 | Chưa đăng nhập vào trang chủ | Mở `http://localhost:8082/` | Chuyển hướng 302 về trang đăng nhập `/login` |
| 2 | Đăng nhập bằng Username | Nhập `user01` và `123456` | Đăng nhập thành công, chuyển hướng vào `/`. Header hiển thị ảnh đại diện, họ tên "Nguyễn Hữu Trung", `(user01)`, `user01@gmail.com` và `ROLE_USER` |
| 3 | Đăng nhập bằng Email | Đăng xuất rồi nhập `user01@gmail.com` và `123456` | Đăng nhập thành công với đầy đủ thông tin trên header |
| 4 | Đăng nhập sai mật khẩu | Nhập `user01` và `sai_pass` | Chuyển về `/login?error=true`, báo lỗi đỏ |
| 5 | Kiểm tra phân quyền USER | Đăng nhập `user01` và truy cập `/admin` | Bị từ chối truy cập, chuyển hướng đến trang `/403` |
| 6 | Kiểm tra quyền ADMIN | Đăng nhập `admin01` / `123456` và truy cập `/admin` | Truy cập thành công vào Admin Control Panel |
