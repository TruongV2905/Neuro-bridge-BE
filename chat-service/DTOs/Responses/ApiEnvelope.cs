namespace ChatService.DTOs.Responses;

public sealed record ApiEnvelope<T>(int Code, string Message, T? Result)
{
    public static ApiEnvelope<T> Success(int code, T result, string message = "Success.") =>
        new(code, message, result);

    public static ApiEnvelope<T> Error(int code, string message) =>
        new(code, message, default);
}
