using Microsoft.AspNetCore.Mvc;

namespace MediBridge.Notify.Api.Controllers;

[ApiController]
[Route("health")]
public class HealthController : ControllerBase
{
    [HttpGet]
    public IActionResult Get() => Ok(new { status = "ok", service = "medibridge-notify-service" });
}
