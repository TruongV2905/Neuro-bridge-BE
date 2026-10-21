using NpgsqlTypes;

namespace AiAvailableService.Domain.Enums;

public enum PreviewType
{
    [PgName("IMAGE")]
    Image,

    [PgName("VIDEO")]
    Video
}
