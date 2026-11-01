export type PlanStatus = 'UPCOMING' | 'ACTIVE' | 'COMPLETED' | 'CANCELLED';
export type WorkoutStatus = 'SCHEDULED' | 'HELD' | 'CANCELLED';
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

/** Group of a subject. A plain enum like surface type and intensity: it has no table of its own. */
export type SubjectCategory = 'ENDURANCE' | 'SPEED' | 'GATE_START' | 'RECOVERY';

export interface Subject {
  id: number;
  trainerId?: number;
  category: SubjectCategory;
  name: string;
  description: string | null;
  surfaceType: SurfaceType;
  targetDistanceMeters: number;
  intensityLevel: IntensityLevel;
  durationMinutes: number;
}

export interface CreateSubjectRequest {
  category: SubjectCategory;
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
  trainerId?: number;
  name: string;
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
  targetGoal?: string;
  totalSessions: number;
  subjects: CourseSubjectItem[];
}

/** Whether a horse can join the plan being previewed. Mirrors the backend enum PlanAvailability. */
export interface CompleteWorkoutRequest {
  actualDistanceMeters: number;
  actualDurationMinutes: number;
  topSpeedKmh: number;
  averageSpeedKmh: number;
  averageHeartRate: number;
  maxHeartRate: number;
  recoveryHeartRate: number;
  performanceRating: number; // 1-10
  trainerFeedback?: string | null;
}

/** A fresh, non-persistent measurement set returned when the result form is opened. */
export interface WorkoutResultPreviewResponse {
  actualDistanceMeters: number;
  actualDurationMinutes: number;
  topSpeedKmh: number;
  averageSpeedKmh: number;
  averageHeartRate: number;
  maxHeartRate: number;
  recoveryHeartRate: number;
  performanceRating?: number;
}

/** One session of a horse in its schedule. The session and the horse's own workout can each be cancelled. */
export interface HorseScheduleItem {
  sessionId: number;
  sessionDate: string; // YYYY-MM-DD
  startTime: string; // "06:00:00"
  endTime: string;
  subjectName: string;
  sessionStatus: SessionStatus;
  workoutStatus: WorkoutStatus;
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

/** Training rules the backend enforces, published so the UI does not hard-code them. */
export interface TrainingRules {
  goldenHoursStart: string; // "06:00:00"
  goldenHoursEnd: string;   // "10:00:00"
  /** The most horses one training group holds. */
  groupCapacity: number;
  /** A session only starts on multiples of this many minutes (00, 15, 30, 45). */
  sessionStartStepMinutes: number;
  minSubjectDurationMinutes: number;
  maxSubjectDurationMinutes: number;
  maxCourseSessions: number;
  maxCourseSubjects: number;
  maxHorsesPerPlan: number;
  /** A maximum heart rate above this raises an alert. */
  maxHeartRateLimit: number;
  /** A recovery heart rate above this raises an alert. */
  recoveryHeartRateLimit: number;
}


// ---------------------------------------------------------------------------
// Training groups
// ---------------------------------------------------------------------------

export interface GroupMember {
  horseId: number;
  horseName: string;
  groomId: number | null;
  groomName: string | null;
  joinedAt: string;
}

export interface TrainingGroup {
  id: number;
  name: string;
  trainerId: number;
  members: GroupMember[];
  currentCourseName: string | null;
  currentPlanStatus: PlanStatus | null;
  currentPlanEndDate: string | null;
  disbanded: boolean;
}

export interface GroupCandidate {
  horseId: number;
  horseName: string;
  groomName: string | null;
  joinable: boolean;
  reason: string | null;
}

export interface CreateTrainingGroupRequest {
  name: string;
  horseIds?: number[];
}

// ---------------------------------------------------------------------------
// Group plans
// ---------------------------------------------------------------------------

export type SessionStatus = 'SCHEDULED' | 'HELD' | 'CANCELLED';

export interface GroupPlanRequest {
  groupId: number;
  courseId: number;
  startDate: string;
  trainingDays: TrainingDay[];
}

export interface PlannedSession {
  date: string;
  subjectName: string;
  durationMinutes: number;
  /** Null when the day has no free slot. */
  startTime: string | null;
  endTime: string | null;
}

export interface PlanOverlap {
  planId: number;
  courseName: string;
  startDate: string;
  endDate: string;
  status: PlanStatus;
}

export interface GroupPlanPreview {
  startDate: string;
  endDate: string;
  memberCount: number;
  sessions: PlannedSession[];
  fullDates: string[];
  overlaps: PlanOverlap[];
  earliestFreeStart: string;
  canCreate: boolean;
}

export interface GroupPlanSummary {
  id: number;
  groupId: number | null;
  groupName: string | null;
  groupDisbanded: boolean;
  courseId: number;
  courseName: string;
  startDate: string;
  endDate: string;
  status: PlanStatus;
  sessionsHeld: number;
  sessionsCancelled: number;
  sessionsTotal: number;
}

export interface PlanSessionItem {
  id: number;
  date: string;
  startTime: string;
  endTime: string;
  subjectName: string;
  status: SessionStatus;
  horseCount: number;
}

export interface GroupPlanDetail {
  plan: GroupPlanSummary;
  trainingDays: TrainingDay[];
  sessions: PlanSessionItem[];
}

// ---------------------------------------------------------------------------
// Group sessions (schedule, results, attendance)
// ---------------------------------------------------------------------------

export interface ScheduleSession {
  id: number;
  planId: number;
  groupId: number | null;
  groupName: string | null;
  groupDisbanded: boolean;
  courseName: string;
  subjectName: string;
  date: string;
  startTime: string;
  endTime: string;
  status: SessionStatus;
  horseCount: number;
  /** Held Workouts that still have no recorded result. */
  needsResultCount: number;
}

export interface SessionHorse {
  workoutId: number;
  horseId: number;
  horseName: string;
  groomName: string | null;
  status: 'SCHEDULED' | 'HELD' | 'CANCELLED';
  hasResult: boolean;
  performanceRating: number | null;
}

export interface SessionDetail {
  session: ScheduleSession;
  horses: SessionHorse[];
}

export interface SessionWorkoutResult {
  id: number;
  sessionId: number;
  horseId: number;
  status: 'SCHEDULED' | 'HELD' | 'CANCELLED';
  hasResult: boolean;
  actualDistanceMeters: number | null;
  actualDurationMinutes: number | null;
  topSpeedKmh: number | null;
  averageSpeedKmh: number | null;
  averageHeartRate: number | null;
  maxHeartRate: number | null;
  recoveryHeartRate: number | null;
  performanceRating: number | null;
  trainerFeedback: string | null;
}

export interface HorseAttendance {
  planId: number;
  groupName: string | null;
  groupDisbanded: boolean;
  courseName: string;
  planStatus: PlanStatus;
  startDate: string;
  endDate: string;
  attended: number;
  total: number;
  joinedOn: string;
}
