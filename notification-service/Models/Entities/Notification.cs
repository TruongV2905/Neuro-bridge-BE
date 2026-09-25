using System.ComponentModel.DataAnnotations;
using System.ComponentModel.DataAnnotations.Schema;

namespace NotificationService.Models.Entities;

[Table("notifications")]
public class Notification
{
    [Key]
    [Column("id")]
    public Guid Id { get; set; } = Guid.NewGuid();

    [Required]
    [Column("recipient_user_id")]
    public Guid RecipientUserId { get; set; }

    [Required]
    [Column("type")]
    public string Type { get; set; } = string.Empty;

    [Required]
    [Column("title")]
    public string Title { get; set; } = string.Empty;

    [Required]
    [Column("message")]
    public string Message { get; set; } = string.Empty;

    [Column("resource_id")]
    public Guid? ResourceId { get; set; }

    [Column("workspace_id")]
    public Guid? WorkspaceId { get; set; }

    [Column("deeplink")]
    public string? Deeplink { get; set; }

    [Column("is_read")]
    public bool IsRead { get; set; } = false;

    [Column("created_at")]
    public DateTime CreatedAt { get; set; } = DateTime.UtcNow;
}
