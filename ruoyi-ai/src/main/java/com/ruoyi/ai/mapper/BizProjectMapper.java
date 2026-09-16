package com.ruoyi.ai.mapper;

import com.ruoyi.ai.entity.BizProject;
import java.util.List;

public interface BizProjectMapper {
    BizProject selectById(Long id);
    List<BizProject> selectAll();
    List<BizProject> selectByName(String projectName);
}
