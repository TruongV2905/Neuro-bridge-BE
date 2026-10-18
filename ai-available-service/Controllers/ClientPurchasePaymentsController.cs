using AiAvailableService.Api;
using AiAvailableService.Application;
using AiAvailableService.Contracts;
using Microsoft.AspNetCore.Authorization;
using Microsoft.AspNetCore.Mvc;

namespace AiAvailableService.Controllers;

[ApiController]
[Authorize(Policy = "ClientOnly")]
[Route("v1/ai-place/client")]
public sealed class ClientPurchasePaymentsController(IPurchasePaymentApplicationService purchases) : ControllerBase
{
    [HttpPost("products/{productId}/purchases")]
    [ProducesResponseType(typeof(ApiEnvelope<PurchaseSettlementResponse>), StatusCodes.Status200OK)]
    public async Task<ActionResult<ApiEnvelope<PurchaseSettlementResponse>>> Purchase(
        Guid productId,
        CancellationToken cancellationToken)
    {
        var response = await purchases.PurchaseAsync(productId, cancellationToken);
        return Ok(ApiEnvelope<PurchaseSettlementResponse>.Success(StatusCodes.Status200OK, response));
    }

    [HttpPost("purchases/{purchaseId}/retry-payment")]
    [ProducesResponseType(typeof(ApiEnvelope<PurchaseSettlementResponse>), StatusCodes.Status200OK)]
    public async Task<ActionResult<ApiEnvelope<PurchaseSettlementResponse>>> RetryPayment(
        Guid purchaseId,
        CancellationToken cancellationToken)
    {
        var response = await purchases.RetryAsync(purchaseId, cancellationToken);
        return Ok(ApiEnvelope<PurchaseSettlementResponse>.Success(StatusCodes.Status200OK, response));
    }
}
