using System.ComponentModel.DataAnnotations;

namespace MediBridge.Notify.Api.Models;

public class SendSmsRequest
{
    [Required]
    public string ToPhone { get; set; } = "";

    [Required]
    public string Body { get; set; } = "";
}
