package com.rtms.backend.service;

import com.rtms.backend.dto.CreatePreventiveCareRecordRequest;
import com.rtms.backend.dto.CreatePreventiveCareScheduleRequest;
import com.rtms.backend.entity.Horse;
import com.rtms.backend.entity.PreventiveCareRecord;
import com.rtms.backend.entity.PreventiveCareSchedule;
import com.rtms.backend.repository.HorseRepository;
import com.rtms.backend.repository.PreventiveCareRecordRepository;
import com.rtms.backend.repository.PreventiveCareScheduleRepository;
import com.rtms.backend.security.AuthenticatedUser;

import org.springframework.security.access.AccessDeniedException;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PreventiveCareService {

    private final PreventiveCareScheduleRepository scheduleRepository;
    private final PreventiveCareRecordRepository recordRepository;
    private final HorseRepository horseRepository;

    public PreventiveCareService(PreventiveCareScheduleRepository scheduleRepository,
            PreventiveCareRecordRepository recordRepository,
            HorseRepository horseRepository) {
        this.scheduleRepository = scheduleRepository;
        this.recordRepository = recordRepository;
        this.horseRepository = horseRepository;
    }

    public PreventiveCareSchedule createSchedule(CreatePreventiveCareScheduleRequest request) {
        PreventiveCareSchedule schedule = new PreventiveCareSchedule();
        schedule.setHorseId(request.getHorseId());
        schedule.setVeterinarianId(request.getVeterinarianId());
        schedule.setCareType(request.getCareType());
        schedule.setScheduledDate(request.getScheduledDate());
        schedule.setDescription(request.getDescription());
        // status mặc định "SCHEDULED" đã set sẵn trong Entity

        return scheduleRepository.save(schedule);
    }

    public List<PreventiveCareSchedule> getSchedulesByHorse(Long horseId, AuthenticatedUser currentUser)
            throws AccessDeniedException {
        if ("HORSE_OWNER".equals(currentUser.getRole())) {
            Horse horse = horseRepository.findById(horseId)
                    .orElseThrow(() -> new RuntimeException("Horse not found with id: " + horseId));

            if (!currentUser.getUserId().equals(horse.getOwnerId())) {
                throw new AccessDeniedException("You can only view schedules for horses you own");
            }
        }
        return scheduleRepository.findByHorseIdOrderByScheduledDateAsc(horseId);
    }

    @Transactional
    public PreventiveCareRecord recordCompletion(Long scheduleId, CreatePreventiveCareRecordRequest request) {
        // 1. Tìm schedule, ném lỗi nếu không tồn tại
        PreventiveCareSchedule schedule = scheduleRepository.findById(scheduleId)
                .orElseThrow(() -> new RuntimeException("Schedule not found with id: " + scheduleId));

        // 2. Tạo Record từ dữ liệu của Request + Schedule
        PreventiveCareRecord record = new PreventiveCareRecord();
        record.setHorseId(schedule.getHorseId());
        record.setScheduleId(schedule.getId());
        record.setVeterinarianId(request.getVeterinarianId());
        record.setCareType(schedule.getCareType());
        record.setPerformedAt(request.getPerformedAt());
        record.setPerformedByName(request.getPerformedByName());
        record.setProductOrService(request.getProductOrService());
        record.setResult(request.getResult());
        record.setNotes(request.getNotes());
        record.setNextDueDate(request.getNextDueDate());

        // 3. Lưu Record
        PreventiveCareRecord savedRecord = recordRepository.save(record);

        // 4. Cập nhật trạng thái Schedule → COMPLETED
        // (@Transactional đảm bảo bước 3 và 4 luôn thực hiện cùng nhau)
        schedule.setStatus("COMPLETED");
        scheduleRepository.save(schedule);

        return savedRecord;
    }

    public List<PreventiveCareRecord> getRecordsByHorse(Long horseId, AuthenticatedUser currentUser) {
        if ("HORSE_OWNER".equals(currentUser.getRole())) {
            Horse horse = horseRepository.findById(horseId)
                    .orElseThrow(() -> new RuntimeException("Horse not found with id: " + horseId));

            if (!currentUser.getUserId().equals(horse.getOwnerId())) {
                throw new org.springframework.security.access.AccessDeniedException(
                        "You can only view medical records for horses you own");
            }
        }
        return recordRepository.findByHorseIdOrderByPerformedAtDesc(horseId);
    }

}
