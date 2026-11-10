import { apiDelete, apiGet, apiPatch, apiPost, apiPut } from '@/services/api';
import type { ApiResponse } from '@/types/horse';
import type {
  Exercise,
  CreateExerciseRequest,
  Course,
  CourseDetailResponse,
  CreateCourseRequest,
  CompleteWorkoutRequest,
  WorkoutResultPreviewResponse,
  HorseFitnessTrendItem,
  HorseScheduleItem,
  HorseAlert,
  TrainingRules,
  TrainingGroup,
  GroupCandidate,
  CreateTrainingGroupRequest,
  CourseRunRequest,
  CourseRunPreview,
  CourseRunSummary,
  CourseRunDetail,
  CourseRunHorse,
  SessionHorseOutcomeItem,
  ScheduleSession,
  SessionDetail,
  SessionWorkoutResult,
  HorseAttendance,
} from '../types';

/**
 * Every call goes through services/api.ts, so it carries the Bearer token, refreshes it once on a 401, and keeps
 * the backend's detailed error message (for example the notice that a session is full with the exact minutes).
 */
export const trainingService = {
  // Rules the backend enforces, so the UI never hard-codes them
  getRules: async (): Promise<TrainingRules> =>
    (await apiGet<ApiResponse<TrainingRules>>('/api/training-rules')).data,

  // Exercises
  getExercises: async (params?: { trainerId?: number; category?: string }): Promise<Exercise[]> => {
    const searchParams = new URLSearchParams();
    if (params?.trainerId) searchParams.set('trainerId', String(params.trainerId));
    if (params?.category) searchParams.set('category', params.category);
    const qs = searchParams.toString();
    return (await apiGet<ApiResponse<Exercise[]>>(`/api/exercises${qs ? `?${qs}` : ''}`)).data;
  },

  createExercise: async (data: CreateExerciseRequest): Promise<Exercise> =>
    (await apiPost<ApiResponse<Exercise>>('/api/exercises', data)).data,

  // Courses
  getCourses: async (params?: { trainerId?: number }): Promise<Course[]> => {
    const searchParams = new URLSearchParams();
    if (params?.trainerId) searchParams.set('trainerId', String(params.trainerId));
    const qs = searchParams.toString();
    return (await apiGet<ApiResponse<Course[]>>(`/api/courses${qs ? `?${qs}` : ''}`)).data;
  },

  getCourse: async (id: number): Promise<CourseDetailResponse> =>
    (await apiGet<ApiResponse<CourseDetailResponse>>(`/api/courses/${id}`)).data,

  createCourse: async (data: CreateCourseRequest): Promise<Course> =>
    (await apiPost<ApiResponse<Course>>('/api/courses', data)).data,

  /** One horse's sessions from a date to a date, cancelled ones included. */
  getHorseSchedule: async (horseId: number, from: string, to: string): Promise<HorseScheduleItem[]> =>
    (await apiGet<ApiResponse<HorseScheduleItem[]>>(`/api/horses/${horseId}/schedule?from=${from}&to=${to}`)).data,

  // Fitness and alerts
  getFitnessTrend: async (horseId: number, from?: string, to?: string): Promise<HorseFitnessTrendItem[]> => {
    const params = new URLSearchParams();
    if (from) params.append('from', from);
    if (to) params.append('to', to);
    const qs = params.toString() ? `?${params.toString()}` : '';
    return (await apiGet<ApiResponse<HorseFitnessTrendItem[]>>(`/api/horses/${horseId}/fitness-trend${qs}`)).data;
  },

  getHorseAlerts: async (horseId: number): Promise<HorseAlert[]> =>
    (await apiGet<ApiResponse<HorseAlert[]>>(`/api/horses/${horseId}/alerts`)).data,


  // Training groups
  getGroups: async (): Promise<TrainingGroup[]> =>
    (await apiGet<ApiResponse<TrainingGroup[]>>('/api/training-groups')).data,

  getDisbandedGroups: async (): Promise<TrainingGroup[]> =>
    (await apiGet<ApiResponse<TrainingGroup[]>>('/api/training-groups?disbanded=true')).data,

  createGroup: async (data: CreateTrainingGroupRequest): Promise<TrainingGroup> =>
    (await apiPost<ApiResponse<TrainingGroup>>('/api/training-groups', data)).data,

  renameGroup: async (id: number, name: string): Promise<TrainingGroup> =>
    (await apiPatch<ApiResponse<TrainingGroup>>(`/api/training-groups/${id}`, { name })).data,

  disbandGroup: async (id: number): Promise<void> => {
    await apiDelete<ApiResponse<null>>(`/api/training-groups/${id}`);
  },

  getGroupCandidates: async (id: number): Promise<GroupCandidate[]> =>
    (await apiGet<ApiResponse<GroupCandidate[]>>(`/api/training-groups/${id}/candidates`)).data,

  addGroupHorse: async (id: number, horseId: number): Promise<TrainingGroup> =>
    (await apiPost<ApiResponse<TrainingGroup>>(`/api/training-groups/${id}/horses`, { horseId })).data,

  removeGroupHorse: async (id: number, horseId: number): Promise<TrainingGroup> =>
    (await apiDelete<ApiResponse<TrainingGroup>>(`/api/training-groups/${id}/horses/${horseId}`)).data,

  moveGroupHorse: async (targetGroupId: number, horseId: number): Promise<TrainingGroup> =>
    (await apiPost<ApiResponse<TrainingGroup>>(`/api/training-groups/${targetGroupId}/horses/move`, { horseId })).data,

  // Course runs
  previewCourseRun: async (request: CourseRunRequest, signal?: AbortSignal): Promise<CourseRunPreview> =>
    (await apiPost<ApiResponse<CourseRunPreview>>('/api/course-runs/preview', request, signal)).data,

  createCourseRun: async (request: CourseRunRequest): Promise<CourseRunDetail> =>
    (await apiPost<ApiResponse<CourseRunDetail>>('/api/course-runs', request)).data,

  getCourseRuns: async (filter?: { groupId?: number; horseId?: number }): Promise<CourseRunSummary[]> => {
    const query = new URLSearchParams();
    if (filter?.groupId) query.set('groupId', String(filter.groupId));
    if (filter?.horseId) query.set('horseId', String(filter.horseId));
    const suffix = query.size > 0 ? `?${query}` : '';
    return (await apiGet<ApiResponse<CourseRunSummary[]>>(`/api/course-runs${suffix}`)).data;
  },

  getCourseRun: async (id: number): Promise<CourseRunDetail> =>
    (await apiGet<ApiResponse<CourseRunDetail>>(`/api/course-runs/${id}`)).data,

  getCourseRunHorses: async (courseRunId: number): Promise<CourseRunHorse[]> =>
    (await apiGet<ApiResponse<CourseRunHorse[]>>(`/api/course-runs/${courseRunId}/horses`)).data,

  getCourseRunSessionHorses: async (courseRunId: number, sessionId: number): Promise<SessionHorseOutcomeItem[]> =>
    (await apiGet<ApiResponse<SessionHorseOutcomeItem[]>>(`/api/course-runs/${courseRunId}/sessions/${sessionId}/horses`)).data,

  cancelCourseRun: async (id: number): Promise<CourseRunDetail> =>
    (await apiPost<ApiResponse<CourseRunDetail>>(`/api/course-runs/${id}/cancel`, {})).data,

  // Group sessions
  getSessions: async (params: { from: string; to: string; groupId?: number; needsResult?: boolean }): Promise<ScheduleSession[]> => {
    const query = new URLSearchParams({ from: params.from, to: params.to });
    if (params.groupId) query.set('groupId', String(params.groupId));
    if (params.needsResult) query.set('needsResult', 'true');
    return (await apiGet<ApiResponse<ScheduleSession[]>>(`/api/training-sessions?${query}`)).data;
  },

  getSession: async (id: number): Promise<SessionDetail> =>
    (await apiGet<ApiResponse<SessionDetail>>(`/api/training-sessions/${id}`)).data,

  rescheduleSession: async (id: number, newStartTime: string): Promise<void> => {
    await apiPatch<ApiResponse<unknown>>(`/api/training-sessions/${id}/reschedule`, { newStartTime });
  },

  cancelSession: async (id: number): Promise<void> => {
    await apiPost<ApiResponse<unknown>>(`/api/training-sessions/${id}/cancel`, {});
  },

  cancelSessionWorkout: async (id: number): Promise<void> => {
    await apiPost<ApiResponse<unknown>>(`/api/session-workouts/${id}/cancel`, {});
  },

  previewSessionResult: async (id: number): Promise<WorkoutResultPreviewResponse> =>
    (await apiGet<ApiResponse<WorkoutResultPreviewResponse>>(`/api/session-workouts/${id}/result-preview`)).data,

  recordSessionResult: async (id: number, data: CompleteWorkoutRequest): Promise<SessionWorkoutResult> =>
    (await apiPatch<ApiResponse<SessionWorkoutResult>>(`/api/session-workouts/${id}/result`, data)).data,

  updateSessionResult: async (id: number, data: CompleteWorkoutRequest): Promise<SessionWorkoutResult> =>
    (await apiPut<ApiResponse<SessionWorkoutResult>>(`/api/session-workouts/${id}/result`, data)).data,

  markDidNotRun: async (id: number): Promise<void> => {
    await apiPost<ApiResponse<unknown>>(`/api/session-workouts/${id}/did-not-run`, {});
  },

  getHorseAttendance: async (horseId: number): Promise<HorseAttendance[]> =>
    (await apiGet<ApiResponse<HorseAttendance[]>>(`/api/horses/${horseId}/course-run-history`)).data,
};
