package com.ruoyi.ai.service;

import com.ruoyi.ai.entity.KbDocument;
import com.ruoyi.ai.domain.query.KbDocumentQuery;
import com.ruoyi.common.core.domain.AjaxResult;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface KbService {

    List<KbDocument> listDocuments(KbDocumentQuery query);

    KbDocument getById(Long id);

    AjaxResult uploadAndProcess(MultipartFile file, String title, String createBy) throws Exception;

    void processDocument(KbDocument doc) throws Exception;

    void deleteDocument(Long docId);

    String reindexAll(Long userId);
}
