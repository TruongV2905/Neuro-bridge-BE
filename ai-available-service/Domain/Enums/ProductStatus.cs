using NpgsqlTypes;

namespace AiAvailableService.Domain.Enums;

public enum ProductStatus
{
    [PgName("DRAFT")]
    Draft,

    [PgName("PUBLISHED")]
    Published,

    [PgName("UNPUBLISHED")]
    Unpublished
}
