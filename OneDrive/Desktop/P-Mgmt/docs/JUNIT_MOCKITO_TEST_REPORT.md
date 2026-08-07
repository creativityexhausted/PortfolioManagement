# JUnit and Mockito Test Report

Generated: 2026-08-06 00:15:33 +05:30

## Scope
- Test framework: JUnit 5 via Spring Boot starter test
- Mocking framework: Mockito
- Build tool: Maven Surefire with JaCoCo coverage reporting

## Summary
- Total tests: 11
- Failures: 0
- Errors: 0
- Skipped: 0
- Total runtime: 24.86 seconds
- Instruction coverage: 16.15%
- Branch coverage: 4.09%

## Test Suites
| Suite | Tests | Failures | Errors | Skipped | Time (s) |
| --- | ---: | ---: | ---: | ---: | ---: |
| com.example.portfoliomanager.chatbot.PortfolioAssistantServiceTest | 2 | 0 | 0 | 0 | 4.155 |
| com.example.portfoliomanager.PortfolioApiIntegrationTest | 1 | 0 | 0 | 0 | 19.858 |
| com.example.portfoliomanager.service.AuthServiceTest | 4 | 0 | 0 | 0 | 0.574 |
| com.example.portfoliomanager.service.PortfolioServiceTest | 4 | 0 | 0 | 0 | 0.273 |

## Mockito-Covered Unit Tests
- com.example.portfoliomanager.chatbot.PortfolioAssistantServiceTest: 2 tests
- com.example.portfoliomanager.service.AuthServiceTest: 4 tests
- com.example.portfoliomanager.service.PortfolioServiceTest: 4 tests

## Integration Tests
- com.example.portfoliomanager.PortfolioApiIntegrationTest: 1 tests

## Line Coverage By Class
| Class | Covered | Missed | Line Coverage |
| --- | ---: | ---: | ---: |
| com.example.portfoliomanager.chatbot.GroqApiChatClient$GroqChatRequest | 0 | 1 | 0% |
| com.example.portfoliomanager.chatbot.GroqApiChatClient$GroqChatResponse | 0 | 1 | 0% |
| com.example.portfoliomanager.chatbot.GroqApiChatClient$GroqChoice | 0 | 1 | 0% |
| com.example.portfoliomanager.chatbot.GroqApiChatClient$GroqMessage | 0 | 1 | 0% |
| com.example.portfoliomanager.chatbot.GroqChatClient | 0 | 1 | 0% |
| com.example.portfoliomanager.chatbot.NewsAiEnrichmentService$1 | 0 | 1 | 0% |
| com.example.portfoliomanager.chatbot.NewsAiEnrichmentService$EnrichmentResult | 0 | 1 | 0% |
| com.example.portfoliomanager.chatbot.PortfolioAssistantContextService$HoldingSnapshot | 0 | 1 | 0% |
| com.example.portfoliomanager.dto.ApiDtos$AssetSearchResult | 0 | 1 | 0% |
| com.example.portfoliomanager.dto.ApiDtos$CandlePoint | 0 | 1 | 0% |
| com.example.portfoliomanager.dto.ApiDtos$CandleResponse | 0 | 1 | 0% |
| com.example.portfoliomanager.dto.ApiDtos$HoldingRequest | 0 | 1 | 0% |
| com.example.portfoliomanager.dto.ApiDtos$HoldingResponse | 0 | 1 | 0% |
| com.example.portfoliomanager.dto.ApiDtos$NewsArticle | 0 | 1 | 0% |
| com.example.portfoliomanager.dto.ApiDtos$NewsPortfolioBrief | 0 | 1 | 0% |
| com.example.portfoliomanager.dto.ApiDtos$StockPriceResponse | 0 | 1 | 0% |
| com.example.portfoliomanager.dto.ApiDtos$TransactionRequest | 0 | 1 | 0% |
| com.example.portfoliomanager.dto.ApiDtos$TransactionResponse | 0 | 1 | 0% |
| com.example.portfoliomanager.dto.ApiDtos$WatchlistRequest | 0 | 1 | 0% |
| com.example.portfoliomanager.dto.ApiDtos$WatchlistResponse | 0 | 1 | 0% |
| com.example.portfoliomanager.exception.ApiError | 0 | 1 | 0% |
| com.example.portfoliomanager.exception.ExternalApiException | 0 | 2 | 0% |
| com.example.portfoliomanager.service.AlphaVantageAssetService$SymbolMatch | 0 | 1 | 0% |
| com.example.portfoliomanager.service.AlphaVantageAssetService$SymbolSearchResponse | 0 | 1 | 0% |
| com.example.portfoliomanager.service.AlphaVantageErrorUtils | 0 | 13 | 0% |
| com.example.portfoliomanager.service.NewsService$ArticlePayload | 0 | 1 | 0% |
| com.example.portfoliomanager.service.NewsService$NewsApiResponse | 0 | 1 | 0% |
| com.example.portfoliomanager.service.PriceResolutionService$PriceLookupResult | 0 | 2 | 0% |
| com.example.portfoliomanager.service.YahooFinanceService | 6 | 157 | 3.68% |
| com.example.portfoliomanager.service.FinnhubStockService | 5 | 118 | 4.07% |
| com.example.portfoliomanager.service.HoldingService | 7 | 133 | 5% |
| com.example.portfoliomanager.service.AlphaVantageAssetService | 6 | 111 | 5.13% |
| com.example.portfoliomanager.chatbot.PortfolioAssistantContextService | 6 | 110 | 5.17% |
| com.example.portfoliomanager.domain.Holding | 1 | 17 | 5.56% |
| com.example.portfoliomanager.domain.Watchlist | 1 | 16 | 5.88% |
| com.example.portfoliomanager.domain.Transaction | 1 | 15 | 6.25% |
| com.example.portfoliomanager.chatbot.NewsAiEnrichmentService | 6 | 73 | 7.59% |
| com.example.portfoliomanager.exception.GlobalExceptionHandler | 1 | 10 | 9.09% |
| com.example.portfoliomanager.service.GNewsRssService | 6 | 60 | 9.09% |
| com.example.portfoliomanager.service.PriceResolutionService | 6 | 46 | 11.54% |
| com.example.portfoliomanager.service.TransactionService | 4 | 30 | 11.76% |
| com.example.portfoliomanager.service.WatchlistService | 4 | 27 | 12.9% |
| com.example.portfoliomanager.controller.MarketDataController | 9 | 54 | 14.29% |
| com.example.portfoliomanager.service.NewsService | 11 | 53 | 17.19% |
| com.example.portfoliomanager.service.MarketDataScheduler | 7 | 31 | 18.42% |
| com.example.portfoliomanager.chatbot.GroqApiChatClient | 6 | 23 | 20.69% |
| com.example.portfoliomanager.chatbot.GroqProperties$Groq | 6 | 14 | 30% |
| com.example.portfoliomanager.controller.HoldingController | 3 | 6 | 33.33% |
| com.example.portfoliomanager.controller.TransactionController | 3 | 6 | 33.33% |
| com.example.portfoliomanager.controller.WatchlistController | 3 | 6 | 33.33% |
| com.example.portfoliomanager.PortfolioManagerApplication | 1 | 2 | 33.33% |
| com.example.portfoliomanager.service.PortfolioAssistantFacade | 3 | 3 | 50% |
| com.example.portfoliomanager.controller.PortfolioController | 5 | 4 | 55.56% |
| com.example.portfoliomanager.chatbot.GroqProperties | 4 | 3 | 57.14% |
| com.example.portfoliomanager.controller.PortfolioAssistantController | 3 | 1 | 75% |
| com.example.portfoliomanager.domain.Portfolio | 16 | 5 | 76.19% |
| com.example.portfoliomanager.domain.AppUser | 7 | 2 | 77.78% |
| com.example.portfoliomanager.controller.AuthController | 4 | 1 | 80% |
| com.example.portfoliomanager.security.JwtAuthenticationFilter | 20 | 2 | 90.91% |
| com.example.portfoliomanager.service.PortfolioService | 30 | 3 | 90.91% |
| com.example.portfoliomanager.chatbot.PortfolioAssistantService | 24 | 1 | 96% |
| com.example.portfoliomanager.chatbot.PortfolioAssistantContextService$PortfolioAssistantContext | 1 | 0 | 100% |
| com.example.portfoliomanager.config.OpenApiConfig | 13 | 0 | 100% |
| com.example.portfoliomanager.config.SecurityConfig | 15 | 0 | 100% |
| com.example.portfoliomanager.domain.TransactionType | 4 | 0 | 100% |
| com.example.portfoliomanager.dto.ApiDtos$AuthResponse | 1 | 0 | 100% |
| com.example.portfoliomanager.dto.ApiDtos$ChatAssistantRequest | 1 | 0 | 100% |
| com.example.portfoliomanager.dto.ApiDtos$ChatAssistantResponse | 1 | 0 | 100% |
| com.example.portfoliomanager.dto.ApiDtos$LoginRequest | 1 | 0 | 100% |
| com.example.portfoliomanager.dto.ApiDtos$PortfolioRequest | 1 | 0 | 100% |
| com.example.portfoliomanager.dto.ApiDtos$PortfolioResponse | 1 | 0 | 100% |
| com.example.portfoliomanager.dto.ApiDtos$RegisterRequest | 1 | 0 | 100% |
| com.example.portfoliomanager.exception.ResourceNotFoundException | 2 | 0 | 100% |
| com.example.portfoliomanager.security.JwtService | 21 | 0 | 100% |
| com.example.portfoliomanager.service.AppUserDetailsService | 9 | 0 | 100% |
| com.example.portfoliomanager.service.AuthService | 25 | 0 | 100% |

## Notes
- AuthServiceTest and PortfolioServiceTest exercise business logic with Mockito-backed collaborators.
- PortfolioApiIntegrationTest verifies token issuance plus authenticated portfolio CRUD through Spring MockMvc.
- Raw XML and coverage artifacts remain available under target/surefire-reports and target/site/jacoco for CI use.
