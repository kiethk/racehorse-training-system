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
"[project]/src/features/admissions/components/GroomAdmissionReview.tsx [app-client] (ecmascript)", ((__turbopack_context__) => {
"use strict";

__turbopack_context__.s([
    "GroomAdmissionReview",
    ()=>GroomAdmissionReview
]);
var __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$compiled$2f$react$2f$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$client$5d$__$28$ecmascript$29$__ = __turbopack_context__.i("[project]/node_modules/next/dist/compiled/react/jsx-dev-runtime.js [app-client] (ecmascript)");
var __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$client$2f$app$2d$dir$2f$link$2e$js__$5b$app$2d$client$5d$__$28$ecmascript$29$__ = __turbopack_context__.i("[project]/node_modules/next/dist/client/app-dir/link.js [app-client] (ecmascript)");
var __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$compiled$2f$react$2f$index$2e$js__$5b$app$2d$client$5d$__$28$ecmascript$29$__ = __turbopack_context__.i("[project]/node_modules/next/dist/compiled/react/index.js [app-client] (ecmascript)");
var __TURBOPACK__imported__module__$5b$project$5d2f$src$2f$components$2f$ui$2f$Button$2e$tsx__$5b$app$2d$client$5d$__$28$ecmascript$29$__ = __turbopack_context__.i("[project]/src/components/ui/Button.tsx [app-client] (ecmascript)");
var __TURBOPACK__imported__module__$5b$project$5d2f$src$2f$components$2f$ui$2f$states$2e$tsx__$5b$app$2d$client$5d$__$28$ecmascript$29$__ = __turbopack_context__.i("[project]/src/components/ui/states.tsx [app-client] (ecmascript)");
var __TURBOPACK__imported__module__$5b$project$5d2f$src$2f$components$2f$ui$2f$FormField$2e$tsx__$5b$app$2d$client$5d$__$28$ecmascript$29$__ = __turbopack_context__.i("[project]/src/components/ui/FormField.tsx [app-client] (ecmascript)");
var __TURBOPACK__imported__module__$5b$project$5d2f$src$2f$components$2f$ui$2f$Input$2e$tsx__$5b$app$2d$client$5d$__$28$ecmascript$29$__ = __turbopack_context__.i("[project]/src/components/ui/Input.tsx [app-client] (ecmascript)");
var __TURBOPACK__imported__module__$5b$project$5d2f$src$2f$components$2f$ui$2f$Notice$2e$tsx__$5b$app$2d$client$5d$__$28$ecmascript$29$__ = __turbopack_context__.i("[project]/src/components/ui/Notice.tsx [app-client] (ecmascript)");
var __TURBOPACK__imported__module__$5b$project$5d2f$src$2f$features$2f$admissions$2f$services$2f$api$2e$ts__$5b$app$2d$client$5d$__$28$ecmascript$29$__ = __turbopack_context__.i("[project]/src/features/admissions/services/api.ts [app-client] (ecmascript)");
var __TURBOPACK__imported__module__$5b$project$5d2f$src$2f$lib$2f$display$2e$ts__$5b$app$2d$client$5d$__$28$ecmascript$29$__ = __turbopack_context__.i("[project]/src/lib/display.ts [app-client] (ecmascript)");
var __TURBOPACK__imported__module__$5b$project$5d2f$src$2f$features$2f$admissions$2f$shared$2f$components$2f$AdmissionDetailLayout$2e$tsx__$5b$app$2d$client$5d$__$28$ecmascript$29$__ = __turbopack_context__.i("[project]/src/features/admissions/shared/components/AdmissionDetailLayout.tsx [app-client] (ecmascript)");
var __TURBOPACK__imported__module__$5b$project$5d2f$src$2f$features$2f$admissions$2f$shared$2f$components$2f$AdmissionDetailHeader$2e$tsx__$5b$app$2d$client$5d$__$28$ecmascript$29$__ = __turbopack_context__.i("[project]/src/features/admissions/shared/components/AdmissionDetailHeader.tsx [app-client] (ecmascript)");
var __TURBOPACK__imported__module__$5b$project$5d2f$src$2f$features$2f$admissions$2f$shared$2f$components$2f$AdmissionPipeline$2e$tsx__$5b$app$2d$client$5d$__$28$ecmascript$29$__ = __turbopack_context__.i("[project]/src/features/admissions/shared/components/AdmissionPipeline.tsx [app-client] (ecmascript)");
var __TURBOPACK__imported__module__$5b$project$5d2f$src$2f$features$2f$admissions$2f$shared$2f$components$2f$AdmissionDetailTabs$2e$tsx__$5b$app$2d$client$5d$__$28$ecmascript$29$__ = __turbopack_context__.i("[project]/src/features/admissions/shared/components/AdmissionDetailTabs.tsx [app-client] (ecmascript)");
var __TURBOPACK__imported__module__$5b$project$5d2f$src$2f$features$2f$admissions$2f$shared$2f$components$2f$AdmissionDocumentsSection$2e$tsx__$5b$app$2d$client$5d$__$28$ecmascript$29$__ = __turbopack_context__.i("[project]/src/features/admissions/shared/components/AdmissionDocumentsSection.tsx [app-client] (ecmascript)");
var __TURBOPACK__imported__module__$5b$project$5d2f$src$2f$features$2f$admissions$2f$shared$2f$components$2f$AdmissionInfoSection$2e$tsx__$5b$app$2d$client$5d$__$28$ecmascript$29$__ = __turbopack_context__.i("[project]/src/features/admissions/shared/components/AdmissionInfoSection.tsx [app-client] (ecmascript)");
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
function date(value) {
    return value ? (0, __TURBOPACK__imported__module__$5b$project$5d2f$src$2f$lib$2f$display$2e$ts__$5b$app$2d$client$5d$__$28$ecmascript$29$__["formatDate"])(value) : 'Not recorded';
}
function GroomAdmissionReview({ admissionId, returnTo, onUpdated }) {
    _s();
    const [detail, setDetail] = (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$compiled$2f$react$2f$index$2e$js__$5b$app$2d$client$5d$__$28$ecmascript$29$__["useState"])(null);
    const [loading, setLoading] = (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$compiled$2f$react$2f$index$2e$js__$5b$app$2d$client$5d$__$28$ecmascript$29$__["useState"])(true);
    const [error, setError] = (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$compiled$2f$react$2f$index$2e$js__$5b$app$2d$client$5d$__$28$ecmascript$29$__["useState"])(null);
    const [feedback, setFeedback] = (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$compiled$2f$react$2f$index$2e$js__$5b$app$2d$client$5d$__$28$ecmascript$29$__["useState"])('');
    const [feedbackError, setFeedbackError] = (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$compiled$2f$react$2f$index$2e$js__$5b$app$2d$client$5d$__$28$ecmascript$29$__["useState"])(null);
    const [submitting, setSubmitting] = (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$compiled$2f$react$2f$index$2e$js__$5b$app$2d$client$5d$__$28$ecmascript$29$__["useState"])(false);
    const [notice, setNotice] = (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$compiled$2f$react$2f$index$2e$js__$5b$app$2d$client$5d$__$28$ecmascript$29$__["useState"])(null);
    const load = (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$compiled$2f$react$2f$index$2e$js__$5b$app$2d$client$5d$__$28$ecmascript$29$__["useCallback"])({
        "GroomAdmissionReview.useCallback[load]": async ()=>{
            setLoading(true);
            setError(null);
            setDetail(null);
            try {
                setDetail(await __TURBOPACK__imported__module__$5b$project$5d2f$src$2f$features$2f$admissions$2f$services$2f$api$2e$ts__$5b$app$2d$client$5d$__$28$ecmascript$29$__["admissionsApi"].getAdmissionDetail(admissionId));
            } catch (cause) {
                setError(cause instanceof Error ? cause.message : 'Unable to load this application.');
            } finally{
                setLoading(false);
            }
        }
    }["GroomAdmissionReview.useCallback[load]"], [
        admissionId
    ]);
    (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$compiled$2f$react$2f$index$2e$js__$5b$app$2d$client$5d$__$28$ecmascript$29$__["useEffect"])({
        "GroomAdmissionReview.useEffect": ()=>{
            // eslint-disable-next-line react-hooks/set-state-in-effect
            void load();
        }
    }["GroomAdmissionReview.useEffect"], [
        load
    ]);
    const review = async (decision)=>{
        if (!feedback.trim()) {
            setFeedbackError('Feedback is required for every Groom decision.');
            return;
        }
        setSubmitting(true);
        setFeedbackError(null);
        setNotice(null);
        try {
            const updated = await __TURBOPACK__imported__module__$5b$project$5d2f$src$2f$features$2f$admissions$2f$services$2f$api$2e$ts__$5b$app$2d$client$5d$__$28$ecmascript$29$__["admissionsApi"].groomReview(admissionId, {
                decision,
                feedback: feedback.trim()
            });
            setDetail(updated);
            onUpdated?.();
            setNotice(updated.status === 'WAITING_FOR_STALL' ? 'Groom approved this application. Capacity is not available yet, so it is waiting for a stall.' : decision === 'REJECTED' ? 'Application rejected.' : 'Application approved. A quarantine stall is reserved for 14 days while waiting for arrival.');
        } catch (cause) {
            setNotice(null);
            setError(cause instanceof Error ? cause.message : 'Unable to submit the Groom decision.');
        } finally{
            setSubmitting(false);
        }
    };
    const retryAllocation = async ()=>{
        setSubmitting(true);
        setError(null);
        setNotice(null);
        try {
            const updated = await __TURBOPACK__imported__module__$5b$project$5d2f$src$2f$features$2f$admissions$2f$services$2f$api$2e$ts__$5b$app$2d$client$5d$__$28$ecmascript$29$__["admissionsApi"].retryQuarantineAllocation(admissionId);
            setDetail(updated);
            onUpdated?.();
            setNotice(updated.status === 'WAITING_FOR_STALL' ? 'Capacity is still unavailable. The application remains in the waiting queue.' : 'A quarantine stall is reserved for 14 days while waiting for arrival.');
        } catch (cause) {
            setError(cause instanceof Error ? cause.message : 'Unable to retry stall allocation.');
        } finally{
            setSubmitting(false);
        }
    };
    const confirmArrival = async ()=>{
        setSubmitting(true);
        setError(null);
        setNotice(null);
        try {
            const updated = await __TURBOPACK__imported__module__$5b$project$5d2f$src$2f$features$2f$admissions$2f$services$2f$api$2e$ts__$5b$app$2d$client$5d$__$28$ecmascript$29$__["admissionsApi"].confirmArrival(admissionId);
            setDetail(updated);
            onUpdated?.();
            setNotice(updated.status === 'ARRIVAL_EXPIRED' ? 'The arrival deadline has passed. The reservation was released; ask an authorized manager to reopen the admission.' : 'Arrival confirmed and the horse was registered. Veterinary assignment is being scheduled with workload and overdue appointments taken into account.');
        } catch (cause) {
            setError(cause instanceof Error ? cause.message : 'Unable to confirm horse arrival.');
        } finally{
            setSubmitting(false);
        }
    };
    if (loading) return /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$compiled$2f$react$2f$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$client$5d$__$28$ecmascript$29$__["jsxDEV"])("div", {
        className: "p-6",
        children: /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$compiled$2f$react$2f$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$client$5d$__$28$ecmascript$29$__["jsxDEV"])(__TURBOPACK__imported__module__$5b$project$5d2f$src$2f$components$2f$ui$2f$states$2e$tsx__$5b$app$2d$client$5d$__$28$ecmascript$29$__["ListSkeleton"], {
            rows: 8
        }, void 0, false, {
            fileName: "[project]/src/features/admissions/components/GroomAdmissionReview.tsx",
            lineNumber: 116,
            columnNumber: 44
        }, this)
    }, void 0, false, {
        fileName: "[project]/src/features/admissions/components/GroomAdmissionReview.tsx",
        lineNumber: 116,
        columnNumber: 23
    }, this);
    if (error && !detail) return /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$compiled$2f$react$2f$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$client$5d$__$28$ecmascript$29$__["jsxDEV"])(__TURBOPACK__imported__module__$5b$project$5d2f$src$2f$components$2f$ui$2f$states$2e$tsx__$5b$app$2d$client$5d$__$28$ecmascript$29$__["EmptyState"], {
        icon: "alert-triangle",
        title: "Unable to load application",
        description: error,
        action: /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$compiled$2f$react$2f$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$client$5d$__$28$ecmascript$29$__["jsxDEV"])(__TURBOPACK__imported__module__$5b$project$5d2f$src$2f$components$2f$ui$2f$Button$2e$tsx__$5b$app$2d$client$5d$__$28$ecmascript$29$__["Button"], {
            size: "sm",
            onClick: ()=>void load(),
            children: "Retry"
        }, void 0, false, {
            fileName: "[project]/src/features/admissions/components/GroomAdmissionReview.tsx",
            lineNumber: 117,
            columnNumber: 129
        }, this)
    }, void 0, false, {
        fileName: "[project]/src/features/admissions/components/GroomAdmissionReview.tsx",
        lineNumber: 117,
        columnNumber: 32
    }, this);
    if (!detail?.candidate) return /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$compiled$2f$react$2f$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$client$5d$__$28$ecmascript$29$__["jsxDEV"])(__TURBOPACK__imported__module__$5b$project$5d2f$src$2f$components$2f$ui$2f$states$2e$tsx__$5b$app$2d$client$5d$__$28$ecmascript$29$__["EmptyState"], {
        title: "Application not found",
        description: "This admission record is unavailable.",
        action: /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$compiled$2f$react$2f$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$client$5d$__$28$ecmascript$29$__["jsxDEV"])(__TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$client$2f$app$2d$dir$2f$link$2e$js__$5b$app$2d$client$5d$__$28$ecmascript$29$__["default"], {
            className: "text-sm font-medium text-[var(--color-primary)]",
            href: returnTo,
            children: "Back to applications"
        }, void 0, false, {
            fileName: "[project]/src/features/admissions/components/GroomAdmissionReview.tsx",
            lineNumber: 118,
            columnNumber: 136
        }, this)
    }, void 0, false, {
        fileName: "[project]/src/features/admissions/components/GroomAdmissionReview.tsx",
        lineNumber: 118,
        columnNumber: 34
    }, this);
    const candidate = detail.candidate;
    const horsePhoto = detail.documents.find((doc)=>doc.documentType === 'HORSE_PHOTO');
    const canReview = detail.status === 'GROOM_REVIEW';
    const waiting = detail.status === 'WAITING_FOR_STALL';
    const capacity = detail.capacity;
    const isReady = capacity?.admissionCapacityAvailable ?? false;
    const blockingText = capacity?.blockingReason === 'NO_QUARANTINE_STALL' ? 'No quarantine stall is available.' : capacity?.blockingReason === 'REGULAR_RESERVE_INSUFFICIENT' ? 'Regular-stall reserve is below the required quarantine turnover reserve.' : 'The current capacity snapshot could not be loaded.';
    return /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$compiled$2f$react$2f$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$client$5d$__$28$ecmascript$29$__["jsxDEV"])("div", {
        className: "space-y-5",
        children: [
            notice && /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$compiled$2f$react$2f$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$client$5d$__$28$ecmascript$29$__["jsxDEV"])(__TURBOPACK__imported__module__$5b$project$5d2f$src$2f$components$2f$ui$2f$Notice$2e$tsx__$5b$app$2d$client$5d$__$28$ecmascript$29$__["Notice"], {
                tone: "success",
                children: notice
            }, void 0, false, {
                fileName: "[project]/src/features/admissions/components/GroomAdmissionReview.tsx",
                lineNumber: 134,
                columnNumber: 18
            }, this),
            error && detail && /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$compiled$2f$react$2f$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$client$5d$__$28$ecmascript$29$__["jsxDEV"])(__TURBOPACK__imported__module__$5b$project$5d2f$src$2f$components$2f$ui$2f$Notice$2e$tsx__$5b$app$2d$client$5d$__$28$ecmascript$29$__["Notice"], {
                tone: "error",
                children: error
            }, void 0, false, {
                fileName: "[project]/src/features/admissions/components/GroomAdmissionReview.tsx",
                lineNumber: 135,
                columnNumber: 27
            }, this),
            /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$compiled$2f$react$2f$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$client$5d$__$28$ecmascript$29$__["jsxDEV"])(__TURBOPACK__imported__module__$5b$project$5d2f$src$2f$features$2f$admissions$2f$shared$2f$components$2f$AdmissionDetailLayout$2e$tsx__$5b$app$2d$client$5d$__$28$ecmascript$29$__["AdmissionDetailLayout"], {
                returnTo: returnTo,
                header: /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$compiled$2f$react$2f$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$client$5d$__$28$ecmascript$29$__["jsxDEV"])(__TURBOPACK__imported__module__$5b$project$5d2f$src$2f$features$2f$admissions$2f$shared$2f$components$2f$AdmissionDetailHeader$2e$tsx__$5b$app$2d$client$5d$__$28$ecmascript$29$__["AdmissionDetailHeader"], {
                    detail: detail,
                    horsePhotoUrl: horsePhoto ? __TURBOPACK__imported__module__$5b$project$5d2f$src$2f$features$2f$admissions$2f$services$2f$api$2e$ts__$5b$app$2d$client$5d$__$28$ecmascript$29$__["admissionsApi"].assetUrl(horsePhoto.fileUrl) : undefined
                }, void 0, false, {
                    fileName: "[project]/src/features/admissions/components/GroomAdmissionReview.tsx",
                    lineNumber: 139,
                    columnNumber: 17
                }, this),
                pipeline: /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$compiled$2f$react$2f$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$client$5d$__$28$ecmascript$29$__["jsxDEV"])(__TURBOPACK__imported__module__$5b$project$5d2f$src$2f$features$2f$admissions$2f$shared$2f$components$2f$AdmissionPipeline$2e$tsx__$5b$app$2d$client$5d$__$28$ecmascript$29$__["AdmissionPipeline"], {
                    detail: detail
                }, void 0, false, {
                    fileName: "[project]/src/features/admissions/components/GroomAdmissionReview.tsx",
                    lineNumber: 140,
                    columnNumber: 19
                }, this),
                content: /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$compiled$2f$react$2f$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$client$5d$__$28$ecmascript$29$__["jsxDEV"])(__TURBOPACK__imported__module__$5b$project$5d2f$src$2f$features$2f$admissions$2f$shared$2f$components$2f$AdmissionDetailTabs$2e$tsx__$5b$app$2d$client$5d$__$28$ecmascript$29$__["AdmissionDetailTabs"], {
                    tabs: [
                        {
                            id: 'overview',
                            label: 'Overview',
                            content: /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$compiled$2f$react$2f$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$client$5d$__$28$ecmascript$29$__["jsxDEV"])(__TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$compiled$2f$react$2f$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$client$5d$__$28$ecmascript$29$__["Fragment"], {
                                children: [
                                    /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$compiled$2f$react$2f$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$client$5d$__$28$ecmascript$29$__["jsxDEV"])(__TURBOPACK__imported__module__$5b$project$5d2f$src$2f$features$2f$admissions$2f$shared$2f$components$2f$AdmissionInfoSection$2e$tsx__$5b$app$2d$client$5d$__$28$ecmascript$29$__["AdmissionInfoSection"], {
                                        title: "Horse and owner",
                                        children: [
                                            /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$compiled$2f$react$2f$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$client$5d$__$28$ecmascript$29$__["jsxDEV"])(__TURBOPACK__imported__module__$5b$project$5d2f$src$2f$features$2f$admissions$2f$shared$2f$components$2f$AdmissionInfoSection$2e$tsx__$5b$app$2d$client$5d$__$28$ecmascript$29$__["InfoRow"], {
                                                label: "Horse name",
                                                value: candidate.name
                                            }, void 0, false, {
                                                fileName: "[project]/src/features/admissions/components/GroomAdmissionReview.tsx",
                                                lineNumber: 150,
                                                columnNumber: 23
                                            }, this),
                                            /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$compiled$2f$react$2f$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$client$5d$__$28$ecmascript$29$__["jsxDEV"])(__TURBOPACK__imported__module__$5b$project$5d2f$src$2f$features$2f$admissions$2f$shared$2f$components$2f$AdmissionInfoSection$2e$tsx__$5b$app$2d$client$5d$__$28$ecmascript$29$__["InfoRow"], {
                                                label: "Owner",
                                                value: detail.ownerName ? `${detail.ownerName} (ID ${detail.ownerId})` : `ID ${detail.ownerId}`
                                            }, void 0, false, {
                                                fileName: "[project]/src/features/admissions/components/GroomAdmissionReview.tsx",
                                                lineNumber: 151,
                                                columnNumber: 23
                                            }, this),
                                            /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$compiled$2f$react$2f$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$client$5d$__$28$ecmascript$29$__["jsxDEV"])(__TURBOPACK__imported__module__$5b$project$5d2f$src$2f$features$2f$admissions$2f$shared$2f$components$2f$AdmissionInfoSection$2e$tsx__$5b$app$2d$client$5d$__$28$ecmascript$29$__["InfoRow"], {
                                                label: "Horse ID",
                                                value: detail.horseId ? String(detail.horseId) : 'Created after arrival confirmation'
                                            }, void 0, false, {
                                                fileName: "[project]/src/features/admissions/components/GroomAdmissionReview.tsx",
                                                lineNumber: 152,
                                                columnNumber: 23
                                            }, this),
                                            /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$compiled$2f$react$2f$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$client$5d$__$28$ecmascript$29$__["jsxDEV"])(__TURBOPACK__imported__module__$5b$project$5d2f$src$2f$features$2f$admissions$2f$shared$2f$components$2f$AdmissionInfoSection$2e$tsx__$5b$app$2d$client$5d$__$28$ecmascript$29$__["InfoRow"], {
                                                label: "Groom feedback",
                                                value: detail.groomFeedback
                                            }, void 0, false, {
                                                fileName: "[project]/src/features/admissions/components/GroomAdmissionReview.tsx",
                                                lineNumber: 153,
                                                columnNumber: 23
                                            }, this)
                                        ]
                                    }, void 0, true, {
                                        fileName: "[project]/src/features/admissions/components/GroomAdmissionReview.tsx",
                                        lineNumber: 149,
                                        columnNumber: 21
                                    }, this),
                                    /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$compiled$2f$react$2f$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$client$5d$__$28$ecmascript$29$__["jsxDEV"])(__TURBOPACK__imported__module__$5b$project$5d2f$src$2f$features$2f$admissions$2f$shared$2f$components$2f$AdmissionInfoSection$2e$tsx__$5b$app$2d$client$5d$__$28$ecmascript$29$__["AdmissionInfoSection"], {
                                        title: "Pedigree & Registration",
                                        children: [
                                            /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$compiled$2f$react$2f$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$client$5d$__$28$ecmascript$29$__["jsxDEV"])(__TURBOPACK__imported__module__$5b$project$5d2f$src$2f$features$2f$admissions$2f$shared$2f$components$2f$AdmissionInfoSection$2e$tsx__$5b$app$2d$client$5d$__$28$ecmascript$29$__["InfoRow"], {
                                                label: "Registry name",
                                                value: candidate.registryName
                                            }, void 0, false, {
                                                fileName: "[project]/src/features/admissions/components/GroomAdmissionReview.tsx",
                                                lineNumber: 156,
                                                columnNumber: 23
                                            }, this),
                                            /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$compiled$2f$react$2f$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$client$5d$__$28$ecmascript$29$__["jsxDEV"])(__TURBOPACK__imported__module__$5b$project$5d2f$src$2f$features$2f$admissions$2f$shared$2f$components$2f$AdmissionInfoSection$2e$tsx__$5b$app$2d$client$5d$__$28$ecmascript$29$__["InfoRow"], {
                                                label: "Registration no.",
                                                value: candidate.registrationNumber
                                            }, void 0, false, {
                                                fileName: "[project]/src/features/admissions/components/GroomAdmissionReview.tsx",
                                                lineNumber: 157,
                                                columnNumber: 23
                                            }, this),
                                            /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$compiled$2f$react$2f$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$client$5d$__$28$ecmascript$29$__["jsxDEV"])(__TURBOPACK__imported__module__$5b$project$5d2f$src$2f$features$2f$admissions$2f$shared$2f$components$2f$AdmissionInfoSection$2e$tsx__$5b$app$2d$client$5d$__$28$ecmascript$29$__["InfoRow"], {
                                                label: "Date of birth",
                                                value: date(candidate.dateOfBirth)
                                            }, void 0, false, {
                                                fileName: "[project]/src/features/admissions/components/GroomAdmissionReview.tsx",
                                                lineNumber: 158,
                                                columnNumber: 23
                                            }, this),
                                            /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$compiled$2f$react$2f$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$client$5d$__$28$ecmascript$29$__["jsxDEV"])("div", {
                                                className: "space-y-3 border-t border-[var(--color-border)] pt-3",
                                                children: [
                                                    /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$compiled$2f$react$2f$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$client$5d$__$28$ecmascript$29$__["jsxDEV"])(__TURBOPACK__imported__module__$5b$project$5d2f$src$2f$features$2f$admissions$2f$shared$2f$components$2f$AdmissionInfoSection$2e$tsx__$5b$app$2d$client$5d$__$28$ecmascript$29$__["InfoRow"], {
                                                        label: "Sire",
                                                        value: candidate.sireName
                                                    }, void 0, false, {
                                                        fileName: "[project]/src/features/admissions/components/GroomAdmissionReview.tsx",
                                                        lineNumber: 160,
                                                        columnNumber: 25
                                                    }, this),
                                                    /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$compiled$2f$react$2f$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$client$5d$__$28$ecmascript$29$__["jsxDEV"])(__TURBOPACK__imported__module__$5b$project$5d2f$src$2f$features$2f$admissions$2f$shared$2f$components$2f$AdmissionInfoSection$2e$tsx__$5b$app$2d$client$5d$__$28$ecmascript$29$__["InfoRow"], {
                                                        label: "Dam",
                                                        value: candidate.damName
                                                    }, void 0, false, {
                                                        fileName: "[project]/src/features/admissions/components/GroomAdmissionReview.tsx",
                                                        lineNumber: 161,
                                                        columnNumber: 25
                                                    }, this),
                                                    candidate.pedigreeNotes && /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$compiled$2f$react$2f$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$client$5d$__$28$ecmascript$29$__["jsxDEV"])("p", {
                                                        className: "italic text-[var(--color-text-secondary)]",
                                                        children: [
                                                            '"',
                                                            candidate.pedigreeNotes,
                                                            '"'
                                                        ]
                                                    }, void 0, true, {
                                                        fileName: "[project]/src/features/admissions/components/GroomAdmissionReview.tsx",
                                                        lineNumber: 162,
                                                        columnNumber: 53
                                                    }, this)
                                                ]
                                            }, void 0, true, {
                                                fileName: "[project]/src/features/admissions/components/GroomAdmissionReview.tsx",
                                                lineNumber: 159,
                                                columnNumber: 23
                                            }, this)
                                        ]
                                    }, void 0, true, {
                                        fileName: "[project]/src/features/admissions/components/GroomAdmissionReview.tsx",
                                        lineNumber: 155,
                                        columnNumber: 21
                                    }, this)
                                ]
                            }, void 0, true, {
                                fileName: "[project]/src/features/admissions/components/GroomAdmissionReview.tsx",
                                lineNumber: 148,
                                columnNumber: 19
                            }, this)
                        },
                        {
                            id: 'documents',
                            label: 'Documents',
                            count: detail.documents.length,
                            content: /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$compiled$2f$react$2f$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$client$5d$__$28$ecmascript$29$__["jsxDEV"])(__TURBOPACK__imported__module__$5b$project$5d2f$src$2f$features$2f$admissions$2f$shared$2f$components$2f$AdmissionDocumentsSection$2e$tsx__$5b$app$2d$client$5d$__$28$ecmascript$29$__["AdmissionDocumentsSection"], {
                                documents: detail.documents,
                                assetUrl: __TURBOPACK__imported__module__$5b$project$5d2f$src$2f$features$2f$admissions$2f$services$2f$api$2e$ts__$5b$app$2d$client$5d$__$28$ecmascript$29$__["admissionsApi"].assetUrl
                            }, void 0, false, {
                                fileName: "[project]/src/features/admissions/components/GroomAdmissionReview.tsx",
                                lineNumber: 172,
                                columnNumber: 26
                            }, this)
                        }
                    ]
                }, void 0, false, {
                    fileName: "[project]/src/features/admissions/components/GroomAdmissionReview.tsx",
                    lineNumber: 142,
                    columnNumber: 11
                }, this),
                sidebar: /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$compiled$2f$react$2f$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$client$5d$__$28$ecmascript$29$__["jsxDEV"])(__TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$compiled$2f$react$2f$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$client$5d$__$28$ecmascript$29$__["Fragment"], {
                    children: [
                        /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$compiled$2f$react$2f$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$client$5d$__$28$ecmascript$29$__["jsxDEV"])(__TURBOPACK__imported__module__$5b$project$5d2f$src$2f$features$2f$admissions$2f$shared$2f$components$2f$AdmissionInfoSection$2e$tsx__$5b$app$2d$client$5d$__$28$ecmascript$29$__["AdmissionSideCard"], {
                            title: "Admission capacity",
                            children: [
                                /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$compiled$2f$react$2f$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$client$5d$__$28$ecmascript$29$__["jsxDEV"])(__TURBOPACK__imported__module__$5b$project$5d2f$src$2f$features$2f$admissions$2f$shared$2f$components$2f$AdmissionInfoSection$2e$tsx__$5b$app$2d$client$5d$__$28$ecmascript$29$__["InfoRow"], {
                                    label: "Available quarantine stalls",
                                    value: String(capacity?.availableQuarantineStalls ?? '—')
                                }, void 0, false, {
                                    fileName: "[project]/src/features/admissions/components/GroomAdmissionReview.tsx",
                                    lineNumber: 180,
                                    columnNumber: 15
                                }, this),
                                /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$compiled$2f$react$2f$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$client$5d$__$28$ecmascript$29$__["jsxDEV"])(__TURBOPACK__imported__module__$5b$project$5d2f$src$2f$features$2f$admissions$2f$shared$2f$components$2f$AdmissionInfoSection$2e$tsx__$5b$app$2d$client$5d$__$28$ecmascript$29$__["InfoRow"], {
                                    label: "Available regular stalls",
                                    value: String(capacity?.availableRegularStalls ?? '—')
                                }, void 0, false, {
                                    fileName: "[project]/src/features/admissions/components/GroomAdmissionReview.tsx",
                                    lineNumber: 181,
                                    columnNumber: 15
                                }, this),
                                /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$compiled$2f$react$2f$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$client$5d$__$28$ecmascript$29$__["jsxDEV"])(__TURBOPACK__imported__module__$5b$project$5d2f$src$2f$features$2f$admissions$2f$shared$2f$components$2f$AdmissionInfoSection$2e$tsx__$5b$app$2d$client$5d$__$28$ecmascript$29$__["InfoRow"], {
                                    label: "Occupied quarantine stalls",
                                    value: String(capacity?.occupiedQuarantineStalls ?? '—')
                                }, void 0, false, {
                                    fileName: "[project]/src/features/admissions/components/GroomAdmissionReview.tsx",
                                    lineNumber: 182,
                                    columnNumber: 15
                                }, this),
                                detail.quarantineStallCode && /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$compiled$2f$react$2f$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$client$5d$__$28$ecmascript$29$__["jsxDEV"])(__TURBOPACK__imported__module__$5b$project$5d2f$src$2f$features$2f$admissions$2f$shared$2f$components$2f$AdmissionInfoSection$2e$tsx__$5b$app$2d$client$5d$__$28$ecmascript$29$__["InfoRow"], {
                                    label: "Assigned Q stall",
                                    value: detail.quarantineStallCode
                                }, void 0, false, {
                                    fileName: "[project]/src/features/admissions/components/GroomAdmissionReview.tsx",
                                    lineNumber: 183,
                                    columnNumber: 46
                                }, this),
                                detail.arrivalDeadlineAt && /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$compiled$2f$react$2f$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$client$5d$__$28$ecmascript$29$__["jsxDEV"])(__TURBOPACK__imported__module__$5b$project$5d2f$src$2f$features$2f$admissions$2f$shared$2f$components$2f$AdmissionInfoSection$2e$tsx__$5b$app$2d$client$5d$__$28$ecmascript$29$__["InfoRow"], {
                                    label: "Arrival deadline",
                                    value: (0, __TURBOPACK__imported__module__$5b$project$5d2f$src$2f$lib$2f$display$2e$ts__$5b$app$2d$client$5d$__$28$ecmascript$29$__["formatDateTime"])(detail.arrivalDeadlineAt)
                                }, void 0, false, {
                                    fileName: "[project]/src/features/admissions/components/GroomAdmissionReview.tsx",
                                    lineNumber: 184,
                                    columnNumber: 44
                                }, this),
                                /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$compiled$2f$react$2f$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$client$5d$__$28$ecmascript$29$__["jsxDEV"])(__TURBOPACK__imported__module__$5b$project$5d2f$src$2f$components$2f$ui$2f$Notice$2e$tsx__$5b$app$2d$client$5d$__$28$ecmascript$29$__["Notice"], {
                                    tone: isReady ? 'success' : 'warning',
                                    children: [
                                        isReady ? 'Capacity is sufficient for admission.' : blockingText,
                                        !isReady && capacity && /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$compiled$2f$react$2f$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$client$5d$__$28$ecmascript$29$__["jsxDEV"])("span", {
                                            className: "mt-1 block text-xs",
                                            children: [
                                                "Regular reserve required: ",
                                                capacity.occupiedQuarantineStalls + 1,
                                                " available regular stall(s)."
                                            ]
                                        }, void 0, true, {
                                            fileName: "[project]/src/features/admissions/components/GroomAdmissionReview.tsx",
                                            lineNumber: 188,
                                            columnNumber: 19
                                        }, this)
                                    ]
                                }, void 0, true, {
                                    fileName: "[project]/src/features/admissions/components/GroomAdmissionReview.tsx",
                                    lineNumber: 185,
                                    columnNumber: 15
                                }, this)
                            ]
                        }, void 0, true, {
                            fileName: "[project]/src/features/admissions/components/GroomAdmissionReview.tsx",
                            lineNumber: 179,
                            columnNumber: 13
                        }, this),
                        canReview && /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$compiled$2f$react$2f$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$client$5d$__$28$ecmascript$29$__["jsxDEV"])(__TURBOPACK__imported__module__$5b$project$5d2f$src$2f$features$2f$admissions$2f$shared$2f$components$2f$AdmissionInfoSection$2e$tsx__$5b$app$2d$client$5d$__$28$ecmascript$29$__["AdmissionSideCard"], {
                            title: "Groom decision",
                            tone: "primary",
                            className: "space-y-4",
                            children: [
                                /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$compiled$2f$react$2f$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$client$5d$__$28$ecmascript$29$__["jsxDEV"])(__TURBOPACK__imported__module__$5b$project$5d2f$src$2f$components$2f$ui$2f$FormField$2e$tsx__$5b$app$2d$client$5d$__$28$ecmascript$29$__["FormField"], {
                                    label: "Feedback",
                                    required: true,
                                    error: feedbackError ?? undefined,
                                    hint: `Required for approval and rejection · ${feedback.length}/2000`,
                                    children: /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$compiled$2f$react$2f$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$client$5d$__$28$ecmascript$29$__["jsxDEV"])(__TURBOPACK__imported__module__$5b$project$5d2f$src$2f$components$2f$ui$2f$Input$2e$tsx__$5b$app$2d$client$5d$__$28$ecmascript$29$__["Textarea"], {
                                        value: feedback,
                                        onChange: (event)=>{
                                            setFeedback(event.target.value);
                                            setFeedbackError(null);
                                        },
                                        rows: 4,
                                        maxLength: 2000
                                    }, void 0, false, {
                                        fileName: "[project]/src/features/admissions/components/GroomAdmissionReview.tsx",
                                        lineNumber: 201,
                                        columnNumber: 19
                                    }, this)
                                }, void 0, false, {
                                    fileName: "[project]/src/features/admissions/components/GroomAdmissionReview.tsx",
                                    lineNumber: 195,
                                    columnNumber: 17
                                }, this),
                                /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$compiled$2f$react$2f$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$client$5d$__$28$ecmascript$29$__["jsxDEV"])("div", {
                                    className: "space-y-2",
                                    children: [
                                        /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$compiled$2f$react$2f$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$client$5d$__$28$ecmascript$29$__["jsxDEV"])(__TURBOPACK__imported__module__$5b$project$5d2f$src$2f$components$2f$ui$2f$Button$2e$tsx__$5b$app$2d$client$5d$__$28$ecmascript$29$__["Button"], {
                                            type: "button",
                                            variant: "primary",
                                            className: "w-full",
                                            loading: submitting,
                                            disabled: !feedback.trim() || !isReady,
                                            onClick: ()=>void review('APPROVED'),
                                            children: "Approve"
                                        }, void 0, false, {
                                            fileName: "[project]/src/features/admissions/components/GroomAdmissionReview.tsx",
                                            lineNumber: 209,
                                            columnNumber: 19
                                        }, this),
                                        /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$compiled$2f$react$2f$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$client$5d$__$28$ecmascript$29$__["jsxDEV"])(__TURBOPACK__imported__module__$5b$project$5d2f$src$2f$components$2f$ui$2f$Button$2e$tsx__$5b$app$2d$client$5d$__$28$ecmascript$29$__["Button"], {
                                            type: "button",
                                            variant: "secondary",
                                            className: "w-full",
                                            loading: submitting,
                                            disabled: !feedback.trim() || isReady,
                                            onClick: ()=>void review('APPROVED'),
                                            children: "Approve & wait for stall"
                                        }, void 0, false, {
                                            fileName: "[project]/src/features/admissions/components/GroomAdmissionReview.tsx",
                                            lineNumber: 210,
                                            columnNumber: 19
                                        }, this),
                                        /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$compiled$2f$react$2f$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$client$5d$__$28$ecmascript$29$__["jsxDEV"])(__TURBOPACK__imported__module__$5b$project$5d2f$src$2f$components$2f$ui$2f$Button$2e$tsx__$5b$app$2d$client$5d$__$28$ecmascript$29$__["Button"], {
                                            type: "button",
                                            variant: "destructive",
                                            className: "w-full",
                                            loading: submitting,
                                            disabled: !feedback.trim(),
                                            onClick: ()=>void review('REJECTED'),
                                            children: "Reject"
                                        }, void 0, false, {
                                            fileName: "[project]/src/features/admissions/components/GroomAdmissionReview.tsx",
                                            lineNumber: 211,
                                            columnNumber: 19
                                        }, this)
                                    ]
                                }, void 0, true, {
                                    fileName: "[project]/src/features/admissions/components/GroomAdmissionReview.tsx",
                                    lineNumber: 208,
                                    columnNumber: 17
                                }, this)
                            ]
                        }, void 0, true, {
                            fileName: "[project]/src/features/admissions/components/GroomAdmissionReview.tsx",
                            lineNumber: 194,
                            columnNumber: 15
                        }, this),
                        waiting && /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$compiled$2f$react$2f$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$client$5d$__$28$ecmascript$29$__["jsxDEV"])(__TURBOPACK__imported__module__$5b$project$5d2f$src$2f$features$2f$admissions$2f$shared$2f$components$2f$AdmissionInfoSection$2e$tsx__$5b$app$2d$client$5d$__$28$ecmascript$29$__["AdmissionSideCard"], {
                            title: "Waiting for stall",
                            tone: "warning",
                            children: [
                                /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$compiled$2f$react$2f$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$client$5d$__$28$ecmascript$29$__["jsxDEV"])("p", {
                                    className: "text-[var(--color-text-secondary)]",
                                    children: "Groom approved this admission, but capacity was unavailable. Retry when a quarantine stall becomes available."
                                }, void 0, false, {
                                    fileName: "[project]/src/features/admissions/components/GroomAdmissionReview.tsx",
                                    lineNumber: 218,
                                    columnNumber: 17
                                }, this),
                                /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$compiled$2f$react$2f$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$client$5d$__$28$ecmascript$29$__["jsxDEV"])(__TURBOPACK__imported__module__$5b$project$5d2f$src$2f$features$2f$admissions$2f$shared$2f$components$2f$AdmissionInfoSection$2e$tsx__$5b$app$2d$client$5d$__$28$ecmascript$29$__["InfoRow"], {
                                    label: "Feedback",
                                    value: detail.groomFeedback || 'Not recorded'
                                }, void 0, false, {
                                    fileName: "[project]/src/features/admissions/components/GroomAdmissionReview.tsx",
                                    lineNumber: 219,
                                    columnNumber: 17
                                }, this),
                                /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$compiled$2f$react$2f$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$client$5d$__$28$ecmascript$29$__["jsxDEV"])(__TURBOPACK__imported__module__$5b$project$5d2f$src$2f$components$2f$ui$2f$Button$2e$tsx__$5b$app$2d$client$5d$__$28$ecmascript$29$__["Button"], {
                                    type: "button",
                                    variant: "warning",
                                    className: "w-full",
                                    loading: submitting,
                                    onClick: ()=>void retryAllocation(),
                                    children: "Retry allocation"
                                }, void 0, false, {
                                    fileName: "[project]/src/features/admissions/components/GroomAdmissionReview.tsx",
                                    lineNumber: 220,
                                    columnNumber: 17
                                }, this)
                            ]
                        }, void 0, true, {
                            fileName: "[project]/src/features/admissions/components/GroomAdmissionReview.tsx",
                            lineNumber: 217,
                            columnNumber: 15
                        }, this),
                        detail.status === 'WAITING_FOR_ARRIVAL' && /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$compiled$2f$react$2f$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$client$5d$__$28$ecmascript$29$__["jsxDEV"])(__TURBOPACK__imported__module__$5b$project$5d2f$src$2f$features$2f$admissions$2f$shared$2f$components$2f$AdmissionInfoSection$2e$tsx__$5b$app$2d$client$5d$__$28$ecmascript$29$__["AdmissionSideCard"], {
                            title: "Confirm horse arrival",
                            tone: "primary",
                            children: [
                                /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$compiled$2f$react$2f$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$client$5d$__$28$ecmascript$29$__["jsxDEV"])("p", {
                                    className: "text-[var(--color-text-secondary)]",
                                    children: [
                                        "Quarantine stall ",
                                        detail.quarantineStallCode || 'reserved',
                                        " is held until ",
                                        detail.arrivalDeadlineAt ? (0, __TURBOPACK__imported__module__$5b$project$5d2f$src$2f$lib$2f$display$2e$ts__$5b$app$2d$client$5d$__$28$ecmascript$29$__["formatDateTime"])(detail.arrivalDeadlineAt) : 'the 14-day arrival deadline',
                                        ". Confirm only after the horse physically arrives. Confirmation creates the horse record and schedules veterinary review."
                                    ]
                                }, void 0, true, {
                                    fileName: "[project]/src/features/admissions/components/GroomAdmissionReview.tsx",
                                    lineNumber: 226,
                                    columnNumber: 17
                                }, this),
                                /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$compiled$2f$react$2f$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$client$5d$__$28$ecmascript$29$__["jsxDEV"])(__TURBOPACK__imported__module__$5b$project$5d2f$src$2f$components$2f$ui$2f$Button$2e$tsx__$5b$app$2d$client$5d$__$28$ecmascript$29$__["Button"], {
                                    type: "button",
                                    variant: "primary",
                                    className: "w-full",
                                    loading: submitting,
                                    onClick: ()=>void confirmArrival(),
                                    children: "Mark horse arrived"
                                }, void 0, false, {
                                    fileName: "[project]/src/features/admissions/components/GroomAdmissionReview.tsx",
                                    lineNumber: 230,
                                    columnNumber: 17
                                }, this)
                            ]
                        }, void 0, true, {
                            fileName: "[project]/src/features/admissions/components/GroomAdmissionReview.tsx",
                            lineNumber: 225,
                            columnNumber: 15
                        }, this),
                        detail.status === 'ARRIVAL_EXPIRED' && /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$compiled$2f$react$2f$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$client$5d$__$28$ecmascript$29$__["jsxDEV"])(__TURBOPACK__imported__module__$5b$project$5d2f$src$2f$features$2f$admissions$2f$shared$2f$components$2f$AdmissionInfoSection$2e$tsx__$5b$app$2d$client$5d$__$28$ecmascript$29$__["AdmissionSideCard"], {
                            title: "Arrival window expired",
                            tone: "warning",
                            children: /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$compiled$2f$react$2f$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$client$5d$__$28$ecmascript$29$__["jsxDEV"])("p", {
                                className: "text-[var(--color-text-secondary)]",
                                children: "The reserved quarantine stall was released after 14 days. An authorized manager can reopen the arrival window if needed."
                            }, void 0, false, {
                                fileName: "[project]/src/features/admissions/components/GroomAdmissionReview.tsx",
                                lineNumber: 236,
                                columnNumber: 17
                            }, this)
                        }, void 0, false, {
                            fileName: "[project]/src/features/admissions/components/GroomAdmissionReview.tsx",
                            lineNumber: 235,
                            columnNumber: 15
                        }, this),
                        !canReview && !waiting && detail.status !== 'WAITING_FOR_ARRIVAL' && detail.status !== 'ARRIVAL_EXPIRED' && /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$compiled$2f$react$2f$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$client$5d$__$28$ecmascript$29$__["jsxDEV"])(__TURBOPACK__imported__module__$5b$project$5d2f$src$2f$features$2f$admissions$2f$shared$2f$components$2f$AdmissionInfoSection$2e$tsx__$5b$app$2d$client$5d$__$28$ecmascript$29$__["AdmissionSideCard"], {
                            title: "Groom decision",
                            children: /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$compiled$2f$react$2f$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$client$5d$__$28$ecmascript$29$__["jsxDEV"])("p", {
                                className: "text-[var(--color-text-secondary)]",
                                children: "This application is read-only at its current stage."
                            }, void 0, false, {
                                fileName: "[project]/src/features/admissions/components/GroomAdmissionReview.tsx",
                                lineNumber: 242,
                                columnNumber: 17
                            }, this)
                        }, void 0, false, {
                            fileName: "[project]/src/features/admissions/components/GroomAdmissionReview.tsx",
                            lineNumber: 241,
                            columnNumber: 15
                        }, this)
                    ]
                }, void 0, true, {
                    fileName: "[project]/src/features/admissions/components/GroomAdmissionReview.tsx",
                    lineNumber: 178,
                    columnNumber: 11
                }, this)
            }, void 0, false, {
                fileName: "[project]/src/features/admissions/components/GroomAdmissionReview.tsx",
                lineNumber: 137,
                columnNumber: 7
            }, this)
        ]
    }, void 0, true, {
        fileName: "[project]/src/features/admissions/components/GroomAdmissionReview.tsx",
        lineNumber: 133,
        columnNumber: 5
    }, this);
}
_s(GroomAdmissionReview, "HUoCvVk324uEkKr/9ZSIsM+BXU4=");
_c = GroomAdmissionReview;
var _c;
__turbopack_context__.k.register(_c, "GroomAdmissionReview");
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
"[project]/src/features/admissions/shared/components/AdmissionDetailTabs.tsx [app-client] (ecmascript)", ((__turbopack_context__) => {
"use strict";

__turbopack_context__.s([
    "AdmissionDetailTabs",
    ()=>AdmissionDetailTabs
]);
var __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$compiled$2f$react$2f$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$client$5d$__$28$ecmascript$29$__ = __turbopack_context__.i("[project]/node_modules/next/dist/compiled/react/jsx-dev-runtime.js [app-client] (ecmascript)");
var __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$compiled$2f$react$2f$index$2e$js__$5b$app$2d$client$5d$__$28$ecmascript$29$__ = __turbopack_context__.i("[project]/node_modules/next/dist/compiled/react/index.js [app-client] (ecmascript)");
var __TURBOPACK__imported__module__$5b$project$5d2f$src$2f$components$2f$ui$2f$Tabs$2e$tsx__$5b$app$2d$client$5d$__$28$ecmascript$29$__ = __turbopack_context__.i("[project]/src/components/ui/Tabs.tsx [app-client] (ecmascript)");
;
var _s = __turbopack_context__.k.signature();
'use client';
;
;
function AdmissionDetailTabs({ tabs }) {
    _s();
    const [active, setActive] = (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$compiled$2f$react$2f$index$2e$js__$5b$app$2d$client$5d$__$28$ecmascript$29$__["useState"])(tabs[0]?.id);
    const current = tabs.find((tab)=>tab.id === active) ?? tabs[0];
    return /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$compiled$2f$react$2f$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$client$5d$__$28$ecmascript$29$__["jsxDEV"])("div", {
        className: "min-w-0 space-y-5",
        children: [
            /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$compiled$2f$react$2f$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$client$5d$__$28$ecmascript$29$__["jsxDEV"])(__TURBOPACK__imported__module__$5b$project$5d2f$src$2f$components$2f$ui$2f$Tabs$2e$tsx__$5b$app$2d$client$5d$__$28$ecmascript$29$__["Tabs"], {
                tabs: tabs.map(({ id, label, count })=>({
                        id,
                        label,
                        count
                    })),
                active: current.id,
                onChange: setActive
            }, void 0, false, {
                fileName: "[project]/src/features/admissions/shared/components/AdmissionDetailTabs.tsx",
                lineNumber: 20,
                columnNumber: 7
            }, this),
            /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$compiled$2f$react$2f$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$client$5d$__$28$ecmascript$29$__["jsxDEV"])("div", {
                className: "space-y-5 animate-in fade-in slide-in-from-bottom-1 duration-200",
                children: current.content
            }, current.id, false, {
                fileName: "[project]/src/features/admissions/shared/components/AdmissionDetailTabs.tsx",
                lineNumber: 21,
                columnNumber: 7
            }, this)
        ]
    }, void 0, true, {
        fileName: "[project]/src/features/admissions/shared/components/AdmissionDetailTabs.tsx",
        lineNumber: 19,
        columnNumber: 5
    }, this);
}
_s(AdmissionDetailTabs, "AXDab7SDVdSf6xugHOOBV5FSNUc=");
_c = AdmissionDetailTabs;
var _c;
__turbopack_context__.k.register(_c, "AdmissionDetailTabs");
if (typeof globalThis.$RefreshHelpers$ === 'object' && globalThis.$RefreshHelpers !== null) {
    __turbopack_context__.k.registerExports(__turbopack_context__.m, globalThis.$RefreshHelpers$);
}
}),
"[project]/src/features/admissions/shared/components/AdmissionDocumentPreview.tsx [app-client] (ecmascript)", ((__turbopack_context__) => {
"use strict";

__turbopack_context__.s([
    "AdmissionDocumentPreview",
    ()=>AdmissionDocumentPreview
]);
var __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$compiled$2f$react$2f$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$client$5d$__$28$ecmascript$29$__ = __turbopack_context__.i("[project]/node_modules/next/dist/compiled/react/jsx-dev-runtime.js [app-client] (ecmascript)");
var __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$compiled$2f$react$2f$index$2e$js__$5b$app$2d$client$5d$__$28$ecmascript$29$__ = __turbopack_context__.i("[project]/node_modules/next/dist/compiled/react/index.js [app-client] (ecmascript)");
var __TURBOPACK__imported__module__$5b$project$5d2f$src$2f$components$2f$ui$2f$Button$2e$tsx__$5b$app$2d$client$5d$__$28$ecmascript$29$__ = __turbopack_context__.i("[project]/src/components/ui/Button.tsx [app-client] (ecmascript)");
var __TURBOPACK__imported__module__$5b$project$5d2f$src$2f$components$2f$ui$2f$Icon$2e$tsx__$5b$app$2d$client$5d$__$28$ecmascript$29$__ = __turbopack_context__.i("[project]/src/components/ui/Icon.tsx [app-client] (ecmascript)");
var __TURBOPACK__imported__module__$5b$project$5d2f$src$2f$features$2f$admissions$2f$services$2f$api$2e$ts__$5b$app$2d$client$5d$__$28$ecmascript$29$__ = __turbopack_context__.i("[project]/src/features/admissions/services/api.ts [app-client] (ecmascript)");
;
var _s = __turbopack_context__.k.signature();
'use client';
;
;
;
;
function AdmissionDocumentPreview({ document, label, fileUrl, onClose }) {
    _s();
    const dialogRef = (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$compiled$2f$react$2f$index$2e$js__$5b$app$2d$client$5d$__$28$ecmascript$29$__["useRef"])(null);
    const titleId = (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$compiled$2f$react$2f$index$2e$js__$5b$app$2d$client$5d$__$28$ecmascript$29$__["useId"])();
    const [file, setFile] = (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$compiled$2f$react$2f$index$2e$js__$5b$app$2d$client$5d$__$28$ecmascript$29$__["useState"])(null);
    const [error, setError] = (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$compiled$2f$react$2f$index$2e$js__$5b$app$2d$client$5d$__$28$ecmascript$29$__["useState"])(null);
    const [zoom, setZoom] = (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$compiled$2f$react$2f$index$2e$js__$5b$app$2d$client$5d$__$28$ecmascript$29$__["useState"])(100);
    const filename = document.originalFileName || label;
    (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$compiled$2f$react$2f$index$2e$js__$5b$app$2d$client$5d$__$28$ecmascript$29$__["useEffect"])({
        "AdmissionDocumentPreview.useEffect": ()=>{
            const dialog = dialogRef.current;
            const previousFocus = window.document.activeElement;
            const previousOverflow = window.document.body.style.overflow;
            dialog.showModal();
            window.document.body.style.overflow = 'hidden';
            return ({
                "AdmissionDocumentPreview.useEffect": ()=>{
                    dialog.close();
                    window.document.body.style.overflow = previousOverflow;
                    if (previousFocus instanceof HTMLElement) previousFocus.focus();
                }
            })["AdmissionDocumentPreview.useEffect"];
        }
    }["AdmissionDocumentPreview.useEffect"], []);
    (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$compiled$2f$react$2f$index$2e$js__$5b$app$2d$client$5d$__$28$ecmascript$29$__["useEffect"])({
        "AdmissionDocumentPreview.useEffect": ()=>{
            const controller = new AbortController();
            let objectUrl;
            __TURBOPACK__imported__module__$5b$project$5d2f$src$2f$features$2f$admissions$2f$services$2f$api$2e$ts__$5b$app$2d$client$5d$__$28$ecmascript$29$__["admissionsApi"].getDocumentFile(document.fileUrl, controller.signal).then({
                "AdmissionDocumentPreview.useEffect": (blob)=>{
                    if (controller.signal.aborted) return;
                    objectUrl = URL.createObjectURL(blob);
                    const kind = [
                        'image/jpeg',
                        'image/png',
                        'image/webp'
                    ].includes(blob.type) ? 'image' : blob.type === 'application/pdf' ? 'pdf' : 'unsupported';
                    setFile({
                        url: objectUrl,
                        kind
                    });
                }
            }["AdmissionDocumentPreview.useEffect"]).catch({
                "AdmissionDocumentPreview.useEffect": ()=>{
                    if (!controller.signal.aborted) setError('Unable to load this file. Try opening it in a new tab.');
                }
            }["AdmissionDocumentPreview.useEffect"]);
            return ({
                "AdmissionDocumentPreview.useEffect": ()=>{
                    controller.abort();
                    if (objectUrl) URL.revokeObjectURL(objectUrl);
                }
            })["AdmissionDocumentPreview.useEffect"];
        }
    }["AdmissionDocumentPreview.useEffect"], [
        document.fileUrl
    ]);
    const actionClass = 'inline-flex h-9 items-center justify-center gap-1.5 rounded-[var(--radius-sm)] border border-[var(--color-border-strong)] bg-[var(--color-surface)] px-3 text-sm font-medium text-[var(--color-text-primary)] hover:bg-[var(--color-surface-muted)] focus-visible:outline-2 focus-visible:outline-[var(--color-focus)]';
    return /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$compiled$2f$react$2f$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$client$5d$__$28$ecmascript$29$__["jsxDEV"])("dialog", {
        ref: dialogRef,
        "aria-labelledby": titleId,
        onCancel: (event)=>{
            event.preventDefault();
            onClose();
        },
        onClick: (event)=>{
            if (event.target === event.currentTarget) onClose();
        },
        className: "fixed inset-0 m-auto h-dvh max-h-dvh w-screen max-w-none overflow-hidden border border-[var(--color-border)] bg-[var(--color-surface)] p-0 text-[var(--color-text-primary)] shadow-[var(--shadow-popover)] backdrop:bg-black/50 sm:h-[90dvh] sm:w-[calc(100%_-_3rem)] sm:max-w-5xl sm:rounded-[var(--radius-lg)]",
        children: /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$compiled$2f$react$2f$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$client$5d$__$28$ecmascript$29$__["jsxDEV"])("div", {
            className: "flex h-full min-h-0 flex-col",
            children: [
                /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$compiled$2f$react$2f$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$client$5d$__$28$ecmascript$29$__["jsxDEV"])("header", {
                    className: "flex shrink-0 items-start gap-3 border-b border-[var(--color-border)] px-4 py-3 sm:px-5",
                    children: [
                        /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$compiled$2f$react$2f$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$client$5d$__$28$ecmascript$29$__["jsxDEV"])(__TURBOPACK__imported__module__$5b$project$5d2f$src$2f$components$2f$ui$2f$Icon$2e$tsx__$5b$app$2d$client$5d$__$28$ecmascript$29$__["Icon"], {
                            name: "file-text",
                            size: 20,
                            className: "mt-1 shrink-0 text-[var(--color-text-muted)]"
                        }, void 0, false, {
                            fileName: "[project]/src/features/admissions/shared/components/AdmissionDocumentPreview.tsx",
                            lineNumber: 67,
                            columnNumber: 11
                        }, this),
                        /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$compiled$2f$react$2f$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$client$5d$__$28$ecmascript$29$__["jsxDEV"])("div", {
                            className: "min-w-0 flex-1",
                            children: [
                                /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$compiled$2f$react$2f$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$client$5d$__$28$ecmascript$29$__["jsxDEV"])("h2", {
                                    id: titleId,
                                    className: "break-words text-lg font-semibold",
                                    children: filename
                                }, void 0, false, {
                                    fileName: "[project]/src/features/admissions/shared/components/AdmissionDocumentPreview.tsx",
                                    lineNumber: 69,
                                    columnNumber: 13
                                }, this),
                                /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$compiled$2f$react$2f$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$client$5d$__$28$ecmascript$29$__["jsxDEV"])("p", {
                                    className: "mt-0.5 text-xs text-[var(--color-text-muted)]",
                                    children: label
                                }, void 0, false, {
                                    fileName: "[project]/src/features/admissions/shared/components/AdmissionDocumentPreview.tsx",
                                    lineNumber: 70,
                                    columnNumber: 13
                                }, this)
                            ]
                        }, void 0, true, {
                            fileName: "[project]/src/features/admissions/shared/components/AdmissionDocumentPreview.tsx",
                            lineNumber: 68,
                            columnNumber: 11
                        }, this),
                        /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$compiled$2f$react$2f$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$client$5d$__$28$ecmascript$29$__["jsxDEV"])(__TURBOPACK__imported__module__$5b$project$5d2f$src$2f$components$2f$ui$2f$Button$2e$tsx__$5b$app$2d$client$5d$__$28$ecmascript$29$__["Button"], {
                            type: "button",
                            variant: "tertiary",
                            icon: "x",
                            "aria-label": "Close preview",
                            onClick: onClose
                        }, void 0, false, {
                            fileName: "[project]/src/features/admissions/shared/components/AdmissionDocumentPreview.tsx",
                            lineNumber: 72,
                            columnNumber: 11
                        }, this)
                    ]
                }, void 0, true, {
                    fileName: "[project]/src/features/admissions/shared/components/AdmissionDocumentPreview.tsx",
                    lineNumber: 66,
                    columnNumber: 9
                }, this),
                /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$compiled$2f$react$2f$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$client$5d$__$28$ecmascript$29$__["jsxDEV"])("div", {
                    className: "flex shrink-0 flex-wrap items-center gap-2 border-b border-[var(--color-border)] px-4 py-2 sm:px-5",
                    children: [
                        file && /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$compiled$2f$react$2f$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$client$5d$__$28$ecmascript$29$__["jsxDEV"])("a", {
                            href: file.url,
                            download: filename,
                            className: actionClass,
                            children: [
                                /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$compiled$2f$react$2f$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$client$5d$__$28$ecmascript$29$__["jsxDEV"])(__TURBOPACK__imported__module__$5b$project$5d2f$src$2f$components$2f$ui$2f$Icon$2e$tsx__$5b$app$2d$client$5d$__$28$ecmascript$29$__["Icon"], {
                                    name: "download"
                                }, void 0, false, {
                                    fileName: "[project]/src/features/admissions/shared/components/AdmissionDocumentPreview.tsx",
                                    lineNumber: 76,
                                    columnNumber: 83
                                }, this),
                                "Download"
                            ]
                        }, void 0, true, {
                            fileName: "[project]/src/features/admissions/shared/components/AdmissionDocumentPreview.tsx",
                            lineNumber: 76,
                            columnNumber: 20
                        }, this),
                        /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$compiled$2f$react$2f$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$client$5d$__$28$ecmascript$29$__["jsxDEV"])("a", {
                            href: file && file.kind !== 'unsupported' ? file.url : fileUrl,
                            target: "_blank",
                            rel: "noreferrer",
                            className: actionClass,
                            children: [
                                /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$compiled$2f$react$2f$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$client$5d$__$28$ecmascript$29$__["jsxDEV"])(__TURBOPACK__imported__module__$5b$project$5d2f$src$2f$components$2f$ui$2f$Icon$2e$tsx__$5b$app$2d$client$5d$__$28$ecmascript$29$__["Icon"], {
                                    name: "external-link"
                                }, void 0, false, {
                                    fileName: "[project]/src/features/admissions/shared/components/AdmissionDocumentPreview.tsx",
                                    lineNumber: 77,
                                    columnNumber: 135
                                }, this),
                                "Open in new tab"
                            ]
                        }, void 0, true, {
                            fileName: "[project]/src/features/admissions/shared/components/AdmissionDocumentPreview.tsx",
                            lineNumber: 77,
                            columnNumber: 11
                        }, this),
                        file?.kind === 'image' && !error && /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$compiled$2f$react$2f$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$client$5d$__$28$ecmascript$29$__["jsxDEV"])("div", {
                            className: "ml-auto flex items-center gap-1",
                            children: [
                                /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$compiled$2f$react$2f$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$client$5d$__$28$ecmascript$29$__["jsxDEV"])(__TURBOPACK__imported__module__$5b$project$5d2f$src$2f$components$2f$ui$2f$Button$2e$tsx__$5b$app$2d$client$5d$__$28$ecmascript$29$__["Button"], {
                                    type: "button",
                                    size: "sm",
                                    variant: "tertiary",
                                    icon: "minus",
                                    "aria-label": "Zoom out",
                                    disabled: zoom === 100,
                                    onClick: ()=>setZoom((value)=>value - 25)
                                }, void 0, false, {
                                    fileName: "[project]/src/features/admissions/shared/components/AdmissionDocumentPreview.tsx",
                                    lineNumber: 80,
                                    columnNumber: 15
                                }, this),
                                /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$compiled$2f$react$2f$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$client$5d$__$28$ecmascript$29$__["jsxDEV"])("span", {
                                    className: "w-11 text-center text-xs",
                                    "aria-live": "polite",
                                    children: [
                                        zoom,
                                        "%"
                                    ]
                                }, void 0, true, {
                                    fileName: "[project]/src/features/admissions/shared/components/AdmissionDocumentPreview.tsx",
                                    lineNumber: 81,
                                    columnNumber: 15
                                }, this),
                                /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$compiled$2f$react$2f$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$client$5d$__$28$ecmascript$29$__["jsxDEV"])(__TURBOPACK__imported__module__$5b$project$5d2f$src$2f$components$2f$ui$2f$Button$2e$tsx__$5b$app$2d$client$5d$__$28$ecmascript$29$__["Button"], {
                                    type: "button",
                                    size: "sm",
                                    variant: "tertiary",
                                    icon: "plus",
                                    "aria-label": "Zoom in",
                                    disabled: zoom === 200,
                                    onClick: ()=>setZoom((value)=>value + 25)
                                }, void 0, false, {
                                    fileName: "[project]/src/features/admissions/shared/components/AdmissionDocumentPreview.tsx",
                                    lineNumber: 82,
                                    columnNumber: 15
                                }, this),
                                /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$compiled$2f$react$2f$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$client$5d$__$28$ecmascript$29$__["jsxDEV"])(__TURBOPACK__imported__module__$5b$project$5d2f$src$2f$components$2f$ui$2f$Button$2e$tsx__$5b$app$2d$client$5d$__$28$ecmascript$29$__["Button"], {
                                    type: "button",
                                    size: "sm",
                                    variant: "tertiary",
                                    onClick: ()=>setZoom(100),
                                    children: "Fit"
                                }, void 0, false, {
                                    fileName: "[project]/src/features/admissions/shared/components/AdmissionDocumentPreview.tsx",
                                    lineNumber: 83,
                                    columnNumber: 15
                                }, this)
                            ]
                        }, void 0, true, {
                            fileName: "[project]/src/features/admissions/shared/components/AdmissionDocumentPreview.tsx",
                            lineNumber: 79,
                            columnNumber: 13
                        }, this)
                    ]
                }, void 0, true, {
                    fileName: "[project]/src/features/admissions/shared/components/AdmissionDocumentPreview.tsx",
                    lineNumber: 75,
                    columnNumber: 9
                }, this),
                /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$compiled$2f$react$2f$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$client$5d$__$28$ecmascript$29$__["jsxDEV"])("div", {
                    className: "min-h-0 flex-1 overflow-auto overscroll-contain bg-[var(--color-surface-muted)]",
                    "aria-busy": !file && !error,
                    children: error ? /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$compiled$2f$react$2f$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$client$5d$__$28$ecmascript$29$__["jsxDEV"])("div", {
                        role: "alert",
                        className: "flex h-full items-center justify-center p-6 text-center text-sm",
                        children: error
                    }, void 0, false, {
                        fileName: "[project]/src/features/admissions/shared/components/AdmissionDocumentPreview.tsx",
                        lineNumber: 90,
                        columnNumber: 13
                    }, this) : !file ? /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$compiled$2f$react$2f$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$client$5d$__$28$ecmascript$29$__["jsxDEV"])("div", {
                        role: "status",
                        className: "flex h-full items-center justify-center gap-2 text-sm text-[var(--color-text-muted)]",
                        children: [
                            /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$compiled$2f$react$2f$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$client$5d$__$28$ecmascript$29$__["jsxDEV"])("span", {
                                className: "h-4 w-4 animate-spin rounded-full border-2 border-current border-t-transparent",
                                "aria-hidden": "true"
                            }, void 0, false, {
                                fileName: "[project]/src/features/admissions/shared/components/AdmissionDocumentPreview.tsx",
                                lineNumber: 92,
                                columnNumber: 129
                            }, this),
                            "Loading document…"
                        ]
                    }, void 0, true, {
                        fileName: "[project]/src/features/admissions/shared/components/AdmissionDocumentPreview.tsx",
                        lineNumber: 92,
                        columnNumber: 13
                    }, this) : file.kind === 'image' ? /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$compiled$2f$react$2f$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$client$5d$__$28$ecmascript$29$__["jsxDEV"])("div", {
                        className: "relative",
                        style: {
                            width: `${zoom}%`,
                            height: `${zoom}%`
                        },
                        children: /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$compiled$2f$react$2f$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$client$5d$__$28$ecmascript$29$__["jsxDEV"])("img", {
                            src: file.url,
                            alt: filename,
                            width: 1600,
                            height: 1000,
                            className: "h-full w-full object-contain p-3",
                            onError: ()=>setError('This image could not be displayed. Download it or open it in a new tab.')
                        }, void 0, false, {
                            fileName: "[project]/src/features/admissions/shared/components/AdmissionDocumentPreview.tsx",
                            lineNumber: 97,
                            columnNumber: 15
                        }, this)
                    }, void 0, false, {
                        fileName: "[project]/src/features/admissions/shared/components/AdmissionDocumentPreview.tsx",
                        lineNumber: 94,
                        columnNumber: 13
                    }, this) : file.kind === 'pdf' ? /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$compiled$2f$react$2f$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$client$5d$__$28$ecmascript$29$__["jsxDEV"])("div", {
                        className: "flex h-full flex-col",
                        children: [
                            /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$compiled$2f$react$2f$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$client$5d$__$28$ecmascript$29$__["jsxDEV"])("p", {
                                className: "shrink-0 px-4 py-2 text-xs text-[var(--color-text-muted)]",
                                children: "If the PDF viewer is unavailable, download the file or open it in a new tab."
                            }, void 0, false, {
                                fileName: "[project]/src/features/admissions/shared/components/AdmissionDocumentPreview.tsx",
                                lineNumber: 101,
                                columnNumber: 15
                            }, this),
                            /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$compiled$2f$react$2f$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$client$5d$__$28$ecmascript$29$__["jsxDEV"])("iframe", {
                                src: file.url,
                                title: `Preview of ${filename}`,
                                className: "min-h-0 w-full flex-1 border-0",
                                onError: ()=>setError('This PDF could not be displayed. Download it or open it in a new tab.')
                            }, void 0, false, {
                                fileName: "[project]/src/features/admissions/shared/components/AdmissionDocumentPreview.tsx",
                                lineNumber: 102,
                                columnNumber: 15
                            }, this)
                        ]
                    }, void 0, true, {
                        fileName: "[project]/src/features/admissions/shared/components/AdmissionDocumentPreview.tsx",
                        lineNumber: 100,
                        columnNumber: 13
                    }, this) : /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$compiled$2f$react$2f$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$client$5d$__$28$ecmascript$29$__["jsxDEV"])("div", {
                        className: "flex h-full items-center justify-center p-6 text-center text-sm",
                        children: "Preview is available for JPEG, PNG, WebP and PDF files. Download this file or open it in a new tab."
                    }, void 0, false, {
                        fileName: "[project]/src/features/admissions/shared/components/AdmissionDocumentPreview.tsx",
                        lineNumber: 105,
                        columnNumber: 13
                    }, this)
                }, void 0, false, {
                    fileName: "[project]/src/features/admissions/shared/components/AdmissionDocumentPreview.tsx",
                    lineNumber: 88,
                    columnNumber: 9
                }, this),
                document.note && /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$compiled$2f$react$2f$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$client$5d$__$28$ecmascript$29$__["jsxDEV"])("p", {
                    className: "max-h-24 shrink-0 overflow-auto whitespace-pre-wrap break-words border-t border-[var(--color-border)] px-4 py-3 text-xs text-[var(--color-text-secondary)]",
                    children: [
                        /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$compiled$2f$react$2f$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$client$5d$__$28$ecmascript$29$__["jsxDEV"])("span", {
                            className: "font-medium",
                            children: "Note: "
                        }, void 0, false, {
                            fileName: "[project]/src/features/admissions/shared/components/AdmissionDocumentPreview.tsx",
                            lineNumber: 108,
                            columnNumber: 197
                        }, this),
                        document.note
                    ]
                }, void 0, true, {
                    fileName: "[project]/src/features/admissions/shared/components/AdmissionDocumentPreview.tsx",
                    lineNumber: 108,
                    columnNumber: 27
                }, this)
            ]
        }, void 0, true, {
            fileName: "[project]/src/features/admissions/shared/components/AdmissionDocumentPreview.tsx",
            lineNumber: 65,
            columnNumber: 7
        }, this)
    }, void 0, false, {
        fileName: "[project]/src/features/admissions/shared/components/AdmissionDocumentPreview.tsx",
        lineNumber: 58,
        columnNumber: 5
    }, this);
}
_s(AdmissionDocumentPreview, "dsMZdFkuzufnpbjqc38qxqFlxAE=", false, function() {
    return [
        __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$compiled$2f$react$2f$index$2e$js__$5b$app$2d$client$5d$__$28$ecmascript$29$__["useId"]
    ];
});
_c = AdmissionDocumentPreview;
var _c;
__turbopack_context__.k.register(_c, "AdmissionDocumentPreview");
if (typeof globalThis.$RefreshHelpers$ === 'object' && globalThis.$RefreshHelpers !== null) {
    __turbopack_context__.k.registerExports(__turbopack_context__.m, globalThis.$RefreshHelpers$);
}
}),
"[project]/src/features/admissions/shared/components/AdmissionDocumentsSection.tsx [app-client] (ecmascript)", ((__turbopack_context__) => {
"use strict";

__turbopack_context__.s([
    "AdmissionDocumentsSection",
    ()=>AdmissionDocumentsSection
]);
var __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$compiled$2f$react$2f$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$client$5d$__$28$ecmascript$29$__ = __turbopack_context__.i("[project]/node_modules/next/dist/compiled/react/jsx-dev-runtime.js [app-client] (ecmascript)");
var __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$compiled$2f$react$2f$index$2e$js__$5b$app$2d$client$5d$__$28$ecmascript$29$__ = __turbopack_context__.i("[project]/node_modules/next/dist/compiled/react/index.js [app-client] (ecmascript)");
var __TURBOPACK__imported__module__$5b$project$5d2f$src$2f$components$2f$ui$2f$Button$2e$tsx__$5b$app$2d$client$5d$__$28$ecmascript$29$__ = __turbopack_context__.i("[project]/src/components/ui/Button.tsx [app-client] (ecmascript)");
var __TURBOPACK__imported__module__$5b$project$5d2f$src$2f$components$2f$ui$2f$Icon$2e$tsx__$5b$app$2d$client$5d$__$28$ecmascript$29$__ = __turbopack_context__.i("[project]/src/components/ui/Icon.tsx [app-client] (ecmascript)");
var __TURBOPACK__imported__module__$5b$project$5d2f$src$2f$lib$2f$display$2e$ts__$5b$app$2d$client$5d$__$28$ecmascript$29$__ = __turbopack_context__.i("[project]/src/lib/display.ts [app-client] (ecmascript)");
var __TURBOPACK__imported__module__$5b$project$5d2f$src$2f$lib$2f$protectedFile$2e$ts__$5b$app$2d$client$5d$__$28$ecmascript$29$__ = __turbopack_context__.i("[project]/src/lib/protectedFile.ts [app-client] (ecmascript)");
var __TURBOPACK__imported__module__$5b$project$5d2f$src$2f$lib$2f$toast$2e$ts__$5b$app$2d$client$5d$__$28$ecmascript$29$__ = __turbopack_context__.i("[project]/src/lib/toast.ts [app-client] (ecmascript)");
var __TURBOPACK__imported__module__$5b$project$5d2f$src$2f$components$2f$ui$2f$Panel$2e$tsx__$5b$app$2d$client$5d$__$28$ecmascript$29$__ = __turbopack_context__.i("[project]/src/components/ui/Panel.tsx [app-client] (ecmascript)");
var __TURBOPACK__imported__module__$5b$project$5d2f$src$2f$features$2f$admissions$2f$shared$2f$components$2f$AdmissionDocumentPreview$2e$tsx__$5b$app$2d$client$5d$__$28$ecmascript$29$__ = __turbopack_context__.i("[project]/src/features/admissions/shared/components/AdmissionDocumentPreview.tsx [app-client] (ecmascript)");
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
const documentNames = {
    HORSE_PHOTO: 'Horse photo',
    REGISTRATION_DOCUMENT: 'Registration document',
    PEDIGREE_CERTIFICATE: 'Pedigree certificate',
    VACCINATION_RECORD: 'Vaccination record',
    DEWORMING_RECORD: 'Deworming record',
    HEALTH_CERTIFICATE: 'Health certificate',
    PREVIOUS_MEDICAL_RECORD: 'Previous medical record',
    PREVIOUS_INJURY_RECORD: 'Previous injury record'
};
function date(value) {
    return value ? (0, __TURBOPACK__imported__module__$5b$project$5d2f$src$2f$lib$2f$display$2e$ts__$5b$app$2d$client$5d$__$28$ecmascript$29$__["formatDate"])(value) : 'Not recorded';
}
function AdmissionDocumentsSection({ documents, assetUrl }) {
    _s();
    const [selectedDocument, setSelectedDocument] = (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$compiled$2f$react$2f$index$2e$js__$5b$app$2d$client$5d$__$28$ecmascript$29$__["useState"])(null);
    return /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$compiled$2f$react$2f$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$client$5d$__$28$ecmascript$29$__["jsxDEV"])(__TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$compiled$2f$react$2f$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$client$5d$__$28$ecmascript$29$__["Fragment"], {
        children: [
            /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$compiled$2f$react$2f$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$client$5d$__$28$ecmascript$29$__["jsxDEV"])(__TURBOPACK__imported__module__$5b$project$5d2f$src$2f$components$2f$ui$2f$Panel$2e$tsx__$5b$app$2d$client$5d$__$28$ecmascript$29$__["Panel"], {
                padded: true,
                children: [
                    /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$compiled$2f$react$2f$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$client$5d$__$28$ecmascript$29$__["jsxDEV"])("div", {
                        className: "flex items-center justify-between gap-3",
                        children: [
                            /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$compiled$2f$react$2f$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$client$5d$__$28$ecmascript$29$__["jsxDEV"])(__TURBOPACK__imported__module__$5b$project$5d2f$src$2f$components$2f$ui$2f$Panel$2e$tsx__$5b$app$2d$client$5d$__$28$ecmascript$29$__["SectionTitle"], {
                                children: "Documents"
                            }, void 0, false, {
                                fileName: "[project]/src/features/admissions/shared/components/AdmissionDocumentsSection.tsx",
                                lineNumber: 34,
                                columnNumber: 11
                            }, this),
                            /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$compiled$2f$react$2f$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$client$5d$__$28$ecmascript$29$__["jsxDEV"])("span", {
                                className: "text-xs text-[var(--color-text-muted)]",
                                children: [
                                    documents.length,
                                    " files"
                                ]
                            }, void 0, true, {
                                fileName: "[project]/src/features/admissions/shared/components/AdmissionDocumentsSection.tsx",
                                lineNumber: 35,
                                columnNumber: 11
                            }, this)
                        ]
                    }, void 0, true, {
                        fileName: "[project]/src/features/admissions/shared/components/AdmissionDocumentsSection.tsx",
                        lineNumber: 33,
                        columnNumber: 9
                    }, this),
                    documents.length ? /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$compiled$2f$react$2f$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$client$5d$__$28$ecmascript$29$__["jsxDEV"])("ul", {
                        className: "mt-4 space-y-2 text-sm",
                        children: documents.map((document)=>/*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$compiled$2f$react$2f$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$client$5d$__$28$ecmascript$29$__["jsxDEV"])("li", {
                                className: "flex items-start gap-2.5 rounded-[var(--radius-md)] border border-[var(--color-border)] p-2.5 transition-colors hover:bg-[var(--color-surface-muted)]",
                                children: [
                                    /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$compiled$2f$react$2f$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$client$5d$__$28$ecmascript$29$__["jsxDEV"])(__TURBOPACK__imported__module__$5b$project$5d2f$src$2f$components$2f$ui$2f$Icon$2e$tsx__$5b$app$2d$client$5d$__$28$ecmascript$29$__["Icon"], {
                                        name: document.documentType === 'HORSE_PHOTO' ? 'image' : 'file-text',
                                        className: "mt-0.5 shrink-0"
                                    }, void 0, false, {
                                        fileName: "[project]/src/features/admissions/shared/components/AdmissionDocumentsSection.tsx",
                                        lineNumber: 41,
                                        columnNumber: 17
                                    }, this),
                                    /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$compiled$2f$react$2f$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$client$5d$__$28$ecmascript$29$__["jsxDEV"])("div", {
                                        className: "min-w-0 flex-1",
                                        children: [
                                            /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$compiled$2f$react$2f$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$client$5d$__$28$ecmascript$29$__["jsxDEV"])(__TURBOPACK__imported__module__$5b$project$5d2f$src$2f$components$2f$ui$2f$Button$2e$tsx__$5b$app$2d$client$5d$__$28$ecmascript$29$__["Button"], {
                                                variant: "link",
                                                "aria-haspopup": "dialog",
                                                onClick: ()=>setSelectedDocument(document),
                                                className: "block max-w-full truncate text-left",
                                                children: document.originalFileName || documentNames[document.documentType] || document.documentType
                                            }, void 0, false, {
                                                fileName: "[project]/src/features/admissions/shared/components/AdmissionDocumentsSection.tsx",
                                                lineNumber: 43,
                                                columnNumber: 19
                                            }, this),
                                            /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$compiled$2f$react$2f$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$client$5d$__$28$ecmascript$29$__["jsxDEV"])("p", {
                                                className: "mt-0.5 truncate text-xs text-[var(--color-text-muted)]",
                                                children: [
                                                    documentNames[document.documentType] || document.documentType,
                                                    " · Recorded ",
                                                    date(document.recordDate),
                                                    " · Uploaded ",
                                                    date(document.uploadedAt)
                                                ]
                                            }, void 0, true, {
                                                fileName: "[project]/src/features/admissions/shared/components/AdmissionDocumentsSection.tsx",
                                                lineNumber: 51,
                                                columnNumber: 19
                                            }, this)
                                        ]
                                    }, void 0, true, {
                                        fileName: "[project]/src/features/admissions/shared/components/AdmissionDocumentsSection.tsx",
                                        lineNumber: 42,
                                        columnNumber: 17
                                    }, this),
                                    /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$compiled$2f$react$2f$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$client$5d$__$28$ecmascript$29$__["jsxDEV"])(__TURBOPACK__imported__module__$5b$project$5d2f$src$2f$components$2f$ui$2f$Button$2e$tsx__$5b$app$2d$client$5d$__$28$ecmascript$29$__["IconButton"], {
                                        icon: "external-link",
                                        size: "sm",
                                        label: `Open ${document.originalFileName || documentNames[document.documentType] || document.documentType} in new tab`,
                                        onClick: ()=>{
                                            (0, __TURBOPACK__imported__module__$5b$project$5d2f$src$2f$lib$2f$protectedFile$2e$ts__$5b$app$2d$client$5d$__$28$ecmascript$29$__["openProtectedFile"])(document.fileUrl).catch((error)=>__TURBOPACK__imported__module__$5b$project$5d2f$src$2f$lib$2f$toast$2e$ts__$5b$app$2d$client$5d$__$28$ecmascript$29$__["toast"].error((0, __TURBOPACK__imported__module__$5b$project$5d2f$src$2f$lib$2f$display$2e$ts__$5b$app$2d$client$5d$__$28$ecmascript$29$__["displayError"])(error, 'Unable to open the file.')));
                                        },
                                        className: "shrink-0"
                                    }, void 0, false, {
                                        fileName: "[project]/src/features/admissions/shared/components/AdmissionDocumentsSection.tsx",
                                        lineNumber: 55,
                                        columnNumber: 17
                                    }, this)
                                ]
                            }, document.id, true, {
                                fileName: "[project]/src/features/admissions/shared/components/AdmissionDocumentsSection.tsx",
                                lineNumber: 40,
                                columnNumber: 15
                            }, this))
                    }, void 0, false, {
                        fileName: "[project]/src/features/admissions/shared/components/AdmissionDocumentsSection.tsx",
                        lineNumber: 38,
                        columnNumber: 11
                    }, this) : /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$compiled$2f$react$2f$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$client$5d$__$28$ecmascript$29$__["jsxDEV"])("p", {
                        className: "mt-4 text-sm italic text-[var(--color-text-muted)]",
                        children: "No documents attached."
                    }, void 0, false, {
                        fileName: "[project]/src/features/admissions/shared/components/AdmissionDocumentsSection.tsx",
                        lineNumber: 68,
                        columnNumber: 11
                    }, this)
                ]
            }, void 0, true, {
                fileName: "[project]/src/features/admissions/shared/components/AdmissionDocumentsSection.tsx",
                lineNumber: 32,
                columnNumber: 7
            }, this),
            selectedDocument && /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$compiled$2f$react$2f$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$client$5d$__$28$ecmascript$29$__["jsxDEV"])(__TURBOPACK__imported__module__$5b$project$5d2f$src$2f$features$2f$admissions$2f$shared$2f$components$2f$AdmissionDocumentPreview$2e$tsx__$5b$app$2d$client$5d$__$28$ecmascript$29$__["AdmissionDocumentPreview"], {
                document: selectedDocument,
                label: documentNames[selectedDocument.documentType] || selectedDocument.documentType,
                fileUrl: assetUrl(selectedDocument.fileUrl),
                onClose: ()=>setSelectedDocument(null)
            }, selectedDocument.id, false, {
                fileName: "[project]/src/features/admissions/shared/components/AdmissionDocumentsSection.tsx",
                lineNumber: 72,
                columnNumber: 9
            }, this)
        ]
    }, void 0, true, {
        fileName: "[project]/src/features/admissions/shared/components/AdmissionDocumentsSection.tsx",
        lineNumber: 31,
        columnNumber: 5
    }, this);
}
_s(AdmissionDocumentsSection, "JC+YdRn5JPq+xB2blXP4mTyfwfs=");
_c = AdmissionDocumentsSection;
var _c;
__turbopack_context__.k.register(_c, "AdmissionDocumentsSection");
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
"[project]/src/lib/toast.ts [app-client] (ecmascript)", ((__turbopack_context__) => {
"use strict";

__turbopack_context__.s([
    "toast",
    ()=>toast
]);
var __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$sonner$2f$dist$2f$index$2e$mjs__$5b$app$2d$client$5d$__$28$ecmascript$29$__ = __turbopack_context__.i("[project]/node_modules/sonner/dist/index.mjs [app-client] (ecmascript)");
;
const toast = {
    success: (message, description)=>__TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$sonner$2f$dist$2f$index$2e$mjs__$5b$app$2d$client$5d$__$28$ecmascript$29$__["toast"].success(message, {
            description
        }),
    error: (message, description)=>__TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$sonner$2f$dist$2f$index$2e$mjs__$5b$app$2d$client$5d$__$28$ecmascript$29$__["toast"].error(message, {
            description
        }),
    info: (message, description)=>__TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$sonner$2f$dist$2f$index$2e$mjs__$5b$app$2d$client$5d$__$28$ecmascript$29$__["toast"].info(message, {
            description
        }),
    warning: (message, description)=>__TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$sonner$2f$dist$2f$index$2e$mjs__$5b$app$2d$client$5d$__$28$ecmascript$29$__["toast"].warning(message, {
            description
        })
};
if (typeof globalThis.$RefreshHelpers$ === 'object' && globalThis.$RefreshHelpers !== null) {
    __turbopack_context__.k.registerExports(__turbopack_context__.m, globalThis.$RefreshHelpers$);
}
}),
]);

//# sourceMappingURL=src_04tvw5n._.js.map