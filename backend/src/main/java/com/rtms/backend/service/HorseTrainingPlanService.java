package com.rtms.backend.service;

import com.rtms.backend.dto.CreateHorseTrainingPlanRequest;
import com.rtms.backend.dto.HorseTrainingPlanDetailResponse;
import com.rtms.backend.dto.TrainingLockStatusResponse;
import com.rtms.backend.dto.UpdatePlanStatusRequest;
import com.rtms.backend.entity.Course;
import com.rtms.backend.entity.CourseSubject;
import com.rtms.backend.entity.HorseTrainingPlan;
import com.rtms.backend.entity.Subject;
import com.rtms.backend.entity.TrainingWorkout;
import com.rtms.backend.repository.CourseRepository;
import com.rtms.backend.repository.CourseSubjectRepository;
import com.rtms.backend.repository.HorseRepository;
import com.rtms.backend.repository.HorseTrainingPlanRepository;
import com.rtms.backend.repository.SubjectRepository;
import com.rtms.backend.repository.TrainingWorkoutRepository;
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
    private final TrainingWorkoutRepository workoutRepository;
    private final CourseRepository courseRepository;
    private final CourseSubjectRepository courseSubjectRepository;
    private final SubjectRepository subjectRepository;
    private final HorseRepository horseRepository;
    private final InjuryRecordService injuryRecordService;

    public HorseTrainingPlanService(HorseTrainingPlanRepository planRepository,
                                    TrainingWorkoutRepository workoutRepository,
                                    CourseRepository courseRepository,
                                    CourseSubjectRepository courseSubjectRepository,
                                    SubjectRepository subjectRepository,
                                    HorseRepository horseRepository,
                                    InjuryRecordService injuryRecordService) {
        this.planRepository = planRepository;
        this.workoutRepository = workoutRepository;
        this.courseRepository = courseRepository;
        this.courseSubjectRepository = courseSubjectRepository;
        this.subjectRepository = subjectRepository;
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
        List<TrainingWorkout> workouts = workoutRepository.findByPlanIdOrderByWorkoutDateAsc(id);
        return new HorseTrainingPlanDetailResponse(plan, workouts);
    }

    @Transactional
    public HorseTrainingPlanDetailResponse createPlan(CreateHorseTrainingPlanRequest request, AuthenticatedUser currentUser) {
        // 1. KIỂM TRA KHÓA HUẤN LUYỆN
        TrainingLockStatusResponse lockStatus = injuryRecordService.getTrainingLockStatus(request.getHorseId());
        if (lockStatus.isLocked()) {
            throw new IllegalStateException("Chiến mã này đang bị KHÓA HUẤN LUYỆN (Trạng thái: "
                    + lockStatus.getCurrentStatus() + "). Không thể tạo kế hoạch mới!");
        }

        // 2. KIỂM TRA TRÙNG LỊCH VỚI KẾ HOẠCH ACTIVE HIỆN TẠI
        List<HorseTrainingPlan> activePlans = planRepository.findByHorseIdAndStatus(request.getHorseId(), "ACTIVE");
        if (!activePlans.isEmpty()) {
            HorseTrainingPlan currentActivePlan = activePlans.get(0);
            if (!request.getStartDate().isAfter(currentActivePlan.getEndDate())) {
                throw new IllegalStateException("Ngựa đang có kế hoạch huấn luyện ACTIVE kéo dài đến ngày "
                        + currentActivePlan.getEndDate() + ". Bạn chỉ có thể lên lịch khóa mới sau ngày này!");
            }
        }

        // 3. Lấy thông tin Course và danh sách bài tập liên kết
        Course course = courseRepository.findById(request.getCourseId())
                .orElseThrow(() -> new RuntimeException("Course not found with id: " + request.getCourseId()));

        List<CourseSubject> courseSubjects = courseSubjectRepository.findByCourseIdOrderByOrderIndexAsc(course.getId());
        if (courseSubjects.isEmpty()) {
            throw new IllegalStateException("Khóa học này chưa có bài tập nào, không thể gán cho ngựa!");
        }

        // 4. Chuẩn bị danh sách thứ tập (mặc định T2, T4, T6)
        Set<String> selectedDays = (request.getTrainingDays() != null && !request.getTrainingDays().isEmpty())
                ? request.getTrainingDays().stream().map(String::toUpperCase).collect(Collectors.toSet())
                : Set.of("MONDAY", "WEDNESDAY", "FRIDAY");

        // 5. Tạo HorseTrainingPlan
        HorseTrainingPlan plan = new HorseTrainingPlan();
        plan.setHorseId(request.getHorseId());
        plan.setCourseId(request.getCourseId());
        plan.setTrainerId(currentUser.getUserId());
        plan.setStartDate(request.getStartDate());
        plan.setEndDate(request.getStartDate());
        plan.setStatus(request.getStartDate().isAfter(LocalDate.now()) ? "UPCOMING" : "ACTIVE");
        plan.setNotes(request.getNotes());

        HorseTrainingPlan savedPlan = planRepository.save(plan);

        // 6. TỰ ĐỘNG SINH CÁC BUỔI TẬP (TRAINING WORKOUTS)
        List<TrainingWorkout> createdWorkouts = new ArrayList<>();
        LocalDate currentDate = request.getStartDate();
        int workoutCount = 0;
        int totalNeeded = course.getTotalSessions();

        while (workoutCount < totalNeeded) {
            String dayOfWeekName = currentDate.getDayOfWeek().name();
            if (selectedDays.contains(dayOfWeekName)) {
                CourseSubject currentCS = courseSubjects.get(workoutCount % courseSubjects.size());
                Subject currentSubject = subjectRepository.findById(currentCS.getSubjectId())
                        .orElseThrow(() -> new RuntimeException("Subject not found with id: " + currentCS.getSubjectId()));

                TrainingWorkout workout = new TrainingWorkout();
                workout.setPlanId(savedPlan.getId());
                workout.setSubjectId(currentSubject.getId());
                workout.setHorseId(request.getHorseId());
                workout.setAssignedToId(request.getAssignedToId());
                workout.setWorkoutDate(currentDate);
                workout.setWorkoutType(currentSubject.getName().toUpperCase().contains("TRIAL RUN") ? "TRIAL_RUN" : "REGULAR");
                workout.setStatus("SCHEDULED");

                createdWorkouts.add(workoutRepository.save(workout));
                workoutCount++;
            }
            if (workoutCount < totalNeeded) {
                currentDate = currentDate.plusDays(1);
            }
        }

        savedPlan.setEndDate(currentDate);
        savedPlan = planRepository.save(savedPlan);

        return new HorseTrainingPlanDetailResponse(savedPlan, createdWorkouts);
    }

    @Transactional
    public HorseTrainingPlan updatePlanStatus(Long id, UpdatePlanStatusRequest request) {
        HorseTrainingPlan plan = planRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Training plan not found with id: " + id));

        plan.setStatus(request.getStatus().toUpperCase());
        return planRepository.save(plan);
    }
}