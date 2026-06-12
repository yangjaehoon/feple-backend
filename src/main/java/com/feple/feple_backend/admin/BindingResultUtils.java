package com.feple.feple_backend.admin;

import org.springframework.context.support.DefaultMessageSourceResolvable;
import org.springframework.validation.BindingResult;

import java.util.List;

public final class BindingResultUtils {

    private BindingResultUtils() {}

    public static List<String> extractErrorMessages(BindingResult br) {
        return br.getAllErrors().stream()
                .map(DefaultMessageSourceResolvable::getDefaultMessage)
                .toList();
    }

    public static String firstError(BindingResult br) {
        return br.getAllErrors().get(0).getDefaultMessage();
    }
}
