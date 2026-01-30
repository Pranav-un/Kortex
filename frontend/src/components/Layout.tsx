import React from 'react';

interface LayoutProps {
  children: React.ReactNode;
}

export const Layout: React.FC<LayoutProps> = ({ children }) => {
  return (
    <div className="min-h-screen bg-[#0D0620]">
      <main>
        <div className="min-h-screen">
          {children}
        </div>
      </main>
    </div>
  );
};
