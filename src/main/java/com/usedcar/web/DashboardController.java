package com.usedcar.web;

import com.usedcar.dto.DashboardResponse;
import com.usedcar.service.DashboardService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class DashboardController {

    private final DashboardService dashboardService;

    public DashboardController(DashboardService dashboardService) {
        this.dashboardService = dashboardService;
    }

    /** ダッシュボード集計（月別粗利 / 在庫評価 / 区分内訳 / アラート） */
    @GetMapping("/dashboard")
    public DashboardResponse dashboard(@RequestParam(defaultValue = "12") int months) {
        return dashboardService.dashboard(months);
    }
}
