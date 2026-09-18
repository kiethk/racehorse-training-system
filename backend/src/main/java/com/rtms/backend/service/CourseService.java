package com.rtms.backend.service;

import com.rtms.backend.dto.CourseDetailResponse;
import com.rtms.backend.dto.CreateCourseRequest;
import com.rtms.backend.dto.CreateCourseSubjectRequest;
import com.rtms.backend.entity.Course;
import com.rtms.backend.entity.CourseSubject;
import com.rtms.backend.repository.CourseRepository;
import com.rtms.backend.repository.CourseSubjectRepository;
import com.rtms.backend.security.AuthenticatedUser;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
public class CourseService {

    private final CourseRepository courseRepository;
    private final CourseSubjectRepository courseSubjectRepository;

    public CourseService(CourseRepository courseRepository, CourseSubjectRepository courseSubjectRepository) {
        this.courseRepository = courseRepository;
        this.courseSubjectRepository = courseSubjectRepository;
    }

    public List<Course> getAllCourses() {
        return courseRepository.findAll();
    }

    public CourseDetailResponse getCourseById(Long id) {
        Course course = courseRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Course not found with id: " + id));
        List<CourseSubject> subjects = courseSubjectRepository.findByCourseIdOrderByOrderIndexAsc(id);
        return new CourseDetailResponse(course, subjects);
    }

    @Transactional
    public CourseDetailResponse createCourse(CreateCourseRequest request, AuthenticatedUser currentUser) {
        if (courseRepository.existsByName(request.getName())) {
            throw new IllegalArgumentException("Khóa học với tên '" + request.getName() + "' đã tồn tại!");
        }

        // 1. Tạo và lưu Course
        Course course = new Course();
        course.setName(request.getName());
        course.setDescription(request.getDescription());
        course.setTargetGoal(request.getTargetGoal());
        course.setTotalSessions(request.getTotalSessions());
        course.setCreatedById(currentUser.getUserId());
        course.setStatus("ACTIVE");

        Course savedCourse = courseRepository.save(course);

        // 2. Nếu có danh sách subjects đi kèm, lưu từng subject
        List<CourseSubject> savedSubjects = new ArrayList<>();
        if (request.getSubjects() != null && !request.getSubjects().isEmpty()) {
            int defaultIndex = 1;
            for (CreateCourseSubjectRequest subReq : request.getSubjects()) {
                CourseSubject subject = new CourseSubject();
                subject.setCourseId(savedCourse.getId());
                subject.setName(subReq.getName());
                subject.setDescription(subReq.getDescription());
                subject.setSurfaceType(subReq.getSurfaceType());
                subject.setTargetDistanceMeters(subReq.getTargetDistanceMeters());
                subject.setIntensityLevel(subReq.getIntensityLevel());
                subject.setOrderIndex(subReq.getOrderIndex() != null ? subReq.getOrderIndex() : defaultIndex++);

                savedSubjects.add(courseSubjectRepository.save(subject));
            }
        }

        return new CourseDetailResponse(savedCourse, savedSubjects);
    }

    @Transactional
    public CourseSubject addSubjectToCourse(Long courseId, CreateCourseSubjectRequest request) {
        // Kiểm tra course có tồn tại không
        if (!courseRepository.existsById(courseId)) {
            throw new RuntimeException("Course not found with id: " + courseId);
        }

        CourseSubject subject = new CourseSubject();
        subject.setCourseId(courseId);
        subject.setName(request.getName());
        subject.setDescription(request.getDescription());
        subject.setSurfaceType(request.getSurfaceType());
        subject.setTargetDistanceMeters(request.getTargetDistanceMeters());
        subject.setIntensityLevel(request.getIntensityLevel());
        subject.setOrderIndex(request.getOrderIndex());

        return courseSubjectRepository.save(subject);
    }
}