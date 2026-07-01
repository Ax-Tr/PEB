import React, { useState } from 'react';
import { View, Text, StyleSheet, TouchableOpacity, ScrollView, TextInput, Alert, StatusBar } from 'react-native';
import { Feather } from '@expo/vector-icons';
import { LinearGradient } from 'expo-linear-gradient';
import { useAppContext } from '../context/AppContext';
import { COLORS, SPACING, FONT_SIZES, BORDER_RADIUS, SHADOWS } from '../utils/theme';
import { formatCurrency, formatDate } from '../utils/helpers';

export default function InstalmentScreen({ navigation }) {
  const { instalments, updateInstalment } = useAppContext();
  const [tab, setTab] = useState('receivable');
  const filtered = instalments.filter(i => i.type === tab);

  return (
    <View style={s.container}>
      <StatusBar barStyle="light-content" />
      <View style={s.topBar}>
        <TouchableOpacity onPress={() => navigation.goBack()} style={s.backBtn}><Feather name="arrow-left" size={22} color={COLORS.textPrimary} /></TouchableOpacity>
        <Text style={s.topTitle}>Instalments</Text>
        <View style={{ width: 40 }} />
      </View>
      <View style={s.tabs}>
        <TouchableOpacity style={[s.tab, tab === 'receivable' && s.tabActive]} onPress={() => setTab('receivable')}>
          <Feather name="download" size={16} color={tab === 'receivable' ? '#FFF' : COLORS.textSecondary} />
          <Text style={[s.tabText, tab === 'receivable' && { color: '#FFF' }]}>Receivables</Text>
        </TouchableOpacity>
        <TouchableOpacity style={[s.tab, tab === 'payable' && s.tabActive]} onPress={() => setTab('payable')}>
          <Feather name="upload" size={16} color={tab === 'payable' ? '#FFF' : COLORS.textSecondary} />
          <Text style={[s.tabText, tab === 'payable' && { color: '#FFF' }]}>Payables</Text>
        </TouchableOpacity>
      </View>
      <ScrollView contentContainerStyle={s.content}>
        {filtered.length === 0 && (
          <View style={s.empty}><Feather name="inbox" size={48} color={COLORS.textTertiary} /><Text style={s.emptyText}>No {tab} instalments</Text></View>
        )}
        {filtered.map(inst => (
          <View key={inst.id} style={s.card}>
            <View style={s.cardHeader}>
              <View style={s.avatarWrap}><Text style={s.avatarText}>{(inst.customerName || inst.vendorName || '?')[0]}</Text></View>
              <View style={{ flex: 1 }}><Text style={s.name}>{inst.customerName || inst.vendorName}</Text>
                <Text style={s.sub}>Total: {formatCurrency(inst.totalAmount)}</Text></View>
            </View>
            <View style={s.progressTrack}>
              <View style={[s.progressBar, { width: `${((inst.amountReceived || inst.amountPaid || 0) / inst.totalAmount) * 100}%` }]} />
            </View>
            <View style={s.statsRow}>
              <View style={s.stat}><Text style={s.statLabel}>{tab === 'receivable' ? 'Received' : 'Paid'}</Text><Text style={[s.statVal, { color: COLORS.success }]}>{formatCurrency(inst.amountReceived || inst.amountPaid || 0)}</Text></View>
              <View style={s.stat}><Text style={s.statLabel}>Remaining</Text><Text style={[s.statVal, { color: COLORS.warning }]}>{formatCurrency(inst.remainingBalance)}</Text></View>
              <View style={s.stat}><Text style={s.statLabel}>EMIs</Text><Text style={s.statVal}>{inst.emis?.length || 0}</Text></View>
            </View>
            {inst.emis?.map((emi, idx) => (
              <View key={idx} style={s.emiItem}>
                <View style={[s.emiDot, { backgroundColor: emi.status === 'paid' ? COLORS.success : COLORS.warning }]} />
                <Text style={s.emiText}>EMI {emi.emiNumber}: {formatCurrency(emi.amount)}</Text>
                <Text style={s.emiDate}>{formatDate(emi.dueDate)}</Text>
                <View style={[s.emiStatus, { backgroundColor: emi.status === 'paid' ? COLORS.successLight : COLORS.warningLight }]}>
                  <Text style={{ fontSize: 10, color: emi.status === 'paid' ? COLORS.success : COLORS.warning, fontWeight: '600' }}>{emi.status.toUpperCase()}</Text>
                </View>
              </View>
            ))}
          </View>
        ))}
        <View style={{ height: 40 }} />
      </ScrollView>
    </View>
  );
}

const s = StyleSheet.create({
  container: { flex: 1, backgroundColor: COLORS.background, paddingTop: 50 },
  topBar: { flexDirection: 'row', alignItems: 'center', justifyContent: 'space-between', paddingHorizontal: SPACING.xl, marginBottom: SPACING.lg },
  backBtn: { width: 40, height: 40, borderRadius: 20, backgroundColor: COLORS.surface, justifyContent: 'center', alignItems: 'center', borderWidth: 1, borderColor: COLORS.cardBorder },
  topTitle: { fontSize: FONT_SIZES.large, fontWeight: '700', color: COLORS.textPrimary },
  tabs: { flexDirection: 'row', marginHorizontal: SPACING.xl, gap: SPACING.sm, marginBottom: SPACING.lg },
  tab: { flex: 1, flexDirection: 'row', alignItems: 'center', justifyContent: 'center', paddingVertical: SPACING.md, borderRadius: BORDER_RADIUS.md, backgroundColor: COLORS.surface, borderWidth: 1, borderColor: COLORS.cardBorder, gap: 6 },
  tabActive: { backgroundColor: COLORS.primary, borderColor: COLORS.primary },
  tabText: { fontSize: FONT_SIZES.body, fontWeight: '600', color: COLORS.textSecondary },
  content: { paddingHorizontal: SPACING.xl },
  empty: { alignItems: 'center', paddingVertical: SPACING.massive },
  emptyText: { color: COLORS.textTertiary, marginTop: SPACING.md, fontSize: FONT_SIZES.medium },
  card: { backgroundColor: COLORS.surface, borderRadius: BORDER_RADIUS.lg, padding: SPACING.xl, marginBottom: SPACING.lg, borderWidth: 1, borderColor: COLORS.cardBorder },
  cardHeader: { flexDirection: 'row', alignItems: 'center', marginBottom: SPACING.lg },
  avatarWrap: { width: 42, height: 42, borderRadius: 21, backgroundColor: COLORS.primaryGlow, justifyContent: 'center', alignItems: 'center', marginRight: SPACING.md },
  avatarText: { fontSize: FONT_SIZES.medium, fontWeight: '700', color: COLORS.primary },
  name: { fontSize: FONT_SIZES.medium, fontWeight: '700', color: COLORS.textPrimary },
  sub: { fontSize: FONT_SIZES.small, color: COLORS.textSecondary, marginTop: 2 },
  progressTrack: { height: 6, backgroundColor: COLORS.surfaceHighlight, borderRadius: 3, marginBottom: SPACING.lg },
  progressBar: { height: 6, backgroundColor: COLORS.success, borderRadius: 3 },
  statsRow: { flexDirection: 'row', marginBottom: SPACING.lg },
  stat: { flex: 1 },
  statLabel: { fontSize: FONT_SIZES.caption, color: COLORS.textTertiary, marginBottom: 2 },
  statVal: { fontSize: FONT_SIZES.body, fontWeight: '700', color: COLORS.textPrimary },
  emiItem: { flexDirection: 'row', alignItems: 'center', paddingVertical: SPACING.sm, borderTopWidth: 1, borderTopColor: COLORS.divider },
  emiDot: { width: 8, height: 8, borderRadius: 4, marginRight: SPACING.sm },
  emiText: { flex: 1, fontSize: FONT_SIZES.small, color: COLORS.textPrimary, fontWeight: '500' },
  emiDate: { fontSize: FONT_SIZES.caption, color: COLORS.textSecondary, marginRight: SPACING.sm },
  emiStatus: { paddingHorizontal: 8, paddingVertical: 2, borderRadius: BORDER_RADIUS.full },
});
