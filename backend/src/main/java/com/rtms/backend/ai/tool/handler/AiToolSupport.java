package com.rtms.backend.ai.tool.handler;

import com.rtms.backend.config.ApiException;
import com.rtms.backend.config.TrainingLimits;
import com.rtms.backend.horse.entity.Horse;
import com.rtms.backend.horse.repository.HorseRepository;
import com.rtms.backend.identity.entity.User;
import com.rtms.backend.identity.repository.UserRepository;
import com.rtms.backend.security.AuthenticatedUser;
import com.rtms.backend.stable.entity.Area;
import com.rtms.backend.stable.entity.StableStall;
import com.rtms.backend.stable.enums.AreaType;
import com.rtms.backend.stable.repository.AreaRepository;
import com.rtms.backend.stable.repository.StableStallRepository;
import com.rtms.backend.training.entity.TrainingWorkout;
import com.rtms.backend.training.enums.DistanceBand;
import com.rtms.backend.training.enums.SessionStatus;
import com.rtms.backend.training.enums.TrainingDecision;
import com.rtms.backend.training.enums.WorkoutStatus;
import com.rtms.backend.training.repository.TrainingWorkoutRepository;
import com.rtms.backend.training.repository.projection.PastWorkoutRow;
import com.rtms.backend.identity.entity.VeterinarianProfile;
import com.rtms.backend.identity.repository.TrainerProfileRepository;
import com.rtms.backend.identity.repository.VeterinarianProfileRepository;
import com.rtms.backend.stable.entity.GroomDailyTask;
import com.rtms.backend.stable.enums.GroomTaskType;
import com.rtms.backend.training.entity.CourseRun;
import com.rtms.backend.training.enums.CourseRunStatus;
import com.rtms.backend.training.repository.CourseRunRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.*;

/**
 * Shared utility class for AI tool handlers (entity lookup, parameter normalization, and label formatting).
 */
@Component
public class AiToolSupport {

    public static final double HIGH_INTENSITY_SHARE = 0.9;
    public static final int WORKOUT_HISTORY_LIMIT = 20;
    public static final int NEEDS_RESULT_LOOKBACK_DAYS = 90;

    private final HorseRepository horseRepository;
    private final StableStallRepository stableStallRepository;
    private final AreaRepository areaRepository;
    private final UserRepository userRepository;
    private final TrainingWorkoutRepository trainingWorkoutRepository;
    private final CourseRunRepository courseRunRepository;
    private final VeterinarianProfileRepository veterinarianProfileRepository;

    @Autowired
    public AiToolSupport(HorseRepository horseRepository,
                         StableStallRepository stableStallRepository,
                         AreaRepository areaRepository,
                         @Autowired(required = false) UserRepository userRepository,
                         TrainingWorkoutRepository trainingWorkoutRepository,
                         @Autowired(required = false) CourseRunRepository courseRunRepository,
                         @Autowired(required = false) VeterinarianProfileRepository veterinarianProfileRepository) {
        this.horseRepository = horseRepository;
        this.stableStallRepository = stableStallRepository;
        this.areaRepository = areaRepository;
        this.userRepository = userRepository;
        this.trainingWorkoutRepository = trainingWorkoutRepository;
        this.courseRunRepository = courseRunRepository;
        this.veterinarianProfileRepository = veterinarianProfileRepository;
    }

    public Horse resolveHorse(Map<String, Object> args) {
        if (args == null || args.isEmpty()) {
            return null;
        }
        Long horseId = parseLong(args.get("horseId"));
        if (horseId != null) {
            return horseRepository.findById(horseId).orElse(null);
        }
        Object nameObj = args.get("horseName");
        if (nameObj == null) {
            nameObj = args.get("name");
        }
        if (nameObj != null && !nameObj.toString().trim().isEmpty()) {
            String horseName = nameObj.toString().trim();
            List<Horse> all = horseRepository.findAll();
            return all.stream()
                    .filter(h -> h.getName() != null && h.getName().equalsIgnoreCase(horseName))
                    .findFirst()
                    .orElseGet(() -> all.stream()
                            .filter(h -> h.getName() != null && h.getName().toLowerCase().contains(horseName.toLowerCase()))
                            .findFirst()
                            .orElse(null));
        }
        return null;
    }

    public boolean isHorseInQuarantineOrBlocked(Horse horse) {
        if (horse == null) return false;
        if (horse.getTrainingDecision() == TrainingDecision.BLOCKED) return true;
        if (horse.getCurrentStallId() != null && stableStallRepository != null && areaRepository != null) {
            StableStall stall = stableStallRepository.findById(horse.getCurrentStallId()).orElse(null);
            if (stall != null && stall.getAreaId() != null) {
                Area area = areaRepository.findById(stall.getAreaId()).orElse(null);
                if (area != null && area.getType() == AreaType.QUARANTINE) {
                    return true;
                }
            }
        }
        return false;
    }

    public String resolveUserName(AuthenticatedUser user, String defaultName) {
        if (user == null || user.getUserId() == null) return defaultName;
        if (userRepository != null) {
            return userRepository.findById(user.getUserId()).map(User::getFullName).orElse(user.getEmail());
        }
        return user.getEmail() != null ? user.getEmail() : defaultName;
    }

    public String resolveStallName(Long stallId) {
        if (stallId == null) {
            return "No stall assigned";
        }
        return stableStallRepository.findById(stallId)
                .map(s -> "Stall " + s.getStallCode() + " (No. " + s.getStallNumber() + ")")
                .orElse("Unspecified stall");
    }

    public String formatSessionWindow(LocalTime start, LocalTime end) {
        if (start == null) {
            return "No time assigned";
        }
        String timeRange = start + " - " + (end != null ? end.toString() : "N/A");
        if (start.isBefore(LocalTime.of(12, 0))) {
            return "Morning shift (" + timeRange + ")";
        } else if (start.isBefore(LocalTime.of(17, 0))) {
            return "Afternoon shift (" + timeRange + ")";
        } else {
            return "Evening shift (" + timeRange + ")";
        }
    }

    public void verifyHorseOwnershipIfOwner(Long horseId, AuthenticatedUser currentUser) {
        if ("HORSE_OWNER".equalsIgnoreCase(currentUser.getRole())) {
            Horse horse = horseRepository.findById(horseId)
                    .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "HORSE_NOT_FOUND", "Horse not found"));
            if (!Objects.equals(horse.getOwnerId(), currentUser.getUserId())) {
                throw new ApiException(HttpStatus.FORBIDDEN, "FORBIDDEN_HORSE", "You do not own this horse.");
            }
        }
    }

    public Long parseLong(Object obj) {
        if (obj == null) return null;
        if (obj instanceof Number num) return num.longValue();
        try {
            return Long.valueOf(obj.toString().trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    public LocalDate parseDate(Object obj, LocalDate defaultDate) {
        if (obj == null || obj.toString().isBlank()) return defaultDate;
        try {
            return LocalDate.parse(obj.toString().trim());
        } catch (Exception e) {
            return defaultDate;
        }
    }

    public int parseInteger(Object obj, int defaultValue) {
        if (obj == null) return defaultValue;
        if (obj instanceof Number num) return num.intValue();
        try {
            return Integer.parseInt(obj.toString().trim());
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    public String resolveHorseActionUrl(Long horseId, AuthenticatedUser currentUser) {
        if (horseId == null) return "/manager/horses";
        String role = currentUser != null && currentUser.getRole() != null ? currentUser.getRole() : "CLUB_MANAGER";
        return switch (role) {
            case "HEAD_TRAINER" -> "/trainer/horses/" + horseId;
            case "HORSE_OWNER" -> "/owner/horses/" + horseId;
            case "VETERINARIAN" -> "/veterinarian/horses/" + horseId;
            default -> "/manager/horses/" + horseId;
        };
    }

    public String groupLabel(String groupName, boolean disbanded) {
        if (groupName == null) {
            return "Group deleted";
        }
        return disbanded ? groupName + " (Disbanded)" : groupName;
    }

    public String courseRunStatusLabel(String status) {
        if (status == null) {
            return "N/A";
        }
        return switch (status) {
            case "ACTIVE" -> "In progress (ACTIVE)";
            case "UPCOMING" -> "Upcoming (UPCOMING)";
            case "COMPLETED" -> "Completed (COMPLETED)";
            case "CANCELLED" -> "Cancelled (CANCELLED)";
            default -> status;
        };
    }

    public List<PastWorkoutRow> pastWorkouts(Long horseId) {
        return trainingWorkoutRepository.findPastWorkouts(horseId, LocalDate.now());
    }

    public List<TrainingWorkout> ranWorkouts(Long horseId) {
        return pastWorkouts(horseId).stream().map(PastWorkoutRow::workout)
                .filter(w -> w.getStatus() == WorkoutStatus.RAN).toList();
    }

    public boolean overLimit(int maxHr, int avgHr, int recoveryHr, double share) {
        return maxHr > share * TrainingLimits.MAX_HEART_RATE_LIMIT
                || avgHr > share * TrainingLimits.AVERAGE_HEART_RATE_LIMIT
                || recoveryHr > share * TrainingLimits.RECOVERY_HEART_RATE_LIMIT;
    }

    public String outcome(PastWorkoutRow row) {
        return switch (row.workout().getStatus()) {
            case RAN -> "Ran";
            case HELD -> "Needs result";
            case CANCELLED -> row.sessionStatus() == SessionStatus.HELD ? "Did not run" : "Cancelled";
            case SCHEDULED -> "Scheduled";
        };
    }

    public String formatDistanceBand(DistanceBand band) {
        if (band == null) return "Undetermined";
        return switch (band) {
            case SPRINT -> "Sprint (SPRINT: <=1400m)";
            case MILE -> "Mid-distance (MILE: 1401m-1800m)";
            case MIDDLE -> "Middle-distance (MIDDLE: 1801m-2400m)";
            case STAYER -> "Long-distance (STAYER: >2400m)";
        };
    }

    public record SpecialtyMetrics(
            int specialtyCount,
            List<String> strongBands,
            List<String> formattedDistances,
            String preferredSurface,
            BigDecimal averageRating,
            BigDecimal bestRating,
            List<String> specialtyBreakdown
    ) {}

    public SpecialtyMetrics computeSpecialtyMetrics(com.rtms.backend.training.dto.HorseSpecialtyResponse resp) {
        return SpecialtyMetricsCalculator.compute(this, resp);
    }

    public int getGroomTaskPriority(GroomDailyTask task) {
        return GroomTaskPresentation.getGroomTaskPriority(task);
    }

    public String formatGroomTaskType(GroomTaskType type) {
        return GroomTaskPresentation.formatGroomTaskType(type);
    }

    public String resolveHorseTrainerName(Horse h) {
        if (h.getCurrentStallId() != null && stableStallRepository != null && areaRepository != null) {
            StableStall stall = stableStallRepository.findById(h.getCurrentStallId()).orElse(null);
            if (stall != null && stall.getAreaId() != null) {
                Area area = areaRepository.findById(stall.getAreaId()).orElse(null);
                if (area != null && area.getTrainerId() != null && userRepository != null) {
                    return userRepository.findById(area.getTrainerId()).map(User::getFullName).orElse("Trainer #" + area.getTrainerId());
                }
            }
        }
        if (courseRunRepository != null) {
            List<CourseRun> plans = courseRunRepository.findCourseRunsOfHorse(
                    h.getId(), List.of(CourseRunStatus.ACTIVE, CourseRunStatus.UPCOMING));
            if (!plans.isEmpty() && plans.get(0).getTrainerId() != null && userRepository != null) {
                return userRepository.findById(plans.get(0).getTrainerId()).map(User::getFullName).orElse("Trainer #" + plans.get(0).getTrainerId());
            }
        }
        return "No trainer assigned";
    }

    public String resolveStaffSpecialty(User u, String roleName) {
        if ("HEAD_TRAINER".equalsIgnoreCase(roleName) || "TRAINER".equalsIgnoreCase(roleName)) {
            List<Area> areas = areaRepository != null ? areaRepository.findAll() : Collections.emptyList();
            List<String> managedAreas = areas.stream()
                    .filter(a -> Objects.equals(a.getTrainerId(), u.getId()))
                    .map(Area::getName)
                    .toList();
            if (!managedAreas.isEmpty()) {
                return "Training areas: " + String.join(", ", managedAreas);
            }
            return "Trainer responsible for training plans and strategy";
        } else if ("VETERINARIAN".equalsIgnoreCase(roleName) || "VET".equalsIgnoreCase(roleName)) {
            if (veterinarianProfileRepository != null) {
                Optional<VeterinarianProfile> vpOpt = veterinarianProfileRepository.findById(u.getId());
                if (vpOpt.isPresent() && vpOpt.get().getSpecialization() != null && !vpOpt.get().getSpecialization().isBlank()) {
                    return "Specialty: " + vpOpt.get().getSpecialization() + " (Professional license no.: " + vpOpt.get().getLicenseNumber() + ")";
                }
            }
            return "Clinical veterinarian responsible for diagnosis and treatment plan monitoring";
        } else if ("GROOM".equalsIgnoreCase(roleName) || "STABLE_STAFF".equalsIgnoreCase(roleName)) {
            if (stableStallRepository != null) {
                long stallCount = stableStallRepository.findAll().stream()
                    .filter(s -> Objects.equals(s.getGroomId(), u.getId()))
                    .count();
                if (stallCount > 0) {
                    return "Directly cares for " + stallCount + " stalls and horses";
                }
            }
            return "Stable care staff member who takes horses to training";
        } else if ("CLUB_MANAGER".equalsIgnoreCase(roleName) || "ADMIN".equalsIgnoreCase(roleName)) {
            return "Manager overseeing facilities, staff, and admission applications";
        }
        return "Club staff member";
    }

    public HorseRepository getHorseRepository() {
        return horseRepository;
    }

    public StableStallRepository getStableStallRepository() {
        return stableStallRepository;
    }

    public AreaRepository getAreaRepository() {
        return areaRepository;
    }

    public UserRepository getUserRepository() {
        return userRepository;
    }

    public TrainingWorkoutRepository getTrainingWorkoutRepository() {
        return trainingWorkoutRepository;
    }
}
