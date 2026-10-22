using ChatService.Domain.Enums;

namespace ChatService.Domain.Entities;

public class ChatSession
{
    public Guid Id { get; set; } = Guid.NewGuid();

    public Guid? WorkspaceId { get; set; }

    public ChatSessionStatus Status { get; set; } = ChatSessionStatus.ACTIVE;

    public ChatType Type { get; set; } = ChatType.DIRECT;

    public DateTime CreatedAt { get; set; } = DateTime.UtcNow;

    public ICollection<ChatMessage> Messages { get; set; } = new List<ChatMessage>();

    public ICollection<ChatSessionParticipant> Participants { get; set; } = new List<ChatSessionParticipant>();
}
