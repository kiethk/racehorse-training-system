export type SurfaceType = 'TURF' | 'DIRT' | 'SYNTHETIC';
export type IntensityLevel = 'LOW' | 'MEDIUM' | 'HIGH';
export type WorkoutType = 'REGULAR' | 'GATE_PRACTICE' | 'BREEZING' | 'SWIMMING' | 'RECOVERY';

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
  workoutType: WorkoutType;
}

export interface CreateSubjectRequest {
  categoryId: number;
  name: string;
  description?: string;
  surfaceType: SurfaceType;
  targetDistanceMeters: number;
  intensityLevel: IntensityLevel;
  durationMinutes: number;
  workoutType?: WorkoutType;
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
  sharedSessions: number;
  totalSessions: number;
  waitDays: number;
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
  workoutType: string;
  horseId: number;
  assignedGroomId: number | null;
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
  status: 'SCHEDULED' | 'IN_PROGRESS' | 'COMPLETED' | 'CANCELLED';
  maxCapacity: number;
  occupied: number;
  remainingSlots: number;
  horseNames: string[];
}

export interface RescheduleLotRequest {
  newStartTime: string; // "HH:mm" or "HH:mm:ss"
  reason?: string;
}

