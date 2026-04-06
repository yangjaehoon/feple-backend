package com.feple.feple_backend.auth.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.services.sesv2.SesV2Client;
import software.amazon.awssdk.services.sesv2.model.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class EmailService {

    private final SesV2Client sesV2Client;

    @Value("${app.mail.from}")
    private String fromEmail;

    public void sendPasswordResetEmail(String toEmail, String resetLink) {
        String html = """
                <!DOCTYPE html>
                <html>
                <body style="font-family: sans-serif; background:#f5f5f5; padding:40px 0;">
                  <div style="max-width:480px; margin:0 auto; background:#fff; border-radius:12px; padding:40px; box-shadow:0 2px 8px rgba(0,0,0,.08);">
                    <h2 style="color:#222; margin-top:0;">비밀번호 재설정</h2>
                    <p style="color:#555; line-height:1.6;">아래 버튼을 클릭하여 비밀번호를 재설정하세요.<br>링크는 <strong>1시간</strong> 동안 유효합니다.</p>
                    <a href="%s"
                       style="display:inline-block; margin-top:16px; padding:14px 28px;
                              background:#6C63FF; color:#fff; text-decoration:none;
                              border-radius:8px; font-weight:700; font-size:15px;">
                      비밀번호 재설정
                    </a>
                    <p style="margin-top:32px; font-size:12px; color:#aaa;">
                      본인이 요청하지 않은 경우 이 이메일을 무시하세요.
                    </p>
                  </div>
                </body>
                </html>
                """.formatted(resetLink);

        try {
            sesV2Client.sendEmail(SendEmailRequest.builder()
                    .fromEmailAddress(fromEmail)
                    .destination(Destination.builder().toAddresses(toEmail).build())
                    .content(EmailContent.builder()
                            .simple(Message.builder()
                                    .subject(Content.builder().data("[Feple] 비밀번호 재설정").charset("UTF-8").build())
                                    .body(Body.builder()
                                            .html(Content.builder().data(html).charset("UTF-8").build())
                                            .build())
                                    .build())
                            .build())
                    .build());
            log.info("[EmailService] 비밀번호 재설정 메일 발송: {}", toEmail);
        } catch (Exception e) {
            log.error("[EmailService] 메일 발송 실패: {}", e.getMessage());
            throw new IllegalStateException("이메일 발송에 실패했습니다. 잠시 후 다시 시도해주세요.");
        }
    }
}
