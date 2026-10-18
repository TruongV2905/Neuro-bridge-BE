# Build from repository root so the authoritative linked proto files are in context:
# docker build -f ai-available-service/Dockerfile -t ai-available-service .
FROM mcr.microsoft.com/dotnet/sdk:8.0 AS build
WORKDIR /src

COPY ["ai-available-service/ai-available-service.csproj", "ai-available-service/"]
COPY ["common-starter/src/main/proto/user.proto", "common-starter/src/main/proto/"]
COPY ["common-starter/src/main/proto/payment.proto", "common-starter/src/main/proto/"]
RUN dotnet restore "ai-available-service/ai-available-service.csproj"

COPY ["ai-available-service/", "ai-available-service/"]
RUN dotnet publish "ai-available-service/ai-available-service.csproj" -c Release -o /app/publish /p:UseAppHost=false --no-restore

FROM mcr.microsoft.com/dotnet/aspnet:8.0 AS final
WORKDIR /app
EXPOSE 8089
ENV ASPNETCORE_URLS=http://+:8089
COPY --from=build /app/publish .
ENTRYPOINT ["dotnet", "AiAvailableService.dll"]
