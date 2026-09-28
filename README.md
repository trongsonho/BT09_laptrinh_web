# BÀI TẬP 09 - LẬP TRÌNH WEB: SPRING SECURITY 6 & ỨNG DỤNG QUẢN LÝ TOÀN DIỆN

Kho lưu trữ hoàn chỉnh các bài thực hành và ứng dụng của đề bài **BT09 Lập trình Web**, được xây dựng dựa trên 4 tài liệu đặc tả:
1. `03_springsecurity6.pdf`
2. `ViDu1.pdf`
3. `ViDu2.pdf`
4. `ViDu3.pdf`

---

## 1. Ma trận Ánh xạ Tài liệu Đề bài & Cấu trúc Dự án

| Tài liệu đề bài | Mục / Nội dung | Thư mục Dự án trong Repo | Cổng (Port) | Database (SQL Server) |
|---|---|---|---|---|
| `03_springsecurity6.pdf` | Kiến thức nền Spring Security 6 (SecurityFilterChain, UserDetailsService, PasswordEncoder, CSRF, Session) | Nền tảng áp dụng chung cho toàn bộ các dự án | - | - |
| `ViDu1.pdf` | **Ví dụ 1**: Login bằng Email, hiển thị thông tin ở header, Thymeleaf thuần (không layout dialect) | [`ViDu1/LoginEmail/`](file:///ViDu1/LoginEmail) | `8080` | `webst4` |
| `ViDu1.pdf` | **Ví dụ 2**: Custom login bằng Username hoặc Email, hiển thị fullname & ảnh ở header, Thymeleaf Layout Dialect | [`ViDu1/LoginUsernameOrEmail/`](file:///ViDu1/LoginUsernameOrEmail) | `8081` | `webst9` |
| `ViDu2.pdf` | Hoàn thiện mã nguồn & quy trình Custom Login Username/Email theo tài liệu ViDu2 | [`ViDu2/`](file:///ViDu2) | `8082` | `webst9` |
| `ViDu3.pdf` | Hệ thống Users, Roles, OtpTokens, Products, xác thực OTP email, phân quyền sở hữu Product, upload ảnh Cloudinary | [`ViDu3/`](file:///ViDu3) | `8083` | `webst3` |

---

## 2. Cấu trúc Kho Lưu trữ (Repository Structure)

```
BT09_laptrinh_web/
├── .gitignore                      # Git ignore loại bỏ target, .env, ide files...
├── README.md                       # Tài liệu tổng quan dự án & ma trận ánh xạ
├── docs/
│   └── HUONG_DAN_CHAY.md           # Hướng dẫn chi tiết thiết lập môi trường & chạy code
├── ViDu1/
│   ├── README.md                   # Tổng quan & so sánh giữa 2 nhánh của ViDu1
│   ├── LoginEmail/                 # Maven project: Đăng nhập bằng Email
│   │   ├── pom.xml
│   │   ├── .env.example
│   │   ├── src/
│   │   └── README.md
│   └── LoginUsernameOrEmail/        # Maven project: Đăng nhập bằng Username/Email (Layout)
│       ├── pom.xml
│       ├── .env.example
│       ├── src/
│       └── README.md
├── ViDu2/                          # Maven project: Độc lập theo tài liệu ViDu2.pdf
│   ├── pom.xml
│   ├── .env.example
│   ├── src/
│   └── README.md
└── ViDu3/                          # Maven project: Hệ thống Users/Roles/OTP/Products/Cloudinary
    ├── pom.xml
    ├── .env.example
    ├── src/
    └── README.md
```

---

## 3. Công nghệ & Thư viện Sử dụng

- **Ngôn ngữ & Runtime**: Java 26 / Java 17+ (tương thích hoàn toàn với JDK 17, 21, 24, 26).
- **Framework**: Spring Boot 4.1.1, Spring Security 6.
- **Template Engine**: Thymeleaf & Thymeleaf Layout Dialect.
- **Database & ORM**: Microsoft SQL Server, Spring Data JPA, Hibernate ORM.
- **Tiện ích & Mapping**: Lombok 1.18.46, MapStruct 1.6.3.
- **Dịch vụ Mở rộng**: Spring Mail (Jakarta Mail), Cloudinary Java SDK.
- **Bảo mật**: Mã hóa BCryptPasswordEncoder, bảo vệ CSRF trên mọi biểu mẫu, kiểm tra phân quyền sở hữu dữ liệu (Server-side Ownership Enforcement) chống can thiệp ID injection.

---

## 4. Tài khoản Đăng nhập Mặc định (Tự động Seed vào CSDL)

Khi khởi động lần đầu, mỗi dự án sẽ tự động kiểm tra và seed dữ liệu người dùng:

### Dự án `ViDu1/LoginEmail`:
- **Admin**: Email `admin@example.com` / Mật khẩu `123456`
- **User**: Email `trungnh@example.com` / Mật khẩu `123456`

### Dự án `ViDu1/LoginUsernameOrEmail` & `ViDu2`:
- **Admin**: Username `admin` (hoặc Email `admin@example.com`) / Mật khẩu `123456`
- **User**: Username `trungnh` (hoặc Email `trungnh@example.com`) / Mật khẩu `123456`

### Dự án `ViDu3`:
- **Admin**: Username `admin` / Email `admin@example.com` / Mật khẩu `123456` (Vai trò: `ROLE_ADMIN`, `ROLE_USER`)
- **User**: Username `trungnh` / Email `trungnh@example.com` / Mật khẩu `123456` (Vai trò: `ROLE_USER`)

---

## 5. Hướng dẫn Khởi chạy Nhanh

Xem tài liệu chi tiết tại [`docs/HUONG_DAN_CHAY.md`](file:///docs/HUONG_DAN_CHAY.md).

### Bước 1: Khởi tạo CSDL trên Microsoft SQL Server
```sql
CREATE DATABASE webst4; -- ViDu1 LoginEmail
CREATE DATABASE webst9; -- ViDu1 LoginUsernameOrEmail & ViDu2
CREATE DATABASE webst3; -- ViDu3
GO
```

### Bước 2: Chạy kiểm thử tự động
Mỗi dự án đều tích hợp bộ kiểm thử tự động độc lập:
```powershell
# Kiểm thử ViDu1 LoginEmail
cd ViDu1\LoginEmail; mvn clean test

# Kiểm thử ViDu1 LoginUsernameOrEmail
cd ..\LoginUsernameOrEmail; mvn clean test

# Kiểm thử ViDu2
cd ..\..\ViDu2; mvn clean test

# Kiểm thử ViDu3
cd ..\ViDu3; mvn clean test
```

### Bước 3: Khởi chạy từng dự án
Chuyển vào thư mục dự án tương ứng và chạy lệnh:
```powershell
mvn spring-boot:run
```
