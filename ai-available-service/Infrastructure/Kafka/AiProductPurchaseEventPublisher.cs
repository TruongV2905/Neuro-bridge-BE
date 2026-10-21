using System.Text.Json;
using Confluent.Kafka;

namespace AiAvailableService.Infrastructure.Kafka;

public interface IAiProductPurchaseEventPublisher
{
    /// <summary>
    /// Best-effort only. Implementations must absorb transport failures so a
    /// committed Purchase is never reported as a failed payment.
    /// </summary>
    Task PublishAsync(AiProductPurchasedEvent @event, CancellationToken cancellationToken = default);
}

public sealed class KafkaAiProductPurchaseEventPublisher : IAiProductPurchaseEventPublisher, IDisposable
{
    private static readonly JsonSerializerOptions JsonOptions = new(JsonSerializerDefaults.Web);

    private readonly IProducer<string, string> _producer;
    private readonly KafkaOptions _options;
    private readonly ILogger<KafkaAiProductPurchaseEventPublisher> _logger;

    public KafkaAiProductPurchaseEventPublisher(
        IProducer<string, string> producer,
        KafkaOptions options,
        ILogger<KafkaAiProductPurchaseEventPublisher> logger)
    {
        _producer = producer;
        _options = options;
        _logger = logger;
    }

    public async Task PublishAsync(AiProductPurchasedEvent @event, CancellationToken cancellationToken = default)
    {
        try
        {
            _logger.LogInformation(
                "AI Product purchase event publish attempted: EventId={EventId} PurchaseId={PurchaseId} ProductId={ProductId} Topic={Topic}",
                @event.EventId, @event.PurchaseId, @event.ProductId, _options.AiProductPurchasedTopic);

            var value = JsonSerializer.Serialize(@event, JsonOptions);
            await _producer.ProduceAsync(
                _options.AiProductPurchasedTopic,
                new Message<string, string> { Key = @event.BuyerId.ToString(), Value = value },
                cancellationToken);

            _logger.LogInformation(
                "AI Product purchase event publish succeeded: EventId={EventId} PurchaseId={PurchaseId} ProductId={ProductId} Topic={Topic}",
                @event.EventId, @event.PurchaseId, @event.ProductId, _options.AiProductPurchasedTopic);
        }
        catch (Exception ex) when (ex is ProduceException<string, string> or KafkaException or OperationCanceledException)
        {
            _logger.LogWarning(ex,
                "AI Product purchase event publish failed best-effort: EventId={EventId} PurchaseId={PurchaseId} ProductId={ProductId} Topic={Topic} ErrorCategory={ErrorCategory}",
                @event.EventId, @event.PurchaseId, @event.ProductId, _options.AiProductPurchasedTopic, ex.GetType().Name);
        }
        catch (Exception ex)
        {
            _logger.LogWarning(ex,
                "AI Product purchase event publish failed best-effort: EventId={EventId} PurchaseId={PurchaseId} ProductId={ProductId} Topic={Topic} ErrorCategory=Unexpected",
                @event.EventId, @event.PurchaseId, @event.ProductId, _options.AiProductPurchasedTopic);
        }
    }

    public void Dispose() => _producer.Dispose();
}

public static class AiProductPurchaseKafkaRegistration
{
    public static IServiceCollection AddAiProductPurchaseKafka(this IServiceCollection services, IConfiguration configuration)
    {
        var options = configuration.GetSection(KafkaOptions.SectionName).Get<KafkaOptions>() ?? new KafkaOptions();
        options.BootstrapServers = Environment.GetEnvironmentVariable("KAFKA_SERVERS")
            ?? options.BootstrapServers;
        options.Validate();

        services.AddSingleton(options);
        services.AddSingleton<IProducer<string, string>>(_ => new ProducerBuilder<string, string>(new ProducerConfig
        {
            BootstrapServers = options.BootstrapServers,
            MessageTimeoutMs = 5000,
            Acks = Acks.Leader,
            EnableIdempotence = false
        }).Build());
        services.AddSingleton<IAiProductPurchaseEventPublisher, KafkaAiProductPurchaseEventPublisher>();
        return services;
    }
}
