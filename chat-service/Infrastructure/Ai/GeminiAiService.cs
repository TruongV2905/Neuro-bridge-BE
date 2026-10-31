using System.Net.Http.Headers;
using System.Text;
using System.Text.Json;
using System.Text.Json.Serialization;

namespace ChatService.Infrastructure.Ai;

public class GeminiAiService : IGeminiAiService
{
    private readonly HttpClient _httpClient;
    private readonly IConfiguration _configuration;
    private readonly ILogger<GeminiAiService> _logger;

    private const string SystemPrompt = 
        "Bạn là NeuroBot, trợ lý thông minh độc quyền của nền tảng Neuro Bridge (sàn kết nối tuyển dụng chuyên gia & sản phẩm AI). " +
        "Nhiệm vụ của bạn là hỗ trợ người dùng (Client và Expert): " +
        "1. Tư vấn viết tin tuyển dụng (Job Post), yêu cầu kỹ thuật và ước lượng ngân sách dự án. " +
        "2. Tư vấn giải pháp kỹ thuật, phân tích công nghệ AI/Machine Learning cho các Freelancer/Chuyên gia. " +
        "3. Giới thiệu các tính năng của sàn Neuro Bridge: tìm kiếm chuyên gia bằng AI Matching, ký quỹ an toàn qua Escrow, mua bán sản phẩm AI đóng gói (AI Place). " +
        "Hãy trả lời bằng tiếng Việt thân thiện, chuyên nghiệp, súc tích và luôn khuyến khích giải pháp tối ưu.";

    public GeminiAiService(HttpClient httpClient, IConfiguration configuration, ILogger<GeminiAiService> logger)
    {
        _httpClient = httpClient;
        _configuration = configuration;
        _logger = logger;
    }

    public async Task<string> GenerateReplyAsync(
        string userMessage, 
        IEnumerable<(string role, string content)>? conversationHistory = null, 
        CancellationToken cancellationToken = default)
    {
        try
        {
            var apiKey = _configuration["Gemini:ApiKey"] 
                         ?? Environment.GetEnvironmentVariable("GEMINI_API_KEY") 
                         ?? "AQ.Ab8RN6JNPGDR1T38xQtsbIg8GkYu-cr5aPnQTNtOTYf4QgktzQ";

            var model = _configuration["Gemini:Model"] ?? "gemini-2.5-flash";
            var baseUrl = _configuration["Gemini:BaseUrl"] ?? "https://generativelanguage.googleapis.com/v1beta/openai/chat/completions";

            var messages = new List<object>
            {
                new { role = "system", content = SystemPrompt }
            };

            // Add previous messages (up to 6)
            if (conversationHistory != null)
            {
                foreach (var (role, content) in conversationHistory.TakeLast(6))
                {
                    messages.Add(new { role, content });
                }
            }

            // Add latest user message
            messages.Add(new { role = "user", content = userMessage });

            var requestBody = new
            {
                model,
                messages,
                temperature = 0.7,
                max_tokens = 1000
            };

            var jsonContent = new StringContent(
                JsonSerializer.Serialize(requestBody), 
                Encoding.UTF8, 
                "application/json");

            using var request = new HttpRequestMessage(HttpMethod.Post, baseUrl)
            {
                Content = jsonContent
            };
            request.Headers.Authorization = new AuthenticationHeaderValue("Bearer", apiKey);

            var response = await _httpClient.SendAsync(request, cancellationToken);
            var responseString = await response.Content.ReadAsStringAsync(cancellationToken);

            if (!response.IsSuccessStatusCode)
            {
                _logger.LogError("[GeminiAiService] Request failed: {StatusCode} - {Body}", response.StatusCode, responseString);
                return "Xin lỗi, hiện tại NeuroBot đang quá tải hoặc gặp sự cố kết nối. Vui lòng thử lại sau ít phút!";
            }

            using var doc = JsonDocument.Parse(responseString);
            var root = doc.RootElement;
            if (root.TryGetProperty("choices", out var choices) && choices.GetArrayLength() > 0)
            {
                var firstChoice = choices[0];
                if (firstChoice.TryGetProperty("message", out var msg) && msg.TryGetProperty("content", out var text))
                {
                    return text.GetString() ?? "Tôi có thể giúp gì thêm cho dự án của bạn?";
                }
            }

            return "Xin lỗi, tôi không thể xử lý câu trả lời lúc này.";
        }
        catch (Exception ex)
        {
            _logger.LogError(ex, "[GeminiAiService] Error generating reply from Gemini");
            return "Đã xảy ra lỗi khi kết nối với máy chủ AI. Vui lòng thử lại sau!";
        }
    }
}
