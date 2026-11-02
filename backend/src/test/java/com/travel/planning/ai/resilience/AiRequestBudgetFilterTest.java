package com.travel.planning.ai.resilience;

import com.travel.planning.retrieval.config.RagDemoProperties;
import jakarta.servlet.ServletException;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.*;

import static com.travel.planning.ai.resilience.AiRequestContext.Stage.INTENT;
import static org.assertj.core.api.Assertions.*;

class AiRequestBudgetFilterTest {
    @Test void filterCleansUpEvenWhenRequestThrows() throws Exception {
        var context = new AiRequestContext(new RagDemoProperties());
        var filter = new AiRequestBudgetFilter(context);
        var request = new MockHttpServletRequest("POST", "/api/v1/public/planning/generate-draft");
        assertThatThrownBy(() -> filter.doFilter(request, new MockHttpServletResponse(), (req, res) -> {
            context.claimRetry(); context.failure(INTENT, "TIMEOUT", null);
            throw new ServletException("fixture");
        })).isInstanceOf(ServletException.class);
        filter.doFilter(new MockHttpServletRequest("POST", "/api/v1/public/planning/generate-draft"),
                new MockHttpServletResponse(), (req, res) -> {
                    assertThat(context.report().retriesUsed()).isZero();
                    assertThat(context.report().stages().getFirst().source()).isEqualTo("SKIPPED");
                });
        assertThatThrownBy(context::report).isInstanceOf(IllegalStateException.class);
    }
}
