using AiAvailableService.Configuration;
using Com.Payment.Grpc;

namespace AiAvailableService.Infrastructure.Payment;

public static class PaymentGrpcClientRegistration
{
    public static IServiceCollection AddPaymentGrpcFoundation(this IServiceCollection services, IConfiguration configuration)
    {
        var address = configuration[$"{GrpcServiceOptions.SectionName}:Payment:Address"];
        if (!Uri.TryCreate(address, UriKind.Absolute, out var uri))
        {
            throw new InvalidOperationException("GrpcServices:Payment:Address must be an absolute URI.");
        }

        // This registers the client generated from the current payment.proto only.
        services.AddGrpcClient<PaymentService.PaymentServiceClient>(options => options.Address = uri);
        services.AddSingleton<IInternalServiceTokenProvider, InternalServiceTokenProvider>();
        services.AddScoped<IPaymentSettlementGateway, PaymentSettlementGateway>();
        return services;
    }
}
