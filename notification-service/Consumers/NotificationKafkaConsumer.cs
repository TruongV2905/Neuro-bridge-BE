using System.Text.Json;
using Confluent.Kafka;
using Confluent.Kafka.Admin;
using NotificationService.Models.DTOs.Events;
using NotificationService.Services;

namespace NotificationService.Consumers;

public class NotificationKafkaConsumer : BackgroundService
{
    private readonly IConfiguration _configuration;
    private readonly IServiceScopeFactory _scopeFactory;
    private readonly ILogger<NotificationKafkaConsumer> _logger;

    private static readonly string[] Topics =
    [
        "neural-bridge.package.submitted",
        "neural-bridge.package.resubmitted",
        "neural-bridge.package.approved",
        "neural-bridge.package.rejected",
        "neural-bridge.workspace.invited",
        "neural-bridge.jobpost.applied",
        "neural-bridge.jobpost.accepted",
        "neural-bridge.contract.sign-required",
        "neural-bridge.contract.fully-signed",
        "neural-bridge.project.completed-payment-released",
        "neural-bridge.ai-product.purchased"
    ];

    private static readonly JsonSerializerOptions JsonOptions = new()
    {
        PropertyNameCaseInsensitive = true
    };

    public NotificationKafkaConsumer(
        IConfiguration configuration,
        IServiceScopeFactory scopeFactory,
        ILogger<NotificationKafkaConsumer> logger)
    {
        _configuration = configuration;
        _scopeFactory = scopeFactory;
        _logger = logger;
    }

    protected override async Task ExecuteAsync(CancellationToken stoppingToken)
    {
        // Run on background thread
        await Task.Yield();

        string bootstrapServers = Environment.GetEnvironmentVariable("KAFKA_SERVERS")
            ?? _configuration["Kafka:BootstrapServers"]
            ?? "localhost:9092";

        var config = new ConsumerConfig
        {
            BootstrapServers = bootstrapServers,
            GroupId = Environment.GetEnvironmentVariable("KAFKA_GROUP_ID")
                ?? _configuration["Kafka:GroupId"]
                ?? "notification-service",
            AutoOffsetReset = AutoOffsetReset.Earliest,
            EnableAutoCommit = false,
            AllowAutoCreateTopics = true
        };

        _logger.LogInformation("[Neural Bridge] Starting Kafka consumer on {BootstrapServers} for group {GroupId}",
            bootstrapServers, config.GroupId);

        await EnsureTopicsExistAsync(bootstrapServers, stoppingToken);

        while (!stoppingToken.IsCancellationRequested)
        {
            try
            {
                using var consumer = new ConsumerBuilder<string, string>(config)
                    .SetErrorHandler((_, e) =>
                    {
                        if (e.Code != ErrorCode.UnknownTopicOrPart)
                        {
                            _logger.LogWarning("[Neural Bridge] Kafka error ({Code}): {Reason}", e.Code, e.Reason);
                        }
                    })
                    .Build();

                consumer.Subscribe(Topics);
                _logger.LogInformation("[Neural Bridge] Subscribed to {Count} Kafka topics", Topics.Length);

                while (!stoppingToken.IsCancellationRequested)
                {
                    ConsumeResult<string, string>? consumeResult = null;
                    try
                    {
                        consumeResult = consumer.Consume(stoppingToken);
                        if (consumeResult == null || string.IsNullOrWhiteSpace(consumeResult.Message?.Value))
                        {
                            if (consumeResult != null)
                            {
                                consumer.Commit(consumeResult);
                            }
                            continue;
                        }

                        string json = consumeResult.Message.Value;
                        if (string.Equals(consumeResult.Topic, "neural-bridge.ai-product.purchased", StringComparison.Ordinal))
                        {
                            await ProcessAiProductPurchasedEventAsync(json, consumeResult.Topic, stoppingToken);
                            consumer.Commit(consumeResult);
                            continue;
                        }

                        var @event = JsonSerializer.Deserialize<NotificationEvent>(json, JsonOptions);

                        if (@event != null)
                        {
                            _logger.LogInformation("[Neural Bridge] Received event={EventType} recipient={RecipientUserId} from topic={Topic}",
                                @event.EventType, @event.RecipientUserId, consumeResult.Topic);

                            using var scope = _scopeFactory.CreateScope();
                            var notificationService = scope.ServiceProvider.GetRequiredService<INotificationService>();
                            await notificationService.SendNotificationAsync(@event);
                        }
                        else
                        {
                            _logger.LogWarning("Malformed notification event ignored from topic={Topic}", consumeResult.Topic);
                        }

                        consumer.Commit(consumeResult);
                    }
                    catch (OperationCanceledException) when (stoppingToken.IsCancellationRequested)
                    {
                        break;
                    }
                    catch (ConsumeException ex) when (ex.Error.Code == ErrorCode.UnknownTopicOrPart)
                    {
                        _logger.LogWarning("[Neural Bridge] Kafka topic not yet available on broker ({Topic}): {Reason}. Retrying in 3 seconds...",
                            ex.ConsumerRecord?.Topic ?? "unknown", ex.Error.Reason);
                        await Task.Delay(3000, stoppingToken);
                    }
                    catch (Exception ex)
                    {
                        _logger.LogError(ex, "[Neural Bridge] Error processing Kafka message");
                        if (consumeResult != null)
                        {
                            consumer.Seek(consumeResult.TopicPartitionOffset);
                        }
                        await Task.Delay(1000, stoppingToken);
                    }
                }

                consumer.Close();
            }
            catch (OperationCanceledException) when (stoppingToken.IsCancellationRequested)
            {
                break;
            }
            catch (Exception ex)
            {
                _logger.LogError(ex, "[Neural Bridge] Kafka consumer encountered fatal error. Retrying in 5 seconds...");
                try
                {
                    await Task.Delay(5000, stoppingToken);
                }
                catch (OperationCanceledException)
                {
                    break;
                }
            }
        }

        _logger.LogInformation("[Neural Bridge] Kafka consumer stopped.");
    }

    private async Task ProcessAiProductPurchasedEventAsync(
        string json,
        string topic,
        CancellationToken cancellationToken)
    {
        AiProductPurchasedEvent? @event;
        try
        {
            @event = JsonSerializer.Deserialize<AiProductPurchasedEvent>(json, JsonOptions);
        }
        catch (JsonException exception)
        {
            _logger.LogWarning(exception, "Malformed AI Product purchase event ignored: Topic={Topic}", topic);
            return;
        }

        var reason = @event is null ? "payload is null" : string.Empty;
        if (@event is null || !@event.IsSupported(out reason))
        {
            _logger.LogWarning(
                "Unsupported AI Product purchase event ignored: Topic={Topic} EventId={EventId} PurchaseId={PurchaseId} Reason={Reason}",
                topic, @event?.EventId, @event?.PurchaseId, reason);
            return;
        }

        _logger.LogInformation(
            "AI Product purchase event received: EventId={EventId} PurchaseId={PurchaseId} ProductId={ProductId} Topic={Topic}",
            @event.EventId, @event.PurchaseId, @event.ProductId, topic);

        using var scope = _scopeFactory.CreateScope();
        var notificationService = scope.ServiceProvider.GetRequiredService<INotificationService>();
        var result = await notificationService.SendAiProductPurchaseNotificationAsync(@event);
        if (result == PurchaseNotificationDeliveryResult.Duplicate)
        {
            _logger.LogInformation(
                "AI Product purchase notification duplicate skipped: EventId={EventId} PurchaseId={PurchaseId} ProductId={ProductId}",
                @event.EventId, @event.PurchaseId, @event.ProductId);
            return;
        }

        _logger.LogInformation(
            "AI Product purchase notification created: EventId={EventId} PurchaseId={PurchaseId} ProductId={ProductId}",
            @event.EventId, @event.PurchaseId, @event.ProductId);
    }

    private async Task EnsureTopicsExistAsync(string bootstrapServers, CancellationToken stoppingToken)
    {
        try
        {
            using var adminClient = new AdminClientBuilder(new AdminClientConfig
            {
                BootstrapServers = bootstrapServers,
                SocketTimeoutMs = 5000
            }).Build();

            var metadata = adminClient.GetMetadata(TimeSpan.FromSeconds(5));
            var existingTopics = metadata.Topics.Select(t => t.Topic).ToHashSet();

            var topicsToCreate = Topics
                .Where(t => !existingTopics.Contains(t))
                .Select(t => new TopicSpecification
                {
                    Name = t,
                    NumPartitions = 1,
                    ReplicationFactor = 1
                })
                .ToList();

            if (topicsToCreate.Count > 0)
            {
                _logger.LogInformation("[Neural Bridge] Auto-creating {Count} missing Kafka topics...", topicsToCreate.Count);
                await adminClient.CreateTopicsAsync(topicsToCreate);
                _logger.LogInformation("[Neural Bridge] Successfully created {Count} Kafka topics.", topicsToCreate.Count);
            }
        }
        catch (CreateTopicsException ex)
        {
            foreach (var result in ex.Results)
            {
                if (result.Error.Code != ErrorCode.TopicAlreadyExists)
                {
                    _logger.LogWarning("[Neural Bridge] Topic creation status for {Topic}: {Reason}",
                        result.Topic, result.Error.Reason);
                }
            }
        }
        catch (Exception ex)
        {
            _logger.LogWarning(ex, "[Neural Bridge] Note: Could not auto-create topics via AdminClient. Will consume existing topics.");
        }
    }
}

