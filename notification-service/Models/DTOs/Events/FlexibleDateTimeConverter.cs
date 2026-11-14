using System.Text.Json;
using System.Text.Json.Serialization;

namespace NotificationService.Models.DTOs.Events;

public class FlexibleDateTimeConverter : JsonConverter<DateTime?>
{
    public override DateTime? Read(ref Utf8JsonReader reader, Type typeToConvert, JsonSerializerOptions options)
    {
        if (reader.TokenType == JsonTokenType.Null)
        {
            return null;
        }

        if (reader.TokenType == JsonTokenType.String)
        {
            string? str = reader.GetString();
            if (string.IsNullOrWhiteSpace(str)) return null;

            if (DateTime.TryParse(str, out var dt))
            {
                return dt;
            }
            return null;
        }

        if (reader.TokenType == JsonTokenType.StartArray)
        {
            // Java Spring Boot Jackson format: [year, month, day, hour, minute, second, nano]
            var parts = new List<int>();
            while (reader.Read() && reader.TokenType != JsonTokenType.EndArray)
            {
                if (reader.TokenType == JsonTokenType.Number && reader.TryGetInt32(out var val))
                {
                    parts.Add(val);
                }
            }

            if (parts.Count >= 3)
            {
                int year = parts[0];
                int month = parts[1];
                int day = parts[2];
                int hour = parts.Count > 3 ? parts[3] : 0;
                int minute = parts.Count > 4 ? parts[4] : 0;
                int second = parts.Count > 5 ? parts[5] : 0;
                int millisecond = parts.Count > 6 ? (parts[6] / 1000000) : 0;

                try
                {
                    return new DateTime(year, month, day, hour, minute, second, millisecond, DateTimeKind.Utc);
                }
                catch
                {
                    return DateTime.UtcNow;
                }
            }
            return null;
        }

        return null;
    }

    public override void Write(Utf8JsonWriter writer, DateTime? value, JsonSerializerOptions options)
    {
        if (value.HasValue)
        {
            writer.WriteStringValue(value.Value.ToString("o"));
        }
        else
        {
            writer.WriteNullValue();
        }
    }
}
