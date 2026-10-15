package com.org.tailSafe.domain.dashboard;

import com.org.tailSafe.core.security.UserPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/dashboard")
@RequiredArgsConstructor
public class DashboardController {

    @Autowired
    private DashboardService dashboardService;

    @GetMapping("/stats")
    public ResponseEntity<UserDashboardStats> getUserDashboardStats(
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(dashboardService.getUserDashboardStats(principal.getUserId()));
    }

    @GetMapping("/stats/global")
    @org.springframework.security.access.prepost.PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<com.org.tailSafe.domain.dashboard.DashboardStats> getGlobalDashboardStats() {
        return ResponseEntity.ok(dashboardService.getDashboardStats());
    }

}
