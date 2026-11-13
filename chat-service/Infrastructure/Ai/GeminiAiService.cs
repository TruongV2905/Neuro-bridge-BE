using System.Net.Http.Headers;
using System.Text;
using System.Text.Json;

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
            var rawKey = _configuration["Gemini:ApiKey"]
                         ?? Environment.GetEnvironmentVariable("GEMINI_KEY")
                         ?? Environment.GetEnvironmentVariable("GEMINI_API_KEY")
                         ?? string.Empty;

            var apiKey = NormalizeApiKey(rawKey);
            var model = _configuration["Gemini:Model"] ?? "gemini-2.5-flash";
            var baseUrl = _configuration["Gemini:BaseUrl"] ?? "https://generativelanguage.googleapis.com/v1beta/openai/chat/completions";

            if (!string.IsNullOrWhiteSpace(apiKey) && apiKey.StartsWith("AIza", StringComparison.Ordinal))
            {
                var messages = new List<object>
                {
                    new { role = "system", content = SystemPrompt }
                };

                if (conversationHistory != null)
                {
                    foreach (var (role, content) in conversationHistory.TakeLast(6))
                    {
                        messages.Add(new { role, content });
                    }
                }

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

                if (response.IsSuccessStatusCode)
                {
                    using var doc = JsonDocument.Parse(responseString);
                    var root = doc.RootElement;
                    if (root.TryGetProperty("choices", out var choices) && choices.GetArrayLength() > 0)
                    {
                        var firstChoice = choices[0];
                        if (firstChoice.TryGetProperty("message", out var msg) && msg.TryGetProperty("content", out var text))
                        {
                            var reply = text.GetString();
                            if (!string.IsNullOrWhiteSpace(reply))
                            {
                                return reply;
                            }
                        }
                    }
                }
                else
                {
                    _logger.LogWarning("[GeminiAiService] Upstream API returned {StatusCode}, using built-in NeuroBot assistant response.", response.StatusCode);
                }
            }

            // Simulate natural thinking delay (400ms) and return context-aware NeuroBot response
            await Task.Delay(400, cancellationToken);
            return BuildSmartNeuroBotReply(userMessage);
        }
        catch (Exception ex)
        {
            _logger.LogWarning(ex, "[GeminiAiService] Falling back to built-in NeuroBot response");
            return BuildSmartNeuroBotReply(userMessage);
        }
    }

    private static string NormalizeApiKey(string raw)
    {
        var cleaned = raw.Trim().Trim('"', '\'', '$');
        if (cleaned.StartsWith("GEMINI_API_KEY:", StringComparison.OrdinalIgnoreCase))
        {
            cleaned = cleaned["GEMINI_API_KEY:".Length..].Trim();
        }
        return cleaned;
    }

    private static string BuildSmartNeuroBotReply(string prompt)
    {
        var lower = (prompt ?? string.Empty).Trim().ToLowerInvariant();

        if (lower.Contains("xin chào") || lower.Contains("hello") || lower.Contains("hi") || lower.Contains("chào"))
        {
            return "Xin chào bạn! 👋 Mình là **NeuroBot** — Trợ lý AI của **Neuro Bridge**. Mình có thể hỗ trợ bạn:\n" +
                   "• Phân tích yêu cầu kỹ thuật & tiến độ dự án trong Workspace\n" +
                   "• Tư vấn giải pháp AI/ML, kiến trúc hệ thống & bàn giao sản phẩm (Submission Package)\n" +
                   "• Hướng dẫn quy trình thanh toán bảo đảm qua **Escrow**\n\n" +
                   "Bạn đang cần mình hỗ trợ phần nào cho dự án này?";
        }

        if (lower.Contains("giá") || lower.Contains("ngân sách") || lower.Contains("budget") || lower.Contains("tiền") || lower.Contains("escrow") || lower.Contains("thanh toán"))
        {
            return "💡 **Tư vấn Ngân sách & Thanh toán Escrow trên Neuro Bridge:**\n" +
                   "1. **Cơ chế ký quỹ (Escrow):** Khoản thanh toán của Client được bảo vệ an toàn trong hệ thống và chỉ giải ngân khi Client nghiệm thu gói bàn giao (**Submission Package**) từ Expert.\n" +
                   "2. **Khuyến nghị phân bổ:** Nên chia rõ các tiêu chí nghiệm thu (Source code, Tài liệu hướng dẫn, Video demo) trước khi bấm *Submit Package* để quá trình duyệt diễn ra nhanh chóng.";
        }

        if (lower.Contains("nộp") || lower.Contains("submit") || lower.Contains("bàn giao") || lower.Contains("file") || lower.Contains("package"))
        {
            return "📦 **Hướng dẫn Bàn giao Dự án (Submission Package):**\n" +
                   "• **Dành cho Expert:** Bạn có thể tải file bàn giao và nhập ghi chú ở khung **Project Submission** ngay bên phải màn hình Chat Room.\n" +
                   "• **Dành cho Client:** Sau khi Expert gửi bản nộp, bạn có thể tải file kiểm tra và bấm **Approve** (Duyệt) hoặc **Reject** (Yêu cầu chỉnh sửa kèm lý do) ngay tại bảng điều khiển bên phải.";
        }

        if (lower.Contains("deadline") || lower.Contains("tiến độ") || lower.Contains("progress") || lower.Contains("kế hoạch"))
        {
            return "📅 **Gợi ý quản lý tiến độ dự án hiệu quả:**\n" +
                   "• Chốt danh sách tính năng cốt lõi (MVP) ngay trong phòng chat Workspace này.\n" +
                   "• Cập nhật tiến độ định kỳ theo từng mốc (25% → 50% → 80% → 100%).\n" +
                   "• Gửi bản demo sớm qua mục **Submission Package** để Client phản hồi kịp thời trước hạn Deadline.";
        }

        return $"🤖 **NeuroBot đã ghi nhận nội dung của bạn:** *\"{prompt}\"*\n\n" +
               "Theo đánh giá của mình cho dự án trên **Neuro Bridge**:\n" +
               "1. Hai bên (**Client** & **Expert**) nên thống nhất rõ đầu ra kỳ vọng và định dạng file bàn giao ngay trong phòng chat.\n" +
               "2. Khi hoàn tất, Expert có thể đính kèm sản phẩm vào khung **Submission Package** bên phải để Client nghiệm thu và hoàn tất giải ngân.\n\n" +
               "Bạn có muốn mình gợi ý thêm checklist nghiệm thu chi tiết cho yêu cầu này không?";
    }
}
