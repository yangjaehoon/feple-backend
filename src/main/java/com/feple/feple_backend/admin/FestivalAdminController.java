package com.feple.feple_backend.admin;

import com.feple.feple_backend.festival.dto.FestivalRequestDto;
import com.feple.feple_backend.festival.dto.FestivalResponseDto;
import com.feple.feple_backend.festival.service.FestivalService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
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

    @GetMapping("/{id}/edit")
    public String showEditForm(@PathVariable Long id, Model model) {
        FestivalResponseDto festival = festivalService.getFestival(id);
        FestivalRequestDto form = new FestivalRequestDto();
        form.setTitle(festival.getTitle());
        form.setDescription(festival.getDescription());
        form.setLocation(festival.getLocation());
        form.setStartDate(festival.getStartDate());
        form.setEndDate(festival.getEndDate());
        form.setPosterUrl(festival.getPosterUrl());

        model.addAttribute("festivalId", id);
        model.addAttribute("festival", form);
        return "admin/festival--edit-form";
    }

    @PostMapping("/{id}/edit")
    public String updateFestival(FestivalRequestDto dto, @PathVariable Long id){
        festivalService.updateFestvial(id, dto);
        return "redirect:/admin/festivals";
    }

    @PostMapping("/{id}/delete")
    public String deleteFestival(@PathVariable Long id){
        festivalService.deleteFestival(id);
        return "redirect:/admin/festivals";
    }


}
