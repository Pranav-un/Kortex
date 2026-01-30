import React from 'react';
import './BentoGrid.css';

type BentoCardProps = {
  Icon?: React.ComponentType<React.SVGProps<SVGSVGElement>>;
  name: string;
  description?: string;
  href?: string;
  cta?: string;
  className?: string;
  background?: React.ReactNode;
  onClick?: () => void;
  children?: React.ReactNode;
};

export function BentoGrid({ children, className = '' }: { children: React.ReactNode; className?: string }) {
  return <div className={`mx-auto bento-grid-container ${className}`}>{children}</div>;
}

export function BentoCard({ Icon, name, description, href, cta, className = '', background, onClick, children }: BentoCardProps) {
  const Content = (
    <div className={`bento-card ${className}`} role={onClick ? 'button' : undefined} onClick={onClick} tabIndex={onClick ? 0 : -1}>
      {background ? <div className="bento-bg">{background}</div> : null}
      <div className="bento-body">
        <div className="bento-header">
          {Icon ? <Icon className="bento-icon" width={18} height={18} /> : null}
          <h3 className="bento-title">{name}</h3>
        </div>
        {description ? <p className="bento-desc">{description}</p> : null}
        {children}
        {cta && href ? (
          <a className="bento-cta" href={href} onClick={(e) => e.stopPropagation()}>
            {cta}
          </a>
        ) : null}
      </div>
    </div>
  );

  return Content;
}

export default BentoGrid;
