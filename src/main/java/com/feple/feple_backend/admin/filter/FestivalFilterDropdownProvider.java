package com.feple.feple_backend.admin.filter;

import com.feple.feple_backend.festival.service.FestivalAdminService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.ui.Model;

@Component
@RequiredArgsConstructor
class FestivalFilterDropdownProvider implements FilterDropdownProvider {

    private final FestivalAdminService festivalService;

    @Override public String filter() { return "FESTIVAL"; }

    @Override public void populate(Model model) {
        model.addAttribute("festivals", festivalService.getAllFestivalsForAdmin());
    }
}
