package com.travel.planning.ai.provider;

import com.travel.planning.ai.copilot.model.AiCopilotAnalysisResult;
import com.travel.planning.ai.copilot.model.AiCopilotPromptContext;

/**
 * Interface hợp đồng chung cho mọi AI Provider (Gemini, OpenAI, Claude, Mock...)
 */
public interface AiProvider {
    /**
     * Phân tích phản biện lịch trình và tạo thẻ đề xuất hành động (Human-in-the-loop)
     *
     * @param context Ngữ cảnh lịch trình, danh sách xung đột và thông tin thời tiết
     * @return Lời chào ân cần + tối đa 3 thẻ đề xuất hành động
     */
    AiCopilotAnalysisResult analyzeAndRecommend(AiCopilotPromptContext context);

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
