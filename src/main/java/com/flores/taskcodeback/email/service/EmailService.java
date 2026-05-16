package com.flores.taskcodeback.email.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmailService {

    private final JavaMailSender mailSender;

    @Value("${app.email.verification.from}")
    private String fromEmail;

    public void sendVerificationCode(String toEmail, String verificationCode, String userName) {
        try {
            MimeMessage mimeMessage = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, "UTF-8");
            helper.setFrom(fromEmail);
            helper.setTo(toEmail);
            helper.setSubject("TaskCodeBack | Codigo de verificacion");
            helper.setText(buildVerificationEmailHtml(userName, verificationCode), true);

            mailSender.send(mimeMessage);
            log.info("Código de verificación enviado exitosamente a: {}", toEmail);

        } catch (MessagingException e) {
            log.error("Error enviando código de verificación a {}: {}", toEmail, e.getMessage());
            throw new RuntimeException("Error enviando email de verificación", e);
        }
    }

    private String buildVerificationEmailHtml(String userName, String verificationCode) {
        String safeUserName = escapeHtml(userName);
        return String.format("""
            <!doctype html>
            <html lang=\"es\">
            <head>
              <meta charset=\"UTF-8\" />
              <meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0\" />
              <title>Codigo de verificacion</title>
            </head>
            <body style=\"margin:0;padding:0;background-color:#f5f7fb;font-family:Arial,sans-serif;color:#1f2937;\">
              <table role=\"presentation\" width=\"100%%\" cellspacing=\"0\" cellpadding=\"0\" style=\"padding:24px 0;\">
                <tr>
                  <td align=\"center\">
                    <table role=\"presentation\" width=\"600\" cellspacing=\"0\" cellpadding=\"0\" style=\"max-width:600px;background:#ffffff;border-radius:12px;overflow:hidden;border:1px solid #e5e7eb;\">
                      <tr>
                        <td style=\"background:#111827;padding:20px 24px;color:#ffffff;font-size:18px;font-weight:700;\">TaskCodeBack</td>
                      </tr>
                      <tr>
                        <td style=\"padding:28px 24px 8px 24px;font-size:16px;line-height:1.6;\">
                          Hola %s,<br/><br/>
                          Recibimos una solicitud para verificar tu correo en <strong>TaskCodeBack</strong>.
                          Usa el siguiente codigo de seguridad:
                        </td>
                      </tr>
                      <tr>
                        <td align=\"center\" style=\"padding:16px 24px;\">
                          <div style=\"display:inline-block;letter-spacing:6px;font-size:28px;font-weight:800;color:#111827;background:#f3f4f6;border:1px dashed #9ca3af;border-radius:10px;padding:14px 24px;\">%s</div>
                        </td>
                      </tr>
                      <tr>
                        <td style=\"padding:4px 24px 24px 24px;font-size:14px;line-height:1.7;color:#4b5563;\">
                          Este codigo expira en <strong>5 minutos</strong>.<br/>
                          Por seguridad, no lo compartas con nadie.<br/><br/>
                          Si no realizaste esta solicitud, puedes ignorar este mensaje.
                        </td>
                      </tr>
                      <tr>
                        <td style=\"padding:16px 24px;background:#f9fafb;border-top:1px solid #e5e7eb;font-size:12px;color:#6b7280;\">
                          Este es un correo automatico, por favor no responder.
                        </td>
                      </tr>
                    </table>
                  </td>
                </tr>
              </table>
            </body>
            </html>
            """, safeUserName, verificationCode);
    }

    private String escapeHtml(String value) {
        if (value == null) {
            return "Usuario";
        }
        return value
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&#39;");
    }
}
