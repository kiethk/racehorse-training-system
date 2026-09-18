package com.rtms.backend.service;

import com.rtms.backend.dto.CreateHorseTrainingPlanRequest;
import com.rtms.backend.dto.HorseTrainingPlanDetailResponse;
import com.rtms.backend.dto.TrainingLockStatusResponse;
import com.rtms.backend.dto.UpdatePlanStatusRequest;
import com.rtms.backend.entity.Course;
import com.rtms.backend.entity.CourseSubject;
import com.rtms.backend.entity.HorseTrainingPlan;
import com.rtms.backend.entity.TrainingSession;
import com.rtms.backend.repository.CourseRepository;
import com.rtms.backend.repository.CourseSubjectRepository;
import com.rtms.backend.repository.HorseRepository;
import com.rtms.backend.repository.HorseTrainingPlanRepository;
import com.rtms.backend.repository.TrainingSessionRepository;
import com.rtms.backend.security.AuthenticatedUser;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class HorseTrainingPlanService {

    private final HorseTrainingPlanRepository planRepository;
    private final TrainingSessionRepository sessionRepository;
    private final CourseRepository courseRepository;
    private final CourseSubjectRepository courseSubjectRepository;
    private final HorseRepository horseRepository;
    private final InjuryRecordService injuryRecordService; // GỌI SANG MODULE CỦA THÀNH

    public HorseTrainingPlanService(HorseTrainingPlanRepository planRepository,
                                    TrainingSessionRepository sessionRepository,
                                    CourseRepository courseRepository,
                                    CourseSubjectRepository courseSubjectRepository,
                                    HorseRepository horseRepository,
                                    InjuryRecordService injuryRecordService) {
        this.planRepository = planRepository;
        this.sessionRepository = sessionRepository;
        this.courseRepository = courseRepository;
        this.courseSubjectRepository = courseSubjectRepository;
        this.horseRepository = horseRepository;
        this.injuryRecordService = injuryRecordService;
    }

    public List<HorseTrainingPlan> getAllPlans() {
        return planRepository.findAll();
    }

    public List<HorseTrainingPlan> getPlansByHorse(Long horseId) {
        return planRepository.findByHorseId(horseId);
    }

    public HorseTrainingPlanDetailResponse getPlanById(Long id) {
        HorseTrainingPlan plan = planRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Training plan not found with id: " + id));
        List<TrainingSession> sessions = sessionRepository.findByPlanIdOrderBySessionDateAsc(id);
        return new HorseTrainingPlanDetailResponse(plan, sessions);
    }

    @Transactional
    public HorseTrainingPlanDetailResponse createPlan(CreateHorseTrainingPlanRequest request, AuthenticatedUser currentUser) {
        // 1. KIỂM TRA KHÓA HUẤN LUYỆN (GỌI SERVICE CỦA THÀNH)
        TrainingLockStatusResponse lockStatus = injuryRecordService.getTrainingLockStatus(request.getHorseId());
        if (lockStatus.isLocked()) {
            throw new IllegalStateException("Chiến mã này đang bị KHÓA HUẤN LUYỆN (Trạng thái: "
                    + lockStatus.getCurrentStatus() + "). Không thể tạo kế hoạch mới!");
        }

        // 2. Lấy thông tin Course và Subjects
        Course course = courseRepository.findById(request.getCourseId())
                .orElseThrow(() -> new RuntimeException("Course not found with id: " + request.getCourseId()));

        List<CourseSubject> subjects = courseSubjectRepository.findByCourseIdOrderByOrderIndexAsc(course.getId());
        if (subjects.isEmpty()) {
            throw new IllegalStateException("Khóa học này chưa có bài tập nào, không thể gán cho ngựa!");
        }

        // 3. Chuẩn bị danh sách thứ tập (nếu không chọn thì mặc định T2, T4, T6)
        Set<String> selectedDays = (request.getTrainingDays() != null && !request.getTrainingDays().isEmpty())
                ? request.getTrainingDays().stream().map(String::toUpperCase).collect(Collectors.toSet())
                : Set.of("MONDAY", "WEDNESDAY", "FRIDAY");

        // 4. Tạo trước HorseTrainingPlan (endDate tạm thời là startDate)
        HorseTrainingPlan plan = new HorseTrainingPlan();
        plan.setHorseId(request.getHorseId());
        plan.setCourseId(request.getCourseId());
        plan.setTrainerId(currentUser.getUserId());
        plan.setStartDate(request.getStartDate());
        plan.setEndDate(request.getStartDate()); // Sẽ cập nhật sau khi sinh xong các session
        plan.setStatus("ACTIVE");
        plan.setNotes(request.getNotes());

        HorseTrainingPlan savedPlan = planRepository.save(plan);

        // 5. TỰ ĐỘNG SINH CÁC BUỔI TẬP (TRAINING SESSIONS)
        List<TrainingSession> createdSessions = new ArrayList<>();
        LocalDate currentDate = request.getStartDate();
        int sessionCount = 0;
        int totalNeeded = course.getTotalSessions();

        while (sessionCount < totalNeeded) {
            String dayOfWeekName = currentDate.getDayOfWeek().name();
            if (selectedDays.contains(dayOfWeekName)) {
                // Chọn môn học xoay vòng theo order_index
                CourseSubject currentSubject = subjects.get(sessionCount % subjects.size());

                TrainingSession session = new TrainingSession();
                session.setPlanId(savedPlan.getId());
                session.setSubjectId(currentSubject.getId());
                session.setHorseId(request.getHorseId());
                session.setAssignedToId(request.getAssignedToId());
                session.setSessionDate(currentDate);
                session.setSessionType(currentSubject.getName().toUpperCase().contains("TRIAL RUN") ? "TRIAL_RUN" : "REGULAR");
                session.setStatus("SCHEDULED");

                createdSessions.add(sessionRepository.save(session));
                sessionCount++;
            }
            if (sessionCount < totalNeeded) {
                currentDate = currentDate.plusDays(1);
            }
        }

        // 6. Cập nhật endDate chính xác là ngày của buổi tập cuối cùng
        savedPlan.setEndDate(currentDate);
        savedPlan = planRepository.save(savedPlan);

        return new HorseTrainingPlanDetailResponse(savedPlan, createdSessions);
    }

    @Transactional
    public HorseTrainingPlan updatePlanStatus(Long id, UpdatePlanStatusRequest request) {
        HorseTrainingPlan plan = planRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Training plan not found with id: " + id));

        plan.setStatus(request.getStatus().toUpperCase());
        return planRepository.save(plan);
    }
}