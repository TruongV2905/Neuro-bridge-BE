namespace AiAvailableService.Infrastructure.Storage;

/// <summary>
/// A trusted duration inspector must inspect the exact server-authorized
/// storage object. Browser-observed media duration is deliberately excluded.
/// </summary>
public interface IMediaDurationInspector
{
    Task<decimal> GetDurationSecondsAsync(
        string bucket,
        string objectKey,
        string contentType,
        CancellationToken cancellationToken = default);
}

/// <summary>
/// Phase 7C intentionally has no verified ffprobe/media-worker deployment.
/// Keeping this explicit prevents a browser-supplied duration from silently
/// becoming trusted until an approved implementation is configured.
/// </summary>
public sealed class UnavailableMediaDurationInspector : IMediaDurationInspector
{
    public Task<decimal> GetDurationSecondsAsync(
        string bucket,
        string objectKey,
        string contentType,
        CancellationToken cancellationToken = default) =>
        Task.FromException<decimal>(new MediaDurationInspectionUnavailableException());
}
