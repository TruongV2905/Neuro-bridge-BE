using ChatService.Domain.Enums;
using ChatService.DTOs.Requests;
using ChatService.DTOs.Responses;

namespace ChatService.Services;

public interface IChatService
{
    Task<ChatSessionResponse> CreateChatSessionAsync(CreateChatSessionRequest request, string userCode, string? userRole, string? bearerToken);
    Task<ChatSessionResponse?> GetChatSessionAsync(Guid id, string? bearerToken);
    Task<List<ChatSessionResponse>> GetMyChatSessionsAsync(ChatType type, string userCode, string? bearerToken);
    Task<PageResponse<MessageResponse>> GetMessagesAsync(Guid sessionId, int page, int size);
    Task<MessageResponse> SendMessageAsync(SendMessageRequest request, string userCode, string? userRole, string? bearerToken);
    Task<ChatSessionResponse> AddParticipantAsync(Guid sessionId, string userCode, string? bearerToken);
    Task<ChatSessionResponse> RemoveParticipantAsync(Guid sessionId, string userCode, string? bearerToken);
    Task<ChatSessionResponse> CloseChatSessionAsync(Guid sessionId, string? bearerToken);
    Task<ChatSessionResponse> BlockChatSessionAsync(Guid sessionId, string? bearerToken);
}
