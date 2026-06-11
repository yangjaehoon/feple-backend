package com.feple.feple_backend.admin;

import org.springframework.ui.Model;

public interface FilterDropdownProvider {
    String filter();
    void populate(Model model);
}
