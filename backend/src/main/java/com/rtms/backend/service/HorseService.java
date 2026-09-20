package com.rtms.backend.service;

import com.rtms.backend.dto.CreateHorseRequest;
import com.rtms.backend.dto.UpdateHorseStatusRequest;
import com.rtms.backend.entity.Horse;
import com.rtms.backend.enums.HorseStatus;
import com.rtms.backend.repository.HorseRepository;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;

import java.util.List;
import com.rtms.backend.security.AuthenticatedUser;

@Service
public class HorseService {

    private final HorseRepository horseRepository;

    public HorseService(HorseRepository horseRepository) {
        this.horseRepository = horseRepository;
    }

    public List<Horse> getAllHorses(AuthenticatedUser currentUser) {
        if ("HORSE_OWNER".equals(currentUser.getRole())) {
            return horseRepository.findByOwnerId(currentUser.getUserId());
        }
        return horseRepository.findAll();
    }

    public Horse createHorse(CreateHorseRequest request) {
        Horse horse = new Horse();
        horse.setName(request.getName());
        horse.setBreed(request.getBreed());
        horse.setDateOfBirth(request.getDateOfBirth());
        horse.setStableLocation(request.getStableLocation());
        horse.setOwnerId(request.getOwnerId());
        // currentStatus KHONG set o day - de mac dinh "ELIGIBLE" theo gia tri default
        // trong Entity/DB
        return horseRepository.save(horse);
    }

    public Horse getHorseById(Long id, AuthenticatedUser currentUser) {
        Horse horse = horseRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Horse not found with id: " + id));
        if ("HORSE_OWNER".equals(currentUser.getRole())) {
            if (!currentUser.getUserId().equals(horse.getOwnerId())) {
                throw new AccessDeniedException("You can only view horses you own");
            }
        }
        return horse;
    }

    public Horse updateHorseStatus(Long id, UpdateHorseStatusRequest request) {
        Horse horse = horseRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Horse not found with id: " + id));

        // Cập nhật trạng thái mới
        horse.setCurrentStatus(HorseStatus.valueOf(request.getStatus()));

        return horseRepository.save(horse);
    }

}