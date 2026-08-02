using MediBridge.Notify.Api.Models;
using MediBridge.Notify.Sms;
using Microsoft.AspNetCore.Mvc;

namespace MediBridge.Notify.Api.Controllers;

[ApiController]
[Route("notifications")]
public class NotificationsController(ISmsSender smsSender) : ControllerBase
{
    // ISmsSender resolves to TwilioSmsSender once Twilio:AccountSid is set in
    // appsettings.Local.json, otherwise falls back to ConsoleSmsSender (logs only).
    [HttpPost("sms")]
    public async Task<IActionResult> SendSms([FromBody] SendSmsRequest request, CancellationToken ct)
    {
        var result = await smsSender.SendAsync(request.ToPhone, request.Body, ct);
        return Ok(result);
    }
}
