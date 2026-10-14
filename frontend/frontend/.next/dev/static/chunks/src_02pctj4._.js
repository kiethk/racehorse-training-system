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
"[project]/src/features/admissions/components/GroomAdmissionsTable.tsx [app-client] (ecmascript)", ((__turbopack_context__) => {
"use strict";

__turbopack_context__.s([
    "GroomAdmissionsTable",
    ()=>GroomAdmissionsTable
]);
var __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$compiled$2f$react$2f$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$client$5d$__$28$ecmascript$29$__ = __turbopack_context__.i("[project]/node_modules/next/dist/compiled/react/jsx-dev-runtime.js [app-client] (ecmascript)");
var __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$compiled$2f$react$2f$index$2e$js__$5b$app$2d$client$5d$__$28$ecmascript$29$__ = __turbopack_context__.i("[project]/node_modules/next/dist/compiled/react/index.js [app-client] (ecmascript)");
var __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$navigation$2e$js__$5b$app$2d$client$5d$__$28$ecmascript$29$__ = __turbopack_context__.i("[project]/node_modules/next/navigation.js [app-client] (ecmascript)");
var __TURBOPACK__imported__module__$5b$project$5d2f$src$2f$components$2f$ui$2f$Button$2e$tsx__$5b$app$2d$client$5d$__$28$ecmascript$29$__ = __turbopack_context__.i("[project]/src/components/ui/Button.tsx [app-client] (ecmascript)");
var __TURBOPACK__imported__module__$5b$project$5d2f$src$2f$components$2f$ui$2f$states$2e$tsx__$5b$app$2d$client$5d$__$28$ecmascript$29$__ = __turbopack_context__.i("[project]/src/components/ui/states.tsx [app-client] (ecmascript)");
var __TURBOPACK__imported__module__$5b$project$5d2f$src$2f$components$2f$ui$2f$Notice$2e$tsx__$5b$app$2d$client$5d$__$28$ecmascript$29$__ = __turbopack_context__.i("[project]/src/components/ui/Notice.tsx [app-client] (ecmascript)");
var __TURBOPACK__imported__module__$5b$project$5d2f$src$2f$features$2f$admissions$2f$services$2f$api$2e$ts__$5b$app$2d$client$5d$__$28$ecmascript$29$__ = __turbopack_context__.i("[project]/src/features/admissions/services/api.ts [app-client] (ecmascript)");
var __TURBOPACK__imported__module__$5b$project$5d2f$src$2f$features$2f$admissions$2f$shared$2f$components$2f$AdmissionListLayout$2e$tsx__$5b$app$2d$client$5d$__$28$ecmascript$29$__ = __turbopack_context__.i("[project]/src/features/admissions/shared/components/AdmissionListLayout.tsx [app-client] (ecmascript)");
var __TURBOPACK__imported__module__$5b$project$5d2f$src$2f$features$2f$admissions$2f$shared$2f$components$2f$AdmissionFilterBar$2e$tsx__$5b$app$2d$client$5d$__$28$ecmascript$29$__ = __turbopack_context__.i("[project]/src/features/admissions/shared/components/AdmissionFilterBar.tsx [app-client] (ecmascript)");
var __TURBOPACK__imported__module__$5b$project$5d2f$src$2f$features$2f$admissions$2f$shared$2f$components$2f$AdmissionTable$2e$tsx__$5b$app$2d$client$5d$__$28$ecmascript$29$__ = __turbopack_context__.i("[project]/src/features/admissions/shared/components/AdmissionTable.tsx [app-client] (ecmascript)");
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
const statuses = [
    {
        value: '',
        label: 'All statuses'
    },
    {
        value: 'GROOM_REVIEW',
        label: 'Groom review'
    },
    {
        value: 'WAITING_FOR_STALL',
        label: 'Waiting for stall'
    },
    {
        value: 'WAITING_FOR_ARRIVAL',
        label: 'Waiting for arrival'
    },
    {
        value: 'ARRIVAL_EXPIRED',
        label: 'Arrival expired'
    },
    {
        value: 'VET_REVIEW',
        label: 'Vet review'
    },
    {
        value: 'TRAINER_REVIEW',
        label: 'Trainer review'
    },
    {
        value: 'MANAGER_REVIEW',
        label: 'Manager review'
    },
    {
        value: 'APPROVED',
        label: 'Approved'
    },
    {
        value: 'REJECTED',
        label: 'Rejected'
    }
];
const emptyFilters = {
    candidateName: '',
    status: '',
    submittedFrom: '',
    submittedTo: '',
    page: 0
};
function toQuery(filters) {
    const params = new URLSearchParams();
    if (filters.candidateName) params.set('candidateName', filters.candidateName);
    if (filters.status) params.set('status', filters.status);
    if (filters.submittedFrom) params.set('submittedFrom', filters.submittedFrom);
    if (filters.submittedTo) params.set('submittedTo', filters.submittedTo);
    if (filters.page > 0) params.set('page', String(filters.page));
    const query = params.toString();
    return query ? `?${query}` : '';
}
function GroomAdmissionsTable({ initialFilters }) {
    _s();
    const router = (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$navigation$2e$js__$5b$app$2d$client$5d$__$28$ecmascript$29$__["useRouter"])();
    const [draft, setDraft] = (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$compiled$2f$react$2f$index$2e$js__$5b$app$2d$client$5d$__$28$ecmascript$29$__["useState"])(initialFilters);
    const [applied, setApplied] = (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$compiled$2f$react$2f$index$2e$js__$5b$app$2d$client$5d$__$28$ecmascript$29$__["useState"])(initialFilters);
    const [result, setResult] = (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$compiled$2f$react$2f$index$2e$js__$5b$app$2d$client$5d$__$28$ecmascript$29$__["useState"])(null);
    const [loading, setLoading] = (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$compiled$2f$react$2f$index$2e$js__$5b$app$2d$client$5d$__$28$ecmascript$29$__["useState"])(true);
    const [error, setError] = (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$compiled$2f$react$2f$index$2e$js__$5b$app$2d$client$5d$__$28$ecmascript$29$__["useState"])(null);
    const [reloadKey, setReloadKey] = (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$compiled$2f$react$2f$index$2e$js__$5b$app$2d$client$5d$__$28$ecmascript$29$__["useState"])(0);
    (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$compiled$2f$react$2f$index$2e$js__$5b$app$2d$client$5d$__$28$ecmascript$29$__["useEffect"])({
        "GroomAdmissionsTable.useEffect": ()=>{
            let active = true;
            // eslint-disable-next-line react-hooks/set-state-in-effect
            setLoading(true);
            setError(null);
            __TURBOPACK__imported__module__$5b$project$5d2f$src$2f$features$2f$admissions$2f$services$2f$api$2e$ts__$5b$app$2d$client$5d$__$28$ecmascript$29$__["admissionsApi"].getGroomQueue(applied).then({
                "GroomAdmissionsTable.useEffect": (data)=>{
                    if (active) setResult(data);
                }
            }["GroomAdmissionsTable.useEffect"]).catch({
                "GroomAdmissionsTable.useEffect": (cause)=>{
                    if (active) setError(cause instanceof Error ? cause.message : 'Unable to load admissions.');
                }
            }["GroomAdmissionsTable.useEffect"]).finally({
                "GroomAdmissionsTable.useEffect": ()=>{
                    if (active) setLoading(false);
                }
            }["GroomAdmissionsTable.useEffect"]);
            return ({
                "GroomAdmissionsTable.useEffect": ()=>{
                    active = false;
                }
            })["GroomAdmissionsTable.useEffect"];
        }
    }["GroomAdmissionsTable.useEffect"], [
        applied,
        reloadKey
    ]);
    const applications = (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$compiled$2f$react$2f$index$2e$js__$5b$app$2d$client$5d$__$28$ecmascript$29$__["useMemo"])({
        "GroomAdmissionsTable.useMemo[applications]": ()=>result?.content ?? []
    }["GroomAdmissionsTable.useMemo[applications]"], [
        result
    ]);
    const total = result?.totalElements ?? 0;
    const apply = ()=>{
        const next = {
            ...draft,
            candidateName: draft.candidateName.trim(),
            page: 0
        };
        setApplied(next);
        router.replace(`/groom/admissions${toQuery(next)}`);
    };
    const clear = ()=>{
        setDraft(emptyFilters);
        setApplied(emptyFilters);
        router.replace('/groom/admissions');
    };
    const changePage = (page)=>{
        const next = {
            ...applied,
            page
        };
        setDraft(next);
        setApplied(next);
        router.replace(`/groom/admissions${toQuery(next)}`);
    };
    if (loading && !result) {
        return /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$compiled$2f$react$2f$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$client$5d$__$28$ecmascript$29$__["jsxDEV"])(__TURBOPACK__imported__module__$5b$project$5d2f$src$2f$features$2f$admissions$2f$shared$2f$components$2f$AdmissionListLayout$2e$tsx__$5b$app$2d$client$5d$__$28$ecmascript$29$__["AdmissionListLayout"], {
            title: "Admissions",
            description: "Review applications, quarantine capacity and horse arrivals.",
            children: /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$compiled$2f$react$2f$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$client$5d$__$28$ecmascript$29$__["jsxDEV"])(__TURBOPACK__imported__module__$5b$project$5d2f$src$2f$components$2f$ui$2f$states$2e$tsx__$5b$app$2d$client$5d$__$28$ecmascript$29$__["ListSkeleton"], {
                rows: 8
            }, void 0, false, {
                fileName: "[project]/src/features/admissions/components/GroomAdmissionsTable.tsx",
                lineNumber: 90,
                columnNumber: 127
            }, this)
        }, void 0, false, {
            fileName: "[project]/src/features/admissions/components/GroomAdmissionsTable.tsx",
            lineNumber: 90,
            columnNumber: 12
        }, this);
    }
    if (error && !result) {
        return /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$compiled$2f$react$2f$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$client$5d$__$28$ecmascript$29$__["jsxDEV"])(__TURBOPACK__imported__module__$5b$project$5d2f$src$2f$features$2f$admissions$2f$shared$2f$components$2f$AdmissionListLayout$2e$tsx__$5b$app$2d$client$5d$__$28$ecmascript$29$__["AdmissionListLayout"], {
            title: "Admissions",
            description: "Review applications, quarantine capacity and horse arrivals.",
            children: /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$compiled$2f$react$2f$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$client$5d$__$28$ecmascript$29$__["jsxDEV"])(__TURBOPACK__imported__module__$5b$project$5d2f$src$2f$components$2f$ui$2f$states$2e$tsx__$5b$app$2d$client$5d$__$28$ecmascript$29$__["EmptyState"], {
                title: "Unable to load applications",
                description: error,
                action: /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$compiled$2f$react$2f$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$client$5d$__$28$ecmascript$29$__["jsxDEV"])(__TURBOPACK__imported__module__$5b$project$5d2f$src$2f$components$2f$ui$2f$Button$2e$tsx__$5b$app$2d$client$5d$__$28$ecmascript$29$__["Button"], {
                    size: "sm",
                    onClick: ()=>setReloadKey((value)=>value + 1),
                    children: "Retry"
                }, void 0, false, {
                    fileName: "[project]/src/features/admissions/components/GroomAdmissionsTable.tsx",
                    lineNumber: 99,
                    columnNumber: 19
                }, this)
            }, void 0, false, {
                fileName: "[project]/src/features/admissions/components/GroomAdmissionsTable.tsx",
                lineNumber: 96,
                columnNumber: 9
            }, this)
        }, void 0, false, {
            fileName: "[project]/src/features/admissions/components/GroomAdmissionsTable.tsx",
            lineNumber: 95,
            columnNumber: 7
        }, this);
    }
    return /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$compiled$2f$react$2f$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$client$5d$__$28$ecmascript$29$__["jsxDEV"])(__TURBOPACK__imported__module__$5b$project$5d2f$src$2f$features$2f$admissions$2f$shared$2f$components$2f$AdmissionListLayout$2e$tsx__$5b$app$2d$client$5d$__$28$ecmascript$29$__["AdmissionListLayout"], {
        title: "Admissions",
        description: "Review applications, quarantine capacity and horse arrivals.",
        children: [
            /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$compiled$2f$react$2f$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$client$5d$__$28$ecmascript$29$__["jsxDEV"])(__TURBOPACK__imported__module__$5b$project$5d2f$src$2f$features$2f$admissions$2f$shared$2f$components$2f$AdmissionFilterBar$2e$tsx__$5b$app$2d$client$5d$__$28$ecmascript$29$__["AdmissionFilterBar"], {
                search: draft.candidateName,
                onSearchChange: (candidateName)=>setDraft({
                        ...draft,
                        candidateName
                    }),
                onApply: apply,
                onClear: clear,
                children: [
                    /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$compiled$2f$react$2f$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$client$5d$__$28$ecmascript$29$__["jsxDEV"])(__TURBOPACK__imported__module__$5b$project$5d2f$src$2f$features$2f$admissions$2f$shared$2f$components$2f$AdmissionFilterBar$2e$tsx__$5b$app$2d$client$5d$__$28$ecmascript$29$__["FilterSelect"], {
                        label: "Status",
                        value: draft.status,
                        onChange: (status)=>setDraft({
                                ...draft,
                                status
                            }),
                        options: statuses
                    }, void 0, false, {
                        fileName: "[project]/src/features/admissions/components/GroomAdmissionsTable.tsx",
                        lineNumber: 113,
                        columnNumber: 9
                    }, this),
                    /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$compiled$2f$react$2f$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$client$5d$__$28$ecmascript$29$__["jsxDEV"])(__TURBOPACK__imported__module__$5b$project$5d2f$src$2f$features$2f$admissions$2f$shared$2f$components$2f$AdmissionFilterBar$2e$tsx__$5b$app$2d$client$5d$__$28$ecmascript$29$__["FilterDateRange"], {
                        from: draft.submittedFrom,
                        to: draft.submittedTo,
                        onFromChange: (submittedFrom)=>setDraft({
                                ...draft,
                                submittedFrom
                            }),
                        onToChange: (submittedTo)=>setDraft({
                                ...draft,
                                submittedTo
                            })
                    }, void 0, false, {
                        fileName: "[project]/src/features/admissions/components/GroomAdmissionsTable.tsx",
                        lineNumber: 114,
                        columnNumber: 9
                    }, this)
                ]
            }, void 0, true, {
                fileName: "[project]/src/features/admissions/components/GroomAdmissionsTable.tsx",
                lineNumber: 107,
                columnNumber: 7
            }, this),
            error && /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$compiled$2f$react$2f$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$client$5d$__$28$ecmascript$29$__["jsxDEV"])(__TURBOPACK__imported__module__$5b$project$5d2f$src$2f$components$2f$ui$2f$Notice$2e$tsx__$5b$app$2d$client$5d$__$28$ecmascript$29$__["Notice"], {
                tone: "error",
                children: error
            }, void 0, false, {
                fileName: "[project]/src/features/admissions/components/GroomAdmissionsTable.tsx",
                lineNumber: 122,
                columnNumber: 17
            }, this),
            /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$compiled$2f$react$2f$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$client$5d$__$28$ecmascript$29$__["jsxDEV"])(__TURBOPACK__imported__module__$5b$project$5d2f$src$2f$features$2f$admissions$2f$shared$2f$components$2f$AdmissionTable$2e$tsx__$5b$app$2d$client$5d$__$28$ecmascript$29$__["AdmissionTable"], {
                admissions: applications,
                detailHref: (id)=>`/groom/admissions/${id}${toQuery(applied)}`,
                searchable: false,
                loading: loading,
                emptyTitle: total > 0 ? 'No matching applications' : 'No applications',
                emptyDescription: total > 0 ? 'Change the filters or clear them to see other records.' : 'New applications will appear here.',
                emptyAction: total > 0 ? /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$compiled$2f$react$2f$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$client$5d$__$28$ecmascript$29$__["jsxDEV"])(__TURBOPACK__imported__module__$5b$project$5d2f$src$2f$components$2f$ui$2f$Button$2e$tsx__$5b$app$2d$client$5d$__$28$ecmascript$29$__["Button"], {
                    size: "sm",
                    onClick: clear,
                    children: "Clear filters"
                }, void 0, false, {
                    fileName: "[project]/src/features/admissions/components/GroomAdmissionsTable.tsx",
                    lineNumber: 131,
                    columnNumber: 34
                }, this) : undefined,
                pagination: result ? {
                    page: result.page,
                    pageSize: result.size,
                    total,
                    onPageChange: changePage,
                    disabled: loading
                } : undefined
            }, void 0, false, {
                fileName: "[project]/src/features/admissions/components/GroomAdmissionsTable.tsx",
                lineNumber: 124,
                columnNumber: 7
            }, this)
        ]
    }, void 0, true, {
        fileName: "[project]/src/features/admissions/components/GroomAdmissionsTable.tsx",
        lineNumber: 106,
        columnNumber: 5
    }, this);
}
_s(GroomAdmissionsTable, "5LBgO57qdZLJLXjfNTuVqAONr/I=", false, function() {
    return [
        __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$navigation$2e$js__$5b$app$2d$client$5d$__$28$ecmascript$29$__["useRouter"]
    ];
});
_c = GroomAdmissionsTable;
var _c;
__turbopack_context__.k.register(_c, "GroomAdmissionsTable");
if (typeof globalThis.$RefreshHelpers$ === 'object' && globalThis.$RefreshHelpers !== null) {
    __turbopack_context__.k.registerExports(__turbopack_context__.m, globalThis.$RefreshHelpers$);
}
}),
"[project]/src/features/admissions/services/api.ts [app-client] (ecmascript)", ((__turbopack_context__) => {
"use strict";

__turbopack_context__.s([
    "admissionsApi",
    ()=>admissionsApi
]);
var __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$build$2f$polyfills$2f$process$2e$js__$5b$app$2d$client$5d$__$28$ecmascript$29$__ = /*#__PURE__*/ __turbopack_context__.i("[project]/node_modules/next/dist/build/polyfills/process.js [app-client] (ecmascript)");
var __TURBOPACK__imported__module__$5b$project$5d2f$src$2f$services$2f$api$2e$ts__$5b$app$2d$client$5d$__$28$ecmascript$29$__ = __turbopack_context__.i("[project]/src/services/api.ts [app-client] (ecmascript)");
;
const admissionsApi = {
    getDocumentFile: (fileUrl, signal)=>(0, __TURBOPACK__imported__module__$5b$project$5d2f$src$2f$services$2f$api$2e$ts__$5b$app$2d$client$5d$__$28$ecmascript$29$__["apiGetBlob"])(fileUrl, signal),
    getGroomQueue: async (filters)=>{
        const params = new URLSearchParams();
        if (filters.candidateName.trim()) params.set('candidateName', filters.candidateName.trim());
        if (filters.status) params.set('status', filters.status);
        if (filters.submittedFrom) params.set('submittedFrom', filters.submittedFrom);
        if (filters.submittedTo) params.set('submittedTo', filters.submittedTo);
        params.set('page', String(filters.page));
        params.set('size', '10');
        const response = await (0, __TURBOPACK__imported__module__$5b$project$5d2f$src$2f$services$2f$api$2e$ts__$5b$app$2d$client$5d$__$28$ecmascript$29$__["apiGet"])(`/api/admissions/groom/queue?${params}`);
        return response.data;
    },
    getAdmissions: async (status)=>{
        const url = status ? `/api/admissions?status=${status}` : '/api/admissions';
        const response = await (0, __TURBOPACK__imported__module__$5b$project$5d2f$src$2f$services$2f$api$2e$ts__$5b$app$2d$client$5d$__$28$ecmascript$29$__["apiGet"])(url);
        return response.data;
    },
    getAdmissionDetail: async (id)=>{
        const response = await (0, __TURBOPACK__imported__module__$5b$project$5d2f$src$2f$services$2f$api$2e$ts__$5b$app$2d$client$5d$__$28$ecmascript$29$__["apiGet"])(`/api/admissions/${id}`);
        return response.data;
    },
    getDocuments: async (id)=>{
        const response = await (0, __TURBOPACK__imported__module__$5b$project$5d2f$src$2f$services$2f$api$2e$ts__$5b$app$2d$client$5d$__$28$ecmascript$29$__["apiGet"])(`/api/admissions/${id}/documents`);
        return response.data;
    },
    getHorseHealthMetrics: async (horseId)=>{
        const response = await (0, __TURBOPACK__imported__module__$5b$project$5d2f$src$2f$services$2f$api$2e$ts__$5b$app$2d$client$5d$__$28$ecmascript$29$__["apiGet"])(`/api/horses/${horseId}/health-metrics`);
        return response.data;
    },
    getVetQueue: async (filters = {})=>{
        const params = new URLSearchParams();
        if (filters.search?.trim()) params.set('search', filters.search.trim());
        if (filters.pill && filters.pill !== 'ALL') params.set('pill', filters.pill);
        if (filters.admissionStatus && filters.admissionStatus !== 'ALL') params.set('admissionStatus', filters.admissionStatus);
        if (filters.scheduleStatus && filters.scheduleStatus !== 'ALL') params.set('scheduleStatus', filters.scheduleStatus);
        if (filters.careType && filters.careType !== 'ALL') params.set('careType', filters.careType);
        if (filters.priority && filters.priority !== 'ALL') params.set('priority', filters.priority);
        params.set('page', String(filters.page ?? 0));
        params.set('size', String(filters.size ?? 10));
        const response = await (0, __TURBOPACK__imported__module__$5b$project$5d2f$src$2f$services$2f$api$2e$ts__$5b$app$2d$client$5d$__$28$ecmascript$29$__["apiGet"])(`/api/admissions/vet/queue?${params}`);
        return response.data;
    },
    getVetQueueSummary: async ()=>{
        const response = await (0, __TURBOPACK__imported__module__$5b$project$5d2f$src$2f$services$2f$api$2e$ts__$5b$app$2d$client$5d$__$28$ecmascript$29$__["apiGet"])('/api/admissions/vet/summary');
        return response.data;
    },
    getVetAdmissionDetail: async (id)=>{
        const response = await (0, __TURBOPACK__imported__module__$5b$project$5d2f$src$2f$services$2f$api$2e$ts__$5b$app$2d$client$5d$__$28$ecmascript$29$__["apiGet"])(`/api/admissions/vet/${id}`);
        return response.data;
    },
    getPendingUrgentAlerts: async ()=>{
        const response = await (0, __TURBOPACK__imported__module__$5b$project$5d2f$src$2f$services$2f$api$2e$ts__$5b$app$2d$client$5d$__$28$ecmascript$29$__["apiGet"])('/api/care-schedules/urgent-alerts/pending');
        return response.data;
    },
    getUrgentCase: async (scheduleId)=>{
        const response = await (0, __TURBOPACK__imported__module__$5b$project$5d2f$src$2f$services$2f$api$2e$ts__$5b$app$2d$client$5d$__$28$ecmascript$29$__["apiGet"])(`/api/care-schedules/urgent-alerts/${scheduleId}`);
        return response.data;
    },
    getCareSchedules: async (filters = {})=>{
        const params = new URLSearchParams();
        if (filters.status) params.set('status', filters.status);
        if (filters.careType) params.set('careType', filters.careType);
        if (filters.horseId) params.set('horseId', String(filters.horseId));
        if (filters.admissionId) params.set('admissionId', String(filters.admissionId));
        const vetId = filters.veterinarianId ?? filters.vetId;
        if (vetId) params.set('veterinarianId', String(vetId));
        params.set('page', String(filters.page ?? 0));
        params.set('size', String(filters.size ?? 50));
        const response = await (0, __TURBOPACK__imported__module__$5b$project$5d2f$src$2f$services$2f$api$2e$ts__$5b$app$2d$client$5d$__$28$ecmascript$29$__["apiGet"])(`/api/care-schedules?${params}`);
        if (response && typeof response === 'object' && 'data' in response) {
            const data = response.data;
            if (data && typeof data === 'object' && 'content' in data && Array.isArray(data.content)) {
                return data;
            }
            if (Array.isArray(data)) {
                return {
                    content: data,
                    totalElements: data.length,
                    totalPages: 1,
                    number: 0,
                    size: data.length
                };
            }
        }
        if (response && typeof response === 'object' && 'content' in response && Array.isArray(response.content)) {
            return response;
        }
        return {
            content: [],
            totalElements: 0,
            totalPages: 0,
            number: 0,
            size: 50
        };
    },
    getCareScheduleDetail: async (id)=>{
        const response = await (0, __TURBOPACK__imported__module__$5b$project$5d2f$src$2f$services$2f$api$2e$ts__$5b$app$2d$client$5d$__$28$ecmascript$29$__["apiGet"])(`/api/care-schedules/${id}`);
        return response.data;
    },
    startCareSchedule: async (id)=>{
        const response = await (0, __TURBOPACK__imported__module__$5b$project$5d2f$src$2f$services$2f$api$2e$ts__$5b$app$2d$client$5d$__$28$ecmascript$29$__["apiPost"])(`/api/care-schedules/${id}/start`, {});
        if (response && 'data' in response && response.data) return response.data;
        return response;
    },
    completeCareSchedule: async (id, data)=>{
        const response = await (0, __TURBOPACK__imported__module__$5b$project$5d2f$src$2f$services$2f$api$2e$ts__$5b$app$2d$client$5d$__$28$ecmascript$29$__["apiPost"])(`/api/care-schedules/${id}/complete`, data);
        if (response && 'data' in response && response.data) return response.data;
        return response;
    },
    vetReview: async (id, request)=>{
        const response = await (0, __TURBOPACK__imported__module__$5b$project$5d2f$src$2f$services$2f$api$2e$ts__$5b$app$2d$client$5d$__$28$ecmascript$29$__["apiPost"])(`/api/admissions/${id}/vet-review`, request);
        return response.data;
    },
    managerReview: async (id, request)=>{
        await (0, __TURBOPACK__imported__module__$5b$project$5d2f$src$2f$services$2f$api$2e$ts__$5b$app$2d$client$5d$__$28$ecmascript$29$__["apiPost"])(`/api/admissions/${id}/manager-review`, request);
    },
    groomReview: async (id, request)=>{
        const response = await (0, __TURBOPACK__imported__module__$5b$project$5d2f$src$2f$services$2f$api$2e$ts__$5b$app$2d$client$5d$__$28$ecmascript$29$__["apiPost"])(`/api/admissions/${id}/groom-review`, request);
        return response.data;
    },
    retryQuarantineAllocation: async (id)=>{
        const response = await (0, __TURBOPACK__imported__module__$5b$project$5d2f$src$2f$services$2f$api$2e$ts__$5b$app$2d$client$5d$__$28$ecmascript$29$__["apiPost"])(`/api/admissions/${id}/quarantine-allocation`, {});
        return response.data;
    },
    confirmArrival: async (id)=>{
        const response = await (0, __TURBOPACK__imported__module__$5b$project$5d2f$src$2f$services$2f$api$2e$ts__$5b$app$2d$client$5d$__$28$ecmascript$29$__["apiPost"])(`/api/admissions/${id}/arrival-confirmation`, {});
        return response.data;
    },
    reopenExpiredArrival: async (id)=>{
        const response = await (0, __TURBOPACK__imported__module__$5b$project$5d2f$src$2f$services$2f$api$2e$ts__$5b$app$2d$client$5d$__$28$ecmascript$29$__["apiPost"])(`/api/admissions/${id}/arrival-reopen`, {});
        return response.data;
    },
    assetUrl: (url)=>{
        if (/^https?:\/\//i.test(url)) return url;
        const baseUrl = ("TURBOPACK compile-time value", "http://localhost:8080") ?? '';
        return `${baseUrl.replace(/\/$/, '')}/${url.replace(/^\//, '')}`;
    }
};
if (typeof globalThis.$RefreshHelpers$ === 'object' && globalThis.$RefreshHelpers !== null) {
    __turbopack_context__.k.registerExports(__turbopack_context__.m, globalThis.$RefreshHelpers$);
}
}),
"[project]/src/features/admissions/shared/components/AdmissionFilterBar.tsx [app-client] (ecmascript)", ((__turbopack_context__) => {
"use strict";

__turbopack_context__.s([
    "AdmissionFilterBar",
    ()=>AdmissionFilterBar,
    "FilterDateRange",
    ()=>FilterDateRange,
    "FilterSelect",
    ()=>FilterSelect
]);
var __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$compiled$2f$react$2f$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$client$5d$__$28$ecmascript$29$__ = __turbopack_context__.i("[project]/node_modules/next/dist/compiled/react/jsx-dev-runtime.js [app-client] (ecmascript)");
var __TURBOPACK__imported__module__$5b$project$5d2f$src$2f$components$2f$ui$2f$index$2e$ts__$5b$app$2d$client$5d$__$28$ecmascript$29$__$3c$locals$3e$__ = __turbopack_context__.i("[project]/src/components/ui/index.ts [app-client] (ecmascript) <locals>");
var __TURBOPACK__imported__module__$5b$project$5d2f$src$2f$components$2f$ui$2f$Button$2e$tsx__$5b$app$2d$client$5d$__$28$ecmascript$29$__ = __turbopack_context__.i("[project]/src/components/ui/Button.tsx [app-client] (ecmascript)");
var __TURBOPACK__imported__module__$5b$project$5d2f$src$2f$components$2f$ui$2f$FilterBar$2e$tsx__$5b$app$2d$client$5d$__$28$ecmascript$29$__ = __turbopack_context__.i("[project]/src/components/ui/FilterBar.tsx [app-client] (ecmascript)");
var __TURBOPACK__imported__module__$5b$project$5d2f$src$2f$components$2f$ui$2f$FormField$2e$tsx__$5b$app$2d$client$5d$__$28$ecmascript$29$__ = __turbopack_context__.i("[project]/src/components/ui/FormField.tsx [app-client] (ecmascript)");
var __TURBOPACK__imported__module__$5b$project$5d2f$src$2f$components$2f$ui$2f$Input$2e$tsx__$5b$app$2d$client$5d$__$28$ecmascript$29$__ = __turbopack_context__.i("[project]/src/components/ui/Input.tsx [app-client] (ecmascript)");
'use client';
;
;
function AdmissionFilterBar({ search, onSearchChange, searchLabel = 'Search horse name', searchPlaceholder = 'Search horse name', onApply, onClear, children }) {
    return /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$compiled$2f$react$2f$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$client$5d$__$28$ecmascript$29$__["jsxDEV"])(__TURBOPACK__imported__module__$5b$project$5d2f$src$2f$components$2f$ui$2f$FilterBar$2e$tsx__$5b$app$2d$client$5d$__$28$ecmascript$29$__["FilterBar"], {
        // Filter controls are one step more compact than form controls.
        className: "[&_input]:min-h-9 [&_select]:min-h-9",
        onSubmit: (event)=>{
            event.preventDefault();
            onApply();
        },
        children: [
            /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$compiled$2f$react$2f$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$client$5d$__$28$ecmascript$29$__["jsxDEV"])(__TURBOPACK__imported__module__$5b$project$5d2f$src$2f$components$2f$ui$2f$FormField$2e$tsx__$5b$app$2d$client$5d$__$28$ecmascript$29$__["FormField"], {
                label: searchLabel,
                className: "min-w-[14rem] flex-[1.4]",
                children: /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$compiled$2f$react$2f$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$client$5d$__$28$ecmascript$29$__["jsxDEV"])(__TURBOPACK__imported__module__$5b$project$5d2f$src$2f$components$2f$ui$2f$Input$2e$tsx__$5b$app$2d$client$5d$__$28$ecmascript$29$__["SearchInput"], {
                    value: search,
                    onChange: onSearchChange,
                    placeholder: searchPlaceholder,
                    label: searchLabel
                }, void 0, false, {
                    fileName: "[project]/src/features/admissions/shared/components/AdmissionFilterBar.tsx",
                    lineNumber: 37,
                    columnNumber: 9
                }, this)
            }, void 0, false, {
                fileName: "[project]/src/features/admissions/shared/components/AdmissionFilterBar.tsx",
                lineNumber: 36,
                columnNumber: 7
            }, this),
            children,
            /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$compiled$2f$react$2f$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$client$5d$__$28$ecmascript$29$__["jsxDEV"])("div", {
                className: "flex shrink-0 items-center gap-2",
                children: [
                    /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$compiled$2f$react$2f$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$client$5d$__$28$ecmascript$29$__["jsxDEV"])(__TURBOPACK__imported__module__$5b$project$5d2f$src$2f$components$2f$ui$2f$Button$2e$tsx__$5b$app$2d$client$5d$__$28$ecmascript$29$__["Button"], {
                        type: "submit",
                        variant: "primary",
                        children: "Apply"
                    }, void 0, false, {
                        fileName: "[project]/src/features/admissions/shared/components/AdmissionFilterBar.tsx",
                        lineNumber: 41,
                        columnNumber: 9
                    }, this),
                    /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$compiled$2f$react$2f$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$client$5d$__$28$ecmascript$29$__["jsxDEV"])(__TURBOPACK__imported__module__$5b$project$5d2f$src$2f$components$2f$ui$2f$Button$2e$tsx__$5b$app$2d$client$5d$__$28$ecmascript$29$__["Button"], {
                        type: "button",
                        variant: "secondary",
                        onClick: onClear,
                        children: "Clear"
                    }, void 0, false, {
                        fileName: "[project]/src/features/admissions/shared/components/AdmissionFilterBar.tsx",
                        lineNumber: 42,
                        columnNumber: 9
                    }, this)
                ]
            }, void 0, true, {
                fileName: "[project]/src/features/admissions/shared/components/AdmissionFilterBar.tsx",
                lineNumber: 40,
                columnNumber: 7
            }, this)
        ]
    }, void 0, true, {
        fileName: "[project]/src/features/admissions/shared/components/AdmissionFilterBar.tsx",
        lineNumber: 28,
        columnNumber: 5
    }, this);
}
_c = AdmissionFilterBar;
function FilterSelect({ label, value, onChange, options }) {
    return /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$compiled$2f$react$2f$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$client$5d$__$28$ecmascript$29$__["jsxDEV"])(__TURBOPACK__imported__module__$5b$project$5d2f$src$2f$components$2f$ui$2f$FormField$2e$tsx__$5b$app$2d$client$5d$__$28$ecmascript$29$__["FormField"], {
        label: label,
        className: "min-w-[10rem] flex-1",
        children: /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$compiled$2f$react$2f$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$client$5d$__$28$ecmascript$29$__["jsxDEV"])(__TURBOPACK__imported__module__$5b$project$5d2f$src$2f$components$2f$ui$2f$Input$2e$tsx__$5b$app$2d$client$5d$__$28$ecmascript$29$__["Select"], {
            value: value,
            onChange: (event)=>onChange(event.target.value),
            children: options.map((option)=>/*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$compiled$2f$react$2f$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$client$5d$__$28$ecmascript$29$__["jsxDEV"])("option", {
                    value: option.value,
                    children: option.label
                }, option.value || 'all', false, {
                    fileName: "[project]/src/features/admissions/shared/components/AdmissionFilterBar.tsx",
                    lineNumber: 63,
                    columnNumber: 11
                }, this))
        }, void 0, false, {
            fileName: "[project]/src/features/admissions/shared/components/AdmissionFilterBar.tsx",
            lineNumber: 61,
            columnNumber: 7
        }, this)
    }, void 0, false, {
        fileName: "[project]/src/features/admissions/shared/components/AdmissionFilterBar.tsx",
        lineNumber: 60,
        columnNumber: 5
    }, this);
}
_c1 = FilterSelect;
function FilterDateRange({ from, to, onFromChange, onToChange }) {
    return /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$compiled$2f$react$2f$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$client$5d$__$28$ecmascript$29$__["jsxDEV"])(__TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$compiled$2f$react$2f$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$client$5d$__$28$ecmascript$29$__["Fragment"], {
        children: [
            /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$compiled$2f$react$2f$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$client$5d$__$28$ecmascript$29$__["jsxDEV"])(__TURBOPACK__imported__module__$5b$project$5d2f$src$2f$components$2f$ui$2f$FormField$2e$tsx__$5b$app$2d$client$5d$__$28$ecmascript$29$__["FormField"], {
                label: "Submitted from",
                className: "min-w-[9.5rem] flex-1",
                children: /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$compiled$2f$react$2f$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$client$5d$__$28$ecmascript$29$__["jsxDEV"])(__TURBOPACK__imported__module__$5b$project$5d2f$src$2f$components$2f$ui$2f$Input$2e$tsx__$5b$app$2d$client$5d$__$28$ecmascript$29$__["Input"], {
                    type: "date",
                    value: from,
                    onChange: (event)=>onFromChange(event.target.value)
                }, void 0, false, {
                    fileName: "[project]/src/features/admissions/shared/components/AdmissionFilterBar.tsx",
                    lineNumber: 84,
                    columnNumber: 9
                }, this)
            }, void 0, false, {
                fileName: "[project]/src/features/admissions/shared/components/AdmissionFilterBar.tsx",
                lineNumber: 83,
                columnNumber: 7
            }, this),
            /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$compiled$2f$react$2f$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$client$5d$__$28$ecmascript$29$__["jsxDEV"])(__TURBOPACK__imported__module__$5b$project$5d2f$src$2f$components$2f$ui$2f$FormField$2e$tsx__$5b$app$2d$client$5d$__$28$ecmascript$29$__["FormField"], {
                label: "Submitted to",
                className: "min-w-[9.5rem] flex-1",
                children: /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$compiled$2f$react$2f$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$client$5d$__$28$ecmascript$29$__["jsxDEV"])(__TURBOPACK__imported__module__$5b$project$5d2f$src$2f$components$2f$ui$2f$Input$2e$tsx__$5b$app$2d$client$5d$__$28$ecmascript$29$__["Input"], {
                    type: "date",
                    min: from || undefined,
                    value: to,
                    onChange: (event)=>onToChange(event.target.value)
                }, void 0, false, {
                    fileName: "[project]/src/features/admissions/shared/components/AdmissionFilterBar.tsx",
                    lineNumber: 87,
                    columnNumber: 9
                }, this)
            }, void 0, false, {
                fileName: "[project]/src/features/admissions/shared/components/AdmissionFilterBar.tsx",
                lineNumber: 86,
                columnNumber: 7
            }, this)
        ]
    }, void 0, true, {
        fileName: "[project]/src/features/admissions/shared/components/AdmissionFilterBar.tsx",
        lineNumber: 82,
        columnNumber: 5
    }, this);
}
_c2 = FilterDateRange;
var _c, _c1, _c2;
__turbopack_context__.k.register(_c, "AdmissionFilterBar");
__turbopack_context__.k.register(_c1, "FilterSelect");
__turbopack_context__.k.register(_c2, "FilterDateRange");
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
"[project]/src/features/admissions/shared/components/AdmissionTable.tsx [app-client] (ecmascript)", ((__turbopack_context__) => {
"use strict";

__turbopack_context__.s([
    "AdmissionTable",
    ()=>AdmissionTable
]);
var __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$compiled$2f$react$2f$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$client$5d$__$28$ecmascript$29$__ = __turbopack_context__.i("[project]/node_modules/next/dist/compiled/react/jsx-dev-runtime.js [app-client] (ecmascript)");
var __TURBOPACK__imported__module__$5b$project$5d2f$src$2f$components$2f$ui$2f$DataTable$2e$tsx__$5b$app$2d$client$5d$__$28$ecmascript$29$__ = __turbopack_context__.i("[project]/src/components/ui/DataTable.tsx [app-client] (ecmascript)");
var __TURBOPACK__imported__module__$5b$project$5d2f$src$2f$components$2f$ui$2f$Button$2e$tsx__$5b$app$2d$client$5d$__$28$ecmascript$29$__ = __turbopack_context__.i("[project]/src/components/ui/Button.tsx [app-client] (ecmascript)");
var __TURBOPACK__imported__module__$5b$project$5d2f$src$2f$components$2f$ui$2f$HorseAvatar$2e$tsx__$5b$app$2d$client$5d$__$28$ecmascript$29$__ = __turbopack_context__.i("[project]/src/components/ui/HorseAvatar.tsx [app-client] (ecmascript)");
var __TURBOPACK__imported__module__$5b$project$5d2f$src$2f$lib$2f$display$2e$ts__$5b$app$2d$client$5d$__$28$ecmascript$29$__ = __turbopack_context__.i("[project]/src/lib/display.ts [app-client] (ecmascript)");
var __TURBOPACK__imported__module__$5b$project$5d2f$src$2f$features$2f$admissions$2f$shared$2f$components$2f$AdmissionStatusBadge$2e$tsx__$5b$app$2d$client$5d$__$28$ecmascript$29$__ = __turbopack_context__.i("[project]/src/features/admissions/shared/components/AdmissionStatusBadge.tsx [app-client] (ecmascript)");
;
;
;
;
;
;
function AdmissionTable({ admissions, detailHref, renderAvatar, extraColumns = [], simplifiedStatus = false, action, emptyTitle, emptyDescription, emptyAction, searchable = true, loading, pagination }) {
    const columns = [
        {
            id: 'horse',
            header: 'Horse',
            sortValue: (admission)=>admission.candidateName,
            render: (admission)=>/*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$compiled$2f$react$2f$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$client$5d$__$28$ecmascript$29$__["jsxDEV"])("div", {
                    className: "flex min-w-0 items-center gap-3",
                    children: [
                        renderAvatar ? renderAvatar(admission) : /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$compiled$2f$react$2f$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$client$5d$__$28$ecmascript$29$__["jsxDEV"])(__TURBOPACK__imported__module__$5b$project$5d2f$src$2f$components$2f$ui$2f$HorseAvatar$2e$tsx__$5b$app$2d$client$5d$__$28$ecmascript$29$__["HorseAvatar"], {
                            name: admission.candidateName,
                            image: admission.imageUrl,
                            size: 32,
                            rounded: "md"
                        }, void 0, false, {
                            fileName: "[project]/src/features/admissions/shared/components/AdmissionTable.tsx",
                            lineNumber: 60,
                            columnNumber: 13
                        }, this),
                        /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$compiled$2f$react$2f$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$client$5d$__$28$ecmascript$29$__["jsxDEV"])("div", {
                            className: "min-w-0",
                            children: [
                                /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$compiled$2f$react$2f$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$client$5d$__$28$ecmascript$29$__["jsxDEV"])("span", {
                                    className: "block truncate font-semibold",
                                    children: admission.candidateName
                                }, void 0, false, {
                                    fileName: "[project]/src/features/admissions/shared/components/AdmissionTable.tsx",
                                    lineNumber: 63,
                                    columnNumber: 13
                                }, this),
                                /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$compiled$2f$react$2f$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$client$5d$__$28$ecmascript$29$__["jsxDEV"])("span", {
                                    className: "block truncate text-xs text-[var(--color-text-muted)]",
                                    children: admission.breed || 'Breed not provided'
                                }, void 0, false, {
                                    fileName: "[project]/src/features/admissions/shared/components/AdmissionTable.tsx",
                                    lineNumber: 64,
                                    columnNumber: 13
                                }, this)
                            ]
                        }, void 0, true, {
                            fileName: "[project]/src/features/admissions/shared/components/AdmissionTable.tsx",
                            lineNumber: 62,
                            columnNumber: 11
                        }, this)
                    ]
                }, void 0, true, {
                    fileName: "[project]/src/features/admissions/shared/components/AdmissionTable.tsx",
                    lineNumber: 58,
                    columnNumber: 9
                }, this)
        },
        {
            id: 'status',
            header: 'Status',
            sortValue: (admission)=>admission.status,
            render: (admission)=>/*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$compiled$2f$react$2f$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$client$5d$__$28$ecmascript$29$__["jsxDEV"])(__TURBOPACK__imported__module__$5b$project$5d2f$src$2f$features$2f$admissions$2f$shared$2f$components$2f$AdmissionStatusBadge$2e$tsx__$5b$app$2d$client$5d$__$28$ecmascript$29$__["AdmissionStatusBadge"], {
                    status: admission.status,
                    simplified: simplifiedStatus
                }, void 0, false, {
                    fileName: "[project]/src/features/admissions/shared/components/AdmissionTable.tsx",
                    lineNumber: 73,
                    columnNumber: 30
                }, this)
        },
        ...extraColumns,
        {
            id: 'submitted',
            header: 'Submitted',
            sortValue: (admission)=>new Date(admission.submittedAt),
            render: (admission)=>(0, __TURBOPACK__imported__module__$5b$project$5d2f$src$2f$lib$2f$display$2e$ts__$5b$app$2d$client$5d$__$28$ecmascript$29$__["formatDate"])(admission.submittedAt)
        },
        {
            id: 'actions',
            header: 'Action',
            align: 'right',
            render: (admission)=>{
                const { label, primary } = action?.(admission) ?? {
                    label: 'View'
                };
                return /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$compiled$2f$react$2f$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$client$5d$__$28$ecmascript$29$__["jsxDEV"])(__TURBOPACK__imported__module__$5b$project$5d2f$src$2f$components$2f$ui$2f$Button$2e$tsx__$5b$app$2d$client$5d$__$28$ecmascript$29$__["LinkButton"], {
                    href: detailHref(admission.admissionId),
                    size: "sm",
                    variant: primary ? 'primary' : 'secondary',
                    className: "min-w-[4.5rem]",
                    children: label
                }, void 0, false, {
                    fileName: "[project]/src/features/admissions/shared/components/AdmissionTable.tsx",
                    lineNumber: 89,
                    columnNumber: 11
                }, this);
            }
        }
    ];
    return /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$compiled$2f$react$2f$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$client$5d$__$28$ecmascript$29$__["jsxDEV"])(__TURBOPACK__imported__module__$5b$project$5d2f$src$2f$components$2f$ui$2f$DataTable$2e$tsx__$5b$app$2d$client$5d$__$28$ecmascript$29$__["DataTable"], {
        rows: admissions,
        columns: columns,
        getRowKey: (admission)=>admission.admissionId,
        ariaLabel: "Admission records",
        loading: loading,
        emptyTitle: emptyTitle ?? 'No admissions found',
        emptyDescription: emptyDescription,
        emptyAction: emptyAction,
        pageSize: pagination ? undefined : 10,
        pagination: pagination,
        getSearchText: searchable ? (admission)=>`${admission.candidateName} ${admission.breed ?? ''} ${admission.status}` : undefined,
        searchPlaceholder: "Search horse, breed, or status…"
    }, void 0, false, {
        fileName: "[project]/src/features/admissions/shared/components/AdmissionTable.tsx",
        lineNumber: 103,
        columnNumber: 5
    }, this);
}
_c = AdmissionTable;
var _c;
__turbopack_context__.k.register(_c, "AdmissionTable");
if (typeof globalThis.$RefreshHelpers$ === 'object' && globalThis.$RefreshHelpers !== null) {
    __turbopack_context__.k.registerExports(__turbopack_context__.m, globalThis.$RefreshHelpers$);
}
}),
"[project]/src/lib/display.ts [app-client] (ecmascript)", ((__turbopack_context__) => {
"use strict";

__turbopack_context__.s([
    "displayError",
    ()=>displayError,
    "formatDate",
    ()=>formatDate,
    "formatDateTime",
    ()=>formatDateTime,
    "formatEnumLabel",
    ()=>formatEnumLabel,
    "formatNumber",
    ()=>formatNumber,
    "formatTime",
    ()=>formatTime
]);
const vietnameseText = /[\u00C0-\u024F]/;
function displayError(error, fallback) {
    if (!(error instanceof Error) || !error.message.trim()) return fallback;
    return vietnameseText.test(error.message) ? fallback : error.message;
}
function formatDate(value, options) {
    if (!value) return '—';
    const date = value instanceof Date ? value : new Date(value);
    if (Number.isNaN(date.getTime())) return '—';
    return new Intl.DateTimeFormat('en-GB', options ?? {
        day: '2-digit',
        month: 'short',
        year: 'numeric'
    }).format(date);
}
function formatDateTime(value) {
    return formatDate(value, {
        day: '2-digit',
        month: 'short',
        year: 'numeric',
        hour: '2-digit',
        minute: '2-digit'
    });
}
function formatTime(value) {
    return value ? value.slice(0, 5) : '—';
}
function formatEnumLabel(value) {
    if (!value) return '—';
    const words = value.replaceAll('_', ' ').toLowerCase();
    return words.charAt(0).toUpperCase() + words.slice(1);
}
function formatNumber(value, fractionDigits = 0) {
    if (value === null || value === undefined || Number.isNaN(value)) return '—';
    return new Intl.NumberFormat('en-GB', {
        minimumFractionDigits: fractionDigits,
        maximumFractionDigits: fractionDigits
    }).format(value);
}
if (typeof globalThis.$RefreshHelpers$ === 'object' && globalThis.$RefreshHelpers !== null) {
    __turbopack_context__.k.registerExports(__turbopack_context__.m, globalThis.$RefreshHelpers$);
}
}),
]);

//# sourceMappingURL=src_02pctj4._.js.map