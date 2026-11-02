package com.travel.planning.ai.intent.service;

import com.travel.planning.ai.intent.dto.response.IntentAnalysisResponse;
import com.travel.planning.ai.intent.dto.response.IntentSearchResponse;
import com.travel.planning.ai.intent.model.TravelIntent;
import com.travel.planning.ai.intent.provider.GeminiIntentClient;
import com.travel.planning.retrieval.config.RagDemoProperties;
import com.travel.planning.retrieval.dto.response.PlaceSearchResponse;
import com.travel.planning.retrieval.model.PlaceCandidate;
import com.travel.planning.retrieval.service.PlaceRetrievalService;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

import java.text.Normalizer;
import java.time.LocalTime;
import java.util.*;

@Service
@Profile("rag-demo")
public class TravelIntentService {
    private final GeminiIntentClient client;
    private final PlaceRetrievalService retrieval;
    private final RagDemoProperties properties;
    private static final Map<String, String> QUERY_WORDS = Map.ofEntries(
            Map.entry("MUSEUM", "bảo tàng"), Map.entry("HISTORY", "lịch sử"), Map.entry("CULTURE", "văn hóa"),
            Map.entry("ART", "nghệ thuật"), Map.entry("LOCAL", "địa phương"), Map.entry("MARKET", "chợ"),
            Map.entry("SHOPPING", "mua sắm"), Map.entry("BEACH", "biển"), Map.entry("SCENERY", "ngắm cảnh"),
            Map.entry("RELAX", "thư giãn"), Map.entry("OUTDOOR", "ngoài trời"), Map.entry("PARK", "công viên"),
            Map.entry("ARCHITECTURE", "kiến trúc"), Map.entry("TEMPLE", "chùa"));

    public TravelIntentService(GeminiIntentClient client, PlaceRetrievalService retrieval,
                               RagDemoProperties properties) {
        this.client = client; this.retrieval = retrieval; this.properties = properties;
    }

    public IntentAnalysisResponse analyze(String text) {
        return analyze(client.extract(text), properties.getIntentModel());
    }

    public IntentAnalysisResponse analyze(TravelIntent preferences) { return analyze(preferences, null); }

    private IntentAnalysisResponse analyze(TravelIntent intent, String model) {
        List<String> missing = new ArrayList<>(), questions = new ArrayList<>(), warnings = new ArrayList<>();
        if (intent.destination() == null) {
            missing.add("destination"); questions.add("Bạn muốn đi tỉnh/thành nào? Demo hiện có dữ liệu Đà Nẵng.");
        }
        if (intent.numberOfDays() == null) missing.add("numberOfDays");
        if (intent.dailyStartTime() == null) missing.add("dailyStartTime");
        if (intent.dailyEndTime() == null) missing.add("dailyEndTime");
        if (intent.travelPace() == null) missing.add("travelPace");
        if (intent.numberOfDays() != null && intent.numberOfDays() > 2) warnings.add("Demo lập lịch tiếp theo giới hạn 1–2 ngày; hiện chỉ truy xuất ứng viên.");
        if (intent.dailyStartTime() != null && intent.dailyEndTime() != null
                && !LocalTime.parse(intent.dailyStartTime()).isBefore(LocalTime.parse(intent.dailyEndTime()))) {
            questions.add("Khung giờ trong ngày chưa hợp lệ. Bạn xác nhận lại giờ bắt đầu và kết thúc; demo chưa hỗ trợ khung qua đêm.");
        }
        Set<String> overlap = new HashSet<>(intent.interests()); overlap.retainAll(intent.excludedTags());
        if (!overlap.isEmpty()) questions.add("Bạn vừa muốn vừa loại trừ nhóm " + String.join(", ", new TreeSet<>(overlap)) + ". Hãy xác nhận lại.");

        boolean supported = intent.destination() != null && Set.of("da nang", "danang", "tp da nang", "thanh pho da nang")
                .contains(normalize(intent.destination()));
        String status = supported ? "READY_FOR_RETRIEVAL" : intent.destination() == null ? "NEEDS_INPUT" : "UNSUPPORTED_DESTINATION";
        if (intent.destination() != null && !supported) questions.add("Kho demo chỉ có Đà Nẵng; chưa truy xuất cho điểm đến bạn yêu cầu.");
        List<PlaceCandidate> required = new ArrayList<>(); Set<UUID> excludedIds = new LinkedHashSet<>();
        // Resolve names only against the curated DB catalog, never via LLM identifiers.
        if (supported) {
            List<PlaceCandidate> catalog = retrieval.catalog();
            for (String name : intent.mustVisitNames()) {
                List<PlaceCandidate> found = resolve(name, catalog);
                if (found.size() != 1) questions.add("Điểm bắt buộc '" + name + "' chưa xác định duy nhất trong kho demo. Hãy chọn lại từ catalog.");
                else if (required.stream().noneMatch(p -> p.placeId().equals(found.getFirst().placeId()))) required.add(found.getFirst());
            }
            for (String name : intent.excludedPlaceNames()) {
                List<PlaceCandidate> found = resolve(name, catalog);
                if (found.size() != 1) questions.add("Điểm loại trừ '" + name + "' chưa xác định duy nhất trong kho demo. Hãy xác nhận lại.");
                else excludedIds.add(found.getFirst().placeId());
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
            missing.add("interests"); questions.add("Bạn muốn trải nghiệm gì: văn hóa, nghệ thuật, đi chợ hay ngắm biển/cảnh quan?");
        }
        if (supported && !questions.isEmpty()) status = "NEEDS_INPUT";
        if (!missing.isEmpty()) warnings.add("Thông tin chưa cung cấp được giữ trống. Truy xuất chỉ cần điểm đến và sở thích/điểm ghé rõ ràng; luồng lập lịch kiểm tra riêng các trường bắt buộc và công bố chính sách cho trường tùy chọn.");
        return new IntentAnalysisResponse(status, intent, supported ? "48" : null, missing, questions, warnings,
                positiveQuery, required, List.copyOf(excludedIds), model);
    }

    public IntentSearchResponse search(String text, int limit) {
        return search(analyze(text), limit);
    }

    public IntentSearchResponse search(TravelIntent preferences, int limit) {
        return search(analyze(preferences, null), limit);
    }

    public IntentSearchResponse search(IntentAnalysisResponse analysis, int limit) {
        if (!analysis.status().equals("READY_FOR_RETRIEVAL")) return new IntentSearchResponse(analysis.status(), analysis, null);
        if (analysis.mustVisitPlaces().size() > limit) {
            var questions = new ArrayList<>(analysis.questions()); questions.add("Số điểm bắt buộc lớn hơn limit. Hãy tăng limit hoặc giảm số điểm bắt buộc.");
            var revised = new IntentAnalysisResponse("NEEDS_INPUT", analysis.intent(), analysis.provinceCode(), analysis.missingFields(),
                    questions, analysis.warnings(), analysis.positiveQuery(), analysis.mustVisitPlaces(), analysis.excludedPlaceIds(), analysis.model());
            return new IntentSearchResponse("NEEDS_INPUT", revised, null);
        }
        var result = retrieval.search(analysis.positiveQuery(), analysis.intent().excludedTags(), 8);
        Set<UUID> excluded = new HashSet<>(analysis.excludedPlaceIds());
        List<PlaceSearchResponse.Match> matches = new ArrayList<>();
        for (PlaceCandidate required : analysis.mustVisitPlaces()) {
            matches.add(result.matches().stream().filter(m -> m.place().placeId().equals(required.placeId())).findFirst()
                    .orElse(new PlaceSearchResponse.Match(required, 0, false, false)));
        }
        Set<UUID> added = new HashSet<>(analysis.mustVisitPlaces().stream().map(PlaceCandidate::placeId).toList());
        for (var match : result.matches()) {
            if (matches.size() >= limit) break;
            if (!excluded.contains(match.place().placeId()) && added.add(match.place().placeId())) matches.add(match);
        }
        var candidates = new PlaceSearchResponse(result.retrievalMode(), result.embeddingModel(), List.copyOf(matches), result.fallbackReason());
        return new IntentSearchResponse(matches.isEmpty() ? "NO_CANDIDATES" : "CANDIDATES_FOUND", analysis, candidates);
    }

    private List<PlaceCandidate> resolve(String name, List<PlaceCandidate> catalog) {
        String query = normalize(name);
        List<PlaceCandidate> exact = catalog.stream().filter(p -> normalize(p.name()).equals(query)).toList();
        if (!exact.isEmpty()) return exact;
        if (query.length() < 4) return List.of();
        return catalog.stream().filter(p -> (" " + normalize(p.name()) + " ").contains(" " + query + " ")).toList();
    }

    private String normalize(String value) {
        return Normalizer.normalize(value, Normalizer.Form.NFD).replaceAll("\\p{M}", "")
                .toLowerCase(Locale.ROOT).replace('đ', 'd').replaceAll("[^a-z0-9]+", " ").strip();
    }

}
