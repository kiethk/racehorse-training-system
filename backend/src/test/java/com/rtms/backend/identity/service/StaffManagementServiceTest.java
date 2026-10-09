package com.rtms.backend.identity.service;
import com.rtms.backend.identity.entity.GroomProfile;
import com.rtms.backend.identity.entity.Role;
import com.rtms.backend.identity.entity.User;
import com.rtms.backend.identity.entity.VeterinarianProfile;
import com.rtms.backend.identity.repository.GroomProfileRepository;
import com.rtms.backend.identity.repository.RoleRepository;
import com.rtms.backend.identity.repository.TrainerProfileRepository;
import com.rtms.backend.identity.repository.UserRepository;
import com.rtms.backend.identity.repository.VeterinarianProfileRepository;
import com.rtms.backend.stable.entity.Area;
import com.rtms.backend.stable.entity.StableStall;
import com.rtms.backend.stable.repository.AreaRepository;
import com.rtms.backend.stable.repository.StableStallRepository;


import com.rtms.backend.identity.dto.StaffCreationRequest;
import com.rtms.backend.identity.dto.StaffCreationResponse;
import com.rtms.backend.identity.dto.StaffSummaryResponse;
import com.rtms.backend.identity.dto.StaffUpdateRequest;
import com.rtms.backend.stable.enums.AreaType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class StaffManagementServiceTest {

    private UserRepository userRepository;
    private RoleRepository roleRepository;
    private PasswordEncoder passwordEncoder;
    private GroomProfileRepository groomProfileRepository;
    private VeterinarianProfileRepository vetProfileRepository;
    private TrainerProfileRepository trainerProfileRepository;
    private AreaRepository areaRepository;
    private StableStallRepository stableStallRepository;
    private StaffManagementService staffService;

    @BeforeEach
    void setUp() {
        userRepository = mock(UserRepository.class);
        roleRepository = mock(RoleRepository.class);
        passwordEncoder = mock(PasswordEncoder.class);
        groomProfileRepository = mock(GroomProfileRepository.class);
        vetProfileRepository = mock(VeterinarianProfileRepository.class);
        trainerProfileRepository = mock(TrainerProfileRepository.class);
        areaRepository = mock(AreaRepository.class);
        stableStallRepository = mock(StableStallRepository.class);

        staffService = new StaffManagementService(
                userRepository, roleRepository, passwordEncoder,
                groomProfileRepository, vetProfileRepository, trainerProfileRepository,
                areaRepository, stableStallRepository
        );
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private User savedUser(long id, String role) {
        Role r = new Role(); r.setName(role);
        User u = new User(); u.setId(id); u.setRole(r); u.setActive(true);
        u.setFullName("Test " + role); u.setEmail("test+" + id + "@rtms.com");
        return u;
    }

    private Area area(long id, String code) {
        Area a = new Area(); a.setId(id); a.setCode(code); a.setType(AreaType.REGULAR); a.setTrainerId(null);
        return a;
    }

    private Area quarantineArea(long id) {
        Area a = new Area(); a.setId(id); a.setCode("Q"); a.setType(AreaType.QUARANTINE); a.setTrainerId(null);
        return a;
    }

    private StableStall stall(long id, long areaId, int stallNumber, Long groomId) {
        StableStall s = new StableStall();
        s.setId(id); s.setAreaId(areaId); s.setStallNumber(stallNumber);
        s.setStallCode("A" + stallNumber); s.setGroomId(groomId);
        return s;
    }

    private void mockUserSaveWithId(long id) {
        when(userRepository.save(any(User.class))).thenAnswer(inv -> {
            User u = inv.getArgument(0); u.setId(id); return u;
        });
    }

    private void mockBasicCreation(String roleName) {
        Role r = new Role(); r.setName(roleName);
        when(userRepository.findByEmail(anyString())).thenReturn(Optional.empty());
        when(roleRepository.findByName(roleName)).thenReturn(Optional.of(r));
        when(passwordEncoder.encode(anyString())).thenReturn("encoded");
    }

    // ── Trainer creation ──────────────────────────────────────────────────────

    @Test
    void testCreateTrainer_TwoFreeAreas_AssignsBoth() {
        mockBasicCreation("HEAD_TRAINER");
        mockUserSaveWithId(10L);

        List<Area> freeAreas = List.of(area(1, "A"), area(2, "B"));
        when(areaRepository.findUnassignedRegularAreasOrdered()).thenReturn(new ArrayList<>(freeAreas));
        when(areaRepository.save(any(Area.class))).thenAnswer(inv -> inv.getArgument(0));

        StaffCreationRequest req = new StaffCreationRequest();
        req.setFullName("Tom"); req.setEmail("tom@rtms.com"); req.setPassword("pass");
        req.setRole("HEAD_TRAINER"); req.setCertificationNumber("CERT-001");

        StaffCreationResponse res = staffService.createStaff(req);

        assertEquals(2, res.getAssignedAreaIds().size());
        assertEquals(List.of("A", "B"), res.getAssignedAreaCodes());
        verify(areaRepository, times(2)).save(any(Area.class));
    }

    @Test
    void testCreateTrainer_OneFreeArea_AssignsOne() {
        mockBasicCreation("HEAD_TRAINER");
        mockUserSaveWithId(10L);

        when(areaRepository.findUnassignedRegularAreasOrdered()).thenReturn(new ArrayList<>(List.of(area(1, "A"))));
        when(areaRepository.save(any(Area.class))).thenAnswer(inv -> inv.getArgument(0));

        StaffCreationRequest req = new StaffCreationRequest();
        req.setFullName("Tom"); req.setEmail("tom@rtms.com"); req.setPassword("pass");
        req.setRole("HEAD_TRAINER"); req.setCertificationNumber("CERT-001");

        StaffCreationResponse res = staffService.createStaff(req);

        assertEquals(1, res.getAssignedAreaIds().size());
    }

    @Test
    void testCreateTrainer_NoFreeArea_StillCreated() {
        mockBasicCreation("HEAD_TRAINER");
        mockUserSaveWithId(10L);

        when(areaRepository.findUnassignedRegularAreasOrdered()).thenReturn(new ArrayList<>());
        when(areaRepository.save(any(Area.class))).thenAnswer(inv -> inv.getArgument(0));

        StaffCreationRequest req = new StaffCreationRequest();
        req.setFullName("Tom"); req.setEmail("tom@rtms.com"); req.setPassword("pass");
        req.setRole("HEAD_TRAINER"); req.setCertificationNumber("CERT-001");

        StaffCreationResponse res = staffService.createStaff(req);

        assertEquals("HEAD_TRAINER", res.getRole());
        assertTrue(res.getAssignedAreaIds().isEmpty());
        verify(areaRepository, never()).save(any(Area.class));
    }

    @Test
    void testCreateTrainer_NeverExceedsTwoAreas() {
        // Even if 4 areas are free, only 2 should be assigned
        mockBasicCreation("HEAD_TRAINER");
        mockUserSaveWithId(10L);

        List<Area> freeAreas = List.of(area(1, "A"), area(2, "B"), area(3, "C"), area(4, "D"));
        when(areaRepository.findUnassignedRegularAreasOrdered()).thenReturn(new ArrayList<>(freeAreas));
        when(areaRepository.save(any(Area.class))).thenAnswer(inv -> inv.getArgument(0));

        StaffCreationRequest req = new StaffCreationRequest();
        req.setFullName("Tom"); req.setEmail("tom@rtms.com"); req.setPassword("pass");
        req.setRole("HEAD_TRAINER"); req.setCertificationNumber("CERT-001");

        StaffCreationResponse res = staffService.createStaff(req);

        assertEquals(StaffManagementService.MAX_AREAS_PER_TRAINER, res.getAssignedAreaIds().size());
    }

    @Test
    void testCreateTrainer_QuarantineAreaNotIncluded() {
        // findUnassignedRegularAreasOrdered returns REGULAR only (JPQL filters by type='REGULAR')
        // So quarantine never appears — confirmed by the query in AreaRepository.
        // This test verifies the service never calls findByType(QUARANTINE) or similar.
        mockBasicCreation("HEAD_TRAINER");
        mockUserSaveWithId(10L);

        when(areaRepository.findUnassignedRegularAreasOrdered()).thenReturn(new ArrayList<>());
        when(areaRepository.save(any(Area.class))).thenAnswer(inv -> inv.getArgument(0));

        StaffCreationRequest req = new StaffCreationRequest();
        req.setFullName("Tom"); req.setEmail("tom@rtms.com"); req.setPassword("pass");
        req.setRole("HEAD_TRAINER"); req.setCertificationNumber("CERT-001");

        staffService.createStaff(req);

        verify(areaRepository, never()).findByType(AreaType.QUARANTINE);
        verify(areaRepository, never()).save(argThat(a -> AreaType.QUARANTINE.equals(a.getType())));
    }

    // ── Groom creation ────────────────────────────────────────────────────────

    @Test
    void testCreateGroom_AutoAssignment_BalancedLoad() {
        mockBasicCreation("GROOM");
        mockUserSaveWithId(20L);

        User trainerA = savedUser(5L, "HEAD_TRAINER");
        User trainerB = savedUser(6L, "HEAD_TRAINER");
        
        when(userRepository.findActiveHeadTrainers()).thenReturn(List.of(trainerA, trainerB));
        
        Area areaA = area(1L, "A"); areaA.setTrainerId(5L);
        when(areaRepository.findByTypeAndTrainerId(AreaType.REGULAR, 5L)).thenReturn(List.of(areaA));
        
        Area areaB = area(2L, "B"); areaB.setTrainerId(6L);
        when(areaRepository.findByTypeAndTrainerId(AreaType.REGULAR, 6L)).thenReturn(List.of(areaB));

        // Both have 1 free block (1-3)
        List<StableStall> blockA = List.of(stall(101, 1, 1, null), stall(102, 1, 2, null), stall(103, 1, 3, null));
        when(stableStallRepository.findByAreaIdAndStallNumberBetweenOrderByStallNumberAsc(1L, 1, 3)).thenReturn(blockA);
        List<StableStall> blockB = List.of(stall(201, 2, 1, null), stall(202, 2, 2, null), stall(203, 2, 3, null));
        when(stableStallRepository.findByAreaIdAndStallNumberBetweenOrderByStallNumberAsc(2L, 1, 3)).thenReturn(blockB);
        
        when(stableStallRepository.findByAreaIdAndStallNumberBetweenOrderByStallNumberAsc(anyLong(), argThat(start -> start != 1), anyInt())).thenReturn(List.of());

        // Trainer A has 5 grooms, Trainer B has 2
        when(groomProfileRepository.countByTrainerId(5L)).thenReturn(5L);
        when(groomProfileRepository.countByTrainerId(6L)).thenReturn(2L);

        // Locked re-read for area 2 (Trainer B wins), block 1-3
        when(stableStallRepository.findBlockForUpdate(2L, 1, 3)).thenReturn(blockB);
        when(stableStallRepository.save(any(StableStall.class))).thenAnswer(inv -> inv.getArgument(0));

        StaffCreationRequest req = new StaffCreationRequest();
        req.setFullName("G"); req.setEmail("g@rtms.com"); req.setPassword("pass");
        req.setRole("GROOM");

        StaffCreationResponse res = staffService.createStaff(req);

        // Should pick Trainer B because 2 < 5
        assertEquals(6L, res.getTrainerId());
        assertEquals("ASSIGNED", res.getAssignmentStatus());
        assertFalse(res.getNoStallBlockAvailable());
        assertEquals(3, res.getAssignedStallIds().size());
        assertEquals(2L, res.getAssignedAreaId());
    }

    @Test
    void testCreateGroom_AutoAssignment_TieOnGroomCount() {
        mockBasicCreation("GROOM");
        mockUserSaveWithId(20L);

        User trainerA = savedUser(5L, "HEAD_TRAINER");
        User trainerB = savedUser(6L, "HEAD_TRAINER");
        
        when(userRepository.findActiveHeadTrainers()).thenReturn(List.of(trainerA, trainerB));
        
        Area areaA = area(1L, "A"); areaA.setTrainerId(5L);
        when(areaRepository.findByTypeAndTrainerId(AreaType.REGULAR, 5L)).thenReturn(List.of(areaA));
        
        Area areaB = area(2L, "B"); areaB.setTrainerId(6L);
        when(areaRepository.findByTypeAndTrainerId(AreaType.REGULAR, 6L)).thenReturn(List.of(areaB));

        // Both have same groom count
        when(groomProfileRepository.countByTrainerId(5L)).thenReturn(3L);
        when(groomProfileRepository.countByTrainerId(6L)).thenReturn(3L);

        // Trainer A has 1 free block (1-3)
        List<StableStall> blockA1 = List.of(stall(101, 1, 1, null), stall(102, 1, 2, null), stall(103, 1, 3, null));
        when(stableStallRepository.findByAreaIdAndStallNumberBetweenOrderByStallNumberAsc(1L, 1, 3)).thenReturn(blockA1);
        when(stableStallRepository.findByAreaIdAndStallNumberBetweenOrderByStallNumberAsc(eq(1L), argThat(start -> start != 1), anyInt())).thenReturn(List.of());

        // Trainer B has 2 free blocks (1-3 and 4-6)
        List<StableStall> blockB1 = List.of(stall(201, 2, 1, null), stall(202, 2, 2, null), stall(203, 2, 3, null));
        when(stableStallRepository.findByAreaIdAndStallNumberBetweenOrderByStallNumberAsc(2L, 1, 3)).thenReturn(blockB1);
        List<StableStall> blockB2 = List.of(stall(204, 2, 4, null), stall(205, 2, 5, null), stall(206, 2, 6, null));
        when(stableStallRepository.findByAreaIdAndStallNumberBetweenOrderByStallNumberAsc(2L, 4, 6)).thenReturn(blockB2);
        when(stableStallRepository.findByAreaIdAndStallNumberBetweenOrderByStallNumberAsc(eq(2L), argThat(start -> start != 1 && start != 4), anyInt())).thenReturn(List.of());

        // Locked re-read for area 2 (Trainer B wins), block 1-3
        when(stableStallRepository.findBlockForUpdate(2L, 1, 3)).thenReturn(blockB1);
        when(stableStallRepository.save(any(StableStall.class))).thenAnswer(inv -> inv.getArgument(0));

        StaffCreationRequest req = new StaffCreationRequest();
        req.setFullName("G"); req.setEmail("g@rtms.com"); req.setPassword("pass");
        req.setRole("GROOM");

        StaffCreationResponse res = staffService.createStaff(req);

        // Should pick Trainer B because more free blocks (2 > 1)
        assertEquals(6L, res.getTrainerId());
        assertEquals("ASSIGNED", res.getAssignmentStatus());
    }

    @Test
    void testCreateGroom_AutoAssignment_NoCapacity() {
        mockBasicCreation("GROOM");
        mockUserSaveWithId(20L);

        User trainerA = savedUser(5L, "HEAD_TRAINER");
        when(userRepository.findActiveHeadTrainers()).thenReturn(List.of(trainerA));
        
        Area areaA = area(1L, "A"); areaA.setTrainerId(5L);
        when(areaRepository.findByTypeAndTrainerId(AreaType.REGULAR, 5L)).thenReturn(List.of(areaA));

        // No free blocks
        when(stableStallRepository.findByAreaIdAndStallNumberBetweenOrderByStallNumberAsc(anyLong(), anyInt(), anyInt())).thenReturn(List.of());

        StaffCreationRequest req = new StaffCreationRequest();
        req.setFullName("G"); req.setEmail("g@rtms.com"); req.setPassword("pass");
        req.setRole("GROOM");

        StaffCreationResponse res = staffService.createStaff(req);

        // Groom still created
        assertNull(res.getTrainerId());
        assertEquals("UNASSIGNED", res.getAssignmentStatus());
        assertTrue(res.getNoStallBlockAvailable());
        assertTrue(res.getAssignedStallIds().isEmpty());
    }

    @Test
    void testCreateGroom_AutoAssignment_FullTieSmallestId() {
        mockBasicCreation("GROOM");
        mockUserSaveWithId(20L);

        User trainerA = savedUser(6L, "HEAD_TRAINER"); // Larger ID first in list to prove order doesn't dictate solely
        User trainerB = savedUser(5L, "HEAD_TRAINER"); // Smaller ID
        
        when(userRepository.findActiveHeadTrainers()).thenReturn(List.of(trainerA, trainerB));
        
        Area areaA = area(1L, "A"); areaA.setTrainerId(6L);
        when(areaRepository.findByTypeAndTrainerId(AreaType.REGULAR, 6L)).thenReturn(List.of(areaA));
        Area areaB = area(2L, "B"); areaB.setTrainerId(5L);
        when(areaRepository.findByTypeAndTrainerId(AreaType.REGULAR, 5L)).thenReturn(List.of(areaB));

        when(groomProfileRepository.countByTrainerId(anyLong())).thenReturn(2L); // Tied on groom count

        // Tied on free blocks (both have 1)
        List<StableStall> blockA = List.of(stall(101, 1, 1, null), stall(102, 1, 2, null), stall(103, 1, 3, null));
        when(stableStallRepository.findByAreaIdAndStallNumberBetweenOrderByStallNumberAsc(1L, 1, 3)).thenReturn(blockA);
        List<StableStall> blockB = List.of(stall(201, 2, 1, null), stall(202, 2, 2, null), stall(203, 2, 3, null));
        when(stableStallRepository.findByAreaIdAndStallNumberBetweenOrderByStallNumberAsc(2L, 1, 3)).thenReturn(blockB);
        when(stableStallRepository.findByAreaIdAndStallNumberBetweenOrderByStallNumberAsc(anyLong(), argThat(start -> start != 1), anyInt())).thenReturn(List.of());

        // Locked re-read for area 2 (Trainer B = 5L wins — smallest ID), block 1-3
        when(stableStallRepository.findBlockForUpdate(2L, 1, 3)).thenReturn(blockB);
        when(stableStallRepository.save(any(StableStall.class))).thenAnswer(inv -> inv.getArgument(0));

        StaffCreationRequest req = new StaffCreationRequest();
        req.setFullName("G"); req.setEmail("g@rtms.com"); req.setPassword("pass");
        req.setRole("GROOM");

        StaffCreationResponse res = staffService.createStaff(req);

        // Pick smaller ID (trainerB = 5L)
        assertEquals(5L, res.getTrainerId());
    }


    // ── Existing tests (kept) ─────────────────────────────────────────────────

    @Test
    void testGetAllStaff_FiltersNonStaff() {
        Role groomRole = new Role(); groomRole.setName("GROOM");
        Role managerRole = new Role(); managerRole.setName("CLUB_MANAGER");

        User groom = new User(); groom.setId(1L); groom.setRole(groomRole); groom.setActive(true);
        User manager = new User(); manager.setId(2L); manager.setRole(managerRole); manager.setActive(true);

        when(userRepository.findAll()).thenReturn(List.of(groom, manager));
        when(groomProfileRepository.findById(1L)).thenReturn(Optional.empty());

        List<StaffSummaryResponse> staff = staffService.getAllStaff();
        assertEquals(1, staff.size());
        assertEquals(1L, staff.get(0).getUserId());
    }

    @Test
    void testCreateVet_WithProfile() {
        mockBasicCreation("VETERINARIAN");
        mockUserSaveWithId(1L);

        StaffCreationRequest request = new StaffCreationRequest();
        request.setFullName("Jane Vet"); request.setEmail("jane@rtms.com");
        request.setPassword("pass"); request.setRole("VETERINARIAN");
        request.setLicenseNumber("VET123");

        StaffCreationResponse response = staffService.createStaff(request);

        assertNotNull(response);
        verify(vetProfileRepository, times(1)).save(any(VeterinarianProfile.class));
    }

    @Test
    void testDuplicateEmailRejected() {
        StaffCreationRequest request = new StaffCreationRequest();
        request.setEmail("dup@rtms.com"); request.setRole("GROOM");

        when(userRepository.findByEmail(anyString())).thenReturn(Optional.of(new User()));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () -> staffService.createStaff(request));
        assertEquals(HttpStatus.CONFLICT, ex.getStatusCode());
    }

    @Test
    void testInvalidRoleRejected() {
        StaffCreationRequest request = new StaffCreationRequest();
        request.setEmail("test@rtms.com"); request.setRole("CLUB_MANAGER");

        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () -> staffService.createStaff(request));
        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
    }

    @Test
    void testActivateDeactivateWorks() {
        Role groomRole = new Role(); groomRole.setName("GROOM");
        User groom = new User(); groom.setId(1L); groom.setRole(groomRole); groom.setActive(true);

        when(userRepository.findById(1L)).thenReturn(Optional.of(groom));
        when(userRepository.save(any(User.class))).thenAnswer(i -> i.getArgument(0));

        StaffSummaryResponse response = staffService.updateStaffStatus(1L, false);
        assertFalse(response.isActive());
        verify(userRepository, times(1)).save(any(User.class));
    }

    @Test
    void testNonStaffAccountCannotBeChanged() {
        Role ownerRole = new Role(); ownerRole.setName("HORSE_OWNER");
        User owner = new User(); owner.setId(1L); owner.setRole(ownerRole); owner.setActive(true);

        when(userRepository.findById(1L)).thenReturn(Optional.of(owner));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () -> staffService.updateStaffStatus(1L, false));
        assertEquals(HttpStatus.FORBIDDEN, ex.getStatusCode());
    }

    // ── Groom trainer reassignment ──────────────────────────────────────

    /** Groom owns stalls belonging to old Trainer’s area → reassignment to a different Trainer is rejected. */
    @Test
    void testUpdateGroomTrainer_WithStalls_RejectsIfAreaMismatch() {
        Role groomRole = new Role(); groomRole.setName("GROOM");
        User groom = new User(); groom.setId(20L); groom.setRole(groomRole); groom.setActive(true);
        groom.setFullName("Groom G"); groom.setEmail("g@rtms.com");

        User newTrainer = savedUser(6L, "HEAD_TRAINER"); // different trainer

        when(userRepository.findById(20L)).thenReturn(Optional.of(groom));
        when(userRepository.findById(6L)).thenReturn(Optional.of(newTrainer));
        when(userRepository.save(any(User.class))).thenAnswer(i -> i.getArgument(0));

        GroomProfile groomProfile = new GroomProfile();
        groomProfile.setUserId(20L);
        groomProfile.setTrainerId(5L); // currently assigned to trainer 5
        when(groomProfileRepository.findById(20L)).thenReturn(Optional.of(groomProfile));

        // Groom owns stalls 1-3 in area 1; area 1 belongs to trainer 5, NOT trainer 6
        StableStall s1 = stall(101, 1, 1, 20L);
        StableStall s2 = stall(102, 1, 2, 20L);
        StableStall s3 = stall(103, 1, 3, 20L);
        when(stableStallRepository.findByGroomId(20L)).thenReturn(List.of(s1, s2, s3));

        Area area1 = area(1L, "A"); area1.setTrainerId(5L); // owned by trainer 5
        when(areaRepository.findById(1L)).thenReturn(Optional.of(area1));

        StaffUpdateRequest req = new StaffUpdateRequest();
        req.setTrainerId(6L); // trying to move to trainer 6

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> staffService.updateStaff(20L, req));
        assertEquals(HttpStatus.CONFLICT, ex.getStatusCode());
        assertTrue(ex.getReason().contains("Unassign the stalls first"));
        verify(groomProfileRepository, never()).save(any(GroomProfile.class));
    }

    /** Groom owns NO stalls → trainer reassignment is allowed freely. */
    @Test
    void testUpdateGroomTrainer_NoStalls_Allowed() {
        Role groomRole = new Role(); groomRole.setName("GROOM");
        User groom = new User(); groom.setId(20L); groom.setRole(groomRole); groom.setActive(true);
        groom.setFullName("Groom G"); groom.setEmail("g@rtms.com");

        User newTrainer = savedUser(6L, "HEAD_TRAINER");

        when(userRepository.findById(20L)).thenReturn(Optional.of(groom));
        when(userRepository.findById(6L)).thenReturn(Optional.of(newTrainer));
        when(userRepository.save(any(User.class))).thenAnswer(i -> i.getArgument(0));

        GroomProfile groomProfile = new GroomProfile();
        groomProfile.setUserId(20L);
        groomProfile.setTrainerId(5L);
        when(groomProfileRepository.findById(20L)).thenReturn(Optional.of(groomProfile));

        // No stalls owned
        when(stableStallRepository.findByGroomId(20L)).thenReturn(List.of());

        // getStaffDetail called at end of updateStaff
        when(groomProfileRepository.findById(20L)).thenReturn(Optional.of(groomProfile));

        StaffUpdateRequest req = new StaffUpdateRequest();
        req.setTrainerId(6L);

        // Should not throw
        assertDoesNotThrow(() -> staffService.updateStaff(20L, req));
        verify(groomProfileRepository, atLeastOnce()).save(any(GroomProfile.class));
    }

    /** Groom owns stalls + clear Trainer (trainerId = null) → rejected. */
    @Test
    void testUpdateGroomTrainer_WithStalls_RejectsClearTrainer() {
        Role groomRole = new Role(); groomRole.setName("GROOM");
        User groom = new User(); groom.setId(20L); groom.setRole(groomRole); groom.setActive(true);
        groom.setFullName("Groom G"); groom.setEmail("g@rtms.com");

        when(userRepository.findById(20L)).thenReturn(Optional.of(groom));

        GroomProfile groomProfile = new GroomProfile();
        groomProfile.setUserId(20L);
        groomProfile.setTrainerId(5L); // currently assigned to trainer 5
        when(groomProfileRepository.findById(20L)).thenReturn(Optional.of(groomProfile));

        // Groom owns stalls
        StableStall s1 = stall(101, 1, 1, 20L);
        when(stableStallRepository.findByGroomId(20L)).thenReturn(List.of(s1));

        StaffUpdateRequest req = new StaffUpdateRequest();
        req.setTrainerIdProvided(true); // implies they explicitly passed trainerId: null
        req.setTrainerId(null);

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> staffService.updateStaff(20L, req));
        assertEquals(HttpStatus.CONFLICT, ex.getStatusCode());
        assertTrue(ex.getReason().contains("Cannot remove Trainer"));
        verify(groomProfileRepository, never()).save(any(GroomProfile.class));
    }

    /** Groom owns NO stalls + clear Trainer (trainerId = null) → allowed. */
    @Test
    void testUpdateGroomTrainer_NoStalls_AllowsClearTrainer() {
        Role groomRole = new Role(); groomRole.setName("GROOM");
        User groom = new User(); groom.setId(20L); groom.setRole(groomRole); groom.setActive(true);
        groom.setFullName("Groom G"); groom.setEmail("g@rtms.com");

        when(userRepository.findById(20L)).thenReturn(Optional.of(groom));
        when(userRepository.save(any(User.class))).thenAnswer(i -> i.getArgument(0));

        GroomProfile groomProfile = new GroomProfile();
        groomProfile.setUserId(20L);
        groomProfile.setTrainerId(5L); // currently assigned to trainer 5
        when(groomProfileRepository.findById(20L)).thenReturn(Optional.of(groomProfile));

        // Groom owns no stalls
        when(stableStallRepository.findByGroomId(20L)).thenReturn(List.of());
        
        when(groomProfileRepository.findById(20L)).thenReturn(Optional.of(groomProfile));

        StaffUpdateRequest req = new StaffUpdateRequest();
        req.setTrainerIdProvided(true); // implies they explicitly passed trainerId: null
        req.setTrainerId(null);

        assertDoesNotThrow(() -> staffService.updateStaff(20L, req));
        
        // Assert trainer is cleared
        assertNull(groomProfile.getTrainerId());
        verify(groomProfileRepository, atLeastOnce()).save(any(GroomProfile.class));
    }



    // ── Concurrency guard tests ───────────────────────────────────────

    /**
     * After the candidate block is chosen, findBlockForUpdate returns a stall already owned
     * by another Groom (concurrent assignment). Service must throw CONFLICT without persisting.
     */
    @Test
    void testCreateGroom_LockedBlock_ConcurrentlyTaken_ThrowsConflict() {
        mockBasicCreation("GROOM");
        mockUserSaveWithId(20L);

        User trainer = savedUser(5L, "HEAD_TRAINER");
        when(userRepository.findActiveHeadTrainers()).thenReturn(List.of(trainer));

        Area a = area(1L, "A"); a.setTrainerId(5L);
        when(areaRepository.findByTypeAndTrainerId(AreaType.REGULAR, 5L)).thenReturn(List.of(a));
        when(groomProfileRepository.countByTrainerId(5L)).thenReturn(0L);

        // Non-locking scan sees the block as free
        List<StableStall> freeBlock = List.of(
                stall(101, 1, 1, null),
                stall(102, 1, 2, null),
                stall(103, 1, 3, null));
        when(stableStallRepository.findByAreaIdAndStallNumberBetweenOrderByStallNumberAsc(1L, 1, 3))
                .thenReturn(freeBlock);
        when(stableStallRepository.findByAreaIdAndStallNumberBetweenOrderByStallNumberAsc(anyLong(), argThat(s -> s != 1), anyInt()))
                .thenReturn(List.of());

        // Locked re-read shows stall 101 already assigned to another groom (concurrent winner)
        List<StableStall> takenBlock = List.of(
                stall(101, 1, 1, 99L),   // taken!
                stall(102, 1, 2, null),
                stall(103, 1, 3, null));
        when(stableStallRepository.findBlockForUpdate(1L, 1, 3)).thenReturn(takenBlock);

        StaffCreationRequest req = new StaffCreationRequest();
        req.setFullName("G"); req.setEmail("g@rtms.com"); req.setPassword("pass");
        req.setRole("GROOM");

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> staffService.createStaff(req));
        assertEquals(HttpStatus.CONFLICT, ex.getStatusCode());
        verify(stableStallRepository, never()).save(any(StableStall.class));
    }

    /** Normal auto-assignment assigns exactly 3 adjacent stalls (block 1-3). */
    @Test
    void testCreateGroom_NormalAssignment_ExactlyThreeAdjacentStalls() {
        mockBasicCreation("GROOM");
        mockUserSaveWithId(20L);

        User trainer = savedUser(5L, "HEAD_TRAINER");
        when(userRepository.findActiveHeadTrainers()).thenReturn(List.of(trainer));

        Area a = area(1L, "A"); a.setTrainerId(5L);
        when(areaRepository.findByTypeAndTrainerId(AreaType.REGULAR, 5L)).thenReturn(List.of(a));
        when(groomProfileRepository.countByTrainerId(5L)).thenReturn(0L);

        List<StableStall> freeBlock = List.of(
                stall(101, 1, 1, null),
                stall(102, 1, 2, null),
                stall(103, 1, 3, null));
        when(stableStallRepository.findByAreaIdAndStallNumberBetweenOrderByStallNumberAsc(1L, 1, 3))
                .thenReturn(freeBlock);
        when(stableStallRepository.findByAreaIdAndStallNumberBetweenOrderByStallNumberAsc(anyLong(), argThat(s -> s != 1), anyInt()))
                .thenReturn(List.of());

        // Locked re-read returns the same free block
        when(stableStallRepository.findBlockForUpdate(1L, 1, 3)).thenReturn(freeBlock);
        when(stableStallRepository.save(any(StableStall.class))).thenAnswer(inv -> inv.getArgument(0));

        StaffCreationRequest req = new StaffCreationRequest();
        req.setFullName("G"); req.setEmail("g@rtms.com"); req.setPassword("pass");
        req.setRole("GROOM");

        StaffCreationResponse res = staffService.createStaff(req);

        assertEquals("ASSIGNED", res.getAssignmentStatus());
        assertEquals(3, res.getAssignedStallIds().size());
        // Verify stall numbers are consecutive 1, 2, 3
        List<Long> ids = res.getAssignedStallIds();
        assertEquals(3, ids.size());
        assertEquals(1L, res.getAssignedAreaId());
        // All 3 stalls saved
        verify(stableStallRepository, times(3)).save(any(StableStall.class));
    }
}
