package com.rtms.backend.training.service;

/**
 * Khóa lịch của một Trainer. Mọi thao tác đặt, dời hay hủy buổi của cùng một Trainer dùng CHUNG một khóa, nên hai request đồng thời
 * không cùng nhìn thấy một khe trống rồi cùng chiếm nó.
 * (The schedule lock of one Trainer. Every operation that places, moves or cancels the Sessions of the same Trainer uses the SAME
 * lock, so two simultaneous requests never see the same free slot and both take it.)
 */
final class ScheduleLocks {

    /** Tên miền của khóa, để khóa của Trainer không đụng khóa advisory của chức năng khác. (Namespace of the lock.) */
    private static final long NAMESPACE = 0x5254_4D53_0001_0000L;

    private ScheduleLocks() {
        // lớp tiện ích, không cho khởi tạo (utility class)
    }

    static long of(Long trainerId) {
        return NAMESPACE + trainerId;
    }
}
