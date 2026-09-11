package com.ruoyi.ai.mapper;

import com.ruoyi.ai.entity.KbDocument;
import java.util.List;

public interface KbDocumentMapper {
    KbDocument selectById(Long id);
    List<KbDocument> selectList(KbDocument query);
    int insert(KbDocument doc);
    int update(KbDocument doc);
    int deleteById(Long id);
}
