package com.rtms.backend.training.repository;

import com.rtms.backend.training.dto.CourseExerciseResponse;
import com.rtms.backend.training.entity.CourseExercise;
import com.rtms.backend.training.entity.Exercise;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;

@Repository
public interface CourseExerciseRepository extends JpaRepository<CourseExercise, Long> {

    long countByCourseId(Long courseId);

    /**
     * Các bài của nhiều khóa, gom theo khóa rồi theo đúng thứ tự xoay vòng; dùng để so danh sách bài của khóa mới.
     * (The exercises of several courses, grouped by course then in rotation order; used to compare a new course exercise list.)
     */
    List<CourseExercise> findByCourseIdInOrderByCourseIdAscOrderIndexAsc(Collection<Long> courseIds);

    /**
     * Các bài tập của khóa theo đúng thứ tự (một câu JOIN thay cho findById từng bài).
     * (A course exercises in order — one JOIN instead of one findById per exercise.)
     */
    @Query("""
           SELECT s FROM CourseExercise cs
           JOIN Exercise s ON cs.exerciseId = s.id
           WHERE cs.courseId = :courseId
           ORDER BY cs.orderIndex ASC
           """)
    List<Exercise> findExercisesOfCourseInOrder(@Param("courseId") Long courseId);

    /**
     * Các bài tập của khóa dưới dạng response, đã JOIN sẵn thông tin bài tập.
     * (A course exercises as response DTOs with the exercise details already joined.)
     */
    @Query("""
           SELECT new com.rtms.backend.training.dto.CourseExerciseResponse(
                      cs.id, s.id, s.name, s.description, s.surfaceType, s.targetDistanceMeters,
                      s.intensityLevel, cs.orderIndex, s.durationMinutes)
           FROM CourseExercise cs
           JOIN Exercise s ON cs.exerciseId = s.id
           WHERE cs.courseId = :courseId
           ORDER BY cs.orderIndex ASC
           """)
    List<CourseExerciseResponse> findCourseExerciseResponses(@Param("courseId") Long courseId);
}
