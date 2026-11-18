package com.rtms.backend.ai.service;

import java.util.*;

/**
 * Formats tool execution results as Markdown and maps raw JSON keys to human-readable English labels.
 */
public class ToolResultFormatter {

    public String formatFallbackToolSummary(List<Map<String, Object>> executedTools) {
        return formatFallbackToolSummary(executedTools, false);
    }

    /**
     * Kept for API compatibility. Fallback summaries are always rendered in English; the legacy
     * {@code isVi} flag is ignored so no Vietnamese text leaks into AI responses.
     */
    public String formatFallbackToolSummary(List<Map<String, Object>> executedTools, boolean isVi) {
        StringBuilder sb = new StringBuilder();
        sb.append("Here is the detailed information retrieved from the system:\n\n");
        for (Map<String, Object> entry : executedTools) {
            Object res = entry.get("result");
            if (res instanceof Map<?, ?> map) {
                if ("ERROR".equals(map.get("status"))) {
                    sb.append("⚠️ **Notice**: ").append(map.get("error")).append("\n\n");
                    continue;
                }
                for (Map.Entry<?, ?> e : map.entrySet()) {
                    String key = String.valueOf(e.getKey());
                    Object val = e.getValue();
                    String label = humanizeKeyEn(key);
                    if (val instanceof List<?> list && !list.isEmpty()) {
                        sb.append("### ").append(label).append("\n");
                        appendListAsMarkdown(sb, list);
                        sb.append("\n");
                    } else if (val != null && !(val instanceof Map)) {
                        sb.append("- **").append(label).append("**: ").append(val).append("\n");
                    }
                }
                sb.append("\n");
            } else if (res instanceof List<?> list && !list.isEmpty()) {
                appendListAsMarkdown(sb, list);
                sb.append("\n");
            }
        }
        return sb.toString().trim();
    }

    private void appendListAsMarkdown(StringBuilder sb, List<?> list) {
        if (list.isEmpty()) return;
        Object first = list.get(0);
        if (first instanceof Map<?, ?> firstMap) {
            List<String> headers = new ArrayList<>();
            for (Object k : firstMap.keySet()) {
                headers.add(String.valueOf(k));
            }
            sb.append("| ").append(String.join(" | ", headers.stream().map(this::humanizeKeyEn).toList())).append(" |\n");
            sb.append("| ").append(String.join(" | ", headers.stream().map(h -> "---").toList())).append(" |\n");
            for (Object item : list) {
                if (item instanceof Map<?, ?> m) {
                    List<String> values = new ArrayList<>();
                    for (String h : headers) {
                        Object v = m.get(h);
                        values.add(v != null ? String.valueOf(v).replace("\n", " ") : "-");
                    }
                    sb.append("| ").append(String.join(" | ", values)).append(" |\n");
                }
            }
        } else {
            for (Object item : list) {
                sb.append("- ").append(item).append("\n");
            }
        }
    }

    /**
     * Legacy entry point kept for backward compatibility. Always returns the English label.
     */
    public String humanizeKey(String key) {
        return humanizeKeyEn(key);
    }

    public String humanizeKeyEn(String key) {
        if (key == null || key.isBlank()) return "";
        return switch (key) {
            // Horse & Pedigree
            case "horseName", "tenNgua" -> "Horse Name";
            case "horseId", "maSoNgua" -> "Horse ID";
            case "registrationNumber", "maDangKy" -> "Registration Number";
            case "candidateName" -> "Candidate Horse Name";
            case "sex" -> "Sex";
            case "dateOfBirth" -> "Date of Birth";
            case "trainingDecisionReason" -> "Training Restriction Reason";
            case "registryName" -> "Registry Name";
            case "sireRegistrationNumber" -> "Sire Registration (UELN)";
            case "damRegistrationNumber" -> "Dam Registration (UELN)";
            case "pedigreeNotes" -> "Pedigree Notes";
            case "breed" -> "Breed";
            case "sire" -> "Sire";
            case "dam" -> "Dam";
            case "origin" -> "Origin";
            case "pedigreeGeneration" -> "Pedigree Generation";
            case "joinedAt" -> "Joined Date";
            case "horseList", "myHorseList", "danhSachNgua" -> "Horse List";
            case "caredHorseList", "danhSachNguaDangChamSoc" -> "Assigned Horses";
            case "horseInStall" -> "Horse in Stall";
            case "totalOwnedHorses", "totalClubHorses", "horseCount", "tongSoNguaQuanLy" -> "Total Horses";

            // Vitals & Health
            case "weight", "canNang" -> "Weight (kg)";
            case "temperature", "thanNhiet" -> "Body Temperature (°C)";
            case "heartRate", "avgHeartRate", "maxHeartRate", "nhipTim" -> "Heart Rate (bpm)";
            case "respiratoryRate", "nhipTho" -> "Respiratory Rate";
            case "bodyConditionScore" -> "Body Condition Score (BCS 1-9)";
            case "hydrationStatus" -> "Hydration Status";
            case "clinicalNotes", "clinicalObservations", "notes", "nhanXet" -> "Clinical Observations";
            case "diagnosis", "latestDiagnosis" -> "Diagnosis";
            case "treatmentPlan" -> "Treatment Plan";
            case "treatmentPlanName" -> "Treatment Plan Name";
            case "medicationName" -> "Medication";
            case "dosage" -> "Dosage";
            case "frequency" -> "Frequency";
            case "route" -> "Route";
            case "duration" -> "Duration (days)";
            case "withdrawalDays" -> "Withdrawal Period (days)";
            case "administeredByRole" -> "Administered By Role";
            case "recordType" -> "Record Type";
            case "veterinarianName" -> "Attending Veterinarian";
            case "rescheduleReason" -> "Reschedule Reason";
            case "rescheduleNote" -> "Reschedule Note";
            case "activeInjuries" -> "Active Injuries";
            case "conditionType" -> "Condition Type";
            case "expectedRecoveryDate" -> "Expected Recovery Date";
            case "zoneId" -> "Anatomical Zone";
            case "medicalRestrictionNotes", "restrictionDetails" -> "Medical Restrictions";
            case "symptoms", "intakeSymptoms" -> "Symptoms";
            case "severity" -> "Severity";
            case "measurementDate", "ngayDo" -> "Measurement Date";
            case "examDate", "lastExamDate" -> "Examination Date";
            case "examType" -> "Examination Type";
            case "totalUrgentAlerts" -> "Urgent Care Alerts";
            case "alertList" -> "Alert List";
            case "totalPrescriptions" -> "Active Prescriptions";
            case "treatmentList" -> "Treatment List";
            case "treatmentType" -> "Treatment Type";
            case "careType" -> "Care Type";
            case "purpose" -> "Care Purpose";
            case "upcomingCareSchedule" -> "Upcoming Care Schedule";
            case "inQuarantineArea" -> "In Quarantine Area";
            case "abnormalSigns" -> "Abnormal Signs";
            case "alertLevel" -> "Alert Level";
            case "immediateActionRequired" -> "Immediate Action Required";
            case "commonConditions" -> "Common Conditions";
            case "recurrenceRisk" -> "Recurrence Risk";
            case "preventiveRecommendations" -> "Preventive Recommendations";
            case "recommendedIntervention" -> "Recommended Intervention";
            case "requiredTests" -> "Required Diagnostic Tests";
            case "minObservationTime" -> "Minimum Observation Period";
            case "referenceGuidelines" -> "Reference Guidelines";
            case "readinessScore" -> "Readiness Score";
            case "readinessStatus" -> "Readiness Status";
            case "vetRecommendations" -> "Veterinary Recommendations";
            case "proposedTrainingRestriction" -> "Proposed Training Restriction";

            // Stables & Capacity
            case "stallLocation", "stall", "stallCode", "stallNumber", "viTriChuong" -> "Stall Location";
            case "areaName", "areaCode", "areaType", "khuVuc" -> "Barn / Area";
            case "totalStalls" -> "Total Stalls";
            case "occupiedStalls" -> "Occupied Stalls";
            case "vacantStalls" -> "Vacant Stalls";
            case "occupiedStallsCount" -> "Occupied Stalls Count";
            case "vacantStallsCount" -> "Vacant Stalls Count";
            case "occupiedRegularStalls" -> "Occupied Regular Stalls";
            case "quarantineCasesCount" -> "Quarantine Cases";
            case "occupancyRate" -> "Occupancy Rate";
            case "bottleneckForecast" -> "Stabling Bottleneck Forecast";
            case "managementRecommendations" -> "Management Recommendations";
            case "stallStatus" -> "Stall Status";
            case "stallCodeList", "danhSachMaChuong" -> "Assigned Stalls";

            // Staff & Assignments
            case "fullName" -> "Full Name";
            case "email" -> "Email";
            case "phone", "groomPhone", "soDienThoai" -> "Phone";
            case "role", "roleTitle", "vaiTro" -> "Role";
            case "activeStatus" -> "Active Status";
            case "specialtyInfo" -> "Specialty Info";
            case "address" -> "Address";
            case "joinedDate" -> "Joined Date";
            case "certificationNumber" -> "Certification Number";
            case "licenseNumber" -> "License Number";
            case "specialization" -> "Specialization";
            case "totalStaff", "tongSoNhanSu" -> "Total Staff";
            case "roleBreakdown", "phanLoaiTheoVaiTro" -> "Role Breakdown";
            case "staffList", "danhSachNhanSu" -> "Staff List";
            case "assignedGroomName", "groomName", "assignedGroom", "tenGroom" -> "Assigned Groom";
            case "assignedGroomCount" -> "Assigned Groom Count";
            case "assignedTrainer", "tenHuanLuyenVien" -> "Assigned Trainer";
            case "assignedVet" -> "Assigned Vet";
            case "designatedTrainer" -> "Designated Trainer";
            case "assignedStallCount", "soChuongPhuTrach" -> "Assigned Stall Count";
            case "totalGrooms", "tongSoGroomLamViecCung" -> "Total Grooms";
            case "tasksTodayCount" -> "Tasks Today Count";
            case "workloadStatus" -> "Workload Status";
            case "groomWorkloadList" -> "Groom Workload List";
            case "overloadedStaff" -> "Overloaded Staff";
            case "proposedStallTransfer" -> "Proposed Stall Transfer";

            // Training & Workouts
            case "trainingDecision", "isTrainingAllowed", "quyetDinhTapLuyen" -> "Training Decision";
            case "decisionReason" -> "Decision Reason";
            case "totalWorkouts" -> "Total Workouts";
            case "completedSessions" -> "Completed Sessions (Ran)";
            case "totalSessions" -> "Total Sessions";
            case "groupName" -> "Training Group";
            case "joinedOn" -> "Joined Date";
            case "outcome" -> "Outcome";
            case "needsResultCount" -> "Needs Result Count";
            case "totalWorkoutsNeedingResult" -> "Total Workouts Needing Result";
            case "totalGroups", "groupList" -> "Active Training Groups";
            case "disbandedGroupCount", "disbandedGroupList" -> "Disbanded Groups";
            case "currentCourse" -> "Current Course";
            case "currentPlanStatus" -> "Course Status";
            case "currentPlanPeriod" -> "Course Period";
            case "planName" -> "Plan Name";
            case "courseName" -> "Course Name";
            case "exerciseName" -> "Training Exercise";
            case "targetGoal" -> "Target Goal";
            case "progress" -> "Progress";
            case "startDate" -> "Start Date";
            case "endDate" -> "End Date";
            case "startTime" -> "Start Time";
            case "endTime" -> "End Time";
            case "sessionTime" -> "Session Time";
            case "surface" -> "Track Surface";
            case "preferredSurface" -> "Preferred Surface";
            case "intensity" -> "Intensity";
            case "durationMinutes" -> "Duration (mins)";
            case "targetDistance" -> "Target Distance";
            case "actualDistanceMeters" -> "Actual Distance (m)";
            case "actualDurationMinutes" -> "Actual Duration (mins)";
            case "averageSpeedKmh" -> "Average Speed (km/h)";
            case "videoUrl" -> "Workout Video URL";
            case "surfaceType" -> "Track Surface Type";
            case "intensityLevel" -> "Intensity Level";
            case "targetDistanceMeters" -> "Target Distance (m)";
            case "trainingDays" -> "Weekly Schedule";
            case "trainerName" -> "Trainer Name";
            case "topSpeedKmh", "optimalSpeed" -> "Optimal Speed (km/h)";
            case "fitnessProgressionRate" -> "Fitness Progression Rate";
            case "racingPotentialEvaluation" -> "Racing Potential Evaluation";
            case "totalDistance14Days" -> "Total 14-Day Distance (m)";
            case "workoutCount" -> "Workout Count";
            case "highIntensitySessions" -> "High Intensity Sessions";
            case "riskAssessment" -> "Risk Assessment";
            case "trainingRecommendations" -> "Training Recommendations";
            case "lastWorkout" -> "Last Workout";
            case "fitnessReview" -> "Fitness Review";

            // Rankings & Specialties
            case "specialties" -> "Specialties";
            case "specialtyCount" -> "Specialty Count";
            case "averageRating" -> "Average Rating";
            case "bestRating" -> "Best Rating";
            case "specialtyBreakdown" -> "Specialty Breakdown";
            case "rankedHorseList" -> "Ranked Horse List";
            case "sortBy" -> "Sort By";
            case "rank" -> "Rank";

            // Admissions
            case "totalApplications" -> "Total Applications";
            case "inProgressApplications" -> "In-Progress Applications";
            case "approvedApplications" -> "Approved Applications";
            case "rejectedOrExpiredApplications" -> "Rejected or Expired Applications";
            case "pendingManagerReviewCount" -> "Pending Manager Review Count";
            case "pendingAdmissionReviews" -> "Pending Admission Reviews";
            case "quarantineStallCode" -> "Quarantine Stall";
            case "groomFeedback" -> "Groom Feedback";
            case "vetFeedback" -> "Vet Feedback";
            case "trainerFeedback" -> "Trainer Feedback";
            case "managerFeedback" -> "Manager Feedback";
            case "currentStage" -> "Current Stage";
            case "submittedDate" -> "Submission Date";
            case "ownerName" -> "Owner Name";
            case "groomCheckResult" -> "Groom Intake Check";
            case "vetExamResult" -> "Vet Screening Exam";
            case "managerApprovalResult" -> "Manager Final Approval";
            case "pendingExamCount" -> "Pending Vet Exams";
            case "examList" -> "Pending Exam List";
            case "pendingReviewCount" -> "Pending Trainer Reviews";
            case "stageBreakdown" -> "Stage Breakdown";

            // Races
            case "location" -> "Racecourse / Location";
            case "distanceMeters" -> "Distance (m)";
            case "trackType" -> "Track Type";
            case "eventDate" -> "Event Date";
            case "eventTime" -> "Start Time";

            // Audit
            case "action" -> "Action";
            case "entityName" -> "Entity Name";
            case "entityId" -> "Entity ID";
            case "httpMethod" -> "HTTP Method";
            case "requestPath" -> "Request Path";
            case "statusCode" -> "Status Code";
            case "details" -> "Details";
            case "performedBy" -> "Performed By";
            case "changeDetails" -> "Change Details";
            case "totalAuditLogs" -> "Total Audit Logs";
            case "auditLogList", "danhSachNhatKy" -> "Audit Log List";

            // Common
            case "status", "currentStatus", "horseStatus", "trangThai" -> "Status";
            case "systemStatus" -> "System Status";
            case "description", "detailedDescription" -> "Description";
            case "actionUrl", "navigationUrl" -> "Navigation URL";

            // Legacy Vietnamese keys still emitted by a few tool handlers
            case "chienLuocCuLyVaSanDua" -> "Distance and Surface Strategy";
            case "dieuChinhBaiTapKeTiep" -> "Next Workout Adjustment";
            case "caTrucCapCuu" -> "Urgent Care Duty";
            case "suCoChuongTrai48h" -> "Stable Incidents (48h)";
            case "tongQuanSoLuong" -> "Quantity Overview";
            case "chiTietCacDon" -> "Application Details";
            default -> {
                String spaced = key.replaceAll("([a-z])([A-Z])", "$1 $2");
                yield Character.toUpperCase(spaced.charAt(0)) + spaced.substring(1);
            }
        };
    }
}
