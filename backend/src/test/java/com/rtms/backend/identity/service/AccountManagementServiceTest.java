package com.rtms.backend.identity.service;

import com.rtms.backend.horse.entity.Horse;
import com.rtms.backend.horse.repository.HorseRepository;
import com.rtms.backend.identity.dto.*;
import com.rtms.backend.identity.entity.Role;
import com.rtms.backend.identity.entity.User;
import com.rtms.backend.identity.repository.*;
import jakarta.validation.Validator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.server.ResponseStatusException;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import com.rtms.backend.admission.enums.AdmissionStatus;
import com.rtms.backend.admission.repository.AdmissionApplicationRepository;
import com.rtms.backend.horse.enums.HorseStatus;
import com.rtms.backend.identity.entity.GroomProfile;
import com.rtms.backend.medical.enums.CareScheduleStatus;
import com.rtms.backend.medical.repository.CareScheduleRepository;
import com.rtms.backend.notification.service.NotificationService;
import com.rtms.backend.stable.entity.Area;
import com.rtms.backend.stable.entity.StableStall;
import com.rtms.backend.stable.enums.AreaType;
import com.rtms.backend.stable.repository.AreaRepository;
import com.rtms.backend.stable.repository.StableStallRepository;
import static org.mockito.Mockito.*;

class AccountManagementServiceTest {

    private UserRepository userRepository;
    private RoleRepository roleRepository;
    private HorseRepository horseRepository;
    private PasswordEncoder passwordEncoder;
    private StaffManagementService staffManagementService;
    private GroomProfileRepository groomProfileRepository;
    private VeterinarianProfileRepository vetProfileRepository;
    private TrainerProfileRepository trainerProfileRepository;
    private Validator validator;
    private AreaRepository areaRepository;
    private StableStallRepository stableStallRepository;
    private CareScheduleRepository careScheduleRepository;
    private AdmissionApplicationRepository admissionApplicationRepository;
    private NotificationService notificationService;

    private AccountManagementService accountService;

    @BeforeEach
    void setUp() {
        userRepository = mock(UserRepository.class);
        roleRepository = mock(RoleRepository.class);
        horseRepository = mock(HorseRepository.class);
        passwordEncoder = mock(PasswordEncoder.class);
        staffManagementService = mock(StaffManagementService.class);
        groomProfileRepository = mock(GroomProfileRepository.class);
        vetProfileRepository = mock(VeterinarianProfileRepository.class);
        trainerProfileRepository = mock(TrainerProfileRepository.class);
        validator = mock(Validator.class);
        areaRepository = mock(AreaRepository.class);
        stableStallRepository = mock(StableStallRepository.class);
        careScheduleRepository = mock(CareScheduleRepository.class);
        admissionApplicationRepository = mock(AdmissionApplicationRepository.class);
        notificationService = mock(NotificationService.class);

        when(validator.validate(any())).thenReturn(Collections.emptySet());

        accountService = new AccountManagementService(
                userRepository,
                roleRepository,
                horseRepository,
                passwordEncoder,
                staffManagementService,
                groomProfileRepository,
                vetProfileRepository,
                trainerProfileRepository,
                validator,
                areaRepository,
                stableStallRepository,
                careScheduleRepository,
                admissionApplicationRepository,
                notificationService
        );
    }

    @Test
    void getAllAccounts_groupsHorseCountsWithoutNPlusOne() {
        Role ownerRole = new Role();
        ownerRole.setName("HORSE_OWNER");

        User owner1 = new User();
        owner1.setId(10L);
        owner1.setFullName("John Owner");
        owner1.setEmail("owner1@test.com");
        owner1.setRole(ownerRole);

        Role adminRole = new Role();
        adminRole.setName("CLUB_MANAGER");

        User admin = new User();
        admin.setId(1L);
        admin.setFullName("Manager One");
        admin.setEmail("admin@test.com");
        admin.setRole(adminRole);

        when(userRepository.findAll()).thenReturn(List.of(owner1, admin));

        // Grouped horse count mock
        List<Object[]> grouped = new ArrayList<>();
        grouped.add(new Object[]{10L, 3L});
        when(horseRepository.countHorsesGroupedByOwner()).thenReturn(grouped);

        List<AccountSummaryResponse> accounts = accountService.getAllAccounts();

        assertEquals(2, accounts.size());
        verify(horseRepository, times(1)).countHorsesGroupedByOwner();
        verify(horseRepository, never()).findByOwnerId(anyLong());

        AccountSummaryResponse ownerSummary = accounts.stream().filter(a -> a.getUserId().equals(10L)).findFirst().orElseThrow();
        assertEquals(3L, ownerSummary.getOwnedHorsesCount());
        assertEquals("3 horses owned", ownerSummary.getProfileSummary());

        AccountSummaryResponse adminSummary = accounts.stream().filter(a -> a.getUserId().equals(1L)).findFirst().orElseThrow();
        assertEquals("Club Administrator", adminSummary.getProfileSummary());
    }

    @Test
    void getAccountDetail_forOwner_loadsOwnedHorses() {
        Role ownerRole = new Role();
        ownerRole.setName("HORSE_OWNER");

        User owner = new User();
        owner.setId(20L);
        owner.setFullName("Alice Owner");
        owner.setEmail("alice@test.com");
        owner.setRole(ownerRole);

        when(userRepository.findById(20L)).thenReturn(Optional.of(owner));

        Horse h1 = new Horse();
        h1.setId(101L);
        h1.setName("Thunder");
        h1.setRegistrationNumber("UELN001");
        h1.setStableLocation("A-01");

        when(horseRepository.findByOwnerId(20L)).thenReturn(List.of(h1));

        AccountDetailResponse detail = accountService.getAccountDetail(20L);

        assertEquals("Alice Owner", detail.getFullName());
        assertNotNull(detail.getOwnedHorses());
        assertEquals(1, detail.getOwnedHorses().size());
        assertEquals("Thunder", detail.getOwnedHorses().get(0).getName());
    }

    @Test
    void createAccount_forOwner_createsUserSuccessfully() {
        AccountCreationRequest req = new AccountCreationRequest();
        req.setFullName("Bob Owner");
        req.setEmail("bob@owner.com");
        req.setPassword("Secret123!");
        req.setRole("HORSE_OWNER");

        when(userRepository.findByEmail("bob@owner.com")).thenReturn(Optional.empty());
        Role ownerRole = new Role();
        ownerRole.setName("HORSE_OWNER");
        when(roleRepository.findByName("HORSE_OWNER")).thenReturn(Optional.of(ownerRole));
        when(passwordEncoder.encode("Secret123!")).thenReturn("hashed_pw");

        User savedUser = new User();
        savedUser.setId(30L);
        savedUser.setFullName("Bob Owner");
        savedUser.setEmail("bob@owner.com");
        savedUser.setRole(ownerRole);
        savedUser.setActive(true);

        when(userRepository.save(any(User.class))).thenReturn(savedUser);

        AccountCreationResponse response = accountService.createAccount(req);

        assertEquals(30L, response.getUserId());
        assertEquals("HORSE_OWNER", response.getRole());
        assertEquals("ASSIGNED", response.getAssignmentStatus());
        verify(passwordEncoder).encode("Secret123!");
        verify(userRepository).save(any(User.class));
    }

    @Test
    void updateAccountStatus_selfLockout_throwsBadRequest() {
        Long currentAdminId = 1L;
        Long targetUserId = 1L;

        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () ->
                accountService.updateAccountStatus(currentAdminId, targetUserId, false)
        );

        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
        assertTrue(ex.getReason().contains("You cannot deactivate your own administrator account"));
        verify(userRepository, never()).save(any());
    }

    @Test
    void updateAccountStatus_otherUser_success() {
        Long currentAdminId = 1L;
        Long targetUserId = 5L;

        User user = new User();
        user.setId(5L);
        user.setActive(true);
        Role role = new Role();
        role.setName("GROOM");
        user.setRole(role);

        when(userRepository.findById(5L)).thenReturn(Optional.of(user));
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        AccountSummaryResponse res = accountService.updateAccountStatus(currentAdminId, targetUserId, false);

        assertFalse(res.isActive());
        verify(userRepository).save(user);
    }

    @Test
    void updateAccountStatus_groomWithAssignedStalls_throwsConflict() {
        Long currentAdminId = 1L;
        Long groomId = 5L;

        User user = new User();
        user.setId(groomId);
        user.setActive(true);
        Role role = new Role();
        role.setName("GROOM");
        user.setRole(role);

        StableStall stall1 = new StableStall();
        stall1.setStallCode("A-01");
        StableStall stall2 = new StableStall();
        stall2.setStallCode("A-02");

        when(userRepository.findById(groomId)).thenReturn(Optional.of(user));
        when(stableStallRepository.findByGroomId(groomId)).thenReturn(List.of(stall1, stall2));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () ->
                accountService.updateAccountStatus(currentAdminId, groomId, false)
        );

        assertEquals(HttpStatus.CONFLICT, ex.getStatusCode());
        assertTrue(ex.getReason().contains("Cannot deactivate Groom while they are assigned to stalls: [A-01, A-02]"));
        verify(userRepository, never()).save(any());
    }

    @Test
    void updateAccountStatus_headTrainerWithAssignedAreas_throwsConflict() {
        Long currentAdminId = 1L;
        Long trainerId = 6L;

        User user = new User();
        user.setId(trainerId);
        user.setActive(true);
        Role role = new Role();
        role.setName("HEAD_TRAINER");
        user.setRole(role);

        Area area = new Area();
        area.setCode("Area A");

        when(userRepository.findById(trainerId)).thenReturn(Optional.of(user));
        when(areaRepository.findByTypeAndTrainerId(AreaType.REGULAR, trainerId)).thenReturn(List.of(area));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () ->
                accountService.updateAccountStatus(currentAdminId, trainerId, false)
        );

        assertEquals(HttpStatus.CONFLICT, ex.getStatusCode());
        assertTrue(ex.getReason().contains("Cannot deactivate Head Trainer while they are assigned to areas: [Area A]"));
        verify(userRepository, never()).save(any());
    }

    @Test
    void updateAccountStatus_headTrainerWithoutAreas_unassignsGroomsAndDeactivates() {
        Long currentAdminId = 1L;
        Long trainerId = 6L;

        User user = new User();
        user.setId(trainerId);
        user.setActive(true);
        Role role = new Role();
        role.setName("HEAD_TRAINER");
        user.setRole(role);

        GroomProfile reportingGroom = new GroomProfile();
        reportingGroom.setUserId(20L);
        reportingGroom.setTrainerId(trainerId);

        when(userRepository.findById(trainerId)).thenReturn(Optional.of(user));
        when(areaRepository.findByTypeAndTrainerId(AreaType.REGULAR, trainerId)).thenReturn(Collections.emptyList());
        when(groomProfileRepository.findByTrainerId(trainerId)).thenReturn(List.of(reportingGroom));
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        AccountSummaryResponse res = accountService.updateAccountStatus(currentAdminId, trainerId, false);

        assertFalse(res.isActive());
        assertNull(reportingGroom.getTrainerId());
        verify(groomProfileRepository).save(reportingGroom);
        verify(userRepository).save(user);
    }

    @Test
    void updateAccountStatus_horseOwnerWithPendingAdmissions_throwsConflict() {
        Long currentAdminId = 1L;
        Long ownerId = 7L;

        User user = new User();
        user.setId(ownerId);
        user.setActive(true);
        Role role = new Role();
        role.setName("HORSE_OWNER");
        user.setRole(role);

        when(userRepository.findById(ownerId)).thenReturn(Optional.of(user));
        when(admissionApplicationRepository.existsByOwnerIdAndStatusNotIn(eq(ownerId), any())).thenReturn(true);

        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () ->
                accountService.updateAccountStatus(currentAdminId, ownerId, false)
        );

        assertEquals(HttpStatus.CONFLICT, ex.getStatusCode());
        assertTrue(ex.getReason().contains("pending admission applications in progress"));
        verify(userRepository, never()).save(any());
    }

    @Test
    void updateAccountStatus_horseOwnerWithActiveHorses_throwsConflict() {
        Long currentAdminId = 1L;
        Long ownerId = 7L;

        User user = new User();
        user.setId(ownerId);
        user.setActive(true);
        Role role = new Role();
        role.setName("HORSE_OWNER");
        user.setRole(role);

        Horse horse1 = new Horse();
        horse1.setName("Thunderbolt");
        horse1.setCurrentStatus(HorseStatus.ELIGIBLE);

        when(userRepository.findById(ownerId)).thenReturn(Optional.of(user));
        when(admissionApplicationRepository.existsByOwnerIdAndStatusNotIn(eq(ownerId), any())).thenReturn(false);
        when(horseRepository.findByOwnerId(ownerId)).thenReturn(List.of(horse1));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () ->
                accountService.updateAccountStatus(currentAdminId, ownerId, false)
        );

        assertEquals(HttpStatus.CONFLICT, ex.getStatusCode());
        assertTrue(ex.getReason().contains("horses active in the facility: [Thunderbolt]"));
        verify(userRepository, never()).save(any());
    }

    @Test
    void updateAccountStatus_veterinarianWithPendingSchedules_throwsConflict() {
        Long currentAdminId = 1L;
        Long vetId = 8L;

        User user = new User();
        user.setId(vetId);
        user.setActive(true);
        Role role = new Role();
        role.setName("VETERINARIAN");
        user.setRole(role);

        when(userRepository.findById(vetId)).thenReturn(Optional.of(user));
        when(careScheduleRepository.existsByVeterinarianIdAndStatusIn(eq(vetId), any())).thenReturn(true);

        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () ->
                accountService.updateAccountStatus(currentAdminId, vetId, false)
        );

        assertEquals(HttpStatus.CONFLICT, ex.getStatusCode());
        assertTrue(ex.getReason().contains("pending or in-progress care schedules"));
        verify(userRepository, never()).save(any());
    }

    @Test
    void updateAccountStatus_lastClubManager_throwsConflict() {
        Long currentAdminId = 1L;
        Long targetManagerId = 2L;

        User targetManager = new User();
        targetManager.setId(targetManagerId);
        targetManager.setActive(true);
        Role role = new Role();
        role.setName("CLUB_MANAGER");
        targetManager.setRole(role);

        when(userRepository.findById(targetManagerId)).thenReturn(Optional.of(targetManager));
        when(userRepository.countActiveClubManagers()).thenReturn(1L);

        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () ->
                accountService.updateAccountStatus(currentAdminId, targetManagerId, false)
        );

        assertEquals(HttpStatus.CONFLICT, ex.getStatusCode());
        assertTrue(ex.getReason().contains("last active Club Manager in the system"));
        verify(userRepository, never()).save(any());
    }

    @Test
    void updateAccountStatus_clubManager_notifiesOtherManagers() {
        Long currentAdminId = 1L;
        Long targetManagerId = 2L;

        User admin1 = new User();
        admin1.setId(currentAdminId);
        admin1.setFullName("Admin One");

        User targetManager = new User();
        targetManager.setId(targetManagerId);
        targetManager.setFullName("Target Manager");
        targetManager.setActive(true);
        Role role = new Role();
        role.setName("CLUB_MANAGER");
        targetManager.setRole(role);

        User anotherManager = new User();
        anotherManager.setId(3L);
        anotherManager.setFullName("Manager Three");

        when(userRepository.findById(currentAdminId)).thenReturn(Optional.of(admin1));
        when(userRepository.findById(targetManagerId)).thenReturn(Optional.of(targetManager));
        when(userRepository.countActiveClubManagers()).thenReturn(3L);
        when(userRepository.findActiveClubManagers()).thenReturn(List.of(admin1, targetManager, anotherManager));
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        AccountSummaryResponse res = accountService.updateAccountStatus(currentAdminId, targetManagerId, false);

        assertFalse(res.isActive());
        verify(notificationService).createNotification(
                eq(1L),
                eq("Manager Account Deactivated"),
                contains("Target Manager was deactivated by Admin One"),
                eq("SECURITY_ALERT"),
                eq("USER"),
                eq(targetManagerId),
                anyString()
        );
        verify(notificationService).createNotification(
                eq(3L),
                eq("Manager Account Deactivated"),
                contains("Target Manager was deactivated by Admin One"),
                eq("SECURITY_ALERT"),
                eq("USER"),
                eq(targetManagerId),
                anyString()
        );
    }
}
