package com.rtms.backend.notification.service;

import jakarta.mail.internet.MimeMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;

@Service
public class EmailService {

    private static final Logger log = LoggerFactory.getLogger(EmailService.class);

    private final JavaMailSender mailSender;

    @Value("${app.frontend.url:http://localhost:3000}")
    private String frontendUrl;

    @Value("${spring.mail.username:no-reply@riversidertms.com}")
    private String fromEmail;

    public EmailService(@Autowired(required = false) JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    /**
     * Send welcome email to staff created by Admin (Trainer, Vet, Groom, Manager).
     */
    @Async
    public void sendStaffWelcomeEmail(String toEmail, String fullName, String role, String tempPassword) {
        String subject = "Welcome to Riverside Training Club - Your Account Details";
        String loginUrl = frontendUrl + "/login";

        String htmlContent = buildStaffWelcomeHtml(fullName, role, toEmail, tempPassword, loginUrl);
        String textContent = String.format(
                "Welcome to Riverside Training Club, %s!\n\n" +
                "An account has been created for you with the role of %s.\n\n" +
                "Sign-in email: %s\n" +
                "Temporary password: %s\n\n" +
                "Please sign in at %s and update your password immediately.\n\n" +
                "— Riverside Racehorse Training Management System",
                fullName, role, toEmail, tempPassword, loginUrl
        );

        sendEmail(toEmail, subject, htmlContent, textContent);
    }

    /**
     * Send welcome email to Horse Owner on self-registration.
     */
    @Async
    public void sendOwnerWelcomeEmail(String toEmail, String fullName) {
        String subject = "Welcome to Riverside Training Club - Registration Confirmed";
        String loginUrl = frontendUrl + "/login";

        String htmlContent = buildOwnerWelcomeHtml(fullName, toEmail, loginUrl);
        String textContent = String.format(
                "Welcome to Riverside Training Club, %s!\n\n" +
                "Your Horse Owner account has been successfully created.\n\n" +
                "Registered email: %s\n" +
                "You can now submit horse admission applications and follow your racehorses' training.\n\n" +
                "Sign in at: %s\n\n" +
                "— Riverside Racehorse Training Management System",
                fullName, toEmail, loginUrl
        );

        sendEmail(toEmail, subject, htmlContent, textContent);
    }

    private void sendEmail(String toEmail, String subject, String htmlBody, String textBody) {
        if (mailSender == null) {
            log.info("[EmailService] No JavaMailSender configured. Mocking email delivery:\nTo: {}\nSubject: {}\n---\n{}",
                    toEmail, subject, textBody);
            return;
        }

        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, MimeMessageHelper.MULTIPART_MODE_MIXED_RELATED, StandardCharsets.UTF_8.name());

            helper.setTo(toEmail);
            helper.setFrom(fromEmail);
            helper.setSubject(subject);
            helper.setText(textBody, htmlBody);

            mailSender.send(message);
            log.info("[EmailService] Email successfully sent to {}", toEmail);
        } catch (Exception e) {
            log.warn("[EmailService] Failed to send email to {} via SMTP (safe fallback to log): {}. Email content:\n{}",
                    toEmail, e.getMessage(), textBody);
        }
    }

    private String buildStaffWelcomeHtml(String fullName, String role, String email, String password, String loginUrl) {
        return "<!DOCTYPE html>" +
                "<html>" +
                "<head><meta charset='UTF-8'><style>" +
                "body { font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, Helvetica, Arial, sans-serif; background-color: #0b0f17; color: #e2e8f0; margin: 0; padding: 24px; }" +
                ".container { max-width: 580px; margin: 0 auto; background-color: #131b2e; border: 1px solid #1e293b; border-radius: 12px; overflow: hidden; }" +
                ".header { background: linear-gradient(135deg, #064e3b 0%, #047857 100%); padding: 32px 24px; text-align: center; color: #ffffff; }" +
                ".header h1 { margin: 0; font-size: 24px; font-weight: 700; letter-spacing: -0.02em; }" +
                ".header p { margin: 6px 0 0; font-size: 13px; color: #a7f3d0; text-transform: uppercase; letter-spacing: 0.1em; }" +
                ".content { padding: 32px 24px; color: #cbd5e1; font-size: 15px; line-height: 1.6; }" +
                ".card { background-color: #0f172a; border: 1px solid #334155; border-radius: 8px; padding: 18px 20px; margin: 24px 0; }" +
                ".card-item { margin-bottom: 10px; font-size: 14px; }" +
                ".card-item:last-child { margin-bottom: 0; }" +
                ".card-label { color: #94a3b8; font-size: 12px; text-transform: uppercase; letter-spacing: 0.05em; display: block; margin-bottom: 2px; }" +
                ".card-value { font-family: monospace; font-size: 15px; font-weight: 600; color: #f8fafc; }" +
                ".btn { display: inline-block; background-color: #10b981; color: #ffffff !important; text-decoration: none; font-weight: 600; padding: 14px 28px; border-radius: 8px; margin: 16px 0 24px; text-align: center; font-size: 15px; }" +
                ".footer { padding: 20px 24px; border-top: 1px solid #1e293b; text-align: center; font-size: 12px; color: #64748b; background-color: #0b0f17; }" +
                "</style></head>" +
                "<body>" +
                "<div class='container'>" +
                "  <div class='header'>" +
                "    <p>Riverside Training Club</p>" +
                "    <h1>Welcome to the Staff Team</h1>" +
                "  </div>" +
                "  <div class='content'>" +
                "    <p>Hello <strong>" + escape(fullName) + "</strong>,</p>" +
                "    <p>Your staff account for the <strong>Racehorse Training Management System (RTMS)</strong> has been established by management. You have been assigned the role of <strong>" + escape(role) + "</strong>.</p>" +
                "    <div class='card'>" +
                "      <div class='card-item'>" +
                "        <span class='card-label'>Sign-in Email</span>" +
                "        <span class='card-value'>" + escape(email) + "</span>" +
                "      </div>" +
                "      <div class='card-item'>" +
                "        <span class='card-label'>Temporary Password</span>" +
                "        <span class='card-value'>" + escape(password) + "</span>" +
                "      </div>" +
                "    </div>" +
                "    <p>Please click the button below to sign in and update your password immediately upon your first login:</p>" +
                "    <div style='text-align: center;'>" +
                "      <a href='" + escape(loginUrl) + "' class='btn'>Sign in to RTMS</a>" +
                "    </div>" +
                "    <p style='font-size: 13px; color: #94a3b8;'>If the button does not work, copy and paste this URL into your browser: <br/><a href='" + escape(loginUrl) + "' style='color: #34d399;'>" + escape(loginUrl) + "</a></p>" +
                "  </div>" +
                "  <div class='footer'>" +
                "    &copy; Riverside Racehorse Training Management System. All rights reserved." +
                "  </div>" +
                "</div>" +
                "</body></html>";
    }

    private String buildOwnerWelcomeHtml(String fullName, String email, String loginUrl) {
        return "<!DOCTYPE html>" +
                "<html>" +
                "<head><meta charset='UTF-8'><style>" +
                "body { font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, Helvetica, Arial, sans-serif; background-color: #0b0f17; color: #e2e8f0; margin: 0; padding: 24px; }" +
                ".container { max-width: 580px; margin: 0 auto; background-color: #131b2e; border: 1px solid #1e293b; border-radius: 12px; overflow: hidden; }" +
                ".header { background: linear-gradient(135deg, #064e3b 0%, #047857 100%); padding: 32px 24px; text-align: center; color: #ffffff; }" +
                ".header h1 { margin: 0; font-size: 24px; font-weight: 700; letter-spacing: -0.02em; }" +
                ".header p { margin: 6px 0 0; font-size: 13px; color: #a7f3d0; text-transform: uppercase; letter-spacing: 0.1em; }" +
                ".content { padding: 32px 24px; color: #cbd5e1; font-size: 15px; line-height: 1.6; }" +
                ".card { background-color: #0f172a; border: 1px solid #334155; border-radius: 8px; padding: 18px 20px; margin: 24px 0; }" +
                ".card-item { margin-bottom: 10px; font-size: 14px; }" +
                ".card-item:last-child { margin-bottom: 0; }" +
                ".card-label { color: #94a3b8; font-size: 12px; text-transform: uppercase; letter-spacing: 0.05em; display: block; margin-bottom: 2px; }" +
                ".card-value { font-family: monospace; font-size: 15px; font-weight: 600; color: #f8fafc; }" +
                ".btn { display: inline-block; background-color: #10b981; color: #ffffff !important; text-decoration: none; font-weight: 600; padding: 14px 28px; border-radius: 8px; margin: 16px 0 24px; text-align: center; font-size: 15px; }" +
                ".footer { padding: 20px 24px; border-top: 1px solid #1e293b; text-align: center; font-size: 12px; color: #64748b; background-color: #0b0f17; }" +
                "</style></head>" +
                "<body>" +
                "<div class='container'>" +
                "  <div class='header'>" +
                "    <p>Riverside Training Club</p>" +
                "    <h1>Account Registration Confirmed</h1>" +
                "  </div>" +
                "  <div class='content'>" +
                "    <p>Hello <strong>" + escape(fullName) + "</strong>,</p>" +
                "    <p>Welcome to Riverside Training Club! Your <strong>Horse Owner</strong> account has been successfully created.</p>" +
                "    <div class='card'>" +
                "      <div class='card-item'>" +
                "        <span class='card-label'>Registered Email</span>" +
                "        <span class='card-value'>" + escape(email) + "</span>" +
                "      </div>" +
                "      <div class='card-item'>" +
                "        <span class='card-label'>Role</span>" +
                "        <span class='card-value'>Horse Owner</span>" +
                "      </div>" +
                "    </div>" +
                "    <p>With your new account, you can now:</p>" +
                "    <ul>" +
                "      <li>Submit admission applications for your thoroughbreds.</li>" +
                "      <li>Track workout regimens, track speeds, and fitness progress.</li>" +
                "      <li>Review veterinary examinations and health records.</li>" +
                "    </ul>" +
                "    <div style='text-align: center;'>" +
                "      <a href='" + escape(loginUrl) + "' class='btn'>Sign in to Your Account</a>" +
                "    </div>" +
                "  </div>" +
                "  <div class='footer'>" +
                "    &copy; Riverside Racehorse Training Management System. All rights reserved." +
                "  </div>" +
                "</div>" +
                "</body></html>";
    }

    private String escape(String s) {
        if (s == null) return "";
        return s.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&#39;");
    }
}
