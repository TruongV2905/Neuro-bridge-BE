using AiAvailableService.Api;
using AiAvailableService.Application;
using AiAvailableService.Contracts;
using Microsoft.AspNetCore.Authorization;
using Microsoft.AspNetCore.Mvc;

namespace AiAvailableService.Controllers;

[ApiController]
[Authorize(Policy = "ExpertOnly")]
[Route("v1/ai-place/expert/products")]
public sealed class ExpertProductStorageController(IProductStorageApplicationService storage) : ControllerBase
{
    [HttpPost("{productId}/preview/upload-authorizations")]
    [ProducesResponseType(typeof(ApiEnvelope<SupabaseUploadAuthorizationResponse>), StatusCodes.Status200OK)]
    public async Task<ActionResult<ApiEnvelope<SupabaseUploadAuthorizationResponse>>> CreatePreviewUploadAuthorization(
        Guid productId,
        [FromBody] PreviewUploadAuthorizationRequest request,
        CancellationToken cancellationToken)
    {
        var response = await storage.CreatePreviewUploadAuthorizationAsync(productId, request, cancellationToken);
        return Ok(ApiEnvelope<SupabaseUploadAuthorizationResponse>.Success(StatusCodes.Status200OK, response));
    }

    [HttpPut("{productId}/preview")]
    [ProducesResponseType(typeof(ApiEnvelope<PublicPreviewResponse>), StatusCodes.Status200OK)]
    public async Task<ActionResult<ApiEnvelope<PublicPreviewResponse>>> ConfirmPreview(
        Guid productId,
        [FromBody] ConfirmPreviewUploadRequest request,
        CancellationToken cancellationToken)
    {
        var response = await storage.ConfirmPreviewAsync(productId, request, cancellationToken);
        return Ok(ApiEnvelope<PublicPreviewResponse>.Success(StatusCodes.Status200OK, response));
    }

    [HttpDelete("{productId}/preview")]
    [ProducesResponseType(StatusCodes.Status204NoContent)]
    public async Task<IActionResult> DeletePreview(Guid productId, CancellationToken cancellationToken)
    {
        await storage.DeletePreviewAsync(productId, cancellationToken);
        return NoContent();
    }

    [HttpPost("{productId}/assets/{assetType}/upload-authorizations")]
    [ProducesResponseType(typeof(ApiEnvelope<SupabaseUploadAuthorizationResponse>), StatusCodes.Status200OK)]
    public async Task<ActionResult<ApiEnvelope<SupabaseUploadAuthorizationResponse>>> CreateAssetUploadAuthorization(
        Guid productId,
        string assetType,
        [FromBody] ProtectedAssetUploadAuthorizationRequest request,
        CancellationToken cancellationToken)
    {
        var response = await storage.CreateAssetUploadAuthorizationAsync(productId, assetType, request, cancellationToken);
        return Ok(ApiEnvelope<SupabaseUploadAuthorizationResponse>.Success(StatusCodes.Status200OK, response));
    }

    [HttpPut("{productId}/assets/{assetType}")]
    [ProducesResponseType(typeof(ApiEnvelope<ProtectedAssetResponse>), StatusCodes.Status200OK)]
    public async Task<ActionResult<ApiEnvelope<ProtectedAssetResponse>>> ConfirmAsset(
        Guid productId,
        string assetType,
        [FromBody] ConfirmProtectedAssetUploadRequest request,
        CancellationToken cancellationToken)
    {
        var response = await storage.ConfirmAssetAsync(productId, assetType, request, cancellationToken);
        return Ok(ApiEnvelope<ProtectedAssetResponse>.Success(StatusCodes.Status200OK, response));
    }

    [HttpDelete("{productId}/assets/{assetType}")]
    [ProducesResponseType(StatusCodes.Status204NoContent)]
    public async Task<IActionResult> DeleteAsset(
        Guid productId,
        string assetType,
        CancellationToken cancellationToken)
    {
        await storage.DeleteAssetAsync(productId, assetType, cancellationToken);
        return NoContent();
    }
}
