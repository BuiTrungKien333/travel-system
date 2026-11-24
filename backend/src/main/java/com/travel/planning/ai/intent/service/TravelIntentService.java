package com.travel.planning.ai.intent.service;

import com.travel.planning.ai.intent.dto.response.IntentAnalysisResponse;
import com.travel.planning.ai.intent.dto.response.IntentSearchResponse;
import com.travel.planning.ai.intent.model.TravelIntent;
import com.travel.planning.ai.intent.provider.GeminiIntentClient;
import com.travel.planning.retrieval.config.RagProperties;
import com.travel.planning.retrieval.dto.response.PlaceSearchResponse;
import com.travel.planning.retrieval.model.PlaceCandidate;
import com.travel.planning.retrieval.service.PlaceRetrievalService;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

import java.text.Normalizer;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
@Profile({"dev", "rag-demo", "planning-ai"})
public class TravelIntentService {

    private final GeminiIntentClient client;
    private final PlaceRetrievalService retrieval;
    private final RagProperties properties;

    private static final Map<String, String> QUERY_WORDS = Map.ofEntries(
            Map.entry("MUSEUM", "bảo tàng"),
            Map.entry("HISTORY", "lịch sử"),
            Map.entry("CULTURE", "văn hóa"),
            Map.entry("ART", "nghệ thuật"),
            Map.entry("LOCAL", "địa phương"),
            Map.entry("MARKET", "chợ"),
            Map.entry("SHOPPING", "mua sắm"),
            Map.entry("BEACH", "biển"),
            Map.entry("SCENERY", "ngắm cảnh"),
            Map.entry("RELAX", "thư giãn"),
            Map.entry("OUTDOOR", "ngoài trời"),
            Map.entry("PARK", "công viên"),
            Map.entry("ARCHITECTURE", "kiến trúc"),
            Map.entry("TEMPLE", "chùa")
    );
    private static final Pattern EXPLICIT_TIME = Pattern.compile("(?<!\\d)([01]?\\d|2[0-3])(?::([0-5]\\d)|\\s*(?:gio|h)(?:\\s*([0-5]?\\d)\\s*(?:phut|p)?)?)(?!\\d)");
    private static final Pattern EXPLICIT_DAYS = Pattern.compile("(?<![a-z0-9])([0-9]+|mot|hai)\\s*ngay(?![a-z0-9])");

    public TravelIntentService(
            GeminiIntentClient client,
            PlaceRetrievalService retrieval,
            RagProperties properties
    ) {
        this.client = client;
        this.retrieval = retrieval;
        this.properties = properties;
    }

    public IntentAnalysisResponse analyze(String text) {
        return analyzeIntent(client.extract(text), properties.getIntentModel());
    }

    public IntentAnalysisResponse analyze(TravelIntent preferences) {
        return analyzeIntent(preferences, null);
    }

    public IntentAnalysisResponse analyze(TravelIntent preferences, String supplementalText) {
        if (supplementalText == null || supplementalText.isBlank()) return analyze(preferences);
        List<String> directConflicts = findExplicitConflicts(preferences, supplementalText);
        if (!directConflicts.isEmpty()) return withConflicts(analyzeIntent(preferences, null), directConflicts);
        TravelIntent extra = client.extract(supplementalText);
        List<String> conflicts = new ArrayList<>();
        compare("điểm đến", preferences.destination(), extra.destination(), conflicts, true);
        compare("số ngày", preferences.numberOfDays(), extra.numberOfDays(), conflicts, false);
        compare("giờ bắt đầu", preferences.dailyStartTime(), extra.dailyStartTime(), conflicts, false);
        compare("giờ kết thúc", preferences.dailyEndTime(), extra.dailyEndTime(), conflicts, false);
        compare("nhịp độ", preferences.travelPace(), extra.travelPace(), conflicts, false);
        TravelIntent merged = new TravelIntent(preferences.destination(), preferences.numberOfDays(),
                union(preferences.interests(), extra.interests()), union(preferences.excludedTags(), extra.excludedTags()),
                preferences.dailyStartTime(), preferences.dailyEndTime(), preferences.travelPace(),
                union(preferences.mustVisitNames(), extra.mustVisitNames()),
                union(preferences.excludedPlaceNames(), extra.excludedPlaceNames()), preferences.evidence());
        IntentAnalysisResponse result = analyzeIntent(merged, null);
        if (conflicts.isEmpty()) return result;
        return withConflicts(result, conflicts);
    }

    private IntentAnalysisResponse withConflicts(IntentAnalysisResponse result, List<String> conflicts) {
        List<String> questions = new ArrayList<>(result.questions());
        questions.add("Thông tin mô tả bổ sung chưa khớp với biểu mẫu:\n" + String.join("\n", conflicts)
                + "\nSửa mô tả để khớp với biểu mẫu; nếu mô tả mới là ý đúng, hãy cập nhật trường tương ứng trong biểu mẫu rồi tạo lại.");
        return new IntentAnalysisResponse("NEEDS_INPUT", result.intent(), result.provinceCode(), result.missingFields(),
                List.copyOf(questions), result.warnings(), result.positiveQuery(), result.mustVisitPlaces(),
                result.excludedPlaceIds(), result.model());
    }

    private List<String> findExplicitConflicts(TravelIntent preferences, String text) {
        String normalized = Normalizer.normalize(text, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "").toLowerCase(Locale.ROOT).replace('đ', 'd');
        List<String> conflicts = new ArrayList<>();
        Matcher dayMatcher = EXPLICIT_DAYS.matcher(normalized);
        Set<Integer> days = new LinkedHashSet<>();
        while (dayMatcher.find()) {
            String value = dayMatcher.group(1);
            try { days.add(Integer.parseInt(value)); }
            catch (NumberFormatException ignored) { days.add("mot".equals(value) ? 1 : 2); }
        }
        if (days.size() == 1) compare("số ngày", preferences.numberOfDays(), days.iterator().next(), conflicts, false);

        Matcher timeMatcher = EXPLICIT_TIME.matcher(normalized);
        List<String> times = new ArrayList<>();
        while (timeMatcher.find()) {
            String minute = timeMatcher.group(2) != null ? timeMatcher.group(2) : timeMatcher.group(3);
            times.add("%02d:%02d".formatted(Integer.parseInt(timeMatcher.group(1)), minute == null ? 0 : Integer.parseInt(minute)));
        }
        if (times.size() == 2) {
            compare("giờ bắt đầu", preferences.dailyStartTime(), times.get(0), conflicts, false);
            compare("giờ kết thúc", preferences.dailyEndTime(), times.get(1), conflicts, false);
        }

        String pace = explicitPace(normalized);
        if (pace != null) compare("nhịp độ", preferences.travelPace(), pace, conflicts, false);
        return conflicts;
    }

    private String explicitPace(String text) {
        Set<String> paces = new LinkedHashSet<>();
        if (text.contains("thong tha") || text.contains("thu tha") || text.contains("nhe nhang") || text.contains("cham rai")) paces.add("RELAXED");
        if (text.contains("vua phai") || text.contains("can bang")) paces.add("BALANCED");
        if (text.contains("nang dong") || text.contains("lich day") || text.contains("di nhieu")) paces.add("ACTIVE");
        return paces.size() == 1 ? paces.iterator().next() : null;
    }

    private void compare(String field, Object structured, Object supplemental, List<String> conflicts, boolean destination) {
        if (structured == null || supplemental == null) return;
        boolean same = destination ? canonicalDestination(structured.toString()).equals(canonicalDestination(supplemental.toString()))
                : structured.equals(supplemental);
        if (!same) conflicts.add("• " + field + ": biểu mẫu = " + displayConflictValue(field, structured)
                + ", mô tả bổ sung = " + displayConflictValue(field, supplemental));
    }

    private String displayConflictValue(String field, Object value) {
        if ("nhịp độ".equals(field)) return switch (value.toString()) {
            case "RELAXED" -> "thong thả";
            case "BALANCED" -> "vừa phải";
            case "ACTIVE" -> "năng động";
            default -> value.toString();
        };
        if ("số ngày".equals(field)) return value + " ngày";
        return value.toString();
    }

    private String canonicalDestination(String value) {
        String normalized = normalize(value);
        return Set.of("danang", "tp da nang", "thanh pho da nang").contains(normalized) ? "da nang" : normalized;
    }

    private List<String> union(List<String> first, List<String> second) {
        LinkedHashSet<String> merged = new LinkedHashSet<>();
        if (first != null) merged.addAll(first);
        if (second != null) merged.addAll(second);
        return List.copyOf(merged);
    }

    private IntentAnalysisResponse analyzeIntent(TravelIntent intent, String model) {
        List<String> missing = new ArrayList<>();
        List<String> questions = new ArrayList<>();
        List<String> warnings = new ArrayList<>();

        if (intent.destination() == null) {
            missing.add("destination");
            questions.add("Bạn muốn đi tỉnh/thành nào? Demo hiện có dữ liệu Đà Nẵng.");
        }
        if (intent.numberOfDays() == null) {
            missing.add("numberOfDays");
        }
        if (intent.dailyStartTime() == null) {
            missing.add("dailyStartTime");
        }
        if (intent.dailyEndTime() == null) {
            missing.add("dailyEndTime");
        }
        if (intent.travelPace() == null) {
            missing.add("travelPace");
        }
        if (intent.numberOfDays() != null && intent.numberOfDays() > 3) {
            warnings.add("Demo lập lịch tiếp theo giới hạn 1–3 ngày; hiện chỉ truy xuất ứng viên.");
        }
        if (intent.dailyStartTime() != null && intent.dailyEndTime() != null
                && !LocalTime.parse(intent.dailyStartTime()).isBefore(LocalTime.parse(intent.dailyEndTime()))) {
            questions.add("Khung giờ trong ngày chưa hợp lệ. Bạn xác nhận lại giờ bắt đầu và kết thúc; demo chưa hỗ trợ khung qua đêm.");
        }

        Set<String> overlap = new HashSet<>(intent.interests());
        overlap.retainAll(intent.excludedTags());
        if (!overlap.isEmpty()) {
            questions.add("Bạn vừa muốn vừa loại trừ nhóm " + String.join(", ", new TreeSet<>(overlap)) + ". Hãy xác nhận lại.");
        }

        boolean supported = intent.destination() != null && Set.of("da nang", "danang", "tp da nang", "thanh pho da nang")
                .contains(normalize(intent.destination()));
        String status = supported ? "READY_FOR_RETRIEVAL" : intent.destination() == null ? "NEEDS_INPUT" : "UNSUPPORTED_DESTINATION";
        if (intent.destination() != null && !supported) {
            questions.add("Kho demo chỉ có Đà Nẵng; chưa truy xuất cho điểm đến bạn yêu cầu.");
        }

        List<PlaceCandidate> required = new ArrayList<>();
        Set<UUID> excludedIds = new LinkedHashSet<>();

        // Resolve names only against the curated DB catalog, never via LLM identifiers.
        if (supported) {
            List<PlaceCandidate> catalog = retrieval.catalog();
            for (String name : intent.mustVisitNames()) {
                List<PlaceCandidate> found = resolve(name, catalog);
                if (found.size() != 1) {
                    questions.add("Điểm bắt buộc '" + name + "' chưa xác định duy nhất trong kho demo. Hãy chọn lại từ catalog.");
                } else if (required.stream().noneMatch(p -> p.placeId().equals(found.getFirst().placeId()))) {
                    required.add(found.getFirst());
                }
            }
            for (String name : intent.excludedPlaceNames()) {
                List<PlaceCandidate> found = resolve(name, catalog);
                if (found.size() != 1) {
                    questions.add("Điểm loại trừ '" + name + "' chưa xác định duy nhất trong kho demo. Hãy xác nhận lại.");
                } else {
                    excludedIds.add(found.getFirst().placeId());
                }
            }
            for (PlaceCandidate place : required) {
                if (excludedIds.contains(place.placeId()) || place.tags().stream().anyMatch(intent.excludedTags()::contains)) {
                    questions.add("Điểm bắt buộc '" + place.name() + "' mâu thuẫn với yêu cầu loại trừ. Bạn muốn giữ hay bỏ điểm này?");
                }
            }
        }

        // Interests are alternatives: a museum need not also be a market to be a useful candidate.
        // Preserve AND within each phrase, OR across independently requested interests/places.
        List<String> positiveGroups = new ArrayList<>(intent.interests().stream().map(QUERY_WORDS::get).toList());
        positiveGroups.addAll(required.stream().map(PlaceCandidate::name).toList());
        String positiveQuery = String.join(" OR ", positiveGroups);

        if (intent.interests().isEmpty() && intent.mustVisitNames().isEmpty()) {
            missing.add("interests");
            questions.add("Bạn muốn trải nghiệm gì: văn hóa, nghệ thuật, đi chợ hay ngắm biển/cảnh quan?");
        }
        if (supported && !questions.isEmpty()) {
            status = "NEEDS_INPUT";
        }
        if (!missing.isEmpty()) {
            warnings.add("Thông tin chưa cung cấp được giữ trống. Truy xuất chỉ cần điểm đến và sở thích/điểm ghé rõ ràng; luồng lập lịch kiểm tra riêng các trường bắt buộc và công bố chính sách cho trường tùy chọn.");
        }

        return new IntentAnalysisResponse(
                status,
                intent,
                supported ? "48" : null,
                missing,
                questions,
                warnings,
                positiveQuery,
                required,
                List.copyOf(excludedIds),
                model
        );
    }

    public IntentSearchResponse search(String text, int limit) {
        return search(analyze(text), limit);
    }

    public IntentSearchResponse search(TravelIntent preferences, int limit) {
        return search(analyze(preferences, null), limit);
    }

    public IntentSearchResponse search(IntentAnalysisResponse analysis, int limit) {
        if (!analysis.status().equals("READY_FOR_RETRIEVAL")) {
            return new IntentSearchResponse(analysis.status(), analysis, null);
        }
        if (analysis.mustVisitPlaces().size() > limit) {
            var questions = new ArrayList<>(analysis.questions());
            questions.add("Số điểm bắt buộc lớn hơn limit. Hãy tăng limit hoặc giảm số điểm bắt buộc.");
            var revised = new IntentAnalysisResponse(
                    "NEEDS_INPUT",
                    analysis.intent(),
                    analysis.provinceCode(),
                    analysis.missingFields(),
                    questions,
                    analysis.warnings(),
                    analysis.positiveQuery(),
                    analysis.mustVisitPlaces(),
                    analysis.excludedPlaceIds(),
                    analysis.model()
            );
            return new IntentSearchResponse("NEEDS_INPUT", revised, null);
        }

        var result = retrieval.search(analysis.positiveQuery(), analysis.intent().excludedTags(), limit);
        Set<UUID> excluded = new HashSet<>(analysis.excludedPlaceIds());
        List<PlaceSearchResponse.Match> matches = new ArrayList<>();
        for (PlaceCandidate required : analysis.mustVisitPlaces()) {
            matches.add(result.matches().stream()
                    .filter(m -> m.place().placeId().equals(required.placeId()))
                    .findFirst()
                    .orElse(new PlaceSearchResponse.Match(required, 0, false, false)));
        }

        Set<UUID> added = new HashSet<>(analysis.mustVisitPlaces().stream().map(PlaceCandidate::placeId).toList());
        for (var match : result.matches()) {
            if (matches.size() >= limit) {
                break;
            }
            if (!excluded.contains(match.place().placeId()) && added.add(match.place().placeId())) {
                matches.add(match);
            }
        }

        var candidates = new PlaceSearchResponse(result.retrievalMode(), result.embeddingModel(), List.copyOf(matches), result.fallbackReason());
        return new IntentSearchResponse(matches.isEmpty() ? "NO_CANDIDATES" : "CANDIDATES_FOUND", analysis, candidates);
    }

    private List<PlaceCandidate> resolve(String name, List<PlaceCandidate> catalog) {
        String query = normalize(name);
        List<PlaceCandidate> exact = catalog.stream().filter(p -> normalize(p.name()).equals(query)).toList();
        if (!exact.isEmpty()) {
            return exact;
        }
        if (query.length() < 4) {
            return List.of();
        }
        return catalog.stream().filter(p -> (" " + normalize(p.name()) + " ").contains(" " + query + " ")).toList();
    }

    private String normalize(String value) {
        return Normalizer.normalize(value, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .toLowerCase(Locale.ROOT)
                .replace('đ', 'd')
                .replaceAll("[^a-z0-9]+", " ")
                .strip();
    }
}
