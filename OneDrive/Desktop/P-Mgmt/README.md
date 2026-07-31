# Portfolio Manager Backend

Spring Boot 3 REST backend for portfolios, holdings, trades, watchlists, live Yahoo Finance
quotes, financial news, scheduled refreshes, and JWT authentication.

## Requirements

- Java 21
- Maven 3.9+
- MySQL 8+
- A NewsAPI.org API key (optional; required only for news refreshes)

## Configure

Edit `src/main/resources/application.properties` and replace `YOUR_DATABASE_NAME`, or set
these environment variables:

```powershell
$env:DB_NAME="portfolio_db"
$env:DB_USERNAME="root"
$env:DB_PASSWORD="your-password"
$env:NEWS_API_KEY="your-newsapi-key"
$env:JWT_SECRET="a-base64-encoded-secret-containing-at-least-32-random-bytes"
$env:GROQ_API_KEY="your-groq-api-key"
$env:GROQ_MODEL="llama-3.1-8b-instant"
```

The MySQL database itself must already exist. Hibernate creates and updates its tables.
Generate a production JWT secret, for example:

```powershell
[Convert]::ToBase64String([Security.Cryptography.RandomNumberGenerator]::GetBytes(48))
```

## Run

```powershell
mvn spring-boot:run
```

## Authenticate

Register once:

```http
POST /api/auth/register
Content-Type: application/json

{
  "username": "demo",
  "password": "change-this-password"
}
```

Or log in later at `POST /api/auth/login` with the same JSON fields. Both return a JWT.
Send it on every other request:

```http
Authorization: Bearer <token>
```

## API

| Resource | Endpoints |
| --- | --- |
| Portfolios | `GET/POST /api/portfolios`, `GET/PUT/DELETE /api/portfolios/{id}` |
| Holdings | `GET/POST /api/holdings`, `GET/PUT/DELETE /api/holdings/{id}` |
| Transactions | `GET/POST /api/transactions`, `GET/PUT/DELETE /api/transactions/{id}` |
| Watchlist | `GET/POST /api/watchlist`, `GET/PUT/DELETE /api/watchlist/{id}` |
| Live quote | `GET /api/stocks/{symbol}` |
| News cache | `GET /api/news` |
| Refresh news | `POST /api/news/refresh` |
| Portfolio assistant | `POST /api/chat/portfolio-assistant` |

Holding, transaction, and watchlist list endpoints accept an optional `portfolioId` query
parameter. Their create/update bodies include a required `portfolioId`.

Example portfolio:

```json
{
  "name": "Retirement",
  "description": "Long-term investments"
}
```

Example holding:

```json
{
  "symbol": "AAPL",
  "companyName": "Apple Inc.",
  "quantity": 10,
  "averagePurchasePrice": 185.50,
  "portfolioId": 1
}
```

Example transaction:

```json
{
  "type": "BUY",
  "symbol": "AAPL",
  "quantity": 10,
  "pricePerShare": 185.50,
  "transactionDate": "2026-07-30T10:00:00",
  "notes": "Initial position",
  "portfolioId": 1
}
```

Example watchlist entry:

```json
{
  "symbol": "MSFT",
  "companyName": "Microsoft Corporation",
  "targetPrice": 400,
  "portfolioId": 1
}
```

Prices refresh every five minutes and news every fifteen minutes by default. All intervals
and initial delays can be overridden using the environment variables documented in
`application.properties`.

## Portfolio Assistant (Groq)

The portfolio assistant is designed to:

- answer portfolio performance questions using your holdings data
- explain terms and performance in beginner-friendly language
- refuse buy/sell recommendations for specific stocks

Example request:

```http
POST /api/chat/portfolio-assistant
Authorization: Bearer <token>
Content-Type: application/json

{
  "message": "Which holdings are falling the most?",
  "portfolioId": 1
}
```

Example response:

```json
{
  "answer": "Your largest decline is in TSLA at -4.20% based on current price versus average purchase price.",
  "model": "llama-3.1-8b-instant",
  "generatedAt": "2026-07-31T10:15:00"
}
```

Chatbot configuration keys are available in `application.properties`:

- `chatbot.groq.base-url`
- `chatbot.groq.api-key`
- `chatbot.groq.model`
- `chatbot.groq.temperature`
- `chatbot.max-holdings-in-context`

## Test

```powershell
mvn test
```

Tests use an in-memory H2 database in MySQL compatibility mode and do not call external APIs.
