namespace AiAvailableService.Api;

public sealed record PageRequest(int Page, int Size)
{
    public int Skip => checked(Page * Size);
}

public static class Pagination
{
    public const int DefaultPage = 0;
    public const int DefaultSize = 10;
    public const int MaximumSize = 50;

    public static PageRequest Validate(int page = DefaultPage, int size = DefaultSize)
    {
        var errors = new Dictionary<string, string[]>();
        if (page < 0)
        {
            errors["page"] = ["page must be zero or greater."];
        }

        if (size is < 1 or > MaximumSize)
        {
            errors["size"] = [$"size must be between 1 and {MaximumSize}."];
        }

        if (page >= 0 && size >= 1 && size <= MaximumSize && page > int.MaxValue / size)
        {
            errors["page"] = ["page is too large."];
        }

        if (errors.Count > 0)
        {
            throw ApiException.Validation(errors);
        }

        return new PageRequest(page, size);
    }

    public static PageResponse<T> Create<T>(IReadOnlyList<T> data, PageRequest page, long totalElements)
    {
        var totalPages = totalElements == 0
            ? 0
            : checked((int)Math.Ceiling(totalElements / (double)page.Size));
        return new PageResponse<T>(data, new PageMetadata(
            page.Page,
            page.Size,
            data.Count,
            totalElements,
            totalPages,
            page.Page + 1 < totalPages,
            page.Page > 0));
    }
}
