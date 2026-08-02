using System.ComponentModel.DataAnnotations;
using MediBridge.Notify.Reports;

namespace MediBridge.Notify.Api.Models;

public class GenerateReportRequest
{
    [Required]
    public List<AppointmentReportRow> Rows { get; set; } = [];
}
