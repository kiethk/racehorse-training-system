package com.rtms.backend.training.controller;

import com.rtms.backend.common.dto.ApiResponse;
import com.rtms.backend.training.dto.CourseDetailResponse;
import com.rtms.backend.training.dto.CourseSubjectItemRequest;
import com.rtms.backend.training.dto.CourseSubjectResponse;
import com.rtms.backend.training.dto.CreateCourseRequest;
import com.rtms.backend.training.entity.Course;
import com.rtms.backend.security.AuthenticatedUser;
import com.rtms.backend.training.service.CourseService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/courses")
public class CourseController {

    private final CourseService courseService;

    public CourseController(CourseService courseService) {
        this.courseService = courseService;
    }

    @PreAuthorize("hasAuthority('COURSE_VIEW')")
    @GetMapping
    public ApiResponse<List<Course>> getAllCourses(
            @RequestParam(required = false) Long trainerId,
            @AuthenticationPrincipal AuthenticatedUser currentUser) {
        return ApiResponse.success(courseService.getAllCourses(trainerId, currentUser));
    }

    @PreAuthorize("hasAuthority('COURSE_VIEW')")
    @GetMapping("/{id}")
    public ApiResponse<CourseDetailResponse> getCourseById(
            @PathVariable Long id,
            @AuthenticationPrincipal AuthenticatedUser currentUser) {
        return ApiResponse.success(courseService.getCourseById(id, currentUser));
    }

    @PreAuthorize("hasAuthority('COURSE_CREATE')")
    @PostMapping
    public ApiResponse<CourseDetailResponse> createCourse(
            @RequestBody CreateCourseRequest request,
            @AuthenticationPrincipal AuthenticatedUser currentUser) {
        return ApiResponse.success(courseService.createCourse(request, currentUser));
    }

    @PreAuthorize("hasAuthority('COURSE_CREATE')")
    @PostMapping("/{id}/subjects")
    public ApiResponse<CourseSubjectResponse> addSubjectToCourse(@PathVariable Long id,
                                                                 @RequestBody CourseSubjectItemRequest request) {
        return ApiResponse.success(courseService.addSubjectToCourse(id, request));
    }
}