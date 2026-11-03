package com.pulsos.medicina.controller;

import com.pulsos.medicina.service.DashboardService;

import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class DashboardController {

    private final DashboardService dashboardService;

    public DashboardController(DashboardService dashboardService) {
        this.dashboardService = dashboardService;
    }

    @GetMapping("/dashboard")
    public String dashboard(
            Authentication authentication,
            Model model) {

        if (authentication == null
                || !authentication.isAuthenticated()) {
            return "redirect:/login";
        }

        DashboardService.ResumenDashboard resumen =
                dashboardService.obtenerResumen(
                        authentication.getName());

        model.addAttribute("resumen", resumen);
        model.addAttribute("turnos", resumen.turnos());
        model.addAttribute("esAdministrador", resumen.administrador());

        return "dashboard";
    }
}
