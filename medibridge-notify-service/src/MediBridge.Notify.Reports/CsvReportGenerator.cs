using System.Globalization;
using System.Text;

namespace MediBridge.Notify.Reports;

// Phase 7c: CSV, not PDF - these are admin exports meant to open in a
// spreadsheet, so there's no rendering engine to fight (contrast the Spring
// side's openhtmltopdf well-formed-XHTML gotcha in CLAUDE.md - no equivalent
// trap here).
public class CsvReportGenerator : IReportGenerator
{
    public byte[] Generate(string reportType, IReadOnlyList<AppointmentReportRow> rows) => reportType switch
    {
        "appointments" => AppointmentsCsv(rows),
        "revenue" => RevenueCsv(rows),
        _ => throw new ArgumentOutOfRangeException(
            nameof(reportType), reportType, "Unknown report type. Expected 'appointments' or 'revenue'."),
    };

    private static byte[] AppointmentsCsv(IReadOnlyList<AppointmentReportRow> rows)
    {
        var sb = new StringBuilder();
        sb.AppendLine("appointment_id,date,time,doctor,specialization,patient,status,consultation_fee");
        foreach (var r in rows)
        {
            sb.AppendLine(string.Join(',',
                r.AppointmentId.ToString(CultureInfo.InvariantCulture),
                Csv(r.AppointmentDate),
                Csv(r.Time),
                Csv(r.Doctor),
                Csv(r.Specialization),
                Csv(r.Patient),
                Csv(r.Status),
                r.ConsultationFee.ToString(CultureInfo.InvariantCulture)));
        }
        return Encoding.UTF8.GetBytes(sb.ToString());
    }

    private static byte[] RevenueCsv(IReadOnlyList<AppointmentReportRow> rows)
    {
        // "completed" is the frontend-facing status (AppointmentStatus.toFrontend()
        // on the Spring side) - the raw DB value 'Completed' never reaches here.
        var byDate = rows
            .Where(r => r.Status == "completed")
            .GroupBy(r => r.AppointmentDate)
            .OrderBy(g => g.Key, StringComparer.Ordinal);

        var sb = new StringBuilder();
        sb.AppendLine("date,completed_appointments,revenue");
        foreach (var g in byDate)
        {
            var revenue = g.Sum(r => r.ConsultationFee);
            sb.AppendLine($"{Csv(g.Key)},{g.Count()},{revenue.ToString(CultureInfo.InvariantCulture)}");
        }
        return Encoding.UTF8.GetBytes(sb.ToString());
    }

    private static string Csv(string value) =>
        value.Contains(',') || value.Contains('"')
            ? $"\"{value.Replace("\"", "\"\"")}\""
            : value;
}
