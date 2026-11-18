#!/usr/bin/env python3
"""
benchmark_ai_react_loop.py
Comprehensive Benchmark and Stateful Evaluation Harness for the RTMS ReAct Agent Loop.

Targets:
  - gpt-6-luna (OpenAI /v1/responses endpoint with CoT reasoning and /v1/chat/completions fallback)
  - Stateful Mock World State with dynamic relational entity dispatching
  - Full Role-Scoped Tool Catalogs (9–12 canonical tools per role for intent disambiguation)
  - 20 Multi-Step Business Scenarios across 5 roles (HORSE_OWNER, HEAD_TRAINER, VETERINARIAN, GROOM, CLUB_MANAGER)
  - DAG Trajectory & Rubric Scoring:
      Benchmark Score = 0.35 * S_Traj + 0.25 * S_Param + 0.20 * S_Eff + 0.20 * S_Rule
  - Deterministic Dry-Run Oracle Simulation (20/20 verifiable without external network/API key)
"""

import argparse
import json
import os
import re
import sys
import time
from datetime import datetime
from typing import Any, Dict, List, Optional, Set, Tuple
from urllib.error import HTTPError, URLError
from urllib.request import Request, urlopen

# ==============================================================================
# 1. CONSTANTS & SYSTEM RULES
# ==============================================================================

MAX_REACT_ITERATIONS = 4

WEIGHT_TRAJECTORY = 0.35
WEIGHT_PARAMETER = 0.25
WEIGHT_EFFICIENCY = 0.20
WEIGHT_RULE = 0.20

# System route table matching SystemPromptBuilder.java Rule 10
VALID_SYSTEM_ROUTES = {
    # Head Trainer
    "/trainer/schedule",
    "/trainer/plans",
    "/trainer/dashboard",
    "/trainer/admissions",
    "/trainer/stable",
    "/trainer/courses",
    "/trainer/horses",
    # Veterinarian
    "/veterinarian/admissions",
    "/veterinarian/health",
    "/veterinarian/preventive-care",
    "/veterinarian/urgent",
    "/veterinarian/schedule",
    # Club Manager
    "/manager/stable",
    "/manager/staff",
    "/manager/horses",
    "/manager/admissions",
    "/manager/audit-log",
    "/manager/access-control",
    # Groom
    "/groom/tasks",
    "/groom/incidents",
    "/groom/incidents/new",
    "/groom/stable",
    "/groom/admissions",
    # Horse Owner
    "/owner/horses",
    "/owner/admissions",
}

# ==============================================================================
# 2. FULL CANONICAL ROLE TOOL CATALOGS (Matching DynamicToolRegistryService)
# ==============================================================================

ROLE_TOOL_DEFINITIONS: Dict[str, List[Dict[str, Any]]] = {
    "HORSE_OWNER": [
        {
            "name": "getMyHorses",
            "description": "Retrieve the list of horses owned by the current owner (total owned horses totalOwnedHorses, horses cleared for training allowedTrainingHorses, horses restricted for medical reasons restrictedHorses, detailed horse list horseList). Always use when asked how many horses the owner has.",
            "parameters": {"type": "object", "properties": {}, "required": []}
        },
        {
            "name": "getMyHorseHealthSummary",
            "description": "View the overall health summary, training decision, and medical notes for an owned horse.",
            "parameters": {
                "type": "object",
                "properties": {
                    "horseId": {"type": "integer", "description": "Horse ID (optional)"},
                    "horseName": {"type": "string", "description": "Horse name (optional)"}
                },
                "required": []
            }
        },
        {
            "name": "getMyHorseTrainingProgress",
            "description": "View training course progress, training plans, completed workouts, and the training decision for an owned horse.",
            "parameters": {
                "type": "object",
                "properties": {
                    "horseId": {"type": "integer", "description": "Horse ID (optional)"},
                    "horseName": {"type": "string", "description": "Horse name (optional)"}
                },
                "required": []
            }
        },
        {
            "name": "getMyHorseUpcomingSchedule",
            "description": "Look up upcoming training schedules for horses owned by the current owner.",
            "parameters": {
                "type": "object",
                "properties": {
                    "fromDate": {"type": "string", "description": "Start date (YYYY-MM-DD, defaults to today)"},
                    "toDate": {"type": "string", "description": "End date (YYYY-MM-DD, defaults to 14 days ahead)"}
                },
                "required": []
            }
        },
        {
            "name": "getMyHorseAdmissions",
            "description": "Retrieve the owner's admission applications.",
            "parameters": {"type": "object", "properties": {}, "required": []}
        },
        {
            "name": "getHorseCareSchedule",
            "description": "View the upcoming veterinary care schedule for an owned horse.",
            "parameters": {
                "type": "object",
                "properties": {
                    "horseId": {"type": "integer", "description": "Horse ID (optional)"},
                    "horseName": {"type": "string", "description": "Horse name (optional)"}
                },
                "required": []
            }
        },
        {
            "name": "getMyHorseIncidentHistory",
            "description": "View the history of health incidents or minor injuries recorded by staff for an owned horse.",
            "parameters": {
                "type": "object",
                "properties": {
                    "horseId": {"type": "integer", "description": "Horse ID (optional)"},
                    "horseName": {"type": "string", "description": "Horse name (optional)"}
                },
                "required": []
            }
        },
        {
            "name": "getMyHorsePedigreeInfo",
            "description": "Look up pedigree/bloodline information for a horse (Sire, Dam, origin).",
            "parameters": {
                "type": "object",
                "properties": {
                    "horseId": {"type": "integer", "description": "Horse ID (optional)"},
                    "horseName": {"type": "string", "description": "Horse name (optional)"}
                },
                "required": []
            }
        },
        {
            "name": "analyzeHorseInvestmentAndFitnessROI",
            "description": "Generate a combined analysis of fitness development progress and training effectiveness for an owned horse.",
            "parameters": {
                "type": "object",
                "properties": {
                    "horseId": {"type": "integer", "description": "Horse ID (optional)"},
                    "horseName": {"type": "string", "description": "Horse name (optional)"}
                },
                "required": []
            }
        },
        {
            "name": "getEdaAnalytics",
            "description": "Perform exploratory data analysis on horse performance and biomedical data.",
            "parameters": {
                "type": "object",
                "properties": {
                    "analysisType": {
                        "type": "string",
                        "enum": ["HORSE_PERFORMANCE", "BIO_SPC"],
                        "description": "Required analysis mode: HORSE_PERFORMANCE or BIO_SPC"
                    },
                    "horseId": {"type": "integer", "description": "Horse ID (optional)"},
                    "horseName": {"type": "string", "description": "Horse name (optional)"},
                    "days": {"type": "integer", "description": "Lookback period in days (optional)"},
                    "metric": {"type": "string", "enum": ["TEMPERATURE", "RESTING_HEART_RATE"], "description": "Metric to analyze (optional)"}
                },
                "required": ["analysisType"]
            }
        }
    ],
    "HEAD_TRAINER": [
        {
            "name": "getTrainerHorses",
            "description": "Retrieve the horses in training plans managed by the trainer, including assigned groom and stable details.",
            "parameters": {"type": "object", "properties": {}, "required": []}
        },
        {
            "name": "getTrainerGroups",
            "description": "Look up the trainer's Training Groups: each group has up to 6 horses working together, including horse list with grooms, current/upcoming Course Run, and schedule.",
            "parameters": {"type": "object", "properties": {}, "required": []}
        },
        {
            "name": "getWorkoutHistory",
            "description": "Look up up to 20 most recent workouts for a horse (newest first); includes date, session, group, exercise, and outcome.",
            "parameters": {
                "type": "object",
                "properties": {
                    "horseId": {"type": "integer", "description": "Horse ID (optional)"},
                    "horseName": {"type": "string", "description": "Horse name (optional)"}
                },
                "required": []
            }
        },
        {
            "name": "getHorseFitnessTrend",
            "description": "View the horse's fitness trend (speed, heart rate) across workouts, analyze 14-day training load, and warn about overtraining risk.",
            "parameters": {
                "type": "object",
                "properties": {
                    "horseId": {"type": "integer", "description": "Horse ID (optional)"},
                    "horseName": {"type": "string", "description": "Horse name (optional)"}
                },
                "required": []
            }
        },
        {
            "name": "getTrainerSchedule",
            "description": "Look up trainer Sessions by date or date range. Pass needsResultOnly=true to retrieve sessions in the last 90 days with horses missing results.",
            "parameters": {
                "type": "object",
                "properties": {
                    "date": {"type": "string", "description": "Date to query (YYYY-MM-DD)"},
                    "fromDate": {"type": "string", "description": "From date (YYYY-MM-DD)"},
                    "toDate": {"type": "string", "description": "To date (YYYY-MM-DD)"},
                    "needsResultOnly": {"type": "boolean", "description": "true: only sessions with horses missing results in last 90 days"}
                },
                "required": []
            }
        },
        {
            "name": "getTrainerPlansSummary",
            "description": "Look up Course Runs the trainer's horses have joined or are currently attending, one row per horse per course.",
            "parameters": {
                "type": "object",
                "properties": {
                    "status": {"type": "string", "description": "Plan status (ACTIVE, UPCOMING, COMPLETED)"},
                    "horseId": {"type": "integer", "description": "Horse ID (optional)"},
                    "horseName": {"type": "string", "description": "Horse name (optional)"}
                },
                "required": []
            }
        },
        {
            "name": "getHorseSpecialtyAnalysis",
            "description": "Analyze horse specialties (optimal distance, surface, top speed, average rating). Leave blank or pass 'all' to return a RANKING of all horses by average specialty rating (sortBy='rating') or specialty count (sortBy='count').",
            "parameters": {
                "type": "object",
                "properties": {
                    "horseId": {"type": "integer", "description": "Horse ID (optional)"},
                    "horseName": {"type": "string", "description": "Horse name (optional)"},
                    "sortBy": {"type": "string", "description": "Ranking criterion: 'rating' (highest average rating) or 'count' (most specialties)"}
                },
                "required": []
            }
        },
        {
            "name": "getTrainerRiskAlerts",
            "description": "Aggregate injury risk alerts (high heart rate, poor cardiac recovery, declining form) and latest race-readiness assessment for managed horses.",
            "parameters": {
                "type": "object",
                "properties": {
                    "horseId": {"type": "integer", "description": "Horse ID (optional)"},
                    "horseName": {"type": "string", "description": "Horse name (optional)"}
                },
                "required": []
            }
        },
        {
            "name": "getTrainerAssignedGrooms",
            "description": "Look up grooms responsible for areas/stalls managed by trainer, including contact phone, email, assigned stalls, and horses.",
            "parameters": {"type": "object", "properties": {}, "required": []}
        },
        {
            "name": "getAssignedAdmissionReviews",
            "description": "Look up admission applications awaiting trainer assessment of intake capacity and stall allocation.",
            "parameters": {"type": "object", "properties": {}, "required": []}
        },
        {
            "name": "recommendTrainerStrategy",
            "description": "Recommend comprehensive racing strategy (optimal distance, Turf/Dirt) combined with next workout adjustment.",
            "parameters": {
                "type": "object",
                "properties": {
                    "horseId": {"type": "integer", "description": "Horse ID (optional)"},
                    "horseName": {"type": "string", "description": "Horse name (optional)"}
                },
                "required": []
            }
        },
        {
            "name": "getEdaAnalytics",
            "description": "Perform exploratory data analysis on trainer fleet, horse performance, and biomedical data.",
            "parameters": {
                "type": "object",
                "properties": {
                    "analysisType": {
                        "type": "string",
                        "enum": ["FLEET_PERFORMANCE", "HORSE_PERFORMANCE", "BIO_SPC"],
                        "description": "Required analysis mode: FLEET_PERFORMANCE, HORSE_PERFORMANCE, or BIO_SPC"
                    },
                    "horseId": {"type": "integer", "description": "Horse ID (optional)"},
                    "horseName": {"type": "string", "description": "Horse name (optional)"},
                    "days": {"type": "integer", "description": "Lookback period in days (optional)"},
                    "metric": {"type": "string", "enum": ["TEMPERATURE", "RESTING_HEART_RATE"], "description": "BIO_SPC metric (optional)"}
                },
                "required": ["analysisType"]
            }
        }
    ],
    "VETERINARIAN": [
        {
            "name": "searchClinicalKnowledge",
            "description": "[RAG Retrieval] Search clinical veterinary knowledge, medical records, and related symptoms using Vector Similarity Search combined with semantic keywords.",
            "parameters": {
                "type": "object",
                "properties": {
                    "query": {"type": "string", "description": "Symptom description or medical question (required)"},
                    "horseId": {"type": "integer", "description": "Horse ID (optional)"},
                    "horseName": {"type": "string", "description": "Horse name (optional)"},
                    "limit": {"type": "integer", "description": "Number of excerpts (default 5)"}
                },
                "required": ["query"]
            }
        },
        {
            "name": "getVetExaminationOverview",
            "description": "Veterinarian examination overview: in progress, awaiting exam, completed, broken down by horse name.",
            "parameters": {"type": "object", "properties": {}, "required": []}
        },
        {
            "name": "getHorseMedicalHistory",
            "description": "Look up medical records, clinical diagnosis history, and currently effective prescriptions/treatment plans for a horse.",
            "parameters": {
                "type": "object",
                "properties": {
                    "horseId": {"type": "integer", "description": "Horse ID (optional)"},
                    "horseName": {"type": "string", "description": "Horse name (optional)"}
                },
                "required": []
            }
        },
        {
            "name": "getPreventiveCareSchedule",
            "description": "Look up vaccination, deworming, and routine dental care schedules.",
            "parameters": {
                "type": "object",
                "properties": {
                    "horseId": {"type": "integer", "description": "Horse ID (optional)"},
                    "horseName": {"type": "string", "description": "Horse name (optional)"},
                    "fromDate": {"type": "string", "description": "From date (YYYY-MM-DD, optional)"},
                    "toDate": {"type": "string", "description": "To date (YYYY-MM-DD, optional)"}
                },
                "required": []
            }
        },
        {
            "name": "getQuarantineStatus",
            "description": "List of horses under medical quarantine or in the quarantine barn (training status is always BLOCKED).",
            "parameters": {"type": "object", "properties": {}, "required": []}
        },
        {
            "name": "getPendingAdmissionExams",
            "description": "List of admission applications awaiting veterinarian's initial clinical exam to decide approval/block.",
            "parameters": {"type": "object", "properties": {}, "required": []}
        },
        {
            "name": "getUrgentCareAndIncidentAlerts",
            "description": "Look up urgent care duty cases combined with stable health incident reports from the last 48 hours.",
            "parameters": {"type": "object", "properties": {}, "required": []}
        },
        {
            "name": "getHorseVitalSignsTrend",
            "description": "Look up the horse's most recent vital signs (temperature, resting heart rate, weight, respiratory rate). Pass blank or 'all' to receive recent vital signs for all horses.",
            "parameters": {
                "type": "object",
                "properties": {
                    "horseId": {"type": "integer", "description": "Horse ID (optional)"},
                    "horseName": {"type": "string", "description": "Horse name (optional)"}
                },
                "required": []
            }
        },
        {
            "name": "analyzeIncidentPatternsAndRecurringRisks",
            "description": "Analyze horse's incident chain and medical records from the last 6 months to detect recurring chronic disease risk.",
            "parameters": {
                "type": "object",
                "properties": {
                    "horseId": {"type": "integer", "description": "Horse ID (optional)"},
                    "horseName": {"type": "string", "description": "Horse name (optional)"}
                },
                "required": []
            }
        },
        {
            "name": "recommendClinicalInterventionPlan",
            "description": "Recommend step 1 and step 2 clinical intervention plan plus diagnostic tests based on symptoms and RAG medical knowledge base.",
            "parameters": {
                "type": "object",
                "properties": {
                    "symptoms": {"type": "string", "description": "Clinical symptom description (required)"},
                    "horseId": {"type": "integer", "description": "Horse ID (optional)"},
                    "horseName": {"type": "string", "description": "Horse name (optional)"}
                },
                "required": ["symptoms"]
            }
        },
        {
            "name": "evaluateTrainingResumptionReadiness",
            "description": "Assess safety and readiness for a horse to resume training (score 0-100%) after quarantine or injury.",
            "parameters": {
                "type": "object",
                "properties": {
                    "horseId": {"type": "integer", "description": "Horse ID (optional)"},
                    "horseName": {"type": "string", "description": "Horse name (optional)"}
                },
                "required": []
            }
        },
        {
            "name": "getEdaAnalytics",
            "description": "Perform exploratory data analysis on horse performance, biomedical, and survival data.",
            "parameters": {
                "type": "object",
                "properties": {
                    "analysisType": {
                        "type": "string",
                        "enum": ["HORSE_PERFORMANCE", "BIO_SPC", "SURVIVAL"],
                        "description": "Required analysis mode: HORSE_PERFORMANCE, BIO_SPC, or SURVIVAL"
                    },
                    "horseId": {"type": "integer", "description": "Horse ID (optional)"},
                    "horseName": {"type": "string", "description": "Horse name (optional)"},
                    "days": {"type": "integer", "description": "Lookback period in days (optional)"},
                    "metric": {"type": "string", "enum": ["TEMPERATURE", "RESTING_HEART_RATE"], "description": "BIO_SPC metric (optional)"},
                    "stratifyBy": {"type": "string", "enum": ["SEVERITY", "BODY_ZONE"], "description": "Survival grouping (optional)"}
                },
                "required": ["analysisType"]
            }
        }
    ],
    "GROOM": [
        {
            "name": "getMyAssignedHorses",
            "description": "Retrieve the stalls and horses that the current groom directly manages.",
            "parameters": {"type": "object", "properties": {}, "required": []}
        },
        {
            "name": "getTodayAggregatedTasks",
            "description": "Retrieve the groom's aggregated task list for the day from SOP tasks, training track schedules, and veterinary exam schedules.",
            "parameters": {
                "type": "object",
                "properties": {
                    "date": {"type": "string", "description": "Date to query (YYYY-MM-DD format, defaults to today)"}
                },
                "required": []
            }
        },
        {
            "name": "getHorseDietaryAndCareAlerts",
            "description": "Look up medical alerts, training restriction decisions, and special care notes for a horse.",
            "parameters": {
                "type": "object",
                "properties": {
                    "horseId": {"type": "integer", "description": "Horse ID (optional)"},
                    "horseName": {"type": "string", "description": "Horse name (optional)"}
                },
                "required": []
            }
        },
        {
            "name": "getHorseStallLocation",
            "description": "Look up a horse's current stall location, area, and assigned caretaker by name or code.",
            "parameters": {
                "type": "object",
                "properties": {
                    "horseId": {"type": "integer", "description": "Horse ID (optional)"},
                    "horseName": {"type": "string", "description": "Horse name (optional)"}
                },
                "required": []
            }
        },
        {
            "name": "getMyReportedIncidents",
            "description": "Look up stable incidents or horse injuries reported by current groom in last 7 days and their handling status.",
            "parameters": {"type": "object", "properties": {}, "required": []}
        },
        {
            "name": "getGroomTaskCompletionRate",
            "description": "Check groom's daily care task completion progress (stall cleaning, feeding, exercise handoff).",
            "parameters": {
                "type": "object",
                "properties": {
                    "date": {"type": "string", "description": "Date to check (YYYY-MM-DD, defaults to today)"}
                },
                "required": []
            }
        },
        {
            "name": "detectHorseBehavioralAnomaly",
            "description": "Detect early behavioral abnormalities (loss of appetite, pawing, lethargy) from logs and warn about acute disease risk (Colic).",
            "parameters": {
                "type": "object",
                "properties": {
                    "horseId": {"type": "integer", "description": "Horse ID (optional)"},
                    "horseName": {"type": "string", "description": "Horse name (optional)"}
                },
                "required": []
            }
        },
        {
            "name": "recommendDailyCarePriorityOrder",
            "description": "Recommend priority order of daily care tasks (horses needing medical exams -> early training sessions -> quarantine stall cleaning last).",
            "parameters": {"type": "object", "properties": {}, "required": []}
        },
        {
            "name": "getEdaAnalytics",
            "description": "Perform exploratory data analysis on allowed biomedical horse data.",
            "parameters": {
                "type": "object",
                "properties": {
                    "analysisType": {
                        "type": "string",
                        "enum": ["BIO_SPC"],
                        "description": "Required analysis mode: BIO_SPC"
                    },
                    "horseId": {"type": "integer", "description": "Horse ID (optional)"},
                    "horseName": {"type": "string", "description": "Horse name (optional)"},
                    "days": {"type": "integer", "description": "Lookback period in days (optional)"},
                    "metric": {"type": "string", "enum": ["TEMPERATURE", "RESTING_HEART_RATE"], "description": "BIO_SPC metric (optional)"}
                },
                "required": ["analysisType"]
            }
        }
    ],
    "CLUB_MANAGER": [
        {
            "name": "getStableOccupancyMetrics",
            "description": "View stable occupancy statistics (regular stalls, quarantine stalls).",
            "parameters": {"type": "object", "properties": {}, "required": []}
        },
        {
            "name": "getAdmissionPipelineOverview",
            "description": "View an overview of admission application counts and approval progress by stage.",
            "parameters": {
                "type": "object",
                "properties": {
                    "status": {"type": "string", "description": "Filter by approval stage: PENDING, APPROVED, REJECTED"}
                },
                "required": []
            }
        },
        {
            "name": "getTrainerAreaDistribution",
            "description": "Statistics on stable area distribution, responsible trainer, stall count, and assigned grooms.",
            "parameters": {"type": "object", "properties": {}, "required": []}
        },
        {
            "name": "getClubHealthOverview",
            "description": "Statistics on total horse count and club-wide health status (total horses, cleared vs restricted, weekly incidents). Always use when asked how many horses are in the club.",
            "parameters": {"type": "object", "properties": {}, "required": []}
        },
        {
            "name": "getSystemStaffOverview",
            "description": "Look up staff directory and staff count statistics in system (total staff 'totalStaff', breakdown 'roleBreakdown': Head Trainer, Veterinarian, Groom, Manager). Always use when asked how many staff or grooms exist.",
            "parameters": {
                "type": "object",
                "properties": {
                    "role": {"type": "string", "description": "Role to filter (HEAD_TRAINER, VETERINARIAN, GROOM, CLUB_MANAGER)"},
                    "isActive": {"type": "boolean", "description": "Active status filter (true/false)"},
                    "keyword": {"type": "string", "description": "Search keyword by name, email, or phone"}
                },
                "required": []
            }
        },
        {
            "name": "getClubHorsesDirectory",
            "description": "Look up the detailed horse roster in the club (horse name, UELN, stall, responsible trainer, specialty count, specialty list).",
            "parameters": {
                "type": "object",
                "properties": {
                    "status": {"type": "string", "description": "Horse status (ACTIVE, IN_TRAINING, RESTING, QUARANTINE)"},
                    "keyword": {"type": "string", "description": "Search keyword by horse name or owner name"}
                },
                "required": []
            }
        },
        {
            "name": "getHorseSpecialtyAnalysis",
            "description": "Analyze horse specialties. Leave blank or pass 'all' to return a RANKING of all horses by average specialty rating (sortBy='rating') or specialty count (sortBy='count').",
            "parameters": {
                "type": "object",
                "properties": {
                    "horseId": {"type": "integer", "description": "Horse ID (optional)"},
                    "horseName": {"type": "string", "description": "Horse name (optional)"},
                    "sortBy": {"type": "string", "description": "Ranking criterion: 'rating' or 'count'"}
                },
                "required": []
            }
        },
        {
            "name": "getRecentAuditLogs",
            "description": "Look up system audit trail for recent sensitive operations (total audit logs totalAuditLogs, detailed list auditLogList).",
            "parameters": {
                "type": "object",
                "properties": {
                    "limit": {"type": "integer", "description": "Number of audit log records (default 10)"}
                },
                "required": []
            }
        },
        {
            "name": "analyzeStableCapacityAndStallBottlenecks",
            "description": "Analyze stable capacity and forecast horse intake bottlenecks over the next 30-60 days.",
            "parameters": {"type": "object", "properties": {}, "required": []}
        },
        {
            "name": "recommendGroomWorkloadRebalancing",
            "description": "Recommend workload rebalancing and stall redistribution across grooms to prevent overload.",
            "parameters": {"type": "object", "properties": {}, "required": []}
        },
        {
            "name": "getEdaAnalytics",
            "description": "Perform exploratory data analysis on club fleet, horse performance, biomedical, survival, and management data.",
            "parameters": {
                "type": "object",
                "properties": {
                    "analysisType": {
                        "type": "string",
                        "enum": ["FLEET_PERFORMANCE", "HORSE_PERFORMANCE", "BIO_SPC", "SURVIVAL", "MANAGER_OVERVIEW", "ADMISSION_FUNNEL", "TRAINER_PERFORMANCE"],
                        "description": "Required analysis mode"
                    },
                    "horseId": {"type": "integer", "description": "Horse ID (optional)"},
                    "horseName": {"type": "string", "description": "Horse name (optional)"},
                    "days": {"type": "integer", "description": "Lookback period in days (optional)"},
                    "metric": {"type": "string", "enum": ["TEMPERATURE", "RESTING_HEART_RATE"], "description": "BIO_SPC metric (optional)"},
                    "stratifyBy": {"type": "string", "enum": ["SEVERITY", "BODY_ZONE"], "description": "Survival grouping (optional)"}
                },
                "required": ["analysisType"]
            }
        }
    ]
}

def get_tools_for_responses_api(role: str) -> List[Dict[str, Any]]:
    """Builds flat tool specifications for OpenAI /v1/responses."""
    tools = ROLE_TOOL_DEFINITIONS.get(role, [])
    res = []
    for t in tools:
        res.append({
            "type": "function",
            "name": t["name"],
            "description": t["description"],
            "parameters": t["parameters"]
        })
    return res

def get_tools_for_chat_completions(role: str) -> List[Dict[str, Any]]:
    """Builds nested tool specifications for OpenAI /v1/chat/completions."""
    tools = ROLE_TOOL_DEFINITIONS.get(role, [])
    res = []
    for t in tools:
        res.append({
            "type": "function",
            "function": {
                "name": t["name"],
                "description": t["description"],
                "parameters": t["parameters"]
            }
        })
    return res

# Consolidation aliases matching DynamicToolRegistryService.java
TOOL_CONSOLIDATION_ALIASES: Dict[str, str] = {
    "getHorseTrainingSummary": "getMyHorseTrainingProgress",
    "analyzeWorkoutLoadAndOvertrainingRisk": "getHorseFitnessTrend",
    "getCourseCurriculumOverview": "getTrainerPlansSummary",
    "getRacingReadinessAssessment": "getTrainerRiskAlerts",
    "recommendOptimalRaceDistanceAndTrack": "recommendTrainerStrategy",
    "recommendNextWorkoutAdjustment": "recommendTrainerStrategy",
    "getActivePrescriptions": "getHorseMedicalHistory",
    "getRecentIncidentReports": "getUrgentCareAndIncidentAlerts",
    "getUrgentCareAlerts": "getUrgentCareAndIncidentAlerts",
    "getDailyGroomTasks": "getTodayAggregatedTasks",
    "getAdmissionRequestsOverview": "getAdmissionPipelineOverview",
    "getAdmissionPipelineDetails": "getAdmissionPipelineOverview",
}

ALL_CANONICAL_TOOLS: Set[str] = {
    t["name"]
    for role_tools in ROLE_TOOL_DEFINITIONS.values()
    for t in role_tools
}

def resolve_canonical_tool_name(tool_name: str) -> str:
    """Resolves tool aliases to their canonical tool name."""
    if not tool_name:
        return ""
    return TOOL_CONSOLIDATION_ALIASES.get(tool_name, tool_name)

def is_tool_allowed_for_role(tool_name: str, role: str) -> bool:
    """Verifies whether a canonical tool or alias is accessible for the specified role."""
    if not role or role not in ROLE_TOOL_DEFINITIONS:
        return False
    canonical_name = resolve_canonical_tool_name(tool_name)
    role_canonical = {t["name"] for t in ROLE_TOOL_DEFINITIONS[role]}
    return canonical_name in role_canonical

# ==============================================================================
# 3. STATEFUL IN-MEMORY MOCK WORLD (MockWorldState)
# ==============================================================================

class MockWorldState:
    """
    Maintains relational mock entities and returns dynamic, parameter-aware
    JSON observation payloads matching RTMS backend schemas.
    """

    def __init__(self):
        # 1. Horses
        self.horses: Dict[str, Dict[str, Any]] = {
            "Thunder": {
                "id": 1,
                "name": "Thunder",
                "ueln": "USA2022010101",
                "status": "ACTIVE",
                "trainingCleared": True,
                "stall": "A-101",
                "area": "Barn A",
                "trainer": "Alex Morgan",
                "groom": "John Doe",
                "rating": 92.5,
                "specialties": ["Sprint 1200m Dirt", "Miler 1600m Turf"],
                "sire": "Storm Cat",
                "dam": "Northern Dancer Line",
                "foalDate": "2022-04-12",
                "breed": "Thoroughbred",
                "diagnosis": "Healthy - Mild Post-Workout Fatigue",
                "prescription": "Electrolyte supplements daily",
                "acwr": 1.45,
                "readinessScore": 94,
                "completedWorkouts": 14,
                "completionRate": "78%",
                "courseName": "Sprint Mastery Level 2",
                "investmentROI": "+24.8% Projected Racing Value",
                "behaviorAnomaly": "Mild anorexia noted in morning feed, pawing ground, early restlessness (Moderate Colic Risk)"
            },
            "Lightning": {
                "id": 2,
                "name": "Lightning",
                "ueln": "USA2022010102",
                "status": "ACTIVE",
                "trainingCleared": True,
                "stall": "A-102",
                "area": "Barn A",
                "trainer": "Alex Morgan",
                "groom": "John Doe",
                "rating": 88.0,
                "specialties": ["Stamina 2400m Turf"],
                "sire": "Sea the Stars",
                "dam": "Galileo Line",
                "foalDate": "2022-05-18",
                "breed": "Thoroughbred",
                "diagnosis": "Minor left carpus abrasion - Clean healing",
                "prescription": "Topical antiseptic cream & daily pressure bandage check",
                "acwr": 1.05,
                "readinessScore": 89,
                "completedWorkouts": 12,
                "completionRate": "85%",
                "courseName": "Distance Endurance Elite",
                "investmentROI": "+18.2%",
                "behaviorAnomaly": "None - Active and normal appetite"
            },
            "Storm Runner": {
                "id": 3,
                "name": "Storm Runner",
                "ueln": "USA2021020303",
                "status": "MEDICAL_REST",
                "trainingCleared": False,
                "stall": "Q-01",
                "area": "Quarantine Barn",
                "trainer": "Alex Morgan",
                "groom": "Sarah Connor",
                "rating": 85.0,
                "specialties": ["Sprint 1000m Dirt"],
                "sire": "Giant's Causeway",
                "dam": "Sunday Silence Line",
                "foalDate": "2021-03-25",
                "breed": "Thoroughbred",
                "diagnosis": "Deep fetlock laceration with secondary purulent infection, elevated body temperature (39.1C)",
                "prescription": "Phenylbutazone 2g oral SID (Active NSAID Therapy)",
                "acwr": 0.0,
                "readinessScore": 68,  # Below 85 threshold!
                "quarantineBlocked": True,
                "completedWorkouts": 8,
                "completionRate": "50%",
                "courseName": "Post-Injury Rehabilitation Phase 1",
                "investmentROI": "-5.4% (Delayed by Medical Rest)",
                "behaviorAnomaly": "Lethargic, guarded gait on left hind leg"
            },
            "Blaze": {
                "id": 4,
                "name": "Blaze",
                "ueln": "USA2022030404",
                "status": "ACTIVE",
                "trainingCleared": True,
                "stall": "A-103",
                "area": "Barn A",
                "trainer": "Alex Morgan",
                "groom": "John Doe",
                "rating": 95.0,  # Top sprint performer!
                "specialties": ["Sprint 1200m Dirt", "Sprint 1400m Turf", "Sprint 1000m Turf"],
                "sire": "Tapit",
                "dam": "Medaglia d'Oro Line",
                "foalDate": "2022-02-14",
                "breed": "Thoroughbred",
                "diagnosis": "Peak Physical Condition",
                "prescription": "None",
                "acwr": 1.15,
                "readinessScore": 98,
                "completedWorkouts": 18,
                "completionRate": "95%",
                "courseName": "Sprint Mastery Level 2",
                "investmentROI": "+38.5%",
                "behaviorAnomaly": "None"
            },
            "Shadow": {
                "id": 5,
                "name": "Shadow",
                "ueln": "USA2022040505",
                "status": "ACTIVE",
                "trainingCleared": True,
                "stall": "A-104",
                "area": "Barn A",
                "trainer": "Alex Morgan",
                "groom": "John Doe",
                "rating": 89.0,
                "specialties": ["Miler 1600m Turf"],
                "sire": "Curlin",
                "dam": "Smart Strike Line",
                "foalDate": "2022-03-30",
                "breed": "Thoroughbred",
                "diagnosis": "Healthy",
                "prescription": "None",
                "acwr": 1.08,
                "readinessScore": 91,
                "completedWorkouts": 15,
                "completionRate": "88%",
                "courseName": "Sprint Mastery Level 2",
                "investmentROI": "+19.0%",
                "behaviorAnomaly": "None"
            },
            "Comet": {
                "id": 6,
                "name": "Comet",
                "ueln": "USA2022050606",
                "status": "ACTIVE",
                "trainingCleared": True,
                "stall": "A-105",
                "area": "Barn A",
                "trainer": "Alex Morgan",
                "groom": "John Doe",
                "rating": 87.5,
                "specialties": ["Middle Distance 2000m Turf"],
                "sire": "American Pharoah",
                "dam": "Pioneerof the Nile Line",
                "foalDate": "2022-05-02",
                "breed": "Thoroughbred",
                "diagnosis": "Healthy",
                "prescription": "None",
                "acwr": 1.02,
                "readinessScore": 90,
                "completedWorkouts": 13,
                "completionRate": "82%",
                "courseName": "Sprint Mastery Level 2",
                "investmentROI": "+15.2%",
                "behaviorAnomaly": "None"
            },
            "Silver Mist": {
                "id": 105,
                "name": "Silver Mist",
                "ueln": "USA2023060707",
                "status": "CANDIDATE",
                "trainingCleared": False,
                "stall": "Q-03 (Reserved)",
                "area": "Quarantine Barn",
                "trainer": "Unassigned (Alex Morgan Nominated)",
                "groom": "Unassigned",
                "rating": 84.0,
                "specialties": ["Sprint 1200m Turf"],
                "sire": "Frankel",
                "dam": "Oasis Dream Line",
                "foalDate": "2023-01-20",
                "breed": "Thoroughbred",
                "diagnosis": "Awaiting initial intake clinical exam",
                "prescription": "None",
                "acwr": 0.0,
                "readinessScore": 0,
                "completedWorkouts": 0,
                "completionRate": "0%",
                "courseName": "None",
                "investmentROI": "N/A",
                "behaviorAnomaly": "None"
            }
        }

        # 2. Training Groups
        self.groups: Dict[str, Dict[str, Any]] = {
            "Sprint Group Alpha": {
                "groupName": "Sprint Group Alpha",
                "trainer": "Alex Morgan",
                "currentHorseCount": 5,
                "maxCapacity": 6,  # 6-horse limit per Rule 11/12
                "horses": ["Thunder", "Blaze", "Lightning", "Shadow", "Comet"],
                "courseRun": "Sprint Mastery Level 2",
                "status": "ACTIVE",
                "dailySchedule": "06:30 - 08:00 Main Track"
            }
        }

        # 3. Stables & Stalls
        self.stables: Dict[str, Any] = {
            "Barn A": {"capacity": 20, "occupied": 18, "trainer": "Alex Morgan", "grooms": 8},
            "Barn B": {"capacity": 25, "occupied": 24, "trainer": "David Wu", "grooms": 5},
            "Barn C": {"capacity": 15, "occupied": 5, "trainer": "Rachel Green", "grooms": 2},
            "Quarantine": {"capacity": 4, "occupied": 2, "stalls": ["Q-01", "Q-02", "Q-03", "Q-04"]}
        }

        # 4. Staff Overview
        self.staff: Dict[str, Any] = {
            "totalStaff": 24,
            "roleBreakdown": {
                "Head Trainer": 3,
                "Veterinarian": 2,
                "Groom": 15,
                "Administrative": 4
            },
            "grooms": [
                {"name": "John Doe", "phone": "+1-555-0192", "email": "johndoe@rtms.club", "barn": "Barn A", "assignedStalls": ["A-101", "A-102", "A-103", "A-104", "A-105"], "horses": ["Thunder", "Lightning", "Blaze", "Shadow", "Comet"]},
                {"name": "Sarah Connor", "phone": "+1-555-0144", "email": "sconnor@rtms.club", "barn": "Quarantine", "assignedStalls": ["Q-01", "Q-02"], "horses": ["Storm Runner"]}
            ]
        }

        # 5. Admission Pipeline Funnel
        self.admissions: Dict[str, Any] = {
            "totalApplications": 18,
            "inProgressApplications": 5,
            "approvedApplications": 12,
            "rejectedApplications": 1,
            "funnel": {
                "SUBMITTED": 5,
                "GROOM_REVIEW": 4,
                "VET_REVIEW": 3,
                "TRAINER_REVIEW": 2,
                "MANAGER_REVIEW": 2,
                "APPROVED": 12
            },
            "pendingApplications": [
                {
                    "applicationId": 105,
                    "candidateHorseName": "Silver Mist",
                    "ownerName": "John Smith",
                    "currentStage": "TRAINER_REVIEW",
                    "assignedTrainer": "Alex Morgan",
                    "status": "PENDING",
                    "submittedDate": "2026-11-12",
                    "quarantineStallReserved": "Q-03",
                    "actionUrl": "/manager/admissions/105"
                }
            ]
        }

        # 6. Clinical RAG Vector Knowledge Base
        self.rag_clinical: Dict[str, str] = {
            "fetlock laceration infection fever": (
                "CLINICAL PROTOCOL CP-402 (Infected Fetlock Laceration):\n"
                "1. Immediate sterile wound debridement & isotonic saline lavage (0.9% NaCl).\n"
                "2. Systemic broad-spectrum antibiotic therapy (Procaine Penicillin G + Gentamicin IV/IM).\n"
                "3. CONTRAINDICATION WARNING (Rule 8): If horse is currently receiving Phenylbutazone, DO NOT co-administer "
                "Flunixin Meglumine or additional non-steroidal anti-inflammatory drugs (NSAIDs). Concurrent NSAID administration "
                "induces catastrophic gastrointestinal ulceration and acute renal papillary necrosis."
            ),
            "colic": (
                "CLINICAL PROTOCOL CP-108 (Equine Acute Abdomen / Colic):\n"
                "Early clinical indicators: anorexia, ground pawing, recumbency, decreased borborygmi. "
                "Immediate actions: withhold all grain/concentrates, check heart rate & capillary refill time, "
                "perform gentle hand walking, and notify attending veterinarian."
            )
        }

        # 7. Audit Logs
        self.audit_logs: List[Dict[str, Any]] = [
            {"id": 1001, "timestamp": "2026-11-18 02:15:33", "actor": "192.168.1.99", "event": "FAILED_LOGIN_ATTEMPT", "details": "Repeated unauthorized authentication attempts targeting administrative dashboard", "severity": "HIGH"},
            {"id": 1002, "timestamp": "2026-11-17 19:40:12", "actor": "trainer2@rtms.club", "event": "WORKOUT_LOG_UPDATED", "details": "Workout result logged for horse Thunder", "severity": "LOW"},
            {"id": 1003, "timestamp": "2026-11-17 14:22:05", "actor": "unknown_ip", "event": "UNAUTHORIZED_PRIVILEGE_ESCALATION", "details": "Attempted role override to CLUB_MANAGER rejected by security gateway", "severity": "CRITICAL"},
            {"id": 1004, "timestamp": "2026-11-17 11:10:00", "actor": "manager@rtms.club", "event": "STALL_REALLOCATION", "details": "Stall Q-03 reserved for admission application #105", "severity": "MEDIUM"}
        ]

    def _resolve_horse_name(self, args: Dict[str, Any]) -> str:
        """Helper to extract horse name from either horseName or horseId."""
        name = args.get("horseName")
        if name:
            return name.strip()
        hid = args.get("horseId")
        if hid is not None:
            for hname, hdata in self.horses.items():
                if hdata["id"] == hid:
                    return hname
        return ""

    def dispatch(self, tool_name: str, arguments: Dict[str, Any], role: str) -> Dict[str, Any]:
        """Dispatches tool call to dynamic in-memory mock handler."""
        if role and not is_tool_allowed_for_role(tool_name, role):
            return {
                "status": "ERROR",
                "error": f"Permission denied: Tool '{tool_name}' is not accessible for role '{role}'.",
                "aiGuidance": f"The requested tool '{tool_name}' is not permitted for your current role ({role}). Only call tools defined in your role's tool catalog."
            }

        tool_name = resolve_canonical_tool_name(tool_name)
        horse_name = self._resolve_horse_name(arguments)
        horse_data = self.horses.get(horse_name, {})

        # ----------------- HORSE_OWNER Tools -----------------
        if tool_name == "getMyHorses":
            return {
                "totalOwnedHorses": 3,
                "allowedTrainingHorses": 2,
                "restrictedHorses": 1,
                "horseList": [
                    {"id": 1, "name": "Thunder", "status": "ACTIVE", "trainingCleared": True, "stall": "A-101", "actionUrl": "/owner/horses/1"},
                    {"id": 2, "name": "Lightning", "status": "ACTIVE", "trainingCleared": True, "stall": "A-102", "actionUrl": "/owner/horses/2"},
                    {"id": 3, "name": "Storm Runner", "status": "MEDICAL_REST", "trainingCleared": False, "stall": "Q-01", "medicalAlert": "Quarantine / Fetlock laceration", "actionUrl": "/owner/horses/3"}
                ]
            }

        elif tool_name == "getMyHorseHealthSummary":
            target = horse_name if horse_name else "Thunder"
            h = self.horses.get(target, self.horses["Storm Runner"])
            return {
                "horseId": h["id"],
                "horseName": h["name"],
                "overallStatus": h["status"],
                "trainingCleared": h["trainingCleared"],
                "diagnosis": h["diagnosis"],
                "activePrescription": h["prescription"],
                "readinessScore": h["readinessScore"],
                "actionUrl": f"/owner/horses/{h['id']}"
            }

        elif tool_name == "getMyHorseTrainingProgress":
            target = horse_name if horse_name else "Thunder"
            h = self.horses.get(target, self.horses["Thunder"])
            return {
                "horseId": h["id"],
                "horseName": h["name"],
                "courseName": h["courseName"],
                "completionRate": h["completionRate"],
                "completedWorkouts": h["completedWorkouts"],
                "trainingDecision": "MONITOR_CARDIAC_RECOVERY_BIO_SPC" if h["name"] == "Thunder" else "CONTINUE_REGULAR_TRAINING",
                "actionUrl": f"/owner/horses/{h['id']}"
            }

        elif tool_name == "getMyHorseUpcomingSchedule":
            return {
                "fromDate": arguments.get("fromDate", "2026-11-18"),
                "toDate": arguments.get("toDate", "2026-11-25"),
                "sessions": [
                    {"date": "2026-11-19", "time": "06:30", "horse": "Thunder", "exercise": "Canter 1200m", "trainer": "Alex Morgan", "status": "SCHEDULED"},
                    {"date": "2026-11-21", "time": "07:00", "horse": "Lightning", "exercise": "Endurance Trot 3000m", "trainer": "Alex Morgan", "status": "SCHEDULED"}
                ]
            }

        elif tool_name == "getMyHorseAdmissions":
            return {
                "applications": self.admissions["pendingApplications"]
            }

        elif tool_name == "getHorseCareSchedule":
            target = horse_name if horse_name else "Storm Runner"
            return {
                "horseName": target,
                "schedules": [
                    {"date": "2026-11-19", "type": "VET_EXAM", "description": "Fetlock wound re-dressing and bacterial culture swab", "vet": "Dr. Sarah Jenkins", "status": "SCHEDULED"},
                    {"date": "2026-11-24", "type": "VACCINATION", "description": "Equine Influenza Booster", "vet": "Dr. Sarah Jenkins", "status": "UPCOMING"}
                ]
            }

        elif tool_name == "getMyHorseIncidentHistory":
            target = horse_name if horse_name else "Storm Runner"
            return {
                "horseName": target,
                "totalIncidents": 1,
                "incidents": [
                    {"date": "2026-11-16", "severity": "HIGH", "description": "Left hind fetlock laceration with fever during paddock turnout; placed under quarantine."}
                ]
            }

        elif tool_name == "getMyHorsePedigreeInfo":
            target = horse_name if horse_name else "Thunder"
            h = self.horses.get(target, self.horses["Thunder"])
            return {
                "horseName": h["name"],
                "sire": h["sire"],
                "dam": h["dam"],
                "breed": h["breed"],
                "foalDate": h["foalDate"],
                "bloodlineRating": "Grade 1 Stakes Sire Lineage"
            }

        elif tool_name == "analyzeHorseInvestmentAndFitnessROI":
            target = horse_name if horse_name else "Thunder"
            h = self.horses.get(target, self.horses["Thunder"])
            return {
                "horseName": h["name"],
                "projectedValueGrowth": h["investmentROI"],
                "careerStage": "Ascending Peak Form",
                "fitnessIndex": "91.2/100",
                "commercialAppraisal": "High Commercial Sire Prospect"
            }

        # ----------------- HEAD_TRAINER Tools -----------------
        elif tool_name == "getTrainerHorses":
            return {
                "trainerName": "Alex Morgan",
                "managedHorsesCount": 5,
                "horses": [
                    {"name": h["name"], "stall": h["stall"], "groom": h["groom"], "rating": h["rating"], "status": h["status"]}
                    for h in self.horses.values() if h["trainer"] == "Alex Morgan"
                ]
            }

        elif tool_name == "getTrainerGroups":
            return {
                "trainerName": "Alex Morgan",
                "groups": [
                    {
                        "groupName": "Sprint Group Alpha",
                        "memberCount": 5,
                        "maxCapacity": 6,
                        "availableSlots": 1,
                        "members": ["Thunder", "Blaze", "Lightning", "Shadow", "Comet"],
                        "courseRun": "Sprint Mastery Level 2",
                        "status": "ACTIVE"
                    }
                ]
            }

        elif tool_name == "getWorkoutHistory":
            target = horse_name if horse_name else "Thunder"
            return {
                "horseName": target,
                "recentWorkouts": [
                    {"date": "2026-11-17", "exercise": "Interval Gallop 1000m", "peakHeartRate": 218, "recoveryMinutes": 18.5, "outcome": "Ran (High Cardiac Strain Detected)"},
                    {"date": "2026-11-14", "exercise": "Breeze 800m", "peakHeartRate": 204, "recoveryMinutes": 14.0, "outcome": "Ran"},
                    {"date": "2026-11-11", "exercise": "Canter 1200m", "peakHeartRate": 195, "recoveryMinutes": 11.2, "outcome": "Ran"}
                ]
            }

        elif tool_name == "getHorseFitnessTrend":
            target = horse_name if horse_name else "Thunder"
            h = self.horses.get(target, self.horses["Thunder"])
            return {
                "horseName": h["name"],
                "acwr": h["acwr"],
                "acuteChronicWorkloadStatus": "HIGH_FATIGUE_OVERLOAD (ACWR > 1.30)",
                "recommendedAction": "Deloading phase: replace intense interval gallops with active recovery trot for 5 days."
            }

        elif tool_name == "getTrainerSchedule":
            needs_result = arguments.get("needsResultOnly", False)
            if needs_result:
                return {
                    "pendingAttendanceCount": 1,
                    "sessionsMissingResults": [
                        {
                            "sessionId": 402,
                            "date": "2026-11-15",
                            "exercise": "Gate Break & Sprint 600m",
                            "unrecordedHorses": ["Comet"],
                            "assignedGroom": "John Doe",
                            "actionUrl": "/trainer/schedule"
                        }
                    ]
                }
            return {
                "date": arguments.get("date", "2026-11-18"),
                "scheduledSessions": [
                    {"sessionId": 410, "time": "06:30", "group": "Sprint Group Alpha", "exercise": "Stamina Canter 1400m"}
                ]
            }

        elif tool_name == "getTrainerPlansSummary":
            return {
                "trainer": "Alex Morgan",
                "plans": [
                    {"courseName": "Sprint Mastery Level 2", "goal": "Graded Sprint Readiness", "activeEnrolledHorses": 5, "attendanceRate": "92%"}
                ]
            }

        elif tool_name == "getHorseSpecialtyAnalysis":
            sort_by = arguments.get("sortBy", "rating")
            ranked = [
                {"rank": 1, "horseName": "Blaze", "averageRating": 95.0, "bestDistance": "Sprint 1200m Dirt", "specialtyCount": 3},
                {"rank": 2, "horseName": "Thunder", "averageRating": 92.5, "bestDistance": "Sprint 1200m Dirt", "specialtyCount": 2},
                {"rank": 3, "horseName": "Shadow", "averageRating": 89.0, "bestDistance": "Miler 1600m Turf", "specialtyCount": 1},
                {"rank": 4, "horseName": "Lightning", "averageRating": 88.0, "bestDistance": "Stamina 2400m Turf", "specialtyCount": 1},
                {"rank": 5, "horseName": "Comet", "averageRating": 87.5, "bestDistance": "Middle Distance 2000m Turf", "specialtyCount": 1}
            ]
            if sort_by == "count":
                ranked.sort(key=lambda x: x["specialtyCount"], reverse=True)
            return {
                "rankingCriteria": sort_by,
                "rankedHorseList": ranked
            }

        elif tool_name == "getTrainerRiskAlerts":
            target = horse_name if horse_name else "Thunder"
            return {
                "horseName": target,
                "fatigueRisk": "ELEVATED",
                "cardiacStrain": "ACUTE_OVERLOAD",
                "alertSummary": f"Horse {target} has Acute-to-Chronic Workload Ratio (ACWR) of 1.45. High risk of soft-tissue or tendon breakdown if workload is not deloaded.",
                "actionUrl": "/trainer/dashboard"
            }

        elif tool_name == "getTrainerAssignedGrooms":
            return {
                "trainer": "Alex Morgan",
                "assignedGrooms": self.staff["grooms"]
            }

        elif tool_name == "getAssignedAdmissionReviews":
            return {
                "trainer": "Alex Morgan",
                "pendingReviews": [
                    {
                        "applicationId": 105,
                        "candidateHorse": "Silver Mist",
                        "targetGroupProposed": "Sprint Group Alpha",
                        "status": "AWAITING_TRAINER_CAPACITY_CONFIRMATION",
                        "actionUrl": "/trainer/admissions/105"
                    }
                ]
            }

        elif tool_name == "recommendTrainerStrategy":
            target = horse_name if horse_name else "Thunder"
            if target == "Blaze":
                return {
                    "horseName": "Blaze",
                    "optimalRaceDistance": "Sprint 1200m",
                    "optimalSurface": "Fast Dirt",
                    "tacticalStyle": "Front-Runner / Pacesetter",
                    "workoutAdjustment": "Maintain anaerobic power with 400m sharp sprints; maintain taper 7 days prior to stakes event."
                }
            return {
                "horseName": target,
                "optimalRaceDistance": "Sprint 1200m",
                "optimalSurface": "Dirt / Good Turf",
                "tacticalStyle": "Stalker / Mid-pack",
                "workoutAdjustment": "Immediate 30% reduction in gallop volume for next 5 days. Replace with light aerobic trotting to reduce ACWR to safe range (< 1.20)."
            }

        # ----------------- VETERINARIAN Tools -----------------
        elif tool_name == "searchClinicalKnowledge":
            query = arguments.get("query", "").lower()
            if "fetlock" in query or "laceration" in query or "infection" in query or "fever" in query:
                return {
                    "query": arguments.get("query"),
                    "matchedProtocols": [self.rag_clinical["fetlock laceration infection fever"]]
                }
            return {
                "query": arguments.get("query"),
                "matchedProtocols": [self.rag_clinical["colic"]]
            }

        elif tool_name == "getVetExaminationOverview":
            return {
                "activeExaminations": [
                    {"horseName": "Storm Runner", "status": "IN_PROGRESS", "priority": "URGENT", "condition": "Infected Fetlock Laceration"},
                    {"horseName": "Silver Mist", "status": "SCHEDULED", "priority": "ROUTINE", "condition": "Intake Screening Exam"}
                ]
            }

        elif tool_name == "getHorseMedicalHistory":
            target = horse_name if horse_name else "Storm Runner"
            h = self.horses.get(target, self.horses["Storm Runner"])
            return {
                "horseName": h["name"],
                "primaryDiagnosis": h["diagnosis"],
                "activePrescriptions": [
                    {"medication": "Phenylbutazone", "dosage": "2g oral SID", "therapeuticClass": "NSAID", "startDate": "2026-11-16", "warning": "Strictly do NOT combine with Flunixin Meglumine (Banamine) or other NSAIDs due to severe ulceration and renal toxicity risk."}
                ],
                "actionUrl": f"/veterinarian/health?horseId={h['id']}"
            }

        elif tool_name == "getPreventiveCareSchedule":
            target = horse_name if horse_name else "Thunder"
            return {
                "horseName": target,
                "careSchedule": [
                    {"type": "Vaccination", "due": "2026-11-28", "status": "PENDING"},
                    {"type": "Deworming", "due": "2026-12-15", "status": "UPCOMING"}
                ]
            }

        elif tool_name == "getQuarantineStatus":
            return {
                "quarantineStallsTotal": 4,
                "quarantineOccupied": 2,
                "quarantineHorses": [
                    {"horseName": "Storm Runner", "stall": "Q-01", "isolationReason": "Infected Fetlock Wound & Fever", "trainingStatus": "BLOCKED (Quarantine Gate Active)", "actionUrl": "/veterinarian/health?horseId=3"},
                    {"horseName": "Silver Mist", "stall": "Q-03 (Reserved)", "isolationReason": "Intake Quarantine Pre-clearance", "trainingStatus": "BLOCKED"}
                ]
            }

        elif tool_name == "getPendingAdmissionExams":
            return {
                "pendingExams": [
                    {"applicationId": 105, "horseName": "Silver Mist", "status": "PENDING_CLINICAL_CHECK"}
                ]
            }

        elif tool_name == "getUrgentCareAndIncidentAlerts":
            return {
                "activeAlertsCount": 1,
                "alerts": [
                    {
                        "alertId": "INC-2026-88",
                        "horseName": "Storm Runner",
                        "location": "Barn A / Paddock 3",
                        "severity": "HIGH",
                        "incidentType": "Fetlock Laceration with Swelling & Acute Fever",
                        "reportedBy": "Sarah Connor (Groom)",
                        "timestamp": "2026-11-17 18:30",
                        "actionUrl": "/veterinarian/urgent/INC-2026-88"
                    }
                ]
            }

        elif tool_name == "getHorseVitalSignsTrend":
            target = horse_name if horse_name else "Storm Runner"
            if target == "Storm Runner":
                return {
                    "horseName": "Storm Runner",
                    "recentVitalSigns": [
                        {"timestamp": "2026-11-18 06:00", "temperature": 39.1, "restingHeartRate": 58, "respiratoryRate": 26, "clinicalStatus": "FEVER_AND_TACHYCARDIA"},
                        {"timestamp": "2026-11-17 18:00", "temperature": 38.9, "restingHeartRate": 54, "respiratoryRate": 22, "clinicalStatus": "ELEVATED"}
                    ]
                }
            return {
                "horseName": target,
                "recentVitalSigns": [
                    {"timestamp": "2026-11-18 06:00", "temperature": 37.9, "restingHeartRate": 38, "respiratoryRate": 14, "clinicalStatus": "NORMAL"}
                ]
            }

        elif tool_name == "analyzeIncidentPatternsAndRecurringRisks":
            return {
                "riskTrend": "RECURRING_DISTAL_LIMB_INJURIES",
                "historicalIncidentsLast6Months": 14,
                "primaryCluster": "Fetlock / Suspensory Ligament Strain (64% of incidents)",
                "survivalAnalysisRecommendation": "Stratify recovery timelines by injury severity via Kaplan-Meier estimator."
            }

        elif tool_name == "recommendClinicalInterventionPlan":
            symptoms = arguments.get("symptoms", "")
            target = horse_name if horse_name else "Storm Runner"
            return {
                "horseName": target,
                "symptomsAnalyzed": symptoms,
                "step1ImmediateIntervention": "Wound debridement, topical hydrogel, and systemic Procaine Penicillin G + Gentamicin.",
                "step2Diagnostics": "Digital radiography and ultrasound of left hind fetlock joint to rule out sesamoid fracture or septic tenosynovitis.",
                "drugInteractionWarning": "CRITICAL: Patient is currently on Phenylbutazone. Co-administration of Flunixin Meglumine (Banamine) or other NSAIDs is strictly contraindicated due to nephrotoxic and ulcerogenic synergism."
            }

        elif tool_name == "evaluateTrainingResumptionReadiness":
            target = horse_name if horse_name else "Storm Runner"
            h = self.horses.get(target, self.horses["Storm Runner"])
            return {
                "horseName": h["name"],
                "readinessScore": h["readinessScore"],
                "minimumPassingThreshold": 85,
                "clearanceDecision": "REJECTED - NOT CLEARED FOR WORKOUT RESUMPTION",
                "clinicalJustification": f"Readiness score of {h['readinessScore']}/100 is below safety threshold (85). Active infection, fever, and incomplete wound granulation require continuing medical rest and quarantine confinement."
            }

        # ----------------- GROOM Tools -----------------
        elif tool_name == "getMyAssignedHorses":
            return {
                "groomName": "John Doe",
                "assignedStallsCount": 5,
                "horses": [
                    {"name": "Thunder", "stall": "A-101", "feedStatus": "PARTIAL_REFUSAL", "actionUrl": "/groom/stable"},
                    {"name": "Lightning", "stall": "A-102", "feedStatus": "NORMAL", "actionUrl": "/groom/stable"},
                    {"name": "Blaze", "stall": "A-103", "feedStatus": "NORMAL", "actionUrl": "/groom/stable"},
                    {"name": "Shadow", "stall": "A-104", "feedStatus": "NORMAL", "actionUrl": "/groom/stable"},
                    {"name": "Comet", "stall": "A-105", "feedStatus": "NORMAL", "actionUrl": "/groom/stable"}
                ]
            }

        elif tool_name == "getTodayAggregatedTasks":
            return {
                "date": arguments.get("date", "2026-11-18"),
                "totalTasks": 8,
                "tasks": [
                    {"time": "06:00", "stall": "A-101", "horse": "Thunder", "task": "Morning Feeding & Water Check (Note: left 40% grain)", "category": "ROUTINE_SOP"},
                    {"time": "06:30", "stall": "A-102", "horse": "Lightning", "task": "Check & Replace Left Carpus Protective Bandage", "category": "VET_CARE"},
                    {"time": "07:00", "stall": "A-103", "horse": "Blaze", "task": "Hand-off to Track Exercise Rider", "category": "TRAINING"},
                    {"time": "08:30", "stall": "A-101", "horse": "Thunder", "task": "Assist Dr. Jenkins with Urgent Abdominal Exam", "category": "VET_CARE"}
                ]
            }

        elif tool_name == "getHorseDietaryAndCareAlerts":
            target = horse_name if horse_name else "Thunder"
            if target == "Thunder":
                return {
                    "horseName": "Thunder",
                    "dietaryAlert": "Withhold sweet grain; provide soaked timothy hay only until colic triage complete.",
                    "careAlert": "Check vital signs every 2 hours; monitor gut sounds."
                }
            elif target == "Lightning":
                return {
                    "horseName": "Lightning",
                    "dietaryAlert": "Standard high-performance ration.",
                    "careAlert": "Inspect and re-wrap left carpus pressure bandage daily; keep bedding clean and dry."
                }
            return {"horseName": target, "alerts": "No special alerts logged."}

        elif tool_name == "getHorseStallLocation":
            target = horse_name if horse_name else "Lightning"
            h = self.horses.get(target, self.horses["Lightning"])
            return {
                "horseName": h["name"],
                "stallLocation": h["stall"],
                "areaName": h["area"],
                "assignedGroom": h["groom"],
                "actionUrl": "/groom/stable"
            }

        elif tool_name == "getMyReportedIncidents":
            return {
                "reportedCount": 1,
                "incidents": [
                    {
                        "incidentId": "INC-GROOM-041",
                        "date": "2026-11-16",
                        "horseName": "Thunder",
                        "summary": "Horse refused 50% morning concentrate ration and showed mild pawing behavior.",
                        "status": "VET_EVALUATION_REQUESTED"
                    }
                ]
            }

        elif tool_name == "getGroomTaskCompletionRate":
            return {
                "date": arguments.get("date", "2026-11-18"),
                "assignedTasks": 8,
                "completedTasks": 5,
                "completionPercentage": "62.5%",
                "status": "IN_PROGRESS"
            }

        elif tool_name == "detectHorseBehavioralAnomaly":
            target = horse_name if horse_name else "Thunder"
            h = self.horses.get(target, self.horses["Thunder"])
            return {
                "horseName": h["name"],
                "anomalyDetected": True if h["name"] == "Thunder" else False,
                "pattern": h["behaviorAnomaly"],
                "clinicalRisk": "MODERATE_COLIC_RISK" if h["name"] == "Thunder" else "LOW",
                "recommendedAction": "Prioritize vital signs check, alert veterinarian immediately, and withhold concentrated feeds."
            }

        elif tool_name == "recommendDailyCarePriorityOrder":
            return {
                "groom": "John Doe",
                "priorityOrder": [
                    {"step": 1, "horse": "Thunder", "action": "Urgent Colic Triage: monitor vital signs & assist attending vet", "urgency": "IMMEDIATE"},
                    {"step": 2, "horse": "Lightning", "action": "Wound Care: replace protective carpus bandage before track turnout", "urgency": "HIGH"},
                    {"step": 3, "horse": "Blaze", "action": "Track Hand-off for Sprint workout", "urgency": "NORMAL"},
                    {"step": 4, "horses": ["Shadow", "Comet"], "action": "Routine stall mucking and water bucket sanitization", "urgency": "ROUTINE"}
                ]
            }

        # ----------------- CLUB_MANAGER Tools -----------------
        elif tool_name == "getStableOccupancyMetrics":
            return {
                "totalStalls": 64,
                "occupiedStalls": 49,
                "occupancyRate": "76.6%",
                "regularStallsTotal": 60,
                "regularStallsOccupied": 47,
                "quarantineStallsTotal": 4,
                "quarantineStallsOccupied": 2,
                "quarantineAvailable": 2,
                "actionUrl": "/manager/stable"
            }

        elif tool_name == "getAdmissionPipelineOverview":
            return self.admissions

        elif tool_name == "getTrainerAreaDistribution":
            return {
                "distribution": [
                    {"trainer": "Alex Morgan", "barn": "Barn A", "stallsAssigned": 20, "occupied": 18, "groomsAssigned": 8},
                    {"trainer": "David Wu", "barn": "Barn B", "stallsAssigned": 25, "occupied": 24, "groomsAssigned": 5},
                    {"trainer": "Rachel Green", "barn": "Barn C", "stallsAssigned": 15, "occupied": 5, "groomsAssigned": 2}
                ]
            }

        elif tool_name == "getClubHealthOverview":
            return {
                "totalClubHorses": 68,
                "fitForTraining": 62,
                "medicallyRestricted": 4,
                "underQuarantine": 2,
                "clubHealthIndex": "94.8%",
                "weeklyIncidents": 2
            }

        elif tool_name == "getSystemStaffOverview":
            role_filter = arguments.get("role")
            if role_filter == "GROOM":
                return {
                    "totalStaffInRole": 15,
                    "role": "GROOM",
                    "breakdownByBarn": {"Barn A": 8, "Barn B": 5, "Barn C": 2},
                    "actionUrl": "/manager/staff"
                }
            return self.staff

        elif tool_name == "getClubHorsesDirectory":
            return {
                "totalHorses": len(self.horses),
                "horses": [
                    {"name": h["name"], "ueln": h["ueln"], "stall": h["stall"], "trainer": h["trainer"], "rating": h["rating"], "status": h["status"], "actionUrl": f"/manager/horses/{h['id']}"}
                    for h in self.horses.values()
                ]
            }

        elif tool_name == "getRecentAuditLogs":
            limit = arguments.get("limit", 10)
            return {
                "totalAuditLogs": len(self.audit_logs),
                "returnedCount": min(limit, len(self.audit_logs)),
                "auditLogList": self.audit_logs[:limit],
                "actionUrl": "/manager/audit-log"
            }

        elif tool_name == "analyzeStableCapacityAndStallBottlenecks":
            return {
                "projected30DayOccupancy": "92.5%",
                "bottleneckRisk": "HIGH_QUARANTINE_CONGESTION",
                "bottleneckDetail": "Quarantine facility is projected to reach 100% capacity within 2 weeks due to incoming admissions and slow discharge clearance.",
                "recommendation": "Accelerate clinical discharge assessments for recovering horses (e.g. Storm Runner if eligible) or temporarily throttle new admissions."
            }

        elif tool_name == "recommendGroomWorkloadRebalancing":
            return {
                "imbalanceDetected": True,
                "analysis": "Barn B has 24 horses managed by 5 grooms (ratio 4.8 horses/groom), whereas Barn C has 5 horses managed by 2 grooms (ratio 2.5 horses/groom).",
                "recommendedRebalancing": "Transfer 1 groom from Barn C to Barn B morning shift to equalize workload to 3.8 horses/groom across all barns."
            }

        # ----------------- EDA Tools (getEdaAnalytics) -----------------
        elif tool_name == "getEdaAnalytics":
            analysis_type = arguments.get("analysisType")
            if analysis_type == "BIO_SPC":
                metric = arguments.get("metric", "TEMPERATURE")
                target = horse_name if horse_name else "Thunder"
                if metric == "RESTING_HEART_RATE":
                    return {
                        "analysisType": "BIO_SPC",
                        "horseName": target,
                        "metric": "RESTING_HEART_RATE",
                        "days": arguments.get("days", 30),
                        "baselineMean": 40.2,
                        "ucl": 48.5,
                        "lcl": 32.0,
                        "westernElectricViolations": 1,
                        "status": "OUT_OF_CONTROL_SHIFT",
                        "interpretation": "Elevated resting heart rate observed following intense interval sessions on Nov 17th."
                    }
                else:
                    return {
                        "analysisType": "BIO_SPC",
                        "horseName": target,
                        "metric": "TEMPERATURE",
                        "days": arguments.get("days", 14),
                        "baselineMean": 38.05,
                        "ucl": 38.55,
                        "lcl": 37.55,
                        "westernElectricViolations": 0,
                        "status": "IN_CONTROL",
                        "interpretation": "Normal temperature variance within physiological bounds."
                    }

            elif analysis_type == "SURVIVAL":
                stratify = arguments.get("stratifyBy", "SEVERITY")
                return {
                    "analysisType": "SURVIVAL",
                    "stratifyBy": stratify,
                    "kaplanMeierCurves": [
                        {"stratum": "MINOR", "medianRecoveryDays": 12, "sampleSize": 28},
                        {"stratum": "MODERATE", "medianRecoveryDays": 28, "sampleSize": 14},
                        {"stratum": "SEVERE", "medianRecoveryDays": 65, "sampleSize": 6}
                    ],
                    "logRankPValue": 0.0012,
                    "interpretation": "Statistically significant difference in recovery duration across injury severities."
                }

            return {
                "analysisType": analysis_type,
                "status": "COMPLETED",
                "dataSummary": "Standard descriptive exploratory analysis completed."
            }

        # Fallback if unknown tool
        return {
            "status": "ERROR",
            "error": f"Tool '{tool_name}' not recognized in RTMS catalog.",
            "aiGuidance": "Review available role tools and ensure canonical tool names are used."
        }

# ==============================================================================
# 4. BENCHMARK SCENARIOS SPECIFICATION (20 Scenarios across 5 Roles)
# ==============================================================================

BENCHMARK_SCENARIOS: List[Dict[str, Any]] = [
    # ----------------- HORSE_OWNER (4 Scenarios) -----------------
    {
        "id": "REACT_OWNER_01",
        "role": "HORSE_OWNER",
        "title": "Health Review & Care Schedule for Restricted Horses",
        "prompt": "Check all my horses. For any horse restricted from regular workouts, find out its medical diagnosis and look up its upcoming care schedule.",
        "expected_dag": {
            "nodes": ["getMyHorses", "getMyHorseHealthSummary", "getHorseCareSchedule"],
            "edges": [
                ("getMyHorses", "getMyHorseHealthSummary"),
                ("getMyHorseHealthSummary", "getHorseCareSchedule")
            ]
        },
        "optimal_steps": 3,
        "param_rules": [
            {"tool": "getMyHorseHealthSummary", "param": "horseName", "expected": ["Storm Runner", "storm runner", 3]},
            {"tool": "getHorseCareSchedule", "param": "horseName", "expected": ["Storm Runner", "storm runner", 3]}
        ],
        "system_rules": ["RULE_1_NO_NUMERIC_ID", "RULE_6_ENTITY_DISCOVERY"]
    },
    {
        "id": "REACT_OWNER_02",
        "role": "HORSE_OWNER",
        "title": "Heart Rate Bio-SPC Anomaly Analysis (Rule 6)",
        "prompt": "Review Thunder's recent training progress. If his recovery time was abnormal, analyze his resting heart rate Bio-SPC trend over the last 30 days.",
        "expected_dag": {
            "nodes": ["getMyHorseTrainingProgress", "getEdaAnalytics"],
            "edges": [
                ("getMyHorseTrainingProgress", "getEdaAnalytics")
            ]
        },
        "optimal_steps": 2,
        "param_rules": [
            {"tool": "getMyHorseTrainingProgress", "param": "horseName", "expected": ["Thunder", "thunder", 1]},
            {"tool": "getEdaAnalytics", "param": "analysisType", "expected": ["BIO_SPC"]},
            {"tool": "getEdaAnalytics", "param": "metric", "expected": ["RESTING_HEART_RATE"]}
        ],
        "system_rules": ["RULE_1_NO_NUMERIC_ID"]
    },
    {
        "id": "REACT_OWNER_03",
        "role": "HORSE_OWNER",
        "title": "Pedigree Lineage & Investment ROI Analysis",
        "prompt": "Evaluate Thunder's racing bloodline and pedigree, and combine that with his investment and fitness ROI development report.",
        "expected_dag": {
            "nodes": ["getMyHorsePedigreeInfo", "analyzeHorseInvestmentAndFitnessROI"],
            "edges": []  # Independent branches allowed in any order
        },
        "optimal_steps": 2,
        "param_rules": [
            {"tool": "getMyHorsePedigreeInfo", "param": "horseName", "expected": ["Thunder", "thunder", 1]},
            {"tool": "analyzeHorseInvestmentAndFitnessROI", "param": "horseName", "expected": ["Thunder", "thunder", 1]}
        ],
        "system_rules": ["RULE_1_NO_NUMERIC_ID"]
    },
    {
        "id": "REACT_OWNER_04",
        "role": "HORSE_OWNER",
        "title": "Quarantine Safety Gate (Rule 3 Compliance)",
        "prompt": "Can Storm Runner be registered for the upcoming weekend race schedule?",
        "expected_dag": {
            "nodes": ["getMyHorses", "getMyHorseHealthSummary"],
            "edges": [("getMyHorses", "getMyHorseHealthSummary")]
        },
        "optimal_steps": 2,
        "param_rules": [
            {"tool": "getMyHorseHealthSummary", "param": "horseName", "expected": ["Storm Runner", "storm runner", 3]}
        ],
        "system_rules": ["RULE_3_QUARANTINE_TRAINING_BLOCKED", "RULE_1_NO_NUMERIC_ID"]
    },

    # ----------------- HEAD_TRAINER (4 Scenarios) -----------------
    {
        "id": "REACT_TRAINER_01",
        "role": "HEAD_TRAINER",
        "title": "Group Fatigue Risk Triage & Strategy Adjustment (Rule 6)",
        "prompt": "Check the horses in Sprint Group Alpha for high fatigue risk, audit the workout history for the affected horse, and recommend an adjusted strategy.",
        "expected_dag": {
            "nodes": ["getTrainerGroups", "getTrainerRiskAlerts", "getWorkoutHistory", "recommendTrainerStrategy"],
            "edges": [
                ("getTrainerGroups", "getTrainerRiskAlerts"),
                ("getTrainerRiskAlerts", "getWorkoutHistory"),
                ("getWorkoutHistory", "recommendTrainerStrategy")
            ]
        },
        "optimal_steps": 4,
        "param_rules": [
            {"tool": "getTrainerRiskAlerts", "param": "horseName", "expected": ["Thunder", "thunder", 1]},
            {"tool": "getWorkoutHistory", "param": "horseName", "expected": ["Thunder", "thunder", 1]},
            {"tool": "recommendTrainerStrategy", "param": "horseName", "expected": ["Thunder", "thunder", 1]}
        ],
        "system_rules": ["RULE_1_NO_NUMERIC_ID", "RULE_6_ENTITY_DISCOVERY"]
    },
    {
        "id": "REACT_TRAINER_02",
        "role": "HEAD_TRAINER",
        "title": "Distance Rating Ranking & Race Strategy (Rule 15)",
        "prompt": "Rank all my horses by their average rating, find the top sprint performer, and generate an optimal race strategy for that horse.",
        "expected_dag": {
            "nodes": ["getHorseSpecialtyAnalysis", "recommendTrainerStrategy"],
            "edges": [
                ("getHorseSpecialtyAnalysis", "recommendTrainerStrategy")
            ]
        },
        "optimal_steps": 2,
        "param_rules": [
            {"tool": "getHorseSpecialtyAnalysis", "param": "sortBy", "expected": ["rating", "averageRating"]},
            {"tool": "recommendTrainerStrategy", "param": "horseName", "expected": ["Blaze", "blaze", 4]}
        ],
        "system_rules": ["RULE_15_HORSE_RANKING", "RULE_1_NO_NUMERIC_ID"]
    },
    {
        "id": "REACT_TRAINER_03",
        "role": "HEAD_TRAINER",
        "title": "Missing Workout Results Audit & Groom Contact (Rule 11)",
        "prompt": "Find any training sessions in the last 90 days with horses missing results, and check which grooms are assigned to care for those horses.",
        "expected_dag": {
            "nodes": ["getTrainerSchedule", "getTrainerAssignedGrooms"],
            "edges": [
                ("getTrainerSchedule", "getTrainerAssignedGrooms")
            ]
        },
        "optimal_steps": 2,
        "param_rules": [
            {"tool": "getTrainerSchedule", "param": "needsResultOnly", "expected": [True, "true"]}
        ],
        "system_rules": ["RULE_11_GROOM_CONTACT_INFO", "RULE_1_NO_NUMERIC_ID"]
    },
    {
        "id": "REACT_TRAINER_04",
        "role": "HEAD_TRAINER",
        "title": "Admission Review & Group Capacity Intake Check",
        "prompt": "Review pending admission reviews assigned to me. Can we accept the candidate into Sprint Group Alpha without exceeding the 6-horse limit?",
        "expected_dag": {
            "nodes": ["getAssignedAdmissionReviews", "getTrainerGroups"],
            "edges": []
        },
        "optimal_steps": 2,
        "param_rules": [],
        "system_rules": ["RULE_1_NO_NUMERIC_ID"]
    },

    # ----------------- VETERINARIAN (4 Scenarios) -----------------
    {
        "id": "REACT_VET_01",
        "role": "VETERINARIAN",
        "title": "Incident Triage -> Vital Signs -> Clinical RAG Protocol",
        "prompt": "There was an incident reported in Barn A. Identify the horse involved, pull up its vital signs, search clinical protocols for the condition, and recommend an intervention plan.",
        "expected_dag": {
            "nodes": ["getUrgentCareAndIncidentAlerts", "getHorseVitalSignsTrend", "searchClinicalKnowledge", "recommendClinicalInterventionPlan"],
            "edges": [
                ("getUrgentCareAndIncidentAlerts", "getHorseVitalSignsTrend"),
                ("getHorseVitalSignsTrend", "searchClinicalKnowledge"),
                ("searchClinicalKnowledge", "recommendClinicalInterventionPlan")
            ]
        },
        "optimal_steps": 4,
        "param_rules": [
            {"tool": "getHorseVitalSignsTrend", "param": "horseName", "expected": ["Storm Runner", "storm runner", 3]},
            {"tool": "recommendClinicalInterventionPlan", "param": "horseName", "expected": ["Storm Runner", "storm runner", 3]}
        ],
        "system_rules": ["RULE_1_NO_NUMERIC_ID", "RULE_6_ENTITY_DISCOVERY"]
    },
    {
        "id": "REACT_VET_02",
        "role": "VETERINARIAN",
        "title": "Prescription Cross-Check & Drug Contraindication (Rule 8)",
        "prompt": "Storm Runner has an inflamed fetlock. Check his current active prescriptions and medical history to ensure no drug interactions, then recommend a clinical intervention plan.",
        "expected_dag": {
            "nodes": ["getHorseMedicalHistory", "recommendClinicalInterventionPlan"],
            "edges": [
                ("getHorseMedicalHistory", "recommendClinicalInterventionPlan")
            ]
        },
        "optimal_steps": 2,
        "param_rules": [
            {"tool": "getHorseMedicalHistory", "param": "horseName", "expected": ["Storm Runner", "storm runner", 3]},
            {"tool": "recommendClinicalInterventionPlan", "param": "horseName", "expected": ["Storm Runner", "storm runner", 3]}
        ],
        "system_rules": ["RULE_8_DRUG_CONTRAINDICATION", "RULE_1_NO_NUMERIC_ID"]
    },
    {
        "id": "REACT_VET_03",
        "role": "VETERINARIAN",
        "title": "Quarantine Discharge Readiness Evaluation (Rule 3)",
        "prompt": "Evaluate whether Storm Runner can be discharged from quarantine and cleared for full workout resumption.",
        "expected_dag": {
            "nodes": ["getQuarantineStatus", "evaluateTrainingResumptionReadiness"],
            "edges": [
                ("getQuarantineStatus", "evaluateTrainingResumptionReadiness")
            ]
        },
        "optimal_steps": 2,
        "param_rules": [
            {"tool": "evaluateTrainingResumptionReadiness", "param": "horseName", "expected": ["Storm Runner", "storm runner", 3]}
        ],
        "system_rules": ["RULE_3_QUARANTINE_TRAINING_BLOCKED", "RULE_1_NO_NUMERIC_ID"]
    },
    {
        "id": "REACT_VET_04",
        "role": "VETERINARIAN",
        "title": "Incident Patterns & Kaplan-Meier Survival EDA",
        "prompt": "Analyze incident patterns for recurring injuries, and run a Kaplan-Meier survival curve stratified by severity to estimate median recovery days.",
        "expected_dag": {
            "nodes": ["analyzeIncidentPatternsAndRecurringRisks", "getEdaAnalytics"],
            "edges": [
                ("analyzeIncidentPatternsAndRecurringRisks", "getEdaAnalytics")
            ]
        },
        "optimal_steps": 2,
        "param_rules": [
            {"tool": "getEdaAnalytics", "param": "analysisType", "expected": ["SURVIVAL"]},
            {"tool": "getEdaAnalytics", "param": "stratifyBy", "expected": ["SEVERITY"]}
        ],
        "system_rules": ["RULE_1_NO_NUMERIC_ID"]
    },

    # ----------------- GROOM (4 Scenarios) -----------------
    {
        "id": "REACT_GROOM_01",
        "role": "GROOM",
        "title": "Colic Anomaly Triage & Shift Task Re-prioritization",
        "prompt": "One of my assigned horses is behaving unusually and leaving feed. Find out which horse it is, check dietary alerts, and prioritize my morning duties.",
        "expected_dag": {
            "nodes": ["getMyAssignedHorses", "detectHorseBehavioralAnomaly", "getHorseDietaryAndCareAlerts", "recommendDailyCarePriorityOrder"],
            "edges": [
                ("getMyAssignedHorses", "detectHorseBehavioralAnomaly"),
                ("detectHorseBehavioralAnomaly", "getHorseDietaryAndCareAlerts"),
                ("getHorseDietaryAndCareAlerts", "recommendDailyCarePriorityOrder")
            ]
        },
        "optimal_steps": 4,
        "param_rules": [
            {"tool": "detectHorseBehavioralAnomaly", "param": "horseName", "expected": ["Thunder", "thunder", 1]},
            {"tool": "getHorseDietaryAndCareAlerts", "param": "horseName", "expected": ["Thunder", "thunder", 1]}
        ],
        "system_rules": ["RULE_1_NO_NUMERIC_ID", "RULE_6_ENTITY_DISCOVERY"]
    },
    {
        "id": "REACT_GROOM_02",
        "role": "GROOM",
        "title": "Stall Location & Wound Bandage Task Check (Rule 7)",
        "prompt": "Where is Lightning stabled, and are there any pending medical tasks or bandages required for him today?",
        "expected_dag": {
            "nodes": ["getHorseStallLocation", "getTodayAggregatedTasks", "getHorseDietaryAndCareAlerts"],
            "edges": [
                ("getHorseStallLocation", "getTodayAggregatedTasks"),
                ("getTodayAggregatedTasks", "getHorseDietaryAndCareAlerts")
            ]
        },
        "optimal_steps": 3,
        "param_rules": [
            {"tool": "getHorseStallLocation", "param": "horseName", "expected": ["Lightning", "lightning", 2]},
            {"tool": "getHorseDietaryAndCareAlerts", "param": "horseName", "expected": ["Lightning", "lightning", 2]}
        ],
        "system_rules": ["RULE_7_STALL_LOCATION_AND_AREA", "RULE_1_NO_NUMERIC_ID"]
    },
    {
        "id": "REACT_GROOM_03",
        "role": "GROOM",
        "title": "Daily Shift Completion Rate & Incident Audit",
        "prompt": "Check my daily task completion rate for today, and retrieve the details of any incident I reported during my shift.",
        "expected_dag": {
            "nodes": ["getGroomTaskCompletionRate", "getMyReportedIncidents"],
            "edges": []
        },
        "optimal_steps": 2,
        "param_rules": [],
        "system_rules": ["RULE_1_NO_NUMERIC_ID"]
    },
    {
        "id": "REACT_GROOM_04",
        "role": "GROOM",
        "title": "Lethargy Anomaly & Groom Bio-SPC Temperature Chart",
        "prompt": "Check my assigned horses, identify any horse showing lethargy, and plot its temperature Bio-SPC chart for the last 14 days.",
        "expected_dag": {
            "nodes": ["getMyAssignedHorses", "detectHorseBehavioralAnomaly", "getEdaAnalytics"],
            "edges": [
                ("getMyAssignedHorses", "detectHorseBehavioralAnomaly"),
                ("detectHorseBehavioralAnomaly", "getEdaAnalytics")
            ]
        },
        "optimal_steps": 3,
        "param_rules": [
            {"tool": "getEdaAnalytics", "param": "analysisType", "expected": ["BIO_SPC"]},
            {"tool": "getEdaAnalytics", "param": "metric", "expected": ["TEMPERATURE"]}
        ],
        "system_rules": ["RULE_1_NO_NUMERIC_ID"]
    },

    # ----------------- CLUB_MANAGER (4 Scenarios) -----------------
    {
        "id": "REACT_MANAGER_01",
        "role": "CLUB_MANAGER",
        "title": "Admissions Funnel & Stable Capacity Bottleneck Forecast (Rules 12 & 13)",
        "prompt": "Review pending admission applications and check if our current regular and quarantine stall capacity can accommodate them over the next month.",
        "expected_dag": {
            "nodes": ["getAdmissionPipelineOverview", "getStableOccupancyMetrics", "analyzeStableCapacityAndStallBottlenecks"],
            "edges": [
                ("getAdmissionPipelineOverview", "getStableOccupancyMetrics"),
                ("getStableOccupancyMetrics", "analyzeStableCapacityAndStallBottlenecks")
            ]
        },
        "optimal_steps": 3,
        "param_rules": [
            {"tool": "getAdmissionPipelineOverview", "param": "status", "expected": ["PENDING", "pending"]}
        ],
        "system_rules": ["RULE_12_DISTINGUISH_HORSE_AND_STALL", "RULE_13_ADMISSION_STATISTICS", "RULE_1_NO_NUMERIC_ID"]
    },
    {
        "id": "REACT_MANAGER_02",
        "role": "CLUB_MANAGER",
        "title": "System Staffing & Groom Workload Rebalancing (Rule 14)",
        "prompt": "Check our system staff breakdown, evaluate groom area distribution, and recommend workload rebalancing if any barn is overloaded.",
        "expected_dag": {
            "nodes": ["getSystemStaffOverview", "getTrainerAreaDistribution", "recommendGroomWorkloadRebalancing"],
            "edges": [
                ("getSystemStaffOverview", "getTrainerAreaDistribution"),
                ("getTrainerAreaDistribution", "recommendGroomWorkloadRebalancing")
            ]
        },
        "optimal_steps": 3,
        "param_rules": [
            {"tool": "getSystemStaffOverview", "param": "role", "expected": ["GROOM", "groom"]}
        ],
        "system_rules": ["RULE_14_STAFF_STATISTICS", "RULE_1_NO_NUMERIC_ID"]
    },
    {
        "id": "REACT_MANAGER_03",
        "role": "CLUB_MANAGER",
        "title": "Club Health & Top Performer Stall Placement (Rules 12 & 15)",
        "prompt": "Give me a club health overview, find our top 3 racing performers, and check their current stall assignments.",
        "expected_dag": {
            "nodes": ["getClubHealthOverview", "getHorseSpecialtyAnalysis", "getClubHorsesDirectory"],
            "edges": [
                ("getClubHealthOverview", "getHorseSpecialtyAnalysis"),
                ("getHorseSpecialtyAnalysis", "getClubHorsesDirectory")
            ]
        },
        "optimal_steps": 3,
        "param_rules": [
            {"tool": "getHorseSpecialtyAnalysis", "param": "sortBy", "expected": ["rating", "averageRating"]}
        ],
        "system_rules": ["RULE_12_DISTINGUISH_HORSE_AND_STALL", "RULE_15_HORSE_RANKING", "RULE_1_NO_NUMERIC_ID"]
    },
    {
        "id": "REACT_MANAGER_04",
        "role": "CLUB_MANAGER",
        "title": "System Security Audit Trail & Staff Cross-Check",
        "prompt": "Audit recent activity logs for failed access attempts or unauthorized modifications, and cross-check with active system staff.",
        "expected_dag": {
            "nodes": ["getRecentAuditLogs", "getSystemStaffOverview"],
            "edges": [
                ("getRecentAuditLogs", "getSystemStaffOverview")
            ]
        },
        "optimal_steps": 2,
        "param_rules": [
            {"tool": "getRecentAuditLogs", "param": "limit", "expected": [50, 10, None]}
        ],
        "system_rules": ["RULE_1_NO_NUMERIC_ID"]
    }
]

# ==============================================================================
# 5. DETERMINISTIC ORACLE TRACES (For Dry-Run Simulation)
# ==============================================================================

ORACLE_SIMULATION_STEPS: Dict[str, List[Dict[str, Any]]] = {
    "REACT_OWNER_01": [
        {
            "thought": "The user is a HORSE_OWNER asking to check all their horses, find any horse restricted from workouts, retrieve its diagnosis, and check its care schedule. First, I must call 'getMyHorses' to discover the owner's horses and identify any restricted horse.",
            "tool_call": {"name": "getMyHorses", "arguments": {}}
        },
        {
            "thought": "From the 'getMyHorses' observation, Storm Runner is under MEDICAL_REST and not cleared for training. Now I must look up its health summary and medical diagnosis using 'getMyHorseHealthSummary'.",
            "tool_call": {"name": "getMyHorseHealthSummary", "arguments": {"horseName": "Storm Runner"}}
        },
        {
            "thought": "Storm Runner is diagnosed with a deep fetlock laceration with secondary infection and fever, currently taking Phenylbutazone. Now I must retrieve its upcoming care schedule using 'getHorseCareSchedule'.",
            "tool_call": {"name": "getHorseCareSchedule", "arguments": {"horseName": "Storm Runner"}}
        },
        {
            "thought": "I now have all the necessary information. I will synthesize a comprehensive report summarizing Storm Runner's medical status, diagnosis, and upcoming care schedule while adhering strictly to Rule 1 (no numeric IDs) and presenting clear Markdown formatting.",
            "final_text": "### Health & Care Schedule Summary for Storm Runner\n\nFollowing your review of all owned horses, **Storm Runner** is currently restricted from regular workouts under medical rest in Quarantine Stall Q-01.\n\n#### Medical Diagnosis & Status\n- **Current Status:** Medical Rest (Training Cleared: False)\n- **Primary Diagnosis:** Deep fetlock laceration with secondary purulent infection and acute fever.\n- **Active Prescription:** Phenylbutazone oral therapy (SID).\n- **Readiness Score:** 68/100 (Below the standard clearance threshold of 85).\n\n#### Upcoming Veterinary Care Schedule\n| Date | Appointment Type | Details | Attending Veterinarian | Status |\n| :--- | :--- | :--- | :--- | :--- |\n| 2026-11-19 | Veterinary Exam | Fetlock wound re-dressing & bacterial swab | Dr. Sarah Jenkins | Scheduled |\n| 2026-11-24 | Preventive Care | Equine Influenza Booster Vaccination | Dr. Sarah Jenkins | Upcoming |\n\nFor more details, you can visit the [Horse Profile](/owner/horses) on your dashboard."
        }
    ],
    "REACT_OWNER_02": [
        {
            "thought": "The owner wants to review Thunder's recent training progress and evaluate his recovery time. I will first call 'getMyHorseTrainingProgress' with horseName='Thunder'.",
            "tool_call": {"name": "getMyHorseTrainingProgress", "arguments": {"horseName": "Thunder"}}
        },
        {
            "thought": "Thunder's training progress shows a training decision of 'MONITOR_CARDIAC_RECOVERY_BIO_SPC'. As instructed by the user prompt, I must now conduct a Bio-SPC analysis of his resting heart rate over the last 30 days using 'getEdaAnalytics'.",
            "tool_call": {"name": "getEdaAnalytics", "arguments": {"analysisType": "BIO_SPC", "horseName": "Thunder", "metric": "RESTING_HEART_RATE", "days": 30}}
        },
        {
            "thought": "The Bio-SPC analysis reveals an out-of-control shift with elevated resting heart rate post-workout. I will synthesize the findings and provide actionable recommendations.",
            "final_text": "### Thunder - Training Progress & Resting Heart Rate Bio-SPC Analysis\n\n#### Training Progress Overview\n- **Course:** Sprint Mastery Level 2 (78% completion across 14 sessions).\n- **Recent Assessment:** An abnormal cardiac recovery time was detected during recent interval gallops, triggering a Bio-SPC monitoring directive.\n\n#### 30-Day Resting Heart Rate Bio-SPC Control Chart Findings\n- **Baseline Mean:** 40.2 bpm (Normal physiological resting baseline: 36–42 bpm).\n- **Upper Control Limit (UCL):** 48.5 bpm\n- **Lower Control Limit (LCL):** 32.0 bpm\n- **Statistical Anomaly:** 1 Western Electric Rule violation (Out-of-control shift detected following high-intensity workouts).\n\n**Recommendation:** Thunder is exhibiting mild cardiac fatigue. It is strongly recommended to schedule 48 hours of light aerobic trot recovery before attempting another interval breezing session."
        }
    ],
    "REACT_OWNER_03": [
        {
            "thought": "The user wants both Thunder's pedigree information and his investment/fitness ROI development report. First, I will look up his bloodline using 'getMyHorsePedigreeInfo'.",
            "tool_call": {"name": "getMyHorsePedigreeInfo", "arguments": {"horseName": "Thunder"}}
        },
        {
            "thought": "Next, I need to look up his investment and fitness development report using 'analyzeHorseInvestmentAndFitnessROI'.",
            "tool_call": {"name": "analyzeHorseInvestmentAndFitnessROI", "arguments": {"horseName": "Thunder"}}
        },
        {
            "thought": "I now combine both the pedigree bloodline and the investment ROI data into a comprehensive report.",
            "final_text": "### Thunder - Pedigree Lineage & Investment ROI Analysis\n\n#### Pedigree & Bloodline Profile\n- **Horse Name:** Thunder (Thoroughbred Stallion, Foaled April 12, 2022)\n- **Sire:** Storm Cat (Renowned Grade 1 sprint and juvenile sire line)\n- **Dam:** Northern Dancer Line\n- **Bloodline Rating:** Grade 1 Stakes Sire Prospect with superior early anaerobic speed markers.\n\n#### Investment & Fitness ROI Development\n- **Projected Racing Value Growth:** +24.8% Year-to-Date\n- **Career Trajectory:** Ascending Peak Form\n- **Fitness Index:** 91.2/100\n- **Commercial Appraisal:** Outstanding breeding and syndication prospects upon completion of the Graded Sprint campaign.\n\nYou can review Thunder's full pedigree and race history at [Horse Profile](/owner/horses)."
        }
    ],
    "REACT_OWNER_04": [
        {
            "thought": "The owner asks if Storm Runner can be registered for the upcoming weekend race schedule. Per Rule 3 (Medical Quarantine Gate), horses in medical quarantine must never be allowed to train or race. I will first query 'getMyHorses' to verify his stabling and clearance status.",
            "tool_call": {"name": "getMyHorses", "arguments": {}}
        },
        {
            "thought": "Storm Runner is listed as MEDICAL_REST in Quarantine Stall Q-01 with trainingCleared=False. I will double-check the clinical details via 'getMyHorseHealthSummary'.",
            "tool_call": {"name": "getMyHorseHealthSummary", "arguments": {"horseName": "Storm Runner"}}
        },
        {
            "thought": "The health summary confirms Storm Runner has a readiness score of 68/100, an active fetlock laceration infection, and trainingCleared=False. Per Rule 3, I must strictly and unconditionally refuse race registration.",
            "final_text": "### Weekend Race Registration Decision: DECLINED (Medical Quarantine Gate)\n\n**Storm Runner CANNOT be registered for the upcoming weekend race schedule.**\n\n#### Safety & Regulatory Grounds (Rule 3 Compliance):\n1. **Quarantine Confinement:** Storm Runner is currently housed in Quarantine Stall Q-01 under active medical isolation.\n2. **Medical Clearance:** Training and racing clearance is strictly **BLOCKED** due to an active fetlock laceration with secondary infection and fever.\n3. **Readiness Evaluation:** His current clinical readiness score is **68/100**, which is well below the mandatory clearance threshold of 85/100.\n4. **Welfare & Safety:** Subjecting a horse undergoing active antimicrobial therapy to race conditions violates both club welfare standards and racing regulations.\n\nStorm Runner must remain in medical rest until formal clinical discharge is granted by Dr. Sarah Jenkins."
        }
    ],
    "REACT_TRAINER_01": [
        {
            "thought": "The HEAD_TRAINER wants to audit Sprint Group Alpha for fatigue risks, inspect workout history for the affected horse, and recommend an adjusted strategy. First, I call 'getTrainerGroups' to examine the members of Sprint Group Alpha.",
            "tool_call": {"name": "getTrainerGroups", "arguments": {}}
        },
        {
            "thought": "Sprint Group Alpha has 5 members (Thunder, Blaze, Lightning, Shadow, Comet). Now I will query 'getTrainerRiskAlerts' for Thunder to inspect the fatigue indicators.",
            "tool_call": {"name": "getTrainerRiskAlerts", "arguments": {"horseName": "Thunder"}}
        },
        {
            "thought": "Thunder has an Acute-to-Chronic Workload Ratio (ACWR) of 1.45, indicating acute fatigue and high injury risk. Now I inspect his recent workouts via 'getWorkoutHistory'.",
            "tool_call": {"name": "getWorkoutHistory", "arguments": {"horseName": "Thunder"}}
        },
        {
            "thought": "Thunder's recent 1000m interval gallop reached a peak HR of 218 bpm with 18.5 minutes recovery. Now I call 'recommendTrainerStrategy' to formulate an adjusted strategy.",
            "tool_call": {"name": "recommendTrainerStrategy", "arguments": {"horseName": "Thunder"}}
        },
        {
            "thought": "I now summarize the entire audit, from group check to individual fatigue diagnosis, workout history, and strategy adjustment.",
            "final_text": "### Sprint Group Alpha - Fatigue Risk Audit & Strategy Adjustment\n\n#### 1. Group Overview & Risk Identification\nSprint Group Alpha currently contains 5 active horses (Thunder, Blaze, Lightning, Shadow, and Comet). \n- **Affected Entity:** **Thunder**\n- **Fatigue Indicator:** Acute-to-Chronic Workload Ratio (ACWR) has surged to **1.45** (Safe corridor: 0.80–1.25). Severe overtraining strain was flagged on his [Trainer Dashboard](/trainer/dashboard).\n\n#### 2. Workout History Audit (Thunder)\n| Date | Exercise | Peak Heart Rate | Recovery Time | Outcome |\n| :--- | :--- | :--- | :--- | :--- |\n| 2026-11-17 | Interval Gallop 1000m | 218 bpm | 18.5 min | Completed (High Cardiac Strain) |\n| 2026-11-14 | Breeze 800m | 204 bpm | 14.0 min | Completed |\n| 2026-11-11 | Canter 1200m | 195 bpm | 11.2 min | Completed |\n\n#### 3. Recommended Tactical Strategy & Workout Adjustment\n- **Deloading Phase:** Implement an immediate 30% reduction in high-speed gallop volume for the next 5 days.\n- **Active Recovery:** Substitute intense gate breezing with 25-minute light aerobic trotting to allow cardiac glycogen re-synthesis.\n- **Target Metric:** Bring ACWR down below 1.20 prior to resuming timed interval sprints."
        }
    ],
    "REACT_TRAINER_02": [
        {
            "thought": "The trainer wants to rank all managed horses by average rating, identify the top sprint performer, and generate an optimal race strategy. I will call 'getHorseSpecialtyAnalysis' with sortBy='rating'.",
            "tool_call": {"name": "getHorseSpecialtyAnalysis", "arguments": {"sortBy": "rating"}}
        },
        {
            "thought": "The ranking shows Blaze is the top sprint performer with an average rating of 95.0. Now I call 'recommendTrainerStrategy' with horseName='Blaze'.",
            "tool_call": {"name": "recommendTrainerStrategy", "arguments": {"horseName": "Blaze"}}
        },
        {
            "thought": "I will synthesize the horse ranking table and present the optimal racing strategy for Blaze.",
            "final_text": "### Horse Fleet Ranking & Strategy for Top Performer\n\n#### Fleet Specialty & Rating Ranking (Rule 15)\n| Rank | Horse Name | Average Rating | Best Distance & Surface | Specialty Count |\n| :---: | :--- | :---: | :--- | :---: |\n| **1** | **Blaze** | **95.0** | **Sprint 1200m Dirt** | **3** |\n| 2 | Thunder | 92.5 | Sprint 1200m Dirt | 2 |\n| 3 | Shadow | 89.0 | Miler 1600m Turf | 1 |\n| 4 | Lightning | 88.0 | Stamina 2400m Turf | 1 |\n| 5 | Comet | 87.5 | Middle Distance 2000m Turf | 1 |\n\n#### Optimal Race Strategy for Blaze (Top Performer)\n- **Distance Specialty:** 1200m Dirt (Fast track preferred).\n- **Pacing Strategy:** Front-runner / Early pacesetter with superior gate break velocity.\n- **Workout Adjustment:** Maintain anaerobic power with sharp 400m blowout sprints twice weekly, tapering 7 days before target graded stakes."
        }
    ],
    "REACT_TRAINER_03": [
        {
            "thought": "The trainer wants to find training sessions in the last 90 days with missing workout results, and check the assigned grooms. I call 'getTrainerSchedule' with needsResultOnly=True.",
            "tool_call": {"name": "getTrainerSchedule", "arguments": {"needsResultOnly": True}}
        },
        {
            "thought": "Session 402 has Comet missing a workout result, and groom John Doe is assigned. Now I call 'getTrainerAssignedGrooms' to retrieve contact details.",
            "tool_call": {"name": "getTrainerAssignedGrooms", "arguments": {}}
        },
        {
            "thought": "I now combine the unrecorded workout session details with John Doe's contact information per Rule 11.",
            "final_text": "### Unrecorded Workout Attendance & Assigned Groom Contact (Rule 11)\n\n#### 1. Sessions Missing Workout Results (Last 90 Days)\n| Session ID | Date | Exercise | Unrecorded Horse | Assigned Groom | Link |\n| :---: | :--- | :--- | :--- | :--- | :--- |\n| Session 402 | 2026-11-15 | Gate Break & Sprint 600m | **Comet** | John Doe | [Update Schedule](/trainer/schedule) |\n\n#### 2. Responsible Groom Contact Information\n- **Groom Name:** John Doe\n- **Contact Phone:** +1-555-0192\n- **Email:** johndoe@rtms.club\n- **Assigned Barn:** Barn A\n- **Stalls Managed:** Stalls A-101 through A-105 (caring for Thunder, Lightning, Blaze, Shadow, and Comet)\n\nPlease contact Groom John Doe directly to record the completion metrics for Comet's sprint session."
        }
    ],
    "REACT_TRAINER_04": [
        {
            "thought": "The trainer wants to review pending admission applications and check if the candidate can be accepted into Sprint Group Alpha without exceeding the 6-horse limit. First, I check pending admissions via 'getAssignedAdmissionReviews'.",
            "tool_call": {"name": "getAssignedAdmissionReviews", "arguments": {}}
        },
        {
            "thought": "The candidate horse is Silver Mist (Application #105), targeting Sprint Group Alpha. Now I inspect 'getTrainerGroups' to evaluate current capacity.",
            "tool_call": {"name": "getTrainerGroups", "arguments": {}}
        },
        {
            "thought": "Sprint Group Alpha currently has 5 horses (Thunder, Blaze, Lightning, Shadow, Comet) out of a maximum capacity of 6. Therefore, Silver Mist CAN be accepted.",
            "final_text": "### Admission Intake Review & Group Capacity Assessment\n\n#### 1. Pending Admission Application\n- **Candidate Horse:** Silver Mist (Application #105)\n- **Nominated Training Group:** Sprint Group Alpha\n- **Current Stage:** Awaiting Trainer Capacity Confirmation\n\n#### 2. Sprint Group Alpha Capacity Audit\n- **Maximum Group Capacity:** 6 horses (Club Standard Limit)\n- **Current Enrolled Horses:** 5 horses (Thunder, Blaze, Lightning, Shadow, and Comet)\n- **Available Openings:** **1 slot available**\n\n#### Recommendation\n**YES, Silver Mist CAN be accepted into Sprint Group Alpha without exceeding the 6-horse limit.** Adding Silver Mist will bring the group to full capacity (6/6). You can confirm acceptance via [Admission Reviews](/trainer/admissions)."
        }
    ],
    "REACT_VET_01": [
        {
            "thought": "The veterinarian received notice of an incident in Barn A. First, I must identify the horse involved using 'getUrgentCareAndIncidentAlerts'.",
            "tool_call": {"name": "getUrgentCareAndIncidentAlerts", "arguments": {}}
        },
        {
            "thought": "The incident involves Storm Runner suffering a fetlock laceration with acute fever. Next, I pull his recent vital signs using 'getHorseVitalSignsTrend'.",
            "tool_call": {"name": "getHorseVitalSignsTrend", "arguments": {"horseName": "Storm Runner"}}
        },
        {
            "thought": "Storm Runner has a temperature of 39.1C and resting heart rate of 58 bpm. Now I search clinical knowledge protocols via 'searchClinicalKnowledge'.",
            "tool_call": {"name": "searchClinicalKnowledge", "arguments": {"query": "fetlock laceration infection fever"}}
        },
        {
            "thought": "Now I recommend a clinical intervention plan via 'recommendClinicalInterventionPlan'.",
            "tool_call": {"name": "recommendClinicalInterventionPlan", "arguments": {"horseName": "Storm Runner", "symptoms": "fetlock laceration infection fever"}}
        },
        {
            "thought": "I synthesize the clinical case, including vital signs, RAG protocol, and intervention plan.",
            "final_text": "### Urgent Clinical Incident Investigation & Intervention Plan\n\n#### 1. Incident Identification (Barn A Alert INC-2026-88)\n- **Patient:** Storm Runner (Housed in Quarantine Stall Q-01)\n- **Incident:** Left hind fetlock laceration with swelling and acute fever.\n\n#### 2. Vital Signs Monitoring\n- **Body Temperature:** 39.1°C (Febrile, baseline: 37.5–38.5°C)\n- **Resting Heart Rate:** 58 bpm (Tachycardia, baseline: 36–42 bpm)\n- **Respiratory Rate:** 26 breaths/min\n\n#### 3. Clinical Protocol & Intervention Plan\n- **Step 1 (Immediate Wound Care):** Sterile wound debridement and continuous isotonic saline lavage (0.9% NaCl). Initiate systemic antimicrobial coverage with Procaine Penicillin G combined with Gentamicin.\n- **Step 2 (Diagnostic Imaging):** Perform digital radiography and soft-tissue ultrasound of the fetlock joint to rule out sesamoidean ligament damage or septic tenosynovitis.\n- **Drug Interaction Warning (Rule 8):** Patient is on Phenylbutazone. Do NOT co-administer Flunixin Meglumine (Banamine) or other NSAIDs due to acute risk of gastrointestinal ulceration and renal damage.\n\nTrack case updates at [Urgent Care](/veterinarian/urgent)."
        }
    ],
    "REACT_VET_02": [
        {
            "thought": "The veterinarian is evaluating Storm Runner's inflamed fetlock. First, I check active prescriptions and medical history via 'getHorseMedicalHistory' to prevent drug conflicts.",
            "tool_call": {"name": "getHorseMedicalHistory", "arguments": {"horseName": "Storm Runner"}}
        },
        {
            "thought": "Storm Runner is actively taking Phenylbutazone 2g oral SID (an NSAID). Now I call 'recommendClinicalInterventionPlan' with symptoms='fetlock inflammation'.",
            "tool_call": {"name": "recommendClinicalInterventionPlan", "arguments": {"horseName": "Storm Runner", "symptoms": "fetlock inflammation"}}
        },
        {
            "thought": "I must highlight the drug contraindication between Phenylbutazone and other NSAIDs such as Flunixin Meglumine per Rule 8.",
            "final_text": "### Prescription Cross-Check & Clinical Intervention (Rule 8)\n\n#### 1. Current Active Prescriptions & Medical History\n- **Horse Name:** Storm Runner (Quarantine Stall Q-01)\n- **Active Medication:** Phenylbutazone (2g oral SID, NSAID class)\n- **Condition:** Left hind fetlock laceration with acute inflammation.\n\n#### 2. Critical Drug Interaction & Contraindication Safeguard\n- **CONTRAINDICATION WARNING:** Because Storm Runner is currently on an active course of **Phenylbutazone**, the co-administration of **Flunixin Meglumine (Banamine)** or any secondary systemic NSAID is **STRICTLY CONTRAINDICATED**.\n- **Physiological Risk:** Simultaneous administration of multiple NSAIDs causes severe synergistic toxicity, including acute right dorsal colitis, severe gastric ulceration, and renal papillary necrosis.\n\n#### 3. Recommended Clinical Plan\n- Continue Phenylbutazone at monitored dosage.\n- Apply cold hydrotherapy / ice boots twice daily for 20 minutes to reduce local fetlock edema.\n- Maintain sterile wound dressing; review full record at [Medical Records](/veterinarian/health)."
        }
    ],
    "REACT_VET_03": [
        {
            "thought": "The veterinarian asks to evaluate if Storm Runner can be discharged from quarantine and cleared for workouts. Per Rule 3, I first check 'getQuarantineStatus'.",
            "tool_call": {"name": "getQuarantineStatus", "arguments": {}}
        },
        {
            "thought": "Storm Runner is in Quarantine Stall Q-01 with trainingStatus='BLOCKED'. Now I evaluate his clinical readiness via 'evaluateTrainingResumptionReadiness'.",
            "tool_call": {"name": "evaluateTrainingResumptionReadiness", "arguments": {"horseName": "Storm Runner"}}
        },
        {
            "thought": "His readiness score is 68/100, which is below the 85 threshold. I must strictly deny discharge and clearance per Rule 3.",
            "final_text": "### Quarantine Discharge & Workout Resumption Assessment: DENIED (Rule 3)\n\n**Storm Runner CANNOT be discharged from quarantine or cleared for workout resumption.**\n\n#### Clinical Evaluation Breakdown\n- **Current Status:** Housed in Quarantine Stall Q-01 with training status **BLOCKED**.\n- **Readiness Score:** **68 / 100** (Passing standard: **>= 85 / 100**).\n- **Primary Deficits:** Persistent fetlock inflammation, elevated body temperature (39.1°C), and ungranulated wound tissue.\n\n#### Decision & Next Steps\nStorm Runner must remain in the quarantine isolation barn for at least another 7 days. Re-evaluation is scheduled for November 24th following the completion of his antimicrobial protocol."
        }
    ],
    "REACT_VET_04": [
        {
            "thought": "The veterinarian wants to analyze incident patterns and run a Kaplan-Meier survival curve stratified by severity. First, I call 'analyzeIncidentPatternsAndRecurringRisks'.",
            "tool_call": {"name": "analyzeIncidentPatternsAndRecurringRisks", "arguments": {}}
        },
        {
            "thought": "The incident analysis shows recurring distal limb injuries and recommends Kaplan-Meier survival analysis. I now call 'getEdaAnalytics' with analysisType='SURVIVAL' and stratifyBy='SEVERITY'.",
            "tool_call": {"name": "getEdaAnalytics", "arguments": {"analysisType": "SURVIVAL", "stratifyBy": "SEVERITY"}}
        },
        {
            "thought": "I synthesize the survival analysis findings and estimated recovery days.",
            "final_text": "### Incident Pattern Analysis & Kaplan-Meier Survival Analysis\n\n#### 1. Historical Incident Patterns (Last 6 Months)\n- **Total Incidents Analyzed:** 14 cases across the training club.\n- **Primary Risk Cluster:** Distal limb / fetlock and suspensory ligament strains account for **64%** of all recorded health interruptions.\n\n#### 2. Kaplan-Meier Survival Analysis (Stratified by Severity)\n| Injury Severity Stratum | Sample Size (n) | Median Recovery Time | 95% Confidence Interval |\n| :--- | :---: | :---: | :---: |\n| **MINOR** (Superficial abrasions, mild bruising) | 28 | **12 days** | [10, 15] days |\n| **MODERATE** (Strains, lacerations requiring sutures) | 14 | **28 days** | [24, 34] days |\n| **SEVERE** (Infected deep wounds, tenosynovitis) | 6 | **65 days** | [52, 78] days |\n\n- **Log-Rank Test:** p = 0.0012 (Statistically significant divergence in recovery trajectories).\n\n**Clinical Takeaway:** Horses suffering moderate-to-severe distal limb injuries require a structured 4-week minimum deloading protocol to prevent chronic re-injury."
        }
    ],
    "REACT_GROOM_01": [
        {
            "thought": "The groom notes one of their assigned horses is behaving unusually and leaving feed. First, I call 'getMyAssignedHorses' to see all assigned horses and their feeding status.",
            "tool_call": {"name": "getMyAssignedHorses", "arguments": {}}
        },
        {
            "thought": "Thunder in Stall A-101 is marked with 'PARTIAL_REFUSAL'. Now I call 'detectHorseBehavioralAnomaly' for Thunder.",
            "tool_call": {"name": "detectHorseBehavioralAnomaly", "arguments": {"horseName": "Thunder"}}
        },
        {
            "thought": "Thunder shows pawing and anorexia, indicating moderate colic risk. Next, I query 'getHorseDietaryAndCareAlerts' for Thunder.",
            "tool_call": {"name": "getHorseDietaryAndCareAlerts", "arguments": {"horseName": "Thunder"}}
        },
        {
            "thought": "Now I recommend the daily care priority order using 'recommendDailyCarePriorityOrder'.",
            "tool_call": {"name": "recommendDailyCarePriorityOrder", "arguments": {}}
        },
        {
            "thought": "I summarize the colic detection, dietary precautions, and prioritized shift duty schedule.",
            "final_text": "### Shift Duty Re-prioritization: Colic Anomaly Detected\n\n#### 1. Affected Horse Identification\n- **Horse:** **Thunder** (Stall A-101)\n- **Behavioral Anomaly:** Refused 40% of morning grain, restless pawing at the stall bedding.\n- **Clinical Risk:** **MODERATE COLIC RISK**.\n\n#### 2. Dietary & Care Alerts\n- Withhold all grain/concentrates immediately; provide soaked timothy hay only.\n- Check temperature, heart rate, and capillary refill time every 2 hours.\n\n#### 3. Prioritized Shift Duty Schedule\n1. **IMMEDIATE:** Assist Dr. Sarah Jenkins with urgent colic diagnostic evaluation for Thunder.\n2. **HIGH:** Inspect and replace left carpus protective bandage on Lightning (Stall A-102).\n3. **NORMAL:** Hand off Blaze to the track exercise rider for scheduled training.\n4. **ROUTINE:** Complete routine mucking and watering for Shadow and Comet.\n\nLog all updates at [Groom Tasks](/groom/tasks)."
        }
    ],
    "REACT_GROOM_02": [
        {
            "thought": "The groom asks where Lightning is stabled and what medical tasks or bandages are required for him today. Per Rule 7, I first call 'getHorseStallLocation' with horseName='Lightning'.",
            "tool_call": {"name": "getHorseStallLocation", "arguments": {"horseName": "Lightning"}}
        },
        {
            "thought": "Lightning is in Stall A-102 in Barn A. Now I check today's tasks via 'getTodayAggregatedTasks'.",
            "tool_call": {"name": "getTodayAggregatedTasks", "arguments": {}}
        },
        {
            "thought": "Today's tasks show a bandage change at 06:30 for Lightning. I also check 'getHorseDietaryAndCareAlerts' for care notes.",
            "tool_call": {"name": "getHorseDietaryAndCareAlerts", "arguments": {"horseName": "Lightning"}}
        },
        {
            "thought": "I synthesize the stall location, area, and bandage instructions per Rule 7.",
            "final_text": "### Stabling Location & Care Tasks for Lightning (Rule 7)\n\n#### 1. Stabling Location\n- **Horse Name:** Lightning\n- **Stall Code:** **Stall A-102**\n- **Area:** **Barn A**\n- **Assigned Caretaker:** John Doe\n\n#### 2. Pending Medical Tasks & Bandage Care\n- **Scheduled Task:** Check and replace left carpus protective pressure bandage (06:30 AM SOP).\n- **Care Guidelines:** Inspect the skin for pressure sores or swelling, apply prescribed topical antiseptic cream, and ensure clean, dry bedding in Stall A-102.\n\nView your full checklist on [Today's Tasks](/groom/tasks)."
        }
    ],
    "REACT_GROOM_03": [
        {
            "thought": "The groom asks for their daily task completion rate and any incidents reported during their shift. First, I call 'getGroomTaskCompletionRate'.",
            "tool_call": {"name": "getGroomTaskCompletionRate", "arguments": {}}
        },
        {
            "thought": "Next, I query 'getMyReportedIncidents' to inspect recently logged incidents.",
            "tool_call": {"name": "getMyReportedIncidents", "arguments": {}}
        },
        {
            "thought": "I synthesize both the shift progress and incident details.",
            "final_text": "### Groom Shift Progress & Incident Report Summary\n\n#### 1. Daily Task Completion Rate\n- **Date:** Today (2026-11-18)\n- **Assigned Tasks:** 8 tasks total\n- **Completed Tasks:** 5 completed\n- **Completion Rate:** **62.5%** (Status: In Progress)\n\n#### 2. Reported Incidents\n- **Incident ID:** INC-GROOM-041 (Reported on 2026-11-16)\n- **Subject Horse:** Thunder\n- **Incident Summary:** Horse refused 50% of morning concentrate ration and exhibited pawing behaviors.\n- **Status:** Handed off to veterinary department for evaluation.\n\nYou can submit further incident reports at [Report Incident](/groom/incidents/new)."
        }
    ],
    "REACT_GROOM_04": [
        {
            "thought": "The groom wants to check assigned horses for lethargy, and plot a temperature Bio-SPC chart for 14 days. First, I call 'getMyAssignedHorses'.",
            "tool_call": {"name": "getMyAssignedHorses", "arguments": {}}
        },
        {
            "thought": "I check 'detectHorseBehavioralAnomaly' for Thunder.",
            "tool_call": {"name": "detectHorseBehavioralAnomaly", "arguments": {"horseName": "Thunder"}}
        },
        {
            "thought": "Now I run 'getEdaAnalytics' with analysisType='BIO_SPC', horseName='Thunder', metric='TEMPERATURE', days=14.",
            "tool_call": {"name": "getEdaAnalytics", "arguments": {"analysisType": "BIO_SPC", "horseName": "Thunder", "metric": "TEMPERATURE", "days": 14}}
        },
        {
            "thought": "I synthesize the temperature Bio-SPC monitoring results for the groom.",
            "final_text": "### Assigned Horses Check & Temperature Bio-SPC Analysis\n\n#### 1. Behavioral Anomaly Review\n- **Subject:** Thunder (Stall A-101)\n- **Observations:** Exhibited mild lethargy and partial appetite refusal during morning turnout.\n\n#### 2. 14-Day Temperature Bio-SPC Control Chart\n- **Baseline Mean:** 38.05°C\n- **Upper Control Limit (UCL):** 38.55°C\n- **Lower Control Limit (LCL):** 37.55°C\n- **Process Status:** **IN CONTROL** (No Western Electric Rule violations).\n\n**Groom Action:** Although body temperature remains within safe statistical control limits, continue monitoring hydration and report any sudden fever spike immediately."
        }
    ],
    "REACT_MANAGER_01": [
        {
            "thought": "The CLUB_MANAGER wants to review pending admission applications and check regular and quarantine stall capacity over the next month. First, I call 'getAdmissionPipelineOverview' with status='PENDING'.",
            "tool_call": {"name": "getAdmissionPipelineOverview", "arguments": {"status": "PENDING"}}
        },
        {
            "thought": "There are 5 in-progress applications with Silver Mist in Stage 2. Now I call 'getStableOccupancyMetrics' to check current stall counts per Rule 12 & 13.",
            "tool_call": {"name": "getStableOccupancyMetrics", "arguments": {}}
        },
        {
            "thought": "Regular stalls are 47/60 occupied, and quarantine stalls are 2/4 occupied. Now I forecast bottlenecks via 'analyzeStableCapacityAndStallBottlenecks'.",
            "tool_call": {"name": "analyzeStableCapacityAndStallBottlenecks", "arguments": {}}
        },
        {
            "thought": "I combine admission pipeline data, stable capacity, and bottleneck forecasts while strictly following Rules 12 and 13.",
            "final_text": "### Admission Pipeline Funnel & Stall Capacity Bottleneck Forecast (Rules 12 & 13)\n\n#### 1. Admission Application Pipeline Overview\n- **Total Applications in System:** 18 applications\n- **Approved Applications:** 12 applications\n- **In-Progress Pipeline:** **5 pending applications** (Awaiting Groom review: 4, Awaiting Vet exam: 3, Awaiting Trainer review: 2, Awaiting Manager approval: 2).\n\n#### 2. Stable Stall Capacity & Occupancy (Rule 12)\n- **Regular Stalls:** 47 occupied out of 60 total stalls (78.3% occupancy, 13 available).\n- **Quarantine Stalls:** 2 occupied out of 4 total stalls (Stall Q-01 occupied by Storm Runner, Stall Q-03 reserved for Silver Mist).\n\n#### 3. 30-Day Capacity Forecast & Bottleneck Warning\n- **Projected Occupancy:** Projected to reach **92.5%** within 30 days.\n- **Bottleneck Identified:** The quarantine facility faces high congestion risk. With 2 active admissions and incoming candidates, quarantine could reach 100% capacity within 2 weeks.\n- **Management Action:** Expedite clinical clearance evaluations for recovering horses and pace admission intakes. Manage stabling at [Stable Management](/manager/stable)."
        }
    ],
    "REACT_MANAGER_02": [
        {
            "thought": "The manager wants to check system staff breakdown, evaluate groom distribution, and recommend workload rebalancing. First, I call 'getSystemStaffOverview' with role='GROOM'.",
            "tool_call": {"name": "getSystemStaffOverview", "arguments": {"role": "GROOM"}}
        },
        {
            "thought": "There are 15 grooms distributed across Barn A (8), Barn B (5), and Barn C (2). Now I inspect 'getTrainerAreaDistribution' to check the horse counts in each barn.",
            "tool_call": {"name": "getTrainerAreaDistribution", "arguments": {}}
        },
        {
            "thought": "Barn B has 24 horses managed by 5 grooms, while Barn C has only 5 horses managed by 2 grooms. Now I call 'recommendGroomWorkloadRebalancing'.",
            "tool_call": {"name": "recommendGroomWorkloadRebalancing", "arguments": {}}
        },
        {
            "thought": "I synthesize the staff overview and groom workload rebalancing plan per Rule 14.",
            "final_text": "### System Staff Breakdown & Groom Workload Rebalancing (Rule 14)\n\n#### 1. System Staff Overview\n- **Total Active Grooms:** 15 grooms across 3 stable barns.\n- **Barn A:** 8 grooms managing 18 horses (Ratio: 2.25 horses/groom).\n- **Barn B:** 5 grooms managing 24 horses (Ratio: 4.80 horses/groom - **High Overload**).\n- **Barn C:** 2 grooms managing 5 horses (Ratio: 2.50 horses/groom - **Low Utilization**).\n\n#### 2. Workload Rebalancing Recommendation\n- **Imbalance Diagnosis:** Barn B is carrying nearly double the horse-to-groom workload of Barn C.\n- **Proposed Rebalancing:** Transfer **1 groom from Barn C to Barn B** during the morning care shift.\n- **Target Outcome:** Rebalances Barn B to 4.0 horses/groom and Barn C to 5.0 horses/groom, ensuring consistent care standards across all barns.\n\nReview staff profiles on [Staff Management](/manager/staff)."
        }
    ],
    "REACT_MANAGER_03": [
        {
            "thought": "The manager asks for a club health overview, the top 3 racing performers, and their stall assignments. Per Rule 12, I first call 'getClubHealthOverview' to get the club-wide horse count.",
            "tool_call": {"name": "getClubHealthOverview", "arguments": {}}
        },
        {
            "thought": "Total club horses is 68. Next, per Rule 15, I rank horses by average rating via 'getHorseSpecialtyAnalysis' with sortBy='rating'.",
            "tool_call": {"name": "getHorseSpecialtyAnalysis", "arguments": {"sortBy": "rating"}}
        },
        {
            "thought": "The top 3 performers are Blaze (95.0), Thunder (92.5), and Shadow (89.0). Now I check their stall locations in 'getClubHorsesDirectory'.",
            "tool_call": {"name": "getClubHorsesDirectory", "arguments": {}}
        },
        {
            "thought": "I combine club health metrics, top 3 performer rankings, and stall assignments.",
            "final_text": "### Club Health Overview & Top Performer Stall Placements\n\n#### 1. Club-Wide Herd Health Overview (Rule 12)\n- **Total Club Horses:** **68 horses**\n- **Cleared for Training:** 62 horses (91.2%)\n- **Medically Restricted:** 4 horses\n- **Under Quarantine:** 2 horses\n- **Overall Club Health Index:** **94.8%**\n\n#### 2. Top 3 Racing Performers (Rule 15)\n| Rank | Horse Name | Average Rating | Best Specialty | Current Stall | Responsible Trainer |\n| :---: | :--- | :---: | :--- | :---: | :--- |\n| **1** | **Blaze** | **95.0** | Sprint 1200m Dirt | Stall A-103 | Alex Morgan |\n| **2** | **Thunder** | **92.5** | Sprint 1200m Dirt | Stall A-101 | Alex Morgan |\n| **3** | **Shadow** | **89.0** | Miler 1600m Turf | Stall A-104 | Alex Morgan |\n\nAll three top performers are stabled in Barn A under Trainer Alex Morgan. For full roster management, see [Horse Directory](/manager/horses)."
        }
    ],
    "REACT_MANAGER_04": [
        {
            "thought": "The manager wants to audit recent security logs for failed access attempts and cross-check with active system staff. First, I call 'getRecentAuditLogs' with limit=50.",
            "tool_call": {"name": "getRecentAuditLogs", "arguments": {"limit": 50}}
        },
        {
            "thought": "The audit logs show repeated failed login attempts from IP 192.168.1.99 and an unauthorized privilege escalation attempt. Next, I query 'getSystemStaffOverview'.",
            "tool_call": {"name": "getSystemStaffOverview", "arguments": {}}
        },
        {
            "thought": "I synthesize the audit trail findings and cross-check with verified system personnel.",
            "final_text": "### Security Audit Log Analysis & Staff Cross-Check\n\n#### 1. Security Incidents Detected in Audit Log\n- **Total Recent Audit Records:** 50 records reviewed.\n- **Critical Incident 1 (Event 1001):** Repeated failed login attempts detected originating from IP `192.168.1.99` targeting administrative endpoints.\n- **Critical Incident 2 (Event 1003):** Unauthorized privilege escalation attempt targeting the CLUB_MANAGER role; immediately blocked by security gateway.\n\n#### 2. Staff Roster Cross-Check\n- **Total Verified Staff:** 24 active accounts (3 Head Trainers, 2 Veterinarians, 15 Grooms, 4 Administrative).\n- **Findings:** The unauthorized requests did NOT originate from any registered staff member credentials.\n\n#### Security Recommendations\n1. Blacklist offending IP `192.168.1.99` on the network firewall.\n2. Enforce two-factor authentication (2FA) for all administrative and manager accounts.\n\nReview complete logs on [Audit Trail](/manager/audit-log)."
        }
    ]
}

# ==============================================================================
# 6. SYSTEM PROMPT BUILDER (Matching Backend SystemPromptBuilder.java)
# ==============================================================================

def build_system_prompt(role: str) -> str:
    """Builds role-scoped RTMS ReAct system prompt."""
    return f"""You are the RTMS (Racehorse Training Management System) Intelligent AI Assistant, a professional assistant for a racehorse training club.
Current Assigned Role: {role}.

CORE LANGUAGE POLICY:
- Formulate and present all responses in professional English.

MANDATORY RULES:
1. NEVER DISPLAY INTERNAL NUMERIC DATABASE IDS IN RESPONSES (e.g. do not write "Horse #1", write "Thunder").
2. NEVER DISPLAY INTERNAL TOOL CODE NAMES IN CHAT (e.g. do not write "called getTrainerHorses", write "According to the trainer schedule...").
3. DATA CONFLICT PREVENTION: Every horse under medical quarantine MUST NOT BE ALLOWED TO TRAIN OR RACE.
4. 100% READ-ONLY / RAG ADVISORY OPERATION within role permissions.
5. GROUNDEDNESS: NEVER guess entity facts; call appropriate tools.
6. AUTOMATIC PARAMETER DISCOVERY: Automatically discover entities via list tools before calling specific detail tools.
7. GROOM REASONING CHAIN: Combine stall code and area when answering stall location queries.
8. VETERINARIAN DRUG INTERACTIONS: Check active prescriptions and warn against NSAID contraindications.
9. CLEAR MARKDOWN FORMATTING: Use tables and structured bullet points.
10. ACCURATE NAVIGATION LINKS: Use valid RTMS frontend routes (e.g., [Horse Profile](/owner/horses)).
15. SPECIALTY & RATING RANKINGS: Rank horses by average rating or specialty count when requested.
"""

# ==============================================================================
# 7. DAG TRAJECTORY & RUBRIC SCORING ENGINE
# ==============================================================================

def validate_dag(actual_tools: List[str], expected_dag: Dict[str, Any]) -> Tuple[float, List[str]]:
    """
    Validates whether the sequence of executed tools satisfies topological ordering of the DAG.
    Returns (score, feedback_notes).
    """
    notes = []
    required_nodes = set(expected_dag.get("nodes", []))
    edges = expected_dag.get("edges", [])

    if not required_nodes:
        return 1.0, ["No DAG constraints specified."]

    canonical_actual = [resolve_canonical_tool_name(t) for t in actual_tools]
    actual_set = set(canonical_actual)
    missing_nodes = required_nodes - actual_set

    # 1. Node coverage
    coverage_score = (len(required_nodes) - len(missing_nodes)) / len(required_nodes)
    if missing_nodes:
        notes.append(f"Missing required tool(s): {list(missing_nodes)}")

    # 2. Dependency Edge Precedence
    edge_violations = 0
    total_edges = len(edges)
    if total_edges > 0:
        for u, v in edges:
            if u in canonical_actual and v in canonical_actual:
                u_idx = canonical_actual.index(u)
                v_idx = canonical_actual.index(v)
                if u_idx > v_idx:
                    edge_violations += 1
                    notes.append(f"Order violation: '{u}' should precede '{v}', but appeared at index {u_idx} vs {v_idx}.")
            elif v in canonical_actual and u not in canonical_actual:
                edge_violations += 1
                notes.append(f"Dependency violation: '{v}' was executed without preceding '{u}'.")

        order_score = max(0.0, (total_edges - edge_violations) / total_edges)
    else:
        order_score = 1.0

    # 3. Trajectory score composite
    score = round(0.6 * coverage_score + 0.4 * order_score, 4)
    if score == 1.0:
        notes.append("Topological DAG ordering completely satisfied.")
    return score, notes

def evaluate_parameters(actual_calls: List[Dict[str, Any]], param_rules: List[Dict[str, Any]]) -> Tuple[float, List[str]]:
    """Evaluates parameter propagation accuracy from observations to tool calls."""
    if not param_rules:
        return 1.0, ["No specific parameter extraction constraints."]

    notes = []
    passed = 0
    for rule in param_rules:
        target_tool = resolve_canonical_tool_name(rule["tool"])
        target_param = rule["param"]
        expected_values = rule["expected"]

        # Find matching call
        matched_call = None
        for call in actual_calls:
            if resolve_canonical_tool_name(call["name"]) == target_tool:
                matched_call = call
                break

        if not matched_call:
            notes.append(f"Parameter check failed: tool '{target_tool}' was not called.")
            continue

        call_args = matched_call.get("arguments", {})
        actual_val = call_args.get(target_param)

        # Dual resolution for horse identification: horseName vs horseId
        if actual_val is None:
            if target_param == "horseName":
                actual_val = call_args.get("horseId")
            elif target_param == "horseId":
                actual_val = call_args.get("horseName")

        val_matches = False
        for exp in expected_values:
            if exp is None and actual_val is None:
                val_matches = True
                break
            if actual_val is not None and exp is not None:
                if isinstance(actual_val, str) and isinstance(exp, str):
                    if exp.lower() in actual_val.lower():
                        val_matches = True
                        break
                elif actual_val == exp:
                    val_matches = True
                    break
                elif str(actual_val).strip().lower() == str(exp).strip().lower():
                    val_matches = True
                    break

        if val_matches:
            passed += 1
            notes.append(f"Param '{target_param}' on '{target_tool}' successfully propagated as '{actual_val}'.")
        else:
            notes.append(f"Param '{target_param}' on '{target_tool}' had value '{actual_val}', expected one of {expected_values}.")

    score = round(passed / len(param_rules), 4)
    return score, notes

def evaluate_efficiency(optimal_steps: int, actual_steps: int) -> float:
    """Computes step efficiency: min(1.0, N_optimal / N_actual)."""
    if actual_steps <= 0:
        return 0.0
    return round(min(1.0, optimal_steps / actual_steps), 4)

def evaluate_system_rules(
    scenario: Dict[str, Any],
    final_text: str,
    reasoning_text: str,
    actual_calls: List[Dict[str, Any]]
) -> Tuple[float, Dict[str, bool], List[str]]:
    """Evaluates compliance with RTMS business rules."""
    rules_to_check = set(scenario.get("system_rules", ["RULE_1_NO_NUMERIC_ID"]))
    # Foundational system-wide constraints from SystemPromptBuilder.java
    rules_to_check.add("RULE_1_NO_NUMERIC_ID")
    rules_to_check.add("RULE_2_NO_INTERNAL_TOOL_NAMES")
    if "[" in final_text and "](" in final_text:
        rules_to_check.add("RULE_10_VALID_NAVIGATION_LINKS")
    rule_results: Dict[str, bool] = {}
    notes = []

    combined_text = final_text

    # Rule 1: No internal numeric database IDs (e.g., Horse #1, Horse ID 5)
    if "RULE_1_NO_NUMERIC_ID" in rules_to_check:
        id_regex = r"\b(Horse\s*#\s*\d+|Horse\s*ID\s*[:=]?\s*\d+|Stall\s*#\s*\d+)\b"
        matches = re.findall(id_regex, combined_text, flags=re.IGNORECASE)
        passed = len(matches) == 0
        rule_results["RULE_1_NO_NUMERIC_ID"] = passed
        if not passed:
            notes.append(f"Rule 1 violation: found numeric ID pattern {matches[:3]}")
        else:
            notes.append("Rule 1 passed: no numeric database IDs leaked.")

    # Rule 2: Never display internal function/tool names in final response
    if "RULE_2_NO_INTERNAL_TOOL_NAMES" in rules_to_check:
        leaked_tools = [
            t for t in ALL_CANONICAL_TOOLS
            if re.search(r"\b" + re.escape(t) + r"\b", combined_text)
        ]
        passed = len(leaked_tools) == 0
        rule_results["RULE_2_NO_INTERNAL_TOOL_NAMES"] = passed
        if passed:
            notes.append("Rule 2 passed: no internal tool names leaked in final response.")
        else:
            notes.append(f"Rule 2 violation: internal tool names leaked in response: {leaked_tools[:3]}")

    # Rule 3: Medical Quarantine Gate (clearance must be refused for quarantine horses)
    if "RULE_3_QUARANTINE_TRAINING_BLOCKED" in rules_to_check:
        refusal_keywords = ["cannot", "not allowed", "denied", "declined", "blocked", "restricted", "refused", "quarantine"]
        passed = any(kw in combined_text.lower() for kw in refusal_keywords)
        rule_results["RULE_3_QUARANTINE_TRAINING_BLOCKED"] = passed
        if passed:
            notes.append("Rule 3 passed: race/training clearance strictly denied for quarantined horse.")
        else:
            notes.append("Rule 3 violation: failed to explicitly refuse clearance for quarantined horse.")

    # Rule 6: Automatic Entity Discovery
    if "RULE_6_ENTITY_DISCOVERY" in rules_to_check:
        tool_names = [resolve_canonical_tool_name(c["name"]) for c in actual_calls]
        discovery_tools = {"getMyHorses", "getTrainerHorses", "getMyAssignedHorses", "getUrgentCareAndIncidentAlerts", "getTrainerGroups"}
        passed = any(t in discovery_tools for t in tool_names)
        rule_results["RULE_6_ENTITY_DISCOVERY"] = passed
        if passed:
            notes.append("Rule 6 passed: entity discovery tool preceded detail lookups.")
        else:
            notes.append("Rule 6 violation: no discovery tool called.")

    # Rule 7: Groom Stall Location & Area
    if "RULE_7_STALL_LOCATION_AND_AREA" in rules_to_check:
        passed = "barn" in combined_text.lower() and "stall" in combined_text.lower()
        rule_results["RULE_7_STALL_LOCATION_AND_AREA"] = passed
        if passed:
            notes.append("Rule 7 passed: combined stall code and area reported.")
        else:
            notes.append("Rule 7 violation: missing stall code or barn area.")

    # Rule 8: Drug Contraindication Warning
    if "RULE_8_DRUG_CONTRAINDICATION" in rules_to_check:
        passed = ("nsaid" in combined_text.lower() or "contraindicat" in combined_text.lower() or "phenylbutazone" in combined_text.lower())
        rule_results["RULE_8_DRUG_CONTRAINDICATION"] = passed
        if passed:
            notes.append("Rule 8 passed: drug contraindication / NSAID conflict flagged.")
        else:
            notes.append("Rule 8 violation: failed to warn against NSAID drug interaction.")

    # Rule 10: Accurate Navigation Links (avoid 404 routes)
    if "RULE_10_VALID_NAVIGATION_LINKS" in rules_to_check:
        markdown_links = re.findall(r"\[.*?\]\((/[^)]+)\)", combined_text)
        invalid_links = []
        for link in markdown_links:
            clean_route = link.split("?")[0].rstrip("/")
            if clean_route in VALID_SYSTEM_ROUTES or any(clean_route.startswith(r) for r in VALID_SYSTEM_ROUTES):
                continue
            invalid_links.append(link)
        passed = len(invalid_links) == 0
        rule_results["RULE_10_VALID_NAVIGATION_LINKS"] = passed
        if passed:
            notes.append("Rule 10 passed: all navigation links match valid system routes.")
        else:
            notes.append(f"Rule 10 violation: invalid routes found: {invalid_links[:3]}")

    # Rule 11: Groom Contact Info
    if "RULE_11_GROOM_CONTACT_INFO" in rules_to_check:
        passed = ("phone" in combined_text.lower() or "email" in combined_text.lower() or "@" in combined_text)
        rule_results["RULE_11_GROOM_CONTACT_INFO"] = passed
        if passed:
            notes.append("Rule 11 passed: groom contact details provided.")
        else:
            notes.append("Rule 11 violation: groom contact info missing.")

    # Rule 12: Distinguish Horse Count from Stall Count
    if "RULE_12_DISTINGUISH_HORSE_AND_STALL" in rules_to_check:
        passed = "stall" in combined_text.lower() and "horse" in combined_text.lower()
        rule_results["RULE_12_DISTINGUISH_HORSE_AND_STALL"] = passed
        if passed:
            notes.append("Rule 12 passed: horse and stall metrics distinguished.")
        else:
            notes.append("Rule 12 violation: horse and stall metrics conflated or absent.")

    # Rule 13: Admission Statistics
    if "RULE_13_ADMISSION_STATISTICS" in rules_to_check:
        passed = "application" in combined_text.lower() or "pipeline" in combined_text.lower()
        rule_results["RULE_13_ADMISSION_STATISTICS"] = passed
        if passed:
            notes.append("Rule 13 passed: admission application pipeline statistics reported.")
        else:
            notes.append("Rule 13 violation: admission pipeline details missing.")

    # Rule 14: System Staff Breakdown
    if "RULE_14_STAFF_STATISTICS" in rules_to_check:
        passed = "groom" in combined_text.lower() and "barn" in combined_text.lower()
        rule_results["RULE_14_STAFF_STATISTICS"] = passed
        if passed:
            notes.append("Rule 14 passed: staff distribution breakdown provided.")
        else:
            notes.append("Rule 14 violation: staff distribution details missing.")

    # Rule 15: Horse Ranking
    if "RULE_15_HORSE_RANKING" in rules_to_check:
        passed = "rank" in combined_text.lower() or "rating" in combined_text.lower()
        rule_results["RULE_15_HORSE_RANKING"] = passed
        if passed:
            notes.append("Rule 15 passed: horse ranking table with ratings provided.")
        else:
            notes.append("Rule 15 violation: horse ranking table missing.")

    total_checked = len(rule_results)
    if total_checked == 0:
        return 1.0, {"DEFAULT": True}, ["No specific rules checked."]

    passed_count = sum(1 for v in rule_results.values() if v)
    score = round(passed_count / total_checked, 4)
    return score, rule_results, notes

# ==============================================================================
# 8. OPENAI API TRANSPORT CLIENTS
# ==============================================================================

def call_openai_responses(
    base_url: str,
    api_key: str,
    model: str,
    input_items: List[Dict[str, Any]],
    tools: Optional[List[Dict[str, Any]]] = None,
    instructions: Optional[str] = None,
    reasoning_effort: str = "medium",
    timeout: int = 90
) -> Dict[str, Any]:
    """Calls OpenAI /v1/responses endpoint (with native CoT reasoning)."""
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

    is_reasoning_model = any(k in model.lower() for k in ["luna", "o1", "o3", "gpt-6"])
    if is_reasoning_model:
        payload["reasoning"] = {"effort": reasoning_effort, "summary": "auto"}
    else:
        payload["temperature"] = 0.2

    data = json.dumps(payload).encode("utf-8")
    req = Request(url, data=data, headers=headers, method="POST")
    try:
        with urlopen(req, timeout=timeout) as response:
            return json.loads(response.read().decode("utf-8"))
    except HTTPError as e:
        err_msg = e.read().decode("utf-8", errors="ignore")
        raise RuntimeError(f"HTTP {e.code}: {err_msg}")
    except URLError as e:
        raise RuntimeError(f"Connection failed: {e.reason}")

def call_openai_chat_completions(
    base_url: str,
    api_key: str,
    model: str,
    messages: List[Dict[str, Any]],
    tools: Optional[List[Dict[str, Any]]] = None,
    timeout: int = 90
) -> Dict[str, Any]:
    """Fallback client for standard OpenAI /v1/chat/completions."""
    url = f"{base_url.rstrip('/')}/chat/completions"
    headers = {
        "Content-Type": "application/json",
        "Authorization": f"Bearer {api_key}"
    }

    payload: Dict[str, Any] = {
        "model": model,
        "messages": messages,
    }
    is_reasoning_model = any(k in model.lower() for k in ["luna", "o1", "o3", "gpt-6"])
    if tools:
        payload["tools"] = tools
        payload["tool_choice"] = "auto"
        if is_reasoning_model:
            payload["reasoning_effort"] = "none"  # Prohibited with tools on Luna
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
            return json.loads(response.read().decode("utf-8"))
    except HTTPError as e:
        err_msg = e.read().decode("utf-8", errors="ignore")
        raise RuntimeError(f"HTTP {e.code}: {err_msg}")
    except URLError as e:
        raise RuntimeError(f"Connection failed: {e.reason}")

# ==============================================================================
# 9. REACT LOOP EXECUTION ENGINE
# ==============================================================================

def execute_react_scenario(
    scenario: Dict[str, Any],
    mock_world: MockWorldState,
    is_dry_run: bool = True,
    base_url: str = "",
    api_key: str = "",
    model: str = "gpt-6-luna",
    transport: str = "auto"
) -> Dict[str, Any]:
    """
    Executes the multi-turn ReAct loop for a single benchmark scenario.
    Supports either the deterministic oracle simulator or the live LLM API.
    """
    scenario_id = scenario["id"]
    role = scenario["role"]
    prompt = scenario["prompt"]
    instructions = build_system_prompt(role)

    start_time = time.time()
    iterations = 0
    actual_tool_calls: List[Dict[str, Any]] = []
    executed_tool_names: List[str] = []
    reasoning_traces: List[str] = []
    final_answer: str = ""
    halt_reason: str = "COMPLETED"
    total_tokens = 0

    # Tracking for infinite loops
    seen_calls: Set[str] = set()

    if is_dry_run:
        # Deterministic Oracle Simulation
        oracle_steps = ORACLE_SIMULATION_STEPS.get(scenario_id, [])
        for step in oracle_steps:
            iterations += 1
            thought = step.get("thought", "")
            if thought:
                reasoning_traces.append(thought)

            tool_spec = step.get("tool_call")
            if tool_spec:
                t_name = tool_spec["name"]
                t_args = tool_spec.get("arguments", {})

                # Check role permission (Section 4 spec)
                if not is_tool_allowed_for_role(t_name, role):
                    halt_reason = "HALT_PERMISSION_VIOLATION"
                    obs = mock_world.dispatch(t_name, t_args, role)
                    actual_tool_calls.append({
                        "iteration": iterations,
                        "name": t_name,
                        "arguments": t_args,
                        "observation": obs
                    })
                    executed_tool_names.append(t_name)
                    break

                # Call hash for infinite loop detection
                call_sig = f"{t_name}:{json.dumps(t_args, sort_keys=True)}"
                if call_sig in seen_calls:
                    halt_reason = "HALT_INFINITE_LOOP"
                    break
                seen_calls.add(call_sig)

                # Mock observation
                obs = mock_world.dispatch(t_name, t_args, role)
                actual_tool_calls.append({
                    "iteration": iterations,
                    "name": t_name,
                    "arguments": t_args,
                    "observation": obs
                })
                executed_tool_names.append(t_name)

            final_text = step.get("final_text")
            if final_text:
                final_answer = final_text
                break
        total_tokens = 2450 + len(actual_tool_calls) * 320

    else:
        # Live LLM execution via /v1/responses or /v1/chat/completions fallback
        use_responses_api = (transport != "chat")

        if use_responses_api:
            tools = get_tools_for_responses_api(role)
            input_items: List[Dict[str, Any]] = [
                {
                    "type": "message",
                    "role": "user",
                    "content": [{"type": "input_text", "text": prompt}]
                }
            ]

            fallback_to_chat = False
            while iterations < MAX_REACT_ITERATIONS:
                iterations += 1
                try:
                    resp = call_openai_responses(
                        base_url=base_url,
                        api_key=api_key,
                        model=model,
                        input_items=input_items,
                        tools=tools,
                        instructions=instructions
                    )
                except Exception as e:
                    if iterations == 1 and transport == "auto":
                        print(f"      ℹ️ /v1/responses unavailable ({e}), falling back to /v1/chat/completions...")
                        fallback_to_chat = True
                        break
                    raise

                usage = resp.get("usage", {})
                total_tokens += usage.get("total_tokens", 0)

                output_items = resp.get("output", [])
                function_calls = []

                for item in output_items:
                    # Collect reasoning thoughts
                    if item.get("type") == "reasoning":
                        for summary in item.get("summary", []):
                            if summary.get("type") == "summary_text":
                                reasoning_traces.append(summary.get("text", ""))

                    # Collect function calls
                    elif item.get("type") == "function_call":
                        try:
                            args = json.loads(item.get("arguments", "{}"))
                        except Exception:
                            args = {}
                        function_calls.append({
                            "call_id": item.get("call_id", ""),
                            "name": item.get("name", ""),
                            "arguments": args,
                            "raw": item
                        })

                    # Collect assistant messages
                    elif item.get("type") == "message" and item.get("role") == "assistant":
                        for block in item.get("content", []):
                            if block.get("type") == "output_text":
                                final_answer += block.get("text", "")

                # If no function calls and we have answer text, terminate
                if not function_calls:
                    if final_answer.strip():
                        break

                # Append raw assistant output items to conversation input
                for item in output_items:
                    input_items.append(item)

                # Execute function calls on MockWorldState
                for fcall in function_calls:
                    t_name = fcall["name"]
                    t_args = fcall["arguments"]

                    if not is_tool_allowed_for_role(t_name, role):
                        halt_reason = "HALT_PERMISSION_VIOLATION"
                        obs = mock_world.dispatch(t_name, t_args, role)
                        actual_tool_calls.append({
                            "iteration": iterations,
                            "name": t_name,
                            "arguments": t_args,
                            "observation": obs
                        })
                        executed_tool_names.append(t_name)
                        break

                    call_sig = f"{t_name}:{json.dumps(t_args, sort_keys=True)}"
                    if call_sig in seen_calls:
                        halt_reason = "HALT_INFINITE_LOOP"
                        break
                    seen_calls.add(call_sig)

                    obs = mock_world.dispatch(t_name, t_args, role)
                    actual_tool_calls.append({
                        "iteration": iterations,
                        "name": t_name,
                        "arguments": t_args,
                        "observation": obs
                    })
                    executed_tool_names.append(t_name)

                    # Append function_call_output
                    input_items.append({
                        "type": "function_call_output",
                        "call_id": fcall["call_id"],
                        "output": json.dumps(obs)
                    })

                if halt_reason in ("HALT_INFINITE_LOOP", "HALT_PERMISSION_VIOLATION"):
                    break

            # Fallback Synthesis Turn if MAX_REACT_ITERATIONS elapsed without final answer
            if not fallback_to_chat and not final_answer.strip() and actual_tool_calls and halt_reason == "COMPLETED":
                try:
                    synthesis_resp = call_openai_responses(
                        base_url=base_url,
                        api_key=api_key,
                        model=model,
                        input_items=input_items,
                        tools=None,  # Disallow further tools
                        instructions=instructions + "\n\nSynthesize all tool observations into an authoritative, structured English Markdown report."
                    )
                    for item in synthesis_resp.get("output", []):
                        if item.get("type") == "message" and item.get("role") == "assistant":
                            for block in item.get("content", []):
                                if block.get("type") == "output_text":
                                    final_answer += block.get("text", "")
                except Exception as e:
                    print(f"      ⚠️ Responses synthesis call failed: {e}")

            if fallback_to_chat:
                use_responses_api = False
                iterations = 0
                actual_tool_calls.clear()
                executed_tool_names.clear()
                seen_calls.clear()

        if not use_responses_api:
            # Fallback chat/completions loop
            tools = get_tools_for_chat_completions(role)
            messages: List[Dict[str, Any]] = [
                {"role": "system", "content": instructions},
                {"role": "user", "content": prompt}
            ]

            while iterations < MAX_REACT_ITERATIONS:
                iterations += 1
                resp = call_openai_chat_completions(
                    base_url=base_url,
                    api_key=api_key,
                    model=model,
                    messages=messages,
                    tools=tools
                )
                choices = resp.get("choices", [])
                if not choices:
                    break

                msg = choices[0].get("message", {})
                messages.append(msg)
                content = msg.get("content") or ""
                tool_calls = msg.get("tool_calls") or []

                if not tool_calls:
                    final_answer = content
                    break

                for tc in tool_calls:
                    func = tc.get("function", {})
                    t_name = func.get("name", "")
                    try:
                        t_args = json.loads(func.get("arguments", "{}"))
                    except Exception:
                        t_args = {}

                    if not is_tool_allowed_for_role(t_name, role):
                        halt_reason = "HALT_PERMISSION_VIOLATION"
                        obs = mock_world.dispatch(t_name, t_args, role)
                        actual_tool_calls.append({
                            "iteration": iterations,
                            "name": t_name,
                            "arguments": t_args,
                            "observation": obs
                        })
                        executed_tool_names.append(t_name)
                        break

                    call_sig = f"{t_name}:{json.dumps(t_args, sort_keys=True)}"
                    if call_sig in seen_calls:
                        halt_reason = "HALT_INFINITE_LOOP"
                        break
                    seen_calls.add(call_sig)

                    obs = mock_world.dispatch(t_name, t_args, role)
                    actual_tool_calls.append({
                        "iteration": iterations,
                        "name": t_name,
                        "arguments": t_args,
                        "observation": obs
                    })
                    executed_tool_names.append(t_name)

                    messages.append({
                        "role": "tool",
                        "tool_call_id": tc.get("id", ""),
                        "name": t_name,
                        "content": json.dumps(obs)
                    })

                if halt_reason in ("HALT_INFINITE_LOOP", "HALT_PERMISSION_VIOLATION"):
                    break

            if not final_answer.strip() and actual_tool_calls and halt_reason == "COMPLETED":
                messages.append({
                    "role": "user",
                    "content": "Synthesize a structured Markdown answer in English based on the retrieved data."
                })
                try:
                    synth_resp = call_openai_chat_completions(
                        base_url=base_url,
                        api_key=api_key,
                        model=model,
                        messages=messages,
                        tools=None
                    )
                    synth_choices = synth_resp.get("choices", [])
                    if synth_choices:
                        final_answer = synth_choices[0].get("message", {}).get("content", "")
                except Exception as e:
                    print(f"      ⚠️ Chat completions synthesis call failed: {e}")

    # Fallback automated summary matching AgentOrchestratorService.java line 295
    if not final_answer.strip() and actual_tool_calls:
        summary_blocks = ["### Automated Tool Data Summary\n"]
        for tc in actual_tool_calls:
            summary_blocks.append(f"- **Executed Tool:** `{tc['name']}`  \n  **Arguments:** `{json.dumps(tc['arguments'])}`  \n  **Observation:** {json.dumps(tc['observation'])}\n")
        final_answer = "\n".join(summary_blocks)

    latency_ms = int((time.time() - start_time) * 1000)

    # ----------------- Scoring & Evaluation -----------------
    s_traj, traj_notes = validate_dag(executed_tool_names, scenario["expected_dag"])
    s_param, param_notes = evaluate_parameters(actual_tool_calls, scenario.get("param_rules", []))
    s_eff = evaluate_efficiency(scenario["optimal_steps"], len(executed_tool_names))
    s_rule, rule_matrix, rule_notes = evaluate_system_rules(
        scenario,
        final_answer,
        "\n".join(reasoning_traces),
        actual_tool_calls
    )

    composite_score = round(
        WEIGHT_TRAJECTORY * s_traj +
        WEIGHT_PARAMETER * s_param +
        WEIGHT_EFFICIENCY * s_eff +
        WEIGHT_RULE * s_rule,
        4
    )

    return {
        "scenario_id": scenario_id,
        "role": role,
        "title": scenario["title"],
        "prompt": prompt,
        "optimal_steps": scenario["optimal_steps"],
        "actual_steps": len(executed_tool_names),
        "iterations": iterations,
        "halt_reason": halt_reason,
        "latency_ms": latency_ms,
        "tokens": total_tokens,
        "executed_trajectory": executed_tool_names,
        "actual_tool_calls": actual_tool_calls,
        "reasoning_traces": reasoning_traces,
        "final_answer": final_answer,
        "rubric_scores": {
            "trajectory_score": s_traj,
            "parameter_score": s_param,
            "efficiency_score": s_eff,
            "rule_score": s_rule,
            "composite_benchmark_score": composite_score
        },
        "rule_compliance": rule_matrix,
        "evaluation_notes": {
            "trajectory": traj_notes,
            "parameter": param_notes,
            "rules": rule_notes
        }
    }

# ==============================================================================
# 10. REPORT GENERATION (JSON & MARKDOWN)
# ==============================================================================

def generate_markdown_report(results: List[Dict[str, Any]], output_path: str, model: str) -> None:
    """Generates the comprehensive Markdown evaluation report."""
    total_scenarios = len(results)
    avg_score = round(sum(r["rubric_scores"]["composite_benchmark_score"] for r in results) / total_scenarios, 4) if total_scenarios else 0.0
    avg_traj = round(sum(r["rubric_scores"]["trajectory_score"] for r in results) / total_scenarios, 4) if total_scenarios else 0.0
    avg_param = round(sum(r["rubric_scores"]["parameter_score"] for r in results) / total_scenarios, 4) if total_scenarios else 0.0
    avg_eff = round(sum(r["rubric_scores"]["efficiency_score"] for r in results) / total_scenarios, 4) if total_scenarios else 0.0
    avg_rule = round(sum(r["rubric_scores"]["rule_score"] for r in results) / total_scenarios, 4) if total_scenarios else 0.0
    total_latency_ms = sum(r["latency_ms"] for r in results)
    total_tokens = sum(r["tokens"] for r in results)

    # Role breakdown
    roles = ["HORSE_OWNER", "HEAD_TRAINER", "VETERINARIAN", "GROOM", "CLUB_MANAGER"]
    role_stats: Dict[str, Dict[str, Any]] = {}
    for r in roles:
        role_items = [res for res in results if res["role"] == r]
        cnt = len(role_items)
        if cnt > 0:
            r_score = round(sum(x["rubric_scores"]["composite_benchmark_score"] for x in role_items) / cnt, 4)
            role_stats[r] = {"count": cnt, "score": r_score}
        else:
            role_stats[r] = {"count": 0, "score": 0.0}

    lines = [
        "# RTMS ReAct Agent Loop Benchmark Report",
        "",
        f"> **Model:** `{model}`  ",
        f"> **Evaluation Timestamp:** {datetime.now().strftime('%Y-%m-%d %H:%M:%S')}  ",
        f"> **Total Evaluated Scenarios:** {total_scenarios} / {len(BENCHMARK_SCENARIOS)}  ",
        f"> **Overall Benchmark Score:** **{avg_score * 100:.2f}%**",
        "",
        "---",
        "",
        "## 1. Executive Summary & Aggregate Rubric Scores",
        "",
        "The RTMS ReAct Agent Loop Benchmark evaluates multi-turn autonomous tool chaining, entity parameter propagation, topological DAG compliance, and business rule enforcement across all 5 operational roles.",
        "",
        "| Metric | Weight | Average Score | Status |",
        "| :--- | :---: | :---: | :---: |",
        f"| **Trajectory Score ($S_{{\\text{{Traj}}}}$)** | 35% | **{avg_traj * 100:.1f}%** | {'✅ PASS' if avg_traj >= 0.90 else '⚠️ SUBOPTIMAL'} |",
        f"| **Parameter Propagation ($S_{{\\text{{Param}}}}$)** | 25% | **{avg_param * 100:.1f}%** | {'✅ PASS' if avg_param >= 0.90 else '⚠️ SUBOPTIMAL'} |",
        f"| **Step Efficiency ($S_{{\\text{{Eff}}}}$)** | 20% | **{avg_eff * 100:.1f}%** | {'✅ PASS' if avg_eff >= 0.90 else '⚠️ SUBOPTIMAL'} |",
        f"| **System Rule Compliance ($S_{{\\text{{Rule}}}}$)** | 20% | **{avg_rule * 100:.1f}%** | {'✅ PASS' if avg_rule >= 0.90 else '⚠️ SUBOPTIMAL'} |",
        f"| **Composite Benchmark Score** | **100%** | **{avg_score * 100:.2f}%** | **{'🌟 EXCELLENT' if avg_score >= 0.95 else '✅ PASS'}** |",
        "",
        f"- **Total Benchmark Latency:** {total_latency_ms:,} ms (Avg: {total_latency_ms // total_scenarios if total_scenarios else 0} ms/scenario)",
        f"- **Estimated Token Consumption:** {total_tokens:,} tokens",
        "",
        "---",
        "",
        "## 2. Performance Breakdown by Operational Role",
        "",
        "| Role | Evaluated Scenarios | Role Benchmark Score | Key Chaining Pattern Tested |",
        "| :--- | :---: | :---: | :--- |",
        f"| **HORSE_OWNER** | {role_stats['HORSE_OWNER']['count']} | **{role_stats['HORSE_OWNER']['score'] * 100:.1f}%** | Entity discovery, Bio-SPC heart rate analysis, Quarantine safety gate |",
        f"| **HEAD_TRAINER** | {role_stats['HEAD_TRAINER']['count']} | **{role_stats['HEAD_TRAINER']['score'] * 100:.1f}%** | Group fatigue triage, Rating ranking (Rule 15), Groom contact lookups |",
        f"| **VETERINARIAN** | {role_stats['VETERINARIAN']['count']} | **{role_stats['VETERINARIAN']['score'] * 100:.1f}%** | Incident alerts -> Vital signs -> Clinical RAG, Drug contraindication (Rule 8) |",
        f"| **GROOM** | {role_stats['GROOM']['count']} | **{role_stats['GROOM']['score'] * 100:.1f}%** | Colic anomaly triage, Stall location & area (Rule 7), Shift re-prioritization |",
        f"| **CLUB_MANAGER** | {role_stats['CLUB_MANAGER']['count']} | **{role_stats['CLUB_MANAGER']['score'] * 100:.1f}%** | Admissions funnel, Stall bottleneck forecasting, Groom workload rebalancing |",
        "",
        "---",
        "",
        "## 3. Detailed Scenario Trajectory & Scoring Matrix",
        "",
        "| Scenario ID | Role | Optimal / Actual | Trajectory ($S_{\\text{Traj}}$) | Param ($S_{\\text{Param}}$) | Eff ($S_{\\text{Eff}}$) | Rule ($S_{\\text{Rule}}$) | Overall Score |",
        "| :--- | :--- | :---: | :---: | :---: | :---: | :---: | :---: |"
    ]

    for r in results:
        sc = r["rubric_scores"]
        lines.append(
            f"| `{r['scenario_id']}` | {r['role']} | {r['optimal_steps']} / {r['actual_steps']} | "
            f"{sc['trajectory_score']*100:.0f}% | {sc['parameter_score']*100:.0f}% | {sc['efficiency_score']*100:.0f}% | "
            f"{sc['rule_score']*100:.0f}% | **{sc['composite_benchmark_score']*100:.1f}%** |"
        )

    lines.extend([
        "",
        "---",
        "",
        "## 4. In-Depth Scenario Execution Records",
        ""
    ])

    for r in results:
        lines.extend([
            f"### Scenario `{r['scenario_id']}`: {r['title']}",
            f"- **Role:** `{r['role']}`",
            f"- **User Prompt:** \"{r['prompt']}\"",
            f"- **Executed Trajectory:** `{' -> '.join(r['executed_trajectory']) if r['executed_trajectory'] else 'Direct Answer'}`",
            f"- **Iterations Taken:** {r['iterations']} | **Halt Reason:** `{r['halt_reason']}`",
            "",
            "#### Reasoning & Decision Trace (Chain-of-Thought):"
        ])
        for t in r["reasoning_traces"]:
            lines.append(f"> {t}\n>")

        lines.extend([
            "",
            "#### Final Assistant Response:",
            r["final_answer"],
            "",
            "---"
        ])

    os.makedirs(os.path.dirname(os.path.abspath(output_path)), exist_ok=True)
    with open(output_path, "w", encoding="utf-8") as f:
        f.write("\n".join(lines) + "\n")
    print(f"📄 Markdown evaluation report generated at: {output_path}")

# ==============================================================================
# 11. MAIN BENCHMARK ORCHESTRATOR
# ==============================================================================

def run_benchmark(
    scenarios: List[Dict[str, Any]],
    is_dry_run: bool,
    base_url: str,
    api_key: str,
    model: str,
    output_json: str,
    output_report: str,
    transport: str = "auto"
) -> int:
    """Executes the benchmark suite across provided scenarios."""
    print("=" * 80)
    print(f"🚀 RTMS ReAct Agent Loop Benchmark Engine")
    print(f"   Mode: {'DRY-RUN (Deterministic Oracle)' if is_dry_run else 'LIVE LLM API'}")
    print(f"   Transport: {'Deterministic Oracle' if is_dry_run else transport}")
    print(f"   Model: {model}")
    print(f"   Total Scenarios: {len(scenarios)}")
    print("=" * 80)

    mock_world = MockWorldState()
    results: List[Dict[str, Any]] = []

    for idx, sc in enumerate(scenarios, 1):
        print(f"\n[{idx:>2}/{len(scenarios)}] Executing `{sc['id']}` ({sc['role']}) - {sc['title']}...")
        try:
            res = execute_react_scenario(
                scenario=sc,
                mock_world=mock_world,
                is_dry_run=is_dry_run,
                base_url=base_url,
                api_key=api_key,
                model=model,
                transport=transport
            )
        except Exception as e:
            print(f"      ❌ Exception during execution: {e}")
            res = {
                "scenario_id": sc["id"],
                "role": sc["role"],
                "title": sc["title"],
                "prompt": sc["prompt"],
                "optimal_steps": sc["optimal_steps"],
                "actual_steps": 0,
                "iterations": 0,
                "halt_reason": f"ERROR: {str(e)}",
                "latency_ms": 0,
                "tokens": 0,
                "executed_trajectory": [],
                "actual_tool_calls": [],
                "reasoning_traces": [],
                "final_answer": f"Scenario execution failed: {str(e)}",
                "rubric_scores": {
                    "trajectory_score": 0.0,
                    "parameter_score": 0.0,
                    "efficiency_score": 0.0,
                    "rule_score": 0.0,
                    "composite_benchmark_score": 0.0
                },
                "rule_compliance": {},
                "evaluation_notes": {
                    "trajectory": [f"Execution error: {str(e)}"],
                    "parameter": [],
                    "rules": []
                }
            }
        results.append(res)
        score = res["rubric_scores"]["composite_benchmark_score"]
        steps = res["actual_steps"]
        traj = " -> ".join(res["executed_trajectory"]) if res["executed_trajectory"] else "Direct Answer"
        print(f"      Trajectory: {traj}")
        print(f"      Steps: {steps} | Score: {score * 100:.1f}% | Latency: {res['latency_ms']}ms")

    # Save JSON results
    os.makedirs(os.path.dirname(os.path.abspath(output_json)), exist_ok=True)
    summary_data = {
        "metadata": {
            "timestamp": datetime.now().isoformat(),
            "model": model,
            "mode": "dry-run" if is_dry_run else "live",
            "transport": "oracle" if is_dry_run else transport,
            "total_scenarios": len(results),
            "average_score": round(sum(r["rubric_scores"]["composite_benchmark_score"] for r in results) / len(results), 4) if results else 0.0
        },
        "results": results
    }
    with open(output_json, "w", encoding="utf-8") as f:
        json.dump(summary_data, f, indent=2, ensure_ascii=False)
    print(f"\n💾 JSON results written to: {output_json}")

    # Save Markdown report
    generate_markdown_report(results, output_report, model)

    avg_score = summary_data["metadata"]["average_score"]
    passed_count = sum(1 for r in results if r["rubric_scores"]["composite_benchmark_score"] >= 0.85)
    print("\n" + "=" * 80)
    print(f"🏁 Benchmark Completed! Overall Score: {avg_score * 100:.2f}% ({passed_count}/{len(results)} Scenarios Passed)")
    print("=" * 80)
    return 0 if (passed_count == len(results)) else 1

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

def main():
    load_env_file(".env")

    parser = argparse.ArgumentParser(description="RTMS ReAct Agent Loop Benchmark Suite")
    parser.add_argument("--dry-run", action="store_true", help="Run internal deterministic oracle simulator (default: True unless --live is specified)")
    parser.add_argument("--live", action="store_true", help="Run against live OpenAI API endpoint")
    parser.add_argument("--transport", choices=["auto", "responses", "chat"], default="auto", help="API transport protocol (auto: try responses then chat fallback, responses: /v1/responses, chat: /v1/chat/completions)")
    parser.add_argument("--base-url", "--api-base-url", dest="api_base_url", default=os.getenv("AI_BASE_URL", os.getenv("AI_API_BASE_URL", "https://api.openai.com/v1")), help="OpenAI-compatible Base URL")
    parser.add_argument("--api-key", default=os.getenv("AI_API_KEY", ""), help="OpenAI API Key")
    parser.add_argument("--model", default=os.getenv("AI_CHAT_MODEL", "gpt-6-luna"), help="Target LLM model (default: gpt-6-luna)")
    parser.add_argument("--role", choices=["HORSE_OWNER", "HEAD_TRAINER", "VETERINARIAN", "GROOM", "CLUB_MANAGER"], help="Filter scenarios by role")
    parser.add_argument("--scenario", help="Filter by specific scenario ID (e.g. REACT_OWNER_01)")
    parser.add_argument("--output-json", default="docs/benchmarks/ai_react_loop_results.json", help="Path for JSON output")
    parser.add_argument("--output-report", default="docs/benchmarks/AI_REACT_LOOP_REPORT.md", help="Path for Markdown report")

    args = parser.parse_args()

    # Determine execution mode: if --live is not specified, run dry-run by default
    is_dry_run = not args.live or args.dry_run

    if not is_dry_run and not args.api_key:
        print("⚠️ Warning: Running in --live mode without an --api-key / AI_API_KEY. Endpoint calls may return 401 Unauthorized.")

    scenarios = BENCHMARK_SCENARIOS
    if args.role:
        scenarios = [s for s in scenarios if s["role"] == args.role]
    if args.scenario:
        scenarios = [s for s in scenarios if s["id"] == args.scenario]

    if not scenarios:
        print(f"❌ No scenarios match the filter (role={args.role}, scenario={args.scenario})")
        sys.exit(1)

    exit_code = run_benchmark(
        scenarios=scenarios,
        is_dry_run=is_dry_run,
        base_url=args.api_base_url,
        api_key=args.api_key,
        model=args.model,
        output_json=args.output_json,
        output_report=args.output_report,
        transport=args.transport
    )
    sys.exit(exit_code)

if __name__ == "__main__":
    main()
