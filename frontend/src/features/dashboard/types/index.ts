import type { HorseSpecialty } from '@/features/horses/types';
import type { PlanStatus } from '@/features/training/types';

/** One horse of the dashboard: its current plan, recent performance and risk alerts. */
export interface DashboardHorse {
  horseId: number;
  horseName: string;
  breed: string | null;
  stallCode: string | null;
  planId: number | null;
  courseName: string;
  /** NONE when the horse has no plan yet. */
  planStatus: PlanStatus | 'NONE';
  completedSessions: number;
  totalSessions: number;
  /** Cancelled sessions; left out of the progress. */
  cancelledSessions: number;
  progressPercent: number;
  latestPerformanceRating: number | null;
  avgPerformanceRating30d: number | null;
  alertsCount: number;
  alertTitles: string[];
  startDate: string | null;
  endDate: string | null;
  specialty: HorseSpecialty;
}

/** One week of the performance chart: each horse counts once, then the horses are averaged. */
export interface PerformanceWeek {
  /** The Monday of the week, YYYY-MM-DD. */
  weekStart: string;
  /** Horses with a completed session in the week. */
  horses: number;
  sessions: number;
  averageRating: number | null;
  averageSpeedKmh: number | null;
  averageMaxHeartRate: number | null;
}

export interface PerformanceTrend {
  /** The Monday of the first week. */
  fromDate: string;
  toDate: string;
  /** Only the weeks with completed sessions, ascending. */
  weeks: PerformanceWeek[];
}

/** A horse with a health reading outside the normal range on a day. */
export interface AbnormalHorse {
  horseId: number;
  horseName: string;
  /** Ready-to-show sentences, for example "Temperature 38.7 °C". */
  issues: string[];
}

/** One day of the health chart: the readings taken and the horses that had an abnormal one. */
export interface HealthDay {
  date: string; // YYYY-MM-DD
  readings: number;
  abnormalHorses: AbnormalHorse[];
}

export interface HealthTrend {
  fromDate: string;
  toDate: string;
  /** Only the days with readings, ascending. */
  days: HealthDay[];
}

/** What a TrendCard draws: the range of the axis and the periods that have data. */
export interface Trend<TItem> {
  fromDate: string;
  toDate: string;
  items: TItem[];
}

export interface StallSummary {
  totalStalls: number;
  occupiedStalls: number;
  availableStalls: number;
  reservedStalls: number;
  occupancyRate: number;
}

export interface FleetSummary {
  totalHorses: number;
  activeEligible: number;
  inTraining: number;
  medicalBlocked: number;
  inactive: number;
}

export interface AdmissionSummary {
  totalPending: number;
  managerReviewCount: number;
  waitingArrivalCount: number;
  groomReviewCount: number;
  arrivalDeadlineAlertCount: number;
}

export interface IncidentSummary {
  openIncidents: number;
  criticalUrgentIncidents: number;
}

export interface BarnOccupancyDto {
  areaId: number;
  areaCode: string;
  areaName: string;
  areaType: string;
  totalStalls: number;
  occupiedStalls: number;
  availableStalls: number;
  reservedStalls: number;
  occupancyRate: number;
}

export interface PendingAdmissionActionDto {
  admissionId: number;
  candidateHorseName: string;
  breed: string;
  ownerId: number;
  ownerName: string;
  status: string;
  submittedAt: string;
  arrivalDeadlineAt: string | null;
  isArrivalOverdueOrExpiring: boolean;
}

export interface UrgentIncidentActionDto {
  incidentId: number;
  horseId: number;
  horseName: string;
  title: string;
  description: string;
  severity: string;
  status: string;
  reportedAt: string;
  reportedById: number;
  reporterName: string;
}

export interface ManagerDashboardSummary {
  stallSummary: StallSummary;
  fleetSummary: FleetSummary;
  admissionSummary: AdmissionSummary;
  incidentSummary: IncidentSummary;
  barnOccupancies: BarnOccupancyDto[];
  pendingAdmissions: PendingAdmissionActionDto[];
  urgentIncidents: UrgentIncidentActionDto[];
}

