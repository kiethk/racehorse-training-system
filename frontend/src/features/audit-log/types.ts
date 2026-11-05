export interface AuditDetailPayload {
  version?: number;
  summary?: string;
  reason?: string;
  module?: string;
  targetUrl?: string;
  before?: Record<string, unknown>;
  after?: Record<string, unknown>;
}

export interface AuditLogItem {
  id: number;
  actorUserId: number | null;
  actorName: string | null;
  actorEmail: string | null;
  actorRole: string | null;
  httpMethod: string;
  requestPath: string;
  statusCode: number;
  action: string | null;
  entityName: string | null;
  entityId: number | null;
  module?: string | null;
  details: string | null;
  success: boolean;
  createdAt: string;
}

export interface PageResponse<T> {
  content: T[];
  pageable: {
    pageNumber: number;
    pageSize: number;
  };
  totalElements: number;
  totalPages: number;
  size: number;
  number: number;
  first: boolean;
  last: boolean;
  empty: boolean;
}

export interface AuditLogFilters {
  search: string;
  httpMethod: string;
  statusCode: string;
  module?: string;
  role?: string;
  from: string;
  to: string;
  page: number;
  size: number;
}

export interface AuditArchiveStatus {
  totalCount: number;
  agedCount: number;
  oldestLogDate: string | null;
  requiresMaintenance: boolean;
}

export interface AuditPurgeRequest {
  beforeDate?: string;
  fromDate?: string;
  reason?: string;
}

export interface AuditPurgeResponse {
  purgedCount: number;
  purgedAt: string;
  message: string;
}

