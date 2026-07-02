# 🌐 HƯỚNG DẪN CẤU HÌNH, VẬN HÀNH & PHÁT TRIỂN HỆ THỐNG MICROSERVICES

Hệ thống Backend Microservices được cấu hình theo kiến trúc **Maven Multi-Module** quản lý tập trung và đóng gói toàn bộ hạ tầng (Postgres, Kafka, Redis, Eureka) tự động hóa qua Docker Compose.

---

## 🛠️ PHẦN 1: Hướng dẫn dành cho Nhà phát triển chính (Khi SCALE hoặc Thêm Service mới)

Khi dự án phình to và bạn cần tạo thêm một Microservice mới (ví dụ: `payment-service` chạy port `8084`), hãy thực hiện mở rộng hệ thống theo đúng quy trình 4 bước chuẩn hóa sau:

### Bước 1: Cập nhật file `pom.xml` tại thư mục gốc `BE`
Để Maven biết có service mới khi chạy lệnh gom, bạn mở file `pom.xml` ở thư mục gốc ra, thêm tên thư mục của service mới vào trong thẻ `<modules>`:
```xml
    <modules>
        <module>common-starter</module>
        <module>discovery</module>
        <module>identity-service</module>
        <module>notification-service</module>
        <module>api-gateway</module>
        <module>payment-service</module> </modules> (ví dụ tạo thêm payment service)
```
### Bước 2: Kế thừa nền tảng Security từ common-starter
Để đảm bảo service mới được bảo vệ ngay lập tức bằng chữ ký Gateway và cơ chế Token JWT, hãy mở file pom.xml của service mới lên và khai báo dependency gọi đến module dùng chung:
```xml
<dependency>
    <groupId>com.nqt</groupId>
    <artifactId>common-starter</artifactId>
    <version>0.0.1-SNAPSHOT</version>
</dependency>
```
### Bước 3: Cấu hình Endpoint công khai tại application.yml nội bộ
Mở file application.yml của service mới ra, cấu hình cổng chạy, định danh tên ứng dụng lên Eureka (bắt buộc phải giữ đuôi -service), và mở cổng cho Swagger UI bằng cách khai báo thụt lề chuẩn dưới nhánh app.security:
```xml
server:
        port: 8083 # Cổng chạy nội bộ

        spring:
        application:
        name: workspace-service # Tên định danh viết thường, có đuôi -service

        app:
        shared-secret: ${SHARED_SECRET}
        security:
        public-endpoints:
        - "/v3/api-docs/**"   # Mở đường dẫn để API Gateway tự động quét tài liệu Swagger
        - "/swagger-ui/**"
```
### Bước 4: Cấu hình nạp vỏ Database tự động qua init.sql
Hệ thống sử dụng cơ chế ánh xạ Volume thông minh thay vì can thiệp script entrypoint thô của Postgres. Bạn chỉ cần mở file init.sql ở thư mục gốc của dự án ra và thêm lệnh tạo database trống cho service mới xuống cuối file:
```xml
CREATE DATABASE identity;

-- Thêm dòng này cho service mới (Ví dụ: workspace-service)
CREATE DATABASE workspace;
```
### Bước 5:Khai báo khối container cho Service mới trong docker-compose.yml
Kéo xuống dưới cùng của file compose, định nghĩa service mới. Hệ thống sử dụng cơ chế Dynamic Routing (Định tuyến động) của API Gateway kết hợp Eureka, do đó BẠN TUYỆT ĐỐI KHÔNG CẦN cấu hình thêm bất kỳ dòng routes: thủ công nào trong file cấu hình của api-gateway.
```xml
workspace-service:
        build:
        context: ./workspace-service  # Đường dẫn đến thư mục chứa source code service mới
        dockerfile: Dockerfile
        container_name: workspace-service
        env_file: .env
        depends_on:
        - postgres
        - redis
        - discovery
        ports:
        - "8083:8083"                 # Ánh xạ cổng ngoài máy thật (Hãy đổi cổng ngoài nếu bị trùng)
        environment:
        SPRING_PROFILES_ACTIVE: docker
        DB_HOST: postgres
        DB_PASSWORD: ${DB_PASSWORD}
        REDIS_HOST: redis
        REDIS_PASSWORD: ${REDIS_PASSWORD}
        EUREKA_CLIENT_SERVICEURL_DEFAULTZONE: http://discovery:8761/eureka/
        APP_SHARED-SECRET: ${SHARED_SECRET}
        networks:
        - microservice-network
```
### Bước 6: Triển khai nạp mã nguồn và làm sạch tài nguyên
Mở terminal tại thư mục gốc chứa file dự án tổng và thực thi chuỗi lệnh sau:
```xml
mvn clean package -DskipTests
```

```xml
docker compose down -v
```

```xml
docker compose up -d --build
```


## 🛠️ PHẦN 2: Hướng dẫn dành cho ĐỒNG ĐỘI (Khi CLONE code về)
Chào mừng bạn tham gia vào đội ngũ phát triển dự án! Để thiết lập môi trường và khởi chạy toàn bộ hệ thống Microservices trên máy cục bộ của bạn một cách nhanh nhất, hãy thực hiện theo đúng 4 bước sau:

🛠️ Yêu cầu tiên quyết
Đã cài đặt và bật sẵn ứng dụng Docker Desktop (Khuyến khích bật môi trường ảo hóa WSL2 trên Windows).

Đã cài đặt Java 21 và Maven trên máy thật để đóng gói ứng dụng.

Bước 1: Tạo file cấu hình môi trường cục bộ .env
Vì lý do bảo mật, file cấu hình mật khẩu thực tế .env không được đẩy lên Git. Bạn cần tự tạo ra nó ở máy cá nhân:

Tạo một file mới đặt tên chính xác là .env nằm tại thư mục gốc của dự án (ngang hàng với file docker-compose.yml và file pom.xml gốc).

Copy đoạn mã dưới đây và paste vào file .env vừa tạo:

Đoạn mã
```xml
DB_PASSWORD=postgres
SHARED_SECRET=justi_secret_key_2026
MAIL_USERNAME=your-email@gmail.com
MAIL_PASSWORD=your-gmail-app-password
FROM_EMAIL=noreply@yourapp.com
FROM_NAME=Your Application
(Lưu ý: Để lấy MAIL_PASSWORD, truy cập https://myaccount.google.com/apppasswords để tạo mật khẩu ứng dụng Gmail, tuyệt đối không dùng mật khẩu email cá nhân thông thường).
```

Bước 2: Đóng gói toàn bộ hệ thống bằng một lệnh duy nhất (Maven Multi-Module)
Mở Terminal tại thư mục gốc BE và chạy duy nhất câu lệnh sau: 
```xml
mvn clean package -DskipTests
```
Hệ thống sẽ tự động phân tích độ phụ thuộc, biên dịch thư viện dùng chung common-starter trước, sau đó tự động quét và đóng gói file .jar cho toàn bộ 100 dịch vụ con cùng một lúc vào thư mục target của từng folder.

Bước 3: Khởi chạy toàn bộ hệ thống bằng Docker Compose
Sau khi Maven đã build xong file .jar cho tất cả các service, tại thư mục gốc dự án, chạy lệnh sau:
```xml
docker-compose up -d --build
```
Mạng nội bộ Docker sẽ khởi chạy. Đoạn script entrypoint cấu hình sẵn bên trong file compose sẽ tự động kích hoạt vào Postgres để đẻ sẵn các vỏ database trống (identity, notification...) giúp toàn bộ microservices kết nối thông suốt mà bạn không cần tạo tay bằng phần mềm bên ngoài.

Bước 4: Kiểm tra trạng thái hệ thống
Sau khi gõ lệnh chạy Docker, hãy đợi khoảng 30 - 45 giây để toàn bộ hệ thống ổn định, sau đó truy cập các đường dẫn sau để kiểm tra:

Bảng điều khiển Microservices (Eureka Dashboard): Truy cập địa chỉ http://localhost:8761. Đảm bảo nhìn thấy đầy đủ các service hiển thị trạng thái UP (Xanh mượt).

Cổng API tập trung (API Gateway): Đầu mối nhận mọi request từ Frontend tại địa chỉ http://localhost:8080.

🗄️ Kết nối Database qua pgAdmin / DBeaver / Datagrip
Để truy cập vào xem cấu trúc bảng dữ liệu do Spring Boot tự sinh ra, hãy tạo một Server Connection mới trên công cụ quản lý của bạn với thông số:

Host name/address: 127.0.0.1 (Bắt buộc dùng IP dạng số này để ép trỏ thẳng vào Docker, tránh xung đột với dịch vụ Postgres chạy ngầm sẵn nếu có của Windows)

Port: 5432

Username: postgres

Password: Nhập đúng giá trị DB_PASSWORD bạn đã khai báo ở file .env cá nhân tại Bước 1 (Ví dụ: postgres).
