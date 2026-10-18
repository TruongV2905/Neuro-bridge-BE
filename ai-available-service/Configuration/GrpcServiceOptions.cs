namespace AiAvailableService.Configuration;

public sealed class GrpcServiceOptions
{
    public const string SectionName = "GrpcServices";

    public GrpcEndpointOptions Identity { get; init; } = new();
    public GrpcEndpointOptions Payment { get; init; } = new();
}

public sealed class GrpcEndpointOptions
{
    public string Address { get; init; } = string.Empty;
}
