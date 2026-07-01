import React, { useState } from 'react';
import { View, Text, StyleSheet, TouchableOpacity, TextInput, ScrollView, Alert, StatusBar, KeyboardAvoidingView, Platform } from 'react-native';
import { Feather } from '@expo/vector-icons';
import { LinearGradient } from 'expo-linear-gradient';
import { useAppContext } from '../context/AppContext';
import { COLORS, SPACING, FONT_SIZES, BORDER_RADIUS, SHADOWS } from '../utils/theme';
import { formatCurrency, calculateNetSalary, generateId } from '../utils/helpers';

export default function PayFlowScreen({ navigation }) {
  const { employees, vendors, addVendor, addTransaction, addInstalment, updateVendorBalance } = useAppContext();
  const [mode, setMode] = useState(null); // 'employee' | 'vendor'
  const [step, setStep] = useState(0);
  // Employee state
  const [selectedEmployee, setSelectedEmployee] = useState(null);
  const [lop, setLop] = useState('0');
  const [deductions, setDeductions] = useState('0');
  const [incentives, setIncentives] = useState('0');
  const [empDone, setEmpDone] = useState(false);
  // Vendor state
  const [selectedVendor, setSelectedVendor] = useState(null);
  const [showCreateVendor, setShowCreateVendor] = useState(false);
  const [newVendor, setNewVendor] = useState({ name: '', bankDetails: '', upi: '', outstandingBalance: 0 });
  const [vProduct, setVProduct] = useState('');
  const [vQty, setVQty] = useState('');
  const [vPrice, setVPrice] = useState('');
  const [vPayType, setVPayType] = useState('full');
  const [vPartial, setVPartial] = useState('');
  const [vDone, setVDone] = useState(false);

  const netSalary = selectedEmployee ? calculateNetSalary(selectedEmployee.grossSalary, lop, deductions, incentives) : 0;
  const vTotal = (Number(vQty) || 0) * (Number(vPrice) || 0);

  const processEmployeePayment = () => {
    addTransaction({ type: 'pay_employee', amount: Math.round(netSalary), date: new Date().toISOString().split('T')[0], status: 'completed', userId: selectedEmployee.id, userName: selectedEmployee.name, description: 'Monthly Salary' });
    setEmpDone(true);
  };

  const processVendorPayment = () => {
    if (!vProduct || !vQty || !vPrice) { Alert.alert('Required', 'Fill all fields'); return; }
    const payAmount = vPayType === 'full' ? vTotal : Number(vPartial) || 0;
    addTransaction({ type: 'pay_vendor', amount: payAmount, date: new Date().toISOString().split('T')[0], status: vPayType === 'full' ? 'completed' : 'partial', userId: selectedVendor.id, userName: selectedVendor.name, description: vProduct });
    if (vPayType === 'partial') {
      const rem = vTotal - payAmount;
      const dueDate = new Date(); dueDate.setMonth(dueDate.getMonth() + 1);
      addInstalment({ type: 'payable', vendorId: selectedVendor.id, vendorName: selectedVendor.name, totalAmount: vTotal, amountPaid: payAmount, remainingBalance: rem, nextDueDate: dueDate.toISOString().split('T')[0], emis: [{ emiNumber: 1, amount: rem, dueDate: dueDate.toISOString().split('T')[0], status: 'pending' }] });
      updateVendorBalance(selectedVendor.id, selectedVendor.outstandingBalance + rem);
    }
    setVDone(true);
  };

  const handleCreateVendor = () => {
    if (!newVendor.name) { Alert.alert('Required', 'Vendor name is required'); return; }
    const v = { ...newVendor, id: generateId(), outstandingBalance: 0 };
    addVendor(v); setSelectedVendor(v); setShowCreateVendor(false); setStep(2);
  };

  const BackButton = () => (
    <TouchableOpacity onPress={() => { if (empDone || vDone) navigation.goBack(); else if (step > 0) setStep(step - 1); else if (mode) setMode(null); else navigation.goBack(); }} style={s.backBtn}>
      <Feather name={empDone || vDone ? 'x' : 'arrow-left'} size={22} color={COLORS.textPrimary} />
    </TouchableOpacity>
  );

  // MODE SELECTION
  if (!mode) return (
    <View style={s.container}>
      <StatusBar barStyle="light-content" />
      <View style={s.topBar}><BackButton /><Text style={s.topTitle}>Pay</Text><View style={{ width: 40 }} /></View>
      <View style={s.content}>
        <Text style={s.heading}>Who do you want to pay?</Text>
        <TouchableOpacity onPress={() => { setMode('employee'); setStep(1); }} activeOpacity={0.85}>
          <LinearGradient colors={['#6C63FF', '#A855F7']} style={s.modeCard} start={{ x: 0, y: 0 }} end={{ x: 1, y: 1 }}>
            <View style={s.modeIcon}><Feather name="users" size={28} color="#FFF" /></View>
            <Text style={s.modeTitle}>Employee</Text>
            <Text style={s.modeDesc}>Process salary payments</Text>
          </LinearGradient>
        </TouchableOpacity>
        <TouchableOpacity onPress={() => { setMode('vendor'); setStep(1); }} activeOpacity={0.85}>
          <LinearGradient colors={['#FF6B6B', '#FFB547']} style={s.modeCard} start={{ x: 0, y: 0 }} end={{ x: 1, y: 1 }}>
            <View style={s.modeIcon}><Feather name="briefcase" size={28} color="#FFF" /></View>
            <Text style={s.modeTitle}>Vendor</Text>
            <Text style={s.modeDesc}>Pay for goods & services</Text>
          </LinearGradient>
        </TouchableOpacity>
      </View>
    </View>
  );

  // EMPLOYEE DONE
  if (mode === 'employee' && empDone) return (
    <View style={s.container}>
      <StatusBar barStyle="light-content" />
      <View style={s.topBar}><BackButton /><Text style={s.topTitle}>Payslip</Text><View style={{ width: 40 }} /></View>
      <ScrollView contentContainerStyle={[s.content, { alignItems: 'center' }]}>
        <View style={s.successCircle}><Feather name="check" size={40} color="#FFF" /></View>
        <Text style={s.successTitle}>Salary Processed!</Text>
        <Text style={s.successSub}>{formatCurrency(Math.round(netSalary))} paid to {selectedEmployee.name}</Text>
        <View style={s.slipCard}>
          <Text style={s.slipHeader}>PAYSLIP</Text>
          <View style={s.slipRow}><Text style={s.slipL}>Employee</Text><Text style={s.slipR}>{selectedEmployee.name}</Text></View>
          <View style={s.slipRow}><Text style={s.slipL}>Department</Text><Text style={s.slipR}>{selectedEmployee.department}</Text></View>
          <View style={s.slipDiv} />
          <View style={s.slipRow}><Text style={s.slipL}>Gross Salary</Text><Text style={s.slipR}>{formatCurrency(selectedEmployee.grossSalary)}</Text></View>
          <View style={s.slipRow}><Text style={s.slipL}>LOP ({lop} days)</Text><Text style={[s.slipR, { color: COLORS.error }]}>-{formatCurrency(Math.round(selectedEmployee.grossSalary / 30 * Number(lop)))}</Text></View>
          <View style={s.slipRow}><Text style={s.slipL}>Deductions</Text><Text style={[s.slipR, { color: COLORS.error }]}>-{formatCurrency(Number(deductions))}</Text></View>
          <View style={s.slipRow}><Text style={s.slipL}>Incentives</Text><Text style={[s.slipR, { color: COLORS.success }]}>+{formatCurrency(Number(incentives))}</Text></View>
          <View style={s.slipDiv} />
          <View style={s.slipRow}><Text style={[s.slipL, { fontWeight: '800' }]}>Net Payable</Text><Text style={[s.slipR, { fontWeight: '800', color: COLORS.success }]}>{formatCurrency(Math.round(netSalary))}</Text></View>
        </View>
        <TouchableOpacity onPress={() => navigation.goBack()} style={{ width: '100%' }}>
          <LinearGradient colors={COLORS.gradientPrimary} style={s.btn}><Feather name="home" size={18} color="#FFF" /><Text style={s.btnText}> Home</Text></LinearGradient>
        </TouchableOpacity>
      </ScrollView>
    </View>
  );

  // VENDOR DONE
  if (mode === 'vendor' && vDone) return (
    <View style={s.container}>
      <StatusBar barStyle="light-content" />
      <View style={s.topBar}><BackButton /><Text style={s.topTitle}>Payment Done</Text><View style={{ width: 40 }} /></View>
      <ScrollView contentContainerStyle={[s.content, { alignItems: 'center' }]}>
        <View style={s.successCircle}><Feather name="check" size={40} color="#FFF" /></View>
        <Text style={s.successTitle}>Payment Sent!</Text>
        <Text style={s.successSub}>{formatCurrency(vPayType === 'full' ? vTotal : Number(vPartial))} to {selectedVendor.name}</Text>
        {vPayType === 'partial' && <View style={s.warnBadge}><Feather name="clock" size={14} color={COLORS.warning} /><Text style={s.warnText}>Balance {formatCurrency(vTotal - Number(vPartial))} scheduled</Text></View>}
        <TouchableOpacity onPress={() => navigation.goBack()} style={{ width: '100%', marginTop: 20 }}>
          <LinearGradient colors={COLORS.gradientPrimary} style={s.btn}><Feather name="home" size={18} color="#FFF" /><Text style={s.btnText}> Home</Text></LinearGradient>
        </TouchableOpacity>
      </ScrollView>
    </View>
  );

  // EMPLOYEE FLOW
  if (mode === 'employee') return (
    <View style={s.container}>
      <StatusBar barStyle="light-content" />
      <View style={s.topBar}><BackButton /><Text style={s.topTitle}>Pay Employee</Text><View style={{ width: 40 }} /></View>
      <KeyboardAvoidingView style={{ flex: 1 }} behavior={Platform.OS === 'ios' ? 'padding' : undefined}>
        <ScrollView contentContainerStyle={s.content}>
          {step === 1 && <>
            <Text style={s.heading}>Select Employee</Text>
            {employees.map(emp => (
              <TouchableOpacity key={emp.id} style={s.listItem} onPress={() => { setSelectedEmployee(emp); setStep(2); }}>
                <View style={s.avatar}><Text style={s.avatarT}>{emp.name[0]}</Text></View>
                <View style={{ flex: 1 }}><Text style={s.itemName}>{emp.name}</Text><Text style={s.itemSub}>{emp.department} • {formatCurrency(emp.grossSalary)}/mo</Text></View>
                <Feather name="chevron-right" size={20} color={COLORS.textTertiary} />
              </TouchableOpacity>
            ))}
          </>}
          {step === 2 && selectedEmployee && <>
            <Text style={s.heading}>Salary Calculation</Text>
            <View style={s.badge}><Feather name="user" size={14} color={COLORS.primary} /><Text style={s.badgeText}>{selectedEmployee.name}</Text></View>
            <View style={s.card}><Text style={s.cardLabel}>Gross Salary</Text><Text style={s.cardValue}>{formatCurrency(selectedEmployee.grossSalary)}</Text></View>
            <Text style={s.fieldLabel}>LOP Days</Text>
            <TextInput style={s.input} keyboardType="numeric" value={lop} onChangeText={setLop} placeholder="0" placeholderTextColor={COLORS.textTertiary} />
            <Text style={s.fieldLabel}>Deductions (PF/ESI/Insurance)</Text>
            <TextInput style={s.input} keyboardType="numeric" value={deductions} onChangeText={setDeductions} placeholder="0" placeholderTextColor={COLORS.textTertiary} />
            <Text style={s.fieldLabel}>Incentives / Bonus</Text>
            <TextInput style={s.input} keyboardType="numeric" value={incentives} onChangeText={setIncentives} placeholder="0" placeholderTextColor={COLORS.textTertiary} />
            <View style={[s.card, { borderLeftWidth: 3, borderLeftColor: COLORS.success }]}><Text style={s.cardLabel}>Net Payable</Text><Text style={[s.cardValue, { color: COLORS.success }]}>{formatCurrency(Math.round(netSalary))}</Text></View>
            <TouchableOpacity onPress={processEmployeePayment}>
              <LinearGradient colors={COLORS.gradientAccent} style={s.btn}><Feather name="send" size={18} color="#FFF" /><Text style={s.btnText}> Process Payment</Text></LinearGradient>
            </TouchableOpacity>
          </>}
        </ScrollView>
      </KeyboardAvoidingView>
    </View>
  );

  // VENDOR FLOW
  return (
    <View style={s.container}>
      <StatusBar barStyle="light-content" />
      <View style={s.topBar}><BackButton /><Text style={s.topTitle}>Pay Vendor</Text><View style={{ width: 40 }} /></View>
      <KeyboardAvoidingView style={{ flex: 1 }} behavior={Platform.OS === 'ios' ? 'padding' : undefined}>
        <ScrollView contentContainerStyle={s.content}>
          {step === 1 && <>
            <Text style={s.heading}>Select Vendor</Text>
            <TouchableOpacity onPress={() => setShowCreateVendor(true)}>
              <LinearGradient colors={COLORS.gradientPrimary} start={{ x: 0, y: 0 }} end={{ x: 1, y: 0 }} style={s.createRow}><Feather name="plus-circle" size={18} color="#FFF" /><Text style={s.createRowText}>Create New Vendor</Text></LinearGradient>
            </TouchableOpacity>
            {showCreateVendor && (
              <View style={s.formCard}>
                <TextInput style={s.input} placeholder="Vendor Name *" placeholderTextColor={COLORS.textTertiary} value={newVendor.name} onChangeText={t => setNewVendor({ ...newVendor, name: t })} />
                <TextInput style={s.input} placeholder="Bank Details" placeholderTextColor={COLORS.textTertiary} value={newVendor.bankDetails} onChangeText={t => setNewVendor({ ...newVendor, bankDetails: t })} />
                <TextInput style={s.input} placeholder="UPI ID" placeholderTextColor={COLORS.textTertiary} value={newVendor.upi} onChangeText={t => setNewVendor({ ...newVendor, upi: t })} />
                <TouchableOpacity style={s.scanBtn} onPress={() => Alert.alert('OCR Simulation', 'Bank details extracted:\nHDFC - 9876543210\nIFSC: HDFC0001234', [{ text: 'Apply', onPress: () => setNewVendor({ ...newVendor, bankDetails: 'HDFC - 9876543210' }) }])}>
                  <Feather name="camera" size={16} color={COLORS.primary} /><Text style={s.scanText}>Scan QR / OCR</Text>
                </TouchableOpacity>
                <TouchableOpacity onPress={handleCreateVendor}><LinearGradient colors={COLORS.gradientAccent} style={s.btn}><Text style={s.btnText}>Save & Select</Text></LinearGradient></TouchableOpacity>
              </View>
            )}
            {vendors.map(v => (
              <TouchableOpacity key={v.id} style={s.listItem} onPress={() => { setSelectedVendor(v); setStep(2); }}>
                <View style={[s.avatar, { backgroundColor: COLORS.warningLight }]}><Text style={[s.avatarT, { color: COLORS.warning }]}>{v.name[0]}</Text></View>
                <View style={{ flex: 1 }}><Text style={s.itemName}>{v.name}</Text><Text style={s.itemSub}>Outstanding: {formatCurrency(v.outstandingBalance)}</Text></View>
                <Feather name="chevron-right" size={20} color={COLORS.textTertiary} />
              </TouchableOpacity>
            ))}
          </>}
          {step === 2 && selectedVendor && <>
            <Text style={s.heading}>Purchase Details</Text>
            <View style={s.badge}><Feather name="briefcase" size={14} color={COLORS.warning} /><Text style={[s.badgeText, { color: COLORS.warning }]}>{selectedVendor.name}</Text></View>
            <TextInput style={s.input} placeholder="Product Name" placeholderTextColor={COLORS.textTertiary} value={vProduct} onChangeText={setVProduct} />
            <View style={{ flexDirection: 'row', gap: 12 }}>
              <TextInput style={[s.input, { flex: 1 }]} placeholder="Qty" placeholderTextColor={COLORS.textTertiary} keyboardType="numeric" value={vQty} onChangeText={setVQty} />
              <TextInput style={[s.input, { flex: 1 }]} placeholder="Unit Price" placeholderTextColor={COLORS.textTertiary} keyboardType="numeric" value={vPrice} onChangeText={setVPrice} />
            </View>
            <View style={s.card}><Text style={s.cardLabel}>Total</Text><Text style={s.cardValue}>{formatCurrency(vTotal)}</Text></View>
            <View style={s.toggleRow}>
              <TouchableOpacity style={[s.toggle, vPayType === 'full' && s.toggleOn]} onPress={() => setVPayType('full')}><Text style={[s.toggleT, vPayType === 'full' && { color: '#FFF' }]}>Full</Text></TouchableOpacity>
              <TouchableOpacity style={[s.toggle, vPayType === 'partial' && s.toggleOn]} onPress={() => setVPayType('partial')}><Text style={[s.toggleT, vPayType === 'partial' && { color: '#FFF' }]}>Partial</Text></TouchableOpacity>
            </View>
            {vPayType === 'partial' && <TextInput style={s.input} placeholder="Amount to pay now" placeholderTextColor={COLORS.textTertiary} keyboardType="numeric" value={vPartial} onChangeText={setVPartial} />}
            <TouchableOpacity onPress={processVendorPayment}>
              <LinearGradient colors={COLORS.gradientWarm} style={s.btn}><Feather name="send" size={18} color="#FFF" /><Text style={s.btnText}> Process Payment</Text></LinearGradient>
            </TouchableOpacity>
          </>}
        </ScrollView>
      </KeyboardAvoidingView>
    </View>
  );
}

const s = StyleSheet.create({
  container: { flex: 1, backgroundColor: COLORS.background, paddingTop: 50 },
  topBar: { flexDirection: 'row', alignItems: 'center', justifyContent: 'space-between', paddingHorizontal: SPACING.xl, marginBottom: SPACING.lg },
  backBtn: { width: 40, height: 40, borderRadius: 20, backgroundColor: COLORS.surface, justifyContent: 'center', alignItems: 'center', borderWidth: 1, borderColor: COLORS.cardBorder },
  topTitle: { fontSize: FONT_SIZES.large, fontWeight: '700', color: COLORS.textPrimary },
  content: { paddingHorizontal: SPACING.xl, paddingBottom: 40 },
  heading: { fontSize: FONT_SIZES.title, fontWeight: '700', color: COLORS.textPrimary, marginBottom: SPACING.lg },
  modeCard: { borderRadius: BORDER_RADIUS.lg, padding: SPACING.xxl, alignItems: 'center', marginBottom: SPACING.lg, ...SHADOWS.md },
  modeIcon: { width: 56, height: 56, borderRadius: 28, backgroundColor: 'rgba(255,255,255,0.2)', justifyContent: 'center', alignItems: 'center', marginBottom: SPACING.md },
  modeTitle: { fontSize: FONT_SIZES.title, fontWeight: '700', color: '#FFF', marginBottom: 4 },
  modeDesc: { fontSize: FONT_SIZES.body, color: 'rgba(255,255,255,0.7)' },
  listItem: { flexDirection: 'row', alignItems: 'center', backgroundColor: COLORS.surface, padding: SPACING.lg, borderRadius: BORDER_RADIUS.md, marginBottom: SPACING.sm, borderWidth: 1, borderColor: COLORS.cardBorder },
  avatar: { width: 42, height: 42, borderRadius: 21, backgroundColor: COLORS.primaryGlow, justifyContent: 'center', alignItems: 'center', marginRight: SPACING.md },
  avatarT: { fontSize: FONT_SIZES.medium, fontWeight: '700', color: COLORS.primary },
  itemName: { fontSize: FONT_SIZES.body, fontWeight: '600', color: COLORS.textPrimary },
  itemSub: { fontSize: FONT_SIZES.small, color: COLORS.textSecondary, marginTop: 2 },
  badge: { flexDirection: 'row', alignItems: 'center', backgroundColor: COLORS.primaryGlow, paddingHorizontal: SPACING.md, paddingVertical: SPACING.sm, borderRadius: BORDER_RADIUS.full, alignSelf: 'flex-start', marginBottom: SPACING.lg, gap: 6 },
  badgeText: { fontSize: FONT_SIZES.small, color: COLORS.primary, fontWeight: '600' },
  card: { backgroundColor: COLORS.surface, borderRadius: BORDER_RADIUS.md, padding: SPACING.lg, marginBottom: SPACING.lg, borderWidth: 1, borderColor: COLORS.cardBorder, flexDirection: 'row', justifyContent: 'space-between', alignItems: 'center' },
  cardLabel: { fontSize: FONT_SIZES.body, color: COLORS.textSecondary },
  cardValue: { fontSize: FONT_SIZES.title, fontWeight: '800', color: COLORS.textPrimary },
  fieldLabel: { fontSize: FONT_SIZES.small, color: COLORS.textSecondary, marginBottom: SPACING.xs, fontWeight: '600' },
  input: { backgroundColor: COLORS.surfaceElevated, borderRadius: BORDER_RADIUS.sm, paddingHorizontal: SPACING.lg, paddingVertical: SPACING.md, color: COLORS.textPrimary, fontSize: FONT_SIZES.body, marginBottom: SPACING.md, borderWidth: 1, borderColor: COLORS.cardBorder },
  btn: { flexDirection: 'row', alignItems: 'center', justifyContent: 'center', paddingVertical: SPACING.lg, borderRadius: BORDER_RADIUS.md, gap: 4, ...SHADOWS.md },
  btnText: { color: '#FFF', fontWeight: '700', fontSize: FONT_SIZES.medium },
  successCircle: { width: 80, height: 80, borderRadius: 40, backgroundColor: COLORS.success, justifyContent: 'center', alignItems: 'center', marginBottom: SPACING.xxl, marginTop: SPACING.xxl },
  successTitle: { fontSize: FONT_SIZES.heading, fontWeight: '800', color: COLORS.textPrimary, marginBottom: SPACING.sm },
  successSub: { fontSize: FONT_SIZES.medium, color: COLORS.textSecondary, marginBottom: SPACING.lg },
  slipCard: { backgroundColor: COLORS.surface, borderRadius: BORDER_RADIUS.lg, padding: SPACING.xl, width: '100%', marginBottom: SPACING.xxl, borderWidth: 1, borderColor: COLORS.cardBorder },
  slipHeader: { fontSize: FONT_SIZES.small, fontWeight: '800', color: COLORS.primary, letterSpacing: 2, textAlign: 'center', marginBottom: SPACING.lg },
  slipRow: { flexDirection: 'row', justifyContent: 'space-between', marginBottom: SPACING.sm },
  slipL: { fontSize: FONT_SIZES.body, color: COLORS.textSecondary },
  slipR: { fontSize: FONT_SIZES.body, color: COLORS.textPrimary, fontWeight: '600' },
  slipDiv: { height: 1, backgroundColor: COLORS.divider, marginVertical: SPACING.sm },
  toggleRow: { flexDirection: 'row', gap: SPACING.md, marginBottom: SPACING.lg },
  toggle: { flex: 1, paddingVertical: SPACING.md, borderRadius: BORDER_RADIUS.md, backgroundColor: COLORS.surface, alignItems: 'center', borderWidth: 1, borderColor: COLORS.cardBorder },
  toggleOn: { backgroundColor: COLORS.primary, borderColor: COLORS.primary },
  toggleT: { fontWeight: '600', color: COLORS.textSecondary },
  createRow: { flexDirection: 'row', alignItems: 'center', justifyContent: 'center', paddingVertical: SPACING.md, borderRadius: BORDER_RADIUS.md, gap: 8, marginBottom: SPACING.lg },
  createRowText: { color: '#FFF', fontWeight: '600' },
  formCard: { backgroundColor: COLORS.surface, borderRadius: BORDER_RADIUS.lg, padding: SPACING.xl, marginBottom: SPACING.lg, borderWidth: 1, borderColor: COLORS.cardBorder },
  scanBtn: { flexDirection: 'row', alignItems: 'center', justifyContent: 'center', paddingVertical: SPACING.md, borderRadius: BORDER_RADIUS.sm, borderWidth: 1, borderColor: COLORS.primary, marginBottom: SPACING.md, gap: 8 },
  scanText: { color: COLORS.primary, fontWeight: '600' },
  warnBadge: { flexDirection: 'row', alignItems: 'center', backgroundColor: COLORS.warningLight, paddingHorizontal: SPACING.lg, paddingVertical: SPACING.sm, borderRadius: BORDER_RADIUS.full, gap: 6 },
  warnText: { color: COLORS.warning, fontWeight: '600', fontSize: FONT_SIZES.small },
});
