package com.feple.feple_backend.notification.service;

import java.util.List;

public interface PushNotificationClient {
    void sendBroadcast(List<String> tokens, String title, String body);
    void sendMulticast(List<String> tokens, PushMessage message);
}
