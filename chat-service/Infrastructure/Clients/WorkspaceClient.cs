using Com.Workspace.Grpc;

namespace ChatService.Infrastructure.Clients;

public class WorkspaceClient : IWorkspaceClient
{
    private readonly WorkspaceService.WorkspaceServiceClient _client;
    private readonly ILogger<WorkspaceClient> _logger;

    public WorkspaceClient(WorkspaceService.WorkspaceServiceClient client, ILogger<WorkspaceClient> logger)
    {
        _client = client;
        _logger = logger;
    }

    public async Task<WorkspaceResponse?> GetWorkspaceByIdAsync(string workspaceId)
    {
        try
        {
            return await _client.GetWorkspaceByIdAsync(new GetWorkspaceByIdRequest
            {
                WorkspaceId = workspaceId
            });
        }
        catch (Exception ex)
        {
            _logger.LogWarning(ex, "[WorkspaceClient] Failed to get workspace by ID {Id}", workspaceId);
            return null;
        }
    }
}
