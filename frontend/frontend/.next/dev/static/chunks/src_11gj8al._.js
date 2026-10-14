(globalThis["TURBOPACK"] || (globalThis["TURBOPACK"] = [])).push([typeof document === "object" ? document.currentScript : undefined,
"[project]/src/features/admissions/components/UrgentAssignmentModal.tsx [app-client] (ecmascript)", ((__turbopack_context__) => {
"use strict";

__turbopack_context__.s([
    "UrgentAssignmentModal",
    ()=>UrgentAssignmentModal
]);
var __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$compiled$2f$react$2f$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$client$5d$__$28$ecmascript$29$__ = __turbopack_context__.i("[project]/node_modules/next/dist/compiled/react/jsx-dev-runtime.js [app-client] (ecmascript)");
var __TURBOPACK__imported__module__$5b$project$5d2f$src$2f$components$2f$ui$2f$index$2e$ts__$5b$app$2d$client$5d$__$28$ecmascript$29$__$3c$locals$3e$__ = __turbopack_context__.i("[project]/src/components/ui/index.ts [app-client] (ecmascript) <locals>");
var __TURBOPACK__imported__module__$5b$project$5d2f$src$2f$components$2f$ui$2f$Button$2e$tsx__$5b$app$2d$client$5d$__$28$ecmascript$29$__ = __turbopack_context__.i("[project]/src/components/ui/Button.tsx [app-client] (ecmascript)");
var __TURBOPACK__imported__module__$5b$project$5d2f$src$2f$components$2f$ui$2f$Modal$2e$tsx__$5b$app$2d$client$5d$__$28$ecmascript$29$__ = __turbopack_context__.i("[project]/src/components/ui/Modal.tsx [app-client] (ecmascript)");
var __TURBOPACK__imported__module__$5b$project$5d2f$src$2f$components$2f$ui$2f$Notice$2e$tsx__$5b$app$2d$client$5d$__$28$ecmascript$29$__ = __turbopack_context__.i("[project]/src/components/ui/Notice.tsx [app-client] (ecmascript)");
var __TURBOPACK__imported__module__$5b$project$5d2f$src$2f$components$2f$ui$2f$StatusBadge$2e$tsx__$5b$app$2d$client$5d$__$28$ecmascript$29$__ = __turbopack_context__.i("[project]/src/components/ui/StatusBadge.tsx [app-client] (ecmascript)");
var __TURBOPACK__imported__module__$5b$project$5d2f$src$2f$components$2f$ui$2f$ProtectedImage$2e$tsx__$5b$app$2d$client$5d$__$28$ecmascript$29$__ = __turbopack_context__.i("[project]/src/components/ui/ProtectedImage.tsx [app-client] (ecmascript)");
var __TURBOPACK__imported__module__$5b$project$5d2f$src$2f$lib$2f$display$2e$ts__$5b$app$2d$client$5d$__$28$ecmascript$29$__ = __turbopack_context__.i("[project]/src/lib/display.ts [app-client] (ecmascript)");
'use client';
;
;
;
function Detail({ label, children, className }) {
    return /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$compiled$2f$react$2f$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$client$5d$__$28$ecmascript$29$__["jsxDEV"])("div", {
        className: className,
        children: [
            /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$compiled$2f$react$2f$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$client$5d$__$28$ecmascript$29$__["jsxDEV"])("dt", {
                className: "text-xs font-medium uppercase tracking-wide text-[var(--color-text-muted)]",
                children: label
            }, void 0, false, {
                fileName: "[project]/src/features/admissions/components/UrgentAssignmentModal.tsx",
                lineNumber: 16,
                columnNumber: 7
            }, this),
            /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$compiled$2f$react$2f$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$client$5d$__$28$ecmascript$29$__["jsxDEV"])("dd", {
                className: "mt-1 text-sm font-semibold text-[var(--color-text-primary)]",
                children: children
            }, void 0, false, {
                fileName: "[project]/src/features/admissions/components/UrgentAssignmentModal.tsx",
                lineNumber: 17,
                columnNumber: 7
            }, this)
        ]
    }, void 0, true, {
        fileName: "[project]/src/features/admissions/components/UrgentAssignmentModal.tsx",
        lineNumber: 15,
        columnNumber: 5
    }, this);
}
_c = Detail;
function UrgentAssignmentModal({ alert, onOpenCase }) {
    if (!alert) return null;
    return /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$compiled$2f$react$2f$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$client$5d$__$28$ecmascript$29$__["jsxDEV"])(__TURBOPACK__imported__module__$5b$project$5d2f$src$2f$components$2f$ui$2f$Modal$2e$tsx__$5b$app$2d$client$5d$__$28$ecmascript$29$__["Modal"], {
        open: true,
        // The assignment is automatic and must be acknowledged by opening the case.
        dismissible: false,
        onClose: ()=>undefined,
        size: "lg",
        tone: "danger",
        title: "Urgent case assigned",
        description: "You have been assigned to this case automatically; no confirmation is required. The horse is blocked from training until the examination is complete.",
        footer: /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$compiled$2f$react$2f$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$client$5d$__$28$ecmascript$29$__["jsxDEV"])(__TURBOPACK__imported__module__$5b$project$5d2f$src$2f$components$2f$ui$2f$Button$2e$tsx__$5b$app$2d$client$5d$__$28$ecmascript$29$__["Button"], {
            variant: "destructive",
            iconRight: "chevron-right",
            className: "w-full",
            onClick: ()=>onOpenCase(alert),
            children: "Open urgent case"
        }, void 0, false, {
            fileName: "[project]/src/features/admissions/components/UrgentAssignmentModal.tsx",
            lineNumber: 36,
            columnNumber: 9
        }, this),
        children: /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$compiled$2f$react$2f$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$client$5d$__$28$ecmascript$29$__["jsxDEV"])("div", {
            className: "space-y-4",
            children: [
                /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$compiled$2f$react$2f$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$client$5d$__$28$ecmascript$29$__["jsxDEV"])("dl", {
                    className: "grid gap-4 rounded-[var(--radius-md)] bg-[var(--color-surface-muted)] p-4 sm:grid-cols-2",
                    children: [
                        /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$compiled$2f$react$2f$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$client$5d$__$28$ecmascript$29$__["jsxDEV"])(Detail, {
                            label: "Horse",
                            children: [
                                alert.horseName,
                                " · #",
                                alert.horseId
                            ]
                        }, void 0, true, {
                            fileName: "[project]/src/features/admissions/components/UrgentAssignmentModal.tsx",
                            lineNumber: 43,
                            columnNumber: 11
                        }, this),
                        /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$compiled$2f$react$2f$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$client$5d$__$28$ecmascript$29$__["jsxDEV"])(Detail, {
                            label: "Severity",
                            children: /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$compiled$2f$react$2f$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$client$5d$__$28$ecmascript$29$__["jsxDEV"])(__TURBOPACK__imported__module__$5b$project$5d2f$src$2f$components$2f$ui$2f$StatusBadge$2e$tsx__$5b$app$2d$client$5d$__$28$ecmascript$29$__["Pill"], {
                                tone: "danger",
                                children: alert.severity
                            }, void 0, false, {
                                fileName: "[project]/src/features/admissions/components/UrgentAssignmentModal.tsx",
                                lineNumber: 44,
                                columnNumber: 36
                            }, this)
                        }, void 0, false, {
                            fileName: "[project]/src/features/admissions/components/UrgentAssignmentModal.tsx",
                            lineNumber: 44,
                            columnNumber: 11
                        }, this),
                        /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$compiled$2f$react$2f$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$client$5d$__$28$ecmascript$29$__["jsxDEV"])(Detail, {
                            label: "Stall / location",
                            children: [
                                alert.stallCode,
                                alert.stableLocation
                            ].filter(Boolean).join(' · ') || 'See report'
                        }, void 0, false, {
                            fileName: "[project]/src/features/admissions/components/UrgentAssignmentModal.tsx",
                            lineNumber: 45,
                            columnNumber: 11
                        }, this),
                        /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$compiled$2f$react$2f$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$client$5d$__$28$ecmascript$29$__["jsxDEV"])(Detail, {
                            label: "Reported by",
                            children: [
                                alert.reportedByName || 'Unknown',
                                " · #",
                                alert.reportedById
                            ]
                        }, void 0, true, {
                            fileName: "[project]/src/features/admissions/components/UrgentAssignmentModal.tsx",
                            lineNumber: 48,
                            columnNumber: 11
                        }, this),
                        /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$compiled$2f$react$2f$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$client$5d$__$28$ecmascript$29$__["jsxDEV"])(Detail, {
                            label: "Reported at",
                            className: "sm:col-span-2",
                            children: (0, __TURBOPACK__imported__module__$5b$project$5d2f$src$2f$lib$2f$display$2e$ts__$5b$app$2d$client$5d$__$28$ecmascript$29$__["formatDateTime"])(alert.reportedAt)
                        }, void 0, false, {
                            fileName: "[project]/src/features/admissions/components/UrgentAssignmentModal.tsx",
                            lineNumber: 49,
                            columnNumber: 11
                        }, this)
                    ]
                }, void 0, true, {
                    fileName: "[project]/src/features/admissions/components/UrgentAssignmentModal.tsx",
                    lineNumber: 42,
                    columnNumber: 9
                }, this),
                /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$compiled$2f$react$2f$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$client$5d$__$28$ecmascript$29$__["jsxDEV"])(__TURBOPACK__imported__module__$5b$project$5d2f$src$2f$components$2f$ui$2f$Notice$2e$tsx__$5b$app$2d$client$5d$__$28$ecmascript$29$__["Notice"], {
                    tone: "error",
                    title: `Training decision: ${alert.trainingDecision}`,
                    children: "Training is paused until the examination is complete."
                }, void 0, false, {
                    fileName: "[project]/src/features/admissions/components/UrgentAssignmentModal.tsx",
                    lineNumber: 52,
                    columnNumber: 9
                }, this),
                /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$compiled$2f$react$2f$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$client$5d$__$28$ecmascript$29$__["jsxDEV"])("section", {
                    className: "rounded-[var(--radius-md)] border border-[var(--color-border)] p-4",
                    children: [
                        /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$compiled$2f$react$2f$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$client$5d$__$28$ecmascript$29$__["jsxDEV"])("h3", {
                            className: "text-sm font-semibold text-[var(--color-text-primary)]",
                            children: alert.title
                        }, void 0, false, {
                            fileName: "[project]/src/features/admissions/components/UrgentAssignmentModal.tsx",
                            lineNumber: 57,
                            columnNumber: 11
                        }, this),
                        /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$compiled$2f$react$2f$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$client$5d$__$28$ecmascript$29$__["jsxDEV"])("p", {
                            className: "mt-2 whitespace-pre-wrap break-words text-sm leading-relaxed text-[var(--color-text-secondary)]",
                            children: alert.description
                        }, void 0, false, {
                            fileName: "[project]/src/features/admissions/components/UrgentAssignmentModal.tsx",
                            lineNumber: 58,
                            columnNumber: 11
                        }, this)
                    ]
                }, void 0, true, {
                    fileName: "[project]/src/features/admissions/components/UrgentAssignmentModal.tsx",
                    lineNumber: 56,
                    columnNumber: 9
                }, this),
                alert.imageUrl && /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$compiled$2f$react$2f$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$client$5d$__$28$ecmascript$29$__["jsxDEV"])(__TURBOPACK__imported__module__$5b$project$5d2f$src$2f$components$2f$ui$2f$ProtectedImage$2e$tsx__$5b$app$2d$client$5d$__$28$ecmascript$29$__["ProtectedImage"], {
                    src: alert.imageUrl,
                    alt: "Urgent case report",
                    openOnClick: true,
                    className: "max-h-72 w-full object-contain"
                }, void 0, false, {
                    fileName: "[project]/src/features/admissions/components/UrgentAssignmentModal.tsx",
                    lineNumber: 64,
                    columnNumber: 11
                }, this)
            ]
        }, void 0, true, {
            fileName: "[project]/src/features/admissions/components/UrgentAssignmentModal.tsx",
            lineNumber: 41,
            columnNumber: 7
        }, this)
    }, void 0, false, {
        fileName: "[project]/src/features/admissions/components/UrgentAssignmentModal.tsx",
        lineNumber: 26,
        columnNumber: 5
    }, this);
}
_c1 = UrgentAssignmentModal;
var _c, _c1;
__turbopack_context__.k.register(_c, "Detail");
__turbopack_context__.k.register(_c1, "UrgentAssignmentModal");
if (typeof globalThis.$RefreshHelpers$ === 'object' && globalThis.$RefreshHelpers !== null) {
    __turbopack_context__.k.registerExports(__turbopack_context__.m, globalThis.$RefreshHelpers$);
}
}),
"[project]/src/features/admissions/components/UrgentAssignmentNotifier.tsx [app-client] (ecmascript)", ((__turbopack_context__) => {
"use strict";

__turbopack_context__.s([
    "UrgentAssignmentNotifier",
    ()=>UrgentAssignmentNotifier
]);
var __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$compiled$2f$react$2f$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$client$5d$__$28$ecmascript$29$__ = __turbopack_context__.i("[project]/node_modules/next/dist/compiled/react/jsx-dev-runtime.js [app-client] (ecmascript)");
var __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$compiled$2f$react$2f$index$2e$js__$5b$app$2d$client$5d$__$28$ecmascript$29$__ = __turbopack_context__.i("[project]/node_modules/next/dist/compiled/react/index.js [app-client] (ecmascript)");
var __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$navigation$2e$js__$5b$app$2d$client$5d$__$28$ecmascript$29$__ = __turbopack_context__.i("[project]/node_modules/next/navigation.js [app-client] (ecmascript)");
var __TURBOPACK__imported__module__$5b$project$5d2f$src$2f$services$2f$api$2e$ts__$5b$app$2d$client$5d$__$28$ecmascript$29$__ = __turbopack_context__.i("[project]/src/services/api.ts [app-client] (ecmascript)");
var __TURBOPACK__imported__module__$5b$project$5d2f$src$2f$features$2f$admissions$2f$services$2f$api$2e$ts__$5b$app$2d$client$5d$__$28$ecmascript$29$__ = __turbopack_context__.i("[project]/src/features/admissions/services/api.ts [app-client] (ecmascript)");
var __TURBOPACK__imported__module__$5b$project$5d2f$src$2f$features$2f$admissions$2f$components$2f$UrgentAssignmentModal$2e$tsx__$5b$app$2d$client$5d$__$28$ecmascript$29$__ = __turbopack_context__.i("[project]/src/features/admissions/components/UrgentAssignmentModal.tsx [app-client] (ecmascript)");
;
var _s = __turbopack_context__.k.signature();
'use client';
;
;
;
;
;
function UrgentAssignmentNotifier() {
    _s();
    const router = (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$navigation$2e$js__$5b$app$2d$client$5d$__$28$ecmascript$29$__["useRouter"])();
    const pathname = (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$navigation$2e$js__$5b$app$2d$client$5d$__$28$ecmascript$29$__["usePathname"])();
    const [alerts, setAlerts] = (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$compiled$2f$react$2f$index$2e$js__$5b$app$2d$client$5d$__$28$ecmascript$29$__["useState"])([]);
    const versions = (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$compiled$2f$react$2f$index$2e$js__$5b$app$2d$client$5d$__$28$ecmascript$29$__["useRef"])(new Map());
    const mergeAlerts = (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$compiled$2f$react$2f$index$2e$js__$5b$app$2d$client$5d$__$28$ecmascript$29$__["useCallback"])({
        "UrgentAssignmentNotifier.useCallback[mergeAlerts]": (incoming)=>{
            setAlerts({
                "UrgentAssignmentNotifier.useCallback[mergeAlerts]": (current)=>{
                    const bySchedule = new Map(current.map({
                        "UrgentAssignmentNotifier.useCallback[mergeAlerts]": (item)=>[
                                item.scheduleId,
                                item
                            ]
                    }["UrgentAssignmentNotifier.useCallback[mergeAlerts]"]));
                    for (const alert of incoming){
                        const seenVersion = versions.current.get(alert.scheduleId);
                        if (seenVersion !== undefined && seenVersion >= alert.eventId) continue;
                        versions.current.set(alert.scheduleId, alert.eventId);
                        bySchedule.set(alert.scheduleId, alert);
                    }
                    return [
                        ...bySchedule.values()
                    ].sort({
                        "UrgentAssignmentNotifier.useCallback[mergeAlerts]": (a, b)=>new Date(a.reportedAt).getTime() - new Date(b.reportedAt).getTime()
                    }["UrgentAssignmentNotifier.useCallback[mergeAlerts]"]);
                }
            }["UrgentAssignmentNotifier.useCallback[mergeAlerts]"]);
        }
    }["UrgentAssignmentNotifier.useCallback[mergeAlerts]"], []);
    const restore = (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$compiled$2f$react$2f$index$2e$js__$5b$app$2d$client$5d$__$28$ecmascript$29$__["useCallback"])({
        "UrgentAssignmentNotifier.useCallback[restore]": async ()=>{
            try {
                mergeAlerts(await __TURBOPACK__imported__module__$5b$project$5d2f$src$2f$features$2f$admissions$2f$services$2f$api$2e$ts__$5b$app$2d$client$5d$__$28$ecmascript$29$__["admissionsApi"].getPendingUrgentAlerts());
            } catch  {
            // A reconnect or the next visibility change retries restoration.
            }
        }
    }["UrgentAssignmentNotifier.useCallback[restore]"], [
        mergeAlerts
    ]);
    (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$compiled$2f$react$2f$index$2e$js__$5b$app$2d$client$5d$__$28$ecmascript$29$__["useEffect"])({
        "UrgentAssignmentNotifier.useEffect": ()=>{
            void restore();
            const source = (0, __TURBOPACK__imported__module__$5b$project$5d2f$src$2f$services$2f$api$2e$ts__$5b$app$2d$client$5d$__$28$ecmascript$29$__["createAuthenticatedEventSource"])('/api/care-schedules/urgent-alerts/stream');
            source.addEventListener('urgent-assigned', {
                "UrgentAssignmentNotifier.useEffect": (event)=>{
                    try {
                        mergeAlerts([
                            JSON.parse(event.data)
                        ]);
                    } catch  {
                        void restore();
                    }
                }
            }["UrgentAssignmentNotifier.useEffect"]);
            source.onerror = ({
                "UrgentAssignmentNotifier.useEffect": ()=>void restore()
            })["UrgentAssignmentNotifier.useEffect"];
            const onVisible = {
                "UrgentAssignmentNotifier.useEffect.onVisible": ()=>document.visibilityState === 'visible' && void restore()
            }["UrgentAssignmentNotifier.useEffect.onVisible"];
            document.addEventListener('visibilitychange', onVisible);
            return ({
                "UrgentAssignmentNotifier.useEffect": ()=>{
                    document.removeEventListener('visibilitychange', onVisible);
                    source.close();
                }
            })["UrgentAssignmentNotifier.useEffect"];
        }
    }["UrgentAssignmentNotifier.useEffect"], [
        mergeAlerts,
        restore
    ]);
    const active = alerts.find((alert)=>pathname !== `/veterinarian/urgent/${alert.scheduleId}`) ?? null;
    function openCase(alert) {
        setAlerts((current)=>current.filter((item)=>item.scheduleId !== alert.scheduleId));
        router.push(`/veterinarian/urgent/${alert.scheduleId}`);
    }
    return /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$compiled$2f$react$2f$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$client$5d$__$28$ecmascript$29$__["jsxDEV"])(__TURBOPACK__imported__module__$5b$project$5d2f$src$2f$features$2f$admissions$2f$components$2f$UrgentAssignmentModal$2e$tsx__$5b$app$2d$client$5d$__$28$ecmascript$29$__["UrgentAssignmentModal"], {
        alert: active,
        onOpenCase: openCase
    }, void 0, false, {
        fileName: "[project]/src/features/admissions/components/UrgentAssignmentNotifier.tsx",
        lineNumber: 63,
        columnNumber: 10
    }, this);
}
_s(UrgentAssignmentNotifier, "ybOygWQuXuFkw0azg6/B8sP0b/M=", false, function() {
    return [
        __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$navigation$2e$js__$5b$app$2d$client$5d$__$28$ecmascript$29$__["useRouter"],
        __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$navigation$2e$js__$5b$app$2d$client$5d$__$28$ecmascript$29$__["usePathname"]
    ];
});
_c = UrgentAssignmentNotifier;
var _c;
__turbopack_context__.k.register(_c, "UrgentAssignmentNotifier");
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

//# sourceMappingURL=src_11gj8al._.js.map