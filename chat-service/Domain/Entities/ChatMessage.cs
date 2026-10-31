using ChatService.Domain.Enums;

namespace ChatService.Domain.Entities;

public class ChatMessage
{
    public Guid Id { get; set; } = Guid.NewGuid();

    public Guid ChatSessionId { get; set; }

    public ChatSession? ChatSession { get; set; }

    public string SenderCode { get; set; } = string.Empty;

    public SenderRole SenderRole { get; set; } = SenderRole.CLIENT;

    public string Content { get; set; } = string.Empty;

    public DateTime CreatedAt { get; set; } = DateTime.UtcNow;
}
