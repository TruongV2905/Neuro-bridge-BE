namespace ChatService.Domain.Entities;

public class ChatSessionParticipant
{
    public Guid ChatSessionId { get; set; }

    public ChatSession? ChatSession { get; set; }

    public string UserCode { get; set; } = string.Empty;
}
