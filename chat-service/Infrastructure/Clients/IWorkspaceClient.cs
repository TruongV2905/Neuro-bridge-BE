using Com.Workspace.Grpc;

namespace ChatService.Infrastructure.Clients;

public interface IWorkspaceClient
{
    Task<WorkspaceResponse?> GetWorkspaceByIdAsync(string workspaceId);
}
