package com.travel.planning.rule.model;

import com.travel.planning.rule.enums.IssueCategory;
import com.travel.planning.rule.enums.IssueSeverity;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.Map;
import java.util.UUID;

@Schema(description = "Chi tiết một vấn đề hoặc xung đột được phát hiện bởi Rule Evaluator")
public record DetectedIssue(
        @Schema(description = "ID địa điểm gặp vấn đề")
        UUID placeId,

        @Schema(description = "Tên địa điểm gặp vấn đề", example = "Bảo tàng Điêu khắc Chăm")
        String placeName,

        @Schema(description = "Thời điểm xảy ra vấn đề")
        LocalDateTime scheduledTime,

        @Schema(description = "Phân loại nhóm vấn đề")
        IssueCategory category,

        @Schema(description = "Mức độ nghiêm trọng (CRITICAL, IMPORTANT, INFO)")
        IssueSeverity severity,

        @Schema(description = "Lý do phát hiện theo quy tắc xác định",
                example = "Bảo tàng đóng cửa lúc 17:00, nhưng thời gian đến dự kiến là 17:15")
        String reason,

        @Schema(description = "Gợi ý khắc phục sơ bộ theo logic xác định",
                example = "Đảo thứ tự tham quan lên trước 16:00 hoặc chuyển sang địa điểm mở cửa buổi tối")
        String suggestionHint,

        @Schema(description = "Dữ liệu ngữ cảnh đi kèm (nhiệt độ, lượng mưa, giờ đóng cửa...)")
        Map<String, Object> metadata
) {
    public DetectedIssue {
        if (metadata == null) {
            metadata = Collections.emptyMap();
        } else {
            metadata = Collections.unmodifiableMap(metadata);
        }
    }
}
