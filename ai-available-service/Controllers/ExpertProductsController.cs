using AiAvailableService.Api;
using AiAvailableService.Application;
using AiAvailableService.Contracts;
using Microsoft.AspNetCore.Authorization;
using Microsoft.AspNetCore.Mvc;

namespace AiAvailableService.Controllers;

[ApiController]
[Authorize(Policy = "ExpertOnly")]
[Route("v1/ai-place/expert/products")]
public sealed class ExpertProductsController(IProductApplicationService products) : ControllerBase
{
    [HttpPost]
    [ProducesResponseType(typeof(ApiEnvelope<OwnerProductDetailResponse>), StatusCodes.Status201Created)]
    public async Task<ActionResult<ApiEnvelope<OwnerProductDetailResponse>>> Create(
        [FromBody] CreateDraftProductRequest request,
        CancellationToken cancellationToken)
    {
        var response = await products.CreateDraftAsync(request, cancellationToken);
        return StatusCode(StatusCodes.Status201Created, ApiEnvelope<OwnerProductDetailResponse>.Success(
            StatusCodes.Status201Created, response, "Draft Product created."));
    }

    [HttpGet]
    [ProducesResponseType(typeof(ApiEnvelope<PageResponse<OwnerProductListItemResponse>>), StatusCodes.Status200OK)]
    public async Task<ActionResult<ApiEnvelope<PageResponse<OwnerProductListItemResponse>>>> List(
        [FromQuery] int page = Pagination.DefaultPage,
        [FromQuery] int size = Pagination.DefaultSize,
        CancellationToken cancellationToken = default)
    {
        var response = await products.ListOwnAsync(page, size, cancellationToken);
        return Ok(ApiEnvelope<PageResponse<OwnerProductListItemResponse>>.Success(StatusCodes.Status200OK, response));
    }

    [HttpGet("{productId}")]
    [ProducesResponseType(typeof(ApiEnvelope<OwnerProductDetailResponse>), StatusCodes.Status200OK)]
    public async Task<ActionResult<ApiEnvelope<OwnerProductDetailResponse>>> Get(
        Guid productId,
        CancellationToken cancellationToken)
    {
        var response = await products.GetOwnAsync(productId, cancellationToken);
        return Ok(ApiEnvelope<OwnerProductDetailResponse>.Success(StatusCodes.Status200OK, response));
    }

    [HttpPut("{productId}")]
    [ProducesResponseType(typeof(ApiEnvelope<OwnerProductDetailResponse>), StatusCodes.Status200OK)]
    public async Task<ActionResult<ApiEnvelope<OwnerProductDetailResponse>>> Update(
        Guid productId,
        [FromBody] UpdateProductRequest request,
        CancellationToken cancellationToken)
    {
        var response = await products.UpdateAsync(productId, request, cancellationToken);
        return Ok(ApiEnvelope<OwnerProductDetailResponse>.Success(StatusCodes.Status200OK, response));
    }

    [HttpDelete("{productId}")]
    [ProducesResponseType(StatusCodes.Status204NoContent)]
    public async Task<IActionResult> Delete(Guid productId, CancellationToken cancellationToken)
    {
        await products.DeleteAsync(productId, cancellationToken);
        return NoContent();
    }

    [HttpPost("{productId}/publish")]
    [ProducesResponseType(typeof(ApiEnvelope<LifecycleActionResponse>), StatusCodes.Status200OK)]
    public async Task<ActionResult<ApiEnvelope<LifecycleActionResponse>>> Publish(
        Guid productId,
        CancellationToken cancellationToken)
    {
        var response = await products.PublishAsync(productId, cancellationToken);
        return Ok(ApiEnvelope<LifecycleActionResponse>.Success(StatusCodes.Status200OK, response));
    }

    [HttpPost("{productId}/unpublish")]
    [ProducesResponseType(typeof(ApiEnvelope<LifecycleActionResponse>), StatusCodes.Status200OK)]
    public async Task<ActionResult<ApiEnvelope<LifecycleActionResponse>>> Unpublish(
        Guid productId,
        CancellationToken cancellationToken)
    {
        var response = await products.UnpublishAsync(productId, cancellationToken);
        return Ok(ApiEnvelope<LifecycleActionResponse>.Success(StatusCodes.Status200OK, response));
    }
}
