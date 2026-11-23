package com.travel.planning.rule.service;

import com.travel.planning.rule.checker.ScheduleRuleChecker;
import com.travel.planning.rule.model.DetectedIssue;
import com.travel.planning.rule.model.ItineraryEvaluationResult;
import com.travel.planning.rule.model.ScheduleItemContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j(topic = "RULE_EVALUATOR_SERVICE")
public class RuleEvaluatorService {

    private final List<ScheduleRuleChecker> ruleCheckers;

    public ItineraryEvaluationResult evaluateItinerary(List<ScheduleItemContext> items) {
        if (items == null || items.isEmpty()) {
            log.info("[RULE_EVALUATOR] Empty schedule items received. Returning all clear.");
            return ItineraryEvaluationResult.of(Collections.emptyList());
        }

        log.info("[RULE_EVALUATOR] Evaluating itinerary with {} locations across {} rule checkers",
                items.size(), ruleCheckers.size());

        List<DetectedIssue> allIssues = new ArrayList<>();

        for (ScheduleRuleChecker checker : ruleCheckers) {
            try {
                List<DetectedIssue> issues = checker.evaluate(items);
                if (issues != null && !issues.isEmpty()) {
                    allIssues.addAll(issues);
                }
            } catch (Exception ex) {
                log.error("[RULE_EVALUATOR] Checker '{}' failed with error: {}. Skipping.",
                        checker.getRuleName(), ex.getMessage(), ex);
            }
        }

        ItineraryEvaluationResult result = ItineraryEvaluationResult.of(allIssues);

        if (result.isAllClear()) {
            log.info("[RULE_EVALUATOR] ALL CLEAR. No conflicts found. AI Copilot remains SILENT (0 token consumed).");
        } else {
            log.warn("[RULE_EVALUATOR] Found {} issues (Critical: {}, Important: {}, Info: {}). AI Copilot flagged to TRIGGER.",
                    result.issues().size(), result.criticalCount(), result.importantCount(), result.infoCount());
        }

        return result;
    }
}
