package com.feple.feple_backend.admin;

import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/admin")
public class AdminLoginController {

    @GetMapping("/login")
    public String loginPage(HttpSession session, Model model) {
        String loginError = (String) session.getAttribute(AdminLoginFailureHandler.SESSION_KEY);
        if (loginError != null) {
            model.addAttribute("loginError", loginError);
            session.removeAttribute(AdminLoginFailureHandler.SESSION_KEY);
        }
        return "admin/login";
    }

    @GetMapping("/access-denied")
    public String accessDenied() {
        return "admin/admin-access-denied";
    }
}
