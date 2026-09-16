package com.ruoyi.ai.mapper;

import com.ruoyi.ai.entity.BizApproval;
import org.apache.ibatis.annotations.Param;
import java.util.List;

public interface BizApprovalMapper {
    List<BizApproval> selectByApplicant(String applicant);
    List<BizApproval> selectAll();
}
