package com.ruoyi.ai.mapper;

import com.ruoyi.ai.entity.AiEvaluation;
import java.util.List;

public interface AiEvaluationMapper {
    int insert(AiEvaluation eval);
    List<AiEvaluation> selectAll();
}
