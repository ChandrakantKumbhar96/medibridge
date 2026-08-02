namespace MediBridge.Notify.Reports;

// Mirrors the subset of AppointmentResponse (Spring) relevant to exports.
// Rows are supplied by the caller in the request body rather than fetched
// here - SpringApiClient can't hold a JWT yet (see its Phase 7/8 comment),
// so this service has no way to pull appointment data on its own until the
// Spring-side internal auth story (Phase 8) exists.
public record AppointmentReportRow(
    int AppointmentId,
    string AppointmentDate,
    string Time,
    string Doctor,
    string Specialization,
    string Patient,
    string Status,
    decimal ConsultationFee);
