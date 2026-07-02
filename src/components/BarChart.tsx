import React from "react";
import { useWindowDimensions, View } from "react-native";
import Svg, { Line, Rect, Text as SvgText } from "react-native-svg";
import { theme } from "../theme/theme";

export interface Bar {
  label: string;
  value: number; // any unit; signed values render above/below the baseline
}

interface BarChartProps {
  data: Bar[];
  height?: number;
  /** Format a value for the axis label (e.g. rupees). */
  format?: (v: number) => string;
}

/**
 * Minimal signed bar chart on react-native-svg — one implementation for native and web, no charting
 * dependency. Positive bars are green, negative red; a zero baseline is drawn when data crosses zero.
 */
export function BarChart({ data, height = 180, format }: BarChartProps): React.ReactElement {
  const { width: screenWidth } = useWindowDimensions();
  const width = Math.max(240, Math.min(screenWidth - theme.space(10), 640));
  const padding = { top: 16, bottom: 28, left: 8, right: 8 };
  const plotH = height - padding.top - padding.bottom;
  const plotW = width - padding.left - padding.right;

  if (data.length === 0) {
    return <View style={{ height }} />;
  }

  const maxAbs = Math.max(1, ...data.map((d) => Math.abs(d.value)));
  const hasNegative = data.some((d) => d.value < 0);
  const baselineY = padding.top + (hasNegative ? plotH / 2 : plotH);
  const maxBarH = hasNegative ? plotH / 2 : plotH;

  const slot = plotW / data.length;
  const barW = slot * 0.6;

  return (
    <Svg width={width} height={height} accessibilityLabel="bar chart">
      <Line x1={padding.left} y1={baselineY} x2={width - padding.right} y2={baselineY} stroke={theme.color.border} strokeWidth={1} />
      {data.map((d, i) => {
        const h = (Math.abs(d.value) / maxAbs) * maxBarH;
        const x = padding.left + i * slot + (slot - barW) / 2;
        const positive = d.value >= 0;
        const y = positive ? baselineY - h : baselineY;
        return (
          <React.Fragment key={`${d.label}-${i}`}>
            <Rect x={x} y={y} width={barW} height={Math.max(1, h)} rx={3} fill={positive ? theme.color.success : theme.color.danger} />
            <SvgText x={x + barW / 2} y={height - padding.bottom + 16} fontSize={10} fill={theme.color.textMuted} textAnchor="middle">
              {d.label}
            </SvgText>
          </React.Fragment>
        );
      })}
      {format ? (
        <SvgText x={padding.left} y={12} fontSize={10} fill={theme.color.textMuted}>
          max {format(maxAbs)}
        </SvgText>
      ) : null}
    </Svg>
  );
}
