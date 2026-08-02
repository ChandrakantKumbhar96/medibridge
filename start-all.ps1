# Starts every MediBridge service, each in its own PowerShell window.
# Prereqs this script does NOT start: MySQL (backend) and Redis on localhost:6379 (chat-service).
# Run from anywhere: .\start-all.ps1

$root = $PSScriptRoot

$services = @(
    @{
        Name    = "backend (8080)"
        Path    = "$root\medibridge-backend"
        Command = "`$env:SPRING_PROFILES_ACTIVE='local'; mvn spring-boot:run"
    },
    @{
        Name    = "frontend (5173)"
        Path    = "$root\medibridge-frontend"
        Command = "npm run dev"
    },
    @{
        Name    = "gateway (4000)"
        Path    = "$root\medibridge-gateway"
        Command = "npm run dev"
    },
    @{
        Name    = "notify-service (5154)"
        Path    = "$root\medibridge-notify-service"
        Command = "dotnet run --project src/MediBridge.Notify.Api"
    },
    @{
        Name    = "chat-service (8000)"
        Path    = "$root\medibridge-chat-service"
        Command = "python -m uvicorn app.main:app --reload --port 8000"
    }
)

foreach ($svc in $services) {
    $title = $svc.Name
    $cmd = "`$host.UI.RawUI.WindowTitle = '$title'; Set-Location '$($svc.Path)'; $($svc.Command)"
    Start-Process powershell -ArgumentList "-NoExit", "-Command", $cmd
}

Write-Host "Launched: $($services.Name -join ', ')"
Write-Host "Make sure MySQL and Redis (localhost:6379) are already running."
