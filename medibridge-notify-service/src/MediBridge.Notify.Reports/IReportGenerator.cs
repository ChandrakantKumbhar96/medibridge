namespace MediBridge.Notify.Reports;

public interface IReportGenerator
{
    byte[] Generate(string reportType, IReadOnlyList<AppointmentReportRow> rows);
}
