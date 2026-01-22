// Minimal browser polyfills for libraries expecting Node globals
// Fixes errors like `global is not defined` from sockjs-client
(window as any).global = window;
(window as any).process = { env: {} };
