package com.hanserdev.interview.model.dto.template;

import lombok.Data;

import java.util.List;

@Data
public class PlanStructureDTO {

    private List<QuestionItem> questions;
    private int totalQuestions;
    private TransitionRules transitionRules;
    private int estimatedDuration;

    @Data
    public static class QuestionItem {
        private String topic;
        private String category;
        private int maxDepth;
        private String questionId;
        private int timeAllocation;
        private String initialQuestion;
        private EvaluationCriteria evaluationCriteria;
    }

    @Data
    public static class EvaluationCriteria {
        private String difficulty;
        private List<String> keyPoints;
    }

    @Data
    public static class TransitionRules {
        private List<String> autoEndConditions;
        private double minScoreToProceed;
    }
}
