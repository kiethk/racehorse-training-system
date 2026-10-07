package com.rtms.backend.service;

import com.rtms.backend.dto.TrainerAdmissionReviewRequest;
import com.rtms.backend.entity.AdmissionApplication;
import com.rtms.backend.entity.TrainerSchedule;
import com.rtms.backend.enums.AdmissionStatus;
import com.rtms.backend.enums.TrainerScheduleStatus;
import com.rtms.backend.repository.AdmissionApplicationRepository;
import com.rtms.backend.repository.TrainerScheduleRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.Objects;

/**
 * Bước Trainer trong luồng nhập học (Compatibility wrapper ủy quyền qua TrainerScheduleService).
 * Assignment source of truth hoàn toàn thuộc về trainer_schedule.
 */
@Service
public class AdmissionTrainerReviewService {

    private final AdmissionApplicationRepository admissionRepository;
    private final TrainerScheduleRepository trainerScheduleRepository;
    private final TrainerScheduleService trainerScheduleService;

    public AdmissionTrainerReviewService(
            AdmissionApplicationRepository admissionRepository,
            TrainerScheduleRepository trainerScheduleRepository,
            TrainerScheduleService trainerScheduleService) {
        this.admissionRepository = admissionRepository;
        this.trainerScheduleRepository = trainerScheduleRepository;
        this.trainerScheduleService = trainerScheduleService;
    }

    @Transactional
    public AdmissionApplication completeAssessment(Long admissionId,
                                                    TrainerAdmissionReviewRequest request,
                                                    Long trainerId) {

        AdmissionApplication admission = admissionRepository.findById(admissionId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Không tìm thấy đơn nhập học #" + admissionId));

        if (admission.getStatus() != AdmissionStatus.TRAINER_REVIEW) {
            throw new IllegalStateException(String.format(
                    "Đơn đang ở bước %s, không phải TRAINER_REVIEW — không thể đánh giá!",
                    admission.getStatus()));
        }

        if (admission.getHorseId() == null) {
            throw new IllegalStateException(
                    "Đơn chưa gắn hồ sơ chiến mã. Bước Groom phải tạo Horse (CANDIDATE) "
                  + "và xếp chuồng cách ly trước khi Trainer đánh giá!");
        }

        TrainerSchedule schedule = trainerScheduleRepository.findByAdmissionId(admissionId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Không tìm thấy lịch phân công Trainer cho đơn nhập học #" + admissionId));

        if (!Objects.equals(schedule.getTrainerId(), trainerId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "Chỉ Trainer được phân công mới có quyền đánh giá đơn này!");
        }

        if (schedule.getStatus() == TrainerScheduleStatus.SCHEDULED) {
            trainerScheduleService.startSchedule(schedule.getId(), trainerId);
        }

        trainerScheduleService.completeSchedule(schedule.getId(), request, trainerId);

        return admissionRepository.findById(admissionId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Không tìm thấy đơn nhập học #" + admissionId));
    }
}