namespace AiAvailableService.Infrastructure.Storage;

/// <summary>
/// Product-agnostic boundary for the future Supabase adapter. Implementations must issue
/// short-lived access only and must never proxy browser upload bytes through this service.
/// </summary>
public interface IObjectStorageService
{
    Task<StorageUploadAuthorization> CreateUploadAuthorizationAsync(
        StorageUploadRequest request,
        CancellationToken cancellationToken = default);

    Task<StorageObjectMetadata?> GetObjectMetadataAsync(
        string bucket,
        string objectKey,
        CancellationToken cancellationToken = default);

    Task<StorageReadAccess> CreateTemporaryReadAccessAsync(
        string bucket,
        string objectKey,
        DateTimeOffset expiresAt,
        CancellationToken cancellationToken = default);

    Task DeleteObjectAsync(
        string bucket,
        string objectKey,
        CancellationToken cancellationToken = default);
}

public sealed record StorageUploadRequest(
    string Bucket,
    string ObjectKey,
    string ContentType,
    long ByteSize,
    DateTimeOffset ExpiresAt);

public sealed record StorageUploadAuthorization(
    Uri UploadUri,
    DateTimeOffset ExpiresAt,
    IReadOnlyDictionary<string, string> RequiredHeaders);

public sealed record StorageObjectMetadata(
    string Bucket,
    string ObjectKey,
    string ContentType,
    long ByteSize,
    string? ETag);

public sealed record StorageReadAccess(Uri AccessUri, DateTimeOffset ExpiresAt);
