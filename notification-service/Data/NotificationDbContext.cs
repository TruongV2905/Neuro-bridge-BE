using Microsoft.EntityFrameworkCore;
using NotificationService.Models.Entities;

namespace NotificationService.Data;

public class NotificationDbContext : DbContext
{
    public NotificationDbContext(DbContextOptions<NotificationDbContext> options)
        : base(options)
    {
    }

    public DbSet<Notification> Notifications => Set<Notification>();

    protected override void OnModelCreating(ModelBuilder modelBuilder)
    {
        base.OnModelCreating(modelBuilder);

        modelBuilder.Entity<Notification>(entity =>
        {
            entity.ToTable("notifications");

            entity.HasKey(e => e.Id);

            entity.Property(e => e.Id)
                .HasColumnName("id");

            entity.Property(e => e.RecipientUserId)
                .HasColumnName("recipient_user_id")
                .IsRequired();

            entity.Property(e => e.Type)
                .HasColumnName("type")
                .IsRequired();

            entity.Property(e => e.Title)
                .HasColumnName("title")
                .IsRequired();

            entity.Property(e => e.Message)
                .HasColumnName("message")
                .IsRequired();

            entity.Property(e => e.ResourceId)
                .HasColumnName("resource_id");

            entity.Property(e => e.WorkspaceId)
                .HasColumnName("workspace_id");

            entity.Property(e => e.EventId)
                .HasColumnName("event_id");

            entity.Property(e => e.PurchaseId)
                .HasColumnName("purchase_id");

            entity.Property(e => e.Deeplink)
                .HasColumnName("deeplink");

            entity.Property(e => e.IsRead)
                .HasColumnName("is_read")
                .HasDefaultValue(false);

            entity.Property(e => e.CreatedAt)
                .HasColumnName("created_at")
                .HasDefaultValueSql("CURRENT_TIMESTAMP");

            entity.HasIndex(e => new { e.RecipientUserId, e.CreatedAt });
            entity.HasIndex(e => new { e.RecipientUserId, e.IsRead });
            entity.HasIndex(e => new { e.EventId, e.RecipientUserId })
                .IsUnique()
                .HasDatabaseName("ux_notifications_event_recipient")
                .HasFilter("event_id IS NOT NULL");
            entity.HasIndex(e => new { e.Type, e.PurchaseId, e.RecipientUserId })
                .IsUnique()
                .HasDatabaseName("ux_notifications_purchase_recipient")
                .HasFilter("purchase_id IS NOT NULL");
        });
    }
}
