package com.ruoyi.ai.controller;

import com.ruoyi.ai.entity.KbDocument;
import com.ruoyi.ai.service.impl.KbService;
import com.ruoyi.common.core.controller.BaseController;
import com.ruoyi.common.core.domain.AjaxResult;
import com.ruoyi.common.core.page.TableDataInfo;
import com.ruoyi.common.utils.SecurityUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

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

    /**
     * 文档列表
     */
    @GetMapping("/list")
    public TableDataInfo list(KbDocument query) {
        startPage();
        List<KbDocument> list = kbService.listDocuments(query);
        return getDataTable(list);
    }

    /**
     * 文档详情
     */
    @GetMapping("/{id}")
    public AjaxResult detail(@PathVariable Long id) {
        return AjaxResult.success(kbService.getById(id));
    }

    /**
     * 上传文档
     *
     * 流程：上传 → 解析 → 切片 → Embedding → 向量存储
     */
    @PostMapping("/upload")
    public AjaxResult upload(@RequestParam("file") MultipartFile file,
                             @RequestParam(value = "title", required = false) String title) {
        try {
            String username = SecurityUtils.getUsername();
            KbDocument doc = kbService.uploadAndProcess(file, title, username);
            return AjaxResult.success("上传处理成功，共生成 " + doc.getChunkCount() + " 个切片", doc);
        } catch (Exception e) {
            return AjaxResult.error("上传处理失败: " + e.getMessage());
        }
    }

    /**
     * 删除文档
     */
    @DeleteMapping("/{id}")
    public AjaxResult delete(@PathVariable Long id) {
        kbService.deleteDocument(id);
        return AjaxResult.success();
    }
}
