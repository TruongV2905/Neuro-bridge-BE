using System.Text.Json.Serialization;

namespace NotificationService.Models.DTOs.Events;

public class NotificationEvent
{
    // Loại event
    [JsonPropertyName("eventType")]
    public string? EventType { get; set; }

    [JsonPropertyName("domain")]
    public string? Domain { get; set; }

    // Người liên quan
    [JsonPropertyName("recipientUserId")]
    public Guid RecipientUserId { get; set; }

    [JsonPropertyName("recipientName")]
    public string? RecipientName { get; set; }

    [JsonPropertyName("senderUserId")]
    public Guid? SenderUserId { get; set; }

    [JsonPropertyName("senderName")]
    public string? SenderName { get; set; }

    // Resource liên quan
    [JsonPropertyName("resourceId")]
    public Guid? ResourceId { get; set; }

    [JsonPropertyName("workspaceId")]
    public Guid? WorkspaceId { get; set; }

    [JsonPropertyName("resourceVersion")]
    public string? ResourceVersion { get; set; }

    // Nội dung tuỳ chỉnh
    [JsonPropertyName("customTitle")]
    public string? CustomTitle { get; set; }

    [JsonPropertyName("customMessage")]
    public string? CustomMessage { get; set; }

    [JsonPropertyName("customDeeplink")]
    public string? CustomDeeplink { get; set; }

    [JsonPropertyName("feedback")]
    public string? Feedback { get; set; }

    // Metadata
    [JsonPropertyName("occurredAt")]
    public DateTime? OccurredAt { get; set; }
}
