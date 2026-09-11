package com.ruoyi.ai.service;

import com.ruoyi.ai.entity.KbDocument;
import com.ruoyi.ai.domain.query.KbDocumentQuery;
import com.ruoyi.common.core.domain.AjaxResult;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * 知识库文档管理服务接口
 */
public interface KbService {

    /** 文档列表查询 */
    List<KbDocument> listDocuments(KbDocumentQuery query);

    /** 文档详情 */
    KbDocument getById(Long id);

    /** 上传并处理文档（异步） */
    AjaxResult uploadAndProcess(MultipartFile file, String title, String createBy) throws Exception;

    /** 同步处理文档（解析→切片→向量化） */
    void processDocument(KbDocument doc) throws Exception;

    /** 删除文档 */
    void deleteDocument(Long docId);
}
