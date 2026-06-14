package com.feple.feple_backend.notification.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class UpdateNotificationPreferenceDto {
    private boolean certEnabled;
    private boolean commentEnabled;
    private boolean festivalEnabled;
    private boolean songRequestEnabled;
}
