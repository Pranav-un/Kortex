import React from 'react';
import { Link } from 'react-router-dom';
import { FileText, Search, MessageSquare, BarChart3, Shield, CheckCircle } from 'lucide-react';
import { Button } from '../components/ui';
import { ROUTES } from '../config/constants';
import LaserFlow from '../components/magicui/LaserFlow';
import './Hero.css';

const LandingPage: React.FC = () => {
  const features = [
    { icon: FileText, title: 'Document Management', description: 'Upload, organize, and version your documents with automatic text extraction and processing.' },
    { icon: Search, title: 'Semantic Search', description: 'Find information using natural language queries powered by AI embeddings.' },
    { icon: MessageSquare, title: 'AI Question Answering', description: 'Ask questions and get accurate answers with citations from your documents.' },
    { icon: BarChart3, title: 'Analytics Dashboard', description: 'Track document statistics, keyword frequencies, and usage patterns.' }
  ];

  const benefits = [
    'Automatic document summarization',
    'Intelligent keyword extraction',
    'Document clustering by topics',
    'Version control and history',
    'Multi-format support (PDF, DOCX, TXT)',
    'RESTful API for integrations'
  ];

  return (
    <div className="min-h-screen bg-[#0A0316] text-slate-200">
      {/* Header */}
      <header className="sticky top-0 z-30 backdrop-blur border-b border-[#1B1230]/60">
        <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-4 flex items-center justify-between">
          <Link to="/" className="flex items-center gap-2">
            <img src="/logo-placeholder.svg" alt="Kortex" className="h-6 w-auto" />
            <span className="sr-only">Kortex</span>
          </Link>
          <div className="flex items-center gap-4">
            <Link to={ROUTES.LOGIN}>
              <Button variant="ghost">Sign in</Button>
            </Link>
            <Link to={ROUTES.REGISTER}>
              <Button>Get Started</Button>
            </Link>
          </div>
        </div>
      </header>

      {/* Hero with LaserFlow */}
      <section className="relative overflow-hidden min-h-[100vh] flex items-center">
        <div className="absolute inset-0">
          <LaserFlow
            color="#CF9EFF"
            horizontalBeamOffset={0.06}
            verticalBeamOffset={0.0}
            horizontalSizing={0.5}
            verticalSizing={1.6}
            wispDensity={1}
            wispSpeed={15}
            wispIntensity={5}
            flowSpeed={0.35}
            flowStrength={0.25}
            fogIntensity={0.45}
            fogScale={0.3}
            fogFallSpeed={0.6}
            decay={1.1}
            falloffStart={1.2}
            className="h-full"
            style={{ backgroundColor: '#060010' }}
          />
        </div>

        <div className="relative z-10 max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-24">
          <div className="grid grid-cols-1 lg:grid-cols-2 gap-10 items-center">
            {/* Left copy */}
            <div className="-mt-10 md:-mt-10 lg:-mt-10 mb-6">
              <h2 className="text-5xl lg:text-6xl font-bold leading-tight mb-6 text-transparent bg-clip-text bg-gradient-to-r from-[#CF9EFF] via-[#A98BFF] to-[#7A5CF5]">
                Semantic Knowledge Retrieval
              </h2>
              <p className="text-lg/7 text-slate-300 max-w-xl mt-3 mb-8">
                Manage, search, and ask across your academic documents. Real citations, reliable answers, and lightning-fast semantic search — all in one place.
              </p>
              {/* Hero CTA and subtle badges (keeps layout locked) */}
              <div className="mt-6 flex items-center gap-4">
                <Link to={ROUTES.REGISTER}>
                  <Button size="md" className="bg-gradient-to-r from-[#7A5CF5] to-[#A98BFF] hover:from-[#6C4DEB] hover:to-[#9576FF] text-white focus:ring-[#7A5CF5] shadow-[0_6px_18px_rgba(122,92,245,0.25)] rounded-md">
                    Get Started
                  </Button>
                </Link>
                <Link to={ROUTES.LOGIN}>
                  <Button variant="ghost">Sign in</Button>
                </Link>
              </div>
              <div className="mt-4 flex flex-wrap items-center gap-3 text-sm">
                <span className="inline-flex items-center gap-2 px-3 py-1 rounded-full border border-[#CF9EFF]/25 text-slate-300/90">
                  <CheckCircle size={16} className="text-[#CF9EFF]" />
                  No credit card required
                </span>
                <span className="inline-flex items-center gap-2 px-3 py-1 rounded-full border border-[#CF9EFF]/25 text-slate-300/90">
                  <FileText size={16} className="text-[#CF9EFF]" />
                  Real citations included
                </span>
                <span className="inline-flex items-center gap-2 px-3 py-1 rounded-full border border-[#CF9EFF]/25 text-slate-300/90">
                  <Shield size={16} className="text-[#CF9EFF]" />
                  Secure by design
                </span>
              </div>
              {/* Hero CTAs and badges removed per request */}
            </div>

            {/* Keep right airy to showcase the beam */}
            <div className="hidden lg:block" />
          </div>

          {/* Bottom tray the beam hits at top-center */}
          <div className="mt-32 sm:mt-36 lg:mt-40 flex justify-center">
            <div className="beam-tray panel-dots rounded-2xl border border-[#CF9EFF]/25 bg-[#0D0620]/70 backdrop-blur-sm p-0 inline-block overflow-hidden">
              <img
                src="/placeholder-screenshot.svg"
                alt="Kortex app preview"
                className="block w-[1100px] max-w-full rounded-2xl border border-[#CF9EFF]/20 shadow-[0_0_60px_rgba(207,158,255,0.15)]"
                loading="eager"
                decoding="async"
              />
            </div>
          </div>
          </div>
      </section>

      {/* Features Grid */}
      <section className="py-24 px-4 sm:px-6 lg:px-8 bg-[#0D0620]">
        <div className="max-w-7xl mx-auto">
          <h3 className="text-3xl font-bold text-center mb-12 text-transparent bg-clip-text bg-gradient-to-r from-[#CF9EFF] via-[#A98BFF] to-[#7A5CF5]">
            Everything you need to manage your research
          </h3>
          <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-8">
            {features.map((feature) => (
              <div key={feature.title} className="bg-[#0A0316]/95 backdrop-blur-sm p-6 rounded-xl border border-[#CF9EFF]/15 hover:border-[#CF9EFF]/35 ring-1 ring-[#CF9EFF]/10 hover:ring-[#CF9EFF]/30 shadow-[0_0_40px_rgba(207,158,255,0.08)] hover:shadow-[0_0_60px_rgba(207,158,255,0.15)] transition-all">
                <feature.icon size={32} className="text-[#CF9EFF] opacity-90 mb-4" />
                <h4 className="text-lg font-semibold text-slate-100 mb-2">
                  {feature.title}
                </h4>
                <p className="text-slate-300/90 text-sm">
                  {feature.description}
                </p>
              </div>
            ))}
          </div>
        </div>
      </section>

      {/* Benefits Section */}
      <section className="py-16 px-4 sm:px-6 lg:px-8">
        <div className="max-w-5xl mx-auto">
          <div className="grid grid-cols-1 md:grid-cols-2 gap-8 items-center">
            <div>
              <h3 className="text-3xl font-bold text-slate-100 mb-4 text-transparent bg-clip-text bg-gradient-to-r from-[#CF9EFF] via-[#A98BFF] to-[#7A5CF5]">
                Built for researchers, powered by AI
              </h3>
              <p className="text-slate-300 mb-6">
                Kortex combines traditional document management with cutting-edge AI to help you work smarter, not harder.
              </p>
              <ul className="space-y-3">
                {benefits.map((benefit) => (
                  <li key={benefit} className="flex items-start gap-3">
                    <CheckCircle size={20} className="text-[#CF9EFF] flex-shrink-0 mt-0.5" />
                    <span className="text-slate-200/95">{benefit}</span>
                  </li>
                ))}
              </ul>
            </div>
            <div className="bg-[#0A0316]/90 backdrop-blur-sm rounded-xl p-8 text-center border border-[#CF9EFF]/20 ring-1 ring-[#CF9EFF]/10 shadow-[0_0_40px_rgba(207,158,255,0.08)]">
              <div className="text-5xl font-bold text-slate-100 mb-2">384</div>
              <div className="text-sm text-slate-300 mb-6">Embedding Dimensions</div>
              <div className="text-5xl font-bold text-slate-100 mb-2">&lt; 1s</div>
              <div className="text-sm text-slate-300 mb-6">Search Response Time</div>
              <div className="text-5xl font-bold text-slate-100 mb-2">99.9%</div>
              <div className="text-sm text-slate-300">Uptime</div>
            </div>
          </div>
        </div>
      </section>

      {/* Tech Stack */}
      <section className="py-16 px-4 sm:px-6 lg:px-8 bg-[#0D0620]">
        <div className="max-w-4xl mx-auto text-center">
          <h3 className="text-2xl font-bold text-slate-100 mb-8 text-transparent bg-clip-text bg-gradient-to-r from-[#CF9EFF] via-[#A98BFF] to-[#7A5CF5]">
            Built with modern technology
          </h3>
          <div className="grid grid-cols-2 md:grid-cols-4 gap-6 text-sm text-slate-300">
            <div>
              <div className="font-semibold text-slate-100 mb-1">Backend</div>
              <div>Java 21</div>
              <div>Spring Boot</div>
            </div>
            <div>
              <div className="font-semibold text-slate-100 mb-1">Database</div>
              <div>PostgreSQL</div>
              <div>Qdrant</div>
            </div>
            <div>
              <div className="font-semibold text-slate-100 mb-1">AI</div>
              <div>HuggingFace</div>
              <div>Groq LLM</div>
            </div>
            <div>
              <div className="font-semibold text-slate-100 mb-1">Frontend</div>
              <div>React</div>
              <div>TypeScript</div>
            </div>
          </div>
        </div>
      </section>

      {/* CTA Section */}
      <section className="py-20 px-4 sm:px-6 lg:px-8">
        <div className="max-w-4xl mx-auto text-center">
          <h3 className="text-4xl font-bold text-slate-100 mb-6">
            Ready to transform your research workflow?
          </h3>
          <p className="text-xl text-slate-300 mb-8">
            Join researchers who are already using Kortex to manage their documents smarter.
          </p>
          <Link to={ROUTES.REGISTER}>
            <Button size="lg">
              Get Started Free
            </Button>
          </Link>
        </div>
      </section>

      {/* Footer */}
      <footer className="border-t border-[#1B1230]/60 py-8 px-4 sm:px-6 lg:px-8">
        <div className="max-w-7xl mx-auto text-center text-sm text-slate-400">
          <p>© 2026 Kortex. AI-Powered Document Management System.</p>
        </div>
      </footer>
    </div>
  );
};

export default LandingPage;
