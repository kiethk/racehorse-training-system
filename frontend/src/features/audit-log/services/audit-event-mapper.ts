export type ActivityCategory =
  | 'Admission'
  | 'Horse'
  | 'Staff'
  | 'Stable'
  | 'Health'
  | 'Training'
  | 'Access Control'
  | 'System';

export type ActivityResult = 'Success' | 'Failed';

export interface AuditLogMapping {
  activity: string;
  target: string;
  category: ActivityCategory;
  result: ActivityResult;
}

type ParsedRoute = {
  segments: string[];
  resource?: string;
  id?: string;
  action?: string;
};

type ResourceConfig = {
  category: ActivityCategory;
  singular: string;
  plural?: string;
  createActivity?: string;
  updateActivity?: string;
};

const RESOURCE_CONFIG: Record<string, ResourceConfig> = {
  admissions: {
    category: 'Admission',
    singular: 'Admission',
    createActivity: 'Submitted admission',
    updateActivity: 'Updated admission',
  },

  horses: {
    category: 'Horse',
    singular: 'Horse',
    updateActivity: 'Updated horse profile',
  },

  staff: {
    category: 'Staff',
    singular: 'Staff Member',
    plural: 'Staff Directory',
    createActivity: 'Created staff account',
    updateActivity: 'Updated staff member',
  },

  users: {
    category: 'Staff',
    singular: 'Staff Member',
    plural: 'Staff Directory',
    createActivity: 'Created staff account',
    updateActivity: 'Updated staff member',
  },

  stalls: {
    category: 'Stable',
    singular: 'Stall',
    updateActivity: 'Updated stable configuration',
  },

  areas: {
    category: 'Stable',
    singular: 'Area',
    updateActivity: 'Updated stable configuration',
  },

  'vet-offers': {
    category: 'Health',
    singular: 'Vet Offer',
    updateActivity: 'Updated vet offer',
  },

  'care-schedules': {
    category: 'Health',
    singular: 'Care Schedule',
    createActivity: 'Created care schedule',
    updateActivity: 'Updated care schedule',
  },

  'health-records': {
    category: 'Health',
    singular: 'Health Record',
    createActivity: 'Created health record',
    updateActivity: 'Updated health record',
  },

  'training-plans': {
    category: 'Training',
    singular: 'Training Plan',
    createActivity: 'Created training plan',
    updateActivity: 'Updated training plan',
  },

  lots: {
    category: 'Training',
    singular: 'Training Lot',
    updateActivity: 'Updated training lot',
  },

  workouts: {
    category: 'Training',
    singular: 'Workout',
    updateActivity: 'Updated workout',
  },

  auth: {
    category: 'Access Control',
    singular: 'Authentication System',
    updateActivity: 'Authentication activity',
  },
};

const ACTION_OVERRIDES: Record<
  string,
  Partial<Pick<AuditLogMapping, 'activity' | 'category'>> & {
    target?: string;
  }
> = {
  // Admission
  'groom-review': {
    activity: 'Reviewed admission (Groom)',
    category: 'Admission',
  },

  'vet-review': {
    activity: 'Reviewed admission (Vet)',
    category: 'Admission',
  },

  'manager-review': {
    activity: 'Reviewed admission (Manager)',
    category: 'Admission',
  },

  'trainer-assessment': {
    activity: 'Assessed admission readiness',
    category: 'Admission',
  },

  // Health
  accept: {
    activity: 'Accepted vet assignment',
    category: 'Health',
  },

  decline: {
    activity: 'Declined vet assignment',
    category: 'Health',
  },

  start: {
    activity: 'Started examination',
    category: 'Health',
  },

  complete: {
    activity: 'Completed examination',
  },

  // Training
  reschedule: {
    activity: 'Rescheduled training lot',
    category: 'Training',
  },

  cancel: {
    activity: 'Cancelled training lot',
    category: 'Training',
  },

  // Stable
  'assign-groom': {
    activity: 'Assigned groom to stall',
    category: 'Stable',
  },

  // Horse
  'assign-stall': {
    activity: 'Assigned horse to stall',
    category: 'Horse',
  },

  // Staff
  status: {
    activity: 'Changed staff active status',
    category: 'Staff',
  },

  // Auth
  login: {
    activity: 'Logged in',
    category: 'Access Control',
    target: 'Authentication System',
  },

  logout: {
    activity: 'Logged out',
    category: 'Access Control',
    target: 'Authentication System',
  },

  register: {
    activity: 'Registered account',
    category: 'Access Control',
    target: 'Authentication System',
  },
};

function normalizeMethod(method: string): string {
  return method.trim().toUpperCase();
}

function normalizePath(path: string): string {
  return path
    .split('?')[0]
    .replace(/\/+/g, '/')
    .replace(/\/$/, '')
    .toLowerCase();
}

function parseRoute(path: string): ParsedRoute {
  const normalized = normalizePath(path);
  const segments = normalized.split('/').filter(Boolean);

  const apiIndex = segments.indexOf('api');
  const apiSegments = apiIndex >= 0 ? segments.slice(apiIndex + 1) : segments;

  const resourceIndex = apiSegments.findIndex(
    segment => RESOURCE_CONFIG[segment]
  );

  if (resourceIndex < 0) {
    return { segments: apiSegments };
  }

  const resource = apiSegments[resourceIndex];

  const maybeId = apiSegments[resourceIndex + 1];
  const id = maybeId && /^\d+$/.test(maybeId) ? maybeId : undefined;

  const actionIndex = resourceIndex + (id ? 2 : 1);
  const action = apiSegments[actionIndex];

  return {
    segments: apiSegments,
    resource,
    id,
    action,
  };
}

function buildTarget(resource: string, id?: string): string {
  const config = RESOURCE_CONFIG[resource];

  if (!config) {
    return 'Resource';
  }

  if (id) {
    return `${config.singular} #${id}`;
  }

  return config.plural ?? config.singular;
}

function getGenericActivity(
  method: string,
  resource: string
): string {
  const config = RESOURCE_CONFIG[resource];

  if (!config) {
    return 'System activity';
  }

  switch (method) {
    case 'POST':
      return config.createActivity ?? `Created ${config.singular.toLowerCase()}`;

    case 'PUT':
    case 'PATCH':
      return config.updateActivity ?? `Updated ${config.singular.toLowerCase()}`;

    case 'DELETE':
      return `Deleted ${config.singular.toLowerCase()}`;

    case 'GET':
      return `Viewed ${config.singular.toLowerCase()}`;

    default:
      return config.updateActivity ?? 'System activity';
  }
}

function mapResult(statusCode: number): ActivityResult {
  return statusCode >= 200 && statusCode < 300
    ? 'Success'
    : 'Failed';
}

export function mapAuditLogEvent(
  method: string,
  path: string,
  statusCode: number
): AuditLogMapping {
  const normalizedMethod = normalizeMethod(method);
  const parsed = parseRoute(path);
  const result = mapResult(statusCode);

  if (!parsed.resource) {
    return {
      activity: 'System activity',
      target: 'Unknown resource',
      category: 'System',
      result,
    };
  }

  const config = RESOURCE_CONFIG[parsed.resource];

  if (!config) {
    return {
      activity: 'System activity',
      target: 'Unknown resource',
      category: 'System',
      result,
    };
  }

  const target = buildTarget(parsed.resource, parsed.id);

  // 1. Highest priority: specific route/action override
  if (parsed.action) {
    const override = ACTION_OVERRIDES[parsed.action];

    if (override) {
      return {
        activity:
          override.activity ??
          getGenericActivity(normalizedMethod, parsed.resource),

        target: override.target ?? target,

        category:
          override.category ?? config.category,

        result,
      };
    }
  }

  // 2. Generic resource + HTTP method
  return {
    activity: getGenericActivity(
      normalizedMethod,
      parsed.resource
    ),
    target,
    category: config.category,
    result,
  };
}
