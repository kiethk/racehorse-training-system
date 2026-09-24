import { redirect } from 'next/navigation';

/**
 * Legacy route — redirects to the unified Management page.
 * The Admission tab is the default active tab on /manager/management.
 */
export default function ManagerAdmissionsRedirectPage() {
  redirect('/manager/management');
}
