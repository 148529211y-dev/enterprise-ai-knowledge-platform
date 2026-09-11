package com.ruoyi.ai.mapper;

import com.ruoyi.ai.entity.AiConversation;
import org.apache.ibatis.annotations.Param;
import java.util.List;

public interface AiConversationMapper {
    int insert(AiConversation msg);
    List<AiConversation> selectBySession(@Param("userId") Long userId, @Param("sessionId") String sessionId);
    List<AiConversation> selectRecent(@Param("userId") Long userId, @Param("sessionId") String sessionId, @Param("limit") int limit);
}
