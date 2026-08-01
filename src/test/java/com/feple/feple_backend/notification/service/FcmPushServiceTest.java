package com.feple.feple_backend.notification.service;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.feple.feple_backend.notification.entity.NotificationType;
import com.feple.feple_backend.user.service.DeviceTokenService;
import com.google.firebase.FirebaseApp;
import com.google.firebase.messaging.BatchResponse;
import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.FirebaseMessagingException;
import com.google.firebase.messaging.MessagingErrorCode;
import com.google.firebase.messaging.MulticastMessage;
import com.google.firebase.messaging.SendResponse;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class FcmPushServiceTest {

    @Mock DeviceTokenService deviceTokenService;
    @Mock FirebaseMessaging messaging;

    @InjectMocks FcmPushService service;

    @Test
    void sendBroadcast_토큰_비어있으면_아무것도_안함() {
        try (MockedStatic<FirebaseApp> appMock = mockStatic(FirebaseApp.class)) {
            service.sendBroadcast(List.of(), "제목", "내용");

            appMock.verifyNoInteractions();
        }
    }

    @Test
    void sendBroadcast_Firebase_미초기화면_생략() {
        try (MockedStatic<FirebaseApp> appMock = mockStatic(FirebaseApp.class)) {
            appMock.when(FirebaseApp::getApps).thenReturn(List.of());

            service.sendBroadcast(List.of("token1"), "제목", "내용");

            verify(deviceTokenService, never()).deleteStaleTokens(any());
        }
    }

    @Test
    void sendBroadcast_정상발송() throws Exception {
        BatchResponse response = mock(BatchResponse.class);
        given(response.getResponses()).willReturn(List.of());
        given(messaging.sendEachForMulticast(any(MulticastMessage.class))).willReturn(response);

        try (MockedStatic<FirebaseApp> appMock = mockStatic(FirebaseApp.class);
                MockedStatic<FirebaseMessaging> messagingMock = mockStatic(FirebaseMessaging.class)) {
            appMock.when(FirebaseApp::getApps).thenReturn(List.of(mock(FirebaseApp.class)));
            messagingMock.when(FirebaseMessaging::getInstance).thenReturn(messaging);

            service.sendBroadcast(List.of("token1"), "제목", "내용");

            verify(deviceTokenService, never()).deleteStaleTokens(any());
        }
    }

    @Test
    void sendBroadcast_실패토큰_UNREGISTERED면_삭제() throws Exception {
        FirebaseMessagingException ex = mock(FirebaseMessagingException.class);
        given(ex.getMessagingErrorCode()).willReturn(MessagingErrorCode.UNREGISTERED);
        SendResponse failed = mock(SendResponse.class);
        given(failed.isSuccessful()).willReturn(false);
        given(failed.getException()).willReturn(ex);
        BatchResponse response = mock(BatchResponse.class);
        given(response.getResponses()).willReturn(List.of(failed));
        given(messaging.sendEachForMulticast(any(MulticastMessage.class))).willReturn(response);

        try (MockedStatic<FirebaseApp> appMock = mockStatic(FirebaseApp.class);
                MockedStatic<FirebaseMessaging> messagingMock = mockStatic(FirebaseMessaging.class)) {
            appMock.when(FirebaseApp::getApps).thenReturn(List.of(mock(FirebaseApp.class)));
            messagingMock.when(FirebaseMessaging::getInstance).thenReturn(messaging);

            service.sendBroadcast(List.of("stale-token"), "제목", "내용");

            verify(deviceTokenService).deleteStaleTokens(List.of("stale-token"));
        }
    }

    @Test
    void sendBroadcast_실패토큰_원인이_다르면_삭제안함() throws Exception {
        FirebaseMessagingException ex = mock(FirebaseMessagingException.class);
        given(ex.getMessagingErrorCode()).willReturn(MessagingErrorCode.INTERNAL);
        SendResponse failed = mock(SendResponse.class);
        given(failed.isSuccessful()).willReturn(false);
        given(failed.getException()).willReturn(ex);
        BatchResponse response = mock(BatchResponse.class);
        given(response.getResponses()).willReturn(List.of(failed));
        given(messaging.sendEachForMulticast(any(MulticastMessage.class))).willReturn(response);

        try (MockedStatic<FirebaseApp> appMock = mockStatic(FirebaseApp.class);
                MockedStatic<FirebaseMessaging> messagingMock = mockStatic(FirebaseMessaging.class)) {
            appMock.when(FirebaseApp::getApps).thenReturn(List.of(mock(FirebaseApp.class)));
            messagingMock.when(FirebaseMessaging::getInstance).thenReturn(messaging);

            service.sendBroadcast(List.of("other-fail-token"), "제목", "내용");

            verify(deviceTokenService, never()).deleteStaleTokens(any());
        }
    }

    @Test
    void sendBroadcast_발송중_예외발생시_전파되지않음() throws Exception {
        given(messaging.sendEachForMulticast(any(MulticastMessage.class)))
                .willThrow(mock(FirebaseMessagingException.class));

        try (MockedStatic<FirebaseApp> appMock = mockStatic(FirebaseApp.class);
                MockedStatic<FirebaseMessaging> messagingMock = mockStatic(FirebaseMessaging.class)) {
            appMock.when(FirebaseApp::getApps).thenReturn(List.of(mock(FirebaseApp.class)));
            messagingMock.when(FirebaseMessaging::getInstance).thenReturn(messaging);

            assertThatCode(() -> service.sendBroadcast(List.of("token1"), "제목", "내용"))
                    .doesNotThrowAnyException();
        }
    }

    @Test
    void sendMulticast_PushMessage_기반_발송() throws Exception {
        BatchResponse response = mock(BatchResponse.class);
        given(response.getResponses()).willReturn(List.of());
        given(messaging.sendEachForMulticast(any(MulticastMessage.class))).willReturn(response);
        PushMessage message = new PushMessage("제목", "내용", "10", NotificationType.NEW_COMMENT, null);

        try (MockedStatic<FirebaseApp> appMock = mockStatic(FirebaseApp.class);
                MockedStatic<FirebaseMessaging> messagingMock = mockStatic(FirebaseMessaging.class)) {
            appMock.when(FirebaseApp::getApps).thenReturn(List.of(mock(FirebaseApp.class)));
            messagingMock.when(FirebaseMessaging::getInstance).thenReturn(messaging);

            service.sendMulticast(List.of("token1"), message);

            verify(messaging).sendEachForMulticast(any(MulticastMessage.class));
        }
    }

    @Test
    void sendMulticast_imageUrl_있어도_정상_발송된다() throws Exception {
        BatchResponse response = mock(BatchResponse.class);
        given(response.getResponses()).willReturn(List.of());
        given(messaging.sendEachForMulticast(any(MulticastMessage.class))).willReturn(response);
        PushMessage message = new PushMessage(
                "제목", "내용", "10", NotificationType.NEW_FESTIVAL, "https://cdn.feple.com/poster.jpg");

        try (MockedStatic<FirebaseApp> appMock = mockStatic(FirebaseApp.class);
                MockedStatic<FirebaseMessaging> messagingMock = mockStatic(FirebaseMessaging.class)) {
            appMock.when(FirebaseApp::getApps).thenReturn(List.of(mock(FirebaseApp.class)));
            messagingMock.when(FirebaseMessaging::getInstance).thenReturn(messaging);

            assertThatCode(() -> service.sendMulticast(List.of("token1"), message))
                    .doesNotThrowAnyException();

            verify(messaging).sendEachForMulticast(any(MulticastMessage.class));
        }
    }
}
