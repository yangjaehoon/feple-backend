package com.feple.feple_backend.admin.appconfig;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.feple.feple_backend.admin.account.RequiresSuperAdmin;
import com.feple.feple_backend.admin.log.AdminAction;
import com.feple.feple_backend.admin.log.AdminLogService;
import com.feple.feple_backend.admin.support.AdminActionUtils;
import com.feple.feple_backend.admin.support.BindingResultUtils;
import com.feple.feple_backend.appconfig.FeatureFlags;
import com.feple.feple_backend.appconfig.dto.AppConfigFormDto;
import com.feple.feple_backend.appconfig.service.AppConfigAdminService;
import com.feple.feple_backend.global.exception.InvalidRequestException;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Slf4j
@PreAuthorize("hasRole('SUPER_ADMIN')")
@RequiresSuperAdmin
@Controller
@RequestMapping("/admin/app-config")
@RequiredArgsConstructor
public class AppConfigAdminController {

    private static final String VIEW = "admin/system/app-config";
    private static final String REDIRECT = "redirect:/admin/app-config";

    private final AppConfigAdminService appConfigAdminService;
    private final AdminLogService adminLogService;
    private final ObjectMapper objectMapper;

    @GetMapping
    public String showForm(Model model) {
        model.addAttribute("config", appConfigAdminService.getConfigForm());
        return VIEW;
    }

    @PostMapping
    public String updateConfig(@Valid @ModelAttribute("config") AppConfigFormDto form,
                               BindingResult bindingResult,
                               Model model,
                               RedirectAttributes ra) {
        rejectInvalidFeatureFlags(form, bindingResult);
        if (bindingResult.hasErrors()) {
            model.addAttribute("errors", BindingResultUtils.extractErrorMessages(bindingResult));
            return VIEW;
        }
        AdminActionUtils.tryAction(
                () -> {
                    appConfigAdminService.updateConfig(form);
                    adminLogService.log(AdminAction.APP_CONFIG_UPDATE, "APP_CONFIG", null,
                            "min=" + form.getMinSupportedVersion()
                                    + " latest=" + form.getLatestVersion()
                                    + " maintenance=" + form.isMaintenance());
                },
                "앱 설정이 저장되었습니다.",
                e -> log.error("앱 설정 저장 실패", e),
                "저장 중 오류가 발생했습니다.",
                ra);
        return REDIRECT;
    }

    // featureFlags JSON 오류도 버전 형식 오류와 동일하게 폼을 다시 렌더해 입력값을 보존한다
    // (서비스에서 예외를 던지면 tryAction이 redirect해 방금 입력한 다른 필드가 전부 날아간다).
    private void rejectInvalidFeatureFlags(AppConfigFormDto form, BindingResult bindingResult) {
        if (bindingResult.hasFieldErrors("featureFlags")) {
            return;
        }
        try {
            FeatureFlags.parse(objectMapper, form.getFeatureFlags());
        } catch (InvalidRequestException e) {
            bindingResult.rejectValue("featureFlags", "invalid", e.getMessage());
        }
    }
}
