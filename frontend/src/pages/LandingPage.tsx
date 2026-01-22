import React from 'react';
import { Link } from 'react-router-dom';
import { FileText, Search, MessageSquare, BarChart3, Shield, Zap, CheckCircle } from 'lucide-react';
import { Button } from '../components/ui';
import { ROUTES } from '../config/constants';

const LandingPage: React.FC = () => {
  const features = [
    {
      icon: FileText,
      title: 'Document Management',
      description: 'Upload, organize, and version your documents with automatic text extraction and processing.'
    },
    {
      icon: Search,
      title: 'Semantic Search',
      description: 'Find information using natural language queries powered by AI embeddings.'
    },
    {
      icon: MessageSquare,
      title: 'AI Question Answering',
      description: 'Ask questions and get accurate answers with citations from your documents.'
    },
    {
      icon: BarChart3,
      title: 'Analytics Dashboard',
      description: 'Track document statistics, keyword frequencies, and usage patterns.'
    },
    {
      icon: Shield,
      title: 'Secure & Private',
      description: 'Enterprise-grade security with JWT authentication and per-user data isolation.'
    },
    {
      icon: Zap,
      title: 'Real-time Updates',
      description: 'Get instant notifications about document processing and system events.'
    }
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
    <div className="min-h-screen bg-white">
      {/* Header */}
      <header className="border-b border-slate-200">
        <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-4 flex items-center justify-between">
          <h1 className="text-xl font-bold text-slate-900">Kortex</h1>
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

      {/* Hero Section */}
      <section className="py-20 px-4 sm:px-6 lg:px-8">
        <div className="max-w-4xl mx-auto text-center">
          <h2 className="text-5xl font-bold text-slate-900 mb-6">
            AI-Powered Document
            <br />
            Management for Researchers
          </h2>
          <p className="text-xl text-slate-600 mb-8 max-w-2xl mx-auto">
            Organize your academic documents, search using natural language, and get instant answers with AI-powered citations.
          </p>
          <div className="flex items-center justify-center gap-4">
            <Link to={ROUTES.REGISTER}>
              <Button size="lg">
                Get Started Free
              </Button>
            </Link>
            <Link to={ROUTES.LOGIN}>
              <Button size="lg" variant="outline">
                Sign in
              </Button>
            </Link>
          </div>
        </div>
      </section>

      {/* Features Grid */}
      <section className="py-16 px-4 sm:px-6 lg:px-8 bg-slate-50">
        <div className="max-w-7xl mx-auto">
          <h3 className="text-3xl font-bold text-slate-900 text-center mb-12">
            Everything you need to manage your research
          </h3>
          <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-8">
            {features.map((feature) => (
              <div key={feature.title} className="bg-white p-6 rounded-lg border border-slate-200 hover:shadow-md transition-shadow">
                <feature.icon size={32} className="text-slate-700 mb-4" />
                <h4 className="text-lg font-semibold text-slate-900 mb-2">
                  {feature.title}
                </h4>
                <p className="text-slate-600 text-sm">
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
              <h3 className="text-3xl font-bold text-slate-900 mb-4">
                Built for researchers, powered by AI
              </h3>
              <p className="text-slate-600 mb-6">
                Kortex combines traditional document management with cutting-edge AI to help you work smarter, not harder.
              </p>
              <ul className="space-y-3">
                {benefits.map((benefit) => (
                  <li key={benefit} className="flex items-start gap-3">
                    <CheckCircle size={20} className="text-green-600 flex-shrink-0 mt-0.5" />
                    <span className="text-slate-700">{benefit}</span>
                  </li>
                ))}
              </ul>
            </div>
            <div className="bg-slate-100 rounded-lg p-8 text-center">
              <div className="text-5xl font-bold text-slate-900 mb-2">384</div>
              <div className="text-sm text-slate-600 mb-6">Embedding Dimensions</div>
              <div className="text-5xl font-bold text-slate-900 mb-2">&lt; 1s</div>
              <div className="text-sm text-slate-600 mb-6">Search Response Time</div>
              <div className="text-5xl font-bold text-slate-900 mb-2">99.9%</div>
              <div className="text-sm text-slate-600">Uptime</div>
            </div>
          </div>
        </div>
      </section>

      {/* Tech Stack */}
      <section className="py-16 px-4 sm:px-6 lg:px-8 bg-slate-50">
        <div className="max-w-4xl mx-auto text-center">
          <h3 className="text-2xl font-bold text-slate-900 mb-8">
            Built with modern technology
          </h3>
          <div className="grid grid-cols-2 md:grid-cols-4 gap-6 text-sm text-slate-600">
            <div>
              <div className="font-semibold text-slate-900 mb-1">Backend</div>
              <div>Java 21</div>
              <div>Spring Boot</div>
            </div>
            <div>
              <div className="font-semibold text-slate-900 mb-1">Database</div>
              <div>PostgreSQL</div>
              <div>Qdrant</div>
            </div>
            <div>
              <div className="font-semibold text-slate-900 mb-1">AI</div>
              <div>HuggingFace</div>
              <div>Groq LLM</div>
            </div>
            <div>
              <div className="font-semibold text-slate-900 mb-1">Frontend</div>
              <div>React</div>
              <div>TypeScript</div>
            </div>
          </div>
        </div>
      </section>

      {/* CTA Section */}
      <section className="py-20 px-4 sm:px-6 lg:px-8">
        <div className="max-w-4xl mx-auto text-center">
          <h3 className="text-4xl font-bold text-slate-900 mb-6">
            Ready to transform your research workflow?
          </h3>
          <p className="text-xl text-slate-600 mb-8">
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
      <footer className="border-t border-slate-200 py-8 px-4 sm:px-6 lg:px-8">
        <div className="max-w-7xl mx-auto text-center text-sm text-slate-600">
          <p>© 2026 Kortex. AI-Powered Document Management System.</p>
        </div>
      </footer>
    </div>
  );
};

export default LandingPage;
