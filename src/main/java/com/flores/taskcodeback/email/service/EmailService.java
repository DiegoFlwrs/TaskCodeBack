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

    // Colores alineados con app/globals.css (TaskCode TAD)
    private static final String COLOR_TAD_GREEN = "#7CA436";
    private static final String COLOR_TAD_BLACK = "#333333";
    private static final String COLOR_BACKGROUND = "#F9F9F9";
    private static final String COLOR_FOREGROUND = "#212121";
    private static final String COLOR_MUTED = "#666666";
    private static final String COLOR_BORDER = "#E5E5E5";
    private static final String COLOR_SECONDARY = "#F0F3EB";
    private static final String COLOR_CARD = "#FFFFFF";
    private static final String FONT_FAMILY = "Arial, Helvetica, sans-serif";

    private final JavaMailSender mailSender;

    @Value("${app.email.verification.from}")
    private String fromEmail;

    public void sendVerificationCode(String toEmail, String verificationCode, String userName) {
        try {
            MimeMessage mimeMessage = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, "UTF-8");
            helper.setFrom(fromEmail);
            helper.setTo(toEmail);
            helper.setSubject("TaskCode TAD · Codigo de verificacion");
            helper.setText(buildVerificationEmailHtml(userName, verificationCode), true);
            mailSender.send(mimeMessage);
            log.info("Código de verificación enviado exitosamente a: {}", toEmail);
        } catch (MessagingException e) {
            log.error("Error enviando código de verificación a {}: {}", toEmail, e.getMessage());
            throw new RuntimeException("Error enviando email de verificación", e);
        }
    }

    public void sendWelcomeMemberEmail(String toEmail, String memberName, String teamName,
                                       String leaderName, String password) {
        try {
            MimeMessage mimeMessage = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, "UTF-8");
            helper.setFrom(fromEmail);
            helper.setTo(toEmail);
            helper.setSubject("TaskCode TAD · Bienvenido a " + teamName);
            helper.setText(buildWelcomeEmailHtml(memberName, teamName, leaderName, toEmail, password), true);
            mailSender.send(mimeMessage);
            log.info("Email de bienvenida enviado a: {}", toEmail);
        } catch (MessagingException e) {
            log.error("Error enviando email de bienvenida a {}: {}", toEmail, e.getMessage());
            throw new RuntimeException("Error enviando email de bienvenida", e);
        }
    }

    private String buildWelcomeEmailHtml(String memberName, String teamName,
                                          String leaderName, String email, String password) {
        String safeName = escapeHtml(memberName);
        String safeTeam = escapeHtml(teamName);
        String safeLeader = escapeHtml(leaderName);
        String safeEmail = escapeHtml(email);
        String safePass = escapeHtml(password);

        String content = String.format("""
            <tr>
              <td style="padding:28px 24px 8px 24px;font-size:16px;line-height:1.6;color:%s;">
                Hola %s,<br/><br/>
                <strong>%s</strong> te ha añadido al equipo <strong>%s</strong> en <strong>TaskCode TAD</strong>.<br/>
                Ya puedes acceder con las siguientes credenciales:
              </td>
            </tr>
            <tr>
              <td style="padding:16px 24px;">
                <table role="presentation" width="100%%" cellspacing="0" cellpadding="0" style="background:%s;border:1px solid %s;border-left:4px solid %s;border-radius:8px;overflow:hidden;">
                  <tr>
                    <td style="padding:14px 20px;border-bottom:1px solid %s;">
                      <span style="font-size:12px;color:%s;text-transform:uppercase;letter-spacing:1px;font-weight:700;">Usuario</span>
                      <p style="margin:4px 0 0 0;font-size:15px;font-weight:600;color:%s;">%s</p>
                    </td>
                  </tr>
                  <tr>
                    <td style="padding:14px 20px;">
                      <span style="font-size:12px;color:%s;text-transform:uppercase;letter-spacing:1px;font-weight:700;">Contraseña temporal</span>
                      <p style="margin:4px 0 0 0;font-size:22px;font-weight:800;color:%s;letter-spacing:4px;font-family:monospace;">%s</p>
                    </td>
                  </tr>
                </table>
              </td>
            </tr>
            <tr>
              <td style="padding:4px 24px 24px 24px;font-size:14px;line-height:1.7;color:%s;">
                Te recomendamos cambiar tu contraseña después del primer ingreso.<br/><br/>
                Por seguridad, no compartas estas credenciales con nadie.
              </td>
            </tr>
            """,
                COLOR_FOREGROUND, safeName, safeLeader, safeTeam,
                COLOR_SECONDARY, COLOR_TAD_GREEN, COLOR_TAD_GREEN, COLOR_BORDER,
                COLOR_MUTED, COLOR_FOREGROUND, safeEmail,
                COLOR_MUTED, COLOR_FOREGROUND, safePass,
                COLOR_MUTED);

        return buildEmailLayout("Bienvenido a TaskCode TAD", content);
    }

    private String buildVerificationEmailHtml(String userName, String verificationCode) {
        String safeUserName = escapeHtml(userName);
        String safeCode = escapeHtml(verificationCode);

        String content = String.format("""
            <tr>
              <td style="padding:28px 24px 8px 24px;font-size:16px;line-height:1.6;color:%s;">
                Hola %s,<br/><br/>
                Recibimos una solicitud para verificar tu correo en <strong>TaskCode TAD</strong>.
                Usa el siguiente codigo de seguridad:
              </td>
            </tr>
            <tr>
              <td align="center" style="padding:16px 24px;">
                <div style="display:inline-block;letter-spacing:6px;font-size:28px;font-weight:800;color:%s;background:%s;border:1px solid %s;border-left:4px solid %s;border-radius:8px;padding:14px 28px;font-family:monospace;">%s</div>
              </td>
            </tr>
            <tr>
              <td style="padding:4px 24px 24px 24px;font-size:14px;line-height:1.7;color:%s;">
                Este codigo expira en <strong style="color:%s;">5 minutos</strong>.<br/>
                Por seguridad, no lo compartas con nadie.<br/><br/>
                Si no realizaste esta solicitud, puedes ignorar este mensaje.
              </td>
            </tr>
            """,
                COLOR_FOREGROUND, safeUserName,
                COLOR_FOREGROUND, COLOR_SECONDARY, COLOR_TAD_GREEN, COLOR_TAD_GREEN, safeCode,
                COLOR_MUTED, COLOR_TAD_GREEN);

        return buildEmailLayout("Codigo de verificacion", content);
    }

    private String buildEmailLayout(String title, String contentHtml) {
        return String.format("""
            <!doctype html>
            <html lang="es">
            <head>
              <meta charset="UTF-8" />
              <meta name="viewport" content="width=device-width, initial-scale=1.0" />
              <title>%s</title>
            </head>
            <body style="margin:0;padding:0;background-color:%s;font-family:%s;color:%s;">
              <table role="presentation" width="100%%" cellspacing="0" cellpadding="0" style="padding:24px 0;">
                <tr>
                  <td align="center">
                    <table role="presentation" width="600" cellspacing="0" cellpadding="0" style="max-width:600px;background:%s;border-radius:8px;overflow:hidden;border:1px solid %s;">
                      %s
                      %s
                      %s
                    </table>
                  </td>
                </tr>
              </table>
            </body>
            </html>
            """,
                escapeHtml(title),
                COLOR_BACKGROUND, FONT_FAMILY, COLOR_FOREGROUND,
                COLOR_CARD, COLOR_BORDER,
                buildEmailHeader(),
                contentHtml,
                buildEmailFooter());
    }

    private String buildEmailHeader() {
        return String.format("""
            <tr>
              <td style="background:%s;padding:20px 24px;">
                <table role="presentation" cellspacing="0" cellpadding="0">
                  <tr>
                    <td style="background:%s;border-radius:8px;padding:8px 10px;text-align:center;vertical-align:middle;">
                      <div style="color:#ffffff;font-weight:900;font-size:11px;line-height:1.1;font-family:%s;">TAD</div>
                      <div style="color:rgba(255,255,255,0.9);font-weight:900;font-size:9px;line-height:1.1;font-family:%s;">CODE</div>
                    </td>
                    <td style="padding-left:12px;vertical-align:middle;">
                      <span style="color:#ffffff;font-size:16px;font-weight:600;font-family:%s;">TaskCode </span>
                      <span style="color:%s;font-size:16px;font-weight:900;font-family:%s;">TAD</span>
                    </td>
                  </tr>
                </table>
              </td>
            </tr>
            """,
                COLOR_TAD_BLACK, COLOR_TAD_GREEN, FONT_FAMILY, FONT_FAMILY,
                FONT_FAMILY, COLOR_TAD_GREEN, FONT_FAMILY);
    }

    private String buildEmailFooter() {
        return String.format("""
            <tr>
              <td style="padding:16px 24px;background:%s;border-top:1px solid %s;font-size:12px;color:%s;">
                Este es un correo automatico de TaskCode TAD, por favor no responder.
              </td>
            </tr>
            """,
                COLOR_BACKGROUND, COLOR_BORDER, COLOR_MUTED);
    }

    private String escapeHtml(String value) {
        if (value == null) return "Usuario";
        return value
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&#39;");
    }
}
