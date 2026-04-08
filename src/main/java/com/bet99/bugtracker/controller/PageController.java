package com.bet99.bugtracker.controller;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class PageController {

    @Value("${api.baseUrl:}")
    private String apiBaseUrl;

    @GetMapping("/")
    public String home(Model model) {
        model.addAttribute("apiBaseUrl", apiBaseUrl == null ? "" : apiBaseUrl);
        return "bugs";
    }
}

