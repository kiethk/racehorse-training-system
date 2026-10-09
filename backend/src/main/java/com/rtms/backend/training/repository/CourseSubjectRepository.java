package com.rtms.backend.training.repository;

import com.rtms.backend.training.entity.CourseSubject;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CourseSubjectRepository extends JpaRepository<CourseSubject, Long> {
    List<CourseSubject> findByCourseIdOrderByOrderIndexAsc(Long courseId);
}