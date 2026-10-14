(globalThis["TURBOPACK"] || (globalThis["TURBOPACK"] = [])).push([typeof document === "object" ? document.currentScript : undefined,
"[project]/src/components/auth/RoleGuard.tsx [app-client] (ecmascript)", ((__turbopack_context__) => {
"use strict";

__turbopack_context__.s([
    "RoleGuard",
    ()=>RoleGuard
]);
var __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$compiled$2f$react$2f$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$client$5d$__$28$ecmascript$29$__ = __turbopack_context__.i("[project]/node_modules/next/dist/compiled/react/jsx-dev-runtime.js [app-client] (ecmascript)");
var __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$compiled$2f$react$2f$index$2e$js__$5b$app$2d$client$5d$__$28$ecmascript$29$__ = __turbopack_context__.i("[project]/node_modules/next/dist/compiled/react/index.js [app-client] (ecmascript)");
var __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$navigation$2e$js__$5b$app$2d$client$5d$__$28$ecmascript$29$__ = __turbopack_context__.i("[project]/node_modules/next/navigation.js [app-client] (ecmascript)");
var __TURBOPACK__imported__module__$5b$project$5d2f$src$2f$context$2f$AuthContext$2e$tsx__$5b$app$2d$client$5d$__$28$ecmascript$29$__ = __turbopack_context__.i("[project]/src/context/AuthContext.tsx [app-client] (ecmascript)");
var __TURBOPACK__imported__module__$5b$project$5d2f$src$2f$components$2f$ui$2f$Spinner$2e$tsx__$5b$app$2d$client$5d$__$28$ecmascript$29$__ = __turbopack_context__.i("[project]/src/components/ui/Spinner.tsx [app-client] (ecmascript)");
var __TURBOPACK__imported__module__$5b$project$5d2f$src$2f$lib$2f$roleRoute$2e$ts__$5b$app$2d$client$5d$__$28$ecmascript$29$__ = __turbopack_context__.i("[project]/src/lib/roleRoute.ts [app-client] (ecmascript)");
;
var _s = __turbopack_context__.k.signature();
'use client';
;
;
;
;
;
function RoleGuard({ allowedRoles, children }) {
    _s();
    const { user, loading, isAuthenticated } = (0, __TURBOPACK__imported__module__$5b$project$5d2f$src$2f$context$2f$AuthContext$2e$tsx__$5b$app$2d$client$5d$__$28$ecmascript$29$__["useAuth"])();
    const router = (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$navigation$2e$js__$5b$app$2d$client$5d$__$28$ecmascript$29$__["useRouter"])();
    (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$compiled$2f$react$2f$index$2e$js__$5b$app$2d$client$5d$__$28$ecmascript$29$__["useEffect"])({
        "RoleGuard.useEffect": ()=>{
            if (loading) return;
            if (!isAuthenticated || !user) {
                router.replace('/login');
                return;
            }
            if (!allowedRoles.includes(user.role)) {
                router.replace((0, __TURBOPACK__imported__module__$5b$project$5d2f$src$2f$lib$2f$roleRoute$2e$ts__$5b$app$2d$client$5d$__$28$ecmascript$29$__["getRoleRoute"])(user.role));
            }
        }
    }["RoleGuard.useEffect"], [
        loading,
        isAuthenticated,
        user,
        allowedRoles,
        router
    ]);
    if (loading || !isAuthenticated || !user || !allowedRoles.includes(user.role)) {
        return /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$compiled$2f$react$2f$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$client$5d$__$28$ecmascript$29$__["jsxDEV"])(__TURBOPACK__imported__module__$5b$project$5d2f$src$2f$components$2f$ui$2f$Spinner$2e$tsx__$5b$app$2d$client$5d$__$28$ecmascript$29$__["LoadingScreen"], {}, void 0, false, {
            fileName: "[project]/src/components/auth/RoleGuard.tsx",
            lineNumber: 43,
            columnNumber: 12
        }, this);
    }
    return /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$compiled$2f$react$2f$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$client$5d$__$28$ecmascript$29$__["jsxDEV"])(__TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$compiled$2f$react$2f$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$client$5d$__$28$ecmascript$29$__["Fragment"], {
        children: children
    }, void 0, false, {
        fileName: "[project]/src/components/auth/RoleGuard.tsx",
        lineNumber: 46,
        columnNumber: 10
    }, this);
}
_s(RoleGuard, "RSKdTqqa8sIcSl0j7/tbD2lfdCw=", false, function() {
    return [
        __TURBOPACK__imported__module__$5b$project$5d2f$src$2f$context$2f$AuthContext$2e$tsx__$5b$app$2d$client$5d$__$28$ecmascript$29$__["useAuth"],
        __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$navigation$2e$js__$5b$app$2d$client$5d$__$28$ecmascript$29$__["useRouter"]
    ];
});
_c = RoleGuard;
var _c;
__turbopack_context__.k.register(_c, "RoleGuard");
if (typeof globalThis.$RefreshHelpers$ === 'object' && globalThis.$RefreshHelpers !== null) {
    __turbopack_context__.k.registerExports(__turbopack_context__.m, globalThis.$RefreshHelpers$);
}
}),
"[project]/src/features/admissions/components/VetAdmissionQueue.tsx [app-client] (ecmascript)", ((__turbopack_context__) => {
"use strict";

__turbopack_context__.s([
    "VetAdmissionQueue",
    ()=>VetAdmissionQueue
]);
var __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$compiled$2f$react$2f$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$client$5d$__$28$ecmascript$29$__ = __turbopack_context__.i("[project]/node_modules/next/dist/compiled/react/jsx-dev-runtime.js [app-client] (ecmascript)");
var __TURBOPACK__imported__module__$5b$project$5d2f$src$2f$components$2f$ui$2f$Table$2e$tsx__$5b$app$2d$client$5d$__$28$ecmascript$29$__ = __turbopack_context__.i("[project]/src/components/ui/Table.tsx [app-client] (ecmascript)");
var __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$compiled$2f$react$2f$index$2e$js__$5b$app$2d$client$5d$__$28$ecmascript$29$__ = __turbopack_context__.i("[project]/node_modules/next/dist/compiled/react/index.js [app-client] (ecmascript)");
var __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$navigation$2e$js__$5b$app$2d$client$5d$__$28$ecmascript$29$__ = __turbopack_context__.i("[project]/node_modules/next/navigation.js [app-client] (ecmascript)");
var __TURBOPACK__imported__module__$5b$project$5d2f$src$2f$context$2f$AuthContext$2e$tsx__$5b$app$2d$client$5d$__$28$ecmascript$29$__ = __turbopack_context__.i("[project]/src/context/AuthContext.tsx [app-client] (ecmascript)");
var __TURBOPACK__imported__module__$5b$project$5d2f$src$2f$services$2f$api$2e$ts__$5b$app$2d$client$5d$__$28$ecmascript$29$__ = __turbopack_context__.i("[project]/src/services/api.ts [app-client] (ecmascript)");
var __TURBOPACK__imported__module__$5b$project$5d2f$src$2f$components$2f$ui$2f$Button$2e$tsx__$5b$app$2d$client$5d$__$28$ecmascript$29$__ = __turbopack_context__.i("[project]/src/components/ui/Button.tsx [app-client] (ecmascript)");
var __TURBOPACK__imported__module__$5b$project$5d2f$src$2f$components$2f$ui$2f$ConfirmDialog$2e$tsx__$5b$app$2d$client$5d$__$28$ecmascript$29$__ = __turbopack_context__.i("[project]/src/components/ui/ConfirmDialog.tsx [app-client] (ecmascript)");
var __TURBOPACK__imported__module__$5b$project$5d2f$src$2f$components$2f$ui$2f$HorseAvatar$2e$tsx__$5b$app$2d$client$5d$__$28$ecmascript$29$__ = __turbopack_context__.i("[project]/src/components/ui/HorseAvatar.tsx [app-client] (ecmascript)");
var __TURBOPACK__imported__module__$5b$project$5d2f$src$2f$components$2f$ui$2f$Icon$2e$tsx__$5b$app$2d$client$5d$__$28$ecmascript$29$__ = __turbopack_context__.i("[project]/src/components/ui/Icon.tsx [app-client] (ecmascript)");
var __TURBOPACK__imported__module__$5b$project$5d2f$src$2f$components$2f$ui$2f$StatusBadge$2e$tsx__$5b$app$2d$client$5d$__$28$ecmascript$29$__ = __turbopack_context__.i("[project]/src/components/ui/StatusBadge.tsx [app-client] (ecmascript)");
var __TURBOPACK__imported__module__$5b$project$5d2f$src$2f$components$2f$ui$2f$Tabs$2e$tsx__$5b$app$2d$client$5d$__$28$ecmascript$29$__ = __turbopack_context__.i("[project]/src/components/ui/Tabs.tsx [app-client] (ecmascript)");
var __TURBOPACK__imported__module__$5b$project$5d2f$src$2f$components$2f$ui$2f$states$2e$tsx__$5b$app$2d$client$5d$__$28$ecmascript$29$__ = __turbopack_context__.i("[project]/src/components/ui/states.tsx [app-client] (ecmascript)");
var __TURBOPACK__imported__module__$5b$project$5d2f$src$2f$components$2f$ui$2f$FilterBar$2e$tsx__$5b$app$2d$client$5d$__$28$ecmascript$29$__ = __turbopack_context__.i("[project]/src/components/ui/FilterBar.tsx [app-client] (ecmascript)");
var __TURBOPACK__imported__module__$5b$project$5d2f$src$2f$components$2f$ui$2f$DataTable$2e$tsx__$5b$app$2d$client$5d$__$28$ecmascript$29$__ = __turbopack_context__.i("[project]/src/components/ui/DataTable.tsx [app-client] (ecmascript)");
var __TURBOPACK__imported__module__$5b$project$5d2f$src$2f$components$2f$ui$2f$FormField$2e$tsx__$5b$app$2d$client$5d$__$28$ecmascript$29$__ = __turbopack_context__.i("[project]/src/components/ui/FormField.tsx [app-client] (ecmascript)");
var __TURBOPACK__imported__module__$5b$project$5d2f$src$2f$components$2f$ui$2f$Input$2e$tsx__$5b$app$2d$client$5d$__$28$ecmascript$29$__ = __turbopack_context__.i("[project]/src/components/ui/Input.tsx [app-client] (ecmascript)");
var __TURBOPACK__imported__module__$5b$project$5d2f$src$2f$components$2f$ui$2f$Notice$2e$tsx__$5b$app$2d$client$5d$__$28$ecmascript$29$__ = __turbopack_context__.i("[project]/src/components/ui/Notice.tsx [app-client] (ecmascript)");
var __TURBOPACK__imported__module__$5b$project$5d2f$src$2f$components$2f$ui$2f$SegmentedControl$2e$tsx__$5b$app$2d$client$5d$__$28$ecmascript$29$__ = __turbopack_context__.i("[project]/src/components/ui/SegmentedControl.tsx [app-client] (ecmascript)");
var __TURBOPACK__imported__module__$5b$project$5d2f$src$2f$features$2f$admissions$2f$shared$2f$components$2f$AdmissionListLayout$2e$tsx__$5b$app$2d$client$5d$__$28$ecmascript$29$__ = __turbopack_context__.i("[project]/src/features/admissions/shared/components/AdmissionListLayout.tsx [app-client] (ecmascript)");
var __TURBOPACK__imported__module__$5b$project$5d2f$src$2f$features$2f$admissions$2f$shared$2f$components$2f$AdmissionDetailLayout$2e$tsx__$5b$app$2d$client$5d$__$28$ecmascript$29$__ = __turbopack_context__.i("[project]/src/features/admissions/shared/components/AdmissionDetailLayout.tsx [app-client] (ecmascript)");
var __TURBOPACK__imported__module__$5b$project$5d2f$src$2f$features$2f$admissions$2f$shared$2f$components$2f$AdmissionDetailHeader$2e$tsx__$5b$app$2d$client$5d$__$28$ecmascript$29$__ = __turbopack_context__.i("[project]/src/features/admissions/shared/components/AdmissionDetailHeader.tsx [app-client] (ecmascript)");
var __TURBOPACK__imported__module__$5b$project$5d2f$src$2f$features$2f$admissions$2f$shared$2f$components$2f$AdmissionPipeline$2e$tsx__$5b$app$2d$client$5d$__$28$ecmascript$29$__ = __turbopack_context__.i("[project]/src/features/admissions/shared/components/AdmissionPipeline.tsx [app-client] (ecmascript)");
var __TURBOPACK__imported__module__$5b$project$5d2f$src$2f$features$2f$admissions$2f$shared$2f$components$2f$AdmissionInfoSection$2e$tsx__$5b$app$2d$client$5d$__$28$ecmascript$29$__ = __turbopack_context__.i("[project]/src/features/admissions/shared/components/AdmissionInfoSection.tsx [app-client] (ecmascript)");
var __TURBOPACK__imported__module__$5b$project$5d2f$src$2f$features$2f$admissions$2f$shared$2f$components$2f$AdmissionStatusBadge$2e$tsx__$5b$app$2d$client$5d$__$28$ecmascript$29$__ = __turbopack_context__.i("[project]/src/features/admissions/shared/components/AdmissionStatusBadge.tsx [app-client] (ecmascript)");
var __TURBOPACK__imported__module__$5b$project$5d2f$src$2f$features$2f$admissions$2f$services$2f$api$2e$ts__$5b$app$2d$client$5d$__$28$ecmascript$29$__ = __turbopack_context__.i("[project]/src/features/admissions/services/api.ts [app-client] (ecmascript)");
var __TURBOPACK__imported__module__$5b$project$5d2f$src$2f$features$2f$admissions$2f$components$2f$VetReviewForm$2e$tsx__$5b$app$2d$client$5d$__$28$ecmascript$29$__ = __turbopack_context__.i("[project]/src/features/admissions/components/VetReviewForm.tsx [app-client] (ecmascript)");
;
var _s = __turbopack_context__.k.signature();
'use client';
;
;
;
;
;
;
;
;
;
;
;
;
;
;
;
;
;
;
;
;
;
;
;
;
;
;
const examStatusOptions = [
    {
        value: 'ALL',
        label: 'All Exam States'
    },
    {
        value: 'REQUESTED',
        label: 'Requested'
    },
    {
        value: 'SCHEDULED',
        label: 'Scheduled'
    },
    {
        value: 'IN_PROGRESS',
        label: 'In Progress'
    },
    {
        value: 'COMPLETED',
        label: 'Completed'
    }
];
const examTypeOptions = [
    {
        value: 'ALL',
        label: 'All Exam Types'
    },
    {
        value: 'INITIAL',
        label: 'Initial Exam'
    },
    {
        value: 'ROUTINE',
        label: 'Routine Exam'
    },
    {
        value: 'URGENT',
        label: 'Urgent Exam'
    }
];
function errorText(error) {
    if (error instanceof __TURBOPACK__imported__module__$5b$project$5d2f$src$2f$services$2f$api$2e$ts__$5b$app$2d$client$5d$__$28$ecmascript$29$__["ApiError"]) {
        return `${error.status === 403 ? 'Permission denied' : error.status === 409 ? 'State conflict' : 'Request error'} (${error.status}${error.errorCode ? ` · ${error.errorCode}` : ''}): ${error.message}`;
    }
    return error instanceof Error ? error.message : 'Operation failed. Please try again.';
}
function formatLabel(value) {
    return value.replaceAll('_', ' ').toLowerCase().replace(/^./, (letter)=>letter.toUpperCase());
}
function formatDate(value, includeTime = false) {
    if (!value) return 'Not scheduled';
    const date = new Date(value);
    if (Number.isNaN(date.getTime())) return value;
    return new Intl.DateTimeFormat(undefined, includeTime ? {
        dateStyle: 'medium',
        timeStyle: 'short'
    } : {
        dateStyle: 'medium'
    }).format(date);
}
function calculateAge(dateOfBirth) {
    if (!dateOfBirth) return 'Age unavailable';
    const dob = new Date(dateOfBirth);
    if (Number.isNaN(dob.getTime())) return 'Age unavailable';
    const now = new Date();
    let years = now.getFullYear() - dob.getFullYear();
    const m = now.getMonth() - dob.getMonth();
    if (m < 0 || m === 0 && now.getDate() < dob.getDate()) {
        years--;
    }
    if (years < 1) {
        const months = Math.max(0, (now.getFullYear() - dob.getFullYear()) * 12 + now.getMonth() - dob.getMonth());
        return `${months} mo`;
    }
    return `${years} yo`;
}
function examTone(status) {
    if (status === 'COMPLETED') return 'success';
    if (status === 'IN_PROGRESS') return 'primary';
    if (status === 'SCHEDULED') return 'info';
    if (status === 'REQUESTED') return 'warning';
    return 'neutral';
}
function examIcon(status) {
    if (status === 'COMPLETED') return 'check';
    if (status === 'IN_PROGRESS') return 'activity';
    if (status === 'SCHEDULED') return 'calendar';
    if (status === 'REQUESTED') return 'clock';
    return 'circle';
}
function getPriorityBadge(careType) {
    if (careType === 'URGENT') {
        return {
            label: 'P1 · Urgent',
            tone: 'danger',
            icon: 'alert-triangle'
        };
    }
    if (careType === 'INITIAL') {
        return {
            label: 'P2 · Initial',
            tone: 'info',
            icon: 'stethoscope'
        };
    }
    return {
        label: 'P4 · Routine',
        tone: 'neutral',
        icon: 'clock'
    };
}
function MedicalNoteBlock({ label, value }) {
    if (!value) return null;
    return /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$compiled$2f$react$2f$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$client$5d$__$28$ecmascript$29$__["jsxDEV"])("div", {
        className: "rounded-[var(--radius-sm)] border border-[var(--color-border)] bg-[var(--color-surface-subtle)] p-3",
        children: [
            /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$compiled$2f$react$2f$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$client$5d$__$28$ecmascript$29$__["jsxDEV"])("span", {
                className: "text-xs font-semibold uppercase tracking-wider text-[var(--color-text-muted)]",
                children: label
            }, void 0, false, {
                fileName: "[project]/src/features/admissions/components/VetAdmissionQueue.tsx",
                lineNumber: 131,
                columnNumber: 7
            }, this),
            /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$compiled$2f$react$2f$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$client$5d$__$28$ecmascript$29$__["jsxDEV"])("p", {
                className: "mt-1 whitespace-pre-wrap text-xs leading-relaxed text-[var(--color-text-primary)]",
                children: value
            }, void 0, false, {
                fileName: "[project]/src/features/admissions/components/VetAdmissionQueue.tsx",
                lineNumber: 134,
                columnNumber: 7
            }, this)
        ]
    }, void 0, true, {
        fileName: "[project]/src/features/admissions/components/VetAdmissionQueue.tsx",
        lineNumber: 130,
        columnNumber: 5
    }, this);
}
_c = MedicalNoteBlock;
function VetAdmissionQueue() {
    _s();
    const { user } = (0, __TURBOPACK__imported__module__$5b$project$5d2f$src$2f$context$2f$AuthContext$2e$tsx__$5b$app$2d$client$5d$__$28$ecmascript$29$__["useAuth"])();
    const router = (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$navigation$2e$js__$5b$app$2d$client$5d$__$28$ecmascript$29$__["useRouter"])();
    const searchParams = (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$navigation$2e$js__$5b$app$2d$client$5d$__$28$ecmascript$29$__["useSearchParams"])();
    // Server-side queue state
    const [items, setItems] = (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$compiled$2f$react$2f$index$2e$js__$5b$app$2d$client$5d$__$28$ecmascript$29$__["useState"])([]);
    const [page, setPage] = (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$compiled$2f$react$2f$index$2e$js__$5b$app$2d$client$5d$__$28$ecmascript$29$__["useState"])(0);
    const [pageSize] = (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$compiled$2f$react$2f$index$2e$js__$5b$app$2d$client$5d$__$28$ecmascript$29$__["useState"])(10);
    const [totalElements, setTotalElements] = (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$compiled$2f$react$2f$index$2e$js__$5b$app$2d$client$5d$__$28$ecmascript$29$__["useState"])(0);
    const [summary, setSummary] = (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$compiled$2f$react$2f$index$2e$js__$5b$app$2d$client$5d$__$28$ecmascript$29$__["useState"])({
        total: 0,
        awaiting: 0,
        inProgress: 0
    });
    // Full-page examination workspace & selection state
    const [selectedId, setSelectedId] = (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$compiled$2f$react$2f$index$2e$js__$5b$app$2d$client$5d$__$28$ecmascript$29$__["useState"])(null);
    const [targetScheduleId, setTargetScheduleId] = (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$compiled$2f$react$2f$index$2e$js__$5b$app$2d$client$5d$__$28$ecmascript$29$__["useState"])(null);
    const [deepLinkedSchedule, setDeepLinkedSchedule] = (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$compiled$2f$react$2f$index$2e$js__$5b$app$2d$client$5d$__$28$ecmascript$29$__["useState"])(null);
    const [drawerOpen, setDrawerOpen] = (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$compiled$2f$react$2f$index$2e$js__$5b$app$2d$client$5d$__$28$ecmascript$29$__["useState"])(false);
    const [isDrawerDirty, setIsDrawerDirty] = (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$compiled$2f$react$2f$index$2e$js__$5b$app$2d$client$5d$__$28$ecmascript$29$__["useState"])(false);
    const [showCloseConfirm, setShowCloseConfirm] = (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$compiled$2f$react$2f$index$2e$js__$5b$app$2d$client$5d$__$28$ecmascript$29$__["useState"])(false);
    // Detail data for opened admission
    const [detail, setDetail] = (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$compiled$2f$react$2f$index$2e$js__$5b$app$2d$client$5d$__$28$ecmascript$29$__["useState"])(null);
    const [documents, setDocuments] = (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$compiled$2f$react$2f$index$2e$js__$5b$app$2d$client$5d$__$28$ecmascript$29$__["useState"])([]);
    const [metrics, setMetrics] = (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$compiled$2f$react$2f$index$2e$js__$5b$app$2d$client$5d$__$28$ecmascript$29$__["useState"])([]);
    const [careSchedules, setCareSchedules] = (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$compiled$2f$react$2f$index$2e$js__$5b$app$2d$client$5d$__$28$ecmascript$29$__["useState"])([]);
    const [stallCodes, setStallCodes] = (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$compiled$2f$react$2f$index$2e$js__$5b$app$2d$client$5d$__$28$ecmascript$29$__["useState"])({});
    const [activeTab, setActiveTab] = (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$compiled$2f$react$2f$index$2e$js__$5b$app$2d$client$5d$__$28$ecmascript$29$__["useState"])('exam');
    // Filters state
    const [searchQuery, setSearchQuery] = (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$compiled$2f$react$2f$index$2e$js__$5b$app$2d$client$5d$__$28$ecmascript$29$__["useState"])('');
    const [queuePillFilter, setQueuePillFilter] = (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$compiled$2f$react$2f$index$2e$js__$5b$app$2d$client$5d$__$28$ecmascript$29$__["useState"])('ALL');
    const [admissionFilter, setAdmissionFilter] = (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$compiled$2f$react$2f$index$2e$js__$5b$app$2d$client$5d$__$28$ecmascript$29$__["useState"])('ALL');
    const [examStatusFilter, setExamStatusFilter] = (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$compiled$2f$react$2f$index$2e$js__$5b$app$2d$client$5d$__$28$ecmascript$29$__["useState"])('ALL');
    const [examTypeFilter, setExamTypeFilter] = (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$compiled$2f$react$2f$index$2e$js__$5b$app$2d$client$5d$__$28$ecmascript$29$__["useState"])('ALL');
    const [priorityFilter, setPriorityFilter] = (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$compiled$2f$react$2f$index$2e$js__$5b$app$2d$client$5d$__$28$ecmascript$29$__["useState"])('ALL');
    // Request & operation state
    const [loading, setLoading] = (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$compiled$2f$react$2f$index$2e$js__$5b$app$2d$client$5d$__$28$ecmascript$29$__["useState"])(true);
    const [loadingDetail, setLoadingDetail] = (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$compiled$2f$react$2f$index$2e$js__$5b$app$2d$client$5d$__$28$ecmascript$29$__["useState"])(false);
    const [startingExam, setStartingExam] = (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$compiled$2f$react$2f$index$2e$js__$5b$app$2d$client$5d$__$28$ecmascript$29$__["useState"])(false);
    const [error, setError] = (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$compiled$2f$react$2f$index$2e$js__$5b$app$2d$client$5d$__$28$ecmascript$29$__["useState"])('');
    const [detailError, setDetailError] = (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$compiled$2f$react$2f$index$2e$js__$5b$app$2d$client$5d$__$28$ecmascript$29$__["useState"])('');
    const [success, setSuccess] = (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$compiled$2f$react$2f$index$2e$js__$5b$app$2d$client$5d$__$28$ecmascript$29$__["useState"])('');
    const [scheduleWarning, setScheduleWarning] = (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$compiled$2f$react$2f$index$2e$js__$5b$app$2d$client$5d$__$28$ecmascript$29$__["useState"])('');
    const [reload, setReload] = (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$compiled$2f$react$2f$index$2e$js__$5b$app$2d$client$5d$__$28$ecmascript$29$__["useState"])(0);
    // Deep link support via ?id= query param
    (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$compiled$2f$react$2f$index$2e$js__$5b$app$2d$client$5d$__$28$ecmascript$29$__["useEffect"])({
        "VetAdmissionQueue.useEffect": ()=>{
            const idParam = searchParams.get('id');
            const scheduleIdParam = searchParams.get('scheduleId');
            if (scheduleIdParam) {
                const parsedScheduleId = Number(scheduleIdParam);
                if (!Number.isNaN(parsedScheduleId) && parsedScheduleId > 0) {
                    let active = true;
                    Promise.resolve().then({
                        "VetAdmissionQueue.useEffect": ()=>{
                            if (!active) return;
                            setTargetScheduleId(parsedScheduleId);
                            setDeepLinkedSchedule(null);
                            setSelectedId(null);
                            setDetail(null);
                            setDrawerOpen(true);
                            setLoadingDetail(true);
                            setDetailError('');
                        }
                    }["VetAdmissionQueue.useEffect"]);
                    __TURBOPACK__imported__module__$5b$project$5d2f$src$2f$features$2f$admissions$2f$services$2f$api$2e$ts__$5b$app$2d$client$5d$__$28$ecmascript$29$__["admissionsApi"].getCareScheduleDetail(parsedScheduleId).then({
                        "VetAdmissionQueue.useEffect": ({ schedule })=>{
                            if (!active) return;
                            if (!schedule.admissionId) {
                                setDetailError('This care schedule is not linked to an Admission record.');
                                return;
                            }
                            setDeepLinkedSchedule(schedule);
                            setSelectedId(schedule.admissionId);
                            setActiveTab('exam');
                        }
                    }["VetAdmissionQueue.useEffect"]).catch({
                        "VetAdmissionQueue.useEffect": (cause)=>{
                            if (active) setDetailError(errorText(cause));
                        }
                    }["VetAdmissionQueue.useEffect"]).finally({
                        "VetAdmissionQueue.useEffect": ()=>{
                            if (active) setLoadingDetail(false);
                        }
                    }["VetAdmissionQueue.useEffect"]);
                    return ({
                        "VetAdmissionQueue.useEffect": ()=>{
                            active = false;
                        }
                    })["VetAdmissionQueue.useEffect"];
                }
            } else if (idParam) {
                const parsed = Number(idParam);
                if (!Number.isNaN(parsed) && parsed > 0) {
                    Promise.resolve().then({
                        "VetAdmissionQueue.useEffect": ()=>{
                            setTargetScheduleId(null);
                            setDeepLinkedSchedule(null);
                            setSelectedId(parsed);
                            setActiveTab('exam');
                            setDrawerOpen(true);
                        }
                    }["VetAdmissionQueue.useEffect"]);
                }
            }
        }
    }["VetAdmissionQueue.useEffect"], [
        searchParams
    ]);
    // Server-side filtered queue loading
    (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$compiled$2f$react$2f$index$2e$js__$5b$app$2d$client$5d$__$28$ecmascript$29$__["useEffect"])({
        "VetAdmissionQueue.useEffect": ()=>{
            let active = true;
            Promise.resolve().then({
                "VetAdmissionQueue.useEffect": ()=>{
                    if (!active) return;
                    setLoading(true);
                    setError('');
                }
            }["VetAdmissionQueue.useEffect"]);
            Promise.all([
                __TURBOPACK__imported__module__$5b$project$5d2f$src$2f$features$2f$admissions$2f$services$2f$api$2e$ts__$5b$app$2d$client$5d$__$28$ecmascript$29$__["admissionsApi"].getVetQueue({
                    search: searchQuery.trim() || undefined,
                    pill: queuePillFilter,
                    admissionStatus: admissionFilter !== 'ALL' ? admissionFilter : undefined,
                    scheduleStatus: examStatusFilter !== 'ALL' ? examStatusFilter : undefined,
                    careType: examTypeFilter !== 'ALL' ? examTypeFilter : undefined,
                    priority: priorityFilter !== 'ALL' ? priorityFilter : undefined,
                    page,
                    size: pageSize
                }),
                __TURBOPACK__imported__module__$5b$project$5d2f$src$2f$features$2f$admissions$2f$services$2f$api$2e$ts__$5b$app$2d$client$5d$__$28$ecmascript$29$__["admissionsApi"].getVetQueueSummary()
            ]).then({
                "VetAdmissionQueue.useEffect": ([queuePage, summaryData])=>{
                    if (!active) return;
                    setItems(queuePage.content);
                    setTotalElements(queuePage.totalElements);
                    setSummary(summaryData);
                }
            }["VetAdmissionQueue.useEffect"]).catch({
                "VetAdmissionQueue.useEffect": (cause)=>{
                    if (active) setError(errorText(cause));
                }
            }["VetAdmissionQueue.useEffect"]).finally({
                "VetAdmissionQueue.useEffect": ()=>{
                    if (active) setLoading(false);
                }
            }["VetAdmissionQueue.useEffect"]);
            return ({
                "VetAdmissionQueue.useEffect": ()=>{
                    active = false;
                }
            })["VetAdmissionQueue.useEffect"];
        }
    }["VetAdmissionQueue.useEffect"], [
        searchQuery,
        queuePillFilter,
        admissionFilter,
        examStatusFilter,
        examTypeFilter,
        priorityFilter,
        page,
        pageSize,
        reload
    ]);
    const hasActiveFilters = searchQuery !== '' || queuePillFilter !== 'ALL' || admissionFilter !== 'ALL' || examStatusFilter !== 'ALL' || examTypeFilter !== 'ALL' || priorityFilter !== 'ALL';
    const resetFilters = ()=>{
        setSearchQuery('');
        setQueuePillFilter('ALL');
        setAdmissionFilter('ALL');
        setExamStatusFilter('ALL');
        setExamTypeFilter('ALL');
        setPriorityFilter('ALL');
        setPage(0);
    };
    // Fetch admission details via vet-enforced endpoint when selected
    (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$compiled$2f$react$2f$index$2e$js__$5b$app$2d$client$5d$__$28$ecmascript$29$__["useEffect"])({
        "VetAdmissionQueue.useEffect": ()=>{
            if (selectedId === null) return;
            let active = true;
            Promise.resolve().then({
                "VetAdmissionQueue.useEffect": ()=>{
                    if (active) {
                        setLoadingDetail(true);
                        setDetailError('');
                        setDetail(null);
                        setDocuments([]);
                        setMetrics([]);
                        setCareSchedules([]);
                    }
                }
            }["VetAdmissionQueue.useEffect"]);
            Promise.all([
                __TURBOPACK__imported__module__$5b$project$5d2f$src$2f$features$2f$admissions$2f$services$2f$api$2e$ts__$5b$app$2d$client$5d$__$28$ecmascript$29$__["admissionsApi"].getVetAdmissionDetail(selectedId),
                __TURBOPACK__imported__module__$5b$project$5d2f$src$2f$features$2f$admissions$2f$services$2f$api$2e$ts__$5b$app$2d$client$5d$__$28$ecmascript$29$__["admissionsApi"].getDocuments(selectedId),
                __TURBOPACK__imported__module__$5b$project$5d2f$src$2f$features$2f$admissions$2f$services$2f$api$2e$ts__$5b$app$2d$client$5d$__$28$ecmascript$29$__["admissionsApi"].getCareSchedules({
                    admissionId: selectedId,
                    size: 50
                })
            ]).then({
                "VetAdmissionQueue.useEffect": async ([detailData, docs, schedulesPage])=>{
                    if (!active) return;
                    setDetail(detailData);
                    setDocuments(docs);
                    setCareSchedules(schedulesPage.content);
                    if (detailData.quarantineStallCode) {
                        setStallCodes({
                            "VetAdmissionQueue.useEffect": (prev)=>({
                                    ...prev,
                                    [detailData.admissionId]: detailData.quarantineStallCode
                                })
                        }["VetAdmissionQueue.useEffect"]);
                    }
                    if (detailData.horseId) {
                        try {
                            const metricData = await __TURBOPACK__imported__module__$5b$project$5d2f$src$2f$features$2f$admissions$2f$services$2f$api$2e$ts__$5b$app$2d$client$5d$__$28$ecmascript$29$__["admissionsApi"].getHorseHealthMetrics(detailData.horseId);
                            if (active) setMetrics(metricData);
                        } catch  {
                            if (active) setMetrics([]);
                        }
                    }
                }
            }["VetAdmissionQueue.useEffect"]).catch({
                "VetAdmissionQueue.useEffect": (cause)=>{
                    if (active) setDetailError(errorText(cause));
                }
            }["VetAdmissionQueue.useEffect"]).finally({
                "VetAdmissionQueue.useEffect": ()=>{
                    if (active) setLoadingDetail(false);
                }
            }["VetAdmissionQueue.useEffect"]);
            return ({
                "VetAdmissionQueue.useEffect": ()=>{
                    active = false;
                }
            })["VetAdmissionQueue.useEffect"];
        }
    }["VetAdmissionQueue.useEffect"], [
        selectedId,
        reload
    ]);
    const selectedRow = items.find((row)=>targetScheduleId ? row.careSchedule?.id === targetScheduleId : row.admissionId === selectedId) ?? null;
    const detailScheduleFallback = detail?.initialExamSchedule ? {
        id: detail.initialExamSchedule.scheduleId,
        horseId: detail.horseId ?? 0,
        admissionId: detail.admissionId,
        careType: detail.initialExamSchedule.careType || 'INITIAL',
        status: detail.initialExamSchedule.status || 'REQUESTED',
        veterinarianId: detail.initialExamSchedule.veterinarianId,
        assignedVetId: detail.initialExamSchedule.veterinarianId,
        scheduledAt: detail.initialExamSchedule.scheduledAt,
        scheduledDate: detail.initialExamSchedule.scheduledDate
    } : null;
    const explicitlySelectedSchedule = targetScheduleId ? careSchedules.find((schedule)=>schedule.id === targetScheduleId) ?? (deepLinkedSchedule?.id === targetScheduleId ? deepLinkedSchedule : null) : null;
    const assignedActiveSchedule = careSchedules.filter((schedule)=>[
            'SCHEDULED',
            'IN_PROGRESS'
        ].includes(schedule.status) && (schedule.veterinarianId ?? schedule.assignedVetId) === user?.userId).sort((a, b)=>{
        if (a.status !== b.status) return a.status === 'IN_PROGRESS' ? -1 : 1;
        return new Date(a.scheduledAt ?? a.scheduledDate ?? 0).getTime() - new Date(b.scheduledAt ?? b.scheduledDate ?? 0).getTime();
    })[0] ?? null;
    const currentActiveSchedule = explicitlySelectedSchedule ?? assignedActiveSchedule ?? selectedRow?.careSchedule ?? detailScheduleFallback;
    const currentScheduleRecord = detail?.healthRecords?.find((record)=>record.careScheduleId === currentActiveSchedule?.id);
    const displayedTrainingDecision = currentScheduleRecord?.trainingDecision ?? (currentActiveSchedule?.careType === 'INITIAL' ? detail?.vetTrainingDecision : null);
    const assignedVetId = currentActiveSchedule?.veterinarianId ?? currentActiveSchedule?.assignedVetId ?? null;
    const assignedElsewhere = Boolean(assignedVetId && assignedVetId !== user?.userId);
    // Requirement: Disable Start for REQUESTED schedules; only allow starting when SCHEDULED
    const canStartExam = Boolean(currentActiveSchedule && currentActiveSchedule.status === 'SCHEDULED' && assignedVetId === user?.userId);
    const canCompleteExam = Boolean(currentActiveSchedule && [
        'SCHEDULED',
        'IN_PROGRESS'
    ].includes(currentActiveSchedule.status) && assignedVetId === user?.userId);
    const latestMetrics = metrics[0] ?? null;
    const selectedHistory = (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$compiled$2f$react$2f$index$2e$js__$5b$app$2d$client$5d$__$28$ecmascript$29$__["useMemo"])({
        "VetAdmissionQueue.useMemo[selectedHistory]": ()=>{
            const list = [];
            careSchedules.filter({
                "VetAdmissionQueue.useMemo[selectedHistory]": (cs)=>cs.admissionId === selectedId
            }["VetAdmissionQueue.useMemo[selectedHistory]"]).forEach({
                "VetAdmissionQueue.useMemo[selectedHistory]": (cs)=>{
                    list.push({
                        id: cs.id,
                        label: `${formatLabel(cs.careType)} Care Schedule #${cs.id}`,
                        status: cs.status,
                        description: cs.description || `${formatLabel(cs.careType)} care schedule`,
                        scheduledAt: cs.scheduledAt || cs.scheduledDate || null,
                        assignedVetId: cs.veterinarianId ?? cs.assignedVetId ?? null,
                        createdAt: cs.createdAt
                    });
                }
            }["VetAdmissionQueue.useMemo[selectedHistory]"]);
            return list.sort({
                "VetAdmissionQueue.useMemo[selectedHistory]": (a, b)=>new Date(a.createdAt || 0).getTime() - new Date(b.createdAt || 0).getTime()
            }["VetAdmissionQueue.useMemo[selectedHistory]"]);
        }
    }["VetAdmissionQueue.useMemo[selectedHistory]"], [
        careSchedules,
        selectedId,
        detail
    ]);
    const handleOpenAdmission = (id, schedule)=>{
        setTargetScheduleId(schedule?.id ?? null);
        setDeepLinkedSchedule(null);
        setSelectedId(id);
        setActiveTab('exam');
        setScheduleWarning('');
        setIsDrawerDirty(false);
        setDrawerOpen(true);
    };
    const handleDirtyChange = (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$compiled$2f$react$2f$index$2e$js__$5b$app$2d$client$5d$__$28$ecmascript$29$__["useCallback"])({
        "VetAdmissionQueue.useCallback[handleDirtyChange]": (dirty)=>{
            setIsDrawerDirty(dirty);
        }
    }["VetAdmissionQueue.useCallback[handleDirtyChange]"], []);
    const handleRequestCloseDrawer = (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$compiled$2f$react$2f$index$2e$js__$5b$app$2d$client$5d$__$28$ecmascript$29$__["useCallback"])({
        "VetAdmissionQueue.useCallback[handleRequestCloseDrawer]": ()=>{
            if (isDrawerDirty) {
                setShowCloseConfirm(true);
            } else {
                setDrawerOpen(false);
                setSelectedId(null);
                setTargetScheduleId(null);
                setDeepLinkedSchedule(null);
                router.replace('/veterinarian/admissions', {
                    scroll: false
                });
            }
        }
    }["VetAdmissionQueue.useCallback[handleRequestCloseDrawer]"], [
        isDrawerDirty,
        router
    ]);
    // Keep the full-page examination workspace aligned with the Trainer detail flow.
    (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$compiled$2f$react$2f$index$2e$js__$5b$app$2d$client$5d$__$28$ecmascript$29$__["useEffect"])({
        "VetAdmissionQueue.useEffect": ()=>{
            if (!drawerOpen) return;
            window.scrollTo({
                top: 0,
                behavior: 'smooth'
            });
        }
    }["VetAdmissionQueue.useEffect"], [
        drawerOpen
    ]);
    function reloadQueue() {
        setLoading(true);
        setReload((curr)=>curr + 1);
    }
    function handleRefresh() {
        setSuccess('');
        setError('');
        setScheduleWarning('');
        reloadQueue();
    }
    async function handleStartExam() {
        if (!currentActiveSchedule || startingExam || !canStartExam) return;
        try {
            setStartingExam(true);
            setError('');
            await __TURBOPACK__imported__module__$5b$project$5d2f$src$2f$features$2f$admissions$2f$services$2f$api$2e$ts__$5b$app$2d$client$5d$__$28$ecmascript$29$__["admissionsApi"].startCareSchedule(currentActiveSchedule.id);
            setSuccess(`Examination #${currentActiveSchedule.id} started. You can now record clinical findings.`);
            setReload((curr)=>curr + 1);
        } catch (cause) {
            setError(errorText(cause));
        } finally{
            setStartingExam(false);
        }
    }
    function handleReviewSuccess(result, completionKind) {
        if (completionKind === 'INITIAL' && 'admissionId' in result) {
            setSuccess(`Admission #${result.admissionId} (${detail?.candidate?.name ?? selectedRow?.candidateName ?? 'Horse'}): clinical examination completed and forwarded to Trainer assessment.`);
        } else {
            setSuccess(`Care schedule #${currentActiveSchedule?.id ?? '—'} (${detail?.candidate?.name ?? selectedRow?.candidateName ?? 'Horse'}): examination completed without changing the Admission workflow.`);
        }
        setScheduleWarning('');
        setIsDrawerDirty(false);
        setDrawerOpen(false);
        reloadQueue();
    }
    const workspaceTabs = [
        {
            id: 'exam',
            label: 'Clinical Examination',
            icon: 'stethoscope'
        },
        {
            id: 'history',
            label: 'Medical Records',
            icon: 'clipboard',
            count: (detail?.healthRecords?.length ?? 0) > 0 ? detail?.healthRecords?.length : undefined
        },
        {
            id: 'vitals',
            label: 'Vitals History',
            icon: 'heart-pulse',
            count: metrics.length > 0 ? metrics.length : undefined
        },
        {
            id: 'documents',
            label: 'Intake & Pedigree',
            icon: 'file-text',
            count: documents.length > 0 ? documents.length : undefined
        }
    ];
    const queueColumns = [
        {
            id: 'horse',
            header: 'Horse',
            render: (row)=>/*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$compiled$2f$react$2f$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$client$5d$__$28$ecmascript$29$__["jsxDEV"])("div", {
                    className: "flex min-w-0 items-center gap-3",
                    children: [
                        /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$compiled$2f$react$2f$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$client$5d$__$28$ecmascript$29$__["jsxDEV"])(__TURBOPACK__imported__module__$5b$project$5d2f$src$2f$components$2f$ui$2f$HorseAvatar$2e$tsx__$5b$app$2d$client$5d$__$28$ecmascript$29$__["HorseAvatar"], {
                            name: row.candidateName,
                            image: row.imageUrl,
                            size: 32,
                            rounded: "md"
                        }, void 0, false, {
                            fileName: "[project]/src/features/admissions/components/VetAdmissionQueue.tsx",
                            lineNumber: 557,
                            columnNumber: 11
                        }, this),
                        /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$compiled$2f$react$2f$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$client$5d$__$28$ecmascript$29$__["jsxDEV"])("div", {
                            className: "min-w-0",
                            children: [
                                /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$compiled$2f$react$2f$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$client$5d$__$28$ecmascript$29$__["jsxDEV"])("span", {
                                    className: "block truncate font-semibold",
                                    children: row.candidateName
                                }, void 0, false, {
                                    fileName: "[project]/src/features/admissions/components/VetAdmissionQueue.tsx",
                                    lineNumber: 559,
                                    columnNumber: 13
                                }, this),
                                /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$compiled$2f$react$2f$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$client$5d$__$28$ecmascript$29$__["jsxDEV"])("span", {
                                    className: "block truncate text-xs text-[var(--color-text-muted)]",
                                    children: [
                                        "#",
                                        row.admissionId,
                                        " · ",
                                        row.breed || 'Breed not provided',
                                        " · ",
                                        calculateAge(row.dateOfBirth)
                                    ]
                                }, void 0, true, {
                                    fileName: "[project]/src/features/admissions/components/VetAdmissionQueue.tsx",
                                    lineNumber: 560,
                                    columnNumber: 13
                                }, this)
                            ]
                        }, void 0, true, {
                            fileName: "[project]/src/features/admissions/components/VetAdmissionQueue.tsx",
                            lineNumber: 558,
                            columnNumber: 11
                        }, this)
                    ]
                }, void 0, true, {
                    fileName: "[project]/src/features/admissions/components/VetAdmissionQueue.tsx",
                    lineNumber: 556,
                    columnNumber: 9
                }, this)
        },
        {
            id: 'priority',
            header: 'Priority',
            render: (row)=>{
                const priority = getPriorityBadge(row.careSchedule?.careType);
                return /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$compiled$2f$react$2f$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$client$5d$__$28$ecmascript$29$__["jsxDEV"])(__TURBOPACK__imported__module__$5b$project$5d2f$src$2f$components$2f$ui$2f$StatusBadge$2e$tsx__$5b$app$2d$client$5d$__$28$ecmascript$29$__["Pill"], {
                    tone: priority.tone,
                    size: "sm",
                    icon: priority.icon,
                    children: priority.label
                }, void 0, false, {
                    fileName: "[project]/src/features/admissions/components/VetAdmissionQueue.tsx",
                    lineNumber: 572,
                    columnNumber: 16
                }, this);
            }
        },
        {
            id: 'status',
            header: 'Status',
            render: (row)=>/*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$compiled$2f$react$2f$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$client$5d$__$28$ecmascript$29$__["jsxDEV"])(__TURBOPACK__imported__module__$5b$project$5d2f$src$2f$features$2f$admissions$2f$shared$2f$components$2f$AdmissionStatusBadge$2e$tsx__$5b$app$2d$client$5d$__$28$ecmascript$29$__["AdmissionStatusBadge"], {
                    status: row.admissionStatus,
                    size: "sm"
                }, void 0, false, {
                    fileName: "[project]/src/features/admissions/components/VetAdmissionQueue.tsx",
                    lineNumber: 578,
                    columnNumber: 24
                }, this)
        },
        {
            id: 'exam',
            header: 'Exam',
            render: (row)=>row.careSchedule ? /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$compiled$2f$react$2f$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$client$5d$__$28$ecmascript$29$__["jsxDEV"])(__TURBOPACK__imported__module__$5b$project$5d2f$src$2f$components$2f$ui$2f$StatusBadge$2e$tsx__$5b$app$2d$client$5d$__$28$ecmascript$29$__["Pill"], {
                    tone: examTone(row.careSchedule.status),
                    size: "sm",
                    icon: examIcon(row.careSchedule.status),
                    children: formatLabel(row.careSchedule.status)
                }, void 0, false, {
                    fileName: "[project]/src/features/admissions/components/VetAdmissionQueue.tsx",
                    lineNumber: 584,
                    columnNumber: 9
                }, this) : /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$compiled$2f$react$2f$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$client$5d$__$28$ecmascript$29$__["jsxDEV"])(__TURBOPACK__imported__module__$5b$project$5d2f$src$2f$components$2f$ui$2f$StatusBadge$2e$tsx__$5b$app$2d$client$5d$__$28$ecmascript$29$__["Pill"], {
                    tone: "neutral",
                    size: "sm",
                    children: "No schedule"
                }, void 0, false, {
                    fileName: "[project]/src/features/admissions/components/VetAdmissionQueue.tsx",
                    lineNumber: 588,
                    columnNumber: 9
                }, this)
        },
        {
            id: 'stall',
            header: 'Stall',
            render: (row)=>stallCodes[row.admissionId] || row.quarantineStallCode || (row.quarantineStallId ? `Q-Stall #${row.quarantineStallId}` : 'Quarantine')
        },
        {
            id: 'scheduled',
            header: 'Scheduled',
            className: 'whitespace-nowrap',
            render: (row)=>row.careSchedule?.scheduledAt ? /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$compiled$2f$react$2f$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$client$5d$__$28$ecmascript$29$__["jsxDEV"])("span", {
                    className: "font-metric",
                    children: formatDate(row.careSchedule.scheduledAt, true)
                }, void 0, false, {
                    fileName: "[project]/src/features/admissions/components/VetAdmissionQueue.tsx",
                    lineNumber: 601,
                    columnNumber: 9
                }, this) : row.careSchedule?.scheduledDate ? /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$compiled$2f$react$2f$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$client$5d$__$28$ecmascript$29$__["jsxDEV"])("span", {
                    className: "font-metric text-[var(--color-text-muted)]",
                    children: [
                        "Target: ",
                        formatDate(row.careSchedule.scheduledDate)
                    ]
                }, void 0, true, {
                    fileName: "[project]/src/features/admissions/components/VetAdmissionQueue.tsx",
                    lineNumber: 603,
                    columnNumber: 9
                }, this) : /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$compiled$2f$react$2f$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$client$5d$__$28$ecmascript$29$__["jsxDEV"])("span", {
                    className: "italic text-[var(--color-text-muted)]",
                    children: "Awaiting slot"
                }, void 0, false, {
                    fileName: "[project]/src/features/admissions/components/VetAdmissionQueue.tsx",
                    lineNumber: 605,
                    columnNumber: 9
                }, this)
        },
        {
            id: 'vet',
            header: 'Veterinarian',
            className: 'whitespace-nowrap',
            render: (row)=>{
                const rowVetId = row.careSchedule?.veterinarianId ?? row.careSchedule?.assignedVetId ?? null;
                if (rowVetId && user?.userId && rowVetId === user.userId) {
                    return /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$compiled$2f$react$2f$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$client$5d$__$28$ecmascript$29$__["jsxDEV"])("span", {
                        className: "font-semibold text-[var(--color-primary)]",
                        children: "Assigned to you"
                    }, void 0, false, {
                        fileName: "[project]/src/features/admissions/components/VetAdmissionQueue.tsx",
                        lineNumber: 615,
                        columnNumber: 18
                    }, this);
                }
                return rowVetId ? `Vet #${rowVetId}` : /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$compiled$2f$react$2f$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$client$5d$__$28$ecmascript$29$__["jsxDEV"])("span", {
                    className: "text-[var(--color-text-muted)]",
                    children: "Unassigned"
                }, void 0, false, {
                    fileName: "[project]/src/features/admissions/components/VetAdmissionQueue.tsx",
                    lineNumber: 617,
                    columnNumber: 48
                }, this);
            }
        },
        {
            id: 'action',
            header: 'Action',
            align: 'right',
            render: (row)=>{
                const status = row.careSchedule?.status;
                const action = status === 'IN_PROGRESS' ? {
                    label: 'Continue',
                    icon: 'activity',
                    primary: true
                } : status === 'SCHEDULED' ? {
                    label: 'Examine',
                    icon: 'stethoscope',
                    primary: true
                } : status === 'COMPLETED' ? {
                    label: 'View results',
                    icon: 'check',
                    primary: false
                } : {
                    label: 'Details',
                    icon: 'chevron-right',
                    primary: false
                };
                return /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$compiled$2f$react$2f$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$client$5d$__$28$ecmascript$29$__["jsxDEV"])(__TURBOPACK__imported__module__$5b$project$5d2f$src$2f$components$2f$ui$2f$Button$2e$tsx__$5b$app$2d$client$5d$__$28$ecmascript$29$__["Button"], {
                    size: "sm",
                    variant: action.primary ? 'primary' : 'secondary',
                    icon: action.icon,
                    className: "min-w-[6.5rem]",
                    onClick: ()=>handleOpenAdmission(row.admissionId, row.careSchedule),
                    children: action.label
                }, void 0, false, {
                    fileName: "[project]/src/features/admissions/components/VetAdmissionQueue.tsx",
                    lineNumber: 632,
                    columnNumber: 11
                }, this);
            }
        }
    ];
    const examDetailsCard = detail ? /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$compiled$2f$react$2f$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$client$5d$__$28$ecmascript$29$__["jsxDEV"])(__TURBOPACK__imported__module__$5b$project$5d2f$src$2f$features$2f$admissions$2f$shared$2f$components$2f$AdmissionInfoSection$2e$tsx__$5b$app$2d$client$5d$__$28$ecmascript$29$__["AdmissionSideCard"], {
        title: "Examination details",
        children: [
            /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$compiled$2f$react$2f$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$client$5d$__$28$ecmascript$29$__["jsxDEV"])(__TURBOPACK__imported__module__$5b$project$5d2f$src$2f$features$2f$admissions$2f$shared$2f$components$2f$AdmissionInfoSection$2e$tsx__$5b$app$2d$client$5d$__$28$ecmascript$29$__["InfoRow"], {
                label: "Priority",
                value: selectedRow ? getPriorityBadge(selectedRow.careSchedule?.careType).label : 'Standard'
            }, void 0, false, {
                fileName: "[project]/src/features/admissions/components/VetAdmissionQueue.tsx",
                lineNumber: 648,
                columnNumber: 7
            }, this),
            /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$compiled$2f$react$2f$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$client$5d$__$28$ecmascript$29$__["jsxDEV"])(__TURBOPACK__imported__module__$5b$project$5d2f$src$2f$features$2f$admissions$2f$shared$2f$components$2f$AdmissionInfoSection$2e$tsx__$5b$app$2d$client$5d$__$28$ecmascript$29$__["InfoRow"], {
                label: "Schedule",
                value: currentActiveSchedule ? formatLabel(currentActiveSchedule.status) : 'Not scheduled'
            }, void 0, false, {
                fileName: "[project]/src/features/admissions/components/VetAdmissionQueue.tsx",
                lineNumber: 649,
                columnNumber: 7
            }, this),
            /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$compiled$2f$react$2f$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$client$5d$__$28$ecmascript$29$__["jsxDEV"])(__TURBOPACK__imported__module__$5b$project$5d2f$src$2f$features$2f$admissions$2f$shared$2f$components$2f$AdmissionInfoSection$2e$tsx__$5b$app$2d$client$5d$__$28$ecmascript$29$__["InfoRow"], {
                label: "Quarantine stall",
                value: detail.quarantineStallCode || 'Not assigned'
            }, void 0, false, {
                fileName: "[project]/src/features/admissions/components/VetAdmissionQueue.tsx",
                lineNumber: 650,
                columnNumber: 7
            }, this),
            /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$compiled$2f$react$2f$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$client$5d$__$28$ecmascript$29$__["jsxDEV"])(__TURBOPACK__imported__module__$5b$project$5d2f$src$2f$features$2f$admissions$2f$shared$2f$components$2f$AdmissionInfoSection$2e$tsx__$5b$app$2d$client$5d$__$28$ecmascript$29$__["InfoRow"], {
                label: "Trainer",
                value: detail.trainerName || (detail.trainerId ? `Trainer #${detail.trainerId}` : 'Pending assignment')
            }, void 0, false, {
                fileName: "[project]/src/features/admissions/components/VetAdmissionQueue.tsx",
                lineNumber: 651,
                columnNumber: 7
            }, this),
            currentActiveSchedule?.scheduledAt && /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$compiled$2f$react$2f$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$client$5d$__$28$ecmascript$29$__["jsxDEV"])(__TURBOPACK__imported__module__$5b$project$5d2f$src$2f$features$2f$admissions$2f$shared$2f$components$2f$AdmissionInfoSection$2e$tsx__$5b$app$2d$client$5d$__$28$ecmascript$29$__["InfoRow"], {
                label: "Scheduled",
                value: formatDate(currentActiveSchedule.scheduledAt, true)
            }, void 0, false, {
                fileName: "[project]/src/features/admissions/components/VetAdmissionQueue.tsx",
                lineNumber: 652,
                columnNumber: 46
            }, this),
            canStartExam && /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$compiled$2f$react$2f$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$client$5d$__$28$ecmascript$29$__["jsxDEV"])(__TURBOPACK__imported__module__$5b$project$5d2f$src$2f$components$2f$ui$2f$Button$2e$tsx__$5b$app$2d$client$5d$__$28$ecmascript$29$__["Button"], {
                type: "button",
                className: "w-full",
                variant: "primary",
                loading: startingExam,
                onClick: handleStartExam,
                icon: "activity",
                children: "Start examination"
            }, void 0, false, {
                fileName: "[project]/src/features/admissions/components/VetAdmissionQueue.tsx",
                lineNumber: 654,
                columnNumber: 9
            }, this)
        ]
    }, void 0, true, {
        fileName: "[project]/src/features/admissions/components/VetAdmissionQueue.tsx",
        lineNumber: 647,
        columnNumber: 5
    }, this) : null;
    return /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$compiled$2f$react$2f$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$client$5d$__$28$ecmascript$29$__["jsxDEV"])(QueueShell, {
        showHeader: !drawerOpen,
        children: [
            success && /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$compiled$2f$react$2f$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$client$5d$__$28$ecmascript$29$__["jsxDEV"])(__TURBOPACK__imported__module__$5b$project$5d2f$src$2f$components$2f$ui$2f$Notice$2e$tsx__$5b$app$2d$client$5d$__$28$ecmascript$29$__["Notice"], {
                tone: "success",
                onDismiss: ()=>setSuccess(''),
                children: success
            }, void 0, false, {
                fileName: "[project]/src/features/admissions/components/VetAdmissionQueue.tsx",
                lineNumber: 663,
                columnNumber: 19
            }, this),
            error && /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$compiled$2f$react$2f$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$client$5d$__$28$ecmascript$29$__["jsxDEV"])(__TURBOPACK__imported__module__$5b$project$5d2f$src$2f$components$2f$ui$2f$Notice$2e$tsx__$5b$app$2d$client$5d$__$28$ecmascript$29$__["Notice"], {
                tone: "error",
                onDismiss: ()=>setError(''),
                children: error
            }, void 0, false, {
                fileName: "[project]/src/features/admissions/components/VetAdmissionQueue.tsx",
                lineNumber: 664,
                columnNumber: 17
            }, this),
            scheduleWarning && /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$compiled$2f$react$2f$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$client$5d$__$28$ecmascript$29$__["jsxDEV"])(__TURBOPACK__imported__module__$5b$project$5d2f$src$2f$components$2f$ui$2f$Notice$2e$tsx__$5b$app$2d$client$5d$__$28$ecmascript$29$__["Notice"], {
                tone: "warning",
                onDismiss: ()=>setScheduleWarning(''),
                children: scheduleWarning
            }, void 0, false, {
                fileName: "[project]/src/features/admissions/components/VetAdmissionQueue.tsx",
                lineNumber: 665,
                columnNumber: 27
            }, this),
            !drawerOpen && /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$compiled$2f$react$2f$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$client$5d$__$28$ecmascript$29$__["jsxDEV"])(__TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$compiled$2f$react$2f$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$client$5d$__$28$ecmascript$29$__["Fragment"], {
                children: [
                    /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$compiled$2f$react$2f$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$client$5d$__$28$ecmascript$29$__["jsxDEV"])("div", {
                        className: "flex flex-wrap items-center justify-between gap-3",
                        children: [
                            /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$compiled$2f$react$2f$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$client$5d$__$28$ecmascript$29$__["jsxDEV"])(__TURBOPACK__imported__module__$5b$project$5d2f$src$2f$components$2f$ui$2f$SegmentedControl$2e$tsx__$5b$app$2d$client$5d$__$28$ecmascript$29$__["FilterChips"], {
                                label: "Queue",
                                value: queuePillFilter,
                                onChange: (value)=>{
                                    setQueuePillFilter(value);
                                    setPage(0);
                                },
                                options: [
                                    {
                                        value: 'ALL',
                                        label: 'Active',
                                        count: summary.total
                                    },
                                    {
                                        value: 'AWAITING',
                                        label: 'Awaiting',
                                        count: summary.awaiting
                                    },
                                    {
                                        value: 'IN_PROGRESS',
                                        label: 'In exam',
                                        count: summary.inProgress
                                    }
                                ]
                            }, void 0, false, {
                                fileName: "[project]/src/features/admissions/components/VetAdmissionQueue.tsx",
                                lineNumber: 670,
                                columnNumber: 9
                            }, this),
                            /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$compiled$2f$react$2f$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$client$5d$__$28$ecmascript$29$__["jsxDEV"])(__TURBOPACK__imported__module__$5b$project$5d2f$src$2f$components$2f$ui$2f$Button$2e$tsx__$5b$app$2d$client$5d$__$28$ecmascript$29$__["Button"], {
                                type: "button",
                                onClick: handleRefresh,
                                disabled: loading,
                                icon: "refresh",
                                variant: "secondary",
                                size: "sm",
                                children: "Refresh"
                            }, void 0, false, {
                                fileName: "[project]/src/features/admissions/components/VetAdmissionQueue.tsx",
                                lineNumber: 683,
                                columnNumber: 9
                            }, this)
                        ]
                    }, void 0, true, {
                        fileName: "[project]/src/features/admissions/components/VetAdmissionQueue.tsx",
                        lineNumber: 669,
                        columnNumber: 7
                    }, this),
                    /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$compiled$2f$react$2f$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$client$5d$__$28$ecmascript$29$__["jsxDEV"])(__TURBOPACK__imported__module__$5b$project$5d2f$src$2f$components$2f$ui$2f$FilterBar$2e$tsx__$5b$app$2d$client$5d$__$28$ecmascript$29$__["FilterBar"], {
                        className: "[&_input]:min-h-9 [&_select]:min-h-9",
                        onSubmit: (event)=>event.preventDefault(),
                        children: [
                            /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$compiled$2f$react$2f$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$client$5d$__$28$ecmascript$29$__["jsxDEV"])(__TURBOPACK__imported__module__$5b$project$5d2f$src$2f$components$2f$ui$2f$FormField$2e$tsx__$5b$app$2d$client$5d$__$28$ecmascript$29$__["FormField"], {
                                label: "Search admissions",
                                className: "min-w-[14rem] flex-[1.4]",
                                children: /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$compiled$2f$react$2f$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$client$5d$__$28$ecmascript$29$__["jsxDEV"])(__TURBOPACK__imported__module__$5b$project$5d2f$src$2f$components$2f$ui$2f$Input$2e$tsx__$5b$app$2d$client$5d$__$28$ecmascript$29$__["SearchInput"], {
                                    label: "Search admissions",
                                    placeholder: "Horse name, admission number, or breed",
                                    value: searchQuery,
                                    onChange: (value)=>{
                                        setSearchQuery(value);
                                        setPage(0);
                                    }
                                }, void 0, false, {
                                    fileName: "[project]/src/features/admissions/components/VetAdmissionQueue.tsx",
                                    lineNumber: 690,
                                    columnNumber: 11
                                }, this)
                            }, void 0, false, {
                                fileName: "[project]/src/features/admissions/components/VetAdmissionQueue.tsx",
                                lineNumber: 689,
                                columnNumber: 9
                            }, this),
                            /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$compiled$2f$react$2f$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$client$5d$__$28$ecmascript$29$__["jsxDEV"])(__TURBOPACK__imported__module__$5b$project$5d2f$src$2f$components$2f$ui$2f$FormField$2e$tsx__$5b$app$2d$client$5d$__$28$ecmascript$29$__["FormField"], {
                                label: "Stage",
                                className: "min-w-[9rem] flex-1",
                                children: /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$compiled$2f$react$2f$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$client$5d$__$28$ecmascript$29$__["jsxDEV"])(__TURBOPACK__imported__module__$5b$project$5d2f$src$2f$components$2f$ui$2f$Input$2e$tsx__$5b$app$2d$client$5d$__$28$ecmascript$29$__["Select"], {
                                    value: admissionFilter,
                                    onChange: (e)=>{
                                        setAdmissionFilter(e.target.value);
                                        setPage(0);
                                    },
                                    children: [
                                        /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$compiled$2f$react$2f$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$client$5d$__$28$ecmascript$29$__["jsxDEV"])("option", {
                                            value: "ALL",
                                            children: "All stages"
                                        }, void 0, false, {
                                            fileName: "[project]/src/features/admissions/components/VetAdmissionQueue.tsx",
                                            lineNumber: 708,
                                            columnNumber: 13
                                        }, this),
                                        /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$compiled$2f$react$2f$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$client$5d$__$28$ecmascript$29$__["jsxDEV"])("option", {
                                            value: "VET_REVIEW",
                                            children: "Initial review"
                                        }, void 0, false, {
                                            fileName: "[project]/src/features/admissions/components/VetAdmissionQueue.tsx",
                                            lineNumber: 709,
                                            columnNumber: 13
                                        }, this)
                                    ]
                                }, void 0, true, {
                                    fileName: "[project]/src/features/admissions/components/VetAdmissionQueue.tsx",
                                    lineNumber: 701,
                                    columnNumber: 11
                                }, this)
                            }, void 0, false, {
                                fileName: "[project]/src/features/admissions/components/VetAdmissionQueue.tsx",
                                lineNumber: 700,
                                columnNumber: 9
                            }, this),
                            /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$compiled$2f$react$2f$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$client$5d$__$28$ecmascript$29$__["jsxDEV"])(__TURBOPACK__imported__module__$5b$project$5d2f$src$2f$components$2f$ui$2f$FormField$2e$tsx__$5b$app$2d$client$5d$__$28$ecmascript$29$__["FormField"], {
                                label: "Exam status",
                                className: "min-w-[9rem] flex-1",
                                children: /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$compiled$2f$react$2f$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$client$5d$__$28$ecmascript$29$__["jsxDEV"])(__TURBOPACK__imported__module__$5b$project$5d2f$src$2f$components$2f$ui$2f$Input$2e$tsx__$5b$app$2d$client$5d$__$28$ecmascript$29$__["Select"], {
                                    value: examStatusFilter,
                                    onChange: (e)=>{
                                        setExamStatusFilter(e.target.value);
                                        setPage(0);
                                    },
                                    children: examStatusOptions.map((opt)=>/*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$compiled$2f$react$2f$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$client$5d$__$28$ecmascript$29$__["jsxDEV"])("option", {
                                            value: opt.value,
                                            children: opt.label
                                        }, opt.value, false, {
                                            fileName: "[project]/src/features/admissions/components/VetAdmissionQueue.tsx",
                                            lineNumber: 721,
                                            columnNumber: 15
                                        }, this))
                                }, void 0, false, {
                                    fileName: "[project]/src/features/admissions/components/VetAdmissionQueue.tsx",
                                    lineNumber: 713,
                                    columnNumber: 11
                                }, this)
                            }, void 0, false, {
                                fileName: "[project]/src/features/admissions/components/VetAdmissionQueue.tsx",
                                lineNumber: 712,
                                columnNumber: 9
                            }, this),
                            /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$compiled$2f$react$2f$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$client$5d$__$28$ecmascript$29$__["jsxDEV"])(__TURBOPACK__imported__module__$5b$project$5d2f$src$2f$components$2f$ui$2f$FormField$2e$tsx__$5b$app$2d$client$5d$__$28$ecmascript$29$__["FormField"], {
                                label: "Exam type",
                                className: "min-w-[9rem] flex-1",
                                children: /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$compiled$2f$react$2f$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$client$5d$__$28$ecmascript$29$__["jsxDEV"])(__TURBOPACK__imported__module__$5b$project$5d2f$src$2f$components$2f$ui$2f$Input$2e$tsx__$5b$app$2d$client$5d$__$28$ecmascript$29$__["Select"], {
                                    value: examTypeFilter,
                                    onChange: (e)=>{
                                        setExamTypeFilter(e.target.value);
                                        setPage(0);
                                    },
                                    children: examTypeOptions.map((opt)=>/*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$compiled$2f$react$2f$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$client$5d$__$28$ecmascript$29$__["jsxDEV"])("option", {
                                            value: opt.value,
                                            children: opt.label
                                        }, opt.value, false, {
                                            fileName: "[project]/src/features/admissions/components/VetAdmissionQueue.tsx",
                                            lineNumber: 734,
                                            columnNumber: 15
                                        }, this))
                                }, void 0, false, {
                                    fileName: "[project]/src/features/admissions/components/VetAdmissionQueue.tsx",
                                    lineNumber: 726,
                                    columnNumber: 11
                                }, this)
                            }, void 0, false, {
                                fileName: "[project]/src/features/admissions/components/VetAdmissionQueue.tsx",
                                lineNumber: 725,
                                columnNumber: 9
                            }, this),
                            /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$compiled$2f$react$2f$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$client$5d$__$28$ecmascript$29$__["jsxDEV"])(__TURBOPACK__imported__module__$5b$project$5d2f$src$2f$components$2f$ui$2f$FormField$2e$tsx__$5b$app$2d$client$5d$__$28$ecmascript$29$__["FormField"], {
                                label: "Priority",
                                className: "min-w-[9rem] flex-1",
                                children: /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$compiled$2f$react$2f$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$client$5d$__$28$ecmascript$29$__["jsxDEV"])(__TURBOPACK__imported__module__$5b$project$5d2f$src$2f$components$2f$ui$2f$Input$2e$tsx__$5b$app$2d$client$5d$__$28$ecmascript$29$__["Select"], {
                                    value: priorityFilter,
                                    onChange: (e)=>{
                                        setPriorityFilter(e.target.value);
                                        setPage(0);
                                    },
                                    children: [
                                        /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$compiled$2f$react$2f$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$client$5d$__$28$ecmascript$29$__["jsxDEV"])("option", {
                                            value: "ALL",
                                            children: "All priorities"
                                        }, void 0, false, {
                                            fileName: "[project]/src/features/admissions/components/VetAdmissionQueue.tsx",
                                            lineNumber: 746,
                                            columnNumber: 13
                                        }, this),
                                        /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$compiled$2f$react$2f$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$client$5d$__$28$ecmascript$29$__["jsxDEV"])("option", {
                                            value: "URGENT",
                                            children: "Urgent (P1)"
                                        }, void 0, false, {
                                            fileName: "[project]/src/features/admissions/components/VetAdmissionQueue.tsx",
                                            lineNumber: 747,
                                            columnNumber: 13
                                        }, this),
                                        /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$compiled$2f$react$2f$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$client$5d$__$28$ecmascript$29$__["jsxDEV"])("option", {
                                            value: "NORMAL",
                                            children: "Standard (P2-P4)"
                                        }, void 0, false, {
                                            fileName: "[project]/src/features/admissions/components/VetAdmissionQueue.tsx",
                                            lineNumber: 748,
                                            columnNumber: 13
                                        }, this)
                                    ]
                                }, void 0, true, {
                                    fileName: "[project]/src/features/admissions/components/VetAdmissionQueue.tsx",
                                    lineNumber: 739,
                                    columnNumber: 11
                                }, this)
                            }, void 0, false, {
                                fileName: "[project]/src/features/admissions/components/VetAdmissionQueue.tsx",
                                lineNumber: 738,
                                columnNumber: 9
                            }, this),
                            /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$compiled$2f$react$2f$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$client$5d$__$28$ecmascript$29$__["jsxDEV"])(__TURBOPACK__imported__module__$5b$project$5d2f$src$2f$components$2f$ui$2f$Button$2e$tsx__$5b$app$2d$client$5d$__$28$ecmascript$29$__["Button"], {
                                type: "button",
                                variant: "secondary",
                                onClick: resetFilters,
                                disabled: !hasActiveFilters,
                                children: "Clear"
                            }, void 0, false, {
                                fileName: "[project]/src/features/admissions/components/VetAdmissionQueue.tsx",
                                lineNumber: 751,
                                columnNumber: 9
                            }, this)
                        ]
                    }, void 0, true, {
                        fileName: "[project]/src/features/admissions/components/VetAdmissionQueue.tsx",
                        lineNumber: 688,
                        columnNumber: 7
                    }, this),
                    /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$compiled$2f$react$2f$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$client$5d$__$28$ecmascript$29$__["jsxDEV"])(__TURBOPACK__imported__module__$5b$project$5d2f$src$2f$components$2f$ui$2f$DataTable$2e$tsx__$5b$app$2d$client$5d$__$28$ecmascript$29$__["DataTable"], {
                        rows: items,
                        columns: queueColumns,
                        getRowKey: (row)=>row.careSchedule?.id ? `schedule-${row.careSchedule.id}` : `admission-${row.admissionId}`,
                        ariaLabel: "Veterinary admission queue",
                        loading: loading,
                        emptyTitle: "No matching admissions found",
                        emptyDescription: "No candidates match your current search query or active filter settings.",
                        emptyAction: /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$compiled$2f$react$2f$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$client$5d$__$28$ecmascript$29$__["jsxDEV"])(__TURBOPACK__imported__module__$5b$project$5d2f$src$2f$components$2f$ui$2f$Button$2e$tsx__$5b$app$2d$client$5d$__$28$ecmascript$29$__["Button"], {
                            size: "sm",
                            variant: "secondary",
                            onClick: resetFilters,
                            children: "Clear filters"
                        }, void 0, false, {
                            fileName: "[project]/src/features/admissions/components/VetAdmissionQueue.tsx",
                            lineNumber: 764,
                            columnNumber: 22
                        }, this),
                        pagination: {
                            page,
                            pageSize,
                            total: totalElements,
                            onPageChange: setPage,
                            disabled: loading
                        }
                    }, void 0, false, {
                        fileName: "[project]/src/features/admissions/components/VetAdmissionQueue.tsx",
                        lineNumber: 756,
                        columnNumber: 7
                    }, this)
                ]
            }, void 0, true, {
                fileName: "[project]/src/features/admissions/components/VetAdmissionQueue.tsx",
                lineNumber: 668,
                columnNumber: 9
            }, this),
            drawerOpen && (selectedId !== null || targetScheduleId !== null) && /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$compiled$2f$react$2f$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$client$5d$__$28$ecmascript$29$__["jsxDEV"])(__TURBOPACK__imported__module__$5b$project$5d2f$src$2f$features$2f$admissions$2f$shared$2f$components$2f$AdmissionDetailLayout$2e$tsx__$5b$app$2d$client$5d$__$28$ecmascript$29$__["AdmissionDetailLayout"], {
                onBack: handleRequestCloseDrawer,
                header: detail ? /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$compiled$2f$react$2f$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$client$5d$__$28$ecmascript$29$__["jsxDEV"])(__TURBOPACK__imported__module__$5b$project$5d2f$src$2f$features$2f$admissions$2f$shared$2f$components$2f$AdmissionDetailHeader$2e$tsx__$5b$app$2d$client$5d$__$28$ecmascript$29$__["AdmissionDetailHeader"], {
                    detail: detail,
                    horsePhotoUrl: detail.documents.find((doc)=>doc.documentType === 'HORSE_PHOTO') ? __TURBOPACK__imported__module__$5b$project$5d2f$src$2f$features$2f$admissions$2f$services$2f$api$2e$ts__$5b$app$2d$client$5d$__$28$ecmascript$29$__["admissionsApi"].assetUrl(detail.documents.find((doc)=>doc.documentType === 'HORSE_PHOTO').fileUrl) : undefined
                }, void 0, false, {
                    fileName: "[project]/src/features/admissions/components/VetAdmissionQueue.tsx",
                    lineNumber: 775,
                    columnNumber: 13
                }, this) : /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$compiled$2f$react$2f$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$client$5d$__$28$ecmascript$29$__["jsxDEV"])("div", {
                    className: "border-b border-[var(--color-border)] bg-[var(--color-surface)] p-4 text-sm font-semibold text-[var(--color-text-primary)]",
                    children: selectedRow?.candidateName ?? 'Loading admission'
                }, void 0, false, {
                    fileName: "[project]/src/features/admissions/components/VetAdmissionQueue.tsx",
                    lineNumber: 782,
                    columnNumber: 13
                }, this),
                pipeline: detail ? /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$compiled$2f$react$2f$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$client$5d$__$28$ecmascript$29$__["jsxDEV"])(__TURBOPACK__imported__module__$5b$project$5d2f$src$2f$features$2f$admissions$2f$shared$2f$components$2f$AdmissionPipeline$2e$tsx__$5b$app$2d$client$5d$__$28$ecmascript$29$__["AdmissionPipeline"], {
                    detail: detail
                }, void 0, false, {
                    fileName: "[project]/src/features/admissions/components/VetAdmissionQueue.tsx",
                    lineNumber: 786,
                    columnNumber: 30
                }, this) : /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$compiled$2f$react$2f$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$client$5d$__$28$ecmascript$29$__["jsxDEV"])(__TURBOPACK__imported__module__$5b$project$5d2f$src$2f$components$2f$ui$2f$states$2e$tsx__$5b$app$2d$client$5d$__$28$ecmascript$29$__["DetailSkeleton"], {}, void 0, false, {
                    fileName: "[project]/src/features/admissions/components/VetAdmissionQueue.tsx",
                    lineNumber: 786,
                    columnNumber: 70
                }, this),
                content: /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$compiled$2f$react$2f$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$client$5d$__$28$ecmascript$29$__["jsxDEV"])("div", {
                    className: "min-w-0 space-y-5",
                    children: [
                        /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$compiled$2f$react$2f$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$client$5d$__$28$ecmascript$29$__["jsxDEV"])(__TURBOPACK__imported__module__$5b$project$5d2f$src$2f$components$2f$ui$2f$Tabs$2e$tsx__$5b$app$2d$client$5d$__$28$ecmascript$29$__["Tabs"], {
                            tabs: workspaceTabs,
                            active: activeTab,
                            onChange: setActiveTab
                        }, void 0, false, {
                            fileName: "[project]/src/features/admissions/components/VetAdmissionQueue.tsx",
                            lineNumber: 789,
                            columnNumber: 15
                        }, this),
                        loadingDetail || !detail && !detailError ? /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$compiled$2f$react$2f$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$client$5d$__$28$ecmascript$29$__["jsxDEV"])(__TURBOPACK__imported__module__$5b$project$5d2f$src$2f$components$2f$ui$2f$states$2e$tsx__$5b$app$2d$client$5d$__$28$ecmascript$29$__["DetailSkeleton"], {}, void 0, false, {
                            fileName: "[project]/src/features/admissions/components/VetAdmissionQueue.tsx",
                            lineNumber: 791,
                            columnNumber: 17
                        }, this) : detailError ? /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$compiled$2f$react$2f$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$client$5d$__$28$ecmascript$29$__["jsxDEV"])(__TURBOPACK__imported__module__$5b$project$5d2f$src$2f$components$2f$ui$2f$states$2e$tsx__$5b$app$2d$client$5d$__$28$ecmascript$29$__["EmptyState"], {
                            icon: "alert-triangle",
                            title: "Could not load examination record",
                            description: detailError,
                            action: selectedId === null ? /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$compiled$2f$react$2f$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$client$5d$__$28$ecmascript$29$__["jsxDEV"])(__TURBOPACK__imported__module__$5b$project$5d2f$src$2f$components$2f$ui$2f$Button$2e$tsx__$5b$app$2d$client$5d$__$28$ecmascript$29$__["Button"], {
                                onClick: handleRequestCloseDrawer,
                                children: "Close"
                            }, void 0, false, {
                                fileName: "[project]/src/features/admissions/components/VetAdmissionQueue.tsx",
                                lineNumber: 799,
                                columnNumber: 25
                            }, this) : /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$compiled$2f$react$2f$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$client$5d$__$28$ecmascript$29$__["jsxDEV"])(__TURBOPACK__imported__module__$5b$project$5d2f$src$2f$components$2f$ui$2f$Button$2e$tsx__$5b$app$2d$client$5d$__$28$ecmascript$29$__["Button"], {
                                onClick: handleRefresh,
                                children: "Retry"
                            }, void 0, false, {
                                fileName: "[project]/src/features/admissions/components/VetAdmissionQueue.tsx",
                                lineNumber: 800,
                                columnNumber: 25
                            }, this)
                        }, void 0, false, {
                            fileName: "[project]/src/features/admissions/components/VetAdmissionQueue.tsx",
                            lineNumber: 793,
                            columnNumber: 17
                        }, this) : detail ? /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$compiled$2f$react$2f$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$client$5d$__$28$ecmascript$29$__["jsxDEV"])("div", {
                            children: [
                                /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$compiled$2f$react$2f$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$client$5d$__$28$ecmascript$29$__["jsxDEV"])("div", {
                                    className: activeTab === 'exam' ? 'block' : 'hidden',
                                    children: canCompleteExam ? /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$compiled$2f$react$2f$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$client$5d$__$28$ecmascript$29$__["jsxDEV"])(__TURBOPACK__imported__module__$5b$project$5d2f$src$2f$features$2f$admissions$2f$components$2f$VetReviewForm$2e$tsx__$5b$app$2d$client$5d$__$28$ecmascript$29$__["VetReviewForm"], {
                                        admissionId: detail.admissionId,
                                        horseId: detail.horseId,
                                        candidateName: detail.candidate?.name ?? selectedRow?.candidateName ?? 'Candidate',
                                        quarantineStallCode: detail.quarantineStallCode,
                                        careScheduleId: currentActiveSchedule?.id,
                                        careType: currentActiveSchedule?.careType,
                                        scheduleStatus: currentActiveSchedule?.status,
                                        onStartExam: handleStartExam,
                                        asideTop: examDetailsCard,
                                        onSuccess: handleReviewSuccess,
                                        onDirtyChange: handleDirtyChange
                                    }, `${detail.admissionId}-${currentActiveSchedule?.id}`, false, {
                                        fileName: "[project]/src/features/admissions/components/VetAdmissionQueue.tsx",
                                        lineNumber: 809,
                                        columnNumber: 23
                                    }, this) : currentActiveSchedule?.status === 'COMPLETED' ? /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$compiled$2f$react$2f$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$client$5d$__$28$ecmascript$29$__["jsxDEV"])("div", {
                                        className: "rounded-[var(--radius-md)] border border-[var(--color-success)] bg-[var(--color-success-soft)] p-5 text-center",
                                        children: [
                                            /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$compiled$2f$react$2f$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$client$5d$__$28$ecmascript$29$__["jsxDEV"])(__TURBOPACK__imported__module__$5b$project$5d2f$src$2f$components$2f$ui$2f$Icon$2e$tsx__$5b$app$2d$client$5d$__$28$ecmascript$29$__["Icon"], {
                                                name: "check",
                                                size: 28,
                                                className: "mx-auto text-[var(--color-success)]"
                                            }, void 0, false, {
                                                fileName: "[project]/src/features/admissions/components/VetAdmissionQueue.tsx",
                                                lineNumber: 825,
                                                columnNumber: 25
                                            }, this),
                                            /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$compiled$2f$react$2f$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$client$5d$__$28$ecmascript$29$__["jsxDEV"])("h3", {
                                                className: "mt-2 text-lg font-bold text-[var(--color-success)]",
                                                children: "Examination Completed"
                                            }, void 0, false, {
                                                fileName: "[project]/src/features/admissions/components/VetAdmissionQueue.tsx",
                                                lineNumber: 826,
                                                columnNumber: 25
                                            }, this),
                                            /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$compiled$2f$react$2f$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$client$5d$__$28$ecmascript$29$__["jsxDEV"])("p", {
                                                className: "mt-1 text-sm text-[var(--color-text-secondary)]",
                                                children: currentActiveSchedule.careType === 'INITIAL' ? 'The veterinary review for this intake stage has already been submitted and finalized.' : 'This care schedule has already been completed. The Admission workflow was not changed.'
                                            }, void 0, false, {
                                                fileName: "[project]/src/features/admissions/components/VetAdmissionQueue.tsx",
                                                lineNumber: 829,
                                                columnNumber: 25
                                            }, this),
                                            displayedTrainingDecision && /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$compiled$2f$react$2f$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$client$5d$__$28$ecmascript$29$__["jsxDEV"])("div", {
                                                className: "mt-3",
                                                children: /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$compiled$2f$react$2f$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$client$5d$__$28$ecmascript$29$__["jsxDEV"])(__TURBOPACK__imported__module__$5b$project$5d2f$src$2f$components$2f$ui$2f$StatusBadge$2e$tsx__$5b$app$2d$client$5d$__$28$ecmascript$29$__["Pill"], {
                                                    tone: displayedTrainingDecision === 'ALLOWED' ? 'success' : 'danger',
                                                    children: [
                                                        "Training Decision: ",
                                                        formatLabel(displayedTrainingDecision)
                                                    ]
                                                }, void 0, true, {
                                                    fileName: "[project]/src/features/admissions/components/VetAdmissionQueue.tsx",
                                                    lineNumber: 836,
                                                    columnNumber: 29
                                                }, this)
                                            }, void 0, false, {
                                                fileName: "[project]/src/features/admissions/components/VetAdmissionQueue.tsx",
                                                lineNumber: 835,
                                                columnNumber: 27
                                            }, this)
                                        ]
                                    }, void 0, true, {
                                        fileName: "[project]/src/features/admissions/components/VetAdmissionQueue.tsx",
                                        lineNumber: 824,
                                        columnNumber: 23
                                    }, this) : /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$compiled$2f$react$2f$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$client$5d$__$28$ecmascript$29$__["jsxDEV"])("div", {
                                        className: "p-8 text-center text-[var(--color-text-muted)]",
                                        children: [
                                            /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$compiled$2f$react$2f$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$client$5d$__$28$ecmascript$29$__["jsxDEV"])(__TURBOPACK__imported__module__$5b$project$5d2f$src$2f$components$2f$ui$2f$Icon$2e$tsx__$5b$app$2d$client$5d$__$28$ecmascript$29$__["Icon"], {
                                                name: "stethoscope",
                                                size: 32,
                                                className: "mx-auto mb-2 opacity-40"
                                            }, void 0, false, {
                                                fileName: "[project]/src/features/admissions/components/VetAdmissionQueue.tsx",
                                                lineNumber: 846,
                                                columnNumber: 25
                                            }, this),
                                            /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$compiled$2f$react$2f$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$client$5d$__$28$ecmascript$29$__["jsxDEV"])("p", {
                                                className: "text-base font-semibold text-[var(--color-text-secondary)]",
                                                children: "Clinical review locked"
                                            }, void 0, false, {
                                                fileName: "[project]/src/features/admissions/components/VetAdmissionQueue.tsx",
                                                lineNumber: 847,
                                                columnNumber: 25
                                            }, this),
                                            /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$compiled$2f$react$2f$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$client$5d$__$28$ecmascript$29$__["jsxDEV"])("p", {
                                                className: "text-xs mt-1",
                                                children: currentActiveSchedule?.status === 'REQUESTED' ? 'The care schedule is waiting for automatic assignment.' : assignedElsewhere ? 'This case is assigned to another veterinarian.' : 'No active examination found for this admission.'
                                            }, void 0, false, {
                                                fileName: "[project]/src/features/admissions/components/VetAdmissionQueue.tsx",
                                                lineNumber: 850,
                                                columnNumber: 25
                                            }, this)
                                        ]
                                    }, void 0, true, {
                                        fileName: "[project]/src/features/admissions/components/VetAdmissionQueue.tsx",
                                        lineNumber: 845,
                                        columnNumber: 23
                                    }, this)
                                }, void 0, false, {
                                    fileName: "[project]/src/features/admissions/components/VetAdmissionQueue.tsx",
                                    lineNumber: 807,
                                    columnNumber: 19
                                }, this),
                                activeTab === 'history' && /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$compiled$2f$react$2f$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$client$5d$__$28$ecmascript$29$__["jsxDEV"])("div", {
                                    className: "space-y-5",
                                    children: [
                                        /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$compiled$2f$react$2f$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$client$5d$__$28$ecmascript$29$__["jsxDEV"])("div", {
                                            children: [
                                                /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$compiled$2f$react$2f$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$client$5d$__$28$ecmascript$29$__["jsxDEV"])("h3", {
                                                    className: "text-sm font-bold text-[var(--color-text-primary)]",
                                                    children: "Completed Health Records"
                                                }, void 0, false, {
                                                    fileName: "[project]/src/features/admissions/components/VetAdmissionQueue.tsx",
                                                    lineNumber: 865,
                                                    columnNumber: 25
                                                }, this),
                                                detail.healthRecords && detail.healthRecords.length > 0 ? /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$compiled$2f$react$2f$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$client$5d$__$28$ecmascript$29$__["jsxDEV"])("ul", {
                                                    className: "mt-3 space-y-3",
                                                    children: detail.healthRecords.map((record)=>/*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$compiled$2f$react$2f$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$client$5d$__$28$ecmascript$29$__["jsxDEV"])("li", {
                                                            className: "rounded-[var(--radius-md)] border border-[var(--color-border)] bg-[var(--color-surface)] p-4",
                                                            children: [
                                                                /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$compiled$2f$react$2f$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$client$5d$__$28$ecmascript$29$__["jsxDEV"])("div", {
                                                                    className: "flex flex-wrap items-center justify-between gap-2 border-b border-[var(--color-border)] pb-2.5",
                                                                    children: [
                                                                        /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$compiled$2f$react$2f$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$client$5d$__$28$ecmascript$29$__["jsxDEV"])("div", {
                                                                            className: "flex items-center gap-2",
                                                                            children: [
                                                                                /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$compiled$2f$react$2f$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$client$5d$__$28$ecmascript$29$__["jsxDEV"])("span", {
                                                                                    className: "text-sm font-bold text-[var(--color-text-primary)]",
                                                                                    children: [
                                                                                        formatLabel(record.recordType),
                                                                                        " Exam"
                                                                                    ]
                                                                                }, void 0, true, {
                                                                                    fileName: "[project]/src/features/admissions/components/VetAdmissionQueue.tsx",
                                                                                    lineNumber: 877,
                                                                                    columnNumber: 37
                                                                                }, this),
                                                                                /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$compiled$2f$react$2f$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$client$5d$__$28$ecmascript$29$__["jsxDEV"])("span", {
                                                                                    className: "font-metric text-xs text-[var(--color-text-muted)]",
                                                                                    children: [
                                                                                        "Record #",
                                                                                        record.id
                                                                                    ]
                                                                                }, void 0, true, {
                                                                                    fileName: "[project]/src/features/admissions/components/VetAdmissionQueue.tsx",
                                                                                    lineNumber: 880,
                                                                                    columnNumber: 37
                                                                                }, this)
                                                                            ]
                                                                        }, void 0, true, {
                                                                            fileName: "[project]/src/features/admissions/components/VetAdmissionQueue.tsx",
                                                                            lineNumber: 876,
                                                                            columnNumber: 35
                                                                        }, this),
                                                                        /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$compiled$2f$react$2f$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$client$5d$__$28$ecmascript$29$__["jsxDEV"])("div", {
                                                                            className: "flex items-center gap-2",
                                                                            children: /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$compiled$2f$react$2f$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$client$5d$__$28$ecmascript$29$__["jsxDEV"])("span", {
                                                                                className: "font-metric text-xs text-[var(--color-text-secondary)]",
                                                                                children: formatDate(record.examinedAt, true)
                                                                            }, void 0, false, {
                                                                                fileName: "[project]/src/features/admissions/components/VetAdmissionQueue.tsx",
                                                                                lineNumber: 885,
                                                                                columnNumber: 37
                                                                            }, this)
                                                                        }, void 0, false, {
                                                                            fileName: "[project]/src/features/admissions/components/VetAdmissionQueue.tsx",
                                                                            lineNumber: 884,
                                                                            columnNumber: 35
                                                                        }, this)
                                                                    ]
                                                                }, void 0, true, {
                                                                    fileName: "[project]/src/features/admissions/components/VetAdmissionQueue.tsx",
                                                                    lineNumber: 875,
                                                                    columnNumber: 33
                                                                }, this),
                                                                /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$compiled$2f$react$2f$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$client$5d$__$28$ecmascript$29$__["jsxDEV"])("div", {
                                                                    className: "mt-3 grid gap-3 sm:grid-cols-2",
                                                                    children: [
                                                                        /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$compiled$2f$react$2f$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$client$5d$__$28$ecmascript$29$__["jsxDEV"])(MedicalNoteBlock, {
                                                                            label: "Symptoms",
                                                                            value: record.symptoms
                                                                        }, void 0, false, {
                                                                            fileName: "[project]/src/features/admissions/components/VetAdmissionQueue.tsx",
                                                                            lineNumber: 892,
                                                                            columnNumber: 35
                                                                        }, this),
                                                                        /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$compiled$2f$react$2f$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$client$5d$__$28$ecmascript$29$__["jsxDEV"])(MedicalNoteBlock, {
                                                                            label: "Findings",
                                                                            value: record.findings
                                                                        }, void 0, false, {
                                                                            fileName: "[project]/src/features/admissions/components/VetAdmissionQueue.tsx",
                                                                            lineNumber: 893,
                                                                            columnNumber: 35
                                                                        }, this),
                                                                        /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$compiled$2f$react$2f$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$client$5d$__$28$ecmascript$29$__["jsxDEV"])(MedicalNoteBlock, {
                                                                            label: "Diagnosis",
                                                                            value: record.diagnosis
                                                                        }, void 0, false, {
                                                                            fileName: "[project]/src/features/admissions/components/VetAdmissionQueue.tsx",
                                                                            lineNumber: 894,
                                                                            columnNumber: 35
                                                                        }, this),
                                                                        /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$compiled$2f$react$2f$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$client$5d$__$28$ecmascript$29$__["jsxDEV"])(MedicalNoteBlock, {
                                                                            label: "Treatment Plan",
                                                                            value: record.treatment
                                                                        }, void 0, false, {
                                                                            fileName: "[project]/src/features/admissions/components/VetAdmissionQueue.tsx",
                                                                            lineNumber: 895,
                                                                            columnNumber: 35
                                                                        }, this),
                                                                        /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$compiled$2f$react$2f$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$client$5d$__$28$ecmascript$29$__["jsxDEV"])(MedicalNoteBlock, {
                                                                            label: "Notes & Context",
                                                                            value: record.notes
                                                                        }, void 0, false, {
                                                                            fileName: "[project]/src/features/admissions/components/VetAdmissionQueue.tsx",
                                                                            lineNumber: 896,
                                                                            columnNumber: 35
                                                                        }, this)
                                                                    ]
                                                                }, void 0, true, {
                                                                    fileName: "[project]/src/features/admissions/components/VetAdmissionQueue.tsx",
                                                                    lineNumber: 891,
                                                                    columnNumber: 33
                                                                }, this),
                                                                record.trainingDecision && /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$compiled$2f$react$2f$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$client$5d$__$28$ecmascript$29$__["jsxDEV"])("div", {
                                                                    className: "mt-2.5 flex items-center gap-2",
                                                                    children: /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$compiled$2f$react$2f$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$client$5d$__$28$ecmascript$29$__["jsxDEV"])("span", {
                                                                        className: `inline-flex items-center px-2 py-0.5 rounded text-xs font-semibold ${record.trainingDecision === 'ALLOWED' ? 'bg-[var(--color-success-soft)] text-[var(--color-success)]' : 'bg-[var(--color-danger-soft)] text-[var(--color-danger)]'}`,
                                                                        children: [
                                                                            "Training Decision: ",
                                                                            record.trainingDecision
                                                                        ]
                                                                    }, void 0, true, {
                                                                        fileName: "[project]/src/features/admissions/components/VetAdmissionQueue.tsx",
                                                                        lineNumber: 901,
                                                                        columnNumber: 37
                                                                    }, this)
                                                                }, void 0, false, {
                                                                    fileName: "[project]/src/features/admissions/components/VetAdmissionQueue.tsx",
                                                                    lineNumber: 900,
                                                                    columnNumber: 35
                                                                }, this),
                                                                record.restrictionDetails && /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$compiled$2f$react$2f$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$client$5d$__$28$ecmascript$29$__["jsxDEV"])("div", {
                                                                    className: "mt-2 rounded bg-[var(--color-warning-soft)] p-2 text-xs text-[var(--color-warning)]",
                                                                    children: [
                                                                        /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$compiled$2f$react$2f$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$client$5d$__$28$ecmascript$29$__["jsxDEV"])("strong", {
                                                                            children: "Restriction Protocol:"
                                                                        }, void 0, false, {
                                                                            fileName: "[project]/src/features/admissions/components/VetAdmissionQueue.tsx",
                                                                            lineNumber: 915,
                                                                            columnNumber: 37
                                                                        }, this),
                                                                        " ",
                                                                        record.restrictionDetails
                                                                    ]
                                                                }, void 0, true, {
                                                                    fileName: "[project]/src/features/admissions/components/VetAdmissionQueue.tsx",
                                                                    lineNumber: 914,
                                                                    columnNumber: 35
                                                                }, this)
                                                            ]
                                                        }, record.id, true, {
                                                            fileName: "[project]/src/features/admissions/components/VetAdmissionQueue.tsx",
                                                            lineNumber: 871,
                                                            columnNumber: 31
                                                        }, this))
                                                }, void 0, false, {
                                                    fileName: "[project]/src/features/admissions/components/VetAdmissionQueue.tsx",
                                                    lineNumber: 869,
                                                    columnNumber: 27
                                                }, this) : /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$compiled$2f$react$2f$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$client$5d$__$28$ecmascript$29$__["jsxDEV"])("p", {
                                                    className: "mt-2 text-xs text-[var(--color-text-muted)] italic",
                                                    children: "No health records filed for this candidate yet."
                                                }, void 0, false, {
                                                    fileName: "[project]/src/features/admissions/components/VetAdmissionQueue.tsx",
                                                    lineNumber: 922,
                                                    columnNumber: 27
                                                }, this)
                                            ]
                                        }, void 0, true, {
                                            fileName: "[project]/src/features/admissions/components/VetAdmissionQueue.tsx",
                                            lineNumber: 864,
                                            columnNumber: 23
                                        }, this),
                                        /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$compiled$2f$react$2f$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$client$5d$__$28$ecmascript$29$__["jsxDEV"])("div", {
                                            className: "border-t border-[var(--color-border)] pt-4",
                                            children: [
                                                /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$compiled$2f$react$2f$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$client$5d$__$28$ecmascript$29$__["jsxDEV"])("h3", {
                                                    className: "text-sm font-bold text-[var(--color-text-primary)]",
                                                    children: "Examination Schedule Log"
                                                }, void 0, false, {
                                                    fileName: "[project]/src/features/admissions/components/VetAdmissionQueue.tsx",
                                                    lineNumber: 929,
                                                    columnNumber: 25
                                                }, this),
                                                selectedHistory.length > 0 ? /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$compiled$2f$react$2f$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$client$5d$__$28$ecmascript$29$__["jsxDEV"])("ol", {
                                                    className: "mt-3 space-y-2.5",
                                                    children: selectedHistory.map((item, idx)=>/*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$compiled$2f$react$2f$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$client$5d$__$28$ecmascript$29$__["jsxDEV"])("li", {
                                                            className: "flex items-start gap-3 rounded-[var(--radius-sm)] border border-[var(--color-border)] bg-[var(--color-surface-subtle)] p-3 text-xs",
                                                            children: [
                                                                /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$compiled$2f$react$2f$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$client$5d$__$28$ecmascript$29$__["jsxDEV"])("span", {
                                                                    className: "flex h-5 w-5 shrink-0 items-center justify-center rounded-full bg-[var(--color-primary-soft)] text-xs font-bold text-[var(--color-primary)]",
                                                                    children: idx + 1
                                                                }, void 0, false, {
                                                                    fileName: "[project]/src/features/admissions/components/VetAdmissionQueue.tsx",
                                                                    lineNumber: 939,
                                                                    columnNumber: 33
                                                                }, this),
                                                                /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$compiled$2f$react$2f$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$client$5d$__$28$ecmascript$29$__["jsxDEV"])("div", {
                                                                    className: "min-w-0 flex-1",
                                                                    children: [
                                                                        /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$compiled$2f$react$2f$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$client$5d$__$28$ecmascript$29$__["jsxDEV"])("div", {
                                                                            className: "flex flex-wrap items-center justify-between gap-2",
                                                                            children: [
                                                                                /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$compiled$2f$react$2f$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$client$5d$__$28$ecmascript$29$__["jsxDEV"])("span", {
                                                                                    className: "font-bold text-[var(--color-text-primary)]",
                                                                                    children: item.label
                                                                                }, void 0, false, {
                                                                                    fileName: "[project]/src/features/admissions/components/VetAdmissionQueue.tsx",
                                                                                    lineNumber: 944,
                                                                                    columnNumber: 37
                                                                                }, this),
                                                                                /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$compiled$2f$react$2f$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$client$5d$__$28$ecmascript$29$__["jsxDEV"])(__TURBOPACK__imported__module__$5b$project$5d2f$src$2f$components$2f$ui$2f$StatusBadge$2e$tsx__$5b$app$2d$client$5d$__$28$ecmascript$29$__["Pill"], {
                                                                                    tone: examTone(item.status),
                                                                                    size: "sm",
                                                                                    children: formatLabel(item.status)
                                                                                }, void 0, false, {
                                                                                    fileName: "[project]/src/features/admissions/components/VetAdmissionQueue.tsx",
                                                                                    lineNumber: 947,
                                                                                    columnNumber: 37
                                                                                }, this)
                                                                            ]
                                                                        }, void 0, true, {
                                                                            fileName: "[project]/src/features/admissions/components/VetAdmissionQueue.tsx",
                                                                            lineNumber: 943,
                                                                            columnNumber: 35
                                                                        }, this),
                                                                        /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$compiled$2f$react$2f$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$client$5d$__$28$ecmascript$29$__["jsxDEV"])("p", {
                                                                            className: "mt-0.5 text-xs text-[var(--color-text-secondary)]",
                                                                            children: item.description
                                                                        }, void 0, false, {
                                                                            fileName: "[project]/src/features/admissions/components/VetAdmissionQueue.tsx",
                                                                            lineNumber: 951,
                                                                            columnNumber: 35
                                                                        }, this),
                                                                        /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$compiled$2f$react$2f$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$client$5d$__$28$ecmascript$29$__["jsxDEV"])("div", {
                                                                            className: "mt-1 flex flex-wrap items-center gap-3 text-xs text-[var(--color-text-muted)]",
                                                                            children: [
                                                                                /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$compiled$2f$react$2f$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$client$5d$__$28$ecmascript$29$__["jsxDEV"])("span", {
                                                                                    children: [
                                                                                        "Scheduled: ",
                                                                                        item.scheduledAt ? formatDate(item.scheduledAt, true) : 'Pending'
                                                                                    ]
                                                                                }, void 0, true, {
                                                                                    fileName: "[project]/src/features/admissions/components/VetAdmissionQueue.tsx",
                                                                                    lineNumber: 955,
                                                                                    columnNumber: 37
                                                                                }, this),
                                                                                /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$compiled$2f$react$2f$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$client$5d$__$28$ecmascript$29$__["jsxDEV"])("span", {
                                                                                    children: "·"
                                                                                }, void 0, false, {
                                                                                    fileName: "[project]/src/features/admissions/components/VetAdmissionQueue.tsx",
                                                                                    lineNumber: 956,
                                                                                    columnNumber: 37
                                                                                }, this),
                                                                                /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$compiled$2f$react$2f$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$client$5d$__$28$ecmascript$29$__["jsxDEV"])("span", {
                                                                                    children: [
                                                                                        "Assigned Vet: ",
                                                                                        item.assignedVetId ? `#${item.assignedVetId}` : 'Unassigned'
                                                                                    ]
                                                                                }, void 0, true, {
                                                                                    fileName: "[project]/src/features/admissions/components/VetAdmissionQueue.tsx",
                                                                                    lineNumber: 957,
                                                                                    columnNumber: 37
                                                                                }, this)
                                                                            ]
                                                                        }, void 0, true, {
                                                                            fileName: "[project]/src/features/admissions/components/VetAdmissionQueue.tsx",
                                                                            lineNumber: 954,
                                                                            columnNumber: 35
                                                                        }, this)
                                                                    ]
                                                                }, void 0, true, {
                                                                    fileName: "[project]/src/features/admissions/components/VetAdmissionQueue.tsx",
                                                                    lineNumber: 942,
                                                                    columnNumber: 33
                                                                }, this)
                                                            ]
                                                        }, `${item.id}-${idx}`, true, {
                                                            fileName: "[project]/src/features/admissions/components/VetAdmissionQueue.tsx",
                                                            lineNumber: 935,
                                                            columnNumber: 31
                                                        }, this))
                                                }, void 0, false, {
                                                    fileName: "[project]/src/features/admissions/components/VetAdmissionQueue.tsx",
                                                    lineNumber: 933,
                                                    columnNumber: 27
                                                }, this) : /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$compiled$2f$react$2f$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$client$5d$__$28$ecmascript$29$__["jsxDEV"])("p", {
                                                    className: "mt-2 text-xs text-[var(--color-text-muted)] italic",
                                                    children: "No care schedule logs recorded."
                                                }, void 0, false, {
                                                    fileName: "[project]/src/features/admissions/components/VetAdmissionQueue.tsx",
                                                    lineNumber: 964,
                                                    columnNumber: 27
                                                }, this)
                                            ]
                                        }, void 0, true, {
                                            fileName: "[project]/src/features/admissions/components/VetAdmissionQueue.tsx",
                                            lineNumber: 928,
                                            columnNumber: 23
                                        }, this)
                                    ]
                                }, void 0, true, {
                                    fileName: "[project]/src/features/admissions/components/VetAdmissionQueue.tsx",
                                    lineNumber: 863,
                                    columnNumber: 21
                                }, this),
                                activeTab === 'vitals' && /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$compiled$2f$react$2f$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$client$5d$__$28$ecmascript$29$__["jsxDEV"])("div", {
                                    className: "space-y-4",
                                    children: [
                                        /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$compiled$2f$react$2f$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$client$5d$__$28$ecmascript$29$__["jsxDEV"])("div", {
                                            className: "flex items-center justify-between",
                                            children: [
                                                /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$compiled$2f$react$2f$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$client$5d$__$28$ecmascript$29$__["jsxDEV"])("h3", {
                                                    className: "text-sm font-bold text-[var(--color-text-primary)]",
                                                    children: "Recorded Vitals & Telemetry"
                                                }, void 0, false, {
                                                    fileName: "[project]/src/features/admissions/components/VetAdmissionQueue.tsx",
                                                    lineNumber: 976,
                                                    columnNumber: 25
                                                }, this),
                                                /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$compiled$2f$react$2f$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$client$5d$__$28$ecmascript$29$__["jsxDEV"])("span", {
                                                    className: "text-xs text-[var(--color-text-muted)]",
                                                    children: [
                                                        metrics.length,
                                                        " readings on record"
                                                    ]
                                                }, void 0, true, {
                                                    fileName: "[project]/src/features/admissions/components/VetAdmissionQueue.tsx",
                                                    lineNumber: 979,
                                                    columnNumber: 25
                                                }, this)
                                            ]
                                        }, void 0, true, {
                                            fileName: "[project]/src/features/admissions/components/VetAdmissionQueue.tsx",
                                            lineNumber: 975,
                                            columnNumber: 23
                                        }, this),
                                        metrics.length > 0 ? /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$compiled$2f$react$2f$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$client$5d$__$28$ecmascript$29$__["jsxDEV"])("div", {
                                            className: "overflow-x-auto rounded-[var(--radius-md)] border border-[var(--color-border)]",
                                            children: /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$compiled$2f$react$2f$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$client$5d$__$28$ecmascript$29$__["jsxDEV"])(__TURBOPACK__imported__module__$5b$project$5d2f$src$2f$components$2f$ui$2f$Table$2e$tsx__$5b$app$2d$client$5d$__$28$ecmascript$29$__["Table"], {
                                                bare: true,
                                                children: [
                                                    /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$compiled$2f$react$2f$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$client$5d$__$28$ecmascript$29$__["jsxDEV"])(__TURBOPACK__imported__module__$5b$project$5d2f$src$2f$components$2f$ui$2f$Table$2e$tsx__$5b$app$2d$client$5d$__$28$ecmascript$29$__["THead"], {
                                                        children: /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$compiled$2f$react$2f$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$client$5d$__$28$ecmascript$29$__["jsxDEV"])(__TURBOPACK__imported__module__$5b$project$5d2f$src$2f$components$2f$ui$2f$Table$2e$tsx__$5b$app$2d$client$5d$__$28$ecmascript$29$__["Tr"], {
                                                            children: [
                                                                /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$compiled$2f$react$2f$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$client$5d$__$28$ecmascript$29$__["jsxDEV"])(__TURBOPACK__imported__module__$5b$project$5d2f$src$2f$components$2f$ui$2f$Table$2e$tsx__$5b$app$2d$client$5d$__$28$ecmascript$29$__["Th"], {
                                                                    className: "px-3 py-2",
                                                                    children: "Recorded At"
                                                                }, void 0, false, {
                                                                    fileName: "[project]/src/features/admissions/components/VetAdmissionQueue.tsx",
                                                                    lineNumber: 989,
                                                                    columnNumber: 33
                                                                }, this),
                                                                /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$compiled$2f$react$2f$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$client$5d$__$28$ecmascript$29$__["jsxDEV"])(__TURBOPACK__imported__module__$5b$project$5d2f$src$2f$components$2f$ui$2f$Table$2e$tsx__$5b$app$2d$client$5d$__$28$ecmascript$29$__["Th"], {
                                                                    className: "px-3 py-2",
                                                                    children: "Temp"
                                                                }, void 0, false, {
                                                                    fileName: "[project]/src/features/admissions/components/VetAdmissionQueue.tsx",
                                                                    lineNumber: 990,
                                                                    columnNumber: 33
                                                                }, this),
                                                                /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$compiled$2f$react$2f$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$client$5d$__$28$ecmascript$29$__["jsxDEV"])(__TURBOPACK__imported__module__$5b$project$5d2f$src$2f$components$2f$ui$2f$Table$2e$tsx__$5b$app$2d$client$5d$__$28$ecmascript$29$__["Th"], {
                                                                    className: "px-3 py-2",
                                                                    children: "Heart Rate"
                                                                }, void 0, false, {
                                                                    fileName: "[project]/src/features/admissions/components/VetAdmissionQueue.tsx",
                                                                    lineNumber: 991,
                                                                    columnNumber: 33
                                                                }, this),
                                                                /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$compiled$2f$react$2f$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$client$5d$__$28$ecmascript$29$__["jsxDEV"])(__TURBOPACK__imported__module__$5b$project$5d2f$src$2f$components$2f$ui$2f$Table$2e$tsx__$5b$app$2d$client$5d$__$28$ecmascript$29$__["Th"], {
                                                                    className: "px-3 py-2",
                                                                    children: "Resp Rate"
                                                                }, void 0, false, {
                                                                    fileName: "[project]/src/features/admissions/components/VetAdmissionQueue.tsx",
                                                                    lineNumber: 992,
                                                                    columnNumber: 33
                                                                }, this),
                                                                /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$compiled$2f$react$2f$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$client$5d$__$28$ecmascript$29$__["jsxDEV"])(__TURBOPACK__imported__module__$5b$project$5d2f$src$2f$components$2f$ui$2f$Table$2e$tsx__$5b$app$2d$client$5d$__$28$ecmascript$29$__["Th"], {
                                                                    className: "px-3 py-2",
                                                                    children: "Weight"
                                                                }, void 0, false, {
                                                                    fileName: "[project]/src/features/admissions/components/VetAdmissionQueue.tsx",
                                                                    lineNumber: 993,
                                                                    columnNumber: 33
                                                                }, this),
                                                                /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$compiled$2f$react$2f$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$client$5d$__$28$ecmascript$29$__["jsxDEV"])(__TURBOPACK__imported__module__$5b$project$5d2f$src$2f$components$2f$ui$2f$Table$2e$tsx__$5b$app$2d$client$5d$__$28$ecmascript$29$__["Th"], {
                                                                    className: "px-3 py-2",
                                                                    children: "BCS"
                                                                }, void 0, false, {
                                                                    fileName: "[project]/src/features/admissions/components/VetAdmissionQueue.tsx",
                                                                    lineNumber: 994,
                                                                    columnNumber: 33
                                                                }, this),
                                                                /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$compiled$2f$react$2f$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$client$5d$__$28$ecmascript$29$__["jsxDEV"])(__TURBOPACK__imported__module__$5b$project$5d2f$src$2f$components$2f$ui$2f$Table$2e$tsx__$5b$app$2d$client$5d$__$28$ecmascript$29$__["Th"], {
                                                                    className: "px-3 py-2",
                                                                    children: "Hydration"
                                                                }, void 0, false, {
                                                                    fileName: "[project]/src/features/admissions/components/VetAdmissionQueue.tsx",
                                                                    lineNumber: 995,
                                                                    columnNumber: 33
                                                                }, this),
                                                                /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$compiled$2f$react$2f$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$client$5d$__$28$ecmascript$29$__["jsxDEV"])(__TURBOPACK__imported__module__$5b$project$5d2f$src$2f$components$2f$ui$2f$Table$2e$tsx__$5b$app$2d$client$5d$__$28$ecmascript$29$__["Th"], {
                                                                    className: "px-3 py-2",
                                                                    children: "Notes"
                                                                }, void 0, false, {
                                                                    fileName: "[project]/src/features/admissions/components/VetAdmissionQueue.tsx",
                                                                    lineNumber: 996,
                                                                    columnNumber: 33
                                                                }, this)
                                                            ]
                                                        }, void 0, true, {
                                                            fileName: "[project]/src/features/admissions/components/VetAdmissionQueue.tsx",
                                                            lineNumber: 988,
                                                            columnNumber: 31
                                                        }, this)
                                                    }, void 0, false, {
                                                        fileName: "[project]/src/features/admissions/components/VetAdmissionQueue.tsx",
                                                        lineNumber: 987,
                                                        columnNumber: 29
                                                    }, this),
                                                    /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$compiled$2f$react$2f$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$client$5d$__$28$ecmascript$29$__["jsxDEV"])(__TURBOPACK__imported__module__$5b$project$5d2f$src$2f$components$2f$ui$2f$Table$2e$tsx__$5b$app$2d$client$5d$__$28$ecmascript$29$__["TBody"], {
                                                        children: metrics.map((m)=>/*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$compiled$2f$react$2f$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$client$5d$__$28$ecmascript$29$__["jsxDEV"])(__TURBOPACK__imported__module__$5b$project$5d2f$src$2f$components$2f$ui$2f$Table$2e$tsx__$5b$app$2d$client$5d$__$28$ecmascript$29$__["Tr"], {
                                                                className: "hover:bg-[var(--color-surface-subtle)]",
                                                                children: [
                                                                    /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$compiled$2f$react$2f$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$client$5d$__$28$ecmascript$29$__["jsxDEV"])(__TURBOPACK__imported__module__$5b$project$5d2f$src$2f$components$2f$ui$2f$Table$2e$tsx__$5b$app$2d$client$5d$__$28$ecmascript$29$__["Td"], {
                                                                        className: "px-3 py-2 text-[var(--color-text-primary)]",
                                                                        children: formatDate(m.recordedAt, true)
                                                                    }, void 0, false, {
                                                                        fileName: "[project]/src/features/admissions/components/VetAdmissionQueue.tsx",
                                                                        lineNumber: 1002,
                                                                        columnNumber: 35
                                                                    }, this),
                                                                    /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$compiled$2f$react$2f$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$client$5d$__$28$ecmascript$29$__["jsxDEV"])(__TURBOPACK__imported__module__$5b$project$5d2f$src$2f$components$2f$ui$2f$Table$2e$tsx__$5b$app$2d$client$5d$__$28$ecmascript$29$__["Td"], {
                                                                        className: "px-3 py-2",
                                                                        children: m.temperature != null ? `${m.temperature} °C` : '—'
                                                                    }, void 0, false, {
                                                                        fileName: "[project]/src/features/admissions/components/VetAdmissionQueue.tsx",
                                                                        lineNumber: 1005,
                                                                        columnNumber: 35
                                                                    }, this),
                                                                    /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$compiled$2f$react$2f$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$client$5d$__$28$ecmascript$29$__["jsxDEV"])(__TURBOPACK__imported__module__$5b$project$5d2f$src$2f$components$2f$ui$2f$Table$2e$tsx__$5b$app$2d$client$5d$__$28$ecmascript$29$__["Td"], {
                                                                        className: "px-3 py-2",
                                                                        children: m.heartRate != null ? `${m.heartRate} bpm` : '—'
                                                                    }, void 0, false, {
                                                                        fileName: "[project]/src/features/admissions/components/VetAdmissionQueue.tsx",
                                                                        lineNumber: 1008,
                                                                        columnNumber: 35
                                                                    }, this),
                                                                    /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$compiled$2f$react$2f$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$client$5d$__$28$ecmascript$29$__["jsxDEV"])(__TURBOPACK__imported__module__$5b$project$5d2f$src$2f$components$2f$ui$2f$Table$2e$tsx__$5b$app$2d$client$5d$__$28$ecmascript$29$__["Td"], {
                                                                        className: "px-3 py-2",
                                                                        children: m.respiratoryRate != null ? `${m.respiratoryRate} rpm` : '—'
                                                                    }, void 0, false, {
                                                                        fileName: "[project]/src/features/admissions/components/VetAdmissionQueue.tsx",
                                                                        lineNumber: 1011,
                                                                        columnNumber: 35
                                                                    }, this),
                                                                    /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$compiled$2f$react$2f$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$client$5d$__$28$ecmascript$29$__["jsxDEV"])(__TURBOPACK__imported__module__$5b$project$5d2f$src$2f$components$2f$ui$2f$Table$2e$tsx__$5b$app$2d$client$5d$__$28$ecmascript$29$__["Td"], {
                                                                        className: "px-3 py-2",
                                                                        children: m.weight != null ? `${m.weight} kg` : '—'
                                                                    }, void 0, false, {
                                                                        fileName: "[project]/src/features/admissions/components/VetAdmissionQueue.tsx",
                                                                        lineNumber: 1014,
                                                                        columnNumber: 35
                                                                    }, this),
                                                                    /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$compiled$2f$react$2f$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$client$5d$__$28$ecmascript$29$__["jsxDEV"])(__TURBOPACK__imported__module__$5b$project$5d2f$src$2f$components$2f$ui$2f$Table$2e$tsx__$5b$app$2d$client$5d$__$28$ecmascript$29$__["Td"], {
                                                                        className: "px-3 py-2",
                                                                        children: m.bodyConditionScore != null ? `${m.bodyConditionScore} / 9` : '—'
                                                                    }, void 0, false, {
                                                                        fileName: "[project]/src/features/admissions/components/VetAdmissionQueue.tsx",
                                                                        lineNumber: 1017,
                                                                        columnNumber: 35
                                                                    }, this),
                                                                    /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$compiled$2f$react$2f$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$client$5d$__$28$ecmascript$29$__["jsxDEV"])(__TURBOPACK__imported__module__$5b$project$5d2f$src$2f$components$2f$ui$2f$Table$2e$tsx__$5b$app$2d$client$5d$__$28$ecmascript$29$__["Td"], {
                                                                        className: "px-3 py-2 font-sans",
                                                                        children: m.hydrationStatus || '—'
                                                                    }, void 0, false, {
                                                                        fileName: "[project]/src/features/admissions/components/VetAdmissionQueue.tsx",
                                                                        lineNumber: 1020,
                                                                        columnNumber: 35
                                                                    }, this),
                                                                    /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$compiled$2f$react$2f$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$client$5d$__$28$ecmascript$29$__["jsxDEV"])(__TURBOPACK__imported__module__$5b$project$5d2f$src$2f$components$2f$ui$2f$Table$2e$tsx__$5b$app$2d$client$5d$__$28$ecmascript$29$__["Td"], {
                                                                        className: "px-3 py-2 font-sans text-[var(--color-text-secondary)] max-w-xs truncate",
                                                                        children: m.notes || '—'
                                                                    }, void 0, false, {
                                                                        fileName: "[project]/src/features/admissions/components/VetAdmissionQueue.tsx",
                                                                        lineNumber: 1023,
                                                                        columnNumber: 35
                                                                    }, this)
                                                                ]
                                                            }, m.id, true, {
                                                                fileName: "[project]/src/features/admissions/components/VetAdmissionQueue.tsx",
                                                                lineNumber: 1001,
                                                                columnNumber: 33
                                                            }, this))
                                                    }, void 0, false, {
                                                        fileName: "[project]/src/features/admissions/components/VetAdmissionQueue.tsx",
                                                        lineNumber: 999,
                                                        columnNumber: 29
                                                    }, this)
                                                ]
                                            }, void 0, true, {
                                                fileName: "[project]/src/features/admissions/components/VetAdmissionQueue.tsx",
                                                lineNumber: 986,
                                                columnNumber: 27
                                            }, this)
                                        }, void 0, false, {
                                            fileName: "[project]/src/features/admissions/components/VetAdmissionQueue.tsx",
                                            lineNumber: 985,
                                            columnNumber: 25
                                        }, this) : /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$compiled$2f$react$2f$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$client$5d$__$28$ecmascript$29$__["jsxDEV"])("div", {
                                            className: "rounded-[var(--radius-md)] border border-dashed border-[var(--color-border)] p-6 text-center text-[var(--color-text-muted)]",
                                            children: [
                                                /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$compiled$2f$react$2f$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$client$5d$__$28$ecmascript$29$__["jsxDEV"])(__TURBOPACK__imported__module__$5b$project$5d2f$src$2f$components$2f$ui$2f$Icon$2e$tsx__$5b$app$2d$client$5d$__$28$ecmascript$29$__["Icon"], {
                                                    name: "heart-pulse",
                                                    size: 24,
                                                    className: "mx-auto mb-1.5 opacity-50"
                                                }, void 0, false, {
                                                    fileName: "[project]/src/features/admissions/components/VetAdmissionQueue.tsx",
                                                    lineNumber: 1033,
                                                    columnNumber: 27
                                                }, this),
                                                /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$compiled$2f$react$2f$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$client$5d$__$28$ecmascript$29$__["jsxDEV"])("p", {
                                                    className: "text-xs font-medium text-[var(--color-text-secondary)]",
                                                    children: "No telemetry metrics recorded yet"
                                                }, void 0, false, {
                                                    fileName: "[project]/src/features/admissions/components/VetAdmissionQueue.tsx",
                                                    lineNumber: 1034,
                                                    columnNumber: 27
                                                }, this)
                                            ]
                                        }, void 0, true, {
                                            fileName: "[project]/src/features/admissions/components/VetAdmissionQueue.tsx",
                                            lineNumber: 1032,
                                            columnNumber: 25
                                        }, this),
                                        latestMetrics && /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$compiled$2f$react$2f$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$client$5d$__$28$ecmascript$29$__["jsxDEV"])("div", {
                                            className: "rounded-[var(--radius-md)] bg-[var(--color-surface-subtle)] p-3 border border-[var(--color-border)]",
                                            children: [
                                                /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$compiled$2f$react$2f$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$client$5d$__$28$ecmascript$29$__["jsxDEV"])("span", {
                                                    className: "text-xs font-bold uppercase tracking-wider text-[var(--color-text-muted)]",
                                                    children: [
                                                        "Latest Telemetry Baseline (",
                                                        formatDate(latestMetrics.recordedAt, true),
                                                        ")"
                                                    ]
                                                }, void 0, true, {
                                                    fileName: "[project]/src/features/admissions/components/VetAdmissionQueue.tsx",
                                                    lineNumber: 1042,
                                                    columnNumber: 27
                                                }, this),
                                                /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$compiled$2f$react$2f$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$client$5d$__$28$ecmascript$29$__["jsxDEV"])("div", {
                                                    className: "mt-2 grid grid-cols-2 gap-2 sm:grid-cols-3 xl:grid-cols-6 font-metric text-xs",
                                                    children: [
                                                        /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$compiled$2f$react$2f$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$client$5d$__$28$ecmascript$29$__["jsxDEV"])("div", {
                                                            children: [
                                                                /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$compiled$2f$react$2f$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$client$5d$__$28$ecmascript$29$__["jsxDEV"])("span", {
                                                                    className: "text-xs text-[var(--color-text-muted)] block font-sans",
                                                                    children: "TEMP"
                                                                }, void 0, false, {
                                                                    fileName: "[project]/src/features/admissions/components/VetAdmissionQueue.tsx",
                                                                    lineNumber: 1047,
                                                                    columnNumber: 31
                                                                }, this),
                                                                /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$compiled$2f$react$2f$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$client$5d$__$28$ecmascript$29$__["jsxDEV"])("strong", {
                                                                    children: [
                                                                        latestMetrics.temperature ?? '—',
                                                                        " °C"
                                                                    ]
                                                                }, void 0, true, {
                                                                    fileName: "[project]/src/features/admissions/components/VetAdmissionQueue.tsx",
                                                                    lineNumber: 1048,
                                                                    columnNumber: 31
                                                                }, this)
                                                            ]
                                                        }, void 0, true, {
                                                            fileName: "[project]/src/features/admissions/components/VetAdmissionQueue.tsx",
                                                            lineNumber: 1046,
                                                            columnNumber: 29
                                                        }, this),
                                                        /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$compiled$2f$react$2f$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$client$5d$__$28$ecmascript$29$__["jsxDEV"])("div", {
                                                            children: [
                                                                /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$compiled$2f$react$2f$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$client$5d$__$28$ecmascript$29$__["jsxDEV"])("span", {
                                                                    className: "text-xs text-[var(--color-text-muted)] block font-sans",
                                                                    children: "PULSE"
                                                                }, void 0, false, {
                                                                    fileName: "[project]/src/features/admissions/components/VetAdmissionQueue.tsx",
                                                                    lineNumber: 1051,
                                                                    columnNumber: 31
                                                                }, this),
                                                                /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$compiled$2f$react$2f$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$client$5d$__$28$ecmascript$29$__["jsxDEV"])("strong", {
                                                                    children: [
                                                                        latestMetrics.heartRate ?? '—',
                                                                        " bpm"
                                                                    ]
                                                                }, void 0, true, {
                                                                    fileName: "[project]/src/features/admissions/components/VetAdmissionQueue.tsx",
                                                                    lineNumber: 1052,
                                                                    columnNumber: 31
                                                                }, this)
                                                            ]
                                                        }, void 0, true, {
                                                            fileName: "[project]/src/features/admissions/components/VetAdmissionQueue.tsx",
                                                            lineNumber: 1050,
                                                            columnNumber: 29
                                                        }, this),
                                                        /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$compiled$2f$react$2f$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$client$5d$__$28$ecmascript$29$__["jsxDEV"])("div", {
                                                            children: [
                                                                /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$compiled$2f$react$2f$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$client$5d$__$28$ecmascript$29$__["jsxDEV"])("span", {
                                                                    className: "text-xs text-[var(--color-text-muted)] block font-sans",
                                                                    children: "RESP"
                                                                }, void 0, false, {
                                                                    fileName: "[project]/src/features/admissions/components/VetAdmissionQueue.tsx",
                                                                    lineNumber: 1055,
                                                                    columnNumber: 31
                                                                }, this),
                                                                /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$compiled$2f$react$2f$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$client$5d$__$28$ecmascript$29$__["jsxDEV"])("strong", {
                                                                    children: [
                                                                        latestMetrics.respiratoryRate ?? '—',
                                                                        " rpm"
                                                                    ]
                                                                }, void 0, true, {
                                                                    fileName: "[project]/src/features/admissions/components/VetAdmissionQueue.tsx",
                                                                    lineNumber: 1056,
                                                                    columnNumber: 31
                                                                }, this)
                                                            ]
                                                        }, void 0, true, {
                                                            fileName: "[project]/src/features/admissions/components/VetAdmissionQueue.tsx",
                                                            lineNumber: 1054,
                                                            columnNumber: 29
                                                        }, this),
                                                        /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$compiled$2f$react$2f$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$client$5d$__$28$ecmascript$29$__["jsxDEV"])("div", {
                                                            children: [
                                                                /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$compiled$2f$react$2f$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$client$5d$__$28$ecmascript$29$__["jsxDEV"])("span", {
                                                                    className: "text-xs text-[var(--color-text-muted)] block font-sans",
                                                                    children: "WEIGHT"
                                                                }, void 0, false, {
                                                                    fileName: "[project]/src/features/admissions/components/VetAdmissionQueue.tsx",
                                                                    lineNumber: 1059,
                                                                    columnNumber: 31
                                                                }, this),
                                                                /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$compiled$2f$react$2f$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$client$5d$__$28$ecmascript$29$__["jsxDEV"])("strong", {
                                                                    children: [
                                                                        latestMetrics.weight ?? '—',
                                                                        " kg"
                                                                    ]
                                                                }, void 0, true, {
                                                                    fileName: "[project]/src/features/admissions/components/VetAdmissionQueue.tsx",
                                                                    lineNumber: 1060,
                                                                    columnNumber: 31
                                                                }, this)
                                                            ]
                                                        }, void 0, true, {
                                                            fileName: "[project]/src/features/admissions/components/VetAdmissionQueue.tsx",
                                                            lineNumber: 1058,
                                                            columnNumber: 29
                                                        }, this),
                                                        /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$compiled$2f$react$2f$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$client$5d$__$28$ecmascript$29$__["jsxDEV"])("div", {
                                                            children: [
                                                                /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$compiled$2f$react$2f$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$client$5d$__$28$ecmascript$29$__["jsxDEV"])("span", {
                                                                    className: "text-xs text-[var(--color-text-muted)] block font-sans",
                                                                    children: "BCS"
                                                                }, void 0, false, {
                                                                    fileName: "[project]/src/features/admissions/components/VetAdmissionQueue.tsx",
                                                                    lineNumber: 1063,
                                                                    columnNumber: 31
                                                                }, this),
                                                                /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$compiled$2f$react$2f$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$client$5d$__$28$ecmascript$29$__["jsxDEV"])("strong", {
                                                                    children: [
                                                                        latestMetrics.bodyConditionScore ?? '—',
                                                                        " / 9"
                                                                    ]
                                                                }, void 0, true, {
                                                                    fileName: "[project]/src/features/admissions/components/VetAdmissionQueue.tsx",
                                                                    lineNumber: 1064,
                                                                    columnNumber: 31
                                                                }, this)
                                                            ]
                                                        }, void 0, true, {
                                                            fileName: "[project]/src/features/admissions/components/VetAdmissionQueue.tsx",
                                                            lineNumber: 1062,
                                                            columnNumber: 29
                                                        }, this),
                                                        /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$compiled$2f$react$2f$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$client$5d$__$28$ecmascript$29$__["jsxDEV"])("div", {
                                                            children: [
                                                                /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$compiled$2f$react$2f$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$client$5d$__$28$ecmascript$29$__["jsxDEV"])("span", {
                                                                    className: "text-xs text-[var(--color-text-muted)] block font-sans",
                                                                    children: "HYDRATION"
                                                                }, void 0, false, {
                                                                    fileName: "[project]/src/features/admissions/components/VetAdmissionQueue.tsx",
                                                                    lineNumber: 1067,
                                                                    columnNumber: 31
                                                                }, this),
                                                                /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$compiled$2f$react$2f$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$client$5d$__$28$ecmascript$29$__["jsxDEV"])("strong", {
                                                                    className: "font-sans text-xs",
                                                                    children: latestMetrics.hydrationStatus ?? 'Normal'
                                                                }, void 0, false, {
                                                                    fileName: "[project]/src/features/admissions/components/VetAdmissionQueue.tsx",
                                                                    lineNumber: 1068,
                                                                    columnNumber: 31
                                                                }, this)
                                                            ]
                                                        }, void 0, true, {
                                                            fileName: "[project]/src/features/admissions/components/VetAdmissionQueue.tsx",
                                                            lineNumber: 1066,
                                                            columnNumber: 29
                                                        }, this)
                                                    ]
                                                }, void 0, true, {
                                                    fileName: "[project]/src/features/admissions/components/VetAdmissionQueue.tsx",
                                                    lineNumber: 1045,
                                                    columnNumber: 27
                                                }, this)
                                            ]
                                        }, void 0, true, {
                                            fileName: "[project]/src/features/admissions/components/VetAdmissionQueue.tsx",
                                            lineNumber: 1041,
                                            columnNumber: 25
                                        }, this)
                                    ]
                                }, void 0, true, {
                                    fileName: "[project]/src/features/admissions/components/VetAdmissionQueue.tsx",
                                    lineNumber: 974,
                                    columnNumber: 21
                                }, this),
                                activeTab === 'documents' && /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$compiled$2f$react$2f$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$client$5d$__$28$ecmascript$29$__["jsxDEV"])("div", {
                                    className: "grid gap-5 xl:grid-cols-2",
                                    children: [
                                        /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$compiled$2f$react$2f$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$client$5d$__$28$ecmascript$29$__["jsxDEV"])("div", {
                                            children: [
                                                /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$compiled$2f$react$2f$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$client$5d$__$28$ecmascript$29$__["jsxDEV"])("h3", {
                                                    className: "text-sm font-bold text-[var(--color-text-primary)]",
                                                    children: "Attached Admission Documents"
                                                }, void 0, false, {
                                                    fileName: "[project]/src/features/admissions/components/VetAdmissionQueue.tsx",
                                                    lineNumber: 1080,
                                                    columnNumber: 25
                                                }, this),
                                                documents.length > 0 ? /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$compiled$2f$react$2f$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$client$5d$__$28$ecmascript$29$__["jsxDEV"])("ul", {
                                                    className: "mt-3 space-y-2",
                                                    children: documents.map((doc)=>/*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$compiled$2f$react$2f$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$client$5d$__$28$ecmascript$29$__["jsxDEV"])("li", {
                                                            className: "flex items-start gap-3 rounded-[var(--radius-md)] border border-[var(--color-border)] bg-[var(--color-surface)] p-3 text-xs",
                                                            children: [
                                                                /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$compiled$2f$react$2f$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$client$5d$__$28$ecmascript$29$__["jsxDEV"])("span", {
                                                                    className: "flex h-7 w-7 shrink-0 items-center justify-center rounded bg-[var(--color-primary-soft)] text-[var(--color-primary)]",
                                                                    children: /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$compiled$2f$react$2f$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$client$5d$__$28$ecmascript$29$__["jsxDEV"])(__TURBOPACK__imported__module__$5b$project$5d2f$src$2f$components$2f$ui$2f$Icon$2e$tsx__$5b$app$2d$client$5d$__$28$ecmascript$29$__["Icon"], {
                                                                        name: "file-text",
                                                                        size: 14
                                                                    }, void 0, false, {
                                                                        fileName: "[project]/src/features/admissions/components/VetAdmissionQueue.tsx",
                                                                        lineNumber: 1091,
                                                                        columnNumber: 35
                                                                    }, this)
                                                                }, void 0, false, {
                                                                    fileName: "[project]/src/features/admissions/components/VetAdmissionQueue.tsx",
                                                                    lineNumber: 1090,
                                                                    columnNumber: 33
                                                                }, this),
                                                                /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$compiled$2f$react$2f$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$client$5d$__$28$ecmascript$29$__["jsxDEV"])("div", {
                                                                    className: "min-w-0 flex-1",
                                                                    children: [
                                                                        /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$compiled$2f$react$2f$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$client$5d$__$28$ecmascript$29$__["jsxDEV"])("a", {
                                                                            href: doc.fileUrl,
                                                                            target: "_blank",
                                                                            rel: "noopener noreferrer",
                                                                            className: "font-semibold text-[var(--color-primary)] hover:underline truncate block",
                                                                            children: formatLabel(doc.documentType)
                                                                        }, void 0, false, {
                                                                            fileName: "[project]/src/features/admissions/components/VetAdmissionQueue.tsx",
                                                                            lineNumber: 1094,
                                                                            columnNumber: 35
                                                                        }, this),
                                                                        /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$compiled$2f$react$2f$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$client$5d$__$28$ecmascript$29$__["jsxDEV"])("div", {
                                                                            className: "mt-0.5 flex items-center gap-2 text-xs text-[var(--color-text-muted)] font-metric",
                                                                            children: [
                                                                                doc.recordDate && /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$compiled$2f$react$2f$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$client$5d$__$28$ecmascript$29$__["jsxDEV"])("span", {
                                                                                    children: [
                                                                                        "Dated: ",
                                                                                        formatDate(doc.recordDate)
                                                                                    ]
                                                                                }, void 0, true, {
                                                                                    fileName: "[project]/src/features/admissions/components/VetAdmissionQueue.tsx",
                                                                                    lineNumber: 1103,
                                                                                    columnNumber: 56
                                                                                }, this),
                                                                                /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$compiled$2f$react$2f$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$client$5d$__$28$ecmascript$29$__["jsxDEV"])("span", {
                                                                                    children: [
                                                                                        "Uploaded: ",
                                                                                        formatDate(doc.uploadedAt)
                                                                                    ]
                                                                                }, void 0, true, {
                                                                                    fileName: "[project]/src/features/admissions/components/VetAdmissionQueue.tsx",
                                                                                    lineNumber: 1104,
                                                                                    columnNumber: 37
                                                                                }, this)
                                                                            ]
                                                                        }, void 0, true, {
                                                                            fileName: "[project]/src/features/admissions/components/VetAdmissionQueue.tsx",
                                                                            lineNumber: 1102,
                                                                            columnNumber: 35
                                                                        }, this)
                                                                    ]
                                                                }, void 0, true, {
                                                                    fileName: "[project]/src/features/admissions/components/VetAdmissionQueue.tsx",
                                                                    lineNumber: 1093,
                                                                    columnNumber: 33
                                                                }, this)
                                                            ]
                                                        }, doc.id, true, {
                                                            fileName: "[project]/src/features/admissions/components/VetAdmissionQueue.tsx",
                                                            lineNumber: 1086,
                                                            columnNumber: 31
                                                        }, this))
                                                }, void 0, false, {
                                                    fileName: "[project]/src/features/admissions/components/VetAdmissionQueue.tsx",
                                                    lineNumber: 1084,
                                                    columnNumber: 27
                                                }, this) : /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$compiled$2f$react$2f$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$client$5d$__$28$ecmascript$29$__["jsxDEV"])("p", {
                                                    className: "mt-2 text-xs text-[var(--color-text-muted)] italic",
                                                    children: "No documents attached."
                                                }, void 0, false, {
                                                    fileName: "[project]/src/features/admissions/components/VetAdmissionQueue.tsx",
                                                    lineNumber: 1111,
                                                    columnNumber: 27
                                                }, this)
                                            ]
                                        }, void 0, true, {
                                            fileName: "[project]/src/features/admissions/components/VetAdmissionQueue.tsx",
                                            lineNumber: 1079,
                                            columnNumber: 23
                                        }, this),
                                        /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$compiled$2f$react$2f$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$client$5d$__$28$ecmascript$29$__["jsxDEV"])("div", {
                                            className: "space-y-4",
                                            children: [
                                                /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$compiled$2f$react$2f$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$client$5d$__$28$ecmascript$29$__["jsxDEV"])("div", {
                                                    className: "rounded-[var(--radius-md)] bg-[var(--color-surface-subtle)] p-3.5 border border-[var(--color-border)]",
                                                    children: [
                                                        /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$compiled$2f$react$2f$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$client$5d$__$28$ecmascript$29$__["jsxDEV"])("h4", {
                                                            className: "text-xs font-bold text-[var(--color-text-primary)]",
                                                            children: "Pedigree & Registry Information"
                                                        }, void 0, false, {
                                                            fileName: "[project]/src/features/admissions/components/VetAdmissionQueue.tsx",
                                                            lineNumber: 1119,
                                                            columnNumber: 27
                                                        }, this),
                                                        /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$compiled$2f$react$2f$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$client$5d$__$28$ecmascript$29$__["jsxDEV"])("dl", {
                                                            className: "mt-2 space-y-1.5 text-xs",
                                                            children: [
                                                                /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$compiled$2f$react$2f$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$client$5d$__$28$ecmascript$29$__["jsxDEV"])("div", {
                                                                    className: "flex justify-between border-b border-[var(--color-border)] pb-1",
                                                                    children: [
                                                                        /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$compiled$2f$react$2f$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$client$5d$__$28$ecmascript$29$__["jsxDEV"])("span", {
                                                                            className: "text-[var(--color-text-muted)]",
                                                                            children: "Registry:"
                                                                        }, void 0, false, {
                                                                            fileName: "[project]/src/features/admissions/components/VetAdmissionQueue.tsx",
                                                                            lineNumber: 1124,
                                                                            columnNumber: 31
                                                                        }, this),
                                                                        /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$compiled$2f$react$2f$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$client$5d$__$28$ecmascript$29$__["jsxDEV"])("span", {
                                                                            className: "font-semibold",
                                                                            children: detail.candidate?.registryName || 'Not recorded'
                                                                        }, void 0, false, {
                                                                            fileName: "[project]/src/features/admissions/components/VetAdmissionQueue.tsx",
                                                                            lineNumber: 1125,
                                                                            columnNumber: 31
                                                                        }, this)
                                                                    ]
                                                                }, void 0, true, {
                                                                    fileName: "[project]/src/features/admissions/components/VetAdmissionQueue.tsx",
                                                                    lineNumber: 1123,
                                                                    columnNumber: 29
                                                                }, this),
                                                                /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$compiled$2f$react$2f$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$client$5d$__$28$ecmascript$29$__["jsxDEV"])("div", {
                                                                    className: "flex justify-between border-b border-[var(--color-border)] pb-1",
                                                                    children: [
                                                                        /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$compiled$2f$react$2f$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$client$5d$__$28$ecmascript$29$__["jsxDEV"])("span", {
                                                                            className: "text-[var(--color-text-muted)]",
                                                                            children: "Registration No:"
                                                                        }, void 0, false, {
                                                                            fileName: "[project]/src/features/admissions/components/VetAdmissionQueue.tsx",
                                                                            lineNumber: 1128,
                                                                            columnNumber: 31
                                                                        }, this),
                                                                        /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$compiled$2f$react$2f$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$client$5d$__$28$ecmascript$29$__["jsxDEV"])("span", {
                                                                            className: "font-semibold font-metric",
                                                                            children: detail.candidate?.registrationNumber || 'Not recorded'
                                                                        }, void 0, false, {
                                                                            fileName: "[project]/src/features/admissions/components/VetAdmissionQueue.tsx",
                                                                            lineNumber: 1129,
                                                                            columnNumber: 31
                                                                        }, this)
                                                                    ]
                                                                }, void 0, true, {
                                                                    fileName: "[project]/src/features/admissions/components/VetAdmissionQueue.tsx",
                                                                    lineNumber: 1127,
                                                                    columnNumber: 29
                                                                }, this),
                                                                /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$compiled$2f$react$2f$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$client$5d$__$28$ecmascript$29$__["jsxDEV"])("div", {
                                                                    className: "flex justify-between border-b border-[var(--color-border)] pb-1",
                                                                    children: [
                                                                        /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$compiled$2f$react$2f$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$client$5d$__$28$ecmascript$29$__["jsxDEV"])("span", {
                                                                            className: "text-[var(--color-text-muted)]",
                                                                            children: "Sire:"
                                                                        }, void 0, false, {
                                                                            fileName: "[project]/src/features/admissions/components/VetAdmissionQueue.tsx",
                                                                            lineNumber: 1132,
                                                                            columnNumber: 31
                                                                        }, this),
                                                                        /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$compiled$2f$react$2f$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$client$5d$__$28$ecmascript$29$__["jsxDEV"])("span", {
                                                                            className: "font-semibold",
                                                                            children: detail.candidate?.sireName || 'Not recorded'
                                                                        }, void 0, false, {
                                                                            fileName: "[project]/src/features/admissions/components/VetAdmissionQueue.tsx",
                                                                            lineNumber: 1133,
                                                                            columnNumber: 31
                                                                        }, this)
                                                                    ]
                                                                }, void 0, true, {
                                                                    fileName: "[project]/src/features/admissions/components/VetAdmissionQueue.tsx",
                                                                    lineNumber: 1131,
                                                                    columnNumber: 29
                                                                }, this),
                                                                /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$compiled$2f$react$2f$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$client$5d$__$28$ecmascript$29$__["jsxDEV"])("div", {
                                                                    className: "flex justify-between",
                                                                    children: [
                                                                        /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$compiled$2f$react$2f$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$client$5d$__$28$ecmascript$29$__["jsxDEV"])("span", {
                                                                            className: "text-[var(--color-text-muted)]",
                                                                            children: "Dam:"
                                                                        }, void 0, false, {
                                                                            fileName: "[project]/src/features/admissions/components/VetAdmissionQueue.tsx",
                                                                            lineNumber: 1136,
                                                                            columnNumber: 31
                                                                        }, this),
                                                                        /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$compiled$2f$react$2f$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$client$5d$__$28$ecmascript$29$__["jsxDEV"])("span", {
                                                                            className: "font-semibold",
                                                                            children: detail.candidate?.damName || 'Not recorded'
                                                                        }, void 0, false, {
                                                                            fileName: "[project]/src/features/admissions/components/VetAdmissionQueue.tsx",
                                                                            lineNumber: 1137,
                                                                            columnNumber: 31
                                                                        }, this)
                                                                    ]
                                                                }, void 0, true, {
                                                                    fileName: "[project]/src/features/admissions/components/VetAdmissionQueue.tsx",
                                                                    lineNumber: 1135,
                                                                    columnNumber: 29
                                                                }, this)
                                                            ]
                                                        }, void 0, true, {
                                                            fileName: "[project]/src/features/admissions/components/VetAdmissionQueue.tsx",
                                                            lineNumber: 1122,
                                                            columnNumber: 27
                                                        }, this)
                                                    ]
                                                }, void 0, true, {
                                                    fileName: "[project]/src/features/admissions/components/VetAdmissionQueue.tsx",
                                                    lineNumber: 1118,
                                                    columnNumber: 25
                                                }, this),
                                                /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$compiled$2f$react$2f$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$client$5d$__$28$ecmascript$29$__["jsxDEV"])("div", {
                                                    className: "rounded-[var(--radius-md)] bg-[var(--color-surface-subtle)] p-3.5 border border-[var(--color-border)]",
                                                    children: [
                                                        /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$compiled$2f$react$2f$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$client$5d$__$28$ecmascript$29$__["jsxDEV"])("h4", {
                                                            className: "text-xs font-bold text-[var(--color-text-primary)]",
                                                            children: "Groom Physical Screening"
                                                        }, void 0, false, {
                                                            fileName: "[project]/src/features/admissions/components/VetAdmissionQueue.tsx",
                                                            lineNumber: 1143,
                                                            columnNumber: 27
                                                        }, this),
                                                        /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$compiled$2f$react$2f$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$client$5d$__$28$ecmascript$29$__["jsxDEV"])("dl", {
                                                            className: "mt-2 space-y-1 text-xs",
                                                            children: [
                                                                /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$compiled$2f$react$2f$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$client$5d$__$28$ecmascript$29$__["jsxDEV"])("div", {
                                                                    className: "flex justify-between",
                                                                    children: [
                                                                        /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$compiled$2f$react$2f$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$client$5d$__$28$ecmascript$29$__["jsxDEV"])("span", {
                                                                            className: "text-[var(--color-text-muted)]",
                                                                            children: "Outcome:"
                                                                        }, void 0, false, {
                                                                            fileName: "[project]/src/features/admissions/components/VetAdmissionQueue.tsx",
                                                                            lineNumber: 1148,
                                                                            columnNumber: 31
                                                                        }, this),
                                                                        /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$compiled$2f$react$2f$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$client$5d$__$28$ecmascript$29$__["jsxDEV"])("span", {
                                                                            className: "font-semibold",
                                                                            children: detail.groomDecision ? formatLabel(detail.groomDecision) : 'Pending'
                                                                        }, void 0, false, {
                                                                            fileName: "[project]/src/features/admissions/components/VetAdmissionQueue.tsx",
                                                                            lineNumber: 1149,
                                                                            columnNumber: 31
                                                                        }, this)
                                                                    ]
                                                                }, void 0, true, {
                                                                    fileName: "[project]/src/features/admissions/components/VetAdmissionQueue.tsx",
                                                                    lineNumber: 1147,
                                                                    columnNumber: 29
                                                                }, this),
                                                                /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$compiled$2f$react$2f$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$client$5d$__$28$ecmascript$29$__["jsxDEV"])("div", {
                                                                    className: "flex justify-between",
                                                                    children: [
                                                                        /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$compiled$2f$react$2f$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$client$5d$__$28$ecmascript$29$__["jsxDEV"])("span", {
                                                                            className: "text-[var(--color-text-muted)]",
                                                                            children: "Reviewed At:"
                                                                        }, void 0, false, {
                                                                            fileName: "[project]/src/features/admissions/components/VetAdmissionQueue.tsx",
                                                                            lineNumber: 1152,
                                                                            columnNumber: 31
                                                                        }, this),
                                                                        /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$compiled$2f$react$2f$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$client$5d$__$28$ecmascript$29$__["jsxDEV"])("span", {
                                                                            className: "font-metric",
                                                                            children: formatDate(detail.groomReviewedAt, true)
                                                                        }, void 0, false, {
                                                                            fileName: "[project]/src/features/admissions/components/VetAdmissionQueue.tsx",
                                                                            lineNumber: 1153,
                                                                            columnNumber: 31
                                                                        }, this)
                                                                    ]
                                                                }, void 0, true, {
                                                                    fileName: "[project]/src/features/admissions/components/VetAdmissionQueue.tsx",
                                                                    lineNumber: 1151,
                                                                    columnNumber: 29
                                                                }, this)
                                                            ]
                                                        }, void 0, true, {
                                                            fileName: "[project]/src/features/admissions/components/VetAdmissionQueue.tsx",
                                                            lineNumber: 1146,
                                                            columnNumber: 27
                                                        }, this),
                                                        detail.groomFeedback && /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$compiled$2f$react$2f$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$client$5d$__$28$ecmascript$29$__["jsxDEV"])("p", {
                                                            className: "mt-2 rounded bg-[var(--color-surface)] p-2 text-xs text-[var(--color-text-secondary)] border border-[var(--color-border)]",
                                                            children: detail.groomFeedback
                                                        }, void 0, false, {
                                                            fileName: "[project]/src/features/admissions/components/VetAdmissionQueue.tsx",
                                                            lineNumber: 1157,
                                                            columnNumber: 29
                                                        }, this)
                                                    ]
                                                }, void 0, true, {
                                                    fileName: "[project]/src/features/admissions/components/VetAdmissionQueue.tsx",
                                                    lineNumber: 1142,
                                                    columnNumber: 25
                                                }, this)
                                            ]
                                        }, void 0, true, {
                                            fileName: "[project]/src/features/admissions/components/VetAdmissionQueue.tsx",
                                            lineNumber: 1117,
                                            columnNumber: 23
                                        }, this)
                                    ]
                                }, void 0, true, {
                                    fileName: "[project]/src/features/admissions/components/VetAdmissionQueue.tsx",
                                    lineNumber: 1078,
                                    columnNumber: 21
                                }, this)
                            ]
                        }, void 0, true, {
                            fileName: "[project]/src/features/admissions/components/VetAdmissionQueue.tsx",
                            lineNumber: 804,
                            columnNumber: 17
                        }, this) : null
                    ]
                }, void 0, true, {
                    fileName: "[project]/src/features/admissions/components/VetAdmissionQueue.tsx",
                    lineNumber: 788,
                    columnNumber: 13
                }, this),
                sidebar: examDetailsCard,
                asideHidden: activeTab === 'exam' && canCompleteExam
            }, void 0, false, {
                fileName: "[project]/src/features/admissions/components/VetAdmissionQueue.tsx",
                lineNumber: 772,
                columnNumber: 9
            }, this),
            /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$compiled$2f$react$2f$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$client$5d$__$28$ecmascript$29$__["jsxDEV"])(__TURBOPACK__imported__module__$5b$project$5d2f$src$2f$components$2f$ui$2f$ConfirmDialog$2e$tsx__$5b$app$2d$client$5d$__$28$ecmascript$29$__["ConfirmDialog"], {
                open: showCloseConfirm,
                title: "Discard Unsaved Clinical Changes?",
                description: "You have unsaved changes in this examination form. Returning to Admissions now will lose any unrecorded clinical documentation.",
                confirmLabel: "Discard & Go Back",
                cancelLabel: "Keep Editing",
                tone: "danger",
                onConfirm: ()=>{
                    setIsDrawerDirty(false);
                    setShowCloseConfirm(false);
                    setDrawerOpen(false);
                    setSelectedId(null);
                    setTargetScheduleId(null);
                    setDeepLinkedSchedule(null);
                    router.replace('/veterinarian/admissions', {
                        scroll: false
                    });
                },
                onCancel: ()=>setShowCloseConfirm(false)
            }, void 0, false, {
                fileName: "[project]/src/features/admissions/components/VetAdmissionQueue.tsx",
                lineNumber: 1175,
                columnNumber: 7
            }, this)
        ]
    }, void 0, true, {
        fileName: "[project]/src/features/admissions/components/VetAdmissionQueue.tsx",
        lineNumber: 662,
        columnNumber: 5
    }, this);
}
_s(VetAdmissionQueue, "zU/5Bfj/qzu0CMyXyBJG1Essyag=", false, function() {
    return [
        __TURBOPACK__imported__module__$5b$project$5d2f$src$2f$context$2f$AuthContext$2e$tsx__$5b$app$2d$client$5d$__$28$ecmascript$29$__["useAuth"],
        __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$navigation$2e$js__$5b$app$2d$client$5d$__$28$ecmascript$29$__["useRouter"],
        __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$navigation$2e$js__$5b$app$2d$client$5d$__$28$ecmascript$29$__["useSearchParams"]
    ];
});
_c1 = VetAdmissionQueue;
/** The list keeps its page header; an open admission brings its own header instead. */ function QueueShell({ showHeader, children }) {
    if (!showHeader) return /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$compiled$2f$react$2f$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$client$5d$__$28$ecmascript$29$__["jsxDEV"])("div", {
        className: "w-full min-w-0 space-y-5",
        children: children
    }, void 0, false, {
        fileName: "[project]/src/features/admissions/components/VetAdmissionQueue.tsx",
        lineNumber: 1199,
        columnNumber: 27
    }, this);
    return /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$compiled$2f$react$2f$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$client$5d$__$28$ecmascript$29$__["jsxDEV"])(__TURBOPACK__imported__module__$5b$project$5d2f$src$2f$features$2f$admissions$2f$shared$2f$components$2f$AdmissionListLayout$2e$tsx__$5b$app$2d$client$5d$__$28$ecmascript$29$__["AdmissionListLayout"], {
        title: "Veterinary admissions",
        description: "Review initial veterinary examinations for horse admissions.",
        children: children
    }, void 0, false, {
        fileName: "[project]/src/features/admissions/components/VetAdmissionQueue.tsx",
        lineNumber: 1201,
        columnNumber: 5
    }, this);
}
_c2 = QueueShell;
var _c, _c1, _c2;
__turbopack_context__.k.register(_c, "MedicalNoteBlock");
__turbopack_context__.k.register(_c1, "VetAdmissionQueue");
__turbopack_context__.k.register(_c2, "QueueShell");
if (typeof globalThis.$RefreshHelpers$ === 'object' && globalThis.$RefreshHelpers !== null) {
    __turbopack_context__.k.registerExports(__turbopack_context__.m, globalThis.$RefreshHelpers$);
}
}),
"[project]/src/features/admissions/components/VetReviewForm.tsx [app-client] (ecmascript)", ((__turbopack_context__) => {
"use strict";

__turbopack_context__.s([
    "VetReviewForm",
    ()=>VetReviewForm
]);
var __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$compiled$2f$react$2f$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$client$5d$__$28$ecmascript$29$__ = __turbopack_context__.i("[project]/node_modules/next/dist/compiled/react/jsx-dev-runtime.js [app-client] (ecmascript)");
var __TURBOPACK__imported__module__$5b$project$5d2f$src$2f$components$2f$ui$2f$SegmentedControl$2e$tsx__$5b$app$2d$client$5d$__$28$ecmascript$29$__ = __turbopack_context__.i("[project]/src/components/ui/SegmentedControl.tsx [app-client] (ecmascript)");
var __TURBOPACK__imported__module__$5b$project$5d2f$src$2f$components$2f$ui$2f$FormField$2e$tsx__$5b$app$2d$client$5d$__$28$ecmascript$29$__ = __turbopack_context__.i("[project]/src/components/ui/FormField.tsx [app-client] (ecmascript)");
var __TURBOPACK__imported__module__$5b$project$5d2f$src$2f$components$2f$ui$2f$Input$2e$tsx__$5b$app$2d$client$5d$__$28$ecmascript$29$__ = __turbopack_context__.i("[project]/src/components/ui/Input.tsx [app-client] (ecmascript)");
var __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$compiled$2f$react$2f$index$2e$js__$5b$app$2d$client$5d$__$28$ecmascript$29$__ = __turbopack_context__.i("[project]/node_modules/next/dist/compiled/react/index.js [app-client] (ecmascript)");
var __TURBOPACK__imported__module__$5b$project$5d2f$src$2f$services$2f$api$2e$ts__$5b$app$2d$client$5d$__$28$ecmascript$29$__ = __turbopack_context__.i("[project]/src/services/api.ts [app-client] (ecmascript)");
var __TURBOPACK__imported__module__$5b$project$5d2f$src$2f$components$2f$ui$2f$Button$2e$tsx__$5b$app$2d$client$5d$__$28$ecmascript$29$__ = __turbopack_context__.i("[project]/src/components/ui/Button.tsx [app-client] (ecmascript)");
var __TURBOPACK__imported__module__$5b$project$5d2f$src$2f$components$2f$ui$2f$ConfirmDialog$2e$tsx__$5b$app$2d$client$5d$__$28$ecmascript$29$__ = __turbopack_context__.i("[project]/src/components/ui/ConfirmDialog.tsx [app-client] (ecmascript)");
var __TURBOPACK__imported__module__$5b$project$5d2f$src$2f$components$2f$ui$2f$Notice$2e$tsx__$5b$app$2d$client$5d$__$28$ecmascript$29$__ = __turbopack_context__.i("[project]/src/components/ui/Notice.tsx [app-client] (ecmascript)");
var __TURBOPACK__imported__module__$5b$project$5d2f$src$2f$features$2f$admissions$2f$shared$2f$components$2f$AdmissionDetailLayout$2e$tsx__$5b$app$2d$client$5d$__$28$ecmascript$29$__ = __turbopack_context__.i("[project]/src/features/admissions/shared/components/AdmissionDetailLayout.tsx [app-client] (ecmascript)");
var __TURBOPACK__imported__module__$5b$project$5d2f$src$2f$components$2f$ui$2f$Icon$2e$tsx__$5b$app$2d$client$5d$__$28$ecmascript$29$__ = __turbopack_context__.i("[project]/src/components/ui/Icon.tsx [app-client] (ecmascript)");
var __TURBOPACK__imported__module__$5b$project$5d2f$src$2f$components$2f$ui$2f$StatusBadge$2e$tsx__$5b$app$2d$client$5d$__$28$ecmascript$29$__ = __turbopack_context__.i("[project]/src/components/ui/StatusBadge.tsx [app-client] (ecmascript)");
var __TURBOPACK__imported__module__$5b$project$5d2f$src$2f$features$2f$admissions$2f$services$2f$api$2e$ts__$5b$app$2d$client$5d$__$28$ecmascript$29$__ = __turbopack_context__.i("[project]/src/features/admissions/services/api.ts [app-client] (ecmascript)");
;
var _s = __turbopack_context__.k.signature();
'use client';
;
;
;
;
;
;
;
;
;
;
;
;
const bodySystems = [
    {
        key: 'general',
        label: 'General Condition'
    },
    {
        key: 'respiratory',
        label: 'Respiratory'
    },
    {
        key: 'cardiovascular',
        label: 'Cardiovascular'
    },
    {
        key: 'musculoskeletal',
        label: 'Musculoskeletal'
    },
    {
        key: 'neurological',
        label: 'Neurological'
    },
    {
        key: 'skinCoat',
        label: 'Skin & Coat'
    },
    {
        key: 'hooves',
        label: 'Hooves'
    },
    {
        key: 'digestive',
        label: 'Digestive'
    }
];
const findingLevelLabels = {
    NORMAL: 'Normal',
    MILD: 'Mild abnormality',
    MODERATE: 'Moderate abnormality',
    SEVERE: 'Severe abnormality',
    NOT_EXAMINED: 'Not examined'
};
const normalSystemFindings = Object.fromEntries(bodySystems.map(({ key })=>[
        key,
        'NORMAL'
    ]));
const symptomOptions = [
    'No symptoms observed',
    'Cough',
    'Nasal discharge',
    'Elevated temperature',
    'Lameness',
    'Joint swelling',
    'Reduced flexion',
    'Hoof pain',
    'Skin lesion',
    'Poor appetite',
    'Digestive discomfort',
    'Neurological signs'
];
const diagnosisOptions = [
    'Clinically healthy',
    'Minor clinical condition',
    'Treatment required',
    'Follow-up examination required',
    'Infectious disease suspected',
    'Musculoskeletal condition suspected',
    'Medically unsuitable for admission'
];
const restrictionOptions = [
    'Walking only; no trotting, cantering, or galloping',
    'Light exercise only; maximum 20 minutes per session',
    'No strenuous exercise until veterinary re-examination',
    'Stall rest with hand-walking only',
    'Complete stall rest; no training activity',
    'Isolation protocol; no contact with other horses'
];
const followUpDescriptionOptions = [
    'Routine follow-up recheck examination',
    'Post-treatment clinical reassessment',
    'Lameness and gait reassessment',
    'Respiratory and infectious disease recheck',
    'Training restriction clearance examination'
];
const trainingDecisions = [
    {
        value: 'ALLOWED',
        title: 'Training Allowed',
        subtitle: 'Medically cleared for athletic conditioning',
        icon: 'check',
        tone: 'success',
        activeBorder: 'border-[var(--color-success)] ring-1 ring-[var(--color-success)]',
        activeBg: 'bg-[var(--color-success-soft)]'
    },
    {
        value: 'BLOCKED',
        title: 'Training Blocked',
        subtitle: 'No training until the follow-up exam; future workouts are cancelled',
        icon: 'lock',
        tone: 'danger',
        activeBorder: 'border-[var(--color-danger)] ring-1 ring-[var(--color-danger)]',
        activeBg: 'bg-[var(--color-danger-soft)]'
    }
];
const metricConfigs = [
    {
        key: 'temperature',
        label: 'Temperature',
        unit: '°C',
        step: '0.1',
        placeholder: '38.0',
        normalMin: 37.2,
        normalMax: 38.3,
        criticalMin: 36.5,
        criticalMax: 38.8,
        normalLabel: '37.2 – 38.3 °C'
    },
    {
        key: 'heartRate',
        label: 'Heart Rate',
        unit: 'bpm',
        step: '1',
        placeholder: '36',
        normalMin: 28,
        normalMax: 44,
        criticalMin: 24,
        criticalMax: 55,
        normalLabel: '28 – 44 bpm'
    },
    {
        key: 'respiratoryRate',
        label: 'Resp Rate',
        unit: 'rpm',
        step: '1',
        placeholder: '12',
        normalMin: 8,
        normalMax: 16,
        criticalMin: 6,
        criticalMax: 24,
        normalLabel: '8 – 16 rpm'
    },
    {
        key: 'weight',
        label: 'Weight',
        unit: 'kg',
        step: '0.5',
        placeholder: '500',
        normalMin: 400,
        normalMax: 600,
        criticalMin: 350,
        criticalMax: 650,
        normalLabel: '400 – 600 kg'
    },
    {
        key: 'bodyConditionScore',
        label: 'BCS',
        unit: '/ 9',
        step: '0.5',
        placeholder: '5.0',
        normalMin: 4.0,
        normalMax: 6.0,
        criticalMin: 3.0,
        criticalMax: 7.0,
        normalLabel: '4.0 – 6.0'
    }
];
const emptyMetrics = {
    heartRate: '',
    temperature: '',
    weight: '',
    respiratoryRate: '',
    hydrationStatus: '',
    bodyConditionScore: '',
    notes: ''
};
const BUSINESS_TIME_ZONE = 'Asia/Ho_Chi_Minh';
function businessDateFromToday(dayOffset) {
    const parts = new Intl.DateTimeFormat('en-CA', {
        timeZone: BUSINESS_TIME_ZONE,
        year: 'numeric',
        month: '2-digit',
        day: '2-digit'
    }).formatToParts(new Date());
    const year = Number(parts.find((part)=>part.type === 'year')?.value);
    const month = Number(parts.find((part)=>part.type === 'month')?.value);
    const day = Number(parts.find((part)=>part.type === 'day')?.value);
    const target = new Date(Date.UTC(year, month - 1, day + dayOffset));
    return [
        target.getUTCFullYear(),
        String(target.getUTCMonth() + 1).padStart(2, '0'),
        String(target.getUTCDate()).padStart(2, '0')
    ].join('-');
}
function tomorrowBusinessDate() {
    return businessDateFromToday(1);
}
function getMetricBorderTone(valStr, normalMin, normalMax, criticalMin, criticalMax) {
    if (!valStr.trim()) {
        return {
            borderClass: 'border-[var(--color-border)] focus:border-[var(--color-primary)]',
            statusLabel: null,
            tone: null
        };
    }
    const num = Number(valStr);
    if (Number.isNaN(num)) {
        return {
            borderClass: 'border-[var(--color-danger)] focus:border-[var(--color-danger)] ring-1 ring-[var(--color-danger)]',
            statusLabel: 'Invalid',
            tone: 'danger'
        };
    }
    if (num < criticalMin) {
        return {
            borderClass: 'border-[var(--color-danger)] focus:border-[var(--color-danger)] ring-1 ring-[var(--color-danger)]',
            statusLabel: 'Critical Low',
            tone: 'danger'
        };
    }
    if (num > criticalMax) {
        return {
            borderClass: 'border-[var(--color-danger)] focus:border-[var(--color-danger)] ring-1 ring-[var(--color-danger)]',
            statusLabel: 'Critical High',
            tone: 'danger'
        };
    }
    if (num < normalMin) {
        return {
            borderClass: 'border-[var(--color-warning)] focus:border-[var(--color-warning)] ring-1 ring-[var(--color-warning)]',
            statusLabel: 'Low',
            tone: 'warning'
        };
    }
    if (num > normalMax) {
        return {
            borderClass: 'border-[var(--color-warning)] focus:border-[var(--color-warning)] ring-1 ring-[var(--color-warning)]',
            statusLabel: 'Elevated',
            tone: 'warning'
        };
    }
    return {
        borderClass: 'border-[var(--color-success)] focus:border-[var(--color-success)]',
        statusLabel: 'Normal',
        tone: 'success'
    };
}
function VetReviewForm({ admissionId, horseId, candidateName = 'Candidate horse', quarantineStallCode, careScheduleId, careType = 'INITIAL', scheduleStatus, onStartExam, asideTop, onSuccess, onDirtyChange }) {
    _s();
    const draftStorageKey = careScheduleId ? `draft:care-schedule:${careScheduleId}` : `draft:admission:${admissionId}`;
    // Decision Panel State
    const [trainingDecision, setTrainingDecision] = (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$compiled$2f$react$2f$index$2e$js__$5b$app$2d$client$5d$__$28$ecmascript$29$__["useState"])('ALLOWED');
    const [restrictionDetails, setRestrictionDetails] = (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$compiled$2f$react$2f$index$2e$js__$5b$app$2d$client$5d$__$28$ecmascript$29$__["useState"])('');
    const [scheduleFollowUp, setScheduleFollowUp] = (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$compiled$2f$react$2f$index$2e$js__$5b$app$2d$client$5d$__$28$ecmascript$29$__["useState"])(false);
    const [followUpDate, setFollowUpDate] = (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$compiled$2f$react$2f$index$2e$js__$5b$app$2d$client$5d$__$28$ecmascript$29$__["useState"])('');
    const [followUpDescription, setFollowUpDescription] = (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$compiled$2f$react$2f$index$2e$js__$5b$app$2d$client$5d$__$28$ecmascript$29$__["useState"])('Routine follow-up recheck examination');
    const [followUpIdempotencyKey, setFollowUpIdempotencyKey] = (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$compiled$2f$react$2f$index$2e$js__$5b$app$2d$client$5d$__$28$ecmascript$29$__["useState"])({
        "VetReviewForm.useState": ()=>typeof crypto !== 'undefined' && 'randomUUID' in crypto ? crypto.randomUUID() : `follow-up-${Date.now()}-${Math.random().toString(36).slice(2)}`
    }["VetReviewForm.useState"]);
    // Clinical Findings Form State
    const [physicalExamConfirmed, setPhysicalExamConfirmed] = (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$compiled$2f$react$2f$index$2e$js__$5b$app$2d$client$5d$__$28$ecmascript$29$__["useState"])(true);
    const [examMode, setExamMode] = (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$compiled$2f$react$2f$index$2e$js__$5b$app$2d$client$5d$__$28$ecmascript$29$__["useState"])('NORMAL');
    const [systemFindings, setSystemFindings] = (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$compiled$2f$react$2f$index$2e$js__$5b$app$2d$client$5d$__$28$ecmascript$29$__["useState"])({
        ...normalSystemFindings
    });
    const [selectedSymptoms, setSelectedSymptoms] = (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$compiled$2f$react$2f$index$2e$js__$5b$app$2d$client$5d$__$28$ecmascript$29$__["useState"])([
        'No symptoms observed'
    ]);
    const [structuredDiagnosis, setStructuredDiagnosis] = (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$compiled$2f$react$2f$index$2e$js__$5b$app$2d$client$5d$__$28$ecmascript$29$__["useState"])('Clinically healthy');
    const [treatment, setTreatment] = (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$compiled$2f$react$2f$index$2e$js__$5b$app$2d$client$5d$__$28$ecmascript$29$__["useState"])('');
    const [notes, setNotes] = (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$compiled$2f$react$2f$index$2e$js__$5b$app$2d$client$5d$__$28$ecmascript$29$__["useState"])('');
    // Urgent form fields override
    const [symptoms, setSymptoms] = (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$compiled$2f$react$2f$index$2e$js__$5b$app$2d$client$5d$__$28$ecmascript$29$__["useState"])('');
    const [findings, setFindings] = (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$compiled$2f$react$2f$index$2e$js__$5b$app$2d$client$5d$__$28$ecmascript$29$__["useState"])('');
    const [diagnosis, setDiagnosis] = (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$compiled$2f$react$2f$index$2e$js__$5b$app$2d$client$5d$__$28$ecmascript$29$__["useState"])('');
    // Vitals State
    const [metrics, setMetrics] = (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$compiled$2f$react$2f$index$2e$js__$5b$app$2d$client$5d$__$28$ecmascript$29$__["useState"])(emptyMetrics);
    // Status & Validation
    const [fieldErrors, setFieldErrors] = (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$compiled$2f$react$2f$index$2e$js__$5b$app$2d$client$5d$__$28$ecmascript$29$__["useState"])({});
    const [formError, setFormError] = (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$compiled$2f$react$2f$index$2e$js__$5b$app$2d$client$5d$__$28$ecmascript$29$__["useState"])('');
    const [submitting, setSubmitting] = (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$compiled$2f$react$2f$index$2e$js__$5b$app$2d$client$5d$__$28$ecmascript$29$__["useState"])(false);
    const [startingExam, setStartingExam] = (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$compiled$2f$react$2f$index$2e$js__$5b$app$2d$client$5d$__$28$ecmascript$29$__["useState"])(false);
    const [showApproveConfirm, setShowApproveConfirm] = (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$compiled$2f$react$2f$index$2e$js__$5b$app$2d$client$5d$__$28$ecmascript$29$__["useState"])(false);
    const [availableDraft, setAvailableDraft] = (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$compiled$2f$react$2f$index$2e$js__$5b$app$2d$client$5d$__$28$ecmascript$29$__["useState"])({
        "VetReviewForm.useState": ()=>{
            if ("TURBOPACK compile-time falsy", 0) //TURBOPACK unreachable
            ;
            try {
                const storageKey = draftStorageKey;
                const fallbackKey = careScheduleId ? null : `rtms_vet_draft_${admissionId}`;
                const saved = localStorage.getItem(storageKey) || (fallbackKey ? localStorage.getItem(fallbackKey) : null);
                if (saved) {
                    const parsed = JSON.parse(saved);
                    if (parsed && parsed.timestamp) {
                        return parsed;
                    }
                }
            } catch  {
            // Ignore storage read errors (e.g. private browsing)
            }
            return null;
        }
    }["VetReviewForm.useState"]);
    const [draftSavedTime, setDraftSavedTime] = (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$compiled$2f$react$2f$index$2e$js__$5b$app$2d$client$5d$__$28$ecmascript$29$__["useState"])(null);
    const inFlight = (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$compiled$2f$react$2f$index$2e$js__$5b$app$2d$client$5d$__$28$ecmascript$29$__["useRef"])(false);
    const isGated = scheduleStatus != null && scheduleStatus !== 'IN_PROGRESS';
    // Blocking training always needs a follow-up exam: "rest until" is that exam's date.
    const followUpRequired = trainingDecision === 'BLOCKED';
    const followUpActive = scheduleFollowUp || followUpRequired;
    const isFormDisabled = submitting || startingExam || isGated;
    const isUrgent = careType === 'URGENT';
    // Compute dirty state
    const isDirty = metrics.heartRate !== '' || metrics.temperature !== '' || metrics.weight !== '' || metrics.respiratoryRate !== '' || metrics.bodyConditionScore !== '' || metrics.hydrationStatus !== '' || metrics.notes !== '' || symptoms.trim() !== '' || findings.trim() !== '' || diagnosis.trim() !== '' || examMode !== 'NORMAL' || Object.values(systemFindings).some((f)=>f !== 'NORMAL') || selectedSymptoms.length > 1 || selectedSymptoms.length === 1 && selectedSymptoms[0] !== 'No symptoms observed' || structuredDiagnosis !== 'Clinically healthy' || treatment.trim() !== '' || notes.trim() !== '' || trainingDecision !== 'ALLOWED' || restrictionDetails.trim() !== '' || scheduleFollowUp || followUpDate !== '';
    (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$compiled$2f$react$2f$index$2e$js__$5b$app$2d$client$5d$__$28$ecmascript$29$__["useEffect"])({
        "VetReviewForm.useEffect": ()=>{
            onDirtyChange?.(isDirty);
        }
    }["VetReviewForm.useEffect"], [
        isDirty,
        onDirtyChange
    ]);
    const handleRestoreDraft = ()=>{
        if (!availableDraft) return;
        if (availableDraft.metrics) setMetrics(availableDraft.metrics);
        if (availableDraft.examMode) setExamMode(availableDraft.examMode);
        if (availableDraft.systemFindings) setSystemFindings(availableDraft.systemFindings);
        if (availableDraft.selectedSymptoms) setSelectedSymptoms(availableDraft.selectedSymptoms);
        if (availableDraft.structuredDiagnosis) setStructuredDiagnosis(availableDraft.structuredDiagnosis);
        if (availableDraft.treatment !== undefined) setTreatment(availableDraft.treatment);
        if (availableDraft.notes !== undefined) setNotes(availableDraft.notes);
        if (availableDraft.symptoms !== undefined) setSymptoms(availableDraft.symptoms);
        if (availableDraft.findings !== undefined) setFindings(availableDraft.findings);
        if (availableDraft.diagnosis !== undefined) setDiagnosis(availableDraft.diagnosis);
        // Old drafts may still hold RESTRICTED -> it is now merged into BLOCKED.
        if (availableDraft.trainingDecision) {
            setTrainingDecision(availableDraft.trainingDecision === 'ALLOWED' ? 'ALLOWED' : 'BLOCKED');
        }
        if (availableDraft.restrictionDetails !== undefined) setRestrictionDetails(availableDraft.restrictionDetails);
        if (availableDraft.scheduleFollowUp !== undefined) setScheduleFollowUp(availableDraft.scheduleFollowUp);
        if (availableDraft.followUpDate !== undefined) setFollowUpDate(availableDraft.followUpDate);
        if (availableDraft.followUpDescription !== undefined) setFollowUpDescription(availableDraft.followUpDescription);
        if (availableDraft.followUpIdempotencyKey) setFollowUpIdempotencyKey(availableDraft.followUpIdempotencyKey);
        setDraftSavedTime(availableDraft.timestamp);
        setAvailableDraft(null);
    };
    const handleDiscardAvailableDraft = ()=>{
        try {
            localStorage.removeItem(draftStorageKey);
            localStorage.removeItem(`rtms_vet_draft_${admissionId}`);
        } catch  {
        // Ignore storage errors in restricted environments
        }
        setAvailableDraft(null);
    };
    const handleSaveDraft = (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$compiled$2f$react$2f$index$2e$js__$5b$app$2d$client$5d$__$28$ecmascript$29$__["useCallback"])({
        "VetReviewForm.useCallback[handleSaveDraft]": ()=>{
            try {
                const storageKey = draftStorageKey;
                const timestamp = new Date().toLocaleTimeString([], {
                    hour: '2-digit',
                    minute: '2-digit',
                    second: '2-digit'
                });
                const payload = {
                    metrics,
                    examMode,
                    systemFindings,
                    selectedSymptoms,
                    structuredDiagnosis,
                    treatment,
                    notes,
                    symptoms,
                    findings,
                    diagnosis,
                    trainingDecision,
                    restrictionDetails,
                    scheduleFollowUp,
                    followUpDate,
                    followUpDescription,
                    followUpIdempotencyKey,
                    timestamp
                };
                localStorage.setItem(storageKey, JSON.stringify(payload));
                if (!careScheduleId) localStorage.setItem(`rtms_vet_draft_${admissionId}`, JSON.stringify(payload));
                setDraftSavedTime(timestamp);
            } catch  {
            // Storage write error
            }
        }
    }["VetReviewForm.useCallback[handleSaveDraft]"], [
        admissionId,
        careScheduleId,
        draftStorageKey,
        metrics,
        examMode,
        systemFindings,
        selectedSymptoms,
        structuredDiagnosis,
        treatment,
        notes,
        symptoms,
        findings,
        diagnosis,
        trainingDecision,
        restrictionDetails,
        scheduleFollowUp,
        followUpDate,
        followUpDescription,
        followUpIdempotencyKey
    ]);
    const handleClearDraft = ()=>{
        try {
            localStorage.removeItem(draftStorageKey);
            localStorage.removeItem(`rtms_vet_draft_${admissionId}`);
        } catch  {
        // Ignore storage errors in restricted environments
        }
        setMetrics(emptyMetrics);
        setExamMode('NORMAL');
        setSystemFindings({
            ...normalSystemFindings
        });
        setSelectedSymptoms([
            'No symptoms observed'
        ]);
        setStructuredDiagnosis('Clinically healthy');
        setTreatment('');
        setNotes('');
        setSymptoms('');
        setFindings('');
        setDiagnosis('');
        setTrainingDecision('ALLOWED');
        setRestrictionDetails('');
        setScheduleFollowUp(false);
        setFollowUpDate('');
        setFollowUpDescription('Routine follow-up recheck examination');
        setPhysicalExamConfirmed(true);
        setFieldErrors({});
        setDraftSavedTime(null);
    };
    // 15-second debounce auto-save when dirty (FR-CHUNG-93)
    // Do NOT autosave while an unrestored draft banner is pending user action to prevent overwriting
    (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$compiled$2f$react$2f$index$2e$js__$5b$app$2d$client$5d$__$28$ecmascript$29$__["useEffect"])({
        "VetReviewForm.useEffect": ()=>{
            if (!isDirty || isGated || availableDraft !== null) return;
            const timer = setTimeout({
                "VetReviewForm.useEffect.timer": ()=>{
                    handleSaveDraft();
                }
            }["VetReviewForm.useEffect.timer"], 15000);
            return ({
                "VetReviewForm.useEffect": ()=>clearTimeout(timer)
            })["VetReviewForm.useEffect"];
        }
    }["VetReviewForm.useEffect"], [
        isDirty,
        isGated,
        availableDraft,
        handleSaveDraft
    ]);
    const generatedSymptoms = examMode === 'NORMAL' ? 'No symptoms observed' : selectedSymptoms.join('; ') || 'No presenting symptoms selected';
    const generatedFindings = examMode === 'NORMAL' ? 'Physical examination completed in person. General condition, respiratory, cardiovascular, musculoskeletal, neurological, skin and coat, hooves, and digestive systems were within normal limits.' : `Physical examination completed in person. ${bodySystems.map(({ key, label })=>`${label}: ${findingLevelLabels[systemFindings[key]].toLowerCase()}`).join('; ')}.`;
    const effectiveSymptoms = isUrgent ? symptoms.trim() : generatedSymptoms;
    const effectiveFindings = isUrgent ? findings.trim() : generatedFindings;
    const effectiveDiagnosis = isUrgent ? diagnosis.trim() : structuredDiagnosis;
    function validate() {
        const errors = {};
        if (!physicalExamConfirmed) {
            errors.physicalExamConfirmed = 'In-person physical examination confirmation is required.';
        }
        if (isUrgent && !symptoms.trim()) {
            errors.symptoms = 'Detailed presenting symptoms are required for urgent care.';
        }
        if (!effectiveFindings) {
            errors.findings = 'Clinical examination findings are required.';
        }
        if (!effectiveDiagnosis) {
            errors.diagnosis = 'Clinical diagnosis is required.';
        }
        if (!isUrgent && examMode === 'ABNORMAL' && bodySystems.every(({ key })=>systemFindings[key] === 'NORMAL')) {
            errors.systemFindings = 'Select at least one abnormal or not-examined body system.';
        }
        if (trainingDecision === 'BLOCKED' && !restrictionDetails.trim()) {
            errors.restrictionDetails = 'Restriction details are mandatory when training is blocked.';
        }
        if (followUpActive) {
            if (!followUpDate) {
                errors.followUpDate = 'Follow-up date is required when scheduling follow-up care.';
            } else if (followUpDate < tomorrowBusinessDate()) {
                errors.followUpDate = 'Follow-up date must be at least one day in the future.';
            }
            if (!followUpDescription.trim()) {
                errors.followUpDescription = 'Follow-up care description is required.';
            }
        }
        for (const cfg of metricConfigs){
            const val = metrics[cfg.key];
            if (val && val.trim()) {
                const num = Number(val);
                if (Number.isNaN(num) || num <= 0) {
                    errors[cfg.key] = `${cfg.label} must be a valid positive number.`;
                }
            }
        }
        return errors;
    }
    function handleFormSubmit(event) {
        event.preventDefault();
        if (scheduleStatus && scheduleStatus !== 'IN_PROGRESS') {
            setFormError('Care schedule is SCHEDULED. Physical examination must be started before recording and submitting findings.');
            return;
        }
        const errors = validate();
        setFieldErrors(errors);
        if (Object.keys(errors).length > 0) {
            setFormError('Please resolve all validation errors before submitting.');
            return;
        }
        setFormError('');
        setShowApproveConfirm(true);
    }
    async function executeSubmit() {
        if (inFlight.current) return;
        if (scheduleStatus && scheduleStatus !== 'IN_PROGRESS') {
            setFormError('Care schedule is SCHEDULED. Physical examination must be started before recording and submitting findings.');
            setShowApproveConfirm(false);
            return;
        }
        const errors = validate();
        if (Object.keys(errors).length > 0) {
            setFieldErrors(errors);
            const msg = 'Please resolve all validation errors before submitting.';
            setFormError(msg);
            setShowApproveConfirm(false);
            return;
        }
        inFlight.current = true;
        setSubmitting(true);
        setFormError('');
        const metricValues = Object.fromEntries(Object.entries(metrics).filter(([, value])=>value.trim()).map(([key, value])=>[
                key,
                [
                    'hydrationStatus',
                    'notes'
                ].includes(key) ? value.trim() : Number(value)
            ]));
        const nextScheduleRequest = followUpActive && followUpDate ? {
            horseId: horseId ?? undefined,
            admissionId,
            careType: 'ROUTINE',
            scheduledDate: followUpDate,
            description: followUpDescription.trim() || 'Follow-up veterinary examination',
            idempotencyKey: followUpIdempotencyKey
        } : undefined;
        try {
            let result;
            let completionKind;
            if (careType === 'INITIAL') {
                const request = {
                    careScheduleId: careScheduleId ?? undefined,
                    trainingDecision,
                    restrictionDetails: restrictionDetails.trim() || undefined,
                    physicalExamConfirmed: true,
                    findings: effectiveFindings,
                    diagnosis: effectiveDiagnosis,
                    treatment: treatment.trim() || undefined,
                    symptoms: effectiveSymptoms || undefined,
                    notes: notes.trim() || undefined,
                    feedback: notes.trim() || undefined,
                    nextSchedule: nextScheduleRequest,
                    metrics: Object.keys(metricValues).length ? [
                        metricValues
                    ] : undefined
                };
                result = await __TURBOPACK__imported__module__$5b$project$5d2f$src$2f$features$2f$admissions$2f$services$2f$api$2e$ts__$5b$app$2d$client$5d$__$28$ecmascript$29$__["admissionsApi"].vetReview(admissionId, request);
                completionKind = 'INITIAL';
            } else {
                if (!careScheduleId) throw new Error('The care schedule identifier is missing. Reload this examination and try again.');
                const request = {
                    findings: effectiveFindings,
                    diagnosis: effectiveDiagnosis,
                    symptoms: effectiveSymptoms || undefined,
                    treatment: treatment.trim() || undefined,
                    trainingDecision,
                    restrictionDetails: restrictionDetails.trim() || undefined,
                    notes: notes.trim() || undefined,
                    nextSchedule: nextScheduleRequest,
                    metrics: Object.keys(metricValues).length ? [
                        metricValues
                    ] : undefined
                };
                result = await __TURBOPACK__imported__module__$5b$project$5d2f$src$2f$features$2f$admissions$2f$services$2f$api$2e$ts__$5b$app$2d$client$5d$__$28$ecmascript$29$__["admissionsApi"].completeCareSchedule(careScheduleId, request);
                completionKind = 'CARE_SCHEDULE';
            }
            // Clear draft on successful submission
            try {
                localStorage.removeItem(draftStorageKey);
                localStorage.removeItem(`rtms_vet_draft_${admissionId}`);
            } catch  {
            // Ignore
            }
            setShowApproveConfirm(false);
            onSuccess(result, completionKind);
        } catch (cause) {
            if (cause instanceof __TURBOPACK__imported__module__$5b$project$5d2f$src$2f$services$2f$api$2e$ts__$5b$app$2d$client$5d$__$28$ecmascript$29$__["ApiError"]) {
                const errorMsg = `${cause.status === 403 ? 'Permission denied' : cause.status === 409 ? 'Review state conflict' : 'Review failed'} (${cause.status}${cause.errorCode ? ` · ${cause.errorCode}` : ''}): ${cause.message}`;
                setFormError(errorMsg);
            } else {
                const errorMsg = cause instanceof Error ? cause.message : 'Failed to submit review.';
                setFormError(errorMsg);
            }
            setShowApproveConfirm(false);
        } finally{
            inFlight.current = false;
            setSubmitting(false);
        }
    }
    return /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$compiled$2f$react$2f$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$client$5d$__$28$ecmascript$29$__["jsxDEV"])(__TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$compiled$2f$react$2f$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$client$5d$__$28$ecmascript$29$__["Fragment"], {
        children: [
            /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$compiled$2f$react$2f$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$client$5d$__$28$ecmascript$29$__["jsxDEV"])("form", {
                onSubmit: handleFormSubmit,
                className: "pb-24 md:pb-0",
                children: [
                    scheduleStatus === 'SCHEDULED' && /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$compiled$2f$react$2f$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$client$5d$__$28$ecmascript$29$__["jsxDEV"])("div", {
                        role: "status",
                        className: "mb-4 flex flex-wrap items-center justify-between gap-3 rounded-[var(--radius-md)] border border-[var(--color-warning)] bg-[var(--color-warning-soft)] p-3 text-sm text-[var(--color-warning)]",
                        children: [
                            /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$compiled$2f$react$2f$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$client$5d$__$28$ecmascript$29$__["jsxDEV"])("div", {
                                className: "flex items-center gap-2",
                                children: [
                                    /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$compiled$2f$react$2f$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$client$5d$__$28$ecmascript$29$__["jsxDEV"])(__TURBOPACK__imported__module__$5b$project$5d2f$src$2f$components$2f$ui$2f$Icon$2e$tsx__$5b$app$2d$client$5d$__$28$ecmascript$29$__["Icon"], {
                                        name: "alert-triangle",
                                        size: 18,
                                        className: "shrink-0 text-[var(--color-warning)]"
                                    }, void 0, false, {
                                        fileName: "[project]/src/features/admissions/components/VetReviewForm.tsx",
                                        lineNumber: 764,
                                        columnNumber: 15
                                    }, this),
                                    /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$compiled$2f$react$2f$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$client$5d$__$28$ecmascript$29$__["jsxDEV"])("div", {
                                        children: [
                                            /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$compiled$2f$react$2f$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$client$5d$__$28$ecmascript$29$__["jsxDEV"])("strong", {
                                                className: "block text-sm font-bold",
                                                children: "Care schedule is SCHEDULED"
                                            }, void 0, false, {
                                                fileName: "[project]/src/features/admissions/components/VetReviewForm.tsx",
                                                lineNumber: 766,
                                                columnNumber: 17
                                            }, this),
                                            /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$compiled$2f$react$2f$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$client$5d$__$28$ecmascript$29$__["jsxDEV"])("p", {
                                                className: "text-xs opacity-90",
                                                children: "Physical examination must be started before clinical findings and decisions can be recorded."
                                            }, void 0, false, {
                                                fileName: "[project]/src/features/admissions/components/VetReviewForm.tsx",
                                                lineNumber: 769,
                                                columnNumber: 17
                                            }, this)
                                        ]
                                    }, void 0, true, {
                                        fileName: "[project]/src/features/admissions/components/VetReviewForm.tsx",
                                        lineNumber: 765,
                                        columnNumber: 15
                                    }, this)
                                ]
                            }, void 0, true, {
                                fileName: "[project]/src/features/admissions/components/VetReviewForm.tsx",
                                lineNumber: 763,
                                columnNumber: 13
                            }, this),
                            onStartExam && !asideTop && /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$compiled$2f$react$2f$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$client$5d$__$28$ecmascript$29$__["jsxDEV"])(__TURBOPACK__imported__module__$5b$project$5d2f$src$2f$components$2f$ui$2f$Button$2e$tsx__$5b$app$2d$client$5d$__$28$ecmascript$29$__["Button"], {
                                type: "button",
                                size: "sm",
                                variant: "primary",
                                loading: startingExam,
                                onClick: async ()=>{
                                    try {
                                        setStartingExam(true);
                                        await onStartExam();
                                    } finally{
                                        setStartingExam(false);
                                    }
                                },
                                icon: "activity",
                                children: "Start Examination"
                            }, void 0, false, {
                                fileName: "[project]/src/features/admissions/components/VetReviewForm.tsx",
                                lineNumber: 775,
                                columnNumber: 15
                            }, this)
                        ]
                    }, void 0, true, {
                        fileName: "[project]/src/features/admissions/components/VetReviewForm.tsx",
                        lineNumber: 759,
                        columnNumber: 11
                    }, this),
                    scheduleStatus === 'REQUESTED' && /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$compiled$2f$react$2f$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$client$5d$__$28$ecmascript$29$__["jsxDEV"])("div", {
                        role: "status",
                        className: "mb-4 flex flex-wrap items-center justify-between gap-3 rounded-[var(--radius-md)] border border-[var(--color-info)] bg-[var(--color-info-soft)] p-3 text-sm text-[var(--color-info)]",
                        children: /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$compiled$2f$react$2f$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$client$5d$__$28$ecmascript$29$__["jsxDEV"])("div", {
                            className: "flex items-center gap-2",
                            children: [
                                /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$compiled$2f$react$2f$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$client$5d$__$28$ecmascript$29$__["jsxDEV"])(__TURBOPACK__imported__module__$5b$project$5d2f$src$2f$components$2f$ui$2f$Icon$2e$tsx__$5b$app$2d$client$5d$__$28$ecmascript$29$__["Icon"], {
                                    name: "clock",
                                    size: 18,
                                    className: "shrink-0 text-[var(--color-info)]"
                                }, void 0, false, {
                                    fileName: "[project]/src/features/admissions/components/VetReviewForm.tsx",
                                    lineNumber: 802,
                                    columnNumber: 15
                                }, this),
                                /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$compiled$2f$react$2f$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$client$5d$__$28$ecmascript$29$__["jsxDEV"])("div", {
                                    children: [
                                        /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$compiled$2f$react$2f$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$client$5d$__$28$ecmascript$29$__["jsxDEV"])("strong", {
                                            className: "block text-sm font-bold",
                                            children: "Care schedule is REQUESTED"
                                        }, void 0, false, {
                                            fileName: "[project]/src/features/admissions/components/VetReviewForm.tsx",
                                            lineNumber: 804,
                                            columnNumber: 17
                                        }, this),
                                        /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$compiled$2f$react$2f$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$client$5d$__$28$ecmascript$29$__["jsxDEV"])("p", {
                                            className: "text-xs opacity-90",
                                            children: "This examination schedule is waiting for slot scheduling and assignment before it can be started."
                                        }, void 0, false, {
                                            fileName: "[project]/src/features/admissions/components/VetReviewForm.tsx",
                                            lineNumber: 807,
                                            columnNumber: 17
                                        }, this)
                                    ]
                                }, void 0, true, {
                                    fileName: "[project]/src/features/admissions/components/VetReviewForm.tsx",
                                    lineNumber: 803,
                                    columnNumber: 15
                                }, this)
                            ]
                        }, void 0, true, {
                            fileName: "[project]/src/features/admissions/components/VetReviewForm.tsx",
                            lineNumber: 801,
                            columnNumber: 13
                        }, this)
                    }, void 0, false, {
                        fileName: "[project]/src/features/admissions/components/VetReviewForm.tsx",
                        lineNumber: 797,
                        columnNumber: 11
                    }, this),
                    formError && /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$compiled$2f$react$2f$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$client$5d$__$28$ecmascript$29$__["jsxDEV"])("div", {
                        role: "alert",
                        className: "mb-4 flex items-start gap-2.5 rounded-[var(--radius-md)] border border-[var(--color-danger)] bg-[var(--color-danger-soft)] p-3 text-sm text-[var(--color-danger)]",
                        children: [
                            /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$compiled$2f$react$2f$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$client$5d$__$28$ecmascript$29$__["jsxDEV"])(__TURBOPACK__imported__module__$5b$project$5d2f$src$2f$components$2f$ui$2f$Icon$2e$tsx__$5b$app$2d$client$5d$__$28$ecmascript$29$__["Icon"], {
                                name: "alert-triangle",
                                size: 16,
                                className: "mt-0.5 shrink-0"
                            }, void 0, false, {
                                fileName: "[project]/src/features/admissions/components/VetReviewForm.tsx",
                                lineNumber: 821,
                                columnNumber: 13
                            }, this),
                            /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$compiled$2f$react$2f$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$client$5d$__$28$ecmascript$29$__["jsxDEV"])("span", {
                                children: formError
                            }, void 0, false, {
                                fileName: "[project]/src/features/admissions/components/VetReviewForm.tsx",
                                lineNumber: 822,
                                columnNumber: 13
                            }, this)
                        ]
                    }, void 0, true, {
                        fileName: "[project]/src/features/admissions/components/VetReviewForm.tsx",
                        lineNumber: 817,
                        columnNumber: 11
                    }, this),
                    availableDraft && /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$compiled$2f$react$2f$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$client$5d$__$28$ecmascript$29$__["jsxDEV"])("div", {
                        role: "status",
                        className: "mb-4 flex flex-wrap items-center justify-between gap-3 rounded-[var(--radius-sm)] border border-[var(--color-primary)] bg-[var(--color-primary-soft)] px-3 py-2 text-xs text-[var(--color-text-primary)]",
                        children: [
                            /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$compiled$2f$react$2f$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$client$5d$__$28$ecmascript$29$__["jsxDEV"])("div", {
                                className: "flex items-center gap-2",
                                children: [
                                    /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$compiled$2f$react$2f$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$client$5d$__$28$ecmascript$29$__["jsxDEV"])(__TURBOPACK__imported__module__$5b$project$5d2f$src$2f$components$2f$ui$2f$Icon$2e$tsx__$5b$app$2d$client$5d$__$28$ecmascript$29$__["Icon"], {
                                        name: "clipboard",
                                        size: 14,
                                        className: "text-[var(--color-primary)] shrink-0"
                                    }, void 0, false, {
                                        fileName: "[project]/src/features/admissions/components/VetReviewForm.tsx",
                                        lineNumber: 833,
                                        columnNumber: 15
                                    }, this),
                                    /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$compiled$2f$react$2f$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$client$5d$__$28$ecmascript$29$__["jsxDEV"])("span", {
                                        children: [
                                            "A saved draft from ",
                                            /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$compiled$2f$react$2f$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$client$5d$__$28$ecmascript$29$__["jsxDEV"])("strong", {
                                                children: availableDraft.timestamp
                                            }, void 0, false, {
                                                fileName: "[project]/src/features/admissions/components/VetReviewForm.tsx",
                                                lineNumber: 835,
                                                columnNumber: 36
                                            }, this),
                                            " was found. Would you like to restore it?"
                                        ]
                                    }, void 0, true, {
                                        fileName: "[project]/src/features/admissions/components/VetReviewForm.tsx",
                                        lineNumber: 834,
                                        columnNumber: 15
                                    }, this)
                                ]
                            }, void 0, true, {
                                fileName: "[project]/src/features/admissions/components/VetReviewForm.tsx",
                                lineNumber: 832,
                                columnNumber: 13
                            }, this),
                            /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$compiled$2f$react$2f$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$client$5d$__$28$ecmascript$29$__["jsxDEV"])("div", {
                                className: "flex items-center gap-2",
                                children: [
                                    /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$compiled$2f$react$2f$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$client$5d$__$28$ecmascript$29$__["jsxDEV"])(__TURBOPACK__imported__module__$5b$project$5d2f$src$2f$components$2f$ui$2f$Button$2e$tsx__$5b$app$2d$client$5d$__$28$ecmascript$29$__["Button"], {
                                        type: "button",
                                        size: "sm",
                                        variant: "primary",
                                        onClick: handleRestoreDraft,
                                        children: "Restore"
                                    }, void 0, false, {
                                        fileName: "[project]/src/features/admissions/components/VetReviewForm.tsx",
                                        lineNumber: 839,
                                        columnNumber: 15
                                    }, this),
                                    /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$compiled$2f$react$2f$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$client$5d$__$28$ecmascript$29$__["jsxDEV"])(__TURBOPACK__imported__module__$5b$project$5d2f$src$2f$components$2f$ui$2f$Button$2e$tsx__$5b$app$2d$client$5d$__$28$ecmascript$29$__["Button"], {
                                        type: "button",
                                        size: "sm",
                                        variant: "secondary",
                                        onClick: handleDiscardAvailableDraft,
                                        children: "Delete draft"
                                    }, void 0, false, {
                                        fileName: "[project]/src/features/admissions/components/VetReviewForm.tsx",
                                        lineNumber: 847,
                                        columnNumber: 15
                                    }, this)
                                ]
                            }, void 0, true, {
                                fileName: "[project]/src/features/admissions/components/VetReviewForm.tsx",
                                lineNumber: 838,
                                columnNumber: 13
                            }, this)
                        ]
                    }, void 0, true, {
                        fileName: "[project]/src/features/admissions/components/VetReviewForm.tsx",
                        lineNumber: 828,
                        columnNumber: 11
                    }, this),
                    draftSavedTime && !availableDraft && /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$compiled$2f$react$2f$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$client$5d$__$28$ecmascript$29$__["jsxDEV"])("div", {
                        className: "mb-4 flex items-center justify-between rounded-[var(--radius-sm)] border border-[var(--color-border)] bg-[var(--color-surface-subtle)] px-3 py-1.5 text-xs text-[var(--color-text-secondary)]",
                        children: [
                            /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$compiled$2f$react$2f$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$client$5d$__$28$ecmascript$29$__["jsxDEV"])("span", {
                                className: "flex items-center gap-1.5 font-medium",
                                children: [
                                    /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$compiled$2f$react$2f$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$client$5d$__$28$ecmascript$29$__["jsxDEV"])(__TURBOPACK__imported__module__$5b$project$5d2f$src$2f$components$2f$ui$2f$Icon$2e$tsx__$5b$app$2d$client$5d$__$28$ecmascript$29$__["Icon"], {
                                        name: "check",
                                        size: 12,
                                        className: "text-[var(--color-success)]"
                                    }, void 0, false, {
                                        fileName: "[project]/src/features/admissions/components/VetReviewForm.tsx",
                                        lineNumber: 863,
                                        columnNumber: 15
                                    }, this),
                                    "Draft saved at ",
                                    draftSavedTime
                                ]
                            }, void 0, true, {
                                fileName: "[project]/src/features/admissions/components/VetReviewForm.tsx",
                                lineNumber: 862,
                                columnNumber: 13
                            }, this),
                            /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$compiled$2f$react$2f$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$client$5d$__$28$ecmascript$29$__["jsxDEV"])(__TURBOPACK__imported__module__$5b$project$5d2f$src$2f$components$2f$ui$2f$Button$2e$tsx__$5b$app$2d$client$5d$__$28$ecmascript$29$__["Button"], {
                                variant: "link",
                                size: "sm",
                                onClick: handleClearDraft,
                                children: "Delete draft"
                            }, void 0, false, {
                                fileName: "[project]/src/features/admissions/components/VetReviewForm.tsx",
                                lineNumber: 866,
                                columnNumber: 13
                            }, this)
                        ]
                    }, void 0, true, {
                        fileName: "[project]/src/features/admissions/components/VetReviewForm.tsx",
                        lineNumber: 861,
                        columnNumber: 11
                    }, this),
                    /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$compiled$2f$react$2f$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$client$5d$__$28$ecmascript$29$__["jsxDEV"])("div", {
                        className: __TURBOPACK__imported__module__$5b$project$5d2f$src$2f$features$2f$admissions$2f$shared$2f$components$2f$AdmissionDetailLayout$2e$tsx__$5b$app$2d$client$5d$__$28$ecmascript$29$__["admissionDetailGridClassName"],
                        children: [
                            /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$compiled$2f$react$2f$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$client$5d$__$28$ecmascript$29$__["jsxDEV"])("div", {
                                className: "min-w-0 space-y-5",
                                children: [
                                    /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$compiled$2f$react$2f$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$client$5d$__$28$ecmascript$29$__["jsxDEV"])("fieldset", {
                                        disabled: isFormDisabled,
                                        className: "min-w-0 rounded-[var(--radius-lg)] border border-[var(--color-border)] bg-[var(--color-surface)] p-4 shadow-[var(--shadow-panel)] space-y-4",
                                        children: [
                                            /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$compiled$2f$react$2f$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$client$5d$__$28$ecmascript$29$__["jsxDEV"])("div", {
                                                className: "flex items-center justify-between border-b border-[var(--color-border)] pb-2",
                                                children: [
                                                    /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$compiled$2f$react$2f$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$client$5d$__$28$ecmascript$29$__["jsxDEV"])("h3", {
                                                        className: "text-sm font-semibold tracking-tight text-[var(--color-text-primary)]",
                                                        children: "1. Patient Vitals & Telemetry"
                                                    }, void 0, false, {
                                                        fileName: "[project]/src/features/admissions/components/VetReviewForm.tsx",
                                                        lineNumber: 879,
                                                        columnNumber: 17
                                                    }, this),
                                                    /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$compiled$2f$react$2f$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$client$5d$__$28$ecmascript$29$__["jsxDEV"])("span", {
                                                        className: "text-xs text-[var(--color-text-muted)]",
                                                        children: "Standard equine reference ranges"
                                                    }, void 0, false, {
                                                        fileName: "[project]/src/features/admissions/components/VetReviewForm.tsx",
                                                        lineNumber: 882,
                                                        columnNumber: 17
                                                    }, this)
                                                ]
                                            }, void 0, true, {
                                                fileName: "[project]/src/features/admissions/components/VetReviewForm.tsx",
                                                lineNumber: 878,
                                                columnNumber: 15
                                            }, this),
                                            /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$compiled$2f$react$2f$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$client$5d$__$28$ecmascript$29$__["jsxDEV"])("div", {
                                                className: "grid grid-cols-2 gap-3 md:grid-cols-3 2xl:grid-cols-5",
                                                children: metricConfigs.map((cfg)=>{
                                                    const borderInfo = getMetricBorderTone(metrics[cfg.key], cfg.normalMin, cfg.normalMax, cfg.criticalMin, cfg.criticalMax);
                                                    return /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$compiled$2f$react$2f$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$client$5d$__$28$ecmascript$29$__["jsxDEV"])("div", {
                                                        className: "space-y-1",
                                                        children: [
                                                            /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$compiled$2f$react$2f$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$client$5d$__$28$ecmascript$29$__["jsxDEV"])("div", {
                                                                className: "flex items-center justify-between text-xs",
                                                                children: [
                                                                    /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$compiled$2f$react$2f$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$client$5d$__$28$ecmascript$29$__["jsxDEV"])("label", {
                                                                        htmlFor: `metric-${cfg.key}`,
                                                                        className: "font-medium text-[var(--color-text-primary)]",
                                                                        children: cfg.label
                                                                    }, void 0, false, {
                                                                        fileName: "[project]/src/features/admissions/components/VetReviewForm.tsx",
                                                                        lineNumber: 900,
                                                                        columnNumber: 25
                                                                    }, this),
                                                                    borderInfo.statusLabel && /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$compiled$2f$react$2f$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$client$5d$__$28$ecmascript$29$__["jsxDEV"])("span", {
                                                                        className: `text-xs font-bold ${borderInfo.tone === 'danger' ? 'text-[var(--color-danger)]' : borderInfo.tone === 'warning' ? 'text-[var(--color-warning)]' : 'text-[var(--color-success)]'}`,
                                                                        children: borderInfo.statusLabel
                                                                    }, void 0, false, {
                                                                        fileName: "[project]/src/features/admissions/components/VetReviewForm.tsx",
                                                                        lineNumber: 907,
                                                                        columnNumber: 27
                                                                    }, this)
                                                                ]
                                                            }, void 0, true, {
                                                                fileName: "[project]/src/features/admissions/components/VetReviewForm.tsx",
                                                                lineNumber: 899,
                                                                columnNumber: 23
                                                            }, this),
                                                            /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$compiled$2f$react$2f$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$client$5d$__$28$ecmascript$29$__["jsxDEV"])("div", {
                                                                className: "relative",
                                                                children: [
                                                                    /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$compiled$2f$react$2f$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$client$5d$__$28$ecmascript$29$__["jsxDEV"])(__TURBOPACK__imported__module__$5b$project$5d2f$src$2f$components$2f$ui$2f$Input$2e$tsx__$5b$app$2d$client$5d$__$28$ecmascript$29$__["Input"], {
                                                                        id: `metric-${cfg.key}`,
                                                                        type: "number",
                                                                        min: "0",
                                                                        step: cfg.step,
                                                                        placeholder: cfg.placeholder,
                                                                        value: metrics[cfg.key],
                                                                        onChange: (e)=>setMetrics((curr)=>({
                                                                                    ...curr,
                                                                                    [cfg.key]: e.target.value
                                                                                })),
                                                                        className: `w-full rounded-[var(--radius-sm)] border bg-[var(--color-surface)] px-3 pr-10 font-metric text-sm font-semibold text-[var(--color-text-primary)] outline-none transition-all ${borderInfo.borderClass}`
                                                                    }, void 0, false, {
                                                                        fileName: "[project]/src/features/admissions/components/VetReviewForm.tsx",
                                                                        lineNumber: 922,
                                                                        columnNumber: 25
                                                                    }, this),
                                                                    /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$compiled$2f$react$2f$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$client$5d$__$28$ecmascript$29$__["jsxDEV"])("span", {
                                                                        className: "pointer-events-none absolute right-2.5 top-1/2 -translate-y-1/2 text-xs text-[var(--color-text-muted)]",
                                                                        children: cfg.unit
                                                                    }, void 0, false, {
                                                                        fileName: "[project]/src/features/admissions/components/VetReviewForm.tsx",
                                                                        lineNumber: 934,
                                                                        columnNumber: 25
                                                                    }, this)
                                                                ]
                                                            }, void 0, true, {
                                                                fileName: "[project]/src/features/admissions/components/VetReviewForm.tsx",
                                                                lineNumber: 921,
                                                                columnNumber: 23
                                                            }, this),
                                                            /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$compiled$2f$react$2f$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$client$5d$__$28$ecmascript$29$__["jsxDEV"])("span", {
                                                                className: "block text-xs text-[var(--color-text-muted)]",
                                                                children: cfg.normalLabel
                                                            }, void 0, false, {
                                                                fileName: "[project]/src/features/admissions/components/VetReviewForm.tsx",
                                                                lineNumber: 939,
                                                                columnNumber: 23
                                                            }, this),
                                                            fieldErrors[cfg.key] && /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$compiled$2f$react$2f$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$client$5d$__$28$ecmascript$29$__["jsxDEV"])("span", {
                                                                className: "block text-xs font-medium text-[var(--color-danger)]",
                                                                children: fieldErrors[cfg.key]
                                                            }, void 0, false, {
                                                                fileName: "[project]/src/features/admissions/components/VetReviewForm.tsx",
                                                                lineNumber: 943,
                                                                columnNumber: 25
                                                            }, this)
                                                        ]
                                                    }, cfg.key, true, {
                                                        fileName: "[project]/src/features/admissions/components/VetReviewForm.tsx",
                                                        lineNumber: 898,
                                                        columnNumber: 21
                                                    }, this);
                                                })
                                            }, void 0, false, {
                                                fileName: "[project]/src/features/admissions/components/VetReviewForm.tsx",
                                                lineNumber: 888,
                                                columnNumber: 15
                                            }, this),
                                            /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$compiled$2f$react$2f$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$client$5d$__$28$ecmascript$29$__["jsxDEV"])("div", {
                                                className: "grid gap-3 sm:grid-cols-2 pt-1",
                                                children: [
                                                    /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$compiled$2f$react$2f$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$client$5d$__$28$ecmascript$29$__["jsxDEV"])("div", {
                                                        children: /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$compiled$2f$react$2f$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$client$5d$__$28$ecmascript$29$__["jsxDEV"])(__TURBOPACK__imported__module__$5b$project$5d2f$src$2f$components$2f$ui$2f$FormField$2e$tsx__$5b$app$2d$client$5d$__$28$ecmascript$29$__["FormField"], {
                                                            label: "Hydration & Mucous Membranes",
                                                            htmlFor: "metric-hydration",
                                                            children: /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$compiled$2f$react$2f$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$client$5d$__$28$ecmascript$29$__["jsxDEV"])(__TURBOPACK__imported__module__$5b$project$5d2f$src$2f$components$2f$ui$2f$Input$2e$tsx__$5b$app$2d$client$5d$__$28$ecmascript$29$__["Select"], {
                                                                id: "metric-hydration",
                                                                value: metrics.hydrationStatus,
                                                                onChange: (e)=>setMetrics((curr)=>({
                                                                            ...curr,
                                                                            hydrationStatus: e.target.value
                                                                        })),
                                                                children: [
                                                                    /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$compiled$2f$react$2f$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$client$5d$__$28$ecmascript$29$__["jsxDEV"])("option", {
                                                                        value: "",
                                                                        children: "Not recorded"
                                                                    }, void 0, false, {
                                                                        fileName: "[project]/src/features/admissions/components/VetReviewForm.tsx",
                                                                        lineNumber: 961,
                                                                        columnNumber: 23
                                                                    }, this),
                                                                    /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$compiled$2f$react$2f$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$client$5d$__$28$ecmascript$29$__["jsxDEV"])("option", {
                                                                        value: "Normal (CRT < 2s, pink)",
                                                                        children: "Normal (CRT < 2s, pink)"
                                                                    }, void 0, false, {
                                                                        fileName: "[project]/src/features/admissions/components/VetReviewForm.tsx",
                                                                        lineNumber: 962,
                                                                        columnNumber: 23
                                                                    }, this),
                                                                    /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$compiled$2f$react$2f$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$client$5d$__$28$ecmascript$29$__["jsxDEV"])("option", {
                                                                        value: "Mild dehydration",
                                                                        children: "Mild dehydration"
                                                                    }, void 0, false, {
                                                                        fileName: "[project]/src/features/admissions/components/VetReviewForm.tsx",
                                                                        lineNumber: 965,
                                                                        columnNumber: 23
                                                                    }, this),
                                                                    /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$compiled$2f$react$2f$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$client$5d$__$28$ecmascript$29$__["jsxDEV"])("option", {
                                                                        value: "Moderate dehydration",
                                                                        children: "Moderate dehydration"
                                                                    }, void 0, false, {
                                                                        fileName: "[project]/src/features/admissions/components/VetReviewForm.tsx",
                                                                        lineNumber: 966,
                                                                        columnNumber: 23
                                                                    }, this),
                                                                    /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$compiled$2f$react$2f$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$client$5d$__$28$ecmascript$29$__["jsxDEV"])("option", {
                                                                        value: "Severe dehydration",
                                                                        children: "Severe dehydration"
                                                                    }, void 0, false, {
                                                                        fileName: "[project]/src/features/admissions/components/VetReviewForm.tsx",
                                                                        lineNumber: 967,
                                                                        columnNumber: 23
                                                                    }, this),
                                                                    /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$compiled$2f$react$2f$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$client$5d$__$28$ecmascript$29$__["jsxDEV"])("option", {
                                                                        value: "Abnormal CRT / mucous membranes",
                                                                        children: "Abnormal mucous membranes / CRT"
                                                                    }, void 0, false, {
                                                                        fileName: "[project]/src/features/admissions/components/VetReviewForm.tsx",
                                                                        lineNumber: 968,
                                                                        columnNumber: 23
                                                                    }, this)
                                                                ]
                                                            }, void 0, true, {
                                                                fileName: "[project]/src/features/admissions/components/VetReviewForm.tsx",
                                                                lineNumber: 956,
                                                                columnNumber: 21
                                                            }, this)
                                                        }, void 0, false, {
                                                            fileName: "[project]/src/features/admissions/components/VetReviewForm.tsx",
                                                            lineNumber: 955,
                                                            columnNumber: 19
                                                        }, this)
                                                    }, void 0, false, {
                                                        fileName: "[project]/src/features/admissions/components/VetReviewForm.tsx",
                                                        lineNumber: 954,
                                                        columnNumber: 17
                                                    }, this),
                                                    /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$compiled$2f$react$2f$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$client$5d$__$28$ecmascript$29$__["jsxDEV"])("div", {
                                                        children: /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$compiled$2f$react$2f$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$client$5d$__$28$ecmascript$29$__["jsxDEV"])(__TURBOPACK__imported__module__$5b$project$5d2f$src$2f$components$2f$ui$2f$FormField$2e$tsx__$5b$app$2d$client$5d$__$28$ecmascript$29$__["FormField"], {
                                                            label: "Telemetry & Observation Context",
                                                            htmlFor: "metric-notes",
                                                            children: /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$compiled$2f$react$2f$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$client$5d$__$28$ecmascript$29$__["jsxDEV"])(__TURBOPACK__imported__module__$5b$project$5d2f$src$2f$components$2f$ui$2f$Input$2e$tsx__$5b$app$2d$client$5d$__$28$ecmascript$29$__["Input"], {
                                                                id: "metric-notes",
                                                                type: "text",
                                                                maxLength: 2000,
                                                                value: metrics.notes,
                                                                onChange: (e)=>setMetrics((curr)=>({
                                                                            ...curr,
                                                                            notes: e.target.value
                                                                        })),
                                                                placeholder: "e.g. Calm disposition, post-transport check"
                                                            }, void 0, false, {
                                                                fileName: "[project]/src/features/admissions/components/VetReviewForm.tsx",
                                                                lineNumber: 977,
                                                                columnNumber: 21
                                                            }, this)
                                                        }, void 0, false, {
                                                            fileName: "[project]/src/features/admissions/components/VetReviewForm.tsx",
                                                            lineNumber: 976,
                                                            columnNumber: 19
                                                        }, this)
                                                    }, void 0, false, {
                                                        fileName: "[project]/src/features/admissions/components/VetReviewForm.tsx",
                                                        lineNumber: 975,
                                                        columnNumber: 17
                                                    }, this)
                                                ]
                                            }, void 0, true, {
                                                fileName: "[project]/src/features/admissions/components/VetReviewForm.tsx",
                                                lineNumber: 953,
                                                columnNumber: 15
                                            }, this)
                                        ]
                                    }, void 0, true, {
                                        fileName: "[project]/src/features/admissions/components/VetReviewForm.tsx",
                                        lineNumber: 877,
                                        columnNumber: 13
                                    }, this),
                                    /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$compiled$2f$react$2f$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$client$5d$__$28$ecmascript$29$__["jsxDEV"])("fieldset", {
                                        disabled: isFormDisabled,
                                        className: "min-w-0 rounded-[var(--radius-lg)] border border-[var(--color-border)] bg-[var(--color-surface)] p-4 shadow-[var(--shadow-panel)] space-y-4",
                                        children: [
                                            /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$compiled$2f$react$2f$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$client$5d$__$28$ecmascript$29$__["jsxDEV"])("div", {
                                                className: "flex items-center justify-between border-b border-[var(--color-border)] pb-2",
                                                children: [
                                                    /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$compiled$2f$react$2f$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$client$5d$__$28$ecmascript$29$__["jsxDEV"])("h3", {
                                                        className: "text-sm font-semibold tracking-tight text-[var(--color-text-primary)]",
                                                        children: "2. Clinical Findings & Examination"
                                                    }, void 0, false, {
                                                        fileName: "[project]/src/features/admissions/components/VetReviewForm.tsx",
                                                        lineNumber: 993,
                                                        columnNumber: 17
                                                    }, this),
                                                    /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$compiled$2f$react$2f$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$client$5d$__$28$ecmascript$29$__["jsxDEV"])("span", {
                                                        className: "text-xs font-medium text-[var(--color-danger)]",
                                                        children: "* Required"
                                                    }, void 0, false, {
                                                        fileName: "[project]/src/features/admissions/components/VetReviewForm.tsx",
                                                        lineNumber: 996,
                                                        columnNumber: 17
                                                    }, this)
                                                ]
                                            }, void 0, true, {
                                                fileName: "[project]/src/features/admissions/components/VetReviewForm.tsx",
                                                lineNumber: 992,
                                                columnNumber: 15
                                            }, this),
                                            /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$compiled$2f$react$2f$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$client$5d$__$28$ecmascript$29$__["jsxDEV"])("div", {
                                                className: "flex items-center justify-between rounded-[var(--radius-sm)] border border-[var(--color-border)] bg-[var(--color-surface-subtle)] px-3 py-2",
                                                children: [
                                                    /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$compiled$2f$react$2f$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$client$5d$__$28$ecmascript$29$__["jsxDEV"])("label", {
                                                        className: "flex cursor-pointer items-center gap-2",
                                                        children: [
                                                            /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$compiled$2f$react$2f$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$client$5d$__$28$ecmascript$29$__["jsxDEV"])(__TURBOPACK__imported__module__$5b$project$5d2f$src$2f$components$2f$ui$2f$Input$2e$tsx__$5b$app$2d$client$5d$__$28$ecmascript$29$__["ChoiceInput"], {
                                                                type: "checkbox",
                                                                checked: physicalExamConfirmed,
                                                                onChange: (e)=>{
                                                                    setPhysicalExamConfirmed(e.target.checked);
                                                                    if (fieldErrors.physicalExamConfirmed) {
                                                                        setFieldErrors((errs)=>({
                                                                                ...errs,
                                                                                physicalExamConfirmed: ''
                                                                            }));
                                                                    }
                                                                }
                                                            }, void 0, false, {
                                                                fileName: "[project]/src/features/admissions/components/VetReviewForm.tsx",
                                                                lineNumber: 1004,
                                                                columnNumber: 19
                                                            }, this),
                                                            /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$compiled$2f$react$2f$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$client$5d$__$28$ecmascript$29$__["jsxDEV"])("span", {
                                                                className: "text-xs font-semibold text-[var(--color-text-primary)]",
                                                                children: "Physical Exam Conducted In-Person"
                                                            }, void 0, false, {
                                                                fileName: "[project]/src/features/admissions/components/VetReviewForm.tsx",
                                                                lineNumber: 1014,
                                                                columnNumber: 19
                                                            }, this)
                                                        ]
                                                    }, void 0, true, {
                                                        fileName: "[project]/src/features/admissions/components/VetReviewForm.tsx",
                                                        lineNumber: 1003,
                                                        columnNumber: 17
                                                    }, this),
                                                    /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$compiled$2f$react$2f$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$client$5d$__$28$ecmascript$29$__["jsxDEV"])("span", {
                                                        className: "text-xs text-[var(--color-text-muted)]",
                                                        children: [
                                                            "Quarantine facility: ",
                                                            quarantineStallCode || 'Stall verified'
                                                        ]
                                                    }, void 0, true, {
                                                        fileName: "[project]/src/features/admissions/components/VetReviewForm.tsx",
                                                        lineNumber: 1018,
                                                        columnNumber: 17
                                                    }, this)
                                                ]
                                            }, void 0, true, {
                                                fileName: "[project]/src/features/admissions/components/VetReviewForm.tsx",
                                                lineNumber: 1002,
                                                columnNumber: 15
                                            }, this),
                                            fieldErrors.physicalExamConfirmed && /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$compiled$2f$react$2f$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$client$5d$__$28$ecmascript$29$__["jsxDEV"])("p", {
                                                className: "text-xs font-medium text-[var(--color-danger)]",
                                                children: fieldErrors.physicalExamConfirmed
                                            }, void 0, false, {
                                                fileName: "[project]/src/features/admissions/components/VetReviewForm.tsx",
                                                lineNumber: 1023,
                                                columnNumber: 17
                                            }, this),
                                            isUrgent ? /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$compiled$2f$react$2f$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$client$5d$__$28$ecmascript$29$__["jsxDEV"])("div", {
                                                className: "grid gap-3 sm:grid-cols-2",
                                                children: [
                                                    /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$compiled$2f$react$2f$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$client$5d$__$28$ecmascript$29$__["jsxDEV"])("div", {
                                                        children: [
                                                            /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$compiled$2f$react$2f$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$client$5d$__$28$ecmascript$29$__["jsxDEV"])(__TURBOPACK__imported__module__$5b$project$5d2f$src$2f$components$2f$ui$2f$FormField$2e$tsx__$5b$app$2d$client$5d$__$28$ecmascript$29$__["FormField"], {
                                                                label: "Urgent Presenting Symptoms",
                                                                htmlFor: "field-symptoms",
                                                                required: true,
                                                                children: /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$compiled$2f$react$2f$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$client$5d$__$28$ecmascript$29$__["jsxDEV"])(__TURBOPACK__imported__module__$5b$project$5d2f$src$2f$components$2f$ui$2f$Input$2e$tsx__$5b$app$2d$client$5d$__$28$ecmascript$29$__["Textarea"], {
                                                                    id: "field-symptoms",
                                                                    rows: 3,
                                                                    value: symptoms,
                                                                    onChange: (e)=>setSymptoms(e.target.value),
                                                                    placeholder: "Describe onset, symptoms, and urgency..."
                                                                }, void 0, false, {
                                                                    fileName: "[project]/src/features/admissions/components/VetReviewForm.tsx",
                                                                    lineNumber: 1033,
                                                                    columnNumber: 23
                                                                }, this)
                                                            }, void 0, false, {
                                                                fileName: "[project]/src/features/admissions/components/VetReviewForm.tsx",
                                                                lineNumber: 1032,
                                                                columnNumber: 21
                                                            }, this),
                                                            fieldErrors.symptoms && /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$compiled$2f$react$2f$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$client$5d$__$28$ecmascript$29$__["jsxDEV"])("p", {
                                                                className: "text-xs font-medium text-[var(--color-danger)]",
                                                                children: fieldErrors.symptoms
                                                            }, void 0, false, {
                                                                fileName: "[project]/src/features/admissions/components/VetReviewForm.tsx",
                                                                lineNumber: 1042,
                                                                columnNumber: 23
                                                            }, this)
                                                        ]
                                                    }, void 0, true, {
                                                        fileName: "[project]/src/features/admissions/components/VetReviewForm.tsx",
                                                        lineNumber: 1031,
                                                        columnNumber: 19
                                                    }, this),
                                                    /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$compiled$2f$react$2f$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$client$5d$__$28$ecmascript$29$__["jsxDEV"])("div", {
                                                        children: [
                                                            /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$compiled$2f$react$2f$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$client$5d$__$28$ecmascript$29$__["jsxDEV"])(__TURBOPACK__imported__module__$5b$project$5d2f$src$2f$components$2f$ui$2f$FormField$2e$tsx__$5b$app$2d$client$5d$__$28$ecmascript$29$__["FormField"], {
                                                                label: "Urgent Physical Findings",
                                                                htmlFor: "field-findings",
                                                                required: true,
                                                                children: /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$compiled$2f$react$2f$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$client$5d$__$28$ecmascript$29$__["jsxDEV"])(__TURBOPACK__imported__module__$5b$project$5d2f$src$2f$components$2f$ui$2f$Input$2e$tsx__$5b$app$2d$client$5d$__$28$ecmascript$29$__["Textarea"], {
                                                                    id: "field-findings",
                                                                    rows: 3,
                                                                    value: findings,
                                                                    onChange: (e)=>setFindings(e.target.value),
                                                                    placeholder: "Document trauma, distress, or acute signs..."
                                                                }, void 0, false, {
                                                                    fileName: "[project]/src/features/admissions/components/VetReviewForm.tsx",
                                                                    lineNumber: 1049,
                                                                    columnNumber: 23
                                                                }, this)
                                                            }, void 0, false, {
                                                                fileName: "[project]/src/features/admissions/components/VetReviewForm.tsx",
                                                                lineNumber: 1048,
                                                                columnNumber: 21
                                                            }, this),
                                                            fieldErrors.findings && /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$compiled$2f$react$2f$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$client$5d$__$28$ecmascript$29$__["jsxDEV"])("p", {
                                                                className: "text-xs font-medium text-[var(--color-danger)]",
                                                                children: fieldErrors.findings
                                                            }, void 0, false, {
                                                                fileName: "[project]/src/features/admissions/components/VetReviewForm.tsx",
                                                                lineNumber: 1058,
                                                                columnNumber: 23
                                                            }, this)
                                                        ]
                                                    }, void 0, true, {
                                                        fileName: "[project]/src/features/admissions/components/VetReviewForm.tsx",
                                                        lineNumber: 1047,
                                                        columnNumber: 19
                                                    }, this)
                                                ]
                                            }, void 0, true, {
                                                fileName: "[project]/src/features/admissions/components/VetReviewForm.tsx",
                                                lineNumber: 1030,
                                                columnNumber: 17
                                            }, this) : /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$compiled$2f$react$2f$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$client$5d$__$28$ecmascript$29$__["jsxDEV"])("div", {
                                                className: "space-y-3",
                                                children: [
                                                    /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$compiled$2f$react$2f$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$client$5d$__$28$ecmascript$29$__["jsxDEV"])("div", {
                                                        className: "space-y-1.5",
                                                        children: [
                                                            /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$compiled$2f$react$2f$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$client$5d$__$28$ecmascript$29$__["jsxDEV"])("label", {
                                                                className: "block text-xs font-semibold text-[var(--color-text-secondary)]",
                                                                children: "Clinical Status Assessment"
                                                            }, void 0, false, {
                                                                fileName: "[project]/src/features/admissions/components/VetReviewForm.tsx",
                                                                lineNumber: 1068,
                                                                columnNumber: 21
                                                            }, this),
                                                            /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$compiled$2f$react$2f$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$client$5d$__$28$ecmascript$29$__["jsxDEV"])("div", {
                                                                children: /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$compiled$2f$react$2f$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$client$5d$__$28$ecmascript$29$__["jsxDEV"])(__TURBOPACK__imported__module__$5b$project$5d2f$src$2f$components$2f$ui$2f$SegmentedControl$2e$tsx__$5b$app$2d$client$5d$__$28$ecmascript$29$__["SegmentedControl"], {
                                                                    label: "Clinical status assessment",
                                                                    value: examMode,
                                                                    onChange: (mode)=>{
                                                                        setExamMode(mode);
                                                                        if (mode === 'NORMAL') {
                                                                            setSystemFindings({
                                                                                ...normalSystemFindings
                                                                            });
                                                                            setSelectedSymptoms([
                                                                                'No symptoms observed'
                                                                            ]);
                                                                            setStructuredDiagnosis('Clinically healthy');
                                                                            setFieldErrors((errs)=>({
                                                                                    ...errs,
                                                                                    systemFindings: ''
                                                                                }));
                                                                        } else {
                                                                            setSelectedSymptoms([]);
                                                                            setStructuredDiagnosis('Minor clinical condition');
                                                                        }
                                                                    },
                                                                    options: [
                                                                        {
                                                                            value: 'NORMAL',
                                                                            label: 'Normal',
                                                                            icon: 'check'
                                                                        },
                                                                        {
                                                                            value: 'ABNORMAL',
                                                                            label: 'Abnormal',
                                                                            icon: 'alert-triangle'
                                                                        }
                                                                    ]
                                                                }, void 0, false, {
                                                                    fileName: "[project]/src/features/admissions/components/VetReviewForm.tsx",
                                                                    lineNumber: 1072,
                                                                    columnNumber: 23
                                                                }, this)
                                                            }, void 0, false, {
                                                                fileName: "[project]/src/features/admissions/components/VetReviewForm.tsx",
                                                                lineNumber: 1071,
                                                                columnNumber: 21
                                                            }, this),
                                                            /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$compiled$2f$react$2f$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$client$5d$__$28$ecmascript$29$__["jsxDEV"])("p", {
                                                                className: "text-xs text-[var(--color-text-muted)]",
                                                                children: examMode === 'NORMAL' ? 'All 8 body systems are recorded as normal. Findings and symptoms will be generated automatically.' : 'Abnormal findings detected. Specify affected body systems and observed symptoms below.'
                                                            }, void 0, false, {
                                                                fileName: "[project]/src/features/admissions/components/VetReviewForm.tsx",
                                                                lineNumber: 1095,
                                                                columnNumber: 21
                                                            }, this)
                                                        ]
                                                    }, void 0, true, {
                                                        fileName: "[project]/src/features/admissions/components/VetReviewForm.tsx",
                                                        lineNumber: 1067,
                                                        columnNumber: 19
                                                    }, this),
                                                    examMode === 'ABNORMAL' && /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$compiled$2f$react$2f$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$client$5d$__$28$ecmascript$29$__["jsxDEV"])("div", {
                                                        className: "rounded-[var(--radius-md)] border border-[var(--color-border)] bg-[var(--color-surface-subtle)] p-3.5 space-y-4",
                                                        children: [
                                                            /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$compiled$2f$react$2f$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$client$5d$__$28$ecmascript$29$__["jsxDEV"])("div", {
                                                                children: [
                                                                    /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$compiled$2f$react$2f$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$client$5d$__$28$ecmascript$29$__["jsxDEV"])("div", {
                                                                        className: "flex items-center justify-between",
                                                                        children: [
                                                                            /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$compiled$2f$react$2f$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$client$5d$__$28$ecmascript$29$__["jsxDEV"])("h4", {
                                                                                className: "text-xs font-bold text-[var(--color-text-primary)]",
                                                                                children: "8 Body Systems Checklist"
                                                                            }, void 0, false, {
                                                                                fileName: "[project]/src/features/admissions/components/VetReviewForm.tsx",
                                                                                lineNumber: 1107,
                                                                                columnNumber: 27
                                                                            }, this),
                                                                            /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$compiled$2f$react$2f$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$client$5d$__$28$ecmascript$29$__["jsxDEV"])(__TURBOPACK__imported__module__$5b$project$5d2f$src$2f$components$2f$ui$2f$Button$2e$tsx__$5b$app$2d$client$5d$__$28$ecmascript$29$__["Button"], {
                                                                                variant: "link",
                                                                                size: "sm",
                                                                                onClick: ()=>setSystemFindings({
                                                                                        ...normalSystemFindings
                                                                                    }),
                                                                                children: "Reset all to normal"
                                                                            }, void 0, false, {
                                                                                fileName: "[project]/src/features/admissions/components/VetReviewForm.tsx",
                                                                                lineNumber: 1110,
                                                                                columnNumber: 27
                                                                            }, this)
                                                                        ]
                                                                    }, void 0, true, {
                                                                        fileName: "[project]/src/features/admissions/components/VetReviewForm.tsx",
                                                                        lineNumber: 1106,
                                                                        columnNumber: 25
                                                                    }, this),
                                                                    /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$compiled$2f$react$2f$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$client$5d$__$28$ecmascript$29$__["jsxDEV"])("div", {
                                                                        className: "mt-2 grid grid-cols-2 gap-2 sm:grid-cols-4",
                                                                        children: bodySystems.map(({ key, label })=>{
                                                                            const isAbnormal = systemFindings[key] !== 'NORMAL';
                                                                            return /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$compiled$2f$react$2f$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$client$5d$__$28$ecmascript$29$__["jsxDEV"])("label", {
                                                                                className: "space-y-1",
                                                                                children: [
                                                                                    /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$compiled$2f$react$2f$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$client$5d$__$28$ecmascript$29$__["jsxDEV"])("span", {
                                                                                        className: "block text-xs font-medium text-[var(--color-text-secondary)]",
                                                                                        children: label
                                                                                    }, void 0, false, {
                                                                                        fileName: "[project]/src/features/admissions/components/VetReviewForm.tsx",
                                                                                        lineNumber: 1119,
                                                                                        columnNumber: 33
                                                                                    }, this),
                                                                                    /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$compiled$2f$react$2f$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$client$5d$__$28$ecmascript$29$__["jsxDEV"])(__TURBOPACK__imported__module__$5b$project$5d2f$src$2f$components$2f$ui$2f$Input$2e$tsx__$5b$app$2d$client$5d$__$28$ecmascript$29$__["Select"], {
                                                                                        value: systemFindings[key],
                                                                                        onChange: (e)=>{
                                                                                            setSystemFindings((curr)=>({
                                                                                                    ...curr,
                                                                                                    [key]: e.target.value
                                                                                                }));
                                                                                            setFieldErrors((errs)=>({
                                                                                                    ...errs,
                                                                                                    systemFindings: ''
                                                                                                }));
                                                                                        },
                                                                                        className: `w-full rounded-[var(--radius-sm)] px-2 py-1 text-xs outline-none border transition-colors ${isAbnormal ? 'border-[var(--color-warning)] bg-[var(--color-warning-soft)] text-[var(--color-warning)] font-semibold' : 'border-[var(--color-border)] bg-[var(--color-surface)] text-[var(--color-text-primary)]'}`,
                                                                                        children: Object.entries(findingLevelLabels).map(([val, optLabel])=>/*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$compiled$2f$react$2f$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$client$5d$__$28$ecmascript$29$__["jsxDEV"])("option", {
                                                                                                value: val,
                                                                                                children: optLabel
                                                                                            }, val, false, {
                                                                                                fileName: "[project]/src/features/admissions/components/VetReviewForm.tsx",
                                                                                                lineNumber: 1139,
                                                                                                columnNumber: 39
                                                                                            }, this))
                                                                                    }, void 0, false, {
                                                                                        fileName: "[project]/src/features/admissions/components/VetReviewForm.tsx",
                                                                                        lineNumber: 1122,
                                                                                        columnNumber: 33
                                                                                    }, this)
                                                                                ]
                                                                            }, key, true, {
                                                                                fileName: "[project]/src/features/admissions/components/VetReviewForm.tsx",
                                                                                lineNumber: 1118,
                                                                                columnNumber: 31
                                                                            }, this);
                                                                        })
                                                                    }, void 0, false, {
                                                                        fileName: "[project]/src/features/admissions/components/VetReviewForm.tsx",
                                                                        lineNumber: 1114,
                                                                        columnNumber: 25
                                                                    }, this),
                                                                    fieldErrors.systemFindings && /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$compiled$2f$react$2f$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$client$5d$__$28$ecmascript$29$__["jsxDEV"])("p", {
                                                                        className: "mt-1.5 text-xs font-medium text-[var(--color-danger)]",
                                                                        children: fieldErrors.systemFindings
                                                                    }, void 0, false, {
                                                                        fileName: "[project]/src/features/admissions/components/VetReviewForm.tsx",
                                                                        lineNumber: 1150,
                                                                        columnNumber: 27
                                                                    }, this)
                                                                ]
                                                            }, void 0, true, {
                                                                fileName: "[project]/src/features/admissions/components/VetReviewForm.tsx",
                                                                lineNumber: 1105,
                                                                columnNumber: 23
                                                            }, this),
                                                            /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$compiled$2f$react$2f$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$client$5d$__$28$ecmascript$29$__["jsxDEV"])("div", {
                                                                className: "border-t border-[var(--color-border)] pt-3",
                                                                children: [
                                                                    /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$compiled$2f$react$2f$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$client$5d$__$28$ecmascript$29$__["jsxDEV"])("span", {
                                                                        className: "block text-xs font-bold text-[var(--color-text-primary)] mb-2",
                                                                        children: "Observed Symptoms Checklist"
                                                                    }, void 0, false, {
                                                                        fileName: "[project]/src/features/admissions/components/VetReviewForm.tsx",
                                                                        lineNumber: 1158,
                                                                        columnNumber: 25
                                                                    }, this),
                                                                    /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$compiled$2f$react$2f$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$client$5d$__$28$ecmascript$29$__["jsxDEV"])("div", {
                                                                        className: "flex flex-wrap gap-1.5",
                                                                        children: symptomOptions.map((symptom)=>{
                                                                            const selected = selectedSymptoms.includes(symptom);
                                                                            return /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$compiled$2f$react$2f$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$client$5d$__$28$ecmascript$29$__["jsxDEV"])(__TURBOPACK__imported__module__$5b$project$5d2f$src$2f$components$2f$ui$2f$Button$2e$tsx__$5b$app$2d$client$5d$__$28$ecmascript$29$__["Button"], {
                                                                                size: "sm",
                                                                                variant: "secondary",
                                                                                "aria-pressed": selected,
                                                                                onClick: ()=>setSelectedSymptoms((curr)=>{
                                                                                        if (symptom === 'No symptoms observed') {
                                                                                            return [
                                                                                                'No symptoms observed'
                                                                                            ];
                                                                                        }
                                                                                        const withoutNone = curr.filter((s)=>s !== 'No symptoms observed');
                                                                                        return selected ? withoutNone.filter((s)=>s !== symptom) : [
                                                                                            ...withoutNone,
                                                                                            symptom
                                                                                        ];
                                                                                    }),
                                                                                className: `rounded-full border px-2.5 py-1 text-xs font-medium transition-colors ${selected ? 'border-[var(--color-primary)] bg-[var(--color-primary-soft)] text-[var(--color-primary)] font-semibold' : 'border-[var(--color-border)] bg-[var(--color-surface)] text-[var(--color-text-secondary)] hover:border-[var(--color-border-strong)]'}`,
                                                                                children: [
                                                                                    selected ? '✓ ' : '',
                                                                                    symptom
                                                                                ]
                                                                            }, symptom, true, {
                                                                                fileName: "[project]/src/features/admissions/components/VetReviewForm.tsx",
                                                                                lineNumber: 1165,
                                                                                columnNumber: 31
                                                                            }, this);
                                                                        })
                                                                    }, void 0, false, {
                                                                        fileName: "[project]/src/features/admissions/components/VetReviewForm.tsx",
                                                                        lineNumber: 1161,
                                                                        columnNumber: 25
                                                                    }, this)
                                                                ]
                                                            }, void 0, true, {
                                                                fileName: "[project]/src/features/admissions/components/VetReviewForm.tsx",
                                                                lineNumber: 1157,
                                                                columnNumber: 23
                                                            }, this)
                                                        ]
                                                    }, void 0, true, {
                                                        fileName: "[project]/src/features/admissions/components/VetReviewForm.tsx",
                                                        lineNumber: 1104,
                                                        columnNumber: 21
                                                    }, this)
                                                ]
                                            }, void 0, true, {
                                                fileName: "[project]/src/features/admissions/components/VetReviewForm.tsx",
                                                lineNumber: 1065,
                                                columnNumber: 17
                                            }, this),
                                            /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$compiled$2f$react$2f$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$client$5d$__$28$ecmascript$29$__["jsxDEV"])("div", {
                                                className: "grid grid-cols-1 md:grid-cols-2 gap-3 pt-2",
                                                children: [
                                                    /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$compiled$2f$react$2f$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$client$5d$__$28$ecmascript$29$__["jsxDEV"])("div", {
                                                        children: [
                                                            /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$compiled$2f$react$2f$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$client$5d$__$28$ecmascript$29$__["jsxDEV"])("label", {
                                                                htmlFor: "field-assessment-diag",
                                                                className: "mb-1 block text-xs font-semibold text-[var(--color-text-primary)]",
                                                                children: [
                                                                    "Clinical Assessment & Diagnosis ",
                                                                    /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$compiled$2f$react$2f$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$client$5d$__$28$ecmascript$29$__["jsxDEV"])("span", {
                                                                        className: "text-[var(--color-danger)]",
                                                                        children: "*"
                                                                    }, void 0, false, {
                                                                        fileName: "[project]/src/features/admissions/components/VetReviewForm.tsx",
                                                                        lineNumber: 1206,
                                                                        columnNumber: 57
                                                                    }, this)
                                                                ]
                                                            }, void 0, true, {
                                                                fileName: "[project]/src/features/admissions/components/VetReviewForm.tsx",
                                                                lineNumber: 1202,
                                                                columnNumber: 19
                                                            }, this),
                                                            isUrgent ? /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$compiled$2f$react$2f$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$client$5d$__$28$ecmascript$29$__["jsxDEV"])(__TURBOPACK__imported__module__$5b$project$5d2f$src$2f$components$2f$ui$2f$Input$2e$tsx__$5b$app$2d$client$5d$__$28$ecmascript$29$__["Input"], {
                                                                id: "field-assessment-diag",
                                                                type: "text",
                                                                value: diagnosis,
                                                                onChange: (e)=>setDiagnosis(e.target.value),
                                                                placeholder: "Working diagnosis..."
                                                            }, void 0, false, {
                                                                fileName: "[project]/src/features/admissions/components/VetReviewForm.tsx",
                                                                lineNumber: 1209,
                                                                columnNumber: 21
                                                            }, this) : examMode === 'NORMAL' ? /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$compiled$2f$react$2f$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$client$5d$__$28$ecmascript$29$__["jsxDEV"])("div", {
                                                                className: "flex h-9 items-center gap-2 rounded-[var(--radius-sm)] border border-[var(--color-success)] bg-[var(--color-success-soft)] px-3 text-xs font-semibold text-[var(--color-success)]",
                                                                children: [
                                                                    /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$compiled$2f$react$2f$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$client$5d$__$28$ecmascript$29$__["jsxDEV"])(__TURBOPACK__imported__module__$5b$project$5d2f$src$2f$components$2f$ui$2f$Icon$2e$tsx__$5b$app$2d$client$5d$__$28$ecmascript$29$__["Icon"], {
                                                                        name: "check",
                                                                        size: 14
                                                                    }, void 0, false, {
                                                                        fileName: "[project]/src/features/admissions/components/VetReviewForm.tsx",
                                                                        lineNumber: 1218,
                                                                        columnNumber: 23
                                                                    }, this),
                                                                    "Clinically Healthy"
                                                                ]
                                                            }, void 0, true, {
                                                                fileName: "[project]/src/features/admissions/components/VetReviewForm.tsx",
                                                                lineNumber: 1217,
                                                                columnNumber: 21
                                                            }, this) : /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$compiled$2f$react$2f$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$client$5d$__$28$ecmascript$29$__["jsxDEV"])(__TURBOPACK__imported__module__$5b$project$5d2f$src$2f$components$2f$ui$2f$Input$2e$tsx__$5b$app$2d$client$5d$__$28$ecmascript$29$__["Select"], {
                                                                id: "field-assessment-diag",
                                                                value: structuredDiagnosis,
                                                                onChange: (e)=>setStructuredDiagnosis(e.target.value),
                                                                children: diagnosisOptions.filter((d)=>d !== 'Clinically healthy').map((opt)=>/*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$compiled$2f$react$2f$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$client$5d$__$28$ecmascript$29$__["jsxDEV"])("option", {
                                                                        value: opt,
                                                                        children: opt
                                                                    }, opt, false, {
                                                                        fileName: "[project]/src/features/admissions/components/VetReviewForm.tsx",
                                                                        lineNumber: 1230,
                                                                        columnNumber: 27
                                                                    }, this))
                                                            }, void 0, false, {
                                                                fileName: "[project]/src/features/admissions/components/VetReviewForm.tsx",
                                                                lineNumber: 1222,
                                                                columnNumber: 21
                                                            }, this),
                                                            fieldErrors.diagnosis && /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$compiled$2f$react$2f$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$client$5d$__$28$ecmascript$29$__["jsxDEV"])("p", {
                                                                className: "mt-1 text-xs font-medium text-[var(--color-danger)]",
                                                                children: fieldErrors.diagnosis
                                                            }, void 0, false, {
                                                                fileName: "[project]/src/features/admissions/components/VetReviewForm.tsx",
                                                                lineNumber: 1237,
                                                                columnNumber: 21
                                                            }, this)
                                                        ]
                                                    }, void 0, true, {
                                                        fileName: "[project]/src/features/admissions/components/VetReviewForm.tsx",
                                                        lineNumber: 1201,
                                                        columnNumber: 17
                                                    }, this),
                                                    /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$compiled$2f$react$2f$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$client$5d$__$28$ecmascript$29$__["jsxDEV"])("div", {
                                                        children: /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$compiled$2f$react$2f$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$client$5d$__$28$ecmascript$29$__["jsxDEV"])(__TURBOPACK__imported__module__$5b$project$5d2f$src$2f$components$2f$ui$2f$FormField$2e$tsx__$5b$app$2d$client$5d$__$28$ecmascript$29$__["FormField"], {
                                                            label: "Treatment & Care Protocol (Optional)",
                                                            htmlFor: "field-treatment",
                                                            children: /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$compiled$2f$react$2f$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$client$5d$__$28$ecmascript$29$__["jsxDEV"])(__TURBOPACK__imported__module__$5b$project$5d2f$src$2f$components$2f$ui$2f$Input$2e$tsx__$5b$app$2d$client$5d$__$28$ecmascript$29$__["Textarea"], {
                                                                id: "field-treatment",
                                                                rows: 2,
                                                                value: treatment,
                                                                onChange: (e)=>setTreatment(e.target.value),
                                                                placeholder: "Enter prescribed medications, bandages, or therapy..."
                                                            }, void 0, false, {
                                                                fileName: "[project]/src/features/admissions/components/VetReviewForm.tsx",
                                                                lineNumber: 1245,
                                                                columnNumber: 21
                                                            }, this)
                                                        }, void 0, false, {
                                                            fileName: "[project]/src/features/admissions/components/VetReviewForm.tsx",
                                                            lineNumber: 1244,
                                                            columnNumber: 19
                                                        }, this)
                                                    }, void 0, false, {
                                                        fileName: "[project]/src/features/admissions/components/VetReviewForm.tsx",
                                                        lineNumber: 1243,
                                                        columnNumber: 17
                                                    }, this)
                                                ]
                                            }, void 0, true, {
                                                fileName: "[project]/src/features/admissions/components/VetReviewForm.tsx",
                                                lineNumber: 1200,
                                                columnNumber: 15
                                            }, this),
                                            /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$compiled$2f$react$2f$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$client$5d$__$28$ecmascript$29$__["jsxDEV"])("div", {
                                                children: /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$compiled$2f$react$2f$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$client$5d$__$28$ecmascript$29$__["jsxDEV"])(__TURBOPACK__imported__module__$5b$project$5d2f$src$2f$components$2f$ui$2f$FormField$2e$tsx__$5b$app$2d$client$5d$__$28$ecmascript$29$__["FormField"], {
                                                    label: "Veterinarian Notes & Stable Directives (Optional)",
                                                    htmlFor: "field-notes",
                                                    children: /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$compiled$2f$react$2f$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$client$5d$__$28$ecmascript$29$__["jsxDEV"])(__TURBOPACK__imported__module__$5b$project$5d2f$src$2f$components$2f$ui$2f$Input$2e$tsx__$5b$app$2d$client$5d$__$28$ecmascript$29$__["Textarea"], {
                                                        id: "field-notes",
                                                        rows: 2,
                                                        value: notes,
                                                        onChange: (e)=>setNotes(e.target.value),
                                                        placeholder: "Internal notes, groom handling directives, or dietary recommendations..."
                                                    }, void 0, false, {
                                                        fileName: "[project]/src/features/admissions/components/VetReviewForm.tsx",
                                                        lineNumber: 1259,
                                                        columnNumber: 19
                                                    }, this)
                                                }, void 0, false, {
                                                    fileName: "[project]/src/features/admissions/components/VetReviewForm.tsx",
                                                    lineNumber: 1258,
                                                    columnNumber: 17
                                                }, this)
                                            }, void 0, false, {
                                                fileName: "[project]/src/features/admissions/components/VetReviewForm.tsx",
                                                lineNumber: 1257,
                                                columnNumber: 15
                                            }, this)
                                        ]
                                    }, void 0, true, {
                                        fileName: "[project]/src/features/admissions/components/VetReviewForm.tsx",
                                        lineNumber: 991,
                                        columnNumber: 13
                                    }, this)
                                ]
                            }, void 0, true, {
                                fileName: "[project]/src/features/admissions/components/VetReviewForm.tsx",
                                lineNumber: 875,
                                columnNumber: 11
                            }, this),
                            /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$compiled$2f$react$2f$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$client$5d$__$28$ecmascript$29$__["jsxDEV"])("div", {
                                className: __TURBOPACK__imported__module__$5b$project$5d2f$src$2f$features$2f$admissions$2f$shared$2f$components$2f$AdmissionDetailLayout$2e$tsx__$5b$app$2d$client$5d$__$28$ecmascript$29$__["admissionAsideClassName"],
                                children: [
                                    asideTop,
                                    /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$compiled$2f$react$2f$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$client$5d$__$28$ecmascript$29$__["jsxDEV"])("div", {
                                        className: "space-y-4 min-w-0 rounded-[var(--radius-lg)] border border-[var(--color-border)] bg-[var(--color-surface)] p-4 shadow-[var(--shadow-panel)]",
                                        children: [
                                            /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$compiled$2f$react$2f$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$client$5d$__$28$ecmascript$29$__["jsxDEV"])("div", {
                                                className: "border-b border-[var(--color-border)] pb-3",
                                                children: [
                                                    /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$compiled$2f$react$2f$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$client$5d$__$28$ecmascript$29$__["jsxDEV"])("h3", {
                                                        className: "text-sm font-semibold tracking-tight text-[var(--color-text-primary)]",
                                                        children: "Veterinary Decision Panel"
                                                    }, void 0, false, {
                                                        fileName: "[project]/src/features/admissions/components/VetReviewForm.tsx",
                                                        lineNumber: 1276,
                                                        columnNumber: 15
                                                    }, this),
                                                    /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$compiled$2f$react$2f$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$client$5d$__$28$ecmascript$29$__["jsxDEV"])("p", {
                                                        className: "text-xs text-[var(--color-text-muted)]",
                                                        children: [
                                                            "Authorizes or restricts athletic activity for ",
                                                            candidateName,
                                                            "."
                                                        ]
                                                    }, void 0, true, {
                                                        fileName: "[project]/src/features/admissions/components/VetReviewForm.tsx",
                                                        lineNumber: 1279,
                                                        columnNumber: 15
                                                    }, this)
                                                ]
                                            }, void 0, true, {
                                                fileName: "[project]/src/features/admissions/components/VetReviewForm.tsx",
                                                lineNumber: 1275,
                                                columnNumber: 13
                                            }, this),
                                            /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$compiled$2f$react$2f$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$client$5d$__$28$ecmascript$29$__["jsxDEV"])("div", {
                                                className: "space-y-2",
                                                children: [
                                                    /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$compiled$2f$react$2f$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$client$5d$__$28$ecmascript$29$__["jsxDEV"])("label", {
                                                        className: "block text-xs font-bold uppercase tracking-wider text-[var(--color-text-secondary)]",
                                                        children: "Training Clearance"
                                                    }, void 0, false, {
                                                        fileName: "[project]/src/features/admissions/components/VetReviewForm.tsx",
                                                        lineNumber: 1286,
                                                        columnNumber: 15
                                                    }, this),
                                                    /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$compiled$2f$react$2f$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$client$5d$__$28$ecmascript$29$__["jsxDEV"])("div", {
                                                        className: "space-y-2",
                                                        children: trainingDecisions.map((opt)=>{
                                                            const isSelected = trainingDecision === opt.value;
                                                            return /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$compiled$2f$react$2f$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$client$5d$__$28$ecmascript$29$__["jsxDEV"])("label", {
                                                                className: `flex cursor-pointer items-start gap-2.5 rounded-[var(--radius-md)] border p-2.5 transition-all ${isSelected ? `${opt.activeBorder} ${opt.activeBg}` : 'border-[var(--color-border)] bg-[var(--color-surface)] hover:bg-[var(--color-surface-subtle)]'}`,
                                                                children: [
                                                                    /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$compiled$2f$react$2f$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$client$5d$__$28$ecmascript$29$__["jsxDEV"])(__TURBOPACK__imported__module__$5b$project$5d2f$src$2f$components$2f$ui$2f$Input$2e$tsx__$5b$app$2d$client$5d$__$28$ecmascript$29$__["ChoiceInput"], {
                                                                        type: "radio",
                                                                        name: "trainingDecision",
                                                                        value: opt.value,
                                                                        checked: isSelected,
                                                                        onChange: ()=>{
                                                                            setTrainingDecision(opt.value);
                                                                            if (opt.value === 'BLOCKED' && !followUpDate) {
                                                                                setFollowUpDate(businessDateFromToday(7));
                                                                            }
                                                                            if (fieldErrors.restrictionDetails) {
                                                                                setFieldErrors((errs)=>({
                                                                                        ...errs,
                                                                                        restrictionDetails: ''
                                                                                    }));
                                                                            }
                                                                        }
                                                                    }, void 0, false, {
                                                                        fileName: "[project]/src/features/admissions/components/VetReviewForm.tsx",
                                                                        lineNumber: 1302,
                                                                        columnNumber: 23
                                                                    }, this),
                                                                    /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$compiled$2f$react$2f$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$client$5d$__$28$ecmascript$29$__["jsxDEV"])("div", {
                                                                        className: "min-w-0 flex-1",
                                                                        children: [
                                                                            /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$compiled$2f$react$2f$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$client$5d$__$28$ecmascript$29$__["jsxDEV"])("div", {
                                                                                className: "flex items-center justify-between gap-1",
                                                                                children: [
                                                                                    /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$compiled$2f$react$2f$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$client$5d$__$28$ecmascript$29$__["jsxDEV"])("span", {
                                                                                        className: "text-xs font-bold text-[var(--color-text-primary)]",
                                                                                        children: opt.title
                                                                                    }, void 0, false, {
                                                                                        fileName: "[project]/src/features/admissions/components/VetReviewForm.tsx",
                                                                                        lineNumber: 1319,
                                                                                        columnNumber: 27
                                                                                    }, this),
                                                                                    /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$compiled$2f$react$2f$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$client$5d$__$28$ecmascript$29$__["jsxDEV"])(__TURBOPACK__imported__module__$5b$project$5d2f$src$2f$components$2f$ui$2f$StatusBadge$2e$tsx__$5b$app$2d$client$5d$__$28$ecmascript$29$__["Pill"], {
                                                                                        tone: opt.tone,
                                                                                        size: "sm",
                                                                                        icon: opt.icon,
                                                                                        children: opt.value
                                                                                    }, void 0, false, {
                                                                                        fileName: "[project]/src/features/admissions/components/VetReviewForm.tsx",
                                                                                        lineNumber: 1322,
                                                                                        columnNumber: 27
                                                                                    }, this)
                                                                                ]
                                                                            }, void 0, true, {
                                                                                fileName: "[project]/src/features/admissions/components/VetReviewForm.tsx",
                                                                                lineNumber: 1318,
                                                                                columnNumber: 25
                                                                            }, this),
                                                                            /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$compiled$2f$react$2f$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$client$5d$__$28$ecmascript$29$__["jsxDEV"])("p", {
                                                                                className: "mt-0.5 text-xs text-[var(--color-text-secondary)]",
                                                                                children: opt.subtitle
                                                                            }, void 0, false, {
                                                                                fileName: "[project]/src/features/admissions/components/VetReviewForm.tsx",
                                                                                lineNumber: 1326,
                                                                                columnNumber: 25
                                                                            }, this)
                                                                        ]
                                                                    }, void 0, true, {
                                                                        fileName: "[project]/src/features/admissions/components/VetReviewForm.tsx",
                                                                        lineNumber: 1317,
                                                                        columnNumber: 23
                                                                    }, this)
                                                                ]
                                                            }, opt.value, true, {
                                                                fileName: "[project]/src/features/admissions/components/VetReviewForm.tsx",
                                                                lineNumber: 1294,
                                                                columnNumber: 21
                                                            }, this);
                                                        })
                                                    }, void 0, false, {
                                                        fileName: "[project]/src/features/admissions/components/VetReviewForm.tsx",
                                                        lineNumber: 1290,
                                                        columnNumber: 15
                                                    }, this)
                                                ]
                                            }, void 0, true, {
                                                fileName: "[project]/src/features/admissions/components/VetReviewForm.tsx",
                                                lineNumber: 1285,
                                                columnNumber: 13
                                            }, this),
                                            trainingDecision === 'BLOCKED' && /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$compiled$2f$react$2f$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$client$5d$__$28$ecmascript$29$__["jsxDEV"])("div", {
                                                className: "rounded-[var(--radius-md)] border border-[var(--color-warning)] bg-[var(--color-warning-soft)] p-3 space-y-1.5 animate-in fade-in duration-200",
                                                children: [
                                                    /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$compiled$2f$react$2f$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$client$5d$__$28$ecmascript$29$__["jsxDEV"])("label", {
                                                        htmlFor: "field-restriction-details",
                                                        className: "block text-xs font-bold text-[var(--color-warning)]",
                                                        children: [
                                                            "Mandatory Restriction Protocol ",
                                                            /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$compiled$2f$react$2f$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$client$5d$__$28$ecmascript$29$__["jsxDEV"])("span", {
                                                                className: "text-[var(--color-danger)]",
                                                                children: "*"
                                                            }, void 0, false, {
                                                                fileName: "[project]/src/features/admissions/components/VetReviewForm.tsx",
                                                                lineNumber: 1343,
                                                                columnNumber: 50
                                                            }, this)
                                                        ]
                                                    }, void 0, true, {
                                                        fileName: "[project]/src/features/admissions/components/VetReviewForm.tsx",
                                                        lineNumber: 1339,
                                                        columnNumber: 17
                                                    }, this),
                                                    /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$compiled$2f$react$2f$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$client$5d$__$28$ecmascript$29$__["jsxDEV"])("p", {
                                                        className: "text-xs text-[var(--color-text-secondary)]",
                                                        children: "Shown to the Head Trainer when planning. The horse rests until the follow-up exam below."
                                                    }, void 0, false, {
                                                        fileName: "[project]/src/features/admissions/components/VetReviewForm.tsx",
                                                        lineNumber: 1345,
                                                        columnNumber: 17
                                                    }, this),
                                                    isUrgent ? /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$compiled$2f$react$2f$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$client$5d$__$28$ecmascript$29$__["jsxDEV"])(__TURBOPACK__imported__module__$5b$project$5d2f$src$2f$components$2f$ui$2f$Input$2e$tsx__$5b$app$2d$client$5d$__$28$ecmascript$29$__["Textarea"], {
                                                        id: "field-restriction-details",
                                                        rows: 2,
                                                        value: restrictionDetails,
                                                        onChange: (e)=>setRestrictionDetails(e.target.value),
                                                        placeholder: "Describe restriction rules..."
                                                    }, void 0, false, {
                                                        fileName: "[project]/src/features/admissions/components/VetReviewForm.tsx",
                                                        lineNumber: 1349,
                                                        columnNumber: 19
                                                    }, this) : /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$compiled$2f$react$2f$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$client$5d$__$28$ecmascript$29$__["jsxDEV"])(__TURBOPACK__imported__module__$5b$project$5d2f$src$2f$components$2f$ui$2f$Input$2e$tsx__$5b$app$2d$client$5d$__$28$ecmascript$29$__["Select"], {
                                                        id: "field-restriction-details",
                                                        value: restrictionDetails,
                                                        onChange: (e)=>{
                                                            setRestrictionDetails(e.target.value);
                                                            if (fieldErrors.restrictionDetails) {
                                                                setFieldErrors((errs)=>({
                                                                        ...errs,
                                                                        restrictionDetails: ''
                                                                    }));
                                                            }
                                                        },
                                                        children: [
                                                            /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$compiled$2f$react$2f$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$client$5d$__$28$ecmascript$29$__["jsxDEV"])("option", {
                                                                value: "",
                                                                children: "Select restriction protocol"
                                                            }, void 0, false, {
                                                                fileName: "[project]/src/features/admissions/components/VetReviewForm.tsx",
                                                                lineNumber: 1367,
                                                                columnNumber: 21
                                                            }, this),
                                                            restrictionOptions.map((opt)=>/*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$compiled$2f$react$2f$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$client$5d$__$28$ecmascript$29$__["jsxDEV"])("option", {
                                                                    value: opt,
                                                                    children: opt
                                                                }, opt, false, {
                                                                    fileName: "[project]/src/features/admissions/components/VetReviewForm.tsx",
                                                                    lineNumber: 1369,
                                                                    columnNumber: 23
                                                                }, this))
                                                        ]
                                                    }, void 0, true, {
                                                        fileName: "[project]/src/features/admissions/components/VetReviewForm.tsx",
                                                        lineNumber: 1357,
                                                        columnNumber: 19
                                                    }, this),
                                                    fieldErrors.restrictionDetails && /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$compiled$2f$react$2f$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$client$5d$__$28$ecmascript$29$__["jsxDEV"])("p", {
                                                        className: "text-xs font-medium text-[var(--color-danger)]",
                                                        children: fieldErrors.restrictionDetails
                                                    }, void 0, false, {
                                                        fileName: "[project]/src/features/admissions/components/VetReviewForm.tsx",
                                                        lineNumber: 1376,
                                                        columnNumber: 19
                                                    }, this)
                                                ]
                                            }, void 0, true, {
                                                fileName: "[project]/src/features/admissions/components/VetReviewForm.tsx",
                                                lineNumber: 1338,
                                                columnNumber: 15
                                            }, this),
                                            /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$compiled$2f$react$2f$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$client$5d$__$28$ecmascript$29$__["jsxDEV"])("div", {
                                                className: "rounded-[var(--radius-md)] border border-[var(--color-border)] bg-[var(--color-surface-subtle)] p-3 space-y-2",
                                                children: [
                                                    /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$compiled$2f$react$2f$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$client$5d$__$28$ecmascript$29$__["jsxDEV"])("label", {
                                                        className: "flex cursor-pointer items-center justify-between",
                                                        children: [
                                                            /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$compiled$2f$react$2f$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$client$5d$__$28$ecmascript$29$__["jsxDEV"])("span", {
                                                                className: "text-xs font-semibold text-[var(--color-text-primary)]",
                                                                children: [
                                                                    "Schedule Follow-up Exam",
                                                                    followUpRequired && /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$compiled$2f$react$2f$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$client$5d$__$28$ecmascript$29$__["jsxDEV"])("span", {
                                                                        className: "text-[var(--color-danger)]",
                                                                        children: " *"
                                                                    }, void 0, false, {
                                                                        fileName: "[project]/src/features/admissions/components/VetReviewForm.tsx",
                                                                        lineNumber: 1388,
                                                                        columnNumber: 40
                                                                    }, this)
                                                                ]
                                                            }, void 0, true, {
                                                                fileName: "[project]/src/features/admissions/components/VetReviewForm.tsx",
                                                                lineNumber: 1386,
                                                                columnNumber: 17
                                                            }, this),
                                                            /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$compiled$2f$react$2f$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$client$5d$__$28$ecmascript$29$__["jsxDEV"])(__TURBOPACK__imported__module__$5b$project$5d2f$src$2f$components$2f$ui$2f$Input$2e$tsx__$5b$app$2d$client$5d$__$28$ecmascript$29$__["ChoiceInput"], {
                                                                type: "checkbox",
                                                                checked: followUpActive,
                                                                disabled: followUpRequired,
                                                                onChange: (e)=>{
                                                                    const checked = e.target.checked;
                                                                    setScheduleFollowUp(checked);
                                                                    if (checked && !followUpDate) {
                                                                        setFollowUpDate(businessDateFromToday(7));
                                                                    }
                                                                }
                                                            }, void 0, false, {
                                                                fileName: "[project]/src/features/admissions/components/VetReviewForm.tsx",
                                                                lineNumber: 1390,
                                                                columnNumber: 17
                                                            }, this)
                                                        ]
                                                    }, void 0, true, {
                                                        fileName: "[project]/src/features/admissions/components/VetReviewForm.tsx",
                                                        lineNumber: 1385,
                                                        columnNumber: 15
                                                    }, this),
                                                    followUpRequired && /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$compiled$2f$react$2f$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$client$5d$__$28$ecmascript$29$__["jsxDEV"])("p", {
                                                        className: "text-xs text-[var(--color-text-secondary)]",
                                                        children: "Required when training is blocked: the horse stays off training until this exam."
                                                    }, void 0, false, {
                                                        fileName: "[project]/src/features/admissions/components/VetReviewForm.tsx",
                                                        lineNumber: 1404,
                                                        columnNumber: 17
                                                    }, this),
                                                    followUpActive && /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$compiled$2f$react$2f$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$client$5d$__$28$ecmascript$29$__["jsxDEV"])("div", {
                                                        className: "space-y-2 border-t border-[var(--color-border)] pt-2 animate-in fade-in duration-200",
                                                        children: [
                                                            /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$compiled$2f$react$2f$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$client$5d$__$28$ecmascript$29$__["jsxDEV"])("div", {
                                                                children: [
                                                                    /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$compiled$2f$react$2f$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$client$5d$__$28$ecmascript$29$__["jsxDEV"])(__TURBOPACK__imported__module__$5b$project$5d2f$src$2f$components$2f$ui$2f$FormField$2e$tsx__$5b$app$2d$client$5d$__$28$ecmascript$29$__["FormField"], {
                                                                        label: "Target Date",
                                                                        htmlFor: "field-followup-date",
                                                                        required: true,
                                                                        children: /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$compiled$2f$react$2f$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$client$5d$__$28$ecmascript$29$__["jsxDEV"])(__TURBOPACK__imported__module__$5b$project$5d2f$src$2f$components$2f$ui$2f$Input$2e$tsx__$5b$app$2d$client$5d$__$28$ecmascript$29$__["Input"], {
                                                                            id: "field-followup-date",
                                                                            type: "date",
                                                                            min: tomorrowBusinessDate(),
                                                                            value: followUpDate,
                                                                            onChange: (e)=>{
                                                                                setFollowUpDate(e.target.value);
                                                                                if (fieldErrors.followUpDate) {
                                                                                    setFieldErrors((errs)=>({
                                                                                            ...errs,
                                                                                            followUpDate: ''
                                                                                        }));
                                                                                }
                                                                            }
                                                                        }, void 0, false, {
                                                                            fileName: "[project]/src/features/admissions/components/VetReviewForm.tsx",
                                                                            lineNumber: 1413,
                                                                            columnNumber: 23
                                                                        }, this)
                                                                    }, void 0, false, {
                                                                        fileName: "[project]/src/features/admissions/components/VetReviewForm.tsx",
                                                                        lineNumber: 1412,
                                                                        columnNumber: 21
                                                                    }, this),
                                                                    /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$compiled$2f$react$2f$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$client$5d$__$28$ecmascript$29$__["jsxDEV"])("div", {
                                                                        className: "mt-1 flex gap-1",
                                                                        children: [
                                                                            3,
                                                                            7,
                                                                            14
                                                                        ].map((days)=>{
                                                                            const iso = businessDateFromToday(days);
                                                                            return /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$compiled$2f$react$2f$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$client$5d$__$28$ecmascript$29$__["jsxDEV"])(__TURBOPACK__imported__module__$5b$project$5d2f$src$2f$components$2f$ui$2f$Button$2e$tsx__$5b$app$2d$client$5d$__$28$ecmascript$29$__["Button"], {
                                                                                size: "sm",
                                                                                variant: "secondary",
                                                                                onClick: ()=>setFollowUpDate(iso),
                                                                                children: [
                                                                                    "+",
                                                                                    days,
                                                                                    "d"
                                                                                ]
                                                                            }, days, true, {
                                                                                fileName: "[project]/src/features/admissions/components/VetReviewForm.tsx",
                                                                                lineNumber: 1430,
                                                                                columnNumber: 27
                                                                            }, this);
                                                                        })
                                                                    }, void 0, false, {
                                                                        fileName: "[project]/src/features/admissions/components/VetReviewForm.tsx",
                                                                        lineNumber: 1426,
                                                                        columnNumber: 21
                                                                    }, this),
                                                                    fieldErrors.followUpDate && /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$compiled$2f$react$2f$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$client$5d$__$28$ecmascript$29$__["jsxDEV"])("p", {
                                                                        className: "mt-0.5 text-xs font-medium text-[var(--color-danger)]",
                                                                        children: fieldErrors.followUpDate
                                                                    }, void 0, false, {
                                                                        fileName: "[project]/src/features/admissions/components/VetReviewForm.tsx",
                                                                        lineNumber: 1437,
                                                                        columnNumber: 23
                                                                    }, this)
                                                                ]
                                                            }, void 0, true, {
                                                                fileName: "[project]/src/features/admissions/components/VetReviewForm.tsx",
                                                                lineNumber: 1411,
                                                                columnNumber: 19
                                                            }, this),
                                                            /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$compiled$2f$react$2f$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$client$5d$__$28$ecmascript$29$__["jsxDEV"])("div", {
                                                                children: [
                                                                    /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$compiled$2f$react$2f$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$client$5d$__$28$ecmascript$29$__["jsxDEV"])(__TURBOPACK__imported__module__$5b$project$5d2f$src$2f$components$2f$ui$2f$FormField$2e$tsx__$5b$app$2d$client$5d$__$28$ecmascript$29$__["FormField"], {
                                                                        label: "Care Procedure",
                                                                        htmlFor: "field-followup-desc",
                                                                        required: true,
                                                                        children: /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$compiled$2f$react$2f$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$client$5d$__$28$ecmascript$29$__["jsxDEV"])(__TURBOPACK__imported__module__$5b$project$5d2f$src$2f$components$2f$ui$2f$Input$2e$tsx__$5b$app$2d$client$5d$__$28$ecmascript$29$__["Select"], {
                                                                            id: "field-followup-desc",
                                                                            value: followUpDescription,
                                                                            onChange: (e)=>{
                                                                                setFollowUpDescription(e.target.value);
                                                                                if (fieldErrors.followUpDescription) {
                                                                                    setFieldErrors((errs)=>({
                                                                                            ...errs,
                                                                                            followUpDescription: ''
                                                                                        }));
                                                                                }
                                                                            },
                                                                            children: followUpDescriptionOptions.map((opt)=>/*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$compiled$2f$react$2f$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$client$5d$__$28$ecmascript$29$__["jsxDEV"])("option", {
                                                                                    value: opt,
                                                                                    children: opt
                                                                                }, opt, false, {
                                                                                    fileName: "[project]/src/features/admissions/components/VetReviewForm.tsx",
                                                                                    lineNumber: 1456,
                                                                                    columnNumber: 27
                                                                                }, this))
                                                                        }, void 0, false, {
                                                                            fileName: "[project]/src/features/admissions/components/VetReviewForm.tsx",
                                                                            lineNumber: 1445,
                                                                            columnNumber: 23
                                                                        }, this)
                                                                    }, void 0, false, {
                                                                        fileName: "[project]/src/features/admissions/components/VetReviewForm.tsx",
                                                                        lineNumber: 1444,
                                                                        columnNumber: 21
                                                                    }, this),
                                                                    fieldErrors.followUpDescription && /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$compiled$2f$react$2f$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$client$5d$__$28$ecmascript$29$__["jsxDEV"])("p", {
                                                                        className: "mt-0.5 text-xs font-medium text-[var(--color-danger)]",
                                                                        children: fieldErrors.followUpDescription
                                                                    }, void 0, false, {
                                                                        fileName: "[project]/src/features/admissions/components/VetReviewForm.tsx",
                                                                        lineNumber: 1463,
                                                                        columnNumber: 23
                                                                    }, this)
                                                                ]
                                                            }, void 0, true, {
                                                                fileName: "[project]/src/features/admissions/components/VetReviewForm.tsx",
                                                                lineNumber: 1443,
                                                                columnNumber: 19
                                                            }, this)
                                                        ]
                                                    }, void 0, true, {
                                                        fileName: "[project]/src/features/admissions/components/VetReviewForm.tsx",
                                                        lineNumber: 1410,
                                                        columnNumber: 17
                                                    }, this)
                                                ]
                                            }, void 0, true, {
                                                fileName: "[project]/src/features/admissions/components/VetReviewForm.tsx",
                                                lineNumber: 1384,
                                                columnNumber: 13
                                            }, this),
                                            /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$compiled$2f$react$2f$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$client$5d$__$28$ecmascript$29$__["jsxDEV"])("div", {
                                                className: "space-y-2 border-t border-[var(--color-border)] pt-3 hidden md:block",
                                                children: [
                                                    formError && /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$compiled$2f$react$2f$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$client$5d$__$28$ecmascript$29$__["jsxDEV"])(__TURBOPACK__imported__module__$5b$project$5d2f$src$2f$components$2f$ui$2f$Notice$2e$tsx__$5b$app$2d$client$5d$__$28$ecmascript$29$__["Notice"], {
                                                        tone: "error",
                                                        children: formError
                                                    }, void 0, false, {
                                                        fileName: "[project]/src/features/admissions/components/VetReviewForm.tsx",
                                                        lineNumber: 1474,
                                                        columnNumber: 29
                                                    }, this),
                                                    /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$compiled$2f$react$2f$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$client$5d$__$28$ecmascript$29$__["jsxDEV"])(__TURBOPACK__imported__module__$5b$project$5d2f$src$2f$components$2f$ui$2f$Button$2e$tsx__$5b$app$2d$client$5d$__$28$ecmascript$29$__["Button"], {
                                                        type: "submit",
                                                        variant: "primary",
                                                        loading: submitting,
                                                        disabled: submitting || startingExam || isGated,
                                                        icon: "check",
                                                        className: "w-full",
                                                        children: "Complete Examination"
                                                    }, void 0, false, {
                                                        fileName: "[project]/src/features/admissions/components/VetReviewForm.tsx",
                                                        lineNumber: 1477,
                                                        columnNumber: 15
                                                    }, this),
                                                    /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$compiled$2f$react$2f$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$client$5d$__$28$ecmascript$29$__["jsxDEV"])(__TURBOPACK__imported__module__$5b$project$5d2f$src$2f$components$2f$ui$2f$Button$2e$tsx__$5b$app$2d$client$5d$__$28$ecmascript$29$__["Button"], {
                                                        type: "button",
                                                        variant: "tertiary",
                                                        size: "sm",
                                                        className: "w-full",
                                                        disabled: submitting,
                                                        onClick: handleSaveDraft,
                                                        children: "Save draft locally"
                                                    }, void 0, false, {
                                                        fileName: "[project]/src/features/admissions/components/VetReviewForm.tsx",
                                                        lineNumber: 1489,
                                                        columnNumber: 15
                                                    }, this)
                                                ]
                                            }, void 0, true, {
                                                fileName: "[project]/src/features/admissions/components/VetReviewForm.tsx",
                                                lineNumber: 1473,
                                                columnNumber: 13
                                            }, this),
                                            /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$compiled$2f$react$2f$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$client$5d$__$28$ecmascript$29$__["jsxDEV"])("div", {
                                                className: "rounded-[var(--radius-sm)] bg-[var(--color-surface-subtle)] p-2.5 text-xs text-[var(--color-text-muted)] leading-relaxed",
                                                children: [
                                                    /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$compiled$2f$react$2f$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$client$5d$__$28$ecmascript$29$__["jsxDEV"])("span", {
                                                        className: "font-semibold text-[var(--color-text-secondary)]",
                                                        children: "Outcome:"
                                                    }, void 0, false, {
                                                        fileName: "[project]/src/features/admissions/components/VetReviewForm.tsx",
                                                        lineNumber: 1496,
                                                        columnNumber: 15
                                                    }, this),
                                                    /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$compiled$2f$react$2f$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$client$5d$__$28$ecmascript$29$__["jsxDEV"])("ul", {
                                                        className: "mt-1 space-y-0.5 list-disc pl-3.5",
                                                        children: [
                                                            /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$compiled$2f$react$2f$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$client$5d$__$28$ecmascript$29$__["jsxDEV"])("li", {
                                                                children: "Permanently links health examination record."
                                                            }, void 0, false, {
                                                                fileName: "[project]/src/features/admissions/components/VetReviewForm.tsx",
                                                                lineNumber: 1498,
                                                                columnNumber: 17
                                                            }, this),
                                                            /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$compiled$2f$react$2f$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$client$5d$__$28$ecmascript$29$__["jsxDEV"])("li", {
                                                                children: "Advances admission to Head Trainer review stage."
                                                            }, void 0, false, {
                                                                fileName: "[project]/src/features/admissions/components/VetReviewForm.tsx",
                                                                lineNumber: 1499,
                                                                columnNumber: 17
                                                            }, this),
                                                            /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$compiled$2f$react$2f$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$client$5d$__$28$ecmascript$29$__["jsxDEV"])("li", {
                                                                children: "Quarantine stall stays locked until final manager clearance."
                                                            }, void 0, false, {
                                                                fileName: "[project]/src/features/admissions/components/VetReviewForm.tsx",
                                                                lineNumber: 1500,
                                                                columnNumber: 17
                                                            }, this)
                                                        ]
                                                    }, void 0, true, {
                                                        fileName: "[project]/src/features/admissions/components/VetReviewForm.tsx",
                                                        lineNumber: 1497,
                                                        columnNumber: 15
                                                    }, this)
                                                ]
                                            }, void 0, true, {
                                                fileName: "[project]/src/features/admissions/components/VetReviewForm.tsx",
                                                lineNumber: 1495,
                                                columnNumber: 13
                                            }, this)
                                        ]
                                    }, void 0, true, {
                                        fileName: "[project]/src/features/admissions/components/VetReviewForm.tsx",
                                        lineNumber: 1274,
                                        columnNumber: 11
                                    }, this)
                                ]
                            }, void 0, true, {
                                fileName: "[project]/src/features/admissions/components/VetReviewForm.tsx",
                                lineNumber: 1272,
                                columnNumber: 11
                            }, this)
                        ]
                    }, void 0, true, {
                        fileName: "[project]/src/features/admissions/components/VetReviewForm.tsx",
                        lineNumber: 873,
                        columnNumber: 9
                    }, this),
                    /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$compiled$2f$react$2f$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$client$5d$__$28$ecmascript$29$__["jsxDEV"])("div", {
                        className: "md:hidden fixed bottom-0 left-0 right-0 z-40 bg-[var(--color-surface)]/95 backdrop-blur-md border-t border-[var(--color-border)] p-3 shadow-[var(--shadow-popover)] space-y-2",
                        children: [
                            formError && /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$compiled$2f$react$2f$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$client$5d$__$28$ecmascript$29$__["jsxDEV"])(__TURBOPACK__imported__module__$5b$project$5d2f$src$2f$components$2f$ui$2f$Notice$2e$tsx__$5b$app$2d$client$5d$__$28$ecmascript$29$__["Notice"], {
                                tone: "error",
                                children: formError
                            }, void 0, false, {
                                fileName: "[project]/src/features/admissions/components/VetReviewForm.tsx",
                                lineNumber: 1509,
                                columnNumber: 25
                            }, this),
                            /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$compiled$2f$react$2f$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$client$5d$__$28$ecmascript$29$__["jsxDEV"])("div", {
                                className: "flex items-center justify-between gap-2.5",
                                children: /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$compiled$2f$react$2f$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$client$5d$__$28$ecmascript$29$__["jsxDEV"])(__TURBOPACK__imported__module__$5b$project$5d2f$src$2f$components$2f$ui$2f$Button$2e$tsx__$5b$app$2d$client$5d$__$28$ecmascript$29$__["Button"], {
                                    type: "submit",
                                    variant: "primary",
                                    loading: submitting,
                                    disabled: submitting || startingExam || isGated,
                                    icon: "check",
                                    className: "w-full",
                                    children: "Complete Examination"
                                }, void 0, false, {
                                    fileName: "[project]/src/features/admissions/components/VetReviewForm.tsx",
                                    lineNumber: 1511,
                                    columnNumber: 13
                                }, this)
                            }, void 0, false, {
                                fileName: "[project]/src/features/admissions/components/VetReviewForm.tsx",
                                lineNumber: 1510,
                                columnNumber: 11
                            }, this)
                        ]
                    }, void 0, true, {
                        fileName: "[project]/src/features/admissions/components/VetReviewForm.tsx",
                        lineNumber: 1508,
                        columnNumber: 9
                    }, this)
                ]
            }, void 0, true, {
                fileName: "[project]/src/features/admissions/components/VetReviewForm.tsx",
                lineNumber: 756,
                columnNumber: 7
            }, this),
            /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$compiled$2f$react$2f$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$client$5d$__$28$ecmascript$29$__["jsxDEV"])(__TURBOPACK__imported__module__$5b$project$5d2f$src$2f$components$2f$ui$2f$ConfirmDialog$2e$tsx__$5b$app$2d$client$5d$__$28$ecmascript$29$__["ConfirmDialog"], {
                open: showApproveConfirm,
                title: "Submit Veterinary Examination?",
                description: /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$compiled$2f$react$2f$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$client$5d$__$28$ecmascript$29$__["jsxDEV"])("div", {
                    className: "space-y-2 text-sm",
                    children: [
                        /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$compiled$2f$react$2f$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$client$5d$__$28$ecmascript$29$__["jsxDEV"])("p", {
                            children: [
                                "You are certifying medical examination for ",
                                /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$compiled$2f$react$2f$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$client$5d$__$28$ecmascript$29$__["jsxDEV"])("strong", {
                                    children: candidateName
                                }, void 0, false, {
                                    fileName: "[project]/src/features/admissions/components/VetReviewForm.tsx",
                                    lineNumber: 1532,
                                    columnNumber: 58
                                }, this),
                                " with training decision",
                                ' ',
                                /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$compiled$2f$react$2f$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$client$5d$__$28$ecmascript$29$__["jsxDEV"])("strong", {
                                    className: "uppercase text-[var(--color-primary)]",
                                    children: trainingDecision
                                }, void 0, false, {
                                    fileName: "[project]/src/features/admissions/components/VetReviewForm.tsx",
                                    lineNumber: 1533,
                                    columnNumber: 15
                                }, this),
                                "."
                            ]
                        }, void 0, true, {
                            fileName: "[project]/src/features/admissions/components/VetReviewForm.tsx",
                            lineNumber: 1531,
                            columnNumber: 13
                        }, this),
                        followUpActive && /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$compiled$2f$react$2f$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$client$5d$__$28$ecmascript$29$__["jsxDEV"])("div", {
                            className: "rounded-[var(--radius-sm)] bg-[var(--color-warning-soft)] p-2 text-xs text-[var(--color-warning)] font-medium",
                            children: [
                                "📅 Follow-up scheduled for ",
                                /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$compiled$2f$react$2f$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$client$5d$__$28$ecmascript$29$__["jsxDEV"])("strong", {
                                    children: followUpDate
                                }, void 0, false, {
                                    fileName: "[project]/src/features/admissions/components/VetReviewForm.tsx",
                                    lineNumber: 1537,
                                    columnNumber: 44
                                }, this),
                                ": ",
                                followUpDescription,
                                "."
                            ]
                        }, void 0, true, {
                            fileName: "[project]/src/features/admissions/components/VetReviewForm.tsx",
                            lineNumber: 1536,
                            columnNumber: 15
                        }, this),
                        /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$compiled$2f$react$2f$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$client$5d$__$28$ecmascript$29$__["jsxDEV"])("p", {
                            className: "text-xs text-[var(--color-text-secondary)]",
                            children: careType === 'INITIAL' ? 'This action writes the official clinical record and advances the admission to trainer review.' : 'This action completes only this care schedule and does not change the admission workflow.'
                        }, void 0, false, {
                            fileName: "[project]/src/features/admissions/components/VetReviewForm.tsx",
                            lineNumber: 1540,
                            columnNumber: 13
                        }, this)
                    ]
                }, void 0, true, {
                    fileName: "[project]/src/features/admissions/components/VetReviewForm.tsx",
                    lineNumber: 1530,
                    columnNumber: 11
                }, this),
                confirmLabel: "Confirm & Submit",
                cancelLabel: "Back to Review",
                tone: "primary",
                loading: submitting,
                onConfirm: ()=>executeSubmit(),
                onCancel: ()=>setShowApproveConfirm(false)
            }, void 0, false, {
                fileName: "[project]/src/features/admissions/components/VetReviewForm.tsx",
                lineNumber: 1526,
                columnNumber: 7
            }, this)
        ]
    }, void 0, true, {
        fileName: "[project]/src/features/admissions/components/VetReviewForm.tsx",
        lineNumber: 755,
        columnNumber: 5
    }, this);
}
_s(VetReviewForm, "7BALCini/u+jmTXGkWyDWbeypPs=");
_c = VetReviewForm;
var _c;
__turbopack_context__.k.register(_c, "VetReviewForm");
if (typeof globalThis.$RefreshHelpers$ === 'object' && globalThis.$RefreshHelpers !== null) {
    __turbopack_context__.k.registerExports(__turbopack_context__.m, globalThis.$RefreshHelpers$);
}
}),
"[project]/src/features/admissions/shared/components/AdmissionDetailHeader.tsx [app-client] (ecmascript)", ((__turbopack_context__) => {
"use strict";

__turbopack_context__.s([
    "AdmissionDetailHeader",
    ()=>AdmissionDetailHeader
]);
var __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$compiled$2f$react$2f$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$client$5d$__$28$ecmascript$29$__ = __turbopack_context__.i("[project]/node_modules/next/dist/compiled/react/jsx-dev-runtime.js [app-client] (ecmascript)");
var __TURBOPACK__imported__module__$5b$project$5d2f$src$2f$components$2f$ui$2f$HorseAvatar$2e$tsx__$5b$app$2d$client$5d$__$28$ecmascript$29$__ = __turbopack_context__.i("[project]/src/components/ui/HorseAvatar.tsx [app-client] (ecmascript)");
var __TURBOPACK__imported__module__$5b$project$5d2f$src$2f$lib$2f$display$2e$ts__$5b$app$2d$client$5d$__$28$ecmascript$29$__ = __turbopack_context__.i("[project]/src/lib/display.ts [app-client] (ecmascript)");
var __TURBOPACK__imported__module__$5b$project$5d2f$src$2f$features$2f$admissions$2f$shared$2f$components$2f$AdmissionStatusBadge$2e$tsx__$5b$app$2d$client$5d$__$28$ecmascript$29$__ = __turbopack_context__.i("[project]/src/features/admissions/shared/components/AdmissionStatusBadge.tsx [app-client] (ecmascript)");
;
;
;
;
function HeaderValue({ label, value }) {
    return /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$compiled$2f$react$2f$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$client$5d$__$28$ecmascript$29$__["jsxDEV"])("div", {
        className: "min-w-0",
        children: [
            /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$compiled$2f$react$2f$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$client$5d$__$28$ecmascript$29$__["jsxDEV"])("span", {
                className: "mb-0.5 block text-xs font-medium uppercase tracking-wide text-[var(--color-text-muted)]",
                children: label
            }, void 0, false, {
                fileName: "[project]/src/features/admissions/shared/components/AdmissionDetailHeader.tsx",
                lineNumber: 18,
                columnNumber: 7
            }, this),
            /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$compiled$2f$react$2f$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$client$5d$__$28$ecmascript$29$__["jsxDEV"])("span", {
                className: "break-words text-sm font-medium text-[var(--color-text-primary)]",
                children: value
            }, void 0, false, {
                fileName: "[project]/src/features/admissions/shared/components/AdmissionDetailHeader.tsx",
                lineNumber: 19,
                columnNumber: 7
            }, this)
        ]
    }, void 0, true, {
        fileName: "[project]/src/features/admissions/shared/components/AdmissionDetailHeader.tsx",
        lineNumber: 17,
        columnNumber: 5
    }, this);
}
_c = HeaderValue;
function date(value) {
    return value ? (0, __TURBOPACK__imported__module__$5b$project$5d2f$src$2f$lib$2f$display$2e$ts__$5b$app$2d$client$5d$__$28$ecmascript$29$__["formatDate"])(value) : 'Not recorded';
}
function AdmissionDetailHeader({ detail, horsePhotoUrl, simplifiedStatus = false }) {
    const candidate = detail.candidate;
    if (!candidate) return null;
    return /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$compiled$2f$react$2f$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$client$5d$__$28$ecmascript$29$__["jsxDEV"])("header", {
        className: "flex min-w-0 shrink-0 items-start gap-3 border-b border-[var(--color-border)] bg-[var(--color-surface)] p-4 sm:items-center sm:gap-4 sm:px-6 sm:py-5",
        children: [
            /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$compiled$2f$react$2f$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$client$5d$__$28$ecmascript$29$__["jsxDEV"])(__TURBOPACK__imported__module__$5b$project$5d2f$src$2f$components$2f$ui$2f$HorseAvatar$2e$tsx__$5b$app$2d$client$5d$__$28$ecmascript$29$__["HorseAvatar"], {
                name: candidate.name,
                image: horsePhotoUrl,
                size: 56,
                rounded: "md"
            }, void 0, false, {
                fileName: "[project]/src/features/admissions/shared/components/AdmissionDetailHeader.tsx",
                lineNumber: 39,
                columnNumber: 7
            }, this),
            /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$compiled$2f$react$2f$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$client$5d$__$28$ecmascript$29$__["jsxDEV"])("div", {
                className: "min-w-0 flex-1",
                children: [
                    /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$compiled$2f$react$2f$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$client$5d$__$28$ecmascript$29$__["jsxDEV"])("h1", {
                        className: "mb-2.5 flex flex-wrap items-center gap-2.5 text-xl font-semibold tracking-tight text-[var(--color-text-primary)]",
                        children: [
                            /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$compiled$2f$react$2f$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$client$5d$__$28$ecmascript$29$__["jsxDEV"])("span", {
                                className: "truncate",
                                children: candidate.name
                            }, void 0, false, {
                                fileName: "[project]/src/features/admissions/shared/components/AdmissionDetailHeader.tsx",
                                lineNumber: 42,
                                columnNumber: 11
                            }, this),
                            /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$compiled$2f$react$2f$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$client$5d$__$28$ecmascript$29$__["jsxDEV"])(__TURBOPACK__imported__module__$5b$project$5d2f$src$2f$features$2f$admissions$2f$shared$2f$components$2f$AdmissionStatusBadge$2e$tsx__$5b$app$2d$client$5d$__$28$ecmascript$29$__["AdmissionStatusBadge"], {
                                status: detail.status,
                                simplified: simplifiedStatus
                            }, void 0, false, {
                                fileName: "[project]/src/features/admissions/shared/components/AdmissionDetailHeader.tsx",
                                lineNumber: 43,
                                columnNumber: 11
                            }, this)
                        ]
                    }, void 0, true, {
                        fileName: "[project]/src/features/admissions/shared/components/AdmissionDetailHeader.tsx",
                        lineNumber: 41,
                        columnNumber: 9
                    }, this),
                    /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$compiled$2f$react$2f$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$client$5d$__$28$ecmascript$29$__["jsxDEV"])("div", {
                        className: "grid min-w-0 grid-cols-1 gap-x-5 gap-y-2 min-[480px]:grid-cols-2 lg:grid-cols-4",
                        children: [
                            /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$compiled$2f$react$2f$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$client$5d$__$28$ecmascript$29$__["jsxDEV"])(HeaderValue, {
                                label: "Admission ID",
                                value: `#${detail.admissionId}`
                            }, void 0, false, {
                                fileName: "[project]/src/features/admissions/shared/components/AdmissionDetailHeader.tsx",
                                lineNumber: 46,
                                columnNumber: 11
                            }, this),
                            /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$compiled$2f$react$2f$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$client$5d$__$28$ecmascript$29$__["jsxDEV"])(HeaderValue, {
                                label: "Owner",
                                value: detail.ownerName ? `${detail.ownerName}${detail.ownerId != null ? ` (#${detail.ownerId})` : ''}` : detail.ownerId != null ? `#${detail.ownerId}` : 'You'
                            }, void 0, false, {
                                fileName: "[project]/src/features/admissions/shared/components/AdmissionDetailHeader.tsx",
                                lineNumber: 47,
                                columnNumber: 11
                            }, this),
                            /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$compiled$2f$react$2f$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$client$5d$__$28$ecmascript$29$__["jsxDEV"])(HeaderValue, {
                                label: "Breed",
                                value: candidate.breed || 'Not provided'
                            }, void 0, false, {
                                fileName: "[project]/src/features/admissions/shared/components/AdmissionDetailHeader.tsx",
                                lineNumber: 48,
                                columnNumber: 11
                            }, this),
                            /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$compiled$2f$react$2f$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$client$5d$__$28$ecmascript$29$__["jsxDEV"])(HeaderValue, {
                                label: "Submitted",
                                value: date(detail.submittedAt)
                            }, void 0, false, {
                                fileName: "[project]/src/features/admissions/shared/components/AdmissionDetailHeader.tsx",
                                lineNumber: 49,
                                columnNumber: 11
                            }, this)
                        ]
                    }, void 0, true, {
                        fileName: "[project]/src/features/admissions/shared/components/AdmissionDetailHeader.tsx",
                        lineNumber: 45,
                        columnNumber: 9
                    }, this)
                ]
            }, void 0, true, {
                fileName: "[project]/src/features/admissions/shared/components/AdmissionDetailHeader.tsx",
                lineNumber: 40,
                columnNumber: 7
            }, this)
        ]
    }, void 0, true, {
        fileName: "[project]/src/features/admissions/shared/components/AdmissionDetailHeader.tsx",
        lineNumber: 38,
        columnNumber: 5
    }, this);
}
_c1 = AdmissionDetailHeader;
var _c, _c1;
__turbopack_context__.k.register(_c, "HeaderValue");
__turbopack_context__.k.register(_c1, "AdmissionDetailHeader");
if (typeof globalThis.$RefreshHelpers$ === 'object' && globalThis.$RefreshHelpers !== null) {
    __turbopack_context__.k.registerExports(__turbopack_context__.m, globalThis.$RefreshHelpers$);
}
}),
"[project]/src/features/admissions/shared/components/AdmissionDetailLayout.tsx [app-client] (ecmascript)", ((__turbopack_context__) => {
"use strict";

__turbopack_context__.s([
    "AdmissionDetailLayout",
    ()=>AdmissionDetailLayout,
    "admissionAsideClassName",
    ()=>admissionAsideClassName,
    "admissionDetailGridClassName",
    ()=>admissionDetailGridClassName
]);
var __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$compiled$2f$react$2f$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$client$5d$__$28$ecmascript$29$__ = __turbopack_context__.i("[project]/node_modules/next/dist/compiled/react/jsx-dev-runtime.js [app-client] (ecmascript)");
var __TURBOPACK__imported__module__$5b$project$5d2f$src$2f$components$2f$ui$2f$index$2e$ts__$5b$app$2d$client$5d$__$28$ecmascript$29$__$3c$locals$3e$__ = __turbopack_context__.i("[project]/src/components/ui/index.ts [app-client] (ecmascript) <locals>");
var __TURBOPACK__imported__module__$5b$project$5d2f$src$2f$components$2f$ui$2f$Button$2e$tsx__$5b$app$2d$client$5d$__$28$ecmascript$29$__ = __turbopack_context__.i("[project]/src/components/ui/Button.tsx [app-client] (ecmascript)");
var __TURBOPACK__imported__module__$5b$project$5d2f$src$2f$lib$2f$cn$2e$ts__$5b$app$2d$client$5d$__$28$ecmascript$29$__ = __turbopack_context__.i("[project]/src/lib/cn.ts [app-client] (ecmascript)");
;
;
;
const admissionDetailGridClassName = 'grid min-w-0 gap-5 lg:grid-cols-[minmax(0,1fr)_minmax(17rem,22rem)]';
const admissionAsideClassName = 'min-w-0 space-y-5 lg:sticky lg:top-[var(--sticky-top)] lg:self-start';
function AdmissionDetailLayout({ returnTo, onBack, header, pipeline, sections, content, sidebar, actions, asideHidden = false }) {
    const aside = asideHidden ? null : sidebar ?? actions;
    return /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$compiled$2f$react$2f$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$client$5d$__$28$ecmascript$29$__["jsxDEV"])("div", {
        className: "space-y-4",
        children: [
            onBack ? /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$compiled$2f$react$2f$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$client$5d$__$28$ecmascript$29$__["jsxDEV"])(__TURBOPACK__imported__module__$5b$project$5d2f$src$2f$components$2f$ui$2f$Button$2e$tsx__$5b$app$2d$client$5d$__$28$ecmascript$29$__["Button"], {
                variant: "tertiary",
                size: "sm",
                icon: "arrow-left",
                onClick: onBack,
                className: "-ml-2.5",
                children: "Back to admissions"
            }, void 0, false, {
                fileName: "[project]/src/features/admissions/shared/components/AdmissionDetailLayout.tsx",
                lineNumber: 39,
                columnNumber: 9
            }, this) : /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$compiled$2f$react$2f$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$client$5d$__$28$ecmascript$29$__["jsxDEV"])(__TURBOPACK__imported__module__$5b$project$5d2f$src$2f$components$2f$ui$2f$Button$2e$tsx__$5b$app$2d$client$5d$__$28$ecmascript$29$__["LinkButton"], {
                variant: "tertiary",
                size: "sm",
                icon: "arrow-left",
                href: returnTo ?? '/',
                className: "-ml-2.5",
                children: "Back to admissions"
            }, void 0, false, {
                fileName: "[project]/src/features/admissions/shared/components/AdmissionDetailLayout.tsx",
                lineNumber: 43,
                columnNumber: 9
            }, this),
            /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$compiled$2f$react$2f$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$client$5d$__$28$ecmascript$29$__["jsxDEV"])("div", {
                className: "min-w-0 rounded-[var(--radius-lg)] border border-[var(--color-border)] shadow-[var(--shadow-panel)]",
                children: [
                    /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$compiled$2f$react$2f$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$client$5d$__$28$ecmascript$29$__["jsxDEV"])("div", {
                        className: "overflow-hidden rounded-t-[var(--radius-lg)]",
                        children: header
                    }, void 0, false, {
                        fileName: "[project]/src/features/admissions/shared/components/AdmissionDetailLayout.tsx",
                        lineNumber: 50,
                        columnNumber: 9
                    }, this),
                    /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$compiled$2f$react$2f$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$client$5d$__$28$ecmascript$29$__["jsxDEV"])("div", {
                        className: "space-y-5 rounded-b-[var(--radius-lg)] bg-[var(--color-surface-subtle)] p-4 sm:p-6",
                        children: [
                            pipeline,
                            /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$compiled$2f$react$2f$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$client$5d$__$28$ecmascript$29$__["jsxDEV"])("div", {
                                className: aside ? admissionDetailGridClassName : 'grid min-w-0 grid-cols-1 gap-5',
                                children: [
                                    /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$compiled$2f$react$2f$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$client$5d$__$28$ecmascript$29$__["jsxDEV"])("main", {
                                        className: "min-w-0 space-y-5",
                                        children: content ?? /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$compiled$2f$react$2f$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$client$5d$__$28$ecmascript$29$__["jsxDEV"])("div", {
                                            className: "grid min-w-0 grid-cols-1 gap-4 xl:grid-cols-2",
                                            children: sections?.map((section, idx)=>/*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$compiled$2f$react$2f$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$client$5d$__$28$ecmascript$29$__["jsxDEV"])("div", {
                                                    className: "min-w-0",
                                                    children: section
                                                }, idx, false, {
                                                    fileName: "[project]/src/features/admissions/shared/components/AdmissionDetailLayout.tsx",
                                                    lineNumber: 60,
                                                    columnNumber: 21
                                                }, this))
                                        }, void 0, false, {
                                            fileName: "[project]/src/features/admissions/shared/components/AdmissionDetailLayout.tsx",
                                            lineNumber: 58,
                                            columnNumber: 17
                                        }, this)
                                    }, void 0, false, {
                                        fileName: "[project]/src/features/admissions/shared/components/AdmissionDetailLayout.tsx",
                                        lineNumber: 56,
                                        columnNumber: 13
                                    }, this),
                                    aside && /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$compiled$2f$react$2f$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$client$5d$__$28$ecmascript$29$__["jsxDEV"])("aside", {
                                        className: (0, __TURBOPACK__imported__module__$5b$project$5d2f$src$2f$lib$2f$cn$2e$ts__$5b$app$2d$client$5d$__$28$ecmascript$29$__["cn"])(admissionAsideClassName),
                                        children: aside
                                    }, void 0, false, {
                                        fileName: "[project]/src/features/admissions/shared/components/AdmissionDetailLayout.tsx",
                                        lineNumber: 65,
                                        columnNumber: 23
                                    }, this)
                                ]
                            }, void 0, true, {
                                fileName: "[project]/src/features/admissions/shared/components/AdmissionDetailLayout.tsx",
                                lineNumber: 55,
                                columnNumber: 11
                            }, this)
                        ]
                    }, void 0, true, {
                        fileName: "[project]/src/features/admissions/shared/components/AdmissionDetailLayout.tsx",
                        lineNumber: 52,
                        columnNumber: 9
                    }, this)
                ]
            }, void 0, true, {
                fileName: "[project]/src/features/admissions/shared/components/AdmissionDetailLayout.tsx",
                lineNumber: 49,
                columnNumber: 7
            }, this)
        ]
    }, void 0, true, {
        fileName: "[project]/src/features/admissions/shared/components/AdmissionDetailLayout.tsx",
        lineNumber: 37,
        columnNumber: 5
    }, this);
}
_c = AdmissionDetailLayout;
var _c;
__turbopack_context__.k.register(_c, "AdmissionDetailLayout");
if (typeof globalThis.$RefreshHelpers$ === 'object' && globalThis.$RefreshHelpers !== null) {
    __turbopack_context__.k.registerExports(__turbopack_context__.m, globalThis.$RefreshHelpers$);
}
}),
"[project]/src/features/admissions/shared/components/AdmissionInfoSection.tsx [app-client] (ecmascript)", ((__turbopack_context__) => {
"use strict";

__turbopack_context__.s([
    "AdmissionInfoSection",
    ()=>AdmissionInfoSection,
    "AdmissionSideCard",
    ()=>AdmissionSideCard,
    "InfoGroupTitle",
    ()=>InfoGroupTitle,
    "InfoRow",
    ()=>InfoRow
]);
var __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$compiled$2f$react$2f$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$client$5d$__$28$ecmascript$29$__ = __turbopack_context__.i("[project]/node_modules/next/dist/compiled/react/jsx-dev-runtime.js [app-client] (ecmascript)");
var __TURBOPACK__imported__module__$5b$project$5d2f$src$2f$components$2f$ui$2f$Panel$2e$tsx__$5b$app$2d$client$5d$__$28$ecmascript$29$__ = __turbopack_context__.i("[project]/src/components/ui/Panel.tsx [app-client] (ecmascript)");
var __TURBOPACK__imported__module__$5b$project$5d2f$src$2f$lib$2f$cn$2e$ts__$5b$app$2d$client$5d$__$28$ecmascript$29$__ = __turbopack_context__.i("[project]/src/lib/cn.ts [app-client] (ecmascript)");
;
;
;
function InfoRow({ label, value }) {
    return /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$compiled$2f$react$2f$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$client$5d$__$28$ecmascript$29$__["jsxDEV"])("div", {
        className: "flex items-start justify-between gap-4",
        children: [
            /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$compiled$2f$react$2f$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$client$5d$__$28$ecmascript$29$__["jsxDEV"])("span", {
                className: "shrink-0 text-[var(--color-text-muted)]",
                children: label
            }, void 0, false, {
                fileName: "[project]/src/features/admissions/shared/components/AdmissionInfoSection.tsx",
                lineNumber: 8,
                columnNumber: 7
            }, this),
            /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$compiled$2f$react$2f$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$client$5d$__$28$ecmascript$29$__["jsxDEV"])("span", {
                className: "min-w-0 break-words text-right font-medium text-[var(--color-text-primary)]",
                children: value || /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$compiled$2f$react$2f$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$client$5d$__$28$ecmascript$29$__["jsxDEV"])("span", {
                    className: "font-normal italic text-[var(--color-text-muted)]",
                    children: "N/A"
                }, void 0, false, {
                    fileName: "[project]/src/features/admissions/shared/components/AdmissionInfoSection.tsx",
                    lineNumber: 10,
                    columnNumber: 19
                }, this)
            }, void 0, false, {
                fileName: "[project]/src/features/admissions/shared/components/AdmissionInfoSection.tsx",
                lineNumber: 9,
                columnNumber: 7
            }, this)
        ]
    }, void 0, true, {
        fileName: "[project]/src/features/admissions/shared/components/AdmissionInfoSection.tsx",
        lineNumber: 7,
        columnNumber: 5
    }, this);
}
_c = InfoRow;
function AdmissionInfoSection({ title, aside, children }) {
    return /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$compiled$2f$react$2f$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$client$5d$__$28$ecmascript$29$__["jsxDEV"])(__TURBOPACK__imported__module__$5b$project$5d2f$src$2f$components$2f$ui$2f$Panel$2e$tsx__$5b$app$2d$client$5d$__$28$ecmascript$29$__["Panel"], {
        padded: true,
        children: [
            /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$compiled$2f$react$2f$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$client$5d$__$28$ecmascript$29$__["jsxDEV"])("div", {
                className: "flex items-center justify-between gap-3",
                children: [
                    /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$compiled$2f$react$2f$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$client$5d$__$28$ecmascript$29$__["jsxDEV"])(__TURBOPACK__imported__module__$5b$project$5d2f$src$2f$components$2f$ui$2f$Panel$2e$tsx__$5b$app$2d$client$5d$__$28$ecmascript$29$__["SectionTitle"], {
                        children: title
                    }, void 0, false, {
                        fileName: "[project]/src/features/admissions/shared/components/AdmissionInfoSection.tsx",
                        lineNumber: 30,
                        columnNumber: 9
                    }, this),
                    aside
                ]
            }, void 0, true, {
                fileName: "[project]/src/features/admissions/shared/components/AdmissionInfoSection.tsx",
                lineNumber: 29,
                columnNumber: 7
            }, this),
            /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$compiled$2f$react$2f$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$client$5d$__$28$ecmascript$29$__["jsxDEV"])("div", {
                className: "mt-4 space-y-3 text-sm",
                children: children
            }, void 0, false, {
                fileName: "[project]/src/features/admissions/shared/components/AdmissionInfoSection.tsx",
                lineNumber: 33,
                columnNumber: 7
            }, this)
        ]
    }, void 0, true, {
        fileName: "[project]/src/features/admissions/shared/components/AdmissionInfoSection.tsx",
        lineNumber: 28,
        columnNumber: 5
    }, this);
}
_c1 = AdmissionInfoSection;
function AdmissionSideCard({ title, tone = 'default', children, className }) {
    return /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$compiled$2f$react$2f$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$client$5d$__$28$ecmascript$29$__["jsxDEV"])(__TURBOPACK__imported__module__$5b$project$5d2f$src$2f$components$2f$ui$2f$Panel$2e$tsx__$5b$app$2d$client$5d$__$28$ecmascript$29$__["Panel"], {
        padded: true,
        tone: tone,
        children: [
            /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$compiled$2f$react$2f$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$client$5d$__$28$ecmascript$29$__["jsxDEV"])(__TURBOPACK__imported__module__$5b$project$5d2f$src$2f$components$2f$ui$2f$Panel$2e$tsx__$5b$app$2d$client$5d$__$28$ecmascript$29$__["SectionTitle"], {
                children: title
            }, void 0, false, {
                fileName: "[project]/src/features/admissions/shared/components/AdmissionInfoSection.tsx",
                lineNumber: 52,
                columnNumber: 7
            }, this),
            /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$compiled$2f$react$2f$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$client$5d$__$28$ecmascript$29$__["jsxDEV"])("div", {
                className: (0, __TURBOPACK__imported__module__$5b$project$5d2f$src$2f$lib$2f$cn$2e$ts__$5b$app$2d$client$5d$__$28$ecmascript$29$__["cn"])('mt-4 space-y-3 text-sm', className),
                children: children
            }, void 0, false, {
                fileName: "[project]/src/features/admissions/shared/components/AdmissionInfoSection.tsx",
                lineNumber: 53,
                columnNumber: 7
            }, this)
        ]
    }, void 0, true, {
        fileName: "[project]/src/features/admissions/shared/components/AdmissionInfoSection.tsx",
        lineNumber: 51,
        columnNumber: 5
    }, this);
}
_c2 = AdmissionSideCard;
function InfoGroupTitle({ children }) {
    return /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$compiled$2f$react$2f$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$client$5d$__$28$ecmascript$29$__["jsxDEV"])("h3", {
        className: "text-xs font-semibold uppercase tracking-wide text-[var(--color-text-secondary)]",
        children: children
    }, void 0, false, {
        fileName: "[project]/src/features/admissions/shared/components/AdmissionInfoSection.tsx",
        lineNumber: 61,
        columnNumber: 5
    }, this);
}
_c3 = InfoGroupTitle;
var _c, _c1, _c2, _c3;
__turbopack_context__.k.register(_c, "InfoRow");
__turbopack_context__.k.register(_c1, "AdmissionInfoSection");
__turbopack_context__.k.register(_c2, "AdmissionSideCard");
__turbopack_context__.k.register(_c3, "InfoGroupTitle");
if (typeof globalThis.$RefreshHelpers$ === 'object' && globalThis.$RefreshHelpers !== null) {
    __turbopack_context__.k.registerExports(__turbopack_context__.m, globalThis.$RefreshHelpers$);
}
}),
"[project]/src/features/admissions/shared/components/AdmissionListLayout.tsx [app-client] (ecmascript)", ((__turbopack_context__) => {
"use strict";

__turbopack_context__.s([
    "AdmissionListLayout",
    ()=>AdmissionListLayout
]);
var __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$compiled$2f$react$2f$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$client$5d$__$28$ecmascript$29$__ = __turbopack_context__.i("[project]/node_modules/next/dist/compiled/react/jsx-dev-runtime.js [app-client] (ecmascript)");
var __TURBOPACK__imported__module__$5b$project$5d2f$src$2f$components$2f$ui$2f$ScreenLayout$2e$tsx__$5b$app$2d$client$5d$__$28$ecmascript$29$__ = __turbopack_context__.i("[project]/src/components/ui/ScreenLayout.tsx [app-client] (ecmascript)");
var __TURBOPACK__imported__module__$5b$project$5d2f$src$2f$components$2f$ui$2f$PageHeader$2e$tsx__$5b$app$2d$client$5d$__$28$ecmascript$29$__ = __turbopack_context__.i("[project]/src/components/ui/PageHeader.tsx [app-client] (ecmascript)");
;
;
;
function AdmissionListLayout({ title = 'Admissions', description, actions, children }) {
    return /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$compiled$2f$react$2f$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$client$5d$__$28$ecmascript$29$__["jsxDEV"])(__TURBOPACK__imported__module__$5b$project$5d2f$src$2f$components$2f$ui$2f$ScreenLayout$2e$tsx__$5b$app$2d$client$5d$__$28$ecmascript$29$__["ScreenLayout"], {
        variant: "list",
        children: [
            /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$compiled$2f$react$2f$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$client$5d$__$28$ecmascript$29$__["jsxDEV"])(__TURBOPACK__imported__module__$5b$project$5d2f$src$2f$components$2f$ui$2f$PageHeader$2e$tsx__$5b$app$2d$client$5d$__$28$ecmascript$29$__["PageHeader"], {
                title: title,
                description: description,
                actions: actions
            }, void 0, false, {
                fileName: "[project]/src/features/admissions/shared/components/AdmissionListLayout.tsx",
                lineNumber: 18,
                columnNumber: 7
            }, this),
            children
        ]
    }, void 0, true, {
        fileName: "[project]/src/features/admissions/shared/components/AdmissionListLayout.tsx",
        lineNumber: 17,
        columnNumber: 5
    }, this);
}
_c = AdmissionListLayout;
var _c;
__turbopack_context__.k.register(_c, "AdmissionListLayout");
if (typeof globalThis.$RefreshHelpers$ === 'object' && globalThis.$RefreshHelpers !== null) {
    __turbopack_context__.k.registerExports(__turbopack_context__.m, globalThis.$RefreshHelpers$);
}
}),
"[project]/src/features/admissions/shared/components/AdmissionPipeline.tsx [app-client] (ecmascript)", ((__turbopack_context__) => {
"use strict";

__turbopack_context__.s([
    "AdmissionPipeline",
    ()=>AdmissionPipeline
]);
var __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$compiled$2f$react$2f$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$client$5d$__$28$ecmascript$29$__ = __turbopack_context__.i("[project]/node_modules/next/dist/compiled/react/jsx-dev-runtime.js [app-client] (ecmascript)");
var __TURBOPACK__imported__module__$5b$project$5d2f$src$2f$components$2f$ui$2f$Panel$2e$tsx__$5b$app$2d$client$5d$__$28$ecmascript$29$__ = __turbopack_context__.i("[project]/src/components/ui/Panel.tsx [app-client] (ecmascript)");
var __TURBOPACK__imported__module__$5b$project$5d2f$src$2f$lib$2f$display$2e$ts__$5b$app$2d$client$5d$__$28$ecmascript$29$__ = __turbopack_context__.i("[project]/src/lib/display.ts [app-client] (ecmascript)");
;
;
;
function PipelineStep({ label, isDone, isActive, note }) {
    if (isActive) return /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$compiled$2f$react$2f$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$client$5d$__$28$ecmascript$29$__["jsxDEV"])("div", {
        className: "rounded-[var(--radius-sm)] border border-[var(--color-primary)] bg-[var(--color-primary-soft)] px-3 py-2 font-medium text-[var(--color-primary)]",
        children: [
            /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$compiled$2f$react$2f$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$client$5d$__$28$ecmascript$29$__["jsxDEV"])("span", {
                className: "mr-1.5",
                children: "●"
            }, void 0, false, {
                fileName: "[project]/src/features/admissions/shared/components/AdmissionPipeline.tsx",
                lineNumber: 8,
                columnNumber: 7
            }, this),
            label,
            note && /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$compiled$2f$react$2f$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$client$5d$__$28$ecmascript$29$__["jsxDEV"])("span", {
                className: "ml-1.5 text-xs font-normal opacity-75",
                children: [
                    "(",
                    note,
                    ")"
                ]
            }, void 0, true, {
                fileName: "[project]/src/features/admissions/shared/components/AdmissionPipeline.tsx",
                lineNumber: 9,
                columnNumber: 16
            }, this)
        ]
    }, void 0, true, {
        fileName: "[project]/src/features/admissions/shared/components/AdmissionPipeline.tsx",
        lineNumber: 7,
        columnNumber: 5
    }, this);
    if (isDone) return /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$compiled$2f$react$2f$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$client$5d$__$28$ecmascript$29$__["jsxDEV"])("div", {
        className: "rounded-[var(--radius-sm)] border border-[var(--color-success)] bg-[var(--color-success-soft)] px-3 py-2 text-[var(--color-success)]",
        children: [
            /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$compiled$2f$react$2f$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$client$5d$__$28$ecmascript$29$__["jsxDEV"])("span", {
                className: "mr-1.5",
                children: "✓"
            }, void 0, false, {
                fileName: "[project]/src/features/admissions/shared/components/AdmissionPipeline.tsx",
                lineNumber: 12,
                columnNumber: 172
            }, this),
            label
        ]
    }, void 0, true, {
        fileName: "[project]/src/features/admissions/shared/components/AdmissionPipeline.tsx",
        lineNumber: 12,
        columnNumber: 22
    }, this);
    return /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$compiled$2f$react$2f$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$client$5d$__$28$ecmascript$29$__["jsxDEV"])("div", {
        className: "rounded-[var(--radius-sm)] border border-[var(--color-border)] bg-[var(--color-surface-subtle)] px-3 py-2 text-[var(--color-text-muted)]",
        children: [
            /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$compiled$2f$react$2f$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$client$5d$__$28$ecmascript$29$__["jsxDEV"])("span", {
                className: "mr-1.5 opacity-40",
                children: "-"
            }, void 0, false, {
                fileName: "[project]/src/features/admissions/shared/components/AdmissionPipeline.tsx",
                lineNumber: 13,
                columnNumber: 164
            }, this),
            label
        ]
    }, void 0, true, {
        fileName: "[project]/src/features/admissions/shared/components/AdmissionPipeline.tsx",
        lineNumber: 13,
        columnNumber: 10
    }, this);
}
_c = PipelineStep;
function AdmissionPipeline({ detail }) {
    const isRejected = detail.status === 'REJECTED';
    const isApproved = detail.status === 'APPROVED';
    // Owner responses omit timestamps. Infer only transitions proven by the current status.
    const stages = [
        'GROOM_REVIEW',
        'WAITING_FOR_STALL',
        'WAITING_FOR_ARRIVAL',
        'ARRIVAL_EXPIRED',
        'VET_REVIEW',
        'TRAINER_REVIEW',
        'MANAGER_REVIEW',
        'APPROVED'
    ];
    const stage = stages.indexOf(detail.status);
    const done = (value, threshold)=>value === undefined ? stage >= threshold : Boolean(value);
    return /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$compiled$2f$react$2f$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$client$5d$__$28$ecmascript$29$__["jsxDEV"])(__TURBOPACK__imported__module__$5b$project$5d2f$src$2f$components$2f$ui$2f$Panel$2e$tsx__$5b$app$2d$client$5d$__$28$ecmascript$29$__["Panel"], {
        padded: true,
        children: [
            /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$compiled$2f$react$2f$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$client$5d$__$28$ecmascript$29$__["jsxDEV"])(__TURBOPACK__imported__module__$5b$project$5d2f$src$2f$components$2f$ui$2f$Panel$2e$tsx__$5b$app$2d$client$5d$__$28$ecmascript$29$__["SectionTitle"], {
                children: "Review pipeline"
            }, void 0, false, {
                fileName: "[project]/src/features/admissions/shared/components/AdmissionPipeline.tsx",
                lineNumber: 28,
                columnNumber: 7
            }, this),
            isRejected && /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$compiled$2f$react$2f$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$client$5d$__$28$ecmascript$29$__["jsxDEV"])("p", {
                role: "status",
                className: "mt-3 rounded-[var(--radius-sm)] border border-[var(--color-danger)] bg-[var(--color-danger-soft)] px-3 py-2 text-sm font-medium text-[var(--color-danger)]",
                children: "This application has been rejected."
            }, void 0, false, {
                fileName: "[project]/src/features/admissions/shared/components/AdmissionPipeline.tsx",
                lineNumber: 30,
                columnNumber: 9
            }, this),
            isApproved && /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$compiled$2f$react$2f$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$client$5d$__$28$ecmascript$29$__["jsxDEV"])("p", {
                role: "status",
                className: "mt-3 rounded-[var(--radius-sm)] border border-[var(--color-success)] bg-[var(--color-success-soft)] px-3 py-2 text-sm font-medium text-[var(--color-success)]",
                children: "This application has been approved."
            }, void 0, false, {
                fileName: "[project]/src/features/admissions/shared/components/AdmissionPipeline.tsx",
                lineNumber: 35,
                columnNumber: 9
            }, this),
            /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$compiled$2f$react$2f$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$client$5d$__$28$ecmascript$29$__["jsxDEV"])("div", {
                className: "mt-4 flex flex-wrap gap-2 text-xs",
                children: [
                    /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$compiled$2f$react$2f$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$client$5d$__$28$ecmascript$29$__["jsxDEV"])(PipelineStep, {
                        label: "Groom Review",
                        isDone: done(detail.groomReviewedAt, 1),
                        isActive: detail.status === 'GROOM_REVIEW'
                    }, void 0, false, {
                        fileName: "[project]/src/features/admissions/shared/components/AdmissionPipeline.tsx",
                        lineNumber: 40,
                        columnNumber: 9
                    }, this),
                    /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$compiled$2f$react$2f$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$client$5d$__$28$ecmascript$29$__["jsxDEV"])(PipelineStep, {
                        label: "Waiting for Stall",
                        isDone: done(detail.quarantineStallCode, 2),
                        isActive: detail.status === 'WAITING_FOR_STALL'
                    }, void 0, false, {
                        fileName: "[project]/src/features/admissions/shared/components/AdmissionPipeline.tsx",
                        lineNumber: 41,
                        columnNumber: 9
                    }, this),
                    /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$compiled$2f$react$2f$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$client$5d$__$28$ecmascript$29$__["jsxDEV"])(PipelineStep, {
                        label: "Waiting for Arrival",
                        isDone: done(detail.arrivedAt, 3),
                        isActive: detail.status === 'WAITING_FOR_ARRIVAL',
                        note: detail.arrivalDeadlineAt ? `by ${(0, __TURBOPACK__imported__module__$5b$project$5d2f$src$2f$lib$2f$display$2e$ts__$5b$app$2d$client$5d$__$28$ecmascript$29$__["formatDate"])(detail.arrivalDeadlineAt)}` : undefined
                    }, void 0, false, {
                        fileName: "[project]/src/features/admissions/shared/components/AdmissionPipeline.tsx",
                        lineNumber: 42,
                        columnNumber: 9
                    }, this),
                    /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$compiled$2f$react$2f$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$client$5d$__$28$ecmascript$29$__["jsxDEV"])(PipelineStep, {
                        label: "Arrival Expired",
                        isDone: detail.status === 'ARRIVAL_EXPIRED',
                        isActive: detail.status === 'ARRIVAL_EXPIRED'
                    }, void 0, false, {
                        fileName: "[project]/src/features/admissions/shared/components/AdmissionPipeline.tsx",
                        lineNumber: 43,
                        columnNumber: 9
                    }, this),
                    /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$compiled$2f$react$2f$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$client$5d$__$28$ecmascript$29$__["jsxDEV"])(PipelineStep, {
                        label: "Vet Review",
                        isDone: done(detail.vetReviewedAt, 5),
                        isActive: detail.status === 'VET_REVIEW'
                    }, void 0, false, {
                        fileName: "[project]/src/features/admissions/shared/components/AdmissionPipeline.tsx",
                        lineNumber: 44,
                        columnNumber: 9
                    }, this),
                    /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$compiled$2f$react$2f$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$client$5d$__$28$ecmascript$29$__["jsxDEV"])(PipelineStep, {
                        label: "Trainer Review",
                        isDone: done(detail.trainerReviewedAt, 6),
                        isActive: detail.status === 'TRAINER_REVIEW'
                    }, void 0, false, {
                        fileName: "[project]/src/features/admissions/shared/components/AdmissionPipeline.tsx",
                        lineNumber: 45,
                        columnNumber: 9
                    }, this),
                    /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$compiled$2f$react$2f$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$client$5d$__$28$ecmascript$29$__["jsxDEV"])(PipelineStep, {
                        label: "Manager Review",
                        isDone: done(detail.managerReviewedAt, 7),
                        isActive: detail.status === 'MANAGER_REVIEW'
                    }, void 0, false, {
                        fileName: "[project]/src/features/admissions/shared/components/AdmissionPipeline.tsx",
                        lineNumber: 46,
                        columnNumber: 9
                    }, this)
                ]
            }, void 0, true, {
                fileName: "[project]/src/features/admissions/shared/components/AdmissionPipeline.tsx",
                lineNumber: 39,
                columnNumber: 7
            }, this)
        ]
    }, void 0, true, {
        fileName: "[project]/src/features/admissions/shared/components/AdmissionPipeline.tsx",
        lineNumber: 27,
        columnNumber: 5
    }, this);
}
_c1 = AdmissionPipeline;
var _c, _c1;
__turbopack_context__.k.register(_c, "PipelineStep");
__turbopack_context__.k.register(_c1, "AdmissionPipeline");
if (typeof globalThis.$RefreshHelpers$ === 'object' && globalThis.$RefreshHelpers !== null) {
    __turbopack_context__.k.registerExports(__turbopack_context__.m, globalThis.$RefreshHelpers$);
}
}),
"[project]/src/features/admissions/shared/components/AdmissionStatusBadge.tsx [app-client] (ecmascript)", ((__turbopack_context__) => {
"use strict";

__turbopack_context__.s([
    "AdmissionStatusBadge",
    ()=>AdmissionStatusBadge,
    "prettyStatus",
    ()=>prettyStatus,
    "simpleStatusLabel",
    ()=>simpleStatusLabel,
    "simplifyStatus",
    ()=>simplifyStatus,
    "statusTone",
    ()=>statusTone
]);
var __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$compiled$2f$react$2f$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$client$5d$__$28$ecmascript$29$__ = __turbopack_context__.i("[project]/node_modules/next/dist/compiled/react/jsx-dev-runtime.js [app-client] (ecmascript)");
var __TURBOPACK__imported__module__$5b$project$5d2f$src$2f$components$2f$ui$2f$StatusBadge$2e$tsx__$5b$app$2d$client$5d$__$28$ecmascript$29$__ = __turbopack_context__.i("[project]/src/components/ui/StatusBadge.tsx [app-client] (ecmascript)");
;
;
function prettyStatus(status) {
    return status.toLowerCase().replaceAll('_', ' ').replace(/\b\w/g, (letter)=>letter.toUpperCase());
}
function statusTone(status) {
    if (status === 'GROOM_REVIEW') return 'primary';
    if (status === 'WAITING_FOR_STALL' || status === 'WAITING_FOR_ARRIVAL' || status === 'ARRIVAL_EXPIRED') return 'warning';
    if (status === 'APPROVED') return 'success';
    if (status === 'REJECTED') return 'danger';
    if (status === 'VET_REVIEW' || status === 'TRAINER_REVIEW' || status === 'MANAGER_REVIEW') return 'info';
    return 'neutral';
}
function simplifyStatus(status) {
    if (status === 'APPROVED') return 'APPROVED';
    if (status === 'REJECTED') return 'REJECTED';
    return 'IN_PROGRESS';
}
const SIMPLE_LABEL = {
    IN_PROGRESS: 'In Progress',
    APPROVED: 'Approved',
    REJECTED: 'Rejected'
};
const SIMPLE_TONE = {
    IN_PROGRESS: 'info',
    APPROVED: 'success',
    REJECTED: 'danger'
};
function simpleStatusLabel(status) {
    return SIMPLE_LABEL[simplifyStatus(status)];
}
function AdmissionStatusBadge({ status, size = 'md', simplified = false }) {
    const tone = simplified ? SIMPLE_TONE[simplifyStatus(status)] : statusTone(status);
    const label = simplified ? simpleStatusLabel(status) : prettyStatus(status);
    return /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$compiled$2f$react$2f$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$client$5d$__$28$ecmascript$29$__["jsxDEV"])(__TURBOPACK__imported__module__$5b$project$5d2f$src$2f$components$2f$ui$2f$StatusBadge$2e$tsx__$5b$app$2d$client$5d$__$28$ecmascript$29$__["Pill"], {
        tone: tone,
        size: size,
        children: label
    }, void 0, false, {
        fileName: "[project]/src/features/admissions/shared/components/AdmissionStatusBadge.tsx",
        lineNumber: 65,
        columnNumber: 10
    }, this);
}
_c = AdmissionStatusBadge;
var _c;
__turbopack_context__.k.register(_c, "AdmissionStatusBadge");
if (typeof globalThis.$RefreshHelpers$ === 'object' && globalThis.$RefreshHelpers !== null) {
    __turbopack_context__.k.registerExports(__turbopack_context__.m, globalThis.$RefreshHelpers$);
}
}),
]);

//# sourceMappingURL=src_1s5_1qu._.js.map