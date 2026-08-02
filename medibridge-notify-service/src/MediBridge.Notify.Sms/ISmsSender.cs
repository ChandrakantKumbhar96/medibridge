namespace MediBridge.Notify.Sms;

public interface ISmsSender
{
    Task<SmsSendResult> SendAsync(string toPhone, string body, CancellationToken ct = default);
}
