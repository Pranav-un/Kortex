import React, { useState } from 'react';
import { useNavigate, Link } from 'react-router-dom';
import { useAuth } from '../contexts/AuthContext';
import { authService } from '../services/authService';
import { ROUTES } from '../config/constants';
import { Button, Input } from '../components/ui';
import LightPillar from '../components/magicui/LightPillar';

const LoginPage: React.FC = () => {
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [error, setError] = useState('');
  const [loading, setLoading] = useState(false);
  const { login } = useAuth();
  const navigate = useNavigate();

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setError('');
    setLoading(true);

    try {
      await login({ email, password });
      // Redirect admins to Admin page, others to Dashboard
      const isAdmin = authService.isAdmin();
      navigate(isAdmin ? ROUTES.ADMIN : ROUTES.DASHBOARD);
    } catch (err: any) {
      setError(err.response?.data?.message || 'Login failed. Please check your credentials.');
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="min-h-screen bg-[#0A0316] relative overflow-hidden">
      <div className="absolute inset-0">
        <LightPillar
          topColor="#5227FF"
          bottomColor="#FF9FFC"
          intensity={1}
          rotationSpeed={0.3}
          glowAmount={0.002}
          pillarWidth={3}
          pillarHeight={0.4}
          noiseIntensity={0.5}
          pillarRotation={25}
          interactive={false}
          mixBlendMode="screen"
          quality="high"
        />
      </div>
      <div className="relative z-10 flex items-center justify-center px-4 py-24">
        <div className="max-w-md w-full">
          <div className="bg-[#0D0620]/90 backdrop-blur-sm p-8 rounded-xl border border-[#CF9EFF]/20 ring-1 ring-[#CF9EFF]/10 shadow-[0_0_40px_rgba(207,158,255,0.08)]">
            <div className="text-center mb-8">
              <h1 className="text-3xl font-bold text-transparent bg-clip-text bg-gradient-to-r from-[#CF9EFF] via-[#A98BFF] to-[#7A5CF5] mb-2">Sign in</h1>
              <p className="text-sm text-slate-300">Welcome back to Kortex</p>
            </div>

            <form onSubmit={handleSubmit} className="space-y-4">
              {error && (
                <div className="bg-red-50 border border-red-200 text-red-700 px-4 py-3 rounded-lg text-sm">{error}</div>
              )}

              <Input
                label="Email"
                type="email"
                required
                value={email}
                onChange={(e) => setEmail(e.target.value)}
                placeholder="you@example.com"
                variant="dark"
              />

              <Input
                label="Password"
                type="password"
                required
                value={password}
                onChange={(e) => setPassword(e.target.value)}
                placeholder="Enter your password"
                variant="dark"
              />

              <Button
                type="submit"
                fullWidth
                loading={loading}
                className="bg-gradient-to-r from-[#7A5CF5] to-[#A98BFF] hover:from-[#6C4DEB] hover:to-[#9576FF] text-white focus:ring-[#7A5CF5] shadow-[0_6px_18px_rgba(122,92,245,0.25)] rounded-md"
              >
                Sign in
              </Button>
            </form>

            <div className="mt-6 text-center">
              <p className="text-sm text-slate-300">
                Don't have an account?{' '}
                <Link to={ROUTES.REGISTER} className="font-medium text-[#CF9EFF] hover:underline">Sign up</Link>
              </p>
            </div>
          </div>

          <div className="mt-4 text-center">
            <Link to={ROUTES.HOME} className="text-sm text-slate-300 hover:text-slate-100">← Back to home</Link>
          </div>
        </div>
      </div>
    </div>
  );
};

export default LoginPage;
