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
  from: string;
  to: string;
  page: number;
  size: number;
}
