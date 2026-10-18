using AiAvailableService.Api;
using AiAvailableService.Application;
using AiAvailableService.Contracts;
using Microsoft.AspNetCore.Authorization;
using Microsoft.AspNetCore.Mvc;

namespace AiAvailableService.Controllers;

[ApiController]
[Authorize(Policy = "ClientOnly")]
[Route("v1/ai-place/products")]
public sealed class MarketplaceProductsController(
    IProductApplicationService products,
    IProductRatingApplicationService ratings) : ControllerBase
{
    [HttpGet]
    [ProducesResponseType(typeof(ApiEnvelope<PageResponse<MarketplaceProductListItemResponse>>), StatusCodes.Status200OK)]
    public async Task<ActionResult<ApiEnvelope<PageResponse<MarketplaceProductListItemResponse>>>> Browse(
        [FromQuery] BrowseProductsQuery query,
        CancellationToken cancellationToken)
    {
        var response = await products.BrowseAsync(query, cancellationToken);
        return Ok(ApiEnvelope<PageResponse<MarketplaceProductListItemResponse>>.Success(StatusCodes.Status200OK, response));
    }

    [HttpGet("{productId}")]
    [ProducesResponseType(typeof(ApiEnvelope<MarketplaceProductDetailResponse>), StatusCodes.Status200OK)]
    public async Task<ActionResult<ApiEnvelope<MarketplaceProductDetailResponse>>> Get(
        Guid productId,
        CancellationToken cancellationToken)
    {
        var response = await products.GetMarketplaceDetailAsync(productId, cancellationToken);
        return Ok(ApiEnvelope<MarketplaceProductDetailResponse>.Success(StatusCodes.Status200OK, response));
    }

    [HttpGet("{productId}/ratings")]
    [ProducesResponseType(typeof(ApiEnvelope<PageResponse<ProductRatingResponse>>), StatusCodes.Status200OK)]
    public async Task<ActionResult<ApiEnvelope<PageResponse<ProductRatingResponse>>>> ListRatings(
        Guid productId,
        [FromQuery] int page = Pagination.DefaultPage,
        [FromQuery] int size = Pagination.DefaultSize,
        CancellationToken cancellationToken = default)
    {
        var response = await ratings.ListAsync(productId, page, size, cancellationToken);
        return Ok(ApiEnvelope<PageResponse<ProductRatingResponse>>.Success(StatusCodes.Status200OK, response));
    }

    [HttpGet("{productId}/ratings/summary")]
    [ProducesResponseType(typeof(ApiEnvelope<ProductRatingSummaryResponse>), StatusCodes.Status200OK)]
    public async Task<ActionResult<ApiEnvelope<ProductRatingSummaryResponse>>> GetRatingSummary(
        Guid productId,
        CancellationToken cancellationToken)
    {
        var response = await ratings.GetSummaryAsync(productId, cancellationToken);
        return Ok(ApiEnvelope<ProductRatingSummaryResponse>.Success(StatusCodes.Status200OK, response));
    }
}
