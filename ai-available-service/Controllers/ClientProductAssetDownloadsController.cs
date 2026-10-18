using AiAvailableService.Api;
using AiAvailableService.Application;
using AiAvailableService.Contracts;
using Microsoft.AspNetCore.Authorization;
using Microsoft.AspNetCore.Mvc;

namespace AiAvailableService.Controllers;

[ApiController]
[Authorize(Policy = "ClientOnly")]
[Route("v1/ai-place/client/purchases")]
public sealed class ClientProductAssetDownloadsController(IProductStorageApplicationService storage) : ControllerBase
{
    [HttpGet("{purchaseId}/products/{productId}/assets/{assetType}/download-access")]
    [ProducesResponseType(typeof(ApiEnvelope<ProtectedDownloadAccessResponse>), StatusCodes.Status200OK)]
    public async Task<ActionResult<ApiEnvelope<ProtectedDownloadAccessResponse>>> GetDownloadAccess(
        Guid purchaseId,
        Guid productId,
        string assetType,
        CancellationToken cancellationToken)
    {
        var response = await storage.GetDownloadAccessAsync(purchaseId, productId, assetType, cancellationToken);
        return Ok(ApiEnvelope<ProtectedDownloadAccessResponse>.Success(StatusCodes.Status200OK, response));
    }
}
