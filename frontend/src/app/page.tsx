/* eslint-disable no-restricted-syntax */
'use client';

import React, { useState, useEffect, useRef } from 'react';
import Image from 'next/image';
import { useRouter } from 'next/navigation';
import RippleDistortion from '@/components/ui/RippleDistortion';
import {
  Timer,
  Stethoscope,
  Eye,
  Menu,
  X,
  ChevronDown,
  ShieldCheck,
  ArrowRight,
  Quote,
  MapPin,
  Phone,
  Mail,
  Lock,
} from 'lucide-react';

export default function LandingPage() {
  const router = useRouter();

  // Scroll state for sticky header and mobile conversion bar
  const [isScrolled, setIsScrolled] = useState(false);
  const [isHeroPassed, setIsHeroPassed] = useState(false);
  const [isCtaInView, setIsCtaInView] = useState(false);
  const [mobileMenuOpen, setMobileMenuOpen] = useState(false);

  // Stats count-up state
  const [statsVisible, setStatsVisible] = useState(false);
  const [counts, setCounts] = useState({ acres: 0, soundness: 0, stakes: 0 });
  const statsRef = useRef<HTMLDivElement | null>(null);
  const finalCtaRef = useRef<HTMLElement | null>(null);

  // FAQ accordion state (one open at a time)
  const [openFaq, setOpenFaq] = useState<number | null>(0);

  // Scroll listener for sticky header and hero threshold
  useEffect(() => {
    const handleScroll = () => {
      setIsScrolled(window.scrollY > 24);
      setIsHeroPassed(window.scrollY > (window.innerHeight || 600) * 0.75);
    };
    window.addEventListener('scroll', handleScroll, { passive: true });
    handleScroll();
    return () => window.removeEventListener('scroll', handleScroll);
  }, []);

  // Observe final CTA section to hide mobile bottom bar
  useEffect(() => {
    const currentCta = finalCtaRef.current;
    if (!currentCta) return;

    const observer = new IntersectionObserver(
      ([entry]) => {
        setIsCtaInView(entry.isIntersecting);
      },
      { threshold: 0.1 }
    );

    observer.observe(currentCta);
    return () => observer.disconnect();
  }, []);

  // Stats IntersectionObserver with 0.1 threshold and rootMargin
  useEffect(() => {
    const currentRef = statsRef.current;
    if (!currentRef) return;

    const observer = new IntersectionObserver(
      (entries) => {
        if (entries[0].isIntersecting) {
          setStatsVisible(true);
          observer.disconnect();
        }
      },
      { threshold: 0.1, rootMargin: '0px 0px -50px 0px' }
    );

    observer.observe(currentRef);
    return () => observer.disconnect();
  }, []);

  useEffect(() => {
    if (!statsVisible) return;

    const duration = 1400;
    const start = performance.now();

    const frame = (now: number) => {
      const progress = Math.min((now - start) / duration, 1);
      const ease = 1 - Math.pow(1 - progress, 3);

      setCounts({
        acres: Math.floor(140 * ease),
        soundness: Number((99.4 * ease).toFixed(1)),
        stakes: Math.floor(47 * ease),
      });

      if (progress < 1) {
        requestAnimationFrame(frame);
      } else {
        setCounts({ acres: 140, soundness: 99.4, stakes: 47 });
      }
    };

    requestAnimationFrame(frame);
  }, [statsVisible]);

  // Smooth scroll helper
  const scrollToId = (id: string) => {
    setMobileMenuOpen(false);
    const element = document.getElementById(id);
    if (element) {
      element.scrollIntoView({ behavior: 'smooth' });
    }
  };

  // FAQ Data
  const faqs = [
    {
      q: 'How long is the biosecurity quarantine?',
      a: 'Fourteen days for domestic arrivals and twenty-one days for international imports. Every horse is housed in an isolated barn with dedicated staff, negative-pressure airflow, and comprehensive clinical clearances before joining the active roster.',
    },
    {
      q: 'Are owners permitted to visit their horses?',
      a: 'Owners are welcome for morning workouts on the viewing terrace and scheduled barn visits following quarantine clearance. Visits are scheduled through the portal and observe standard biosecurity protocols.',
    },
    {
      q: 'What telemetry instrumentation is utilized during training?',
      a: 'Each horse is equipped with a custom girth-mounted sensor pod capturing electrocardiogram telemetry, stride cadence, ground impact force, and velocity at 100 Hz without encumbrance.',
    },
    {
      q: 'How are syndication and boarding terms structured?',
      a: 'Syndicates are billed monthly at a transparent daily training rate, with clinical procedures itemized directly from the veterinary ledger. Each shareholder receives individualized portal access with shared financial transparency.',
    },
  ];

  return (
    <div className="min-h-screen bg-background text-foreground font-sans antialiased selection:bg-primary-soft selection:text-primary">
      {/* =========================================================================
          1. STICKY HEADER
         ========================================================================= */}
      <header
        className={`fixed top-0 left-0 right-0 z-50 transition-all duration-200 ${
          isScrolled
            ? 'bg-background/90 backdrop-blur-md border-b border-border py-3.5 shadow-sm'
            : 'bg-transparent py-5'
        }`}
      >
        <div className="max-w-7xl mx-auto px-6 lg:px-8">
          <div className="flex items-center justify-between">
            {/* Brand Logo with real /logo.png asset */}
            <a
              href="#hero"
              onClick={(e) => {
                e.preventDefault();
                scrollToId('hero');
              }}
              className="flex items-center gap-3 group focus:outline-none focus-visible:ring-2 focus-visible:ring-focus rounded-lg"
            >
              <div className="relative h-9 w-9 shrink-0 overflow-hidden rounded-full border border-border shadow-sm">
                <Image
                  src="/logo.png"
                  alt="RTMS"
                  fill
                  sizes="36px"
                  className="object-cover"
                  priority
                />
              </div>
              <div className="flex flex-col">
                <span
                  className={`text-xl font-bold tracking-wider leading-none transition-colors ${
                    isScrolled ? 'text-foreground' : 'text-white'
                  }`}
                >
                  RTMS
                </span>
                
              </div>
            </a>

            {/* Desktop Center Navigation */}
            <nav
              className={`hidden md:flex items-center gap-8 text-sm font-medium transition-colors ${
                isScrolled ? 'text-muted-foreground' : 'text-white/80'
              }`}
            >
              <button
                onClick={() => scrollToId('facilities')}
                className={`transition-colors cursor-pointer focus:outline-none focus-visible:ring-2 focus-visible:ring-focus rounded py-1 ${
                  isScrolled ? 'hover:text-foreground' : 'hover:text-white'
                }`}
              >
                Facilities
              </button>
              <button
                onClick={() => scrollToId('care')}
                className={`transition-colors cursor-pointer focus:outline-none focus-visible:ring-2 focus-visible:ring-focus rounded py-1 ${
                  isScrolled ? 'hover:text-foreground' : 'hover:text-white'
                }`}
              >
                Care
              </button>
              <button
                onClick={() => scrollToId('how-it-works')}
                className={`transition-colors cursor-pointer focus:outline-none focus-visible:ring-2 focus-visible:ring-focus rounded py-1 ${
                  isScrolled ? 'hover:text-foreground' : 'hover:text-white'
                }`}
              >
                Process
              </button>
              <button
                onClick={() => scrollToId('faq')}
                className={`transition-colors cursor-pointer focus:outline-none focus-visible:ring-2 focus-visible:ring-focus rounded py-1 ${
                  isScrolled ? 'hover:text-foreground' : 'hover:text-white'
                }`}
              >
                FAQ
              </button>
            </nav>

            {/* Desktop Action Buttons: "Login" (ghost/outline) + "Register" (solid primary) */}
            <div className="hidden md:flex items-center gap-3">
              <button
                onClick={() => router.push('/login')}
                className={`h-10 px-4 rounded-lg text-sm font-semibold transition-colors cursor-pointer focus:outline-none focus-visible:ring-2 focus-visible:ring-focus ${
                  isScrolled
                    ? 'border border-border text-foreground hover:bg-muted'
                    : 'border border-white/30 text-white hover:bg-white/10'
                }`}
              >
                Login
              </button>
              <button
                onClick={() => router.push('/register')}
                className="h-10 px-5 rounded-lg bg-primary hover:bg-primary-hover text-primary-foreground text-sm font-semibold shadow-sm transition-colors cursor-pointer focus:outline-none focus-visible:ring-2 focus-visible:ring-focus"
              >
                Register
              </button>
            </div>

            {/* Mobile Hamburger */}
            <button
              onClick={() => setMobileMenuOpen(!mobileMenuOpen)}
              className={`md:hidden p-2 transition-colors focus:outline-none focus-visible:ring-2 focus-visible:ring-focus rounded ${
                isScrolled ? 'text-foreground' : 'text-white'
              }`}
              aria-label="Toggle navigation drawer"
              aria-expanded={mobileMenuOpen}
            >
              {mobileMenuOpen ? <X className="w-6 h-6" strokeWidth={1.5} /> : <Menu className="w-6 h-6" strokeWidth={1.5} />}
            </button>
          </div>

          {/* Mobile Full-Width Drawer */}
          {mobileMenuOpen && (
            <div className="md:hidden mt-3 pt-4 pb-5 border-t border-border bg-card rounded-xl px-4 shadow-lg space-y-3">
              <button
                onClick={() => scrollToId('facilities')}
                className="block w-full text-left py-2 text-base font-medium text-foreground hover:text-primary"
              >
                Facilities
              </button>
              <button
                onClick={() => scrollToId('care')}
                className="block w-full text-left py-2 text-base font-medium text-foreground hover:text-primary"
              >
                Care
              </button>
              <button
                onClick={() => scrollToId('how-it-works')}
                className="block w-full text-left py-2 text-base font-medium text-foreground hover:text-primary"
              >
                Process
              </button>
              <button
                onClick={() => scrollToId('faq')}
                className="block w-full text-left py-2 text-base font-medium text-foreground hover:text-primary"
              >
                FAQ
              </button>
              <div className="pt-3 border-t border-border space-y-2">
                <button
                  onClick={() => router.push('/register')}
                  className="w-full h-10 rounded-lg bg-primary hover:bg-primary-hover text-primary-foreground text-sm font-semibold"
                >
                  Register
                </button>
                <button
                  onClick={() => router.push('/login')}
                  className="w-full h-10 rounded-lg border border-border text-foreground hover:bg-muted text-sm font-semibold"
                >
                  Login
                </button>
              </div>
            </div>
          )}
        </div>
      </header>

      <main id="hero">
        {/* =========================================================================
            2. HERO SECTION (Exactly one viewport: h-[100svh], min-h-[600px], max-h-[1080px])
           ========================================================================= */}
        <section className="relative h-[100svh] min-h-[600px] max-h-[1080px] flex flex-col justify-between overflow-hidden bg-black text-white">
          {/* Interactive WebGL RippleDistortion background */}
          <div className="absolute inset-0 z-0 bg-black">
            <RippleDistortion
              src="/hero.jpg"
              brushSize={60}
              strength={0.05}
              swirl={0.9}
              rings={2.25}
              grayscale={false}
              spread={4.5}
              fade={2.6}
              spacing={29}
              tintAmount={0.36}
              trigger="both"
              quality="medium"
              className="w-full h-full"
            />
            {/* Cinematic overlay layers: left-to-right (60% to 25% at 55% to transparent) and bottom gradient */}
            <div className="absolute inset-0 bg-gradient-to-r from-black/60 via-black/25 to-transparent via-[55%] pointer-events-none" />
            <div className="absolute bottom-0 inset-x-0 h-[30%] bg-gradient-to-t from-black/40 to-transparent pointer-events-none" />
          </div>

          {/* Content vertically centered in space under the 72px header */}
          <div className="relative z-10 flex-1 flex flex-col justify-center max-w-7xl mx-auto px-6 lg:px-8 w-full pt-[72px] pb-6">
            <div className="max-w-3xl space-y-4 md:space-y-6">
              {/* H1: clamp(2.5rem, 5.2vw, 5rem), max 3 lines on desktop, soft text-shadow */}
              <h1 className="text-[clamp(2.5rem,5.2vw,5rem)] font-semibold leading-[1.05] tracking-tight text-white text-balance max-w-3xl [text-shadow:_0_2px_24px_rgb(0_0_0_/_0.35)]">
                Where bloodline meets biometric science.
              </h1>

              {/* Subtitle: 18px desktop, 16px mobile, max-w-xl */}
              <p className="text-base md:text-lg text-white/90 font-normal max-w-xl leading-relaxed text-balance [text-shadow:_0_2px_24px_rgb(0_0_0_/_0.35)]">
                Elite conditioning and ICU-grade veterinary care for thoroughbreds, with live stride data and full transparency for every owner.
              </p>

              <div className="pt-2 flex flex-wrap items-center gap-4">
                <button
                  onClick={() => router.push('/register')}
                  className="h-12 px-8 rounded-lg bg-primary hover:bg-primary-hover text-primary-foreground text-sm font-semibold tracking-wide shadow-md transition-colors cursor-pointer flex items-center gap-2 focus:outline-none focus-visible:ring-2 focus-visible:ring-focus"
                >
                  <span>Get Started</span>
                  <ArrowRight className="w-4 h-4" strokeWidth={1.5} />
                </button>

                
              </div>
            </div>
          </div>

          {/* Small centered scroll cue at bottom of hero */}
          <div className="relative z-10 w-full pb-4 text-center">
            <button
              onClick={() => scrollToId('stats')}
              className="inline-flex flex-col items-center gap-1 text-white/80 hover:text-white transition-colors cursor-pointer focus:outline-none focus-visible:ring-2 focus-visible:ring-focus rounded py-1 px-2"
              aria-label="Scroll to next section"
            >
              <span className="text-[11px] font-medium uppercase tracking-[0.2em]">Scroll</span>
              <ChevronDown className="w-4 h-4 animate-bounce motion-reduce:animate-none" strokeWidth={1.5} />
            </button>
          </div>
        </section>

        {/* =========================================================================
            3. TRUST BAR (Deep primary brand surface)
           ========================================================================= */}
        <section
          id="stats"
          ref={statsRef}
          className="scroll-mt-24 relative overflow-hidden bg-[color-mix(in_oklab,var(--color-primary)_85%,black)] text-white py-24 lg:py-32"
        >
          {/* Soft radial glow */}
          <div className="absolute inset-0 pointer-events-none bg-[radial-gradient(ellipse_at_center,_var(--color-primary)_0%,_transparent_70%)] opacity-25" />

          <div className="relative z-10 max-w-7xl mx-auto px-6 lg:px-8">
            <div className="grid grid-cols-2 lg:grid-cols-4 gap-8 lg:gap-12 text-center">
              <div>
                <div className="font-metric text-5xl lg:text-6xl font-semibold tabular-nums text-white">
                  {counts.acres}+
                </div>
                <div className="text-sm text-white/70 font-medium mt-2">
                  Acres
                </div>
              </div>

              <div className="lg:border-l border-white/15">
                <div className="font-metric text-5xl lg:text-6xl font-semibold tabular-nums text-white">
                  {counts.soundness}%
                </div>
                <div className="text-sm text-white/70 font-medium mt-2">
                  Soundness rate
                </div>
              </div>

              <div className="lg:border-l border-white/15">
                <div className="font-metric text-5xl lg:text-6xl font-semibold tabular-nums text-white">
                  {counts.stakes}
                </div>
                <div className="text-sm text-white/70 font-medium mt-2">
                  Graded stakes wins
                </div>
              </div>

              <div className="lg:border-l border-white/15">
                <div className="text-5xl lg:text-6xl font-semibold text-white">
                  24/7
                </div>
                <div className="text-sm text-white/70 font-medium mt-2">
                  Resident vets
                </div>
              </div>
            </div>

            <div className="mt-14 pt-8 border-t border-white/15 flex items-center justify-center gap-2 text-sm text-white/70">
              <ShieldCheck className="w-4 h-4 text-white/90 shrink-0" strokeWidth={1.5} />
              <span>Accredited by the International Equine Federation · FEI Veterinary Standards</span>
            </div>
          </div>
        </section>

        {/* =========================================================================
            4. WHY RTMS (EXACTLY 3 LARGE CARDS)
           ========================================================================= */}
        <section id="facilities" className="scroll-mt-24 py-24 lg:py-32 bg-background">
          <div className="max-w-7xl mx-auto px-6 lg:px-8 space-y-12">
            {/* Centered header block */}
            <div className="max-w-2xl mx-auto text-center space-y-2">
              <span className="text-xs uppercase tracking-[0.2em] font-semibold text-primary block">
                Why RTMS
              </span>
              <h2 className="text-3xl sm:text-4xl lg:text-5xl font-semibold tracking-tight text-foreground text-balance">
                Precision in every furlong and clinical exam.
              </h2>
            </div>

            <div className="grid md:grid-cols-3 gap-8">
              {/* Card 1: Train */}
              <div className="group rounded-2xl border border-border bg-card overflow-hidden flex flex-col shadow-md transition-all duration-200 hover:-translate-y-1 hover:border-primary/40 hover:shadow-xl h-full">
                <div className="relative aspect-[4/3] w-full overflow-hidden bg-muted">
                  <Image
                    src="/images/training.jpg"
                    alt="Horses working on an all-weather gallop at sunrise"
                    fill
                    sizes="(max-width: 768px) 100vw, (max-width: 1200px) 33vw, 400px"
                    className="object-cover object-center filter saturate-[0.9] contrast-[1.05] group-hover:scale-105 transition-transform duration-500"
                  />
                </div>
                <div className="p-6 lg:p-8 flex-1 flex flex-col justify-between space-y-4">
                  <div className="space-y-3">
                    <div className="w-10 h-10 rounded-xl bg-primary-soft text-primary flex items-center justify-center">
                      <Timer className="w-5 h-5" strokeWidth={1.5} />
                    </div>
                    <h3 className="text-[22px] font-semibold text-card-foreground">Train</h3>
                    <p className="text-base text-muted-foreground leading-relaxed">
                      Eight furlongs of shock-absorbent Polytrack and turf gallops, with a conditioning plan built around each horse&apos;s stride.
                    </p>
                  </div>
                </div>
              </div>

              {/* Card 2: Heal */}
              <div id="care" className="scroll-mt-24 group rounded-2xl border border-border bg-card overflow-hidden flex flex-col shadow-md transition-all duration-200 hover:-translate-y-1 hover:border-primary/40 hover:shadow-xl h-full">
                <div className="relative aspect-[4/3] w-full overflow-hidden bg-muted">
                  <Image
                    src="/images/care.jpg"
                    alt="Veterinarian conducting an equine clinical examination"
                    fill
                    sizes="(max-width: 768px) 100vw, (max-width: 1200px) 33vw, 400px"
                    className="object-cover object-center filter saturate-[0.9] contrast-[1.05] group-hover:scale-105 transition-transform duration-500"
                  />
                </div>
                <div className="p-6 lg:p-8 flex-1 flex flex-col justify-between space-y-4">
                  <div className="space-y-3">
                    <div className="w-10 h-10 rounded-xl bg-primary-soft text-primary flex items-center justify-center">
                      <Stethoscope className="w-5 h-5" strokeWidth={1.5} />
                    </div>
                    <h3 className="text-[22px] font-semibold text-card-foreground">Heal</h3>
                    <p className="text-base text-muted-foreground leading-relaxed">
                      A 24/7 clinical ICU, endoscopic and radiographic checks, and a 14-day biosecure quarantine for every arrival.
                    </p>
                  </div>
                </div>
              </div>

              {/* Card 3: Know */}
              <div className="group rounded-2xl border border-border bg-card overflow-hidden flex flex-col shadow-md transition-all duration-200 hover:-translate-y-1 hover:border-primary/40 hover:shadow-xl h-full">
                <div className="relative aspect-[4/3] w-full overflow-hidden bg-muted">
                  <Image
                    src="/images/facility.jpg"
                    alt="Stable barn aisle with warm light and thoroughbred in stall"
                    fill
                    sizes="(max-width: 768px) 100vw, (max-width: 1200px) 33vw, 400px"
                    className="object-cover object-center filter saturate-[0.9] contrast-[1.05] group-hover:scale-105 transition-transform duration-500"
                  />
                </div>
                <div className="p-6 lg:p-8 flex-1 flex flex-col justify-between space-y-4">
                  <div className="space-y-3">
                    <div className="w-10 h-10 rounded-xl bg-primary-soft text-primary flex items-center justify-center">
                      <Eye className="w-5 h-5" strokeWidth={1.5} />
                    </div>
                    <h3 className="text-[22px] font-semibold text-card-foreground">Know</h3>
                    <p className="text-base text-muted-foreground leading-relaxed">
                      Live stall cameras, feed logs, vet reports and race readiness scores on your phone, any hour of the day.
                    </p>
                  </div>
                </div>
              </div>
            </div>
          </div>
        </section>

        {/* =========================================================================
            5. HOW IT WORKS (Process: bg-muted)
           ========================================================================= */}
        <section id="how-it-works" className="scroll-mt-24 py-24 lg:py-32 bg-muted border-t border-border">
          <div className="max-w-7xl mx-auto px-6 lg:px-8 space-y-12">
            {/* Centered header block */}
            <div className="max-w-2xl mx-auto text-center space-y-2">
              <span className="text-xs uppercase tracking-[0.2em] font-semibold text-primary block">
                Process
              </span>
              <h2 className="text-3xl sm:text-4xl lg:text-5xl font-semibold tracking-tight text-foreground text-balance">
                From admission to the active roster.
              </h2>
            </div>

            {/* Timeline Cards */}
            <div className="grid md:grid-cols-2 lg:grid-cols-4 gap-6 relative z-10">
              {/* Step 1 */}
              <div className="bg-card rounded-2xl p-6 border border-border shadow-sm flex flex-col items-start gap-4 transition-all duration-200 hover:-translate-y-0.5 hover:shadow-md">
                <div className="w-12 h-12 rounded-full border-2 border-primary bg-primary-soft text-primary font-semibold text-lg flex items-center justify-center shadow-sm shrink-0">
                  1
                </div>
                <div className="space-y-2">
                  <h3 className="text-lg font-semibold text-card-foreground">
                    Apply and verify pedigree
                  </h3>
                  <p className="text-sm text-muted-foreground leading-relaxed">
                    Submit registration papers and prior medical history for digital verification before arrival.
                  </p>
                </div>
              </div>

              {/* Step 2 */}
              <div className="bg-card rounded-2xl p-6 border border-border shadow-sm flex flex-col items-start gap-4 transition-all duration-200 hover:-translate-y-0.5 hover:shadow-md">
                <div className="w-12 h-12 rounded-full border-2 border-primary bg-primary-soft text-primary font-semibold text-lg flex items-center justify-center shadow-sm shrink-0">
                  2
                </div>
                <div className="space-y-2">
                  <h3 className="text-lg font-semibold text-card-foreground">
                    Biosecure quarantine and clinical assessment
                  </h3>
                  <p className="text-sm text-muted-foreground leading-relaxed">
                    Fourteen days of dedicated isolation with endoscopy, digital radiographs, and baseline blood panels.
                  </p>
                </div>
              </div>

              {/* Step 3 */}
              <div className="bg-card rounded-2xl p-6 border border-border shadow-sm flex flex-col items-start gap-4 transition-all duration-200 hover:-translate-y-0.5 hover:shadow-md">
                <div className="w-12 h-12 rounded-full border-2 border-primary bg-primary-soft text-primary font-semibold text-lg flex items-center justify-center shadow-sm shrink-0">
                  3
                </div>
                <div className="space-y-2">
                  <h3 className="text-lg font-semibold text-card-foreground">
                    Custom nutrition and conditioning plan
                  </h3>
                  <p className="text-sm text-muted-foreground leading-relaxed">
                    Our head trainer and equine nutritionist calibrate daily rations and gallop intervals to your horse.
                  </p>
                </div>
              </div>

              {/* Step 4 */}
              <div className="bg-card rounded-2xl p-6 border border-border shadow-sm flex flex-col items-start gap-4 transition-all duration-200 hover:-translate-y-0.5 hover:shadow-md">
                <div className="w-12 h-12 rounded-full border-2 border-primary bg-primary text-primary-foreground font-semibold text-lg flex items-center justify-center shadow-sm shrink-0">
                  4
                </div>
                <div className="space-y-2">
                  <h3 className="text-lg font-semibold text-card-foreground">
                    Active roster and graded race campaigning
                  </h3>
                  <p className="text-sm text-muted-foreground leading-relaxed">
                    Daily biometric monitoring with race readiness scores and workout splits delivered to your phone.
                  </p>
                </div>
              </div>
            </div>
          </div>
        </section>

        {/* =========================================================================
            6. OWNER DASHBOARD PREVIEW + PROOF (Deep primary brand surface)
           ========================================================================= */}
        <section className="relative overflow-hidden bg-[color-mix(in_oklab,var(--color-primary)_85%,black)] text-white py-24 lg:py-32">
          {/* Soft radial glow */}
          <div className="absolute inset-0 pointer-events-none bg-[radial-gradient(ellipse_at_center,_var(--color-primary)_0%,_transparent_70%)] opacity-30" />

          <div className="relative z-10 max-w-7xl mx-auto px-6 lg:px-8 space-y-12 lg:space-y-16">
            {/* Centered header block */}
            <div className="max-w-2xl mx-auto text-center space-y-2">
              <span className="text-xs uppercase tracking-[0.2em] font-semibold text-white/80 block">
                OWNER PORTAL
              </span>
              <h2 className="text-3xl sm:text-4xl lg:text-5xl font-semibold tracking-tight text-white text-balance">
                Your horse&apos;s day, on your phone.
              </h2>
              <p className="text-base text-white/70 max-w-xl mx-auto">
                Vitals, readiness scores and trainer notes, updated after every work.
              </p>
            </div>

            <div className="grid lg:grid-cols-12 gap-12 lg:gap-16 items-center">
              {/* Left Column: Mock Dashboard (Stays light bg-card with shadow-2xl) */}
              <div className="lg:col-span-7">
                <div className="rounded-2xl border border-white/10 bg-card p-6 lg:p-8 shadow-2xl space-y-6 text-card-foreground">
                  {/* Top Bar */}
                  <div className="flex items-center justify-between pb-6 border-b border-border">
                    <div>
                      <div className="text-2xl font-bold text-card-foreground">Gold Sovereign</div>
                      <div className="text-xs text-muted-foreground mt-1">Colt, 3yo · Stall 14B · Active Roster</div>
                    </div>

                    {/* Circular Readiness Meter (72px) */}
                    <div className="flex items-center gap-3">
                      <div className="relative w-[72px] h-[72px] flex items-center justify-center shrink-0">
                        <svg className="w-[72px] h-[72px] -rotate-90" viewBox="0 0 36 36">
                          <path
                            className="text-border"
                            strokeWidth="3.2"
                            stroke="currentColor"
                            fill="none"
                            d="M18 2.0845 a 15.9155 15.9155 0 0 1 0 31.831 a 15.9155 15.9155 0 0 1 0 -31.831"
                          />
                          <path
                            className="text-primary"
                            strokeDasharray="92, 100"
                            strokeWidth="3.2"
                            strokeLinecap="round"
                            stroke="currentColor"
                            fill="none"
                            d="M18 2.0845 a 15.9155 15.9155 0 0 1 0 31.831 a 15.9155 15.9155 0 0 1 0 -31.831"
                          />
                        </svg>
                        <span className="absolute font-metric text-xl font-bold text-card-foreground">92</span>
                      </div>
                      <div className="text-right">
                        <span className="text-[11px] uppercase tracking-wider text-primary font-semibold block">Readiness</span>
                        <span className="text-xs text-muted-foreground font-medium">Cleared for racing</span>
                      </div>
                    </div>
                  </div>

                  {/* Vitals Grid */}
                  <div className="grid grid-cols-3 gap-3.5">
                    <div className="rounded-xl border border-border bg-muted p-4">
                      <span className="text-xs uppercase tracking-wider text-muted-foreground block mb-1">
                        Resting HR
                      </span>
                      <div className="font-metric text-[28px] font-bold text-foreground">34 bpm</div>
                      <span className="text-xs text-primary font-medium">Baseline 36</span>
                    </div>

                    <div className="rounded-xl border border-border bg-muted p-4">
                      <span className="text-xs uppercase tracking-wider text-muted-foreground block mb-1">
                        Recovery Time
                      </span>
                      <div className="font-metric text-[28px] font-bold text-foreground">58 s</div>
                      <span className="text-xs text-primary font-medium">-4s vs average</span>
                    </div>

                    <div className="rounded-xl border border-border bg-muted p-4">
                      <span className="text-xs uppercase tracking-wider text-muted-foreground block mb-1">
                        Body Weight
                      </span>
                      <div className="font-metric text-[28px] font-bold text-foreground">512 kg</div>
                      <span className="text-xs text-primary font-medium">Target range</span>
                    </div>
                  </div>

                  {/* Trainer Note */}
                  <div className="rounded-xl border border-primary/20 bg-primary/5 p-5 space-y-1.5">
                    <div className="flex items-center justify-between text-xs text-primary font-semibold">
                      <span className="tracking-wide uppercase">Trainer Note · Morning gallop</span>
                      <span className="text-muted-foreground font-normal">07:15 EST</span>
                    </div>
                    <p className="text-[15px] text-card-foreground leading-relaxed">
                      Breezed five furlongs in 59.8 with clean lead changes. Recovery ahead of baseline. Cleared for Saturday.
                    </p>
                  </div>
                </div>
              </div>

              {/* Right Column: Testimonial in pure white text */}
              <div className="lg:col-span-5 space-y-6">
                <Quote className="w-12 h-12 text-white/20" strokeWidth={1.5} />
                <div className="text-[28px] lg:text-[32px] font-normal text-white leading-snug">
                  &ldquo;I open the app at midnight and see the vet&apos;s note before I have finished asking the question.&rdquo;
                </div>

                <div className="flex items-center gap-4 pt-2">
                  <div className="w-14 h-14 rounded-full border border-white/30 bg-white/10 flex items-center justify-center font-bold text-lg text-white shrink-0">
                    EW
                  </div>
                  <div>
                    <div className="font-semibold text-base text-white">Eleanor Whitcombe</div>
                    <div className="text-sm text-white/70 mt-0.5">Syndicate principal, Newmarket</div>
                  </div>
                </div>
              </div>
            </div>
          </div>
        </section>

        {/* =========================================================================
            7. FAQ (Two columns: bg-background)
           ========================================================================= */}
        <section id="faq" className="scroll-mt-24 py-24 lg:py-32 bg-background border-t border-border">
          <div className="max-w-7xl mx-auto px-6 lg:px-8">
            <div className="grid lg:grid-cols-12 gap-12 lg:gap-16 items-start">
              {/* Left Column (Sticky) */}
              <div className="lg:col-span-5 lg:sticky lg:top-28 lg:self-start space-y-4 text-left">
                <span className="text-xs uppercase tracking-[0.2em] font-semibold text-primary block">
                  FAQ
                </span>
                <h2 className="text-3xl sm:text-4xl font-semibold tracking-tight text-foreground">
                  Frequently asked questions
                </h2>
                <p className="text-sm text-muted-foreground">
                  Ready to get started?
                </p>
                <div>
                  <button
                    onClick={() => router.push('/register')}
                    className="inline-flex items-center gap-2 text-sm font-semibold text-primary hover:text-primary-hover transition-colors cursor-pointer group"
                  >
                    <span>Create an account</span>
                    <ArrowRight className="w-4 h-4 transition-transform group-hover:translate-x-1" strokeWidth={1.5} />
                  </button>
                </div>
              </div>

              {/* Right Column (Accordion) */}
              <div className="lg:col-span-7 divide-y divide-border border-y border-border">
                {faqs.map((faq, idx) => (
                  <div key={idx} className="py-6">
                    <button
                      onClick={() => setOpenFaq(openFaq === idx ? null : idx)}
                      className="w-full flex items-center justify-between text-left text-[17px] font-semibold text-foreground hover:text-primary transition-colors cursor-pointer gap-4 focus:outline-none focus-visible:ring-2 focus-visible:ring-focus rounded py-1"
                      aria-expanded={openFaq === idx}
                      aria-controls={`faq-answer-${idx}`}
                    >
                      <span>{faq.q}</span>
                      <ChevronDown
                        className={`w-5 h-5 text-primary shrink-0 transition-transform duration-200 ${
                          openFaq === idx ? 'rotate-180' : ''
                        }`}
                        strokeWidth={1.5}
                      />
                    </button>
                    {openFaq === idx && (
                      <div
                        id={`faq-answer-${idx}`}
                        className="mt-4 text-base text-muted-foreground leading-relaxed pr-6"
                      >
                        {faq.a}
                      </div>
                    )}
                  </div>
                ))}
              </div>
            </div>
          </div>
        </section>

        {/* =========================================================================
            8. FINAL CTA SECTION (Centered CTA over closing.jpg)
           ========================================================================= */}
        <section
          id="cta"
          ref={finalCtaRef}
          className="scroll-mt-24 relative min-h-[80svh] flex items-center justify-center py-24 lg:py-32 overflow-hidden border-t border-border"
        >
          {/* Background image: closing.jpg clearly visible with dark overlay */}
          <div className="absolute inset-0 -z-10 bg-black pointer-events-none">
            <Image
              src="/images/closing.jpg"
              alt="Thoroughbred standing quietly in paddock during golden hour"
              fill
              sizes="100vw"
              className="object-cover object-center"
            />
            {/* Center-weighted overlay for clean contrast */}
            <div className="absolute inset-0 bg-gradient-to-t from-black/80 via-black/55 to-black/70" />
          </div>

          <div className="max-w-5xl mx-auto px-6 lg:px-8 w-full text-center space-y-8">
            <div className="space-y-4">
              <span className="text-xs uppercase tracking-[0.2em] font-semibold text-white/90 block">
                Admissions &amp; Boarding
              </span>
              <h2 className="text-[clamp(2.5rem,4.5vw,4.5rem)] font-semibold text-white leading-tight max-w-3xl mx-auto text-balance [text-shadow:_0_2px_24px_rgb(0_0_0_/_0.35)]">
                Entrust your horse to the pinnacle of equine excellence.
              </h2>
              <p className="text-base sm:text-lg text-white/85 max-w-2xl mx-auto leading-relaxed [text-shadow:_0_2px_24px_rgb(0_0_0_/_0.35)]">
                Create your owner account to apply for a stall, follow every work and every vet note, and stay close to your horse from anywhere.
              </p>
            </div>

            {/* Dual CTA buttons */}
            <div className="flex flex-wrap items-center justify-center gap-4 pt-2">
              <button
                onClick={() => router.push('/register')}
                className="h-12 px-8 rounded-lg bg-primary hover:bg-primary-hover text-primary-foreground text-sm font-semibold tracking-wide shadow-md transition-colors cursor-pointer inline-flex items-center gap-2 focus:outline-none focus-visible:ring-2 focus-visible:ring-focus"
              >
                <span>Let go!</span>
                <ArrowRight className="w-4 h-4" strokeWidth={1.5} />
              </button>

              
            </div>

            {/* 3-item trust row */}
            <div className="pt-8 border-t border-white/15 flex flex-wrap items-center justify-center gap-6 sm:gap-10 text-sm text-white/80">
              <div className="flex items-center gap-2">
                <span className="h-1.5 w-1.5 rounded-full bg-primary" />
                <span>14-day biosecure quarantine</span>
              </div>
              <div className="flex items-center gap-2">
                <span className="h-1.5 w-1.5 rounded-full bg-primary" />
                <span>24/7 resident veterinarians</span>
              </div>
              <div className="flex items-center gap-2">
                <span className="h-1.5 w-1.5 rounded-full bg-primary" />
                <span>Live owner dashboard</span>
              </div>
            </div>
          </div>
        </section>
      </main>

      {/* =========================================================================
          9. FOOTER (Refined 4-column luxury layout with metadata & compliance)
         ========================================================================= */}
      <footer className="border-t border-border bg-card pt-16 pb-12 text-sm text-muted-foreground">
        <div className="max-w-7xl mx-auto px-6 lg:px-8">
          <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-12 gap-10 lg:gap-8 pb-12 border-b border-border">
            {/* Col 1: Brand & Accreditation (4 cols) */}
            <div className="lg:col-span-4 space-y-4">
              <div className="flex items-center gap-3">
                <div className="relative h-9 w-9 shrink-0 overflow-hidden rounded-full border border-border shadow-sm">
                  <Image
                    src="/logo.png"
                    alt="RTMS"
                    fill
                    sizes="36px"
                    className="object-cover"
                  />
                </div>
                <div className="flex flex-col">
                  <span className="text-xl font-bold tracking-wider leading-none text-foreground">
                    RTMS
                  </span>
                  
                </div>
              </div>

              <p className="text-sm text-muted-foreground leading-relaxed max-w-sm">
                Royal Thoroughbred Management &amp; Training System. Premier conditioning, ICU veterinary care, and biometric telemetry for thoroughbred racing syndicates.
              </p>

              <div className="flex items-center gap-2 pt-1 text-xs text-muted-foreground">
                <ShieldCheck className="w-4 h-4 text-primary shrink-0" strokeWidth={1.5} />
                <span>FEI Veterinary Standards Compliant</span>
              </div>
            </div>

            {/* Col 2: Facilities & Programs (3 cols) */}
            <div className="lg:col-span-3 space-y-3">
              <div className="text-xs font-semibold uppercase tracking-wider text-foreground">
                Programs &amp; Facilities
              </div>
              <ul className="space-y-2.5 text-sm">
                <li className="text-muted-foreground">8-Furlong Polytrack Gallop</li>
                <li className="text-muted-foreground">Turf Course &amp; Uphill Incline</li>
                <li className="text-muted-foreground">24/7 Clinical ICU &amp; Surgery</li>
                <li className="text-muted-foreground">Real-time Stride Telemetry Pods</li>
                <li className="text-muted-foreground">Biosecure Isolation Quarantine</li>
              </ul>
            </div>

            {/* Col 3: Navigation (2 cols) */}
            <div className="lg:col-span-2 space-y-3">
              <div className="text-xs font-semibold uppercase tracking-wider text-foreground">
                Navigation
              </div>
              <ul className="space-y-2.5 text-sm">
                <li>
                  <button
                    onClick={() => scrollToId('facilities')}
                    className="hover:text-primary transition-colors cursor-pointer text-left"
                  >
                    Facilities
                  </button>
                </li>
                <li>
                  <button
                    onClick={() => scrollToId('care')}
                    className="hover:text-primary transition-colors cursor-pointer text-left"
                  >
                    Clinical Care
                  </button>
                </li>
                <li>
                  <button
                    onClick={() => scrollToId('how-it-works')}
                    className="hover:text-primary transition-colors cursor-pointer text-left"
                  >
                    Process
                  </button>
                </li>
                <li>
                  <button
                    onClick={() => scrollToId('faq')}
                    className="hover:text-primary transition-colors cursor-pointer text-left"
                  >
                    FAQ
                  </button>
                </li>
                <li className="pt-1">
                  <button
                    onClick={() => router.push('/login')}
                    className="inline-flex items-center gap-1.5 text-xs font-semibold text-primary hover:text-primary-hover transition-colors cursor-pointer"
                  >
                    <Lock className="w-3 h-3" strokeWidth={1.5} />
                    <span>Client Portal</span>
                  </button>
                </li>
              </ul>
            </div>

            {/* Col 4: Facility Location & Contact (3 cols) */}
            <div className="lg:col-span-3 space-y-3">
              <div className="text-xs font-semibold uppercase tracking-wider text-foreground">
                Facility Location
              </div>
              <div className="space-y-2.5 text-sm">
                <div className="flex items-start gap-2.5">
                  <MapPin className="w-4 h-4 text-primary shrink-0 mt-0.5" strokeWidth={1.5} />
                  <span>1200 Paddock Lane, Lexington, KY 40511</span>
                </div>
                <div className="flex items-center gap-2.5">
                  <Phone className="w-4 h-4 text-primary shrink-0" strokeWidth={1.5} />
                  <span>+1 (859) 555-0142</span>
                </div>
                <div className="flex items-center gap-2.5">
                  <Mail className="w-4 h-4 text-primary shrink-0" strokeWidth={1.5} />
                  <span>contact@rtms.equine</span>
                </div>
                <div className="pt-2 text-xs text-muted-foreground border-t border-border mt-3">
                  Morning gallop viewings by advance appointment.
                </div>
              </div>
            </div>
          </div>

          {/* Bottom Bar: Copyright & Compliance */}
          <div className="pt-8 flex flex-col sm:flex-row items-center justify-between text-xs text-muted-foreground gap-4">
            <div>
              &copy; 2026 Royal Thoroughbred Management &amp; Training System. All rights reserved.
            </div>
            <div className="flex flex-wrap items-center gap-6">
              <a href="#hero" className="hover:text-primary transition-colors">Privacy Policy</a>
              <a href="#hero" className="hover:text-primary transition-colors">Terms of Service</a>
              <a href="#hero" className="hover:text-primary transition-colors">Biosecurity Protocol</a>
            </div>
          </div>
        </div>
      </footer>

      {/* =========================================================================
          10. MOBILE CONVERSION: FIXED BOTTOM BAR (PAST HERO, HIDDEN ON FINAL CTA)
         ========================================================================= */}
      {isHeroPassed && !isCtaInView && (
        <div className="md:hidden fixed bottom-0 inset-x-0 z-40 bg-background/95 backdrop-blur-md border-t border-border p-3 pb-[calc(0.75rem+env(safe-area-inset-bottom,0px))] shadow-2xl transition-all duration-300">
          <div className="flex flex-col gap-2">
            <button
              onClick={() => router.push('/register')}
              className="w-full h-12 rounded-lg bg-primary hover:bg-primary-hover text-primary-foreground text-sm font-semibold shadow-md transition-colors cursor-pointer flex items-center justify-center gap-2"
            >
              <span>Register</span>
              <ArrowRight className="w-4 h-4" strokeWidth={1.5} />
            </button>
            <div className="text-center">
              <button
                onClick={() => router.push('/login')}
                className="text-xs text-muted-foreground hover:text-foreground font-medium transition-colors cursor-pointer py-1"
              >
                Already an owner? <span className="text-primary font-semibold underline underline-offset-2">Login</span>
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}
