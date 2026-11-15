package com.rtms.backend.admission.repository;

import com.rtms.backend.admission.enums.AdmissionStatus;
import com.rtms.backend.admission.entity.AdmissionApplication;
import com.rtms.backend.admission.entity.CandidateHorseProfile;
import com.rtms.backend.identity.entity.User;
import com.rtms.backend.identity.entity.Role;
import jakarta.persistence.EntityManager;
import org.springframework.transaction.annotation.Transactional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class GroomQueueRepositoryTest {
    @Autowired
    private AdmissionApplicationRepository admissions;
    @Autowired private EntityManager em;

    @Test
    void filteredQueueQueryExecutesAgainstPostgres() {
        var result = admissions.findGroomQueue(-1L,
                true, AdmissionStatus.GROOM_REVIEW,
                true, "__groom_queue_query_smoke__", true, LocalDateTime.of(1900, 1, 1, 0, 0),
                true, LocalDateTime.of(1901, 1, 1, 0, 0),
                PageRequest.of(0, 10, Sort.by(Sort.Order.desc("createdAt"), Sort.Order.desc("id"))));

        assertEquals(10, result.getSize());
    }
    @Test
    @Transactional
    void onlyAssignedGroomSeesPendingAdmissionInAllThreeQueries() {
        User owner = user("Owner", "HORSE_OWNER");
        User assigned = user("Assigned", "GROOM");
        User other = user("Other", "GROOM");
        AdmissionApplication admission = new AdmissionApplication();
        admission.setOwnerId(owner.getId());
        admission.setAssignedGroomId(assigned.getId());
        admission.setStatus(AdmissionStatus.GROOM_REVIEW);
        admissions.saveAndFlush(admission);
        CandidateHorseProfile candidate = new CandidateHorseProfile();
        candidate.setAdmissionId(admission.getId());
        candidate.setName("Assigned visibility " + UUID.randomUUID());
        em.persist(candidate);
        em.flush();
        assertTrue(admissions.isVisibleToGroom(admission.getId(), assigned.getId()));
        assertFalse(admissions.isVisibleToGroom(admission.getId(), other.getId()));
        assertTrue(admissions.findVisibleToGroom(assigned.getId(), true, AdmissionStatus.GROOM_REVIEW)
                .stream().anyMatch(a -> a.getId().equals(admission.getId())));
        assertTrue(admissions.findVisibleToGroom(other.getId(), true, AdmissionStatus.GROOM_REVIEW).isEmpty());
        for (User groom : new User[]{assigned, other}) {
            var result = admissions.findGroomQueue(groom.getId(), true, AdmissionStatus.GROOM_REVIEW,
                    true, candidate.getName(), false, LocalDateTime.of(1900, 1, 1, 0, 0),
                    false, LocalDateTime.of(9999, 1, 1, 0, 0), PageRequest.of(0, 10));
            assertEquals(groom == assigned ? 1 : 0, result.getTotalElements());
        }
    }

    private User user(String name, String role) {
        User user = new User();
        user.setFullName(name);
        user.setEmail(UUID.randomUUID() + "@groom-visibility.test.invalid");
        user.setPasswordHash("unused-test-password-hash");
        user.setRole(em.createQuery("SELECT r FROM Role r WHERE r.name = :name", Role.class)
                .setParameter("name", role).getSingleResult());
        em.persist(user);
        em.flush();
        return user;
    }
}
