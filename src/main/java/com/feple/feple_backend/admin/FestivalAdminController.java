package com.feple.feple_backend.admin;

import com.feple.feple_backend.festival.dto.FestivalRequestDto;
import com.feple.feple_backend.festival.dto.FestivalResponseDto;
import com.feple.feple_backend.festival.service.FestivalService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.List;

@Controller
@RequiredArgsConstructor
@RequestMapping("/admin/festivals")
public class FestivalAdminController {

    private final FestivalService festivalService;

    @GetMapping("/new")
    public String showCreateForm(Model model) {
        model.addAttribute("festival", new FestivalRequestDto());
        return "admin/festival-form";
    }

    @PostMapping("/new")
    public String createFestival(FestivalRequestDto dto){
        festivalService.createFestival(dto);
        return "redirect:/admin/festivals/new?success";
    }

    //목록 페이지
    @GetMapping
    public String listFestivals(Model model) {
        List<FestivalResponseDto> festivals = festivalService.getAllFestivals();
        model.addAttribute("festivals",festivals);
        return "admin/festival-list";
    }

}
