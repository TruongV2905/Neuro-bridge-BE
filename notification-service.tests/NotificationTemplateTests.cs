using System.Text.Json;
using NotificationService.Models.DTOs.Events;
using NotificationService.Models.Enums;
using Xunit;

namespace NotificationService.Tests;

public class NotificationTemplateTests
{
    [Fact]
    public void TestPackageSubmittedTemplate()
    {
        var @event = new NotificationEvent
        {
            EventType = "PACKAGE_SUBMITTED",
            SenderName = "John Doe",
            ResourceVersion = "1",
            WorkspaceId = Guid.Parse("11111111-1111-1111-1111-111111111111"),
            ResourceId = Guid.Parse("22222222-2222-2222-2222-222222222222")
        };

        var template = NotificationTemplate.FromEventType(@event.EventType);

        Assert.Equal("Gói bàn giao mới", template.Title);
        Assert.Equal("John Doe đã nộp gói bàn giao v1", template.BuildMessage(@event));
        Assert.Equal("/workspaces/11111111-1111-1111-1111-111111111111/packages/22222222-2222-2222-2222-222222222222", template.BuildDeeplink(@event));
    }

    [Fact]
    public void TestPackageApprovedTemplate()
    {
        var @event = new NotificationEvent
        {
            EventType = "PACKAGE_APPROVED",
            SenderName = "Client A",
            ResourceVersion = "2",
            WorkspaceId = Guid.Parse("11111111-1111-1111-1111-111111111111"),
            ResourceId = Guid.Parse("22222222-2222-2222-2222-222222222222")
        };

        var template = NotificationTemplate.FromEventType(@event.EventType);

        Assert.Equal("Gói bàn giao được chấp thuận ✓", template.Title);
        Assert.Contains("Client A đã chấp thuận gói bàn giao v2", template.BuildMessage(@event));
        Assert.Contains("Tiền trong ví chung đã được giải phóng", template.BuildMessage(@event));
        Assert.Equal("/workspaces/11111111-1111-1111-1111-111111111111/packages/22222222-2222-2222-2222-222222222222", template.BuildDeeplink(@event));
    }

    [Fact]
    public void TestPackageRejectedTemplate()
    {
        var @event = new NotificationEvent
        {
            EventType = "PACKAGE_REJECTED",
            SenderName = "Client B",
            ResourceVersion = "1",
            Feedback = "Needs bugfix",
            WorkspaceId = Guid.Parse("11111111-1111-1111-1111-111111111111"),
            ResourceId = Guid.Parse("22222222-2222-2222-2222-222222222222")
        };

        var template = NotificationTemplate.FromEventType(@event.EventType);

        Assert.Equal("Gói bàn giao bị từ chối", template.Title);
        Assert.Equal("Client B đã từ chối gói bàn giao v1: Needs bugfix", template.BuildMessage(@event));
    }

    [Fact]
    public void TestKafkaEventJsonDeserialization()
    {
        string json = """
        {
            "eventType": "WORKSPACE_INVITED",
            "domain": "workspace",
            "recipientUserId": "3fa85f64-5717-4562-b3fc-2c963f66afa6",
            "workspaceId": "e1136453-e58f-4d9d-b851-9e73ef5e0031"
        }
        """;

        var @event = JsonSerializer.Deserialize<NotificationEvent>(json);
        Assert.NotNull(@event);
        Assert.Equal("WORKSPACE_INVITED", @event.EventType);
        Assert.Equal("workspace", @event.Domain);
        Assert.Equal(Guid.Parse("3fa85f64-5717-4562-b3fc-2c963f66afa6"), @event.RecipientUserId);
        Assert.Equal(Guid.Parse("e1136453-e58f-4d9d-b851-9e73ef5e0031"), @event.WorkspaceId);

        var template = NotificationTemplate.FromEventType(@event.EventType);
        Assert.Equal("Bạn đã được thêm vào Workspace", template.Title);
        Assert.Equal("/workspaces/e1136453-e58f-4d9d-b851-9e73ef5e0031", template.BuildDeeplink(@event));
    }

    [Fact]
    public void TestUnknownEventTypeFallsBackToDefault()
    {
        var template = NotificationTemplate.FromEventType("NON_EXISTENT_EVENT");
        Assert.Equal("Thông báo mới", template.Title);
        Assert.Null(template.BuildDeeplink(new NotificationEvent()));
    }
}
