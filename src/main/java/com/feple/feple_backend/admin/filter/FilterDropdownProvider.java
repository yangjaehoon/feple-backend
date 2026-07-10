package com.feple.feple_backend.admin.filter;

import org.springframework.ui.Model;

public interface FilterDropdownProvider {
    String filterKey();
    void populate(Model model);
}
