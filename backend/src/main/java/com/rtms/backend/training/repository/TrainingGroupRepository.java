package com.rtms.backend.training.repository;

import com.rtms.backend.training.entity.TrainingGroup;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TrainingGroupRepository extends JpaRepository<TrainingGroup, Long> {

    List<TrainingGroup> findByTrainerIdAndDisbandedAtIsNullOrderByCreatedAtDesc(Long trainerId);

    List<TrainingGroup> findByTrainerIdAndDisbandedAtIsNotNullOrderByCreatedAtDesc(Long trainerId);

    List<TrainingGroup> findByTrainerIdAndDisbandedAtIsNullOrderByNameAsc(Long trainerId);

    List<TrainingGroup> findByTrainerIdAndDisbandedAtIsNotNullOrderByNameAsc(Long trainerId);

    List<TrainingGroup> findByDisbandedAtIsNull();

    List<TrainingGroup> findByDisbandedAtIsNullOrderByCreatedAtDesc();

    List<TrainingGroup> findByDisbandedAtIsNotNull();

    List<TrainingGroup> findByDisbandedAtIsNotNullOrderByCreatedAtDesc();

    /** Tên chỉ cần khác nhau giữa các nhóm còn hoạt động. (Names only need to differ among live groups.) */
    boolean existsByTrainerIdAndNameIgnoreCaseAndDisbandedAtIsNull(Long trainerId, String name);
}
