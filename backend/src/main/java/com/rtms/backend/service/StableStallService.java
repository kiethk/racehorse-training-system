package com.rtms.backend.service;

import com.rtms.backend.config.FarmSchedulePolicy;
import com.rtms.backend.entity.StableStall;
import com.rtms.backend.entity.User;
import com.rtms.backend.repository.StableStallRepository;
import com.rtms.backend.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class StableStallService {

    private final StableStallRepository stableStallRepository;
    private final UserRepository userRepository;

    public StableStallService(StableStallRepository stableStallRepository,
                              UserRepository userRepository) {
        this.stableStallRepository = stableStallRepository;
        this.userRepository = userRepository;
    }

    /**
     * BR-06 — Mỗi Groom tối đa 3 chuồng.
     *
     * Căn cứ thực tế: nhân viên chuồng trại ở lò đua thường chăm 2–3 con mỗi
     * ngày. Con số này cũng ăn khớp với số lot mỗi sáng (2–3 lot trong khung
     * 06:00–10:00): nếu cả 3 con cùng tập một ngày, chúng vừa đủ xếp vào 3 lot
     * khác nhau mà không vi phạm BR-09 (một Groom không dắt 2 ngựa cùng lot).
     */
    @Transactional
    public StableStall assignGroom(Long stallId, Long groomId) {
        StableStall stall = stableStallRepository.findById(stallId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy chuồng #" + stallId));

        // TH1: gỡ Groom khỏi chuồng -> không cần kiểm tra gì
        if (groomId == null) {
            stall.setGroomId(null);
            return stableStallRepository.save(stall);
        }

        // TH2: gán lại ĐÚNG Groom đang giữ chuồng này -> không tính thêm 1
        if (groomId.equals(stall.getGroomId())) {
            return stall;
        }

        // TH3: kiểm tra người được gán đúng là GROOM
        User user = userRepository.findById(groomId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy người dùng #" + groomId));

        if (user.getRole() == null || !"GROOM".equalsIgnoreCase(user.getRole().getName())) {
            throw new IllegalArgumentException(String.format(
                    "Người dùng '%s' có vai trò %s, không phải GROOM — không thể gán vào chuồng!",
                    user.getFullName(),
                    user.getRole() != null ? user.getRole().getName() : "KHÔNG XÁC ĐỊNH"));
        }

        // TH4: BR-06
        long assignedCount = stableStallRepository.countByGroomId(groomId);
        if (assignedCount >= FarmSchedulePolicy.MAX_STALLS_PER_GROOM) {
            throw new IllegalStateException(String.format(
                    "Groom '%s' đã phụ trách %d chuồng. Mỗi Groom chỉ được nhận tối đa %d chuồng "
                  + "để đảm bảo chất lượng chăm sóc và phúc lợi chiến mã!",
                    user.getFullName(), assignedCount, FarmSchedulePolicy.MAX_STALLS_PER_GROOM));
        }

        stall.setGroomId(groomId);
        return stableStallRepository.save(stall);
    }
}
