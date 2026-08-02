FROM mcr.microsoft.com/dotnet/sdk:10.0 AS build
WORKDIR /src

COPY src/MediBridge.Notify.Api/MediBridge.Notify.Api.csproj src/MediBridge.Notify.Api/
COPY src/MediBridge.Notify.Sms/MediBridge.Notify.Sms.csproj src/MediBridge.Notify.Sms/
COPY src/MediBridge.Notify.SpringClient/MediBridge.Notify.SpringClient.csproj src/MediBridge.Notify.SpringClient/
RUN dotnet restore src/MediBridge.Notify.Api/MediBridge.Notify.Api.csproj

COPY src/ src/
RUN dotnet publish src/MediBridge.Notify.Api/MediBridge.Notify.Api.csproj -c Release -o /app

FROM mcr.microsoft.com/dotnet/aspnet:10.0 AS runtime
WORKDIR /app
COPY --from=build /app .

EXPOSE 8000
ENV ASPNETCORE_URLS=http://+:8000
ENTRYPOINT ["dotnet", "MediBridge.Notify.Api.dll"]
