package com.rtms.backend.service;

import com.rtms.backend.dto.StaffCreationRequest;
import com.rtms.backend.dto.StaffSummaryResponse;
import com.rtms.backend.entity.*;
import com.rtms.backend.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class StaffManagementServiceTest {

    private UserRepository userRepository;
    private RoleRepository roleRepository;
    private PasswordEncoder passwordEncoder;
    private GroomProfileRepository groomProfileRepository;
    private VeterinarianProfileRepository vetProfileRepository;
    private TrainerProfileRepository trainerProfileRepository;
    private StaffManagementService staffService;

    @BeforeEach
    void setUp() {
        userRepository = mock(UserRepository.class);
        roleRepository = mock(RoleRepository.class);
        passwordEncoder = mock(PasswordEncoder.class);
        groomProfileRepository = mock(GroomProfileRepository.class);
        vetProfileRepository = mock(VeterinarianProfileRepository.class);
        trainerProfileRepository = mock(TrainerProfileRepository.class);

        staffService = new StaffManagementService(
                userRepository, roleRepository, passwordEncoder,
                groomProfileRepository, vetProfileRepository, trainerProfileRepository
        );
    }

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
    void testCreateGroom() {
        StaffCreationRequest request = new StaffCreationRequest();
        request.setFullName("John Groom");
        request.setEmail("john@rtms.com");
        request.setPassword("pass");
        request.setRole("GROOM");
        request.setTrainerId(2L);

        Role role = new Role(); role.setName("GROOM");
        User trainer = new User(); 
        trainer.setId(2L);
        Role trainerRole = new Role(); trainerRole.setName("HEAD_TRAINER");
        trainer.setRole(trainerRole);
        trainer.setActive(true);

        when(userRepository.findByEmail(anyString())).thenReturn(Optional.empty());
        when(roleRepository.findByName("GROOM")).thenReturn(Optional.of(role));
        when(passwordEncoder.encode(anyString())).thenReturn("encodedPass");
        when(userRepository.findById(2L)).thenReturn(Optional.of(trainer));
        when(userRepository.save(any(User.class))).thenAnswer(i -> {
            User u = i.getArgument(0);
            u.setId(1L);
            return u;
        });

        StaffSummaryResponse response = staffService.createStaff(request);
        
        assertNotNull(response);
        assertEquals("John Groom", response.getFullName());
        assertEquals("GROOM", response.getRole());
        assertTrue(response.isActive());
        verify(groomProfileRepository, times(1)).save(any(GroomProfile.class));
    }

    @Test
    void testCreateVet_WithProfile() {
        StaffCreationRequest request = new StaffCreationRequest();
        request.setFullName("Jane Vet");
        request.setEmail("jane@rtms.com");
        request.setPassword("pass");
        request.setRole("VETERINARIAN");
        request.setLicenseNumber("VET123");

        Role role = new Role(); role.setName("VETERINARIAN");

        when(userRepository.findByEmail(anyString())).thenReturn(Optional.empty());
        when(roleRepository.findByName("VETERINARIAN")).thenReturn(Optional.of(role));
        when(passwordEncoder.encode(anyString())).thenReturn("encodedPass");
        when(userRepository.save(any(User.class))).thenAnswer(i -> {
            User u = i.getArgument(0);
            u.setId(1L);
            return u;
        });

        StaffSummaryResponse response = staffService.createStaff(request);
        
        assertNotNull(response);
        verify(vetProfileRepository, times(1)).save(any(VeterinarianProfile.class));
    }

    @Test
    void testCreateTrainer_WithProfile() {
        StaffCreationRequest request = new StaffCreationRequest();
        request.setFullName("Tom Trainer");
        request.setEmail("tom@rtms.com");
        request.setPassword("pass");
        request.setRole("HEAD_TRAINER");
        request.setCertificationNumber("CERT123");

        Role role = new Role(); role.setName("HEAD_TRAINER");

        when(userRepository.findByEmail(anyString())).thenReturn(Optional.empty());
        when(roleRepository.findByName("HEAD_TRAINER")).thenReturn(Optional.of(role));
        when(passwordEncoder.encode(anyString())).thenReturn("encodedPass");
        when(userRepository.save(any(User.class))).thenAnswer(i -> {
            User u = i.getArgument(0);
            u.setId(1L);
            return u;
        });

        StaffSummaryResponse response = staffService.createStaff(request);
        
        assertNotNull(response);
        verify(trainerProfileRepository, times(1)).save(any(TrainerProfile.class));
    }

    @Test
    void testDuplicateEmailRejected() {
        StaffCreationRequest request = new StaffCreationRequest();
        request.setEmail("dup@rtms.com");
        request.setRole("GROOM");

        when(userRepository.findByEmail(anyString())).thenReturn(Optional.of(new User()));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () -> staffService.createStaff(request));
        assertEquals(HttpStatus.CONFLICT, ex.getStatusCode());
    }

    @Test
    void testInvalidRoleRejected() {
        StaffCreationRequest request = new StaffCreationRequest();
        request.setEmail("test@rtms.com");
        request.setRole("CLUB_MANAGER");

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
