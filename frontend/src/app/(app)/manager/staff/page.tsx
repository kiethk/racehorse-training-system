import { redirect } from 'next/navigation';

export default async function ManagerStaffPage({
  searchParams,
}: {
  searchParams: Promise<{ [key: string]: string | string[] | undefined }>;
}) {
  const params = await searchParams;
  const urlParams = new URLSearchParams();
  for (const [key, value] of Object.entries(params)) {
    if (typeof value === 'string') {
      urlParams.set(key, value);
    } else if (Array.isArray(value) && value.length > 0) {
      urlParams.set(key, value[0]);
    }
  }
  const queryString = urlParams.toString();
  redirect(`/manager/accounts${queryString ? `?${queryString}` : ''}`);
}
