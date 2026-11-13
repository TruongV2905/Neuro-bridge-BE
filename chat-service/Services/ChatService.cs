using ChatService.Data;
using ChatService.Domain.Entities;
using ChatService.Domain.Enums;
using ChatService.DTOs.Requests;
using ChatService.DTOs.Responses;
using ChatService.Infrastructure.Ai;
using ChatService.Infrastructure.Clients;
using ChatService.WebSocket;
using Microsoft.EntityFrameworkCore;

namespace ChatService.Services;

public class ChatServiceImpl : IChatService
{
    private readonly ChatDbContext _dbContext;
    private readonly IIdentityClient _identityClient;
    private readonly IWorkspaceClient _workspaceClient;
    private readonly IGeminiAiService _geminiAiService;
    private readonly IWebSocketChatSender _wsChatSender;
    private readonly IServiceScopeFactory _scopeFactory;
    private readonly ILogger<ChatServiceImpl> _logger;

    public ChatServiceImpl(
        ChatDbContext dbContext,
        IIdentityClient identityClient,
        IWorkspaceClient workspaceClient,
        IGeminiAiService geminiAiService,
        IWebSocketChatSender wsChatSender,
        IServiceScopeFactory scopeFactory,
        ILogger<ChatServiceImpl> logger)
    {
        _dbContext = dbContext;
        _identityClient = identityClient;
        _workspaceClient = workspaceClient;
        _geminiAiService = geminiAiService;
        _wsChatSender = wsChatSender;
        _scopeFactory = scopeFactory;
        _logger = logger;
    }

    private async Task<(string ResolvedCode, string? ResolvedRole)> NormalizeUserAsync(
        string userCodeOrId,
        string? userRole,
        string? bearerToken)
    {
        if (string.IsNullOrWhiteSpace(userCodeOrId) || userCodeOrId == "ANONYMOUS_USER")
        {
            var current = await _identityClient.GetCurrentUserAsync(bearerToken);
            if (current != null && !string.IsNullOrWhiteSpace(current.Code))
            {
                return (current.Code, current.Role ?? userRole);
            }
            return (userCodeOrId, userRole);
        }

        if (Guid.TryParse(userCodeOrId, out _))
        {
            var detail = await _identityClient.GetUserByCodeAsync(userCodeOrId, bearerToken);
            if (detail != null && !string.IsNullOrWhiteSpace(detail.Code))
            {
                return (detail.Code, detail.Role ?? userRole);
            }
        }

        return (userCodeOrId, userRole);
    }

    public async Task<ChatSessionResponse> GetOrCreateWorkspaceSessionAsync(
        Guid workspaceId,
        string userCode,
        string? userRole,
        string? bearerToken)
    {
        return await CreateChatSessionAsync(
            new CreateChatSessionRequest
            {
                WorkspaceId = workspaceId,
                Type = ChatType.PROJECT
            },
            userCode,
            userRole,
            bearerToken);
    }

    public async Task<ChatSessionResponse> CreateChatSessionAsync(
        CreateChatSessionRequest request,
        string userCode,
        string? userRole,
        string? bearerToken)
    {
        var (resolvedUserCode, resolvedRole) = await NormalizeUserAsync(userCode, userRole, bearerToken);

        // 1. Idempotent check for PROJECT workspace session
        if (request.WorkspaceId.HasValue)
        {
            var existingProjectSession = await _dbContext.ChatSessions
                .Include(s => s.Participants)
                .Include(s => s.Messages)
                .OrderByDescending(s => s.CreatedAt)
                .FirstOrDefaultAsync(s => s.WorkspaceId == request.WorkspaceId.Value);

            if (existingProjectSession != null)
            {
                await EnsureWorkspaceParticipantsAsync(existingProjectSession, request.WorkspaceId.Value, resolvedUserCode, bearerToken);
                return await ToSessionResponseAsync(existingProjectSession, bearerToken);
            }
        }

        // 2. Idempotent check for AI session of current user
        if (request.Type == ChatType.AI && !string.IsNullOrWhiteSpace(resolvedUserCode))
        {
            var existingAiSession = await _dbContext.ChatSessions
                .Include(s => s.Participants)
                .Include(s => s.Messages)
                .Where(s => s.Type == ChatType.AI &&
                            s.Status == ChatSessionStatus.ACTIVE &&
                            s.Participants.Any(p => p.UserCode == resolvedUserCode))
                .OrderByDescending(s => s.CreatedAt)
                .FirstOrDefaultAsync();

            if (existingAiSession != null)
            {
                return await ToSessionResponseAsync(existingAiSession, bearerToken);
            }
        }

        var session = new ChatSession
        {
            Type = request.Type,
            Status = ChatSessionStatus.ACTIVE,
            CreatedAt = DateTime.UtcNow
        };

        if (request.Type == ChatType.AI)
        {
            session.Participants.Add(new ChatSessionParticipant { ChatSessionId = session.Id, UserCode = resolvedUserCode });
            session.Participants.Add(new ChatSessionParticipant { ChatSessionId = session.Id, UserCode = "NEURO_BOT" });

            var welcomeMsg = new ChatMessage
            {
                ChatSessionId = session.Id,
                SenderCode = "NEURO_BOT",
                SenderRole = SenderRole.AI_BOT,
                Content = "Xin chào! Tôi là NeuroBot - Trợ lý AI của nền tảng Neuro Bridge. Tôi có thể hỗ trợ bạn phân tích yêu cầu kỹ thuật, theo dõi tiến độ Workspace hoặc tư vấn bàn giao dự án hôm nay!",
                CreatedAt = DateTime.UtcNow
            };
            session.Messages.Add(welcomeMsg);
        }
        else if (request.WorkspaceId.HasValue)
        {
            session.WorkspaceId = request.WorkspaceId.Value;
            session.Type = ChatType.PROJECT;

            var addedCodes = new HashSet<string>(StringComparer.OrdinalIgnoreCase);

            var workspace = await _workspaceClient.GetWorkspaceByIdAsync(request.WorkspaceId.Value.ToString());
            if (workspace != null)
            {
                if (!string.IsNullOrWhiteSpace(workspace.ClientId))
                {
                    var clientUser = await _identityClient.GetUserByCodeAsync(workspace.ClientId, bearerToken);
                    var clientCode = !string.IsNullOrWhiteSpace(clientUser?.Code) ? clientUser.Code : workspace.ClientId;
                    if (addedCodes.Add(clientCode))
                    {
                        session.Participants.Add(new ChatSessionParticipant { ChatSessionId = session.Id, UserCode = clientCode });
                    }
                }

                if (!string.IsNullOrWhiteSpace(workspace.ExpertId))
                {
                    var expertUser = await _identityClient.GetUserByCodeAsync(workspace.ExpertId, bearerToken);
                    var expertCode = !string.IsNullOrWhiteSpace(expertUser?.Code) ? expertUser.Code : workspace.ExpertId;
                    if (addedCodes.Add(expertCode))
                    {
                        session.Participants.Add(new ChatSessionParticipant { ChatSessionId = session.Id, UserCode = expertCode });
                    }
                }
            }

            if (!string.IsNullOrWhiteSpace(resolvedUserCode) && addedCodes.Add(resolvedUserCode))
            {
                session.Participants.Add(new ChatSessionParticipant { ChatSessionId = session.Id, UserCode = resolvedUserCode });
            }
        }
        else
        {
            session.Type = ChatType.DIRECT;
            var addedCodes = new HashSet<string>(StringComparer.OrdinalIgnoreCase);

            if (!string.IsNullOrWhiteSpace(resolvedUserCode) && addedCodes.Add(resolvedUserCode))
            {
                session.Participants.Add(new ChatSessionParticipant { ChatSessionId = session.Id, UserCode = resolvedUserCode });
            }

            if (request.ParticipantCodes != null)
            {
                foreach (var pCode in request.ParticipantCodes.Where(c => !string.IsNullOrWhiteSpace(c)))
                {
                    if (addedCodes.Add(pCode))
                    {
                        session.Participants.Add(new ChatSessionParticipant { ChatSessionId = session.Id, UserCode = pCode });
                    }
                }
            }
        }

        _dbContext.ChatSessions.Add(session);
        await _dbContext.SaveChangesAsync();

        return await ToSessionResponseAsync(session, bearerToken);
    }

    private async Task EnsureWorkspaceParticipantsAsync(
        ChatSession session,
        Guid workspaceId,
        string currentUserCode,
        string? bearerToken)
    {
        bool changed = false;
        var existingCodes = session.Participants
            .Select(p => p.UserCode)
            .ToHashSet(StringComparer.OrdinalIgnoreCase);

        if (!string.IsNullOrWhiteSpace(currentUserCode) &&
            currentUserCode != "ANONYMOUS_USER" &&
            !existingCodes.Contains(currentUserCode))
        {
            session.Participants.Add(new ChatSessionParticipant
            {
                ChatSessionId = session.Id,
                UserCode = currentUserCode
            });
            existingCodes.Add(currentUserCode);
            changed = true;
        }

        var workspace = await _workspaceClient.GetWorkspaceByIdAsync(workspaceId.ToString());
        if (workspace != null)
        {
            foreach (var rawId in new[] { workspace.ClientId, workspace.ExpertId })
            {
                if (string.IsNullOrWhiteSpace(rawId)) continue;
                var detail = await _identityClient.GetUserByCodeAsync(rawId, bearerToken);
                var code = !string.IsNullOrWhiteSpace(detail?.Code) ? detail.Code : rawId;
                if (!existingCodes.Contains(code))
                {
                    session.Participants.Add(new ChatSessionParticipant
                    {
                        ChatSessionId = session.Id,
                        UserCode = code
                    });
                    existingCodes.Add(code);
                    changed = true;
                }
            }
        }

        if (changed)
        {
            await _dbContext.SaveChangesAsync();
        }
    }

    public async Task<ChatSessionResponse?> GetChatSessionAsync(Guid id, string? bearerToken)
    {
        var session = await _dbContext.ChatSessions
            .Include(s => s.Participants)
            .Include(s => s.Messages)
            .FirstOrDefaultAsync(s => s.Id == id);

        if (session == null) return null;
        return await ToSessionResponseAsync(session, bearerToken);
    }

    public async Task<List<ChatSessionResponse>> GetMyChatSessionsAsync(ChatType type, string userCode, string? bearerToken)
    {
        var (resolvedUserCode, _) = await NormalizeUserAsync(userCode, null, bearerToken);

        var sessions = await _dbContext.ChatSessions
            .Include(s => s.Participants)
            .Include(s => s.Messages)
            .Where(s => s.Type == type &&
                        s.Participants.Any(p => p.UserCode == resolvedUserCode || p.UserCode == userCode))
            .OrderByDescending(s => s.CreatedAt)
            .ToListAsync();

        var responses = new List<ChatSessionResponse>();
        foreach (var s in sessions)
        {
            responses.Add(await ToSessionResponseAsync(s, bearerToken));
        }
        return responses;
    }

    public async Task<PageResponse<MessageResponse>> GetMessagesAsync(Guid sessionId, int page, int size)
    {
        var query = _dbContext.Messages
            .Where(m => m.ChatSessionId == sessionId)
            .OrderBy(m => m.CreatedAt);

        var totalElements = await query.CountAsync();
        var totalPages = (int)Math.Ceiling(totalElements / (double)size);

        var items = await query
            .Skip(page * size)
            .Take(size)
            .Select(m => new MessageResponse
            {
                Id = m.Id,
                ChatSessionId = m.ChatSessionId,
                SenderCode = m.SenderCode,
                SenderRole = m.SenderRole,
                Content = m.Content,
                CreatedAt = m.CreatedAt
            })
            .ToListAsync();

        return new PageResponse<MessageResponse>
        {
            CurrentPage = page,
            TotalPages = totalPages,
            PageSize = size,
            TotalElements = totalElements,
            Content = items
        };
    }

    public async Task<MessageResponse> SendMessageAsync(
        SendMessageRequest request,
        string userCode,
        string? userRole,
        string? bearerToken)
    {
        var (resolvedUserCode, resolvedRole) = await NormalizeUserAsync(userCode, userRole, bearerToken);

        var session = await _dbContext.ChatSessions
            .Include(s => s.Participants)
            .FirstOrDefaultAsync(s => s.Id == request.ChatSessionId)
            ?? throw new KeyNotFoundException($"Chat session {request.ChatSessionId} not found");

        if (session.Status != ChatSessionStatus.ACTIVE)
        {
            throw new InvalidOperationException($"Chat session is {session.Status}, cannot send message");
        }

        // Map role
        var senderRole = SenderRole.CLIENT;
        if (!string.IsNullOrWhiteSpace(resolvedRole) && Enum.TryParse<SenderRole>(resolvedRole, true, out var parsedRole))
        {
            senderRole = parsedRole;
        }

        var message = new ChatMessage
        {
            ChatSessionId = session.Id,
            SenderCode = resolvedUserCode,
            SenderRole = senderRole,
            Content = request.Content,
            CreatedAt = DateTime.UtcNow
        };

        _dbContext.Messages.Add(message);
        await _dbContext.SaveChangesAsync();

        var messageResponse = new MessageResponse
        {
            Id = message.Id,
            ChatSessionId = message.ChatSessionId,
            SenderCode = message.SenderCode,
            SenderRole = message.SenderRole,
            Content = message.Content,
            CreatedAt = message.CreatedAt
        };

        // Broadcast to both STOMP WebSocket and SignalR Hub
        await _wsChatSender.BroadcastMessageToSessionAsync(session.Id, messageResponse);

        // NẾU LÀ PHÒNG CHAT VỚI AI => KÍCH HOẠT GEMINI AI BOT TRẢ LỜI TỰ ĐỘNG!
        if (session.Type == ChatType.AI)
        {
            var sessionId = session.Id;
            var prompt = request.Content;
            var scopeFactory = _scopeFactory;
            var geminiService = _geminiAiService;
            var wsSender = _wsChatSender;
            var logger = _logger;

            _ = Task.Run(async () =>
            {
                try
                {
                    using var scope = scopeFactory.CreateScope();
                    var db = scope.ServiceProvider.GetRequiredService<ChatDbContext>();

                    var rawRecent = await db.Messages
                        .Where(m => m.ChatSessionId == sessionId)
                        .OrderByDescending(m => m.CreatedAt)
                        .Take(6)
                        .OrderBy(m => m.CreatedAt)
                        .Select(m => new { m.SenderRole, m.Content })
                        .ToListAsync();

                    var recentMessages = rawRecent
                        .Select(m => (m.SenderRole == SenderRole.AI_BOT ? "assistant" : "user", m.Content))
                        .ToList();

                    var aiReplyContent = await geminiService.GenerateReplyAsync(prompt, recentMessages);

                    var botMessage = new ChatMessage
                    {
                        ChatSessionId = sessionId,
                        SenderCode = "NEURO_BOT",
                        SenderRole = SenderRole.AI_BOT,
                        Content = aiReplyContent,
                        CreatedAt = DateTime.UtcNow
                    };

                    db.Messages.Add(botMessage);
                    await db.SaveChangesAsync();

                    var botResponse = new MessageResponse
                    {
                        Id = botMessage.Id,
                        ChatSessionId = botMessage.ChatSessionId,
                        SenderCode = botMessage.SenderCode,
                        SenderRole = botMessage.SenderRole,
                        Content = botMessage.Content,
                        CreatedAt = botMessage.CreatedAt
                    };

                    await wsSender.BroadcastMessageToSessionAsync(sessionId, botResponse);
                }
                catch (Exception ex)
                {
                    logger.LogError(ex, "[ChatService] Error in AI reply background task for session {SessionId}", sessionId);
                }
            });
        }

        return messageResponse;
    }

    public async Task<ChatSessionResponse> AddParticipantAsync(Guid sessionId, string userCode, string? bearerToken)
    {
        var session = await _dbContext.ChatSessions
            .Include(s => s.Participants)
            .FirstOrDefaultAsync(s => s.Id == sessionId)
            ?? throw new KeyNotFoundException($"Session {sessionId} not found");

        if (!session.Participants.Any(p => p.UserCode == userCode))
        {
            session.Participants.Add(new ChatSessionParticipant { ChatSessionId = sessionId, UserCode = userCode });
            await _dbContext.SaveChangesAsync();
        }

        return await ToSessionResponseAsync(session, bearerToken);
    }

    public async Task<ChatSessionResponse> RemoveParticipantAsync(Guid sessionId, string userCode, string? bearerToken)
    {
        var session = await _dbContext.ChatSessions
            .Include(s => s.Participants)
            .FirstOrDefaultAsync(s => s.Id == sessionId)
            ?? throw new KeyNotFoundException($"Session {sessionId} not found");

        var participant = session.Participants.FirstOrDefault(p => p.UserCode == userCode);
        if (participant != null)
        {
            session.Participants.Remove(participant);
            await _dbContext.SaveChangesAsync();
        }

        return await ToSessionResponseAsync(session, bearerToken);
    }

    public async Task<ChatSessionResponse> CloseChatSessionAsync(Guid sessionId, string? bearerToken)
    {
        var session = await _dbContext.ChatSessions
            .Include(s => s.Participants)
            .FirstOrDefaultAsync(s => s.Id == sessionId)
            ?? throw new KeyNotFoundException($"Session {sessionId} not found");

        session.Status = ChatSessionStatus.CLOSED;
        await _dbContext.SaveChangesAsync();
        return await ToSessionResponseAsync(session, bearerToken);
    }

    public async Task<ChatSessionResponse> BlockChatSessionAsync(Guid sessionId, string? bearerToken)
    {
        var session = await _dbContext.ChatSessions
            .Include(s => s.Participants)
            .FirstOrDefaultAsync(s => s.Id == sessionId)
            ?? throw new KeyNotFoundException($"Session {sessionId} not found");

        session.Status = ChatSessionStatus.BLOCKED;
        await _dbContext.SaveChangesAsync();
        return await ToSessionResponseAsync(session, bearerToken);
    }

    private async Task<ChatSessionResponse> ToSessionResponseAsync(ChatSession session, string? bearerToken)
    {
        var participantCodes = session.Participants.Select(p => p.UserCode).ToHashSet();
        var participantDetails = new List<ParticipantDetail>();

        foreach (var code in participantCodes)
        {
            if (code == "NEURO_BOT")
            {
                participantDetails.Add(new ParticipantDetail
                {
                    Id = "NEURO_BOT",
                    Code = "NEURO_BOT",
                    FullName = "NeuroBot (AI Assistant)",
                    Role = "AI_BOT",
                    AvatarUrl = "https://cdn-icons-png.flaticon.com/512/4712/4712035.png"
                });
            }
            else
            {
                var detail = await _identityClient.GetUserByCodeAsync(code, bearerToken);
                participantDetails.Add(detail ?? new ParticipantDetail { Code = code, FullName = code });
            }
        }

        var lastMessage = session.Messages?
            .OrderByDescending(m => m.CreatedAt)
            .Select(m => new MessageResponse
            {
                Id = m.Id,
                ChatSessionId = m.ChatSessionId,
                SenderCode = m.SenderCode,
                SenderRole = m.SenderRole,
                Content = m.Content,
                CreatedAt = m.CreatedAt
            })
            .FirstOrDefault();

        return new ChatSessionResponse
        {
            Id = session.Id,
            WorkspaceId = session.WorkspaceId,
            Status = session.Status,
            Type = session.Type,
            CreatedAt = session.CreatedAt,
            ParticipantCodes = participantCodes,
            Participants = participantDetails,
            LastMessage = lastMessage
        };
    }
}
