using System.Net.Http.Json;
using Microsoft.Extensions.Options;

namespace MediBridge.Notify.SpringClient;

// Phase 8: Spring now accepts a shared secret on X-Internal-Api-Key for
// /internal/** routes (InternalApiKeyAuthFilter + SecurityConfig on the
// Spring side), so this can finally make an authenticated call instead of
// throwing. SpringApiOptions.InternalApiKey must match
// medibridge.internal.api-key in Spring's application-local.yml.
public class SpringApiClient
{
    private const string InternalApiKeyHeader = "X-Internal-Api-Key";

    private readonly HttpClient _httpClient;
    private readonly SpringApiOptions _options;

    public SpringApiClient(HttpClient httpClient, IOptions<SpringApiOptions> options)
    {
        _options = options.Value;
        httpClient.BaseAddress = new Uri(_options.BaseUrl);
        _httpClient = httpClient;
    }

    public async Task<string> GetAppointmentAsync(string appointmentId, CancellationToken ct = default)
    {
        using var request = new HttpRequestMessage(HttpMethod.Get, $"internal/appointments/{appointmentId}");
        request.Headers.Add(InternalApiKeyHeader, _options.InternalApiKey);

        var response = await _httpClient.SendAsync(request, ct);
        response.EnsureSuccessStatusCode();
        return await response.Content.ReadAsStringAsync(ct);
    }

    public async Task<IReadOnlyList<ReminderCandidate>> GetReminderCandidatesAsync(CancellationToken ct = default)
    {
        using var request = new HttpRequestMessage(HttpMethod.Get, "internal/appointments/reminder-candidates");
        request.Headers.Add(InternalApiKeyHeader, _options.InternalApiKey);

        var response = await _httpClient.SendAsync(request, ct);
        response.EnsureSuccessStatusCode();
        var candidates = await response.Content.ReadFromJsonAsync<List<ReminderCandidate>>(cancellationToken: ct);
        return candidates ?? [];
    }
}
