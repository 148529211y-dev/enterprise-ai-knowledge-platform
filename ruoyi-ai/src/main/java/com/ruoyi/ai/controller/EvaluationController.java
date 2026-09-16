package com.ruoyi.ai.controller;

import com.ruoyi.ai.entity.AiFeedback;
import com.ruoyi.ai.service.evaluation.EvaluationService;
import com.ruoyi.common.core.controller.BaseController;
import com.ruoyi.common.core.domain.AjaxResult;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/ai/evaluation")
public class EvaluationController extends BaseController {

    private final EvaluationService evaluationService;

    public EvaluationController(EvaluationService evaluationService) {
        this.evaluationService = evaluationService;
    }

    @PostMapping("/feedback")
    public AjaxResult submitFeedback(@RequestBody AiFeedback feedback) {
        evaluationService.submitFeedback(feedback);
        return AjaxResult.success();
    }

    @GetMapping("/bad-cases")
    public AjaxResult getBadCases() {
        return AjaxResult.success(evaluationService.getBadCases());
    }

    @PostMapping("/bad-cases/{id}/fix")
    public AjaxResult fixBadCase(@PathVariable Long id) {
        evaluationService.fixBadCase(id);
        return AjaxResult.success();
    }

    @GetMapping("/stats")
    public AjaxResult getStats() {
        return AjaxResult.success(evaluationService.getFeedbackStats());
    }
}
