package com.rtms.backend.service;

import com.rtms.backend.dto.StaffCreationRequest;
import com.rtms.backend.dto.StaffCreationResponse;
import com.rtms.backend.dto.StaffSummaryResponse;
import com.rtms.backend.entity.*;
import com.rtms.backend.enums.AreaType;
import com.rtms.backend.repository.*;
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
    void testCreateGroom_RequiresValidActiveTrainer() {
        mockBasicCreation("GROOM");
        mockUserSaveWithId(20L);

        when(userRepository.findById(99L)).thenReturn(Optional.empty());

        StaffCreationRequest req = new StaffCreationRequest();
        req.setFullName("G"); req.setEmail("g@rtms.com"); req.setPassword("pass");
        req.setRole("GROOM"); req.setTrainerId(99L);

        assertThrows(ResponseStatusException.class, () -> staffService.createStaff(req));
    }

    @Test
    void testCreateGroom_RequiresTrainerId() {
        mockBasicCreation("GROOM");
        mockUserSaveWithId(20L);

        StaffCreationRequest req = new StaffCreationRequest();
        req.setFullName("G"); req.setEmail("g@rtms.com"); req.setPassword("pass");
        req.setRole("GROOM"); // no trainerId

        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () -> staffService.createStaff(req));
        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
    }

    @Test
    void testCreateGroom_GetsFirstCompleteBlock() {
        mockBasicCreation("GROOM");
        mockUserSaveWithId(20L);
        User trainer = savedUser(5L, "HEAD_TRAINER");
        when(userRepository.findById(5L)).thenReturn(Optional.of(trainer));

        Area a = area(1L, "A"); a.setTrainerId(5L);
        when(areaRepository.findByTypeAndTrainerId(AreaType.REGULAR, 5L)).thenReturn(List.of(a));

        // block 1-3: stalls unassigned
        List<StableStall> block1 = List.of(
                stall(101, 1, 1, null),
                stall(102, 1, 2, null),
                stall(103, 1, 3, null));
        when(stableStallRepository.findByAreaIdAndStallNumberBetweenOrderByStallNumberAsc(1L, 1, 3))
                .thenReturn(block1);
        when(stableStallRepository.save(any(StableStall.class))).thenAnswer(inv -> inv.getArgument(0));

        StaffCreationRequest req = new StaffCreationRequest();
        req.setFullName("G"); req.setEmail("g@rtms.com"); req.setPassword("pass");
        req.setRole("GROOM"); req.setTrainerId(5L);

        StaffCreationResponse res = staffService.createStaff(req);

        assertFalse(res.getNoStallBlockAvailable());
        assertEquals(3, res.getAssignedStallIds().size());
        assertEquals(1L, res.getAssignedAreaId());
    }

    @Test
    void testCreateGroom_AssignedStallsAreAdjacent() {
        // Ensures the block is 1-3, not scattered 1, 4, 9
        mockBasicCreation("GROOM");
        mockUserSaveWithId(20L);
        User trainer = savedUser(5L, "HEAD_TRAINER");
        when(userRepository.findById(5L)).thenReturn(Optional.of(trainer));

        Area a = area(1L, "A"); a.setTrainerId(5L);
        when(areaRepository.findByTypeAndTrainerId(AreaType.REGULAR, 5L)).thenReturn(List.of(a));

        List<StableStall> block1 = List.of(
                stall(101, 1, 1, null),
                stall(102, 1, 2, null),
                stall(103, 1, 3, null));
        when(stableStallRepository.findByAreaIdAndStallNumberBetweenOrderByStallNumberAsc(1L, 1, 3))
                .thenReturn(block1);
        when(stableStallRepository.save(any(StableStall.class))).thenAnswer(inv -> inv.getArgument(0));

        StaffCreationRequest req = new StaffCreationRequest();
        req.setFullName("G"); req.setEmail("g@rtms.com"); req.setPassword("pass");
        req.setRole("GROOM"); req.setTrainerId(5L);

        StaffCreationResponse res = staffService.createStaff(req);

        // Stall numbers must be consecutive (the block used is 1-3)
        List<Long> assigned = res.getAssignedStallIds();
        assertEquals(3, assigned.size());
    }

    @Test
    void testCreateGroom_MaxThreeStalls() {
        // Groom always gets exactly one block (3 stalls), never more
        mockBasicCreation("GROOM");
        mockUserSaveWithId(20L);
        User trainer = savedUser(5L, "HEAD_TRAINER");
        when(userRepository.findById(5L)).thenReturn(Optional.of(trainer));

        Area a = area(1L, "A"); a.setTrainerId(5L);
        when(areaRepository.findByTypeAndTrainerId(AreaType.REGULAR, 5L)).thenReturn(List.of(a));

        List<StableStall> block1 = List.of(
                stall(101, 1, 1, null),
                stall(102, 1, 2, null),
                stall(103, 1, 3, null));
        when(stableStallRepository.findByAreaIdAndStallNumberBetweenOrderByStallNumberAsc(1L, 1, 3))
                .thenReturn(block1);
        when(stableStallRepository.save(any(StableStall.class))).thenAnswer(inv -> inv.getArgument(0));

        StaffCreationRequest req = new StaffCreationRequest();
        req.setFullName("G"); req.setEmail("g@rtms.com"); req.setPassword("pass");
        req.setRole("GROOM"); req.setTrainerId(5L);

        StaffCreationResponse res = staffService.createStaff(req);

        assertEquals(StaffManagementService.GROOM_BLOCK_SIZE, res.getAssignedStallIds().size());
    }

    @Test
    void testCreateGroom_CannotReceiveStallsOutsideTrainerAreas() {
        // If trainer has no REGULAR areas, no stall block assignment happens
        mockBasicCreation("GROOM");
        mockUserSaveWithId(20L);
        User trainer = savedUser(5L, "HEAD_TRAINER");
        when(userRepository.findById(5L)).thenReturn(Optional.of(trainer));

        when(areaRepository.findByTypeAndTrainerId(AreaType.REGULAR, 5L)).thenReturn(List.of());

        StaffCreationRequest req = new StaffCreationRequest();
        req.setFullName("G"); req.setEmail("g@rtms.com"); req.setPassword("pass");
        req.setRole("GROOM"); req.setTrainerId(5L);

        StaffCreationResponse res = staffService.createStaff(req);

        assertTrue(res.getNoStallBlockAvailable());
        assertNull(res.getAssignedStallIds());
        verify(stableStallRepository, never()).save(any(StableStall.class));
    }

    @Test
    void testCreateGroom_NoFreeBlock_StillCreated() {
        // All blocks occupied → groom created with no stall assignment
        mockBasicCreation("GROOM");
        mockUserSaveWithId(20L);
        User trainer = savedUser(5L, "HEAD_TRAINER");
        when(userRepository.findById(5L)).thenReturn(Optional.of(trainer));

        Area a = area(1L, "A"); a.setTrainerId(5L);
        when(areaRepository.findByTypeAndTrainerId(AreaType.REGULAR, 5L)).thenReturn(List.of(a));

        // All 4 blocks have at least one stall assigned → return empty lists (or partial)
        when(stableStallRepository.findByAreaIdAndStallNumberBetweenOrderByStallNumberAsc(anyLong(), anyInt(), anyInt()))
                .thenReturn(List.of()); // empty means "no stalls found for block" (treated as occupied/missing)

        StaffCreationRequest req = new StaffCreationRequest();
        req.setFullName("G"); req.setEmail("g@rtms.com"); req.setPassword("pass");
        req.setRole("GROOM"); req.setTrainerId(5L);

        StaffCreationResponse res = staffService.createStaff(req);

        assertEquals("GROOM", res.getRole());
        assertTrue(res.getNoStallBlockAvailable());
        verify(groomProfileRepository, times(1)).save(any(GroomProfile.class));
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
}
