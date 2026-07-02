import React from "react";
import { ActivityIndicator, Pressable, StyleSheet, Text, View } from "react-native";
import { createBottomTabNavigator } from "@react-navigation/bottom-tabs";
import { DashboardScreen } from "../features/dashboard/DashboardScreen";
import { ReceiveScreen } from "../features/receive/ReceiveScreen";
import { PayScreen } from "../features/pay/PayScreen";
import { LoginScreen } from "../features/auth/LoginScreen";
import { useAuth } from "../features/auth/AuthContext";
import { useI18n } from "../features/i18n/I18nContext";
import { BooksNavigator } from "./BooksNavigator";
import { MoreNavigator } from "./MoreNavigator";
import { PendingSyncBadge } from "../components/PendingSyncBadge";
import { theme } from "../theme/theme";

const Tab = createBottomTabNavigator();

function HeaderRight(): React.ReactElement {
  const { logout } = useAuth();
  const { t } = useI18n();
  return (
    <View style={styles.headerRight}>
      <PendingSyncBadge />
      <Pressable accessibilityRole="button" accessibilityLabel={t("common.signOut")} onPress={() => void logout()} style={styles.signOut}>
        <Text style={styles.signOutText}>{t("common.signOut")}</Text>
      </Pressable>
    </View>
  );
}

export function RootNavigator(): React.ReactElement {
  const { ready, authed } = useAuth();
  const { t } = useI18n();

  if (!ready) {
    return (
      <View style={styles.splash}>
        <ActivityIndicator color={theme.color.primary} size="large" />
      </View>
    );
  }

  if (!authed) {
    return <LoginScreen />;
  }

  return (
    <Tab.Navigator
      screenOptions={{
        headerStyle: { backgroundColor: theme.color.surface },
        headerTitleStyle: { color: theme.color.text },
        headerRight: () => <HeaderRight />,
        tabBarStyle: { backgroundColor: theme.color.surface, borderTopColor: theme.color.border },
        tabBarActiveTintColor: theme.color.primary,
        tabBarInactiveTintColor: theme.color.textMuted,
      }}
    >
      <Tab.Screen name="Home" component={DashboardScreen} options={{ title: t("tab.home") }} />
      <Tab.Screen name="Receive" component={ReceiveScreen} options={{ title: t("tab.receive") }} />
      <Tab.Screen name="Pay" component={PayScreen} options={{ title: t("tab.pay") }} />
      <Tab.Screen name="Books" component={BooksNavigator} options={{ headerShown: false, title: t("tab.books") }} />
      <Tab.Screen name="More" component={MoreNavigator} options={{ headerShown: false, title: t("tab.more") }} />
    </Tab.Navigator>
  );
}

const styles = StyleSheet.create({
  splash: { flex: 1, alignItems: "center", justifyContent: "center", backgroundColor: theme.color.bg },
  headerRight: { flexDirection: "row", alignItems: "center" },
  signOut: { paddingHorizontal: theme.space(4) },
  signOutText: { color: theme.color.primary, fontWeight: "600" },
});
