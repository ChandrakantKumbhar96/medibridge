using MediBridge.Notify.Api.Models;
using MediBridge.Notify.Reports;
using Microsoft.AspNetCore.Mvc;

namespace MediBridge.Notify.Api.Controllers;

// Rows come from the caller (the admin dashboard, via the gateway) rather
// than being fetched from Spring here - SpringApiClient has no way to hold a
// caller's JWT yet, see its own comment. That part is Phase 8.
[ApiController]
[Route("reports")]
public class ReportsController(IReportGenerator reportGenerator) : ControllerBase
{
    [HttpPost("{reportType}")]
    public IActionResult Generate(string reportType, [FromBody] GenerateReportRequest request)
    {
        byte[] csv;
        try
        {
            csv = reportGenerator.Generate(reportType, request.Rows);
        }
        catch (ArgumentOutOfRangeException ex)
        {
            return BadRequest(new { error = ex.Message });
        }

        return File(csv, "text/csv", $"{reportType}-report.csv");
    }
}
