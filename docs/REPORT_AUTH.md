# Báo cáo bảo mật và mức sẵn sàng production của auth

## 1. Kết luận

**Auth có nền tảng phù hợp MVP, nhưng mình chưa coi hệ thống đã sẵn sàng phát hành production.** Những phần cần hoàn thiện gần nhất là quản lý secret, bảo vệ workload public, throttling khi mở Internet, độ tin cậy gửi email và xác minh cấu hình/luồng native trên thiết bị. Tối ưu hiệu năng có mục tiêu là rút ngắn thời gian giữ DB lock và chuyển housekeeping khỏi request login.

Chưa có vulnerability **confirmed** theo mức bằng chứng của skill. Có **4 needs_validation**, đều đã qua hai lượt source review độc lập. Các lead có đường source cụ thể và kết quả cần quan sát; chưa có bằng chứng về account takeover, service interruption, provider spend hoặc race display đã xảy ra. Vì vậy chúng chưa có severity.

**Giới hạn:** đây là kết quả audit source, chưa là audit đã kiểm chứng runtime. Theo skill security-audit, target tests/builds/browser/provider traffic chỉ được chạy trong sandbox cô lập mạng, môi trường, quyền ghi và tài nguyên; host chưa đáp ứng các điều kiện đó. Hai CLI validators của skill cũng fail closed trên Windows vì Node thiếu O_NOFOLLOW/O_NONBLOCK. Schema/semantic checks bằng exported validateDocument APIs cho dữ liệu parent-owned đều có 0 lỗi, nhưng không thay thế CLI gate. Run metadata được đánh dấu **incomplete** với lý do chính xác này. Toàn bộ 4 candidate đã có disposition độc lập; không còn candidate bị bỏ dở chưa review.

## 2. Phạm vi và phương pháp

- Profile **standard**, không đặt budget cứng. 23 agent invocations đã dùng/23 theo kế hoạch cập nhật: 4 recon, 3 hunter bị gián đoạn vì usage limit và không nhận bằng chứng, 5 hunter hợp lệ, 2 post-wave critics, 1 final-clean critic, 4 candidate reviewers và 4 final-record reviewers.
- System source ref: **1333089ffaf62ee158eb6a5b95c5edf4854893c0**, có thay đổi chưa commit. Mobile: **7b4d0102350ca9be57ff148e67f3b292b63fe1c5**, có thay đổi chưa commit. Rà cả auth vừa triển khai trong working tree.
- Kiểm tra backend auth/security/profile, public planning/weather/provider sinks, migrations, config/deploy/build inputs; mobile token/session/query/deep links/Google/native config; Next starter và dependency provenance.
- Không có prior ledger tương thích để dùng; không có carried confirmation hay changed-source prior revalidation. Không xét ma trận actor/RBAC, tính đúng nghiệp vụ/thuật toán planning, live endpoints hoặc provider accounts. Không sửa application source.
- Chỉ source/metadata đọc trực tiếp và trusted parent JSON processing. Không chạy lại tests cũ, npm audit, Maven, native build, server hay database. Con số test trong docs là lịch sử, không phải kết quả audit này. Chưa đối chiếu toàn bộ transitive dependency tree với mọi advisory, nên không có tuyên bố “dependencies không có CVE”.

## 3. Confirmed findings

| Severity | Finding | Boundary | Kết quả quan sát |
|---|---|---|---|
| — | Không có confirmed record | — | Chưa thực hiện bounded target runtime validation |

## 4. NEEDS VALIDATION — ưu tiên kiểm chứng

| Thứ tự | Lead và source | Dữ kiện đã đọc được | Phần còn thiếu và bước nhỏ nhất |
|---|---|---|---|
| 1 | Gemini literal fallback — [application.yaml](E:/travel-system/backend/src/main/resources/application.yaml:68) | Common resource chứa một giá trị kiểu credential, có thể vào provider config/request khi thiếu env override | Owner xem riêng status/restrictions/authority và reader policy, không gửi thử key. Bỏ literal fallback; rotate/revoke theo quy trình nếu đây là credential thật đã lộ. Không cần chờ thử khai thác để cải thiện config |
| 2 | Forecast range — [MockWeatherProvider](E:/travel-system/backend/src/main/java/com/travel/weather/provider/MockWeatherProvider.java:35) | Loop tạo 24 hourly objects/ngày cho toàn bộ khoảng ngày; OpenMeteo lỗi/null cũng vào mock. includeHourly=false bỏ chi tiết sau khi đã tạo | Fixture 1/2/3 ngày + provider stub; owner xem horizon guard/heap/worker/cancellation. Thêm giới hạn horizon trước provider; chưa có quan sát shared DoS |
| 3 | Public planning fanout — [OutdoorWeatherRuleChecker](E:/travel-system/backend/src/main/java/com/travel/planning/rule/checker/OutdoorWeatherRuleChecker.java:45) | Lists/days chưa có aggregate cap; gọi forecast từng item, pipeline từng ngày và có thể nhiều lượt AI | Counting stubs 1/2/3 items, hai ngày nhỏ; owner xem body/call budgets/deadline/quota. Giới hạn ngày/item/tổng call, deduplicate cùng tọa độ/ngày. Solver đã chặn trên 16 places |
| 4 | Account replacement/profile — [API client](E:/travel-mobile-ui/src/services/api/client.ts:43), [auth-session](E:/travel-mobile-ui/src/features/auth/services/auth-session.ts:30) | Response success không kiểm generation; profile key chung; accept account mới không cancel fetch cũ | Cần quan sát warm-link/router/query observer. Last-observer unmount sẽ abort và có thể chặn race. Fixture hai account giả với response trì hoãn; chưa chứng minh tên/email cũ xuất hiện dưới phiên mới hoặc server bypass |

Full trace, lớp bảo vệ, blocker và kế hoạch an toàn cho từng lead: [NEEDS-VALIDATION.md](C:/Users/DELL/security-audit-skill/travel-system/run-1/NEEDS-VALIDATION.md). Thứ tự trên là ưu tiên xử lý kỹ thuật; chưa gán severity.

Key của Gemini nằm ở module provider/config, không phải OAuth client ID hay JWT auth token. Báo cáo không chứa giá trị key. Google hướng dẫn giữ key khỏi source control, lấy từ env/secret store và hỗ trợ header x-goog-api-key ([tài liệu Gemini](https://ai.google.dev/gemini-api/docs/api-key?authuser=00)). Đưa key vào query và log exception nguyên văn là điểm nên harden; hiện chưa chứng minh framework/log readership gây lộ key qua logs.

## 5. Auth API: còn gì trước production?

### Cần xử lý trước khi mở Internet

1. **Throttling thực tế.** Source có cooldown gửi email theo account/purpose, nhưng chưa thấy limiter cho toàn bộ login/register/challenge hoặc tổng nhiều account. Cooldown không chặn password guessing hay toàn bộ preauth work. Nếu gateway đã xử lý, cần xác minh hiệu lực và scope; nếu chưa, thêm limiter có giới hạn bộ nhớ theo IP + account, trả 429/Retry-After phù hợp. Không dùng permanent account lock để tránh biến cơ chế chống abuse thành khóa account của người khác. Đây là production control gap, chưa là một exploit đã tái hiện. [OWASP Authentication](https://cheatsheetseries.owasp.org/cheatsheets/Authentication_Cheat_Sheet.html), [Forgot Password](https://cheatsheetseries.owasp.org/cheatsheets/Forgot_Password_Cheat_Sheet.html).

2. **Gửi email có thể mất.** [Executor](E:/travel-system/backend/src/main/java/com/travel/auth/config/AuthEmailConfiguration.java:20) có 2 worker/queue 100. [Listener](E:/travel-system/backend/src/main/java/com/travel/auth/mail/AuthEmailListener.java:25) gửi sau commit, catch lỗi SMTP/queue overflow và log an toàn. Account/token/cooldown đã commit nhưng không có delivery status/durable retry; restart cũng có thể mất job. Không phải memory leak, nhưng ảnh hưởng trực tiếp register/recovery. Với production, cần handoff bền vững hoặc outbox nhỏ dùng PostgreSQL hiện có, retry có giới hạn/expiry và monitoring. Không cần thêm broker riêng chỉ để làm việc này. Nếu tiếp tục best-effort MVP, phải chấp nhận resend và đo delivery failure; 202 là nhận yêu cầu, không bảo đảm email đến inbox.

3. **Release config và device validation.** Chọn prod rõ ràng, signing keys riêng/ngẫu nhiên, HTTPS API, CORS đúng web origin, SMTP sender/domain thật, Google clients/package/bundle/certificate đúng. Magic-link production cần domain HTTPS, App Links/Universal Links hoặc trang fallback đã host; source hiện chủ yếu chuẩn bị dev scheme và docs. Chưa chạy SMTP thật hay release Android/iOS với OAuth thật; typecheck/export cũ không chứng minh native callback hay login UX thực tế.

4. **Hạn chế public workloads đang chưa ổn định.** Các alias public planning/weather vẫn reachable trong source. Trước khi mở server production, quyết định rõ route nào được public và áp giới hạn tài nguyên; có thể dùng ingress/feature gate trong thời gian planning chưa tích hợp. Việc này độc lập với ma trận actor/RBAC.

### Nên tối ưu có mục tiêu

- Password login giữ user lock/transaction trong lúc BCrypt kể cả mật khẩu sai ([login](E:/travel-system/backend/src/main/java/com/travel/auth/service/impl/AuthServiceImpl.java:79)). Chạy verification trên snapshot ngoài transaction, rồi khóa ngắn để kiểm lại hash/state/version trước issue sẽ giảm contention; có thể tăng một SELECT nhưng giảm chiếm connection/lock. Giữ kiểm lại để reset race vẫn an toàn.
- Google challenge chạy hai DELETE hết hạn trên mỗi request ([challenge](E:/travel-system/backend/src/main/java/com/travel/auth/service/GoogleAuthService.java:52)). Có expiry indexes nhưng lượng backlog vẫn ảnh hưởng latency. Dùng cleanup job theo batch; thêm cleanup expired refresh/action rows. TTL chặn sử dụng, không tự xóa row.
- Kiểm chứng rồi xử lý account replacement cache/generation trước khi thêm query chứa dữ liệu riêng tư. Guard JS/cache cancellation phù hợp có thể giải quyết mà không thêm DB round trip.

### Chính sách cần chốt, chưa cần dựng framework mới

- Logout revoke refresh được gửi lên; access đã cấp còn có thể dùng tối đa 30 phút. Source ghi rõ điều này ([logout](E:/travel-system/backend/src/main/java/com/travel/auth/service/impl/AuthServiceImpl.java:120)). Nếu cần logout có hiệu lực server ngay/từng thiết bị, session/family state là một yêu cầu mới có chi phí rõ. Reset password hiện tăng version và revoke toàn bộ refresh.
- Rotation consume token cũ một lần, nhưng chưa có token-family linkage để phát hiện reuse rồi revoke token mới của cùng family. Đây là giới hạn phản ứng sau khi bearer đã bị lộ; không có bằng chứng app làm lộ token. OAuth security BCP mô tả rotation kèm giữ quan hệ/revoke active refresh khi reuse ([RFC 9700 §4.14.2](https://www.rfc-editor.org/rfc/rfc9700.html#section-4.14.2)). Phiên JWT riêng hiện tại chưa phải một OAuth authorization server tuân thủ đầy đủ chuẩn đó.
- Register trả 409 khi email tồn tại, nên có discrepancy cho account enumeration; request forgot/resend trả body 202 chung. Chốt yêu cầu privacy của sản phẩm và UX trước khi thay response. Generic body cũng không tự chứng minh timing đồng đều; chưa đo timing.
- V7 backfill mọi email legacy thành verified theo policy tương thích. Cần xem chất lượng dữ liệu legacy trước upgrade; đây không phải bằng chứng sở hữu mailbox. Luồng Google chỉ xóa planted password nếu account còn unverified, phù hợp policy hiện có nhưng phụ thuộc backfill đáng tin.

## 6. Có overengineering không?

**Phần lớn cơ chế auth hiện tại có mục đích thực tế.** Hash token + TTL + atomic consume bảo vệ capability; unique email/sub và user locking bảo vệ identity/reset races; generation/storage queue/single-flight bảo vệ phiên trên mobile; Google verification ngoài transaction tránh giữ DB khi chờ mạng. Những race này có thể ảnh hưởng quyền truy cập và phiên, nên các cơ chế nhỏ hiện tại có cơ sở.

**Có phần nặng hơn mức tối thiểu của MVP:** Google DB challenge thêm protocol/table, khoảng một lượt HTTP và housekeeping. Nó tăng bảo vệ one-use/replay; không phải mọi backend Google login đều cần một table challenge. Web button prefetch/renew idle đổi giảm click latency lấy request cả khi người dùng chưa chọn Google. Có thể giới hạn renew khi active hoặc hoãn tối ưu web nếu chưa phát hành. Không cần thay giao thức ngay; giải quyết inline cleanup và đo độ trễ trước.

Việc yêu cầu mailbox proof cho Google email ngoài Gmail/Workspace là có lý do: email_verified không luôn chứng minh chủ hiện tại của third-party email; auto-link Gmail/Workspace phù hợp policy authoritative email ([Google](https://developers.google.com/identity/sign-in/android/backend-auth?hl=en)).

Chưa có dữ liệu để thêm Redis/Kafka/microservice auth, DPoP/MFA toàn hệ thống hoặc cache authorization phức tạp. Ưu tiên các thay đổi nhỏ có invariant rõ: giới hạn đầu vào, short transaction, cleanup batch và delivery reliability.

## 7. Có đảm bảo hiệu năng cao không?

**Chưa thể bảo đảm bằng source review.** Ước lượng logic: password login khoảng 3 SQL + BCrypt; refresh khoảng 4 SQL; returning Google khoảng 5 SQL + challenge khoảng 3 SQL qua hai API. Mỗi protected request thêm một primary-key auth-state projection; /me thêm một User query. Chưa tính Hibernate flush/retry/transaction round trips, provider hoặc độ trễ thực tế.

Auth-state query mỗi request cho phép reset/khóa/xóa account được kiểm lại trước request mới; đây là đánh đổi bảo mật/DB hợp lý ở MVP. Không cần bỏ nó để tiết kiệm một query khi chưa đo bottleneck. Cached summary có thể hiển thị ngay; profile cache updatedAt=0 hiện làm thêm /me để lấy full/fresh data. Single-flight giúp tránh một loạt 401 tạo nhiều refresh.

Để cam kết app mượt cần đo release build/device, p50/p95/p99, BCrypt, lock/pool waits, số SQL và warm/cold Google; test provider và storage/mạng lỗi bằng fixtures trước. Đánh giá đầy đủ và các đánh đổi: [PERFORMANCE-AND-COMPLEXITY.md](C:/Users/DELL/security-audit-skill/travel-system/run-1/PERFORMANCE-AND-COMPLEXITY.md).

## 8. Cấu hình phụ và các cảnh báo đã loại

- Compose DB port chưa bind host IP: nếu chỉ local, đổi sang loopback. Chưa chứng minh PostgreSQL đang public qua Internet hay bypass password.
- Default dev profile và actuator wildcard: cần explicit prod/narrow health trong release. Chưa có source cấu hình env/heapdump exposure, nên không kết luận lộ Actuator secrets.
- Dockerfile backend trống; .dockerignore thiếu .env exclusions. Thêm exclusions trước khi có image COPY/build. Chưa có current image-disclosure trace.
- Wrapper dùng HTTPS nhưng chưa pin distributionSha256Sum. Thêm checksum; hai npm lockfiles có registry origin/integrity. Không suy ra dependency attack chỉ từ installScript/phiên bản cũ.
- V3 seed được V6 soft-delete nếu còn hash mặc định; V7 không clear deletion; login/refresh/filter/email/Google đều reject deletion. Không còn căn cứ cáo buộc fresh migration cho login default seed.
- TSP solver cap 16 hoạt động; lỗi DTO quảng cáo 20 là contract/UX mismatch. Rủi ro resource ở số batch/day/call, không phải bỏ qua cap exponential allocation.
- Own profile lấy ID từ security context và chỉ update name/avatar; DTO không trả credentials. Avatar URL không được server fetch ở source đã rà, nên chưa có SSRF trace.
- Prompt text có thể ảnh hưởng advice; hiện không có tool dispatcher, persistent itinerary write hay cross-user context từ apply. Không có cơ sở gán prompt influence thành RCE/IDOR.
- Hai advisory được đối chiếu phạm vi: [CVE-2026-40976](https://spring.io/security/cve-2026-40976/) đòi Boot4.0.0–4.0.5/default security; target Boot4.1/custom chain. [CVE-2026-47877](https://spring.io/security/cve-2026-47877/) yêu cầu Authorization Server DefaultConsentPage; source target không dùng path này. Đây không phải full dependency advisory sweep.

## 9. Coverage và thứ tự hành động

Ledger có **16 units: 11 covered, 3 candidate units chứa 4 final needs_validation records, 0 blocked, 0 deferred, 2 out_of_scope**. Wave1 critic thêm bootstrap migration gap; wave2 và final-clean critic độc lập không yêu cầu thêm unit/reassignment trong source scope. “Covered” nghĩa là assigned source invariants đã review, không phải chứng nhận runtime/deployment an toàn. Tất cả 4 record đã có Phase3/Phase5 reviewer khác hunter; không có retained rejected/confirmed record.

Thứ tự khuyến nghị:

1. Bỏ literal credential fallback; owner xác định key thật và rotate/revoke nếu cần.
2. Quyết định exposure và áp resource/throttle limits trước mở Internet, ưu tiên forecast horizon và batch downstream budgets.
3. Hoàn thiện email delivery reliability + prod link/OAuth/SMTP config; kiểm tra thiết bị thật trong môi trường phù hợp.
4. Kiểm chứng account replacement race; rút ngắn BCrypt lock/transaction, đưa cleanup sang job nhỏ.
5. Đo latency/pool contention rồi mới quyết định cache/session infrastructure bổ sung.

Artifacts: [architecture](C:/Users/DELL/security-audit-skill/travel-system/run-1/architecture.md), [findings.json](C:/Users/DELL/security-audit-skill/travel-system/run-1/findings.json), [coverage-ledger](C:/Users/DELL/security-audit-skill/travel-system/run-1/coverage-ledger.json), [run-metadata](C:/Users/DELL/security-audit-skill/travel-system/run-1/run-metadata.json), [confirmed detail](C:/Users/DELL/security-audit-skill/travel-system/run-1/FINDINGS-DETAIL.md), [needs validation](C:/Users/DELL/security-audit-skill/travel-system/run-1/NEEDS-VALIDATION.md).
