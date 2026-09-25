using System.Text.Json;
using NotificationService.Models.DTOs.Responses;
using StackExchange.Redis;

namespace NotificationService.Services;

public class NotificationCacheService : INotificationCacheService
{
    private readonly IConnectionMultiplexer _redis;
    private readonly ILogger<NotificationCacheService> _logger;
    private static readonly TimeSpan Ttl = TimeSpan.FromHours(24);

    private const string UnreadCountKey = "notification:unread:{0}";
    private const string NotificationListKey = "notification:list:{0}";

    private static readonly JsonSerializerOptions JsonOptions = new()
    {
        PropertyNamingPolicy = JsonNamingPolicy.CamelCase,
        PropertyNameCaseInsensitive = true
    };

    public NotificationCacheService(IConnectionMultiplexer redis, ILogger<NotificationCacheService> logger)
    {
        _redis = redis;
        _logger = logger;
    }

    private IDatabase? GetDatabase()
    {
        try
        {
            return _redis.GetDatabase();
        }
        catch (Exception ex)
        {
            _logger.LogWarning(ex, "[Redis] Cannot connect to Redis database.");
            return null;
        }
    }

    public async Task<long> GetUnreadCountAsync(Guid userId)
    {
        try
        {
            var db = GetDatabase();
            if (db == null) return -1;

            string key = string.Format(UnreadCountKey, userId);
            RedisValue value = await db.StringGetAsync(key);

            if (value.HasValue && long.TryParse(value.ToString(), out long count))
            {
                return count;
            }

            return -1;
        }
        catch (Exception ex)
        {
            _logger.LogWarning(ex, "[Redis] Error reading unread count for user {UserId}", userId);
            return -1;
        }
    }

    public async Task SetUnreadCountAsync(Guid userId, long count)
    {
        try
        {
            var db = GetDatabase();
            if (db == null) return;

            string key = string.Format(UnreadCountKey, userId);
            await db.StringSetAsync(key, count, Ttl);
        }
        catch (Exception ex)
        {
            _logger.LogWarning(ex, "[Redis] Error setting unread count for user {UserId}", userId);
        }
    }

    public async Task IncrementUnreadCountAsync(Guid userId)
    {
        try
        {
            var db = GetDatabase();
            if (db == null) return;

            string key = string.Format(UnreadCountKey, userId);
            if (await db.KeyExistsAsync(key))
            {
                await db.StringIncrementAsync(key);
                await db.KeyExpireAsync(key, Ttl);
            }
        }
        catch (Exception ex)
        {
            _logger.LogWarning(ex, "[Redis] Error incrementing unread count for user {UserId}", userId);
        }
    }

    public async Task ResetUnreadCountAsync(Guid userId)
    {
        try
        {
            var db = GetDatabase();
            if (db == null) return;

            string key = string.Format(UnreadCountKey, userId);
            await db.StringSetAsync(key, 0L, Ttl);
        }
        catch (Exception ex)
        {
            _logger.LogWarning(ex, "[Redis] Error resetting unread count for user {UserId}", userId);
        }
    }

    public async Task<List<NotificationResponse>?> GetNotificationListAsync(Guid userId)
    {
        try
        {
            var db = GetDatabase();
            if (db == null) return null;

            string key = string.Format(NotificationListKey, userId);
            RedisValue value = await db.StringGetAsync(key);

            if (!value.HasValue) return null;

            return JsonSerializer.Deserialize<List<NotificationResponse>>(value.ToString(), JsonOptions);
        }
        catch (Exception ex)
        {
            _logger.LogWarning(ex, "[Redis] Failed to deserialize notification list for user {UserId}", userId);
            return null;
        }
    }

    public async Task SetNotificationListAsync(Guid userId, List<NotificationResponse> list)
    {
        try
        {
            var db = GetDatabase();
            if (db == null) return;

            string key = string.Format(NotificationListKey, userId);
            string json = JsonSerializer.Serialize(list, JsonOptions);
            await db.StringSetAsync(key, json, Ttl);
        }
        catch (Exception ex)
        {
            _logger.LogWarning(ex, "[Redis] Error caching notification list for user {UserId}", userId);
        }
    }

    public async Task AddToNotificationListAsync(Guid userId, NotificationResponse notification)
    {
        try
        {
            var db = GetDatabase();
            if (db == null) return;

            string key = string.Format(NotificationListKey, userId);
            if (await db.KeyExistsAsync(key))
            {
                var current = await GetNotificationListAsync(userId);
                if (current != null)
                {
                    current.Insert(0, notification);
                    string json = JsonSerializer.Serialize(current, JsonOptions);
                    await db.StringSetAsync(key, json, Ttl);
                }
            }
        }
        catch (Exception ex)
        {
            _logger.LogWarning(ex, "[Redis] Error prepending notification to cache for user {UserId}", userId);
        }
    }

    public async Task InvalidateUserAsync(Guid userId)
    {
        try
        {
            var db = GetDatabase();
            if (db == null) return;

            string unreadKey = string.Format(UnreadCountKey, userId);
            string listKey = string.Format(NotificationListKey, userId);

            await Task.WhenAll(
                db.KeyDeleteAsync(unreadKey),
                db.KeyDeleteAsync(listKey)
            );
        }
        catch (Exception ex)
        {
            _logger.LogWarning(ex, "[Redis] Error invalidating cache for user {UserId}", userId);
        }
    }
}
