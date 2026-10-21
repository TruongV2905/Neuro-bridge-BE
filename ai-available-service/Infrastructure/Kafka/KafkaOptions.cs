namespace AiAvailableService.Infrastructure.Kafka;

public sealed class KafkaOptions
{
    public const string SectionName = "Kafka";

    public string BootstrapServers { get; set; } = "localhost:9092";
    public string AiProductPurchasedTopic { get; set; } = "neural-bridge.ai-product.purchased";

    public void Validate()
    {
        if (string.IsNullOrWhiteSpace(BootstrapServers))
        {
            throw new InvalidOperationException("Kafka:BootstrapServers is required.");
        }

        if (!string.Equals(AiProductPurchasedTopic, "neural-bridge.ai-product.purchased", StringComparison.Ordinal))
        {
            throw new InvalidOperationException("Kafka:AiProductPurchasedTopic must be neural-bridge.ai-product.purchased.");
        }
    }
}
