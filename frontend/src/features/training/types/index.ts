export type SurfaceType = 'TURF' | 'DIRT' | 'SYNTHETIC';
export type IntensityLevel = 'LOW' | 'MEDIUM' | 'HIGH';

export type TrainingDay =
  | 'MONDAY'
  | 'TUESDAY'
  | 'WEDNESDAY'
  | 'THURSDAY'
  | 'FRIDAY'
  | 'SATURDAY'
  | 'SUNDAY';

export interface SubjectCategory {
  id: number;
  name: string;
  description: string | null;
}

export interface Subject {
  id: number;
  categoryId: number;
  name: string;
  description: string | null;
  surfaceType: SurfaceType;
  targetDistanceMeters: number;
  intensityLevel: IntensityLevel;
  durationMinutes: number;
}

export interface CreateSubjectRequest {
  categoryId: number;
  name: string;
  description?: string;
  surfaceType: SurfaceType;
  targetDistanceMeters: number;
  intensityLevel: IntensityLevel;
  durationMinutes: number;
}

export interface CourseSubjectItem {
  subjectId: number;
  orderIndex: number;
}

export interface Course {
  id: number;
  name: string;
  description: string | null;
  targetGoal: string | null;
  totalSessions: number;
}

export interface CourseSubjectResponse {
  id: number;
  courseId: number;
  subjectId: number;
  orderIndex: number;
  subjectName: string;
  durationMinutes: number;
}

export interface CourseDetailResponse {
  course: Course;
  subjects: CourseSubjectResponse[];
}

export interface CreateCourseRequest {
  name: string;
  description?: string;
  targetGoal?: string;
  totalSessions: number;
  subjects: CourseSubjectItem[];
}

export interface HorseEnrollmentItem {
  horseId: number;
  groomId?: number | null;
}

export interface CreateHorseTrainingPlanRequest {
  courseId: number;
  startDate: string; // YYYY-MM-DD
  trainingDays: TrainingDay[];
  notes?: string;
  horses: HorseEnrollmentItem[];
}

export interface JoinableCohortResponse {
  cohortStatus: 'UPCOMING' | 'ACTIVE';
  courseId: number;
  courseName: string;
  suggestedStartDate: string;
  trainingDays: TrainingDay[];
  horseCount: number;
  /**
   * Số buổi ĐỒNG PHA — cùng ngày, cùng bài tập.
   * KHÔNG phải "số buổi chung lot": đồng pha chỉ là điều kiện CẦN.
   * Nhóm vẫn bị tách lot nếu vượt sức chứa (BR-10) hoặc trùng Groom (BR-09).
   */
  sharedSessions: number;
  totalSessions: number;
  waitDays: number;
  /** Sức chứa một lot, backend trả về để không phải hardcode số 6. */
  lotCapacity: number;
  note: string;
}

export interface PlanWorkoutItemResponse {
  workoutId: number;
  lotId: number;
  lotDate: string;
  startTime: string;
  endTime: string;
  subjectId: number;
  subjectName: string;
  horseId: number;
  /** Người dắt RIÊNG con ngựa này — KHÔNG phải "groom của lot". */
  assignedGroomId: number | null;
  /** Tổng số chiến mã còn hiệu lực trong lot, để thấy cơ chế ghép nhóm. */
  lotOccupancy: number | null;
  status: 'SCHEDULED' | 'COMPLETED' | 'CANCELLED';
  actualDistanceMeters: number | null;
  averageHeartRate: number | null;
  maxHeartRate: number | null;
  topSpeedKmh: number | null;
  performanceRating: number | null;
  trainerFeedback: string | null;
}

export interface HorseTrainingPlan {
  id: number;
  horseId: number;
  courseId: number;
  startDate: string;
  endDate: string;
  status: 'UPCOMING' | 'ACTIVE' | 'COMPLETED' | 'CANCELLED';
  notes: string | null;
}

export interface HorseTrainingPlanDetailResponse {
  plan: HorseTrainingPlan;
  workouts: PlanWorkoutItemResponse[];
  horseName: string | null;
  courseName: string | null;
}

/** Một dòng trong danh sách kế hoạch huấn luyện. */
export interface PlanSummaryResponse {
  planId: number;
  horseId: number;
  horseName: string;
  courseId: number;
  courseName: string;
  startDate: string;
  endDate: string;
  status: 'UPCOMING' | 'ACTIVE' | 'COMPLETED' | 'CANCELLED';
  totalSessions: number;
  completedSessions: number;
  /** Buổi đã huỷ — bị trừ khỏi mẫu số khi tính tiến độ. */
  cancelledSessions: number;
}

export interface CompleteWorkoutRequest {
  actualDistanceMeters?: number | null;
  actualDurationMinutes?: number | null;
  topSpeedKmh?: number | null;
  averageSpeedKmh?: number | null;
  averageHeartRate?: number | null;
  maxHeartRate?: number | null;
  recoveryHeartRate?: number | null;
  performanceRating: number; // 1-10
  trainerFeedback?: string | null;
  videoUrl?: string | null;
}

export interface TrainingLotResponse {
  lotId: number;
  lotDate: string;
  startTime: string; // "06:00:00"
  endTime: string;   // "07:30:00"
  subjectId: number;
  subjectName: string;
  durationMinutes: number;
  status: 'SCHEDULED' | 'COMPLETED' | 'CANCELLED';
  maxCapacity: number;
  occupied: number;
  remainingSlots: number;
  horseNames: string[];
}

export interface RescheduleLotRequest {
  newStartTime: string; // "HH:mm" or "HH:mm:ss"
  reason?: string;
}

export interface HorseFitnessTrendItem {
  workoutId: number;
  date: string; // YYYY-MM-DD
  subjectName: string;
  distanceMeters: number | null;
  averageSpeedKmh: number | null;
  topSpeedKmh: number | null;
  averageHeartRate: number | null;
  maxHeartRate: number | null;
  recoveryHeartRate: number | null;
  performanceRating: number | null;
}

export interface HorseAlert {
  ruleCode: string;
  severity: 'WARNING' | 'DANGER';
  title: string;
  description: string;
  triggeredAt: string;
  metricValue: number;
  thresholdValue: number;
}

export interface TrainerDashboardHorse {
  horseId: number;
  horseName: string;
  breed?: string;
  stallCode?: string;
  planId: number | null;
  courseName: string | null;
  planStatus: string;
  completedSessions: number;
  totalSessions: number;
  progressPercent: number;
  latestPerformanceRating: number | null;
  avgPerformanceRating30d: number | null;
  alertCount: number;
  alertsCount?: number;
  alertTitles?: string[];
  startDate?: string;
  endDate?: string;
}

export interface ReadinessAssessment {
  id: number;
  horseId: number;
  readinessStatus: string;
  fitnessScore: number | null;
  conformationScore: number | null;
  temperamentScore: number | null;
  gaitQualityScore: number | null;
  estimatedMonthsToRace: number | null;
  assessmentDate: string;
}

