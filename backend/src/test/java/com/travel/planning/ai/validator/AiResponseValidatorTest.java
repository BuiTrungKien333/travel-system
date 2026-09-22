package com.travel.planning.ai.validator;

import com.travel.planning.ai.context.model.AiItineraryPromptContext;
import com.travel.planning.ai.context.model.PlaceContext;
import com.travel.planning.ai.dto.response.AiActivitySlotResponse;
import com.travel.planning.ai.dto.response.AiDayPlanResponse;
import com.travel.planning.ai.dto.response.AiItineraryResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class AiResponseValidatorTest {

    private AiResponseValidator validator;

    @BeforeEach
    void setUp() {
        validator = new AiResponseValidator();
    }

    @Test
    @DisplayName("validateAndEnforce when all places are present should return response unchanged")
    void validateAndEnforce_allPlacesPresent_returnsUnchanged() {
        UUID id1 = UUID.randomUUID();
        UUID id2 = UUID.randomUUID();

        PlaceContext p1 = new PlaceContext(id1, "P1", null, 16.0, 108.0, 60, null);
        PlaceContext p2 = new PlaceContext(id2, "P2", null, 16.1, 108.1, 60, null);

        AiItineraryPromptContext context = new AiItineraryPromptContext(
                "Đà Nẵng", LocalDate.now(), LocalDate.now(), 1, null,
                List.of(p1, p2), List.of(), 10.0, 20
        );

        AiItineraryResponse response = new AiItineraryResponse(
                "Tiêu đề",
                "Tóm tắt",
                List.of(new AiDayPlanResponse(
                        1, "2026-10-01", "Chủ đề",
                        List.of(
                                new AiActivitySlotResponse(id1, "P1", "08:00 - 10:00", "VISIT", "desc", "60 phút", null),
                                new AiActivitySlotResponse(id2, "P2", "14:00 - 16:00", "VISIT", "desc", "60 phút", null)
                        ),
                        "Ăn trưa", "Lưu ý"
                )),
                List.of("Tip"),
                "Ngân sách"
        );

        AiItineraryResponse validated = validator.validateAndEnforce(response, context);

        assertThat(validated.dailyPlans().getFirst().activities()).hasSize(2);
    }

    @Test
    @DisplayName("validateAndEnforce when AI omits a place should auto-heal and insert the missing place")
    void validateAndEnforce_omittedPlace_autoHealsMissingPlace() {
        UUID id1 = UUID.randomUUID();
        UUID id2 = UUID.randomUUID();
        UUID id3 = UUID.randomUUID();

        PlaceContext p1 = new PlaceContext(id1, "P1", null, 16.0, 108.0, 60, null);
        PlaceContext p2 = new PlaceContext(id2, "P2", null, 16.1, 108.1, 60, null);
        PlaceContext p3 = new PlaceContext(id3, "P3 (Bị AI bỏ sót)", null, 16.2, 108.2, 60, null);

        AiItineraryPromptContext context = new AiItineraryPromptContext(
                "Đà Nẵng", LocalDate.now(), LocalDate.now().plusDays(1), 2, null,
                List.of(p1, p2, p3), List.of(), 20.0, 40
        );

        // AI chỉ trả về P1 ở Ngày 1 và P2 ở Ngày 2 (bỏ quên P3)
        AiItineraryResponse response = new AiItineraryResponse(
                "Tiêu đề",
                "Tóm tắt",
                List.of(
                        new AiDayPlanResponse(
                                1, "2026-10-01", "Ngày 1",
                                List.of(
                                        new AiActivitySlotResponse(id1, "P1", "08:00 - 10:00", "VISIT", "desc", "60 phút", null),
                                        new AiActivitySlotResponse(null, "Ăn trưa", "12:00 - 13:00", "MEAL", "desc", "60 phút", null)
                                ),
                                "Ăn trưa", "Lưu ý"
                        ),
                        new AiDayPlanResponse(
                                2, "2026-10-02", "Ngày 2",
                                List.of(
                                        new AiActivitySlotResponse(id2, "P2", "08:00 - 10:00", "VISIT", "desc", "60 phút", null)
                                ),
                                "Ăn trưa", "Lưu ý"
                        )
                ),
                List.of("Tip"),
                "Ngân sách"
        );

        AiItineraryResponse validated = validator.validateAndEnforce(response, context);

        // Ngày 2 có ít hoạt động hơn (1 hoạt động so với 2 ở ngày 1), nên P3 sẽ được chèn vào Ngày 2
        assertThat(validated.dailyPlans().get(1).activities()).hasSize(2);
        assertThat(validated.dailyPlans().get(1).activities().get(1).placeId()).isEqualTo(id3);
        assertThat(validated.dailyPlans().get(1).activities().get(1).placeName()).isEqualTo("P3 (Bị AI bỏ sót)");
    }
}
