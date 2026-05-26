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
### Bước 2: Cập nhật script entrypoint của Postgres để tự sinh thêm vỏ DB
Mở file docker-compose.yml, tìm đến service postgres. Tại đoạn script bash, chèn thêm một dòng lệnh psql để tự động tạo vỏ database trống cho service mới ngay trước chữ wait:
```xml
entrypoint: [
      "bash", "-c",
      "exec docker-entrypoint.sh postgres & \
           until pg_isready -U postgres; do sleep 1; done; \
           psql -U postgres -c 'CREATE DATABASE identity;' || true; \
           psql -U postgres -c 'CREATE DATABASE notification;' || true; \
           psql -U postgres -c 'CREATE DATABASE payment;' || true; \  # 👈 CHÈN THÊM DÒNG NÀY ĐỂ ĐẺ DB TỰ ĐỘNG (ví dụ tạo thêm payment service)
           wait"
    ]
```
### Bước 3: Khai báo khối container cho Service mới trong docker-compose.yml
Kéo xuống dưới cùng của file compose, định nghĩa service mới bằng cách copy và chỉnh sửa lại tên folder (context) cũng như cổng (ports) tương ứng:
```xml
payment-service:
    build:
      context: ./payment-service  # Thư mục chứa source code của service mới (ví dụ tạo thêm payment service)
      dockerfile: Dockerfile
    container_name: payment-service
    env_file: .env
    depends_on:
      - postgres
      - discovery
    ports:
      - "8084:8084"               # Port chạy ngoài máy thật của service mới
    environment:
      SPRING_PROFILES_ACTIVE: docker
      DB_HOST: postgres
      DB_PASSWORD: ${DB_PASSWORD}
      EUREKA_CLIENT_SERVICEURL_DEFAULTZONE: http://discovery:8761/eureka/
      APP_SHARED-SECRET: ${SHARED_SECRET}
    networks:
      - microservice-network
```
### Bước 4: Cấu hình định tuyến Route trên API Gateway
Mở file application.yml nội bộ của dự án api-gateway và bổ sung thêm thẻ route để Gateway biết đường dẫn request tới service mới:
```xml
YAML
spring:
  cloud:
    gateway:
      routes:
        # ... các routes cũ giữ nguyên
        - id: payment-service
          uri: lb://payment-service
          predicates:
            - Path=/payment/**
          filters:
            - StripPrefix=1
```
### Bước 5:
  - Chạy lệnh đóng gói lại toàn bộ: mvn clean package -DskipTests
  - Chạy lệnh hạ và nâng Docker (xóa volume cũ): docker-compose down -v rồi docker-compose up -d --build





PHẦN 2: Hướng dẫn dành cho ĐỒNG ĐỘI (Khi CLONE code về)
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
