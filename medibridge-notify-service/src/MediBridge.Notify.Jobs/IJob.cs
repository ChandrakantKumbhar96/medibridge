namespace MediBridge.Notify.Jobs;

public interface IJob
{
    Task RunAsync(CancellationToken ct = default);
}
