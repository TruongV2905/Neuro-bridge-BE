using ChatService.Domain.Enums;

namespace ChatService.DTOs.Responses;

public class MessageResponse
{
    public Guid Id { get; set; }
    public Guid ChatSessionId { get; set; }
    public string SenderCode { get; set; } = string.Empty;
    public SenderRole SenderRole { get; set; }
    public string Content { get; set; } = string.Empty;
    public DateTime CreatedAt { get; set; }
}
