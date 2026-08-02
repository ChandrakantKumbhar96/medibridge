using Microsoft.Extensions.Logging;
using Microsoft.Extensions.Options;
using Twilio.Clients;
using Twilio.Rest.Api.V2010.Account;
using Twilio.Types;

namespace MediBridge.Notify.Sms;

// Phase 7b: real carrier call via Twilio. TwilioClient is a static, so the
// account credentials are applied once via ITwilioRestClient rather than
// mutating global state on every send.
public class TwilioSmsSender : ISmsSender
{
    private readonly ITwilioRestClient _client;
    private readonly TwilioOptions _options;
    private readonly ILogger<TwilioSmsSender> _logger;

    public TwilioSmsSender(IOptions<TwilioOptions> options, ILogger<TwilioSmsSender> logger)
    {
        _options = options.Value;
        _logger = logger;
        _client = new TwilioRestClient(_options.AccountSid, _options.AuthToken);
    }

    public async Task<SmsSendResult> SendAsync(string toPhone, string body, CancellationToken ct = default)
    {
        try
        {
            var message = await MessageResource.CreateAsync(
                to: new PhoneNumber(toPhone),
                from: new PhoneNumber(_options.FromNumber),
                body: body,
                client: _client);

            if (message.ErrorCode is not null)
            {
                _logger.LogWarning("Twilio SMS to {ToPhone} failed: {ErrorCode} {ErrorMessage}",
                    toPhone, message.ErrorCode, message.ErrorMessage);
                return new SmsSendResult(false, Provider: "twilio", MessageId: message.Sid, Error: message.ErrorMessage);
            }

            return new SmsSendResult(true, Provider: "twilio", MessageId: message.Sid);
        }
        catch (Exception ex)
        {
            _logger.LogError(ex, "Twilio SMS to {ToPhone} threw", toPhone);
            return new SmsSendResult(false, Provider: "twilio", MessageId: null, Error: ex.Message);
        }
    }
}
