namespace AiAvailableService.Api;

public sealed record PageResponse<T>(IReadOnlyList<T> Data, PageMetadata Metadata);

public sealed record PageMetadata(
    int Page,
    int Size,
    int CurrentElements,
    long TotalElements,
    int TotalPages,
    bool HasNext,
    bool HasPrevious);
