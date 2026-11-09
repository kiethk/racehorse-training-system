/**
 * Utility formatters for Executive EDA and Manager Analytics views.
 * Ensures all raw backend codes and enums (e.g. VET_REVIEW, TIME_EXCEEDED, QUARANTINE)
 * are properly translated into user-friendly, non-academic Plain English labels.
 */

/**
 * Formats admission lifecycle stage identifiers into human-readable stage names.
 * Example: 'VET_REVIEW' -> 'Veterinary Review'
 */
export const formatAdmissionStage = (stage: string): string => {
  switch (stage) {
    case 'ARRIVAL':
    case 'WAITING_FOR_ARRIVAL':
      return 'Facility Arrival';
    case 'GROOM_REVIEW':
      return 'Groom Inspection';
    case 'VET_REVIEW':
      return 'Veterinary Review';
    case 'TRAINER_REVIEW':
      return 'Trainer Assessment';
    case 'MANAGER_REVIEW':
      return 'Manager Approval';
    case 'APPROVED':
      return 'Admitted';
    case 'REJECTED':
      return 'Rejected';
    case 'NONE':
      return 'None';
    default:
      return stage
        .replace(/_/g, ' ')
        .toLowerCase()
        .replace(/\b\w/g, (c) => c.toUpperCase());
  }
};

/**
 * Formats veterinary care schedule reschedule reasons into plain English.
 * Example: 'TIME_EXCEEDED' -> 'Appointment Overtime'
 */
export const formatRescheduleReason = (reason: string): string => {
  switch (reason) {
    case 'TIME_EXCEEDED':
      return 'Appointment Overtime';
    case 'URGENT_PREEMPTION':
      return 'Emergency Care Preemption';
    case 'STAFF_UNAVAILABLE':
      return 'Staff Shortage';
    case 'EQUIPMENT_UNAVAILABLE':
      return 'Equipment Unavailable';
    case 'HORSE_UNAVAILABLE':
      return 'Horse Unavailable';
    default:
      return reason
        .replace(/_/g, ' ')
        .toLowerCase()
        .replace(/\b\w/g, (c) => c.toUpperCase());
  }
};

/**
 * Formats facility area classification types.
 * Example: 'QUARANTINE' -> 'Quarantine Ward', 'REGULAR' -> 'Standard Stabling'
 */
export const formatFacilityAreaType = (type: string): string => {
  switch (type.toUpperCase()) {
    case 'QUARANTINE':
      return 'Quarantine Ward';
    case 'REGULAR':
    case 'TRAINING':
      return 'Standard Stabling';
    case 'HOSPITAL':
      return 'Medical Clinic';
    default:
      return type
        .replace(/_/g, ' ')
        .toLowerCase()
        .replace(/\b\w/g, (c) => c.toUpperCase());
  }
};
