# Các lead cần kiểm chứng

Tất cả record dưới đây là **needs_validation**, đã qua hai reviewer source độc lập. Không có severity hoặc kết quả khai thác đã quan sát. Audit chưa chạy target tests, provider requests, browser/native app hoặc load test. Run full skill vẫn incomplete do CLI validator không hỗ trợ input guard bắt buộc trên Windows; kiểm tra schema/semantics bằng exported APIs đã qua.

Thứ tự là ưu tiên xử lý kỹ thuật, không phải severity. Token/key thật được bỏ khỏi mọi báo cáo.

## Common configuration contains a literal Gemini credential fallback

Fingerprint: `gemini/common-config-credential-fallback`. Verdict: **needs_validation**.

Tracked common configuration exposes a nonempty literal through GEMINI_API_KEY's fallback to source readers and potentially packaged-resource readers. Configuration binding and both Gemini methods route this value into provider requests when the fallback is selected. Unauthorized provider authority remains conditional on the literal being usable and readers lacking authorization; neither fact is established. Environment overrides and the test profile's mock provider can prevent its runtime use but do not remove the source copy.

Root cause cần kiểm chứng: A literal is distributed in common application resources as the provider credential fallback instead of requiring an operator-managed value.

### Source trace

- **entrypoint — application.ai.gemini.api-key**: [backend/src/main/resources/application.yaml:68](E:/travel-system/backend/src/main/resources/application.yaml:68). Source readers can obtain the nonempty literal GEMINI_API_KEY fallback. Its value is omitted.
- **propagation — AiConfigProperties**: [backend/src/main/java/com/travel/planning/ai/config/AiConfigProperties.java:9](E:/travel-system/backend/src/main/java/com/travel/planning/ai/config/AiConfigProperties.java:9). Configuration binding maps application.ai to the Gemini apiKey property.
- **propagation — getProvider**: [backend/src/main/java/com/travel/planning/ai/provider/AiProviderFactory.java:44](E:/travel-system/backend/src/main/java/com/travel/planning/ai/provider/AiProviderFactory.java:44). The selected provider is retained when isAvailable accepts its configured key.
- **sink — generateItinerary**: [backend/src/main/java/com/travel/planning/ai/provider/GeminiAiProvider.java:131](E:/travel-system/backend/src/main/java/com/travel/planning/ai/provider/GeminiAiProvider.java:131). The configured value becomes the request query credential; analyzeAndRecommend repeats this at line 202. Provider acceptance was not observed.

### Bằng chứng và lớp bảo vệ

- [backend/src/main/resources/application.yaml:66](E:/travel-system/backend/src/main/resources/application.yaml:66): Gemini is the default selected provider, subject to an AI_PROVIDER override.
- [backend/src/main/resources/application-prod.yaml:1](E:/travel-system/backend/src/main/resources/application-prod.yaml:1): The production profile contains no replacement Gemini credential property; effective deployment overrides remain unknown.
- [backend/src/main/resources/application-test.yaml:32](E:/travel-system/backend/src/main/resources/application-test.yaml:32): The test profile selects mock, containing runtime use of Gemini under that profile.
- [backend/src/main/java/com/travel/planning/ai/provider/GeminiAiProvider.java:265](E:/travel-system/backend/src/main/java/com/travel/planning/ai/provider/GeminiAiProvider.java:265): Availability checks only nonnull and nonblank; both provider methods also fall back for missing or blank keys.
- [backend/src/main/java/com/travel/planning/ai/provider/GeminiAiProvider.java:202](E:/travel-system/backend/src/main/java/com/travel/planning/ai/provider/GeminiAiProvider.java:202): The copilot method independently places the configured value in its request query.
- [backend/src/main/resources/application.yaml:49](E:/travel-system/backend/src/main/resources/application.yaml:49): As calibration, common JWT signing properties require supplied values without literal fallbacks.

### Blockers

- Provider-owner records are needed to establish whether the literal is a real, currently usable credential, its owner, restrictions and effective resource authority.
- Repository and distributed-artifact access policies are unavailable; no actual reader unauthorized for that provider authority has been identified, and distributed inclusion has not been observed.
- Review is source only. No approved OS-enforced execution sandbox or authorization for provider traffic or real-credential testing is available.

### Kế hoạch tối thiểu an toàn

**Owner observation:** The provider owner should privately match the literal against existing credential-management records, inspect status, API/application restrictions and associated authority without using or sharing it, and compare authorized provider principals with repository and actual packaged-resource readers. Inspect release contents and effective deployment overrides through existing owner records. If usable authority was disclosed to unauthorized readers, remove the fallback and revoke or rotate the credential through normal owner procedures. Reject the authority-disclosure claim if the value is demonstrably dummy, unusable, or confined to authorized readers.

## Anonymous forecast ranges select uncapped in-memory weather generation

Fingerprint: `weather/MockWeatherProvider/unbounded-forecast-range`. Verdict: **needs_validation**.

Source confirms that an anonymous forecast query can select a date interval without an application horizon limit. Mock generation accumulates one summary and 24 hourly forecasts per inclusive day, including when OpenMeteo falls back after an exception or missing hourly data. Default includeHourly=false removes details only after generation. Date parsing, coordinate validation, reversed-range normalization and external request timeouts do not bound this allocation path. Shared service interruption and recovery remain unobserved.

Root cause cần kiểm chứng: The controller and service forward the selected interval without a maximum horizon. MockWeatherProvider synchronously generates and retains the whole interval; OpenMeteo fallback preserves the normalized interval.

### Source trace

- **entrypoint — Public forecast**: [backend/src/main/java/com/travel/weather/controller/WeatherController.java:56](E:/travel-system/backend/src/main/java/com/travel/weather/controller/WeatherController.java:56). GET /api/v1/public/weather/forecast accepts ISO LocalDate parameters without a maximum interval.
- **propagation — getWeatherForecast**: [backend/src/main/java/com/travel/weather/controller/WeatherController.java:87](E:/travel-system/backend/src/main/java/com/travel/weather/controller/WeatherController.java:87). The selected range reaches the service before includeHourly response reduction.
- **propagation — getForecast**: [backend/src/main/java/com/travel/weather/service/WeatherServiceImpl.java:25](E:/travel-system/backend/src/main/java/com/travel/weather/service/WeatherServiceImpl.java:25). The configured provider receives the dates unchanged.
- **propagation — Exception fallback**: [backend/src/main/java/com/travel/weather/provider/OpenMeteoWeatherProvider.java:94](E:/travel-system/backend/src/main/java/com/travel/weather/provider/OpenMeteoWeatherProvider.java:94). Caught provider or mapping exceptions invoke mock generation at line 96 with the normalized original interval.
- **sink — Daily generation**: [backend/src/main/java/com/travel/weather/provider/MockWeatherProvider.java:35](E:/travel-system/backend/src/main/java/com/travel/weather/provider/MockWeatherProvider.java:35). The inclusive loop generates 24 hourly objects per day and accumulates daily summaries without a horizon or cancellation check.

### Bằng chứng và lớp bảo vệ

- [backend/src/main/java/com/travel/security/config/SecurityConfig.java:47](E:/travel-system/backend/src/main/java/com/travel/security/config/SecurityConfig.java:47): The public weather route matches /api/v1/public/**, permitted anonymously at line 68.
- [backend/src/main/java/com/travel/weather/provider/WeatherProviderFactory.java:38](E:/travel-system/backend/src/main/java/com/travel/weather/provider/WeatherProviderFactory.java:38): Configuration selects the provider; missing or unavailable selections fall back to mock.
- [backend/src/main/java/com/travel/weather/provider/OpenMeteoWeatherProvider.java:40](E:/travel-system/backend/src/main/java/com/travel/weather/provider/OpenMeteoWeatherProvider.java:40): Connect/read timeouts at lines 42–43 constrain external exchange, without imposing a fallback generation deadline.
- [backend/src/main/java/com/travel/weather/provider/OpenMeteoWeatherProvider.java:87](E:/travel-system/backend/src/main/java/com/travel/weather/provider/OpenMeteoWeatherProvider.java:87): Null response, hourly data or hourly timestamps independently trigger mock generation with the same normalized interval.
- [backend/src/main/java/com/travel/weather/provider/MockWeatherProvider.java:48](E:/travel-system/backend/src/main/java/com/travel/weather/provider/MockWeatherProvider.java:48): Each accumulated daily summary retains its hourly list; reversed dates are normalized to one day at lines 28–29.
- [backend/src/main/java/com/travel/weather/controller/WeatherController.java:88](E:/travel-system/backend/src/main/java/com/travel/weather/controller/WeatherController.java:88): Hourly detail removal happens after the provider returns the fully generated forecast.
- [backend/src/main/java/com/travel/weather/model/WeatherForecastResult.java:25](E:/travel-system/backend/src/main/java/com/travel/weather/model/WeatherForecastResult.java:25): Response reduction creates summaries without hourly lists; it does not prevent original allocation.

### Blockers

- No approved OS-enforced sandbox supplies disabled external network, an empty allowlisted environment, read-only target/toolchain, scratch-only writes and explicit resource/time limits; no target fixture was executed.
- Effective ingress horizon enforcement, shared JVM heap and worker isolation, cancellation behavior and recovery are unobserved; source allocation growth does not establish meaningful shared service interruption.
- Deployed provider selection and fallback conditions are unobserved.

### Kế hoạch tối thiểu an toàn

**Local:** In the required offline sandbox with explicit low heap, CPU and wall-clock limits, exercise dummy controller/service/mock fixtures for one, two and three inclusive days and verify 1/2/3 summaries with 24/48/72 hourly objects. Use a stub RestClient with no network to return missing hourly data and throw a provider exception; verify both preserve the small interval in fallback. Verify includeHourly=false reduces only the returned details. For remediation, assert an over-limit horizon is rejected before provider invocation. Never allocate a large interval or stress the process.

**Owner observation:** The owner should inspect effective ingress date-span enforcement, active provider configuration, JVM/request-worker sharing, request cancellation and supervisor recovery settings. Establish whether uncapped generation can affect other requests through configuration and existing operational evidence, without sending large requests or inducing an outage.

## Anonymous planning batches lack an aggregate downstream work budget

Fingerprint: `planning/public-batches/unbounded-downstream-fanout`. Verdict: **needs_validation**.

Source permits nonempty rules/apply collections without a cardinality maximum and synchronously requests one forecast per qualifying OUTDOOR/HYBRID item. Multiday loops over uncapped days, running the daily pipeline and potentially daily plus aggregate AI analysis. Each Held-Karp invocation correctly rejects more than 16 places before matrix/DP allocation. These source paths establish downstream fanout, but runtime acceptance, meaningful shared worker impact and provider cost/quota impact remain unobserved.

Root cause cần kiểm chứng: Submitted item/day collections have no source-visible aggregate cardinality or downstream call budget. Solver limits, individual HTTP timeouts, AI output-token limits and fallback handling constrain individual operations without bounding the whole request.

### Source trace

- **entrypoint — Public rules alias**: [backend/src/main/java/com/travel/planning/rule/controller/RuleEvaluationController.java:19](E:/travel-system/backend/src/main/java/com/travel/planning/rule/controller/RuleEvaluationController.java:19). Public route accepts a validated JSON rules request.
- **propagation — items validation**: [backend/src/main/java/com/travel/planning/rule/dto/EvaluateItineraryRequest.java:12](E:/travel-system/backend/src/main/java/com/travel/planning/rule/dto/EvaluateItineraryRequest.java:12). Items require nonempty input and cascade validation, with no collection maximum.
- **propagation — evaluateItinerary**: [backend/src/main/java/com/travel/planning/rule/controller/RuleEvaluationController.java:36](E:/travel-system/backend/src/main/java/com/travel/planning/rule/controller/RuleEvaluationController.java:36). The complete submitted collection reaches the rule service.
- **propagation — Checker dispatch**: [backend/src/main/java/com/travel/planning/rule/service/RuleEvaluatorService.java:33](E:/travel-system/backend/src/main/java/com/travel/planning/rule/service/RuleEvaluatorService.java:33). Each checker receives the complete collection; exceptions are caught per checker.
- **propagation — Per-item forecast**: [backend/src/main/java/com/travel/planning/rule/checker/OutdoorWeatherRuleChecker.java:45](E:/travel-system/backend/src/main/java/com/travel/planning/rule/checker/OutdoorWeatherRuleChecker.java:45). Each OUTDOOR/HYBRID item with an arrival time independently calls getForecastAt, without deduplication.
- **propagation — Forecast dispatch**: [backend/src/main/java/com/travel/weather/service/WeatherServiceImpl.java:36](E:/travel-system/backend/src/main/java/com/travel/weather/service/WeatherServiceImpl.java:36). Each call requests a forecast for the item's date; getForecast delegates directly to the selected provider.
- **sink — Synchronous provider HTTP**: [backend/src/main/java/com/travel/weather/provider/OpenMeteoWeatherProvider.java:81](E:/travel-system/backend/src/main/java/com/travel/weather/provider/OpenMeteoWeatherProvider.java:81). When Open-Meteo is selected, each forecast executes an HTTP request and falls back to mock data on failure.

### Bằng chứng và lớp bảo vệ

- [backend/src/main/java/com/travel/security/config/SecurityConfig.java:68](E:/travel-system/backend/src/main/java/com/travel/security/config/SecurityConfig.java:68): The whitelist includes /api/v1/public/** and permits anonymous access.
- [backend/src/main/java/com/travel/weather/provider/OpenMeteoWeatherProvider.java:40](E:/travel-system/backend/src/main/java/com/travel/weather/provider/OpenMeteoWeatherProvider.java:40): HTTP uses a five-second connect timeout and configured read timeout, without a request-wide batch deadline.
- [backend/src/main/java/com/travel/weather/provider/WeatherProviderFactory.java:33](E:/travel-system/backend/src/main/java/com/travel/weather/provider/WeatherProviderFactory.java:33): Weather provider selection depends on configuration and availability; mock fallback can remove external HTTP impact.
- [backend/src/main/java/com/travel/planning/dto/request/ApplyRecommendationRequest.java:25](E:/travel-system/backend/src/main/java/com/travel/planning/dto/request/ApplyRecommendationRequest.java:25): currentSchedule requires nonempty input but has no cardinality maximum.
- [backend/src/main/java/com/travel/planning/controller/ItineraryOptimizationController.java:57](E:/travel-system/backend/src/main/java/com/travel/planning/controller/ItineraryOptimizationController.java:57): Public itinerary alias exposes apply-recommendation with controller validation.
- [backend/src/main/java/com/travel/planning/service/TripOptimizationServiceImpl.java:366](E:/travel-system/backend/src/main/java/com/travel/planning/service/TripOptimizationServiceImpl.java:366): Apply recalculates and re-evaluates the whole schedule, reaching the per-item weather checker.
- [backend/src/main/java/com/travel/planning/dto/request/MultiDayOptimizeAndAuditRequest.java:16](E:/travel-system/backend/src/main/java/com/travel/planning/dto/request/MultiDayOptimizeAndAuditRequest.java:16): days requires nonempty input and cascade validation, without a maximum.
- [backend/src/main/java/com/travel/planning/dto/request/DayPlanRequest.java:29](E:/travel-system/backend/src/main/java/com/travel/planning/dto/request/DayPlanRequest.java:29): Each day's locations requires nonempty input and cascade validation, without a maximum.
- [backend/src/main/java/com/travel/planning/service/TripOptimizationServiceImpl.java:252](E:/travel-system/backend/src/main/java/com/travel/planning/service/TripOptimizationServiceImpl.java:252): Every submitted day constructs a single-day request and invokes its pipeline directly.
- [backend/src/main/java/com/travel/planning/dto/request/OptimizeAndAuditRequest.java:39](E:/travel-system/backend/src/main/java/com/travel/planning/dto/request/OptimizeAndAuditRequest.java:39): The single-day HTTP DTO has a 2–20 location constraint; direct construction in the multiday loop does not invoke controller validation.
- [backend/src/main/java/com/travel/planning/algorithm/HeldKarpSolver.java:92](E:/travel-system/backend/src/main/java/com/travel/planning/algorithm/HeldKarpSolver.java:92): MAX_LOCATIONS is 16; every solver invocation rejects larger input before distance-matrix and DP allocation. This does not bound days or rules/apply collections.
- [backend/src/main/java/com/travel/planning/service/TripOptimizationServiceImpl.java:197](E:/travel-system/backend/src/main/java/com/travel/planning/service/TripOptimizationServiceImpl.java:197): All-clear daily evaluation suppresses AI; the conflict path calls analyzeAndRecommend at line 228.
- [backend/src/main/java/com/travel/planning/service/TripOptimizationServiceImpl.java:319](E:/travel-system/backend/src/main/java/com/travel/planning/service/TripOptimizationServiceImpl.java:319): Multiday critical/important conflicts permit an additional aggregate AI analysis after daily processing.
- [backend/src/main/java/com/travel/planning/ai/provider/AiProviderFactory.java:44](E:/travel-system/backend/src/main/java/com/travel/planning/ai/provider/AiProviderFactory.java:44): Unavailable configured AI providers fall back to mock, making paid-provider impact configuration dependent.
- [backend/src/main/java/com/travel/planning/ai/provider/GeminiAiProvider.java:195](E:/travel-system/backend/src/main/java/com/travel/planning/ai/provider/GeminiAiProvider.java:195): Missing credentials trigger fallback; configured calls use output-token limits, individual HTTP timeouts and quota-error fallback, without an aggregate call budget.

### Blockers

- No approved isolated execution environment is available; HTTP acceptance and small counting-stub reproductions were not executed.
- Effective ingress/parser/body bounds, request-wide cancellation, worker topology/isolation and recovery behavior are unknown. Source fanout alone does not establish meaningful shared availability impact.
- Selected production providers, available Gemini credentials and operator quota/billing policies are unverified; external spend or shared quota depletion cannot be asserted.

### Kế hoạch tối thiểu an toàn

**Local:** In the required offline sandbox, exercise the native HTTP routes using counting provider stubs: rules with one, two and three identical valid OUTDOOR items and arrival times; apply with small valid OUTDOOR schedules and DISMISS; multiday with two small conflict days. Record accepted inputs, forecast counts and daily/aggregate AI counts. Use no external provider traffic, saturation or large allocations. Verify controlled cancellation and that a proposed aggregate budget rejects excess work before downstream dispatch.

**Owner observation:** Owner should inspect effective JSON/body and collection bounds, total request deadlines and cancellation propagation, shared request-worker/provider isolation, configured provider selection, quota/billing policies and recovery behavior. Establish whether the accepted bounded request can materially affect resources shared with other callers using configuration/topology evidence and existing telemetry, without load or paid-provider tests.

## A pending profile response may cross an authenticated account replacement

Fingerprint: `mobile:profile:account-replacement-stale-success`. Verdict: **needs_validation**.

In an authenticated A instance, a valid Google link for B requires explicit confirmation before installing B's session. If A's pending profile query remains observed through navigation and completes after B is seeded, it may replace the displayed name/email with A's. Source does not establish that lifecycle result or any server authorization bypass.

Root cause cần kiểm chứng: Account replacement seeds the shared profile query without cancellation, while successful protected API responses do not check the credential generation.

### Source trace

- **entrypoint — MagicLinkProvider URL subscription**: [mobile/src/features/auth/components/magic-link-provider.tsx:22](E:/travel-mobile-ui/src/features/auth/components/magic-link-provider.tsx:22). A warm external URL enters client state through the linking event.
- **propagation — GoogleLinkScreen**: [mobile/src/features/auth/screens/google-link-screen.tsx:15](E:/travel-mobile-ui/src/features/auth/screens/google-link-screen.tsx:15). The screen reads a purpose-checked, syntactically validated Google link token.
- **propagation — GoogleLinkConfirmation.confirm**: [mobile/src/features/auth/screens/google-link-screen.tsx:25](E:/travel-mobile-ui/src/features/auth/screens/google-link-screen.tsx:25). Explicit confirmation exchanges the token, then replaces the route.
- **propagation — useGoogleEmailConfirmation**: [mobile/src/features/auth/hooks/use-google-login.ts:17](E:/travel-mobile-ui/src/features/auth/hooks/use-google-login.ts:17). The link exchange authenticates through the session manager.
- **propagation — authenticateOutcome**: [mobile/src/features/auth/services/session-manager.ts:57](E:/travel-mobile-ui/src/features/auth/services/session-manager.ts:57). Authentication increments generation; acceptance installs credentials after generation checks and serialized storage.
- **propagation — onUser**: [mobile/src/features/auth/services/auth-session.ts:30](E:/travel-mobile-ui/src/features/auth/services/auth-session.ts:30). Acceptance seeds B into the existing shared profile query without explicitly cancelling its fetch.
- **propagation — useProfile**: [mobile/src/features/auth/hooks/use-auth.ts:30](E:/travel-mobile-ui/src/features/auth/hooks/use-auth.ts:30). A previously dispatched profile query consumes an AbortSignal and returns the parsed API profile.
- **propagation — Response interceptor**: [mobile/src/services/api/client.ts:43](E:/travel-mobile-ui/src/services/api/client.ts:43). Successful responses pass through without a session generation comparison.
- **sink — ProfileScreen**: [mobile/src/features/auth/screens/profile-screen.tsx:48](E:/travel-mobile-ui/src/features/auth/screens/profile-screen.tsx:48). The screen displays the shared query's fullName and, at line 50, email.

### Bằng chứng và lớp bảo vệ

- [mobile/src/app/_layout.tsx:59](E:/travel-mobile-ui/src/app/_layout.tsx:59): Google link is outside both protected route groups; authenticated status remains allowed during replacement.
- [mobile/src/features/auth/api/auth-query-keys.ts:1](E:/travel-mobile-ui/src/features/auth/api/auth-query-keys.ts:1): The profile key contains neither account identity nor session generation.
- [mobile/src/features/auth/services/session-manager.ts:46](E:/travel-mobile-ui/src/features/auth/services/session-manager.ts:46): Acceptance invokes onUser; expiration separately invokes onClear at line 130.
- [mobile/src/features/auth/api/auth-api.ts:58](E:/travel-mobile-ui/src/features/auth/api/auth-api.ts:58): Profile transport receives the query AbortSignal and validates response shape, without binding its user ID to the current session.
- [mobile/node_modules/@tanstack/query-core/src/queryClient.ts:305](E:/travel-mobile-ui/node_modules/@tanstack/query-core/src/queryClient.ts:305): Installed QueryClient setQueryData performs a manual data update, which does not itself cancel the fetch.
- [mobile/node_modules/@tanstack/query-core/src/query.ts:537](E:/travel-mobile-ui/node_modules/@tanstack/query-core/src/query.ts:537): Strong containment: removing the last observer cancels and reverts a fetch whose AbortSignal was consumed.
- [mobile/node_modules/axios/lib/core/dispatchRequest.js:56](E:/travel-mobile-ui/node_modules/axios/lib/core/dispatchRequest.js:56): Axios checks cancellation again before delivering a successful adapter response.
- [mobile/node_modules/expo-router/build/fork/useLinking.native.js:162](E:/travel-mobile-ui/node_modules/expo-router/build/fork/useLinking.native.js:162): Warm native linking dispatches a derived navigation action or resets root state; observer retention must be established for the actual route transition.

### Blockers

- Target execution is unavailable under the required OS-enforced external-network isolation, empty allowlisted environment, read-only target/toolchain access, scratch-only writes, and explicit resource limits.
- The decisive missing observation is whether the actual warm-link and replacement route lifecycle retains A's profile observer and permits its deferred success to overwrite B's cache and rendered name/email. Last-observer removal would cancel the request.

### Kế hoạch tối thiểu an toàn

**Local:** In the required offline sandbox, use the actual QueryClient and Expo Router with mock transport and two dummy accounts. Authenticate A and start a deferred /users/me response. Deliver a syntactically valid warm Google link whose mock exchange returns B, explicitly press confirmation, and allow the existing replacement and redirect sequence. Record observer removal and AbortSignal state, then release A's response within the transport timeout. Inspect current session, profile cache, and displayed name/email. Cancellation refutes this path; observing A's name/email under B validates only that display effect. Stop after this result; use no provider or deployed requests.

