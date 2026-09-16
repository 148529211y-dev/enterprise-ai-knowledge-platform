package com.ruoyi.ai.service.evaluation;

import com.ruoyi.ai.entity.AiBadCase;
import com.ruoyi.ai.entity.AiEvaluation;
import com.ruoyi.ai.entity.AiFeedback;
import com.ruoyi.ai.mapper.AiBadCaseMapper;
import com.ruoyi.ai.mapper.AiEvaluationMapper;
import com.ruoyi.ai.mapper.AiFeedbackMapper;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class EvaluationService {

    private final AiFeedbackMapper aiFeedbackMapper;
    private final AiEvaluationMapper aiEvaluationMapper;
    private final AiBadCaseMapper aiBadCaseMapper;

    public EvaluationService(AiFeedbackMapper aiFeedbackMapper, AiEvaluationMapper aiEvaluationMapper, AiBadCaseMapper aiBadCaseMapper) {
        this.aiFeedbackMapper = aiFeedbackMapper;
        this.aiEvaluationMapper = aiEvaluationMapper;
        this.aiBadCaseMapper = aiBadCaseMapper;
    }

    public void submitFeedback(AiFeedback feedback) {
        aiFeedbackMapper.insert(feedback);
    }

    public void recordEvaluation(AiEvaluation eval) {
        aiEvaluationMapper.insert(eval);
    }

    public void addBadCase(AiBadCase badCase) {
        aiBadCaseMapper.insert(badCase);
    }

    public void fixBadCase(Long id) {
        aiBadCaseMapper.updateStatus(id, "FIXED");
    }

    public List<AiBadCase> getBadCases() {
        return aiBadCaseMapper.selectAll();
    }

    public Map<String, Object> getFeedbackStats() {
        List<AiFeedback> feedbacks = aiFeedbackMapper.selectAll();
        Map<String, Object> stats = new HashMap<>();
        if (feedbacks.isEmpty()) {
            stats.put("avgScore", 0);
            stats.put("totalCount", 0);
            return stats;
        }
        double totalScore = 0;
        for (AiFeedback f : feedbacks) {
            if (f.getScore() != null) {
                totalScore += f.getScore();
            }
        }
        stats.put("avgScore", totalScore / feedbacks.size());
        stats.put("totalCount", feedbacks.size());
        return stats;
    }
}
