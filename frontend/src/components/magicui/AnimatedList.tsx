import { AnimatePresence, motion } from 'motion/react';
import React from 'react';

interface AnimatedListProps {
  children: React.ReactNode[] | React.ReactNode;
  className?: string;
  delay?: number; // ms between items
}

export default function AnimatedList({ children, className = '', delay = 1000 }: AnimatedListProps) {
  const items = Array.isArray(children) ? children : [children];

  return (
    <div className={`animated-list ${className}`}>
      <AnimatePresence>
        {items.map((child, idx) => (
          <motion.div
            key={idx}
            initial={{ opacity: 0, y: 10 }}
            animate={{ opacity: 1, y: 0 }}
            exit={{ opacity: 0, y: -10 }}
            transition={{ duration: 0.25, delay: (idx * delay) / 1000 }}
          >
            {child}
          </motion.div>
        ))}
      </AnimatePresence>
    </div>
  );
}
