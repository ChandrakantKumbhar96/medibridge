namespace MediBridge.Notify.Jobs;

public class JobsOptions
{
    public const string SectionName = "Jobs";

    public int NoShowReminderIntervalMinutes { get; set; } = 15;
}
