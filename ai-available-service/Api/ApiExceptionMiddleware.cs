using System.Text.Json;
using Microsoft.AspNetCore.Http;
using Microsoft.Extensions.Logging;

namespace AiAvailableService.Api;

public sealed class ApiExceptionMiddleware(RequestDelegate next, ILogger<ApiExceptionMiddleware> logger)
{
    public async Task Invoke(HttpContext context)
    {
        try
        {
            await next(context);
        }
        catch (ApiException exception)
        {
            await WriteErrorAsync(context, exception.StatusCode, exception.ErrorCode, exception.Message,
                exception.FieldErrors, exception.Details);
        }
        catch (Exception exception)
        {
            logger.LogError(exception, "Unhandled request failure for {RequestPath}.", context.Request.Path);
            await WriteErrorAsync(context, StatusCodes.Status500InternalServerError, "INTERNAL_ERROR",
                "An unexpected server error occurred.", null, null);
        }
    }

    public static Task WriteErrorAsync(
        HttpContext context,
        int statusCode,
        string errorCode,
        string message,
        IReadOnlyDictionary<string, string[]>? fieldErrors,
        object? details)
    {
        if (context.Response.HasStarted)
        {
            return Task.CompletedTask;
        }

        context.Response.Clear();
        context.Response.StatusCode = statusCode;
        context.Response.ContentType = "application/json";
        var requestId = context.TraceIdentifier;
        var envelope = new ApiEnvelope<ApiError>(statusCode, message,
            new ApiError(errorCode, fieldErrors, details, requestId));
        return context.Response.WriteAsync(JsonSerializer.Serialize(envelope, ApiJson.Options));
    }
}
