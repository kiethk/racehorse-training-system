import { NextResponse } from 'next/server';

export const dynamic = 'force-dynamic';

export function GET() {
  const now = new Date();
  return NextResponse.json({
    success: true,
    data: {
      epochMillis: now.getTime(),
      isoUtc: now.toISOString(),
      hours: now.getHours(),
      minutes: now.getMinutes(),
    },
  });
}
