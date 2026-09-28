'use client';

import { useState } from 'react';
import { Tabs } from '@/components/ui/Tabs';
import { CourseList } from './CourseList';
import { SubjectList } from './SubjectList';

export function TrainingCoursesView() {
  const [activeTab, setActiveTab] = useState<'courses' | 'subjects'>('courses');

  return (
    <div className="space-y-6">
      <Tabs
        tabs={[
          { id: 'courses', label: 'Khóa huấn luyện (Courses)', icon: 'activity' },
          { id: 'subjects', label: 'Thư viện bài tập (Subjects)', icon: 'clipboard' },
        ]}
        active={activeTab}
        onChange={(id) => setActiveTab(id as 'courses' | 'subjects')}
      />

      {activeTab === 'courses' ? <CourseList /> : <SubjectList />}
    </div>
  );
}
