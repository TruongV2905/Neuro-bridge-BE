namespace AiAvailableService.Api;

public sealed record ApiError(
    string ErrorCode,
    IReadOnlyDictionary<string, string[]>? FieldErrors = null,
    object? Details = null,
    string? RequestId = null);
