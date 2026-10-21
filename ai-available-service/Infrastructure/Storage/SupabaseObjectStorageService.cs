using System.Net;
using System.Net.Http.Headers;
using System.Net.Http.Json;
using System.Text.Json;
using AiAvailableService.Configuration;
using Microsoft.Extensions.Options;

namespace AiAvailableService.Infrastructure.Storage;

/// <summary>
/// Minimal server-side Supabase Storage REST adapter. It creates signed
/// browser-direct upload/read authority and reads metadata only; it never
/// accepts or proxies object bytes through ASP.NET Core.
/// </summary>
public sealed class SupabaseObjectStorageService(
    HttpClient httpClient,
    IOptions<SupabaseOptions> optionsAccessor) : IObjectStorageService
{
    private static readonly TimeSpan ProviderSignedUploadLifetime = TimeSpan.FromHours(2);

    public async Task<StorageUploadAuthorization> CreateUploadAuthorizationAsync(
        StorageUploadRequest request,
        CancellationToken cancellationToken = default)
    {
        var apiBase = GetStorageApiBaseUri();
        using var message = CreateAuthorizedRequest(
            HttpMethod.Post,
            new Uri(apiBase, $"object/upload/sign/{EscapeSegment(request.Bucket)}/{EscapeObjectKey(request.ObjectKey)}"));
        message.Content = JsonContent.Create(new { });

        using var response = await SendAsync(message, cancellationToken);
        if (!response.IsSuccessStatusCode)
        {
            throw new StorageUnavailableException();
        }

        var providerPath = await ReadRequiredStringAsync(response, "url", cancellationToken);
        var uploadUri = ResolveProviderPath(apiBase, providerPath);
        return new StorageUploadAuthorization(
            uploadUri,
            DateTimeOffset.UtcNow.Add(ProviderSignedUploadLifetime),
            new Dictionary<string, string>(StringComparer.OrdinalIgnoreCase)
            {
                ["content-type"] = request.ContentType
            });
    }

    public async Task<StorageObjectMetadata?> GetObjectMetadataAsync(
        string bucket,
        string objectKey,
        CancellationToken cancellationToken = default)
    {
        var apiBase = GetStorageApiBaseUri();
        using var message = CreateAuthorizedRequest(
            HttpMethod.Get,
            new Uri(apiBase, $"object/info/{EscapeSegment(bucket)}/{EscapeObjectKey(objectKey)}"));
        using var response = await SendAsync(message, cancellationToken);
        if (response.StatusCode == HttpStatusCode.NotFound)
        {
            return null;
        }

        if (!response.IsSuccessStatusCode)
        {
            throw new StorageUnavailableException();
        }

        try
        {
            await using var responseStream = await response.Content.ReadAsStreamAsync(cancellationToken);
            using var document = await JsonDocument.ParseAsync(responseStream, cancellationToken: cancellationToken);
            var root = document.RootElement;
            var returnedName = GetOptionalString(root, "name");
            if (returnedName is not null && !string.Equals(returnedName, objectKey, StringComparison.Ordinal))
            {
                throw new StorageUnavailableException();
            }

            var metadata = root.TryGetProperty("metadata", out var nestedMetadata)
                && nestedMetadata.ValueKind == JsonValueKind.Object
                ? nestedMetadata
                : default;
            var contentType = GetOptionalString(root, "contentType")
                ?? GetOptionalString(root, "content_type")
                ?? GetOptionalString(metadata, "mimetype")
                ?? GetOptionalString(metadata, "contentType")
                ?? GetOptionalString(metadata, "content_type");
            var byteSize = GetOptionalInt64(root, "size")
                ?? GetOptionalInt64(metadata, "size");
            var eTag = GetOptionalString(root, "etag")
                ?? GetOptionalString(root, "eTag")
                ?? GetOptionalString(metadata, "etag")
                ?? GetOptionalString(metadata, "eTag")
                ?? GetOptionalString(root, "version");

            if (string.IsNullOrWhiteSpace(contentType) || byteSize is null || byteSize < 0)
            {
                throw new StorageUnavailableException();
            }

            return new StorageObjectMetadata(bucket, objectKey, contentType, byteSize.Value, eTag);
        }
        catch (StorageUnavailableException)
        {
            throw;
        }
        catch (JsonException)
        {
            throw new StorageUnavailableException();
        }
    }

    public async Task<StorageReadAccess> CreateTemporaryReadAccessAsync(
        string bucket,
        string objectKey,
        DateTimeOffset expiresAt,
        CancellationToken cancellationToken = default)
    {
        var now = DateTimeOffset.UtcNow;
        var seconds = (int)Math.Floor((expiresAt - now).TotalSeconds);
        if (seconds <= 0)
        {
            throw new StorageUnavailableException();
        }

        var apiBase = GetStorageApiBaseUri();
        using var message = CreateAuthorizedRequest(
            HttpMethod.Post,
            new Uri(apiBase, $"object/sign/{EscapeSegment(bucket)}/{EscapeObjectKey(objectKey)}"));
        message.Content = JsonContent.Create(new { expiresIn = seconds });

        using var response = await SendAsync(message, cancellationToken);
        if (!response.IsSuccessStatusCode)
        {
            throw new StorageUnavailableException();
        }

        var providerPath = await ReadRequiredStringAsync(response, "signedURL", cancellationToken);
        return new StorageReadAccess(ResolveProviderPath(apiBase, providerPath), now.AddSeconds(seconds));
    }

    public async Task DeleteObjectAsync(
        string bucket,
        string objectKey,
        CancellationToken cancellationToken = default)
    {
        var apiBase = GetStorageApiBaseUri();
        using var message = CreateAuthorizedRequest(
            HttpMethod.Delete,
            new Uri(apiBase, $"object/{EscapeSegment(bucket)}"));
        message.Content = JsonContent.Create(new { prefixes = new[] { objectKey } });

        using var response = await SendAsync(message, cancellationToken);
        // Cleanup is idempotent: the object can already have expired under a
        // provider lifecycle policy or from a previous best-effort deletion.
        if (response.StatusCode == HttpStatusCode.NotFound || response.IsSuccessStatusCode)
        {
            return;
        }

        throw new StorageUnavailableException();
    }

    private HttpRequestMessage CreateAuthorizedRequest(HttpMethod method, Uri uri)
    {
        var serviceRoleKey = GetServiceRoleKey();
        var message = new HttpRequestMessage(method, uri);
        message.Headers.Authorization = new AuthenticationHeaderValue("Bearer", serviceRoleKey);
        message.Headers.Add("apikey", serviceRoleKey);
        return message;
    }

    private async Task<HttpResponseMessage> SendAsync(HttpRequestMessage request, CancellationToken cancellationToken)
    {
        try
        {
            return await httpClient.SendAsync(request, HttpCompletionOption.ResponseHeadersRead, cancellationToken);
        }
        catch (HttpRequestException)
        {
            throw new StorageUnavailableException();
        }
        catch (TaskCanceledException) when (!cancellationToken.IsCancellationRequested)
        {
            throw new StorageUnavailableException();
        }
    }

    private Uri GetStorageApiBaseUri()
    {
        var configuredUrl = optionsAccessor.Value.Url?.Trim();
        if (!Uri.TryCreate(configuredUrl, UriKind.Absolute, out var configuredUri) ||
            configuredUri.Scheme is not ("https" or "http"))
        {
            throw new StorageUnavailableException();
        }

        var normalized = configuredUrl!.TrimEnd('/');
        return normalized.EndsWith("/storage/v1", StringComparison.OrdinalIgnoreCase)
            ? new Uri(normalized + "/", UriKind.Absolute)
            : new Uri(normalized + "/storage/v1/", UriKind.Absolute);
    }

    private string GetServiceRoleKey()
    {
        var key = optionsAccessor.Value.ServiceRoleKey?.Trim();
        if (string.IsNullOrWhiteSpace(key))
        {
            throw new StorageUnavailableException();
        }

        return key;
    }

    private static async Task<string> ReadRequiredStringAsync(
        HttpResponseMessage response,
        string propertyName,
        CancellationToken cancellationToken)
    {
        try
        {
            await using var responseStream = await response.Content.ReadAsStreamAsync(cancellationToken);
            using var document = await JsonDocument.ParseAsync(responseStream, cancellationToken: cancellationToken);
            var value = GetOptionalString(document.RootElement, propertyName);
            if (string.IsNullOrWhiteSpace(value))
            {
                throw new StorageUnavailableException();
            }

            return value;
        }
        catch (StorageUnavailableException)
        {
            throw;
        }
        catch (JsonException)
        {
            throw new StorageUnavailableException();
        }
    }

    private static Uri ResolveProviderPath(Uri apiBase, string providerPath)
    {
        if (Uri.TryCreate(providerPath, UriKind.Absolute, out var absolute))
        {
            return absolute;
        }

        try
        {
            return new Uri(apiBase, providerPath.TrimStart('/'));
        }
        catch (UriFormatException)
        {
            throw new StorageUnavailableException();
        }
    }

    private static string? GetOptionalString(JsonElement element, string propertyName)
    {
        if (element.ValueKind != JsonValueKind.Object ||
            !element.TryGetProperty(propertyName, out var property) ||
            property.ValueKind != JsonValueKind.String)
        {
            return null;
        }

        return property.GetString();
    }

    private static long? GetOptionalInt64(JsonElement element, string propertyName)
    {
        if (element.ValueKind != JsonValueKind.Object ||
            !element.TryGetProperty(propertyName, out var property))
        {
            return null;
        }

        if (property.ValueKind == JsonValueKind.Number && property.TryGetInt64(out var numeric))
        {
            return numeric;
        }

        return property.ValueKind == JsonValueKind.String &&
            long.TryParse(property.GetString(), System.Globalization.NumberStyles.None,
                System.Globalization.CultureInfo.InvariantCulture, out var parsed)
            ? parsed
            : null;
    }

    private static string EscapeSegment(string value) => Uri.EscapeDataString(value);

    private static string EscapeObjectKey(string key) => string.Join('/', key.Split('/', StringSplitOptions.None)
        .Select(Uri.EscapeDataString));
}
