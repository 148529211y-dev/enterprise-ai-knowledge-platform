package com.ruoyi.ai.controller;

import com.ruoyi.ai.domain.query.KbDocumentQuery;
import com.ruoyi.ai.domain.vo.KbDocumentVO;
import com.ruoyi.ai.entity.AsyncTask;
import com.ruoyi.ai.entity.KbDocument;
import com.ruoyi.ai.service.AsyncTaskService;
import com.ruoyi.ai.service.KbService;
import com.ruoyi.common.core.controller.BaseController;
import com.ruoyi.common.core.domain.AjaxResult;
import com.ruoyi.common.core.page.TableDataInfo;
import com.ruoyi.common.utils.SecurityUtils;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/ai/kb")
public class KbController extends BaseController {

    private final KbService kbService;
    private final AsyncTaskService asyncTaskService;

    public KbController(KbService kbService, AsyncTaskService asyncTaskService) {
        this.kbService = kbService;
        this.asyncTaskService = asyncTaskService;
    }

    @PreAuthorize("@ss.hasPermi('ai:kb:list')")
    @GetMapping("/list")
    public TableDataInfo list(KbDocumentQuery query) {
        startPage();
        List<KbDocument> list = kbService.listDocuments(query);
        List<KbDocumentVO> voList = list.stream()
                .map(KbDocumentVO::fromEntity)
                .collect(Collectors.toList());
        return getDataTable(voList);
    }

    @PreAuthorize("@ss.hasPermi('ai:kb:list')")
    @GetMapping("/{id}")
    public AjaxResult detail(@PathVariable Long id) {
        KbDocument doc = kbService.getById(id);
        return AjaxResult.success(doc != null ? KbDocumentVO.fromEntity(doc) : null);
    }

    @PreAuthorize("@ss.hasPermi('ai:kb:upload')")
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

    @PreAuthorize("@ss.hasPermi('ai:kb:delete')")
    @DeleteMapping("/{id}")
    public AjaxResult delete(@PathVariable Long id) {
        kbService.deleteDocument(id);
        return AjaxResult.success();
    }

    @PreAuthorize("@ss.hasPermi('ai:kb:list')")
    @GetMapping("/task/{taskId}")
    public AjaxResult taskStatus(@PathVariable String taskId) {
        AsyncTask task = asyncTaskService.getTaskStatus(taskId);
        return AjaxResult.success(task);
    }

    @PreAuthorize("@ss.hasPermi('ai:kb:upload')")
    @PostMapping("/reindex")
    public AjaxResult reindex() {
        Long userId = SecurityUtils.getUserId();
        String taskId = kbService.reindexAll(userId);
        return AjaxResult.success("reindex submitted", taskId);
    }
}
