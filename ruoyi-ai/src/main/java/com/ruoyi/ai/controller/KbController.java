package com.ruoyi.ai.controller;

import com.ruoyi.ai.domain.dto.ChatRequest;
import com.ruoyi.ai.domain.dto.ChatResponse;
import com.ruoyi.ai.domain.query.KbDocumentQuery;
import com.ruoyi.ai.domain.vo.KbDocumentVO;
import com.ruoyi.ai.entity.KbDocument;
import com.ruoyi.ai.service.KbService;
import com.ruoyi.common.core.controller.BaseController;
import com.ruoyi.common.core.domain.AjaxResult;
import com.ruoyi.common.core.page.TableDataInfo;
import com.ruoyi.common.utils.SecurityUtils;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 知识库文档管理接口
 */
@RestController
@RequestMapping("/ai/kb")
public class KbController extends BaseController {

    private final KbService kbService;

    public KbController(KbService kbService) {
        this.kbService = kbService;
    }

    /** 文档列表 —— 使用 Query 对象接收查询条件，返回 VO */
    @GetMapping("/list")
    public TableDataInfo list(KbDocumentQuery query) {
        startPage();
        List<KbDocument> list = kbService.listDocuments(query);
        List<KbDocumentVO> voList = list.stream()
                .map(KbDocumentVO::fromEntity)
                .collect(Collectors.toList());
        return getDataTable(voList);
    }

    /** 文档详情 */
    @GetMapping("/{id}")
    public AjaxResult detail(@PathVariable Long id) {
        KbDocument doc = kbService.getById(id);
        return AjaxResult.success(doc != null ? KbDocumentVO.fromEntity(doc) : null);
    }

    /** 上传文档（异步处理） */
    @PostMapping("/upload")
    public AjaxResult upload(@RequestParam("file") MultipartFile file,
                             @RequestParam(value = "title", required = false) String title) {
        try {
            String username = SecurityUtils.getUsername();
            AjaxResult result = kbService.uploadAndProcess(file, title, username);
            return result;
        } catch (Exception e) {
            return AjaxResult.error("上传处理失败: " + e.getMessage());
        }
    }

    /** 删除文档 */
    @DeleteMapping("/{id}")
    public AjaxResult delete(@PathVariable Long id) {
        kbService.deleteDocument(id);
        return AjaxResult.success();
    }
}
