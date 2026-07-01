import React, { useRef, useEffect } from 'react';
import {
  View,
  Text,
  StyleSheet,
  TouchableOpacity,
  ScrollView,
  Animated,
  Dimensions,
  StatusBar,
} from 'react-native';
import { Feather } from '@expo/vector-icons';
import { useAppContext } from '../context/AppContext';
import { COLORS, SPACING, FONT_SIZES, BORDER_RADIUS, SHADOWS } from '../utils/theme';
import { formatCurrency, getGreeting } from '../utils/helpers';
import { LinearGradient } from 'expo-linear-gradient';

const { width } = Dimensions.get('window');

export default function HomeScreen({ navigation }) {
  const { analytics, transactions } = useAppContext();
  const fadeAnim = useRef(new Animated.Value(0)).current;
  const slideAnim = useRef(new Animated.Value(30)).current;
  const scaleAnim1 = useRef(new Animated.Value(0.8)).current;
  const scaleAnim2 = useRef(new Animated.Value(0.8)).current;
  const scaleAnim3 = useRef(new Animated.Value(0.8)).current;

  useEffect(() => {
    Animated.parallel([
      Animated.timing(fadeAnim, {
        toValue: 1,
        duration: 600,
        useNativeDriver: true,
      }),
      Animated.timing(slideAnim, {
        toValue: 0,
        duration: 600,
        useNativeDriver: true,
      }),
    ]).start();

    Animated.stagger(150, [
      Animated.spring(scaleAnim1, { toValue: 1, friction: 6, useNativeDriver: true }),
      Animated.spring(scaleAnim2, { toValue: 1, friction: 6, useNativeDriver: true }),
      Animated.spring(scaleAnim3, { toValue: 1, friction: 6, useNativeDriver: true }),
    ]).start();
  }, []);

  const recentTransactions = transactions.slice(0, 5);

  const ActionTile = ({ icon, label, subtitle, colors, onPress, animValue }) => (
    <Animated.View style={{ transform: [{ scale: animValue }] }}>
      <TouchableOpacity
        onPress={onPress}
        activeOpacity={0.85}
        style={styles.actionTile}
      >
        <LinearGradient
          colors={colors}
          start={{ x: 0, y: 0 }}
          end={{ x: 1, y: 1 }}
          style={styles.actionTileGradient}
        >
          <View style={styles.tileIconContainer}>
            <Feather name={icon} size={28} color="#FFF" />
          </View>
          <Text style={styles.tileLabel}>{label}</Text>
          <Text style={styles.tileSubtitle}>{subtitle}</Text>
        </LinearGradient>
      </TouchableOpacity>
    </Animated.View>
  );

  return (
    <View style={styles.container}>
      <StatusBar barStyle="light-content" backgroundColor={COLORS.background} />
      <ScrollView showsVerticalScrollIndicator={false} contentContainerStyle={styles.scrollContent}>
        {/* Header */}
        <Animated.View style={[styles.header, { opacity: fadeAnim, transform: [{ translateY: slideAnim }] }]}>
          <View>
            <Text style={styles.greeting}>{getGreeting()} 👋</Text>
            <Text style={styles.businessName}>PayWithEase Business</Text>
          </View>
          <TouchableOpacity style={styles.notificationBtn}>
            <Feather name="bell" size={22} color={COLORS.textSecondary} />
            <View style={styles.notifDot} />
          </TouchableOpacity>
        </Animated.View>

        {/* Balance Card */}
        <Animated.View style={{ opacity: fadeAnim, transform: [{ translateY: slideAnim }] }}>
          <LinearGradient
            colors={COLORS.gradientPrimary}
            start={{ x: 0, y: 0 }}
            end={{ x: 1, y: 1 }}
            style={styles.balanceCard}
          >
            <View style={styles.balanceCardOverlay}>
              <Text style={styles.balanceLabel}>Gross Profit</Text>
              <Text style={styles.balanceAmount}>{formatCurrency(analytics.grossProfit)}</Text>
              <View style={styles.balanceRow}>
                <View style={styles.balanceStat}>
                  <Feather name="trending-up" size={14} color="#00D9A6" />
                  <Text style={styles.balanceStatLabel}> Revenue</Text>
                  <Text style={styles.balanceStatValue}>{formatCurrency(analytics.totalRevenue)}</Text>
                </View>
                <View style={styles.balanceDivider} />
                <View style={styles.balanceStat}>
                  <Feather name="trending-down" size={14} color="#FF6B6B" />
                  <Text style={styles.balanceStatLabel}> Costs</Text>
                  <Text style={styles.balanceStatValue}>{formatCurrency(analytics.totalCost)}</Text>
                </View>
              </View>
            </View>
          </LinearGradient>
        </Animated.View>

        {/* Action Tiles */}
        <Text style={styles.sectionTitle}>Quick Actions</Text>
        <View style={styles.tilesContainer}>
          <ActionTile
            icon="download"
            label="Receive"
            subtitle="Collect payments"
            colors={['#00D9A6', '#00B4D8']}
            onPress={() => navigation.navigate('ReceiveFlow')}
            animValue={scaleAnim1}
          />
          <ActionTile
            icon="send"
            label="Pay"
            subtitle="Pay employees & vendors"
            colors={['#FF6B6B', '#FFB547']}
            onPress={() => navigation.navigate('PayFlow')}
            animValue={scaleAnim2}
          />
          <ActionTile
            icon="bar-chart-2"
            label="Dashboard"
            subtitle="Financial analytics"
            colors={['#6C63FF', '#A855F7']}
            onPress={() => navigation.navigate('Analytics')}
            animValue={scaleAnim3}
          />
        </View>

        {/* Quick Stats */}
        <View style={styles.quickStats}>
          <View style={[styles.statCard, { borderLeftColor: COLORS.accent }]}>
            <Text style={styles.statValue}>{formatCurrency(analytics.receivables)}</Text>
            <Text style={styles.statLabel}>Receivables</Text>
          </View>
          <View style={[styles.statCard, { borderLeftColor: COLORS.warning }]}>
            <Text style={styles.statValue}>{formatCurrency(analytics.vendorPayables)}</Text>
            <Text style={styles.statLabel}>Payables</Text>
          </View>
        </View>

        {/* Recent Transactions */}
        <View style={styles.sectionHeader}>
          <Text style={styles.sectionTitle}>Recent Activity</Text>
          <TouchableOpacity onPress={() => navigation.navigate('Analytics')}>
            <Text style={styles.seeAll}>See All →</Text>
          </TouchableOpacity>
        </View>
        {recentTransactions.map((txn, index) => (
          <Animated.View
            key={txn.id}
            style={[
              styles.transactionItem,
              { opacity: fadeAnim },
            ]}
          >
            <View style={[
              styles.txnIcon,
              { backgroundColor: txn.type === 'receive' ? COLORS.successLight : COLORS.errorLight }
            ]}>
              <Feather
                name={txn.type === 'receive' ? 'arrow-down-left' : 'arrow-up-right'}
                size={18}
                color={txn.type === 'receive' ? COLORS.success : COLORS.error}
              />
            </View>
            <View style={styles.txnInfo}>
              <Text style={styles.txnName}>{txn.userName}</Text>
              <Text style={styles.txnDesc}>{txn.description}</Text>
            </View>
            <View style={styles.txnAmountContainer}>
              <Text style={[
                styles.txnAmount,
                { color: txn.type === 'receive' ? COLORS.success : COLORS.error }
              ]}>
                {txn.type === 'receive' ? '+' : '-'}{formatCurrency(txn.amount)}
              </Text>
              <Text style={styles.txnDate}>{txn.date}</Text>
            </View>
          </Animated.View>
        ))}

        <View style={{ height: 100 }} />
      </ScrollView>
    </View>
  );
}

const styles = StyleSheet.create({
  container: {
    flex: 1,
    backgroundColor: COLORS.background,
  },
  scrollContent: {
    paddingHorizontal: SPACING.xl,
    paddingTop: 60,
  },
  header: {
    flexDirection: 'row',
    justifyContent: 'space-between',
    alignItems: 'center',
    marginBottom: SPACING.xxl,
  },
  greeting: {
    fontSize: FONT_SIZES.medium,
    color: COLORS.textSecondary,
    marginBottom: SPACING.xs,
  },
  businessName: {
    fontSize: FONT_SIZES.heading,
    fontWeight: '800',
    color: COLORS.textPrimary,
    letterSpacing: -0.5,
  },
  notificationBtn: {
    width: 44,
    height: 44,
    borderRadius: 22,
    backgroundColor: COLORS.surface,
    justifyContent: 'center',
    alignItems: 'center',
    borderWidth: 1,
    borderColor: COLORS.cardBorder,
  },
  notifDot: {
    position: 'absolute',
    top: 10,
    right: 12,
    width: 8,
    height: 8,
    borderRadius: 4,
    backgroundColor: COLORS.error,
  },
  balanceCard: {
    borderRadius: BORDER_RADIUS.xl,
    marginBottom: SPACING.xxl,
    ...SHADOWS.lg,
  },
  balanceCardOverlay: {
    padding: SPACING.xxl,
    borderRadius: BORDER_RADIUS.xl,
  },
  balanceLabel: {
    fontSize: FONT_SIZES.body,
    color: 'rgba(255,255,255,0.7)',
    marginBottom: SPACING.xs,
    fontWeight: '500',
  },
  balanceAmount: {
    fontSize: FONT_SIZES.display,
    fontWeight: '800',
    color: '#FFF',
    marginBottom: SPACING.lg,
    letterSpacing: -1,
  },
  balanceRow: {
    flexDirection: 'row',
    alignItems: 'center',
  },
  balanceStat: {
    flex: 1,
    flexDirection: 'row',
    alignItems: 'center',
    flexWrap: 'wrap',
  },
  balanceStatLabel: {
    fontSize: FONT_SIZES.small,
    color: 'rgba(255,255,255,0.7)',
  },
  balanceStatValue: {
    fontSize: FONT_SIZES.body,
    color: '#FFF',
    fontWeight: '700',
    marginLeft: SPACING.xs,
  },
  balanceDivider: {
    width: 1,
    height: 30,
    backgroundColor: 'rgba(255,255,255,0.2)',
    marginHorizontal: SPACING.lg,
  },
  sectionTitle: {
    fontSize: FONT_SIZES.large,
    fontWeight: '700',
    color: COLORS.textPrimary,
    marginBottom: SPACING.lg,
  },
  sectionHeader: {
    flexDirection: 'row',
    justifyContent: 'space-between',
    alignItems: 'center',
    marginBottom: SPACING.lg,
  },
  seeAll: {
    fontSize: FONT_SIZES.body,
    color: COLORS.primary,
    fontWeight: '600',
  },
  tilesContainer: {
    flexDirection: 'row',
    justifyContent: 'space-between',
    marginBottom: SPACING.xxl,
    gap: SPACING.md,
  },
  actionTile: {
    flex: 1,
    borderRadius: BORDER_RADIUS.lg,
    ...SHADOWS.md,
  },
  actionTileGradient: {
    flex: 1,
    padding: SPACING.lg,
    borderRadius: BORDER_RADIUS.lg,
    alignItems: 'center',
    minHeight: 130,
    justifyContent: 'center',
  },
  tileIconContainer: {
    width: 52,
    height: 52,
    borderRadius: 26,
    backgroundColor: 'rgba(255,255,255,0.2)',
    justifyContent: 'center',
    alignItems: 'center',
    marginBottom: SPACING.md,
  },
  tileLabel: {
    fontSize: FONT_SIZES.medium,
    fontWeight: '700',
    color: '#FFF',
    marginBottom: SPACING.xs,
  },
  tileSubtitle: {
    fontSize: FONT_SIZES.caption,
    color: 'rgba(255,255,255,0.7)',
    textAlign: 'center',
  },
  quickStats: {
    flexDirection: 'row',
    gap: SPACING.md,
    marginBottom: SPACING.xxl,
  },
  statCard: {
    flex: 1,
    backgroundColor: COLORS.surface,
    borderRadius: BORDER_RADIUS.md,
    padding: SPACING.lg,
    borderLeftWidth: 3,
    borderWidth: 1,
    borderColor: COLORS.cardBorder,
  },
  statValue: {
    fontSize: FONT_SIZES.large,
    fontWeight: '700',
    color: COLORS.textPrimary,
    marginBottom: SPACING.xs,
  },
  statLabel: {
    fontSize: FONT_SIZES.small,
    color: COLORS.textSecondary,
  },
  transactionItem: {
    flexDirection: 'row',
    alignItems: 'center',
    backgroundColor: COLORS.surface,
    padding: SPACING.lg,
    borderRadius: BORDER_RADIUS.md,
    marginBottom: SPACING.sm,
    borderWidth: 1,
    borderColor: COLORS.cardBorder,
  },
  txnIcon: {
    width: 40,
    height: 40,
    borderRadius: 20,
    justifyContent: 'center',
    alignItems: 'center',
    marginRight: SPACING.md,
  },
  txnInfo: {
    flex: 1,
  },
  txnName: {
    fontSize: FONT_SIZES.body,
    fontWeight: '600',
    color: COLORS.textPrimary,
  },
  txnDesc: {
    fontSize: FONT_SIZES.small,
    color: COLORS.textSecondary,
    marginTop: 2,
  },
  txnAmountContainer: {
    alignItems: 'flex-end',
  },
  txnAmount: {
    fontSize: FONT_SIZES.body,
    fontWeight: '700',
  },
  txnDate: {
    fontSize: FONT_SIZES.caption,
    color: COLORS.textTertiary,
    marginTop: 2,
  },
});
