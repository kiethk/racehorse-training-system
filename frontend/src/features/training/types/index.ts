export type CourseRunStatus = 'UPCOMING' | 'ACTIVE' | 'COMPLETED' | 'CANCELLED';
/** HELD: the Session took place but the Workout is not yet confirmed; RAN: results recorded, so the horse took part. */
export type WorkoutStatus = 'SCHEDULED' | 'HELD' | 'RAN' | 'CANCELLED';
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

/** Group of an exercise. A plain enum like surface type and intensity: it has no table of its own. */
export type ExerciseCategory = 'ENDURANCE' | 'SPEED' | 'GATE_START' | 'RECOVERY';

export interface Exercise {
  id: number;
  trainerId?: number;
  category: ExerciseCategory;
  name: string;
  description: string | null;
  surfaceType: SurfaceType;
  targetDistanceMeters: number;
  intensityLevel: IntensityLevel;
  durationMinutes: number;
}

export interface CreateExerciseRequest {
  category: ExerciseCategory;
  name: string;
  description?: string;
  surfaceType: SurfaceType;
  targetDistanceMeters: number;
  intensityLevel: IntensityLevel;
  durationMinutes: number;
}

export interface CourseExerciseItem {
  exerciseId: number;
  orderIndex: number;
}

export interface Course {
  id: number;
  trainerId?: number;
  name: string;
  targetGoal: string | null;
  totalSessions: number;
}

export interface CourseExerciseResponse {
  id: number;
  courseId: number;
  exerciseId: number;
  orderIndex: number;
  exerciseName: string;
  durationMinutes: number;
}

export interface CourseDetailResponse {
  course: Course;
  exercises: CourseExerciseResponse[];
}

export interface CreateCourseRequest {
  name: string;
  targetGoal?: string;
  totalSessions: number;
  exercises: CourseExerciseItem[];
}

/** Whether a horse can join the course run being previewed. Mirrors the backend enum CourseRunAvailability. */
export interface CompleteWorkoutRequest {
  actualDistanceMeters: number;
  actualDurationMinutes: number;
  topSpeedKmh: number;
  averageSpeedKmh: number;
  averageHeartRate: number;
  maxHeartRate: number;
  recoveryHeartRate: number;
  trainerFeedback?: string | null;
}

/** A simulated measurement set returned only when the Trainer presses "Generate sample data"; never saved by itself. */
export interface WorkoutResultPreviewResponse {
  actualDistanceMeters: number;
  actualDurationMinutes: number;
  topSpeedKmh: number;
  averageSpeedKmh: number;
  averageHeartRate: number;
  maxHeartRate: number;
  recoveryHeartRate: number;
}

/** One session of a horse in its schedule. The session and the horse's own workout can each be cancelled. */
export interface HorseScheduleItem {
  sessionId: number;
  sessionDate: string; // YYYY-MM-DD
  startTime: string; // "06:00:00"
  endTime: string;
  exerciseName: string;
  sessionStatus: SessionStatus;
  workoutStatus: WorkoutStatus;
}

export interface HorseFitnessTrendItem {
  workoutId: number;
  date: string; // YYYY-MM-DD
  exerciseName: string;
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
  minExerciseDurationMinutes: number;
  maxExerciseDurationMinutes: number;
  maxCourseSessions: number;
  maxCourseExercises: number;
  maxHorsesPerCourseRun: number;
  /** A maximum heart rate above this raises an alert. */
  maxHeartRateLimit: number;
  /** An average heart rate above this raises an alert. */
  averageHeartRateLimit?: number;
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
  currentCourseRunStatus: CourseRunStatus | null;
  currentCourseRunStartDate?: string | null;
  currentCourseRunEndDate?: string | null;
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
// Course runs
// ---------------------------------------------------------------------------

export type SessionStatus = 'SCHEDULED' | 'HELD' | 'CANCELLED';

export interface CourseRunRequest {
  groupId: number;
  courseId: number;
  startDate: string;
  trainingDays: TrainingDay[];
}

export interface PlannedSession {
  date: string;
  exerciseName: string;
  durationMinutes: number;
  /** Null when the day has no free slot. */
  startTime: string | null;
  endTime: string | null;
}

export interface CourseRunOverlap {
  courseRunId: number;
  courseName: string;
  startDate: string;
  endDate: string;
  status: CourseRunStatus;
}

export interface CourseRunPreview {
  startDate: string;
  endDate: string;
  memberCount: number;
  sessions: PlannedSession[];
  fullDates: string[];
  overlaps: CourseRunOverlap[];
  earliestFreeStart: string;
  canCreate: boolean;
}

export interface CourseRunSummary {
  id: number;
  groupId: number | null;
  groupName: string | null;
  groupDisbanded: boolean;
  courseId: number;
  courseName: string;
  startDate: string;
  endDate: string;
  status: CourseRunStatus;
  sessionsHeld: number;
  sessionsCancelled: number;
  sessionsTotal: number;
}

export interface CourseRunSessionItem {
  id: number;
  date: string;
  startTime: string;
  endTime: string;
  exerciseName: string;
  status: SessionStatus;
  horseCount: number;
}

export type SessionHorseOutcome = 'TO_RUN' | 'NEEDS_RESULT' | 'RAN' | 'CANCELLED' | 'DID_NOT_RUN';

/** A horse of a course run with its Attendance (attended of total Sessions). */
export interface CourseRunHorse {
  horseId: number;
  horseName: string;
  attended: number;
  total: number;
}

/** A horse in one Session of a course run with what became of it. */
export interface SessionHorseOutcomeItem {
  workoutId: number;
  horseId: number;
  horseName: string;
  outcome: SessionHorseOutcome;
}

export interface CourseRunDetail {
  courseRun: CourseRunSummary;
  trainingDays: TrainingDay[];
  sessions: CourseRunSessionItem[];
}

// ---------------------------------------------------------------------------
// Group sessions (schedule, results, attendance)
// ---------------------------------------------------------------------------

export interface ScheduleSession {
  id: number;
  courseRunId: number;
  groupId: number | null;
  groupName: string | null;
  groupDisbanded: boolean;
  courseName: string;
  exerciseName: string;
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
  status: WorkoutStatus;
  performanceRating: number | null;
  /** The stored result, null until one is recorded; the edit form is pre-filled from it. */
  result: SessionWorkoutResult | null;
}

export interface SessionDetail {
  session: ScheduleSession;
  horses: SessionHorse[];
}

export interface SessionWorkoutResult {
  id: number;
  sessionId: number;
  horseId: number;
  status: WorkoutStatus;
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
  courseRunId: number;
  groupName: string | null;
  groupDisbanded: boolean;
  courseName: string;
  courseRunStatus: CourseRunStatus;
  startDate: string;
  endDate: string;
  attended: number;
  total: number;
  joinedOn: string;
}
