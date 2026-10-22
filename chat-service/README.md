# Chat Service (.NET 8) — AI Chat & Real-Time Messaging

Dịch vụ quản lý tin nhắn thời gian thực và trợ lý AI (Real-time Messaging & AI Assistant Service) được chuyển đổi từ **Java (Spring Boot 3)** sang **.NET 8 (C#)**, nâng cấp thêm tính năng **AI Chatbot (Google Gemini 2.5 Flash)** và tương thích 100% với kiến trúc Microservices hiện tại của **Neuro Bridge**.

---

## 🛠️ Công Nghệ Áp Dụng (Technology Stack Mapping)

| Thành phần | Java (Spring Boot) | .NET 8 (C#) | Ghi chú |
| :--- | :--- | :--- | :--- |
| **Framework** | Spring Boot 3.2.5 (Web MVC) | ASP.NET Core 8.0 Web API | RESTful Controllers |
| **Database & ORM** | PostgreSQL + Spring Data JPA (Hibernate) | PostgreSQL + EF Core 8 (`Npgsql.EntityFrameworkCore.PostgreSQL`) | Bảng `chat_session`, `messages`, `chat_session_participants` |
| **AI Engine** | Chưa có | **Google Gemini 2.5 Flash** (`GeminiAiService`) | Hỗ trợ chat hỏi đáp thông minh, tư vấn dự án |
| **Real-time Engine**| Spring WebSocket + STOMP | **SignalR Hub** (`/hubs/chat`, `/ws/chat`) | Đẩy tin nhắn và câu trả lời AI thời gian thực |
| **Service Discovery**| Spring Cloud Eureka Client | Steeltoe Eureka Client (`Steeltoe.Discovery.Eureka`) | Tự động đăng ký `chat-service` vào Eureka |
| **Inter-service RPC**| gRPC Stubs Java | **gRPC Client .NET** (`Grpc.Net.ClientFactory`) | Gọi `identity-service` và `workspace-service` |
| **API Documentation**| SpringDoc OpenAPI | Swashbuckle OpenAPI (Swagger UI) | Tích hợp JWT Bearer authorization |

---

## 📁 Cấu Trúc Dự Án

```
chat-service/
├── Controllers/
│   ├── ChatController.cs             # REST API quản lý phiên chat, gửi tin nhắn, hỏi AI
│   └── HealthController.cs           # Health check endpoint
├── Data/
│   └── ChatDbContext.cs              # EF Core DbContext cho PostgreSQL (snake_case)
├── Domain/
│   ├── Entities/
│   │   ├── ChatSession.cs            # Phiên chat (DIRECT, PROJECT, AI)
│   │   ├── ChatMessage.cs            # Tin nhắn trao đổi
│   │   └── ChatSessionParticipant.cs # Thành viên tham gia phòng chat
│   └── Enums/
│       ├── ChatSessionStatus.cs      # ACTIVE, CLOSED, BLOCKED
│       ├── ChatType.cs               # DIRECT, PROJECT, AI
│       └── SenderRole.cs             # CLIENT, EXPERT, SYSTEM, AI_BOT
├── DTOs/
│   ├── Requests/
│   │   ├── CreateChatSessionRequest.cs
│   │   └── SendMessageRequest.cs
│   └── Responses/
│       ├── ApiEnvelope.cs            # Response chuẩn: { code, message, result }
│       ├── ChatSessionResponse.cs
│       ├── MessageResponse.cs
│       ├── ParticipantDetail.cs
│       └── PageResponse.cs
├── Hubs/
│   └── ChatHub.cs                    # SignalR Hub thời gian thực (/hubs/chat)
├── Infrastructure/
│   ├── Ai/
│   │   ├── IGeminiAiService.cs
│   │   └── GeminiAiService.cs        # Kết nối Google Gemini API
│   └── Clients/
│       ├── IIdentityClient.cs & IdentityClient.cs   # gRPC gọi identity-service
│       └── IWorkspaceClient.cs & WorkspaceClient.cs # gRPC gọi workspace-service
├── Services/
│   ├── IChatService.cs
│   └── ChatService.cs                # Core logic chat & điều phối bot AI
├── appsettings.json
├── Dockerfile
└── ChatService.csproj
```

---

## 📡 Danh Sách API Endpoints

| Phương thức | Endpoint | Mô tả |
| :--- | :--- | :--- |
| `POST` | `/api/chat/sessions` | Tạo phòng chat mới (`type`: `"DIRECT"`, `"PROJECT"`, `"AI"`) |
| `GET` | `/api/chat/sessions/{id}` | Lấy thông tin phòng chat và danh sách người tham gia |
| `GET` | `/api/chat/sessions/mine/direct` | Lấy danh sách phòng chat trực tiếp của tôi |
| `GET` | `/api/chat/sessions/mine/project` | Lấy danh sách phòng chat theo dự án/workspace của tôi |
| `GET` | `/api/chat/sessions/mine/ai` | Lấy danh sách phòng chat với Trợ lý AI NeuroBot |
| `GET` | `/api/chat/sessions/{id}/messages`| Lấy lịch sử tin nhắn trong phòng chat (phân trang: `page`, `size`) |
| `POST` | `/api/chat/messages` | Gửi tin nhắn (Nếu là phòng chat AI, bot sẽ tự động trả lời) |
| `POST` | `/api/chat/ai/ask` | Chat nhanh trực tiếp với NeuroBot không cần tạo phòng |
| `POST` | `/api/chat/sessions/{id}/close` | Đóng phiên chat |
| `POST` | `/api/chat/sessions/{id}/block` | Khóa phiên chat |
| `GET` | `/api/chat/health` | Kiểm tra tình trạng hoạt động của service |

---

## 🤖 Tính Năng AI Chatbot (NeuroBot)

Khi người dùng tạo phòng chat với `type = "AI"`:
1. Hệ thống tự động tạo session và thêm người dùng cùng bot `NEURO_BOT` (`SenderRole.AI_BOT`).
2. Bot gửi lời chào ban đầu giới thiệu về nền tảng **Neuro Bridge**.
3. Mỗi khi người dùng gửi tin nhắn:
   - Tin nhắn của người dùng được lưu vào database PostgreSQL.
   - Tin nhắn lập tức được phát qua SignalR.
   - Hệ thống ngầm gọi **Gemini 2.5 Flash** với ngữ cảnh các câu trao đổi trước đó.
   - Câu trả lời của bot được lưu vào database và phát ngay qua SignalR tới người dùng.

---

## 🚀 Hướng Dẫn Chạy Cục Bộ

### 1. Chạy Trực Tiếp Bằng .NET CLI
```bash
cd chat-service
dotnet run
```
Dịch vụ sẽ khởi động tại cổng `http://localhost:4565`.

### 2. Truy Cập Swagger UI
```
http://localhost:4565/swagger
```

### 3. Chạy Bằng Docker Compose
```bash
docker compose up -d --build chat-service
```
