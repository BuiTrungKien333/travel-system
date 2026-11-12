# Graph Report - backend  (2026-11-10)

## Corpus Check
- 292 files · ~64,083 words
- Verdict: corpus is large enough that graph structure adds value.
- Unclassified: 4 file(s) not represented in the graph (top: (none) 4)

## Summary
- 292 nodes · 849 edges · 17 communities (9 shown, 8 thin omitted)
- Extraction: 79% EXTRACTED · 21% INFERRED · 0% AMBIGUOUS · INFERRED: 177 edges (avg confidence: 0.85)
- Token cost: 0 input · 0 output

## Community Hubs (Navigation)
- Auth, Security & Users
- Places, Routing & TSP
- Cloudinary Config
- I18n Locale Config
- JPA Auditing Config
- OpenAPI Swagger Config
- Connection Check Filter
- Draft Execution Config
- Spring Boot Application
- Media, Memory & Exceptions
- Itinerary & Trip Optimization
- AI RAG Retrieval & Intent
- AI Copilot & Planning Rules
- Trips & Home Dashboard
- Weather Forecast & Rules
- Place Discovery & Destinations
- Jackson App Config

## God Nodes (most connected - your core abstractions)
1. `AppException` - 37 edges
2. `ErrorCode` - 33 edges
3. `TripOptimizationServiceImpl` - 31 edges
4. `ItineraryServiceImpl` - 27 edges
5. `ApiResponse` - 24 edges
6. `Place` - 21 edges
7. `DraftService` - 21 edges
8. `GoogleAuthService` - 20 edges
9. `ScheduleItemContext` - 20 edges
10. `AuthServiceImpl` - 19 edges

## Surprising Connections (you probably didn't know these)
- `GoogleAuthService` --calls--> `GoogleChallengeRepository`  [INFERRED]
  src/main/java/com/travel/auth/service/GoogleAuthService.java → src/main/java/com/travel/auth/repository/GoogleChallengeRepository.java
- `GoogleAuthService` --calls--> `GoogleLinkTicketRepository`  [INFERRED]
  src/main/java/com/travel/auth/service/GoogleAuthService.java → src/main/java/com/travel/auth/repository/GoogleLinkTicketRepository.java
- `EmailActionService` --calls--> `BaseEntity`  [INFERRED]
  src/main/java/com/travel/auth/service/EmailActionService.java → src/main/java/com/travel/common/persistence/BaseEntity.java
- `GoogleAuthService` --calls--> `BaseEntity`  [INFERRED]
  src/main/java/com/travel/auth/service/GoogleAuthService.java → src/main/java/com/travel/common/persistence/BaseEntity.java
- `AuthServiceImpl` --calls--> `BaseEntity`  [INFERRED]
  src/main/java/com/travel/auth/service/impl/AuthServiceImpl.java → src/main/java/com/travel/common/persistence/BaseEntity.java

## Import Cycles
- None detected.

## Communities (17 total, 8 thin omitted)

### Community 0 - "Auth, Security & Users"
Cohesion: 0.08
Nodes (55): AuthEmailConfiguration, AuthEmailProperties, GoogleAuthProperties, GoogleJwtConfiguration, AuthController, EmailActionController, GoogleAuthController, ActionTokenRequest (+47 more)

### Community 1 - "Places, Routing & TSP"
Cohesion: 0.14
Nodes (46): Place, PlaceOpeningStatus, PlaceType, DaySchedule, WeeklyOpeningHours, DistanceCalculator, DistanceMatrixService, HaversineDistanceMatrixService (+38 more)

### Community 2 - "Media, Memory & Exceptions"
Cohesion: 0.12
Nodes (40): AppException, ErrorCode, GlobalExceptionHandler, BaseEntity, ErrorResponse, Translator, MediaController, BatchUploadResponse (+32 more)

### Community 3 - "Itinerary & Trip Optimization"
Cohesion: 0.15
Nodes (36): ItineraryController, DaySettings, ApplyItineraryOptimizationRequest, ItineraryDayRequest, ItineraryStopRequest, SaveItineraryDayRequest, SaveItineraryRequest, UpdateDayScheduleRequest (+28 more)

### Community 4 - "AI RAG Retrieval & Intent"
Cohesion: 0.20
Nodes (31): TravelIntentController, IntentAnalysisResponse, IntentSearchResponse, TravelIntent, GeminiIntentClient, TravelIntentService, GeminiStructuredClient, AiCallFailure (+23 more)

### Community 5 - "AI Copilot & Planning Rules"
Cohesion: 0.17
Nodes (27): AiConfigProperties, AiContextBuilder, AiItineraryPromptContext, PlaceContext, RouteLegContext, UserPreferencesContext, AiCopilotContextBuilder, CopilotActionType (+19 more)

### Community 6 - "Trips & Home Dashboard"
Cohesion: 0.22
Nodes (18): HomeController, HomeDashboard, HomeRepository, HomeService, TripController, CompleteTripRequest, CreateTripRequest, AddTripPlaceRequest (+10 more)

### Community 7 - "Weather Forecast & Rules"
Cohesion: 0.33
Nodes (18): WeatherConfigProperties, TripWeatherController, WeatherController, WeatherCondition, WeatherRiskLevel, CurrentWeather, DailyWeatherSummary, HourlyWeather (+10 more)

### Community 8 - "Place Discovery & Destinations"
Cohesion: 0.23
Nodes (13): ApiResponse, HealthController, DestinationController, MapboxAddressController, PlaceDiscoveryController, DestinationPage, PlaceDiscovery, Province (+5 more)

## Knowledge Gaps
- **23 isolated node(s):** `AuthEmailConfiguration`, `GoogleJwtConfiguration`, `EmailRequest`, `GoogleChallenge`, `GoogleLinkTicket` (+18 more)
  These have ≤1 connection - possible missing edges. (Counts symbols only; 23 node(s) total have ≤1 connection when file, concept and rationale nodes are included.)
- **8 thin communities (<3 nodes) omitted from report** — run `graphify query` to explore isolated nodes.

## Suggested Questions
_Questions this graph is uniquely positioned to answer:_

- **Why does `AppException` connect `Media, Memory & Exceptions` to `Auth, Security & Users`, `Places, Routing & TSP`, `Itinerary & Trip Optimization`, `AI RAG Retrieval & Intent`, `AI Copilot & Planning Rules`, `Trips & Home Dashboard`, `Weather Forecast & Rules`, `Place Discovery & Destinations`?**
  _High betweenness centrality (0.319) - this node is a cross-community bridge._
- **Are the 9 inferred relationships involving `TripOptimizationServiceImpl` (e.g. with `PlaceRepository` and `AiProvider`) actually correct?**
  _`TripOptimizationServiceImpl` has 9 INFERRED edges - model-reasoned connections that need verification._
- **What connects `AuthEmailConfiguration`, `GoogleJwtConfiguration`, `EmailRequest` to the rest of the system?**
  _23 weakly-connected nodes found - possible missing edges._
- **Should `Auth, Security & Users` be split into smaller, more focused modules?**
  _Cohesion score 0.08013468013468013 - nodes in this community are weakly interconnected._
- **Why does `ErrorCode` connect `Media, Memory & Exceptions` to `Auth, Security & Users`, `Places, Routing & TSP`, `Itinerary & Trip Optimization`, `AI RAG Retrieval & Intent`, `AI Copilot & Planning Rules`, `Trips & Home Dashboard`, `Weather Forecast & Rules`, `Place Discovery & Destinations`?**
  _High betweenness centrality (0.184) - this node is a cross-community bridge._
- **Are the 6 inferred relationships involving `ItineraryServiceImpl` (e.g. with `ItineraryResponse` and `Itinerary`) actually correct?**
  _`ItineraryServiceImpl` has 6 INFERRED edges - model-reasoned connections that need verification._
- **Should `Places, Routing & TSP` be split into smaller, more focused modules?**
  _Cohesion score 0.13526570048309178 - nodes in this community are weakly interconnected._