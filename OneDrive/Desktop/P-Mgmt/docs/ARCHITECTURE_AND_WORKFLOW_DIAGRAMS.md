# Portfolio Manager Architecture and Workflow Diagrams

This document provides visual references for the project architecture and key workflows.

## 1. High-Level System Architecture

```mermaid
flowchart LR
    U[User Browser]
    FE[React Frontend\nVite]
    API[Spring Boot Backend\nPortfolio Manager API]
    DB[(MySQL)]
    YF[Yahoo Finance]
    AV[Alpha Vantage]
    FH[Finnhub]
    GR[Groq API]

    U --> FE
    FE -->|HTTP JSON| API
    API --> DB
    API --> YF
    API --> AV
    API --> FH
    API --> GR
```

## 2. Backend Layered Architecture

```mermaid
flowchart TD
    C[Controllers\nREST Endpoints]
    S[Services\nBusiness Logic]
    R[Repositories\nSpring Data JPA]
    D[Domain and DTOs]
    SEC[Security\nJWT Filter and Auth]

    C --> S
    S --> R
    R --> D
    SEC --> C
    SEC --> S
```

## 3. Authentication Workflow

```mermaid
sequenceDiagram
    participant Client as Frontend Client
    participant API as Auth API
    participant DB as MySQL

    Client->>API: POST /api/auth/register
    API->>DB: Save user (hashed password)
    DB-->>API: User created
    API-->>Client: 201 + JWT token

    Client->>API: POST /api/auth/login
    API->>DB: Validate username/password
    DB-->>API: User found
    API-->>Client: 200 + JWT token

    Client->>API: Secured endpoint with Bearer token
    API-->>Client: 200 if token valid
```

## 4. Portfolio CRUD Workflow

```mermaid
sequenceDiagram
    participant Client as Frontend Client
    participant Ctrl as Portfolio Controller
    participant Svc as Portfolio Service
    participant Repo as Portfolio Repository
    participant DB as MySQL

    Client->>Ctrl: POST /api/portfolios
    Ctrl->>Svc: Validate and map request
    Svc->>Repo: Save portfolio
    Repo->>DB: INSERT portfolio
    DB-->>Repo: Saved row
    Repo-->>Svc: Entity
    Svc-->>Ctrl: Response DTO
    Ctrl-->>Client: 201 Created

    Client->>Ctrl: GET /api/portfolios
    Ctrl->>Svc: Fetch list
    Svc->>Repo: Query portfolios
    Repo->>DB: SELECT
    DB-->>Repo: Rows
    Repo-->>Svc: Entities
    Svc-->>Ctrl: DTO list
    Ctrl-->>Client: 200 OK
```

## 5. Market Data and News Workflow

```mermaid
flowchart TD
    T[Scheduler or API Trigger] --> M[Market Data Service]
    M --> Q[Quote Providers\nYahoo Finance or Finnhub]
    M --> N[News Provider\nAlpha Vantage]
    Q --> X[Normalize Data]
    N --> X
    X --> P[Persist or Return Response]
```

## 6. Portfolio Assistant Workflow

```mermaid
sequenceDiagram
    participant Client as Frontend Client
    participant Chat as Chat Controller
    participant Svc as Assistant Service
    participant Port as Portfolio Data Service
    participant LLM as Groq API

    Client->>Chat: POST /api/chat/portfolio-assistant
    Chat->>Svc: message + portfolioId
    Svc->>Port: Get holdings and metrics
    Port-->>Svc: Context data
    Svc->>LLM: Prompt with context
    LLM-->>Svc: Assistant response
    Svc-->>Chat: Final answer payload
    Chat-->>Client: 200 OK
```

## 7. End-to-End User Request Workflow

```mermaid
flowchart LR
    A[User Action in UI] --> B[React Component]
    B --> C[Service Layer in frontend/src/services]
    C --> D[Backend REST Endpoint]
    D --> E[Business Logic and DB Operations]
    E --> D
    D --> C
    C --> B
    B --> F[UI Render Update]
```
