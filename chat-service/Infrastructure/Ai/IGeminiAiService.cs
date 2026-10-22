namespace ChatService.Infrastructure.Ai;

public interface IGeminiAiService
{
    Task<string> GenerateReplyAsync(string userMessage, IEnumerable<(string role, string content)>? conversationHistory = null, CancellationToken cancellationToken = default);
}
