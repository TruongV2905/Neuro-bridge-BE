using NotificationService.Models.DTOs.Events;

namespace NotificationService.Models.Enums;

public class NotificationTemplateDefinition
{
    public string Title { get; }
    public Func<NotificationEvent, string> MessageBuilder { get; }
    public Func<NotificationEvent, string?> DeeplinkBuilder { get; }

    public NotificationTemplateDefinition(
        string title,
        Func<NotificationEvent, string> messageBuilder,
        Func<NotificationEvent, string?> deeplinkBuilder)
    {
        Title = title;
        MessageBuilder = messageBuilder;
        DeeplinkBuilder = deeplinkBuilder;
    }

    public string BuildMessage(NotificationEvent @event) => MessageBuilder(@event);
    public string? BuildDeeplink(NotificationEvent @event) => DeeplinkBuilder(@event);
}

public static class NotificationTemplate
{
    private static readonly Dictionary<string, NotificationTemplateDefinition> Templates =
        new(StringComparer.OrdinalIgnoreCase)
        {
            ["PACKAGE_SUBMITTED"] = new(
                "Gói bàn giao mới",
                e => $"{e.SenderName} đã nộp gói bàn giao v{e.ResourceVersion}",
                e => $"/workspaces/{e.WorkspaceId}/packages/{e.ResourceId}"
            ),
            ["PACKAGE_RESUBMITTED"] = new(
                "Gói bàn giao đã được nộp lại",
                e => $"{e.SenderName} đã nộp lại gói bàn giao v{e.ResourceVersion}",
                e => $"/workspaces/{e.WorkspaceId}/packages/{e.ResourceId}"
            ),
            ["PACKAGE_APPROVED"] = new(
                "Gói bàn giao được chấp thuận ✓",
                e => $"{e.SenderName} đã chấp thuận gói bàn giao v{e.ResourceVersion}. Tiền trong ví chung đã được giải phóng vào tài khoản của bạn.",
                e => $"/workspaces/{e.WorkspaceId}/packages/{e.ResourceId}"
            ),
            ["PACKAGE_REJECTED"] = new(
                "Gói bàn giao bị từ chối",
                e => $"{e.SenderName} đã từ chối gói bàn giao v{e.ResourceVersion}: {e.Feedback}",
                e => $"/workspaces/{e.WorkspaceId}/packages/{e.ResourceId}"
            ),
            ["WORKSPACE_INVITED"] = new(
                "Bạn đã được thêm vào Workspace",
                _ => "Neural Bridge đã tạo Workspace mới cho dự án của bạn. Hãy bắt đầu cộng tác!",
                e => $"/workspaces/{e.WorkspaceId}"
            ),
            ["CONTRACT_SIGN_REQUIRED"] = new(
                "Hợp đồng cần được ký",
                e => $"{e.SenderName} đã ký hợp đồng. Vui lòng vào ký để tiếp tục",
                e => $"/contracts/{e.ResourceId}"
            ),
            ["CONTRACT_FULLY_SIGNED"] = new(
                "Hợp đồng đã được ký đầy đủ",
                e => $"Hợp đồng với {e.SenderName} đã được ký đầy đủ. Vui lòng tiến hành thanh toán để bắt đầu dự án.",
                e => $"/contracts/{e.ResourceId}"
            ),
            ["JOBPOST_APPLIED"] = new(
                "Có Expert mới ứng tuyển",
                e => $"{e.SenderName} đã ứng tuyển vào công việc của bạn",
                e => $"/job-posts/{e.ResourceId}/applies"
            ),
            ["PROJECT_COMPLETED_PAYMENT_RELEASED"] = new(
                "Dự án đã hoàn tất",
                e => $"Dự án \"{e.SenderName}\" đã hoàn tất. Số tiền dự án đã được chuyển vào ví của bạn.",
                e => $"/workspaces/{e.WorkspaceId}/projects/{e.ResourceId}"
            )
        };

    private static readonly NotificationTemplateDefinition UnknownTemplate = new(
        "Thông báo mới",
        _ => "Có sự kiện mới trong hệ thống",
        _ => null
    );

    public static NotificationTemplateDefinition FromEventType(string? eventType)
    {
        if (!string.IsNullOrWhiteSpace(eventType) && Templates.TryGetValue(eventType, out var template))
        {
            return template;
        }

        return UnknownTemplate;
    }
}
