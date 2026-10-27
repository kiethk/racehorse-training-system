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
import java.time.LocalDate;

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

        String subject = "Admission Accepted — Quarantine Stall Reserved for " + horseName;
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

    private String buildWaitingForArrivalHtml(
            String fullName,
            String horseName,
            String stallCode,
            String deadline,
            String feedback,
            String url) {

        String feedbackSection = (feedback != null && !feedback.isBlank()) ?
                "    <div style='margin-top: 16px; padding: 12px 16px; background-color: #1e293b; border-radius: 6px; font-size: 13px;'>" +
                "      <strong style='color: #cbd5e1;'>Quarantine Groom Notes:</strong><br/>" +
                "      <span style='color: #94a3b8;'>" + escape(feedback) + "</span>" +
                "    </div>" : "";

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
                ".card-item { margin-bottom: 12px; font-size: 14px; }" +
                ".card-item:last-child { margin-bottom: 0; }" +
                ".card-label { color: #94a3b8; font-size: 12px; text-transform: uppercase; letter-spacing: 0.05em; display: block; margin-bottom: 2px; }" +
                ".card-value { font-family: monospace; font-size: 15px; font-weight: 600; color: #f8fafc; }" +
                ".btn { display: inline-block; background-color: #10b981; color: #ffffff !important; text-decoration: none; font-weight: 600; padding: 14px 28px; border-radius: 8px; margin: 16px 0 24px; text-align: center; font-size: 15px; }" +
                ".footer { padding: 20px 24px; border-top: 1px solid #1e293b; text-align: center; font-size: 12px; color: #64748b; background-color: #0b0f17; }" +
                "</style></head>" +
                "<body>" +
                "<div class='container'>" +
                "  <div class='header'>" +
                "    <p>Riverside Training Club · Admissions</p>" +
                "    <h1>Admission Accepted</h1>" +
                "  </div>" +
                "  <div class='content'>" +
                "    <p>Dear <strong>" + escape(fullName) + "</strong>,</p>" +
                "    <p>Great news! The admission application for <strong>" + escape(horseName) + "</strong> has been reviewed and accepted by our quarantine team. A biosecure quarantine stall is now reserved for your horse.</p>" +
                "    <div class='card'>" +
                "      <div class='card-item'>" +
                "        <span class='card-label'>Horse Name</span>" +
                "        <span class='card-value'>" + escape(horseName) + "</span>" +
                "      </div>" +
                "      <div class='card-item'>" +
                "        <span class='card-label'>Reserved Quarantine Stall</span>" +
                "        <span class='card-value' style='color: #34d399;'>" + escape(stallCode) + "</span>" +
                "      </div>" +
                "      <div class='card-item'>" +
                "        <span class='card-label'>Arrival Deadline</span>" +
                "        <span class='card-value' style='color: #fbbf24;'>" + escape(deadline) + "</span>" +
                "      </div>" +
                "    </div>" +
                feedbackSection +
                "    <p style='margin-top: 20px;'><strong>Next Step:</strong> Please deliver your horse to Riverside Training Facility before the deadline above so our veterinary team can conduct the intake examination and quarantine check-in.</p>" +
                "    <div style='text-align: center;'>" +
                "      <a href='" + escape(url) + "' class='btn'>View Admission &amp; Arrival Details</a>" +
                "    </div>" +
                "  </div>" +
                "  <div class='footer'>" +
                "    &copy; Riverside Racehorse Training Management System. All rights reserved." +
                "  </div>" +
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
                "    <div style='margin-top: 16px; padding: 12px 16px; background-color: #1e293b; border-radius: 6px; font-size: 13px;'>" +
                "      <strong style='color: #cbd5e1;'>Club Manager Notes:</strong><br/>" +
                "      <span style='color: #94a3b8;'>" + escape(feedback) + "</span>" +
                "    </div>" : "";

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
                ".card-item { margin-bottom: 12px; font-size: 14px; }" +
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
                "    <h1>Admission Officially Approved</h1>" +
                "  </div>" +
                "  <div class='content'>" +
                "    <p>Dear <strong>" + escape(fullName) + "</strong>,</p>" +
                "    <p>We are delighted to inform you that <strong>" + escape(horseName) + "</strong> has successfully passed veterinary intake checks, clinical conditioning assessments, and final Club Manager approval.</p>" +
                "    <p>Your horse is now officially admitted to Riverside Training Club as an active racehorse on our roster.</p>" +
                "    <div class='card'>" +
                "      <div class='card-item'>" +
                "        <span class='card-label'>Horse Name</span>" +
                "        <span class='card-value'>" + escape(horseName) + "</span>" +
                "      </div>" +
                "      <div class='card-item'>" +
                "        <span class='card-label'>Regular Stable Stall</span>" +
                "        <span class='card-value' style='color: #34d399;'>" + escape(stallCode) + "</span>" +
                "      </div>" +
                "      <div class='card-item'>" +
                "        <span class='card-label'>Roster Status</span>" +
                "        <span class='card-value'>Active · Eligible for Training</span>" +
                "      </div>" +
                "    </div>" +
                feedbackSection +
                "    <p style='margin-top: 20px;'>You can now view live workout regimens, biometric stride logs, and veterinary health records directly in your Owner Portal:</p>" +
                "    <div style='text-align: center;'>" +
                "      <a href='" + escape(horseUrl) + "' class='btn'>View Horse Profile &amp; Telemetry</a>" +
                "    </div>" +
                "  </div>" +
                "  <div class='footer'>" +
                "    &copy; Riverside Racehorse Training Management System. All rights reserved." +
                "  </div>" +
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
                "<head><meta charset='UTF-8'><style>" +
                "body { font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, Helvetica, Arial, sans-serif; background-color: #0b0f17; color: #e2e8f0; margin: 0; padding: 24px; }" +
                ".container { max-width: 580px; margin: 0 auto; background-color: #131b2e; border: 1px solid #1e293b; border-radius: 12px; overflow: hidden; }" +
                ".header { background: linear-gradient(135deg, #7f1d1d 0%, #991b1b 100%); padding: 32px 24px; text-align: center; color: #ffffff; }" +
                ".header h1 { margin: 0; font-size: 24px; font-weight: 700; letter-spacing: -0.02em; }" +
                ".header p { margin: 6px 0 0; font-size: 13px; color: #fecaca; text-transform: uppercase; letter-spacing: 0.1em; }" +
                ".content { padding: 32px 24px; color: #cbd5e1; font-size: 15px; line-height: 1.6; }" +
                ".card { background-color: #0f172a; border: 1px solid #334155; border-radius: 8px; padding: 18px 20px; margin: 24px 0; }" +
                ".card-item { margin-bottom: 12px; font-size: 14px; }" +
                ".card-item:last-child { margin-bottom: 0; }" +
                ".card-label { color: #94a3b8; font-size: 12px; text-transform: uppercase; letter-spacing: 0.05em; display: block; margin-bottom: 2px; }" +
                ".card-value { font-family: monospace; font-size: 15px; font-weight: 600; color: #f8fafc; }" +
                ".btn { display: inline-block; background-color: #475569; color: #ffffff !important; text-decoration: none; font-weight: 600; padding: 14px 28px; border-radius: 8px; margin: 16px 0 24px; text-align: center; font-size: 15px; }" +
                ".footer { padding: 20px 24px; border-top: 1px solid #1e293b; text-align: center; font-size: 12px; color: #64748b; background-color: #0b0f17; }" +
                "</style></head>" +
                "<body>" +
                "<div class='container'>" +
                "  <div class='header'>" +
                "    <p>Riverside Training Club · Admissions</p>" +
                "    <h1>Admission Review Notice</h1>" +
                "  </div>" +
                "  <div class='content'>" +
                "    <p>Dear <strong>" + escape(fullName) + "</strong>,</p>" +
                "    <p>We regret to inform you that your admission application for <strong>" + escape(horseName) + "</strong> was not approved during the <strong>" + escape(stage) + "</strong> review.</p>" +
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
                "        <span class='card-label'>Reviewer Feedback / Reason</span>" +
                "        <span class='card-value' style='color: #f87171; white-space: pre-wrap;'>" + escape(feedback != null && !feedback.isBlank() ? feedback : "No specific details provided.") + "</span>" +
                "      </div>" +
                "    </div>" +
                "    <p>You can review your full admission file, uploaded documents, and reviewer comments in your portal:</p>" +
                "    <div style='text-align: center;'>" +
                "      <a href='" + escape(url) + "' class='btn'>View Application Details</a>" +
                "    </div>" +
                "  </div>" +
                "  <div class='footer'>" +
                "    &copy; Riverside Racehorse Training Management System. All rights reserved." +
                "  </div>" +
                "</div>" +
                "</body></html>";
    }

    private String buildArrivalExpiredHtml(
            String fullName,
            String horseName,
            String url) {

        return "<!DOCTYPE html>" +
                "<html>" +
                "<head><meta charset='UTF-8'><style>" +
                "body { font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, Helvetica, Arial, sans-serif; background-color: #0b0f17; color: #e2e8f0; margin: 0; padding: 24px; }" +
                ".container { max-width: 580px; margin: 0 auto; background-color: #131b2e; border: 1px solid #1e293b; border-radius: 12px; overflow: hidden; }" +
                ".header { background: linear-gradient(135deg, #78350f 0%, #92400e 100%); padding: 32px 24px; text-align: center; color: #ffffff; }" +
                ".header h1 { margin: 0; font-size: 24px; font-weight: 700; letter-spacing: -0.02em; }" +
                ".header p { margin: 6px 0 0; font-size: 13px; color: #fde68a; text-transform: uppercase; letter-spacing: 0.1em; }" +
                ".content { padding: 32px 24px; color: #cbd5e1; font-size: 15px; line-height: 1.6; }" +
                ".card { background-color: #0f172a; border: 1px solid #334155; border-radius: 8px; padding: 18px 20px; margin: 24px 0; }" +
                ".card-item { margin-bottom: 12px; font-size: 14px; }" +
                ".card-item:last-child { margin-bottom: 0; }" +
                ".card-label { color: #94a3b8; font-size: 12px; text-transform: uppercase; letter-spacing: 0.05em; display: block; margin-bottom: 2px; }" +
                ".card-value { font-family: monospace; font-size: 15px; font-weight: 600; color: #f8fafc; }" +
                ".btn { display: inline-block; background-color: #d97706; color: #ffffff !important; text-decoration: none; font-weight: 600; padding: 14px 28px; border-radius: 8px; margin: 16px 0 24px; text-align: center; font-size: 15px; }" +
                ".footer { padding: 20px 24px; border-top: 1px solid #1e293b; text-align: center; font-size: 12px; color: #64748b; background-color: #0b0f17; }" +
                "</style></head>" +
                "<body>" +
                "<div class='container'>" +
                "  <div class='header'>" +
                "    <p>Riverside Training Club · Admissions</p>" +
                "    <h1>Reservation Window Expired</h1>" +
                "  </div>" +
                "  <div class='content'>" +
                "    <p>Dear <strong>" + escape(fullName) + "</strong>,</p>" +
                "    <p>The 14-day quarantine arrival window for <strong>" + escape(horseName) + "</strong> has expired as the horse was not received at the facility within the scheduled deadline.</p>" +
                "    <div class='card'>" +
                "      <div class='card-item'>" +
                "        <span class='card-label'>Horse Name</span>" +
                "        <span class='card-value'>" + escape(horseName) + "</span>" +
                "      </div>" +
                "      <div class='card-item'>" +
                "        <span class='card-label'>Status</span>" +
                "        <span class='card-value' style='color: #f59e0b;'>Arrival Overdue · Quarantine Stall Released</span>" +
                "      </div>" +
                "    </div>" +
                "    <p>The reserved quarantine stall has been returned to the facility capacity. If you still intend to board your horse at Riverside, please contact management or resubmit an admission application.</p>" +
                "    <div style='text-align: center;'>" +
                "      <a href='" + escape(url) + "' class='btn'>Review Admission Details</a>" +
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
