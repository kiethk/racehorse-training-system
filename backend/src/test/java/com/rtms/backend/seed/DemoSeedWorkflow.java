package com.rtms.backend.seed;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.rtms.backend.admission.dto.*;
import com.rtms.backend.admission.entity.AdmissionApplication;
import com.rtms.backend.admission.enums.*;
import com.rtms.backend.admission.repository.AdmissionApplicationRepository;
import com.rtms.backend.admission.service.*;
import com.rtms.backend.common.storage.FileStorageService;
import com.rtms.backend.horse.enums.HorseSex;
import com.rtms.backend.horse.repository.HorseRepository;
import com.rtms.backend.identity.dto.*;
import com.rtms.backend.identity.entity.User;
import com.rtms.backend.identity.repository.UserRepository;
import com.rtms.backend.identity.service.StaffManagementService;
import com.rtms.backend.identity.service.AuthService;
import com.rtms.backend.scheduler.*;
import com.rtms.backend.security.JwtUtil;
import com.rtms.backend.stable.dto.*;
import com.rtms.backend.stable.enums.AreaType;
import com.rtms.backend.stable.repository.*;
import com.rtms.backend.stable.service.*;
import com.rtms.backend.training.enums.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.mockito.MockedStatic;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.multipart.MultipartFile;

import javax.sql.DataSource;
import java.math.BigDecimal;
import java.nio.file.*;
import java.security.MessageDigest;
import java.time.*;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.setup.MockMvcBuilders.webAppContextSetup;

/** Shared opt-in workflow; environment binding is defined only by the concrete local/cloud runners. */
abstract class DemoSeedWorkflow {
    protected abstract void verifyTarget(String jdbcUrl) throws Exception;
    protected boolean cloudMode() { return false; }

    private void verifyCloudStorage() throws Exception {
        Path folder=root.resolve(text(data.get("horses").get(0),"assetFolder"));
        for (String name:List.of("horse-photo.jpg","registration.pdf")) {
            byte[] expected=Files.readAllBytes(folder.resolve(name));
            String key=null;
            try {
                key=storage.store(new MockMultipartFile("file",name,name.endsWith("jpg")?"image/jpeg":"application/pdf",expected),"rtms/seed-preflight");
                assertFalse(storage.isLocal(key));
                try(var stream=storage.load(key).getInputStream()) { assertArrayEquals(expected,stream.readAllBytes(),"Private Cloudinary delivery must preserve original bytes"); }
            } finally { if (key!=null) storage.delete(key); }
        }
    }

    @Autowired DataSource datasource;
    @Autowired JdbcTemplate jdbc;
    @Autowired PlatformTransactionManager transactions;
    @Autowired OwnerAdmissionService owners;
    @Autowired AdmissionGroomReviewService grooms;
    @Autowired AdmissionReviewService vets;
    @Autowired AdmissionTrainerReviewService trainers;
    @Autowired AdmissionManagerReviewService managers;
    @Autowired TrainerAssignmentService trainerAssignments;
    @Autowired AdmissionApplicationRepository admissions;
    @Autowired HorseRepository horses;
    @Autowired StaffManagementService staffService;
    @Autowired AuthService authService;
    @Autowired UserRepository users;
    @Autowired AreaService areaService;
    @Autowired AreaRepository areas;
    @Autowired StableStallRepository stalls;
    @Autowired StableAssignmentService assignments;
    @Autowired FileStorageService storage;
    @Autowired WebApplicationContext web;
    @Autowired JwtUtil jwt;
    // Seed context has no maintenance jobs; production service/event handlers remain real.
    @MockitoBean AdmissionArrivalScheduler arrivalScheduler;
    @MockitoBean CareScheduleScheduler careScheduler;
    @MockitoBean GroomDailyTaskScheduler dailyScheduler;
    @MockitoBean TrainingPlanStatusScheduler planScheduler;

    final ObjectMapper json = new ObjectMapper();
    final Map<String, Long> staffIds = new LinkedHashMap<>();
    final Map<String, Long> admissionIds = new LinkedHashMap<>();
    final Map<String, Long> ownerIds = new LinkedHashMap<>();
    final AtomicReference<LocalDateTime> now = new AtomicReference<>(LocalDateTime.of(2026, 3, 1, 9, 0));
    Path root;
    JsonNode data;

    @Test
    void seedAndVerify() throws Exception {
        try (var connection = datasource.getConnection()) { verifyTarget(connection.getMetaData().getURL()); }
        root = Path.of(Objects.requireNonNull(System.getProperty("rtms.seed.assetsRoot"), "Pass assetsRoot")).toRealPath();
        data = json.readTree(root.resolve("seed-preview.json").toFile());
        if (Boolean.getBoolean("rtms.seed.verifyOnly") || Boolean.getBoolean("rtms.seed.addOwners")) {
            Path savedPath=resultPath();
            if (cloudMode() && !Files.exists(savedPath) && Boolean.getBoolean("rtms.seed.verifyOnly"))
                savedPath=Path.of("target/cloud-seed/checkpoint.json");
            JsonNode saved=json.readTree(savedPath.toFile());
            saved.get("staffIds").fields().forEachRemaining(e -> staffIds.put(e.getKey(),e.getValue().asLong()));
            saved.get("admissionIds").fields().forEachRemaining(e -> admissionIds.put(e.getKey(),e.getValue().asLong()));
            if (saved.has("ownerIds")) saved.get("ownerIds").fields().forEachRemaining(e -> ownerIds.put(e.getKey(),e.getValue().asLong()));
            if (Boolean.getBoolean("rtms.seed.addOwners")) { supplementOwners(); return; }
            verifyResult();
            return;
        }
        preflight();
        if (cloudMode()) {
            verifyCloudStorage();
            if (Boolean.getBoolean("rtms.seed.cloud.preflightOnly")) { System.out.println("CLOUD_PREFLIGHT_PASS: files and private Cloudinary delivery checked; database unchanged"); return; }
        }
        try (MockedStatic<LocalDateTime> time = mockStatic(LocalDateTime.class, CALLS_REAL_METHODS);
             MockedStatic<LocalDate> day = mockStatic(LocalDate.class, CALLS_REAL_METHODS)) {
            time.when(LocalDateTime::now).thenAnswer(i -> now.get());
            day.when(LocalDate::now).thenAnswer(i -> now.get().toLocalDate());
            tx(() -> { prepareOwners(); prepareStaffAndStable(); return null; });
            for (JsonNode p : data.get("horses")) seedAdmission(p);
            // Expired/rejected records first: no outstanding reservation may skew Q allocation.
            for (String key : List.of("AP054", "AP053", "AP051", "AP052", "AP049", "AP050")) {
                for (JsonNode p : data.get("additionalAdmissions")) if (key.equals(text(p, "key"))) seedAdmission(p);
            }
        }
        postProcessAssignments();
        verifyResult();
    }

    private void prepareOwners() {
        if (!data.has("owners")) return; // Compatibility with the original one-Owner pack.
        assertEquals(8, data.get("owners").size());
        for (JsonNode owner : data.get("owners")) {
            String key=text(owner,"key"), email=text(owner,"email");
            if (owner.hasNonNull("existingUserId")) {
                User existing=users.findById(owner.get("existingUserId").asLong()).orElseThrow();
                assertEquals("HORSE_OWNER",existing.getRole().getName());
                assertEquals(email,existing.getEmail());
                assertTrue(existing.isActive());
                assertTrue(Set.of("Test Owner",text(owner,"fullName")).contains(existing.getFullName()),
                        "Unexpected custom Owner name; review rather than overwriting it");
                existing.setFullName(text(owner,"fullName"));
                if (existing.getPhone()==null || existing.getPhone().isBlank()) existing.setPhone(text(owner,"phone"));
                if (existing.getAddress()==null || existing.getAddress().isBlank()) existing.setAddress(text(owner,"address"));
                users.saveAndFlush(existing); // Password, email, ID and avatar are untouched.
                ownerIds.put(key,existing.getId());
            } else {
                assertTrue(users.findByEmail(email).isEmpty(),"Owner account already exists; refusing duplicate seed");
                OwnerRegistrationRequest request=new OwnerRegistrationRequest();
                request.setFullName(text(owner,"fullName")); request.setEmail(email);
                request.setPhone(text(owner,"phone")); request.setAddress(text(owner,"address"));
                request.setPassword(Objects.requireNonNull(System.getenv("RTMS_OWNER_SEED_PASSWORD"),"Set RTMS_OWNER_SEED_PASSWORD"));
                Long id=authService.registerOwner(request).getUserId();
                // Seed-only account chronology; no public backdating capability is added.
                jdbc.update("UPDATE users SET created_at=? WHERE id=?",date(owner,"createdDate").atStartOfDay(),id);
                ownerIds.put(key,id);
            }
        }
    }

    /** Refines only the exact already-seeded local dataset; never a production ownership transfer. */
    private void supplementOwners() {
        assertEquals(32L,count("users"));
        assertEquals(1L,jdbc.queryForObject("SELECT count(*) FROM users u JOIN roles r ON r.id=u.role_id WHERE r.name='HORSE_OWNER'",Long.class));
        assertEquals(48L,count("horses")); assertEquals(54L,count("admission_applications"));
        assertEquals(54,admissionIds.size());
        assertTrue(ownerIds.isEmpty(),"Owner supplement already applied; use VerifyOnly");
        Map<String,List<String>> baseline=unchangedRows();
        User original=users.findById(5L).orElseThrow();
        String originalHash=original.getPasswordHash(), originalAvatar=original.getAvatarUrl();
        for (JsonNode profile:profiles()) {
            Long id=admissionIds.get(text(profile,"key")); assertNotNull(id);
            assertEquals(1L,jdbc.queryForObject("SELECT count(*) FROM admission_applications a JOIN candidate_horse_profiles p ON p.admission_id=a.id WHERE a.id=? AND a.owner_id=5 AND p.registration_number=? AND a.status=?",Long.class,
                    id,text(profile,"registrationNumber"),text(profile,"admissionStatus")),text(profile,"key"));
        }
        assertEquals(0L,jdbc.queryForObject("SELECT count(*) FROM horses WHERE owner_id<>5 OR owner_id IS NULL",Long.class));
        // Stop instead of accidentally keeping notifications addressed to a different Owner.
        assertEquals(0L,jdbc.queryForObject("SELECT count(*) FROM notifications WHERE recipient_id=5",Long.class));
        tx(() -> {
            prepareOwners();
            for (JsonNode profile:profiles()) {
                Long id=admissionIds.get(text(profile,"key")), owner=ownerIds.get(text(profile,"ownerKey"));
                assertNotNull(owner);
                assertEquals(1,jdbc.update("UPDATE admission_applications SET owner_id=? WHERE id=? AND owner_id=5",owner,id));
                Long horse=jdbc.queryForObject("SELECT horse_id FROM admission_applications WHERE id=?",Long.class,id);
                if (horse!=null) assertEquals(1,jdbc.update("UPDATE horses SET owner_id=? WHERE id=? AND owner_id=5",owner,horse));
            }
            assertEquals(baseline,unchangedRows(),"Only Owner identity associations may change");
            User preserved=users.findById(5L).orElseThrow();
            assertEquals(originalHash,preserved.getPasswordHash()); assertEquals(originalAvatar,preserved.getAvatarUrl());
            try { verifyResult(); } catch (Exception failure) { throw new IllegalStateException("Owner supplement verification failed",failure); }
            return null;
        });
        // Expected 403s must run after commit: transactional services mark joined transactions
        // rollback-only when denying access, even when MockMvc correctly handles the exception.
        verifyAfterOwnerCommit();
    }

    private void verifyAfterOwnerCommit() {
        try { verifyResult(); } catch (Exception failure) { throw new IllegalStateException("Post-commit Owner verification failed; inspect the backup before retrying",failure); }
    }

    private Map<String,List<String>> unchangedRows() {
        Map<String,List<String>> result=new LinkedHashMap<>();
        for (String table:List.of("horses","admission_applications","admission_documents","candidate_horse_profiles",
                "areas","stable_stalls","groom_profiles","trainer_profiles","veterinarian_profiles","subjects",
                "health_records","racing_readiness_assessments","notifications","audit_logs")) {
            String order=table.endsWith("_profiles") && !table.equals("candidate_horse_profiles") ? "user_id" : "id";
            result.put(table,jdbc.queryForList("SELECT (to_jsonb(t)-'owner_id')::text FROM "+table+" t ORDER BY "+order,String.class));
        }
        return result;
    }

    private void preflight() throws Exception {
        assertEquals(48, data.get("horses").size());
        assertEquals(6, data.get("additionalAdmissions").size());
        assertEquals(30, data.get("staff").size());
        // Never wipe an already seeded/user-populated database. Fresh migration fixtures only.
        assertEquals(List.of("BOOSTER WINNER", "JEWELLE DARK", "QABALAH MERCURY"),
                jdbc.queryForList("SELECT name FROM horses ORDER BY id", String.class),
                "Expected only migration fixtures; refusing cleanup/reseed of other data");
        assertEquals(3L,jdbc.queryForObject("SELECT count(*) FROM horses WHERE id IN (5,6,7) AND registration_number IS NULL AND date_of_birth IS NULL AND sex IS NULL AND owner_id=5",Long.class),
                "Migration horse identities changed; refusing cleanup");
        assertEquals(1L, count("admission_applications"));
        assertEquals(0L, count("admission_documents"));
        assertEquals(11L, count("users"));
        for (String table : List.of("horse_training_plans", "training_lots", "training_workouts", "groom_daily_tasks",
                "health_records", "horse_health_metrics", "injury_records", "treatment_plans", "race_registrations")) {
            assertEquals(0L, count(table), "Unexpected existing dependencies in " + table);
        }
        List<String> manifest = Files.readAllLines(root.resolve("upload-manifest.csv"));
        assertEquals(325, manifest.size());
        for (String line : manifest.subList(1, manifest.size())) {
            List<String> fields = csv(line);
            Path file = root.resolve(fields.get(3)).normalize();
            assertTrue(file.startsWith(root) && Files.isRegularFile(file));
            assertEquals(Long.parseLong(fields.get(8)), Files.size(file));
            assertEquals(fields.get(9), HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(Files.readAllBytes(file))));
        }
        for (JsonNode p : profiles()) {
            assertTrue(Set.of("MALE", "FEMALE").contains(text(p, "sex")));
            assertTrue(LocalDate.parse(text(p, "dateOfBirth")).isBefore(LocalDate.parse(text(p, "submittedDate"))));
        }
    }

    private void prepareStaffAndStable() {
        // Delete only identified migration fixtures. FK errors abort this entire preparation transaction.
        long oldCare=count("care_schedule");
        assertTrue(oldCare>=1 && oldCare<=2,"Unexpected legacy care schedules");
        assertEquals(oldCare,(long)jdbc.update("DELETE FROM care_schedule WHERE horse_id=7 AND id IN (1,2)"));
        assertEquals(1, jdbc.update("DELETE FROM horse_pedigrees WHERE horse_id=7 AND sire_id=5 AND dam_id=6"));
        assertEquals(1L,jdbc.queryForObject("SELECT count(*) FROM candidate_horse_profiles WHERE admission_id=1",Long.class));
        // Delete the parent: ON DELETE CASCADE removes its snapshot without disabling immutability triggers.
        assertEquals(1, jdbc.update("DELETE FROM admission_applications WHERE id=1 AND horse_id=7"));
        assertEquals(0L,count("candidate_horse_profiles"));
        assertEquals(3, jdbc.update("DELETE FROM horses WHERE id IN (5,6,7) AND registration_number IS NULL"));
        jdbc.update("UPDATE stable_stalls SET status='AVAILABLE' WHERE id IN (1,6,10)");
        Map<String, String> legacy = Map.of("TR03", "trainer@rtms.com", "VT03", "vet@rtms.com", "GR03", "groom@rtms.com");
        for (JsonNode s : data.get("staff")) {
            String key = text(s, "key"), email = text(s, "email");
            Optional<User> old = users.findByEmail(email);
            if (old.isEmpty() && legacy.containsKey(key)) old = users.findByEmail(legacy.get(key));
            if (old.isPresent()) {
                User user = old.get();
                assertEquals(text(s, "role"), user.getRole().getName());
                // Seed-only identity normalization; public Staff API deliberately cannot rename email.
                user.setEmail(email);
                users.saveAndFlush(user);
                StaffUpdateRequest update = new StaffUpdateRequest();
                update.setFullName(text(s, "fullName")); update.setPhone(text(s, "phone")); update.setAddress(text(s, "address"));
                update.setCertificationNumber(text(s, "certificationNumber")); update.setCertificationIssuedDate(date(s,"certificationIssuedDate"));
                update.setLicenseNumber(text(s, "licenseNumber")); update.setLicenseIssuedDate(date(s,"licenseIssuedDate"));
                update.setSpecialization(text(s,"specialization"));
                staffService.updateStaff(user.getId(), update);
                staffIds.put(key,user.getId());
            }
        }
        assertEquals(9, staffIds.size());
        assertEquals(4L, jdbc.queryForObject("SELECT count(*) FROM subjects WHERE trainer_id=2",Long.class));
        assertTrue(areas.findByCode("E").isEmpty());
        assertEquals("E", areaService.createArea(AreaType.REGULAR).getCode());
        List<AreaTrainerAssignmentDto> trainerEdits = new ArrayList<>();
        for (JsonNode a : data.get("areas")) {
            var area = areas.findByCode(text(a,"key")).orElseThrow();
            trainerEdits.add(new AreaTrainerAssignmentDto(area.getId(),staffIds.get(text(a,"trainerKey")),area.getTrainerId()));
        }
        assignments.applyAssignments(new BatchStableAssignmentRequest(trainerEdits,List.of()));
        for (JsonNode s : data.get("staff")) {
            String key = text(s,"key");
            if (staffIds.containsKey(key)) continue;
            assertEquals("GROOM",text(s,"role"));
            StaffCreationRequest create = new StaffCreationRequest();
            create.setFullName(text(s,"fullName")); create.setEmail(text(s,"email")); create.setRole("GROOM");
            create.setPhone(text(s,"phone")); create.setAddress(text(s,"address"));
            create.setPassword(Objects.requireNonNull(System.getenv("RTMS_SEED_PASSWORD"),"Set RTMS_SEED_PASSWORD for new Grooms"));
            staffIds.put(key,staffService.createStaff(create).getUserId());
        }
        List<BlockGroomAssignmentDto> groomEdits = new ArrayList<>();
        for (JsonNode s : data.get("staff")) if ("GROOM".equals(text(s,"role"))) {
            var area = areas.findByCode(text(s,"areaKey")).orElseThrow();
            int block = s.get("blockNumber").asInt();
            var baseline = stalls.findByAreaId(area.getId()).stream()
                    .filter(st -> st.getStallNumber() > (block-1)*3 && st.getStallNumber() <= block*3)
                    .sorted(Comparator.comparing(com.rtms.backend.stable.entity.StableStall::getStallNumber))
                    .map(com.rtms.backend.stable.entity.StableStall::getGroomId).toList();
            BlockGroomAssignmentDto edit = new BlockGroomAssignmentDto(area.getId(),block,staffIds.get(text(s,"key")));
            edit.setExpectedGroomIds(baseline);
            groomEdits.add(edit);
        }
        assignments.applyAssignments(new BatchStableAssignmentRequest(List.of(),groomEdits));
        // Migration accounts predate this demo's May admissions; no password or ID changes.
        jdbc.update("UPDATE users SET created_at=? WHERE id BETWEEN 1 AND 11 AND created_at>?",now.get(),now.get());
        jdbc.update("UPDATE areas SET created_at=? WHERE created_at>?",now.get(),now.get());
        jdbc.update("UPDATE stable_stalls SET created_at=? WHERE created_at>?",now.get(),now.get());
        System.out.println("SEED: prepared 30 Staff, 6 Areas, 72 Stalls; 4 Subjects preserved");
    }

    private void seedAdmission(JsonNode p) throws Exception {
        String key=text(p,"key"), target=text(p,"admissionStatus");
        Path folder=root.resolve(text(p,"assetFolder"));
        JsonNode request=json.readTree(folder.resolve("admission-request.json").toFile());
        CreateOwnerAdmissionRequest candidate=new CreateOwnerAdmissionRequest(text(request,"name"),text(request,"breed"),date(request,"dateOfBirth"),
                HorseSex.valueOf(text(request,"sex")),text(request,"registrationNumber"),text(request,"registryName"),text(request,"sireName"),
                text(request,"sireRegistrationNumber"),text(request,"damName"),text(request,"damRegistrationNumber"),text(request,"pedigreeNotes"));
        List<AdmissionDocumentMetadataRequest> metadata=new ArrayList<>();
        for (JsonNode m:json.readTree(folder.resolve("documents-metadata.json").toFile()))
            metadata.add(new AdmissionDocumentMetadataRequest(AdmissionDocumentType.valueOf(text(m,"documentType")),date(m,"recordDate"),text(m,"note")));
        List<MultipartFile> files=new ArrayList<>();
        for (String name:List.of("horse-photo.jpg","registration.pdf","pedigree.pdf","vaccination-record.pdf","health-certificate.pdf","deworming-record.pdf"))
            files.add(new MockMultipartFile("files",name,name.endsWith("jpg")?"image/jpeg":"application/pdf",Files.readAllBytes(folder.resolve(name))));
        Long groomId = staffIds.get(
                "WAITING_FOR_ARRIVAL".equals(target) ? "GR01" :
                (key.startsWith("H") ? (p.get("areaKey").asText().equals("Q-A") ? text(p, "groomKey") : "GR21") : "GR23")
        );
        Long id=tx(() -> {
            at(date(p,"submittedDate"),9,0);
            Long owner=ownerIds.isEmpty()?5L:Objects.requireNonNull(ownerIds.get(text(p,"ownerKey")));
            Long created=owners.submit(owner,candidate,metadata,files).admissionId();
            admissionIds.put(key,created);
            if ("GROOM_REVIEW".equals(target)) return created;
            at(date(p,"groomReviewedDate")!=null?date(p,"groomReviewedDate"):date(p,"submittedDate"),10,0);
            GroomAdmissionReviewRequest review=new GroomAdmissionReviewRequest();
            review.setDecision("REJECTED".equals(target)?ReviewDecision.REJECTED:ReviewDecision.APPROVED);
            review.setFeedback("REJECTED".equals(target)?"Demo: vaccination records are out of date; updated documents required.":"Demo: identity and required dossier checked; arrival reservation confirmed.");
            grooms.review(created,groomId,review);
            if ("REJECTED".equals(target)||"WAITING_FOR_ARRIVAL".equals(target)) return created;
            if ("ARRIVAL_EXPIRED".equals(target)) {
                at(date(p,"expiredDate"),9,0); grooms.expireOverdueArrivalReservations(); return created;
            }
            at(date(p,"arrivedDate"),9,0);
            grooms.confirmArrival(created,groomId);

            if ("VET_REVIEW".equals(target)) {
                Long vet1Id = staffIds.get("VT01");
                jdbc.update("UPDATE care_schedule SET veterinarian_id = ? WHERE admission_id = ?", vet1Id, created);
                jdbc.update("UPDATE admission_applications SET veterinarian_id = ? WHERE id = ?", vet1Id, created);
                return created;
            }

            at(date(p,"arrivedDate"),15,0);
            AdmissionApplication adm=admissions.findById(created).orElseThrow();
            assertNotNull(adm.getVeterinarianId(),"Initial exam must have a real Vet assignment");
            VetReviewRequest vet=new VetReviewRequest();
            vet.setTrainingDecision(TrainingDecision.ALLOWED); vet.setPhysicalExamConfirmed(true);
            vet.setFindings("Demo admission examination: alert, normal gait at walk, no visible injury or respiratory distress.");
            vet.setDiagnosis("No contraindication identified in this simulated intake examination.");
            vet.setNotes("DEMO intake examination after arrival; supplied certificates checked. No treatment indicated.");
            vets.reviewByVet(created,vet,adm.getVeterinarianId());

            // Sau khi reviewByVet hoan tat, chia deu lich su kham cho 3 Vet theo khu vuc
            Long targetVetId;
            String stallCode = p.hasNonNull("stallCode") ? text(p, "stallCode") : "";
            if (stallCode.startsWith("A") || stallCode.startsWith("B") || "H047".equals(key)) {
                targetVetId = staffIds.get("VT01");
            } else if (stallCode.startsWith("C") || stallCode.startsWith("D") || "H048".equals(key)) {
                targetVetId = staffIds.get("VT02");
            } else {
                targetVetId = staffIds.get("VT03");
            }
            jdbc.update("UPDATE care_schedule SET veterinarian_id = ? WHERE admission_id = ?", targetVetId, created);
            jdbc.update("UPDATE admission_applications SET veterinarian_id = ? WHERE id = ?", targetVetId, created);
            jdbc.update("UPDATE health_records SET veterinarian_id = ? WHERE care_schedule_id IN (SELECT id FROM care_schedule WHERE admission_id = ?)", targetVetId, created);

            return created;
        });
        if (Set.of("TRAINER_REVIEW","MANAGER_REVIEW","APPROVED").contains(target)) {
            trainerAssignments.assignIfNeeded(id);

            // Reassign trainer:
            // - If TRAINER_REVIEW (H045, H046): both assigned to trainer1 (TR01)
            // - If APPROVED / MANAGER_REVIEW: Area A/B -> TR01, Area C/D -> TR02, Area E -> TR03
            Long desiredTrainerId;
            String stallCode = p.hasNonNull("stallCode") ? text(p, "stallCode") : "";
            if ("TRAINER_REVIEW".equals(target) || stallCode.startsWith("A") || stallCode.startsWith("B") || "H047".equals(key)) {
                desiredTrainerId = staffIds.get("TR01");
            } else if (stallCode.startsWith("C") || stallCode.startsWith("D") || "H048".equals(key)) {
                desiredTrainerId = staffIds.get("TR02");
            } else {
                desiredTrainerId = staffIds.get("TR03");
            }
            jdbc.update("UPDATE admission_applications SET trainer_id = ? WHERE id = ?", desiredTrainerId, id);

            if (!"TRAINER_REVIEW".equals(target)) tx(() -> {
                at(date(p,"arrivedDate").plusDays(1),10,0);
                AdmissionApplication adm=admissions.findById(id).orElseThrow();
                TrainerAdmissionReviewRequest assess=new TrainerAdmissionReviewRequest();
                int n=Integer.parseInt(key.substring(1));
                assess.setReadinessStatus(RacingReadinessStatus.NEEDS_MORE_TRAINING);
                assess.setConformationScore(BigDecimal.valueOf(7+(n%3)*0.5));
                assess.setTemperamentScore(BigDecimal.valueOf(7+((n+1)%3)*0.5));
                assess.setGaitQualityScore(BigDecimal.valueOf(7+((n+2)%3)*0.5));
                assess.setEstimatedMonthsToRace(3+n%4);
                assess.setRemarks("DEMO admission assessment: suitable to begin conditioning after intake; review readiness after a structured training period.");
                trainers.completeAssessment(id,assess,adm.getTrainerId());
                if ("APPROVED".equals(target)) {
                    at(date(p,"approvedDate"),14,0);
                    ManagerReviewRequest decision=new ManagerReviewRequest(); decision.setDecision(ReviewDecision.APPROVED);
                    decision.setFeedback("DEMO: dossier, intake examination and trainer assessment reviewed; admitted to regular stable.");
                    decision.setStallId(stalls.findAll().stream().filter(s -> text(p,"stallCode").equals(s.getStallCode())).findFirst().orElseThrow().getId());
                    managers.review(id,1L,decision);
                }
                return null;
            });
        }
        assertEquals(AdmissionStatus.valueOf(target),admissions.findById(id).orElseThrow().getStatus(),key);
        if (cloudMode()) {
            Path checkpoint=Path.of("target/cloud-seed/checkpoint.json");Files.createDirectories(checkpoint.getParent());
            json.writerWithDefaultPrettyPrinter().writeValue(checkpoint.toFile(),Map.of("staffIds",staffIds,"ownerIds",ownerIds,"admissionIds",admissionIds,"lastCompleted",key));
        }
        System.out.println("SEED: "+key+" "+text(p,"name")+" -> "+target);
    }

    private void postProcessAssignments() {
        Long vt01 = staffIds.get("VT01");
        Long vt02 = staffIds.get("VT02");
        Long vt03 = staffIds.get("VT03");
        jdbc.update("""
            UPDATE admission_applications a
            SET veterinarian_id = CASE
                WHEN ar.code IN ('A', 'B') THEN ?
                WHEN ar.code IN ('C', 'D') THEN ?
                ELSE ?
            END
            FROM horses h
            JOIN stable_stalls s ON h.current_stall_id = s.id
            JOIN areas ar ON s.area_id = ar.id
            WHERE a.horse_id = h.id AND a.status = 'APPROVED'
        """, vt01, vt02, vt03);

        jdbc.update("""
            UPDATE care_schedule cs
            SET veterinarian_id = a.veterinarian_id
            FROM admission_applications a
            WHERE cs.admission_id = a.id AND a.status = 'APPROVED'
        """);

        jdbc.update("""
            UPDATE health_records hr
            SET veterinarian_id = cs.veterinarian_id
            FROM care_schedule cs
            WHERE hr.care_schedule_id = cs.id
        """);
    }

    private void verifyResult() throws Exception {
        assertEquals(48L,count("horses")); assertEquals(54L,count("admission_applications"));
        assertEquals(324L,count("admission_documents")); assertEquals(72L,count("stable_stalls")); assertEquals(6L,count("areas"));
        assertEquals(ownerIds.isEmpty()?32L:39L,count("users")); assertEquals(24L,count("groom_profiles"));
        assertEquals(3L,count("trainer_profiles")); assertEquals(3L,count("veterinarian_profiles")); assertEquals(4L,count("subjects"));
        assertEquals(0L,jdbc.queryForObject("SELECT count(*) FROM horses WHERE sex IS NULL OR date_of_birth IS NULL OR registration_number IS NULL OR owner_id IS NULL",Long.class));
        assertEquals(0L,jdbc.queryForObject("SELECT count(*) FROM (SELECT groom_id FROM stable_stalls GROUP BY groom_id HAVING count(*)<>3 OR groom_id IS NULL) b",Long.class));
        assertEquals(0L,jdbc.queryForObject("SELECT count(*) FROM stable_stalls s JOIN areas a ON a.id=s.area_id JOIN groom_profiles g ON g.user_id=s.groom_id WHERE g.trainer_id IS DISTINCT FROM a.trainer_id",Long.class));
        assertEquals(42L,jdbc.queryForObject("SELECT count(*) FROM horses WHERE current_status='ELIGIBLE'",Long.class));
        assertEquals(6L,jdbc.queryForObject("SELECT count(*) FROM horses WHERE current_status='CANDIDATE'",Long.class));
        assertEquals(48L,jdbc.queryForObject("SELECT count(*) FROM stable_stalls WHERE status='OCCUPIED'",Long.class));
        assertEquals(2L,jdbc.queryForObject("SELECT count(*) FROM stable_stalls WHERE status='RESERVED'",Long.class));
        assertEquals(46L,count("health_records")); assertEquals(44L,count("racing_readiness_assessments"));
        assertEquals(0L,jdbc.queryForObject("SELECT count(*) FROM admission_applications WHERE arrived_at<submitted_at OR vet_reviewed_at<arrived_at OR trainer_reviewed_at<vet_reviewed_at OR manager_reviewed_at<trainer_reviewed_at",Long.class));
        for (String table:List.of("horse_training_plans","training_lots","training_workouts","groom_daily_tasks","race_registrations","horse_health_metrics")) assertEquals(0L,count(table));
        int checkedFiles=0;
        for (JsonNode p:profiles()) for (var row:jdbc.queryForList("SELECT file_url,original_file_name FROM admission_documents WHERE admission_id=?",admissionIds.get(text(p,"key")))) {
            String key=row.get("file_url").toString();
            if (cloudMode()) assertTrue(key.startsWith("https://res.cloudinary.com/") && key.contains("/image/authenticated/"), "Cloud seed cannot contain local/public file keys");
            else assertTrue(storage.isLocal(key));
            byte[] expected=Files.readAllBytes(root.resolve(text(p,"assetFolder")).resolve(row.get("original_file_name").toString()));
            try(var stream=storage.load(key).getInputStream()) {
                assertArrayEquals(MessageDigest.getInstance("SHA-256").digest(expected),MessageDigest.getInstance("SHA-256").digest(stream.readAllBytes()));
            }
            checkedFiles++;
        }
        MockMvc mvc=webAppContextSetup(web).apply(springSecurity()).build();
        User manager=users.findById(1L).orElseThrow();
        String token=jwt.generateToken(manager.getId(),manager.getEmail(),manager.getRole().getName());
        for(var horse:horses.findAll()) {
            byte[] bytes=mvc.perform(get("/api/horses/"+horse.getId()+"/photo").header("Authorization","Bearer "+token))
                    .andExpect(status().isOk()).andReturn().getResponse().getContentAsByteArray();
            assertTrue(bytes.length>1000);
        }
        AdmissionApplication first=admissions.findById(admissionIds.get("H001")).orElseThrow();
        for (Long userId:List.of(1L,5L,first.getGroomId(),first.getVeterinarianId(),first.getTrainerId())) {
            User actor=users.findById(userId).orElseThrow();
            String access=jwt.generateToken(actor.getId(),actor.getEmail(),actor.getRole().getName());
            mvc.perform(get("/api/admissions/"+first.getId()).header("Authorization","Bearer "+access)).andExpect(status().isOk());
        }
        for(JsonNode p:profiles()) {
            var adm=admissions.findById(admissionIds.get(text(p,"key"))).orElseThrow();
            if (!ownerIds.isEmpty()) assertEquals(ownerIds.get(text(p,"ownerKey")),adm.getOwnerId());
            assertEquals(date(p,"submittedDate"),adm.getSubmittedAt().toLocalDate());
            if (adm.getHorseId()!=null) {
                assertEquals(adm.getOwnerId(),horses.findById(adm.getHorseId()).orElseThrow().getOwnerId());
                assertEquals(text(p,"stallCode"),stalls.findById(horses.findById(adm.getHorseId()).orElseThrow().getCurrentStallId()).orElseThrow().getStallCode());
            }
        }
        int ownerChecks=verifyOwnerScopes(mvc);
        Map<String,Object> report=new LinkedHashMap<>();
        report.put("status",cloudMode()?"PASS_CLOUD_SEED":"PASS_LOCAL_SEED");report.put("database",cloudMode()?"Neon (reviewed target)":"localhost:5433/"+System.getenv().getOrDefault("RTMS_DEMO_SEED_DATABASE","rtms_db"));
        report.put("storage",cloudMode()?"Cloudinary authenticated assets":"local disk, no Cloudinary/Neon writes");report.put("horseCount",48);report.put("admissionCount",54);
        report.put("staffCount",30);report.put("filesVerified",checkedFiles);report.put("photoHttpChecks",48);report.put("admissionActorHttpChecks",5);
        report.put("staffIds",staffIds);report.put("admissionIds",admissionIds);report.put("ownerIds",ownerIds);
        report.put("ownerCount",ownerIds.isEmpty()?1:8); report.put("ownerScopeHttpChecks",ownerChecks);
        report.put("admissionStatuses",jdbc.queryForList("SELECT status,count(*) AS total FROM admission_applications GROUP BY status ORDER BY status"));
        if (!org.springframework.transaction.support.TransactionSynchronizationManager.isActualTransactionActive()) {
            Path output=Path.of(cloudMode()?"target/cloud-seed/result.json":"target/demo-seed/result.json");Files.createDirectories(output.getParent());
            json.writerWithDefaultPrettyPrinter().writeValue(output.toFile(),report);
            json.writerWithDefaultPrettyPrinter().writeValue(resultPath().toFile(),report);
        }
        System.out.println("SEED VERIFIED: 48 Horses, 54 Admissions, 30 Staff, 324 files, 48 photo HTTP checks");
    }

    private int verifyOwnerScopes(MockMvc mvc) throws Exception {
        if (ownerIds.isEmpty()) return 0;
        int checks=0;
        for (JsonNode owner:data.get("owners")) {
            Long id=ownerIds.get(text(owner,"key")); assertNotNull(id);
            User actor=users.findById(id).orElseThrow();
            String access=jwt.generateToken(id,actor.getEmail(),actor.getRole().getName());
            var mine=jdbc.queryForList("SELECT id,horse_id FROM admission_applications WHERE owner_id=? ORDER BY id",id);
            var other=jdbc.queryForMap("SELECT id,horse_id FROM admission_applications WHERE owner_id<>? AND horse_id IS NOT NULL ORDER BY id LIMIT 1",id);
            assertEquals(owner.get("admissionCount").asInt(),mine.size());
            JsonNode list=json.readTree(mvc.perform(get("/api/owner/admissions").header("Authorization","Bearer "+access)).andExpect(status().isOk()).andReturn().getResponse().getContentAsByteArray()).get("data");
            assertEquals(mine.size(),list.size());
            Set<Long> expectedIds=new HashSet<>(); mine.forEach(a->expectedIds.add(((Number)a.get("id")).longValue()));
            for (JsonNode item:list) assertTrue(expectedIds.contains(item.get("admissionId").asLong()));
            JsonNode horseList=json.readTree(mvc.perform(get("/api/horses?mine=true").header("Authorization","Bearer "+access)).andExpect(status().isOk()).andReturn().getResponse().getContentAsByteArray()).get("data");
            assertEquals(owner.get("horseCount").asInt(),horseList.size());
            for (JsonNode horse:horseList) assertEquals(id,horse.get("ownerId").asLong());
            Long first=((Number)mine.getFirst().get("id")).longValue();
            Long foreign=((Number)other.get("id")).longValue(), foreignHorse=((Number)other.get("horse_id")).longValue();
            mvc.perform(get("/api/owner/admissions/"+first).header("Authorization","Bearer "+access)).andExpect(status().isOk());
            boolean outsideTransaction=!org.springframework.transaction.support.TransactionSynchronizationManager.isActualTransactionActive();
            if (outsideTransaction) {
                mvc.perform(get("/api/owner/admissions/"+foreign).header("Authorization","Bearer "+access)).andExpect(status().isForbidden());
                mvc.perform(get("/api/owner/admissions/"+foreign+"/documents").header("Authorization","Bearer "+access)).andExpect(status().isForbidden());
                mvc.perform(get("/api/horses/"+foreignHorse).header("Authorization","Bearer "+access)).andExpect(status().isForbidden());
                mvc.perform(get("/api/horses/"+foreignHorse+"/photo").header("Authorization","Bearer "+access)).andExpect(status().isForbidden());
            }
            assertEquals(0L,jdbc.queryForObject("SELECT count(*) FROM admission_applications a JOIN users u ON u.id=a.owner_id WHERE a.owner_id=? AND u.created_at>a.submitted_at",Long.class,id));
            checks+=outsideTransaction?7:3;
        }
        return checks;
    }

    private <T> T tx(Supplier<T> action) { return new TransactionTemplate(transactions).execute(s -> action.get()); }
    private Path resultPath() { return cloudMode()?Path.of("target/cloud-seed/result.json"):Path.of("target/demo-seed/"+System.getenv().getOrDefault("RTMS_DEMO_SEED_DATABASE","rtms_db")+"-result.json"); }
    private long count(String table) { return jdbc.queryForObject("SELECT count(*) FROM "+table,Long.class); }
    private void at(LocalDate date,int hour,int minute) { now.set(date.atTime(hour,minute)); }
    private static String text(JsonNode n,String key) { return n.hasNonNull(key)?n.get(key).asText():null; }
    private static LocalDate date(JsonNode n,String key) { String value=text(n,key);return value==null?null:LocalDate.parse(value); }
    private List<JsonNode> profiles() { List<JsonNode> list=new ArrayList<>();data.get("horses").forEach(list::add);data.get("additionalAdmissions").forEach(list::add);return list; }
    private static List<String> csv(String line) {
        List<String> result=new ArrayList<>();StringBuilder field=new StringBuilder();boolean quoted=false;
        for(int i=0;i<line.length();i++) {
            char c=line.charAt(i);
            if(c=='"') { if(quoted && i+1<line.length() && line.charAt(i+1)=='"') {field.append('"');i++;} else quoted=!quoted; }
            else if(c==','&&!quoted) {result.add(field.toString());field.setLength(0);} else field.append(c);
        }
        result.add(field.toString());return result;
    }
}
