package com.travel.planning.ai.resilience;

import com.travel.common.exception.AppException;
import com.travel.common.exception.ErrorCode;

/**
 * Safe diagnostic data only; never retains the provider body, key, or user prompt.
 */
public class AiCallFailure extends AppException {
    private final String reason;
    private final Integer providerStatus;

    public AiCallFailure(String reason, Integer providerStatus) {
        super(reason.equals("RATE_LIMITED") || reason.equals("QUOTA_EXHAUSTED") ? ErrorCode.AI_RATE_LIMIT_EXCEEDED
                : reason.equals("INVALID_RESPONSE") ? ErrorCode.AI_VALIDATION_FAILED : ErrorCode.AI_SERVICE_UNAVAILABLE);
        this.reason = reason;
        this.providerStatus = providerStatus;
    }

    public String reason() {
        return reason;
    }

    public Integer providerStatus() {
        return providerStatus;
    }
}
