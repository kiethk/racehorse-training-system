# EDA Analytics Widget Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Build the EDA (Exploratory Data Analysis) Analytics Widget for the RTMS Dashboard to extract descriptive statistical features (Mean, Median, IQR, Outliers) and visualize correlation (Pearson $r$, Linear Regression) between training performance (Speed) and recovery biometrics (Recovery Heart Rate), supporting both **Fleet Overview (Toàn đàn)** and **Individual Horse (Cá nhân chiến mã)** views.

**Architecture:**
- **Backend (Spring Boot):**
  - `EdaStatisticsCalculator`: Pure mathematical statistical utility (Mean, Median, Quartiles $Q_1, Q_3$, Tukey's Fences Outliers, Pearson Correlation $r$, Simple Linear Regression).
  - `HorseEdaAnalyticsService`: Queries real workout and biometric data from `TrainingWorkout` and `HorseHealthMetric`, normalizes data, and produces EDA analysis DTOs.
  - `HorseEdaAnalyticsController`: Exposes two REST endpoints: `/api/analytics/eda/fleet` and `/api/analytics/eda/horses/{id}` with RBAC checks (`TRAINING_PLAN_VIEW`, `HORSE_VIEW`).
- **Frontend (Next.js / Recharts):**
  - `edaAnalyticsService.ts`: Type-safe API client.
  - `EdaScatterChart.tsx`: Recharts-based interactive scatter plot with trend line and outlier highlights.
  - `HorseEdaAnalyticsCard.tsx`: Complete Dashboard widget with view toggle (Fleet vs. Individual), statistical summary table, and anomaly alerts.

**Tech Stack:** Java 21, Spring Boot 3.x, Spring Data JPA, JUnit 5, Next.js 15, TypeScript, Tailwind CSS, Recharts 3.10.1.

---

## Global Constraints

- **Language & Runtime:** Java 21, Spring Boot 3.x, Node.js 20+, Next.js 15.
- **Math Rigor:** Strict zero-division guard for Pearson $r$ when $\sigma_x = 0$ or $\sigma_y = 0$ or $N < 2$.
- **Outlier Standard:** Tukey's fences using $[Q_1 - 1.5 \times \text{IQR}, Q_3 + 1.5 \times \text{IQR}]$.
- **Component Styling:** Tailwind CSS following existing RTMS dashboard design tokens (`var(--color-...)`, `Panel`, `SectionTitle`, `Pill`).
- **Worktree:** Work is conducted in `D:/Project/racehorse-training-system/.worktrees/ai-eda` on branch `feature/ai-eda`.

---

## Review Focus

1. **Empty / Insufficient Data:** When a horse or fleet has 0 or 1 workout session, the statistics calculator must return safe defaults without throwing exceptions (`NaN` or `ArithmeticException`).
2. **Identical Values (Zero Variance):** When all speed values or recovery heart rates are identical, $\sigma = 0$, Pearson $r$ must safely return `0.0` with `NO_VARIANCE` label.
3. **Outlier Precision:** Ensure Tukey's fence boundary checks accurately mark outliers on both upper and lower bounds.
4. **Data Isolation & Scope:** Backend must scope fleet and horse queries to what the authenticated user is allowed to see (Trainer sees their assigned horses; Manager sees all active horses).
5. **Responsive Visualization:** Recharts scatter plot must handle screen resize smoothly via `ResponsiveContainer` without layout shifts.

---

## Tasks

### Task 1: Mathematical Statistics Engine (`EdaStatisticsCalculator`)
- [ ] Create `backend/src/main/java/com/rtms/backend/analytics/util/EdaStatisticsCalculator.java`
- [ ] Implement `computeDistribution(List<Double> values)` (min, max, mean, median, q1, q3, iqr, stdDev, outliers)
- [ ] Implement `computeCorrelation(List<Double> x, List<Double> y)` (Pearson $r$, slope, intercept, sampleSize, correlationStrength)
- [ ] Create `backend/src/test/java/com/rtms/backend/analytics/util/EdaStatisticsCalculatorTest.java`
- [ ] Run `./mvnw test -Dtest=EdaStatisticsCalculatorTest` and verify all tests pass

### Task 2: Backend EDA DTOs, Service & Controller
- [ ] Create DTOs:
  - `backend/src/main/java/com/rtms/backend/analytics/dto/EdaDistributionStats.java`
  - `backend/src/main/java/com/rtms/backend/analytics/dto/EdaCorrelationSummary.java`
  - `backend/src/main/java/com/rtms/backend/analytics/dto/FleetHorseEdaPoint.java`
  - `backend/src/main/java/com/rtms/backend/analytics/dto/IndividualWorkoutEdaPoint.java`
  - `backend/src/main/java/com/rtms/backend/analytics/dto/FleetEdaResponse.java`
  - `backend/src/main/java/com/rtms/backend/analytics/dto/IndividualHorseEdaResponse.java`
- [ ] Create `backend/src/main/java/com/rtms/backend/analytics/service/HorseEdaAnalyticsService.java`
- [ ] Create `backend/src/main/java/com/rtms/backend/analytics/controller/HorseEdaAnalyticsController.java`
- [ ] Create unit tests: `backend/src/test/java/com/rtms/backend/analytics/service/HorseEdaAnalyticsServiceTest.java`
- [ ] Run `./mvnw test -Dtest=HorseEdaAnalytics*Test` and verify all pass

### Task 3: Frontend EDA Types & API Service
- [ ] Create `frontend/src/features/analytics/types/eda.ts`
- [ ] Create `frontend/src/features/analytics/services/edaAnalyticsService.ts`
- [ ] Create test `frontend/src/features/analytics/services/__tests__/edaAnalyticsService.test.ts`
- [ ] Run `npm test -- edaAnalyticsService.test.ts` and verify

### Task 4: Frontend EDA Widget Components
- [ ] Create `frontend/src/features/analytics/components/CorrelationBadge.tsx`
- [ ] Create `frontend/src/features/analytics/components/StatisticalSummaryTable.tsx`
- [ ] Create `frontend/src/features/analytics/components/EdaScatterChart.tsx`
- [ ] Create `frontend/src/features/analytics/components/HorseEdaAnalyticsCard.tsx`
- [ ] Create unit test `frontend/src/features/analytics/components/__tests__/HorseEdaAnalyticsCard.test.ts`
- [ ] Run `npm test -- HorseEdaAnalyticsCard.test.ts` and verify

### Task 5: Integration into HorseDashboardView & Verification
- [ ] Add `HorseEdaAnalyticsCard` to `frontend/src/features/dashboard/components/HorseDashboardView.tsx`
- [ ] Run frontend test suite: `npm test`
- [ ] Run backend compilation: `./mvnw test-compile`
- [ ] Verify everything compiles cleanly and passes with 0 failures
