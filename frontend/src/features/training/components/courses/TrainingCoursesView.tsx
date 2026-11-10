'use client';

import { useState } from 'react';
import { PageHeader, ScreenLayout, Tabs } from '@/components/ui';
import { CourseList } from './CourseList';
import { ExerciseList } from './ExerciseList';

type TrainingTab = 'courses' | 'exercises';

export function TrainingCoursesView() {
  const [tab, setTab] = useState<TrainingTab>('courses');

  return (
    <ScreenLayout variant="list">
      <PageHeader title="Course library" />
      <Tabs
        tabs={[
          { id: 'courses', label: 'Courses', icon: 'activity' },
          { id: 'exercises', label: 'Exercises', icon: 'clipboard' },
        ]}
        active={tab}
        onChange={(id) => setTab(id as TrainingTab)}
      />
      {tab === 'courses' ? <CourseList /> : <ExerciseList />}
    </ScreenLayout>
  );
}
