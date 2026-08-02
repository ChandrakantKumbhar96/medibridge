using MediBridge.Notify.SpringClient;
using MediBridge.Notify.Sms;
using Microsoft.Extensions.Logging;

namespace MediBridge.Notify.Jobs;

// Phase 8: SpringApiClient can now authenticate against Spring, so this pulls
// the reminder-candidate list and sends each via the notify-service's own
// ISmsSender.
//
// NOTE: Spring's own AppointmentScheduler.sendUpcomingReminders already sends
// this same reminder on its hourly sweep, with its own dedup ledger. The two
// are not coordinated, so running both sends the SMS twice today. Retiring
// the Spring-side sweep (or adding shared dedup) is a follow-up, not done
// here - this job proves the notify-service path end to end.
public class NoShowReminderJob(
    SpringApiClient springApiClient,
    ISmsSender smsSender,
    ILogger<NoShowReminderJob> logger) : IJob
{
    public async Task RunAsync(CancellationToken ct = default)
    {
        var candidates = await springApiClient.GetReminderCandidatesAsync(ct);
        if (candidates.Count == 0)
        {
            logger.LogInformation("No-show reminder sweep: no candidates due");
            return;
        }

        int sent = 0;
        foreach (var candidate in candidates)
        {
            if (string.IsNullOrWhiteSpace(candidate.PatientPhone))
            {
                logger.LogWarning(
                    "Skipping reminder for appointment {AppointmentId}: no resolvable phone",
                    candidate.AppointmentId);
                continue;
            }

            var body =
                $"MediBridge reminder: consultation with {candidate.DoctorName} on {candidate.AppointmentDate}. Open the app and press Join at your slot time.";
            var result = await smsSender.SendAsync(candidate.PatientPhone, body, ct);
            if (result.Success)
            {
                sent++;
            }
            else
            {
                logger.LogWarning(
                    "Reminder SMS failed for appointment {AppointmentId}: {Error}",
                    candidate.AppointmentId, result.Error);
            }
        }

        logger.LogInformation("No-show reminder sweep: sent {Sent} of {Total}", sent, candidates.Count);
    }
}
