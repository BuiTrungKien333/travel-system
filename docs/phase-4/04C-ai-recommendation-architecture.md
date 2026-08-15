# PHASE 4C — AI RECOMMENDATION ARCHITECTURE

> **Project:** Hệ thống hỗ trợ lập kế hoạch và quản lý hành trình du lịch cá nhân tích hợp AI  
> **Scope:** MVP — cá nhân là đối tượng chính, hỗ trợ mở rộng nhóm trong tương lai  
> **Region:** Toàn bộ Việt Nam  
> **Status:** Approved / Frozen

## 4C.1 — AI RESPONSIBILITY

### AI được sử dụng cho
- Gợi ý thứ tự tham quan.
- Phân bổ địa điểm theo ngày.
- Cá nhân hóa lịch trình dựa trên sở thích và nhịp độ.
- Đưa ra đề xuất có xét đến thông tin thời tiết.
- Giải thích ngắn gọn lý do một lịch trình được đề xuất.
- Hỗ trợ tạo lại lịch trình khi người dùng không hài lòng.

### AI không chịu trách nhiệm
- Lưu trữ dữ liệu.
- Xác thực người dùng và quyền truy cập.
- Tính toán dữ liệu địa lý chính xác.
- Cung cấp dữ liệu thời tiết gốc.
- Tạo địa điểm mới.
- Xác nhận giờ mở cửa hoặc giá vé thực tế.
- Đặt vé / booking.
- Ghi trực tiếp vào Database.
- Tự động thay đổi itinerary mà không có sự xác nhận của người dùng.

### Nguyên tắc
```text
AI = Recommendation
Backend = Source of Truth
User = Final Decision Maker
```

---

## 4C.2 — AI CONTEXT

Backend phải chuẩn bị `AIContext` trước khi gọi AI.

```text
Trip
+
Selected Places
+
Route / Distance Data
+
Weather Data
+
User Preferences
+
Trip Constraints
↓
AIContext
```

### Trip Context
Có thể bao gồm:
- `tripId`
- ngày bắt đầu / kết thúc
- số ngày
- khu vực / tỉnh thành
- danh sách địa điểm người dùng chọn
- các ràng buộc liên quan

### Place Context
Chỉ truyền dữ liệu cần thiết:

```json
{
  "placeId": "p001",
  "name": "Ba Na Hills",
  "category": "nature",
  "latitude": 16.0,
  "longitude": 108.0
}
```

Không gửi toàn bộ raw response nếu không cần.

### Route Context
AI có thể nhận dữ liệu đã được Routing API xử lý:

```json
{
  "from": "p001",
  "to": "p002",
  "distanceKm": 4.2,
  "durationMinutes": 15
}
```

AI không được tự tạo travel time chính xác.

### Weather Context
Weather API là nguồn dữ liệu chính thức:

```json
{
  "date": "2026-08-15",
  "condition": "rain",
  "temperature": 28,
  "rainProbability": 70
}
```

AI không được tự tạo hoặc thay thế dữ liệu thời tiết.

### User Preferences
Có thể bao gồm:
- loại trải nghiệm yêu thích;
- pace: relaxed / balanced / active;
- ưu tiên tham quan;
- mức độ di chuyển chấp nhận được.

### Context Minimization
Chỉ gửi dữ liệu cần thiết; không gửi toàn bộ Database, raw API response hoặc dữ liệu không liên quan.

---

## 4C.3 — RECOMMENDATION LOGIC

### Input
Ví dụ:

```text
Trip: 15/08 → 17/08
Places: A, B, C, D
Preferences: Balanced
Weather: A Sunny, B Rain 70%, C Cloudy, D Sunny
```

### Output có thể là
```text
Day 1: A → C
Day 2: D → B
```

kèm lý do ngắn.

### Ưu tiên
1. Địa điểm người dùng đã chọn.
2. Tính hợp lý về di chuyển.
3. Phân bổ thời gian.
4. Sở thích.
5. Thời tiết.
6. Tính linh hoạt.

### Weather là Soft Signal
Thời tiết chỉ là **advisory signal**, không phải hard constraint. Dữ liệu API có thể khác thực tế và người dùng vẫn có thể muốn đi địa điểm đó.

---

## 4C.4 — PROMPT & STRUCTURED OUTPUT

AI phải trả về cấu trúc định trước:

```json
{
  "days": [
    {
      "date": "2026-08-15",
      "items": [
        {
          "placeId": "p001",
          "order": 1,
          "suggestedStartTime": "09:00",
          "suggestedDurationMinutes": 120,
          "reason": "..."
        }
      ]
    }
  ],
  "summary": "...",
  "notes": []
}
```

### Rules
AI phải:
- chỉ sử dụng `placeId` được cung cấp;
- không tự tạo địa điểm;
- không tự tạo weather / route data;
- không tạo ngày ngoài Trip;
- cố gắng phân bổ các địa điểm được yêu cầu;
- trả đúng schema.

Explanation chỉ cần ngắn gọn, không yêu cầu chain-of-thought.

User-provided content phải được xem là **data, not instruction** để giảm rủi ro prompt injection.

---

## 4C.5 — ITINERARY GENERATION

### Flow
```text
User
 ↓
Select places
 ↓
Set trip dates
 ↓
Optional preferences
 ↓
Generate
 ↓
Backend builds context
 ↓
AI generates recommendation
 ↓
Validation
 ↓
Preview
 ↓
Apply
```

AI không được overwrite itinerary ngay.

```text
Generate → Preview → User confirms → Apply
```

Khi regenerate, recommendation mới cũng phải qua Preview trước khi thay thế.

Người dùng luôn là người quyết định cuối cùng.

---

## 4C.6 — WEATHER-AWARE RECOMMENDATION

Weather được lấy từ:

```text
Weather API
 ↓
Normalized Weather Data
 ↓
AI Context
 ↓
AI Recommendation
```

Ví dụ outdoor A có mưa 80%, indoor B có mưa 20%, AI có thể đề xuất B trước A.

Tuy nhiên AI không được ép người dùng thay đổi lịch trình.

Nếu Weather API lỗi, AI vẫn có thể tạo itinerary dựa trên địa điểm, khoảng cách, ngày và preferences; tuyệt đối không bịa weather.

---

## 4C.7 — PERSONALIZATION

Cùng một danh sách địa điểm có thể tạo itinerary khác nhau cho người dùng khác nhau.

Ví dụ:

```text
User A: Relaxed + Nature + Few locations/day
User B: Active + Food + Many locations/day
```

MVP chỉ cần preference đơn giản:

```text
Pace:
- Relaxed
- Balanced
- Active
```

Có thể mở rộng interests:

```text
Nature / Food / Culture / Photography / Shopping / Entertainment
```

Không xây dựng trong MVP:
- AI personality model;
- long-term behavioral model;
- embeddings;
- vector database;
- machine-learning personalization.

Preference là **soft constraint**, không phải luật bắt buộc.

---

## 4C.8 — AI SAFETY, VALIDATION & FALLBACK

### Validation Pipeline
```text
Raw AI Response
 ↓
JSON Parse
 ↓
Schema Validation
 ↓
Business Validation
 ↓
Sanitization / Normalization
 ↓
Recommendation
```

### Schema Validation
Kiểm tra:
- JSON syntax;
- required fields;
- data types;
- enum values;
- array structure.

### Business Validation
Kiểm tra:
- `placeId` tồn tại;
- `placeId` thuộc Trip;
- không có địa điểm lạ;
- không duplicate;
- không thiếu địa điểm ngoài quy tắc cho phép;
- ngày thuộc Trip;
- order hợp lệ;
- duration hợp lệ.

### Data Integrity
Khi Apply:
- kiểm tra ownership;
- kiểm tra Foreign Key;
- kiểm tra Trip còn tồn tại;
- kiểm tra dữ liệu chưa bị thay đổi;
- sử dụng Database Transaction.

### Retry
Nếu output invalid:
```text
Maximum retry = 1
```

Retry vẫn phải được validate đầy đủ.

### Fallback
Nếu retry thất bại:

```text
AI
 ↓
Failed
 ↓
Rule-based fallback
```

Fallback có thể tạo itinerary cơ bản dựa trên khoảng cách, route efficiency, ngày và phân bổ địa điểm.

UI phải phân biệt:

```text
✨ AI đề xuất
```

và:

```text
🧭 Lịch trình cơ bản
```

AI failure không được làm mất Trip, Place, Memory hoặc làm Database không nhất quán.

### Safety Boundary
```text
AI
↓
Untrusted Output
↓
Backend Validation
↓
Trusted Data
↓
Database
```

> **AI có thể sai, nhưng hệ thống không được sai theo AI.**

---

## 4C.9 — AI COST, PERFORMANCE & API STRATEGY

### API Strategy
MVP sử dụng AI API có sẵn, ưu tiên OpenAI API. Không xây dựng model AI riêng.

### AI Call Budget
Mỗi lần Generate:
```text
1 AI call + maximum 1 retry
```

Tối đa 2 AI calls / generation.

### Không gọi AI cho
- CRUD Trip;
- CRUD Place;
- lưu Memory;
- achievement;
- weather;
- distance;
- map;
- authentication.

### Model
MVP chỉ dùng một model chính, nhưng model/provider phải configurable.

Đề xuất abstraction:

```text
AIProvider
    ↓
OpenAIProvider
```

### Context Optimization
Backend phải:
- filter candidate places;
- loại bỏ dữ liệu thừa;
- normalize route data;
- normalize weather data;
- chỉ gửi field cần thiết.

### Components
```text
AIContextBuilder
AIService
AIProvider
OpenAIProvider
```

`AIService` chỉ nhận `AIContext` và trả `AIRecommendation`; không tự truy cập Database hoặc external services.

### Configuration
Có thể cấu hình:
```text
AI_PROVIDER
AI_MODEL
AI_TIMEOUT
AI_MAX_RETRIES
AI_MAX_TOKENS
```

API Key chỉ tồn tại ở Backend:

```text
Frontend
 ↓
Backend
 ↓
OpenAI API
```

### Performance Target
```text
Normal backend request: P95 < ~500ms
AI generation target: ~3–10 seconds
```

Đây là target MVP, không phải hard guarantee vì AI provider nằm ngoài quyền kiểm soát.

### UX
Không dùng loading chung chung. Có thể hiển thị:
```text
✨ Đang phân tích các địa điểm...
🗺️ Đang tối ưu lịch trình...
✨ Đang hoàn thiện đề xuất...
```

Không dùng streaming hoặc background AI queue trong MVP.

### Cache
Recommendation cache không bắt buộc trong MVP. Có thể bổ sung `contextHash` nếu chi phí tăng.

### Duplicate Request
Frontend disable Generate khi đang xử lý; Backend dùng rate limiting và có thể dùng `generationRequestId`.

### Logging
Nên ghi:
- requestId;
- generationId;
- userId;
- tripId;
- provider;
- model;
- latency;
- retry count;
- validation result;
- fallback usage;
- token usage nếu provider hỗ trợ.

Không log password, API key, access token hoặc dữ liệu nhạy cảm không cần thiết.

---

## 4C.10 — MVP SCOPE

### In Scope
- OpenAI API hoặc AI provider tương đương.
- AI itinerary generation.
- Structured Output.
- Prompt engineering.
- AI Context Builder.
- Personalization cơ bản.
- Weather-aware recommendation ở mức đề xuất.
- Schema validation.
- Business validation.
- Retry tối đa 1 lần.
- Rule-based fallback.
- AI metadata.
- Basic rate limiting.
- Duplicate request protection.
- AI loading UX.
- AI Provider abstraction.
- Configurable model.
- Backend-only API key.

### Out of Scope
- Custom AI model.
- Fine-tuning.
- AI Agent.
- Autonomous planning.
- Tool-calling agent.
- AI Chat.
- AI long-term memory.
- Multi-model orchestration.
- Streaming.
- Background AI queue.
- Complex recommendation cache.
- Vector database.
- Embedding-based personalization.
- Machine-learning training pipeline.

---

## 4C.11 — FINAL ARCHITECTURAL PRINCIPLES

1. **AI is a recommendation engine, not the system's source of truth.**
2. **User remains the final decision maker.**
3. **Backend validates every AI-generated result before persistence.**
4. **Weather and route information come from their respective services, not from AI hallucination.**
5. **AI receives only the minimum context necessary.**
6. **One generation uses one AI call by default and at most one retry.**
7. **AI failure must not break core Trip functionality.**
8. **A rule-based fallback must be available for basic itinerary generation.**
9. **AI must improve UX, not make UX slower or unnecessarily complex.**
10. **The MVP prioritizes API integration over building custom AI infrastructure.**

---

## 4C.12 — FINAL DECISION

Phase 4C is **APPROVED / FROZEN**.

MVP sử dụng AI như một external recommendation service, kết hợp với deterministic backend logic, map/route services, weather services và user preferences.

Kiến trúc chính:

```text
External APIs
      +
Backend Business Logic
      +
AI Recommendation
      +
Validation / Fallback
      +
Human-in-the-loop
```

Thiết kế này phù hợp với mục tiêu **solo-developer MVP graduation project**, có chiều sâu về AI integration nhưng tránh over-engineering.

---

## Changelog

### Phase 4C — Approved
- Defined AI responsibility boundary.
- Defined AI context.
- Defined recommendation logic.
- Defined Structured Output.
- Defined itinerary generation flow.
- Defined weather-aware recommendation.
- Defined personalization.
- Defined validation, retry and fallback.
- Defined AI cost and performance strategy.
- Defined MVP AI boundaries.
- Frozen architecture for implementation planning.
