package com.ruoyi.ai.mapper;

import com.ruoyi.ai.entity.AiBadCase;
import java.util.List;

public interface AiBadCaseMapper {
    int insert(AiBadCase badCase);
    List<AiBadCase> selectAll();
    int updateStatus(Long id, String status);
}
