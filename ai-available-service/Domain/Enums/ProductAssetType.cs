using NpgsqlTypes;

namespace AiAvailableService.Domain.Enums;

public enum ProductAssetType
{
    [PgName("SOURCE_CODE")]
    SourceCode,

    [PgName("VIDEO")]
    Video,

    [PgName("DATASET")]
    Dataset,

    [PgName("DOCUMENT")]
    Document
}
