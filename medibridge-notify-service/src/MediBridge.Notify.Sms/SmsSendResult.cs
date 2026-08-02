namespace MediBridge.Notify.Sms;

public record SmsSendResult(bool Success, string Provider, string? MessageId, string? Error = null);
