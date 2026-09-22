package com.travel.planning.ai.context;

import com.travel.planning.ai.context.model.AiItineraryPromptContext;
import com.travel.planning.ai.context.model.PlaceContext;
import com.travel.planning.ai.context.model.RouteLegContext;
import com.travel.planning.ai.context.model.UserPreferencesContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class AiContextBuilderTest {

    private AiContextBuilder builder;

    @BeforeEach
    void setUp() {
        builder = new AiContextBuilder();
    }

    @Test
    @DisplayName("buildSystemInstruction should return non-blank instruction with key rules")
    void buildSystemInstruction_containsMandatoryRules() {
        String instruction = builder.buildSystemInstruction();

        assertThat(instruction).isNotBlank();
        assertThat(instruction).contains("Held-Karp");
        assertThat(instruction).contains("QUY TẮC BẮT BUỘC");
        assertThat(instruction).contains("TÔN TRỌNG THỨ TỰ ĐỊA ĐIỂM");
        assertThat(instruction).contains("ĐẦY ĐỦ 100% ĐỊA ĐIỂM");
        assertThat(instruction).contains("GIỮ NGUYÊN PLACE_ID");
    }

    @Test
    @DisplayName("buildUserPrompt should format context including destination, places, legs and preferences")
    void buildUserPrompt_formatsCompleteContext() {
        UUID id1 = UUID.randomUUID();
        UUID id2 = UUID.randomUUID();

        PlaceContext p1 = new PlaceContext(id1, "Cầu Rồng", "Đà Nẵng", 16.0610, 108.2272, 60, "Check-in cầu rồng");
        PlaceContext p2 = new PlaceContext(id2, "Bà Nà Hills", "Hòa Vang", 15.9988, 107.9959, 240, "Khu du lịch sinh thái");

        RouteLegContext leg = new RouteLegContext("Cầu Rồng", "Bà Nà Hills", 25.6, 44);

        UserPreferencesContext prefs = new UserPreferencesContext(
                "MODERATE",
                "FRIENDS",
                "STANDARD",
                List.of("Ẩm thực", "Nhiếp ảnh"),
                "Ăn trưa gần Bà Nà"
        );

        AiItineraryPromptContext context = new AiItineraryPromptContext(
                "Đà Nẵng",
                LocalDate.of(2026, 10, 1),
                LocalDate.of(2026, 10, 2),
                2,
                prefs,
                List.of(p1, p2),
                List.of(leg),
                25.6,
                44
        );

        String prompt = builder.buildUserPrompt(context);

        assertThat(prompt).contains("Đà Nẵng");
        assertThat(prompt).contains("2 ngày");
        assertThat(prompt).contains("MODERATE");
        assertThat(prompt).contains("Ẩm thực, Nhiếp ảnh");
        assertThat(prompt).contains(id1.toString());
        assertThat(prompt).contains("Cầu Rồng");
        assertThat(prompt).contains("Bà Nà Hills");
        assertThat(prompt).contains("Chặng 1: Cầu Rồng ➔ Bà Nà Hills");
        assertThat(prompt).contains("dailyPlans");
    }
}
