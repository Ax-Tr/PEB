import React, { useState, useRef } from 'react';
import {
  View, Text, StyleSheet, TouchableOpacity, TextInput, ScrollView,
  FlatList, Animated, Alert, Dimensions, StatusBar, KeyboardAvoidingView, Platform,
} from 'react-native';
import { Feather } from '@expo/vector-icons';
import { LinearGradient } from 'expo-linear-gradient';
import { useAppContext } from '../context/AppContext';
import { COLORS, SPACING, FONT_SIZES, BORDER_RADIUS, SHADOWS } from '../utils/theme';
import { formatCurrency, generateId } from '../utils/helpers';

const { width } = Dimensions.get('window');

export default function ReceiveFlowScreen({ navigation }) {
  const { customers, addCustomer, addTransaction, addInstalment } = useAppContext();
  const [step, setStep] = useState(1);
  const [searchQuery, setSearchQuery] = useState('');
  const [selectedCustomer, setSelectedCustomer] = useState(null);
  const [showCreateForm, setShowCreateForm] = useState(false);
  const [newCustomer, setNewCustomer] = useState({ name: '', mobile: '', email: '' });
  const [product, setProduct] = useState('');
  const [quantity, setQuantity] = useState('');
  const [unitPrice, setUnitPrice] = useState('');
  const [paymentType, setPaymentType] = useState('full');
  const [partialAmount, setPartialAmount] = useState('');
  const [numberOfEMIs, setNumberOfEMIs] = useState('2');
  const [paymentConfirmed, setPaymentConfirmed] = useState(false);
  const progressAnim = useRef(new Animated.Value(0.2)).current;

  const subtotal = (Number(quantity) || 0) * (Number(unitPrice) || 0);
  const amountToCollect = paymentType === 'full' ? subtotal : (Number(partialAmount) || 0);
  const remainingBalance = subtotal - amountToCollect;

  const filteredCustomers = customers.filter(
    (c) => c.name.toLowerCase().includes(searchQuery.toLowerCase()) ||
           c.mobile.includes(searchQuery)
  );

  const animateProgress = (toValue) => {
    Animated.spring(progressAnim, { toValue, friction: 8, useNativeDriver: false }).start();
  };

  const goToStep = (s) => { setStep(s); animateProgress(s * 0.2); };

  const handleCreateCustomer = () => {
    if (!newCustomer.name || !newCustomer.mobile) {
      Alert.alert('Required', 'Name and mobile are mandatory.');
      return;
    }
    const customer = { ...newCustomer, id: generateId() };
    addCustomer(customer);
    setSelectedCustomer(customer);
    setShowCreateForm(false);
    goToStep(2);
  };

  const handleSelectCustomer = (customer) => {
    setSelectedCustomer(customer);
    goToStep(2);
  };

  const handleProceedToPayment = () => {
    if (!product || !quantity || !unitPrice) {
      Alert.alert('Required', 'Please fill all product details.');
      return;
    }
    goToStep(3);
  };

  const handleGenerateQR = () => {
    if (paymentType === 'partial' && (!partialAmount || Number(partialAmount) <= 0)) {
      Alert.alert('Required', 'Please enter partial payment amount.');
      return;
    }
    if (paymentType === 'partial' && Number(partialAmount) >= subtotal) {
      Alert.alert('Invalid', 'Partial amount must be less than total.');
      return;
    }
    goToStep(4);
  };

  const handleConfirmPayment = () => {
    addTransaction({
      type: 'receive', amount: amountToCollect, date: new Date().toISOString().split('T')[0],
      status: paymentType === 'full' ? 'completed' : 'partial',
      userId: selectedCustomer.id, userName: selectedCustomer.name, description: product,
    });
    if (paymentType === 'partial' && remainingBalance > 0) {
      const emiCount = Number(numberOfEMIs) || 2;
      const emiAmount = Math.ceil(remainingBalance / emiCount);
      const emis = Array.from({ length: emiCount }, (_, i) => {
        const dueDate = new Date();
        dueDate.setMonth(dueDate.getMonth() + i + 1);
        return { emiNumber: i + 1, amount: emiAmount, dueDate: dueDate.toISOString().split('T')[0], status: 'pending' };
      });
      addInstalment({
        type: 'receivable', customerId: selectedCustomer.id, customerName: selectedCustomer.name,
        totalAmount: subtotal, amountReceived: amountToCollect, remainingBalance, numberOfEMIs: emiCount, emis,
      });
    }
    setPaymentConfirmed(true);
    goToStep(5);
  };

  const renderStepIndicator = () => (
    <View style={styles.stepContainer}>
      <View style={styles.stepTrack}>
        <Animated.View style={[styles.stepProgress, {
          width: progressAnim.interpolate({ inputRange: [0, 1], outputRange: ['0%', '100%'] })
        }]} />
      </View>
      <View style={styles.stepLabels}>
        {['Customer', 'Product', 'Payment', 'QR Code', 'Done'].map((label, i) => (
          <View key={i} style={styles.stepItem}>
            <View style={[styles.stepDot, step > i && styles.stepDotActive, step === i + 1 && styles.stepDotCurrent]}>
              {step > i + 1 ? <Feather name="check" size={10} color="#FFF" /> :
                <Text style={[styles.stepDotText, step >= i + 1 && { color: '#FFF' }]}>{i + 1}</Text>}
            </View>
            <Text style={[styles.stepLabel, step >= i + 1 && styles.stepLabelActive]}>{label}</Text>
          </View>
        ))}
      </View>
    </View>
  );

  const renderStep1 = () => (
    <View style={styles.stepContent}>
      <Text style={styles.stepTitle}>Select Customer</Text>
      <View style={styles.searchContainer}>
        <Feather name="search" size={18} color={COLORS.textTertiary} style={{ marginRight: 8 }} />
        <TextInput style={styles.searchInput} placeholder="Search by name or mobile..."
          placeholderTextColor={COLORS.textTertiary} value={searchQuery} onChangeText={setSearchQuery} />
      </View>
      <TouchableOpacity style={styles.createBtn} onPress={() => setShowCreateForm(true)}>
        <LinearGradient colors={COLORS.gradientPrimary} start={{ x: 0, y: 0 }} end={{ x: 1, y: 0 }} style={styles.createBtnGradient}>
          <Feather name="user-plus" size={18} color="#FFF" />
          <Text style={styles.createBtnText}>Create New Customer</Text>
        </LinearGradient>
      </TouchableOpacity>
      {showCreateForm && (
        <View style={styles.formCard}>
          <Text style={styles.formTitle}>New Customer</Text>
          <TextInput style={styles.input} placeholder="Full Name *" placeholderTextColor={COLORS.textTertiary}
            value={newCustomer.name} onChangeText={(t) => setNewCustomer({ ...newCustomer, name: t })} />
          <TextInput style={styles.input} placeholder="Mobile Number *" placeholderTextColor={COLORS.textTertiary}
            keyboardType="phone-pad" value={newCustomer.mobile}
            onChangeText={(t) => setNewCustomer({ ...newCustomer, mobile: t })} />
          <TextInput style={styles.input} placeholder="Email (Optional)" placeholderTextColor={COLORS.textTertiary}
            keyboardType="email-address" value={newCustomer.email}
            onChangeText={(t) => setNewCustomer({ ...newCustomer, email: t })} />
          <View style={{ flexDirection: 'row', gap: 10 }}>
            <TouchableOpacity style={[styles.actionBtn, { flex: 1, backgroundColor: COLORS.surfaceElevated }]}
              onPress={() => setShowCreateForm(false)}>
              <Text style={styles.actionBtnText}>Cancel</Text>
            </TouchableOpacity>
            <TouchableOpacity style={[styles.actionBtn, { flex: 1 }]} onPress={handleCreateCustomer}>
              <LinearGradient colors={COLORS.gradientAccent} style={styles.actionBtnGrad}>
                <Text style={styles.actionBtnTextWhite}>Save & Select</Text>
              </LinearGradient>
            </TouchableOpacity>
          </View>
        </View>
      )}
      <FlatList data={filteredCustomers} keyExtractor={(item) => item.id} scrollEnabled={false}
        renderItem={({ item }) => (
          <TouchableOpacity style={styles.customerItem} onPress={() => handleSelectCustomer(item)} activeOpacity={0.7}>
            <View style={styles.customerAvatar}>
              <Text style={styles.avatarText}>{item.name.charAt(0)}</Text>
            </View>
            <View style={{ flex: 1 }}>
              <Text style={styles.customerName}>{item.name}</Text>
              <Text style={styles.customerMobile}>{item.mobile}</Text>
            </View>
            <Feather name="chevron-right" size={20} color={COLORS.textTertiary} />
          </TouchableOpacity>
        )}
      />
    </View>
  );

  const renderStep2 = () => (
    <View style={styles.stepContent}>
      <Text style={styles.stepTitle}>Product / Service Details</Text>
      <View style={styles.selectedBadge}>
        <Feather name="user" size={14} color={COLORS.primary} />
        <Text style={styles.selectedBadgeText}>{selectedCustomer?.name}</Text>
      </View>
      <TextInput style={styles.input} placeholder="Product / Service Name" placeholderTextColor={COLORS.textTertiary}
        value={product} onChangeText={setProduct} />
      <View style={{ flexDirection: 'row', gap: 12 }}>
        <TextInput style={[styles.input, { flex: 1 }]} placeholder="Quantity" placeholderTextColor={COLORS.textTertiary}
          keyboardType="numeric" value={quantity} onChangeText={setQuantity} />
        <TextInput style={[styles.input, { flex: 1 }]} placeholder="Unit Price (₹)" placeholderTextColor={COLORS.textTertiary}
          keyboardType="numeric" value={unitPrice} onChangeText={setUnitPrice} />
      </View>
      <View style={styles.subtotalCard}>
        <Text style={styles.subtotalLabel}>Subtotal</Text>
        <Text style={styles.subtotalValue}>{formatCurrency(subtotal)}</Text>
      </View>
      <TouchableOpacity onPress={handleProceedToPayment}>
        <LinearGradient colors={COLORS.gradientPrimary} style={styles.primaryBtn}>
          <Text style={styles.primaryBtnText}>Continue</Text>
          <Feather name="arrow-right" size={18} color="#FFF" />
        </LinearGradient>
      </TouchableOpacity>
    </View>
  );

  const renderStep3 = () => (
    <View style={styles.stepContent}>
      <Text style={styles.stepTitle}>Payment Type</Text>
      <View style={styles.subtotalCard}>
        <Text style={styles.subtotalLabel}>Invoice Total</Text>
        <Text style={styles.subtotalValue}>{formatCurrency(subtotal)}</Text>
      </View>
      <View style={styles.toggleContainer}>
        <TouchableOpacity style={[styles.toggleBtn, paymentType === 'full' && styles.toggleActive]}
          onPress={() => setPaymentType('full')}>
          <Feather name="check-circle" size={18} color={paymentType === 'full' ? '#FFF' : COLORS.textSecondary} />
          <Text style={[styles.toggleText, paymentType === 'full' && styles.toggleTextActive]}>Full Payment</Text>
        </TouchableOpacity>
        <TouchableOpacity style={[styles.toggleBtn, paymentType === 'partial' && styles.toggleActive]}
          onPress={() => setPaymentType('partial')}>
          <Feather name="divide-circle" size={18} color={paymentType === 'partial' ? '#FFF' : COLORS.textSecondary} />
          <Text style={[styles.toggleText, paymentType === 'partial' && styles.toggleTextActive]}>Partial Payment</Text>
        </TouchableOpacity>
      </View>
      {paymentType === 'partial' && (
        <View style={styles.formCard}>
          <TextInput style={styles.input} placeholder="Amount to collect now (₹)" placeholderTextColor={COLORS.textTertiary}
            keyboardType="numeric" value={partialAmount} onChangeText={setPartialAmount} />
          <View style={styles.emiRow}>
            <Text style={styles.emiLabel}>Number of EMIs for balance:</Text>
            <TextInput style={[styles.input, { width: 80, marginBottom: 0 }]} keyboardType="numeric"
              value={numberOfEMIs} onChangeText={setNumberOfEMIs} />
          </View>
          {Number(partialAmount) > 0 && (
            <View style={styles.balanceInfo}>
              <Text style={styles.balanceInfoText}>Remaining: {formatCurrency(subtotal - Number(partialAmount))}</Text>
              <Text style={styles.balanceInfoText}>
                EMI: {formatCurrency(Math.ceil((subtotal - Number(partialAmount)) / (Number(numberOfEMIs) || 1)))} × {numberOfEMIs}
              </Text>
            </View>
          )}
        </View>
      )}
      <TouchableOpacity onPress={handleGenerateQR}>
        <LinearGradient colors={COLORS.gradientAccent} style={styles.primaryBtn}>
          <Text style={styles.primaryBtnText}>Generate QR Code</Text>
          <Feather name="arrow-right" size={18} color="#FFF" />
        </LinearGradient>
      </TouchableOpacity>
    </View>
  );

  const renderStep4 = () => (
    <View style={styles.stepContent}>
      <Text style={styles.stepTitle}>Scan to Pay</Text>
      <View style={styles.qrContainer}>
        <View style={styles.qrBox}>
          <View style={styles.qrMock}>
            {Array.from({ length: 8 }).map((_, row) => (
              <View key={row} style={{ flexDirection: 'row' }}>
                {Array.from({ length: 8 }).map((_, col) => (
                  <View key={col} style={{
                    width: 20, height: 20, margin: 1,
                    backgroundColor: (row + col) % 3 === 0 ? COLORS.primary :
                      (row * col) % 5 === 0 ? '#1a1a3e' : (row + col) % 2 === 0 ? '#333' : '#FFF',
                    borderRadius: 2,
                  }} />
                ))}
              </View>
            ))}
          </View>
          <Text style={styles.qrAmount}>{formatCurrency(amountToCollect)}</Text>
          <Text style={styles.qrUpi}>peb@upi • PayWithEase</Text>
        </View>
      </View>
      <TouchableOpacity onPress={handleConfirmPayment}>
        <LinearGradient colors={COLORS.gradientAccent} style={styles.primaryBtn}>
          <Feather name="check-circle" size={18} color="#FFF" />
          <Text style={styles.primaryBtnText}> Simulate Payment Confirmed</Text>
        </LinearGradient>
      </TouchableOpacity>
    </View>
  );

  const renderStep5 = () => (
    <View style={[styles.stepContent, { alignItems: 'center' }]}>
      <View style={styles.successIcon}>
        <Feather name="check" size={48} color="#FFF" />
      </View>
      <Text style={styles.successTitle}>Payment Received!</Text>
      <Text style={styles.successSubtitle}>{formatCurrency(amountToCollect)} from {selectedCustomer?.name}</Text>
      <View style={styles.invoiceCard}>
        <View style={styles.invoiceRow}><Text style={styles.invoiceLabel}>Invoice #</Text><Text style={styles.invoiceValue}>INV-{Date.now().toString().slice(-6)}</Text></View>
        <View style={styles.invoiceRow}><Text style={styles.invoiceLabel}>Product</Text><Text style={styles.invoiceValue}>{product}</Text></View>
        <View style={styles.invoiceRow}><Text style={styles.invoiceLabel}>Qty × Price</Text><Text style={styles.invoiceValue}>{quantity} × {formatCurrency(Number(unitPrice))}</Text></View>
        <View style={styles.invoiceDivider} />
        <View style={styles.invoiceRow}><Text style={styles.invoiceLabel}>Total</Text><Text style={[styles.invoiceValue, { fontWeight: '800' }]}>{formatCurrency(subtotal)}</Text></View>
        <View style={styles.invoiceRow}><Text style={styles.invoiceLabel}>Paid Now</Text><Text style={[styles.invoiceValue, { color: COLORS.success }]}>{formatCurrency(amountToCollect)}</Text></View>
        {paymentType === 'partial' && (
          <View style={styles.invoiceRow}><Text style={styles.invoiceLabel}>Balance</Text><Text style={[styles.invoiceValue, { color: COLORS.warning }]}>{formatCurrency(remainingBalance)}</Text></View>
        )}
      </View>
      <View style={styles.notifRow}>
        <View style={styles.notifBadge}><Feather name="message-circle" size={16} color={COLORS.success} /><Text style={styles.notifText}>SMS Sent</Text></View>
        <View style={styles.notifBadge}><Feather name="send" size={16} color={COLORS.success} /><Text style={styles.notifText}>WhatsApp Sent</Text></View>
      </View>
      <TouchableOpacity onPress={() => navigation.goBack()} style={{ width: '100%' }}>
        <LinearGradient colors={COLORS.gradientPrimary} style={styles.primaryBtn}>
          <Feather name="home" size={18} color="#FFF" />
          <Text style={styles.primaryBtnText}> Back to Home</Text>
        </LinearGradient>
      </TouchableOpacity>
    </View>
  );

  return (
    <View style={styles.container}>
      <StatusBar barStyle="light-content" />
      <View style={styles.topBar}>
        <TouchableOpacity onPress={() => step > 1 && step < 5 ? goToStep(step - 1) : navigation.goBack()} style={styles.backBtn}>
          <Feather name={step > 1 && step < 5 ? 'arrow-left' : 'x'} size={22} color={COLORS.textPrimary} />
        </TouchableOpacity>
        <Text style={styles.topBarTitle}>Receive Payment</Text>
        <View style={{ width: 40 }} />
      </View>
      {renderStepIndicator()}
      <KeyboardAvoidingView style={{ flex: 1 }} behavior={Platform.OS === 'ios' ? 'padding' : undefined}>
        <ScrollView showsVerticalScrollIndicator={false} contentContainerStyle={{ paddingBottom: 40 }}>
          {step === 1 && renderStep1()}
          {step === 2 && renderStep2()}
          {step === 3 && renderStep3()}
          {step === 4 && renderStep4()}
          {step === 5 && renderStep5()}
        </ScrollView>
      </KeyboardAvoidingView>
    </View>
  );
}

const styles = StyleSheet.create({
  container: { flex: 1, backgroundColor: COLORS.background, paddingTop: 50 },
  topBar: { flexDirection: 'row', alignItems: 'center', justifyContent: 'space-between', paddingHorizontal: SPACING.xl, marginBottom: SPACING.lg },
  backBtn: { width: 40, height: 40, borderRadius: 20, backgroundColor: COLORS.surface, justifyContent: 'center', alignItems: 'center', borderWidth: 1, borderColor: COLORS.cardBorder },
  topBarTitle: { fontSize: FONT_SIZES.large, fontWeight: '700', color: COLORS.textPrimary },
  stepContainer: { paddingHorizontal: SPACING.xl, marginBottom: SPACING.xxl },
  stepTrack: { height: 4, backgroundColor: COLORS.surfaceElevated, borderRadius: 2, marginBottom: SPACING.md },
  stepProgress: { height: 4, backgroundColor: COLORS.primary, borderRadius: 2 },
  stepLabels: { flexDirection: 'row', justifyContent: 'space-between' },
  stepItem: { alignItems: 'center' },
  stepDot: { width: 22, height: 22, borderRadius: 11, backgroundColor: COLORS.surfaceElevated, justifyContent: 'center', alignItems: 'center', marginBottom: 4 },
  stepDotActive: { backgroundColor: COLORS.primary },
  stepDotCurrent: { backgroundColor: COLORS.primary, borderWidth: 2, borderColor: COLORS.primaryLight },
  stepDotText: { fontSize: 10, fontWeight: '700', color: COLORS.textTertiary },
  stepLabel: { fontSize: 9, color: COLORS.textTertiary },
  stepLabelActive: { color: COLORS.primary },
  stepContent: { paddingHorizontal: SPACING.xl },
  stepTitle: { fontSize: FONT_SIZES.title, fontWeight: '700', color: COLORS.textPrimary, marginBottom: SPACING.lg },
  searchContainer: { flexDirection: 'row', alignItems: 'center', backgroundColor: COLORS.surface, borderRadius: BORDER_RADIUS.md, paddingHorizontal: SPACING.lg, borderWidth: 1, borderColor: COLORS.cardBorder, marginBottom: SPACING.md },
  searchInput: { flex: 1, paddingVertical: SPACING.md, color: COLORS.textPrimary, fontSize: FONT_SIZES.body },
  createBtn: { marginBottom: SPACING.lg, borderRadius: BORDER_RADIUS.md, ...SHADOWS.sm },
  createBtnGradient: { flexDirection: 'row', alignItems: 'center', justifyContent: 'center', paddingVertical: SPACING.md, borderRadius: BORDER_RADIUS.md, gap: 8 },
  createBtnText: { color: '#FFF', fontWeight: '600', fontSize: FONT_SIZES.body },
  formCard: { backgroundColor: COLORS.surface, borderRadius: BORDER_RADIUS.lg, padding: SPACING.xl, marginBottom: SPACING.lg, borderWidth: 1, borderColor: COLORS.cardBorder },
  formTitle: { fontSize: FONT_SIZES.medium, fontWeight: '700', color: COLORS.textPrimary, marginBottom: SPACING.lg },
  input: { backgroundColor: COLORS.surfaceElevated, borderRadius: BORDER_RADIUS.sm, paddingHorizontal: SPACING.lg, paddingVertical: SPACING.md, color: COLORS.textPrimary, fontSize: FONT_SIZES.body, marginBottom: SPACING.md, borderWidth: 1, borderColor: COLORS.cardBorder },
  actionBtn: { borderRadius: BORDER_RADIUS.sm, overflow: 'hidden' },
  actionBtnGrad: { paddingVertical: SPACING.md, alignItems: 'center', borderRadius: BORDER_RADIUS.sm },
  actionBtnText: { color: COLORS.textSecondary, textAlign: 'center', paddingVertical: SPACING.md, fontWeight: '600' },
  actionBtnTextWhite: { color: '#FFF', fontWeight: '600' },
  customerItem: { flexDirection: 'row', alignItems: 'center', backgroundColor: COLORS.surface, padding: SPACING.lg, borderRadius: BORDER_RADIUS.md, marginBottom: SPACING.sm, borderWidth: 1, borderColor: COLORS.cardBorder },
  customerAvatar: { width: 42, height: 42, borderRadius: 21, backgroundColor: COLORS.primaryGlow, justifyContent: 'center', alignItems: 'center', marginRight: SPACING.md },
  avatarText: { fontSize: FONT_SIZES.medium, fontWeight: '700', color: COLORS.primary },
  customerName: { fontSize: FONT_SIZES.body, fontWeight: '600', color: COLORS.textPrimary },
  customerMobile: { fontSize: FONT_SIZES.small, color: COLORS.textSecondary, marginTop: 2 },
  selectedBadge: { flexDirection: 'row', alignItems: 'center', backgroundColor: COLORS.primaryGlow, paddingHorizontal: SPACING.md, paddingVertical: SPACING.sm, borderRadius: BORDER_RADIUS.full, alignSelf: 'flex-start', marginBottom: SPACING.lg, gap: 6 },
  selectedBadgeText: { fontSize: FONT_SIZES.small, color: COLORS.primary, fontWeight: '600' },
  subtotalCard: { backgroundColor: COLORS.surface, borderRadius: BORDER_RADIUS.md, padding: SPACING.lg, marginBottom: SPACING.lg, borderWidth: 1, borderColor: COLORS.cardBorder, flexDirection: 'row', justifyContent: 'space-between', alignItems: 'center' },
  subtotalLabel: { fontSize: FONT_SIZES.body, color: COLORS.textSecondary },
  subtotalValue: { fontSize: FONT_SIZES.title, fontWeight: '800', color: COLORS.textPrimary },
  primaryBtn: { flexDirection: 'row', alignItems: 'center', justifyContent: 'center', paddingVertical: SPACING.lg, borderRadius: BORDER_RADIUS.md, gap: 8, ...SHADOWS.md },
  primaryBtnText: { color: '#FFF', fontWeight: '700', fontSize: FONT_SIZES.medium },
  toggleContainer: { flexDirection: 'row', gap: SPACING.md, marginBottom: SPACING.lg },
  toggleBtn: { flex: 1, flexDirection: 'row', alignItems: 'center', justifyContent: 'center', paddingVertical: SPACING.lg, borderRadius: BORDER_RADIUS.md, backgroundColor: COLORS.surface, borderWidth: 1, borderColor: COLORS.cardBorder, gap: 8 },
  toggleActive: { backgroundColor: COLORS.primary, borderColor: COLORS.primary },
  toggleText: { fontSize: FONT_SIZES.body, color: COLORS.textSecondary, fontWeight: '600' },
  toggleTextActive: { color: '#FFF' },
  emiRow: { flexDirection: 'row', alignItems: 'center', justifyContent: 'space-between', marginBottom: SPACING.md },
  emiLabel: { fontSize: FONT_SIZES.body, color: COLORS.textSecondary, flex: 1 },
  balanceInfo: { backgroundColor: COLORS.surfaceHighlight, borderRadius: BORDER_RADIUS.sm, padding: SPACING.md },
  balanceInfoText: { fontSize: FONT_SIZES.small, color: COLORS.textSecondary, marginBottom: 4 },
  qrContainer: { alignItems: 'center', marginBottom: SPACING.xxl },
  qrBox: { backgroundColor: '#FFF', borderRadius: BORDER_RADIUS.xl, padding: SPACING.xxl, alignItems: 'center', ...SHADOWS.lg },
  qrMock: { marginBottom: SPACING.lg },
  qrAmount: { fontSize: FONT_SIZES.heading, fontWeight: '800', color: '#1a1a3e', marginBottom: 4 },
  qrUpi: { fontSize: FONT_SIZES.small, color: '#666' },
  successIcon: { width: 80, height: 80, borderRadius: 40, backgroundColor: COLORS.success, justifyContent: 'center', alignItems: 'center', marginBottom: SPACING.xxl, ...SHADOWS.glow },
  successTitle: { fontSize: FONT_SIZES.heading, fontWeight: '800', color: COLORS.textPrimary, marginBottom: SPACING.sm },
  successSubtitle: { fontSize: FONT_SIZES.medium, color: COLORS.textSecondary, marginBottom: SPACING.xxl },
  invoiceCard: { backgroundColor: COLORS.surface, borderRadius: BORDER_RADIUS.lg, padding: SPACING.xl, width: '100%', marginBottom: SPACING.lg, borderWidth: 1, borderColor: COLORS.cardBorder },
  invoiceRow: { flexDirection: 'row', justifyContent: 'space-between', marginBottom: SPACING.sm },
  invoiceLabel: { fontSize: FONT_SIZES.body, color: COLORS.textSecondary },
  invoiceValue: { fontSize: FONT_SIZES.body, color: COLORS.textPrimary, fontWeight: '600' },
  invoiceDivider: { height: 1, backgroundColor: COLORS.divider, marginVertical: SPACING.sm },
  notifRow: { flexDirection: 'row', gap: SPACING.md, marginBottom: SPACING.xxl },
  notifBadge: { flexDirection: 'row', alignItems: 'center', backgroundColor: COLORS.successLight, paddingHorizontal: SPACING.md, paddingVertical: SPACING.sm, borderRadius: BORDER_RADIUS.full, gap: 6 },
  notifText: { fontSize: FONT_SIZES.small, color: COLORS.success, fontWeight: '600' },
});
