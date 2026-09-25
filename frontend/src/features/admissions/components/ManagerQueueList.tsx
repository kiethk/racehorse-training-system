'use client';

import { useEffect, useState, useMemo } from 'react';

import { admissionsApi } from '../services/api';
import type { AdmissionSummaryResponse, AdmissionStatus, AdmissionDetailResponse } from '../types';
import { Panel, SectionTitle } from '@/components/ui/Panel';
import { Pill } from '@/components/ui/StatusBadge';
import { Icon } from '@/components/ui/Icon';
import { EmptyState, ListSkeleton } from '@/components/ui/states';
import { MetricCard } from '@/components/ui/MetricCard';
import { ManagerFinalReviewPanel } from './ManagerFinalReviewPanel';

export function ManagerQueueList() {
  const [admissions, setAdmissions] = useState<AdmissionSummaryResponse[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [filterStatus, setFilterStatus] = useState<AdmissionStatus | 'ALL'>('ALL');
  const [selectedAdmissionId, setSelectedAdmissionId] = useState<number | null>(null);
  const [detailData, setDetailData] = useState<AdmissionDetailResponse | null>(null);
  const [loadingDetail, setLoadingDetail] = useState(false);
  const [reloadCounter, setReloadCounter] = useState(0);

  useEffect(() => {
    async function loadQueue() {
      try {
        setLoading(true);
        setError(null);
        const data = await admissionsApi.getAdmissions();
        setAdmissions(data);
      } catch (err) {
        console.error('Failed to load manager queue:', err);
        setError('Failed to load admissions. Please try again.');
      } finally {
        setLoading(false);
      }
    }
    loadQueue();
  }, [reloadCounter]);

  const filteredAdmissions = useMemo(() => {
    return admissions
      .filter((a) => filterStatus === 'ALL' || a.status === filterStatus)
      .sort((a, b) => new Date(b.submittedAt).getTime() - new Date(a.submittedAt).getTime());
  }, [admissions, filterStatus]);

  // Auto-select first item
  useEffect(() => {
    if (filteredAdmissions.length > 0) {
      const isSelectedInFiltered = filteredAdmissions.some(a => a.admissionId === selectedAdmissionId);
      if (!selectedAdmissionId || !isSelectedInFiltered) {
        // eslint-disable-next-line react-hooks/set-state-in-effect
        setSelectedAdmissionId(filteredAdmissions[0].admissionId);
      }
    } else if (selectedAdmissionId !== null) {
      setSelectedAdmissionId(null);
    }
  }, [filteredAdmissions, selectedAdmissionId]);

  useEffect(() => {
    if (!selectedAdmissionId) return;
    async function loadDetail() {
      try {
        setLoadingDetail(true);
        const data = await admissionsApi.getAdmissionDetail(selectedAdmissionId!);
        setDetailData(data);
      } catch (err) {
        console.error('Failed to load detail', err);
      } finally {
        setLoadingDetail(false);
      }
    }
    loadDetail();
  }, [selectedAdmissionId, reloadCounter]);

  if (loading) {
    return (
      <Panel>
        <ListSkeleton rows={4} />
      </Panel>
    );
  }

  if (error) {
    return (
      <Panel>
        <EmptyState
          icon="alert-triangle"
          title="Error loading admissions"
          description={error}
        />
      </Panel>
    );
  }

  // Calculate metrics
  const total = admissions.length;
  const awaitingManager = admissions.filter((a) => a.status === 'MANAGER_REVIEW').length;
  const approvedCount = admissions.filter((a) => a.status === 'APPROVED').length;
  const issuesCount = admissions.filter((a) => 
    a.status === 'ADDITIONAL_INFORMATION_REQUIRED' || a.status === 'REJECTED'
  ).length;

  const statuses: { value: AdmissionStatus | 'ALL'; label: string }[] = [
    { value: 'ALL', label: 'All' },
    { value: 'SUBMITTED', label: 'Submitted' },
    { value: 'GROOM_REVIEW', label: 'Groom Review' },
    { value: 'WAITING_FOR_STALL', label: 'Waiting for Stall' },
    { value: 'VET_REVIEW', label: 'Vet Review' },
    { value: 'TRAINER_REVIEW', label: 'Trainer Review' },
    { value: 'MANAGER_REVIEW', label: 'Manager Review' },
    { value: 'APPROVED', label: 'Approved' },
    { value: 'REJECTED', label: 'Rejected' },
  ];

  return (
    <div className="space-y-4">
      {/* Metric Cards Row */}
      <div className="grid grid-cols-2 gap-3 md:grid-cols-4">
        <MetricCard label="APPLICATIONS" value={total} unit="total" icon="users" tone="neutral" />
        <MetricCard label="AWAITING MY REVIEW" value={awaitingManager} unit="applications" icon="clock" tone="warning" />
        <MetricCard label="APPROVED" value={approvedCount} unit="official horses" icon="check" tone="success" />
        <MetricCard label="REGISTRY ISSUES" value={issuesCount} unit="flagged" icon="alert-triangle" tone="danger" />
      </div>

      {/* Two Column Layout */}
      <div className="flex flex-col gap-4 lg:flex-row lg:items-start">
        {/* Left Column: List */}
        <div className="w-full lg:w-1/3 shrink-0">
          <Panel className="overflow-hidden flex flex-col h-[750px]">
            <div className="flex items-center justify-between border-b border-[var(--color-border)] px-4 py-3 shrink-0">
              <SectionTitle>Admission applications</SectionTitle>
              <span className="text-[11px] font-medium text-[var(--color-text-muted)]">
                {filteredAdmissions.length} results
              </span>
            </div>
            
            {/* Filter Dropdown */}
            <div className="border-b border-[var(--color-border)] px-4 py-2 bg-[var(--color-surface-subtle)] shrink-0 flex items-center gap-2">
               <span className="text-[12px] font-medium text-[var(--color-text-secondary)]">Filter:</span>
               <select 
                 className="text-[12px] p-1 border rounded bg-[var(--color-surface)] outline-none text-[var(--color-text-primary)]"
                 value={filterStatus}
                 onChange={(e) => setFilterStatus(e.target.value as AdmissionStatus | 'ALL')}
               >
                 {statuses.map(s => <option key={s.value} value={s.value}>{s.label}</option>)}
               </select>
            </div>

            <div className="overflow-y-auto flex-1 scroll-slim">
              <ul className="divide-y divide-[var(--color-border)]">
                {filteredAdmissions.map((admission) => {
                  const isSelected = selectedAdmissionId === admission.admissionId;
                  return (
                    <li key={admission.admissionId}>
                      <button 
                        onClick={() => setSelectedAdmissionId(admission.admissionId)}
                        className={`flex w-full text-left items-start gap-3 px-4 py-3 transition-colors outline-none
                          ${isSelected ? 'bg-[var(--color-primary-subtle)]' : 'hover:bg-[var(--color-surface-subtle)]'}
                        `}
                      >
                        <div className="min-w-0 flex-1">
                          <div className="flex items-center justify-between mb-1">
                            <div className="truncate text-[13px] font-bold text-[var(--color-text-primary)]">
                              {admission.candidateName}
                            </div>
                            <Pill tone={admission.status === 'APPROVED' ? 'success' : admission.status === 'REJECTED' ? 'danger' : admission.status === 'MANAGER_REVIEW' ? 'warning' : 'info'} size="sm">
                              {admission.status.replace(/_/g, ' ')}
                            </Pill>
                          </div>
                          <div className="truncate text-[11px] text-[var(--color-text-muted)]">
                            {admission.breed} · Submitted {new Date(admission.submittedAt).toLocaleDateString()}
                          </div>
                        </div>
                      </button>
                    </li>
                  );
                })}
                {filteredAdmissions.length === 0 && (
                  <li className="px-4 py-6 text-center text-[12px] text-[var(--color-text-muted)]">
                    No applications found.
                  </li>
                )}
              </ul>
            </div>
          </Panel>
        </div>

        {/* Right Column: Detail */}
        <div className="w-full lg:flex-1">
          {selectedAdmissionId ? (
            <Panel className="min-h-[750px] p-0 flex flex-col overflow-hidden">
               {loadingDetail || !detailData ? (
                 <div className="p-6"><ListSkeleton rows={8} /></div>
               ) : (
                 <div className="flex flex-col h-[750px] overflow-y-auto scroll-slim">
                    <div className="px-6 py-5 border-b border-[var(--color-border)] flex items-center justify-between shrink-0">
                       <div>
                         <h2 className="text-[20px] font-bold text-[var(--color-text-primary)] mb-2 flex items-center gap-2">
                           {detailData.candidate?.name || 'Unknown Candidate'}
                           <Pill tone="info" size="sm">{detailData.status.replace(/_/g, ' ')}</Pill>
                         </h2>
                         <div className="grid grid-cols-4 gap-6 text-[12px]">
                           <div>
                             <span className="text-[var(--color-text-muted)] block mb-1">OWNER ID</span>
                             <span className="font-medium text-[var(--color-text-primary)]">#{detailData.ownerId}</span>
                           </div>
                           <div>
                             <span className="text-[var(--color-text-muted)] block mb-1">BREED</span>
                             <span className="font-medium text-[var(--color-text-primary)]">{detailData.candidate?.breed || '-'}</span>
                           </div>
                           <div>
                             <span className="text-[var(--color-text-muted)] block mb-1">AGE / DOB</span>
                             <span className="font-medium text-[var(--color-text-primary)]">
                               {detailData.candidate?.dateOfBirth ? new Date(detailData.candidate.dateOfBirth).toLocaleDateString() : '-'}
                             </span>
                           </div>
                           <div>
                             <span className="text-[var(--color-text-muted)] block mb-1">SUBMITTED</span>
                             <span className="font-medium text-[var(--color-text-primary)]">
                               {new Date(detailData.submittedAt).toLocaleDateString()}
                             </span>
                           </div>
                         </div>
                       </div>
                       
                       <div className="shrink-0 pl-4">
                       </div>
                    </div>

                    <div className="p-6 space-y-6 flex-1 bg-[var(--color-surface-subtle)]">
                       
                       {/* Review Pipeline */}
                       <Panel padded className="bg-[var(--color-surface)]">
                         <SectionTitle>Review Pipeline</SectionTitle>
                         <div className="mt-4 flex flex-wrap gap-2 text-[12px]">
                           <PipelineStep 
                             label="Groom review" 
                             isDone={!!detailData.groomReviewedAt}
                             isActive={detailData.status === 'GROOM_REVIEW'}
                           />
                           <PipelineStep 
                             label="Stall assignment" 
                             isDone={!!detailData.quarantineStallCode}
                             isActive={detailData.status === 'WAITING_FOR_STALL'}
                           />
                           <PipelineStep 
                             label="Veterinarian review" 
                             isDone={!!detailData.vetReviewedAt}
                             isActive={detailData.status === 'VET_REVIEW'}
                           />
                           <PipelineStep 
                             label="Head Trainer review" 
                             isDone={!!detailData.trainerReviewedAt}
                             isActive={detailData.status === 'TRAINER_REVIEW'}
                           />
                           <PipelineStep 
                             label="Manager final review" 
                             isDone={!!detailData.managerReviewedAt}
                             isActive={detailData.status === 'MANAGER_REVIEW'}
                           />
                         </div>
                       </Panel>

                       <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
                         {/* Pedigree & Registration */}
                         <Panel padded className="bg-[var(--color-surface)]">
                           <SectionTitle>Pedigree & Registration</SectionTitle>
                           <div className="mt-4 text-[12px] space-y-3">
                              <InfoRow label="Registry Name" value={detailData.candidate?.registryName} />
                              <InfoRow label="Registration No." value={detailData.candidate?.registrationNumber} />
                              <div className="pt-2 border-t border-[var(--color-border)] space-y-3">
                                <InfoRow label="Sire" value={detailData.candidate?.sireName} />
                                <InfoRow label="Dam" value={detailData.candidate?.damName} />
                                {detailData.candidate?.pedigreeNotes && (
                                  <div className="mt-2 text-[var(--color-text-secondary)] italic">
                                    &quot;{detailData.candidate.pedigreeNotes}&quot;
                                  </div>
                                )}
                              </div>
                           </div>
                         </Panel>
                         
                         {/* Health Screening */}
                         <Panel padded className="bg-[var(--color-surface)]">
                           <SectionTitle>Health Screening</SectionTitle>
                           <div className="mt-4 text-[12px] space-y-3">
                             <InfoRow label="Vet Decision" value={detailData.vetDecision} />
                             {detailData.vetFeedback && (
                               <div>
                                 <span className="text-[var(--color-text-muted)] block mb-1">Feedback:</span>
                                 <p className="text-[var(--color-text-primary)]">{detailData.vetFeedback}</p>
                               </div>
                             )}
                             
                             <div className="pt-2 border-t border-[var(--color-border)]">
                               <span className="text-[var(--color-text-muted)] block mb-2 font-medium">Health Records</span>
                               {detailData.healthRecords && detailData.healthRecords.length > 0 ? (
                                 <ul className="space-y-2">
                                   {detailData.healthRecords.map(hr => (
                                     <li key={hr.id} className="bg-[var(--color-surface-muted)] p-2 rounded">
                                       <div className="flex justify-between mb-1">
                                         <span className="font-semibold">{hr.recordType}</span>
                                         <span className="text-[10px] text-[var(--color-text-muted)]">{new Date(hr.examinedAt).toLocaleDateString()}</span>
                                       </div>
                                       {hr.diagnosis && <div className="text-[11px]">Diag: {hr.diagnosis}</div>}
                                       {hr.notes && <div className="text-[11px] text-[var(--color-text-secondary)] mt-1">{hr.notes}</div>}
                                     </li>
                                   ))}
                                 </ul>
                               ) : (
                                 <span className="text-[var(--color-text-muted)] italic">No health records available.</span>
                               )}
                             </div>
                           </div>
                         </Panel>
                       </div>
                       
                       <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
                         {/* Documents */}
                         <Panel padded className="bg-[var(--color-surface)]">
                           <SectionTitle>Documents</SectionTitle>
                           <div className="mt-4 text-[12px]">
                             {detailData.documents && detailData.documents.length > 0 ? (
                               <ul className="space-y-2">
                                 {detailData.documents.map(doc => (
                                   <li key={doc.id} className="flex items-start gap-2 p-2 border border-[var(--color-border)] rounded hover:bg-[var(--color-surface-muted)] transition-colors">
                                     <Icon name="file-text" className="shrink-0 mt-0.5" />
                                     <div className="min-w-0 flex-1">
                                       <a href={doc.fileUrl} target="_blank" rel="noopener noreferrer" className="font-medium text-[var(--color-primary)] hover:underline truncate block">
                                         {doc.documentType.replace(/_/g, ' ')}
                                       </a>
                                       {doc.note && <div className="text-[11px] text-[var(--color-text-muted)] truncate">{doc.note}</div>}
                                     </div>
                                   </li>
                                 ))}
                               </ul>
                             ) : (
                               <span className="text-[var(--color-text-muted)] italic">No documents attached.</span>
                             )}
                           </div>
                         </Panel>
                         
                         {/* Available Regular Stalls */}
                         <Panel padded className="bg-[var(--color-surface)]">
                           <SectionTitle>Available Regular Stalls</SectionTitle>
                           <div className="mt-4 text-[12px]">
                             {detailData.availableRegularStalls && detailData.availableRegularStalls.length > 0 ? (
                               <div className="flex flex-wrap gap-2">
                                 {detailData.availableRegularStalls.map(stall => (
                                   <Pill key={stall.id} tone="neutral" size="sm">
                                     Stall {stall.stallCode}
                                   </Pill>
                                 ))}
                               </div>
                             ) : (
                               <span className="text-[var(--color-text-muted)] italic">No regular stalls currently available.</span>
                             )}
                           </div>
                         </Panel>
                       </div>

                       {detailData.status === 'MANAGER_REVIEW' && (
                         <ManagerFinalReviewPanel
                           detailData={detailData}
                           onSuccess={() => setReloadCounter(c => c + 1)}
                         />
                       )}
                     </div>
                 </div>
               )}
            </Panel>
          ) : (
            <div className="h-[750px] flex items-center justify-center border-2 border-dashed border-[var(--color-border)] rounded-[var(--radius-lg)]">
               <div className="text-center text-[var(--color-text-muted)]">
                 <Icon name="clipboard" size={32} className="mx-auto mb-3 opacity-50" />
                 <p className="text-[14px] font-medium text-[var(--color-text-secondary)]">Select an application</p>
                 <p className="text-[12px] mt-1">Choose an application from the list to view details</p>
               </div>
            </div>
          )}
        </div>
      </div>
    </div>
  );
}

// Helpers

function PipelineStep({ label, isDone, isActive }: { label: string, isDone: boolean, isActive: boolean }) {
  if (isActive) {
    return (
      <div className="px-3 py-2 border rounded border-blue-400 bg-blue-50 text-blue-700 font-medium shadow-sm">
        <span className="mr-1.5 opacity-60">●</span>{label}
      </div>
    );
  }
  if (isDone) {
    return (
      <div className="px-3 py-2 border rounded border-green-300 bg-green-50 text-green-700">
        <span className="mr-1.5">✓</span>{label}
      </div>
    );
  }
  return (
    <div className="px-3 py-2 border rounded border-[var(--color-border)] text-[var(--color-text-muted)] bg-[var(--color-surface-subtle)]">
      <span className="mr-1.5 opacity-40">-</span>{label}
    </div>
  );
}

function InfoRow({ label, value }: { label: string, value: string | null | undefined }) {
  return (
    <div className="flex justify-between items-start gap-4">
      <span className="text-[var(--color-text-muted)] shrink-0">{label}</span>
      <span className="font-medium text-[var(--color-text-primary)] text-right">
        {value || <span className="text-[var(--color-text-muted)] font-normal italic">N/A</span>}
      </span>
    </div>
  );
}
