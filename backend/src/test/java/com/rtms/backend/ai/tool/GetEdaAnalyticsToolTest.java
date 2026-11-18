package com.rtms.backend.ai.tool;

import com.rtms.backend.analytics.dto.BioSpcSeriesResponse;
import com.rtms.backend.analytics.dto.FleetEdaResponse;
import com.rtms.backend.analytics.dto.IndividualHorseEdaResponse;
import com.rtms.backend.analytics.dto.SurvivalEdaResponse;
import com.rtms.backend.analytics.dto.manager.AdmissionFunnelEdaDto;
import com.rtms.backend.analytics.dto.manager.ManagerEdaOverviewResponse;
import com.rtms.backend.analytics.dto.manager.TrainerEvaluationEdaDto;
import com.rtms.backend.analytics.service.BioSpcAnalyticsService;
import com.rtms.backend.analytics.service.HorseEdaAnalyticsService;
import com.rtms.backend.analytics.service.ManagerEdaAnalyticsService;
import com.rtms.backend.analytics.service.SurvivalEdaAnalyticsService;
import com.rtms.backend.ai.tool.handler.AiToolSupport;
import com.rtms.backend.ai.tool.handler.eda.GetEdaAnalyticsTool;
import com.rtms.backend.config.ApiException;
import com.rtms.backend.horse.entity.Horse;
import com.rtms.backend.horse.repository.HorseRepository;
import com.rtms.backend.security.AuthenticatedUser;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class GetEdaAnalyticsToolTest {

    private static final Long HORSE_ID = 42L;
    private static final AuthenticatedUser MANAGER = user("CLUB_MANAGER");

    private HorseEdaAnalyticsService horseEdaAnalyticsService;
    private BioSpcAnalyticsService bioSpcAnalyticsService;
    private SurvivalEdaAnalyticsService survivalEdaAnalyticsService;
    private ManagerEdaAnalyticsService managerEdaAnalyticsService;
    private HorseRepository horseRepository;
    private GetEdaAnalyticsTool tool;
    private Horse horse;

    @BeforeEach
    void setUp() {
        horseEdaAnalyticsService = mock(HorseEdaAnalyticsService.class);
        bioSpcAnalyticsService = mock(BioSpcAnalyticsService.class);
        survivalEdaAnalyticsService = mock(SurvivalEdaAnalyticsService.class);
        managerEdaAnalyticsService = mock(ManagerEdaAnalyticsService.class);
        horseRepository = mock(HorseRepository.class);
        horse = mock(Horse.class);
        when(horse.getId()).thenReturn(HORSE_ID);
        when(horseRepository.findById(HORSE_ID)).thenReturn(Optional.of(horse));

        AiToolSupport support = new AiToolSupport(
                horseRepository, null, null, null, null, null, null
        );
        tool = new GetEdaAnalyticsTool(
                horseEdaAnalyticsService,
                bioSpcAnalyticsService,
                survivalEdaAnalyticsService,
                managerEdaAnalyticsService,
                support
        );
    }

    @Test
    void exposesCanonicalToolName() {
        assertEquals("getEdaAnalytics", tool.getToolName());
    }

    @Test
    void routesFleetPerformanceAndDefaultsTo180Days() {
        AuthenticatedUser actor = user("HEAD_TRAINER");
        FleetEdaResponse expected = new FleetEdaResponse(null, null, null, List.of());
        when(horseEdaAnalyticsService.getFleetEdaAnalytics(actor, 180)).thenReturn(expected);

        Object result = tool.execute(Map.of("analysisType", "FLEET_PERFORMANCE"), actor);

        assertSame(expected, result);
        verify(horseEdaAnalyticsService).getFleetEdaAnalytics(actor, 180);
    }

    @Test
    void routesHorsePerformanceWithHorseIdPreferredAndDefaultsTo365Days() {
        AuthenticatedUser actor = user("HORSE_OWNER");
        IndividualHorseEdaResponse expected = new IndividualHorseEdaResponse(
                HORSE_ID, "Silver Comet", "Arabian", null, null, null, List.of()
        );
        when(horseEdaAnalyticsService.getIndividualEdaAnalytics(HORSE_ID, actor, 365))
                .thenReturn(expected);

        Object result = tool.execute(Map.of(
                "analysisType", "HORSE_PERFORMANCE",
                "horseId", HORSE_ID,
                "horseName", "Another Horse"
        ), actor);

        assertSame(expected, result);
        verify(horseRepository).findById(HORSE_ID);
        verify(horseRepository, never()).findAll();
        verify(horseEdaAnalyticsService).getIndividualEdaAnalytics(HORSE_ID, actor, 365);
    }

    @Test
    void acceptsNumericStringHorseIds() {
        IndividualHorseEdaResponse expected = new IndividualHorseEdaResponse(
                HORSE_ID, "Silver Comet", "Arabian", null, null, null, List.of()
        );
        when(horseEdaAnalyticsService.getIndividualEdaAnalytics(HORSE_ID, MANAGER, 365))
                .thenReturn(expected);

        Object result = tool.execute(Map.of(
                "analysisType", "HORSE_PERFORMANCE",
                "horseId", HORSE_ID.toString()
        ), MANAGER);

        assertSame(expected, result);
        verify(horseRepository).findById(HORSE_ID);
    }

    @Test
    void rejectsFractionalHorseIdsBeforeLookup() {
        ApiException exception = assertThrows(ApiException.class, () ->
                tool.execute(Map.of(
                        "analysisType", "HORSE_PERFORMANCE",
                        "horseId", new BigDecimal("42.5")
                ), MANAGER)
        );

        assertApiError(exception, HttpStatus.BAD_REQUEST, "INVALID_ARGS");
        verifyNoInteractions(horseRepository);
    }

    @Test
    void rejectsHorseIdsOutsideLongRangeBeforeLookup() {
        ApiException exception = assertThrows(ApiException.class, () ->
                tool.execute(Map.of(
                        "analysisType", "HORSE_PERFORMANCE",
                        "horseId", BigInteger.valueOf(Long.MAX_VALUE).add(BigInteger.ONE)
                ), MANAGER)
        );

        assertApiError(exception, HttpStatus.BAD_REQUEST, "INVALID_ARGS");
        verifyNoInteractions(horseRepository);
    }

    @Test
    void routesBioSpcWithTemperatureDefault() {
        AuthenticatedUser actor = user("GROOM");
        BioSpcSeriesResponse expected = new BioSpcSeriesResponse(
                HORSE_ID, "Silver Comet", "TEMPERATURE", null, null, 0, false,
                null, null, null, null, null, null, List.of()
        );
        when(bioSpcAnalyticsService.getBioSpcSeriesForAi(HORSE_ID, "TEMPERATURE", actor))
                .thenReturn(expected);

        Object result = tool.execute(Map.of(
                "analysisType", "BIO_SPC",
                "horseId", HORSE_ID
        ), actor);

        assertSame(expected, result);
        verify(bioSpcAnalyticsService).getBioSpcSeriesForAi(HORSE_ID, "TEMPERATURE", actor);
    }

    @Test
    void routesSurvivalAndDefaultsToSeverity() {
        AuthenticatedUser actor = user("VETERINARIAN");
        SurvivalEdaResponse expected = new SurvivalEdaResponse("SEVERITY", List.of());
        when(survivalEdaAnalyticsService.getSurvivalAnalytics("SEVERITY")).thenReturn(expected);

        Object result = tool.execute(Map.of("analysisType", "SURVIVAL"), actor);

        assertSame(expected, result);
        verify(survivalEdaAnalyticsService).getSurvivalAnalytics("SEVERITY");
    }

    @Test
    void routesManagerOverviewAndDefaultsTo90Days() {
        ManagerEdaOverviewResponse expected = new ManagerEdaOverviewResponse(
                90, null, null, null, null
        );
        when(managerEdaAnalyticsService.getOverview(90)).thenReturn(expected);

        Object result = tool.execute(Map.of("analysisType", "MANAGER_OVERVIEW"), MANAGER);

        assertSame(expected, result);
        verify(managerEdaAnalyticsService).getOverview(90);
    }

    @Test
    void routesAdmissionFunnelToFocusedManagerService() {
        AdmissionFunnelEdaDto expected = new AdmissionFunnelEdaDto(
                0, 0, 0, 0, 0, 0.0, 0.0, "NONE", null, List.of()
        );
        when(managerEdaAnalyticsService.getAdmissionFunnel(30)).thenReturn(expected);

        Object result = tool.execute(Map.of(
                "analysisType", "ADMISSION_FUNNEL",
                "days", 30
        ), MANAGER);

        assertSame(expected, result);
        verify(managerEdaAnalyticsService).getAdmissionFunnel(30);
    }

    @Test
    void routesTrainerPerformanceToFocusedManagerService() {
        TrainerEvaluationEdaDto expected = new TrainerEvaluationEdaDto(
                0.0, 0.0, 0, 0, List.of()
        );
        when(managerEdaAnalyticsService.getTrainerEvaluation(90)).thenReturn(expected);

        Object result = tool.execute(Map.of("analysisType", "TRAINER_PERFORMANCE"), MANAGER);

        assertSame(expected, result);
        verify(managerEdaAnalyticsService).getTrainerEvaluation(90);
    }

    @Test
    void rejectsMissingAndUnknownAnalysisTypesAsInvalidArguments() {
        ApiException missingMode = assertThrows(ApiException.class, () ->
                tool.execute(Map.of(), MANAGER)
        );
        assertApiError(missingMode, HttpStatus.BAD_REQUEST, "INVALID_ARGS");

        ApiException unknownMode = assertThrows(ApiException.class, () ->
                tool.execute(Map.of("analysisType", "NOT_A_MODE"), MANAGER)
        );
        assertApiError(unknownMode, HttpStatus.BAD_REQUEST, "INVALID_ARGS");
    }

    @Test
    void rejectsModesNotAllowedForTheCurrentRole() {
        ApiException exception = assertThrows(ApiException.class, () ->
                tool.execute(Map.of("analysisType", "FLEET_PERFORMANCE"), user("GROOM"))
        );

        assertApiError(exception, HttpStatus.FORBIDDEN, "TOOL_FORBIDDEN");
    }

    @Test
    void requiresHorseIdentifierForHorseAndBioSpcModes() {
        for (String mode : List.of("HORSE_PERFORMANCE", "BIO_SPC")) {
            ApiException exception = assertThrows(ApiException.class, () ->
                    tool.execute(Map.of("analysisType", mode), MANAGER)
            );
            assertApiError(exception, HttpStatus.BAD_REQUEST, "INVALID_ARGS");
        }
    }

    @Test
    void reportsUnknownHorseForHorseAndBioSpcModes() {
        when(horseRepository.findById(HORSE_ID)).thenReturn(Optional.empty());

        for (String mode : List.of("HORSE_PERFORMANCE", "BIO_SPC")) {
            ApiException exception = assertThrows(ApiException.class, () ->
                    tool.execute(Map.of("analysisType", mode, "horseId", HORSE_ID), MANAGER)
            );
            assertApiError(exception, HttpStatus.NOT_FOUND, "HORSE_NOT_FOUND");
        }
    }

    @Test
    void rejectsUnsupportedBioSpcMetric() {
        ApiException exception = assertThrows(ApiException.class, () ->
                tool.execute(Map.of(
                        "analysisType", "BIO_SPC",
                        "horseId", HORSE_ID,
                        "metric", "PULSE"
                ), MANAGER)
        );

        assertApiError(exception, HttpStatus.BAD_REQUEST, "INVALID_ARGS");
    }

    @Test
    void rejectsUnsupportedSurvivalStratification() {
        ApiException exception = assertThrows(ApiException.class, () ->
                tool.execute(Map.of(
                        "analysisType", "SURVIVAL",
                        "stratifyBy", "AGE"
                ), user("VETERINARIAN"))
        );

        assertApiError(exception, HttpStatus.BAD_REQUEST, "INVALID_ARGS");
    }

    @Test
    void rejectsNonIntegerAndOutOfRangePerformanceDays() {
        List<Map<String, Object>> invalidRequests = List.of(
                Map.of("analysisType", "FLEET_PERFORMANCE", "days", 0),
                Map.of("analysisType", "FLEET_PERFORMANCE", "days", 366),
                Map.of("analysisType", "HORSE_PERFORMANCE", "horseId", HORSE_ID, "days", 1.5),
                Map.of("analysisType", "HORSE_PERFORMANCE", "horseId", HORSE_ID, "days", "180")
        );

        for (Map<String, Object> arguments : invalidRequests) {
            ApiException exception = assertThrows(ApiException.class, () ->
                    tool.execute(arguments, MANAGER)
            );
            assertApiError(exception, HttpStatus.BAD_REQUEST, "INVALID_ARGS");
        }
    }

    @Test
    void passesManagerDaysToTheServiceWithoutApplyingItsClampInTheHandler() {
        AdmissionFunnelEdaDto expected = new AdmissionFunnelEdaDto(
                0, 0, 0, 0, 0, 0.0, 0.0, "NONE", null, List.of()
        );
        when(managerEdaAnalyticsService.getAdmissionFunnel(1)).thenReturn(expected);

        Object result = tool.execute(Map.of(
                "analysisType", "ADMISSION_FUNNEL",
                "days", 1
        ), MANAGER);

        assertSame(expected, result);
        verify(managerEdaAnalyticsService).getAdmissionFunnel(1);
    }

    private static AuthenticatedUser user(String role) {
        return new AuthenticatedUser(7L, "user@example.com", role);
    }

    private static void assertApiError(ApiException exception, HttpStatus status, String code) {
        assertEquals(status, exception.getStatus());
        assertEquals(code, exception.getErrorCode());
    }
}
