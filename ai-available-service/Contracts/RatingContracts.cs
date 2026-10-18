namespace AiAvailableService.Contracts;

public sealed class CreateProductRatingRequest
{
    public short Score { get; init; }
    public string? ReviewText { get; init; }
}

public sealed class UpdateProductRatingRequest
{
    public short Score { get; init; }
    public string? ReviewText { get; init; }
}

public sealed record ProductRatingResponse(
    Guid RatingId,
    short Score,
    string? ReviewText,
    DateTimeOffset CreatedAt,
    DateTimeOffset UpdatedAt);

public sealed record ProductRatingSummaryResponse(
    Guid ProductId,
    long RatingCount,
    decimal? AverageScore);
