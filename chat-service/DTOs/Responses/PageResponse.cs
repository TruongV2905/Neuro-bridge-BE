namespace ChatService.DTOs.Responses;

public class PageResponse<T>
{
    public int CurrentPage { get; set; }
    public int TotalPages { get; set; }
    public int PageSize { get; set; }
    public long TotalElements { get; set; }
    public List<T> Content { get; set; } = new();
}
