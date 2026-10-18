namespace AiAvailableService.Api;

public sealed record ApiEnvelope<T>(int Code, string Message, T Result)
{
    public static ApiEnvelope<T> Success(int code, T result, string message = "Success.") =>
        new(code, message, result);
}
