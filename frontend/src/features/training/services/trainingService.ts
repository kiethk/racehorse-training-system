import { apiDelete, apiGet, apiPatch, apiPost, apiPut } from '@/services/api';
import type { ApiResponse } from '@/types/horse';
import type {
  Subject,
  CreateSubjectRequest,
  Course,
  CourseDetailResponse,
  CreateCourseRequest,
  JoinableCohortResponse,
  CreateHorseTrainingPlanRequest,
  PlanPreviewRequest,
  PlanPreviewResponse,
  HorseTrainingPlanDetailResponse,
  PlanSummaryResponse,
  CompleteWorkoutRequest,
  WorkoutResultPreviewResponse,
  TrainingLotResponse,
  RescheduleLotRequest,
  HorseFitnessTrendItem,
  HorseScheduleItem,
  HorseAlert,
  TrainingRules,
  TrainingGroup,
  GroupCandidate,
  CreateTrainingGroupRequest,
  GroupPlanRequest,
  GroupPlanPreview,
  GroupPlanSummary,
  GroupPlanDetail,
  ScheduleSession,
  SessionDetail,
  SessionWorkoutResult,
  HorseAttendance,
} from '../types';

/**
 * Every call goes through services/api.ts, so it carries the Bearer token, refreshes it once on a 401, and keeps
 * the backend's detailed error message (for example the notice that a lot is full with the exact minutes).
 */
export const trainingService = {
  // Rules the backend enforces, so the UI never hard-codes them
  getRules: async (): Promise<TrainingRules> =>
    (await apiGet<ApiResponse<TrainingRules>>('/api/training-rules')).data,

  // Subjects
  getSubjects: async (params?: { trainerId?: number; category?: string }): Promise<Subject[]> => {
    const searchParams = new URLSearchParams();
    if (params?.trainerId) searchParams.set('trainerId', String(params.trainerId));
    if (params?.category) searchParams.set('category', params.category);
    const qs = searchParams.toString();
    return (await apiGet<ApiResponse<Subject[]>>(`/api/subjects${qs ? `?${qs}` : ''}`)).data;
  },

  createSubject: async (data: CreateSubjectRequest): Promise<Subject> =>
    (await apiPost<ApiResponse<Subject>>('/api/subjects', data)).data,

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

  // Training plans
  getJoinableCohorts: async (courseId: number): Promise<JoinableCohortResponse[]> =>
    (await apiGet<ApiResponse<JoinableCohortResponse[]>>(
      `/api/training-plans/joinable-cohorts?courseId=${courseId}`,
    )).data,

  /** Dry run of createPlan: the course period and which horses are free, busy or blocked. Nothing is saved. */
  previewPlan: async (data: PlanPreviewRequest, signal?: AbortSignal): Promise<PlanPreviewResponse> =>
    (await apiPost<ApiResponse<PlanPreviewResponse>>('/api/training-plans/preview', data, signal)).data,

  /** Enrols a group; the response holds one plan per horse. */
  createPlan: async (data: CreateHorseTrainingPlanRequest): Promise<HorseTrainingPlanDetailResponse[]> =>
    (await apiPost<ApiResponse<HorseTrainingPlanDetailResponse[]>>('/api/training-plans', data)).data,

  /** Cancels an upcoming or running plan; sessions not held yet are cancelled. */
  cancelPlan: async (id: number): Promise<HorseTrainingPlanDetailResponse> =>
    (await apiPost<ApiResponse<HorseTrainingPlanDetailResponse>>(`/api/training-plans/${id}/cancel`, {})).data,

  getPlanById: async (id: number): Promise<HorseTrainingPlanDetailResponse> =>
    (await apiGet<ApiResponse<HorseTrainingPlanDetailResponse>>(`/api/training-plans/${id}`)).data,

  /** Plans with horse name, course name and progress. */
  getPlans: async (): Promise<PlanSummaryResponse[]> =>
    (await apiGet<ApiResponse<PlanSummaryResponse[]>>('/api/training-plans')).data,

  // Training lots
  getLots: async (from: string, to: string, includeCancelled = false): Promise<TrainingLotResponse[]> =>
    (await apiGet<ApiResponse<TrainingLotResponse[]>>(
      `/api/lots?from=${from}&to=${to}&includeCancelled=${includeCancelled}`,
    )).data,

  /** One horse's sessions from a date to a date, cancelled ones included. */
  getHorseSchedule: async (horseId: number, from: string, to: string): Promise<HorseScheduleItem[]> =>
    (await apiGet<ApiResponse<HorseScheduleItem[]>>(`/api/horses/${horseId}/schedule?from=${from}&to=${to}`)).data,

  rescheduleLot: async (id: number, data: RescheduleLotRequest): Promise<TrainingLotResponse> =>
    (await apiPatch<ApiResponse<TrainingLotResponse>>(`/api/lots/${id}/reschedule`, data)).data,

  cancelLot: async (id: number): Promise<TrainingLotResponse> =>
    (await apiPatch<ApiResponse<TrainingLotResponse>>(`/api/lots/${id}/cancel`)).data,

  // Workouts
  getWorkoutResultPreview: async (workoutId: number): Promise<WorkoutResultPreviewResponse> =>
    (await apiGet<ApiResponse<WorkoutResultPreviewResponse>>(
      `/api/workouts/${workoutId}/result-preview`,
    )).data,

  completeWorkout: async (workoutId: number, data: CompleteWorkoutRequest): Promise<void> => {
    await apiPatch(`/api/workouts/${workoutId}/complete`, data);
  },

  updateWorkoutResult: async (workoutId: number, data: CompleteWorkoutRequest): Promise<void> => {
    await apiPut(`/api/workouts/${workoutId}/result`, data);
  },

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

  createGroup: async (data: CreateTrainingGroupRequest): Promise<TrainingGroup> =>
    (await apiPost<ApiResponse<TrainingGroup>>('/api/training-groups', data)).data,

  renameGroup: async (id: number, name: string): Promise<TrainingGroup> =>
    (await apiPatch<ApiResponse<TrainingGroup>>(`/api/training-groups/${id}`, { name })).data,

  deleteGroup: async (id: number): Promise<void> => {
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

  // Group plans
  previewGroupPlan: async (request: GroupPlanRequest, signal?: AbortSignal): Promise<GroupPlanPreview> =>
    (await apiPost<ApiResponse<GroupPlanPreview>>('/api/group-plans/preview', request, signal)).data,

  createGroupPlan: async (request: GroupPlanRequest): Promise<GroupPlanDetail> =>
    (await apiPost<ApiResponse<GroupPlanDetail>>('/api/group-plans', request)).data,

  getGroupPlans: async (groupId?: number): Promise<GroupPlanSummary[]> =>
    (await apiGet<ApiResponse<GroupPlanSummary[]>>(`/api/group-plans${groupId ? `?groupId=${groupId}` : ''}`)).data,

  getGroupPlan: async (id: number): Promise<GroupPlanDetail> =>
    (await apiGet<ApiResponse<GroupPlanDetail>>(`/api/group-plans/${id}`)).data,

  cancelGroupPlan: async (id: number): Promise<GroupPlanDetail> =>
    (await apiPost<ApiResponse<GroupPlanDetail>>(`/api/group-plans/${id}/cancel`, {})).data,

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
    (await apiGet<ApiResponse<HorseAttendance[]>>(`/api/horses/${horseId}/group-plan-history`)).data,
};
