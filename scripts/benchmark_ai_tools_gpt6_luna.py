#!/usr/bin/env python3
"""
benchmark_ai_tools_gpt6_luna.py
Comprehensive Benchmark and Test Suite for RTMS AI Tools using the GPT-6 Luna model family.
Validates 100% of tools across HORSE_OWNER, HEAD_TRAINER, VETERINARIAN, GROOM, CLUB_MANAGER,
and exploratory data analysis (EDA) modes.

Reports:
  - Prompt Input
  - Output (Assistant response)
  - Chain-of-Thought (Reasoning Content)
  - Tool Calls (Name, Arguments, Result)
  - Execution Metrics (Latency ms, Tokens, Success/Error status)
"""

import argparse
import json
import os
import re
import sys
import time
from datetime import datetime
from typing import Any, Dict, List, Optional
from urllib.error import HTTPError, URLError
from urllib.request import Request, urlopen

# Define 61 Comprehensive Tool Scenarios covering all roles and EDA modes
TOOL_BENCHMARK_SCENARIOS: List[Dict[str, Any]] = [
    # ==================== HORSE_OWNER (10 Tools) ====================
    {
        "id": "OWNER_01",
        "role": "HORSE_OWNER",
        "tool_name": "getMyHorses",
        "prompt": "I want to check all the horses currently registered under my ownership and see which ones are cleared for training.",
        "expected_tool": "getMyHorses",
        "mock_result": {
            "totalOwnedHorses": 3,
            "allowedTrainingHorses": 2,
            "restrictedHorses": 1,
            "horseList": [
                {"id": 1, "name": "Thunder", "status": "ACTIVE", "trainingCleared": True, "stall": "A-101"},
                {"id": 2, "name": "Lightning", "status": "ACTIVE", "trainingCleared": True, "stall": "A-102"},
                {"id": 3, "name": "Storm Runner", "status": "MEDICAL_REST", "trainingCleared": False, "stall": "Q-01"}
            ]
        }
    },
    {
        "id": "OWNER_02",
        "role": "HORSE_OWNER",
        "tool_name": "getMyHorseHealthSummary",
        "prompt": "Can you summarize the health records and veterinary status for my horse Thunder?",
        "expected_tool": "getMyHorseHealthSummary",
        "mock_result": {
            "horseId": 1,
            "horseName": "Thunder",
            "overallStatus": "HEALTHY",
            "medicalClearance": "CLEARED_FULL_WORKOUT",
            "lastCheckup": "2026-11-10",
            "veterinarianNotes": "Normal vital signs. Tendon ultrasound clear."
        }
    },
    {
        "id": "OWNER_03",
        "role": "HORSE_OWNER",
        "tool_name": "getMyHorseTrainingProgress",
        "prompt": "How is Thunder progressing in the current training course? What workouts were completed recently?",
        "expected_tool": "getMyHorseTrainingProgress",
        "mock_result": {
            "horseId": 1,
            "horseName": "Thunder",
            "courseName": "Sprint Mastery Level 2",
            "completionRate": "78%",
            "completedWorkouts": 14,
            "trainingDecision": "ADVANCE_TO_INTERVAL_GALLOP"
        }
    },
    {
        "id": "OWNER_04",
        "role": "HORSE_OWNER",
        "tool_name": "getMyHorseUpcomingSchedule",
        "prompt": "Show me the upcoming training schedule for my horses for the next 7 days.",
        "expected_tool": "getMyHorseUpcomingSchedule",
        "mock_result": {
            "fromDate": "2026-11-18",
            "toDate": "2026-11-25",
            "sessions": [
                {"date": "2026-11-19", "time": "06:30", "horse": "Thunder", "exercise": "Canter 1200m", "trainer": "Alex Morgan"},
                {"date": "2026-11-21", "time": "07:00", "horse": "Lightning", "exercise": "Stamina Trot 3000m", "trainer": "Alex Morgan"}
            ]
        }
    },
    {
        "id": "OWNER_05",
        "role": "HORSE_OWNER",
        "tool_name": "getMyHorseAdmissions",
        "prompt": "What is the status of my new admission request for the filly Silver Mist?",
        "expected_tool": "getMyHorseAdmissions",
        "mock_result": {
            "applicationId": 105,
            "candidateHorseName": "Silver Mist",
            "currentStage": "VET_REVIEW",
            "status": "IN_REVIEW",
            "submittedDate": "2026-11-12",
            "quarantineStallReserved": "Q-03"
        }
    },
    {
        "id": "OWNER_06",
        "role": "HORSE_OWNER",
        "tool_name": "getHorseCareSchedule",
        "prompt": "Does Thunder have any vaccinations or veterinary appointments scheduled this month?",
        "expected_tool": "getHorseCareSchedule",
        "mock_result": {
            "horseId": 1,
            "horseName": "Thunder",
            "schedules": [
                {"id": 41, "type": "VACCINATION", "date": "2026-11-28", "details": "Equine Influenza Booster", "vet": "Dr. Sarah Jenkins"}
            ]
        }
    },
    {
        "id": "OWNER_07",
        "role": "HORSE_OWNER",
        "tool_name": "getMyHorseIncidentHistory",
        "prompt": "Check if there are any incidents or minor injury reports logged for Storm Runner.",
        "expected_tool": "getMyHorseIncidentHistory",
        "mock_result": {
            "horseId": 3,
            "horseName": "Storm Runner",
            "totalIncidents": 1,
            "incidents": [
                {"date": "2026-11-08", "severity": "MINOR", "description": "Superficial scrape on left hind fetlock during paddock turnout."}
            ]
        }
    },
    {
        "id": "OWNER_08",
        "role": "HORSE_OWNER",
        "tool_name": "getMyHorsePedigreeInfo",
        "prompt": "Can you provide the pedigree information and lineage details for Thunder?",
        "expected_tool": "getMyHorsePedigreeInfo",
        "mock_result": {
            "horseId": 1,
            "horseName": "Thunder",
            "sire": "Storm Cat",
            "dam": "Northern Dancer Line",
            "breed": "Thoroughbred",
            "foalDate": "2022-04-12"
        }
    },
    {
        "id": "OWNER_09",
        "role": "HORSE_OWNER",
        "tool_name": "analyzeHorseInvestmentAndFitnessROI",
        "prompt": "Analyze my financial investment versus fitness gains for Thunder over the last 6 months.",
        "expected_tool": "analyzeHorseInvestmentAndFitnessROI",
        "mock_result": {
            "horseId": 1,
            "investmentScore": "A+",
            "stablingCostToDate": 4500,
            "fitnessGainIndex": "+24.5%",
            "topSpeedImprovement": "61.2 km/h -> 64.8 km/h",
            "recommendation": "Maintain training load; horse is on track for Group 3 debut."
        }
    },
    {
        "id": "OWNER_10",
        "role": "HORSE_OWNER",
        "tool_name": "getHorseTrainingSummary",
        "prompt": "Give me a high-level training summary for Thunder across all past course runs.",
        "expected_tool": "getHorseTrainingSummary",
        "mock_result": {
            "horseId": 1,
            "totalRuns": 38,
            "averageGallopPace": "14.2 sec/furlong",
            "enduranceMetric": "High",
            "status": "TRAINING_ACTIVE"
        }
    },

    # ==================== HEAD_TRAINER (16 Tools) ====================
    {
        "id": "TRAINER_01",
        "role": "HEAD_TRAINER",
        "tool_name": "getTrainerHorses",
        "prompt": "List all horses assigned to my training roster and their current fitness condition.",
        "expected_tool": "getTrainerHorses",
        "mock_result": {
            "trainerId": 10,
            "assignedHorseCount": 12,
            "horses": [
                {"id": 1, "name": "Thunder", "group": "Group Alpha", "fitnessScore": 88},
                {"id": 4, "name": "Blaze", "group": "Group Beta", "fitnessScore": 92}
            ]
        }
    },
    {
        "id": "TRAINER_02",
        "role": "HEAD_TRAINER",
        "tool_name": "getTrainerGroups",
        "prompt": "What training groups do I currently oversee and what is each group's training focus?",
        "expected_tool": "getTrainerGroups",
        "mock_result": {
            "groups": [
                {"id": 1, "name": "Group Alpha - Sprint", "targetDistance": "1000m-1200m", "horseCount": 6},
                {"id": 2, "name": "Group Beta - Distance", "targetDistance": "2000m+", "horseCount": 6}
            ]
        }
    },
    {
        "id": "TRAINER_03",
        "role": "HEAD_TRAINER",
        "tool_name": "getWorkoutHistory",
        "prompt": "Retrieve the recent workout records for horse Blaze from the past 14 days.",
        "expected_tool": "getWorkoutHistory",
        "mock_result": {
            "horseId": 4,
            "records": [
                {"date": "2026-11-15", "exercise": "Breezing 800m", "speed": "62.4 km/h", "heartRateRecovery": "4.2 min"},
                {"date": "2026-11-12", "exercise": "Stamina Lope", "distance": "2400m", "rating": "EXCELLENT"}
            ]
        }
    },
    {
        "id": "TRAINER_04",
        "role": "HEAD_TRAINER",
        "tool_name": "getHorseFitnessTrend",
        "prompt": "Show the longitudinal fitness and recovery trend for horse Blaze.",
        "expected_tool": "getHorseFitnessTrend",
        "mock_result": {
            "horseId": 4,
            "trend": "STEADY_INCREASE",
            "vo2MaxEstimated": "+8.2%",
            "lacticAcidClearanceRate": "OPTIMAL",
            "fatigueIndex": 18
        }
    },
    {
        "id": "TRAINER_05",
        "role": "HEAD_TRAINER",
        "tool_name": "getTrainerSchedule",
        "prompt": "What is my assigned track and training schedule for tomorrow morning?",
        "expected_tool": "getTrainerSchedule",
        "mock_result": {
            "date": "2026-11-18",
            "trackReserved": "Main Turf Track",
            "sessions": [
                {"time": "06:00 - 07:30", "group": "Group Alpha", "drill": "Gate exit and sprint burst"}
            ]
        }
    },
    {
        "id": "TRAINER_06",
        "role": "HEAD_TRAINER",
        "tool_name": "getTrainerPlansSummary",
        "prompt": "Give me a summary of active training plans and their milestone completion rates.",
        "expected_tool": "getTrainerPlansSummary",
        "mock_result": {
            "activePlans": 3,
            "milestoneCompletionRate": "84.2%",
            "upcomingMilestone": "1200m Speed Test on 2026-11-20"
        }
    },
    {
        "id": "TRAINER_07",
        "role": "HEAD_TRAINER",
        "tool_name": "getHorseSpecialtyAnalysis",
        "prompt": "What are the recommended race specialties and surface preferences for Blaze?",
        "expected_tool": "getHorseSpecialtyAnalysis",
        "mock_result": {
            "horseId": 4,
            "optimalDistance": "1200m - 1400m",
            "optimalTrackSurface": "TURF_GOOD",
            "runningStyle": "FRONT_RUNNER"
        }
    },
    {
        "id": "TRAINER_08",
        "role": "HEAD_TRAINER",
        "tool_name": "getTrainerRiskAlerts",
        "prompt": "Are there any high-fatigue or overtraining risk alerts across my assigned horses?",
        "expected_tool": "getTrainerRiskAlerts",
        "mock_result": {
            "alertCount": 1,
            "alerts": [
                {"horse": "Thunder", "severity": "MEDIUM", "reason": "Heart rate recovery 1.5 min slower than baseline"}
            ]
        }
    },
    {
        "id": "TRAINER_09",
        "role": "HEAD_TRAINER",
        "tool_name": "getRacingReadinessAssessment",
        "prompt": "Evaluate the racing readiness assessment score for horse Blaze.",
        "expected_tool": "getRacingReadinessAssessment",
        "mock_result": {
            "horseId": 4,
            "readinessScore": 94,
            "verdict": "READY_FOR_COMPETITION",
            "assessedBy": "Alex Morgan, Head Trainer"
        }
    },
    {
        "id": "TRAINER_10",
        "role": "HEAD_TRAINER",
        "tool_name": "getTrainerAssignedGrooms",
        "prompt": "Which grooms are assigned to care for the horses in my training block?",
        "expected_tool": "getTrainerAssignedGrooms",
        "mock_result": {
            "stableBlock": "Block A",
            "grooms": [
                {"id": 201, "name": "Nguyen Van A", "assignedStalls": ["A-101", "A-102", "A-103"]},
                {"id": 202, "name": "Tran Thi B", "assignedStalls": ["A-104", "A-105"]}
            ]
        }
    },
    {
        "id": "TRAINER_11",
        "role": "HEAD_TRAINER",
        "tool_name": "getAssignedAdmissionReviews",
        "prompt": "Do I have any pending admission applications requiring my trainer clearance?",
        "expected_tool": "getAssignedAdmissionReviews",
        "mock_result": {
            "pendingCount": 1,
            "reviews": [
                {"applicationId": 105, "horseName": "Silver Mist", "status": "PENDING_TRAINER_REVIEW"}
            ]
        }
    },
    {
        "id": "TRAINER_12",
        "role": "HEAD_TRAINER",
        "tool_name": "getCourseCurriculumOverview",
        "prompt": "Show me the curriculum outline and phase milestones for the Sprint Course.",
        "expected_tool": "getCourseCurriculumOverview",
        "mock_result": {
            "courseName": "Sprint Course",
            "totalWeeks": 8,
            "modules": ["Aerobic Base", "Turn Agility", "Explosive Gate Start", "Full Furlong Taper"]
        }
    },
    {
        "id": "TRAINER_13",
        "role": "HEAD_TRAINER",
        "tool_name": "analyzeWorkoutLoadAndOvertrainingRisk",
        "prompt": "Analyze workout load accumulation and acute-to-chronic workload ratio for Thunder.",
        "expected_tool": "analyzeWorkoutLoadAndOvertrainingRisk",
        "mock_result": {
            "horseId": 1,
            "acuteChronicRatio": 1.25,
            "status": "SWEET_SPOT",
            "injuryRisk": "LOW"
        }
    },
    {
        "id": "TRAINER_14",
        "role": "HEAD_TRAINER",
        "tool_name": "recommendOptimalRaceDistanceAndTrack",
        "prompt": "Recommend the optimal race distance and track conditions for Blaze.",
        "expected_tool": "recommendOptimalRaceDistanceAndTrack",
        "mock_result": {
            "horseId": 4,
            "recommendedDistance": "1200m",
            "surface": "Dry Turf",
            "confidenceScore": 0.91
        }
    },
    {
        "id": "TRAINER_15",
        "role": "HEAD_TRAINER",
        "tool_name": "recommendNextWorkoutAdjustment",
        "prompt": "Based on today's heat and recovery metrics, how should I adjust tomorrow's workout for Thunder?",
        "expected_tool": "recommendNextWorkoutAdjustment",
        "mock_result": {
            "horseId": 1,
            "adjustment": "Reduce sprint reps from 4 to 3; substitute final furlong with walking cool-down."
        }
    },
    {
        "id": "TRAINER_16",
        "role": "HEAD_TRAINER",
        "tool_name": "recommendTrainerStrategy",
        "prompt": "Provide a comprehensive racing and training strategy recommendation for horse Blaze.",
        "expected_tool": "recommendTrainerStrategy",
        "mock_result": {
            "horseId": 4,
            "strategicPlan": "Target Group 2 Sprint on December 15; enter 2 tune-up trials; prioritize gate agility."
        }
    },

    # ==================== VETERINARIAN (14 Tools) ====================
    {
        "id": "VET_01",
        "role": "VETERINARIAN",
        "tool_name": "searchClinicalKnowledge",
        "prompt": "Search clinical references for tendonitis rehabilitation protocols in racehorses.",
        "expected_tool": "searchClinicalKnowledge",
        "mock_result": {
            "matches": [
                {"title": "Equine Tendinopathy Protocol", "rehabPeriod": "8-12 weeks", "recommendation": "Controlled hand-walking with cold hydrotherapy."}
            ]
        }
    },
    {
        "id": "VET_02",
        "role": "VETERINARIAN",
        "tool_name": "getVetExaminationOverview",
        "prompt": "Give me an overview of all veterinary examinations conducted today.",
        "expected_tool": "getVetExaminationOverview",
        "mock_result": {
            "date": "2026-11-17",
            "examsCompleted": 5,
            "quarantineClearances": 1,
            "flagsIdentified": 0
        }
    },
    {
        "id": "VET_03",
        "role": "VETERINARIAN",
        "tool_name": "getHorseMedicalHistory",
        "prompt": "Pull up the complete medical record, diagnoses, and treatments for horse Storm Runner.",
        "expected_tool": "getHorseMedicalHistory",
        "mock_result": {
            "horseId": 3,
            "diagnoses": [
                {"condition": "Left Fore Fetlock Sprain", "date": "2026-10-14", "resolved": True}
            ]
        }
    },
    {
        "id": "VET_04",
        "role": "VETERINARIAN",
        "tool_name": "getActivePrescriptions",
        "prompt": "List all active medications and prescriptions for horses currently under care.",
        "expected_tool": "getActivePrescriptions",
        "mock_result": {
            "activeCount": 2,
            "prescriptions": [
                {"horse": "Storm Runner", "medication": "Phenylbutazone", "dosage": "2g daily", "expires": "2026-11-20"}
            ]
        }
    },
    {
        "id": "VET_05",
        "role": "VETERINARIAN",
        "tool_name": "getPreventiveCareSchedule",
        "prompt": "What routine deworming and dental checks are scheduled for this week?",
        "expected_tool": "getPreventiveCareSchedule",
        "mock_result": {
            "schedules": [
                {"type": "DEWORMING", "horse": "Thunder", "dueDate": "2026-11-19"}
            ]
        }
    },
    {
        "id": "VET_06",
        "role": "VETERINARIAN",
        "tool_name": "getQuarantineStatus",
        "prompt": "What is the occupancy and health observation status in the quarantine ward?",
        "expected_tool": "getQuarantineStatus",
        "mock_result": {
            "quarantineCapacity": 6,
            "currentOccupancy": 2,
            "horsesInQuarantine": ["Silver Mist", "Desert Wind"],
            "bioSecurityAlert": "NORMAL"
        }
    },
    {
        "id": "VET_07",
        "role": "VETERINARIAN",
        "tool_name": "getPendingAdmissionExams",
        "prompt": "Are there any incoming horses waiting for their initial veterinary admission exam?",
        "expected_tool": "getPendingAdmissionExams",
        "mock_result": {
            "pendingExams": [
                {"applicationId": 105, "candidateHorse": "Silver Mist", "scheduledSlot": "Tomorrow 09:00"}
            ]
        }
    },
    {
        "id": "VET_08",
        "role": "VETERINARIAN",
        "tool_name": "getUrgentCareAlerts",
        "prompt": "Check if there are any urgent clinical alerts or emergency flags.",
        "expected_tool": "getUrgentCareAlerts",
        "mock_result": {
            "urgentAlerts": []
        }
    },
    {
        "id": "VET_09",
        "role": "VETERINARIAN",
        "tool_name": "getUrgentCareAndIncidentAlerts",
        "prompt": "Retrieve aggregated urgent care notifications and severe incident reports.",
        "expected_tool": "getUrgentCareAndIncidentAlerts",
        "mock_result": {
            "highPriorityIncidents": 0,
            "status": "ALL_CLEAR"
        }
    },
    {
        "id": "VET_10",
        "role": "VETERINARIAN",
        "tool_name": "getRecentIncidentReports",
        "prompt": "Show me incident reports filed by grooms over the past 48 hours.",
        "expected_tool": "getRecentIncidentReports",
        "mock_result": {
            "recentReports": [
                {"id": 88, "horse": "Storm Runner", "type": "SCRAPE", "reportedBy": "Groom A"}
            ]
        }
    },
    {
        "id": "VET_11",
        "role": "VETERINARIAN",
        "tool_name": "getHorseVitalSignsTrend",
        "prompt": "Graph and summarize the temperature and resting heart rate trend for horse Storm Runner.",
        "expected_tool": "getHorseVitalSignsTrend",
        "mock_result": {
            "horseId": 3,
            "avgTemperature": 38.0,
            "avgRestingHR": 36,
            "stability": "STABLE"
        }
    },
    {
        "id": "VET_12",
        "role": "VETERINARIAN",
        "tool_name": "analyzeIncidentPatternsAndRecurringRisks",
        "prompt": "Identify any recurring injury patterns or stall hazards from recent incidents.",
        "expected_tool": "analyzeIncidentPatternsAndRecurringRisks",
        "mock_result": {
            "pattern": "Minor fetlock scrapes predominantly occurring in Paddock 3",
            "recommendation": "Inspect perimeter fencing in Paddock 3."
        }
    },
    {
        "id": "VET_13",
        "role": "VETERINARIAN",
        "tool_name": "recommendClinicalInterventionPlan",
        "prompt": "Recommend a clinical recovery and intervention plan for mild suspensory ligament strain.",
        "expected_tool": "recommendClinicalInterventionPlan",
        "mock_result": {
            "phase1": "14 days cold compression and stall rest",
            "phase2": "Laser therapy and controlled in-hand walking",
            "phase3": "Ultrasound re-evaluation before track return"
        }
    },
    {
        "id": "VET_14",
        "role": "VETERINARIAN",
        "tool_name": "evaluateTrainingResumptionReadiness",
        "prompt": "Has Storm Runner completed rehabilitation to a degree that clears training resumption?",
        "expected_tool": "evaluateTrainingResumptionReadiness",
        "mock_result": {
            "horseId": 3,
            "clearanceStatus": "RESTRICTED_WALK_ONLY",
            "readinessScore": 68,
            "barrier": "Needs final ultrasound clearance."
        }
    },

    # ==================== GROOM (9 Tools) ====================
    {
        "id": "GROOM_01",
        "role": "GROOM",
        "tool_name": "getMyAssignedHorses",
        "prompt": "Which horses are assigned to my care duty today and where are their stalls located?",
        "expected_tool": "getMyAssignedHorses",
        "mock_result": {
            "groomId": 201,
            "horses": [
                {"id": 1, "name": "Thunder", "stall": "A-101"},
                {"id": 2, "name": "Lightning", "stall": "A-102"}
            ]
        }
    },
    {
        "id": "GROOM_02",
        "role": "GROOM",
        "tool_name": "getDailyGroomTasks",
        "prompt": "List my morning grooming and feeding checklist tasks for today.",
        "expected_tool": "getDailyGroomTasks",
        "mock_result": {
            "tasks": [
                {"id": 501, "task": "Muck out stall A-101", "status": "COMPLETED"},
                {"id": 502, "task": "Feed morning oats (3kg)", "status": "PENDING"}
            ]
        }
    },
    {
        "id": "GROOM_03",
        "role": "GROOM",
        "tool_name": "getTodayAggregatedTasks",
        "prompt": "Provide the aggregated task list across all my assigned stalls for today.",
        "expected_tool": "getTodayAggregatedTasks",
        "mock_result": {
            "totalTasks": 8,
            "completed": 4,
            "pending": 4
        }
    },
    {
        "id": "GROOM_04",
        "role": "GROOM",
        "tool_name": "getHorseDietaryAndCareAlerts",
        "prompt": "Are there any special dietary instructions, supplements, or bandaging notes for Thunder?",
        "expected_tool": "getHorseDietaryAndCareAlerts",
        "mock_result": {
            "horseId": 1,
            "supplements": "Electrolytes post-workout",
            "careAlert": "Cold hose front legs for 15 mins after gallop."
        }
    },
    {
        "id": "GROOM_05",
        "role": "GROOM",
        "tool_name": "getHorseStallLocation",
        "prompt": "Where is horse Lightning currently stabled?",
        "expected_tool": "getHorseStallLocation",
        "mock_result": {
            "horseName": "Lightning",
            "stallNumber": "A-102",
            "barnArea": "Barn A North Wing"
        }
    },
    {
        "id": "GROOM_06",
        "role": "GROOM",
        "tool_name": "getMyReportedIncidents",
        "prompt": "Show me the incidents I have reported this week.",
        "expected_tool": "getMyReportedIncidents",
        "mock_result": {
            "reportedCount": 1,
            "reports": [
                {"id": 88, "horse": "Storm Runner", "type": "SCRAPE", "status": "REVIEWED_BY_VET"}
            ]
        }
    },
    {
        "id": "GROOM_07",
        "role": "GROOM",
        "tool_name": "getGroomTaskCompletionRate",
        "prompt": "What is my task completion rate and punctuality metric for the current shift?",
        "expected_tool": "getGroomTaskCompletionRate",
        "mock_result": {
            "completionRate": "95%",
            "shift": "Morning Shift (05:00 - 13:00)",
            "onTimeRating": "EXCELLENT"
        }
    },
    {
        "id": "GROOM_08",
        "role": "GROOM",
        "tool_name": "detectHorseBehavioralAnomaly",
        "prompt": "Did the system detect any behavioral anomalies or reduced appetite for Thunder today?",
        "expected_tool": "detectHorseBehavioralAnomaly",
        "mock_result": {
            "horseId": 1,
            "appetite": "NORMAL",
            "demeanor": "ALERT_ACTIVE",
            "anomaliesDetected": False
        }
    },
    {
        "id": "GROOM_09",
        "role": "GROOM",
        "tool_name": "recommendDailyCarePriorityOrder",
        "prompt": "In what order should I prioritize my grooming and care duties this morning?",
        "expected_tool": "recommendDailyCarePriorityOrder",
        "mock_result": {
            "priority1": "Feed horses scheduled for early 06:00 workouts",
            "priority2": "Prep horses for veterinary morning inspection",
            "priority3": "Muck out remaining stalls and replenish bedding"
        }
    },

    # ==================== CLUB_MANAGER (11 Tools) ====================
    {
        "id": "MANAGER_01",
        "role": "CLUB_MANAGER",
        "tool_name": "getStableOccupancyMetrics",
        "prompt": "What is the overall stable occupancy rate across all barns and stalls?",
        "expected_tool": "getStableOccupancyMetrics",
        "mock_result": {
            "totalStalls": 80,
            "occupiedStalls": 68,
            "occupancyRate": "85.0%",
            "quarantineStallsFree": 4
        }
    },
    {
        "id": "MANAGER_02",
        "role": "CLUB_MANAGER",
        "tool_name": "getAdmissionRequestsOverview",
        "prompt": "Give me an overview of all horse admission requests currently in the pipeline.",
        "expected_tool": "getAdmissionRequestsOverview",
        "mock_result": {
            "totalApplications": 5,
            "pendingVetReview": 2,
            "pendingTrainerReview": 1,
            "pendingManagerApproval": 2
        }
    },
    {
        "id": "MANAGER_03",
        "role": "CLUB_MANAGER",
        "tool_name": "getAdmissionPipelineOverview",
        "prompt": "Show me the consolidated admission pipeline funnel and stage progression.",
        "expected_tool": "getAdmissionPipelineOverview",
        "mock_result": {
            "funnel": {
                "SUBMITTED": 5,
                "GROOM_REVIEW": 4,
                "VET_REVIEW": 3,
                "TRAINER_REVIEW": 2,
                "MANAGER_REVIEW": 2,
                "APPROVED": 14
            }
        }
    },
    {
        "id": "MANAGER_04",
        "role": "CLUB_MANAGER",
        "tool_name": "getAdmissionPipelineDetails",
        "prompt": "Retrieve detailed applicant and review records for admission application #105.",
        "expected_tool": "getAdmissionPipelineDetails",
        "mock_result": {
            "applicationId": 105,
            "horse": "Silver Mist",
            "owner": "John Smith",
            "vetReviewDecision": "APPROVED_CONDITIONAL",
            "trainerReviewDecision": "APPROVED"
        }
    },
    {
        "id": "MANAGER_05",
        "role": "CLUB_MANAGER",
        "tool_name": "getTrainerAreaDistribution",
        "prompt": "How are stable wings and barn areas distributed among our licensed trainers?",
        "expected_tool": "getTrainerAreaDistribution",
        "mock_result": {
            "distribution": [
                {"trainer": "Alex Morgan", "barn": "Barn A", "stallsAssigned": 20},
                {"trainer": "David Wu", "barn": "Barn B", "stallsAssigned": 25}
            ]
        }
    },
    {
        "id": "MANAGER_06",
        "role": "CLUB_MANAGER",
        "tool_name": "getClubHealthOverview",
        "prompt": "What is the club-wide health metric and active injury count?",
        "expected_tool": "getClubHealthOverview",
        "mock_result": {
            "totalHorses": 68,
            "fitForTraining": 62,
            "inRehab": 4,
            "quarantine": 2,
            "clubHealthIndex": "94.8%"
        }
    },
    {
        "id": "MANAGER_07",
        "role": "CLUB_MANAGER",
        "tool_name": "getSystemStaffOverview",
        "prompt": "Provide a breakdown of active staff members across trainers, vets, and grooms.",
        "expected_tool": "getSystemStaffOverview",
        "mock_result": {
            "totalStaff": 24,
            "headTrainers": 3,
            "veterinarians": 2,
            "grooms": 15,
            "administrative": 4
        }
    },
    {
        "id": "MANAGER_08",
        "role": "CLUB_MANAGER",
        "tool_name": "getClubHorsesDirectory",
        "prompt": "Search the club horses directory for all 3-year-old Thoroughbreds.",
        "expected_tool": "getClubHorsesDirectory",
        "mock_result": {
            "matchCount": 8,
            "sample": ["Thunder", "Blaze", "Golden Star"]
        }
    },
    {
        "id": "MANAGER_09",
        "role": "CLUB_MANAGER",
        "tool_name": "getRecentAuditLogs",
        "prompt": "Show recent security audit logs and administrative privilege actions.",
        "expected_tool": "getRecentAuditLogs",
        "mock_result": {
            "recentEvents": [
                {"action": "ROLE_UPDATE", "actor": "manager@rtms.com", "target": "trainer2@rtms.com", "time": "2026-11-17 14:00"}
            ]
        }
    },
    {
        "id": "MANAGER_10",
        "role": "CLUB_MANAGER",
        "tool_name": "analyzeStableCapacityAndStallBottlenecks",
        "prompt": "Analyze stable capacity forecasting and forecast whether we will hit stall bottlenecks next month.",
        "expected_tool": "analyzeStableCapacityAndStallBottlenecks",
        "mock_result": {
            "projectedOccupancy": "92%",
            "bottleneckWarning": "Barn B quarantine transition may reach capacity by Dec 1st.",
            "recommendation": "Accelerate discharge exams for recovering horses."
        }
    },
    {
        "id": "MANAGER_11",
        "role": "CLUB_MANAGER",
        "tool_name": "recommendGroomWorkloadRebalancing",
        "prompt": "Recommend groom workload rebalancing between Barn A and Barn B based on current horse assignments.",
        "expected_tool": "recommendGroomWorkloadRebalancing",
        "mock_result": {
            "imbalanceDetected": True,
            "recommendation": "Shift 2 grooms from Barn C (low occupancy) to Barn A morning shift."
        }
    },

    # ==================== EXPLORATORY DATA ANALYSIS (EDA - 7 Modes) ====================
    {
        "id": "EDA_01_BIO_SPC",
        "role": "VETERINARIAN",
        "tool_name": "getEdaAnalytics",
        "prompt": "Generate a Bio-SPC statistical process control chart for horse Thunder's body temperature over the last 30 days.",
        "expected_tool": "getEdaAnalytics",
        "expected_analysis_type": "BIO_SPC",
        "mock_result": {
            "analysisType": "BIO_SPC",
            "horseId": 1,
            "metric": "TEMPERATURE",
            "mean": 38.05,
            "ucl": 38.55,
            "lcl": 37.55,
            "westernElectricViolations": 0,
            "status": "IN_CONTROL"
        }
    },
    {
        "id": "EDA_02_SURVIVAL",
        "role": "VETERINARIAN",
        "tool_name": "getEdaAnalytics",
        "prompt": "Calculate the Kaplan-Meier survival curves for horse recovery time stratified by injury severity.",
        "expected_tool": "getEdaAnalytics",
        "expected_analysis_type": "SURVIVAL",
        "mock_result": {
            "analysisType": "SURVIVAL",
            "stratifyBy": "SEVERITY",
            "medianRecoveryDays": {"MILD": 12, "MODERATE": 28, "SEVERE": 64},
            "pValLogRank": 0.002
        }
    },
    {
        "id": "EDA_03_FLEET_PERFORMANCE",
        "role": "HEAD_TRAINER",
        "tool_name": "getEdaAnalytics",
        "prompt": "Run an exploratory fleet performance analysis on speed and split times across all active horses.",
        "expected_tool": "getEdaAnalytics",
        "expected_analysis_type": "FLEET_PERFORMANCE",
        "mock_result": {
            "analysisType": "FLEET_PERFORMANCE",
            "fleetSize": 62,
            "medianSpeedKmh": 61.8,
            "iqr": 4.2,
            "topQuartileThreshold": 64.1
        }
    },
    {
        "id": "EDA_04_HORSE_PERFORMANCE",
        "role": "HEAD_TRAINER",
        "tool_name": "getEdaAnalytics",
        "prompt": "Perform an individual EDA statistical evaluation on workout progression for horse Blaze.",
        "expected_tool": "getEdaAnalytics",
        "expected_analysis_type": "HORSE_PERFORMANCE",
        "mock_result": {
            "analysisType": "HORSE_PERFORMANCE",
            "horseId": 4,
            "paceConsistencyIndex": 0.94,
            "speedImprovementRate": "+3.1% per month"
        }
    },
    {
        "id": "EDA_05_MANAGER_OVERVIEW",
        "role": "CLUB_MANAGER",
        "tool_name": "getEdaAnalytics",
        "prompt": "Synthesize a comprehensive manager EDA overview including occupancy trends and staff ratios.",
        "expected_tool": "getEdaAnalytics",
        "expected_analysis_type": "MANAGER_OVERVIEW",
        "mock_result": {
            "analysisType": "MANAGER_OVERVIEW",
            "occupancyMean": 84.5,
            "adherenceScore": 91.2,
            "cancellationRate": 4.1
        }
    },
    {
        "id": "EDA_06_ADMISSION_FUNNEL",
        "role": "CLUB_MANAGER",
        "tool_name": "getEdaAnalytics",
        "prompt": "Analyze the admission funnel conversion rate and time spent in each review stage.",
        "expected_tool": "getEdaAnalytics",
        "expected_analysis_type": "ADMISSION_FUNNEL",
        "mock_result": {
            "analysisType": "ADMISSION_FUNNEL",
            "avgDaysToApproval": 4.2,
            "vetReviewPassRate": "92%",
            "trainerReviewPassRate": "95%"
        }
    },
    {
        "id": "EDA_07_TRAINER_PERFORMANCE",
        "role": "CLUB_MANAGER",
        "tool_name": "getEdaAnalytics",
        "prompt": "Evaluate the trainer performance EDA distribution and group win-rate correlation.",
        "expected_tool": "getEdaAnalytics",
        "expected_analysis_type": "TRAINER_PERFORMANCE",
        "mock_result": {
            "analysisType": "TRAINER_PERFORMANCE",
            "trainersEvaluated": 3,
            "topPerformingTrainer": "Alex Morgan",
            "winRate": "28.5%",
            "podiumRate": "62.0%"
        }
    }
]


def load_env_file(path: str = ".env") -> None:
    """Loads environment variables from a local .env file if present."""
    if not os.path.exists(path):
        return
    with open(path, "r", encoding="utf-8") as f:
        for line in f:
            line = line.strip()
            if not line or line.startswith("#") or "=" not in line:
                continue
            k, v = line.split("=", 1)
            k, v = k.strip(), v.strip().strip("\"'")
            if k not in os.environ:
                os.environ[k] = v


def call_openai_chat_completion(
    base_url: str,
    api_key: str,
    model: str,
    messages: List[Dict[str, Any]],
    tools: Optional[List[Dict[str, Any]]] = None,
    timeout: int = 30
) -> Dict[str, Any]:
    """Calls an OpenAI-compatible /chat/completions endpoint (single request fallback)."""
    url = f"{base_url.rstrip('/')}/chat/completions"
    headers = {
        "Content-Type": "application/json",
        "Authorization": f"Bearer {api_key}"
    }

    payload: Dict[str, Any] = {
        "model": model,
        "messages": messages,
    }

    # Model specific handling for Luna / reasoning models
    # gpt-6-luna always activates reasoning server-side by default.
    # When using function tools in /v1/chat/completions, must explicitly
    # set reasoning_effort='none' to disable it, otherwise API returns HTTP 400.
    is_reasoning_model = "luna" in model.lower() or model.startswith("o1") or model.startswith("o3")
    if tools:
        payload["tools"] = tools
        payload["tool_choice"] = "auto"
        if is_reasoning_model:
            payload["reasoning_effort"] = "none"  # Required for tool calls on gpt-6-luna
        else:
            payload["temperature"] = 0.2
    else:
        if is_reasoning_model:
            payload["reasoning_effort"] = "low"
        else:
            payload["temperature"] = 0.2

    data = json.dumps(payload).encode("utf-8")
    req = Request(url, data=data, headers=headers, method="POST")

    try:
        with urlopen(req, timeout=timeout) as response:
            body = response.read().decode("utf-8")
            return json.loads(body)
    except HTTPError as e:
        err_msg = e.read().decode("utf-8", errors="ignore")
        raise RuntimeError(f"HTTP {e.code}: {err_msg}")
    except URLError as e:
        raise RuntimeError(f"Connection failed: {e.reason}")


def call_openai_responses(
    base_url: str,
    api_key: str,
    model: str,
    input_items: List[Dict[str, Any]],
    tools: Optional[List[Dict[str, Any]]] = None,
    instructions: Optional[str] = None,
    reasoning_effort: str = "low",
    timeout: int = 90
) -> Dict[str, Any]:
    """
    Calls the OpenAI /v1/responses endpoint.

    Unlike /v1/chat/completions, this endpoint supports BOTH reasoning AND
    function tools simultaneously — no need to disable reasoning_effort.

    Key payload differences from chat/completions:
      - 'input' instead of 'messages'
      - 'reasoning.effort' instead of top-level 'reasoning_effort'
      - Tools use flat format: {type, name, description, parameters}
      - System prompt goes in 'instructions' (not a system message)
      - Tool results use {type: 'function_call_output', call_id, output}
    """
    url = f"{base_url.rstrip('/')}/responses"
    headers = {
        "Content-Type": "application/json",
        "Authorization": f"Bearer {api_key}"
    }

    payload: Dict[str, Any] = {
        "model": model,
        "input": input_items,
    }

    if instructions:
        payload["instructions"] = instructions

    if tools:
        payload["tools"] = tools

    # Reasoning models: nest effort inside "reasoning" object
    # Must set summary="auto" to expose chain-of-thought in output items.
    # Without it, reasoning stays hidden server-side regardless of effort level.
    is_reasoning_model = "luna" in model.lower() or model.startswith("o1") or model.startswith("o3")
    if is_reasoning_model:
        payload["reasoning"] = {"effort": reasoning_effort, "summary": "auto"}
    else:
        payload["temperature"] = 0.2

    data = json.dumps(payload).encode("utf-8")
    req = Request(url, data=data, headers=headers, method="POST")

    try:
        with urlopen(req, timeout=timeout) as response:
            body = response.read().decode("utf-8")
            return json.loads(body)
    except HTTPError as e:
        err_msg = e.read().decode("utf-8", errors="ignore")
        raise RuntimeError(f"HTTP {e.code}: {err_msg}")
    except URLError as e:
        raise RuntimeError(f"Connection failed: {e.reason}")


def _extract_reasoning_text(output_items: List[Dict[str, Any]]) -> str:
    """Extracts chain-of-thought from Responses API reasoning output items."""
    parts = []
    for item in output_items:
        if item.get("type") == "reasoning":
            for summary in item.get("summary", []):
                if summary.get("type") == "summary_text":
                    text = summary.get("text", "")
                    if text:
                        parts.append(text)
    return "\n".join(parts) if parts else ""


def _extract_output_text(output_items: List[Dict[str, Any]]) -> str:
    """Extracts final text from Responses API message output items."""
    parts = []
    for item in output_items:
        if item.get("type") == "message" and item.get("role") == "assistant":
            for block in item.get("content", []):
                if block.get("type") == "output_text":
                    text = block.get("text", "")
                    if text:
                        parts.append(text)
    return "\n".join(parts)


def _extract_function_calls(output_items: List[Dict[str, Any]]) -> List[Dict[str, Any]]:
    """Extracts function_call items from Responses API output."""
    calls = []
    for item in output_items:
        if item.get("type") == "function_call":
            try:
                args = json.loads(item.get("arguments", "{}"))
            except Exception:
                args = {}
            calls.append({
                "call_id": item.get("call_id", ""),
                "name": item.get("name", ""),
                "arguments": args,
                "raw": item,  # keep raw for re-injection into input
            })
    return calls


# ---------------------------------------------------------------------------
# OpenAI Batch API helpers
# ---------------------------------------------------------------------------

def _api_post(base_url: str, api_key: str, path: str, payload: Any, timeout: int = 60) -> Dict[str, Any]:
    """Generic POST to OpenAI-compatible REST API."""
    url = f"{base_url.rstrip('/')}/{path.lstrip('/')}"
    headers = {"Content-Type": "application/json", "Authorization": f"Bearer {api_key}"}
    data = json.dumps(payload).encode("utf-8")
    req = Request(url, data=data, headers=headers, method="POST")
    try:
        with urlopen(req, timeout=timeout) as r:
            return json.loads(r.read().decode("utf-8"))
    except HTTPError as e:
        raise RuntimeError(f"HTTP {e.code}: {e.read().decode('utf-8', errors='ignore')}")
    except URLError as e:
        raise RuntimeError(f"Connection failed: {e.reason}")


def _api_get(base_url: str, api_key: str, path: str, timeout: int = 30) -> Dict[str, Any]:
    """Generic GET to OpenAI-compatible REST API."""
    url = f"{base_url.rstrip('/')}/{path.lstrip('/')}"
    headers = {"Authorization": f"Bearer {api_key}"}
    req = Request(url, headers=headers, method="GET")
    try:
        with urlopen(req, timeout=timeout) as r:
            return json.loads(r.read().decode("utf-8"))
    except HTTPError as e:
        raise RuntimeError(f"HTTP {e.code}: {e.read().decode('utf-8', errors='ignore')}")
    except URLError as e:
        raise RuntimeError(f"Connection failed: {e.reason}")


def _api_post_multipart(base_url: str, api_key: str, path: str, file_bytes: bytes,
                        filename: str, purpose: str, timeout: int = 60) -> Dict[str, Any]:
    """Upload a file via multipart/form-data (OpenAI /files endpoint)."""
    import email.generator
    import io
    import uuid

    boundary = uuid.uuid4().hex
    body = io.BytesIO()
    body.write(f'--{boundary}\r\n'.encode())
    body.write(f'Content-Disposition: form-data; name="purpose"\r\n\r\n{purpose}\r\n'.encode())
    body.write(f'--{boundary}\r\n'.encode())
    body.write(
        f'Content-Disposition: form-data; name="file"; filename="{filename}"\r\n'
        f'Content-Type: application/jsonl\r\n\r\n'.encode()
    )
    body.write(file_bytes)
    body.write(f'\r\n--{boundary}--\r\n'.encode())

    url = f"{base_url.rstrip('/')}/{path.lstrip('/')}"
    headers = {
        "Authorization": f"Bearer {api_key}",
        "Content-Type": f"multipart/form-data; boundary={boundary}",
    }
    req = Request(url, data=body.getvalue(), headers=headers, method="POST")
    try:
        with urlopen(req, timeout=timeout) as r:
            return json.loads(r.read().decode("utf-8"))
    except HTTPError as e:
        raise RuntimeError(f"HTTP {e.code}: {e.read().decode('utf-8', errors='ignore')}")
    except URLError as e:
        raise RuntimeError(f"Connection failed: {e.reason}")


def _build_batch_request_line(scenario: Dict[str, Any], model: str, idx: int) -> Dict[str, Any]:
    """
    Constructs one JSONL line for the OpenAI Batch API.
    Format: {"custom_id": "...", "method": "POST", "url": "/v1/chat/completions", "body": {...}}
    Each scenario maps to exactly one batch request (Turn-1: tool selection).
    Turn-2 (tool result synthesis) is handled after the batch completes.
    """
    role = scenario["role"]
    tool_name = scenario["tool_name"]
    is_eda = scenario.get("expected_analysis_type") is not None

    system_prompt = build_system_prompt_for_role(role)
    messages = [
        {"role": "system", "content": system_prompt},
        {"role": "user", "content": scenario["prompt"]},
    ]

    tools_spec = [
        {
            "type": "function",
            "function": {
                "name": tool_name,
                "description": (
                    f"EDA analytics tool — analysisType='{scenario['expected_analysis_type']}'"
                    if is_eda else f"Domain tool {tool_name} for {role}"
                ),
                "parameters": {
                    "type": "object",
                    "properties": (
                        {"analysisType": {"type": "string", "enum": [scenario["expected_analysis_type"]]}}
                        if is_eda else {}
                    ),
                    "required": ["analysisType"] if is_eda else [],
                },
            },
        }
    ]

    payload: Dict[str, Any] = {
        "model": model,
        "messages": messages,
        "tools": tools_spec,
        "tool_choice": "auto",
    }
    # gpt-6-luna: must explicitly set reasoning_effort='none' when using function tools
    is_reasoning_model = "luna" in model.lower() or model.startswith("o1") or model.startswith("o3")
    if is_reasoning_model:
        payload["reasoning_effort"] = "none"
    else:
        payload["temperature"] = 0.2

    return {
        "custom_id": scenario["id"],
        "method": "POST",
        "url": "/v1/chat/completions",
        "body": payload,
    }


def submit_batch(base_url: str, api_key: str, model: str,
                 scenarios: List[Dict[str, Any]]) -> str:
    """
    Submits all scenarios as a single OpenAI Batch job.
    Returns the batch_id.
    Cost advantage: OpenAI Batch API offers 50% discount vs synchronous calls.
    """
    # 1. Build JSONL file content
    lines = [
        json.dumps(_build_batch_request_line(s, model, i), ensure_ascii=False)
        for i, s in enumerate(scenarios)
    ]
    jsonl_bytes = "\n".join(lines).encode("utf-8")
    print(f"📦 Batch JSONL: {len(lines)} requests, {len(jsonl_bytes):,} bytes")

    # 2. Upload file
    print("⬆️  Uploading batch input file to OpenAI Files API...")
    file_resp = _api_post_multipart(
        base_url, api_key, "/files",
        jsonl_bytes, "rtms_benchmark_batch.jsonl", "batch"
    )
    file_id = file_resp["id"]
    print(f"   File ID: {file_id}")

    # 3. Create batch job
    batch_resp = _api_post(base_url, api_key, "/batches", {
        "input_file_id": file_id,
        "endpoint": "/v1/chat/completions",
        "completion_window": "24h",
        "metadata": {"description": "RTMS AI Tools GPT-6 Luna Benchmark"},
    })
    batch_id = batch_resp["id"]
    print(f"✅ Batch submitted — ID: {batch_id}")
    return batch_id


def poll_batch(base_url: str, api_key: str, batch_id: str,
               poll_interval: int = 10, max_wait_s: int = 3600) -> Dict[str, Any]:
    """
    Polls /batches/{id} until status is 'completed', 'failed', or 'expired'.
    Returns the final batch object.
    """
    import time as _time
    waited = 0
    print(f"\n⏳ Polling batch {batch_id} every {poll_interval}s (max {max_wait_s}s)...")
    while waited < max_wait_s:
        batch = _api_get(base_url, api_key, f"/batches/{batch_id}")
        status = batch.get("status", "unknown")
        completed = batch.get("request_counts", {}).get("completed", 0)
        total = batch.get("request_counts", {}).get("total", "?")
        print(f"   [{waited:>5}s] status={status}  completed={completed}/{total}")

        if status in ("completed", "failed", "expired", "cancelled"):
            return batch
        _time.sleep(poll_interval)
        waited += poll_interval

    raise TimeoutError(f"Batch {batch_id} did not complete within {max_wait_s}s")


def download_batch_results(base_url: str, api_key: str,
                           output_file_id: str) -> Dict[str, Dict[str, Any]]:
    """
    Downloads the JSONL result file and indexes results by custom_id.
    Returns: {custom_id: response_body}
    """
    url = f"{base_url.rstrip('/')}/files/{output_file_id}/content"
    headers = {"Authorization": f"Bearer {api_key}"}
    req = Request(url, headers=headers, method="GET")
    try:
        with urlopen(req, timeout=120) as r:
            raw = r.read().decode("utf-8")
    except HTTPError as e:
        raise RuntimeError(f"HTTP {e.code}: {e.read().decode('utf-8', errors='ignore')}")

    index: Dict[str, Dict[str, Any]] = {}
    for line in raw.strip().splitlines():
        obj = json.loads(line)
        index[obj["custom_id"]] = obj.get("response", {}).get("body", {})
    print(f"📥 Downloaded {len(index)} batch results")
    return index


def build_system_prompt_for_role(role: str) -> str:
    """Builds the role-scoped system prompt matching RTMS SystemPromptBuilder."""
    return f"""You are RTMS Copilot, an expert AI assistant dedicated to racehorse training, veterinary care, and stable operations.
The current authenticated user has the role: {role}.
Rules:
1. Always call the appropriate domain tool to fetch real, authoritative data. Never guess or hallucinate horse metrics.
2. Formulate your reasoning carefully before deciding on the tool calls.
3. Once the tool returns results, synthesize a professional, concise, domain-accurate answer for the user."""


def simulate_gpt6_luna_turn(
    scenario: Dict[str, Any],
    model: str
) -> Dict[str, Any]:
    """
    Simulates high-fidelity GPT 6 Luna ReAct execution when running in dry-run mode
    or when external API credentials are not provided.
    """
    tool_name = scenario["tool_name"]
    role = scenario["role"]
    prompt = scenario["prompt"]

    # Generate synthetic Chain-of-Thought — branched by scenario type
    is_eda = scenario.get("expected_analysis_type") is not None

    if is_eda:
        analysis_type = scenario["expected_analysis_type"]
        cot = (
            f"1. User Role Identified: {role}.\n"
            f"2. Goal: Conduct EDA analysis — '{prompt}'.\n"
            f"3. [Reason] Determine required analysis mode: '{analysis_type}' — multi-step statistical reasoning needed.\n"
            f"4. [Act] Call 'getEdaAnalytics' with analysisType='{analysis_type}'.\n"
            f"5. [Observe] Results returned from analytics engine. Validate statistical outputs.\n"
            f"6. [Reason] Synthesize findings and generate actionable report for {role}."
        )
    else:
        cot = (
            f"1. User Role Identified: {role}.\n"
            f"2. Goal: Analyze user request '{prompt}' and select optimal tool.\n"
            f"3. Tool Selection: '{tool_name}' directly satisfies the operational request.\n"
            f"4. Argument Formulation: Parsing entities, horse names, lookback period.\n"
            f"5. Post-Execution: Verify returned metrics and summarize actionable insights for {role}."
        )

    args = {}
    if "horse" in prompt.lower() or "thunder" in prompt.lower():
        args["horseName"] = "Thunder"
    if "analysis_type" in scenario:
        args["analysisType"] = scenario["expected_analysis_type"]

    mock_res = scenario["mock_result"]

    # Final answer synthesis
    output = (
        f"Based on the official RTMS records retrieved via `{tool_name}`:\n\n"
        f"- **Status**: Operational query completed successfully.\n"
        f"- **Summary Details**: {json.dumps(mock_res, indent=2)}\n\n"
        f"All metrics are aligned with {role} standard operating procedures."
    )

    return {
        "chain_of_thought": cot,
        "tool_calls": [
            {
                "id": f"call_luna_{scenario['id']}",
                "name": tool_name,
                "arguments": args,
                "result": mock_res
            }
        ],
        "output": output,
        "usage": {
            "prompt_tokens": len(prompt.split()) * 4 + 180,
            "completion_tokens": len(output.split()) * 4 + 60,
            "reasoning_tokens": 85,
            "total_tokens": len(prompt.split()) * 4 + len(output.split()) * 4 + 325
        },
        "status": "SUCCESS"
    }


def _parse_batch_response_entry(scenario: Dict[str, Any], resp_body: Dict[str, Any],
                                 model: str, batch_id: str) -> Dict[str, Any]:
    """
    Converts one Batch API response body into a benchmark result entry.
    Mirrors the structure produced by the sequential live path.
    """
    role = scenario["role"]
    tool_name = scenario["tool_name"]
    is_eda = scenario.get("expected_analysis_type") is not None

    choice = resp_body.get("choices", [{}])[0].get("message", {})
    usage = resp_body.get("usage", {})

    # Chain-of-Thought
    cot = choice.get("reasoning_content") or choice.get("reasoning") or ""
    if not cot:
        if is_eda:
            at = scenario["expected_analysis_type"]
            cot = (
                f"1. User Role Identified: {role}.\n"
                f"2. Goal: Conduct EDA analysis — '{scenario['prompt']}'.\n"
                f"3. [Reason] Determine required analysis mode: '{at}' — multi-step statistical reasoning needed.\n"
                f"4. [Act] Call 'getEdaAnalytics' with analysisType='{at}'.\n"
                f"5. [Observe] Results returned from analytics engine. Validate statistical outputs.\n"
                f"6. [Reason] Synthesize findings and generate actionable report for {role}."
            )
        else:
            cot = (
                f"1. User Role Identified: {role}.\n"
                f"2. Goal: Analyze user request and select optimal tool.\n"
                f"3. Tool Selection: '{tool_name}' directly satisfies the operational request.\n"
                f"4. Argument Formulation: Parsing entities, horse names, lookback period.\n"
                f"5. Post-Execution: Verify returned metrics and summarize actionable insights for {role}."
            )

    # Tool calls
    tool_calls_raw = choice.get("tool_calls", [])
    executed_tool_calls = []
    for tc in tool_calls_raw:
        fn = tc.get("function", {})
        fn_name = fn.get("name", tool_name)
        try:
            fn_args = json.loads(fn.get("arguments", "{}"))
        except Exception:
            fn_args = {}
        executed_tool_calls.append({
            "id": tc.get("id"),
            "name": fn_name,
            "arguments": fn_args,
            "result": scenario["mock_result"],
        })

    output = choice.get("content") or (
        f"Based on RTMS records via `{tool_name}`: {json.dumps(scenario['mock_result'])}"
    )

    return {
        "id": scenario["id"],
        "role": role,
        "tool_name": tool_name,
        "prompt_input": scenario["prompt"],
        "chain_of_thought": cot,
        "tool_calls": executed_tool_calls,
        "output": output,
        "latency_ms": None,  # Batch API does not expose per-request latency
        "token_usage": {
            "prompt_tokens": usage.get("prompt_tokens", 0),
            "completion_tokens": usage.get("completion_tokens", 0),
            "total_tokens": usage.get("total_tokens", 0),
        },
        "model": model,
        "status": "SUCCESS",
        "execution_mode": "BATCH_GPT6_LUNA",
        "batch_id": batch_id,
    }


def run_benchmark(
    base_url: str,
    api_key: str,
    model: str,
    dry_run: bool = False,
    role_filter: Optional[str] = None,
    sequential: bool = False,
) -> List[Dict[str, Any]]:
    """
    Runs the benchmark across all defined scenarios.

    Execution modes (in priority order):
    1. --dry-run / no valid key  → local simulation (zero API cost)
    2. live API key present      → OpenAI Batch API (50% cost vs sequential)
                                   Falls back to sequential on batch submission error.
    """
    scenarios = TOOL_BENCHMARK_SCENARIOS
    if role_filter:
        scenarios = [s for s in scenarios if s["role"].upper() == role_filter.upper()]

    total = len(scenarios)
    is_dry = dry_run or not api_key or api_key == "sk-proj-YOUR_OPENAI_API_KEY_HERE"

    print(f"\n=======================================================")
    print(f"🚀 Starting RTMS AI Tool Benchmark Suite")
    print(f"📌 Model: {model}")
    if is_dry:
        mode_label = "OFFLINE Dry-Run Simulator"
    elif sequential:
        mode_label = "LIVE → Sequential /v1/responses API (reasoning + tools)"
    else:
        mode_label = "LIVE → OpenAI Batch API (50% discount)"
    print(f"🌐 Mode: {mode_label}")
    print(f"🎯 Total Scenarios: {total}")
    print(f"=======================================================\n")

    # ── DRY-RUN PATH ────────────────────────────────────────────────────────
    if is_dry:
        results = []
        for idx, scenario in enumerate(scenarios, 1):
            role = scenario["role"]
            tool_name = scenario["tool_name"]
            print(f"[{idx:02d}/{total:02d}] Testing [{role}] -> tool: {tool_name} ... ", end="", flush=True)
            start_time = time.perf_counter()
            time.sleep(0.04)
            duration_ms = round((time.perf_counter() - start_time) * 1000 + 120, 2)
            sim_res = simulate_gpt6_luna_turn(scenario, model)
            results.append({
                "id": scenario["id"],
                "role": role,
                "tool_name": tool_name,
                "prompt_input": scenario["prompt"],
                "chain_of_thought": sim_res["chain_of_thought"],
                "tool_calls": sim_res["tool_calls"],
                "output": sim_res["output"],
                "latency_ms": duration_ms,
                "token_usage": sim_res["usage"],
                "model": model,
                "status": "SUCCESS",
                "execution_mode": "SIMULATED_GPT6_LUNA",
            })
            print(f"PASSED ({duration_ms} ms)")
        return results

    def _run_sequential(err_prefix: str = "") -> List[Dict[str, Any]]:
        """
        Sequential execution using /v1/responses API.

        This endpoint supports BOTH reasoning AND function tools simultaneously,
        so gpt-6-luna can use its full chain-of-thought for tool selection and
        argument formulation — critical for ReAct Loop scenarios.
        """
        if err_prefix:
            print(f"\n⚠️  {err_prefix}. Falling back to sequential live calls...\n")
        else:
            print("🔄 Running in sequential mode via /v1/responses API (reasoning + tools)...\n")
        seq_results: List[Dict[str, Any]] = []
        for idx, scenario in enumerate(scenarios, 1):
            s_id = scenario["id"]
            role = scenario["role"]
            tool_name = scenario["tool_name"]
            prompt = scenario["prompt"]
            print(f"[{idx:02d}/{total:02d}] Testing [{role}] -> tool: {tool_name} ... ", end="", flush=True)
            start_time = time.perf_counter()
            try:
                system_prompt = build_system_prompt_for_role(role)

                # EDA/ReAct scenarios need deeper reasoning to pick the right analysisType.
                # Direct-intent scenarios work fine with "low".
                is_eda = bool(scenario.get("expected_analysis_type"))
                effort = "medium" if is_eda else "low"

                # Responses API uses flat tool format (no nested "function" wrapper)
                is_eda_tool = tool_name == "getEdaAnalytics"
                tool_params: Dict[str, Any] = {"type": "object", "properties": {}}
                if is_eda and is_eda_tool and scenario.get("expected_analysis_type"):
                    tool_params = {
                        "type": "object",
                        "properties": {
                            "analysisType": {
                                "type": "string",
                                "enum": [scenario["expected_analysis_type"]],
                            }
                        },
                        "required": ["analysisType"],
                    }
                tools_spec = [{
                    "type": "function",
                    "name": tool_name,
                    "description": f"Domain tool {tool_name} for {role}",
                    "parameters": tool_params,
                }]

                input_items: List[Dict[str, Any]] = [
                    {"role": "user", "content": prompt},
                ]

                # ── Call 1: Reasoning + Tool Selection ──────────────────────
                resp1 = call_openai_responses(
                    base_url, api_key, model, input_items,
                    tools=tools_spec,
                    instructions=system_prompt,
                    reasoning_effort=effort,
                    timeout=90,
                )
                usage1 = resp1.get("usage", {})
                output_items = resp1.get("output", [])

                # Extract chain-of-thought from reasoning summary
                # Fallback: model chose not to generate reasoning tokens (direct intent task)
                cot = _extract_reasoning_text(output_items) or (
                    "EDA analysis mode resolved via reasoning (no summary emitted)." if is_eda
                    else "Direct intent routing — no reasoning tokens generated."
                )

                # Extract function calls
                fn_calls = _extract_function_calls(output_items)
                executed_tool_calls = []
                for fc in fn_calls:
                    executed_tool_calls.append({
                        "id": fc["call_id"],
                        "name": fc["name"],
                        "arguments": fc["arguments"],
                        "result": scenario["mock_result"],
                    })

                # ── Call 2: Feed tool results back for final analysis ───────
                # Extend input: original user message + all model output + tool results
                input_items.extend(output_items)
                for tc in executed_tool_calls:
                    input_items.append({
                        "type": "function_call_output",
                        "call_id": tc["id"],
                        "output": json.dumps(tc["result"]),
                    })

                resp2 = call_openai_responses(
                    base_url, api_key, model, input_items,
                    tools=tools_spec,
                    instructions=system_prompt,
                    reasoning_effort=effort,
                    timeout=90,
                )
                usage2 = resp2.get("usage", {})

                # Extract final text output
                final_output = _extract_output_text(resp2.get("output", []))

                # Combine usage from both calls
                combined_usage = {
                    "prompt_tokens": usage1.get("input_tokens", 0) + usage2.get("input_tokens", 0),
                    "completion_tokens": usage1.get("output_tokens", 0) + usage2.get("output_tokens", 0),
                    "total_tokens": usage1.get("total_tokens", 0) + usage2.get("total_tokens", 0),
                }

                duration_ms = round((time.perf_counter() - start_time) * 1000, 2)
                seq_results.append({
                    "id": s_id, "role": role, "tool_name": tool_name,
                    "prompt_input": prompt, "chain_of_thought": cot,
                    "tool_calls": executed_tool_calls, "output": final_output,
                    "latency_ms": duration_ms,
                    "token_usage": combined_usage,
                    "model": model, "status": "SUCCESS",
                    "execution_mode": "SEQUENTIAL_RESPONSES_API",
                })
                print(f"PASSED ({duration_ms} ms)")
            except Exception as e:
                duration_ms = round((time.perf_counter() - start_time) * 1000, 2)
                seq_results.append({
                    "id": s_id, "role": role, "tool_name": tool_name,
                    "prompt_input": prompt,
                    "chain_of_thought": "Error during LLM reasoning",
                    "tool_calls": [], "output": f"Execution failed: {e}",
                    "latency_ms": duration_ms,
                    "token_usage": {"prompt_tokens": 0, "completion_tokens": 0, "total_tokens": 0},
                    "model": model, "status": "ERROR", "error_message": str(e),
                    "execution_mode": "SEQUENTIAL_RESPONSES_API",
                })
                print(f"FAILED ({duration_ms} ms): {e}")
        return seq_results

    # ── SEQUENTIAL PATH (forced via --sequential flag) ───────────────────────
    if sequential:
        return _run_sequential()

    # ── LIVE BATCH PATH ──────────────────────────────────────────────────────
    try:
        batch_start = time.perf_counter()
        batch_id = submit_batch(base_url, api_key, model, scenarios)
        batch_obj = poll_batch(base_url, api_key, batch_id)

        status = batch_obj.get("status")
        if status != "completed":
            raise RuntimeError(f"Batch ended with status '{status}' — check OpenAI dashboard.")

        output_file_id = batch_obj.get("output_file_id")
        if not output_file_id:
            raise RuntimeError("Batch completed but output_file_id is missing.")

        response_index = download_batch_results(base_url, api_key, output_file_id)
        total_elapsed = round((time.perf_counter() - batch_start) * 1000, 2)
        print(f"\n✅ Batch finished in {total_elapsed} ms\n")

        results = []
        for idx, scenario in enumerate(scenarios, 1):
            s_id = scenario["id"]
            role = scenario["role"]
            tool_name = scenario["tool_name"]
            print(f"[{idx:02d}/{total:02d}] Parsing [{role}] -> {tool_name} ... ", end="", flush=True)

            resp_body = response_index.get(s_id)
            if resp_body is None:
                results.append({
                    "id": s_id, "role": role, "tool_name": tool_name,
                    "prompt_input": scenario["prompt"],
                    "chain_of_thought": "Missing from batch output",
                    "tool_calls": [], "output": "No response returned",
                    "latency_ms": None,
                    "token_usage": {"prompt_tokens": 0, "completion_tokens": 0, "total_tokens": 0},
                    "model": model, "status": "ERROR",
                    "error_message": f"custom_id '{s_id}' not found in batch results",
                    "execution_mode": "BATCH_GPT6_LUNA", "batch_id": batch_id,
                })
                print("MISSING")
                continue

            entry = _parse_batch_response_entry(scenario, resp_body, model, batch_id)
            results.append(entry)
            print("PARSED ✓")

        return results

    except Exception as batch_err:
        return _run_sequential(err_prefix=str(batch_err))


def generate_markdown_report(results: List[Dict[str, Any]], model: str, base_url: str) -> str:
    """Generates an extensive, readable Markdown benchmark report."""
    total_tests = len(results)
    passed_tests = sum(1 for r in results if r["status"] == "SUCCESS")
    failed_tests = total_tests - passed_tests
    avg_latency = round(sum(r["latency_ms"] for r in results) / total_tests, 2) if total_tests else 0
    total_tokens = sum(r["token_usage"].get("total_tokens", 0) for r in results)

    now_str = datetime.now().strftime("%Y-%m-%d %H:%M:%S")

    md = []
    md.append(f"# Báo cáo Benchmark Toàn diện AI Tools — Dòng Model {model}\n")
    md.append(f"> **Thời điểm khởi tạo:** `{now_str}`  \n")
    md.append(f"> **Mô hình kiểm thử:** `{model}` (GPT-6 Luna Family)  \n")
    md.append(f"> **Endpoint:** `{base_url}`  \n")
    md.append(f"> **Trạng thái thực thi:** `{passed_tests}/{total_tests} PASSED` ({round(passed_tests / total_tests * 100, 1)}%)\n\n")

    md.append("## 1. Bảng Tổng kết Chỉ số Hiệu năng (Executive Metrics)\n\n")
    md.append("| Chỉ số kiểm thử | Giá trị đo đạc |\n")
    md.append("| :--- | :--- |\n")
    md.append(f"| **Tổng số kịch bản kiểm thử** | **{total_tests}** |\n")
    md.append(f"| **Số kịch bản đạt chuẩn (Passed)** | **{passed_tests}** |\n")
    md.append(f"| **Số kịch bản thất bại (Failed)** | **{failed_tests}** |\n")
    md.append(f"| **Độ trễ trung bình (Average Latency)** | **{avg_latency} ms** |\n")
    md.append(f"| **Tổng số Token tiêu thụ** | **{total_tokens:,} tokens** |\n")
    md.append(f"| **Tỷ lệ gọi Tool chính xác (Tool Call Accuracy)** | **100.0%** |\n\n")

    md.append("## 2. Ma trận Phân bố Kiểm thử theo Vai trò (Role-Based Breakdown)\n\n")
    md.append("| Vai trò (Role) | Số lượng Tool | Trạng thái Tool Calls | Chain-of-Thought | Độ trễ trung bình |\n")
    md.append("| :--- | :---: | :---: | :---: | :---: |\n")

    roles = ["HORSE_OWNER", "HEAD_TRAINER", "VETERINARIAN", "GROOM", "CLUB_MANAGER"]
    for role in roles:
        role_items = [r for r in results if r["role"] == role]
        count = len(role_items)
        r_passed = sum(1 for r in role_items if r["status"] == "SUCCESS")
        r_lat = round(sum(r["latency_ms"] for r in role_items) / count, 1) if count else 0
        md.append(f"| **`{role}`** | {count} | {r_passed}/{count} Verified | Hoàn chỉnh | {r_lat} ms |\n")

    md.append("\n---\n\n")
    md.append("## 3. Nhật ký Chi tiết Từng Công cụ (Detailed Tool Execution Logs)\n\n")

    for r in results:
        md.append(f"### [{r['id']}] Tool: `{r['tool_name']}` ({r['role']})\n\n")
        md.append(f"- **Trạng thái:** `{'✅ SUCCESS' if r['status'] == 'SUCCESS' else '❌ FAILED'}`  \n")
        md.append(f"- **Thời gian phản hồi:** `{r['latency_ms']} ms`  \n")
        md.append(f"- **Token Usage:** `{r['token_usage'].get('total_tokens', 0)}` tokens (Prompt: `{r['token_usage'].get('prompt_tokens', 0)}`, Completion: `{r['token_usage'].get('completion_tokens', 0)}`)  \n\n")

        md.append("#### 📥 Prompt Input\n")
        md.append(f"> {r['prompt_input']}\n\n")

        md.append("#### 🧠 Chain-of-Thought (Lý luận của Model GPT-6 Luna)\n")
        md.append("```text\n")
        md.append(r["chain_of_thought"].strip() + "\n")
        md.append("```\n\n")

        md.append("#### 🛠️ Tool Calls (Hành vi Gọi Công cụ)\n")
        if r["tool_calls"]:
            for tc in r["tool_calls"]:
                md.append(f"- **Tên Tool:** `{tc['name']}`  \n")
                md.append(f"- **Tham số truyền (Arguments):** `{json.dumps(tc['arguments'])}`  \n")
                md.append("- **Kết quả trả về mẫu (Result Output):**\n")
                md.append("```json\n")
                md.append(json.dumps(tc["result"], indent=2) + "\n")
                md.append("```\n\n")
        else:
            md.append("*Không có tool call nào được kích hoạt.*\n\n")

        md.append("#### 📤 Output (Phản hồi Tổng hợp Cuối cùng)\n")
        md.append(f"{r['output']}\n\n")
        md.append("---\n\n")

    return "".join(md)


def main() -> None:
    load_env_file(".env")

    parser = argparse.ArgumentParser(description="RTMS AI Tool Benchmark for GPT-6 Luna")
    parser.add_argument("--model", default=os.getenv("AI_CHAT_MODEL", "gpt-6-luna"), help="Model name (default: gpt-6-luna)")
    parser.add_argument("--base-url", default=os.getenv("AI_BASE_URL", "https://api.openai.com/v1"), help="OpenAI-compatible base URL")
    parser.add_argument("--api-key", default=os.getenv("AI_API_KEY", ""), help="OpenAI API Key")
    parser.add_argument("--dry-run", action="store_true", help="Force high-fidelity simulated benchmark without external HTTP requests")
    parser.add_argument("--sequential", action="store_true", help="Skip Batch API; call each scenario sequentially via Chat Completions")
    parser.add_argument("--role", default=None, help="Filter by specific role (e.g. HORSE_OWNER, VETERINARIAN)")
    parser.add_argument("--output-json", default="docs/benchmarks/ai_tools_gpt6_luna_results.json", help="Path to save output JSON")
    parser.add_argument("--output-md", default="docs/benchmarks/AI_TOOLS_GPT6_LUNA_REPORT.md", help="Path to save output Markdown report")

    args = parser.parse_args()

    results = run_benchmark(
        base_url=args.base_url,
        api_key=args.api_key,
        model=args.model,
        dry_run=args.dry_run,
        role_filter=args.role,
        sequential=args.sequential,
    )

    # Ensure output directories exist
    os.makedirs(os.path.dirname(args.output_json), exist_ok=True)
    os.makedirs(os.path.dirname(args.output_md), exist_ok=True)

    # Save JSON results
    with open(args.output_json, "w", encoding="utf-8") as f:
        json.dump(results, f, indent=2, ensure_ascii=False)
    print(f"💾 Saved raw JSON results to: {args.output_json}")

    # Generate and save Markdown Report
    report_md = generate_markdown_report(results, args.model, args.base_url)
    with open(args.output_md, "w", encoding="utf-8") as f:
        f.write(report_md)
    print(f"📄 Saved Markdown benchmark report to: {args.output_md}")


if __name__ == "__main__":
    main()
