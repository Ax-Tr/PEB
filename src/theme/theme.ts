/** Design tokens shared across native and web. Single source for colour, spacing, and type. */
export const theme = {
  color: {
    bg: "#0B1020",
    surface: "#151B2E",
    surfaceAlt: "#1E2740",
    primary: "#4F7CFF",
    primaryText: "#FFFFFF",
    text: "#EAF0FF",
    textMuted: "#9AA6C6",
    border: "#2A3350",
    success: "#3DDC97",
    warning: "#F5A623",
    danger: "#FF5C7A",
  },
  space: (n: number) => n * 4,
  radius: { sm: 8, md: 12, lg: 20, pill: 999 },
  font: {
    h1: 28,
    h2: 22,
    title: 18,
    body: 15,
    caption: 12,
  },
} as const;

export type Theme = typeof theme;
