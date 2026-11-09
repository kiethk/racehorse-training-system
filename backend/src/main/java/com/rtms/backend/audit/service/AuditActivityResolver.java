package com.rtms.backend.audit.service;

import org.springframework.stereotype.Component;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Maps technical HTTP methods and paths to clear, manager-friendly English business activities.
 */
@Component
public class AuditActivityResolver {

    public record ResolvedActivity(
            String action,
            String entityName,
            Long entityId,
            String details
    ) {}

    // Pattern definitions - Admissions
    private static final Pattern ADMISSION_GROOM_REVIEW = Pattern.compile("^/api/admissions/(\\d+)/groom-review/?$");
    private static final Pattern ADMISSION_ARRIVAL_CONFIRM = Pattern.compile("^/api/admissions/(\\d+)/confirm-arrival/?$");
    private static final Pattern ADMISSION_WAITING_STALL = Pattern.compile("^/api/admissions/(\\d+)/process-waiting-stall/?$");
    private static final Pattern ADMISSION_VET_REVIEW = Pattern.compile("^/api/admissions/(\\d+)/vet-review/?$");
    private static final Pattern ADMISSION_TRAINER_REVIEW = Pattern.compile("^/api/admissions/(\\d+)/trainer-assessment/?$");
    private static final Pattern ADMISSION_MANAGER_REVIEW = Pattern.compile("^/api/admissions/(\\d+)/manager-review/?$");
    private static final Pattern ADMISSION_DETAIL = Pattern.compile("^/api/admissions/(\\d+)/?$");
    private static final Pattern ADMISSION_BASE = Pattern.compile("^/api/admissions/?$");
    private static final Pattern ADMISSION_SUBMIT = Pattern.compile("^/api/owner/admissions/?$");

    // Pattern definitions - Stable & Stalls
    private static final Pattern STABLE_ASSIGNMENTS = Pattern.compile("^/api/areas/assignments/?$");
    private static final Pattern STABLE_AREAS_DETAIL = Pattern.compile("^/api/areas/(\\d+)/?$");
    private static final Pattern STABLE_AREAS_BASE = Pattern.compile("^/api/areas/?$");
    private static final Pattern STABLE_STALL_DETAIL = Pattern.compile("^/api/stalls/(\\d+)/?$");
    private static final Pattern STABLE_STALL_BASE = Pattern.compile("^/api/stalls/?$");
    private static final Pattern STALL_ASSIGN_GROOM = Pattern.compile("^/api/stalls/(\\d+)/assign-groom/?$");
    private static final Pattern AREA_ASSIGN_TRAINER = Pattern.compile("^/api/areas/(\\d+)/trainer/?$");
    private static final Pattern AREA_ASSIGN_GROOM = Pattern.compile("^/api/areas/(\\d+)/blocks/(\\d+)/groom/?$");
    private static final Pattern GROOM_TASK_COMPLETE = Pattern.compile("^/api/groom-daily-tasks/(\\d+)/complete/?$");
    private static final Pattern GROOM_GENERATE_ROUTINE = Pattern.compile("^/api/groom-daily-tasks/generate-routine/?$");
    private static final Pattern GROOM_DAILY_TASKS_DETAIL = Pattern.compile("^/api/groom-daily-tasks/(\\d+)/.*$");
    private static final Pattern GROOM_DAILY_TASKS_BASE = Pattern.compile("^/api/groom-daily-tasks/?$");

    // Pattern definitions - Staff & Accounts & Users
    private static final Pattern ACCOUNT_STATUS = Pattern.compile("^/api/accounts/(\\d+)/status/?$");
    private static final Pattern ACCOUNT_DETAIL = Pattern.compile("^/api/accounts/(\\d+)/?$");
    private static final Pattern ACCOUNT_BASE = Pattern.compile("^/api/accounts/?$");
    private static final Pattern STAFF_STATUS = Pattern.compile("^/api/staff/(\\d+)/status/?$");
    private static final Pattern STAFF_DETAIL = Pattern.compile("^/api/staff/(\\d+)/?$");
    private static final Pattern STAFF_BASE = Pattern.compile("^/api/staff/?$");
    private static final Pattern USER_DETAIL = Pattern.compile("^/api/users/(\\d+)/?$");
    private static final Pattern USER_BASE = Pattern.compile("^/api/users/?$");

    // Pattern definitions - Training
    private static final Pattern TRAINING_PLANS_DETAIL = Pattern.compile("^/api/group-plans/(\\d+)/.*$");
    private static final Pattern TRAINING_PLANS_ID = Pattern.compile("^/api/group-plans/(\\d+)/?$");
    private static final Pattern TRAINING_PLANS_BASE = Pattern.compile("^/api/group-plans/?$");
    private static final Pattern TRAINING_GROUP_MOVE = Pattern.compile("^/api/training-groups/(\\d+)/horses/move/?$");
    private static final Pattern SESSION_WORKOUT_RESULT = Pattern.compile("^/api/session-workouts/(\\d+)/result/?$");
    private static final Pattern SESSION_WORKOUT_DNR = Pattern.compile("^/api/session-workouts/(\\d+)/did-not-run/?$");

    private static final Pattern COURSES_DETAIL = Pattern.compile("^/api/courses/(\\d+)/?$");
    private static final Pattern COURSES_BASE = Pattern.compile("^/api/courses/?$");

    private static final Pattern SUBJECTS_DETAIL = Pattern.compile("^/api/subjects/(\\d+)/?$");
    private static final Pattern SUBJECTS_BASE = Pattern.compile("^/api/subjects/?$");

    private static final Pattern LOTS_DETAIL = Pattern.compile("^/api/training-sessions/(\\d+)/?$");
    private static final Pattern LOTS_BASE = Pattern.compile("^/api/training-sessions/?$");

    private static final Pattern WORKOUTS_DETAIL = Pattern.compile("^/api/workouts/(\\d+)/?$");
    private static final Pattern WORKOUTS_BASE = Pattern.compile("^/api/workouts/?$");

    private static final Pattern TRAINING_RULES = Pattern.compile("^/api/training-rules/?.*$");

    // Pattern definitions - Medical & Health
    private static final Pattern CARE_START = Pattern.compile("^/api/(?:care-schedules|vet/schedules)/(\\d+)/start/?$");
    private static final Pattern CARE_COMPLETE = Pattern.compile("^/api/(?:care-schedules|vet/schedules)/(\\d+)/complete/?$");
    private static final Pattern CARE_CANCEL = Pattern.compile("^/api/(?:care-schedules|vet/schedules)/(\\d+)/cancel/?$");
    private static final Pattern CARE_CREATE_NEXT = Pattern.compile("^/api/(?:care-schedules|vet/schedules)/create-next/?$");
    private static final Pattern CARE_RESCHEDULE = Pattern.compile("^/api/care-schedules/(\\d+)/reschedule/?$");
    private static final Pattern CARE_SCHEDULE_DETAIL = Pattern.compile("^/api/care-schedules/(\\d+)/?$");
    private static final Pattern CARE_SCHEDULE_BASE = Pattern.compile("^/api/care-schedules/?$");
    private static final Pattern VET_SCHEDULES = Pattern.compile("^/api/vet/schedules/?.*$");
    private static final Pattern PREVENTIVE_CARE = Pattern.compile("^/api/preventive-care-schedules/?.*$");
    private static final Pattern HEALTH_RECORDS = Pattern.compile("^/api/health-records/?.*$");
    private static final Pattern TREATMENT_PLANS = Pattern.compile("^/api/treatment-plans/?.*$");
    private static final Pattern TREATMENT_PRESCRIPTIONS = Pattern.compile("^/api/treatment-plans/(\\d+)/prescriptions/?$");
    private static final Pattern TREATMENT_PLAN_STATUS = Pattern.compile("^/api/(?:health-records/)?treatment-plans/(\\d+)/status/?$");
    private static final Pattern INJURY_RECOVER = Pattern.compile("^/api/horses/(\\d+)/injury-records/(\\d+)/recover/?$");
    private static final Pattern INJURY_CORRECTIONS = Pattern.compile("^/api/horses/(\\d+)/injury-records/(\\d+)/corrections/?$");
    private static final Pattern HORSE_VACCINATIONS = Pattern.compile("^/api/horses/(\\d+)/vaccinations/?$");
    private static final Pattern INCIDENTS = Pattern.compile("^/api/(incidents|groom-incident-reports)/?.*$");

    // Pattern definitions - Horses
    private static final Pattern HORSES_STATUS = Pattern.compile("^/api/horses/(\\d+)/status/?$");
    private static final Pattern HORSES_DETAIL = Pattern.compile("^/api/horses/(\\d+)/?$");
    private static final Pattern HORSES_BASE = Pattern.compile("^/api/horses/?$");
    private static final Pattern HORSE_PEDIGREE = Pattern.compile("^/api/horses/(\\d+)/pedigree/?$");
    private static final Pattern HORSE_EDIT_OWNER = Pattern.compile("^/api/owner/horses/(\\d+)/edit-requests/?$");
    private static final Pattern HORSE_EDIT_GROOM = Pattern.compile("^/api/groom/horse-edit-requests/(\\d+)/review/?$");

    public ResolvedActivity resolve(String method, String path, int statusCode) {
        String normalizedMethod = method == null ? "POST" : method.toUpperCase();
        String normalizedPath = path == null ? "" : path.trim();
        boolean isSuccess = statusCode >= 200 && statusCode < 300;
        String statusSuffix = isSuccess ? "" : " (Attempt failed)";

        Matcher m;

        // 0. AI Assistant & HITL
        if (normalizedPath.contains("/ai/chat") || normalizedPath.contains("/ai/conversations")) {
            return new ResolvedActivity(
                    "AI Assistant Consultation",
                    "AI Assistant",
                    null,
                    "Interacted with AI Assistant for consultation" + statusSuffix
            );
        }
        if (normalizedPath.contains("/ai/actions/commit")) {
            return new ResolvedActivity(
                    "AI Action Commit",
                    "AI Action",
                    null,
                    "Committed confirmed action recommended by AI Assistant" + statusSuffix
            );
        }

        // 1. Admissions Workflow
        if ((m = ADMISSION_GROOM_REVIEW.matcher(normalizedPath)).matches()) {
            Long id = parseId(m.group(1));
            return new ResolvedActivity(
                    "Groom Admission Review",
                    "AdmissionApplication",
                    id,
                    "Groom reviewed intake dossier and verified arrival schedule for application #" + id + statusSuffix
            );
        }
        if ((m = ADMISSION_ARRIVAL_CONFIRM.matcher(normalizedPath)).matches()) {
            Long id = parseId(m.group(1));
            return new ResolvedActivity(
                    "Confirm Horse Arrival",
                    "AdmissionApplication",
                    id,
                    "Confirmed physical arrival of horse into quarantine for application #" + id + statusSuffix
            );
        }
        if ((m = ADMISSION_WAITING_STALL.matcher(normalizedPath)).matches()) {
            Long id = parseId(m.group(1));
            return new ResolvedActivity(
                    "Move To Waiting For Stall",
                    "AdmissionApplication",
                    id,
                    "Moved application #" + id + " to waiting queue for regular stall assignment" + statusSuffix
            );
        }
        if ((m = ADMISSION_VET_REVIEW.matcher(normalizedPath)).matches()) {
            Long id = parseId(m.group(1));
            return new ResolvedActivity(
                    "Veterinarian Intake Exam",
                    "AdmissionApplication",
                    id,
                    "Veterinarian performed initial clinical examination for application #" + id + statusSuffix
            );
        }
        if ((m = ADMISSION_TRAINER_REVIEW.matcher(normalizedPath)).matches()) {
            Long id = parseId(m.group(1));
            return new ResolvedActivity(
                    "Trainer Fitness Assessment",
                    "AdmissionApplication",
                    id,
                    "Head Trainer assessed conformation, gait, and racing readiness for application #" + id + statusSuffix
            );
        }
        if ((m = ADMISSION_MANAGER_REVIEW.matcher(normalizedPath)).matches()) {
            Long id = parseId(m.group(1));
            return new ResolvedActivity(
                    "Manager Admission Approval",
                    "AdmissionApplication",
                    id,
                    "Club Manager approved application #" + id + " and allocated regular stable stall" + statusSuffix
            );
        }
        if ((m = ADMISSION_SUBMIT.matcher(normalizedPath)).matches()) {
            return new ResolvedActivity(
                    "Submit Admission Application",
                    "AdmissionApplication",
                    null,
                    "Horse owner submitted a new admission application dossier" + statusSuffix
            );
        }
        if ((m = ADMISSION_DETAIL.matcher(normalizedPath)).matches()) {
            Long id = parseId(m.group(1));
            String action = "DELETE".equals(normalizedMethod) ? "Delete Admission Application" : "Update Admission Application";
            return new ResolvedActivity(
                    action,
                    "AdmissionApplication",
                    id,
                    action + " #" + id + statusSuffix
            );
        }

        // 2. Training Management
        if ((m = COURSES_DETAIL.matcher(normalizedPath)).matches()) {
            Long id = parseId(m.group(1));
            String action = "DELETE".equals(normalizedMethod) ? "Delete Training Course" : "Update Training Course";
            return new ResolvedActivity(
                    action,
                    "Course",
                    id,
                    ("DELETE".equals(normalizedMethod) ? "Removed training curriculum course #" : "Updated training curriculum course #") + id + statusSuffix
            );
        }
        if (COURSES_BASE.matcher(normalizedPath).matches() && "POST".equals(normalizedMethod)) {
            return new ResolvedActivity(
                    "Create Training Course",
                    "Course",
                    null,
                    "Trainer registered a new curriculum course" + statusSuffix
            );
        }

        if ((m = TRAINING_PLANS_DETAIL.matcher(normalizedPath)).matches() || (m = TRAINING_PLANS_ID.matcher(normalizedPath)).matches()) {
            Long id = parseId(m.group(1));
            String action = "DELETE".equals(normalizedMethod) ? "Cancel Training Plan" : "Update Training Plan";
            return new ResolvedActivity(
                    action,
                    "GroupPlan",
                    id,
                    ("DELETE".equals(normalizedMethod) ? "Removed training plan #" : "Updated milestones and schedule for training plan #") + id + statusSuffix
            );
        }
        if (TRAINING_PLANS_BASE.matcher(normalizedPath).matches() && "POST".equals(normalizedMethod)) {
            return new ResolvedActivity(
                    "Create Training Plan",
                    "GroupPlan",
                    null,
                    "Trainer created a new training plan for horse" + statusSuffix
            );
        }

        if ((m = SUBJECTS_DETAIL.matcher(normalizedPath)).matches()) {
            Long id = parseId(m.group(1));
            String action = "DELETE".equals(normalizedMethod) ? "Delete Training Subject" : "Update Training Subject";
            return new ResolvedActivity(
                    action,
                    "Subject",
                    id,
                    ("DELETE".equals(normalizedMethod) ? "Removed training subject #" : "Updated training subject #") + id + statusSuffix
            );
        }
        if (SUBJECTS_BASE.matcher(normalizedPath).matches() && "POST".equals(normalizedMethod)) {
            return new ResolvedActivity(
                    "Create Training Subject",
                    "Subject",
                    null,
                    "Trainer created a new training discipline/subject" + statusSuffix
            );
        }

        if ((m = LOTS_DETAIL.matcher(normalizedPath)).matches()) {
            Long id = parseId(m.group(1));
            String action = "DELETE".equals(normalizedMethod) ? "Cancel Training Session" : "Update Training Session";
            return new ResolvedActivity(
                    action,
                    "TrainingSession",
                    id,
                    ("DELETE".equals(normalizedMethod) ? "Cancelled training session #" : "Updated training session #") + id + statusSuffix
            );
        }
        if (LOTS_BASE.matcher(normalizedPath).matches() && "POST".equals(normalizedMethod)) {
            return new ResolvedActivity(
                    "Schedule Training Session",
                    "TrainingSession",
                    null,
                    "Trainer scheduled a new training session" + statusSuffix
            );
        }

        if (WORKOUTS_DETAIL.matcher(normalizedPath).matches() || WORKOUTS_BASE.matcher(normalizedPath).matches()) {
            return new ResolvedActivity(
                    "Record Training Workout",
                    "TrainingWorkout",
                    null,
                    "Trainer logged workout session exercise and performance metrics" + statusSuffix
            );
        }

        if (TRAINING_RULES.matcher(normalizedPath).matches()) {
            return new ResolvedActivity(
                    "Update Training Rules",
                    "TrainingRule",
                    null,
                    "Configured training capacity, session limits, and scheduling safety rules" + statusSuffix
            );
        }

        if ((m = SESSION_WORKOUT_RESULT.matcher(normalizedPath)).matches()) {
            Long id = parseId(m.group(1));
            return new ResolvedActivity(
                    "Record Workout Result",
                    "TrainingWorkout",
                    id,
                    "Recorded workout performance result for workout #" + id + statusSuffix
            );
        }
        if ((m = SESSION_WORKOUT_DNR.matcher(normalizedPath)).matches()) {
            Long id = parseId(m.group(1));
            return new ResolvedActivity(
                    "Mark Workout Did Not Run",
                    "TrainingWorkout",
                    id,
                    "Marked horse did not run for workout #" + id + statusSuffix
            );
        }
        if (TRAINING_GROUP_MOVE.matcher(normalizedPath).matches()) {
            return new ResolvedActivity(
                    "Transfer Group Horse",
                    "TrainingGroup",
                    null,
                    "Transferred horse between training groups" + statusSuffix
            );
        }

        // 3. Stable & Stalls
        if ((m = GROOM_TASK_COMPLETE.matcher(normalizedPath)).matches()) {
            Long id = parseId(m.group(1));
            return new ResolvedActivity(
                    "Complete Groom Task",
                    "GroomDailyTask",
                    id,
                    "Groom completed daily care task #" + id + statusSuffix
            );
        }
        if (GROOM_GENERATE_ROUTINE.matcher(normalizedPath).matches()) {
            return new ResolvedActivity(
                    "Generate Daily Routine",
                    "GroomDailyTask",
                    null,
                    "Generated routine daily stable tasks" + statusSuffix
            );
        }
        if ((m = STALL_ASSIGN_GROOM.matcher(normalizedPath)).matches()) {
            Long id = parseId(m.group(1));
            return new ResolvedActivity(
                    "Assign Stall Groom",
                    "StableStall",
                    id,
                    "Assigned groom staff to stable stall #" + id + statusSuffix
            );
        }
        if ((m = AREA_ASSIGN_TRAINER.matcher(normalizedPath)).matches()) {
            Long id = parseId(m.group(1));
            return new ResolvedActivity(
                    "Assign Area Trainer",
                    "Area",
                    id,
                    "Assigned head trainer to stable area #" + id + statusSuffix
            );
        }
        if ((m = AREA_ASSIGN_GROOM.matcher(normalizedPath)).matches()) {
            Long id = parseId(m.group(1));
            return new ResolvedActivity(
                    "Assign Block Groom",
                    "Area",
                    id,
                    "Assigned groom to stable area #" + id + statusSuffix
            );
        }
        if (STABLE_ASSIGNMENTS.matcher(normalizedPath).matches()) {
            return new ResolvedActivity(
                    "Update Stable Assignments",
                    "StableStall",
                    null,
                    "Manager updated area trainer assignments and groom stall assignments" + statusSuffix
            );
        }
        if ((m = STABLE_STALL_DETAIL.matcher(normalizedPath)).matches()) {
            Long id = parseId(m.group(1));
            return new ResolvedActivity(
                    "Update Stall Details",
                    "StableStall",
                    id,
                    "Updated configuration and status for stable stall #" + id + statusSuffix
            );
        }
        if (STABLE_STALL_BASE.matcher(normalizedPath).matches() && "POST".equals(normalizedMethod)) {
            return new ResolvedActivity(
                    "Create Stable Stall",
                    "StableStall",
                    null,
                    "Created a new stable stall" + statusSuffix
            );
        }
        if (STABLE_AREAS_BASE.matcher(normalizedPath).matches() && "POST".equals(normalizedMethod)) {
            return new ResolvedActivity(
                    "Create Barn Area",
                    "Area",
                    null,
                    "Manager created a new stable area" + statusSuffix
            );
        }
        if (GROOM_DAILY_TASKS_DETAIL.matcher(normalizedPath).matches() || GROOM_DAILY_TASKS_BASE.matcher(normalizedPath).matches()) {
            return new ResolvedActivity(
                    "Update Daily Groom Task",
                    "GroomDailyTask",
                    null,
                    "Groom updated daily care task checklist and feeding log" + statusSuffix
            );
        }

        // 4. Staff & Account Management
        if ((m = ACCOUNT_STATUS.matcher(normalizedPath)).matches()) {
            Long id = parseId(m.group(1));
            return new ResolvedActivity(
                    "Update Account Status",
                    "User",
                    id,
                    "Manager changed active status for account #" + id + statusSuffix
            );
        }
        if ((m = ACCOUNT_DETAIL.matcher(normalizedPath)).matches()) {
            Long id = parseId(m.group(1));
            return new ResolvedActivity(
                    "Update Account Profile",
                    "User",
                    id,
                    "Updated contact or profile information for account #" + id + statusSuffix
            );
        }
        if (ACCOUNT_BASE.matcher(normalizedPath).matches() && "POST".equals(normalizedMethod)) {
            return new ResolvedActivity(
                    "Create Account",
                    "User",
                    null,
                    "Manager created a new account" + statusSuffix
            );
        }
        if ((m = STAFF_STATUS.matcher(normalizedPath)).matches()) {
            Long id = parseId(m.group(1));
            return new ResolvedActivity(
                    "Update Staff Status",
                    "User",
                    id,
                    "Manager changed active status for staff account #" + id + statusSuffix
            );
        }
        if ((m = STAFF_DETAIL.matcher(normalizedPath)).matches()) {
            Long id = parseId(m.group(1));
            return new ResolvedActivity(
                    "Update Staff Profile",
                    "User",
                    id,
                    "Updated contact and certification information for staff member #" + id + statusSuffix
            );
        }
        if (STAFF_BASE.matcher(normalizedPath).matches() && "POST".equals(normalizedMethod)) {
            return new ResolvedActivity(
                    "Create Staff Account",
                    "User",
                    null,
                    "Manager created a new staff member account" + statusSuffix
            );
        }

        // 5. Medical & Health
        if ((m = CARE_START.matcher(normalizedPath)).matches()) {
            Long id = parseId(m.group(1));
            return new ResolvedActivity(
                    "Start Clinical Examination",
                    "CareSchedule",
                    id,
                    "Veterinarian started clinical examination for appointment #" + id + statusSuffix
            );
        }
        if ((m = CARE_COMPLETE.matcher(normalizedPath)).matches()) {
            Long id = parseId(m.group(1));
            return new ResolvedActivity(
                    "Complete Clinical Examination",
                    "CareSchedule",
                    id,
                    "Veterinarian completed clinical examination for appointment #" + id + statusSuffix
            );
        }
        if ((m = CARE_CANCEL.matcher(normalizedPath)).matches()) {
            Long id = parseId(m.group(1));
            return new ResolvedActivity(
                    "Cancel Medical Appointment",
                    "CareSchedule",
                    id,
                    "Cancelled medical care appointment #" + id + statusSuffix
            );
        }
        if (CARE_CREATE_NEXT.matcher(normalizedPath).matches()) {
            return new ResolvedActivity(
                    "Schedule Follow-up Care",
                    "CareSchedule",
                    null,
                    "Scheduled follow-up medical care appointment" + statusSuffix
            );
        }
        if ((m = INJURY_RECOVER.matcher(normalizedPath)).matches()) {
            Long horseId = parseId(m.group(1));
            Long injuryId = parseId(m.group(2));
            return new ResolvedActivity(
                    "Resolve Horse Injury",
                    "InjuryRecord",
                    injuryId,
                    "Marked injury #" + injuryId + " on horse #" + horseId + " as recovered" + statusSuffix
            );
        }
        if ((m = INJURY_CORRECTIONS.matcher(normalizedPath)).matches()) {
            Long horseId = parseId(m.group(1));
            Long injuryId = parseId(m.group(2));
            return new ResolvedActivity(
                    "Correct Injury Record",
                    "InjuryRecord",
                    injuryId,
                    "Updated anatomical marker for injury #" + injuryId + " on horse #" + horseId + statusSuffix
            );
        }
        if ((m = HORSE_VACCINATIONS.matcher(normalizedPath)).matches()) {
            Long horseId = parseId(m.group(1));
            return new ResolvedActivity(
                    "Administer Vaccination",
                    "PreventiveCare",
                    horseId,
                    "Veterinarian administered vaccination for horse #" + horseId + statusSuffix
            );
        }
        if ((m = TREATMENT_PRESCRIPTIONS.matcher(normalizedPath)).matches()) {
            Long planId = parseId(m.group(1));
            return new ResolvedActivity(
                    "Prescribe Medication",
                    "Prescription",
                    planId,
                    "Added medication prescription to treatment plan #" + planId + statusSuffix
            );
        }
        if ((m = TREATMENT_PLAN_STATUS.matcher(normalizedPath)).matches()) {
            Long planId = parseId(m.group(1));
            return new ResolvedActivity(
                    "Update Treatment Plan Status",
                    "TreatmentPlan",
                    planId,
                    "Updated status for treatment plan #" + planId + statusSuffix
            );
        }
        if ((m = CARE_RESCHEDULE.matcher(normalizedPath)).matches()) {
            Long id = parseId(m.group(1));
            return new ResolvedActivity(
                    "Reschedule Medical Care",
                    "CareSchedule",
                    id,
                    "Rescheduled medical care appointment #" + id + statusSuffix
            );
        }
        if ((m = CARE_SCHEDULE_DETAIL.matcher(normalizedPath)).matches()) {
            Long id = parseId(m.group(1));
            return new ResolvedActivity(
                    "Update Medical Care",
                    "CareSchedule",
                    id,
                    "Updated medical care appointment #" + id + statusSuffix
            );
        }
        if (CARE_SCHEDULE_BASE.matcher(normalizedPath).matches() && "POST".equals(normalizedMethod)) {
            return new ResolvedActivity(
                    "Create Medical Care Schedule",
                    "CareSchedule",
                    null,
                    "Scheduled medical care appointment for horse" + statusSuffix
            );
        }
        if (PREVENTIVE_CARE.matcher(normalizedPath).matches()) {
            return new ResolvedActivity(
                    "Update Preventive Protocol",
                    "PreventiveCareSchedule",
                    null,
                    "Updated horse vaccination, deworming, or preventive care protocol" + statusSuffix
            );
        }
        if (HEALTH_RECORDS.matcher(normalizedPath).matches() && "POST".equals(normalizedMethod)) {
            return new ResolvedActivity(
                    "Record Clinical Examination",
                    "HealthRecord",
                    null,
                    "Veterinarian documented horse health examination record" + statusSuffix
            );
        }
        if (TREATMENT_PLANS.matcher(normalizedPath).matches() && "POST".equals(normalizedMethod)) {
            return new ResolvedActivity(
                    "Prescribe Treatment Plan",
                    "TreatmentPlan",
                    null,
                    "Veterinarian prescribed medical treatment plan and medications" + statusSuffix
            );
        }
        if (INCIDENTS.matcher(normalizedPath).matches() && "POST".equals(normalizedMethod)) {
            return new ResolvedActivity(
                    "Report Horse Incident",
                    "IncidentReport",
                    null,
                    "Documented horse injury or health incident report" + statusSuffix
            );
        }

        // 6. Horse Management
        if ((m = HORSES_STATUS.matcher(normalizedPath)).matches()) {
            Long id = parseId(m.group(1));
            return new ResolvedActivity(
                    "Change Horse Status",
                    "Horse",
                    id,
                    "Updated lifecycle status for horse #" + id + statusSuffix
            );
        }
        if ((m = HORSES_DETAIL.matcher(normalizedPath)).matches()) {
            Long id = parseId(m.group(1));
            return new ResolvedActivity(
                    "Update Horse Profile",
                    "Horse",
                    id,
                    "Updated identity or registration attributes for horse #" + id + statusSuffix
            );
        }
        if (HORSES_BASE.matcher(normalizedPath).matches() && "POST".equals(normalizedMethod)) {
            return new ResolvedActivity(
                    "Register Horse",
                    "Horse",
                    null,
                    "Registered new horse into club database" + statusSuffix
            );
        }
        if ((m = HORSE_PEDIGREE.matcher(normalizedPath)).matches()) {
            Long id = parseId(m.group(1));
            return new ResolvedActivity(
                    "Update Horse Pedigree",
                    "Horse",
                    id,
                    "Updated sire, dam, and pedigree lineage for horse #" + id + statusSuffix
            );
        }
        if ((m = HORSE_EDIT_OWNER.matcher(normalizedPath)).matches()) {
            Long id = parseId(m.group(1));
            return new ResolvedActivity(
                    "Submit Horse Edit Request",
                    "HorseEditRequest",
                    id,
                    "Owner submitted profile update request for horse #" + id + statusSuffix
            );
        }
        if ((m = HORSE_EDIT_GROOM.matcher(normalizedPath)).matches()) {
            Long id = parseId(m.group(1));
            return new ResolvedActivity(
                    "Review Horse Edit Request",
                    "HorseEditRequest",
                    id,
                    "Groom reviewed horse profile change request #" + id + statusSuffix
            );
        }

        // 7. Personal Profile Update
        if (normalizedPath.startsWith("/api/auth/me/profile")) {
            return new ResolvedActivity(
                    "Update Personal Profile",
                    "User",
                    null,
                    "User updated their personal profile and contact details" + statusSuffix
            );
        }

        // Clean Fallback: Never display raw technical HTTP methods or /api/ URLs
        String entityName = guessEntity(normalizedPath);
        String actionVerb = switch (normalizedMethod) {
            case "POST" -> "Create";
            case "PUT", "PATCH" -> "Update";
            case "DELETE" -> "Remove";
            default -> "Update";
        };
        String friendlyEntity = humanizeEntity(entityName);
        String actionTitle = "System".equalsIgnoreCase(entityName)
                ? actionVerb + " Resource"
                : actionVerb + " " + friendlyEntity;
        return new ResolvedActivity(
                actionTitle,
                entityName,
                null,
                (isSuccess ? "Successfully processed " : "Attempted ")
                        + actionVerb.toLowerCase() + " operation for " + friendlyEntity.toLowerCase()
                        + statusSuffix
        );
    }

    private static Long parseId(String s) {
        try {
            return Long.parseLong(s);
        } catch (Exception e) {
            return null;
        }
    }

    private static String guessEntity(String path) {
        if (path.contains("/admissions")) return "AdmissionApplication";
        if (path.contains("/courses")) return "Course";
        if (path.contains("/group-plans")) return "GroupPlan";
        if (path.contains("/training-groups")) return "TrainingGroup";
        if (path.contains("/subjects")) return "Subject";
        if (path.contains("/training-sessions")) return "TrainingSession";
        if (path.contains("/workouts") || path.contains("/session-workouts")) return "TrainingWorkout";
        if (path.contains("/training-rules")) return "TrainingRule";
        if (path.contains("/horses")) return "Horse";
        if (path.contains("/staff") || path.contains("/users") || path.contains("/accounts")) return "Staff";
        if (path.contains("/stalls") || path.contains("/areas")) return "Stable";
        if (path.contains("/care-schedules") || path.contains("/health-records") || path.contains("/preventive")) return "Medical";
        if (path.contains("/incidents")) return "IncidentReport";
        if (path.contains("/ai")) return "AI Assistant";
        return "System";
    }

    private static String humanizeEntity(String entityName) {
        return switch (entityName) {
            case "AdmissionApplication" -> "Admission Application";
            case "GroupPlan" -> "Training Plan";
            case "Course" -> "Training Course";
            case "Subject" -> "Training Subject";
            case "TrainingSession" -> "Training Session";
            case "TrainingWorkout" -> "Workout Session";
            case "TrainingRule" -> "Training Rules";
            case "IncidentReport" -> "Incident Report";
            case "StableStall" -> "Stable Stall";
            case "CareSchedule" -> "Medical Schedule";
            case "HealthRecord" -> "Health Record";
            case "HorseEditRequest" -> "Horse Edit Request";
            case "InjuryRecord" -> "Injury Record";
            case "Prescription" -> "Prescription";
            case "PreventiveCare" -> "Preventive Care";
            case "GroomDailyTask" -> "Groom Daily Task";
            default -> entityName;
        };
    }
}
