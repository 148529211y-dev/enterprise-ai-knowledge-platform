package com.ruoyi.ai.mapper;

import com.ruoyi.ai.entity.KbChunk;
import org.apache.ibatis.annotations.Param;
import java.util.List;

public interface KbChunkMapper {
    int insertBatch(@Param("list") List<KbChunk> chunks);
    List<KbChunk> selectByDocId(Long docId);
    int deleteByDocId(Long docId);
    List<KbChunk> selectAll();
}
