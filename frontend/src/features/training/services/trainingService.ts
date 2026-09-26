import { apiGet, apiPost } from '@/services/api';
import type {
  Subject,
  CreateSubjectRequest,
  SubjectCategory,
  Course,
  CourseDetailResponse,
  CreateCourseRequest,
  CourseSubjectItem,
  CourseSubjectResponse,
  JoinableCohortResponse,
  CreateHorseTrainingPlanRequest,
  HorseTrainingPlanDetailResponse,
  HorseTrainingPlan,
  CompleteWorkoutRequest,
  TrainingLotResponse,
  RescheduleLotRequest,
} from '../types';

interface ApiResponse<T> {
  success: boolean;
  data: T;
  message?: string;
}

const API_URL = process.env.NEXT_PUBLIC_API_URL || 'http://localhost:8080';

/**
 * Giữ nguyên message lỗi chi tiết từ backend (ví dụ: thông báo hết khe giờ vàng kèm số phút cụ thể)
 */
async function postWithMessage<T>(path: string, body: unknown): Promise<T> {
  const res = await fetch(`${API_URL}${path}`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    credentials: 'include',
    body: JSON.stringify(body),
  });
  const payload = await res.json().catch(() => null);
  if (!res.ok) {
    throw new Error(payload?.message || `API error: ${res.status}`);
  }
  return payload?.data !== undefined ? payload.data : (payload as T);
}

async function patchWithMessage<T>(path: string, body?: unknown): Promise<T> {
  const res = await fetch(`${API_URL}${path}`, {
    method: 'PATCH',
    headers: { 'Content-Type': 'application/json' },
    credentials: 'include',
    body: body !== undefined ? JSON.stringify(body) : undefined,
  });
  const payload = await res.json().catch(() => null);
  if (!res.ok) {
    throw new Error(payload?.message || `API error: ${res.status}`);
  }
  return payload?.data !== undefined ? payload.data : (payload as T);
}

export const trainingApi = {
  // Bài tập (Subjects)
  getSubjects: async (): Promise<Subject[]> =>
    (await apiGet<ApiResponse<Subject[]>>('/api/subjects')).data,

  createSubject: async (data: CreateSubjectRequest): Promise<Subject> =>
    (await apiPost<ApiResponse<Subject>>('/api/subjects', data)).data,

  getCategories: async (): Promise<SubjectCategory[]> =>
    (await apiGet<ApiResponse<SubjectCategory[]>>('/api/subject-categories')).data,

  // Khóa học (Courses)
  getCourses: async (): Promise<Course[]> =>
    (await apiGet<ApiResponse<Course[]>>('/api/courses')).data,

  getCourse: async (id: number): Promise<CourseDetailResponse> =>
    (await apiGet<ApiResponse<CourseDetailResponse>>(`/api/courses/${id}`)).data,

  createCourse: async (data: CreateCourseRequest): Promise<Course> =>
    (await apiPost<ApiResponse<Course>>('/api/courses', data)).data,

  addSubjectToCourse: async (
    courseId: number,
    data: CourseSubjectItem,
  ): Promise<CourseSubjectResponse> =>
    (await apiPost<ApiResponse<CourseSubjectResponse>>(`/api/courses/${courseId}/subjects`, data)).data,

  // Kế hoạch huấn luyện (Training Plans)
  getJoinableCohorts: async (courseId: number): Promise<JoinableCohortResponse[]> =>
    (await apiGet<ApiResponse<JoinableCohortResponse[]>>(
      `/api/training-plans/joinable-cohorts?courseId=${courseId}`,
    )).data,

  createPlan: async (
    data: CreateHorseTrainingPlanRequest,
  ): Promise<HorseTrainingPlanDetailResponse[]> =>
    await postWithMessage<HorseTrainingPlanDetailResponse[]>('/api/training-plans', data),

  getPlanById: async (id: number): Promise<HorseTrainingPlanDetailResponse> =>
    (await apiGet<ApiResponse<HorseTrainingPlanDetailResponse>>(`/api/training-plans/${id}`)).data,

  getPlans: async (horseId?: number): Promise<HorseTrainingPlan[]> => {
    const url = horseId ? `/api/training-plans?horseId=${horseId}` : '/api/training-plans';
    return (await apiGet<ApiResponse<HorseTrainingPlan[]>>(url)).data;
  },

  // Vận hành Lot (Training Lots)
  getLots: async (from: string, to: string, includeCancelled: boolean = false): Promise<TrainingLotResponse[]> =>
    (await apiGet<ApiResponse<TrainingLotResponse[]>>(
      `/api/lots?from=${from}&to=${to}&includeCancelled=${includeCancelled}`,
    )).data,

  getLotById: async (id: number): Promise<TrainingLotResponse> =>
    (await apiGet<ApiResponse<TrainingLotResponse>>(`/api/lots/${id}`)).data,

  rescheduleLot: async (id: number, data: RescheduleLotRequest): Promise<TrainingLotResponse> =>
    await patchWithMessage<TrainingLotResponse>(`/api/lots/${id}/reschedule`, data),

  cancelLot: async (id: number): Promise<TrainingLotResponse> =>
    await patchWithMessage<TrainingLotResponse>(`/api/lots/${id}/cancel`),

  // Buổi tập (Workouts)
  completeWorkout: async (workoutId: number, data: CompleteWorkoutRequest): Promise<void> => {
    await patchWithMessage(`/api/workouts/${workoutId}/complete`, data);
  },
};

