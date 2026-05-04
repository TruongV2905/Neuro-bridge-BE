# 🚀 Microservices Backend System

Dự án này được xây dựng theo kiến trúc Microservices sử dụng hệ sinh thái Spring Cloud, tập trung vào tính bảo mật, khả năng mở rộng và quản lý dịch vụ tập trung.

## 🛠 Công nghệ sử dụng (Tech Stack)

- **Backend Framework:** Java Spring Boot, Spring AI
- **Microservices Orchestration:** Spring Cloud (Netflix Eureka, Gateway)
- **Security:** Centralized JWT, OAuth2
- **DevOps & Infrastructure:** Docker, Docker Compose, Nginx
- **Database:** (Tùy thuộc vào từng service cụ thể)

## 📂 Cấu trúc thư mục (Project Structure)

| **Thư mục / File** | **Vai trò chính** |
|---------------------|-------------------|
| **api-gateway/** | Cửa ngõ duy nhất của hệ thống, xử lý định tuyến (routing) và bảo mật tập trung (Centralized Security). |
| **identity-service/**| Dịch vụ quản lý định danh, xử lý xác thực (Authentication) và phân quyền (Authorization). |
| **discovery/** | Eureka Server giúp các dịch vụ tự động đăng ký và tìm kiếm lẫn nhau (Service Discovery). |
| **common-starter/** | Chứa các thư viện, cấu hình hoặc tiện ích dùng chung cho tất cả các service (Shared Library). |
| **demo-service/** | Service mẫu triển khai logic nghiệp vụ cụ thể. |
| **docker-compose.yml**| File cấu hình để khởi chạy toàn bộ hệ thống Microservices dưới dạng các container Docker. |
| **.env** | Lưu trữ các biến môi trường nhạy cảm (DB Credentials, Secret Keys...). |
| **.gitignore** | Các file và thư mục không được đưa lên hệ thống quản lý phiên bản. |

## ⚙️ Hướng dẫn cài đặt & Khởi chạy

### 1. Yêu cầu hệ thống
* Java 17+
* Docker & Docker Compose
* IDE (IntelliJ IDEA được khuyến nghị - dựa trên thư mục `.idea`)

### 2. Khởi chạy với Docker
Tại thư mục gốc của dự án, chạy lệnh sau:
```bash
docker-compose up -d