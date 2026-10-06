package com.rtms.backend.service;
import com.rtms.backend.entity.Horse;
import com.rtms.backend.enums.HorseStatus;
import com.rtms.backend.repository.HorseRepository;
import com.rtms.backend.security.AuthenticatedUser;
import com.rtms.backend.dto.CreateRaceRegistrationRequest;
import com.rtms.backend.dto.RaceRegistrationResponse;
import com.rtms.backend.dto.ReviewRaceRegistrationRequest;
import com.rtms.backend.entity.RaceRegistration;
import com.rtms.backend.repository.RaceRegistrationRepository;
import com.rtms.backend.config.ApiException;
import com.rtms.backend.entity.Area;
import com.rtms.backend.entity.StableStall;
import com.rtms.backend.repository.AreaRepository;
import com.rtms.backend.repository.StableStallRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RaceRegistrationServiceTest {

    @Mock
    private RaceRegistrationRepository raceRegistrationRepository;

    @Mock
    private HorseRepository horseRepository;

    @Mock
    private StableStallRepository stableStallRepository;

    @Mock
    private AreaRepository areaRepository;

    private RaceRegistrationService raceRegistrationService;

    private final AuthenticatedUser trainerUser = new AuthenticatedUser(10L, "trainer@rtms.vn", "HEAD_TRAINER");
    private final AuthenticatedUser otherTrainer = new AuthenticatedUser(99L, "other@rtms.vn", "HEAD_TRAINER");
    private final AuthenticatedUser managerUser = new AuthenticatedUser(1L, "manager@rtms.vn", "CLUB_MANAGER");

    @BeforeEach
    void setUp() {
        raceRegistrationService = new RaceRegistrationService(
                raceRegistrationRepository,
                horseRepository,
                stableStallRepository,
                areaRepository
        );
    }

    private Horse createEligibleHorse(Long id, Long stallId) {
        Horse h = new Horse();
        h.setId(id);
        h.setName("Bạch Long Mã");
        h.setRegistrationNumber("VN-2026-001");
        h.setCurrentStatus(HorseStatus.ELIGIBLE);
        h.setTrainingLocked(false);
        h.setCurrentStallId(stallId);
        return h;
    }

    private CreateRaceRegistrationRequest createValidRequest(Long horseId) {
        CreateRaceRegistrationRequest req = new CreateRaceRegistrationRequest();
        req.setHorseId(horseId);
        req.setRaceName("Giải Mùa Thu 2026");
        req.setRaceCategory("Hạng 3 tuổi - 1.200 m");
        req.setLocation("Trường đua A, Hà Nội");
        req.setEventDate(LocalDate.now().plusDays(30));
        req.setEventTime(LocalTime.of(8, 30));
        req.setOrganizer("Câu lạc bộ A");
        req.setSourceUrl("https://example.org/race-conditions");
        req.setNominationDeadline(LocalDate.now().plusDays(15));
        req.setDistanceMeters(new BigDecimal("1200"));
        req.setTrackType("Cỏ");
        req.setPrizeDetails("Nhất 50 triệu VND; nhì 25 triệu VND");
        req.setSelectionReason("Ngựa có kết quả tốt ở cự ly 1.200 m và đang tập ổn định.");
        req.setTrainerNotes("Cần xác minh điều kiện tuổi với ban tổ chức.");
        return req;
    }

    @Test
    @DisplayName("Nộp đơn hợp lệ -> Tạo bản ghi PENDING với trainerId của phiên đăng nhập")
    void testCreate_Valid_Success() {
        Long horseId = 42L;
        Long stallId = 5L;
        Long areaId = 2L;

        Horse horse = createEligibleHorse(horseId, stallId);
        StableStall stall = new StableStall();
        stall.setId(stallId);
        stall.setAreaId(areaId);

        Area area = new Area();
        area.setId(areaId);
        area.setTrainerId(10L); // Khớp trainerId của trainerUser

        when(horseRepository.findById(horseId)).thenReturn(Optional.of(horse));
        when(stableStallRepository.findById(stallId)).thenReturn(Optional.of(stall));
        when(areaRepository.findById(areaId)).thenReturn(Optional.of(area));
        when(raceRegistrationRepository.save(any(RaceRegistration.class))).thenAnswer(inv -> {
            RaceRegistration saved = inv.getArgument(0);
            saved.setId(100L);
            return saved;
        });

        CreateRaceRegistrationRequest req = createValidRequest(horseId);
        RaceRegistrationResponse res = raceRegistrationService.create(req, trainerUser);

        assertNotNull(res);
        assertEquals(100L, res.getId());
        assertEquals("PENDING", res.getStatus());
        assertEquals(10L, res.getTrainerId());
        assertEquals("Bạch Long Mã", res.getHorseName());
        assertEquals("VN-2026-001", res.getHorseRegistrationNumber());
        assertEquals("Giải Mùa Thu 2026", res.getRaceName());
        assertEquals("Hạng 3 tuổi - 1.200 m", res.getRaceCategory());
        assertEquals("https://example.org/race-conditions", res.getSourceUrl());
        verify(raceRegistrationRepository).save(any(RaceRegistration.class));
    }

    @Test
    @DisplayName("Nộp đơn cho ngựa không tồn tại -> 404 RESOURCE_NOT_FOUND")
    void testCreate_HorseNotFound_ThrowsException() {
        when(horseRepository.findById(999L)).thenReturn(Optional.empty());

        CreateRaceRegistrationRequest req = createValidRequest(999L);
        ApiException ex = assertThrows(ApiException.class, () -> raceRegistrationService.create(req, trainerUser));

        assertEquals(HttpStatus.NOT_FOUND, ex.getStatus());
        assertEquals("RESOURCE_NOT_FOUND", ex.getErrorCode());
    }

    @Test
    @DisplayName("Nộp đơn cho ngựa chưa xếp chuồng -> 400 HORSE_NOT_ASSIGNED")
    void testCreate_HorseNotAssignedStall_ThrowsException() {
        Horse horse = createEligibleHorse(42L, null);
        when(horseRepository.findById(42L)).thenReturn(Optional.of(horse));

        CreateRaceRegistrationRequest req = createValidRequest(42L);
        ApiException ex = assertThrows(ApiException.class, () -> raceRegistrationService.create(req, trainerUser));

        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatus());
        assertEquals("HORSE_NOT_ASSIGNED", ex.getErrorCode());
    }

    @Test
    @DisplayName("Nộp đơn cho ngựa thuộc khu Trainer khác -> 403 AccessDeniedException")
    void testCreate_HorseInOtherTrainerArea_ThrowsAccessDenied() {
        Long horseId = 42L;
        Long stallId = 5L;
        Long areaId = 2L;

        Horse horse = createEligibleHorse(horseId, stallId);
        StableStall stall = new StableStall();
        stall.setId(stallId);
        stall.setAreaId(areaId);

        Area area = new Area();
        area.setId(areaId);
        area.setTrainerId(888L); // Thuộc Trainer khác

        when(horseRepository.findById(horseId)).thenReturn(Optional.of(horse));
        when(stableStallRepository.findById(stallId)).thenReturn(Optional.of(stall));
        when(areaRepository.findById(areaId)).thenReturn(Optional.of(area));

        CreateRaceRegistrationRequest req = createValidRequest(horseId);
        assertThrows(AccessDeniedException.class, () -> raceRegistrationService.create(req, trainerUser));
    }

    @Test
    @DisplayName("Nộp đơn cho ngựa bị khoá huấn luyện hoặc không ELIGIBLE -> 400 HORSE_NOT_ELIGIBLE")
    void testCreate_HorseLockedOrNotEligible_ThrowsException() {
        Long horseId = 42L;
        Long stallId = 5L;
        Long areaId = 2L;

        Horse horse = createEligibleHorse(horseId, stallId);
        horse.setTrainingLocked(true); // Bị khoá huấn luyện

        StableStall stall = new StableStall();
        stall.setId(stallId);
        stall.setAreaId(areaId);

        Area area = new Area();
        area.setId(areaId);
        area.setTrainerId(10L);

        when(horseRepository.findById(horseId)).thenReturn(Optional.of(horse));
        when(stableStallRepository.findById(stallId)).thenReturn(Optional.of(stall));
        when(areaRepository.findById(areaId)).thenReturn(Optional.of(area));

        CreateRaceRegistrationRequest req = createValidRequest(horseId);
        ApiException ex = assertThrows(ApiException.class, () -> raceRegistrationService.create(req, trainerUser));

        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatus());
        assertEquals("HORSE_NOT_ELIGIBLE", ex.getErrorCode());
    }

    @Test
    @DisplayName("Nộp đơn với ngày thi đấu trong quá khứ -> 400 INVALID_EVENT_DATE")
    void testCreate_PastEventDate_ThrowsException() {
        Long horseId = 42L;
        Long stallId = 5L;
        Long areaId = 2L;

        Horse horse = createEligibleHorse(horseId, stallId);
        StableStall stall = new StableStall();
        stall.setId(stallId);
        stall.setAreaId(areaId);

        Area area = new Area();
        area.setId(areaId);
        area.setTrainerId(10L);

        when(horseRepository.findById(horseId)).thenReturn(Optional.of(horse));
        when(stableStallRepository.findById(stallId)).thenReturn(Optional.of(stall));
        when(areaRepository.findById(areaId)).thenReturn(Optional.of(area));

        CreateRaceRegistrationRequest req = createValidRequest(horseId);
        req.setEventDate(LocalDate.now().minusDays(1)); // Quá khứ

        ApiException ex = assertThrows(ApiException.class, () -> raceRegistrationService.create(req, trainerUser));
        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatus());
        assertEquals("INVALID_EVENT_DATE", ex.getErrorCode());
    }

    @Test
    @DisplayName("Hạn nộp hồ sơ sau ngày thi đấu -> 400 INVALID_DEADLINE")
    void testCreate_DeadlineAfterEventDate_ThrowsException() {
        Long horseId = 42L;
        Long stallId = 5L;
        Long areaId = 2L;

        Horse horse = createEligibleHorse(horseId, stallId);
        StableStall stall = new StableStall();
        stall.setId(stallId);
        stall.setAreaId(areaId);

        Area area = new Area();
        area.setId(areaId);
        area.setTrainerId(10L);

        when(horseRepository.findById(horseId)).thenReturn(Optional.of(horse));
        when(stableStallRepository.findById(stallId)).thenReturn(Optional.of(stall));
        when(areaRepository.findById(areaId)).thenReturn(Optional.of(area));

        CreateRaceRegistrationRequest req = createValidRequest(horseId);
        req.setEventDate(LocalDate.now().plusDays(10));
        req.setNominationDeadline(LocalDate.now().plusDays(15)); // Sau ngày thi

        ApiException ex = assertThrows(ApiException.class, () -> raceRegistrationService.create(req, trainerUser));
        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatus());
        assertEquals("INVALID_DEADLINE", ex.getErrorCode());
    }

    @Test
    @DisplayName("Link nguồn không phải http/https -> 400 INVALID_SOURCE_URL")
    void testCreate_InvalidSourceUrl_ThrowsException() {
        Long horseId = 42L;
        Long stallId = 5L;
        Long areaId = 2L;

        Horse horse = createEligibleHorse(horseId, stallId);
        StableStall stall = new StableStall();
        stall.setId(stallId);
        stall.setAreaId(areaId);

        Area area = new Area();
        area.setId(areaId);
        area.setTrainerId(10L);

        when(horseRepository.findById(horseId)).thenReturn(Optional.of(horse));
        when(stableStallRepository.findById(stallId)).thenReturn(Optional.of(stall));
        when(areaRepository.findById(areaId)).thenReturn(Optional.of(area));

        CreateRaceRegistrationRequest req = createValidRequest(horseId);
        req.setSourceUrl("ftp://invalid-url.com");

        ApiException ex = assertThrows(ApiException.class, () -> raceRegistrationService.create(req, trainerUser));
        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatus());
        assertEquals("INVALID_SOURCE_URL", ex.getErrorCode());
    }

    @Test
    @DisplayName("listVisible: Trainer chỉ thấy đơn do chính mình tạo")
    void testListVisible_Trainer_OnlySeesOwnProposals() {
        RaceRegistration reg = new RaceRegistration();
        reg.setId(1L);
        reg.setTrainerId(10L);
        reg.setHorseId(42L);

        when(raceRegistrationRepository.findByTrainerIdOrderByCreatedAtDesc(10L))
                .thenReturn(List.of(reg));
        when(horseRepository.findAllById(anySet()))
                .thenReturn(List.of(createEligibleHorse(42L, 5L)));

        List<RaceRegistrationResponse> list = raceRegistrationService.listVisible(trainerUser, null);

        assertEquals(1, list.size());
        assertEquals(10L, list.get(0).getTrainerId());
        verify(raceRegistrationRepository).findByTrainerIdOrderByCreatedAtDesc(10L);
        verify(raceRegistrationRepository, never()).findAllByOrderByCreatedAtDesc();
    }

    @Test
    @DisplayName("listVisible: Manager thấy tất cả đơn")
    void testListVisible_Manager_SeesAllProposals() {
        RaceRegistration reg1 = new RaceRegistration();
        reg1.setId(1L);
        reg1.setTrainerId(10L);
        reg1.setHorseId(42L);

        RaceRegistration reg2 = new RaceRegistration();
        reg2.setId(2L);
        reg2.setTrainerId(99L);
        reg2.setHorseId(43L);

        when(raceRegistrationRepository.findAllByOrderByCreatedAtDesc())
                .thenReturn(List.of(reg1, reg2));
        when(horseRepository.findAllById(anySet()))
                .thenReturn(List.of(createEligibleHorse(42L, 5L), createEligibleHorse(43L, 6L)));

        List<RaceRegistrationResponse> list = raceRegistrationService.listVisible(managerUser, null);

        assertEquals(2, list.size());
        verify(raceRegistrationRepository).findAllByOrderByCreatedAtDesc();
    }

    @Test
    @DisplayName("getVisible: Trainer xem đơn của Trainer khác -> 403 AccessDeniedException")
    void testGetVisible_OtherTrainerProposal_ThrowsAccessDenied() {
        RaceRegistration reg = new RaceRegistration();
        reg.setId(50L);
        reg.setTrainerId(99L); // Của trainer khác
        reg.setHorseId(42L);

        when(raceRegistrationRepository.findById(50L)).thenReturn(Optional.of(reg));

        assertThrows(AccessDeniedException.class, () -> raceRegistrationService.getVisible(50L, trainerUser));
    }

    @Test
    @DisplayName("getVisible: Manager có thể xem đơn của bất kỳ Trainer nào")
    void testGetVisible_Manager_CanViewAnyProposal() {
        RaceRegistration reg = new RaceRegistration();
        reg.setId(50L);
        reg.setTrainerId(99L);
        reg.setHorseId(42L);

        when(raceRegistrationRepository.findById(50L)).thenReturn(Optional.of(reg));
        when(horseRepository.findById(42L)).thenReturn(Optional.of(createEligibleHorse(42L, 5L)));

        RaceRegistrationResponse res = raceRegistrationService.getVisible(50L, managerUser);
        assertNotNull(res);
        assertEquals(50L, res.getId());
    }

    @Test
    @DisplayName("review: Manager duyệt đơn PENDING -> Thành APPROVED với feedback")
    void testReview_Manager_Success() {
        RaceRegistration reg = new RaceRegistration();
        reg.setId(50L);
        reg.setStatus("PENDING");
        reg.setHorseId(42L);
        reg.setTrainerId(10L);

        when(raceRegistrationRepository.findById(50L)).thenReturn(Optional.of(reg));
        when(raceRegistrationRepository.save(any(RaceRegistration.class))).thenAnswer(i -> i.getArgument(0));
        when(horseRepository.findById(42L)).thenReturn(Optional.of(createEligibleHorse(42L, 5L)));

        ReviewRaceRegistrationRequest req = new ReviewRaceRegistrationRequest();
        req.setStatus("APPROVED");
        req.setManagerFeedback("Đã duyệt cho chiến mã tham gia.");

        RaceRegistrationResponse res = raceRegistrationService.review(50L, req, managerUser);

        assertEquals("APPROVED", res.getStatus());
        assertEquals("Đã duyệt cho chiến mã tham gia.", res.getManagerFeedback());
        assertEquals(1L, res.getReviewedById());
        assertNotNull(res.getReviewedAt());
    }
}
