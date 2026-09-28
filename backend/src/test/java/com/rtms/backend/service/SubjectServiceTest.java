package com.rtms.backend.service;

import com.rtms.backend.dto.CreateSubjectRequest;
import com.rtms.backend.entity.Subject;
import com.rtms.backend.enums.WorkoutType;
import com.rtms.backend.repository.SubjectCategoryRepository;
import com.rtms.backend.repository.SubjectRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SubjectServiceTest {

    @Mock
    private SubjectRepository subjectRepository;

    @Mock
    private SubjectCategoryRepository categoryRepository;

    private SubjectService subjectService;

    @BeforeEach
    void setUp() {
        subjectService = new SubjectService(subjectRepository, categoryRepository);
    }

    @Test
    @DisplayName("1. Lấy danh sách bài tập - không lọc theo danh mục")
    void testGetAllSubjects_WithoutCategory_ReturnsAll() {
        Subject s1 = new Subject();
        s1.setId(1L);
        s1.setName("Khởi động");

        Subject s2 = new Subject();
        s2.setId(2L);
        s2.setName("Chạy bền");

        when(subjectRepository.findAll()).thenReturn(List.of(s1, s2));

        List<Subject> result = subjectService.getAllSubjects(null);

        assertEquals(2, result.size());
        verify(subjectRepository).findAll();
        verify(subjectRepository, never()).findByCategoryId(anyLong());
    }

    @Test
    @DisplayName("2. Lấy danh sách bài tập - lọc theo danh mục")
    void testGetAllSubjects_WithCategory_ReturnsFiltered() {
        Subject s = new Subject();
        s.setId(1L);
        s.setCategoryId(10L);
        s.setName("Bài tập thể lực");

        when(subjectRepository.findByCategoryId(10L)).thenReturn(List.of(s));

        List<Subject> result = subjectService.getAllSubjects(10L);

        assertEquals(1, result.size());
        assertEquals("Bài tập thể lực", result.get(0).getName());
        verify(subjectRepository).findByCategoryId(10L);
    }

    @Test
    @DisplayName("3. Lấy chi tiết bài tập theo ID thành công")
    void testGetSubjectById_Success() {
        Subject s = new Subject();
        s.setId(5L);
        s.setName("Bứt tốc cự ly ngắn");

        when(subjectRepository.findById(5L)).thenReturn(Optional.of(s));

        Subject result = subjectService.getSubjectById(5L);

        assertNotNull(result);
        assertEquals(5L, result.getId());
        assertEquals("Bứt tốc cự ly ngắn", result.getName());
    }

    @Test
    @DisplayName("4. Lấy chi tiết bài tập thất bại khi không tìm thấy ID")
    void testGetSubjectById_NotFound_ThrowsException() {
        when(subjectRepository.findById(999L)).thenReturn(Optional.empty());

        assertThrows(RuntimeException.class, () -> subjectService.getSubjectById(999L));
    }

    @Test
    @DisplayName("5. Tạo bài tập mới thành công với thời lượng hợp lệ (15 - 240 phút)")
    void testCreateSubject_Success() {
        CreateSubjectRequest req = new CreateSubjectRequest();
        req.setCategoryId(2L);
        req.setName("Luyện nhịp tim 45 phút");
        req.setDescription("Duy trì tốc độ vừa phải");
        req.setSurfaceType(com.rtms.backend.enums.SurfaceType.TURF);
        req.setTargetDistanceMeters(java.math.BigDecimal.valueOf(1600));
        req.setIntensityLevel(com.rtms.backend.enums.IntensityLevel.MEDIUM);
        req.setDurationMinutes(45);
        req.setWorkoutType(WorkoutType.REGULAR);

        when(categoryRepository.existsById(2L)).thenReturn(true);
        when(subjectRepository.save(any(Subject.class))).thenAnswer(inv -> {
            Subject s = inv.getArgument(0);
            s.setId(88L);
            return s;
        });

        Subject created = subjectService.createSubject(req);

        assertNotNull(created);
        assertEquals(88L, created.getId());
        assertEquals("Luyện nhịp tim 45 phút", created.getName());
        assertEquals(45, created.getDurationMinutes());
        assertEquals(WorkoutType.REGULAR, created.getWorkoutType());
        verify(subjectRepository).save(any(Subject.class));
    }

    @Test
    @DisplayName("6. Chặn tạo bài tập nếu danh mục bài tập không tồn tại")
    void testCreateSubject_CategoryNotFound_ThrowsIllegalArgument() {
        CreateSubjectRequest req = new CreateSubjectRequest();
        req.setCategoryId(999L);
        req.setName("Bài tập lỗi");

        when(categoryRepository.existsById(999L)).thenReturn(false);

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> subjectService.createSubject(req));
        assertTrue(ex.getMessage().contains("Category not found"));
    }

    @Test
    @DisplayName("7. Chặn tạo bài tập nếu thời lượng quá ngắn (< 15 phút)")
    void testCreateSubject_DurationTooShort_ThrowsIllegalArgument() {
        CreateSubjectRequest req = new CreateSubjectRequest();
        req.setCategoryId(1L);
        req.setName("Tập siêu tốc");
        req.setDurationMinutes(10); // < 15

        when(categoryRepository.existsById(1L)).thenReturn(true);

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> subjectService.createSubject(req));
        assertTrue(ex.getMessage().contains("từ 15 đến 240 phút"));
    }

    @Test
    @DisplayName("8. Chặn tạo bài tập nếu thời lượng quá dài (> 240 phút)")
    void testCreateSubject_DurationTooLong_ThrowsIllegalArgument() {
        CreateSubjectRequest req = new CreateSubjectRequest();
        req.setCategoryId(1L);
        req.setName("Tập marathon");
        req.setDurationMinutes(300); // > 240

        when(categoryRepository.existsById(1L)).thenReturn(true);

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> subjectService.createSubject(req));
        assertTrue(ex.getMessage().contains("từ 15 đến 240 phút"));
    }
}
