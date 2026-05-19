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

    public void sendWelcomeMemberEmail(String toEmail, String memberName, String teamName,
                                       String leaderName, String password) {
        try {
            MimeMessage mimeMessage = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, "UTF-8");
            helper.setFrom(fromEmail);
            helper.setTo(toEmail);
            helper.setSubject("TaskCode · Bienvenido a " + teamName);
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
        String safeName   = escapeHtml(memberName);
        String safeTeam   = escapeHtml(teamName);
        String safeLeader = escapeHtml(leaderName);
        String safeEmail  = escapeHtml(email);
        String safePass   = escapeHtml(password);
        return String.format("""
            <!doctype html>
            <html lang="es">
            <head>
              <meta charset="UTF-8" />
              <meta name="viewport" content="width=device-width, initial-scale=1.0" />
              <title>Bienvenido a TaskCode</title>
            </head>
            <body style="margin:0;padding:0;background-color:#f5f7fb;font-family:Arial,sans-serif;color:#1f2937;">
              <table role="presentation" width="100%%" cellspacing="0" cellpadding="0" style="padding:24px 0;">
                <tr>
                  <td align="center">
                    <table role="presentation" width="600" cellspacing="0" cellpadding="0" style="max-width:600px;background:#ffffff;border-radius:12px;overflow:hidden;border:1px solid #e5e7eb;">
                      <tr>
                        <td style="background:#111827;padding:20px 24px;color:#ffffff;font-size:18px;font-weight:700;">TaskCodeBack</td>
                      </tr>
                      <tr>
                        <td style="padding:28px 24px 8px 24px;font-size:16px;line-height:1.6;">
                          Hola %s,<br/><br/>
                          <strong>%s</strong> te ha añadido al equipo <strong>%s</strong> en <strong>TaskCodeBack</strong>.<br/>
                          Ya puedes acceder con las siguientes credenciales:
                        </td>
                      </tr>
                      <tr>
                        <td style="padding:16px 24px;">
                          <table role="presentation" width="100%%" cellspacing="0" cellpadding="0" style="background:#f3f4f6;border:1px dashed #9ca3af;border-radius:10px;overflow:hidden;">
                            <tr>
                              <td style="padding:14px 20px;border-bottom:1px solid #e5e7eb;">
                                <span style="font-size:12px;color:#6b7280;text-transform:uppercase;letter-spacing:1px;font-weight:700;">Usuario</span>
                                <p style="margin:4px 0 0 0;font-size:15px;font-weight:600;color:#111827;">%s</p>
                              </td>
                            </tr>
                            <tr>
                              <td style="padding:14px 20px;">
                                <span style="font-size:12px;color:#6b7280;text-transform:uppercase;letter-spacing:1px;font-weight:700;">Contraseña temporal</span>
                                <p style="margin:4px 0 0 0;font-size:22px;font-weight:800;color:#111827;letter-spacing:4px;font-family:monospace;">%s</p>
                              </td>
                            </tr>
                          </table>
                        </td>
                      </tr>
                      <tr>
                        <td style="padding:4px 24px 24px 24px;font-size:14px;line-height:1.7;color:#4b5563;">
                          Te recomendamos cambiar tu contraseña después del primer ingreso.<br/><br/>
                          Por seguridad, no compartas estas credenciales con nadie.
                        </td>
                      </tr>
                      <tr>
                        <td style="padding:16px 24px;background:#f9fafb;border-top:1px solid #e5e7eb;font-size:12px;color:#6b7280;">
                          Este es un correo automatico, por favor no responder.
                        </td>
                      </tr>
                    </table>
                  </td>
                </tr>
              </table>
            </body>
            </html>
            """, safeName, safeLeader, safeTeam, safeEmail, safePass);
    }

    private String buildVerificationEmailHtml(String userName, String verificationCode) {
        String safeUserName = escapeHtml(userName);
        return String.format("""
            <!doctype html>
            <html lang="es">
            <head>
              <meta charset="UTF-8" />
              <meta name="viewport" content="width=device-width, initial-scale=1.0" />
              <title>Codigo de verificacion</title>
            </head>
            <body style="margin:0;padding:0;background-color:#f5f7fb;font-family:Arial,sans-serif;color:#1f2937;">
              <table role="presentation" width="100%%" cellspacing="0" cellpadding="0" style="padding:24px 0;">
                <tr>
                  <td align="center">
                    <table role="presentation" width="600" cellspacing="0" cellpadding="0" style="max-width:600px;background:#ffffff;border-radius:12px;overflow:hidden;border:1px solid #e5e7eb;">
                      <tr>
                        <td style="background:#111827;padding:20px 24px;color:#ffffff;font-size:18px;font-weight:700;">TaskCodeBack</td>
                      </tr>
                      <tr>
                        <td style="padding:28px 24px 8px 24px;font-size:16px;line-height:1.6;">
                          Hola %s,<br/><br/>
                          Recibimos una solicitud para verificar tu correo en <strong>TaskCodeBack</strong>.
                          Usa el siguiente codigo de seguridad:
                        </td>
                      </tr>
                      <tr>
                        <td align="center" style="padding:16px 24px;">
                          <div style="display:inline-block;letter-spacing:6px;font-size:28px;font-weight:800;color:#111827;background:#f3f4f6;border:1px dashed #9ca3af;border-radius:10px;padding:14px 24px;">%s</div>
                        </td>
                      </tr>
                      <tr>
                        <td style="padding:4px 24px 24px 24px;font-size:14px;line-height:1.7;color:#4b5563;">
                          Este codigo expira en <strong>5 minutos</strong>.<br/>
                          Por seguridad, no lo compartas con nadie.<br/><br/>
                          Si no realizaste esta solicitud, puedes ignorar este mensaje.
                        </td>
                      </tr>
                      <tr>
                        <td style="padding:16px 24px;background:#f9fafb;border-top:1px solid #e5e7eb;font-size:12px;color:#6b7280;">
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
        if (value == null) return "Usuario";
        return value
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&#39;");
    }
}
