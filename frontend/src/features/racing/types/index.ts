export type RaceRegistrationStatus = 'PENDING' | 'APPROVED' | 'REJECTED';

export interface CreateRaceRegistrationRequest {
  horseId: number;
  raceName: string;
  raceCategory: string;
  location: string;
  eventDate: string; // YYYY-MM-DD
  eventTime?: string | null; // HH:mm
  organizer?: string | null;
  sourceUrl?: string | null;
  nominationDeadline?: string | null; // YYYY-MM-DD
  distanceMeters?: number | null;
  trackType?: string | null;
  prizeDetails?: string | null;
  selectionReason: string;
  trainerNotes?: string | null;
}

export interface RaceRegistrationResponse {
  id: number;
  horseId: number;
  horseName: string | null;
  horseRegistrationNumber: string | null;
  trainerId: number;
  raceName: string;
  raceCategory: string;
  location: string;
  eventDate: string;
  eventTime: string | null;
  organizer: string | null;
  sourceUrl: string | null;
  nominationDeadline: string | null;
  distanceMeters: number | null;
  trackType: string | null;
  prizeDetails: string | null;
  selectionReason: string;
  trainerNotes: string | null;
  status: RaceRegistrationStatus;
  reviewedById: number | null;
  managerFeedback: string | null;
  reviewedAt: string | null;
  createdAt: string;
}
