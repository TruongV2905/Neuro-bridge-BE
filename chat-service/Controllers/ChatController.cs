using System.Security.Claims;
using ChatService.Domain.Enums;
using ChatService.DTOs.Requests;
using ChatService.DTOs.Responses;
using ChatService.Infrastructure.Ai;
using ChatService.Services;
using Microsoft.AspNetCore.Mvc;

namespace ChatService.Controllers;

[ApiController]
[Route("chat")]
[Route("api/chat")]
[Route("")]
public class ChatController : ControllerBase
{
    private readonly IChatService _chatService;
    private readonly IGeminiAiService _geminiAiService;
    private readonly ILogger<ChatController> _logger;

    public ChatController(
        IChatService chatService, 
        IGeminiAiService geminiAiService,
        ILogger<ChatController> logger)
    {
        _chatService = chatService;
        _geminiAiService = geminiAiService;
        _logger = logger;
    }

    [HttpPost("sessions")]
    public async Task<ActionResult<ApiEnvelope<ChatSessionResponse>>> CreateSession([FromBody] CreateChatSessionRequest request)
    {
        var (userCode, userRole, token) = ExtractUserContext();
        var result = await _chatService.CreateChatSessionAsync(request, userCode, userRole, token);
        return StatusCode(StatusCodes.Status201Created, ApiEnvelope<ChatSessionResponse>.Success(201, result, "Create chat session successfully"));
    }

    [HttpGet("sessions/{id:guid}")]
    public async Task<ActionResult<ApiEnvelope<ChatSessionResponse>>> GetSessionById(Guid id)
    {
        var (_, _, token) = ExtractUserContext();
        var result = await _chatService.GetChatSessionAsync(id, token);
        if (result == null)
        {
            return NotFound(ApiEnvelope<ChatSessionResponse>.Error(404, $"Chat session {id} not found"));
        }
        return Ok(ApiEnvelope<ChatSessionResponse>.Success(200, result, "Get chat session successfully"));
    }

    [HttpGet("sessions/mine/direct")]
    public async Task<ActionResult<ApiEnvelope<List<ChatSessionResponse>>>> GetMyDirectSessions()
    {
        var (userCode, _, token) = ExtractUserContext();
        var result = await _chatService.GetMyChatSessionsAsync(ChatType.DIRECT, userCode, token);
        return Ok(ApiEnvelope<List<ChatSessionResponse>>.Success(200, result, "Get my chat sessions successfully"));
    }

    [HttpGet("sessions/mine/project")]
    public async Task<ActionResult<ApiEnvelope<List<ChatSessionResponse>>>> GetMyProjectSessions()
    {
        var (userCode, _, token) = ExtractUserContext();
        var result = await _chatService.GetMyChatSessionsAsync(ChatType.PROJECT, userCode, token);
        return Ok(ApiEnvelope<List<ChatSessionResponse>>.Success(200, result, "Get my chat sessions successfully"));
    }

    [HttpGet("sessions/mine/ai")]
    public async Task<ActionResult<ApiEnvelope<List<ChatSessionResponse>>>> GetMyAiSessions()
    {
        var (userCode, _, token) = ExtractUserContext();
        var result = await _chatService.GetMyChatSessionsAsync(ChatType.AI, userCode, token);
        return Ok(ApiEnvelope<List<ChatSessionResponse>>.Success(200, result, "Get my AI chat sessions successfully"));
    }

    [HttpPost("sessions/{id:guid}/participants/{userCode}")]
    public async Task<ActionResult<ApiEnvelope<ChatSessionResponse>>> AddParticipant(Guid id, string userCode)
    {
        var (_, _, token) = ExtractUserContext();
        var result = await _chatService.AddParticipantAsync(id, userCode, token);
        return Ok(ApiEnvelope<ChatSessionResponse>.Success(200, result, "Add participant successfully"));
    }

    [HttpDelete("sessions/{id:guid}/participants/{userCode}")]
    public async Task<ActionResult<ApiEnvelope<ChatSessionResponse>>> RemoveParticipant(Guid id, string userCode)
    {
        var (_, _, token) = ExtractUserContext();
        var result = await _chatService.RemoveParticipantAsync(id, userCode, token);
        return Ok(ApiEnvelope<ChatSessionResponse>.Success(200, result, "Remove participant successfully"));
    }

    [HttpPost("sessions/{id:guid}/close")]
    public async Task<ActionResult<ApiEnvelope<ChatSessionResponse>>> CloseSession(Guid id)
    {
        var (_, _, token) = ExtractUserContext();
        var result = await _chatService.CloseChatSessionAsync(id, token);
        return Ok(ApiEnvelope<ChatSessionResponse>.Success(200, result, "Close chat session successfully"));
    }

    [HttpPost("sessions/{id:guid}/block")]
    public async Task<ActionResult<ApiEnvelope<ChatSessionResponse>>> BlockSession(Guid id)
    {
        var (_, _, token) = ExtractUserContext();
        var result = await _chatService.BlockChatSessionAsync(id, token);
        return Ok(ApiEnvelope<ChatSessionResponse>.Success(200, result, "Block chat session successfully"));
    }

    [HttpGet("sessions/{id:guid}/messages")]
    public async Task<ActionResult<ApiEnvelope<PageResponse<MessageResponse>>>> GetMessages(
        Guid id, 
        [FromQuery] int page = 0, 
        [FromQuery] int size = 30)
    {
        var result = await _chatService.GetMessagesAsync(id, page, size);
        return Ok(ApiEnvelope<PageResponse<MessageResponse>>.Success(200, result, "Get messages successfully"));
    }

    [HttpPost("messages")]
    public async Task<ActionResult<ApiEnvelope<MessageResponse>>> SendMessage([FromBody] SendMessageRequest request)
    {
        var (userCode, userRole, token) = ExtractUserContext();
        var result = await _chatService.SendMessageAsync(request, userCode, userRole, token);
        return StatusCode(StatusCodes.Status201Created, ApiEnvelope<MessageResponse>.Success(201, result, "Send message successfully"));
    }

    /// <summary>
    /// Endpoint chat nhanh trực tiếp với AI mà không cần lưu phiên.
    /// </summary>
    [HttpPost("ai/ask")]
    public async Task<ActionResult<ApiEnvelope<string>>> AskAiQuick([FromBody] QuickAskRequest request)
    {
        if (string.IsNullOrWhiteSpace(request.Prompt))
        {
            return BadRequest(ApiEnvelope<string>.Error(400, "Prompt cannot be empty"));
        }

        var reply = await _geminiAiService.GenerateReplyAsync(request.Prompt);
        return Ok(ApiEnvelope<string>.Success(200, reply, "AI response generated successfully"));
    }

    private (string UserCode, string? UserRole, string? Token) ExtractUserContext()
    {
        // 1. Check Gateway custom headers
        var userCode = Request.Headers["X-User-Code"].FirstOrDefault()
                       ?? Request.Headers["X-User-Id"].FirstOrDefault()
                       ?? User.FindFirstValue("code")
                       ?? User.FindFirstValue(ClaimTypes.NameIdentifier)
                       ?? User.FindFirstValue("sub")
                       ?? "ANONYMOUS_USER";

        var userRole = Request.Headers["X-User-Role"].FirstOrDefault()
                       ?? User.FindFirstValue(ClaimTypes.Role)
                       ?? User.FindFirstValue("role")
                       ?? "CLIENT";

        var token = Request.Headers["Authorization"].FirstOrDefault();

        return (userCode, userRole, token);
    }
}

public class QuickAskRequest
{
    public string Prompt { get; set; } = string.Empty;
}
