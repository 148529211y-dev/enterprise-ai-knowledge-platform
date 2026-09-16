package com.ruoyi.ai.mapper;

import com.ruoyi.ai.entity.AiFeedback;
import java.util.List;

public interface AiFeedbackMapper {
    int insert(AiFeedback feedback);
    List<AiFeedback> selectAll();
}
