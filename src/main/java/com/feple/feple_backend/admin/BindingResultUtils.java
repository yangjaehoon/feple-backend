package com.feple.feple_backend.admin;

import java.util.List;
import org.springframework.context.support.DefaultMessageSourceResolvable;
import org.springframework.validation.BindingResult;
import org.springframework.validation.ObjectError;

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
