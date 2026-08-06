# Portfolio Manager API Usage Guide

This guide explains how to use the Portfolio Manager backend API in day-to-day development and testing.

## 1. Base URLs

- API base URL: http://localhost:8080
- Swagger UI: http://localhost:8080/swagger-ui/index.html
- OpenAPI JSON: http://localhost:8080/v3/api-docs

## 2. Authentication Flow

Most endpoints require a JWT bearer token.

### Step 1: Register

Request

```http
POST /api/auth/register
Content-Type: application/json

{
  "username": "demo_user",
  "password": "ChangeMe123!"
}
```

Expected response: 201 Created

### Step 2: Login

Request

```http
POST /api/auth/login
Content-Type: application/json

{
  "username": "demo_user",
  "password": "ChangeMe123!"
}
```

Expected response: 200 OK with token payload

```json
{
  "token": "<jwt-token>",
  "tokenType": "Bearer",
  "expiresInMs": 86400000
}
```

### Step 3: Use the token

Add this header for secured endpoints:

```http
Authorization: Bearer <jwt-token>
```

## 3. Core Endpoint Groups

### Auth

- POST /api/auth/register
- POST /api/auth/login

### Portfolios

- GET /api/portfolios
- POST /api/portfolios
- GET /api/portfolios/{id}
- PUT /api/portfolios/{id}
- DELETE /api/portfolios/{id}

### Holdings

- GET /api/holdings
- GET /api/holdings?portfolioId={id}
- POST /api/holdings
- GET /api/holdings/{id}
- PUT /api/holdings/{id}
- DELETE /api/holdings/{id}

### Transactions

- GET /api/transactions
- GET /api/transactions?portfolioId={id}
- POST /api/transactions
- GET /api/transactions/{id}
- PUT /api/transactions/{id}
- DELETE /api/transactions/{id}

### Watchlist

- GET /api/watchlist
- GET /api/watchlist?portfolioId={id}
- POST /api/watchlist
- GET /api/watchlist/{id}
- PUT /api/watchlist/{id}
- DELETE /api/watchlist/{id}

### Market and News

- GET /api/stocks/{symbol}
- GET /api/news
- POST /api/news/refresh

### Portfolio Assistant

- POST /api/chat/portfolio-assistant

## 4. Common Request Payloads

### Create portfolio

```json
{
  "name": "Retirement",
  "description": "Long-term investments"
}
```

### Create holding

```json
{
  "symbol": "AAPL",
  "companyName": "Apple Inc.",
  "quantity": 10,
  "averagePurchasePrice": 185.5,
  "portfolioId": 1
}
```

### Create transaction

```json
{
  "type": "BUY",
  "symbol": "AAPL",
  "quantity": 10,
  "pricePerShare": 185.5,
  "transactionDate": "2026-08-01T10:00:00",
  "notes": "Initial position",
  "portfolioId": 1
}
```

### Create watchlist entry

```json
{
  "symbol": "MSFT",
  "companyName": "Microsoft Corporation",
  "targetPrice": 400,
  "portfolioId": 1
}
```

### Ask portfolio assistant

```json
{
  "message": "Which holdings are currently down the most?",
  "portfolioId": 1
}
```

## 5. End-to-End API Usage Sequence

1. Register a user.
2. Login and store the JWT token.
3. Create a portfolio.
4. Add holdings and transactions linked to that portfolio.
5. Add watchlist entries for symbols to monitor.
6. Fetch stock quote snapshots through /api/stocks/{symbol}.
7. Fetch or refresh cached news.
8. Query the portfolio assistant for performance-oriented insights.

## 6. Example cURL Commands

### Register

```bash
curl -X POST "http://localhost:8080/api/auth/register" \
  -H "Content-Type: application/json" \
  -d "{\"username\":\"demo_user\",\"password\":\"ChangeMe123!\"}"
```

### Login

```bash
curl -X POST "http://localhost:8080/api/auth/login" \
  -H "Content-Type: application/json" \
  -d "{\"username\":\"demo_user\",\"password\":\"ChangeMe123!\"}"
```

### Create portfolio (secured)

```bash
curl -X POST "http://localhost:8080/api/portfolios" \
  -H "Authorization: Bearer <jwt-token>" \
  -H "Content-Type: application/json" \
  -d "{\"name\":\"Retirement\",\"description\":\"Long-term investments\"}"
```

## 7. Example PowerShell Commands

### Register

```powershell
Invoke-RestMethod -Method Post -Uri "http://localhost:8080/api/auth/register" `
  -ContentType "application/json" `
  -Body '{"username":"demo_user","password":"ChangeMe123!"}'
```

### Login and keep token

```powershell
$login = Invoke-RestMethod -Method Post -Uri "http://localhost:8080/api/auth/login" `
  -ContentType "application/json" `
  -Body '{"username":"demo_user","password":"ChangeMe123!"}'

$token = $login.token
```

### Call secured endpoint

```powershell
$headers = @{ Authorization = "Bearer $token" }

Invoke-RestMethod -Method Get -Uri "http://localhost:8080/api/portfolios" -Headers $headers
```

## 8. Error Handling Notes

- 400 Bad Request: Request body validation failed (for example blank required fields).
- 401 Unauthorized: Missing or invalid JWT.
- 404 Not Found: Resource id does not exist.
- 500 Internal Server Error: Server-side issue, often tied to database or external API failures.

## 9. Environment Requirements

Minimum backend requirements:

- Java 21
- Maven 3.9+
- MySQL 8+

Typical database variables used by this project:

- DB_URL
- DB_USERNAME
- DB_PASSWORD

If startup fails with access denied for MySQL user, verify DB credentials and DB user privileges before testing API endpoints.
