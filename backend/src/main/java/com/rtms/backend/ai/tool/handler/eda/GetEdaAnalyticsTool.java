package com.rtms.backend.ai.tool.handler.eda;

import com.rtms.backend.analytics.service.BioSpcAnalyticsService;
import com.rtms.backend.analytics.service.HorseEdaAnalyticsService;
import com.rtms.backend.analytics.service.ManagerEdaAnalyticsService;
import com.rtms.backend.analytics.service.SurvivalEdaAnalyticsService;
import com.rtms.backend.ai.tool.handler.AiToolHandler;
import com.rtms.backend.ai.tool.handler.AiToolSupport;
import com.rtms.backend.config.ApiException;
import com.rtms.backend.horse.entity.Horse;
import com.rtms.backend.security.AuthenticatedUser;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.Locale;
import java.util.Map;

@Component
public class GetEdaAnalyticsTool implements AiToolHandler {

    private final HorseEdaAnalyticsService horseEdaAnalyticsService;
    private final BioSpcAnalyticsService bioSpcAnalyticsService;
    private final SurvivalEdaAnalyticsService survivalEdaAnalyticsService;
    private final ManagerEdaAnalyticsService managerEdaAnalyticsService;
    private final AiToolSupport support;

    public GetEdaAnalyticsTool(
            HorseEdaAnalyticsService horseEdaAnalyticsService,
            BioSpcAnalyticsService bioSpcAnalyticsService,
            SurvivalEdaAnalyticsService survivalEdaAnalyticsService,
            ManagerEdaAnalyticsService managerEdaAnalyticsService,
            AiToolSupport support
    ) {
        this.horseEdaAnalyticsService = horseEdaAnalyticsService;
        this.bioSpcAnalyticsService = bioSpcAnalyticsService;
        this.survivalEdaAnalyticsService = survivalEdaAnalyticsService;
        this.managerEdaAnalyticsService = managerEdaAnalyticsService;
        this.support = support;
    }

    @Override
    public String getToolName() {
        return "getEdaAnalytics";
    }

    @Override
    public Object execute(Map<String, Object> arguments, AuthenticatedUser currentUser) {
        Map<String, Object> args = arguments != null ? arguments : Map.of();
        EdaAnalysisType analysisType = parseAnalysisType(args.get("analysisType"));

        if (currentUser == null || currentUser.getRole() == null
                || !analysisType.isAllowedForRole(currentUser.getRole())) {
            throw new ApiException(
                    HttpStatus.FORBIDDEN,
                    "TOOL_FORBIDDEN",
                    "The current role is not allowed to use this EDA analysis"
            );
        }

        return switch (analysisType) {
            case FLEET_PERFORMANCE -> horseEdaAnalyticsService.getFleetEdaAnalytics(
                    currentUser, days(args, 180, true)
            );
            case HORSE_PERFORMANCE -> horseEdaAnalyticsService.getIndividualEdaAnalytics(
                    resolveHorseId(args), currentUser, days(args, 365, true)
            );
            case BIO_SPC -> bioSpcAnalyticsService.getBioSpcSeriesForAi(
                    resolveHorseId(args), bioSpcMetric(args), currentUser
            );
            case SURVIVAL -> survivalEdaAnalyticsService.getSurvivalAnalytics(survivalStratification(args));
            case MANAGER_OVERVIEW -> managerEdaAnalyticsService.getOverview(days(args, 90, false));
            case ADMISSION_FUNNEL -> managerEdaAnalyticsService.getAdmissionFunnel(days(args, 90, false));
            case TRAINER_PERFORMANCE -> managerEdaAnalyticsService.getTrainerEvaluation(days(args, 90, false));
        };
    }

    private EdaAnalysisType parseAnalysisType(Object value) {
        if (!(value instanceof String analysisType)) {
            throw invalidArguments("A valid analysisType is required");
        }
        try {
            return EdaAnalysisType.parse(analysisType);
        } catch (IllegalArgumentException exception) {
            throw invalidArguments(exception.getMessage());
        }
    }

    private Long resolveHorseId(Map<String, Object> args) {
        if (!hasValue(args.get("horseId")) && !hasValue(args.get("horseName"))) {
            throw invalidArguments("A horseId or horseName is required");
        }

        Horse horse = support.resolveHorse(args);
        if (horse == null) {
            throw new ApiException(HttpStatus.NOT_FOUND, "HORSE_NOT_FOUND", "Horse not found");
        }
        return horse.getId();
    }

    private int days(Map<String, Object> args, int defaultDays, boolean performanceWindow) {
        Object value = args.get("days");
        if (value == null) {
            return defaultDays;
        }

        Integer parsedDays = parseInteger(value);
        if (parsedDays == null) {
            throw invalidArguments("days must be an integer");
        }
        if (performanceWindow && (parsedDays < 1 || parsedDays > 365)) {
            throw invalidArguments("Performance days must be between 1 and 365");
        }
        return parsedDays;
    }

    private Integer parseInteger(Object value) {
        if (!(value instanceof Number number)) {
            return null;
        }
        try {
            return new BigDecimal(number.toString()).intValueExact();
        } catch (NumberFormatException | ArithmeticException exception) {
            return null;
        }
    }

    private String bioSpcMetric(Map<String, Object> args) {
        Object value = args.get("metric");
        if (value == null) {
            return "TEMPERATURE";
        }
        if (!(value instanceof String metric)) {
            throw invalidArguments("metric must be TEMPERATURE or RESTING_HEART_RATE");
        }

        String normalizedMetric = metric.trim().toUpperCase(Locale.ROOT);
        if (!normalizedMetric.equals("TEMPERATURE") && !normalizedMetric.equals("RESTING_HEART_RATE")) {
            throw invalidArguments("metric must be TEMPERATURE or RESTING_HEART_RATE");
        }
        return normalizedMetric;
    }

    private String survivalStratification(Map<String, Object> args) {
        Object value = args.get("stratifyBy");
        if (value == null) {
            return "SEVERITY";
        }
        if (!(value instanceof String stratifyBy)) {
            throw invalidArguments("stratifyBy must be SEVERITY or BODY_ZONE");
        }

        String normalizedStratification = stratifyBy.trim().toUpperCase(Locale.ROOT);
        if (!normalizedStratification.equals("SEVERITY") && !normalizedStratification.equals("BODY_ZONE")) {
            throw invalidArguments("stratifyBy must be SEVERITY or BODY_ZONE");
        }
        return normalizedStratification;
    }

    private boolean hasValue(Object value) {
        return value != null && (!(value instanceof String text) || !text.isBlank());
    }

    private ApiException invalidArguments(String message) {
        return new ApiException(HttpStatus.BAD_REQUEST, "INVALID_ARGS", message);
    }
}
