package com.feple.feple_backend.admin;

import org.springframework.context.support.DefaultMessageSourceResolvable;
import org.springframework.validation.BindingResult;
import org.springframework.validation.ObjectError;

import java.util.List;

public final class BindingResultUtils {

    private BindingResultUtils() {}

    public static List<String> extractErrorMessages(BindingResult br) {
        return br.getAllErrors().stream()
                .map(DefaultMessageSourceResolvable::getDefaultMessage)
                .toList();
    }

    public static String firstError(BindingResult br) {
        List<ObjectError> errors = br.getAllErrors();
        return errors.isEmpty() ? "" : errors.get(0).getDefaultMessage();
    }
}
