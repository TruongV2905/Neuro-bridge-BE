using System.ComponentModel.DataAnnotations;

namespace ChatService.DTOs.Requests;

public class SendMessageRequest
{
    [Required]
    public Guid ChatSessionId { get; set; }

    [Required]
    public string Content { get; set; } = string.Empty;
}
