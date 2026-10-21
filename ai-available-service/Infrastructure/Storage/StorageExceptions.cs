namespace AiAvailableService.Infrastructure.Storage;

/// <summary>
/// Safe infrastructure failures. The HTTP adapter deliberately does not retain
/// or expose a provider response body, bucket credential, or signed URL.
/// </summary>
public sealed class StorageUnavailableException : Exception
{
    public StorageUnavailableException() : base("Object storage is temporarily unavailable.") { }
}

public sealed class StorageUploadTokenExpiredException : Exception
{
    public StorageUploadTokenExpiredException() : base("The upload authorization has expired.") { }
}

public sealed class InvalidStorageUploadTokenException : Exception
{
    public InvalidStorageUploadTokenException() : base("The upload authorization is invalid.") { }
}

public sealed class MediaDurationInspectionUnavailableException : Exception
{
    public MediaDurationInspectionUnavailableException()
        : base("Trusted media duration inspection is not configured.") { }
}
