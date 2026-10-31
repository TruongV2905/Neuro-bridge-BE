using ChatService.Domain.Enums;

namespace ChatService.DTOs.Responses;

public class ChatSessionResponse
{
    public Guid Id { get; set; }
    public Guid? WorkspaceId { get; set; }
    public ChatSessionStatus Status { get; set; }
    public ChatType Type { get; set; }
    public DateTime CreatedAt { get; set; }
    public HashSet<string> ParticipantCodes { get; set; } = new();
    public List<ParticipantDetail> Participants { get; set; } = new();
    public MessageResponse? LastMessage { get; set; }
}
