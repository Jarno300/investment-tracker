# Investment Tracker

Full stack starter with Vue 3 + Spring Boot + Postgres, containerized with Docker.

## Run

```bash
docker compose up --build
```

- Frontend: `http://localhost:5173`
- Backend: `http://localhost:8080`

## Auth flow (PowerShell)

```powershell
$register = Invoke-RestMethod -Method Post -Uri http://localhost:8080/api/auth/register `
  -ContentType "application/json" `
  -Body '{"email":"demo@example.com","password":"DemoPass123"}'

$login = Invoke-RestMethod -Method Post -Uri http://localhost:8080/api/auth/login `
  -ContentType "application/json" `
  -Body '{"email":"demo@example.com","password":"DemoPass123"}'

$token = $login.accessToken

Invoke-RestMethod -Method Get -Uri http://localhost:8080/api/summary `
  -Headers @{ Authorization = "Bearer $token" }
```

## Notes

- JWT secret is configured via `JWT_SECRET` (must be at least 32 characters).
- Access token defaults to 15 minutes; refresh token defaults to 30 days.
- Stock search uses Alpha Vantage; set `STOCK_API_KEY` in `.env`.
