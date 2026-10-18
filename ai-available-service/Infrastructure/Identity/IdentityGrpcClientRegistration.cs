using AiAvailableService.Configuration;
using Com.Identity.Grpc;
using Microsoft.Extensions.Options;

namespace AiAvailableService.Infrastructure.Identity;

public static class IdentityGrpcClientRegistration
{
    public static IServiceCollection AddIdentityGrpcFoundation(this IServiceCollection services, IConfiguration configuration)
    {
        var address = GetRequiredAddress(configuration, "Identity");
        services.AddGrpcClient<UserService.UserServiceClient>(options => options.Address = address);
        return services;
    }

    private static Uri GetRequiredAddress(IConfiguration configuration, string serviceName)
    {
        var address = configuration[$"{GrpcServiceOptions.SectionName}:{serviceName}:Address"];
        if (!Uri.TryCreate(address, UriKind.Absolute, out var uri))
        {
            throw new InvalidOperationException(
                $"GrpcServices:{serviceName}:Address must be an absolute URI.");
        }

        return uri;
    }
}
