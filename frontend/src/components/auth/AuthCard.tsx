import type { ReactNode } from 'react';
import Link from 'next/link';
import { BrandLogo } from '@/components/ui/BrandLogo';

interface AuthCardProps {
  title: string;
  description: string;
  footer?: ReactNode;
  note?: ReactNode;
  children: ReactNode;
  cardWidth?: 'md' | 'xl';
}

export function AuthCard({
  title,
  description,
  footer,
  note,
  children,
  cardWidth = 'md',
}: AuthCardProps) {
  const maxWidthClass = cardWidth === 'xl' ? 'max-w-xl' : 'max-w-md';

  return (
    <main className="relative min-h-screen w-full flex flex-col lg:flex-row bg-[#040d09] overflow-hidden">
      {/* ── FIXED RACETRACK ATMOSPHERIC BLUR BACKDROP (RIGHT HALF) ── */}
      {/* Anchored fixed to viewport so scrolling never exposes any black gap */}
      <div className="fixed inset-y-0 right-0 w-full lg:w-[52%] xl:w-[48%] z-0 pointer-events-none overflow-hidden">
        <img
          src="/images/auth-hero.jpg"
          alt="Racetrack Backdrop"
          className="h-full w-full object-cover object-right lg:object-[70%_center] scale-110 blur-xl opacity-70"
        />
        <div className="absolute inset-0 backdrop-blur-2xl bg-black/25" />
        <div className="absolute inset-0 bg-gradient-to-b from-black/25 via-transparent to-black/35" />
      </div>

      {/* ── LEFT HERO PANEL (DESKTOP) ── */}
      <section className="relative z-10 hidden lg:flex lg:w-[48%] xl:w-[52%] flex-col justify-between p-10 xl:p-14 text-white overflow-hidden">
        {/* Sharp Hero Image Perfectly Centered in Left Half */}
        <div className="absolute inset-0 z-0 pointer-events-none">
          <img
            src="/images/auth-hero.jpg"
            alt="Riverside Thoroughbred Training"
            className="h-full w-full object-cover object-[68%_40%] scale-105 transition-transform duration-700 ease-out"
          />
          {/* Cinematic Vignette */}
          <div className="absolute inset-0 bg-gradient-to-t from-[#040d09]/95 via-[#040d09]/45 to-black/35" />
          <div className="absolute inset-0 bg-radial-gradient from-transparent via-black/10 to-black/40" />
        </div>

        {/* Top Header & Brand */}
        <div className="relative z-10 flex items-center justify-between">
          <Link href="/" className="group flex items-center gap-3">
            <div className="flex h-11 w-11 items-center justify-center rounded-xl bg-white/10 backdrop-blur-md border border-white/20 shadow-lg transition-transform group-hover:scale-105">
              <BrandLogo className="h-7 w-7 text-emerald-400" />
            </div>
            <div>
              <div className="flex items-center gap-2">
                <span className="font-bold text-lg tracking-wider text-white">RTMS</span>
                <span className="rounded-full bg-emerald-500/20 px-2 py-0.5 text-[10px] font-semibold uppercase tracking-wider text-emerald-300 border border-emerald-500/30">
                  Official Club Portal
                </span>
              </div>
              <p className="text-xs text-emerald-100/70 font-medium tracking-wide">
                Riverside Training Club
              </p>
            </div>
          </Link>
        </div>

        {/* Bottom Editorial Content */}
        <div className="relative z-10 space-y-6 max-w-xl">
          <div className="space-y-2.5">
            <div className="inline-flex items-center gap-2 rounded-full bg-black/40 px-3 py-1 text-xs font-medium text-emerald-300 backdrop-blur-md border border-emerald-500/20">
              <span className="h-1.5 w-1.5 rounded-full bg-emerald-400 animate-pulse" />
              Thoroughbred Performance & Science
            </div>
            <h2 className="text-2xl xl:text-3xl font-extrabold tracking-tight text-white leading-tight">
              Conditioning champions with precision, science & care.
            </h2>
            <p className="text-xs xl:text-sm text-gray-200 leading-relaxed drop-shadow-sm">
              Real-time racehorse lifecycle monitoring, veterinary clearance, training regimens, and stable capacity management for owners, trainers, and equine professionals.
            </p>
          </div>

          {/* Quick Pillars */}
          <div className="grid grid-cols-3 gap-3 pt-3 border-t border-white/15">
            <div className="rounded-lg bg-black/30 backdrop-blur-md p-3 border border-white/15">
              <div className="text-emerald-400 font-bold text-lg">240+</div>
              <div className="text-[11px] text-gray-300 uppercase tracking-wider font-medium mt-0.5">
                Stalls & Paddocks
              </div>
            </div>
            <div className="rounded-lg bg-black/30 backdrop-blur-md p-3 border border-white/15">
              <div className="text-emerald-400 font-bold text-lg">24/7</div>
              <div className="text-[11px] text-gray-300 uppercase tracking-wider font-medium mt-0.5">
                Equine Medical Care
              </div>
            </div>
            <div className="rounded-lg bg-black/30 backdrop-blur-md p-3 border border-white/15">
              <div className="text-emerald-400 font-bold text-lg">100%</div>
              <div className="text-[11px] text-gray-300 uppercase tracking-wider font-medium mt-0.5">
                Audit & Compliance
              </div>
            </div>
          </div>
        </div>
      </section>

      {/* ── RIGHT FORM PANEL (SCROLL-SLIM, RESPONSIVE & ELEGANT) ── */}
      <section className="relative z-10 flex-1 flex flex-col justify-between p-6 sm:p-8 lg:p-10 xl:p-12 overflow-y-auto scroll-slim border-l border-white/15">
        {/* Mobile Header Banner */}
        <div className="relative z-10 lg:hidden mb-4 flex items-center justify-between pb-3 border-b border-white/15">
          <Link href="/" className="flex items-center gap-2.5">
            <div className="flex h-9 w-9 items-center justify-center rounded-lg bg-white/15 backdrop-blur-md border border-white/20">
              <BrandLogo className="h-5 w-5 text-emerald-400" />
            </div>
            <div>
              <span className="font-bold text-base text-white tracking-wide">RTMS</span>
              <p className="text-[11px] text-emerald-300 font-medium">Riverside Training Club</p>
            </div>
          </Link>
        </div>

        {/* Center Frosted Glass Card with Glass Highlights */}
        <div className={`relative z-10 my-auto mx-auto w-full ${maxWidthClass} animate-in fade-in slide-in-from-bottom-2 duration-300`}>
          <div className="rounded-2xl border border-white/60 bg-white/80 backdrop-blur-2xl p-6 sm:p-7 shadow-[0_25px_60px_-15px_rgba(0,0,0,0.4),inset_0_1px_1px_rgba(255,255,255,0.7)]">
            <div className="mb-5">
              <p className="text-xs font-bold uppercase tracking-[0.16em] text-[var(--color-primary)]">
                Riverside Training Management
              </p>
              <h1 className="mt-1 text-2xl font-bold tracking-tight text-[var(--color-text-primary)]">
                {title}
              </h1>
              <p className="mt-1 text-xs sm:text-sm text-[var(--color-text-secondary)] leading-relaxed">
                {description}
              </p>
            </div>

            {children}

            {footer && (
              <p className="mt-5 text-center text-xs text-[var(--color-text-muted)] border-t border-[var(--color-border)]/60 pt-3.5">
                {footer}
              </p>
            )}
          </div>

          {note && (
            <p className="mt-3.5 text-center text-[11px] text-white/80 drop-shadow-sm leading-relaxed px-2">
              {note}
            </p>
          )}
        </div>

        {/* Subtle Bottom Copyright */}
        <div className="relative z-10 mt-6 text-center text-[11px] text-white/50 drop-shadow-sm">
          &copy; {new Date().getFullYear()} Riverside Racehorse Training Management System. All rights reserved.
        </div>
      </section>
    </main>
  );
}
