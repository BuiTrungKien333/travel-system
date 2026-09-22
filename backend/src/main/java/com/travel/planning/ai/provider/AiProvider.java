package com.travel.planning.ai.provider;

import com.travel.planning.ai.context.model.AiItineraryPromptContext;
import com.travel.planning.ai.dto.response.AiItineraryResponse;

/**
 * Interface hợp đồng chung cho mọi AI Provider (Gemini, OpenAI, Claude, Mock...)
 */
public interface AiProvider {

    /**
     * Tạo lịch trình du lịch thông minh dựa trên ngữ cảnh đầu vào (TSP, sở thích, thông tin chuyến đi)
     *
     * @param context Ngữ cảnh đầy đủ của chuyến đi
     * @return Lịch trình chi tiết từng ngày dạng có cấu trúc
     */
    AiItineraryResponse generateItinerary(AiItineraryPromptContext context);

    /**
     * Kiểm tra xem provider có khả dụng hay không (ví dụ: đã cấu hình API key hợp lệ chưa)
     *
     * @return true nếu sẵn sàng phục vụ
     */
    boolean isAvailable();

    /**
     * Tên định danh của Provider (ví dụ: "gemini", "mock", "openai")
     *
     * @return Tên provider
     */
    String getProviderName();
}
