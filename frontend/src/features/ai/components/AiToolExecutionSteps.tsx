'use client';

import { useState } from 'react';
import { ChevronRight, AlertCircle, Check, Loader2 } from 'lucide-react';
import { Button } from '@/components/ui';

const TOOL_LABELS: Record<string, string> = {
  getHorsesList: 'Retrieve horses list',
  getHorseDetails: 'View horse details',
  getTrainingPlans: 'Check training plans',
  getMedicalRecords: 'Inspect medical records',
  getUrgentAlerts: 'Scan urgent medical alerts',
  getStallStatus: 'Check stable occupancy',
  getStaffSchedule: 'Look up staff schedule',
  queryClubMetrics: 'Synthesize club metrics',
  generateChartData: 'Visualize data chart',
  getTrainerSchedule: 'Look up trainer schedule',
  getTrainerPlansSummary: 'Check trainer plans summary',
  getTrainerHorses: 'Retrieve trainer horses',
  getTrainerGroups: 'List training groups',
  getWorkoutHistory: 'Check workout history',
  getMyAssignedHorses: 'List assigned horses & stalls',
  getDailyGroomTasks: 'Retrieve daily groom tasks',
  getTodayAggregatedTasks: 'Check aggregated tasks',
  getMyReportedIncidents: 'List reported incidents',
  getHorseStallLocation: 'Locate horse stall',
  getVetExaminationOverview: 'Inspect veterinary exam overview',
  getQuarantineStatus: 'Check quarantine status',
  getHorseVitalSignsTrend: 'Analyze vital signs trend',
  getActivePrescriptions: 'Check active prescriptions',
  getPendingAdmissionExams: 'Check pending admission exams',
  getUrgentCareAlerts: 'Scan urgent care alerts',
  evaluateTrainingResumptionReadiness: 'Evaluate training resumption readiness',
  getClubHealthOverview: 'Review club health overview',
  getStableOccupancyMetrics: 'Check stable occupancy metrics',
  getSystemStaffOverview: 'Inspect staff overview',
  getAdmissionRequestsOverview: 'Review admission requests',
  getRecentAuditLogs: 'Review recent audit logs',
  getMyHorses: 'Retrieve owned horses',
  getMyHorseAdmissions: 'Check admission applications',
  getMyHorseUpcomingSchedule: 'Look up upcoming schedule',
  getMyHorsePedigreeInfo: 'Inspect horse pedigree',
  getHorseSpecialtyAnalysis: 'Analyze horse specialties',
  recommendOptimalRaceDistanceAndTrack: 'Recommend optimal distance & track',
  recommendNextWorkoutAdjustment: 'Recommend next workout adjustment',
  recommendTrainerStrategy: 'Recommend training strategy',
  analyzeStableCapacityAndStallBottlenecks: 'Analyze stable bottlenecks',
  analyzeIncidentPatternsAndRecurringRisks: 'Analyze recurring risks',
  getMyHorseHealthSummary: 'Review horse health summary',
  getMyHorseTrainingProgress: 'Review training progress',
  getHorseCareSchedule: 'Check care schedule',
  getMyHorseIncidentHistory: 'Review horse incident history',
  analyzeHorseInvestmentAndFitnessROI: 'Analyze investment and fitness return',
  getHorseFitnessTrend: 'Analyze horse fitness trend',
  getTrainerRiskAlerts: 'Review trainer risk alerts',
  getTrainerAssignedGrooms: 'Look up assigned grooms',
  getAssignedAdmissionReviews: 'Review assigned admission applications',
  searchClinicalKnowledge: 'Search clinical knowledge',
  getHorseMedicalHistory: 'Review horse medical history',
  getPreventiveCareSchedule: 'Check preventive care schedule',
  getUrgentCareAndIncidentAlerts: 'Review urgent care and incident alerts',
  recommendClinicalInterventionPlan: 'Recommend clinical intervention plan',
  getHorseDietaryAndCareAlerts: 'Review dietary and care alerts',
  getGroomTaskCompletionRate: 'Check groom task completion',
  detectHorseBehavioralAnomaly: 'Detect behavioral anomalies',
  recommendDailyCarePriorityOrder: 'Recommend daily care priorities',
  getAdmissionPipelineOverview: 'Review admission pipeline',
  getTrainerAreaDistribution: 'Review trainer area assignments',
  getClubHorsesDirectory: 'Review club horse directory',
  recommendGroomWorkloadRebalancing: 'Recommend groom workload balancing',
};

function formatToolName(name: string): string {
  if (TOOL_LABELS[name]) return TOOL_LABELS[name];
  return name.replace(/([A-Z])/g, ' $1').replace(/^./, (str) => str.toUpperCase());
}

function formatToolArgs(args: unknown): string | null {
  if (!args || typeof args !== 'object') return null;
  const entries = Object.entries(args as Record<string, unknown>);
  if (entries.length === 0) return null;
  return entries.map(([k, v]) => `${k}: ${String(v)}`).join(', ');
}

export function AiToolExecutionSteps({
  tools,
  isRunning,
}: {
  tools?: Record<string, unknown>[];
  isRunning?: boolean;
}) {
  const [expanded, setExpanded] = useState(false);

  if (!tools || tools.length === 0) {
    if (!isRunning) return null;
    return (
      <div className="flex h-7 items-center gap-2 rounded-md text-xs text-[var(--color-text-secondary)]">
        <Loader2 className="size-3.5 animate-spin text-[var(--color-primary)]" />
        <span>Processing data...</span>
      </div>
    );
  }

  const errorCount = tools.filter(
    (t) => t.status === 'ERROR' || Boolean(t.error)
  ).length;

  const summary = errorCount > 0
    ? `Used ${tools.length} tools, ${errorCount} errors`
    : `Used ${tools.length} tools`;

  return (
    <div className="my-1 text-xs">
      <Button
        type="button"
        variant="tertiary"
        size="sm"
        onClick={() => setExpanded(!expanded)}
        aria-expanded={expanded}
        className="flex h-7 cursor-pointer items-center gap-1.5 whitespace-normal rounded-md px-0 text-[var(--color-text-secondary)] hover:text-[var(--color-text-primary)] outline-hidden transition-colors"
      >
        <ChevronRight
          className={`size-3.5 transition-transform duration-200 ${
            expanded ? 'rotate-90' : ''
          }`}
        />
        <span>{summary}</span>
      </Button>

      {/* Grid-based animated accordion */}
      <div
        className={`grid transition-[grid-template-rows] duration-200 ease-out ${
          expanded ? 'grid-rows-[1fr]' : 'grid-rows-[0fr]'
        }`}
      >
        <div className="overflow-hidden" inert={!expanded}>
          <ul className="ml-[7px] my-1 space-y-2 border-l border-[var(--color-border-strong)] pl-4 py-1">
            {tools.map((tool, idx) => {
              const toolName = String(tool.tool || tool.name || `Tool ${idx + 1}`);
              const isError = tool.status === 'ERROR' || Boolean(tool.error);
              const errorMessage = typeof tool.error === 'string' ? tool.error : null;
              const argsSummary = formatToolArgs(tool.arguments || tool.input);
              const resultCount =
                !isError && Array.isArray(tool.result)
                  ? `${tool.result.length} records`
                  : null;
              const detailText = isError && errorMessage
                ? `Error: ${errorMessage}`
                : resultCount
                ? `Result: ${resultCount}`
                : argsSummary;

              return (
                <li key={idx} className="flex items-start gap-2">
                  {isError ? (
                    <AlertCircle className="size-3.5 text-red-600 shrink-0 mt-0.5" />
                  ) : (
                    <Check className="size-3.5 text-[var(--color-text-secondary)] shrink-0 mt-0.5" />
                  )}
                  <div className="min-w-0">
                    <p className="font-medium text-[var(--color-text-primary)]">
                      {formatToolName(toolName)}
                    </p>
                    {detailText && (
                      <p className="text-[11px] text-[var(--color-text-secondary)] truncate max-w-[280px] sm:max-w-[400px]">
                        {detailText}
                      </p>
                    )}
                  </div>
                </li>
              );
            })}
          </ul>
        </div>
      </div>
    </div>
  );
}
