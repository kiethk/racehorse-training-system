package com.rtms.backend.config;

/**
 * Giới hạn kích thước của dữ liệu huấn luyện: tên, mô tả, số buổi của khóa, số môn, số ngựa mỗi lần ghi danh và tầm nhìn
 * của lịch. Mỗi giới hạn có MỘT chỗ khai báo ở đây; request kiểm tra bằng Bean Validation, giao diện đọc lại qua
 * GET /api/training-rules, còn ràng buộc ở DB dùng cùng con số (xem migration V202610251100).
 * (Size limits of the training data: names, descriptions, the number of sessions of a course, the number of exercises, the
 * number of horses per enrollment and the horizon of the calendar. Each limit is declared ONCE here; requests check it with Bean
 * Validation, the UI reads it back through GET /api/training-rules, and the DB constraints use the same numbers (see migration
 * V202610251100).)
 *
 * Vì sao phải có trần: số buổi của khóa quyết định số vòng lặp tính lịch và số dòng sinh ra (ghi danh N ngựa vào khóa M buổi
 * ghi N x M buổi tập). Không có trần thì một con số gõ nhầm, như 1000000, làm máy chủ đứng.
 * (Why a ceiling: the number of sessions drives the calendar loops and the rows generated (enrolling N horses in an M-session
 * course writes N x M workouts). Without one, a mistyped number such as 1000000 freezes the server.)
 */
public final class TrainingLimits {

    private TrainingLimits() {
        // lớp tiện ích, không cho khởi tạo (utility class)
    }

    /** Độ dài tối đa của tên môn và tên khóa, bằng độ rộng cột VARCHAR(255). (Longest exercise or course name, the VARCHAR(255) width.) */
    public static final int NAME_MAX_LENGTH = 255;

    public static final int DESCRIPTION_MAX_LENGTH = 2000;
    public static final int GOAL_MAX_LENGTH = 1000;

    /** Thời lượng một môn, tính bằng phút; nó quyết định độ dài của session. (Length of an exercise in minutes; it sets the length of the session.) */
    public static final int MIN_EXERCISE_DURATION_MINUTES = 15;
    public static final int MAX_EXERCISE_DURATION_MINUTES = 240;

    /** Cự ly mục tiêu của một môn, tính bằng mét. (Target distance of an exercise, in meters.) */
    public static final int MIN_EXERCISE_DISTANCE_METERS = 100;
    public static final int MAX_EXERCISE_DISTANCE_METERS = 10_000;

    /** Số buổi tối đa của một khóa (khoảng hai tháng tập hằng ngày). (Most sessions of one course, about two months of daily training.) */
    public static final int MIN_COURSE_SESSIONS = 1;
    public static final int MAX_COURSE_SESSIONS = 60;

    /** Số môn tối đa trong một khóa; nhiều hơn số buổi thì có môn không bao giờ được tập. (Most exercises in one course.) */
    public static final int MAX_COURSE_EXERCISES = 30;

    /** Số ngựa tối đa của một lần ghi danh. (Most horses in one enrollment.) */
    public static final int MAX_HORSES_PER_COURSE_RUN = 30;

    /** Ngày bắt đầu kế hoạch không được xa hơn số ngày này kể từ hôm nay. (A plan may not start further than this from today.) */
    public static final int MAX_START_HORIZON_DAYS = 365;

    /** Độ dài tối đa của tên nhóm huấn luyện. (Longest name of a training group.) */
    public static final int GROUP_NAME_MAX_LENGTH = 100;

    /** Số chiến mã tối đa của một nhóm huấn luyện (BR-10). (Most horses in one training group (BR-10).) */
    public static final int MAX_HORSES_PER_GROUP = 6;
}
