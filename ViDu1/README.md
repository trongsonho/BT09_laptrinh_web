# Thư mục ViDu1

Thư mục `ViDu1` chứa 2 bài toán được mô tả trong tài liệu `ViDu1.pdf`:

---

## 1. Danh sách các bài toán

### 1. [LoginEmail](./LoginEmail/)
- **Mô tả**: Cho bảng `User`, `Role`, viết chức năng login xác thực bằng **Email**, thông tin của user hiển thị ở `header.html`.
- **Đặc điểm kiến trúc**:
  - Xác thực qua email và BCrypt PasswordEncoder.
  - Phân quyền `ADMIN` và `USER`.
  - Dựng layout Thymeleaf bằng cú pháp `th:replace` tiêu chuẩn, **không sử dụng Layout Dialect** đúng theo yêu cầu phần 1 trong `ViDu1.pdf`.
  - Cổng chạy mặc định: `8088`.
  - Database mặc định: `webst4` (SQL Server) kèm H2 test profile.

### 2. [LoginUsernameOrEmail](./LoginUsernameOrEmail/)
- **Mô tả**: Cho bảng `User`, `Role`, viết chức năng Custom login cho phép đăng nhập bằng **Username hoặc Email** đều được; thông tin `fullName` và `images` (ảnh đại diện) của user được hiển thị trực tiếp ở `header.html`.
- **Đặc điểm kiến trúc**:
  - Triển khai `CustomUserDetails` lưu trữ các trường mở rộng (`fullName`, `images`, `email`, `role`, `enabled`).
  - `CustomUserDetailsService` tìm kiếm tài khoản linh hoạt qua `findByUsernameOrEmail`.
  - Sử dụng **Thymeleaf Layout Dialect** (`nz.net.ultraq.thymeleaf:thymeleaf-layout-dialect` với thuộc tính `layout:decorate` và `layout:fragment`).
  - Cổng chạy mặc định: `8081`.
  - Database mặc định: `webst9` (SQL Server) kèm H2 test profile.

---

## 2. Hướng dẫn chạy nhanh

Mỗi dự án là một ứng dụng Maven độc lập. Để chạy hoặc kiểm thử từng ứng dụng:

```bash
# Chạy Ví dụ 1 (LoginEmail)
cd D:\BT09_laptrinh_web\ViDu1\LoginEmail
mvn clean test
mvn spring-boot:run

# Chạy Ví dụ 2 (LoginUsernameOrEmail)
cd D:\BT09_laptrinh_web\ViDu1\LoginUsernameOrEmail
mvn clean test
mvn spring-boot:run
```
