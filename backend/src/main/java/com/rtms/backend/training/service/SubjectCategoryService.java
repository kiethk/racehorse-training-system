package com.rtms.backend.training.service;

import com.rtms.backend.training.entity.SubjectCategory;
import com.rtms.backend.training.repository.SubjectCategoryRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class SubjectCategoryService {

    private final SubjectCategoryRepository categoryRepository;

    public SubjectCategoryService(SubjectCategoryRepository categoryRepository) {
        this.categoryRepository = categoryRepository;
    }

    public List<SubjectCategory> getAllCategories() {
        return categoryRepository.findAll();
    }
}