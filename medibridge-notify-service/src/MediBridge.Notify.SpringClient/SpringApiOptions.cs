namespace MediBridge.Notify.SpringClient;

public class SpringApiOptions
{
    public const string SectionName = "Spring";

    // Trailing slash matters: HttpClient/Uri combines a relative path with the
    // base by replacing everything after the base's final "/", so a base
    // without one silently drops the last path segment ("/api") entirely.
    public string BaseUrl { get; set; } = "http://localhost:8080/api/";
    public string InternalApiKey { get; set; } = "";
}
