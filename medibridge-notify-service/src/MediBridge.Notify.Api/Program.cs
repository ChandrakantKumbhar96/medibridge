using MediBridge.Notify.Jobs;
using MediBridge.Notify.Reports;
using MediBridge.Notify.Sms;
using MediBridge.Notify.SpringClient;

var builder = WebApplication.CreateBuilder(args);

builder.Configuration.AddJsonFile("appsettings.Local.json", optional: true, reloadOnChange: true);

builder.Services.AddControllers();
builder.Services.AddOpenApi();

builder.Services.Configure<SpringApiOptions>(builder.Configuration.GetSection(SpringApiOptions.SectionName));
builder.Services.AddHttpClient<SpringApiClient>();

builder.Services.Configure<TwilioOptions>(builder.Configuration.GetSection(TwilioOptions.SectionName));

// Phase 7b: real carrier only once AccountSid is actually filled in
// appsettings.Local.json (gitignored); falls back to the console logger so a
// fresh checkout still boots and the /notifications/sms round trip still works.
var twilioAccountSid = builder.Configuration[$"{TwilioOptions.SectionName}:AccountSid"];
if (!string.IsNullOrWhiteSpace(twilioAccountSid) && twilioAccountSid != "set-me")
{
    builder.Services.AddScoped<ISmsSender, TwilioSmsSender>();
}
else
{
    builder.Services.AddScoped<ISmsSender, ConsoleSmsSender>();
}

builder.Services.AddScoped<IReportGenerator, CsvReportGenerator>();

// Phase 8: NoShowReminderJob now pulls reminder candidates from Spring
// (authenticated via SpringApiClient) and sends each through ISmsSender.
// Runs unconditionally alongside Spring's own AppointmentScheduler reminder
// sweep - the two are not deduplicated against each other yet.
builder.Services.Configure<JobsOptions>(builder.Configuration.GetSection(JobsOptions.SectionName));
builder.Services.AddScoped<NoShowReminderJob>();
builder.Services.AddHostedService<RecurringJobHostedService<NoShowReminderJob>>();

var app = builder.Build();

if (app.Environment.IsDevelopment())
{
    app.MapOpenApi();
}

app.UseHttpsRedirection();
app.UseAuthorization();
app.MapControllers();

app.Run();
