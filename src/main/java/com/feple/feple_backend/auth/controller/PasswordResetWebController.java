package com.feple.feple_backend.auth.controller;

import com.feple.feple_backend.auth.service.PasswordResetService;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
@RequestMapping("/reset-password")
@RequiredArgsConstructor
public class PasswordResetWebController {

    private final PasswordResetService passwordResetService;

    /** 비밀번호 재설정 폼 표시 */
    @GetMapping
    public String showForm(@RequestParam String token, Model model) {
        if (!passwordResetService.isValidToken(token)) {
            model.addAttribute("error", "유효하지 않거나 만료된 링크입니다. 비밀번호 재설정을 다시 요청해주세요.");
            return "reset-password";
        }
        model.addAttribute("token", token);
        return "reset-password";
    }

    /** 비밀번호 재설정 처리 */
    @PostMapping
    public String processReset(
            @RequestParam String token,
            @RequestParam String newPassword,
            @RequestParam String confirmPassword,
            Model model
    ) {
        model.addAttribute("token", token);

        if (!newPassword.equals(confirmPassword)) {
            model.addAttribute("error", "비밀번호가 일치하지 않습니다.");
            return "reset-password";
        }

        if (newPassword.length() < 8 || !newPassword.matches("^(?=.*[A-Za-z])(?=.*\\d).+$")) {
            model.addAttribute("error", "비밀번호는 8자 이상, 영문자와 숫자를 모두 포함해야 합니다.");
            return "reset-password";
        }

        try {
            passwordResetService.resetPassword(token, newPassword);
            model.addAttribute("success", true);
        } catch (IllegalArgumentException e) {
            model.addAttribute("error", e.getMessage());
        }

        return "reset-password";
    }
}
