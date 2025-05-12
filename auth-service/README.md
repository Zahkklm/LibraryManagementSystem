### What happens for `/api/auth/login` and `/api/auth/register`?

#### **Login (`POST /api/auth/login`)**
1. Client sends email and password to `/api/auth/login`.
2. `AuthController`:
    - Calls `userServiceClient.validateCredentials(request)` to check credentials in user-service.
    - If valid, prepares a request for Keycloak's token endpoint and gets a JWT.
    - Returns `JwtResponse` with the token to the client.
    - If invalid, returns `401 Unauthorized`.

#### **Register (`POST /api/auth/register`)**
1. Client sends registration data to `/api/auth/register`.
2. `AuthController`:
    - Calls `userServiceClient.createUser(request)` to create the user in user-service (which also creates the user in Keycloak and its own DB).
    - If successful, auto-logs in the user by calling the `login` method with the new credentials.
    - Returns a response with:
        - `"user"`: the created user's info (`UserDTO`)
        - `"token"`: the JWT for immediate authentication
    - If the email already exists, returns `409 Conflict`.
    - If any other error occurs, returns `500 Internal Server Error`.

---

**Summary Table**

| Endpoint              | Step 1 (Validation/Creation)         | Step 2 (Token)         | Response                                 |
|-----------------------|--------------------------------------|------------------------|------------------------------------------|
| `/api/auth/login`     | Validate credentials in user-service | Get JWT from Keycloak  | JWT token (if valid)                     |
| `/api/auth/register`  | Create user in user-service/Keycloak | Auto-login, get JWT    | User info + JWT token (if successful)    |

- All authentication is handled by Keycloak.
- user-service manages application-specific user data.
- auth-service is the single entry point for login and registration.