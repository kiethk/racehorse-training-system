'use client';

import {useEffect, useMemo, useState} from 'react';
import Link from 'next/link';


import {Button} from '@/components/ui/Button';
import {EmptyState, ListSkeleton} from '@/components/ui/states';
import {Notice} from '@/components/ui/Notice';
import {HorseAvatar} from '@/components/ui/HorseAvatar';
import {FilterBar} from '@/components/ui/FilterBar';
import {admissionsApi} from '@/features/admissions/services/api';
import {ownerAdmissionApi} from '@/features/admissions/services/ownerApi';
import type {AdmissionSummaryResponse} from '../types';
import {AdmissionTable} from '../shared/components/AdmissionTable';
import {AdmissionListLayout} from '../shared/components/AdmissionListLayout';
import {AdmissionSearchField} from '../shared/components/AdmissionSearchField';

function AdmissionHorseAvatar({ name, imageUrl }: { name: string; imageUrl?: string | null }) {
  const [loadedImage, setLoadedImage] = useState<{ source: string; url: string } | null>(null);
  const image = loadedImage && loadedImage.source === imageUrl ? loadedImage.url : null;

  useEffect(() => {
    if (!imageUrl) return;

    const controller = new AbortController();
    let objectUrl: string | undefined;
    admissionsApi.getDocumentFile(imageUrl, controller.signal).then((blob) => {
      if (controller.signal.aborted) return;
      if (!blob.type.startsWith('image/')) return;
      objectUrl = URL.createObjectURL(blob);
      setLoadedImage({ source: imageUrl, url: objectUrl });
    }).catch(() => undefined);

    return () => {
      controller.abort();
      if (objectUrl) URL.revokeObjectURL(objectUrl);
    };
  }, [imageUrl]);

  return <HorseAvatar name={name} image={image} size={32} rounded="md" />;
}

export function OwnerAdmissionsListView() {
  const [rows, setRows] = useState<AdmissionSummaryResponse[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [search, setSearch] = useState('');
  useEffect(() => {
    ownerAdmissionApi.list().then(setRows).catch(e => setError(String(e))).finally(() => setLoading(false));
  }, []);
  const filteredRows = useMemo(() => {
    const query = search.trim().toLowerCase();
    if (!query) return rows;
    return rows.filter((admission) =>
      `${admission.candidateName} ${admission.breed ?? ''} ${admission.status}`.toLowerCase().includes(query),
    );
  }, [rows, search]);
  return (
    <AdmissionListLayout
        title="My admissions"
        description="Track each application and its review status."
        actions={<Link href="/owner/admissions/new"><Button variant="primary">New admission</Button></Link>}
    >
      {loading ? <ListSkeleton rows={5} /> : error ? <Notice tone="error">{error}</Notice> : (
        <>
          <FilterBar onSubmit={(event) => event.preventDefault()} className="items-end">
            <AdmissionSearchField
              className="w-full sm:max-w-md"
              label="Search admissions"
              placeholder="Search horse name, breed, or status"
              value={search}
              onChange={setSearch}
            />
            {search && (
              <Button type="button" variant="secondary" size="sm" onClick={() => setSearch('')}>
                Clear
              </Button>
            )}
          </FilterBar>
          {rows.length === 0 ? (
            <EmptyState title="No admission history yet" description="Submit an application to start tracking its review." />
          ) : (
            <AdmissionTable
              admissions={filteredRows}
              searchable={false}
              emptyTitle="No matching admissions"
              emptyDescription="Try another search or clear the search field to see all applications."
              emptyAction={<Button size="sm" onClick={() => setSearch('')}>Clear search</Button>}
              detailHref={(id) => `/owner/admissions/${id}`}
              renderAvatar={(admission) => (
                <AdmissionHorseAvatar name={admission.candidateName} imageUrl={admission.imageUrl} />
              )}
            />
          )}
        </>
      )}
    </AdmissionListLayout>
  );
}

