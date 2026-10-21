using NpgsqlTypes;

namespace AiAvailableService.Domain.Enums;

public enum PurchaseStatus
{
    [PgName("PENDING_PAYMENT")]
    PendingPayment,

    [PgName("SUCCESS")]
    Success
}
