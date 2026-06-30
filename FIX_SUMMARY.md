# ✅ Fixed: Workspace Initialize Endpoint

## 🐛 Issues Fixed:

### 1. **API Gateway AuthenticationFilter Error Handling**
   - **Problem**: When identity-service introspection failed, the filter returned `Mono.empty()` which caused incomplete error responses
   - **Fix**: Now properly returns `unauthenticatedResponse()` on any error
   - **File**: `api-gateway/src/main/java/com/nqt/api_gateway/security/AuthenticationFilter.java`

### 2. **WorkspaceController RequestMapping**
   - **Problem**: Endpoint was at `/api/v1/workspaces` but the gateway's RewritePath removes the service name, so requests arrived at `/v1/workspaces` (causing 404)
   - **Fix**: Changed @RequestMapping from `/api/v1/workspaces` → `/v1/workspaces`
   - **File**: `workspace-service/src/main/java/com/nqt/workspace_service/controllers/WorkspaceController.java`

### 3. **Null Check for User Claims**
   - **Problem**: If token had no role claim, null value could break signature calculation
   - **Fix**: Added validation to ensure userId and role are not null before processing
   - **File**: `api-gateway/src/main/java/com/nqt/api_gateway/security/AuthenticationFilter.java`

## 📝 Correct Usage:

### URL: `/api/workspace/v1/workspaces/initialize`
**INCORRECT**: `/api/workspace/api/v1/workspaces/initialize` (double "api")

### Request Headers Required:
```
Authorization: Bearer <your-valid-jwt-token>
Content-Type: application/json
```

### Request Body:
```json
{
  "jobId": "3fa85f64-5717-4562-b3fc-2c963f66afa6",
  "escrowId": "3fa85f64-5717-4562-b3fc-2c963f66afa6",
  "clientId": "95323196-607c-4efe-8720-2789a8c74033",
  "expertId": "53447fde-63a4-4317-9acb-21b8d37e8633",
  "projectName": "Capstone Project: MusicSync",
  "deadline": "2026-06-30T23:59:59.000Z"
}
```

### Full cURL Command:
```bash
curl -X POST \
  'http://localhost:8080/api/workspace/v1/workspaces/initialize' \
  -H 'Authorization: Bearer YOUR_JWT_TOKEN_HERE' \
  -H 'Content-Type: application/json' \
  -d '{
    "jobId": "3fa85f64-5717-4562-b3fc-2c963f66afa6",
    "escrowId": "3fa85f64-5717-4562-b3fc-2c963f66afa6",
    "clientId": "95323196-607c-4efe-8720-2789a8c74033",
    "expertId": "53447fde-63a4-4317-9acb-21b8d37e8633",
    "projectName": "Capstone Project: MusicSync",
    "deadline": "2026-06-30T23:59:59.000Z"
  }'
```

## 🔄 Request Flow (Now Fixed):

1. ✅ Client sends request to `/api/workspace/v1/workspaces/initialize` with Bearer token
2. ✅ API Gateway's InternalApiBlockFilter allows the request (no "/internal/" in path)
3. ✅ API Gateway's AuthenticationFilter:
   - Extracts Bearer token from Authorization header
   - Calls identity-service's `/auth/introspect` to validate token
   - Extracts userId and role from JWT
   - Generates HMAC-SHA256 signature
   - Adds headers: X-User-Id, X-User-Role, X-Timestamp, X-Request-Id, X-Signature
   - Forwards request with new headers
4. ✅ API Gateway's RewritePath filter rewrites path: `/api/workspace/v1/workspaces/initialize` → `/v1/workspaces/initialize`
5. ✅ Request routed to workspace-service
6. ✅ Workspace service's GatewaySignatureFilter validates custom headers
7. ✅ WorkspaceController receives request at `/v1/workspaces/initialize` (FIXED!)
8. ✅ @PreAuthorize checks: hasRole('ADMIN') or hasAuthority('SCOPE_internal')
9. ✅ initializeWorkspace() method is invoked

## 📊 Authorization Requirements:

The endpoint requires:
- **For admin users**: `hasRole('ADMIN')` - User must have ADMIN role in their JWT
- **For inter-service calls**: `hasAuthority('SCOPE_internal')` - Service must send SCOPE_internal in custom headers

Current JWT generation in identity-service only supports roles (ROLE_ADMIN, etc.), not specialized scopes. For inter-service calls that need SCOPE_internal, additional implementation may be needed.

## ✅ Success Response (201 Created):
```json
{
  "code": 201,
  "message": "Workspace initialized successfully",
  "data": {
    "id": "...",
    "name": "Capstone Project: MusicSync",
    ...
  }
}
```

