using ChatService.Domain.Entities;
using Microsoft.EntityFrameworkCore;

namespace ChatService.Data;

public class ChatDbContext : DbContext
{
    public ChatDbContext(DbContextOptions<ChatDbContext> options) : base(options)
    {
    }

    public DbSet<ChatSession> ChatSessions => Set<ChatSession>();
    public DbSet<ChatMessage> Messages => Set<ChatMessage>();
    public DbSet<ChatSessionParticipant> ChatSessionParticipants => Set<ChatSessionParticipant>();

    protected override void OnModelCreating(ModelBuilder modelBuilder)
    {
        base.OnModelCreating(modelBuilder);

        // ChatSession mapping
        modelBuilder.Entity<ChatSession>(entity =>
        {
            entity.ToTable("chat_session");
            entity.HasKey(e => e.Id);
            entity.Property(e => e.Id).HasColumnName("id");
            entity.Property(e => e.WorkspaceId).HasColumnName("workspace_id");
            entity.Property(e => e.Status)
                .HasColumnName("status")
                .HasConversion<string>();
            entity.Property(e => e.Type)
                .HasColumnName("type")
                .HasConversion<string>();
            entity.Property(e => e.CreatedAt).HasColumnName("created_at");

            entity.HasMany(e => e.Messages)
                .WithOne(m => m.ChatSession)
                .HasForeignKey(m => m.ChatSessionId)
                .OnDelete(DeleteBehavior.Cascade);

            entity.HasMany(e => e.Participants)
                .WithOne(p => p.ChatSession)
                .HasForeignKey(p => p.ChatSessionId)
                .OnDelete(DeleteBehavior.Cascade);
        });

        // ChatMessage mapping
        modelBuilder.Entity<ChatMessage>(entity =>
        {
            entity.ToTable("messages");
            entity.HasKey(e => e.Id);
            entity.Property(e => e.Id).HasColumnName("id");
            entity.Property(e => e.ChatSessionId).HasColumnName("chat_session_id");
            entity.Property(e => e.SenderCode).HasColumnName("sender_code").IsRequired();
            entity.Property(e => e.SenderRole)
                .HasColumnName("sender_role")
                .HasConversion<string>()
                .IsRequired();
            entity.Property(e => e.Content).HasColumnName("content").HasColumnType("text").IsRequired();
            entity.Property(e => e.CreatedAt).HasColumnName("created_at");
        });

        // ChatSessionParticipant mapping
        modelBuilder.Entity<ChatSessionParticipant>(entity =>
        {
            entity.ToTable("chat_session_participants");
            entity.HasKey(e => new { e.ChatSessionId, e.UserCode });
            entity.Property(e => e.ChatSessionId).HasColumnName("chat_session_id");
            entity.Property(e => e.UserCode).HasColumnName("user_code").IsRequired();
        });
    }
}
