using System.Text.Json.Serialization;

namespace NotificationService.Models.DTOs.Responses;

public class NotificationPayload
{
    [JsonPropertyName("type")]
    public string? Type { get; set; }

    [JsonPropertyName("title")]
    public string? Title { get; set; }

    [JsonPropertyName("message")]
    public string? Message { get; set; }

    [JsonPropertyName("resourceId")]
    public Guid? ResourceId { get; set; }

    [JsonPropertyName("workspaceId")]
    public Guid? WorkspaceId { get; set; }

    [JsonPropertyName("deeplink")]
    public string? Deeplink { get; set; }

    [JsonPropertyName("timestamp")]
    public DateTime Timestamp { get; set; }
}
