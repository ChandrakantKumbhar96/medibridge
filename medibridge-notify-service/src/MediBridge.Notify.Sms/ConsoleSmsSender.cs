using Microsoft.Extensions.Logging;

namespace MediBridge.Notify.Sms;

// Phase 7a: logs instead of placing a real carrier call. Wired into the API
// today so the /notifications/sms round trip is real end-to-end; swap the DI
// registration to TwilioSmsSender once carrier credentials exist.
public class ConsoleSmsSender(ILogger<ConsoleSmsSender> logger) : ISmsSender
{
    public Task<SmsSendResult> SendAsync(string toPhone, string body, CancellationToken ct = default)
    {
        logger.LogInformation("SMS to {ToPhone}: {Body}", toPhone, body);
        return Task.FromResult(new SmsSendResult(true, Provider: "console", MessageId: Guid.NewGuid().ToString()));
    }
}
