using ChatService.Domain.Enums;

namespace ChatService.DTOs.Requests;

public class CreateChatSessionRequest
{
    public Guid? WorkspaceId { get; set; }

    public ChatType Type { get; set; } = ChatType.DIRECT;

    public HashSet<string>? ParticipantCodes { get; set; }
}
