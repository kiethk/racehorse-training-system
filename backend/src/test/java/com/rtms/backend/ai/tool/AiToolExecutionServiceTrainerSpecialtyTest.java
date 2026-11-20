package com.rtms.backend.ai.tool;

import com.rtms.backend.horse.entity.Horse;
import com.rtms.backend.horse.enums.HorseStatus;
import com.rtms.backend.security.AuthenticatedUser;

import com.rtms.backend.training.dto.*;
//
import com.rtms.backend.training.enums.DistanceBand;
//
import com.rtms.backend.training.enums.SurfaceType;
//
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;

class AiToolExecutionServiceTrainerSpecialtyTest extends AiToolExecutionServiceTestSupport {

    // =================================================================
    // HEAD_TRAINER Tests
    // =================================================================

    @Test
    @DisplayName("HEAD_TRAINER calls getHorseSpecialtyAnalysis to analyze horse specialties with surface and average rating")
    void testTrainer_GetHorseSpecialtyAnalysis() {
        AuthenticatedUser trainer = new AuthenticatedUser(5L, "trainer@rtms.com", "HEAD_TRAINER");

        Horse horse = new Horse();
        horse.setId(10L);
        horse.setName("RED RUM");
        when(horseRepository.findById(10L)).thenReturn(Optional.of(horse));

        HorseSpecialtyResponse specialty = new HorseSpecialtyResponse(
                3, new BigDecimal("7.0"),
                List.of(DistanceBand.SPRINT),
                SurfaceType.TURF,
                List.of(new DistanceStat(DistanceBand.SPRINT, 5, new BigDecimal("8.5"), new BigDecimal("62.0")))
        );
        when(horseSpecialtyService.getSpecialties(List.of(10L)))
                .thenReturn(Map.of(10L, specialty));

        Object result = toolExecutionService.executeTool(
                "getHorseSpecialtyAnalysis",
                Map.of("horseId", 10L),
                trainer
        );

        assertNotNull(result);
        assertTrue(result instanceof Map<?, ?>);
        Map<?, ?> map = (Map<?, ?>) result;
        assertEquals("RED RUM", map.get("horseName"));
        assertEquals(1, map.get("specialtyCount"));
        assertEquals(new BigDecimal("8.5"), map.get("averageRating"));
        assertEquals(new BigDecimal("8.5"), map.get("bestRating"));
        assertEquals(10L, map.get("horseId"));
        assertTrue(map.get("preferredSurface").toString().contains("TURF"));
    }

    @Test
    @DisplayName("HEAD_TRAINER calls getHorseSpecialtyAnalysis with horseName='all' to rank horses by specialty count (sortBy='count')")
    void testTrainer_GetHorseSpecialtyAnalysis_FleetRanking_SortByCount() {
        AuthenticatedUser trainer = new AuthenticatedUser(5L, "trainer@rtms.com", "HEAD_TRAINER");

        Horse h1 = new Horse();
        h1.setId(10L);
        h1.setName("ALPHA SPRINT");
        h1.setRegistrationNumber("VN-010");
        h1.setCurrentStatus(HorseStatus.ELIGIBLE);

        Horse h2 = new Horse();
        h2.setId(20L);
        h2.setName("BETA DUAL");
        h2.setRegistrationNumber("VN-020");
        h2.setCurrentStatus(HorseStatus.ELIGIBLE);

        when(horseRepository.findManagedHorsesByTrainerIdAndStatusIn(eq(5L), anyCollection()))
                .thenReturn(List.of(h1, h2));

        // h1 has 1 specialty (SPRINT, rating 9.5), h2 has 2 specialties (MILE 7.1, MIDDLE 7.3 => avg 7.2)
        HorseSpecialtyResponse spec1 = new HorseSpecialtyResponse(
                3, new BigDecimal("7.0"), List.of(DistanceBand.SPRINT), SurfaceType.TURF,
                List.of(new DistanceStat(DistanceBand.SPRINT, 5, new BigDecimal("9.5"), new BigDecimal("64.0"))));
        HorseSpecialtyResponse spec2 = new HorseSpecialtyResponse(
                3, new BigDecimal("7.0"), List.of(DistanceBand.MILE, DistanceBand.MIDDLE), SurfaceType.DIRT,
                List.of(
                        new DistanceStat(DistanceBand.MILE, 3, new BigDecimal("7.1"), new BigDecimal("58.0")),
                        new DistanceStat(DistanceBand.MIDDLE, 3, new BigDecimal("7.3"), new BigDecimal("56.0"))
                ));

        when(horseSpecialtyService.getSpecialties(anyCollection()))
                .thenReturn(Map.of(10L, spec1, 20L, spec2));

        Object result = toolExecutionService.executeTool(
                "getHorseSpecialtyAnalysis",
                Map.of("horseName", "all", "sortBy", "count"),
                trainer
        );

        assertNotNull(result);
        assertTrue(result instanceof Map<?, ?>);
        Map<?, ?> map = (Map<?, ?>) result;
        assertEquals(2, map.get("totalHorses"));
        assertEquals(2, map.get("horsesWithSpecialties"));
        assertEquals("COUNT", map.get("sortBy"));
        assertTrue(map.containsKey("rankedHorseList"));

        List<?> rankedList = (List<?>) map.get("rankedHorseList");
        assertEquals(2, rankedList.size());

        // Top horse should be BETA DUAL (2 specialties)
        Map<?, ?> top = (Map<?, ?>) rankedList.get(0);
        assertEquals(1, top.get("rank"));
        assertEquals("BETA DUAL", top.get("horseName"));
        assertEquals(2, top.get("specialtyCount"));
        assertEquals(new BigDecimal("7.2"), top.get("averageRating"));

        // Second horse should be ALPHA SPRINT (1 specialty)
        Map<?, ?> second = (Map<?, ?>) rankedList.get(1);
        assertEquals(2, second.get("rank"));
        assertEquals("ALPHA SPRINT", second.get("horseName"));
        assertEquals(1, second.get("specialtyCount"));
        assertEquals(new BigDecimal("9.5"), second.get("averageRating"));
    }

    @Test
    @DisplayName("HEAD_TRAINER calls getHorseSpecialtyAnalysis with sortBy='rating' to rank by average specialty performance")
    void testTrainer_GetHorseSpecialtyAnalysis_FleetRanking_SortByRating() {
        AuthenticatedUser trainer = new AuthenticatedUser(5L, "trainer@rtms.com", "HEAD_TRAINER");

        Horse h1 = new Horse();
        h1.setId(10L);
        h1.setName("ALPHA SPRINT");
        h1.setRegistrationNumber("VN-010");
        h1.setCurrentStatus(HorseStatus.ELIGIBLE);

        Horse h2 = new Horse();
        h2.setId(20L);
        h2.setName("BETA DUAL");
        h2.setRegistrationNumber("VN-020");
        h2.setCurrentStatus(HorseStatus.ELIGIBLE);

        when(horseRepository.findManagedHorsesByTrainerIdAndStatusIn(eq(5L), anyCollection()))
                .thenReturn(List.of(h1, h2));

        // h1 has 1 specialty (SPRINT, rating 9.5), h2 has 2 specialties (MILE 7.1, MIDDLE 7.3 => avg 7.2)
        HorseSpecialtyResponse spec1 = new HorseSpecialtyResponse(
                3, new BigDecimal("7.0"), List.of(DistanceBand.SPRINT), SurfaceType.TURF,
                List.of(new DistanceStat(DistanceBand.SPRINT, 5, new BigDecimal("9.5"), new BigDecimal("64.0"))));
        HorseSpecialtyResponse spec2 = new HorseSpecialtyResponse(
                3, new BigDecimal("7.0"), List.of(DistanceBand.MILE, DistanceBand.MIDDLE), SurfaceType.DIRT,
                List.of(
                        new DistanceStat(DistanceBand.MILE, 3, new BigDecimal("7.1"), new BigDecimal("58.0")),
                        new DistanceStat(DistanceBand.MIDDLE, 3, new BigDecimal("7.3"), new BigDecimal("56.0"))
                ));

        when(horseSpecialtyService.getSpecialties(anyCollection()))
                .thenReturn(Map.of(10L, spec1, 20L, spec2));

        Object result = toolExecutionService.executeTool(
                "getHorseSpecialtyAnalysis",
                Map.of("horseName", "all", "sortBy", "rating"),
                trainer
        );

        assertNotNull(result);
        assertTrue(result instanceof Map<?, ?>);
        Map<?, ?> map = (Map<?, ?>) result;
        assertEquals(2, map.get("totalHorses"));
        assertEquals("RATING", map.get("sortBy"));

        List<?> rankedList = (List<?>) map.get("rankedHorseList");
        assertEquals(2, rankedList.size());

        // In sortBy='rating', ALPHA SPRINT (9.5) MUST be Rank 1!
        Map<?, ?> top = (Map<?, ?>) rankedList.get(0);
        assertEquals(1, top.get("rank"));
        assertEquals("ALPHA SPRINT", top.get("horseName"));
        assertEquals(new BigDecimal("9.5"), top.get("averageRating"));

        // BETA DUAL (7.2) is Rank 2
        Map<?, ?> second = (Map<?, ?>) rankedList.get(1);
        assertEquals(2, second.get("rank"));
        assertEquals("BETA DUAL", second.get("horseName"));
        assertEquals(new BigDecimal("7.2"), second.get("averageRating"));
    }
}
