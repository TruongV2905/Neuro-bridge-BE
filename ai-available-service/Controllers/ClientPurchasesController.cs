using AiAvailableService.Api;
using AiAvailableService.Application;
using AiAvailableService.Contracts;
using Microsoft.AspNetCore.Authorization;
using Microsoft.AspNetCore.Mvc;

namespace AiAvailableService.Controllers;

[ApiController]
[Authorize(Policy = "ClientOnly")]
[Route("v1/ai-place/client/purchases")]
public sealed class ClientPurchasesController(
    IPurchaseReadApplicationService purchases,
    IProductRatingApplicationService ratings) : ControllerBase
{
    [HttpGet]
    [ProducesResponseType(typeof(ApiEnvelope<PageResponse<PurchaseHistoryItemResponse>>), StatusCodes.Status200OK)]
    public async Task<ActionResult<ApiEnvelope<PageResponse<PurchaseHistoryItemResponse>>>> List(
        [FromQuery] int page = Pagination.DefaultPage,
        [FromQuery] int size = Pagination.DefaultSize,
        CancellationToken cancellationToken = default)
    {
        var response = await purchases.ListAsync(page, size, cancellationToken);
        return Ok(ApiEnvelope<PageResponse<PurchaseHistoryItemResponse>>.Success(StatusCodes.Status200OK, response));
    }

    [HttpGet("{purchaseId}")]
    [ProducesResponseType(typeof(ApiEnvelope<PurchaseDetailResponse>), StatusCodes.Status200OK)]
    public async Task<ActionResult<ApiEnvelope<PurchaseDetailResponse>>> Get(
        Guid purchaseId,
        CancellationToken cancellationToken)
    {
        var response = await purchases.GetDetailAsync(purchaseId, cancellationToken);
        return Ok(ApiEnvelope<PurchaseDetailResponse>.Success(StatusCodes.Status200OK, response));
    }

    [HttpPost("{purchaseId}/rating")]
    [ProducesResponseType(typeof(ApiEnvelope<ProductRatingResponse>), StatusCodes.Status201Created)]
    public async Task<ActionResult<ApiEnvelope<ProductRatingResponse>>> CreateRating(
        Guid purchaseId,
        [FromBody] CreateProductRatingRequest request,
        CancellationToken cancellationToken)
    {
        var response = await ratings.CreateAsync(purchaseId, request, cancellationToken);
        return StatusCode(StatusCodes.Status201Created, ApiEnvelope<ProductRatingResponse>.Success(
            StatusCodes.Status201Created, response, "Product rating created."));
    }

    [HttpPut("{purchaseId}/rating")]
    [ProducesResponseType(typeof(ApiEnvelope<ProductRatingResponse>), StatusCodes.Status200OK)]
    public async Task<ActionResult<ApiEnvelope<ProductRatingResponse>>> UpdateRating(
        Guid purchaseId,
        [FromBody] UpdateProductRatingRequest request,
        CancellationToken cancellationToken)
    {
        var response = await ratings.UpdateAsync(purchaseId, request, cancellationToken);
        return Ok(ApiEnvelope<ProductRatingResponse>.Success(StatusCodes.Status200OK, response));
    }

    [HttpDelete("{purchaseId}/rating")]
    [ProducesResponseType(StatusCodes.Status204NoContent)]
    public async Task<IActionResult> DeleteRating(Guid purchaseId, CancellationToken cancellationToken)
    {
        await ratings.DeleteAsync(purchaseId, cancellationToken);
        return NoContent();
    }
}
