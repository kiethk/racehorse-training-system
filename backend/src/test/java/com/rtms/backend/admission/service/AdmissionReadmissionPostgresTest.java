package com.rtms.backend.admission.service;

import com.rtms.backend.admission.dto.CreateOwnerAdmissionRequest;
import com.rtms.backend.horse.enums.HorseSex;
import com.rtms.backend.admission.entity.AdmissionDocument;
import com.rtms.backend.admission.enums.AdmissionDocumentType;
import com.rtms.backend.admission.enums.AdmissionStatus;
import com.rtms.backend.admission.repository.AdmissionApplicationRepository;
import com.rtms.backend.admission.repository.AdmissionDocumentRepository;
import com.rtms.backend.config.ApiException;
import com.rtms.backend.horse.entity.Horse;
import com.rtms.backend.horse.enums.HorseStatus;
import com.rtms.backend.horse.repository.HorseRepository;
import com.rtms.backend.stable.enums.StallStatus;
import com.rtms.backend.stable.repository.StableStallRepository;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.web.context.WebApplicationContext;
import com.rtms.backend.security.AuthenticatedUser;

import java.time.LocalDateTime;
import java.time.LocalDate;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import org.springframework.mock.web.MockMultipartFile;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.springframework.test.web.servlet.setup.MockMvcBuilders.webAppContextSetup;

/** Run only against a disposable local database; concurrency cases intentionally commit. */
@SpringBootTest(properties = "spring.datasource.url=${RTMS_UELN_TEST_DB_URL}")
@ActiveProfiles("local")
@EnabledIfEnvironmentVariable(named = "RTMS_UELN_TEST_DB_URL",
        matches = "jdbc:postgresql://localhost:5433/rtms_ueln_test_[a-zA-Z0-9_]+")
class AdmissionReadmissionPostgresTest {
    @Autowired private OwnerAdmissionService owners;
    @Autowired private AdmissionGroomReviewService groom;
    @Autowired private AdmissionApplicationRepository admissions;
    @Autowired private AdmissionDocumentRepository documents;
    @Autowired private HorseRepository horses;
    @Autowired private StableStallRepository stalls;
    @Autowired private JdbcTemplate jdbc;
    @Autowired private PlatformTransactionManager transactions;
    @Autowired private WebApplicationContext webContext;
    private List<Long> ownerIds;

    @BeforeAll
    static void requireDisposableDatabase() {
        assertTrue(System.getenv("RTMS_UELN_TEST_DB_URL").contains("/rtms_ueln_test_"));
    }

    @BeforeEach
    void ownerAccounts() {
        // Fresh migrations seed one owner; add a second only in this disposable database.
        jdbc.update("""
                INSERT INTO users (full_name, email, password_hash, role_id, is_active)
                SELECT 'Readmission test owner', 'readmission-owner@test.invalid', u.password_hash, u.role_id, true
                FROM users u JOIN roles r ON r.id = u.role_id
                WHERE r.name = 'HORSE_OWNER' ORDER BY u.id LIMIT 1
                ON CONFLICT (email) DO NOTHING
                """);
        ownerIds = jdbc.queryForList("""
                SELECT u.id FROM users u JOIN roles r ON r.id = u.role_id
                WHERE r.name = 'HORSE_OWNER' ORDER BY u.id LIMIT 2
                """, Long.class);
        assertEquals(2, ownerIds.size(), "The disposable DB must contain two seeded owners");
    }

    @ParameterizedTest
    @EnumSource(value = AdmissionStatus.class, names = {"APPROVED", "REJECTED"},
            mode = EnumSource.Mode.EXCLUDE)
    void openAdmissionBlocksSameUelnFromAnotherOwner(AdmissionStatus status) {
        String ueln = "999999OPN%06d".formatted(status.ordinal());
        var first = owners.create(ownerIds.getFirst(), request(ueln, "First application"));
        new TransactionTemplate(transactions).executeWithoutResult(tx -> {
            var admission = admissions.findById(first.admissionId()).orElseThrow();
            if (status == AdmissionStatus.WAITING_FOR_ARRIVAL) {
                admission.setQuarantineStallId(stalls.findAll().getFirst().getId());
                admission.setArrivalDeadlineAt(LocalDateTime.now().plusDays(1));
            }
            if (Set.of(AdmissionStatus.VET_REVIEW, AdmissionStatus.TRAINER_REVIEW,
                    AdmissionStatus.MANAGER_REVIEW).contains(status)) {
                Horse horse = new Horse();
                horse.setName("Horse under review");
                horse.setRegistrationNumber(ueln);
                horse.setOwnerId(ownerIds.getFirst());
                horse.setCurrentStatus(HorseStatus.CANDIDATE);
                admission.setHorseId(horses.saveAndFlush(horse).getId());
                admission.setQuarantineStallId(stalls.findAll().getFirst().getId());
            }
            admission.setStatus(status);
            admissions.saveAndFlush(admission);
        });

        ApiException error = assertThrows(ApiException.class,
                () -> owners.create(ownerIds.getLast(), request(" " + ueln.toLowerCase() + " ", "Duplicate")));
        assertEquals(409, error.getStatus().value());
        assertEquals(AdmissionUelnPolicy.CONFLICT_CODE, error.getErrorCode());
    }

    @Test
    void simultaneousOwnersCreateExactlyOneAdmission() throws Exception {
        String ueln = "999999RAC000001";
        CyclicBarrier start = new CyclicBarrier(2);
        try (var executor = Executors.newFixedThreadPool(2)) {
            var tasks = ownerIds.stream().map(ownerId -> executor.submit(() -> {
                start.await(10, TimeUnit.SECONDS);
                try {
                    owners.create(ownerId, request(ueln, "Concurrent submission"));
                    return 201;
                } catch (ApiException error) {
                    return error.getStatus().value();
                }
            })).toList();
            var results = List.of(tasks.getFirst().get(20, TimeUnit.SECONDS),
                    tasks.getLast().get(20, TimeUnit.SECONDS));
            assertEquals(1, results.stream().filter(code -> code == 201).count());
            assertEquals(1, results.stream().filter(code -> code == 409).count());
        }
        assertEquals(1, jdbc.queryForObject(
                "SELECT COUNT(*) FROM candidate_horse_profiles WHERE registration_number = ?", Integer.class, ueln));
    }

    @Test
    void rejectedHorseChangesOwnerAtArrivalAndPreservesOldAdmissionAndDocuments() {
        String ueln = "999999OWN000001";
        Long previousOwner = ownerIds.getFirst();
        Long newOwner = ownerIds.getLast();
        var oldAdmission = owners.create(previousOwner, request(ueln, "Historical horse name"));
        Long horseId = new TransactionTemplate(transactions).execute(tx -> {
            Horse horse = new Horse();
            horse.setName("Historical horse name");
            horse.setRegistrationNumber(ueln);
            horse.setOwnerId(previousOwner);
            horse.setCurrentStatus(HorseStatus.REJECTED);
            horse = horses.saveAndFlush(horse);
            AdmissionDocument document = new AdmissionDocument();
            document.setAdmissionId(oldAdmission.admissionId());
            document.setDocumentType(AdmissionDocumentType.REGISTRATION_DOCUMENT);
            document.setFileUrl("local:history-regression.pdf");
            document.setOriginalFileName("history-regression.pdf");
            documents.saveAndFlush(document);
            var historical = admissions.findById(oldAdmission.admissionId()).orElseThrow();
            historical.setHorseId(horse.getId());
            historical.setStatus(AdmissionStatus.REJECTED);
            historical.setManagerFeedback("Original rejection feedback");
            admissions.saveAndFlush(historical);
            return horse.getId();
        });

        var newAdmission = owners.create(newOwner, request(ueln, "New application name"));
        assertEquals(previousOwner, horses.findById(horseId).orElseThrow().getOwnerId());
        Long groomId = jdbc.queryForObject("""
                SELECT u.id FROM users u JOIN roles r ON r.id = u.role_id
                WHERE r.name = 'GROOM' AND u.is_active = true ORDER BY u.id LIMIT 1
                """, Long.class);
        new TransactionTemplate(transactions).executeWithoutResult(tx -> {
            var admission = admissions.findById(newAdmission.admissionId()).orElseThrow();
            var stall = stalls.findFirstAvailableQuarantineStallForUpdate().orElseThrow();
            stall.setStatus(StallStatus.RESERVED);
            stalls.saveAndFlush(stall);
            admission.setGroomId(groomId);
            admission.setQuarantineStallId(stall.getId());
            admission.setArrivalDeadlineAt(LocalDateTime.now().plusDays(1));
            admission.setStatus(AdmissionStatus.WAITING_FOR_ARRIVAL);
            admissions.saveAndFlush(admission);
        });

        var arrived = groom.confirmArrival(newAdmission.admissionId(), groomId);
        assertEquals(horseId, arrived.getHorseId());
        Horse currentHorse = horses.findById(horseId).orElseThrow();
        assertEquals(newOwner, currentHorse.getOwnerId());
        assertEquals(ueln, currentHorse.getRegistrationNumber());
        assertEquals(HorseStatus.CANDIDATE, currentHorse.getCurrentStatus());
        assertEquals(1, horses.findByRegistrationNumber(ueln).size());

        var history = owners.getMyAdmission(previousOwner, oldAdmission.admissionId());
        assertEquals(AdmissionStatus.REJECTED, history.status());
        assertEquals("Historical horse name", history.candidate().name());
        assertEquals("Original rejection feedback", history.managerFeedback());
        assertEquals("history-regression.pdf", history.documents().getFirst().getOriginalFileName());
        assertEquals(previousOwner, admissions.findById(oldAdmission.admissionId()).orElseThrow().getOwnerId());
        assertEquals(horseId, admissions.findById(oldAdmission.admissionId()).orElseThrow().getHorseId());
    }

    private CreateOwnerAdmissionRequest request(String ueln, String name) {
        return new CreateOwnerAdmissionRequest(name, "Thoroughbred", null, HorseSex.MALE, ueln,
                "Test Registry", null, null, null, null, null);
    }

    @Test
    void sexPersistsThroughAdmissionAndArrivalAndIsVisibleToTheOwner() throws Exception {
        var request = new CreateOwnerAdmissionRequest("Sex round-trip horse", "Arabian",
                LocalDate.of(2021, 3, 21), HorseSex.FEMALE, "999999SEX000001",
                "Test Registry", null, null, null, null, null);
        var created = owners.create(ownerIds.getFirst(), request);
        assertEquals(HorseSex.FEMALE, created.candidate().sex());
        assertEquals("FEMALE", jdbc.queryForObject(
                "SELECT sex FROM candidate_horse_profiles WHERE admission_id = ?", String.class, created.admissionId()));
        assertThrows(org.springframework.dao.DataIntegrityViolationException.class,
                () -> jdbc.update("UPDATE candidate_horse_profiles SET sex = 'OTHER' WHERE admission_id = ?", created.admissionId()));
        assertEquals("YES", jdbc.queryForObject("""
                SELECT is_nullable FROM information_schema.columns
                WHERE table_schema = 'public' AND table_name = 'horses' AND column_name = 'sex'
                """, String.class));
        assertTrue(horses.findAll().stream().anyMatch(h -> h.getSex() == null),
                "Pre-migration seed horses must remain readable without an invented sex");

        Long groomId = jdbc.queryForObject("""
                SELECT u.id FROM users u JOIN roles r ON r.id = u.role_id
                WHERE r.name = 'GROOM' AND u.is_active = true ORDER BY u.id LIMIT 1
                """, Long.class);
        new TransactionTemplate(transactions).executeWithoutResult(tx -> {
            var admission = admissions.findById(created.admissionId()).orElseThrow();
            var stall = stalls.findFirstAvailableQuarantineStallForUpdate().orElseThrow();
            stall.setStatus(StallStatus.RESERVED);
            stalls.saveAndFlush(stall);
            admission.setGroomId(groomId);
            admission.setQuarantineStallId(stall.getId());
            admission.setArrivalDeadlineAt(LocalDateTime.now().plusDays(1));
            admission.setStatus(AdmissionStatus.WAITING_FOR_ARRIVAL);
            admissions.saveAndFlush(admission);
        });
        var arrived = groom.confirmArrival(created.admissionId(), groomId);
        Horse horse = horses.findById(arrived.getHorseId()).orElseThrow();
        assertEquals(HorseSex.FEMALE, horse.getSex());
        assertEquals(HorseStatus.CANDIDATE, horse.getCurrentStatus());

        var mvc = webAppContextSetup(webContext).apply(springSecurity()).build();
        var auth = new UsernamePasswordAuthenticationToken(
                new AuthenticatedUser(ownerIds.getFirst(), "owner@test.invalid", "HORSE_OWNER"), null,
                List.of(new SimpleGrantedAuthority("HORSE_VIEW"),
                        new SimpleGrantedAuthority("ADMISSION_APPLICATION_VIEW")));
        mvc.perform(get("/api/horses/{id}", horse.getId()).with(authentication(auth)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.sex").value("FEMALE"));
        mvc.perform(get("/api/owner/admissions/{id}", created.admissionId()).with(authentication(auth)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.candidate.sex").value("FEMALE"));

        assertThrows(org.springframework.dao.DataIntegrityViolationException.class,
                () -> jdbc.update("UPDATE horses SET sex = 'GELDING' WHERE id = ?", horse.getId()));
        var locked = assertThrows(org.springframework.dao.DataAccessException.class,
                () -> jdbc.update("UPDATE candidate_horse_profiles SET sex = 'MALE' WHERE admission_id = ?", created.admissionId()));
        assertTrue(locked.getMessage().contains("snapshot is locked"));
        assertEquals(HorseSex.FEMALE, owners.getMyAdmission(ownerIds.getFirst(), created.admissionId()).candidate().sex());
    }

    @Test
    void availabilityEndpointIsReadOnlyAndRequiresOwnerWithCreatePermission() throws Exception {
        String ueln = "999999API000001";
        owners.create(ownerIds.getFirst(), request(ueln, "Availability endpoint test"));
        long countBefore = admissions.count();
        var mvc = webAppContextSetup(webContext).apply(springSecurity()).build();
        var permission = List.of(new SimpleGrantedAuthority("ADMISSION_APPLICATION_CREATE"));
        var owner = new AuthenticatedUser(ownerIds.getFirst(), "owner@test.invalid", "HORSE_OWNER");
        var auth = new UsernamePasswordAuthenticationToken(owner, null, permission);

        mvc.perform(get("/api/owner/admissions/ueln-availability")
                        .param("ueln", " " + ueln.toLowerCase() + " ").with(authentication(auth)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.available").value(false))
                .andExpect(jsonPath("$.data.ownerId").doesNotExist());
        mvc.perform(get("/api/owner/admissions/ueln-availability")
                        .param("ueln", "999999API000002").with(authentication(auth)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.available").value(true));
        mvc.perform(get("/api/owner/admissions/ueln-availability")
                        .param("ueln", "invalid").with(authentication(auth)))
                .andExpect(status().isBadRequest());

        var manager = new AuthenticatedUser(ownerIds.getFirst(), "manager@test.invalid", "CLUB_MANAGER");
        mvc.perform(get("/api/owner/admissions/ueln-availability").param("ueln", ueln)
                        .with(authentication(new UsernamePasswordAuthenticationToken(manager, null, permission))))
                .andExpect(status().isForbidden());
        mvc.perform(get("/api/owner/admissions/ueln-availability").param("ueln", ueln)
                        .with(authentication(new UsernamePasswordAuthenticationToken(owner, null, List.of()))))
                .andExpect(status().isForbidden());
        assertEquals(countBefore, admissions.count());
    }

    @Test
    void submitChecksInternalParentBirthDatesAndAllowsExternalParents() throws Exception {
        String parentUeln = "999999DAT000001";
        Long parentId = new TransactionTemplate(transactions).execute(tx -> {
            Horse parent = new Horse();
            parent.setName("Birth date test parent");
            parent.setRegistrationNumber(parentUeln);
            parent.setDateOfBirth(LocalDate.of(2022, 4, 15));
            parent.setOwnerId(ownerIds.getFirst());
            return horses.saveAndFlush(parent).getId();
        });
        var mvc = webAppContextSetup(webContext).apply(springSecurity()).build();
        var auth = new UsernamePasswordAuthenticationToken(
                new AuthenticatedUser(ownerIds.getFirst(), "owner@test.invalid", "HORSE_OWNER"), null,
                List.of(new SimpleGrantedAuthority("ADMISSION_APPLICATION_CREATE"),
                        new SimpleGrantedAuthority("ADMISSION_DOCUMENT_CREATE")));
        long admissionCount = admissions.count();
        long documentCount = documents.count();
        String metadata = """
                [{"documentType":"HORSE_PHOTO"},{"documentType":"REGISTRATION_DOCUMENT"},
                 {"documentType":"PEDIGREE_CERTIFICATE"},{"documentType":"VACCINATION_RECORD"}]
                """;
        byte[] png = Base64.getDecoder().decode(
                "iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAQAAAC1HAwCAAAAC0lEQVR42mP8/x8AAwMCAO+a4ZkAAAAASUVORK5CYII=");
        byte[] pdf = "%PDF-1.4\n%%EOF\n".getBytes(StandardCharsets.UTF_8);
        for (String relation : List.of("sire", "dam")) {
            for (String birthDate : List.of("2022-04-14", "2022-04-15")) {
                String candidate = """
                        {"name":"Birth date test child","sex":"MALE","dateOfBirth":"%s", "registrationNumber":"999999DAT000002",
                         "%sRegistrationNumber":"999999DAT000001"}
                        """.formatted(birthDate, relation);
                mvc.perform(multipart("/api/owner/admissions")
                                .file(new MockMultipartFile("candidate", "", "application/json", candidate.getBytes(StandardCharsets.UTF_8)))
                                .file(new MockMultipartFile("documents", "", "application/json", metadata.getBytes(StandardCharsets.UTF_8)))
                                .file(new MockMultipartFile("files", "horse.png", "image/png", png))
                                .file(new MockMultipartFile("files", "registration.pdf", "application/pdf", pdf))
                                .file(new MockMultipartFile("files", "pedigree.pdf", "application/pdf", pdf))
                                .file(new MockMultipartFile("files", "vaccination.pdf", "application/pdf", pdf))
                                .with(authentication(auth)))
                        .andExpect(status().isBadRequest())
                        .andExpect(jsonPath("$.message").value(containsString(relation)));
            }
        }
        assertEquals(admissionCount, admissions.count());
        assertEquals(documentCount, documents.count());

        new TransactionTemplate(transactions).executeWithoutResult(tx -> {
            Horse parent = horses.findById(parentId).orElseThrow();
            parent.setDateOfBirth(LocalDate.of(2018, 4, 15));
            horses.saveAndFlush(parent);
        });
        assertDoesNotThrow(() -> owners.create(ownerIds.getFirst(), new CreateOwnerAdmissionRequest(
                "Valid internal parent", null, LocalDate.of(2022, 4, 15), HorseSex.MALE, "999999DAT000002",
                null, null, parentUeln, null, null, null)));
        assertDoesNotThrow(() -> owners.create(ownerIds.getFirst(), new CreateOwnerAdmissionRequest(
                "External parent", null, LocalDate.of(2022, 4, 15), HorseSex.MALE, "999999DAT000003",
                null, "External sire", "999999EXT000001", null, null, null)));
    }
}
