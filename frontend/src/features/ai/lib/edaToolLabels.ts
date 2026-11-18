const EDA_TOOL_LABELS: Record<string, string> = {
  FLEET_PERFORMANCE: 'Analyze fleet EDA statistics',
  HORSE_PERFORMANCE: 'Analyze horse EDA statistics',
  BIO_SPC: 'Generate Bio-SPC control chart',
  SURVIVAL: 'Compute Kaplan-Meier survival analysis',
  MANAGER_OVERVIEW: 'Synthesize manager EDA overview',
  ADMISSION_FUNNEL: 'Analyze admission funnel',
  TRAINER_PERFORMANCE: 'Evaluate trainer performance',
};

export function getEdaToolLabel(analysisType: unknown): string | null {
  return typeof analysisType === 'string' &&
    Object.prototype.hasOwnProperty.call(EDA_TOOL_LABELS, analysisType)
    ? EDA_TOOL_LABELS[analysisType]
    : null;
}
