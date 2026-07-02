/**
 * Minimal ambient declaration for build-time env access. Expo inlines EXPO_PUBLIC_* vars into
 * process.env via its Babel transform; we avoid pulling in all of @types/node just for this.
 */
declare const process: {
  env: Record<string, string | undefined>;
};
