package com.rtms.backend.training.repository;

import com.rtms.backend.training.entity.SubjectCategory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface SubjectCategoryRepository extends JpaRepository<SubjectCategory, Long> {
}