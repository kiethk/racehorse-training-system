package com.rtms.backend.service;

import com.rtms.backend.dto.StaffCreationRequest;
import com.rtms.backend.dto.StaffSummaryResponse;
import com.rtms.backend.entity.*;
import com.rtms.backend.repository.*;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class StaffManagementService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final GroomProfileRepository groomProfileRepository;
    private final VeterinarianProfileRepository vetProfileRepository;
    private final TrainerProfileRepository trainerProfileRepository;

    private static final List<String> STAFF_ROLES = Arrays.asList("GROOM", "VETERINARIAN", "HEAD_TRAINER");

    public StaffManagementService(
            UserRepository userRepository,
            RoleRepository roleRepository,
            PasswordEncoder passwordEncoder,
            GroomProfileRepository groomProfileRepository,
            VeterinarianProfileRepository vetProfileRepository,
            TrainerProfileRepository trainerProfileRepository) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.passwordEncoder = passwordEncoder;
        this.groomProfileRepository = groomProfileRepository;
        this.vetProfileRepository = vetProfileRepository;
        this.trainerProfileRepository = trainerProfileRepository;
    }

    public List<StaffSummaryResponse> getAllStaff() {
        return userRepository.findAll().stream()
                .filter(u -> u.getRole() != null && STAFF_ROLES.contains(u.getRole().getName()))
                .map(this::mapToSummary)
                .collect(Collectors.toList());
    }

    @Transactional
    public StaffSummaryResponse createStaff(StaffCreationRequest request) {
        if (request.getRole() == null || !STAFF_ROLES.contains(request.getRole())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid or unauthorized role for staff creation");
        }

        if (userRepository.findByEmail(request.getEmail().toLowerCase().trim()).isPresent()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Email already in use");
        }

        Role role = roleRepository.findByName(request.getRole())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Role not found"));

        User user = new User();
        user.setFullName(request.getFullName().trim());
        user.setEmail(request.getEmail().toLowerCase().trim());
        user.setPasswordHash(passwordEncoder.encode(request.getPassword()));
        user.setPhone(request.getPhone());
        user.setAddress(request.getAddress());
        user.setRole(role);
        user.setActive(true);
        
        user = userRepository.save(user);

        switch (request.getRole()) {
            case "GROOM":
                GroomProfile groom = new GroomProfile();
                groom.setUserId(user.getId());
                if (request.getTrainerId() != null) {
                    User trainer = userRepository.findById(request.getTrainerId())
                            .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Trainer not found"));
                    if (trainer.getRole() == null || !"HEAD_TRAINER".equals(trainer.getRole().getName()) || !trainer.isActive()) {
                        throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid or inactive HEAD_TRAINER");
                    }
                    groom.setTrainerId(trainer.getId());
                }
                groomProfileRepository.save(groom);
                break;
            case "VETERINARIAN":
                if (request.getLicenseNumber() == null || request.getLicenseNumber().trim().isEmpty()) {
                    throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "License number is required for Veterinarian");
                }
                VeterinarianProfile vet = new VeterinarianProfile();
                vet.setUserId(user.getId());
                vet.setLicenseNumber(request.getLicenseNumber().trim());
                vet.setLicenseIssuedDate(request.getLicenseIssuedDate());
                vet.setSpecialization(request.getSpecialization());
                vetProfileRepository.save(vet);
                break;
            case "HEAD_TRAINER":
                if (request.getCertificationNumber() == null || request.getCertificationNumber().trim().isEmpty()) {
                    throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Certification number is required for Head Trainer");
                }
                TrainerProfile trainer = new TrainerProfile();
                trainer.setUserId(user.getId());
                trainer.setCertificationNumber(request.getCertificationNumber().trim());
                trainer.setCertificationIssuedDate(request.getCertificationIssuedDate());
                trainerProfileRepository.save(trainer);
                break;
        }

        return mapToSummary(user);
    }

    @Transactional
    public StaffSummaryResponse updateStaffStatus(Long userId, boolean active) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));

        if (user.getRole() == null || !STAFF_ROLES.contains(user.getRole().getName())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Cannot update status of non-staff account through this API");
        }

        user.setActive(active);
        user = userRepository.save(user);
        return mapToSummary(user);
    }

    private StaffSummaryResponse mapToSummary(User user) {
        StaffSummaryResponse summary = new StaffSummaryResponse();
        summary.setUserId(user.getId());
        summary.setFullName(user.getFullName());
        summary.setEmail(user.getEmail());
        summary.setPhone(user.getPhone());
        summary.setAddress(user.getAddress());
        summary.setRole(user.getRole() != null ? user.getRole().getName() : null);
        summary.setActive(user.isActive());
        summary.setCreatedAt(user.getCreatedAt());

        if (summary.getRole() != null) {
            switch (summary.getRole()) {
                case "GROOM":
                    groomProfileRepository.findById(user.getId()).ifPresent(p -> {
                        summary.setProfileSummary(p.getTrainerId() != null ? "Trainer ID: " + p.getTrainerId() : "No Trainer Assigned");
                    });
                    break;
                case "VETERINARIAN":
                    vetProfileRepository.findById(user.getId()).ifPresent(p -> {
                        summary.setProfileSummary("License: " + p.getLicenseNumber());
                    });
                    break;
                case "HEAD_TRAINER":
                    trainerProfileRepository.findById(user.getId()).ifPresent(p -> {
                        summary.setProfileSummary("Cert: " + p.getCertificationNumber());
                    });
                    break;
            }
        }

        return summary;
    }
}
