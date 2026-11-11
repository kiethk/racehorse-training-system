package com.rtms.backend.notification.service;

import jakarta.mail.internet.MimeMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.UUID;

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
        String subject = "Welcome to Riverside Training Club, " + fullName;
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
        String subject = "Welcome to Riverside Training Club, " + fullName;
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

    /**
     * Send email to Owner when Quarantine Groom accepts application and reserves a quarantine stall.
     */
    @Async
    public void sendAdmissionWaitingForArrivalEmail(
            String toEmail,
            String fullName,
            String horseName,
            String stallCode,
            LocalDate arrivalDeadline,
            String feedback,
            String admissionUrl) {

        String subject = "Quarantine Stall Reserved for " + horseName;
        String deadlineStr = arrivalDeadline != null ? arrivalDeadline.toString() : "Within 14 days";

        String htmlContent = buildWaitingForArrivalHtml(fullName, horseName, stallCode, deadlineStr, feedback, admissionUrl);
        String textContent = String.format(
                "Dear %s,\n\n" +
                "Your admission application for \"%s\" has been accepted by our quarantine team.\n\n" +
                "Quarantine Stall: %s\n" +
                "Arrival Deadline: %s\n" +
                (feedback != null && !feedback.isBlank() ? "Reviewer Notes: " + feedback + "\n" : "") + "\n" +
                "Please deliver your horse to Riverside Training Facility before the deadline above.\n\n" +
                "View full details: %s\n\n" +
                "— Riverside Racehorse Training Management System",
                fullName, horseName, stallCode, deadlineStr, admissionUrl
        );

        sendEmail(toEmail, subject, htmlContent, textContent);
    }

    /**
     * Send congratulatory email to Owner when Club Manager approves the horse into regular stable.
     */
    @Async
    public void sendAdmissionApprovedEmail(
            String toEmail,
            String fullName,
            String horseName,
            String stallCode,
            String feedback,
            String horseUrl) {

        String subject = "Congratulations — " + horseName + " is Officially Admitted to Riverside";

        String htmlContent = buildApprovedHtml(fullName, horseName, stallCode, feedback, horseUrl);
        String textContent = String.format(
                "Dear %s,\n\n" +
                "Congratulations! \"%s\" has passed clinical examinations, veterinary quarantine, and trainer assessment.\n\n" +
                "Your horse is now officially admitted to Riverside Training Club as an active racehorse.\n\n" +
                "Regular Stable Stall: %s\n" +
                "Status: Active / Eligible for Training\n" +
                (feedback != null && !feedback.isBlank() ? "Manager Notes: " + feedback + "\n" : "") + "\n" +
                "You can now track workout sessions, veterinary reports, and live stride telemetry at:\n%s\n\n" +
                "— Riverside Racehorse Training Management System",
                fullName, horseName, stallCode, horseUrl
        );

        sendEmail(toEmail, subject, htmlContent, textContent);
    }

    /**
     * Send email to Owner when an admission application is rejected.
     */
    @Async
    public void sendAdmissionRejectedEmail(
            String toEmail,
            String fullName,
            String horseName,
            String stage,
            String feedback,
            String admissionUrl) {

        String subject = "Update on Admission Application for " + horseName;

        String htmlContent = buildRejectedHtml(fullName, horseName, stage, feedback, admissionUrl);
        String textContent = String.format(
                "Dear %s,\n\n" +
                "We regret to inform you that your admission application for \"%s\" was not accepted during the %s stage.\n\n" +
                "Reason / Feedback:\n%s\n\n" +
                "You can review your submission and reviewer notes at:\n%s\n\n" +
                "— Riverside Racehorse Training Management System",
                fullName, horseName, stage, feedback != null ? feedback : "No feedback provided", admissionUrl
        );

        sendEmail(toEmail, subject, htmlContent, textContent);
    }

    /**
     * Send email to Owner when a 14-day quarantine reservation expires without arrival.
     */
    @Async
    public void sendAdmissionArrivalExpiredEmail(
            String toEmail,
            String fullName,
            String horseName,
            String admissionUrl) {

        String subject = "Quarantine Reservation Expired — " + horseName;

        String htmlContent = buildArrivalExpiredHtml(fullName, horseName, admissionUrl);
        String textContent = String.format(
                "Dear %s,\n\n" +
                "The 14-day arrival reservation for \"%s\" has expired because the horse did not check in by the scheduled deadline.\n\n" +
                "The reserved quarantine stall has been released back to the facility capacity.\n\n" +
                "Review admission status at: %s\n\n" +
                "— Riverside Racehorse Training Management System",
                fullName, horseName, admissionUrl
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

            // Prevent email clients (such as Gmail) from collapsing distinct transactional emails into threads
            String messageId = String.format("<%s.%d@riversidertms.com>", UUID.randomUUID(), System.currentTimeMillis());
            message.setHeader("Message-ID", messageId);
            message.setHeader("X-Entity-Ref-ID", UUID.randomUUID().toString());

            helper.setTo(toEmail);
            helper.setFrom(fromEmail);
            helper.setSubject(subject);
            helper.setText(textBody, htmlBody);

            try {
                ClassPathResource logoResource = new ClassPathResource("static/logo-email.png");
                if (logoResource.exists()) {
                    helper.addInline("rtms-logo", logoResource, "image/png");
                }
            } catch (Exception logoEx) {
                log.debug("[EmailService] Could not attach inline logo: {}", logoEx.getMessage());
            }

            mailSender.send(message);
            log.info("[EmailService] Email successfully sent to {}", toEmail);
        } catch (Exception e) {
            log.warn("[EmailService] Failed to send email to {} via SMTP (safe fallback to log): {}. Email content:\n{}",
                    toEmail, e.getMessage(), textBody);
        }
    }

    private String buildEmailStyles() {
        return "body { margin: 0; padding: 24px 12px; background-color: #f6f6f8; font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, Helvetica, Arial, sans-serif; color: #27272a; -webkit-font-smoothing: antialiased; }" +
                ".wrapper { max-width: 560px; margin: 0 auto; background-color: #ffffff; border-radius: 12px; overflow: hidden; box-shadow: 0 4px 20px rgba(102, 87, 199, 0.08), 0 1px 3px rgba(0,0,0,0.05); border: 1px solid #ded8fc; }" +
                ".header { background: linear-gradient(135deg, #6657c7 0%, #4f41b3 100%); padding: 32px 28px 26px; text-align: center; color: #ffffff; }" +
                ".brand-logo { margin: 0 auto 12px; text-align: center; }" +
                ".brand-logo img { width: 56px; height: 56px; border-radius: 50%; display: inline-block; margin: 0 auto; vertical-align: middle; box-shadow: 0 4px 14px rgba(0,0,0,0.15); }" +
                ".brand-sub { margin: 0; font-size: 11px; font-weight: 700; letter-spacing: 0.12em; text-transform: uppercase; color: #ded8fc; text-align: center; }" +
                ".header h1 { margin: 8px 0 0; font-size: 22px; font-weight: 700; letter-spacing: -0.01em; color: #ffffff; line-height: 1.3; text-align: center; }" +
                ".content { padding: 32px 32px 24px; font-size: 14px; line-height: 1.65; color: #52525b; }" +
                ".greeting { font-size: 15px; font-weight: 600; color: #27272a; margin-bottom: 12px; }" +
                ".card { background-color: #faf9ff; border: 1px solid #ded8fc; border-radius: 10px; padding: 18px 20px; margin: 22px 0; }" +
                ".card-item { margin-bottom: 12px; font-size: 13px; }" +
                ".card-item:last-child { margin-bottom: 0; }" +
                ".card-label { color: #8c82bb; font-size: 11px; text-transform: uppercase; letter-spacing: 0.06em; font-weight: 700; display: block; margin-bottom: 3px; }" +
                ".card-value { font-size: 14px; font-weight: 600; color: #27272a; }" +
                ".notice-box { background-color: #f8f6ff; border-left: 3px solid #6657c7; border-radius: 0 6px 6px 0; padding: 12px 14px; font-size: 13px; color: #52525b; margin: 18px 0; line-height: 1.5; }" +
                ".btn-container { text-align: center; margin: 26px 0 16px; }" +
                ".btn { display: inline-block; background: linear-gradient(135deg, #6657c7 0%, #4f41b3 100%); color: #ffffff !important; text-decoration: none; font-weight: 700; font-size: 14px; padding: 13px 32px; border-radius: 8px; box-shadow: 0 4px 12px rgba(102, 87, 199, 0.28); }" +
                ".footer { padding: 18px 24px; border-top: 1px solid #ede9fb; text-align: center; font-size: 11px; color: #8c82bb; background-color: #fcfbfe; line-height: 1.6; }" +
                ".footer strong { color: #6657c7; }";
    }

    private String buildEmailHeader(String title, String subtitle) {
        return "<div class='header'>" +
                "  <div class='brand-logo'>" +
                "    <img src='cid:rtms-logo' alt='RTMS' width='56' height='56' style='display:inline-block; border-radius:50%; vertical-align:middle;' />" +
                "  </div>" +
                "  <p class='brand-sub'>" + escape(subtitle != null ? subtitle : "Riverside Training Club") + "</p>" +
                "  <h1>" + escape(title) + "</h1>" +
                "</div>";
    }

    private String buildEmailFooter() {
        return "<div class='footer'>" +
                "  &copy; 2026 <strong>Riverside Racehorse Training Management System</strong>. All rights reserved.<br/>" +
                "  This is an automated operational notification." +
                "</div>";
    }

    private String buildStaffWelcomeHtml(String fullName, String role, String email, String password, String loginUrl) {
        return "<!DOCTYPE html>" +
                "<html>" +
                "<head><meta charset='UTF-8'><style>" + buildEmailStyles() + "</style></head>" +
                "<body>" +
                "<div class='wrapper'>" +
                buildEmailHeader("Welcome to the Staff Team", "Riverside Training Club") +
                "  <div class='content'>" +
                "    <div class='greeting'>Hello <strong>" + escape(fullName) + "</strong>,</div>" +
                "    <p>Your staff account for the <strong>Racehorse Training Management System (RTMS)</strong> has been established by club management. You have been assigned the role of <strong>" + escape(role) + "</strong>.</p>" +
                "    <div class='card'>" +
                "      <div class='card-item'>" +
                "        <span class='card-label'>Sign-in Email</span>" +
                "        <span class='card-value' style='color: #6657c7;'>" + escape(email) + "</span>" +
                "      </div>" +
                "      <div class='card-item'>" +
                "        <span class='card-label'>Temporary Password</span>" +
                "        <span class='card-value' style='font-family: monospace; letter-spacing: 0.05em;'>" + escape(password) + "</span>" +
                "      </div>" +
                "    </div>" +
                "    <div class='notice-box'>🔒 For security reasons, please sign in and <strong>change your temporary password</strong> upon your first login.</div>" +
                "    <div class='btn-container'>" +
                "      <a href='" + escape(loginUrl) + "' class='btn'>Sign in to RTMS</a>" +
                "    </div>" +
                "  </div>" +
                buildEmailFooter() +
                "</div>" +
                "</body></html>";
    }

    private String buildOwnerWelcomeHtml(String fullName, String email, String loginUrl) {
        return "<!DOCTYPE html>" +
                "<html>" +
                "<head><meta charset='UTF-8'><style>" + buildEmailStyles() + "</style></head>" +
                "<body>" +
                "<div class='wrapper'>" +
                buildEmailHeader("Welcome, Horse Owner!", "Riverside Training Club") +
                "  <div class='content'>" +
                "    <div class='greeting'>Hello <strong>" + escape(fullName) + "</strong>,</div>" +
                "    <p>Welcome to Riverside Training Club! Your <strong>Horse Owner</strong> account has been successfully created.</p>" +
                "    <div class='card'>" +
                "      <div class='card-item'>" +
                "        <span class='card-label'>Registered Email</span>" +
                "        <span class='card-value' style='color: #6657c7;'>" + escape(email) + "</span>" +
                "      </div>" +
                "      <div class='card-item'>" +
                "        <span class='card-label'>Account Role</span>" +
                "        <span class='card-value'>Horse Owner</span>" +
                "      </div>" +
                "    </div>" +
                "    <p>With your account, you can conveniently:</p>" +
                "    <ul style='padding-left: 20px; margin: 12px 0 20px; color: #52525b;'>" +
                "      <li style='margin-bottom: 6px;'>Submit admission requests for your racehorses.</li>" +
                "      <li style='margin-bottom: 6px;'>Follow workout regimens, track times, and training schedules.</li>" +
                "      <li>Monitor health examinations, vet care logs, and quarantine records.</li>" +
                "    </ul>" +
                "    <div class='btn-container'>" +
                "      <a href='" + escape(loginUrl) + "' class='btn'>Sign in to Owner Portal</a>" +
                "    </div>" +
                "  </div>" +
                buildEmailFooter() +
                "</div>" +
                "</body></html>";
    }

    private String buildWaitingForArrivalHtml(
            String fullName,
            String horseName,
            String stallCode,
            String deadline,
            String feedback,
            String url) {

        String feedbackSection = (feedback != null && !feedback.isBlank()) ?
                "    <div class='notice-box'>" +
                "      <strong style='color: #27272a;'>Quarantine Team Note:</strong><br/>" +
                "      <span>" + escape(feedback) + "</span>" +
                "    </div>" : "";

        return "<!DOCTYPE html>" +
                "<html>" +
                "<head><meta charset='UTF-8'><style>" + buildEmailStyles() + "</style></head>" +
                "<body>" +
                "<div class='wrapper'>" +
                buildEmailHeader("Quarantine Stall Reserved", "Riverside Training Club · Admissions") +
                "  <div class='content'>" +
                "    <div class='greeting'>Dear <strong>" + escape(fullName) + "</strong>,</div>" +
                "    <p>Great news! The admission request for <strong>" + escape(horseName) + "</strong> has been accepted by our quarantine team. A designated quarantine stall has been reserved for intake.</p>" +
                "    <div class='card'>" +
                "      <div class='card-item'>" +
                "        <span class='card-label'>Horse Name</span>" +
                "        <span class='card-value'>" + escape(horseName) + "</span>" +
                "      </div>" +
                "      <div class='card-item'>" +
                "        <span class='card-label'>Reserved Quarantine Stall</span>" +
                "        <span class='card-value' style='color: #6657c7;'>" + escape(stallCode) + "</span>" +
                "      </div>" +
                "      <div class='card-item'>" +
                "        <span class='card-label'>Arrival Deadline</span>" +
                "        <span class='card-value' style='color: #b45309;'>" + escape(deadline) + "</span>" +
                "      </div>" +
                "    </div>" +
                feedbackSection +
                "    <div class='notice-box'>⚠️ Please deliver your horse to the Riverside facility before the arrival deadline so our intake veterinary examination can proceed.</div>" +
                "    <div class='btn-container'>" +
                "      <a href='" + escape(url) + "' class='btn'>View Admission Details</a>" +
                "    </div>" +
                "  </div>" +
                buildEmailFooter() +
                "</div>" +
                "</body></html>";
    }

    private String buildApprovedHtml(
            String fullName,
            String horseName,
            String stallCode,
            String feedback,
            String horseUrl) {

        String feedbackSection = (feedback != null && !feedback.isBlank()) ?
                "    <div class='notice-box'>" +
                "      <strong style='color: #27272a;'>Club Manager Note:</strong><br/>" +
                "      <span>" + escape(feedback) + "</span>" +
                "    </div>" : "";

        return "<!DOCTYPE html>" +
                "<html>" +
                "<head><meta charset='UTF-8'><style>" + buildEmailStyles() + "</style></head>" +
                "<body>" +
                "<div class='wrapper'>" +
                buildEmailHeader("Admission Approved", "Riverside Training Club · Admissions") +
                "  <div class='content'>" +
                "    <div class='greeting'>Dear <strong>" + escape(fullName) + "</strong>,</div>" +
                "    <p>We are pleased to inform you that <strong>" + escape(horseName) + "</strong> has successfully completed veterinary checks and quarantine intake. Your horse is officially admitted as an active training racehorse at Riverside.</p>" +
                "    <div class='card'>" +
                "      <div class='card-item'>" +
                "        <span class='card-label'>Horse Name</span>" +
                "        <span class='card-value'>" + escape(horseName) + "</span>" +
                "      </div>" +
                "      <div class='card-item'>" +
                "        <span class='card-label'>Assigned Regular Stall</span>" +
                "        <span class='card-value' style='color: #6657c7;'>" + escape(stallCode) + "</span>" +
                "      </div>" +
                "      <div class='card-item'>" +
                "        <span class='card-label'>Roster Status</span>" +
                "        <span class='card-value' style='color: #15803d;'>Active · Ready for Training</span>" +
                "      </div>" +
                "    </div>" +
                feedbackSection +
                "    <p>You can now follow daily training workouts, veterinary care schedules, and health records in your portal:</p>" +
                "    <div class='btn-container'>" +
                "      <a href='" + escape(horseUrl) + "' class='btn'>View Horse Profile</a>" +
                "    </div>" +
                "  </div>" +
                buildEmailFooter() +
                "</div>" +
                "</body></html>";
    }

    private String buildRejectedHtml(
            String fullName,
            String horseName,
            String stage,
            String feedback,
            String url) {

        return "<!DOCTYPE html>" +
                "<html>" +
                "<head><meta charset='UTF-8'><style>" + buildEmailStyles() + "</style></head>" +
                "<body>" +
                "<div class='wrapper'>" +
                buildEmailHeader("Admission Review Notice", "Riverside Training Club · Admissions") +
                "  <div class='content'>" +
                "    <div class='greeting'>Dear <strong>" + escape(fullName) + "</strong>,</div>" +
                "    <p>We regret to inform you that the admission application for <strong>" + escape(horseName) + "</strong> was not approved during the <strong>" + escape(stage) + "</strong> review stage.</p>" +
                "    <div class='card'>" +
                "      <div class='card-item'>" +
                "        <span class='card-label'>Horse Name</span>" +
                "        <span class='card-value'>" + escape(horseName) + "</span>" +
                "      </div>" +
                "      <div class='card-item'>" +
                "        <span class='card-label'>Review Stage</span>" +
                "        <span class='card-value'>" + escape(stage) + "</span>" +
                "      </div>" +
                "      <div class='card-item'>" +
                "        <span class='card-label'>Reviewer Reason / Feedback</span>" +
                "        <span class='card-value' style='color: #b91c1c; white-space: pre-wrap;'>" + escape(feedback != null && !feedback.isBlank() ? feedback : "No specific comments provided.") + "</span>" +
                "      </div>" +
                "    </div>" +
                "    <p>You can review your full application submission and notes directly in your portal:</p>" +
                "    <div class='btn-container'>" +
                "      <a href='" + escape(url) + "' class='btn'>View Application Details</a>" +
                "    </div>" +
                "  </div>" +
                buildEmailFooter() +
                "</div>" +
                "</body></html>";
    }

    private String buildArrivalExpiredHtml(
            String fullName,
            String horseName,
            String url) {

        return "<!DOCTYPE html>" +
                "<html>" +
                "<head><meta charset='UTF-8'><style>" + buildEmailStyles() + "</style></head>" +
                "<body>" +
                "<div class='wrapper'>" +
                buildEmailHeader("Reservation Window Expired", "Riverside Training Club · Admissions") +
                "  <div class='content'>" +
                "    <div class='greeting'>Dear <strong>" + escape(fullName) + "</strong>,</div>" +
                "    <p>The quarantine arrival reservation for <strong>" + escape(horseName) + "</strong> has expired because the horse did not arrive at the facility within the scheduled arrival deadline.</p>" +
                "    <div class='card'>" +
                "      <div class='card-item'>" +
                "        <span class='card-label'>Horse Name</span>" +
                "        <span class='card-value'>" + escape(horseName) + "</span>" +
                "      </div>" +
                "      <div class='card-item'>" +
                "        <span class='card-label'>Reservation Status</span>" +
                "        <span class='card-value' style='color: #b45309;'>Expired · Quarantine Stall Released</span>" +
                "      </div>" +
                "    </div>" +
                "    <div class='notice-box'>💡 The reserved stall has been released back to general capacity. If you still wish to board and train your horse, please submit a new admission request.</div>" +
                "    <div class='btn-container'>" +
                "      <a href='" + escape(url) + "' class='btn'>Review Admission Details</a>" +
                "    </div>" +
                "  </div>" +
                buildEmailFooter() +
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
