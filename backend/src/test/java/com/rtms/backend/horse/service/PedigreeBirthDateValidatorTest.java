package com.rtms.backend.horse.service;

import com.rtms.backend.horse.entity.Horse;
import com.rtms.backend.horse.repository.HorseRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class PedigreeBirthDateValidatorTest {
    private final HorseRepository horses = mock(HorseRepository.class);
    private final PedigreeBirthDateValidator validator = new PedigreeBirthDateValidator(horses);
    private static final LocalDate CHILD_BIRTH = LocalDate.of(2022, 4, 15);

    @ParameterizedTest
    @CsvSource({"sire,0", "sire,1", "dam,0", "dam,1"})
    void rejectsChildBornOnOrBeforeEitherParent(String relation, int parentOffset) {
        Horse parent = parentBorn(CHILD_BIRTH.plusDays(parentOffset));
        var error = assertThrows(ResponseStatusException.class,
                () -> validator.validate(CHILD_BIRTH,
                        relation.equals("sire") ? parent : null, relation.equals("dam") ? parent : null));
        assertEquals(400, error.getStatusCode().value());
        assertTrue(error.getReason().contains(relation));
    }

    @Test
    void acceptsChildBornAfterBothParents() {
        assertDoesNotThrow(() -> validator.validate(CHILD_BIRTH,
                parentBorn(CHILD_BIRTH.minusDays(1)), parentBorn(CHILD_BIRTH.minusYears(5))));
    }

    @Test
    void missingChildBirthDateDoesNotRequireParentLookup() {
        assertDoesNotThrow(() -> validator.validateByUeln(null, "999999SIR000001", "999999DAM000001"));
        verifyNoInteractions(horses);
    }

    @Test
    void skipsParentsWithUnknownBirthDates() {
        assertDoesNotThrow(() -> validator.validate(CHILD_BIRTH, parentBorn(null), parentBorn(null)));
    }

    @Test
    void skipsExternalParentsAndMissingParentUelns() {
        assertDoesNotThrow(() -> validator.validateByUeln(CHILD_BIRTH, "999999SIR000001", null));
        verify(horses).findByRegistrationNumber("999999SIR000001");
        verifyNoMoreInteractions(horses);
    }

    @Test
    void resolvesInternalParentUsingNormalizedUeln() {
        when(horses.findByRegistrationNumber("999999DAM000001"))
                .thenReturn(List.of(parentBorn(CHILD_BIRTH)));
        assertThrows(ResponseStatusException.class,
                () -> validator.validateByUeln(CHILD_BIRTH, null, " 999999dam000001 "));
    }

    private Horse parentBorn(LocalDate date) {
        Horse parent = new Horse();
        parent.setDateOfBirth(date);
        return parent;
    }
}
