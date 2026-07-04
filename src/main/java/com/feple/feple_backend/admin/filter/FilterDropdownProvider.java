package com.feple.feple_backend.admin.filter;

import org.springframework.ui.Model;

public interface FilterDropdownProvider {
    String filter();
    void populate(Model model);
}
