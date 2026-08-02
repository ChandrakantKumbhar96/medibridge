using System.Text.Json.Serialization;

namespace MediBridge.Notify.SpringClient;

// Mirrors Spring's ReminderCandidateResponse (snake_case on the wire).
public record ReminderCandidate(
    [property: JsonPropertyName("appointment_id")] int AppointmentId,
    [property: JsonPropertyName("patient_phone")] string? PatientPhone,
    [property: JsonPropertyName("patient_name")] string PatientName,
    [property: JsonPropertyName("doctor_name")] string DoctorName,
    [property: JsonPropertyName("appointment_date")] string AppointmentDate);
