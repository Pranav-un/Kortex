import './Globe.css';
import createGlobe from 'cobe';
import { useEffect, useRef } from 'react';

type GlobeProps = {
  className?: string;
  config?: Partial<Parameters<typeof createGlobe>[1]>;
};

export default function Globe({ className = '', config }: GlobeProps) {
  const wrapRef = useRef<HTMLDivElement | null>(null);
  const canvasRef = useRef<HTMLCanvasElement | null>(null);

  useEffect(() => {
    const wrap = wrapRef.current!;
    const canvas = canvasRef.current!;
    let phi = 0;
    let width = wrap.clientWidth;
    let height = wrap.clientHeight;

    const resize = () => {
      width = wrap.clientWidth;
      height = wrap.clientHeight;
      const dpr = Math.min(2, window.devicePixelRatio || 1);
      canvas.width = width * dpr;
      canvas.height = height * dpr;
    };
    resize();

    let pointerDown = false;
    let pointerStartX = 0;
    let targetPhi = 0;
    let velocity = 0;

    const onPointerDown = (e: PointerEvent) => {
      pointerDown = true;
      pointerStartX = e.clientX;
      canvas.setPointerCapture(e.pointerId);
    };
    const onPointerMove = (e: PointerEvent) => {
      if (!pointerDown) return;
      const delta = e.clientX - pointerStartX;
      pointerStartX = e.clientX;
      targetPhi += delta / 200;
      velocity = delta / 6000;
    };
    const onPointerUp = (e: PointerEvent) => {
      pointerDown = false;
      canvas.releasePointerCapture(e.pointerId);
    };

    canvas.addEventListener('pointerdown', onPointerDown);
    canvas.addEventListener('pointermove', onPointerMove);
    canvas.addEventListener('pointerup', onPointerUp);
    const ro = new ResizeObserver(resize);
    ro.observe(wrap);

    const globe = createGlobe(canvas, {
      devicePixelRatio: Math.min(2, window.devicePixelRatio || 1),
      width: canvas.width,
      height: canvas.height,
      phi: 0,
      theta: 0.3,
      dark: 1,
      diffuse: 1.2,
      mapSamples: 16000,
      mapBrightness: 1,
      baseColor: [0.05, 0.02, 0.12],
      markerColor: [0.81, 0.62, 1.0],
      glowColor: [1, 1, 1],
      markers: [
        { location: [37.77, -122.42], size: 0.06 },
        { location: [1.29, 103.85], size: 0.06 },
        { location: [51.50, -0.12], size: 0.06 },
      ],
      onRender: (state) => {
        phi += 0.002 + velocity;
        velocity *= 0.98;
        phi += (targetPhi - phi) * 0.04;
        state.width = canvas.width;
        state.height = canvas.height;
        state.phi = phi;
      },
      ...(config as any),
    });

    return () => {
      ro.disconnect();
      canvas.removeEventListener('pointerdown', onPointerDown);
      canvas.removeEventListener('pointermove', onPointerMove);
      canvas.removeEventListener('pointerup', onPointerUp);
      globe.destroy();
    };
  }, [config]);

  return (
    <div ref={wrapRef} className={`globe-wrap ${className}`}>
      <canvas ref={canvasRef} className="globe-canvas" />
    </div>
  );
}
