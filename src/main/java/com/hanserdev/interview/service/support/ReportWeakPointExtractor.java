package com.hanserdev.interview.service.support;

import com.fasterxml.jackson.databind.JsonNode;

import java.util.ArrayList;
import java.util.List;

/**
 * 从面试报告 JSON 中提取薄弱点/待加强表述，供推荐与学习包复用。
 */
public final class ReportWeakPointExtractor {

    private ReportWeakPointExtractor() {
    }

    public static List<String> extractWeakPoints(JsonNode reportContent) {
        if (reportContent == null || reportContent.isNull() || reportContent.isMissingNode()) {
            return List.of();
        }
        List<String> weakPoints = new ArrayList<>();

        JsonNode questionAnalysis = reportContent.get("question_analysis");
        if (questionAnalysis != null && questionAnalysis.isArray()) {
            for (JsonNode qa : questionAnalysis) {
                JsonNode missed = qa.get("key_points_missed");
                if (missed != null && missed.isArray()) {
                    for (JsonNode point : missed) {
                        String pointText = point.asText();
                        if (!pointText.isEmpty() && !weakPoints.contains(pointText)) {
                            weakPoints.add(pointText);
                        }
                    }
                }
            }
        }

        if (weakPoints.isEmpty()) {
            JsonNode weaknesses = reportContent.get("weaknesses");
            if (weaknesses != null && weaknesses.isArray()) {
                for (JsonNode weakness : weaknesses) {
                    String weaknessText = weakness.asText();
                    if (!weaknessText.isEmpty()) {
                        weakPoints.add(weaknessText);
                    }
                }
            }
        }

        return weakPoints;
    }
}
