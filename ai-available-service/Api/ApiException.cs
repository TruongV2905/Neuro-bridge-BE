using Microsoft.AspNetCore.Http;

namespace AiAvailableService.Api;

public sealed class ApiException : Exception
{
    public ApiException(
        int statusCode,
        string errorCode,
        string message,
        IReadOnlyDictionary<string, string[]>? fieldErrors = null,
        object? details = null)
        : base(message)
    {
        StatusCode = statusCode;
        ErrorCode = errorCode;
        FieldErrors = fieldErrors;
        Details = details;
    }

    public int StatusCode { get; }
    public string ErrorCode { get; }
    public IReadOnlyDictionary<string, string[]>? FieldErrors { get; }
    public object? Details { get; }

    public static ApiException Validation(IReadOnlyDictionary<string, string[]> errors) =>
        new(StatusCodes.Status400BadRequest, "VALIDATION_ERROR", "One or more fields are invalid.", errors);

    public static ApiException Forbidden(string message = "You do not have permission to perform this action.") =>
        new(StatusCodes.Status403Forbidden, "FORBIDDEN", message);
}
