import React from 'react';
import { View, Text, StyleSheet, ScrollView, StatusBar, Dimensions } from 'react-native';
import { Feather } from '@expo/vector-icons';
import { LinearGradient } from 'expo-linear-gradient';
import { useAppContext } from '../context/AppContext';
import { COLORS, SPACING, FONT_SIZES, BORDER_RADIUS, SHADOWS } from '../utils/theme';
import { formatCurrency } from '../utils/helpers';

const { width } = Dimensions.get('window');

export default function AnalyticsScreen() {
  const { analytics, transactions } = useAppContext();
  const margin = analytics.totalRevenue > 0 ? ((analytics.grossProfit / analytics.totalRevenue) * 100).toFixed(1) : 0;

  // Mock chart data
  const chartData = [35, 52, 45, 68, 55, 80, 72];
  const chartLabels = ['Mon', 'Tue', 'Wed', 'Thu', 'Fri', 'Sat', 'Sun'];
  const maxVal = Math.max(...chartData);

  const MetricCard = ({ icon, label, value, color, bgColor }) => (
    <View style={[s.metricCard, { borderLeftColor: color }]}>
      <View style={[s.metricIcon, { backgroundColor: bgColor }]}>
        <Feather name={icon} size={18} color={color} />
      </View>
      <Text style={s.metricLabel}>{label}</Text>
      <Text style={[s.metricValue, { color }]}>{value}</Text>
    </View>
  );

  return (
    <View style={s.container}>
      <StatusBar barStyle="light-content" />
      <ScrollView showsVerticalScrollIndicator={false} contentContainerStyle={s.scroll}>
        <Text style={s.heading}>Analytics</Text>
        <Text style={s.sub}>Financial Intelligence Dashboard</Text>

        {/* Profit Card */}
        <LinearGradient colors={COLORS.gradientPrimary} start={{ x: 0, y: 0 }} end={{ x: 1, y: 1 }} style={s.profitCard}>
          <Text style={s.profitLabel}>Gross Profit</Text>
          <Text style={s.profitValue}>{formatCurrency(analytics.grossProfit)}</Text>
          <View style={s.profitBadge}>
            <Feather name="trending-up" size={14} color="#00D9A6" />
            <Text style={s.profitBadgeText}>{margin}% margin</Text>
          </View>
        </LinearGradient>

        {/* Metric Cards */}
        <View style={s.metricsGrid}>
          <MetricCard icon="trending-up" label="Total Revenue" value={formatCurrency(analytics.totalRevenue)} color={COLORS.success} bgColor={COLORS.successLight} />
          <MetricCard icon="trending-down" label="Total Cost" value={formatCurrency(analytics.totalCost)} color={COLORS.error} bgColor={COLORS.errorLight} />
          <MetricCard icon="users" label="Employee Cost" value={formatCurrency(analytics.totalEmployeeCost)} color={COLORS.info} bgColor={COLORS.infoLight} />
          <MetricCard icon="briefcase" label="Vendor Cost" value={formatCurrency(analytics.totalVendorCost)} color={COLORS.warning} bgColor={COLORS.warningLight} />
        </View>

        {/* Outstanding */}
        <View style={s.outstandingRow}>
          <View style={[s.outCard, { borderColor: COLORS.warning }]}>
            <Feather name="clock" size={20} color={COLORS.warning} />
            <Text style={s.outLabel}>Vendor Payables</Text>
            <Text style={[s.outValue, { color: COLORS.warning }]}>{formatCurrency(analytics.vendorPayables)}</Text>
          </View>
          <View style={[s.outCard, { borderColor: COLORS.info }]}>
            <Feather name="download" size={20} color={COLORS.info} />
            <Text style={s.outLabel}>Receivables</Text>
            <Text style={[s.outValue, { color: COLORS.info }]}>{formatCurrency(analytics.receivables)}</Text>
          </View>
        </View>

        {/* Chart */}
        <View style={s.chartCard}>
          <Text style={s.chartTitle}>Weekly Revenue Trend</Text>
          <View style={s.chart}>
            {chartData.map((val, i) => (
              <View key={i} style={s.chartCol}>
                <View style={s.barWrap}>
                  <LinearGradient colors={COLORS.gradientPrimary} style={[s.bar, { height: (val / maxVal) * 120 }]} />
                </View>
                <Text style={s.chartLabel}>{chartLabels[i]}</Text>
              </View>
            ))}
          </View>
        </View>

        {/* Recent Txns */}
        <Text style={s.sectionTitle}>All Transactions</Text>
        {transactions.map(txn => (
          <View key={txn.id} style={s.txnItem}>
            <View style={[s.txnDot, { backgroundColor: txn.type === 'receive' ? COLORS.successLight : COLORS.errorLight }]}>
              <Feather name={txn.type === 'receive' ? 'arrow-down-left' : 'arrow-up-right'} size={16} color={txn.type === 'receive' ? COLORS.success : COLORS.error} />
            </View>
            <View style={{ flex: 1 }}>
              <Text style={s.txnName}>{txn.userName}</Text>
              <Text style={s.txnDesc}>{txn.description} • {txn.date}</Text>
            </View>
            <Text style={[s.txnAmt, { color: txn.type === 'receive' ? COLORS.success : COLORS.error }]}>
              {txn.type === 'receive' ? '+' : '-'}{formatCurrency(txn.amount)}
            </Text>
          </View>
        ))}
        <View style={{ height: 100 }} />
      </ScrollView>
    </View>
  );
}

const s = StyleSheet.create({
  container: { flex: 1, backgroundColor: COLORS.background, paddingTop: 60 },
  scroll: { paddingHorizontal: SPACING.xl },
  heading: { fontSize: FONT_SIZES.heading, fontWeight: '800', color: COLORS.textPrimary },
  sub: { fontSize: FONT_SIZES.body, color: COLORS.textSecondary, marginBottom: SPACING.xxl },
  profitCard: { borderRadius: BORDER_RADIUS.xl, padding: SPACING.xxl, marginBottom: SPACING.xxl, ...SHADOWS.lg },
  profitLabel: { fontSize: FONT_SIZES.body, color: 'rgba(255,255,255,0.7)', marginBottom: 4 },
  profitValue: { fontSize: FONT_SIZES.hero, fontWeight: '800', color: '#FFF', marginBottom: SPACING.md },
  profitBadge: { flexDirection: 'row', alignItems: 'center', backgroundColor: 'rgba(0,217,166,0.15)', paddingHorizontal: SPACING.md, paddingVertical: SPACING.xs, borderRadius: BORDER_RADIUS.full, alignSelf: 'flex-start', gap: 4 },
  profitBadgeText: { fontSize: FONT_SIZES.small, color: '#00D9A6', fontWeight: '600' },
  metricsGrid: { flexDirection: 'row', flexWrap: 'wrap', gap: SPACING.md, marginBottom: SPACING.xxl },
  metricCard: { width: (width - SPACING.xl * 2 - SPACING.md) / 2, backgroundColor: COLORS.surface, borderRadius: BORDER_RADIUS.md, padding: SPACING.lg, borderLeftWidth: 3, borderWidth: 1, borderColor: COLORS.cardBorder },
  metricIcon: { width: 36, height: 36, borderRadius: 18, justifyContent: 'center', alignItems: 'center', marginBottom: SPACING.sm },
  metricLabel: { fontSize: FONT_SIZES.small, color: COLORS.textSecondary, marginBottom: 4 },
  metricValue: { fontSize: FONT_SIZES.medium, fontWeight: '700' },
  outstandingRow: { flexDirection: 'row', gap: SPACING.md, marginBottom: SPACING.xxl },
  outCard: { flex: 1, backgroundColor: COLORS.surface, borderRadius: BORDER_RADIUS.md, padding: SPACING.lg, alignItems: 'center', borderWidth: 1 },
  outLabel: { fontSize: FONT_SIZES.small, color: COLORS.textSecondary, marginVertical: SPACING.xs },
  outValue: { fontSize: FONT_SIZES.large, fontWeight: '800' },
  chartCard: { backgroundColor: COLORS.surface, borderRadius: BORDER_RADIUS.lg, padding: SPACING.xl, marginBottom: SPACING.xxl, borderWidth: 1, borderColor: COLORS.cardBorder },
  chartTitle: { fontSize: FONT_SIZES.medium, fontWeight: '700', color: COLORS.textPrimary, marginBottom: SPACING.lg },
  chart: { flexDirection: 'row', justifyContent: 'space-between', alignItems: 'flex-end', height: 140 },
  chartCol: { alignItems: 'center', flex: 1 },
  barWrap: { justifyContent: 'flex-end', height: 120 },
  bar: { width: 24, borderRadius: 12, minHeight: 8 },
  chartLabel: { fontSize: 10, color: COLORS.textTertiary, marginTop: 6 },
  sectionTitle: { fontSize: FONT_SIZES.large, fontWeight: '700', color: COLORS.textPrimary, marginBottom: SPACING.lg },
  txnItem: { flexDirection: 'row', alignItems: 'center', backgroundColor: COLORS.surface, padding: SPACING.md, borderRadius: BORDER_RADIUS.md, marginBottom: SPACING.sm, borderWidth: 1, borderColor: COLORS.cardBorder },
  txnDot: { width: 36, height: 36, borderRadius: 18, justifyContent: 'center', alignItems: 'center', marginRight: SPACING.md },
  txnName: { fontSize: FONT_SIZES.body, fontWeight: '600', color: COLORS.textPrimary },
  txnDesc: { fontSize: FONT_SIZES.caption, color: COLORS.textTertiary, marginTop: 2 },
  txnAmt: { fontSize: FONT_SIZES.body, fontWeight: '700' },
});
