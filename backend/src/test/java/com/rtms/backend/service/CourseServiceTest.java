package com.rtms.backend.service;

import com.rtms.backend.dto.CourseDetailResponse;
import com.rtms.backend.dto.CourseSubjectItemRequest;
import com.rtms.backend.dto.CourseSubjectResponse;
import com.rtms.backend.dto.CreateCourseRequest;
import com.rtms.backend.entity.Course;
import com.rtms.backend.entity.CourseSubject;
import com.rtms.backend.entity.Subject;
import com.rtms.backend.enums.CourseStatus;
import com.rtms.backend.repository.CourseRepository;
import com.rtms.backend.repository.CourseSubjectRepository;
import com.rtms.backend.repository.SubjectRepository;
import com.rtms.backend.security.AuthenticatedUser;
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
class CourseServiceTest {

    @Mock
    private CourseRepository courseRepository;

    @Mock
    private CourseSubjectRepository courseSubjectRepository;

    @Mock
    private SubjectRepository subjectRepository;

    private CourseService courseService;

    private final AuthenticatedUser trainerUser = new AuthenticatedUser(9L, "trainer@example.com", "HEAD_TRAINER");

    @BeforeEach
    void setUp() {
        courseService = new CourseService(courseRepository, courseSubjectRepository, subjectRepository);
    }

    @Test
    @DisplayName("1. Lấy danh sách tất cả các khoá học")
    void testGetAllCourses() {
        Course c1 = new Course();
        c1.setId(1L);
        c1.setName("Khóa sơ cấp");

        Course c2 = new Course();
        c2.setId(2L);
        c2.setName("Khóa nâng cao");

        when(courseRepository.findAll()).thenReturn(List.of(c1, c2));

        List<Course> result = courseService.getAllCourses();

        assertEquals(2, result.size());
        assertEquals("Khóa sơ cấp", result.get(0).getName());
    }

    @Test
    @DisplayName("2. Lấy chi tiết khoá học kèm danh sách bài tập sắp xếp theo thứ tự")
    void testGetCourseById_Success() {
        Long courseId = 10L;
        Course course = new Course();
        course.setId(courseId);
        course.setName("Khóa sức bền");

        CourseSubject cs1 = new CourseSubject();
        cs1.setId(101L);
        cs1.setCourseId(courseId);
        cs1.setSubjectId(50L);
        cs1.setOrderIndex(1);

        Subject sub1 = new Subject();
        sub1.setId(50L);
        sub1.setName("Chạy chậm 20'");

        when(courseRepository.findById(courseId)).thenReturn(Optional.of(course));
        when(courseSubjectRepository.findByCourseIdOrderByOrderIndexAsc(courseId)).thenReturn(List.of(cs1));
        when(subjectRepository.findById(50L)).thenReturn(Optional.of(sub1));

        CourseDetailResponse response = courseService.getCourseById(courseId);

        assertNotNull(response);
        assertEquals("Khóa sức bền", response.getCourse().getName());
        assertEquals(1, response.getSubjects().size());
        assertEquals("Chạy chậm 20'", response.getSubjects().get(0).getSubjectName());
        assertEquals(1, response.getSubjects().get(0).getOrderIndex());
    }

    @Test
    @DisplayName("3. Lỗi khi không tìm thấy khoá học theo ID")
    void testGetCourseById_NotFound_ThrowsException() {
        when(courseRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(RuntimeException.class, () -> courseService.getCourseById(99L));
    }

    @Test
    @DisplayName("4. Tạo khoá học mới thành công kèm danh sách bài tập")
    void testCreateCourse_Success() {
        CreateCourseRequest req = new CreateCourseRequest();
        req.setName("Khóa bứt tốc 1200m");
        req.setDescription("Tập luyện nước rút");
        req.setTargetGoal("Chuẩn bị giải đua mùa thu");
        req.setTotalSessions(12);

        CourseSubjectItemRequest item1 = new CourseSubjectItemRequest();
        item1.setSubjectId(1L);
        item1.setOrderIndex(1);

        CourseSubjectItemRequest item2 = new CourseSubjectItemRequest();
        item2.setSubjectId(2L);
        item2.setOrderIndex(2);

        req.setSubjects(List.of(item1, item2));

        when(courseRepository.existsByName("Khóa bứt tốc 1200m")).thenReturn(false);
        when(subjectRepository.existsById(1L)).thenReturn(true);
        when(subjectRepository.existsById(2L)).thenReturn(true);

        when(courseRepository.save(any(Course.class))).thenAnswer(inv -> {
            Course c = inv.getArgument(0);
            c.setId(100L);
            return c;
        });

        when(courseSubjectRepository.save(any(CourseSubject.class))).thenAnswer(inv -> {
            CourseSubject cs = inv.getArgument(0);
            cs.setId(cs.getSubjectId() + 500L);
            return cs;
        });

        Subject s1 = new Subject();
        s1.setId(1L);
        s1.setName("Nước rút 400m");

        Subject s2 = new Subject();
        s2.setId(2L);
        s2.setName("Hồi phục 15m");

        when(subjectRepository.findById(1L)).thenReturn(Optional.of(s1));
        when(subjectRepository.findById(2L)).thenReturn(Optional.of(s2));

        // Act
        CourseDetailResponse res = courseService.createCourse(req, trainerUser);

        // Assert
        assertNotNull(res);
        assertEquals(100L, res.getCourse().getId());
        assertEquals("Khóa bứt tốc 1200m", res.getCourse().getName());
        assertEquals(CourseStatus.ACTIVE, res.getCourse().getStatus());
        assertEquals(2, res.getSubjects().size());
        verify(courseRepository).save(any(Course.class));
        verify(courseSubjectRepository, times(2)).save(any(CourseSubject.class));
    }

    @Test
    @DisplayName("5. Chặn tạo khoá học nếu trùng tên khoá học đã tồn tại")
    void testCreateCourse_DuplicateName_ThrowsIllegalArgument() {
        CreateCourseRequest req = new CreateCourseRequest();
        req.setName("Khóa đã có");

        when(courseRepository.existsByName("Khóa đã có")).thenReturn(true);

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> courseService.createCourse(req, trainerUser));
        assertTrue(ex.getMessage().contains("đã tồn tại"));
    }

    @Test
    @DisplayName("6. Chặn tạo khoá học nếu bài tập gán kèm không tồn tại trong hệ thống")
    void testCreateCourse_SubjectNotFound_ThrowsIllegalArgument() {
        CreateCourseRequest req = new CreateCourseRequest();
        req.setName("Khóa tập thử");
        CourseSubjectItemRequest item = new CourseSubjectItemRequest();
        item.setSubjectId(999L);
        req.setSubjects(List.of(item));

        when(courseRepository.existsByName("Khóa tập thử")).thenReturn(false);
        when(courseRepository.save(any(Course.class))).thenAnswer(inv -> {
            Course c = inv.getArgument(0);
            c.setId(101L);
            return c;
        });
        when(subjectRepository.existsById(999L)).thenReturn(false);

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> courseService.createCourse(req, trainerUser));
        assertTrue(ex.getMessage().contains("Subject not found"));
    }

    @Test
    @DisplayName("7. Thêm bài tập vào khoá học thành công")
    void testAddSubjectToCourse_Success() {
        Long courseId = 5L;
        CourseSubjectItemRequest req = new CourseSubjectItemRequest();
        req.setSubjectId(20L);
        req.setOrderIndex(3);

        when(courseRepository.existsById(courseId)).thenReturn(true);

        Subject subject = new Subject();
        subject.setId(20L);
        subject.setName("Phi nước đại");
        when(subjectRepository.findById(20L)).thenReturn(Optional.of(subject));

        when(courseSubjectRepository.save(any(CourseSubject.class))).thenAnswer(inv -> {
            CourseSubject cs = inv.getArgument(0);
            cs.setId(777L);
            return cs;
        });

        CourseSubjectResponse res = courseService.addSubjectToCourse(courseId, req);

        assertNotNull(res);
        assertEquals(777L, res.getId());
        assertEquals("Phi nước đại", res.getSubjectName());
        assertEquals(3, res.getOrderIndex());
    }

    @Test
    @DisplayName("8. Thêm bài tập vào khoá học thất bại khi khoá học không tồn tại")
    void testAddSubjectToCourse_CourseNotFound_ThrowsRuntimeException() {
        CourseSubjectItemRequest req = new CourseSubjectItemRequest();
        req.setSubjectId(1L);

        when(courseRepository.existsById(999L)).thenReturn(false);

        assertThrows(RuntimeException.class,
                () -> courseService.addSubjectToCourse(999L, req));
    }
}
