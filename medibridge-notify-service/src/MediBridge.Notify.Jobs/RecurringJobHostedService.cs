using Microsoft.Extensions.DependencyInjection;
using Microsoft.Extensions.Hosting;
using Microsoft.Extensions.Logging;
using Microsoft.Extensions.Options;

namespace MediBridge.Notify.Jobs;

// Phase 7d: the scheduler shell. A built-in BackgroundService + PeriodicTimer
// was chosen over Hangfire/Quartz - a single recurring job doesn't need an
// external job store or dashboard, and it keeps this in line with the
// no-new-complex-dependency call made for CSV over PDF in Phase 7c.
//
// TJob is resolved from a fresh scope on every tick (not injected directly)
// so a future job that depends on scoped services - e.g. SpringApiClient's
// eventual per-request HttpClient - gets a clean instance each run, same as
// it would per HTTP request.
//
// Each tick is try/caught and logged rather than left to fault the loop:
// NoShowReminderJob.RunAsync still throws NotImplementedException until
// SpringApiClient can authenticate to Spring (Phase 8), and one throwing job
// must not take down the rest of the host.
public class RecurringJobHostedService<TJob>(
    IServiceScopeFactory scopeFactory,
    IOptions<JobsOptions> options,
    ILogger<RecurringJobHostedService<TJob>> logger) : BackgroundService
    where TJob : IJob
{
    protected override async Task ExecuteAsync(CancellationToken stoppingToken)
    {
        var interval = TimeSpan.FromMinutes(options.Value.NoShowReminderIntervalMinutes);
        using var timer = new PeriodicTimer(interval);

        do
        {
            try
            {
                using var scope = scopeFactory.CreateScope();
                var job = scope.ServiceProvider.GetRequiredService<TJob>();
                await job.RunAsync(stoppingToken);
            }
            catch (Exception ex) when (ex is not OperationCanceledException)
            {
                logger.LogError(ex, "{JobType} run failed", typeof(TJob).Name);
            }
        } while (await timer.WaitForNextTickAsync(stoppingToken));
    }
}
