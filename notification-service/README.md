# Notification Service (.NET 8)

Dịch vụ quản lý thông báo thời gian thực (Real-time Notification Service) được chuyển đổi từ **Java (Spring Boot 3.2.5)** sang **.NET 8 (C#)**, giữ nguyên 100% logic nghiệp vụ, các công nghệ áp dụng và khả năng tương thích toàn diện với toàn bộ hệ thống microservices hiện tại.

---

## 🛠️ Công Nghệ Áp Dụng (Technology Stack Mapping)

| Thành phần | Java (Spring Boot) | .NET 8 (C#) | Ghi chú |
| :--- | :--- | :--- | :--- |
| **Framework** | Spring Boot 3.2.5 (Web MVC) | ASP.NET Core 8.0 Web API | Controller RESTful endpoints |
| **Database & ORM** | PostgreSQL + Spring Data JPA (Hibernate) | PostgreSQL + EF Core 8 (`Npgsql.EntityFrameworkCore.PostgreSQL`) | Bảng `notifications`, hỗ trợ UUID và timestamps |
| **Cache** | Redis (`Jedis` / `RedisTemplate`) | Redis (`StackExchange.Redis`) | Cache unread count & notification list (TTL 24h) |
| **Message Broker** | Apache Kafka (`spring-kafka`) | Apache Kafka (`Confluent.Kafka`) | Consumer BackgroundService cho 10 topics |
| **Real-time Engine** | Spring WebSocket + STOMP over SockJS | Custom STOMP WebSocket Middleware + SignalR | Tương thích 100% với Frontend STOMP/SockJS |
| **Service Discovery**| Netflix Eureka Client (`spring-cloud-starter-netflix-eureka-client`) | Steeltoe Eureka Client (`Steeltoe.Discovery.Eureka`) | Tự động đăng ký `notification-service` vào Eureka |
| **API Documentation**| SpringDoc OpenAPI (Swagger UI) | Swashbuckle OpenAPI (Swagger UI) | Tích hợp JWT Bearer authorization |

---

## 📁 Cấu Trúc Dự Án

```
notification-service/
├── Controllers/
│   ├── NotificationController.cs     # Endpoints GET / PUT notifications
│   └── HealthController.cs           # Health check endpoint
├── Data/
│   └── NotificationDbContext.cs      # EF Core DbContext cho PostgreSQL
├── Models/
│   ├── Entities/
│   │   └── Notification.cs           # Entity bảng notifications
│   ├── DTOs/
│   │   ├── Events/NotificationEvent.cs        # Model Kafka event
│   │   └── Responses/
│   │       ├── NotificationPayload.cs
│   │       └── NotificationResponse.cs
│   └── Enums/
│       └── NotificationTemplate.cs   # Template render title, message, deeplink
├── Repositories/
│   ├── INotificationRepository.cs
│   └── NotificationRepository.cs
├── Services/
│   ├── INotificationCacheService.cs
│   ├── NotificationCacheService.cs   # Redis cache service
│   ├── INotificationService.cs
│   └── NotificationService.cs        # Core business logic
├── Consumers/
│   └── NotificationKafkaConsumer.cs  # Kafka Consumer BackgroundService
├── WebSocket/
│   ├── IWebSocketNotificationSender.cs
│   ├── WebSocketNotificationSender.cs
│   └── StompWebSocketMiddleware.cs   # STOMP protocol over WebSocket & SockJS
├── Hubs/
│   └── NotificationHub.cs            # SignalR Hub (/hubs/notifications)
├── appsettings.json
├── Dockerfile
└── NotificationService.csproj
```

---

## 🚀 Hướng Dẫn Chạy Dịch Vụ

### 1. Chạy Trực Tiếp Bằng .NET CLI
```bash
cd notification-service
dotnet run
```
Dịch vụ sẽ khởi động tại cổng mặc định `http://localhost:8086`.

### 2. Swagger UI
Truy cập:
```
http://localhost:8086/swagger
```

### 3. Chạy Bằng Docker Compose
`notification-service` đã được cấu hình sẵn trong [docker-compose.yml](file:///c:/FALL26/PRN232/BE/docker-compose.yml):
```bash
docker compose up -d notification-service
```

---

## 📡 API Endpoints

| Phương thức | Endpoint | Mô tả |
| :--- | :--- | :--- |
| `GET` | `/api/v1/notifications/user/{userId}` | Lấy danh sách thông báo của user (mới nhất trước, ưu tiên Redis cache) |
| `GET` | `/api/v1/notifications/user/{userId}/unread-count` | Lấy số lượng thông báo chưa đọc (ưu tiên Redis cache) |
| `PUT` | `/api/v1/notifications/user/{userId}/read-all` | Đánh dấu tất cả thông báo của user là đã đọc |
| `PUT` | `/api/v1/notifications/{notificationId}/read` | Đánh dấu 1 thông báo cụ thể là đã đọc |
| `GET` | `/api/notifications/health` | Health check endpoint |

---

## ⚡ Real-time WebSocket & STOMP

- **Endpoint**: `/ws/notifications` (hỗ trợ SockJS tại `/ws/notifications/info`)
- **Handshake Authentication**: Truyền header `X-User-Id: <userId>` (từ Gateway) hoặc query parameter `?userId=<userId>`.
- **Destination Subscribe**: Client subscribe vào `/user/queue/notifications`.
- **Đồng thời hỗ trợ SignalR**: Tại endpoint `/hubs/notifications`.

---

## 📨 Kafka Topics Được Lắng Nghe

Consumer group: `notification-service` (Offset: `earliest`)

1. `neural-bridge.package.submitted`
2. `neural-bridge.package.resubmitted`
3. `neural-bridge.package.approved`
4. `neural-bridge.package.rejected`
5. `neural-bridge.workspace.invited`
6. `neural-bridge.jobpost.applied`
7. `neural-bridge.jobpost.accepted`
8. `neural-bridge.contract.sign-required`
9. `neural-bridge.contract.fully-signed`
10. `neural-bridge.project.completed-payment-released`
