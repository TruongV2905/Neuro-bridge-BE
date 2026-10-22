# Build from repository root so the authoritative linked proto files are in context:
# docker build -f chat-service/Dockerfile -t chat-service .
FROM mcr.microsoft.com/dotnet/sdk:8.0 AS build
WORKDIR /src

COPY ["chat-service/ChatService.csproj", "chat-service/"]
COPY ["common-starter/src/main/proto/user.proto", "common-starter/src/main/proto/"]
COPY ["common-starter/src/main/proto/workspace_service.proto", "common-starter/src/main/proto/"]
COPY ["common-starter/src/main/proto/google/protobuf/timestamp.proto", "common-starter/src/main/proto/google/protobuf/"]
RUN dotnet restore "chat-service/ChatService.csproj"

COPY ["chat-service/", "chat-service/"]
RUN dotnet publish "chat-service/ChatService.csproj" -c Release -o /app/publish /p:UseAppHost=false --no-restore

FROM mcr.microsoft.com/dotnet/aspnet:8.0 AS final
WORKDIR /app
EXPOSE 4565
ENV ASPNETCORE_URLS=http://+:4565
COPY --from=build /app/publish .
ENTRYPOINT ["dotnet", "ChatService.dll"]
